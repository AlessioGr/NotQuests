package com.notquests.core.commands.framework;

import com.notquests.core.managers.UtilManager;

import java.util.Locale;
import java.util.Objects;

public record NQArgumentType(Kind kind, String valueTypeName, SuggestionSource suggestions)
        implements NQCommandSchema.Argument {
    public enum Kind {
        WORD,
        GREEDY_STRING,
        PLAYER,
        QUEST,
        ACTIVE_QUEST,
        ACTION,
        CONDITION,
        CATEGORY,
        CONVERSATION,
        NPC_SELECTOR,
        NPC_SELECTOR_OR_NONE,
        SPEAKER,
        WORLD,
        LOCATION,
        ITEM_SELECTION,
        ACTION_LIST,
        ENTITY_TYPE,
        ENCHANTMENT,
        INTEGER,
        DOUBLE,
        NUMBER_EXPRESSION,
        NUMBER_EXPRESSION_TOKEN,
        BOOLEAN,
        BOOLEAN_EXPRESSION,
        DURATION
    }

    public enum SuggestionSource {
        NONE,
        OBJECTIVE_TYPES,
        ACTION_TYPES,
        SAVED_ACTIONS,
        SAVED_CONDITIONS,
        CONDITION_TYPES,
        VARIABLE_NAMES,
        QUEST_NAMES,
        TAKEABLE_QUEST_NAMES,
        CATEGORY_NAMES,
        CONVERSATION_NAMES,
        ACTIVE_QUEST_NAMES,
        PROFILE_NAMES,
        ITEM_NAMES,
        TAG_TYPES,
        TAG_NAMES,
        TRIGGER_OBJECTIVE_NAMES,
        CONVERSATION_SPEAKER_NAMES,
        BOOLEAN_VALUES
    }

    public NQArgumentType {
        kind = Objects.requireNonNull(kind, "kind");
        valueTypeName = Objects.requireNonNull(valueTypeName, "valueTypeName");
        suggestions = suggestions == null ? SuggestionSource.NONE : suggestions;
    }

    public static NQArgumentType word(final String valueTypeName) {
        return new NQArgumentType(Kind.WORD, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType word(final String valueTypeName, final SuggestionSource suggestions) {
        return new NQArgumentType(Kind.WORD, valueTypeName, suggestions);
    }

    public static NQArgumentType greedyString(final String valueTypeName) {
        return new NQArgumentType(Kind.GREEDY_STRING, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType greedyString(final String valueTypeName, final SuggestionSource suggestions) {
        return new NQArgumentType(Kind.GREEDY_STRING, valueTypeName, suggestions);
    }

    public static NQArgumentType player() {
        return new NQArgumentType(Kind.PLAYER, "player", SuggestionSource.NONE);
    }

    public static NQArgumentType quest() {
        return new NQArgumentType(Kind.QUEST, "quest name", SuggestionSource.QUEST_NAMES);
    }

    public static NQArgumentType takeableQuest() {
        return new NQArgumentType(Kind.QUEST, "quest name", SuggestionSource.TAKEABLE_QUEST_NAMES);
    }

    public static NQArgumentType activeQuest() {
        return new NQArgumentType(Kind.ACTIVE_QUEST, "active quest name", SuggestionSource.ACTIVE_QUEST_NAMES);
    }

    public static NQArgumentType action() {
        return new NQArgumentType(Kind.ACTION, "saved action name", SuggestionSource.SAVED_ACTIONS);
    }

    public static NQArgumentType condition() {
        return new NQArgumentType(Kind.CONDITION, "saved condition name", SuggestionSource.SAVED_CONDITIONS);
    }

    public static NQArgumentType profileName() {
        return new NQArgumentType(Kind.WORD, "profile name", SuggestionSource.PROFILE_NAMES);
    }

    public static NQArgumentType category() {
        return new NQArgumentType(Kind.CATEGORY, "category name", SuggestionSource.CATEGORY_NAMES);
    }

    public static NQArgumentType location() {
        return new NQArgumentType(Kind.LOCATION, "world and coordinates", SuggestionSource.NONE);
    }

    public static NQArgumentType world() {
        return new NQArgumentType(Kind.WORLD, "world", SuggestionSource.NONE);
    }

    public static NQArgumentType itemSelection() {
        return new NQArgumentType(Kind.ITEM_SELECTION, "item selection", SuggestionSource.NONE);
    }

    public static NQArgumentType actionList() {
        return new NQArgumentType(Kind.ACTION_LIST, "comma-separated saved action names", SuggestionSource.SAVED_ACTIONS);
    }

    public static NQArgumentType entityType() {
        return new NQArgumentType(Kind.ENTITY_TYPE, "entity type or any", SuggestionSource.NONE);
    }

    public static NQArgumentType enchantment() {
        return new NQArgumentType(Kind.ENCHANTMENT, "Minecraft enchantment", SuggestionSource.NONE);
    }

    public static NQArgumentType tagType() {
        return new NQArgumentType(Kind.WORD, "tag type", SuggestionSource.TAG_TYPES);
    }

    public static NQArgumentType tagName() {
        return new NQArgumentType(Kind.WORD, "tag name", SuggestionSource.TAG_NAMES);
    }

    public static NQArgumentType itemName() {
        return new NQArgumentType(Kind.WORD, "item name", SuggestionSource.ITEM_NAMES);
    }

    public static NQArgumentType conversation() {
        return new NQArgumentType(Kind.CONVERSATION, "conversation name", SuggestionSource.CONVERSATION_NAMES);
    }

    public static NQArgumentType npcSelector() {
        return new NQArgumentType(Kind.NPC_SELECTOR, "NPC selector", SuggestionSource.NONE);
    }

    public static NQArgumentType npcSelectorOrNone() {
        return new NQArgumentType(Kind.NPC_SELECTOR_OR_NONE, "NPC selector or none", SuggestionSource.NONE);
    }

    public static NQArgumentType speaker() {
        return new NQArgumentType(Kind.SPEAKER, "speaker", SuggestionSource.CONVERSATION_SPEAKER_NAMES);
    }

    public static NQArgumentType triggerObjectiveName() {
        return new NQArgumentType(Kind.WORD, "trigger objective name", SuggestionSource.TRIGGER_OBJECTIVE_NAMES);
    }

    public static NQArgumentType integer(final String valueTypeName) {
        return new NQArgumentType(Kind.INTEGER, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType number(final String valueTypeName) {
        return new NQArgumentType(Kind.DOUBLE, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType numberExpression(final String valueTypeName) {
        return new NQArgumentType(Kind.NUMBER_EXPRESSION, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType numberExpressionToken(final String valueTypeName) {
        return new NQArgumentType(Kind.NUMBER_EXPRESSION_TOKEN, valueTypeName, SuggestionSource.NONE);
    }

    public static NQArgumentType bool(final String valueTypeName) {
        return new NQArgumentType(Kind.BOOLEAN, valueTypeName, SuggestionSource.BOOLEAN_VALUES);
    }

    public static NQArgumentType booleanExpression(final String valueTypeName) {
        return new NQArgumentType(Kind.BOOLEAN_EXPRESSION, valueTypeName, SuggestionSource.BOOLEAN_VALUES);
    }

    public static NQArgumentType duration() {
        return new NQArgumentType(Kind.DURATION, "duration", SuggestionSource.NONE);
    }

    @Override
    public String argumentTypeName() {
        return kind.name();
    }

    /** Converts flag text for the portable argument kinds. Native command arguments are still read by Brigadier. */
    public Object convert(final String input) {
        final String value = input == null ? "" : input.trim();
        return switch (kind) {
            case INTEGER -> Integer.parseInt(value);
            case DOUBLE -> Double.parseDouble(value);
            case BOOLEAN -> switch (value.toLowerCase(Locale.ROOT)) {
                case "true", "yes", "y", "on" -> Boolean.TRUE;
                case "false", "no", "n", "off" -> Boolean.FALSE;
                default -> throw new IllegalArgumentException("'" + input + "' is not a boolean (true/false)");
            };
            case DURATION -> UtilManager.parseDuration(value);
            default -> input == null ? "" : input;
        };
    }

    public boolean commaSeparated() {
        return kind == Kind.ITEM_SELECTION || kind == Kind.ACTION_LIST;
    }
}
