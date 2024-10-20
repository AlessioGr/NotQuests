package rocks.gravili.notquests.paper.gui;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.gui.icon.Button;
import rocks.gravili.notquests.paper.gui.icon.ButtonType;
import rocks.gravili.notquests.paper.gui.icon.SpecialIconType;
import rocks.gravili.notquests.paper.gui.property.types.StringIconProperty;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomGui {
    private final String pathToTitle;
    private final String type;
    private String[] structure;
    private final Map<Character, Button> icons;
    private final List<String> additionalGuis;
    private NotQuests notQuests;

    private Gui gui;

    public CustomGui(String[] structure, String pathToTitle, String type, Map<Character, Button> icons, List<String> additionalGuis) {
        this.pathToTitle = pathToTitle;
        this.structure = structure;
        this.type = type;
        this.icons = icons;
        this.additionalGuis = additionalGuis;
    }

    public Gui buildGui(NotQuests notQuests, GuiContext guiContext) {
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
        if (icon.getIconProperty("specialicontype").isPresent()) {
            if (icon.getIconProperty("specialicontype").get().getValue() instanceof StringIconProperty stringIconProperty) {
                return SpecialIconType.valueOf(stringIconProperty.value());
            }
        }
        return SpecialIconType.DEFAULT;
    }

    private Gui handleNormalGui(GuiContext guiContext) {
        var guiBuilder = Gui.normal().setStructure(structure);

        icons.forEach((key, icon) -> guiBuilder.addIngredient(key, icon.buildItem(notQuests, guiContext)));
        for (Object o : guiContext.getAsObjectArray()) {
            notQuests.getLogManager().debug(pathToTitle + o);
        }

        return guiBuilder.build();
    }
    private Gui handlePagedItemsGui(GuiContext guiContext) {
        var structureObject = new Structure(structure);
        var targetQuestPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(guiContext.getPlayer().getUniqueId());

        var guiBuilder = PagedGui.items().setStructure(structure);

        //X and Y are reserved characters!
        var pagedIcon = icons.get('X');
        var fillerIcon = icons.get('Y');

        if (pagedIcon == null) {
            notQuests.getLogManager().debug("Paged icon null");
            return guiBuilder.build();
        }


        var pagedIconSpecialType = fetchSpecialIconType(pagedIcon);

        guiBuilder.addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL);
        structureObject.addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL);


        var items = new ArrayList<Item>();
        var guiSize = structureObject.getIngredientList().findContentListSlots().length;
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
                    numberOfTotalItems = guiSize * ((npcQuests.size() / guiSize) + 1);
                    npcQuests.forEach(quest -> {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setQuest(quest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    });
                } else if (guiContext.getArmorStand() != null) {
                    var armorStand = guiContext.getArmorStand();
                    var armorStandQuests = notQuests.getQuestManager().getQuestsFromListWithVisibilityEvaluations(targetQuestPlayer, notQuests.getQuestManager().getQuestsAttachedToArmorstandWithShowing(armorStand));
                    armorStandQuests.forEach(quest -> {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setQuest(quest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    });
                }
            }

            case CATEGORY_AVAILABLE_QUEST -> {
                if (guiContext.getCategory() != null) {
                    var category = guiContext.getCategory();
                    var categoryQuests = category.getQuests();
                    numberOfTotalItems = guiSize * ((categoryQuests.size() / guiSize) + 1);
                    categoryQuests.forEach(quest -> {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setQuest(quest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    });
                }
            }

            case CATEGORY_PLAYER_AVAILABLE_QUEST -> {
                if (guiContext.getCategory() != null) {
                    var category = guiContext.getCategory();
                    var categoryQuests = notQuests.getQuestManager().getAllQuestsWithVisibilityEvaluations(targetQuestPlayer)
                            .stream()
                            .filter(quest -> quest.getCategory().getCategoryFullName().equals(category.getCategoryFullName()))
                            .collect(Collectors.toSet());
                    numberOfTotalItems = guiSize * ((categoryQuests.size() / guiSize) + 1);
                    categoryQuests.forEach(quest -> {
                        var itemGuiContext = guiContext.clone();
                        itemGuiContext.setQuest(quest);
                        items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                    });
                }
            }

            case CATEGORY -> {
                var categories = notQuests.getDataManager().getCategories();
                numberOfTotalItems = guiSize * ((categories.size() / guiSize) + 1);
                categories.forEach(category -> {
                    var itemGuiContext = guiContext.clone();
                    itemGuiContext.setCategory(category);
                    notQuests.getLogManager().info("category: " + category);
                    items.add(pagedIcon.buildItem(notQuests, itemGuiContext));
                });
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


        guiBuilder.setContent(items);

        return guiBuilder.build();
    }

    /*
    private GUI handlePagedGuisGui(GuiObjectContext guiObjectContext) {}
    private GUI handleScrollItemsGui(GuiObjectContext guiObjectContext){}
    private GUI handleScrollGuisGui(GuiObjectContext guiObjectContext) {}

     */
    private Gui handleTabGui(GuiContext guiContext) {
        var tabGuis = new ArrayList<Gui>();

        additionalGuis.forEach(string -> {
            var tabGui = notQuests.getGuiService().getGuis().get(string);
            if (tabGui == null) {
                return;
            }

            tabGuis.add(tabGui.buildGui(notQuests, guiContext));
        });

        var guiBuilder = TabGui.normal()
                .setTabs(tabGuis)
                .setStructure(structure)
                .addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL);

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
