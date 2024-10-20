package rocks.gravili.notquests.paper.gui.item;

import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem;

public class ScrollUpItem extends ScrollItem {
    private final ItemWrapper itemWrapper;
    public ScrollUpItem(ItemWrapper itemWrapper) {
        super(-1);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(ScrollGui gui) {
        return itemWrapper;
    }
}
