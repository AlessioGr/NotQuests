package rocks.gravili.notquests.paper.gui.item;

import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class PageForwardItem extends PageItem {
    private final ItemWrapper itemWrapper;

    public PageForwardItem(ItemWrapper itemWrapper) {
        super(true);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull PagedGui<?> gui) {
        return itemWrapper;
    }
}
