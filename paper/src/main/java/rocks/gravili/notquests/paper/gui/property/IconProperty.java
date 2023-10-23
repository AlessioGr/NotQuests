package rocks.gravili.notquests.paper.gui.property;

import rocks.gravili.notquests.paper.gui.property.types.BaseIconProperty;

public class IconProperty {
    private final String key;
    private final BaseIconProperty value;

    private IconProperty(String key, BaseIconProperty value) {
        this.key = key;
        this.value = value;
    }

    public static IconProperty of(String key, BaseIconProperty value) {
        return  new IconProperty(key, value);
    }

    public String getKey() {
        return key;
    }

    public BaseIconProperty getValue() {
        return value;
    }
}
