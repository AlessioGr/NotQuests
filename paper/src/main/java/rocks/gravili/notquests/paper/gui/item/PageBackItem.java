package rocks.gravili.notquests.paper.gui.item;

import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class PageBackItem extends PageItem {
    private final ItemWrapper itemWrapper;

    public PageBackItem(ItemWrapper itemWrapper) {
        super(false);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull PagedGui<?> gui) {
        return itemWrapper;
    }
}
