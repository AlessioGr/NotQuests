package rocks.gravili.notquests.paper.gui.item;

import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem;

public class ScrollDownItem extends ScrollItem {
    private final ItemWrapper itemWrapper;

    public ScrollDownItem(ItemWrapper itemWrapper) {
        super(1);
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull ScrollGui<?> gui) {
        return itemWrapper;
    }
}
