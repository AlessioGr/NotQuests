package rocks.gravili.notquests.common.managers;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.nio.file.Path;

public class ConfigurationManager {
    GsonConfigurationLoader loader;
    BasicConfigurationNode root;

    public ConfigurationManager(){


        try {
            loader = GsonConfigurationLoader.builder()
                    .path(Path.of("general2.conf")) // Set where we will load and save to
                    .build();
            root = loader.load();
        } catch (Throwable e) {
            System.err.println("An error occurred while loading this configuration: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return;
        }

        try{
            stuff();
            save();
        }catch (Throwable e){
            e.printStackTrace();
        }

    }

    public void stuff() throws SerializationException {
        final ConfigurationNode countNode = root.node("messages", "count");
        final ConfigurationNode moodNode = root.node("messages", "mood");

        final String name = root.node("name").getString();
        final int count = countNode.getInt(Integer.MIN_VALUE);

        if (name == null || count == Integer.MIN_VALUE) {
            System.err.println("Invalid configuration");
            System.exit(2);
            return;
        }

        System.out.println("Hello, " + name + "!");
        System.out.println("You have " + count + " messages!");
        System.out.println("Thanks for viewing your messages");

        countNode.raw(0); // native type

        root.node("accesses").act(n -> {
            n.appendListNode().set(System.currentTimeMillis());
        });
    }

    public void save(){
        try {
            loader.save(root);
        } catch (final ConfigurateException e) {
            System.err.println("Unable to save your messages configuration! Sorry! " + e.getMessage());
            System.exit(1);
        }
    }

}
