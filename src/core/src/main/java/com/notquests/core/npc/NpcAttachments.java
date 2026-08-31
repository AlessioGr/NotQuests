package com.notquests.core.npc;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class NpcAttachments {
    private NpcAttachments() {}

    public interface Npc {
        String npcType();

        NQNPCID npcId();

        String npcName();
    }

    public static QuestAttachments collect(final Collection<Quest> quests) {
        final ArrayList<Attachment> attachments = new ArrayList<>();
        final ArrayList<String> warnings = new ArrayList<>();
        for (final Quest quest : quests == null ? List.<Quest>of() : quests) {
            if (quest == null) {
                continue;
            }
            for (final NpcAttachment attachment : quest.getNpcAttachments()) {
                if (attachment.npcId() == null || attachment.npcType() == null || attachment.npcType().isBlank()) {
                    warnings.add("Skipping invalid NPC attachment for quest <highlight>"
                            + quest.getIdentifier() + "</highlight>.");
                    continue;
                }
                attachments.add(new Attachment(quest.getIdentifier(), attachment));
            }
        }
        return new QuestAttachments(List.copyOf(attachments), List.copyOf(warnings));
    }

    public static List<NpcAttachment> questAttachments(
            final Collection<NpcAttachment> attachments,
            final Boolean questShowing) {
        final ArrayList<NpcAttachment> resolved = new ArrayList<>();
        for (final NpcAttachment attachment : attachments == null ? List.<NpcAttachment>of() : attachments) {
            if (attachment == null || attachment.npcId() == null) {
                continue;
            }
            if (questShowing != null && attachment.questShowing() != questShowing) {
                continue;
            }
            resolved.add(attachment);
        }
        return List.copyOf(resolved);
    }

    public static boolean hasAttachment(
            final Collection<Quest> quests,
            final Collection<ConversationManager.Conversation> conversations,
            final String npcType,
            final NQNPCID npcId) {
        if (npcType == null || npcType.isBlank() || npcId == null) {
            return false;
        }
        for (final Quest quest : quests == null ? List.<Quest>of() : quests) {
            if (quest == null) {
                continue;
            }
            for (final NpcAttachment attachment : quest.getNpcAttachments()) {
                if (sameNpc(attachment, npcType, npcId)) {
                    return true;
                }
            }
        }
        for (final ConversationManager.Conversation conversation
                : conversations == null ? List.<ConversationManager.Conversation>of() : conversations) {
            if (conversation == null) {
                continue;
            }
            for (final NpcAttachment attachment : conversation.npcAttachments()) {
                if (sameNpc(attachment, npcType, npcId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<NQNPCID> staleNativeAttachments(
            final Collection<NQNPCID> expected,
            final Collection<NQNPCID> nativeAttachments) {
        final HashSet<NQNPCID> expectedIds = new HashSet<>(
                expected == null ? List.of() : expected);
        return (nativeAttachments == null ? List.<NQNPCID>of() : nativeAttachments).stream()
                .filter(Objects::nonNull)
                .filter(id -> !expectedIds.contains(id))
                .distinct()
                .toList();
    }

    private static boolean sameNpc(
            final NpcAttachment attachment,
            final String npcType,
            final NQNPCID npcId) {
        return attachment != null
                && attachment.npcId() != null
                && attachment.npcId().equals(npcId)
                && npcType.equalsIgnoreCase(attachment.npcType());
    }

    public static String missingNpc(final String label) {
        return (label == null || label.isBlank() ? "NPC" : label) + " not found!";
    }

    public static String selectionToolDisplayName() {
        return "<LIGHT_PURPLE>Right click any NQNPC to execute action";
    }

    public static String selectionToolGivenMessage() {
        return "<success>You have been given an item with which you can execute a certain action when right-clicking any NQNPC. Check your inventory!";
    }

    public static String traitAttachedMessage(final int npcId, final String npcName) {
        return "NPC with the ID <highlight>" + npcId
                + "</highlight> and name <highlight>" + cleanName(npcName)
                + "</highlight> has been assigned the NotQuests quest-giver trait!";
    }

    public static String npcRemovedMessage(final int npcId, final String npcName) {
        return "NPC with the ID <highlight>" + npcId
                + "</highlight> and name <highlight>" + cleanName(npcName)
                + "</highlight> has been removed!";
    }

    public static String cleanupMessage(final int removed, final int checked) {
        return removed == 0
                ? "No bugged NPCs found! Amount of checked NPCs: <highlight>" + checked + "</highlight>"
                : "<YELLOW><highlight>" + removed
                        + "</highlight> bugged NPCs have been found and removed! Amount of checked NPCs: <highlight>"
                        + checked + "</highlight>";
    }

    public static String missingEscortNpcMessage() {
        return "<error>The NPC you have to escort does not exist. Please consult an admin.";
    }

    public static String missingEscortDestinationMessage() {
        return "<error>The destination NPC does not exist. Please consult an admin.";
    }

    public static String missingEscortNpcWarning(final int npcId) {
        return "The escort NPC with the ID <highlight>" + npcId + "</highlight> was not found.";
    }

    public static String missingEscortDestinationWarning(final int npcId) {
        return "The destination NPC with the ID <highlight>" + npcId + "</highlight> was not found.";
    }

    public static String missingEscortPlayerWarning(final String playerIdentifier) {
        return "The escort objective could not be started because player <highlight>"
                + cleanName(playerIdentifier) + "</highlight> was not found.";
    }

    public static String escortStartedMessage(final String escortName, final String destinationName) {
        return "<success>Escort quest started! Please escort <highlight>"
                + cleanName(escortName)
                + "</highlight> to <highlight>"
                + cleanName(destinationName)
                + "</highlight>.";
    }

    public static String escortTooFarMessage() {
        return "<error>The NPC you have to escort is not close enough to you!";
    }

    public static String escortDeliveredMessage(final String escortName) {
        return "<success>You have successfully delivered the NPC <highlight>"
                + cleanName(escortName) + "</highlight>.";
    }

    private static String cleanName(final String npcName) {
        return npcName == null ? "" : npcName.replace("&", "").replace("§", "");
    }

    public static ConversationAttachments collectConversations(
            final Collection<ConversationManager.Conversation> conversations) {
        final ArrayList<ConversationAttachment> attachments = new ArrayList<>();
        final ArrayList<String> warnings = new ArrayList<>();
        for (final ConversationManager.Conversation conversation
                : conversations == null ? List.<ConversationManager.Conversation>of() : conversations) {
            if (conversation == null) {
                continue;
            }
            for (final NpcAttachment attachment : conversation.npcAttachments()) {
                if (attachment.npcId() == null || attachment.npcType() == null || attachment.npcType().isBlank()) {
                    warnings.add("Skipping invalid NPC attachment for conversation <highlight>"
                            + conversation.name() + "</highlight>.");
                    continue;
                }
                attachments.add(new ConversationAttachment(conversation.name(), attachment));
            }
        }
        return new ConversationAttachments(List.copyOf(attachments), List.copyOf(warnings));
    }

    public record QuestAttachments(List<Attachment> attachments, List<String> warnings) {}

    public static String formatAttachedNPCs(final Collection<? extends Npc> npcs) {
        if (npcs == null || npcs.isEmpty()) {
            return "none";
        }
        return npcs.stream()
                .map(NpcAttachments::formatAttachedNPC)
                .collect(Collectors.joining(", "));
    }

    public static String formatAttachedNPC(final Npc npc) {
        if (npc == null) {
            return "unknown";
        }
        return formatAttachedNPC(npc.npcType(), npc.npcId(), npc.npcName());
    }

    public static String formatAttachedNPC(
            final String npcType,
            final NQNPCID npcId,
            final String npcName) {
        final String type = npcType == null ? "npc" : npcType.toLowerCase(Locale.ROOT);
        final String id = npcId == null ? "unknown" : npcId.getEitherAsString();
        return npcName == null || npcName.isBlank() ? type + ":" + id : type + ":" + id + " (" + npcName + ")";
    }

    public static String selector(final String npcType, final NQNPCID npcId) {
        if (npcType == null || npcType.isBlank() || npcId == null) {
            return "";
        }
        return npcType.toLowerCase(Locale.ROOT) + ":" + npcId.getEitherAsString();
    }

    public record NpcAttachment(
            String npcType,
            NQNPCID npcId,
            String npcName,
            boolean questShowing) {
        public NpcAttachment {
            npcType = npcType == null ? "" : npcType;
            npcName = npcName == null ? "" : npcName;
        }

        public String identifyingString() {
            return npcType + "-" + (npcId == null ? "" : npcId.getEitherAsString());
        }
    }

    public record Detachments(List<Detachment> attachments) {
        public Detachments {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    public record Detachment(String questIdentifier, NpcAttachment attachment, boolean removePlatformTrait) {
        public Detachment {
            questIdentifier = questIdentifier == null ? "" : questIdentifier;
        }
    }

    public record Selector(String type, NQNPCID id) {
        public static Optional<Selector> parse(final String selector) {
            if (selector == null || selector.isBlank() || !selector.contains(":")) {
                return Optional.empty();
            }
            final String[] parts = selector.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return Optional.empty();
            }
            final String type = parts[0].toLowerCase(Locale.ROOT);
            final String rawId = parts[1];
            if ("armorstand".equals(type)) {
                try {
                    return Optional.of(new Selector(type, NQNPCID.fromUUID(UUID.fromString(rawId))));
                } catch (final IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
            if ("fancynpcs".equals(type)) {
                return Optional.of(new Selector(type, NQNPCID.fromString(rawId)));
            }
            try {
                return Optional.of(new Selector(type, NQNPCID.fromInteger(Integer.parseInt(rawId))));
            } catch (final NumberFormatException ignored) {
                return Optional.empty();
            }
        }
    }

    public record Attachment(String questIdentifier, NpcAttachment attachment) {}

    public record ConversationAttachments(List<ConversationAttachment> attachments, List<String> warnings) {}

    public record ConversationAttachment(String conversationName, NpcAttachment attachment) {}

    /** Core-owned pending NPC selections. Platforms only create and read their native selector item. */
    public static final class Selections {
        private final AtomicInteger nextId = new AtomicInteger();
        private final Map<Integer, Consumer<NotQuestsAdapter.NpcSelection>> actions = new ConcurrentHashMap<>();

        public int register(final Consumer<NotQuestsAdapter.NpcSelection> selected) {
            if (selected == null) {
                return -1;
            }
            final int id = nextId.getAndIncrement();
            actions.put(id, selected);
            return id;
        }

        public boolean complete(
                final int id,
                final String npcType,
                final NQNPCID npcId,
                final String npcName) {
            if (npcId == null || npcType == null || npcType.isBlank()) {
                return false;
            }
            final Consumer<NotQuestsAdapter.NpcSelection> selected = actions.remove(id);
            if (selected == null) {
                return false;
            }
            final String selector = selector(npcType, npcId);
            selected.accept(new NotQuestsAdapter.NpcSelection(
                    selector,
                    formatAttachedNPC(npcType, npcId, npcName),
                    npcType,
                    npcId,
                    npcName == null ? "" : npcName));
            return true;
        }

        public void remove(final int id) {
            actions.remove(id);
        }

        public void clear() {
            actions.clear();
        }
    }

    public record Interaction(
            boolean handled,
            boolean objectiveHandled,
            boolean previewShown,
            boolean conversationStarted,
            boolean focusConversation,
            String conversationName) {
        public Interaction {
            conversationName = conversationName == null ? "" : conversationName;
        }

        public static Interaction objective() {
            return new Interaction(true, true, false, false, false, "");
        }

        public static Interaction none() {
            return new Interaction(false, false, false, false, false, "");
        }
    }

    /** Immediate native effects requested after core has handled a Citizens NPC click. */
    public record ClickEffects(
            boolean handled,
            boolean selectionHandled,
            boolean objectiveHandled,
            boolean previewShown,
            boolean conversationStarted,
            boolean pauseNavigation,
            boolean stopPlayerMovement,
            boolean focusStarted) {
        public static ClickEffects selection() {
            return new ClickEffects(true, true, false, false, false, false, false, false);
        }

        public static ClickEffects none() {
            return new ClickEffects(false, false, false, false, false, false, false, false);
        }
    }

    public record Indicator(
            boolean attached,
            boolean showParticle,
            int particleCount,
            String particleType,
            boolean showText,
            String text) {
        public Indicator {
            particleType = particleType == null ? "" : particleType;
            text = text == null ? "" : text;
        }

        public static Indicator hidden(final boolean attached) {
            return new Indicator(attached, false, 0, "", false, "");
        }
    }

    /** Native Citizens observation/effect used by core's escort-completion decision. */
    public record EscortNpc(boolean sameWorld, double distanceSquared, String name, Runnable finish) {
        public EscortNpc {
            name = name == null ? "" : name;
            finish = finish == null ? () -> {} : finish;
        }
    }

    @FunctionalInterface
    public interface EscortStarter {
        boolean start(int npcId, String playerIdentifier, NQLocation spawnLocation);
    }
}
