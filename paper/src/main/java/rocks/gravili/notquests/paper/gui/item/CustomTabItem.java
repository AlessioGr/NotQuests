package rocks.gravili.notquests.paper.gui.item;

import de.studiocode.inventoryaccess.component.AdventureComponentWrapper;
import de.studiocode.invui.gui.impl.TabGUI;
import de.studiocode.invui.item.ItemProvider;
import de.studiocode.invui.item.ItemWrapper;
import de.studiocode.invui.item.impl.controlitem.TabItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

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
    public ItemProvider getItemProvider(TabGUI gui) {
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
