package rocks.gravili.notquests.paper.gui.item;

import de.studiocode.invui.gui.impl.ScrollGUI;
import de.studiocode.invui.item.ItemProvider;
import de.studiocode.invui.item.ItemWrapper;
import de.studiocode.invui.item.impl.controlitem.ScrollItem;

public class ScrollUpItem extends ScrollItem {
    private ItemWrapper itemWrapper;
    public ScrollUpItem(ItemWrapper itemWrapper) {
        super(-1);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(ScrollGUI gui) {
        return itemWrapper;
    }
}
