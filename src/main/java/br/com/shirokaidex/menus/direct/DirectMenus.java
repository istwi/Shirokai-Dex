package br.com.shirokaidex.menus.direct;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.menus.MainMenu;
import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.rewards.*;
import br.com.shirokaidex.spawning.SpawnCatalog;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import java.util.function.*;

/** Only the requested standalone pages. Close controls optionally run a configured command as the player. */
public final class DirectMenus {
    public record RankingLine(int position,String name,String score) { }
    private DirectMenus() { }
    private static ItemStack icon(MenuSpec.Icon icon,Map<String,String> values,String fallback,List<Component> expanded){
        var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon.item().isEmpty()?fallback:icon.item())));decorate(item,icon,values,expanded);return item;
    }
    private static void decorate(ItemStack item,MenuSpec.Icon icon,Map<String,String> vars,List<Component> expanded){
        item.set(DataComponents.CUSTOM_NAME,RichText.render(icon.name(),vars));var lore=new ArrayList<Component>();for(String line:icon.lore()){if(line.equals("%ranking%")||line.equals("%spawns%"))lore.addAll(expanded);else lore.add(RichText.render(line,vars));}item.set(DataComponents.LORE,new ItemLore(lore));item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,icon.glint());
    }
    private static SimpleContainer background(MenuSpec spec,Map<String,String> vars){var c=new SimpleContainer(spec.rows()*9);for(int i=0;i<c.getContainerSize();i++)if(!spec.contentSlots().contains(i))c.setItem(i,icon(spec.background(),vars,"minecraft:gray_stained_glass_pane",List.of()));return c;}
    private static void button(SimpleContainer c,Map<Integer,Runnable> actions,MenuSpec spec,String role,Map<String,String> vars,Runnable action){var b=spec.buttons().get(role);if(b==null)return;c.setItem(b.slot(),icon(b.icon(),vars,"minecraft:paper",List.of()));if(action!=null)actions.put(b.slot(),action);}
    private static void show(ServerPlayer player,MenuSpec spec,Map<String,String> vars,SimpleContainer c,Map<Integer,Runnable> actions){MainMenu.show(player,RichText.render(spec.title(),vars),c,actions);}
    public static Map<String,String> progress(ServerPlayer player,CollectionDefinition collection,DexProgress p){var vars=new HashMap<String,String>();String registered=p.registered().isPresent()?Integer.toString(p.registered().getAsInt()):"N/D",total=Integer.toString(p.total()),percent=p.formattedPercentage("pt_br").orElse("N/D");vars.put("player",player.getGameProfile().getName());vars.put("dex_type",collection.name().replace(" Dex",""));vars.put("registered",registered);vars.put("player_dex_count",registered);vars.put("total",total);vars.put("pokedex_total",total);vars.put("percent",percent);vars.put("progress",registered.equals("N/D")?"N/D":registered+"/"+total);vars.put("player_dex_percent",percent.endsWith("%")?percent.substring(0,percent.length()-1):percent);return vars;}
    public static void dex(ServerPlayer player,DirectMenuCatalog catalog,CollectionDefinition collection,DexProgress progress,List<RewardPlan> plans,Set<String> unlocked,Map<String,RewardJournal.State> states,int requested,Consumer<RewardPlan> claim,IntConsumer change){
        String key="dex_"+DirectMenuCatalog.command(collection.id());var spec=catalog.menus().get(key);var all=catalog.displays().get(key);int size=spec.rewardSlots().size();if(size==0)throw new IllegalArgumentException("Dex has no reward slots");var page=Wiki.paginate(all,requested,size);var vars=progress(player,collection,progress);String last="Nenhuma";for(var d:all)if(states.get(d.id())==RewardJournal.State.COMPLETED)last=RichText.render(d.name(),vars).getString();vars.put("player_reward_group",last);vars.put("page",Integer.toString(page.number()+1));vars.put("pages",Integer.toString(page.pages()));var c=background(spec,vars);var actions=new HashMap<Integer,Runnable>();button(c,actions,spec,"info",vars,null);button(c,actions,spec,"close",vars,()->{
            player.closeContainer();
            String command=spec.buttons().get("close").command();
            if(!command.isEmpty())player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(),command);
        });
        if(page.number()>0)button(c,actions,spec,"previous",vars,()->change.accept(page.number()-1));if(page.number()+1<page.pages())button(c,actions,spec,"next",vars,()->change.accept(page.number()+1));
        for(int i=0;i<page.items().size();i++){var d=page.items().get(i);var plan=plans.stream().filter(p->p.id().equals(d.id())).findFirst().orElseThrow();var status=br.com.shirokaidex.menus.layout.RewardView.state(unlocked.contains(d.id()),states.get(d.id()));String state=status.status()==br.com.shirokaidex.menus.layout.RewardView.Status.COMPLETED?"claimed":status.canClaim()?"available":"locked";var stateIcon=spec.rewardStates().get(state);var local=new HashMap<>(vars);local.put("group_name",RichText.render(d.name(),vars).getString());local.put("group_percent",plan.minimumPercent().stripTrailingZeros().toPlainString());var lore=new ArrayList<String>(d.lore());lore.addAll(stateIcon.lore());var appearance=new MenuSpec.Icon(stateIcon.item(),stateIcon.name().replace("%group_name%",d.name()),lore,stateIcon.glint());int slot=spec.rewardSlots().get(i);c.setItem(slot,icon(appearance,local,d.icon(),List.of()));if(status.canClaim())actions.put(slot,()->claim.accept(plan));}
        show(player,spec,vars,c,actions);
    }
    public static void future(ServerPlayer player,MenuSpec spec){var vars=Map.of("player",player.getGameProfile().getName());var c=background(spec,vars);var actions=new HashMap<Integer,Runnable>();button(c,actions,spec,"future",vars,null);show(player,spec,vars,c,actions);}
    public static void ranking(ServerPlayer player,MenuSpec spec,Map<String,List<RankingLine>> rankings,String empty){var vars=Map.of("player",player.getGameProfile().getName());var c=background(spec,vars);var actions=new HashMap<Integer,Runnable>();for(var entry:spec.buttons().entrySet()){var b=entry.getValue();var lines=new ArrayList<Component>();for(var row:rankings.getOrDefault(entry.getKey(),List.of())){var fields=new HashMap<>(vars);fields.put("position",Integer.toString(row.position()));fields.put("player_name",row.name());fields.put("score",row.score());lines.add(RichText.render(spec.rankingLine(),fields));}if(lines.isEmpty()&&!entry.getKey().equals("forms"))lines.add(RichText.render(empty,vars));c.setItem(b.slot(),icon(b.icon(),vars,"minecraft:gold_ingot",lines));}show(player,spec,vars,c,actions);}
    public static void wiki(ServerPlayer player,MenuSpec spec,CollectionDefinition collection,Wiki.Request request,Wiki.Page page,SpawnCatalog spawns,Set<String> captured,Consumer<Wiki.Request> navigate,Runnable search){
        var vars=new HashMap<String,String>();vars.put("player",player.getGameProfile().getName());vars.put("query",request.query().isEmpty()?"Nenhuma":request.query());vars.put("filter",spec.filterNames().get(request.filter().name()));var c=background(spec,vars);var actions=new HashMap<Integer,Runnable>();
        for(int i=0;i<page.rows().size();i++){var row=page.rows().get(i);var species=com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(ResourceLocation.parse(row.id()));var stack=species==null?new ItemStack(Items.PAPER):com.cobblemon.mod.common.item.PokemonItem.from(species,collection.mode()==CollectionDefinition.Mode.SHINY_CAPTURED?new String[]{"shiny"}:new String[0]);var local=new HashMap<>(vars);local.put("pokemon_name",row.name());local.put("pokemon_id",row.id());local.put("seen",row.seen()?"Sim":"Não");local.put("owned",row.owned()?"Sim":"Não");local.put("captured",captured==null?"N/D":captured.contains(row.id())?"Sim":"Não");var spawnLines=new ArrayList<Component>();if(spawns.available()){for(var value:spawns.cards(row.id()).stream().filter(card->card.kind()==SpawnCatalog.Kind.ALLOW).flatMap(card->card.values().stream()).distinct().limit(8).toList())spawnLines.add(Component.literal(switch(value.field()){case BIOME->"Bioma: ";case DIMENSION->"Dimensão: ";case TICKS->"Horário (ticks): ";}+value.value()).withStyle(style->style.withColor(0xAAAAAA).withItalic(false)));}if(spawnLines.isEmpty())spawnLines.add(Component.literal("Dados de spawn indisponíveis ou sem condições simples."));else spawnLines.add(Component.literal("Resumo parcial; não garante aparição."));if(!spec.entry().item().isEmpty())stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.entry().item())));decorate(stack,spec.entry(),local,spawnLines);c.setItem(spec.contentSlots().get(i),stack);}
        button(c,actions,spec,"filter",vars,new MainMenu.ClickAction(()->navigate.accept(request.filtered(request.filter().next())),()->navigate.accept(request.filtered(request.filter().previous()))));button(c,actions,spec,"search",vars,search);button(c,actions,spec,"clear",vars,()->navigate.accept(Wiki.Request.initial("")));if(page.number()>0)button(c,actions,spec,"previous",vars,()->navigate.accept(request.page(page.number()-1)));if(page.number()+1<page.pages())button(c,actions,spec,"next",vars,()->navigate.accept(request.page(page.number()+1)));show(player,spec,vars,c,actions);
    }
}
