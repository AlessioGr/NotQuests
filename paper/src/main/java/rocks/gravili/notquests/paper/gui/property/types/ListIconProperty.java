package rocks.gravili.notquests.paper.gui.property.types;

import java.util.List;

public record ListIconProperty(List<String> value) implements BaseIconProperty {

    public static ListIconProperty of(List<String> value) {
        return new ListIconProperty(value);
    }
}
