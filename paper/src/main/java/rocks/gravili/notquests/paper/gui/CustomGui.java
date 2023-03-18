package rocks.gravili.notquests.paper.gui;

import de.studiocode.invui.gui.GUI;
import de.studiocode.invui.gui.builder.GUIBuilder;
import de.studiocode.invui.gui.builder.guitype.GUIType;
import de.studiocode.invui.gui.structure.Markers;
import de.studiocode.invui.gui.structure.Structure;
import de.studiocode.invui.item.Item;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.gui.icon.Button;
import rocks.gravili.notquests.paper.gui.icon.ButtonType;
import rocks.gravili.notquests.paper.gui.icon.SpecialIconType;
import rocks.gravili.notquests.paper.gui.property.types.StringIconProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomGui {
    private final String pathToTitle;
    private final String type;
    private String[] structure;
    private final Map<Character, Button> icons;
    private final List<String> additionalGuis;
    private NotQuests notQuests;

    private GUI gui;

    public CustomGui(String[] structure, String pathToTitle, String type, Map<Character, Button> icons, List<String> additionalGuis) {
        this.pathToTitle = pathToTitle;
        this.structure = structure;
        this.type = type;
        this.icons = icons;
        this.additionalGuis = additionalGuis;
    }

    public GUI buildGui(NotQuests notQuests, GuiContext guiContext) {
        this.notQuests = notQuests;

        switch (type) {
            case "TAB" -> this.gui = handleTabGui(guiContext);
            case "PAGED_ITEMS" -> this.gui = handlePagedItemsGui(guiContext);
            /*
            case "PAGED_GUIS" -> this.gui = handlePagedGuisGui(guiObjectContext);
            case "SCROLL_ITEMS" -> this.gui = handleScrollItemsGui(guiObjectContext);
            case "SCROLL_GUIS" -> this.gui = handleScrollGuisGui(guiObjectContext);

             */
            default -> this.gui = handleNormalGui(guiContext);
        }
        return this.gui;
    }



    private SpecialIconType fetchSpecialIconType(Button icon) {
        SpecialIconType specialIconType;
        if (icon.getIconProperty("specialicontype").isEmpty()) {
            specialIconType = SpecialIconType.DEFAULT;
            notQuests.getLogManager().debug("Case 1 " + icon.getType());
        } else {
            if (icon.getIconProperty("specialicontype").get().getValue() instanceof StringIconProperty stringIconProperty) {
                notQuests.getLogManager().debug("Case 2 " + icon.getType());
                specialIconType = SpecialIconType.valueOf(stringIconProperty.value());
                notQuests.getLogManager().debug(specialIconType.name());
            } else {
                notQuests.getLogManager().debug("Case 3 " + icon.getType());
                specialIconType = SpecialIconType.DEFAULT;
            }
        }
        return specialIconType;
    }

    private GUI handleNormalGui(GuiContext guiContext) {
        var guibuilder = new GUIBuilder<>(GUIType.NORMAL).setStructure(structure);

        icons.forEach((key, icon) -> guibuilder.addIngredient(key, icon.buildItem(notQuests, guiContext)));
        for (Object o : guiContext.getAsObjectArray()) {
            notQuests.getLogManager().debug(String.valueOf(o));
        }

        return guibuilder.build();
    }
    private GUI handlePagedItemsGui(GuiContext guiContext) {
        var structureObject = new Structure(structure);
        var targetQuestPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(guiContext.getPlayer().getUniqueId());

        var guiBuilder = new GUIBuilder<>(GUIType.PAGED_ITEMS).setStructure(structure);

        //X and Y are reserved characters!
        var pagedIcon = icons.get('X');
        var fillerIcon = icons.get('Y');

        if (pagedIcon == null) {
            notQuests.getLogManager().debug("Paged icon null");
            return guiBuilder.build();
        }


        var pagedIconSpecialType = fetchSpecialIconType(pagedIcon);

        guiBuilder.addIngredient('X', Markers.ITEM_LIST_SLOT_HORIZONTAL);
        structureObject.addIngredient('X', Markers.ITEM_LIST_SLOT_HORIZONTAL);

        var items = new ArrayList<Item>();
        var guiSize = structureObject.getIngredientList().findItemListSlots().length;
        var numberOfTotalItems = guiSize;

        switch (pagedIconSpecialType) {
            case PLAYER_ACTIVE_QUEST -> {
                numberOfTotalItems = guiSize * ((targetQuestPlayer.getActiveQuests().size() / guiSize) + 1);
                targetQuestPlayer.getActiveQuests().forEach(activeQuest -> {
                    if (activeQuest != null) {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setActiveQuest(activeQuest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    }
                });
            }

            case NPC_SHOWN_QUEST -> {
                if (guiContext.getNqnpc() != null) {
                    var npc = guiContext.getNqnpc();
                    var npcQuests = notQuests.getQuestManager().getQuestsFromListWithVisibilityEvaluations(targetQuestPlayer, notQuests.getQuestManager().getQuestsAttachedToNPCWithShowing(npc));
                    npcQuests.forEach(quest -> notQuests.getLogManager().info(quest.getIdentifier()));
                    numberOfTotalItems = guiSize * ((npcQuests.size() / guiSize) + 1);
                    npcQuests.forEach(quest -> {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setQuest(quest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    });
                }
            }
            case CATEGORY -> {

            }
            case DEFAULT -> items.add(pagedIcon.buildItem(notQuests, guiContext));
        }

        if (fillerIcon != null) {
            while(items.size() < numberOfTotalItems) {
                items.add(fillerIcon.buildItem(notQuests, guiContext));
            }
        }

        icons.forEach((key, icon) -> {
            if (icon == null) {
                return;
            }
            if (fetchSpecialIconType(icon) != SpecialIconType.DEFAULT) {
                return;
            }
            guiBuilder.addIngredient(key, icon.buildItem(notQuests, guiContext));
        });


        guiBuilder.setItems(items);

        return guiBuilder.build();
    }

    /*
    private GUI handlePagedGuisGui(GuiObjectContext guiObjectContext) {}
    private GUI handleScrollItemsGui(GuiObjectContext guiObjectContext){}
    private GUI handleScrollGuisGui(GuiObjectContext guiObjectContext) {}

     */
    private GUI handleTabGui(GuiContext guiContext) {
        var tabGuis = new ArrayList<GUI>();

        additionalGuis.forEach(string -> {
            var tabGui = notQuests.getGuiService().getGuis().get(string);
            if (tabGui == null) {
                return;
            }

            tabGuis.add(tabGui.buildGui(notQuests, guiContext));
        });

        var guiBuilder = new GUIBuilder<>(GUIType.TAB)
                .setStructure(structure)
                .setGUIs(tabGuis)
                .addIngredient('X', Markers.ITEM_LIST_SLOT_HORIZONTAL);

        icons.forEach((key, icon) -> {
            if (icon == null) {
                return;
            }
            if (icon.getType() == ButtonType.PAGED) {
                return;
            }
            guiBuilder.addIngredient(key, icon.buildItem(notQuests, guiContext));
        });

        return guiBuilder.build();
    }


    public String getPathToTitle() {
        return pathToTitle;
    }

    public String getType() {
        return type;
    }

    public Map<Character, Button> getIcons() {
        return icons;
    }
}
