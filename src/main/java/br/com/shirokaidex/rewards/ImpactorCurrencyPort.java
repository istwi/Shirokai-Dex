package br.com.shirokaidex.rewards;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType;
import net.kyori.adventure.key.Key;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Loaded only with the optional audited Impactor 5.3.5 provider. No command or primary-currency fallback. */
public final class ImpactorCurrencyPort implements CurrencyRewardAdapter.Port {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final Predicate<RewardPlan> valid;
    private record Resolved(EconomyService service,Currency currency,Path root) { }
    public ImpactorCurrencyPort(MinecraftServer server,ServerPlayer player,Predicate<RewardPlan> valid){this.server=server;this.player=player;this.valid=valid;}
    public static boolean supported(){
        return FabricLoader.getInstance().getModContainer("impactor").map(mod->{String version=mod.getMetadata().getVersion().getFriendlyString();return version.equals("5.3.5")||version.startsWith("5.3.5+");}).orElse(false);
    }
    private Resolved resolve(CurrencyReward entry){
        if(!supported())throw new IllegalStateException("Unsupported Impactor version");
        var service=EconomyService.instance();var currency=service.currencies().currency(Key.key(entry.currency())).orElseThrow(()->new IllegalStateException("Requested currency unavailable"));
        if(!currency.key().asString().equals(entry.currency())||currency.decimals()<0||Math.max(0,entry.amount().scale())>currency.decimals())throw new IllegalStateException("Currency identity or decimal precision mismatch");
        return new Resolved(service,currency,nativeJsonRoot(service));
    }
    @Override public CompletableFuture<Boolean> available(List<CurrencyReward> entries){
        return onServer(()->{if(player.hasDisconnected()||server.getPlayerList().getPlayer(player.getUUID())!=player)return false;try{for(var entry:entries)resolve(entry);return true;}catch(RuntimeException|LinkageError unavailable){return false;}});
    }
    @Override public CompletableFuture<String> deposit(RewardPlan plan,CurrencyReward entry){
        return onServer(()->{
            check(plan);var context=resolve(entry);
            // Account creation/loading follows the already-persisted intent. A timeout cannot run a late deposit.
            return context.service().account(context.currency(),player.getUUID()).thenApply(account->account).orTimeout(30,TimeUnit.SECONDS)
                    .thenCompose(account->onServer(()->{
                        check(plan);var now=resolve(entry);
                        if(now.service()!=context.service()||account.virtual()||!account.owner().equals(player.getUUID())||!account.currency().key().equals(context.currency().key()))throw new IllegalStateException("Account/provider identity changed");
                        if(!CurrencyPersistence.roundTrips(account.balance().add(entry.amount())))throw new IllegalStateException("Native JSON double representation would lose monetary precision");
                        var transaction=account.deposit(entry.amount());
                        if(!transaction.successful()||transaction.type()!=EconomyTransactionType.DEPOSIT||transaction.amount().compareTo(entry.amount())!=0||!transaction.account().owner().equals(player.getUUID())||!transaction.currency().key().equals(context.currency().key()))throw new IllegalStateException("Deposit rejected or result differs; review required");
                        // Account.deposit starts a save but does not await it; explicitly await provider persistence.
                        var expected=account.balance();var owner=player.getUUID().toString();
                        var file=context.root().resolve("accounts").resolve("users").resolve(owner.substring(0,2)).resolve(owner+".conf");
                        return context.service().save(account).thenCompose(unused->CurrencyPersistence.verify(file,entry.currency(),expected))
                                .thenApply(unused->"Impactor 5.3.5 native JSON; currency="+entry.currency()+"; amount="+entry.amount().toPlainString()+"; SUCCESS; save_and_readback_verified=true").orTimeout(30,TimeUnit.SECONDS);
                    }).thenCompose(future->future));
        }).thenCompose(future->future);
    }
    private static Path nativeJsonRoot(EconomyService service){
        // Exact 5.3.5 fields audited in official sources and distribution; read-only capability check.
        // Reflection is isolated here because the public API cannot identify persistence providers.
        try{
            if(!service.getClass().getName().equals("net.impactdev.impactor.core.economy.ImpactorEconomyService"))throw new IllegalStateException("Custom economy service not supported");
            Object storage=service.getClass().getMethod("storage").invoke(service);
            var implementation=storage.getClass().getDeclaredField("implementation");implementation.setAccessible(true);Object provider=implementation.get(storage);
            if(!provider.getClass().getName().equals("net.impactdev.impactor.core.economy.storage.implementations.ConfigurateProvider"))throw new IllegalStateException("Only audited native JSON persistence supported");
            var loader=provider.getClass().getDeclaredField("loader");loader.setAccessible(true);
            if(!loader.get(provider).getClass().getName().equals("net.impactdev.impactor.api.storage.connection.configurate.loaders.JsonLoader"))throw new IllegalStateException("Only audited native JSON persistence supported");
            var root=provider.getClass().getDeclaredField("root");root.setAccessible(true);return ((Path)root.get(provider)).toAbsolutePath().normalize();
        }catch(ReflectiveOperationException|ClassCastException failure){throw new IllegalStateException("Cannot verify native persistence capability",failure);}
    }
    private void check(RewardPlan plan){if(player.hasDisconnected()||server.getPlayerList().getPlayer(player.getUUID())!=player||!valid.test(plan))throw new IllegalStateException("Player/configuration/permission changed");}
    private <T> CompletableFuture<T> onServer(Callable<T> work){
        var future=new CompletableFuture<T>();
        server.execute(()->{if(future.isDone())return;try{future.complete(work.call());}catch(Exception|LinkageError error){future.completeExceptionally(error);}});
        return future.orTimeout(30,TimeUnit.SECONDS);
    }
}
