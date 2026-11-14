package br.com.shirokaidex.rewards;
import java.util.*;
import java.util.concurrent.*;
public final class PermissionRewardAdapter implements RewardDelivery.Adapter {
    public interface Port {CompletableFuture<Boolean> available(List<PermissionReward> entries);CompletableFuture<String> grant(PermissionReward entry);}
    private final RewardDelivery.Adapter base;private final Port port;
    public PermissionRewardAdapter(RewardDelivery.Adapter base,Port port){this.base=Objects.requireNonNull(base);this.port=Objects.requireNonNull(port);}
    private int offset(RewardPlan p){return p.items().size()+p.pokemon().size()+p.currencies().size();}
    public CompletableFuture<Boolean> preflight(RewardPlan p,int from){int offset=offset(p);return base.preflight(p,Math.min(from,offset)).thenCompose(ok->ok?port.available(p.permissions().subList(Math.max(0,from-offset),p.permissions().size())):CompletableFuture.completedFuture(false));}
    public CompletableFuture<String> deliver(RewardPlan p,int step){int offset=offset(p);return step<offset?base.deliver(p,step):port.grant(p.permissions().get(step-offset));}
}
