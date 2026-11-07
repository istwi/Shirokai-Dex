package br.com.shirokaidex.rewards;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/** SQL operations run only on Database's worker. No external delivery is performed here. */
public final class RewardJournal {
    public enum State { PENDING, PROCESSING, NEEDS_REVIEW, COMPLETED, CANCELLED }
    public record Operation(UUID id, UUID player, String reward, String plan, State state) { }
    private RewardJournal() { }
    public static Operation reserve(Connection c, UUID player, RewardPlan plan, String actor) throws SQLException {
        text(actor);
        UUID id = UUID.randomUUID(); String now = Instant.now().toString();
        try (PreparedStatement s = c.prepareStatement("INSERT INTO reward_operations VALUES(?,?,?,?, 'PENDING',?,?) ON CONFLICT(player_uuid,reward_id) DO NOTHING")) {
            s.setString(1,id.toString());s.setString(2,player.toString());s.setString(3,plan.id());s.setString(4,plan.json());s.setString(5,now);s.setString(6,now);
            if(s.executeUpdate()==1) audit(c,id,"RESERVED",actor,"Immutable plan recorded before external effects");
        }
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM reward_operations WHERE player_uuid=? AND reward_id=?")) {
            s.setString(1,player.toString());s.setString(2,plan.id());try(ResultSet rs=s.executeQuery()) { if(!rs.next())throw new SQLException("Reward reservation missing");return row(rs); }
        }
    }
    public static List<Operation> incomplete(Connection c,int page)throws SQLException {
        if(page<0||page>100000)throw new IllegalArgumentException("invalid review page");
        List<Operation> rows=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM reward_operations WHERE state IN ('PENDING','PROCESSING','NEEDS_REVIEW') ORDER BY created_at,operation_id LIMIT 10 OFFSET ?")){
            s.setInt(1,page*10);try(ResultSet rs=s.executeQuery()){while(rs.next())rows.add(row(rs));}
        }
        return List.copyOf(rows);
    }
    public static Optional<Operation> findForReward(Connection c,UUID player,String reward)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM reward_operations WHERE player_uuid=? AND reward_id=?")){
            s.setString(1,player.toString());s.setString(2,reward);try(ResultSet rs=s.executeQuery()){return rs.next()?Optional.of(row(rs)):Optional.empty();}
        }
    }
    public static Optional<Operation> find(Connection c, UUID id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM reward_operations WHERE operation_id=?")) {
            s.setString(1,id.toString());try(ResultSet rs=s.executeQuery()){return rs.next()?Optional.of(row(rs)):Optional.empty();}
        }
    }
    public static boolean begin(Connection c, UUID id) throws SQLException { return transition(c,id,State.PENDING,State.PROCESSING,"system","Preconditions checked; processing requested"); }
    public static boolean intent(Connection c, UUID id, int index) throws SQLException {
        Operation operation=find(c,id).orElseThrow(()->new SQLException("Unknown operation"));
        if(operation.state()!=State.PROCESSING || index<0) return false;
        int size=steps(operation.plan());
        if(index>=size) throw new SQLException("Step outside preserved plan");
        try(PreparedStatement s=c.prepareStatement("SELECT count(*) FROM reward_steps WHERE operation_id=? AND step_index<? AND state='COMPLETED'")) {
            s.setString(1,id.toString());s.setInt(2,index);try(ResultSet rs=s.executeQuery()){if(rs.getInt(1)!=index)return false;}
        }
        try(PreparedStatement s=c.prepareStatement("INSERT INTO reward_steps VALUES(?,?,'INTENT','') ON CONFLICT DO NOTHING")) {
            s.setString(1,id.toString());s.setInt(2,index);if(s.executeUpdate()!=1)return false;
        }
        audit(c,id,"STEP_INTENT","system","step="+index);return true;
    }
    public static boolean delivered(Connection c, UUID id, int index, String evidence) throws SQLException {
        text(evidence);if(find(c,id).orElseThrow(()->new SQLException("Unknown operation")).state()!=State.PROCESSING)return false;
        try(PreparedStatement s=c.prepareStatement("UPDATE reward_steps SET state='COMPLETED',evidence=? WHERE operation_id=? AND step_index=? AND state='INTENT'")) {
            s.setString(1,evidence);s.setString(2,id.toString());s.setInt(3,index);if(s.executeUpdate()!=1)return false;
        }
        audit(c,id,"STEP_COMPLETED","system","step="+index+"; "+evidence);return true;
    }
    public static boolean complete(Connection c, UUID id) throws SQLException {
        Operation operation=find(c,id).orElseThrow(()->new SQLException("Unknown operation"));
        int expected=steps(operation.plan());
        try(PreparedStatement s=c.prepareStatement("SELECT count(*) FROM reward_steps WHERE operation_id=? AND state='COMPLETED'")) {
            s.setString(1,id.toString());try(ResultSet rs=s.executeQuery()){if(rs.getInt(1)!=expected)return false;}
        }
        return transition(c,id,State.PROCESSING,State.COMPLETED,"system","All preserved plan steps acknowledged");
    }
    public static boolean review(Connection c, UUID id, String reason) throws SQLException { text(reason);return transition(c,id,State.PROCESSING,State.NEEDS_REVIEW,"system",reason); }
    public static boolean cancel(Connection c, UUID id, String actor, String reason) throws SQLException {
        text(actor);text(reason);Operation op=find(c,id).orElseThrow(()->new SQLException("Unknown operation"));
        if(op.state()!=State.PENDING && op.state()!=State.NEEDS_REVIEW)return false;
        return transition(c,id,op.state(),State.CANCELLED,actor,reason);
    }
    /** Explicit external evidence only; never executes or retries any delivery. */
    public static boolean resolveCompleted(Connection c, UUID id, String actor, String evidence) throws SQLException {
        text(actor);text(evidence);return transition(c,id,State.NEEDS_REVIEW,State.COMPLETED,actor,evidence);
    }
    public static int recover(Connection c) throws SQLException {
        List<UUID> interrupted=new ArrayList<>();
        try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT operation_id FROM reward_operations WHERE state='PROCESSING'")){while(rs.next())interrupted.add(UUID.fromString(rs.getString(1)));}
        for(UUID id:interrupted)review(c,id,"Server restarted; external result may be uncertain; no automatic retry");
        return interrupted.size();
    }
    private static boolean transition(Connection c,UUID id,State from,State to,String actor,String reason)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE reward_operations SET state=?,updated_at=? WHERE operation_id=? AND state=?")) {
            s.setString(1,to.name());s.setString(2,Instant.now().toString());s.setString(3,id.toString());s.setString(4,from.name());if(s.executeUpdate()!=1)return false;
        }
        audit(c,id,from+"->"+to,actor,reason);return true;
    }
    private static int steps(String plan){var json=com.google.gson.JsonParser.parseString(plan).getAsJsonObject();return json.getAsJsonArray("items").size()+(json.has("pokemon")?json.getAsJsonArray("pokemon").size():0)+(json.has("currencies")?json.getAsJsonArray("currencies").size():0)+(json.has("commands")?json.getAsJsonArray("commands").size():0);}
    private static void audit(Connection c,UUID id,String action,String actor,String reason)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO reward_audit(operation_id,action,actor,reason,recorded_at) VALUES(?,?,?,?,?)")) {
            s.setString(1,id.toString());s.setString(2,action);s.setString(3,actor);s.setString(4,reason);s.setString(5,Instant.now().toString());s.executeUpdate();
        }
    }
    private static Operation row(ResultSet rs)throws SQLException {return new Operation(UUID.fromString(rs.getString("operation_id")),UUID.fromString(rs.getString("player_uuid")),rs.getString("reward_id"),rs.getString("plan_json"),State.valueOf(rs.getString("state")));}
    private static void text(String value){if(value==null||value.isBlank()||value.length()>2048||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Evidence/actor/reason must be nonempty text up to 2048 characters");}
}
