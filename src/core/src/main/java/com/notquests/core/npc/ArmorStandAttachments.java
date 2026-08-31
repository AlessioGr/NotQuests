package com.notquests.core.npc;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.List;

public final class ArmorStandAttachments {
    private static final char SEPARATOR = '°';
    public static final int QUEST_ADD_SHOWING = 0;
    public static final int QUEST_ADD_HIDDEN = 1;
    public static final int QUEST_REMOVE_SHOWING = 2;
    public static final int QUEST_REMOVE_HIDDEN = 3;
    public static final int QUEST_CHECK = 4;
    public static final int OBJECTIVE_COMPLETION_NPC = 6;
    public static final int CONVERSATION_ADD = 8;
    public static final int CONVERSATION_REMOVE = 9;

    private ArmorStandAttachments() {}

    public enum QuestTool {
        CHECK,
        ADD,
        REMOVE
    }

    public enum Operation {
        ADD_QUEST,
        REMOVE_QUEST,
        CHECK_QUESTS,
        SET_OBJECTIVE_COMPLETION_NPC,
        ADD_CONVERSATION,
        REMOVE_CONVERSATION,
        UNKNOWN
    }

    public static Operation operation(final int itemId) {
        if (isQuestAttachTool(itemId)) {
            return Operation.ADD_QUEST;
        }
        if (isQuestRemoveTool(itemId)) {
            return Operation.REMOVE_QUEST;
        }
        return switch (itemId) {
            case QUEST_CHECK -> Operation.CHECK_QUESTS;
            case OBJECTIVE_COMPLETION_NPC -> Operation.SET_OBJECTIVE_COMPLETION_NPC;
            case CONVERSATION_ADD -> Operation.ADD_CONVERSATION;
            case CONVERSATION_REMOVE -> Operation.REMOVE_CONVERSATION;
            default -> Operation.UNKNOWN;
        };
    }

    public static boolean contains(final String storedQuests, final String questName) {
        if (storedQuests == null || storedQuests.isBlank() || questName == null || questName.isBlank()) {
            return false;
        }
        return storedQuests.equals(questName) || storedQuests.contains(token(questName));
    }

    public static String add(final String storedQuests, final String questName) {
        if (questName == null || questName.isBlank()) {
            return normalize(storedQuests);
        }
        final String current = normalize(storedQuests);
        if (contains(current, questName)) {
            return current;
        }
        return current + questName + SEPARATOR;
    }

    public static Removal remove(final String storedQuests, final String questName) {
        if (!contains(storedQuests, questName)) {
            return new Removal(false, false, storedQuests == null ? "" : storedQuests);
        }
        final String next = storedQuests.replace(token(questName), String.valueOf(SEPARATOR));
        final boolean empty = onlySeparators(next);
        return new Removal(true, empty, empty ? "" : next);
    }

    public static QuestAttachmentUpdate addQuest(
            final String storedQuests,
            final String questName,
            final boolean showing) {
        if (contains(storedQuests, questName)) {
            return new QuestAttachmentUpdate(
                    false,
                    false,
                    storedQuests == null ? "" : storedQuests,
                    "<RED>Error: That armor stand already has the Quest <highlight>" + questName
                            + "</highlight> attached to it!");
        }
        final String next = add(storedQuests, questName);
        return new QuestAttachmentUpdate(
                true,
                false,
                next,
                "<DARK_GREEN>Quest with the name <highlight>" + questName
                        + "</highlight> was added to this armor stand"
                        + (showing ? " (showing)" : " (non-showing)") + "!\n"
                        + "<DARK_GREEN>Attached Quests: <highlight>" + next);
    }

    public static QuestAttachmentUpdate removeQuest(final String storedQuests, final String questName) {
        if (storedQuests == null || storedQuests.isBlank()) {
            return new QuestAttachmentUpdate(
                    false,
                    false,
                    "",
                    "<RED>This armor stand has no quests attached to it!");
        }
        final Removal removal = remove(storedQuests, questName);
        if (!removal.removed()) {
            return new QuestAttachmentUpdate(
                    false,
                    false,
                    removal.storedQuests(),
                    "<RED>Error: That armor stand does not have the Quest <highlight>" + questName
                            + "</highlight> attached to it!\n"
                            + "<DARK_GREEN>Attached Quests: <highlight>" + removal.storedQuests());
        }
        return new QuestAttachmentUpdate(
                true,
                removal.empty(),
                removal.storedQuests(),
                "<DARK_GREEN>Quest with the name <highlight>" + questName
                        + "</highlight> was removed from this armor stand!\n"
                        + "<DARK_GREEN>Attached Quests: <highlight>" + removal.storedQuests());
    }

    public static List<String> checkMessages(
            final String armorStandId,
            final boolean hasShowingStorage,
            final String showingQuests,
            final boolean hasNonShowingStorage,
            final String nonShowingQuests) {
        final ArrayList<String> messages = new ArrayList<>();
        messages.add("<GRAY>Armor Stand Entity ID: <WHITE>" + armorStandId);
        addCheckSection(messages, "showing", hasShowingStorage, questNames(showingQuests));
        addCheckSection(messages, "non-showing", hasNonShowingStorage, questNames(nonShowingQuests));
        return List.copyOf(messages);
    }

    public static String questAttachedToNpc(final String questName, final String npcLabel) {
        return "<success>Quest <highlight>" + clean(questName)
                + "</highlight> attached to <highlight2>" + clean(npcLabel) + "</highlight2>.";
    }

    public static String questRemovedFromNpc(final String questName, final String npcLabel) {
        return "<success>Quest <highlight>" + clean(questName)
                + "</highlight> removed from <highlight2>" + clean(npcLabel) + "</highlight2>.";
    }

    public static String attachedQuestsMessage(
            final String npcLabel,
            final List<String> questNames) {
        if (questNames == null || questNames.isEmpty()) {
            return "<main>No NotQuests quests are attached to <highlight>" + clean(npcLabel) + "</highlight>.";
        }
        return "<main>Quests attached to <highlight>" + clean(npcLabel) + "</highlight>: <highlight2>"
                + String.join(", ", questNames) + "</highlight2>";
    }

    private static void addCheckSection(
            final ArrayList<String> messages,
            final String label,
            final boolean hasStorage,
            final List<String> quests) {
        if (quests.isEmpty()) {
            messages.add("<BLUE>All attached " + label + " Quests: <GRAY>" + (hasStorage ? "Empty" : "None"));
            return;
        }
        messages.add("<BLUE>All " + quests.size() + " attached " + label + " Quests:");
        int counter = 0;
        for (final String questName : quests) {
            if (!questName.isBlank()) {
                counter++;
                messages.add("<GRAY>" + counter + ". <YELLOW>" + questName);
            }
        }
    }

    public static List<String> questNames(final String storedQuests) {
        if (storedQuests == null || storedQuests.isBlank()) {
            return List.of();
        }
        final ArrayList<String> quests = new ArrayList<>();
        for (final String quest : storedQuests.split(String.valueOf(SEPARATOR))) {
            if (quest != null && !quest.isBlank()) {
                quests.add(quest);
            }
        }
        return List.copyOf(quests);
    }

    private static String normalize(final String storedQuests) {
        if (storedQuests == null || storedQuests.isBlank()) {
            return String.valueOf(SEPARATOR);
        }
        return storedQuests.endsWith(String.valueOf(SEPARATOR)) ? storedQuests : storedQuests + SEPARATOR;
    }

    private static String token(final String questName) {
        return SEPARATOR + questName + String.valueOf(SEPARATOR);
    }

    private static boolean onlySeparators(final String storedQuests) {
        if (storedQuests == null || storedQuests.isBlank()) {
            return true;
        }
        for (int i = 0; i < storedQuests.length(); i++) {
            if (storedQuests.charAt(i) != SEPARATOR) {
                return false;
            }
        }
        return true;
    }

    private static String clean(final String value) {
        return value == null ? "" : value;
    }

    public static int questToolId(
            final QuestTool tool,
            final boolean hiddenAttachment) {
        return switch (tool) {
            case CHECK -> QUEST_CHECK;
            case ADD -> hiddenAttachment ? QUEST_ADD_HIDDEN : QUEST_ADD_SHOWING;
            case REMOVE -> hiddenAttachment ? QUEST_REMOVE_HIDDEN : QUEST_REMOVE_SHOWING;
        };
    }

    public static String questToolAction(final QuestTool tool) {
        return switch (tool) {
            case CHECK -> "Check armor stand quest attachments";
            case ADD -> "Attach quest to armor stand";
            case REMOVE -> "Remove quest from armor stand";
        };
    }

    public static NotQuestsAdapter.ArmorStandToolItem questToolItem(
            final String questName,
            final QuestTool tool,
            final boolean hiddenAttachment) {
        final String materialId = switch (tool) {
            case CHECK -> "leather";
            case ADD -> "ghast_tear";
            case REMOVE -> "nether_star";
        };
        return new NotQuestsAdapter.ArmorStandToolItem(
                materialId,
                questToolId(tool, hiddenAttachment),
                questName,
                "<LIGHT_PURPLE>" + questToolAction(tool),
                List.of(
                        "<GRAY>Quest: <YELLOW>" + (questName == null ? "" : questName),
                        "<GRAY>Right-click an armor stand."));
    }

    public static NotQuestsAdapter.ArmorStandToolItem conversationRemoveToolItem() {
        return new NotQuestsAdapter.ArmorStandToolItem(
                "paper",
                CONVERSATION_REMOVE,
                "",
                "<LIGHT_PURPLE>Remove conversation from armor stand",
                List.of("<GRAY>Right-click an armor stand to remove its attached conversation."));
    }

    public static boolean isQuestAttachTool(final int itemId) {
        return itemId == QUEST_ADD_SHOWING || itemId == QUEST_ADD_HIDDEN;
    }

    public static boolean isQuestRemoveTool(final int itemId) {
        return itemId == QUEST_REMOVE_SHOWING || itemId == QUEST_REMOVE_HIDDEN;
    }

    public static boolean isShowingQuestTool(final int itemId) {
        return itemId == QUEST_ADD_SHOWING || itemId == QUEST_REMOVE_SHOWING;
    }

    public static boolean requiresQuestName(final int itemId) {
        return isQuestAttachTool(itemId)
                || isQuestRemoveTool(itemId)
                || itemId == OBJECTIVE_COMPLETION_NPC;
    }

    public static String missingQuestName() {
        return "<RED>Error: Your item has no valid quest attached to it.";
    }

    public static String missingItemConversation() {
        return "<error>Error: this item has no valid conversation.";
    }

    public static String conversationDoesNotExist(final String conversationIdentifier) {
        return "<error>Error: Conversation <highlight>" + conversationIdentifier + "</highlight> does not exist.";
    }

    public static String alreadyAttached(final String conversationIdentifier) {
        return "<RED>Error: That armor stand already has the Conversation <highlight>"
                + conversationIdentifier + "</highlight> attached to it!";
    }

    public static String added(final String conversationIdentifier) {
        return "<GREEN>Conversation with the name <highlight>" + conversationIdentifier
                + "</highlight> was added to this poor little armorstand!";
    }

    public static String removedAll() {
        return "<GREEN>All conversations were removed from this armorStand!";
    }

    public static String noneAttached() {
        return "<RED>This armorstand doesn't have the conversation attached to it.";
    }

    public static String conversationAttachedToNpc(final String conversationName, final String npcLabel) {
        return "<success>Conversation <highlight>" + clean(conversationName)
                + "</highlight> attached to <highlight2>" + clean(npcLabel) + "</highlight2>.";
    }

    public static String conversationsRemovedFromNpc(final String npcLabel) {
        return "<success>Removed attached conversations from <highlight2>" + clean(npcLabel) + "</highlight2>.";
    }

    public record Removal(boolean removed, boolean empty, String storedQuests) {}

    public record QuestAttachmentUpdate(
            boolean changed,
            boolean removeStorage,
            String storedQuests,
            String message) {}

    public record ConversationAddition(String message, boolean storeAttachment, String conversationName) {}

    public record ConversationRemoval(String message, boolean removeAttachment, String conversationName) {}

    public record ToolUse(boolean handled, boolean trackIndicator, List<String> messages) {
        public ToolUse {
            messages = messages == null ? List.of() : List.copyOf(messages);
        }

        public static ToolUse ignored() {
            return new ToolUse(false, false, List.of());
        }
    }

    public record Interaction(
            boolean handled,
            boolean cancelPlatformInteraction,
            boolean trackIndicator,
            List<String> messages) {
        public Interaction {
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }
}
