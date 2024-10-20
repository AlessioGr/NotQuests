package rocks.gravili.notquests.paper.gui.property.types;

public record StringIconProperty(String value) implements BaseIconProperty {

    public static StringIconProperty of(String value) {
        return new StringIconProperty(value);
    }
}
