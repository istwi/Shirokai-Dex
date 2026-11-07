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

/** Vanilla inventory protocol. Only explicitly registered left-click actions are executed. */
public final class MainMenu extends ChestMenu {
    private final UUID owner;
    private final Map<Integer, Runnable> actions;
    private MainMenu(int id, ServerPlayer player, SimpleContainer container, Map<Integer, Runnable> actions) {
        super(MenuType.GENERIC_9x3, id, player.getInventory(), container, 3);
        owner = player.getUUID(); this.actions = Map.copyOf(actions);
    }
    public static void open(ServerPlayer player, Settings settings, Messages messages,
                            Map<DexCollection, ProgressCalculator.CollectionProgress> values, Consumer<DexCollection> navigate) {
        var contents = new SimpleContainer(27);
        Map<Integer, Runnable> actions = new HashMap<>();
        for (var entry : Map.of(11, DexCollection.SHINY, 13, DexCollection.NATIONAL, 15, DexCollection.LEGENDARY).entrySet()) {
            contents.setItem(entry.getKey(), collectionItem(entry.getValue(), values.get(entry.getValue()), settings.language(), messages));
            actions.put(entry.getKey(), () -> navigate.accept(entry.getValue()));
        }
        closeButton(contents, actions, player, settings.language(), messages);
        show(player, settings.menuTitle(), contents, actions);
    }
    public static void openCollection(ServerPlayer player, Settings settings, Messages messages,
                                      DexCollection collection, ProgressCalculator.CollectionProgress value, Runnable back) {
        var contents = new SimpleContainer(27);
        Map<Integer, Runnable> actions = new HashMap<>();
        contents.setItem(13, collectionItem(collection, value, settings.language(), messages));
        var arrow = new ItemStack(Items.ARROW);
        arrow.set(DataComponents.CUSTOM_NAME, messages.text(settings.language(), "menu_back"));
        contents.setItem(18, arrow); actions.put(18, back);
        closeButton(contents, actions, player, settings.language(), messages);
        String title = messages.text(settings.language(), "dex." + collection.id()).getString();
        show(player, settings.menuTitle() + " — " + title, contents, actions);
    }
    private static ItemStack collectionItem(DexCollection collection, ProgressCalculator.CollectionProgress value, String language, Messages messages) {
        var item = new ItemStack(collection == DexCollection.LEGENDARY ? Items.NETHER_STAR : collection == DexCollection.SHINY ? Items.AMETHYST_SHARD : Items.BOOK);
        item.set(DataComponents.CUSTOM_NAME, messages.text(language, "dex." + collection.id()));
        List<Component> lore = new ArrayList<>();
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
        if (action != null) action.run();
        else broadcastFullState();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean stillValid(Player player) { return owner.equals(player.getUUID()); }
}
