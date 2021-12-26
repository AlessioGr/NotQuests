package rocks.gravili.notquests.paper.managers;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.interfaces.core.arguments.ArgumentKey;
import org.incendo.interfaces.core.click.ClickHandler;
import org.incendo.interfaces.paper.PaperInterfaceListeners;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.incendo.interfaces.paper.transform.PaperTransform;
import org.incendo.interfaces.paper.type.ChestInterface;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GUIManager {
    private final NotQuests main;
    private ChestInterface interfaceActiveQuests;

    public GUIManager(final NotQuests main) {
        this.main = main;
        constructInterfaces();
    }

    public final String convert(final String old) { //Converts MiniMessage to legacy
        return main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old);
    }

    public void constructInterfaces(){
        PaperInterfaceListeners.install(main.getMain());

        interfaceActiveQuests = ChestInterface.builder()
                // This interface will have one row.
                .rows(1)
                // This interface will update every five ticks.
                .updates(true, 5)
                // Cancel all inventory click events
                .clickHandler(ClickHandler.cancel())
                // Fill the background with black stained glass panes
                .addTransform(PaperTransform.chestFill(
                        ItemStackElement.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                ))
                // Add some information to the pane
                .addTransform((pane, view) -> {
                    // Get the view arguments
                    // (Keep in mind - these arguments may be coming from a Supplier, so their values can change!)
                    final @NonNull Player player = view.arguments().get(ArgumentKey.of("player", Player.class));
                    final @NonNull ActiveQuest activeQuest = view.arguments().get(ArgumentKey.of("activequest", ActiveQuest.class));

                    ItemStack itemStack = new ItemStack(Material.BOOK);
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    itemMeta.displayName(main.getLanguageManager().getComponent("gui.activeQuests.button.activeQuestButton.name", player, activeQuest));
                    itemMeta.lore(main.getLanguageManager().getComponentList("gui.activeQuests.button.activeQuestButton.lore", player, activeQuest));

                    itemStack.setItemMeta(itemMeta);


                    // Return a pane with
                    return pane.element(ItemStackElement.of(itemStack,
                            // Handle click
                            (clickHandler) -> {
                               // final @NonNull InterfaceArguments argument = view.arguments();
                                player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                            }
                    ), 4, 0);
                })
                // Set the title
                .title(main.getLanguageManager().getComponent("gui.activeQuests.title", null ))
                // Build the interface
                .build();
    }


    public void showActiveQuests(QuestPlayer questPlayer, Player player) {
// Open the interface to the player.
        interfaceActiveQuests.open(PlayerViewer.of(player),
                // Create a HashMapInterfaceArgument with a time argument set to a
                // supplier that returns the current time printed all nice and pretty.
                HashMapInterfaceArgument.with("time", () -> {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                            LocalDateTime now = LocalDateTime.now();
                            return dtf.format(now);
                        })
                        .with("clicks", 0)
                        .build()
        );
    }


    public void showActiveQuestsOld(QuestPlayer questPlayer, Player player) {
        if (questPlayer != null) {
            String[] guiSetup = {
                    "zxxxxxxxx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "pxxxxxxxn"
            };
            InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.activeQuests.title", player)), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1));

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                final ItemStack materialToUse;
                if (!activeQuest.isCompleted()) {
                    materialToUse = activeQuest.getQuest().getTakeItem();
                } else {
                    materialToUse = new ItemStack(Material.EMERALD_BLOCK);
                }

                if (main.getConfiguration().showQuestItemAmount) {
                    count++;
                }

                group.addElement(new StaticGuiElement('e',
                        materialToUse,
                        count,
                        click -> {
                            player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                            return true;
                        },
                        convert(main.getLanguageManager().getString("gui.activeQuests.button.activeQuestButton.text", player, activeQuest))
                ));
            }

            gui.addElement(group);

            // Previous page
            gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
            // Next page
            gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

            gui.show(player);
        } else {
            player.sendMessage(main.parse(
                    main.getLanguageManager().getString("chat.no-quests-accepted", player)
            ));
        }
    }
}
