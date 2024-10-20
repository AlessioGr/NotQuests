package rocks.gravili.notquests.paper.gui.typeserializer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import rocks.gravili.notquests.paper.gui.icon.Icon;

import java.lang.reflect.Type;

public class ItemTypeSerializer implements TypeSerializer<Icon> {
    @Override
    public Icon deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var material = node.node("material").getString();
        var pathToDisplayname = node.node("displayname").getString();
        var pathToLore = node.node("lore").getString();
        var skullTexture = node.node("skulltexture").getString();
        return new Icon(material, pathToDisplayname, pathToLore, skullTexture);
    }

    @Override
    public void serialize(Type type, @Nullable Icon obj, ConfigurationNode node) throws SerializationException {

    }
}
