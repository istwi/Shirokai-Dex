package br.com.shirokaidex.menus;

import br.com.shirokaidex.cobblemon.CobblemonAccess;
import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.List;

/** Vanilla inventory protocol: every interaction is blocked except the close button. */
public final class MainMenu extends ChestMenu {
    private MainMenu(int id, ServerPlayer player, SimpleContainer container) {
        super(MenuType.GENERIC_9x3, id, player.getInventory(), container, 3);
    }
    public static void open(ServerPlayer player, Settings settings, Messages messages, CobblemonAccess.Progress progress) {
        var contents = new SimpleContainer(27);
        var book = new ItemStack(Items.BOOK);
        book.set(DataComponents.CUSTOM_NAME, messages.text(settings.language(), "menu_progress"));
        book.set(DataComponents.LORE, new ItemLore(List.of(
                messages.text(settings.language(), "progress", progress.owned(), progress.seen(), progress.total()),
                messages.text(settings.language(), "foundation"))));
        contents.setItem(13, book);
        var close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, messages.text(settings.language(), "menu_close"));
        contents.setItem(22, close);
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new MainMenu(id, player, contents), Component.literal(settings.menuTitle())));
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == 22 && type == ClickType.PICKUP && button == 0 && player instanceof ServerPlayer serverPlayer) serverPlayer.closeContainer();
        else broadcastFullState();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean stillValid(Player player) { return true; }
}
