package br.com.shirokaidex.competitions;

import java.util.*;

/** Exclusive prize slots; redistribution never changes official positions or replaces assigned awards. */
public record PrizePolicy(boolean enabled,List<Band> bands,int minimumParticipants,long minimumCaptures,boolean redistribute,int expiryDays) {
    public record Band(int maximumPosition,String reward,long minimumScore) {
        public Band {if(!Set.of(1,3,10).contains(maximumPosition)||reward==null||!reward.matches("[a-z][a-z0-9_]{0,47}")||minimumScore<0||minimumScore>1_000_000_000L)throw new IllegalArgumentException("Invalid prize band");}
    }
    public record Candidate(UUID player,int position,long score,boolean eligible) { }
    public record Assignment(UUID player,int sourcePosition,Band band) { }
    public PrizePolicy {
        bands=List.copyOf(bands);var seen=new HashSet<Integer>();for(var band:bands)if(!seen.add(band.maximumPosition()))throw new IllegalArgumentException("Duplicate prize band");
        if(minimumParticipants<1||minimumParticipants>100000||minimumCaptures<0||minimumCaptures>1000000000L||expiryDays<0||expiryDays>36500||enabled&&bands.isEmpty())throw new IllegalArgumentException("Invalid prize policy");
    }
    public static PrizePolicy disabled(){return new PrizePolicy(false,List.of(),2,1,false,0);}
    public List<Assignment> assign(List<Candidate> candidates){
        if(!enabled||candidates.size()<minimumParticipants)return List.of();
        var sorted=candidates.stream().sorted(Comparator.comparingInt(Candidate::position)).toList();var awarded=new HashSet<UUID>();var assignments=new ArrayList<Assignment>();
        for(int slot=1;slot<=10;slot++){
            int group=slot==1?1:slot<=3?3:10;var band=bands.stream().filter(b->b.maximumPosition()==group).findFirst().orElse(null);if(band==null)continue;
            for(var candidate:sorted){
                if(candidate.position()<slot||(!redistribute&&candidate.position()!=slot)||!candidate.eligible()||candidate.score()<Math.max(minimumCaptures,band.minimumScore())||awarded.contains(candidate.player()))continue;
                assignments.add(new Assignment(candidate.player(),slot,band));awarded.add(candidate.player());break;
            }
        }
        return List.copyOf(assignments);
    }
}
