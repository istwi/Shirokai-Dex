package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.menus.layout.MenuNavigation.Screen;
import br.com.shirokaidex.wiki.Wiki;
import br.com.shirokaidex.spawning.SpawnCatalog;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.pokedex.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Vanilla inventory protocol. Only explicitly registered left-click actions are executed. */
public final class MainMenu extends ChestMenu {
    private final UUID owner;
    private final Map<Integer, Runnable> actions;
    private MainMenu(int id, ServerPlayer player, SimpleContainer container, Map<Integer, Runnable> actions) {
        super(switch(container.getContainerSize()){case 27->MenuType.GENERIC_9x3;case 36->MenuType.GENERIC_9x4;case 45->MenuType.GENERIC_9x5;case 54->MenuType.GENERIC_9x6;default->throw new IllegalArgumentException("unsupported menu size");}, id, player.getInventory(), container, container.getContainerSize() / 9);
        owner = player.getUUID(); this.actions = Map.copyOf(actions);
    }
    private static final List<Integer> CONTENT_SLOTS = List.of(13, 11, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17);
    public static String displayName(CollectionDefinition definition, String language, Messages messages) {
        return definition.name().isEmpty() ? messages.text(language, "dex." + definition.id()).getString() : definition.name();
    }
    public static void open(ServerPlayer player, Settings settings, Messages messages,
                            List<CollectionDefinition> visible, Map<String, ProgressCalculator.CollectionProgress> values,
                            int requestedPage, Consumer<String> navigate, IntConsumer pageNavigation,Runnable home) {
        var contents = new SimpleContainer(27);
        Map<Integer, Runnable> actions = new HashMap<>();
        int pages = Math.max(1, (visible.size() + CONTENT_SLOTS.size() - 1) / CONTENT_SLOTS.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int first = page * CONTENT_SLOTS.size();
        for (int i = 0; i < CONTENT_SLOTS.size() && first + i < visible.size(); i++) {
            var definition = visible.get(first + i); int slot = CONTENT_SLOTS.get(i);
            contents.setItem(slot, collectionItem(definition, values.get(definition.id()), settings.language(), messages));
            actions.put(slot, () -> navigate.accept(definition.id()));
        }
        if (page > 0) navigation(contents, actions, 18, messages.text(settings.language(), "page_previous"), () -> pageNavigation.accept(page - 1));
        if (page + 1 < pages) navigation(contents, actions, 26, messages.text(settings.language(), "page_next"), () -> pageNavigation.accept(page + 1));
        navigation(contents,actions,23,messages.text(settings.language(),"home.return_home"),home);
        closeButton(contents, actions, player, settings.language(), messages);
        String title = settings.menuTitle() + (pages > 1 ? " [" + (page + 1) + "/" + pages + "]" : "");
        styledShow(player,settings,messages,Screen.SELECTOR,title,contents,actions,Set.of());
    }
    public static void openCollection(ServerPlayer player, Settings settings, Messages messages,
                                      CollectionDefinition definition, ProgressCalculator.CollectionProgress value, Runnable back, Runnable wiki) {
        var contents = new SimpleContainer(27);
        Map<Integer, Runnable> actions = new HashMap<>();
        contents.setItem(13, collectionItem(definition, value, settings.language(), messages));
        navigation(contents, actions, 15, messages.text(settings.language(), "wiki_open"), wiki);
        navigation(contents, actions, 18, messages.text(settings.language(), "menu_back"), back);
        closeButton(contents, actions, player, settings.language(), messages);
        styledShow(player,settings,messages,Screen.COLLECTION,settings.menuTitle()+" — "+displayName(definition,settings.language(),messages),contents,actions,Set.of());
    }
    public static void openWiki(ServerPlayer player, Settings settings, Messages messages, CollectionDefinition definition,
                                Wiki.Request request, Wiki.Page page, SpawnCatalog spawns, Consumer<Wiki.Request> navigate, Runnable back, Runnable searchHelp) {
        var contents = new SimpleContainer(36);
        Map<Integer, Runnable> actions = new HashMap<>();
        String language = settings.language();
        String title = messages.text(language, "wiki_title", displayName(definition, language, messages), page.number() + 1, page.pages(), page.total()).getString();
        if (request.facet() != Wiki.Facet.NONE) {
            var options = Wiki.options(request.facet() == Wiki.Facet.TYPE ? page.types() : page.categories(), request.optionPage(), 18);
            String selected = request.facet() == Wiki.Facet.TYPE ? request.type() : request.category();
            for (int i = 0; i < options.items().size(); i++) {
                String value = options.items().get(i);
                var item = new ItemStack(Items.PAPER);
                item.set(DataComponents.CUSTOM_NAME, Component.literal(facetName(value, language, messages)));
                item.set(DataComponents.LORE, new ItemLore(List.of(messages.text(language, selected.equals(value) ? "wiki_option_selected" : "wiki_option_apply"))));
                contents.setItem(i, item);
                actions.put(i, () -> navigate.accept(request.facet() == Wiki.Facet.TYPE ? request.type(value) : request.category(value)));
            }
            if (options.number() > 0) navigation(contents, actions, 18, messages.text(language, "page_previous"), () -> navigate.accept(request.pick(request.facet(), options.number() - 1)));
            if (options.number() + 1 < options.pages()) navigation(contents, actions, 26, messages.text(language, "page_next"), () -> navigate.accept(request.pick(request.facet(), options.number() + 1)));
            navigation(contents, actions, 23, messages.text(language, "wiki_back"), () -> navigate.accept(request.page(page.number())));
            title = messages.text(language, request.facet() == Wiki.Facet.TYPE ? "wiki_types_title" : "wiki_categories_title", options.number() + 1, options.pages()).getString();
        } else if (page.detail().isPresent() && request.spawnPage() >= 0) {
            var row = page.detail().get();
            var cards = Wiki.paginate(spawns.cards(row.id()), request.spawnPage(), 18);
            for (int i = 0; i < cards.items().size(); i++) contents.setItem(i, spawnItem(cards.items().get(i), language, messages));
            if (cards.items().isEmpty()) {
                var empty = new ItemStack(Items.PAPER);
                empty.set(DataComponents.CUSTOM_NAME, messages.text(language, spawns.available() ? "spawn_none" : "wiki_spawn_unknown"));
                empty.set(DataComponents.LORE, new ItemLore(List.of(messages.text(language, "spawn_scope"))));contents.setItem(13,empty);
            }
            if (cards.number() > 0) navigation(contents, actions, 18, messages.text(language,"page_previous"), () -> navigate.accept(request.spawns(cards.number()-1)));
            if (cards.number()+1 < cards.pages()) navigation(contents, actions, 26, messages.text(language,"page_next"), () -> navigate.accept(request.spawns(cards.number()+1)));
            navigation(contents, actions, 23, messages.text(language,"wiki_details_back"), () -> navigate.accept(request.detail(row.id())));
            var scope = new ItemStack(Items.PAPER);scope.set(DataComponents.CUSTOM_NAME,messages.text(language,"spawn_scope"));
            scope.set(DataComponents.LORE,new ItemLore(List.of(spawns.available()?messages.text(language,"spawn_skipped",spawns.skipped()):messages.text(language,"wiki_spawn_unknown"),messages.text(language,"spawn_form_warning"))));contents.setItem(27,scope);
            title = messages.text(language,"spawn_title",row.name(),cards.number()+1,cards.pages()).getString();
        } else if (page.detail().isPresent() && request.formPage() >= 0) {
            var row = page.detail().get();
            var forms = Wiki.forms(row, request.formPage(), 18);
            for (int i = 0; i < forms.items().size(); i++) contents.setItem(i, formItem(row.id(), forms.items().get(i), language, messages));
            if (forms.number() > 0) navigation(contents, actions, 18, messages.text(language, "page_previous"), () -> navigate.accept(request.forms(forms.number() - 1)));
            if (forms.number() + 1 < forms.pages()) navigation(contents, actions, 26, messages.text(language, "page_next"), () -> navigate.accept(request.forms(forms.number() + 1)));
            navigation(contents, actions, 23, messages.text(language, "wiki_details_back"), () -> navigate.accept(request.detail(row.id())));
            title = messages.text(language, "wiki_forms_title", row.name(), forms.number() + 1, forms.pages(), forms.total()).getString();
        } else if (page.detail().isPresent()) {
            var row = page.detail().get();
            contents.setItem(13, wikiItem(row, language, messages, true, spawns.available()));
            navigation(contents, actions, 15, messages.text(language, "wiki_forms_open", row.forms().size()), () -> navigate.accept(request.forms(0)));
            navigation(contents, actions, 16, messages.text(language, "spawn_open"), () -> navigate.accept(request.spawns(0)));
            navigation(contents, actions, 18, messages.text(language, "wiki_back"), () -> navigate.accept(request.page(page.number())));
            navigation(contents, actions, 23, messages.text(language, "wiki_collection_back"), back);
        } else {
            for (int i = 0; i < page.rows().size(); i++) {
                var row = page.rows().get(i);
                contents.setItem(i, wikiItem(row, language, messages, false, spawns.available()));
                actions.put(i, () -> navigate.accept(request.detail(row.id())));
            }
            if (page.rows().isEmpty()) {
                var empty = new ItemStack(Items.PAPER);
                empty.set(DataComponents.CUSTOM_NAME, messages.text(language, "wiki_empty")); contents.setItem(13, empty);
            }
            if (page.number() > 0) navigation(contents, actions, 18, messages.text(language, "page_previous"), () -> navigate.accept(request.page(page.number() - 1)));
            if (page.number() + 1 < page.pages()) navigation(contents, actions, 26, messages.text(language, "page_next"), () -> navigate.accept(request.page(page.number() + 1)));
            navigation(contents, actions, 19, messages.text(language, "wiki_filter", messages.text(language, "wiki.filter." + request.filter()).getString()), () -> navigate.accept(request.filtered(request.filter().next())));
            navigation(contents, actions, 20, messages.text(language, "wiki_order", messages.text(language, "wiki.order." + request.order()).getString()), () -> navigate.accept(request.ordered(request.order().next())));
            navigation(contents, actions, 21, messages.text(language, "wiki_search", request.query()), searchHelp);
            navigation(contents, actions, 24, messages.text(language, "wiki_clear"), () -> navigate.accept(Wiki.Request.initial("")));
            navigation(contents, actions, 25, messages.text(language, "wiki_type", facetName(request.type(), language, messages)), () -> navigate.accept(request.pick(Wiki.Facet.TYPE, 0)));
            navigation(contents, actions, 27, messages.text(language, "wiki_category", facetName(request.category(), language, messages)), () -> navigate.accept(request.pick(Wiki.Facet.CATEGORY, 0)));
            navigation(contents, actions, 23, messages.text(language, "wiki_collection_back"), back);
        }
        closeButton(contents, actions, player, language, messages);
        Screen screen=request.facet()==Wiki.Facet.TYPE?Screen.WIKI_TYPES:request.facet()==Wiki.Facet.CATEGORY?Screen.WIKI_CATEGORIES:
                page.detail().isEmpty()?Screen.WIKI:request.spawnPage()>=0?Screen.WIKI_SPAWNS:request.formPage()>=0?Screen.WIKI_FORMS:Screen.WIKI_DETAILS;
        styledShow(player,settings,messages,screen,title,contents,actions,Set.of());
    }
    private static String facetName(String value, String language, Messages messages) {
        return value.isEmpty() ? messages.text(language, "wiki_facet_all").getString() : value;
    }
    public static void openReward(ServerPlayer player, Settings settings, Messages messages,
                                  br.com.shirokaidex.rewards.RewardPlan plan, br.com.shirokaidex.menus.layout.RewardView view, int requestedPage,
                                  IntConsumer navigate, Runnable back, Runnable claim) {
        var contents = new SimpleContainer(27); Map<Integer,Runnable> actions = new HashMap<>();
        var entries=new java.util.ArrayList<Object>();entries.addAll(plan.items());entries.addAll(plan.pokemon());entries.addAll(plan.currencies());entries.addAll(plan.commands());
        var page = Wiki.paginate(entries, requestedPage, 18);
        for (int i = 0; i < page.items().size(); i++) {
            var action=page.items().get(i);
            if(action instanceof br.com.shirokaidex.rewards.CommandReward command){contents.setItem(i,commandItem(command,settings.language(),messages));continue;}
            if(action instanceof br.com.shirokaidex.rewards.CurrencyReward money){contents.setItem(i,currencyItem(money,settings.language(),messages));continue;}
            if(action instanceof br.com.shirokaidex.rewards.PokemonReward pokemon){contents.setItem(i,pokemonItem(pokemon,settings.language(),messages));continue;}
            var entry=(br.com.shirokaidex.rewards.RewardPlan.Item)action;
            var item = br.com.shirokaidex.rewards.ItemPayload.restore(player.getServer(),entry);
            item.setCount(Math.min(entry.count(), item.getMaxStackSize()));
            var lines = new java.util.ArrayList<>(item.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());
            lines.add(messages.text(settings.language(), "reward_item_count", entry.count()));
            item.set(DataComponents.LORE, new ItemLore(lines));
            contents.setItem(i, item);
        }
        var status=new ItemStack(switch(view.status()){case AVAILABLE,RESERVED->Items.LIME_DYE;case COMPLETED->Items.EMERALD;case PROCESSING->Items.CLOCK;case NEEDS_REVIEW,PLAN_CHANGED,UNAVAILABLE->Items.BARRIER;case LOCKED,CANCELLED->Items.GRAY_DYE;});
        String threshold=plan.minimumPercent()==null?String.valueOf(plan.minimumRegistered()):plan.minimumPercent().toPlainString()+"%";
        List<Component> statusLore=new ArrayList<>();statusLore.add(messages.text(settings.language(),"layout.reward_requirement",threshold));statusLore.add(messages.text(settings.language(),"layout.reward_hint."+view.status().name()));
        if(!view.operation().isEmpty()){
            statusLore.add(messages.text(settings.language(),"layout.operation_short",view.operation().substring(0,8)));
            statusLore.add(messages.text(settings.language(),"layout.operation_hint"));actions.put(19,()->player.sendSystemMessage(messages.text(settings.language(),"saved_item_operation",view.operation())));
        }
        MenuStyle.card(status,messages.text(settings.language(),"layout.reward_status."+view.status().name()),statusLore,rewardColor(view));contents.setItem(19,status);
        if(view.canClaim())navigation(contents,actions,24,messages.text(settings.language(),"reward_claim"),claim);
        navigation(contents,actions,18,messages.text(settings.language(),"menu_back"),back);
        if(page.number()>0) navigation(contents,actions,20,messages.text(settings.language(),"page_previous"),()->navigate.accept(page.number()-1));
        if(page.number()+1<page.pages()) navigation(contents,actions,26,messages.text(settings.language(),"page_next"),()->navigate.accept(page.number()+1));
        closeButton(contents,actions,player,settings.language(),messages);
        styledShow(player,settings,messages,Screen.REWARD,messages.text(settings.language(),"reward_preview",plan.id(),page.number()+1,page.pages()).getString(),contents,actions,new HashSet<>(java.util.stream.IntStream.range(0,18).boxed().toList()));
    }

    public static void confirmReward(ServerPlayer player, Settings settings, Messages messages,
                                     br.com.shirokaidex.rewards.RewardPlan plan, Runnable confirm, Runnable back) {
        confirmReward(player,settings,messages,plan,plan.id(),confirm,back);
    }
    public static void confirmReward(ServerPlayer player,Settings settings,Messages messages,
                                     br.com.shirokaidex.rewards.RewardPlan plan,String label,Runnable confirm,Runnable back){
        var contents = new SimpleContainer(27); Map<Integer,Runnable> actions = new HashMap<>();
        var preview=!plan.commands().isEmpty()?commandItem(plan.commands().getFirst(),settings.language(),messages):plan.items().isEmpty()?(plan.pokemon().isEmpty()?currencyItem(plan.currencies().getFirst(),settings.language(),messages):pokemonItem(plan.pokemon().getFirst(),settings.language(),messages)):br.com.shirokaidex.rewards.ItemPayload.restore(player.getServer(),plan.items().getFirst());
        if(!plan.items().isEmpty())preview.setCount(Math.min(plan.items().getFirst().count(),preview.getMaxStackSize()));contents.setItem(11,preview);
        var confirmation=new ItemStack(Items.LIME_DYE);confirmation.set(DataComponents.CUSTOM_NAME,messages.text(settings.language(),"reward_confirm"));confirmation.set(DataComponents.LORE,new ItemLore(List.of(messages.text(settings.language(),"layout.confirm_hint"))));contents.setItem(13,confirmation);actions.put(13,confirm);
        var summary=new ItemStack(Items.PAPER);summary.set(DataComponents.CUSTOM_NAME,messages.text(settings.language(),"layout.confirm_summary"));summary.set(DataComponents.LORE,new ItemLore(List.of(messages.text(settings.language(),"layout.confirm_steps",plan.steps()),messages.text(settings.language(),"layout.confirm_contents",plan.items().size(),plan.pokemon().size()))));contents.setItem(15,summary);
        if(!plan.currencies().isEmpty()){var lore=new ArrayList<>(summary.get(DataComponents.LORE).lines());for(var money:plan.currencies())lore.add(messages.text(settings.language(),"currency_amount",money.currency(),money.amount().toPlainString()));summary.set(DataComponents.LORE,new ItemLore(lore));}
        if(!plan.commands().isEmpty()){
            var lore=new ArrayList<>(summary.get(DataComponents.LORE).lines());lore.add(messages.text(settings.language(),"command_review_warning"));summary.set(DataComponents.LORE,new ItemLore(lore));
            confirmation.set(DataComponents.LORE,new ItemLore(List.of(messages.text(settings.language(),"command_review_warning"))));
        }
        navigation(contents,actions,18,messages.text(settings.language(),"menu_back"),back);
        closeButton(contents,actions,player,settings.language(),messages);
        styledShow(player,settings,messages,Screen.CONFIRMATION,messages.text(settings.language(),"reward_confirmation",label).getString(),contents,actions,Set.of(11));
    }

    private static int rewardColor(br.com.shirokaidex.menus.layout.RewardView view){return switch(view.status()){case AVAILABLE,COMPLETED->MenuStyle.SUCCESS;case RESERVED,PROCESSING->MenuStyle.NAVIGATION;case NEEDS_REVIEW,PLAN_CHANGED,UNAVAILABLE->MenuStyle.DANGER;case LOCKED,CANCELLED->0x999999;};}
    private static void styledShow(ServerPlayer player,Settings settings,Messages messages,Screen screen,String title,SimpleContainer contents,Map<Integer,Runnable> actions,Set<Integer> preserved){
        var layout=settings.menus().layout(screen);var palette=layout.palette().orElse(settings.home().palette());
        for(int slot=0;slot<contents.getContainerSize();slot++){
            if(preserved.contains(slot))continue;var item=contents.getItem(slot);if(item.isEmpty())continue;
            if(actions.containsKey(slot)&&item.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines().isEmpty())item.set(DataComponents.LORE,new ItemLore(List.of(messages.text(settings.language(),slot==22?"home.click_close":"layout.navigation_hint"))));
            MenuStyle.polish(item,slot==22?MenuStyle.DANGER:item.is(Items.LIME_DYE)?MenuStyle.SUCCESS:item.is(Items.ARROW)?MenuStyle.NAVIGATION:palette.start);
        }
        contents=MenuStyle.appearance(contents,layout,player.getGameProfile().getName());MenuStyle.navigation(contents,actions,layout);
        if(layout.footer())MenuStyle.footer(contents,palette);
        show(player,MenuStyle.title(MenuStyle.titleLabel(layout,title,player.getGameProfile().getName()),palette),contents,actions);
    }

    private static ItemStack commandItem(br.com.shirokaidex.rewards.CommandReward command,String language,Messages messages){
        var item=new ItemStack(Items.PAPER);item.set(DataComponents.CUSTOM_NAME,messages.text(language,"command_title"));
        item.set(DataComponents.LORE,new ItemLore(List.of(messages.text(language,"command_context",command.context()),Component.literal(command.command()),messages.text(language,"command_review_warning"))));return item;
    }
    private static ItemStack currencyItem(br.com.shirokaidex.rewards.CurrencyReward money,String language,Messages messages){
        var item=new ItemStack(money.currency().equals("impactor:dollars")?Items.GOLD_INGOT:money.currency().equals("impactor:tokens")?Items.NETHER_STAR:Items.GOLD_NUGGET);
        item.set(DataComponents.CUSTOM_NAME,messages.text(language,money.currency().equals("impactor:dollars")?"currency_dollars":money.currency().equals("impactor:tokens")?"currency_tokens":"currency_generic",money.amount().toPlainString(),money.currency()));
        item.set(DataComponents.LORE,new ItemLore(List.of(messages.text(language,"currency_amount",money.currency(),money.amount().toPlainString()))));return item;
    }
    private static ItemStack pokemonItem(br.com.shirokaidex.rewards.PokemonReward pokemon,String language,Messages messages){
        var item=new ItemStack(Items.DRAGON_EGG);item.set(DataComponents.CUSTOM_NAME,messages.text(language,"pokemon_reward",pokemon.species()));
        var lore=new java.util.ArrayList<Component>();String defaults=messages.text(language,"pokemon_default").getString();
        lore.add(messages.text(language,"pokemon_level",pokemon.level()==null?defaults:pokemon.level()));lore.add(messages.text(language,"pokemon_shiny",pokemon.shiny()));
        for(var entry:java.util.Map.of("form",pokemon.form(),"nature",pokemon.nature(),"ability",pokemon.ability(),"gender",pokemon.gender(),"ball",pokemon.pokeball(),"held",pokemon.heldItem()).entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList())lore.add(messages.text(language,"pokemon_"+entry.getKey(),entry.getValue().isEmpty()?defaults:entry.getValue()));
        lore.add(messages.text(language,"pokemon_moves",pokemon.moves().isEmpty()?defaults:String.join(", ",pokemon.moves())));lore.add(messages.text(language,"pokemon_ivs",pokemon.ivs().isEmpty()?defaults:pokemon.ivs()));lore.add(messages.text(language,"pokemon_evs",pokemon.evs().isEmpty()?defaults:pokemon.evs()));
        item.set(DataComponents.LORE,new ItemLore(lore));return item;
    }
    private static ItemStack wikiItem(Wiki.Row row, String language, Messages messages, boolean detail, boolean spawnAvailable) {
        var item = new ItemStack(Items.BOOK);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(row.name()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(row.id()));
        lore.add(messages.text(language, "wiki_seen", yesNo(row.seen(), language, messages)));
        lore.add(messages.text(language, "wiki_owned", yesNo(row.owned(), language, messages)));
        lore.add(messages.text(language, "wiki_shiny_seen", yesNo(row.shinySeen(), language, messages)));
        lore.add(messages.text(language, "collection_unavailable"));
        if (detail) {
            Set<String> types = new TreeSet<>(); row.forms().forEach(form -> types.addAll(form.types()));
            lore.add(messages.text(language, "wiki_types", String.join(", ", types)));
            lore.add(messages.text(language, "wiki_forms_count", row.forms().size()));
            lore.add(messages.text(language, spawnAvailable ? "spawn_hint" : "wiki_spawn_unknown"));
        } else lore.add(messages.text(language, "wiki_details"));
        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }
    private static ItemStack formItem(String species, ProgressCalculator.Form form, String language, Messages messages) {
        var item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(form.name()));
        item.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(species),
                messages.text(language, "wiki_types", String.join(", ", new TreeSet<>(form.types()))),
                messages.text(language, "wiki_form_categories", String.join(", ", new TreeSet<>(form.labels()))),
                messages.text(language, "wiki_seen", yesNo(form.seen(), language, messages)),
                messages.text(language, "wiki_owned", yesNo(form.owned(), language, messages)),
                messages.text(language, "wiki_shiny_seen", yesNo(form.shinySeen(), language, messages)),
                messages.text(language, "collection_unavailable"))));
        return item;
    }
    private static ItemStack spawnItem(SpawnCatalog.Card card, String language, Messages messages) {
        var item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(card.rule()+" — "+messages.text(language,"spawn.kind."+card.kind()).getString()+(card.kind()==SpawnCatalog.Kind.RULE?"":" "+card.group()+" ["+card.part()+"/"+card.parts()+"]")));
        List<Component> lore = new ArrayList<>();
        lore.add(messages.text(language,"spawn_declared_form",card.declaredForm().isEmpty()?messages.text(language,"spawn_not_declared").getString():card.declaredForm()));
        if(card.kind()==SpawnCatalog.Kind.RULE)lore.add(messages.text(language,"spawn_rule_header"));
        else if(card.values().isEmpty())lore.add(messages.text(language,"spawn_values_unknown"));
        for(var value:card.values())lore.add(messages.text(language,"spawn.field."+value.field(),value.value()));
        if(card.composite())lore.add(messages.text(language,"spawn_composite"));
        lore.add(messages.text(language,"spawn_partial"));lore.add(messages.text(language,"spawn_form_warning"));
        item.set(DataComponents.LORE,new ItemLore(lore));return item;
    }
    private static String yesNo(boolean value, String language, Messages messages) {
        return messages.text(language, value ? "wiki_yes" : "wiki_no").getString();
    }
    private static void navigation(SimpleContainer contents, Map<Integer, Runnable> actions, int slot, Component name, Runnable action) {
        var arrow = new ItemStack(Items.ARROW); arrow.set(DataComponents.CUSTOM_NAME, name);
        contents.setItem(slot, arrow); actions.put(slot, action);
    }
    private static ItemStack collectionItem(CollectionDefinition definition, ProgressCalculator.CollectionProgress value, String language, Messages messages) {
        var item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.icon())));
        item.set(DataComponents.CUSTOM_NAME, Component.literal(displayName(definition, language, messages)));
        List<Component> lore = new ArrayList<>();
        if (!definition.description().isEmpty()) lore.add(Component.literal(definition.description()));
        lore.add(messages.text(language, "mode." + definition.mode().name()));
        var progress = value.progress();
        if (progress.registered().isPresent()) {
            lore.add(messages.text(language, "collection_counts", progress.registered().getAsInt(), progress.total()));
            lore.add(messages.text(language, "collection_seen", value.seen()));
        } else {
            lore.add(messages.text(language, "collection_unavailable"));
            lore.add(messages.text(language, "shiny_reason"));
            lore.add(messages.text(language, "collection_total", progress.total()));
        }
        String percent = progress.formattedPercentage(language).orElse(messages.text(language, "percent_unavailable").getString());
        lore.add(messages.text(language, "collection_percent", percent));
        if (progress.total() == 0) lore.add(messages.text(language, "empty_collection"));
        var filled=br.com.shirokaidex.menus.layout.ProgressMeter.filled(progress,12);
        if(filled.isPresent())lore.add(Component.empty().append(Component.literal("▰".repeat(filled.getAsInt())).withStyle(style->style.withColor(MenuStyle.SUCCESS))).append(Component.literal("▱".repeat(12-filled.getAsInt())).withStyle(style->style.withColor(0x555B67))));
        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }
    private static void closeButton(SimpleContainer contents, Map<Integer, Runnable> actions, ServerPlayer player, String language, Messages messages) {
        var close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, messages.text(language, "menu_close"));
        contents.setItem(22, close); actions.put(22, player::closeContainer);
    }
    static void show(ServerPlayer player,String title,SimpleContainer contents,Map<Integer,Runnable> actions){show(player,Component.literal(title),contents,actions);}
    static void show(ServerPlayer player, Component title, SimpleContainer contents, Map<Integer, Runnable> actions) {
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new MainMenu(id, player, contents, actions), title));
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || player.containerMenu != this) return;
        Runnable action = type == ClickType.PICKUP && button == 0 ? actions.get(slot) : null;
        try { if (action != null) action.run(); }
        finally { if (player.containerMenu == this) broadcastFullState(); }
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean stillValid(Player player) { return owner.equals(player.getUUID()); }
}
