package rocks.gravili.notquests.paper.gui;

import de.studiocode.inventoryaccess.component.AdventureComponentWrapper;
import de.studiocode.invui.window.impl.single.SimpleWindow;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.gui.icon.Button;
import rocks.gravili.notquests.paper.gui.icon.Icon;
import rocks.gravili.notquests.paper.gui.typeserializer.IconTypeSerializer;
import rocks.gravili.notquests.paper.gui.typeserializer.ItemTypeSerializer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public class GuiService {
    private final NotQuests notQuests;
    private final Map<String, CustomGui> guis;
    private final Set<String> knownProperties;

    public GuiService(NotQuests notQuests) {
        this.notQuests = notQuests;
        this.guis = new HashMap<>();
        this.knownProperties = new HashSet<>();
    }

    public void showGui(String guiName, Player player, GuiContext guiContext) {

        var customGui = guis.get(guiName);
        if (customGui == null) {
            notQuests.getLogManager().info("Failed showing gui '" + guiName + "' to player " + player.getName());
            return;
        }
        var title = new AdventureComponentWrapper(notQuests.getLanguageManager().getComponent(customGui.getPathToTitle(), player, guiContext.getAsObjectArray()));
        var window = new SimpleWindow(player, title, customGui.buildGui(notQuests, guiContext));
        window.show();
    }

    public void saveAllDefaultGuis() {
        saveDefaultGui("main-base");
        saveDefaultGui("main-tab-one");
        saveDefaultGui("main-tab-two");
        saveDefaultGui("main-tab-three");
    }

    public void saveDefaultGui(String guiName) {
        notQuests.getMain().saveResource("guis/" + guiName + ".yml", false);
    }

    public void loadAllGuis() {
        var guisFolder = notQuests.getMain().getDataFolder().toPath().resolve(Paths.get("guis"));

        var fileNames = guisFolder.toFile().list();

        if (fileNames == null) {
            notQuests.getLogManager().warn("No guis found");
            return;
        }

        Arrays.stream(fileNames).filter(fileName -> fileName.endsWith(".yml")).forEach(fileName -> {
            notQuests.getLogManager().info("Found gui file: " + fileName + "");
            notQuests.getLogManager().info("Attempting to load gui from file...");
            loadGui(guisFolder, fileName);
        });
    }

    public void loadGui(Path path, String name) {
        var guiName = name.replace(".yml", "");
        var guiPath = path.resolve(Paths.get(name));
        var yamlLoader = YamlConfigurationLoader.builder()
                .path(guiPath)
                .defaultOptions(opts -> opts.serializers(build -> build.register(Button.class, new IconTypeSerializer())))
                .defaultOptions(opts -> opts.serializers(build -> build.register(Icon.class, new ItemTypeSerializer())))
                .build();

        CommentedConfigurationNode rootNode = null;

        try {
            rootNode = yamlLoader.load();
        } catch (ConfigurateException e) {
            notQuests.getLogManager().warn("Failed loading gui '" + name + "'", e);
            notQuests.getMain().getLogger().log(Level.WARNING, "Error:", e);

            return;
        }

        List<String> structure = null;

        try {
            structure = rootNode.node("structure").getList(String.class);
        } catch (SerializationException e) {
            notQuests.getLogManager().warn("An error occurred while loading the gui " + name);
            notQuests.getMain().getLogger().log(Level.WARNING, "ERROR:", e);
        }

        if (structure == null) {
            notQuests.getLogManager().warn("An error occurred while loading the gui " + name);
            notQuests.getLogManager().warn("ERROR: No structure specified");
            return;
        }

        var type = rootNode.node("type").getString();
        if (type == null) {
            type = "NORMAL";
        }

        var pathToTitle = rootNode.node("title").getString();

        // Load gui "tabs"
        var additionalGuis = List.of("");

        try {
            additionalGuis = rootNode.node("additionalguis").getList(String.class);
        } catch (SerializationException e) {
            notQuests.getLogManager().warn("An error occurred while loading the gui " + name);
            notQuests.getMain().getLogger().log(Level.WARNING, "Error:", e);
        }


        var iconsMap = rootNode.node("icons").childrenMap();
        var icons = new HashMap<Character, Button>();
        iconsMap.forEach((key, node) -> {
            try {
                var icon = iconsMap.get(key).get(Button.class);
                icons.put(String.valueOf(key).charAt(0), icon);
            } catch (SerializationException | IllegalArgumentException e) {
                notQuests.getLogManager().warn("An error occurred while loading the gui " + name);
                notQuests.getMain().getLogger().log(Level.WARNING, "Error:", e);
            }

        });

        var itemsMap = rootNode.node("items").childrenMap();
        var items = new HashMap<String, Icon>();
        itemsMap.forEach((key, node) -> {
            try {
                var item = itemsMap.get(key).get(Icon.class);
                items.put(String.valueOf(key), item);
            } catch (SerializationException e) {
                notQuests.getLogManager().warn("An error occurred while loading the gui " + name);
                notQuests.getMain().getLogger().log(Level.WARNING, "Error:", e);
            }

        });

        icons.forEach((key, icon) -> icon.registerIcons(items));

        var customGui = new CustomGui(structure.toArray(String[]::new), pathToTitle, type, icons, additionalGuis);

        guis.put(guiName, customGui);
    }

    public Map<String, CustomGui> getGuis() {
        return guis;
    }
}
