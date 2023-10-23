package rocks.gravili.notquests.paper.gui.typeserializer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import rocks.gravili.notquests.paper.gui.icon.Button;
import rocks.gravili.notquests.paper.gui.icon.ButtonType;
import rocks.gravili.notquests.paper.gui.property.IconProperty;
import rocks.gravili.notquests.paper.gui.property.types.ListIconProperty;
import rocks.gravili.notquests.paper.gui.property.types.StringIconProperty;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class IconTypeSerializer implements TypeSerializer<Button> {
    @Override
    public Button deserialize(Type type, ConfigurationNode node) throws SerializationException, IllegalArgumentException {
        var iconType = node.node("type").getString();

        var propertiesMap = node.node("properties").childrenMap();
        var iconProperties = new HashSet<IconProperty>();

        propertiesMap.forEach((key, value) -> {
            IconProperty iconProperty;
            if (value.isList()) {
                try {
                    iconProperty = IconProperty.of(String.valueOf(key), ListIconProperty.of(value.getList(String.class)));
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                iconProperty = IconProperty.of(String.valueOf(key), StringIconProperty.of(value.getString()));
            }

            iconProperties.add(iconProperty);

        });

        var itemKeys = node.node("items").getList(String.class);

        return new Button(
                ButtonType.valueOf(iconType),
                iconProperties,
                itemKeys == null ? Set.of("") : new HashSet<>(itemKeys)

        );
    }

    @Override
    public void serialize(Type type, @Nullable Button obj, ConfigurationNode node) throws SerializationException {

    }
}
