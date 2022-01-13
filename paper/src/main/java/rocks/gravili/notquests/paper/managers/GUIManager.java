package rocks.gravili.notquests.paper.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.incendo.interfaces.core.arguments.ArgumentKey;
import org.incendo.interfaces.core.arguments.HashMapInterfaceArguments;
import org.incendo.interfaces.core.arguments.InterfaceArguments;
import org.incendo.interfaces.core.click.ClickHandler;
import org.incendo.interfaces.core.element.Element;
import org.incendo.interfaces.core.pane.GridPane;
import org.incendo.interfaces.core.pane.Pane;
import org.incendo.interfaces.core.transform.types.PaginatedTransform;
import org.incendo.interfaces.core.util.Vector2;
import org.incendo.interfaces.paper.PaperInterfaceListeners;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.incendo.interfaces.paper.pane.ChestPane;
import org.incendo.interfaces.paper.transform.PaperTransform;
import org.incendo.interfaces.paper.type.ChestInterface;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;


public class GUIManager {
    private final NotQuests main;
    private ChestInterface mainInterface;

    private ChestInterface interfaceActiveQuests;

    private final ItemStack chest, abort, coins, books, info;


    public GUIManager(final NotQuests main) {
        this.main = main;
        chest = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) chest.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTcxNDUxNmU2NTY1YjgxMmZmNWIzOWVhMzljZDI4N2FmZWM4ZmNjMDZkOGYzMDUyMWNjZDhmMWI0Y2JmZGM2YiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            chest.setItemMeta(meta);
        }

        abort = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) abort.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQwYTE0MjA4NDRjZTIzN2E0NWQyZTdlNTQ0ZDEzNTg0MWU5ZjgyZDA5ZTIwMzI2N2NmODg5NmM4NTE1ZTM2MCJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            abort.setItemMeta(meta);
        }

        coins = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) coins.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODM4MWM1MjlkNTJlMDNjZDc0YzNiZjM4YmI2YmEzZmRlMTMzN2FlOWJmNTAzMzJmYWE4ODllMGEyOGU4MDgxZiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            coins.setItemMeta(meta);
        }

        books = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) books.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVlOGQ2ZjVjYjdhMzVhNGRkYmRhNDZmMDQ3ODkxNWRkOWViYmNlZjkyNGViOGNhMjg4ZTkxZDE5YzhjYiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            books.setItemMeta(meta);
        }

        info = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) info.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q5MWY1MTI2NmVkZGM2MjA3ZjEyYWU4ZDdhNDljNWRiMDQxNWFkYTA0ZGFiOTJiYjc2ODZhZmRiMTdmNGQ0ZSJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            info.setItemMeta(meta);
        }

        constructInterfaces();
    }

    public final String convert(final String old) { //Converts MiniMessage to legacy
        return main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old);
    }

    public void constructInterfaces(){
        PaperInterfaceListeners.install(main.getMain());

        mainInterface = ChestInterface.builder()
                // This interface will have one row.
                .rows(6)
                // This interface will update every five ticks.
                .updates(true, 5)
                // Cancel all inventory click events
                .clickHandler(ClickHandler.cancel())
                // Fill the background with black stained glass panes
                .addTransform(PaperTransform.chestFill(
                        ItemStackElement.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                ))
                .addTransform((pane, view) -> {
                    ChestPane result;
                    // Get the view arguments
                    // (Keep in mind - these arguments may be coming from a Supplier, so their values can change!)
                    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));
                    final QuestPlayer questPlayer = view.arguments().getOrDefault(ArgumentKey.of("questPlayer", QuestPlayer.class), null);

                    ItemMeta coinsItemMeta = coins.getItemMeta();
                    ArrayList<Component> coinsLore = new ArrayList<>();
                    coinsLore.add(main.getLanguageManager().getComponent("gui.main.button.questpoints.text", player, questPlayer));
                    coinsItemMeta.lore(coinsLore);
                    coins.setItemMeta(coinsItemMeta);

                    result = pane.element(ItemStackElement.of(coins,
                            // Handle click
                            (clickHandler) -> {
                                player.sendMessage("clicked me!");
                            }
                    ), 8, 0);

                    ItemMeta takeItemMeta = chest.getItemMeta();
                    ArrayList<Component> takeLore = new ArrayList<>();
                    takeLore.add(main.getLanguageManager().getComponent("gui.main.button.takequest.text", player, questPlayer));
                    takeItemMeta.lore(takeLore);
                    chest.setItemMeta(takeItemMeta);

                    result = result.element(ItemStackElement.of(chest,
                            // Handle click
                            (clickHandler) -> {
                                // final @NonNull InterfaceArguments argument = view.arguments();
                                player.chat("/notquests progress ");
                            }
                    ), 0, 1);

                    result = result.element(ItemStackElement.of(abort,
                            // Handle click
                            (clickHandler) -> {
                                // final @NonNull InterfaceArguments argument = view.arguments();
                                player.chat("/notquests progress ");
                            }
                    ), 0, 2);

                    result = result.element(ItemStackElement.of(books,
                            // Handle click
                            (clickHandler) -> {
                                // final @NonNull InterfaceArguments argument = view.arguments();
                                player.chat("/notquests progress ");
                            }
                    ), 0, 3);


                    return result;
                })
                // Set the title
                .title(main.getLanguageManager().getComponent("gui.main.title", null ))
                // Build the interface
                .build();

        interfaceActiveQuests = ChestInterface.builder()
                // This interface will have one row.
                .rows(6)
                // This interface will update every five ticks.
                .updates(true, 5)
                // Cancel all inventory click events
                .clickHandler(ClickHandler.cancel())
                // Fill the background with black stained glass panes
                .addTransform(PaperTransform.chestFill(
                        ItemStackElement.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                ))
                .addTransform((pane, view) -> {
                    // Get the view arguments
                    // (Keep in mind - these arguments may be coming from a Supplier, so their values can change!)
                    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));
                    final QuestPlayer questPlayer = view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));

                    if(questPlayer != null){
                        ArrayList<ItemStackElement<ChestPane>> list = new ArrayList<>() {
                            {
                                for(ActiveQuest activeQuest : questPlayer.getActiveQuests()){
                                    player.sendMessage("Added one active Quest!");
                                    ItemStack itemStack = new ItemStack(Material.BOOK);
                                    ItemMeta itemMeta = itemStack.getItemMeta();

                                    itemMeta.displayName(main.getLanguageManager().getComponent("gui.activeQuests.button.activeQuestButton.name", player, activeQuest));
                                    itemMeta.lore(main.getLanguageManager().getComponentList("gui.activeQuests.button.activeQuestButton.lore", player, activeQuest));

                                    itemStack.setItemMeta(itemMeta);

                                    add(ItemStackElement.of(itemStack,
                                            // Handle click
                                            (clickHandler) -> {
                                                // final @NonNull InterfaceArguments argument = view.arguments();
                                                player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                                            }
                                    ));
                                }
                            }
                        };
                        PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> paginatedTransform = new PaginatedTransform<>(
                                Vector2.at(2, 1), Vector2.at(6, 2), list);
                        return paginatedTransform.apply(pane, view);
                    }
                    return pane;

                   /* // Return a pane with
                    return pane.element(ItemStackElement.of(itemStack,
                            // Handle click
                            (clickHandler) -> {
                               // final @NonNull InterfaceArguments argument = view.arguments();
                                player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                            }
                    ), 4, 0);*/
                })
                // Set the title
                .title(main.getLanguageManager().getComponent("gui.activeQuests.title", null ))
                // Build the interface
                .build();

    }


    public void showActiveQuestsNew(QuestPlayer questPlayer, Player player) {
        // Open the interface to the player.

        mainInterface.open(PlayerViewer.of(player), HashMapInterfaceArguments.with(ArgumentKey.of("player", Player.class), player).with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer).build()
        );
    }


    public void showActiveQuestsGUI(QuestPlayer questPlayer, Player player) {
        showActiveQuestsNew(questPlayer, player);
        if(true){
            return;
        }
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



    public void showTakeQuestsGUI(QuestPlayer questPlayer, Player player) {
        if(main.getDataManager().getCategories().size() == 1) {
            showTakeQuestsGUIOfCategory(questPlayer, player, main.getDataManager().getDefaultCategory());
            return;
        }

        String[] guiSetup = {
                "zxxxxxxxx",
                "xgggggggx",
                "xgggggggx",
                "xgggggggx",
                "xgggggggx",
                "pxxxxxxxn"
        };
        InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.takeQuestChoose.title", player)), guiSetup);
        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

        int count = 0;
        GuiElementGroup group = new GuiElementGroup('g');

        for (final Category category : main.getDataManager().getCategories()) {
            final ItemStack materialToUse = new ItemStack(Material.CHEST);

            String displayName = "<RESET><WHITE>" + category.getCategoryName();

            group.addElement(new StaticGuiElement('e',
                    materialToUse,
                    count,
                    click -> {
                        showTakeQuestsGUIOfCategory(questPlayer, player, category);
                        return true;
                    },
                    convert(displayName)
            ));

        }


        gui.addElement(group);
        // Previous page
        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
        // Next page
        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

        gui.show(player);
    }

    public void showTakeQuestsGUIOfCategory(QuestPlayer questPlayer, Player player, final Category category) {
        String[] guiSetup = {
                "zxxxxxxxx",
                "xgggggggx",
                "xgggggggx",
                "xgggggggx",
                "xgggggggx",
                "pxxxxxxxn"
        };
        InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.takeQuestChoose.title", player)), guiSetup);
        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

        int count = 0;
        GuiElementGroup group = new GuiElementGroup('g');

        for (final Quest quest : main.getQuestManager().getAllQuests()) {
            if (quest.isTakeEnabled() && quest.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())) {
                final ItemStack materialToUse = quest.getTakeItem();

                if (main.getConfiguration().showQuestItemAmount) {
                    count++;
                }

                String displayName = quest.getQuestFinalName();

                displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questNamePrefix", player, quest) + displayName;

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.acceptedSuffix", player, quest);
                }
                String description = "";
                if (!quest.getQuestDescription().isBlank()) {

                    description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix", player, quest)
                            + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength
                    );
                }

                group.addElement(new StaticGuiElement('e',
                        materialToUse,
                        count,
                        click -> {
                            player.chat("/notquests preview " + quest.getQuestName());
                            return true;
                        },
                        convert(displayName),
                        convert(description),
                        convert(main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.bottomText", player))
                ));

            }
        }

        gui.addElement(group);
        // Previous page
        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
        // Next page
        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

        gui.show(player);
    }
}
