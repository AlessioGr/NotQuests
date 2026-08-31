package com.notquests.core.commands.framework;

public final class NQFlags {
    private NQFlags() {}

    public static final Flag NAMETAG_CONTAINS_ANY =
            flag("nametag_containsany", "Only count entities whose nametag contains every provided word.");
    public static final Flag NAMETAG_EQUALS =
            flag("nametag_equals", "Only count entities whose nametag exactly matches the provided text.");
    public static final Flag TASK_DESCRIPTION =
            flag("taskDescription", "Custom task text shown to players for this objective instead of the default description.");
    public static final Flag SPEAKER_COLOR =
            flag("speakerColor", "MiniMessage color or tag used for conversation speaker names.");
    public static final Flag MAX_DISTANCE =
            flag("maxDistance", "Maximum distance allowed from the target location or NPC.");
    public static final Flag WORLD =
            flag("world", "World where this sound or location-based effect should be played.");
    public static final Flag APPLY_ON =
            flag("applyOn", "Quest or objective target this trigger, action, or condition should apply to, such as Quest, O1, or O2.");
    public static final Flag TRIGGER_WORLD =
            flag("world_name", "World name filter for world enter/leave triggers, or ALL for every world.");
    public static final Flag WAIT_TIME_AFTER_COMPLETION =
            flag("waitTimeAfterCompletion", "Minimum time that must pass after completing the quest before this condition can pass.");
    public static final Flag CATEGORY =
            flag("category", "Quest category used to store or look up the created NotQuests object.");
    public static final Flag FORCE =
            flag("force", "Bypass normal checks and force the command to make the requested quest change.");
    public static final Flag DELAY =
            flag("delay", "How long to wait before running this action, such as 1s, 500ms, or 2m.");
    public static final Flag PLAYER =
            flag("player", "Player whose quest, tag, inventory, permission, or variable data should be checked.");
    public static final Flag ACTION_TARGET_PLAYER =
            flag("player", "Player who should be used as the target when executing this action from a command.");
    public static final Flag IGNORE_ACTION_CONDITIONS =
            flag("ignoreConditions", "Run the selected saved action without checking its attached conditions.");
    public static final Flag SILENT_ACTION_EXECUTION =
            flag("silent", "Run the selected saved action without sending the usual execution message.");
    public static final Flag VARIABLE_CHECK_PLAYER =
            flag("player", "Player whose current variable value should be checked; defaults to the command sender when possible.");
    public static final Flag CONVERSATION_DEMO =
            flag("demo", "Fill the new conversation file with the demo conversation instead of an empty template.");
    public static final Flag PRINT_TO_CONSOLE =
            flag("printToConsole", "Print the command output to the server console instead of only chat.");
    public static final Flag NEGATE =
            flag("negate", "Invert the result so this condition passes when it would normally fail.");
    public static final Flag ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED =
            flag(
                    "allowProgressDecreaseIfNotFulfilled",
                    "Allow objective progress to decrease even when this progress condition is not currently fulfilled.");
    public static final Flag LOCATION_X =
            flag("locationX", "X coordinate used by this command.");
    public static final Flag LOCATION_Y =
            flag("locationY", "Y coordinate used by this command.");
    public static final Flag LOCATION_Z =
            flag("locationZ", "Z coordinate used by this command.");
    public static final Flag GUI_ITEM_GLOW =
            flag("glow", "Adds the enchanted glow effect to the quest GUI item.");
    public static final Flag HIDE_IN_NPC =
            flag("hideInNPC", "Attach the quest to the NPC without showing quest-giver particles or preview hints there.");
    public static final Flag HIDE_IN_ARMOR_STAND =
            flag("hideInArmorStand", "Use the hidden armor-stand quest attachment instead of the visible one.");

    private static Flag flag(final String name, final String description) {
        return new Flag(name, NQDescription.of(description));
    }

    public record Flag(String name, NQDescription description) {}
}
