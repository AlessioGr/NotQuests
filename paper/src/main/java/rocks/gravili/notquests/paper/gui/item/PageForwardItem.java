package rocks.gravili.notquests.paper.gui.item;

import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class PageForwardItem extends PageItem {
    private ItemWrapper itemWrapper;

    public PageForwardItem(ItemWrapper itemWrapper) {
        super(true);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(PagedGui gui) {
        return itemWrapper;
    }
}
