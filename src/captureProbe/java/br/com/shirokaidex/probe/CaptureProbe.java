package br.com.shirokaidex.probe;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;
import static net.minecraft.commands.Commands.literal;

/** Test-only console fixture. Never fabricates a spawn cause or a first capture event. */
public final class CaptureProbe implements ModInitializer {
    private static String lastBattleStart="none",lastFaint="none";
    private PokemonCapturedEvent last;
    private com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity fishing;private boolean reeled;
    private EmptyPokeBallEntity thrown;private PokemonEntity aimed;private int hits;
    public void onInitialize(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            if(fishing==null||reeled||fishing.isRemoved()||fishing.getPlannedSpawnAction()==null)return;
            var player=server.getPlayerList().getPlayerByName("DexCapture");if(player==null)return;
            reeled=true;player.getMainHandItem().getItem().use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe((Consumer<com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent>) event->{if(event.getPokeBall().getOwner() instanceof ServerPlayer p&&p.getGameProfile().getName().equals("DexCapture"))hits++;});
        CobblemonEvents.BATTLE_STARTED_POST.subscribe((Consumer<com.cobblemon.mod.common.api.events.battles.BattleStartedEvent.Post>)event->{var b=event.getBattle();var rows=new ArrayList<String>();for(var actor:b.getActors())for(var p:actor.getPokemonList()){var e=p.getEntity();rows.add(actor.getClass().getSimpleName()+"/"+p.getUuid()+"/wild="+p.getOriginalPokemon().isWild()+"/entity="+(e!=null)+"/cause="+(e==null||e.getSpawnCause()==null?"none":e.getSpawnCause().getSpawner().getClass().getSimpleName()));}lastFaint="none";lastBattleStart="players="+b.getPlayers().size()+";"+rows;});
        CobblemonEvents.BATTLE_FAINTED.subscribe((Consumer<com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent>)event->{lastFaint="thread="+Thread.currentThread().getName()+"; battle="+event.getBattle().getBattleId()+"; pokemon="+event.getKilled().getUuid()+"; health="+event.getKilled().getHealth()+"; actor="+event.getKilled().getActor().getClass().getSimpleName();});
        CobblemonEvents.POKEMON_CAPTURED.subscribe((Consumer<PokemonCapturedEvent>) event->{if(event.getPlayer().getGameProfile().getName().equals("DexCapture"))last=event;});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            var root=literal("dexcaptureprobe").requires(source->source.getPlayer()==null&&source.getServer().getWorldData().getLevelName().equals("native-capture-validation"));
            for(String mode:List.of("scan","throw","unknown","replay","park","parked","status","battle","battlethrow","fishcast","fishstatus","fishthrow","fishpark","fishparked","preparefighter","battleattack","battleunknown","modelid","menu","battlestatus","tag","headskin","rewardstats","randomcheck","rewardtag"))root.then(literal(mode).executes(context->run(context.getSource(),mode)));
            root.then(literal("placeholder").then(net.minecraft.commands.Commands.argument("text",com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(context->{
                var player=context.getSource().getServer().getPlayerList().getPlayerByName("DexCapture");
                String text=com.mojang.brigadier.arguments.StringArgumentType.getString(context,"text");
                String result=DirectPlaceholderProbe.parse(player,text);
                context.getSource().sendSuccess(()->Component.literal("Placeholder: "+result),false);return 1;
            })));
            root.then(literal("rewardkeys").then(net.minecraft.commands.Commands.argument("crate",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(context->{
                try{
                    var player=context.getSource().getServer().getPlayerList().getPlayerByName("DexCapture");if(player==null)throw new IllegalStateException("Reserved player required");
                    Object service=Class.forName("dev.shirokaicrates.ShirokaiCrates").getMethod("service").invoke(null);Object snapshot=service.getClass().getMethod("snapshot").invoke(service);
                    Object crate=((java.util.Map<?,?>)snapshot.getClass().getMethod("crates").invoke(snapshot)).get(com.mojang.brigadier.arguments.StringArgumentType.getString(context,"crate"));if(crate==null)throw new IllegalStateException("Unknown crate");
                    var field=service.getClass().getDeclaredField("tokens");field.setAccessible(true);Object tokens=field.get(service);var matches=tokens.getClass().getMethod("matches",net.minecraft.world.item.ItemStack.class,crate.getClass(),boolean.class);int total=0;
                    for(int i=0;i<36;i++){var item=player.getInventory().getItem(i);if(Boolean.TRUE.equals(matches.invoke(tokens,item,crate,false)))total+=item.getCount();}
                    final int count=total;context.getSource().sendSuccess(()->Component.literal("Valid reward keys:"+count),false);return count;
                }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            })));
            dispatcher.register(root);
        });
    }
    private int run(CommandSourceStack source,String mode){
        var server=source.getServer();
        if(mode.equals("modelid")){int id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("cobblemon:pokemon_model")));int component=net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getId(com.cobblemon.mod.common.CobblemonItemComponents.POKEMON_ITEM);source.sendSuccess(()->Component.literal("Pokemon model item id: "+id+"; component id: "+component),false);return 1;}
        if(mode.equals("randomcheck")){
            try{
                var request=br.com.shirokaidex.config.ConfigManager.parse("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Test\",\"rewards\":[{\"id\":\"random_test\",\"collection\":\"national\",\"minimum_registered\":1,\"pokemon\":[{\"random_pool\":\"legendary\",\"shiny\":true,\"ivs\":{\"hp\":31,\"attack\":31,\"defence\":31,\"special_attack\":31,\"special_defence\":31,\"speed\":31}}]}]}").rewards().getFirst();
                for(int i=0;i<24;i++){
                    var frozen=br.com.shirokaidex.rewards.PokemonPayload.freeze(server,null,request.pokemon().getFirst());
                    var plan=request.withPokemon(List.of(frozen));if(!request.reuse(plan.json()).equals(plan))throw new IllegalStateException("Frozen random roll changed");
                    var pokemon=br.com.shirokaidex.rewards.PokemonPayload.restore(server,frozen);
                    if(!pokemon.getShiny()||!pokemon.getSpecies().getImplemented()||!pokemon.getSpecies().getStandardForm().getLabels().contains("legendary"))throw new IllegalStateException("Invalid random pool result");
                }
                source.sendSuccess(()->Component.literal("Random legendary frozen/restored checks:24"),false);return 24;
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        if(mode.equals("rewardstats")){
            var player=server.getPlayerList().getPlayerByName("DexCapture");if(player==null)throw new IllegalStateException("Reserved player required");
            var entries=new com.google.gson.JsonArray();
            for(var pokemon:com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player)){
                var entry=new com.google.gson.JsonObject();entry.addProperty("uuid",pokemon.getUuid().toString());entry.addProperty("species",pokemon.getSpecies().getResourceIdentifier().toString());entry.addProperty("shiny",pokemon.getShiny());entry.addProperty("legendary",pokemon.getSpecies().getStandardForm().getLabels().contains("legendary"));
                var ivs=new com.google.gson.JsonArray();for(var stat:List.of(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP,com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK,com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE,com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK,com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE,com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED))ivs.add(pokemon.getIvs().get(stat));entry.add("ivs",ivs);entries.add(entry);
            }
            source.sendSuccess(()->Component.literal("Reward Pokemon:"+entries),false);return entries.size();
        }
        if(mode.equals("headskin")){
            try {
                var path=java.nio.file.Path.of("head-skin-fixture.json");
                var fixture=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
                int updated=0;
                for(String name:List.of("DexHeadA","DexHeadB")){
                    var viewer=server.getPlayerList().getPlayerByName(name);if(viewer==null)continue;
                    var property=fixture.getAsJsonObject(name);var properties=viewer.getGameProfile().getProperties();properties.removeAll("textures");
                    properties.put("textures",new com.mojang.authlib.properties.Property("textures",property.get("value").getAsString(),property.get("signature").getAsString()));updated++;
                }
                final int count=updated;source.sendSuccess(()->Component.literal("Test skin profiles updated: "+count),false);return count;
            }catch(java.io.IOException e){throw new IllegalStateException("Missing disposable head skin fixture",e);}
        }
        var player=server.getPlayerList().getPlayerByName("DexCapture");if(player==null)throw new IllegalStateException("Reserved player required");
        if(mode.equals("tag")||mode.equals("rewardtag")){boolean granted=mode.equals("rewardtag")?DirectPermissionProbe.granted(player,"shirokai.dex.shiny.100"):DirectPermissionProbe.granted(player);source.sendSuccess(()->Component.literal("Dex tag: "+granted),false);return 1;}
        if(mode.equals("battlestatus")){var battle=com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player);var info=new StringBuilder("start=").append(lastBattleStart).append("; faint=").append(lastFaint);if(battle==null)info.append("; active=none");else{info.append("; turn=").append(battle.getTurn()).append("; ended=").append(battle.getEnded()).append("; dispatch=").append(battle.getDispatchResult());for(var actor:battle.getActors())info.append("; actor=").append(actor.getClass().getSimpleName()).append("/choose=").append(actor.getMustChoose()).append("/responses=").append(actor.getResponses()).append("/request=").append(actor.getRequest());var log=battle.getBattleLog();info.append("; log=").append(log.subList(Math.max(0,log.size()-12),log.size()));}String text=info.toString();source.sendSuccess(()->Component.literal(text.substring(0,Math.min(8000,text.length()))),false);return 1;}
        if(mode.equals("menu")){var entries=new com.google.gson.JsonArray();for(int slot=0;slot<player.containerMenu.slots.size()-36;slot++){var stack=player.containerMenu.getSlot(slot).getItem();if(stack.isEmpty())continue;var item=new com.google.gson.JsonObject();item.addProperty("slot",slot);item.addProperty("id",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());item.addProperty("name",stack.getHoverName().getString());var lore=new com.google.gson.JsonArray();for(var line:stack.getOrDefault(net.minecraft.core.component.DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines())lore.add(line.getString());item.add("lore",lore);entries.add(item);}source.sendSuccess(()->Component.literal("Menu snapshot: "+entries),false);return 1;}
        if(mode.equals("preparefighter")){for(var p:com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player))if(p.getSpecies().getResourceIdentifier().toString().equals("cobblemon:mewtwo")){p.getMoveSet().clear();p.getMoveSet().setMove(0,com.cobblemon.mod.common.api.moves.Moves.getByName("psychic").create());p.heal();source.sendSuccess(()->Component.literal("Fighter ready"),false);return 1;}throw new IllegalStateException("Give the reserved test player Mewtwo first");}

        if(mode.equals("battleattack")){var battle=com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player);if(battle==null){source.sendSuccess(()->Component.literal("No battle"),false);return 0;}for(var actor:battle.getActors())if(actor.isForPlayer(player)&&actor.getMustChoose()){String target=null;for(var opponent:battle.getActors())if(opponent instanceof com.cobblemon.mod.common.battles.actor.PokemonBattleActor&&!opponent.getActivePokemon().isEmpty())target=opponent.getActivePokemon().getFirst().getPNX();if(target==null)throw new IllegalStateException("No active wild target");actor.setActionResponses(List.of(new com.cobblemon.mod.common.battles.MoveActionResponse("psychic",target,null)));source.sendSuccess(()->Component.literal("Attack submitted"),false);return 1;}source.sendSuccess(()->Component.literal("Awaiting turn"),false);return 0;}
        if(mode.equals("fishstatus")){long count=0;for(var e:server.overworld().getAllEntities())if(e instanceof PokemonEntity p&&p.getPokemon().isWild()&&p.getSpawnCause() instanceof com.cobblemon.mod.common.api.spawning.fishing.FishingSpawnCause)count++;final long found=count;source.sendSuccess(()->Component.literal("Fishing candidates="+found+"; reeled="+reeled+"; bobber="+(fishing==null?"none":fishing.isRemoved()+"/planned="+(fishing.getPlannedSpawnAction()!=null))),false);return 1;}
        if(mode.equals("fishcast")){
            int x=player.blockPosition().getX(),y=Math.max(100,player.blockPosition().getY()),z=player.blockPosition().getZ();
            for(int dx=-2;dx<=12;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=-4;dy<=3;dy++)server.overworld().setBlock(new net.minecraft.core.BlockPos(x+dx,y+dy,z+dz),(dy==-4?net.minecraft.world.level.block.Blocks.STONE:dy<0?net.minecraft.world.level.block.Blocks.WATER:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),3);
            server.overworld().setBlock(new net.minecraft.core.BlockPos(x,y-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);player.teleportTo(x+0.5,y,z+0.5);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.cobblemon.mod.common.CobblemonItems.POKE_ROD));
            player.getMainHandItem().getItem().use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            fishing=null;reeled=false;for(var e:server.overworld().getAllEntities())if(e instanceof com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity b&&b.getPlayerOwner()==player)fishing=b;
            if(fishing==null)throw new IllegalStateException("Native rod did not create bobber");fishing.setPos(x+5.5,y+0.2,z+0.5);fishing.setDeltaMovement(0,0,0);
            source.sendSuccess(()->Component.literal("Native rod cast"),false);return 1;
        }
        if(mode.equals("status")){source.sendSuccess(()->Component.literal("Hits="+hits+"; ball="+(thrown==null?"none":thrown.getCaptureState()+"/removed="+thrown.isRemoved()+"/pos="+thrown.position())+"; target="+(aimed==null?"none":aimed.getBoundingBox()+"/wild="+aimed.getPokemon().isWild())+"; event="+(last!=null)),false);return 1;}
        if(mode.equals("replay")){if(last==null)throw new IllegalStateException("No native capture to replay");CobblemonEvents.POKEMON_CAPTURED.emit(last);source.sendSuccess(()->Component.literal("Duplicate native event replayed"),false);return 1;}
        List<PokemonEntity> candidates=new ArrayList<>();for(var entity:server.overworld().getAllEntities())if(entity instanceof PokemonEntity pokemon&&pokemon.isAlive()&&pokemon.getPokemon().isWild())candidates.add(pokemon);
        var selected=candidates.stream().filter(p->(mode.equals("parked")||mode.equals("fishparked"))?p.getTags().contains("dex-capture-parked"):mode.startsWith("fish")?p.getSpawnCause() instanceof com.cobblemon.mod.common.api.spawning.fishing.FishingSpawnCause&&!p.getTags().contains("dex-capture-parked"):p.getSpawnCause()!=null&&p.getSpawnCause().getSpawner().getClass()==PlayerSpawner.class&&p.getPokemon().getSpecies().getResourceIdentifier().toString().equals("cobblemon:pikachu")&&p.getPokemon().getShiny()&&!p.getTags().contains("dex-capture-parked")).findFirst();
        if(mode.equals("scan")){int count=(int)candidates.stream().filter(p->p.getSpawnCause()!=null).count();source.sendSuccess(()->Component.literal("Natural candidate: "+selected.isPresent()+"; caused wild entities: "+count),false);return selected.isPresent()?1:0;}
        PokemonEntity pokemon;
        if(mode.equals("unknown")||mode.equals("battleunknown")){var properties=new PokemonProperties();properties.setSpecies("pikachu");properties.setLevel(5);pokemon=properties.createEntity(server.overworld());server.overworld().addFreshEntity(pokemon);}
        else if(mode.equals("battlethrow")){pokemon=aimed;if(pokemon==null||com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player)==null)throw new IllegalStateException("Native battle required");}
        else{if(selected.isEmpty()){source.sendSuccess(()->Component.literal("No matching native candidate"),false);return 0;}pokemon=selected.get();}
        if((mode.equals("park")||mode.equals("fishpark"))){pokemon.addTag("dex-capture-parked");pokemon.setPersistenceRequired();pokemon.setNoAi(true);pokemon.moveTo(player.getX()+3,player.getY(),player.getZ());source.sendSuccess(()->Component.literal("Native candidate parked: "+pokemon.getPokemon().getUuid()),false);return 1;}
        if(!mode.equals("battlethrow")){
        // Clear only a tiny owned test arena so terrain cannot intercept the physical projectile.
        int bx=player.blockPosition().getX(),by=player.blockPosition().getY()+1,bz=player.blockPosition().getZ();
        for(int x=bx-1;x<=bx+5;x++)for(int z=bz-1;z<=bz+1;z++){
            server.overworld().setBlock(new net.minecraft.core.BlockPos(x,by-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            for(int y=by;y<=by+3;y++)server.overworld().setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        }
        player.teleportTo(bx+0.5,by,bz+0.5);pokemon.setNoAi(true);pokemon.moveTo(bx+3.5,by,bz+0.5);
        }
        if(mode.equals("battle")||mode.equals("battleunknown")){aimed=pokemon;var result=com.cobblemon.mod.common.battles.BattleBuilder.INSTANCE.pve(player,pokemon);source.sendSuccess(()->Component.literal("Native battle start: "+result.getClass().getSimpleName()),false);return 1;}
        var ball=new EmptyPokeBallEntity(PokeBalls.INSTANCE.getMASTER_BALL(),server.overworld(),player,CobblemonEntities.EMPTY_POKEBALL);
        ball.setNoGravity(true);ball.setPos(player.getX()+0.2,player.getY()+0.8,player.getZ());ball.setDeltaMovement(0.65,0,0);server.overworld().addFreshEntity(ball);thrown=ball;aimed=pokemon;
        var id=pokemon.getPokemon().getUuid();var cause=pokemon.getSpawnCause();source.sendSuccess(()->Component.literal("Native ball launched: "+id+"; spawn cause: "+(cause==null?"unknown":cause.getSpawner().getClass().getSimpleName())),false);return 1;
    }
}
