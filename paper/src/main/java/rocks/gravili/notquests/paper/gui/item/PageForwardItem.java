package rocks.gravili.notquests.paper.gui.item;

import de.studiocode.invui.gui.impl.PagedGUI;
import de.studiocode.invui.item.ItemProvider;
import de.studiocode.invui.item.ItemWrapper;
import de.studiocode.invui.item.impl.controlitem.PageItem;

public class PageForwardItem extends PageItem {
    private ItemWrapper itemWrapper;

    public PageForwardItem(ItemWrapper itemWrapper) {
        super(true);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(PagedGUI gui) {
        return itemWrapper;
    }
}
