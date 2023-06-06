package rocks.gravili.notquests.paper.gui.item;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.TabItem;

import java.util.List;

public class CustomTabItem extends TabItem {

    private final int tab;
    private final List<ItemStack> itemStacks;
    private final Component newTitle;
    public CustomTabItem(int tab, Component newTitle, List<ItemStack> itemStacks) {
        super(tab);
        this.tab = tab;
        this.itemStacks = itemStacks;
        this.newTitle = newTitle;
    }

    @Override
    public ItemProvider getItemProvider(TabGui gui) {
        var activeTabItem = itemStacks.get(0);
        var inactiveTabItem = itemStacks.get(0);
        if (itemStacks.size() > 1) {
            inactiveTabItem = itemStacks.get(1);
        }
        if (gui.getCurrentTab() == tab) {
            return new ItemWrapper(activeTabItem);
        } else {
            return new ItemWrapper(inactiveTabItem);
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        super.handleClick(clickType, player, event);
        var window = getWindows().stream().filter(w -> w.getCurrentViewer().equals(player)).findFirst().get();

        window.changeTitle(new AdventureComponentWrapper(newTitle));
    }
}
