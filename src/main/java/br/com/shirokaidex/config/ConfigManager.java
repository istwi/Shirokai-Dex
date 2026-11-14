package br.com.shirokaidex.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import java.util.concurrent.atomic.AtomicReference;

/** Reads/writes must be called from the configuration executor, never a menu callback. */
public final class ConfigManager {
    private final Path directory;
    public record State(Settings settings,DirectMenuCatalog menus) { }
    private final AtomicReference<State> current = new AtomicReference<>();
    private final boolean directMode;
    @FunctionalInterface public interface MenuValidator {void validate(DirectMenuCatalog menus)throws IOException;}
    private MenuValidator menuValidator=menus->{};
    public synchronized void setMenuValidator(MenuValidator validator){menuValidator=java.util.Objects.requireNonNull(validator);}
    public State snapshot(){return current.get();}
    public DirectMenuCatalog directMenus(){return snapshot().menus();}
    private volatile boolean recovered;
    @FunctionalInterface public interface Validator { void validate(Settings settings) throws IOException; }
    private Validator validator = settings -> { };
    private java.util.function.Function<String,br.com.shirokaidex.rewards.SavedItem> itemResolver = id -> null;
    public synchronized void setItemResolver(java.util.function.Function<String,br.com.shirokaidex.rewards.SavedItem> resolver) { itemResolver=java.util.Objects.requireNonNull(resolver); }

    public ConfigManager(Path directory){this(directory,false);}
    public ConfigManager(Path directory,boolean directMode){this.directory=directory;this.directMode=directMode;}
    public Settings current() {
        State state=current.get();Settings settings=state==null?null:state.settings();
        if (settings == null) throw new IllegalStateException("Configuration has not initialized");
        return settings;
    }
    public boolean recoveredFromBackup() { return recovered; }
    public synchronized void setValidator(Validator validator) { this.validator = java.util.Objects.requireNonNull(validator); }

    public synchronized void initialize() throws IOException {
        Files.createDirectories(directory);
        Path config = directory.resolve("config.json");
        if (Files.notExists(config)) {
            try(var input=ConfigManager.class.getResourceAsStream("/config/focused.json")){if(input==null)throw new IOException("Missing default preset");writeAtomic(config,new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        }
        if(directMode){DirectMenuCatalog.initialize(directory);try{reload();}catch(IOException failure){try{recoverDirect();}catch(IOException invalid){failure.addSuppressed(invalid);throw failure;}}return;}
        try { reload(); }
        catch (IOException failure) {
            Path backup = directory.resolve("config.last-good.json");
            if (!Files.isRegularFile(backup)) throw failure;
            try {
                Settings fallback = parse(readBounded(backup,524288),itemResolver); validator.validate(fallback);
                current.set(new State(fallback,null)); recovered = true;
            }
            catch (IOException invalidBackup) { failure.addSuppressed(invalidBackup); throw failure; }
        }
    }

    public synchronized Settings reload() throws IOException {
        if(directMode)return reloadDirect();
        String json = readBounded(directory.resolve("config.json"));
        Settings candidate = parse(json,itemResolver);
        validator.validate(candidate);
        // Persist the validated recovery copy before publishing the immutable candidate.
        writeAtomic(directory.resolve("config.last-good.json"), resolvedBackup(json,candidate));
        current.set(new State(candidate,null));
        recovered = false;
        return candidate;
    }

    private Settings reloadDirect()throws IOException {
        var catalog=DirectMenuCatalog.load(directory);String effective=catalog.effective(readBounded(directory.resolve("config.json")));
        Settings candidate=parse(effective,itemResolver);menuValidator.validate(catalog);validator.validate(candidate);
        var backup=new com.google.gson.JsonObject();backup.addProperty("config",effective);var files=new com.google.gson.JsonObject();catalog.rawFiles().forEach(files::addProperty);backup.add("menus",files);writeAtomic(directory.resolve("direct.last-good.json"),backup.toString());
        current.set(new State(candidate,catalog));recovered=false;return candidate;
    }
    private void recoverDirect()throws IOException {
        var backup=DirectMenuCatalog.object(readBounded(directory.resolve("direct.last-good.json"),4194304));var files=new java.util.HashMap<String,String>();for(var entry:backup.getAsJsonObject("menus").entrySet())files.put(entry.getKey(),entry.getValue().getAsString());var catalog=DirectMenuCatalog.parse(files);var settings=parse(backup.get("config").getAsString(),itemResolver);menuValidator.validate(catalog);validator.validate(settings);current.set(new State(settings,catalog));recovered=true;
    }
    private static String readBounded(Path path) throws IOException { return readBounded(path,65536); }
    private static String readBounded(Path path,int maximum) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(maximum+1);
            if (bytes.length > maximum) throw new IOException(path.getFileName() + ": maximum size exceeded");
            try { return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
            catch (java.nio.charset.CharacterCodingException failure) { throw new IOException(path.getFileName() + ": invalid UTF-8", failure); }
        }
    }

    private static String resolvedBackup(String json,Settings settings)throws IOException {
        var root=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if(!root.has("rewards"))return json;
        var rewards=root.getAsJsonArray("rewards");boolean changed=false;
        for(int i=0;i<rewards.size();i++){
            var items=rewards.get(i).getAsJsonObject().getAsJsonArray("items");if(items==null)continue;
            for(int j=0;j<items.size();j++){
                var raw=items.get(j).getAsJsonObject();if(!raw.has("saved_item"))continue;
                var item=settings.rewards().get(i).items().get(j);raw.remove("saved_item");
                raw.addProperty("type","CUSTOM_ITEM");raw.addProperty("id",item.id());raw.addProperty("count",item.count());raw.addProperty("payload_snbt",item.data());changed=true;
            }
        }
        String backup=changed?root.toString():json;
        if(backup.getBytes(StandardCharsets.UTF_8).length>524288)throw new IOException("Resolved recovery backup exceeds 512 KiB");
        return backup;
    }

    public static Settings parse(String json) throws IOException { return parse(json,id -> null); }
    public static Settings parse(String json, java.util.function.Function<String,br.com.shirokaidex.rewards.SavedItem> resolver) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            Set<String> keys = new HashSet<>();
            Integer version = null;
            String language = null, title = null;
            List<CollectionDefinition> collections = CollectionDefinition.defaults();
            List<br.com.shirokaidex.rewards.RewardPlan> rewards = List.of();
            var notifications=br.com.shirokaidex.notifications.NotificationPolicy.defaults();
            var customPlaceholders=java.util.List.<br.com.shirokaidex.placeholders.CustomPlaceholder>of();
            var achievements=java.util.List.<br.com.shirokaidex.achievements.AchievementDefinition>of();
            var statisticsPrivacy=br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy();
            var competitions=java.util.List.<br.com.shirokaidex.competitions.CompetitionDefinition>of();
            java.util.List<br.com.shirokaidex.rankings.CaptureRanking> rankings=null;
            var captureHistory=br.com.shirokaidex.captures.CaptureRetention.defaults();
            var statisticsTimezone=java.time.ZoneId.of("UTC");
            var commandPolicy=br.com.shirokaidex.rewards.CommandPolicy.disabled();
            var home=br.com.shirokaidex.menus.layout.HomeLayout.defaults();
            var menus=br.com.shirokaidex.menus.layout.MenuNavigation.defaults();
            var categories = new br.com.shirokaidex.categories.CategoryCatalog(List.of());
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (!keys.add(key)) throw new IOException("config.json: duplicate field " + key);
                switch (key) {
                    case "schema_version" -> {
                        if (reader.peek() != JsonToken.NUMBER) throw new IOException("schema_version must be an integer");
                        String value = reader.nextString();
                        if (!value.equals("1")) throw new IOException("Unsupported schema_version: " + value);
                        version = 1;
                    }
                    case "language" -> language = string(reader, key);
                    case "menu_title" -> title = string(reader, key);
                    case "statistics_timezone"->{try{statisticsTimezone=java.time.ZoneId.of(string(reader,key));}catch(java.time.DateTimeException invalid){throw new IOException("Invalid statistics timezone",invalid);}}
                    case "notifications"->notifications=NotificationsCodec.read(reader);
                    case "custom_placeholders"->customPlaceholders=CustomPlaceholdersCodec.read(reader);
                    case "achievements"->achievements=AchievementsCodec.read(reader);
                    case "statistics_privacy"->statisticsPrivacy=StatisticsPrivacyCodec.read(reader);
                    case "competitions"->competitions=CompetitionsCodec.read(reader);
                    case "rankings"->rankings=RankingsCodec.read(reader);
                    case "capture_history"->captureHistory=CaptureRetentionCodec.read(reader);
                    case "command_policy"->commandPolicy=CommandRewardsCodec.policy(reader);
                    case "home"->home=HomeLayoutCodec.read(reader);
                    case "menus"->menus=MenuNavigationCodec.read(reader);
                    case "collections" -> collections = CollectionsCodec.read(reader);
                    case "categories" -> categories = CategoriesCodec.read(reader);
                    case "rewards" -> rewards = RewardsCodec.read(reader,resolver);
                    default -> throw new IOException("config.json: unknown field " + key);
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("config.json: trailing content");
            if (version == null || language == null || title == null) throw new IOException("config.json: schema_version, language and menu_title are required");
            if (!Set.of("pt_br", "en_us").contains(language)) throw new IOException("config.json: unsupported language");
            if (title.isBlank() || title.length() > 128 || title.codePoints().anyMatch(Character::isISOControl)) throw new IOException("config.json: invalid menu_title");
            return new Settings(version, language, title, collections, categories, rewards,home,menus,commandPolicy,statisticsTimezone,captureHistory,rankings==null?br.com.shirokaidex.rankings.CaptureRanking.defaultsFor(collections):rankings,competitions,achievements,statisticsPrivacy,customPlaceholders,notifications);
        } catch (RuntimeException failure) {
            throw new IOException("config.json: invalid field or JSON structure", failure);
        }
    }

    private static String string(JsonReader reader, String field) throws IOException {
        if (reader.peek() != JsonToken.STRING) throw new IOException("config.json: " + field + " must be a string");
        return reader.nextString();
    }
    private static void writeAtomic(Path target, String value) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".config-", ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
