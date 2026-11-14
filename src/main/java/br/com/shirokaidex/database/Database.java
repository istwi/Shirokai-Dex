package br.com.shirokaidex.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

/** The connection and all SQL are confined to one worker. No Minecraft objects cross this boundary. */
public final class Database implements AutoCloseable {
    public static final int SCHEMA_VERSION=14;
    private final Path file;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "shirokaidex-sqlite"));
    private Connection connection;
    private boolean closed;
    private record RankingKey(br.com.shirokaidex.rankings.CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity) { }
    private record CachedRanking(long at,br.com.shirokaidex.rankings.CaptureRankings.Page page) { }
    private final java.util.LinkedHashMap<RankingKey,CachedRanking> rankingCache=new java.util.LinkedHashMap<>(128,0.75f,true);

    private volatile long privacyRevision;
    private java.util.List<br.com.shirokaidex.achievements.AchievementDefinition> preservedAchievements;
    private CompletableFuture<Void> initialization;

    public Database(Path file) { this.file = file; }
    public synchronized CompletableFuture<Void> initialize() {
        if (initialization != null) return initialization;
        initialization = submit(() -> {
            Files.createDirectories(file.toAbsolutePath().getParent());
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys=ON");
                    statement.execute("PRAGMA busy_timeout=5000");
                    statement.execute("PRAGMA journal_mode=WAL");
                    statement.execute("PRAGMA synchronous=FULL");
                }
                br.com.shirokaidex.profiles.ProfileProgress.registerCollation(connection);
                migrate();
                transaction(() -> br.com.shirokaidex.rewards.RewardJournal.recover(connection));
            } catch (Exception failure) { connection.close(); connection = null; throw failure; }
            return null;
        });
        return initialization;
    }

    private void migrate() throws Exception {
        int version;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("PRAGMA user_version")) { version = rs.getInt(1); }
        if (version > SCHEMA_VERSION) throw new SQLException("Database version " + version + " is newer than supported version "+SCHEMA_VERSION);
        if (version > 0) {
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT count(*) FROM schema_migrations WHERE version BETWEEN 1 AND " + version)) {
                if (!rs.next() || rs.getInt(1) != version) throw new SQLException("Missing migration history for version " + version);
            }
        }
        for (int target = version + 1; target <= SCHEMA_VERSION; target++) applyMigration(target);
    }

    private void applyMigration(int version) throws Exception {
        String resource = switch (version) {
            case 1 -> "/database/V001__foundation.sql";
            case 2 -> "/database/V002__category_rules.sql";
            case 3 -> "/database/V003__reward_journal.sql";
            case 4 -> "/database/V004__reward_unlocks.sql";
            case 5 -> "/database/V005__saved_items.sql";
            case 6 -> "/database/V006__captures.sql";
            case 7 -> "/database/V007__native_spawn_proofs.sql";
            case 8 -> "/database/V008__native_source.sql";
            case 9 -> "/database/V009__captured_shiny_forms.sql";
            case 10 -> "/database/V010__capture_receipts.sql";
            case 11 -> "/database/V011__rankings_records.sql";
            case 12 -> "/database/V012__competitions.sql";
            case 13 -> "/database/V013__competition_awards.sql";
            case 14 -> "/database/V014__profiles_achievements.sql";
            default -> throw new SQLException("Unsupported migration " + version);
        };
        String sql;
        try (var input = Database.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Migration is missing: " + resource);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String part : sql.split(";")) if (!part.isBlank()) statement.execute(part);
                if(version==11)br.com.shirokaidex.rankings.CaptureRankings.backfill(connection);
                statement.execute("PRAGMA user_version=" + version);
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO schema_migrations VALUES (?, ?)")) {
                statement.setInt(1, version); statement.setString(2, Instant.now().toString()); statement.executeUpdate();
            }
            connection.commit();
        } catch (Exception failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }

    /** Stores immutable validated rules before config publication; an unactivated candidate may remain for audit. */
    public CompletableFuture<Void> preserveCategoryRules(br.com.shirokaidex.categories.CategoryRules rules) {
        return submit(() -> {
            if (connection == null) throw new SQLException("Database is not initialized");
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO category_rule_versions(fingerprint,format_version,rules_json,first_observed_at) VALUES(?,?,?,?) ON CONFLICT(fingerprint) DO NOTHING")) {
                statement.setString(1, rules.fingerprint()); statement.setInt(2, rules.formatVersion());
                statement.setString(3, rules.json()); statement.setString(4, Instant.now().toString()); statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT format_version,rules_json FROM category_rule_versions WHERE fingerprint=?")) {
                statement.setString(1, rules.fingerprint());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != rules.formatVersion() || !rs.getString(2).equals(rules.json()))
                        throw new SQLException("Category rule fingerprint conflicts with stored content");
                }
            }
            return null;
        });
    }

    public CompletableFuture<java.util.Optional<br.com.shirokaidex.categories.CategoryRules>> categoryRules(String fingerprint) {
        if (fingerprint == null || !fingerprint.matches("[a-f0-9]{64}"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid category rule fingerprint"));
        return submit(() -> {
            if (connection == null) throw new SQLException("Database is not initialized");
            try (PreparedStatement statement = connection.prepareStatement("SELECT format_version,rules_json FROM category_rule_versions WHERE fingerprint=?")) {
                statement.setString(1, fingerprint);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) return java.util.Optional.empty();
                    return java.util.Optional.of(new br.com.shirokaidex.categories.CategoryRules(rs.getInt(1), rs.getString(2), fingerprint));
                }
            }
        });
    }

    public CompletableFuture<Void> recordLogin(UUID player, String name, Instant time) {
        return submit(() -> {
            if (connection == null) throw new SQLException("Database is not initialized");
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO players(uuid,last_known_name,first_join_at,last_seen_at) VALUES(?,?,?,?)
                    ON CONFLICT(uuid) DO UPDATE SET last_known_name=excluded.last_known_name,last_seen_at=excluded.last_seen_at
                    """)) {
                statement.setString(1, player.toString()); statement.setString(2, name);
                statement.setString(3, time.toString()); statement.setString(4, time.toString()); statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<java.util.Optional<br.com.shirokaidex.rewards.SavedItem>> savedItem(String id) {
        return submit(() -> readSavedItem(id));
    }
    private java.util.Optional<br.com.shirokaidex.rewards.SavedItem> readSavedItem(String id) throws SQLException {
        try(PreparedStatement s=connection.prepareStatement("SELECT * FROM saved_items WHERE id=?")){
            s.setString(1,id);try(ResultSet rs=s.executeQuery()){
                if(!rs.next())return java.util.Optional.empty();
                return java.util.Optional.of(new br.com.shirokaidex.rewards.SavedItem(rs.getString("id"),rs.getString("item_id"),rs.getInt("item_count"),rs.getString("payload"),rs.getString("checksum")));
            }
        }
    }
    private java.util.Optional<String> rawSavedItem(String id)throws SQLException {
        try(PreparedStatement s=connection.prepareStatement("SELECT * FROM saved_items WHERE id=?")){
            s.setString(1,id);try(ResultSet rs=s.executeQuery()){
                if(!rs.next())return java.util.Optional.empty();
                var raw=new com.google.gson.JsonObject();raw.addProperty("id",rs.getString("id"));raw.addProperty("item",rs.getString("item_id"));raw.addProperty("count",rs.getInt("item_count"));raw.addProperty("data",rs.getString("payload"));raw.addProperty("checksum",rs.getString("checksum"));raw.addProperty("updated_by",rs.getString("updated_by"));raw.addProperty("updated_at",rs.getString("updated_at"));
                return java.util.Optional.of(raw.toString());
            }
        }
    }
    public CompletableFuture<java.util.List<String>> savedItems() {
        return submit(() -> {
            var ids=new java.util.ArrayList<String>();
            try(Statement s=connection.createStatement();ResultSet rs=s.executeQuery("SELECT id FROM saved_items ORDER BY id")){while(rs.next())ids.add(rs.getString(1));}
            return java.util.List.copyOf(ids);
        });
    }
    public CompletableFuture<Void> saveItem(br.com.shirokaidex.rewards.SavedItem item,UUID actor) {
        return submit(() -> transaction(() -> {
            var previous=rawSavedItem(item.id());
            if(previous.isEmpty())try(Statement s=connection.createStatement();ResultSet rs=s.executeQuery("SELECT count(*) FROM saved_items")){
                if(rs.getInt(1)>=256)throw new SQLException("Maximum 256 saved item templates");
            }
            try(PreparedStatement s=connection.prepareStatement("INSERT INTO saved_items VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET item_id=excluded.item_id,item_count=excluded.item_count,payload=excluded.payload,checksum=excluded.checksum,updated_by=excluded.updated_by,updated_at=excluded.updated_at")){
                s.setString(1,item.id());s.setString(2,item.item());s.setInt(3,item.count());s.setString(4,item.data());s.setString(5,item.checksum());s.setString(6,actor.toString());s.setString(7,Instant.now().toString());s.executeUpdate();
            }
            savedItemAudit(item.id(),"SAVE",actor,previous.orElse(null),item);return null;
        }));
    }
    public CompletableFuture<Boolean> deleteItem(String id,UUID actor) {
        return submit(() -> transaction(() -> {
            var previous=rawSavedItem(id);if(previous.isEmpty())return false;
            try(PreparedStatement s=connection.prepareStatement("DELETE FROM saved_items WHERE id=?")){s.setString(1,id);s.executeUpdate();}
            savedItemAudit(id,"DELETE",actor,previous.get(),null);return true;
        }));
    }
    private void savedItemAudit(String id,String action,UUID actor,String previous,br.com.shirokaidex.rewards.SavedItem next)throws SQLException {
        try(PreparedStatement s=connection.prepareStatement("INSERT INTO saved_item_audit(item_id,action,actor,previous_payload,new_payload,recorded_at) VALUES(?,?,?,?,?,?)")){
            var gson=new com.google.gson.Gson();
            s.setString(1,id);s.setString(2,action);s.setString(3,actor.toString());s.setString(4,previous);s.setString(5,next==null?null:gson.toJson(next));s.setString(6,Instant.now().toString());s.executeUpdate();
        }
    }

    public CompletableFuture<java.util.Set<String>> rewardUnlocks(UUID player, java.util.List<br.com.shirokaidex.rewards.RewardUnlock> observed) {
        var immutable = java.util.List.copyOf(observed);
        return submit(() -> transaction(() -> {
            for (var unlock : immutable) {
                try (PreparedStatement s = connection.prepareStatement("INSERT INTO reward_unlocks VALUES(?,?,?,?,?,?) ON CONFLICT(player_uuid,reward_id) DO NOTHING")) {
                    s.setString(1,player.toString()); s.setString(2,unlock.plan().id()); s.setString(3,unlock.plan().json());
                    s.setInt(4,unlock.registered()); s.setInt(5,unlock.total()); s.setString(6,Instant.now().toString()); s.executeUpdate();
                }
            }
            java.util.Set<String> ids = new java.util.HashSet<>();
            try (PreparedStatement s = connection.prepareStatement("SELECT reward_id FROM reward_unlocks WHERE player_uuid=?")) {
                s.setString(1,player.toString()); try(ResultSet rs=s.executeQuery()){while(rs.next())ids.add(rs.getString(1));}
            }
            return java.util.Set.copyOf(ids);
        }));
    }

    public CompletableFuture<java.util.Set<br.com.shirokaidex.pokedex.CapturedShinyForm>> capturedShinyForms(UUID player){return submit(()->br.com.shirokaidex.captures.CaptureLedger.shinyForms(connection,player));}
    public CompletableFuture<Boolean> recordNativeSpawnProof(br.com.shirokaidex.captures.NativeSpawnProof proof){return submit(()->transaction(()->br.com.shirokaidex.captures.NativeProvenance.observe(connection,proof)));}
    public CompletableFuture<br.com.shirokaidex.rankings.CaptureRankings.Page> captureRanking(br.com.shirokaidex.rankings.CaptureRanking definition,int page){return captureRanking(definition,page,br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy(),null);}
    public CompletableFuture<br.com.shirokaidex.rankings.CaptureRankings.Page> captureRanking(br.com.shirokaidex.rankings.CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version){return captureRanking(definition,page,privacy,version,null);}
    public CompletableFuture<br.com.shirokaidex.rankings.CaptureRankings.Page> captureRanking(br.com.shirokaidex.rankings.CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity){return submit(()->rankingPage(definition,page,privacy,version,sensitivity));}
    public record RankingView(br.com.shirokaidex.rankings.CaptureRankings.Page page,long privacyRevision) { }
    public CompletableFuture<RankingView> captureRankingView(br.com.shirokaidex.rankings.CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity){return submit(()->new RankingView(rankingPage(definition,page,privacy,version,sensitivity),privacyRevision));}
    private br.com.shirokaidex.rankings.CaptureRankings.Page rankingPage(br.com.shirokaidex.rankings.CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity)throws SQLException {
        var key=new RankingKey(definition,page,privacy,version,sensitivity);var cached=rankingCache.get(key);long now=System.nanoTime();
        if(cached!=null&&now-cached.at()<TimeUnit.SECONDS.toNanos(5))return cached.page();
        var value=br.com.shirokaidex.rankings.CaptureRankings.page(connection,definition,page,privacy,version,sensitivity);rankingCache.put(key,new CachedRanking(now,value));
        if(rankingCache.size()>128)rankingCache.remove(rankingCache.keySet().iterator().next());return value;
    }
    public CompletableFuture<Void> rankingPrivacy(UUID player,boolean hidden){return submit(()->{br.com.shirokaidex.rankings.CaptureRankings.privacy(connection,player,hidden);rankingCache.clear();privacyRevision++;return null;});}
    public CompletableFuture<java.util.List<br.com.shirokaidex.captures.CaptureRecords.Best>> captureRecords(UUID player,java.time.ZoneId zone){return submit(()->transaction(()->br.com.shirokaidex.captures.CaptureRecords.best(connection,player,zone)));}
    public record PublicPrivacy(java.util.Map<br.com.shirokaidex.profiles.StatisticsPrivacy.Key,Boolean> visible,boolean rankingHidden,long privacyRevision) {public PublicPrivacy{visible=java.util.Map.copyOf(visible);}}
    public CompletableFuture<PublicPrivacy> publicPrivacy(UUID player,br.com.shirokaidex.profiles.StatisticsPrivacy policy){return submit(()->{
        var preferences=br.com.shirokaidex.profiles.StatisticsPrivacy.preferences(connection,player);var values=new java.util.EnumMap<br.com.shirokaidex.profiles.StatisticsPrivacy.Key,Boolean>(br.com.shirokaidex.profiles.StatisticsPrivacy.Key.class);for(var key:br.com.shirokaidex.profiles.StatisticsPrivacy.Key.values())values.put(key,policy.visible(key,preferences));boolean hidden=false;
        try(var q=connection.prepareStatement("SELECT hidden FROM player_ranking_privacy WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){if(rows.next())hidden=rows.getBoolean(1);}}
        return new PublicPrivacy(values,hidden,privacyRevision);
    });}
    public long privacyRevision(){return privacyRevision;}
    public CompletableFuture<java.util.Map<br.com.shirokaidex.profiles.StatisticsPrivacy.Key,Boolean>> statisticsPrivacy(UUID player){return submit(()->br.com.shirokaidex.profiles.StatisticsPrivacy.preferences(connection,player));}
    public CompletableFuture<Void> statisticsPrivacy(UUID player,br.com.shirokaidex.profiles.StatisticsPrivacy.Key key,boolean visible){return submit(()->{br.com.shirokaidex.profiles.StatisticsPrivacy.set(connection,player,key,visible);rankingCache.clear();privacyRevision++;return null;});}
    public record ProfileView(java.util.Optional<br.com.shirokaidex.profiles.ProfileStore.Data> data,long privacyRevision) { }
    public CompletableFuture<ProfileView> profile(String input,UUID viewer,br.com.shirokaidex.config.Settings settings,java.util.Map<String,String> versions,java.util.Set<String> visibleCollections,java.util.List<br.com.shirokaidex.rankings.CaptureRanking> visibleRankings,Instant now){return submit(()->transaction(()->new ProfileView(br.com.shirokaidex.profiles.ProfileStore.view(connection,input,viewer,settings.statisticsPrivacy(),versions,visibleCollections,settings.collections().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(br.com.shirokaidex.pokedex.CollectionDefinition::id,br.com.shirokaidex.pokedex.CollectionDefinition::mode)),settings.achievements(),visibleRankings,now,settings.statisticsTimezone()),privacyRevision)));}
    public CompletableFuture<java.util.List<String>> observeProfile(UUID player,java.util.List<br.com.shirokaidex.profiles.ProfileProgress.Proof> progress,br.com.shirokaidex.config.Settings settings,Instant now){return submit(()->{
        boolean preserve=!settings.achievements().equals(preservedAchievements);
        var result=transaction(()->{
            if(preserve)br.com.shirokaidex.achievements.AchievementStore.preserve(connection,settings.achievements(),now);
            if(br.com.shirokaidex.profiles.ProfileProgress.observe(connection,player,progress))rankingCache.clear();
            return br.com.shirokaidex.achievements.AchievementStore.evaluate(connection,player,settings.achievements(),progress,now);
        });
        if(preserve)preservedAchievements=java.util.List.copyOf(settings.achievements());return result;
    });}
    public CompletableFuture<java.util.List<br.com.shirokaidex.achievements.AchievementStore.Medal>> medals(UUID player){return submit(()->br.com.shirokaidex.achievements.AchievementStore.medals(connection,player));}
    public CompletableFuture<Boolean> favoriteMedal(UUID player,String achievement,int slot){return submit(()->transaction(()->br.com.shirokaidex.achievements.AchievementStore.favorite(connection,player,achievement,slot)));}
    public CompletableFuture<br.com.shirokaidex.profiles.ProfileStore.Preferences> preferences(UUID player){return submit(()->br.com.shirokaidex.profiles.ProfileStore.preferences(connection,player));}
    public CompletableFuture<Void> notificationPreference(UUID player,boolean enabled){return submit(()->{br.com.shirokaidex.profiles.ProfileStore.notifications(connection,player,enabled);return null;});}
    public record PersonalSettings(java.util.Map<br.com.shirokaidex.profiles.StatisticsPrivacy.Key,Boolean> privacy,boolean rankingHidden,br.com.shirokaidex.profiles.ProfileStore.Preferences preferences) { }
    public CompletableFuture<PersonalSettings> personalSettings(UUID player){return submit(()->{boolean hidden=false;try(var q=connection.prepareStatement("SELECT hidden FROM player_ranking_privacy WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){if(rows.next())hidden=rows.getBoolean(1);}}return new PersonalSettings(br.com.shirokaidex.profiles.StatisticsPrivacy.preferences(connection,player),hidden,br.com.shirokaidex.profiles.ProfileStore.preferences(connection,player));});}
    public record AwardView(br.com.shirokaidex.competitions.CompetitionAwards.Award award,java.util.Optional<br.com.shirokaidex.rewards.RewardJournal.Operation> operation) { }
    public CompletableFuture<java.util.List<AwardView>> competitionAwardViews(UUID player,int page){return submit(()->{
        var result=new java.util.ArrayList<AwardView>();for(var award:br.com.shirokaidex.competitions.CompetitionAwards.list(connection,player,page))result.add(new AwardView(award,br.com.shirokaidex.rewards.RewardJournal.findForReward(connection,player,award.id())));return java.util.List.copyOf(result);
    });}
    public CompletableFuture<java.util.List<br.com.shirokaidex.competitions.CompetitionAwards.Award>> competitionAwards(UUID player,int page){return submit(()->br.com.shirokaidex.competitions.CompetitionAwards.list(connection,player,page));}
    public CompletableFuture<java.util.Optional<br.com.shirokaidex.competitions.CompetitionAwards.Award>> competitionAward(String id,UUID player){return submit(()->br.com.shirokaidex.competitions.CompetitionAwards.find(connection,id,player));}
    public CompletableFuture<br.com.shirokaidex.competitions.CompetitionStore.HistoryPage> competitionHistory(br.com.shirokaidex.competitions.CompetitionStore.HistoryFilter filter,int page){return submit(()->br.com.shirokaidex.competitions.CompetitionStore.history(connection,filter,page));}
    public CompletableFuture<java.util.List<String>> competitionAdminRows(String edition,String mode,String event,int page){return submit(()->br.com.shirokaidex.competitions.CompetitionStore.adminRows(connection,edition,mode,event,page));}
    public CompletableFuture<Void> synchronizeCompetitions(br.com.shirokaidex.config.Settings settings,Instant now){return submit(()->transaction(()->{br.com.shirokaidex.competitions.CompetitionStore.synchronize(connection,settings.competitions(),settings.categories(),settings.rewards(),now);return null;}));}
    public CompletableFuture<java.util.List<br.com.shirokaidex.competitions.CompetitionStore.Edition>> competitionEditions(boolean history){return submit(()->br.com.shirokaidex.competitions.CompetitionStore.editions(connection,history));}
    public CompletableFuture<Boolean> enrollCompetition(String edition,UUID player,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.enroll(connection,edition,player,now)));}
    public CompletableFuture<Boolean> cancelCompetition(String edition,UUID player,boolean confirmed,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.cancel(connection,edition,player,confirmed,now)));}
    public record CompetitionView(br.com.shirokaidex.competitions.CompetitionStore.Edition edition,br.com.shirokaidex.competitions.CompetitionStore.Membership membership,java.util.List<br.com.shirokaidex.competitions.CompetitionStore.Standing> standings,long privacyRevision) { }
    public CompletableFuture<java.util.Optional<CompetitionView>> competitionView(String id,UUID player,int page){return competitionView(id,player,page,br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy());}
    public CompletableFuture<java.util.Optional<CompetitionView>> competitionView(String id,UUID player,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy){return submit(()->{
        var edition=br.com.shirokaidex.competitions.CompetitionStore.edition(connection,id);if(edition.isEmpty())return java.util.Optional.empty();
        return java.util.Optional.of(new CompetitionView(edition.get(),br.com.shirokaidex.competitions.CompetitionStore.membership(connection,id,player),br.com.shirokaidex.competitions.CompetitionStore.standings(connection,id,page,edition.get().state()==br.com.shirokaidex.competitions.CompetitionStore.State.FINAL,privacy,edition.get().definition().metric()),privacyRevision));
    });}
    public CompletableFuture<Boolean> reviewCompetitionPoint(String edition,String event,String state,String actor,String reason,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.reviewPoint(connection,edition,event,state,actor,reason,now)));}
    public CompletableFuture<java.util.Optional<String>> adjustCompetition(String edition,UUID player,long delta,String actor,String reason,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.adjust(connection,edition,player,delta,null,actor,reason,now)));}
    public CompletableFuture<java.util.Optional<String>> revertCompetitionAdjustment(String id,String actor,String reason,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.revertAdjustment(connection,id,actor,reason,now)));}
    public CompletableFuture<Boolean> finalizeCompetition(String edition,String actor,String reason,Instant now){return submit(()->transaction(()->br.com.shirokaidex.competitions.CompetitionStore.finalizeEdition(connection,edition,actor,reason,now)));}
    public CompletableFuture<Boolean> recordObservedCapture(br.com.shirokaidex.captures.CaptureObservation observation,br.com.shirokaidex.config.Settings settings){return submit(()->captureTransaction(()->{
        br.com.shirokaidex.competitions.CompetitionStore.synchronize(connection,settings.competitions(),settings.categories(),settings.rewards(),observation.capture().capturedAt());
        boolean retainDetail=settings.captureHistory().enabled()||br.com.shirokaidex.competitions.CompetitionStore.needsCaptureDetail(connection,observation.capture().player(),observation.capture().capturedAt());
        boolean inserted=br.com.shirokaidex.captures.NativeProvenance.capture(connection,observation,retainDetail);
        if(inserted){try(var q=connection.prepareStatement("SELECT verified FROM capture_receipts WHERE event_key=?")){q.setString(1,observation.capture().eventKey());try(var rows=q.executeQuery()){if(rows.next()&&rows.getBoolean(1))br.com.shirokaidex.competitions.CompetitionStore.capture(connection,observation.capture().withOrigin(br.com.shirokaidex.captures.CaptureRecord.Origin.NATIVE_PLAYER_SPAWN),settings.competitions(),settings.categories());}}}return inserted;
    }));}
    public CompletableFuture<Integer> cleanupCaptureHistory(br.com.shirokaidex.captures.CaptureRetention policy,Instant now){return submit(()->transaction(()->policy.cleanup(connection,now)));}
    public CompletableFuture<Boolean> recordObservedCapture(br.com.shirokaidex.captures.CaptureObservation observation,boolean historyEnabled){return submit(()->captureTransaction(()->br.com.shirokaidex.captures.NativeProvenance.capture(connection,observation,historyEnabled)));}
    public CompletableFuture<Boolean> recordObservedCapture(br.com.shirokaidex.captures.CaptureObservation observation){return submit(()->captureTransaction(()->br.com.shirokaidex.captures.NativeProvenance.capture(connection,observation)));}
    public CompletableFuture<Boolean> recordCapture(br.com.shirokaidex.captures.CaptureRecord record){return submit(()->captureTransaction(()->br.com.shirokaidex.captures.CaptureLedger.record(connection,record)));}
    public CompletableFuture<br.com.shirokaidex.captures.CaptureLedger.Summary> captureSummary(UUID player,Instant now,java.time.ZoneId zone){return submit(()->br.com.shirokaidex.captures.CaptureLedger.summary(connection,player,now,zone));}
    public CompletableFuture<br.com.shirokaidex.captures.CaptureHistory.Page> captureHistory(UUID player,int page){return submit(()->br.com.shirokaidex.captures.CaptureHistory.read(connection,player,page));}
    public CompletableFuture<Long> captureCount(UUID player,String kind,String key){return submit(()->br.com.shirokaidex.captures.CaptureLedger.count(connection,player,kind,key));}

    public CompletableFuture<br.com.shirokaidex.rewards.RewardJournal.Operation> reserveReward(UUID player, br.com.shirokaidex.rewards.RewardPlan plan) {
        return reserveReward(player,plan,player.toString());
    }
    public CompletableFuture<br.com.shirokaidex.rewards.RewardJournal.Operation> reserveReward(UUID player, br.com.shirokaidex.rewards.RewardPlan plan, String actor) {
        return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.reserve(connection, player, plan,actor)));
    }
    public CompletableFuture<Boolean> rewardResumable(UUID id){return submit(()->br.com.shirokaidex.rewards.RewardJournal.resumable(connection,id));}
    public CompletableFuture<java.util.Optional<br.com.shirokaidex.rewards.RewardJournal.Operation>> authorizeRewardResume(UUID id,UUID player,br.com.shirokaidex.rewards.RewardPlan plan,String actor,String reason){
        return submit(()->transaction(()->br.com.shirokaidex.rewards.RewardJournal.authorizeResume(connection,id,player,plan,actor,reason)));
    }
    public CompletableFuture<java.util.Optional<br.com.shirokaidex.rewards.RewardJournal.Operation>> rewardOperation(UUID id) {
        return submit(() -> br.com.shirokaidex.rewards.RewardJournal.find(connection, id));
    }
    public CompletableFuture<java.util.Map<String,br.com.shirokaidex.rewards.RewardJournal.State>> rewardStates(UUID player,java.util.Collection<String> rewards){
        var ids=java.util.List.copyOf(new java.util.TreeSet<>(rewards));
        if(ids.size()>128)return CompletableFuture.failedFuture(new IllegalArgumentException("maximum 128 reward states"));
        if(ids.isEmpty())return CompletableFuture.completedFuture(java.util.Map.of());
        return submit(()->{
            var result=new java.util.HashMap<String,br.com.shirokaidex.rewards.RewardJournal.State>();
            String marks=String.join(",",java.util.Collections.nCopies(ids.size(),"?"));
            try(PreparedStatement s=connection.prepareStatement("SELECT reward_id,state FROM reward_operations WHERE player_uuid=? AND reward_id IN ("+marks+")")){
                s.setString(1,player.toString());for(int i=0;i<ids.size();i++)s.setString(i+2,ids.get(i));
                try(ResultSet rs=s.executeQuery()){while(rs.next())result.put(rs.getString(1),br.com.shirokaidex.rewards.RewardJournal.State.valueOf(rs.getString(2)));}
            }return java.util.Map.copyOf(result);
        });
    }

    public CompletableFuture<java.util.List<br.com.shirokaidex.rewards.RewardJournal.Operation>> incompleteRewards(int page){
        return submit(()->br.com.shirokaidex.rewards.RewardJournal.incomplete(connection,page));
    }
    public CompletableFuture<java.util.Optional<br.com.shirokaidex.rewards.RewardJournal.Operation>> rewardForPlayer(UUID player,String reward){
        return submit(()->br.com.shirokaidex.rewards.RewardJournal.findForReward(connection,player,reward));
    }
    public CompletableFuture<Boolean> beginReward(UUID id) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.begin(connection,id))); }
    public CompletableFuture<Boolean> rewardIntent(UUID id, int step) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.intent(connection,id,step))); }
    public CompletableFuture<Boolean> rewardDelivered(UUID id, int step, String evidence) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.delivered(connection,id,step,evidence))); }
    public CompletableFuture<Boolean> completeReward(UUID id) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.complete(connection,id))); }
    public CompletableFuture<Boolean> reviewReward(UUID id, String reason) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.review(connection,id,reason))); }
    public CompletableFuture<Boolean> cancelReward(UUID id, String actor, String reason) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.cancel(connection,id,actor,reason))); }
    public CompletableFuture<Boolean> resolveReward(UUID id, String actor, String evidence) { return submit(() -> transaction(() -> br.com.shirokaidex.rewards.RewardJournal.resolveCompleted(connection,id,actor,evidence))); }

    private boolean captureTransaction(Callable<Boolean> action)throws Exception {
        boolean inserted=transaction(action);if(inserted)rankingCache.clear();return inserted;
    }

    private <T> T transaction(Callable<T> action) throws Exception {
        if (connection == null) throw new SQLException("Database is not initialized");
        connection.setAutoCommit(false);
        try { T result = action.call(); connection.commit(); return result; }
        catch (Exception failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }

    private synchronized <T> CompletableFuture<T> submit(Callable<T> action) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Database is closed"));
        return CompletableFuture.supplyAsync(() -> {
            try { return action.call(); }
            catch (Exception failure) { throw new CompletionException(failure); }
        }, worker);
    }

    @Override public void close() {
        Future<?> closing;
        synchronized (this) {
            if (closed) return;
            closing = worker.submit(() -> {
                if (connection != null) {
                    try { connection.close(); }
                    catch (SQLException failure) { throw new CompletionException(failure); }
                    finally { connection = null; }
                }
            });
            closed = true;
            worker.shutdown();
        }
        try {
            closing.get(15, TimeUnit.SECONDS);
            if (!worker.awaitTermination(15, TimeUnit.SECONDS)) throw new IllegalStateException("Database shutdown timed out");
        } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Database shutdown interrupted", failure); }
        catch (ExecutionException | TimeoutException failure) { throw new IllegalStateException("Database close failed", failure); }
    }
}
