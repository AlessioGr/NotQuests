package rocks.gravili.notquests.paper.gui.propertytype;

import java.util.Arrays;
import java.util.List;

public class ConditionPropertyType {
    private static final String STRING_REPRESENTATION = "action";
    private final String value;

    private ConditionPropertyType(String value) {
        this.value = value;
    }

    public static ConditionPropertyType of(String string) {
        return new ConditionPropertyType(string);
    }

    public List<String> toStringList() {
        return Arrays.asList(value.split("\n"));
    }
}
