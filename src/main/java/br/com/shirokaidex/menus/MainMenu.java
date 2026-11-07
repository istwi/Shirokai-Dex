package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
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
        super(MenuType.GENERIC_9x3, id, player.getInventory(), container, 3);
        owner = player.getUUID(); this.actions = Map.copyOf(actions);
    }
    private static final List<Integer> CONTENT_SLOTS = List.of(13, 11, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17);
    public static String displayName(CollectionDefinition definition, String language, Messages messages) {
        return definition.name().isEmpty() ? messages.text(language, "dex." + definition.id()).getString() : definition.name();
    }
    public static void open(ServerPlayer player, Settings settings, Messages messages,
                            List<CollectionDefinition> visible, Map<String, ProgressCalculator.CollectionProgress> values,
                            int requestedPage, Consumer<String> navigate, IntConsumer pageNavigation) {
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
        closeButton(contents, actions, player, settings.language(), messages);
        String title = settings.menuTitle() + (pages > 1 ? " [" + (page + 1) + "/" + pages + "]" : "");
        show(player, title, contents, actions);
    }
    public static void openCollection(ServerPlayer player, Settings settings, Messages messages,
                                      CollectionDefinition definition, ProgressCalculator.CollectionProgress value, Runnable back) {
        var contents = new SimpleContainer(27);
        Map<Integer, Runnable> actions = new HashMap<>();
        contents.setItem(13, collectionItem(definition, value, settings.language(), messages));
        navigation(contents, actions, 18, messages.text(settings.language(), "menu_back"), back);
        closeButton(contents, actions, player, settings.language(), messages);
        show(player, settings.menuTitle() + " — " + displayName(definition, settings.language(), messages), contents, actions);
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
        lore.add(messages.text(language, "foundation"));
        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }
    private static void closeButton(SimpleContainer contents, Map<Integer, Runnable> actions, ServerPlayer player, String language, Messages messages) {
        var close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, messages.text(language, "menu_close"));
        contents.setItem(22, close); actions.put(22, player::closeContainer);
    }
    private static void show(ServerPlayer player, String title, SimpleContainer contents, Map<Integer, Runnable> actions) {
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new MainMenu(id, player, contents, actions), Component.literal(title)));
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
