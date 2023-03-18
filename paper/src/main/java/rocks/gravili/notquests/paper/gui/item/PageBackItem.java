package rocks.gravili.notquests.paper.gui.item;

import de.studiocode.invui.gui.impl.PagedGUI;
import de.studiocode.invui.item.ItemProvider;
import de.studiocode.invui.item.ItemWrapper;
import de.studiocode.invui.item.impl.controlitem.PageItem;

public class PageBackItem extends PageItem {

    private ItemWrapper itemWrapper;

    public PageBackItem(ItemWrapper itemWrapper) {
        super(false);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(PagedGUI gui) {
        return itemWrapper;
    }
}
