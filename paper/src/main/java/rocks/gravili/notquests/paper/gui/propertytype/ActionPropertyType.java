package rocks.gravili.notquests.paper.gui.propertytype;

import java.util.Arrays;
import java.util.List;

public class ActionPropertyType {
    private static final String STRING_REPRESENTATION = "action";
    private final String value;

    private ActionPropertyType(String value) {
        this.value = value;
    }

    public static ActionPropertyType of(String string) {
        return new ActionPropertyType(string);
    }

    public List<String> toStringList() {
        return Arrays.asList(value.split("\n"));
    }
}
