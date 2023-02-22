package rocks.gravili.notquests.paper.gui;

import com.github.stefvanschie.inventoryframework.exception.XMLLoadException;
import com.github.stefvanschie.inventoryframework.gui.type.*;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GuiService {
    private final NotQuests notQuests;

    // TODO: add a config variable for that
    private static final String EMPTY_DISPLAYNAME_KEYWORD = "EMPTY";
    private final GuiActions guiActions;
    private final Map<String, Gui> guis;

    public GuiService(NotQuests notQuests) {
        this.notQuests = notQuests;
        this.guis = new HashMap<>();
        this.guiActions = new GuiActions(notQuests);
    }

    /**
     * Saves all default guis to the guis folder of the plugin
     */
    public void saveDefaultGuis() {
        saveDefaultGui("testgui");
    }

    /**
     * Saves a gui with the specified name from the resources folder to the guis folder of the plugin
     * @param name the name of the gui that should be saved
     */
    public void saveDefaultGui(String name){
        notQuests.getMain().saveResource(Paths.get("guis/", name + ".xml").toString(), false);
    }

    /**
     * Loads a Gui from an XML file located in a specified folder
     * @param guisFolder the folder in which the gui is located
     * @param fileName the name of the gui file. The name is also used to identify the gui later on
     */
    public void loadGui(Path guisFolder, String fileName) {
        var guiPath = guisFolder.resolve(fileName);
        Gui gui = null;
        try (InputStream inputStream = Files.newInputStream(guiPath)) {
            gui = Gui.load(guiActions, inputStream);
            var guiName = fileName.replace(".xml", "");
            guis.put(guiName, gui);
        } catch (IOException e) {
            notQuests.getLogManager().warn("Failed to load gui with name ' " + fileName + "' See below for more information", e);
            e.printStackTrace();
        } catch (XMLLoadException e) {
            notQuests.getLogManager().warn("Failed to load gui with name ' " + fileName + "'", e);
            notQuests.getLogManager().warn("Type: Error in XML syntax, see below for more information ");
            e.printStackTrace();
        }
    }

    /**
     * Loads all guis found in the predefined folder into the gui storage
     */
    public void loadAllGuis() {
        var guisFolder = notQuests.getMain().getDataFolder().toPath().resolve(Paths.get("guis"));

        var fileNames = guisFolder.toFile().list();

        if (fileNames == null) {
            saveDefaultGuis();
            loadAllGuis();
            return;
        }

        Arrays.stream(fileNames).filter(fileName -> fileName.endsWith(".xml")).forEach(fileName -> {
            loadGui(guisFolder, fileName);
        });
    }

    /**
     * Opens the first gui matching the given name for the player
     * @param guiName the nome of the gui that should be opened
     * @param player the player to open the gui for
     */
    public void showGui(String guiName, Player player) {
        var firstMatch = guis.get(guiName);

        if (firstMatch == null) {
            // TODO: Error message
            return;
        }

        var gui = firstMatch.copy();

        var langManager = notQuests.getLanguageManager();

        // Replace title path with actual title
        if (gui instanceof NamedGui namedGui) {
            var title = LegacyComponentSerializer.legacySection().serialize(langManager.getComponent(namedGui.getTitle(), player));
            namedGui.setTitle(title);
        }

        /*
        CRAFTING TABLE: replace display name and lore for all items
         */
        if (gui instanceof CraftingTableGui craftingTableGui) {
            craftingTableGui.getInputComponent().getPanes().forEach(
                    pane -> pane.getItems().forEach(
                            guiItem -> {
                                var itemStack = getWithReplacedLoreAndDisplayName(guiItem.getItem(), player);
                                guiItem.setItem(itemStack);
                            }
                    )

            );
            craftingTableGui.getOutputComponent().getPanes().forEach(
                    pane -> pane.getItems().forEach(
                            guiItem -> {
                                var itemStack = getWithReplacedLoreAndDisplayName(guiItem.getItem(), player);
                                guiItem.setItem(itemStack);
                            }
                    )
            );
        }

        /*
        CHEST: replace display name and lore for all items
         */
        if (gui instanceof ChestGui chestGui) {
            chestGui.getInventoryComponent().getPanes().forEach(
                    pane -> pane.getItems().forEach(
                            guiItem -> {
                                var itemStack = getWithReplacedLoreAndDisplayName(guiItem.getItem(), player);
                                guiItem.setItem(itemStack);
                            }
                    )
            );
        }

        /*
        HOPPER: replace display name and lore for all items
         */
        if (gui instanceof HopperGui hopperGui) {
            hopperGui.getSlotsComponent().getPanes().forEach(
                    pane -> pane.getItems().forEach(
                            guiItem -> {
                                var itemStack = getWithReplacedLoreAndDisplayName(guiItem.getItem(), player);
                                guiItem.setItem(itemStack);
                            }
                    )
            );
        }

        /*
        DROPPER: replace display name and lore for all items
         */
        if (gui instanceof DropperGui dropperGui) {
            dropperGui.getContentsComponent().getPanes().forEach(
                    pane -> pane.getItems().forEach(
                            guiItem -> {
                                var itemStack = getWithReplacedLoreAndDisplayName(guiItem.getItem(), player);
                                guiItem.setItem(itemStack);
                            }

                    )
            );
        }

        // TODO: Implement missing gui types

        // Important for the changes to take effect
        gui.update();

        // Open the gui for the player
        gui.show(player);
    }


    /**
     * Replace display name and lore for given item
     * @param itemStack the item to replace lore and display name
     * @param player the player which the placeholders should be replaced for
     * @return the item with new lore and display name
     */
    private ItemStack getWithReplacedLoreAndDisplayName(ItemStack itemStack, Player player) {
        var newLore = fetchLore(itemStack, player);
        itemStack.lore(newLore);

        var displayName = fetchDisplayName(itemStack, player);
        var itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(displayName);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Retrieve the lore of an item assuming the current first lore line is a path to the actual lore
     * @param itemStack the item for which we are looking for the lore
     * @param player the player for whom we want to replace the placeholders in the lore
     * @return the new lore as a component list with all placeholder replaced
     */
    private List<Component> fetchLore(ItemStack itemStack, Player player) {
        var currentLore = itemStack.lore();
        if (currentLore == null) {
            return Collections.emptyList();
        }
        var loreConfigPath = PlainTextComponentSerializer.plainText().serialize(currentLore.get(0));
        return notQuests.getLanguageManager().getComponentList(loreConfigPath, player);
    }

    /**
     * Retrieve the display name of an item assuming that the current display name is a path to the actual name
     * @param itemStack the item for which we are looking for the display name
     * @param player the player for whom we want to replace the placeholders in the display name
     * @return the new lore as a component list with all placeholder replaced
     */
    private Component fetchDisplayName(ItemStack itemStack, Player player) {
        var currentDisplayName = itemStack.displayName();

        var displayNameAsString = PlainTextComponentSerializer.plainText().serialize(currentDisplayName).replace("[", "").replace("]", "");

        if (displayNameAsString.equalsIgnoreCase(EMPTY_DISPLAYNAME_KEYWORD)) {
            return Component.empty();
        }

        return notQuests.getLanguageManager().getComponent(displayNameAsString, player);
    }

    /**
     * Returns a with all guis and their names
     * @return A map with all guis, identified by their names
     */
    public final Map<String, Gui> getGuis() {
        return guis;
    }
}
