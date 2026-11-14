package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.rewards.*;
import br.com.shirokaidex.menus.layout.RewardView;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Eligibility in the persisted journal; delivery still revalidates backend/capacity and permissions. */
public final class RewardAvailability {
    private RewardAvailability() { }
    public static long count(Connection c,UUID player,List<RewardPlan> authorized,Instant now)throws SQLException {
        var unlocked=new HashSet<String>();try(var q=c.prepareStatement("SELECT reward_id FROM reward_unlocks WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())unlocked.add(rows.getString(1));}}
        long count=0;for(var plan:authorized)if(unlocked.contains(plan.id())&&RewardView.preview(true,plan,RewardJournal.findForReward(c,player,plan.id())).canClaim())count++;
        try(var q=c.prepareStatement("SELECT count(*) FROM competition_awards a JOIN competition_editions d ON d.edition_id=a.edition_id AND d.state='FINAL' LEFT JOIN reward_operations o ON o.player_uuid=a.player_uuid AND o.reward_id=a.award_id WHERE a.player_uuid=? AND (a.expires_at IS NULL OR a.expires_at>?) AND (o.state IS NULL OR o.state='PENDING')")){q.setString(1,player.toString());q.setLong(2,now.toEpochMilli());try(var rows=q.executeQuery()){rows.next();count+=rows.getLong(1);}}
        return count;
    }
}
