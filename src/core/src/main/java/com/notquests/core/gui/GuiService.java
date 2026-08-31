package com.notquests.core.gui;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.managers.LanguageManager;
import com.notquests.core.managers.UtilManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class GuiService {
    private static final String INVALID_MATERIAL_FALLBACK = "gray_stained_glass_pane";

    public static final List<String> GUI_NAMES = List.of(
            "main-base",
            "main-take",
            "main-active",
            "category-take",
            "active-quest-abort-confirm",
            "active-quest-info",
            "npc-available-quests",
            "quest-preview");

    private final NotQuestsPlugin plugin;
    private final Map<String, GuiLayout> layouts;
    private final Text text;

    public GuiService(final NotQuestsPlugin plugin) {
        this(plugin, loadBundledDefaults());
    }

    public GuiService(
            final NotQuestsPlugin plugin,
            final Map<String, GuiLayout> layouts) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.layouts = layouts == null ? loadBundledDefaults() : Map.copyOf(layouts);
        this.text = Text.from(plugin.languageManager());
    }

    public static void copyMissingDefaults(final Path dataFolder) throws IOException {
        final Path guiFolder = dataFolder.resolve("guis");
        Files.createDirectories(guiFolder);
        for (final String guiName : GUI_NAMES) {
            copyMissingDefault(guiName, guiFolder);
        }
    }

    public static void copyMissingDefault(final String guiName, final Path guiFolder) throws IOException {
        final Path target = guiFolder.resolve(guiName + ".yml");
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(guiFolder);
        Files.writeString(
                target,
                ConfigurationManager.bundledResourceText("guis/" + guiName + ".yml"),
                StandardCharsets.UTF_8);
    }

    public static Map<String, GuiLayout> loadFromFolder(final Path dataFolder) throws IOException {
        final Path guiFolder = dataFolder.resolve("guis");
        copyMissingDefaults(dataFolder);
        final Map<String, GuiLayout> layouts = new LinkedHashMap<>();
        try (var paths = Files.list(guiFolder)) {
            for (final Path path : paths.filter(path -> path.getFileName().toString().endsWith(".yml")).toList()) {
                final GuiLayout layout = loadLayout(path);
                layouts.put(layout.id(), layout);
            }
        }
        return layouts;
    }

    public static Map<String, GuiLayout> loadBundledDefaults() {
        final Map<String, GuiLayout> layouts = new LinkedHashMap<>();
        for (final String guiName : GUI_NAMES) {
            layouts.put(guiName, loadLayout(
                    guiName,
                    YamlConfig.parse(ConfigurationManager.bundledResourceText("guis/" + guiName + ".yml"))));
        }
        return layouts;
    }

    private static GuiLayout loadLayout(final Path path) throws IOException {
        final String fileName = path.getFileName().toString();
        final String id = fileName.endsWith(".yml") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return loadLayout(id, YamlConfig.load(path));
    }

    private static GuiLayout loadLayout(final String id, final YamlConfig root) {
        final Map<Character, GuiButton> buttons = new HashMap<>();
        final YamlConfig.Section iconsSection = root.getConfigurationSection("icons");
        if (iconsSection != null) {
            for (final String key : iconsSection.getKeys(false)) {
                final YamlConfig.Section buttonSection = iconsSection.getConfigurationSection(key);
                if (buttonSection != null && !key.isEmpty()) {
                    buttons.put(key.charAt(0), parseButton(buttonSection));
                }
            }
        }

        final Map<String, Icon> items = new HashMap<>();
        final YamlConfig.Section itemsSection = root.getConfigurationSection("items");
        if (itemsSection != null) {
            for (final String key : itemsSection.getKeys(false)) {
                final YamlConfig.Section itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    items.put(key, parseIcon(itemSection));
                }
            }
        }

        return new GuiLayout(
                id,
                root.getString("title"),
                root.getString("type", "NORMAL"),
                root.getStringList("structure"),
                buttons,
                items,
                root.getStringList("additionalguis"));
    }

    private static GuiButton parseButton(final YamlConfig.Section section) {
        final String iconType = section.getString("type", "BLANK");
        final Set<IconProperty> properties = new HashSet<>();
        final YamlConfig.Section propertiesSection = section.getConfigurationSection("properties");
        if (propertiesSection != null) {
            for (final String key : propertiesSection.getKeys(false)) {
                final Object value = propertiesSection.get(key);
                if (value instanceof List<?> list) {
                    properties.add(IconProperty.of(
                            key,
                            new IconProperty.ListValue(list.stream().map(Object::toString).toList())));
                } else if (value != null) {
                    properties.add(IconProperty.of(key, new IconProperty.StringValue(value.toString())));
                }
            }
        }
        return new GuiButton(
                ButtonType.valueOf(iconType.toUpperCase(Locale.ROOT)),
                properties,
                section.getStringList("items"));
    }

    private static Icon parseIcon(final YamlConfig.Section section) {
        return new Icon(
                section.getString("material"),
                section.getString("displayname"),
                section.getString("lore"),
                section.getString("skulltexture"));
    }

    public ResolvedGui build(
            final String guiName,
            final PlatformPlayer player,
            final String questIdentifier,
            final String categoryIdentifier) {
        return build(guiName, player, GuiContext.of(questIdentifier, categoryIdentifier));
    }

    public ResolvedGui build(
            final String guiName,
            final PlatformPlayer player,
            final String questIdentifier,
            final String categoryIdentifier,
            final String npcType,
            final NQNPCID npcId) {
        return build(
                guiName,
                player,
                new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId));
    }

    public ResolvedGui build(
            final String guiName,
            final PlatformPlayer player,
            final GuiContext context) {
        final GuiContext guiContext = context == null ? GuiContext.EMPTY : context;
        final String id = guiName == null || guiName.isBlank() ? "main-base" : guiName;
        final GuiLayout layout = layouts.getOrDefault(id, layouts.get("main-base"));
        if (layout == null) {
            return new ResolvedGui("main-base", "NotQuests", 6, List.of());
        }
        return build(layout, player, guiContext);
    }

    /**
     * Applies the core-owned GUI material policy after placeholders and quest/category item choices
     * have been resolved. The platform contributes only its native item-material catalogue.
     */
    public ResolvedGui withValidMaterials(
            final ResolvedGui gui,
            final Predicate<String> itemMaterialExists,
            final Consumer<String> warningSink) {
        if (gui == null) {
            return new ResolvedGui("main-base", "NotQuests", 1, List.of());
        }
        Objects.requireNonNull(itemMaterialExists, "itemMaterialExists");
        final List<GuiSlot> slots = gui.slots().stream()
                .map(slot -> validMaterial(slot, itemMaterialExists, warningSink))
                .toList();
        return new ResolvedGui(gui.id(), gui.title(), gui.rows(), slots);
    }

    private static GuiSlot validMaterial(
            final GuiSlot slot,
            final Predicate<String> itemMaterialExists,
            final Consumer<String> warningSink) {
        if (slot.empty() || itemMaterialExists.test(slot.material())) {
            return slot;
        }
        if (warningSink != null) {
            warningSink.accept("Invalid GUI item material '" + slot.material()
                    + "'. Falling back to GRAY_STAINED_GLASS_PANE for this server version.");
        }
        return new GuiSlot(
                slot.index(),
                INVALID_MATERIAL_FALLBACK,
                slot.skullTexture(),
                slot.displayName(),
                slot.lore(),
                slot.actions());
    }

    private ResolvedGui build(
            final GuiLayout layout,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<GuiSlot> slots = new ArrayList<>();
        final Map<String, String> placeholders = placeholders(
                player,
                context.questIdentifier(),
                context.categoryIdentifier(),
                context.npcType(),
                context.npcId());
        final List<PositionedButton> positionedButtons = positionedButtons(layout);
        final Set<Character> processedPagedSymbols = new HashSet<>();
        for (final PositionedButton positioned : positionedButtons) {
            final GuiButton button = positioned.button();
            if (isPagedContentButton(layout, positioned.symbol(), button)) {
                if (processedPagedSymbols.add(positioned.symbol())) {
                    slots.addAll(pagedSlots(
                            positionedButtons,
                            layout,
                            positioned.symbol(),
                            player,
                            context));
                }
                continue;
            }
            slots.add(slot(
                    layout,
                    positioned.index(),
                    button,
                    placeholders,
                    player,
                    context));
        }
        final String titlePath = tabTitlePath(layout, context.tab());
        return new ResolvedGui(
                layout.id(),
                text.resolve(titlePath, placeholders),
                layout.rows(),
                slots.stream().filter(Objects::nonNull).toList());
    }

    private List<GuiSlot> pagedSlots(
            final List<PositionedButton> positionedButtons,
            final GuiLayout layout,
            final char symbol,
            final PlatformPlayer player,
            final GuiContext context) {
        final GuiButton pagedButton = layout.buttons().get(symbol);
        final List<Integer> indexes = positionedButtons.stream()
                .filter(button -> button.symbol() == symbol)
                .map(PositionedButton::index)
                .toList();
        final List<GuiSlot> content = contentSlots(
                layout,
                pagedButton,
                player,
                context);
        final TabPageControls tabControls = tabPageControls(layout, context);
        final int controlCount = tabControls == null ? 0 : tabControls.count();
        final int pageSize = Math.max(0, indexes.size() - controlCount);
        final int pageCount = pageSize == 0 ? 1 : Math.max(1, (content.size() + pageSize - 1) / pageSize);
        final int page = Math.min(context.page(), pageCount - 1);
        final GuiContext effectiveContext = context.withPage(page);
        final int offset = page * pageSize;
        final List<GuiSlot> slots = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            final int contentIndex = offset + i;
            if (contentIndex < content.size()) {
                final GuiSlot source = content.get(contentIndex);
                slots.add(new GuiSlot(
                        indexes.get(i),
                        source.material(),
                        source.skullTexture(),
                        source.displayName(),
                        source.lore(),
                        source.actions()));
            } else {
                final GuiSlot filler = fillerSlot(layout, indexes.get(i));
                if (filler != null) {
                    slots.add(filler);
                }
            }
        }
        if (tabControls != null) {
            int controlIndex = pageSize;
            if (tabControls.back() != null) {
                slots.add(tabNavigationSlot(
                        layout,
                        tabControls.layout(),
                        tabControls.back(),
                        indexes.get(controlIndex++),
                        player,
                        effectiveContext,
                        page > 0,
                        Math.max(0, page - 1)));
            }
            if (tabControls.forward() != null) {
                slots.add(tabNavigationSlot(
                        layout,
                        tabControls.layout(),
                        tabControls.forward(),
                        indexes.get(controlIndex),
                        player,
                        effectiveContext,
                        page + 1 < pageCount,
                        page + 1));
            }
        }
        return slots;
    }

    private GuiSlot tabNavigationSlot(
            final GuiLayout parentLayout,
            final GuiLayout tabLayout,
            final GuiButton button,
            final int index,
            final PlatformPlayer player,
            final GuiContext context,
            final boolean enabled,
            final int targetPage) {
        final GuiSlot rendered = slot(
                tabLayout,
                index,
                button,
                placeholders(
                        player,
                        context.questIdentifier(),
                        context.categoryIdentifier(),
                        context.npcType(),
                        context.npcId()),
                player,
                context);
        if (rendered == null) {
            return fillerSlot(parentLayout, index);
        }
        final List<String> conditions = propertyLines(button, "conditions").stream()
                .map(condition -> resolveGuiLine(condition, Map.of(), player))
                .filter(condition -> !condition.isBlank())
                .toList();
        final List<GuiAction> actions = enabled
                ? List.of(GuiAction.openGui(parentLayout.id(), context.withPage(targetPage))
                        .withConditions(conditions))
                : List.of();
        return new GuiSlot(
                index,
                rendered.material(),
                rendered.skullTexture(),
                rendered.displayName(),
                rendered.lore(),
                actions);
    }

    private List<GuiSlot> contentSlots(
            final GuiLayout layout,
            final GuiButton pagedButton,
            final PlatformPlayer player,
            final GuiContext context) {
        if ("TAB".equals(layout.type()) && pagedButton != null && pagedButton.type() == ButtonType.PAGED) {
            return tabContentSlots(layout, player, context);
        }
        final SpecialIconType special = specialIconType(pagedButton);
        return switch (special) {
            case PLAYER_ACTIVE_QUEST -> activeQuestSlots(layout, pagedButton, player, context);
            case CATEGORY -> categorySlots(layout, pagedButton, player, context);
            case CATEGORY_AVAILABLE_QUEST, CATEGORY_PLAYER_AVAILABLE_QUEST -> questSlots(
                    layout,
                    pagedButton,
                    (special == SpecialIconType.CATEGORY_PLAYER_AVAILABLE_QUEST
                                    ? visibleQuests(player, plugin.quests())
                                    : plugin.quests())
                            .stream()
                            .filter(quest -> sameCategory(quest.getCategory(), context.categoryIdentifier()))
                            .toList(),
                    player,
                    context);
            case NPC_SHOWN_QUEST, PLAYER_AVAILABLE_QUEST, DEFAULT -> questSlots(
                    layout,
                    pagedButton,
                    special == SpecialIconType.NPC_SHOWN_QUEST
                            ? visibleQuests(player, plugin.quests()).stream()
                                    .filter(quest -> hasShownNpcAttachment(quest, context.npcType(), context.npcId()))
                                    .toList()
                            : special == SpecialIconType.PLAYER_AVAILABLE_QUEST
                                    ? visibleQuests(player, plugin.quests())
                                    : plugin.quests(),
                    player,
                    context);
        };
    }

    private List<Quest> visibleQuests(
            final PlatformPlayer player,
            final List<Quest> quests) {
        final List<String> visibleIdentifiers = plugin.visibleQuestIdentifiers(
                player,
                quests,
                System.currentTimeMillis(),
                ignored -> {});
        return quests.stream()
                .filter(quest -> visibleIdentifiers.stream()
                        .anyMatch(identifier -> identifier.equalsIgnoreCase(quest.getIdentifier())))
                .toList();
    }

    private List<GuiSlot> tabContentSlots(
            final GuiLayout layout,
            final PlatformPlayer player,
            final GuiContext context) {
        final GuiLayout activeTab = activeTabLayout(layout, context);
        if (activeTab == null) {
            return List.of();
        }
        final GuiButton contentButton = activeTab.buttons().get('X');
        if (contentButton == null) {
            return List.of();
        }
        return contentSlots(activeTab, contentButton, player, context);
    }

    private GuiLayout activeTabLayout(final GuiLayout layout, final GuiContext context) {
        if (layout == null || layout.additionalGuis().isEmpty()) {
            return null;
        }
        final int tab = Math.min(Math.max(0, context.tab()), layout.additionalGuis().size() - 1);
        return layouts.get(layout.additionalGuis().get(tab));
    }

    private TabPageControls tabPageControls(final GuiLayout layout, final GuiContext context) {
        if (!"TAB".equals(layout.type())) {
            return null;
        }
        final GuiLayout activeTab = activeTabLayout(layout, context);
        if (activeTab == null) {
            return null;
        }
        GuiButton back = null;
        GuiButton forward = null;
        for (final GuiButton button : activeTab.buttons().values()) {
            if (button.type() == ButtonType.BACK) {
                back = button;
            } else if (button.type() == ButtonType.FORWARD) {
                forward = button;
            }
        }
        return back == null && forward == null ? null : new TabPageControls(activeTab, back, forward);
    }

    private List<GuiSlot> activeQuestSlots(
            final GuiLayout layout,
            final GuiButton button,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<Quest> quests = questPlayer(player).getActiveQuestIdentifiers().stream()
                .map(plugin::quest)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Quest::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return questSlots(layout, button, quests, player, context);
    }

    private List<GuiSlot> questSlots(
            final GuiLayout layout,
            final GuiButton button,
            final List<Quest> quests,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<GuiSlot> slots = new ArrayList<>();
        for (final Quest quest : quests.stream()
                .sorted(Comparator.comparing(Quest::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList()) {
            final Map<String, String> placeholders = placeholders(
                    player,
                    quest.getIdentifier(),
                    context.categoryIdentifier(),
                    context.npcType(),
                    context.npcId());
            slots.add(slot(
                    layout,
                    -1,
                    button,
                    placeholders,
                    player,
                    context.withQuest(quest.getIdentifier())));
        }
        return slots;
    }

    private List<GuiSlot> categorySlots(
            final GuiLayout layout,
            final GuiButton button,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<GuiSlot> slots = new ArrayList<>();
        final List<Category> categories = new ArrayList<>(plugin.categories());
        if (categories.isEmpty()) {
            categories.add(plugin.getOrCreateCategory(Category.DEFAULT_NAME));
        }
        for (final Category category : categories.stream()
                .sorted(Comparator.comparing(Category::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList()) {
            final GuiContext categoryContext = context.withCategory(category.getIdentifier()).withQuest("");
            final Map<String, String> placeholders = placeholders(
                    player,
                    "",
                    category.getIdentifier(),
                    context.npcType(),
                    context.npcId());
            slots.add(slot(layout, -1, button, placeholders, player, categoryContext));
        }
        return slots;
    }

    private GuiSlot fillerSlot(final GuiLayout layout, final int index) {
        for (final GuiButton button : layout.buttons().values()) {
            if (specialIconType(button) == SpecialIconType.DEFAULT && button.itemKeys().contains("emptyspacefiller")) {
                return slot(layout, index, button, Map.of(), null, GuiContext.EMPTY);
            }
        }
        return null;
    }

    private GuiSlot slot(
            final GuiLayout layout,
            final int index,
            final GuiButton button,
            final Map<String, String> placeholders,
            final PlatformPlayer player,
            final GuiContext context) {
        final Icon icon = icon(layout, button, context);
        if (icon == null) {
            return null;
        }
        final String material = material(icon, context.questIdentifier(), context.categoryIdentifier());
        final String displayName = text.resolve(icon.pathToDisplayName(), placeholders);
        final List<String> lore = expandLore(
                text.resolveList(icon.pathToLore(), placeholders),
                player,
                context);
        return new GuiSlot(
                index,
                material,
                icon.skullTexture(),
                displayName,
                lore,
                actions(layout, button, placeholders, player, context));
    }

    private Icon icon(final GuiLayout layout, final GuiButton button, final GuiContext context) {
        List<String> itemKeys = button.itemKeys();
        if (button.type() == ButtonType.TAB && itemKeys.size() > 1) {
            itemKeys = List.of(itemKeys.get(tabIndex(button) == Math.max(0, context.tab()) ? 0 : 1));
        }
        for (final String itemKey : itemKeys) {
            final Icon icon = layout.items().get(itemKey);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    private String material(final Icon icon, final String questIdentifier, final String categoryIdentifier) {
        if ("%QUEST_ITEM_MATERIAL%".equals(icon.material())) {
            final Quest quest = plugin.quest(questIdentifier);
            return firstMaterial(quest == null ? "" : quest.getGuiItem(), "book");
        }
        if ("%CATEGORY_ITEM_MATERIAL%".equals(icon.material())) {
            final Category category = plugin.category(categoryIdentifier);
            return firstMaterial(category == null ? "" : category.getGuiItem(), "chest");
        }
        return icon.material();
    }

    private static String firstMaterial(final String materialList, final String fallback) {
        if (materialList == null || materialList.isBlank()) {
            return fallback;
        }
        for (final String material : materialList.split(",")) {
            if (material != null && !material.isBlank()) {
                return material.trim();
            }
        }
        return fallback;
    }

    private List<String> expandLore(
            final List<String> source,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<String> lore = new ArrayList<>();
        final Quest quest = plugin.quest(context.questIdentifier());
        for (final String line : source == null ? List.<String>of() : source) {
            if (line.contains("%QUESTREWARDS%")) {
                if (quest != null && player != null) {
                    lore.add(line.replace("%QUESTREWARDS%", ""));
                    lore.addAll(plugin.questRewardsList(player, quest.getIdentifier()));
                }
                continue;
            }
            if (line.contains("%QUESTREQUIREMENTS%")) {
                if (quest != null && player != null) {
                    lore.add(line.replace("%QUESTREQUIREMENTS%", ""));
                    lore.addAll(plugin.questRequirementsList(player, quest.getIdentifier()));
                }
                continue;
            }
            if (line.contains("%WRAPPEDQUESTDESCRIPTION%")) {
                if (quest != null) {
                    lore.add(line.replace("%WRAPPEDQUESTDESCRIPTION%", ""));
                    lore.addAll(UtilManager.wrapToList(
                            quest.getDescription(),
                            plugin.configuration().questDescriptionMaxLineLength(),
                            plugin.configuration().wrapLongWords()));
                }
                continue;
            }
            lore.add(line);
        }
        return lore.stream().map(line -> resolveGuiLine(line, Map.of(), player)).toList();
    }

    private boolean hasNextPage(
            final GuiLayout layout,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<PositionedButton> positioned = positionedButtons(layout);
        for (final PositionedButton candidate : positioned) {
            if (!isPagedContentButton(layout, candidate.symbol(), candidate.button())) {
                continue;
            }
            final int pageSize = (int) positioned.stream()
                    .filter(button -> button.symbol() == candidate.symbol())
                    .count();
            final TabPageControls tabControls = tabPageControls(layout, context);
            final int contentPageSize = pageSize - (tabControls == null ? 0 : tabControls.count());
            if (contentPageSize <= 0) {
                return false;
            }
            return contentSlots(layout, candidate.button(), player, context).size()
                    > (context.page() + 1) * contentPageSize;
        }
        return false;
    }

    private List<GuiAction> actions(
            final GuiLayout layout,
            final GuiButton button,
            final Map<String, String> placeholders,
            final PlatformPlayer player,
            final GuiContext context) {
        final List<GuiAction> parsed = new ArrayList<>();
        final List<String> conditions = propertyLines(button, "conditions").stream()
                .map(condition -> resolveGuiLine(condition, placeholders, player))
                .filter(condition -> !condition.isBlank())
                .toList();
        if (button.type() == ButtonType.TAB) {
            final int tab = tabIndex(button);
            if (tab >= 0 && tab < layout.additionalGuis().size()
                    && tab != Math.max(0, context.tab())) {
                parsed.add(GuiAction.openGui(layout.id(), context.withTab(tab)).withConditions(conditions));
            }
        }
        if (button.type() == ButtonType.BACK && context.page() > 0) {
            parsed.add(GuiAction.openGui(layout.id(), context.withPage(context.page() - 1))
                    .withConditions(conditions));
        }
        if (button.type() == ButtonType.FORWARD && hasNextPage(layout, player, context)) {
            parsed.add(GuiAction.openGui(layout.id(), context.withPage(context.page() + 1))
                    .withConditions(conditions));
        }
        for (final String action : propertyLines(button, "actions")) {
            parsed.addAll(parseAction(
                    resolveGuiLine(action, placeholders, player),
                    context,
                    conditions));
        }
        return List.copyOf(parsed);
    }

    private static List<String> propertyLines(final GuiButton button, final String propertyName) {
        final IconProperty property = button.property(propertyName);
        if (property == null || !(property.getValue() instanceof IconProperty.ListValue list)) {
            return List.of();
        }
        return list.value();
    }

    private String resolveGuiLine(
            final String line,
            final Map<String, String> placeholders,
            final PlatformPlayer player) {
        String resolved = line == null ? "" : line;
        for (final Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            resolved = resolved.replace(placeholder.getKey(), placeholder.getValue());
        }
        return plugin.resolvePlaceholders(player, resolved);
    }

    private List<GuiAction> parseAction(
            final String action,
            final GuiContext context,
            final List<String> conditions) {
        if (action == null || action.isBlank()) {
            return List.of();
        }
        final String replaced = action
                .replace("%QUESTID%", context.questIdentifier())
                .replace("%CATEGORYID%", context.categoryIdentifier());
        final String[] parts = replaced.strip().split("\\s+");
        if (parts.length == 0) {
            return List.of();
        }
        return switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "opengui" -> List.of(openGuiAction(parts, context).withConditions(conditions));
            case "givequest" -> List.of(GuiAction.takeQuest(
                    parts.length > 1 ? parts[1] : context.questIdentifier()).withConditions(conditions));
            case "failquest" -> List.of(GuiAction.failQuest(
                    parts.length > 1 ? parts[1] : context.questIdentifier()).withConditions(conditions));
            case "closeinventory" -> List.of(GuiAction.close().withConditions(conditions));
            default -> List.of(GuiAction.registryAction(replaced, conditions));
        };
    }

    private GuiAction openGuiAction(final String[] parts, final GuiContext currentContext) {
        final String quest = flagValue(parts, "--quest");
        final String category = flagValue(parts, "--category");
        final String rawNpc = flagValue(parts, "--npc");
        final String npcType = flagValue(parts, "--npctype");
        final String resolvedNpcType = npcType.isBlank() ? currentContext.npcType() : npcType;
        final String targetPlayer = firstPresent(
                flagValue(parts, "--targetplayer"),
                flagValue(parts, "--player"));
        final GuiContext actionContext = new GuiContext(
                quest,
                category,
                resolvedNpcType,
                rawNpc.isBlank() ? currentContext.npcId() : npcId(resolvedNpcType, rawNpc),
                0,
                -1);
        return GuiAction.openGui(
                parts.length > 1 ? parts[1] : "main-base",
                actionContext).forPlayer(targetPlayer);
    }

    private static String firstPresent(final String first, final String second) {
        return first == null || first.isBlank() ? clean(second) : first;
    }

    private static NQNPCID npcId(final String npcType, final String value) {
        if (npcType != null && !npcType.isBlank()) {
            final var parsed = NpcAttachments.Selector.parse(npcType + ":" + value);
            if (parsed.isPresent()) {
                return parsed.orElseThrow().id();
            }
        }
        try {
            return NQNPCID.fromInteger(Integer.parseInt(value));
        } catch (final NumberFormatException ignored) {
            try {
                return NQNPCID.fromUUID(UUID.fromString(value));
            } catch (final IllegalArgumentException ignoredUuid) {
                return NQNPCID.fromString(value);
            }
        }
    }

    private static String flagValue(final String[] parts, final String flag) {
        for (int i = 0; i < parts.length - 1; i++) {
            if (flag.equalsIgnoreCase(parts[i])) {
                return parts[i + 1];
            }
        }
        return "";
    }

    private static int tabIndex(final GuiButton button) {
        final IconProperty tabIndex = button.property("tabindex");
        if (tabIndex == null || !(tabIndex.getValue() instanceof IconProperty.StringValue value)) {
            return -1;
        }
        try {
            return Integer.parseInt(value.value());
        } catch (final NumberFormatException exception) {
            return -1;
        }
    }

    private static String tabTitlePath(final GuiLayout layout, final int selectedTab) {
        if (selectedTab < 0) {
            return layout.titlePath();
        }
        for (final GuiButton button : layout.buttons().values()) {
            if (button.type() != ButtonType.TAB || tabIndex(button) != selectedTab) {
                continue;
            }
            final IconProperty title = button.property("tabtitle");
            if (title != null && title.getValue() instanceof IconProperty.StringValue value
                    && !value.value().isBlank()) {
                return value.value();
            }
        }
        return layout.titlePath();
    }

    private Map<String, String> placeholders(
            final PlatformPlayer player,
            final String questIdentifier,
            final String categoryIdentifier,
            final String npcType,
            final NQNPCID npcId) {
        final Map<String, String> placeholders = new HashMap<>();
        final Quest quest = plugin.quest(questIdentifier);
        final Category category = plugin.category(categoryIdentifier);
        placeholders.put("%QUESTID%", clean(questIdentifier));
        placeholders.put("%QUESTNAME%", quest == null ? clean(questIdentifier) : quest.getDisplayNameOrIdentifier());
        placeholders.put("%QUESTDESCRIPTION%", quest == null ? "" : quest.getDescription());
        placeholders.put("%CATEGORYID%", clean(categoryIdentifier));
        placeholders.put("%CATEGORYNAME%", category == null
                ? clean(categoryIdentifier)
                : (category.getDisplayName().isBlank() ? category.getIdentifier() : category.getDisplayName()));
        placeholders.put("%NPCTYPE%", clean(npcType));
        placeholders.put("%NPCID%", npcId == null ? "" : npcId.getEitherAsString());
        placeholders.put("%QUESTPOINTS%", String.valueOf(questPlayer(player).getQuestPoints()));
        placeholders.put("%player_name%", player == null
                ? ""
                : (!player.playerName().isBlank() ? player.playerName() : player.playerIdentifier()));
        return placeholders;
    }

    private static boolean isPagedContentButton(
            final GuiLayout layout,
            final char symbol,
            final GuiButton button) {
        if (button.type() == ButtonType.PAGED) {
            return true;
        }
        return "PAGED_ITEMS".equals(layout.type()) && symbol == 'X';
    }

    private QuestPlayer questPlayer(final PlatformPlayer player) {
        final String id = player == null || !player.hasPlayer() || player.playerIdentifier().isBlank()
                ? "unknown"
                : player.playerIdentifier();
        return plugin.activeQuestPlayer(id);
    }

    private List<PositionedButton> positionedButtons(final GuiLayout layout) {
        final List<PositionedButton> buttons = new ArrayList<>();
        for (int row = 0; row < layout.structure().size(); row++) {
            final String compact = layout.structure().get(row).replace(" ", "");
            for (int column = 0; column < Math.min(9, compact.length()); column++) {
                final char symbol = compact.charAt(column);
                final GuiButton button = layout.buttons().get(symbol);
                if (button != null) {
                    buttons.add(new PositionedButton(row * 9 + column, symbol, button));
                }
            }
        }
        return buttons;
    }

    private static SpecialIconType specialIconType(final GuiButton button) {
        if (button == null) {
            return SpecialIconType.DEFAULT;
        }
        final IconProperty property = button.property("specialicontype");
        if (property != null && property.getValue() instanceof IconProperty.StringValue value) {
            try {
                return SpecialIconType.valueOf(value.value().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ignored) {
                return SpecialIconType.DEFAULT;
            }
        }
        return SpecialIconType.DEFAULT;
    }

    private static boolean sameCategory(final String questCategory, final String selectedCategory) {
        final String quest = canonicalCategory(questCategory);
        final String selected = canonicalCategory(selectedCategory);
        return quest.equalsIgnoreCase(selected);
    }

    private static boolean hasShownNpcAttachment(
            final Quest quest,
            final String npcType,
            final NQNPCID npcId) {
        if (quest == null || npcId == null) {
            return false;
        }
        for (final NpcAttachment attachment : quest.getNpcAttachments()) {
            if (!attachment.questShowing() || attachment.npcId() == null) {
                continue;
            }
            if (!attachment.npcId().equals(npcId)) {
                continue;
            }
            if (npcType == null || npcType.isBlank() || attachment.npcType().isBlank()) {
                return true;
            }
            if (attachment.npcType().equalsIgnoreCase(npcType)) {
                return true;
            }
        }
        return false;
    }

    private static String canonicalCategory(final String category) {
        return category == null || category.isBlank() ? Category.DEFAULT_NAME : category;
    }

    private static String clean(final String value) {
        return value == null ? "" : value;
    }

    public record ResolvedGui(String id, String title, int rows, List<GuiSlot> slots) {
        public ResolvedGui {
            id = id == null || id.isBlank() ? "main-base" : id;
            title = title == null || title.isBlank() ? "NotQuests" : title;
            rows = Math.max(1, Math.min(6, rows));
            slots = slots == null ? List.of() : List.copyOf(slots);
        }

        public int size() {
            return rows * 9;
        }

        public GuiSlot slot(final int index) {
            for (final GuiSlot slot : slots) {
                if (slot.index() == index) {
                    return slot;
                }
            }
            return null;
        }
    }

    public record GuiSlot(
            int index,
            String material,
            String skullTexture,
            String displayName,
            List<String> lore,
            List<GuiAction> actions) {

        public GuiSlot {
            material = material == null || material.isBlank() ? "gray_stained_glass_pane" : material;
            skullTexture = skullTexture == null ? "" : skullTexture;
            displayName = displayName == null ? "" : displayName;
            lore = lore == null ? List.of() : List.copyOf(lore);
            actions = actions == null ? List.of() : List.copyOf(actions);
        }

        public boolean clickable() {
            return !actions.isEmpty();
        }

        public boolean empty() {
            return material.equalsIgnoreCase("air")
                    || material.equalsIgnoreCase("minecraft:air");
        }
    }

    public record GuiAction(
            Type type,
            String target,
            GuiContext context,
            String playerName,
            List<String> conditions) {
        public enum Type {
            OPEN_GUI,
            TAKE_QUEST,
            FAIL_QUEST,
            CLOSE,
            REGISTRY_ACTION
        }

        public GuiAction {
            type = type == null ? Type.REGISTRY_ACTION : type;
            target = clean(target);
            context = context == null ? GuiContext.EMPTY : context;
            playerName = clean(playerName);
            conditions = conditions == null ? List.of() : List.copyOf(conditions);
        }

        public static GuiAction openGui(
                final String target,
                final String questIdentifier,
                final String categoryIdentifier) {
            return openGui(target, GuiContext.of(questIdentifier, categoryIdentifier));
        }

        public static GuiAction openGui(final String target, final GuiContext context) {
            return new GuiAction(Type.OPEN_GUI, target, context, "", List.of());
        }

        public static GuiAction takeQuest(final String questIdentifier) {
            return new GuiAction(
                    Type.TAKE_QUEST,
                    "",
                    GuiContext.of(questIdentifier, ""),
                    "",
                    List.of());
        }

        public static GuiAction failQuest(final String questIdentifier) {
            return new GuiAction(
                    Type.FAIL_QUEST,
                    "",
                    GuiContext.of(questIdentifier, ""),
                    "",
                    List.of());
        }

        public static GuiAction close() {
            return new GuiAction(Type.CLOSE, "", GuiContext.EMPTY, "", List.of());
        }

        public static GuiAction registryAction(final String actionLine, final List<String> conditions) {
            return new GuiAction(Type.REGISTRY_ACTION, actionLine, GuiContext.EMPTY, "", conditions);
        }

        public String questIdentifier() {
            return context.questIdentifier();
        }

        public String categoryIdentifier() {
            return context.categoryIdentifier();
        }

        public GuiAction forPlayer(final String playerName) {
            return new GuiAction(type, target, context, playerName, conditions);
        }

        public GuiAction withConditions(final List<String> conditions) {
            return new GuiAction(type, target, context, playerName, conditions);
        }
    }

    public record GuiLayout(
            String id,
            String titlePath,
            String type,
            List<String> structure,
            Map<Character, GuiButton> buttons,
            Map<String, Icon> items,
            List<String> additionalGuis) {
        public GuiLayout {
            id = clean(id, "main-base");
            titlePath = clean(titlePath, "NotQuests");
            type = clean(type, "NORMAL").toUpperCase(Locale.ROOT);
            structure = structure == null ? List.of() : List.copyOf(structure);
            buttons = buttons == null ? Map.of() : Map.copyOf(buttons);
            items = items == null ? Map.of() : Map.copyOf(items);
            additionalGuis = additionalGuis == null ? List.of() : List.copyOf(additionalGuis);
        }

        public int rows() {
            return Math.max(1, Math.min(6, structure.size()));
        }

        private static String clean(final String value, final String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record GuiButton(ButtonType type, Set<IconProperty> properties, List<String> itemKeys) {
        public GuiButton {
            type = type == null ? ButtonType.BLANK : type;
            properties = properties == null ? Set.of() : Set.copyOf(properties);
            itemKeys = itemKeys == null || itemKeys.isEmpty() ? List.of("") : List.copyOf(itemKeys);
        }

        private IconProperty property(final String key) {
            if (key == null || key.isBlank()) {
                return null;
            }
            for (final IconProperty property : properties) {
                if (key.equalsIgnoreCase(property.key())) {
                    return property;
                }
            }
            return null;
        }
    }

    public record Icon(String material, String pathToDisplayName, String pathToLore, String skullTexture) {}

    public enum ButtonType {
        BLANK,
        PAGED,
        TAB,
        ACTION,
        BACK,
        UP,
        DOWN,
        FORWARD
    }

    public enum SpecialIconType {
        DEFAULT,
        PLAYER_ACTIVE_QUEST,
        PLAYER_AVAILABLE_QUEST,
        CATEGORY,
        CATEGORY_AVAILABLE_QUEST,
        CATEGORY_PLAYER_AVAILABLE_QUEST,
        NPC_SHOWN_QUEST
    }

    public record IconProperty(String key, Value value) {
        private static IconProperty of(final String key, final Value value) {
            return new IconProperty(key, value);
        }

        private Value getValue() {
            return value;
        }

        public interface Value {}

        public record ListValue(List<String> value) implements Value {}

        public record StringValue(String value) implements Value {}
    }

    private interface Text {
        String resolve(String path, Map<String, String> placeholders);

        default List<String> resolveList(final String path, final Map<String, String> placeholders) {
            return List.of(resolve(path, placeholders));
        }

        static Text from(final LanguageManager translations) {
            return new Text() {
                @Override
                public String resolve(final String path, final Map<String, String> placeholders) {
                    if (path == null || path.isBlank() || "EMPTY".equals(path)) {
                        return "";
                    }
                    final String translated = translations == null ? null : translations.string(path);
                    return LanguageManager.apply(translated == null ? path : translated, placeholders);
                }

                @Override
                public List<String> resolveList(final String path, final Map<String, String> placeholders) {
                    if (path == null || path.isBlank()) {
                        return List.of();
                    }
                    if ("EMPTY".equals(path)) {
                        return List.of("");
                    }
                    final List<String> lines = translations == null ? List.of() : translations.stringList(path);
                    if (!lines.isEmpty()) {
                        return lines.stream()
                                .map(line -> LanguageManager.apply(line, placeholders))
                                .toList();
                    }
                    final String translated = translations == null ? null : translations.string(path);
                    return List.of(LanguageManager.apply(translated == null ? path : translated, placeholders));
                }
            };
        }
    }

    private record PositionedButton(int index, char symbol, GuiButton button) {}

    private record TabPageControls(GuiLayout layout, GuiButton back, GuiButton forward) {
        private int count() {
            return (back == null ? 0 : 1) + (forward == null ? 0 : 1);
        }
    }
}
