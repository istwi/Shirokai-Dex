package br.com.shirokaidex.competitions;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrizePolicyTest {
    private List<PrizePolicy.Candidate> candidates(int size){var values=new ArrayList<PrizePolicy.Candidate>();for(int i=1;i<=size;i++)values.add(new PrizePolicy.Candidate(new UUID(0,i),i,100-i,true));return values;}
    private List<PrizePolicy.Band> bands(){return List.of(new PrizePolicy.Band(1,"winner",1),new PrizePolicy.Band(3,"podium",1),new PrizePolicy.Band(10,"finalist",1));}
    @Test void exclusiveBandsAwardExactlyOnePackagePerPlayerWithoutSumming(){
        var policy=new PrizePolicy(true,bands(),2,1,false,0);var awards=policy.assign(candidates(12));assertEquals(10,awards.size());assertEquals(10,awards.stream().map(PrizePolicy.Assignment::player).distinct().count());assertEquals(1,awards.stream().filter(a->a.band().maximumPosition()==1).count());assertEquals(2,awards.stream().filter(a->a.band().maximumPosition()==3).count());assertEquals(7,awards.stream().filter(a->a.band().maximumPosition()==10).count());
    }
    @Test void defaultNoRedistributionLeavesIneligibleSlotUnassignedWithoutPromotingPublicPositions(){
        var values=candidates(4);values.set(0,new PrizePolicy.Candidate(values.getFirst().player(),1,99,false));var awards=new PrizePolicy(true,bands(),2,1,false,0).assign(values);assertEquals(3,awards.size());assertTrue(awards.stream().noneMatch(a->a.band().maximumPosition()==1));assertEquals(2,awards.getFirst().sourcePosition());assertEquals(2,values.get(1).position());
    }
    @Test void redistributionMovesPackagesOnceToNextEligibleWithoutChangingPositions(){
        var values=candidates(12);values.set(0,new PrizePolicy.Candidate(values.getFirst().player(),1,99,false));var awards=new PrizePolicy(true,bands(),2,1,true,7).assign(values);assertEquals(new UUID(0,2),awards.getFirst().player());assertEquals(1,awards.getFirst().sourcePosition());assertEquals(1,awards.getFirst().band().maximumPosition());assertEquals(10,awards.size());assertEquals(10,awards.stream().map(PrizePolicy.Assignment::player).distinct().count());assertEquals(2,values.get(1).position());
    }
    @Test void participantAndScoreRequirementsAffectOnlyAwardsAndDisabledPolicyDoesNothing(){
        var values=candidates(2);assertTrue(new PrizePolicy(true,bands(),3,1,false,0).assign(values).isEmpty());assertTrue(new PrizePolicy(true,bands(),2,100,false,0).assign(values).isEmpty());assertTrue(PrizePolicy.disabled().assign(values).isEmpty());assertEquals(99,values.getFirst().score());
    }
}
