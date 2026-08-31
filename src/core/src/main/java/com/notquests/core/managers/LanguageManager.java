package com.notquests.core.managers;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.text.NotQuestsMiniMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class LanguageManager {
    public static final String MISSING_STRING_PREFIX = "Language string not found: ";
    public static final String MISSING_STRING_LIST_PREFIX = "Language string list not found: ";
    private static final List<String> LANGUAGE_FILES = List.of(
            "af-ZA.yml",
            "ar-SA.yml",
            "ca-ES.yml",
            "cs-CZ.yml",
            "da-DK.yml",
            "de-DE.yml",
            "el-GR.yml",
            "en-US.yml",
            "es-ES.yml",
            "fi-FI.yml",
            "fr-FR.yml",
            "he-IL.yml",
            "hu-HU.yml",
            "id-ID.yml",
            "it-IT.yml",
            "ja-JP.yml",
            "ko-KR.yml",
            "nl-NL.yml",
            "no-NO.yml",
            "pl-PL.yml",
            "pt-BR.yml",
            "pt-PT.yml",
            "ro-RO.yml",
            "ru-RU.yml",
            "sr-Cyrl.yml",
            "sr-Latn.yml",
            "sv-SE.yml",
            "tr-TR.yml",
            "uk-UA.yml",
            "vi-VN.yml",
            "zh-CN.yml",
            "zh-TW.yml");

    private YamlConfig language = YamlConfig.empty();
    private YamlConfig defaults = YamlConfig.empty();
    private String languageCode = "en-US";
    private boolean loaded = false;

    public void load(final Path dataFolder, final String configuredLanguageCode, final Logger logger)
            throws IOException {
        final String code = configuredLanguageCode == null || configuredLanguageCode.isBlank()
                ? "en-US"
                : configuredLanguageCode;
        copyMissingLanguages(dataFolder);

        final Path languageFolder = dataFolder.resolve("languages");
        final Path defaultFile = languageFolder.resolve("default.yml");
        final Path languageFile = languageFolder.resolve(code + ".yml");

        defaults = YamlConfig.load(defaultFile);
        language = YamlConfig.load(languageFile);
        languageCode = code;
        loaded = true;

        final Merge result = mergeMissingStrings(language, defaults);
        if (result.changed()) {
            if (logger != null) {
                for (final String addedKey : result.addedKeys()) {
                    logger.info("Updating string: <highlight>" + addedKey + "</highlight>");
                }
                logger.info("<DARK_PURPLE>Language ConfigurationManager <highlight>" + code
                        + ".yml <DARK_PURPLE>was updated with new values! Saving it...");
            }
            YamlConfig.save(language, languageFile);
        }
    }

    public String languageCode() {
        return languageCode;
    }

    public boolean loaded() {
        return loaded;
    }

    public YamlConfig configuration() {
        return language;
    }

    public static List<String> languageFiles() {
        return LANGUAGE_FILES;
    }

    public static void copyMissingLanguages(final Path dataFolder) throws IOException {
        final Path languageFolder = dataFolder.resolve("languages");
        Files.createDirectories(languageFolder);
        for (final String fileName : LANGUAGE_FILES) {
            final Path target = languageFolder.resolve(fileName);
            if (Files.notExists(target)) {
                Files.writeString(
                        target,
                        ConfigurationManager.bundledResourceText("translations/" + fileName),
                        StandardCharsets.UTF_8);
            }
        }
        Files.writeString(
                languageFolder.resolve("default.yml"),
                ConfigurationManager.bundledResourceText("translations/en-US.yml"),
                StandardCharsets.UTF_8);
    }

    public static Merge mergeMissingStrings(
            final YamlConfig languageConfiguration,
            final YamlConfig defaultLanguageConfiguration) {
        if (languageConfiguration == null || defaultLanguageConfiguration == null) {
            return new Merge(false, List.of());
        }
        final YamlConfig.Section defaultSection = defaultLanguageConfiguration.getConfigurationSection("");
        if (defaultSection == null) {
            return new Merge(false, List.of());
        }

        final ArrayList<String> addedKeys = new ArrayList<>();
        for (final String defaultString : defaultSection.getKeys(true)) {
            if (defaultSection.isConfigurationSection(defaultString)) {
                continue;
            }
            if (!languageConfiguration.contains(defaultString)) {
                languageConfiguration.set(defaultString, defaultSection.get(defaultString));
                addedKeys.add(defaultString);
            }
        }
        return new Merge(!addedKeys.isEmpty(), addedKeys);
    }

    public record Merge(boolean changed, List<String> addedKeys) {
        public Merge {
            addedKeys = addedKeys == null ? List.of() : List.copyOf(addedKeys);
        }
    }

    public String string(final String key) {
        if (key == null || key.isBlank() || !language.isString(key)) {
            return null;
        }
        return language.getString(key);
    }

    public List<String> stringList(final String key) {
        if (key == null || key.isBlank() || !language.isList(key)) {
            return List.of();
        }
        return language.getStringList(key);
    }

    public int integer(final String key) {
        if (key == null || key.isBlank() || !language.isInt(key)) {
            return 0;
        }
        return language.getInt(key);
    }

    public String translateString(
            final String key,
            final Placeholders placeholders,
            final UnaryOperator<String> externalPlaceholders) {
        final String raw = string(key);
        if (raw == null) {
            return missingString(key);
        }
        return apply(raw, placeholders, externalPlaceholders);
    }

    public List<String> translateStringList(
            final String key,
            final Placeholders placeholders,
            final UnaryOperator<List<String>> externalPlaceholders) {
        final List<String> raw = existingStringList(key);
        if (raw == null || raw.isEmpty()) {
            return List.of(missingStringList(key));
        }
        return apply(raw, placeholders, externalPlaceholders);
    }

    public List<String> translateComponentStrings(
            final String key,
            final Placeholders placeholders,
            final UnaryOperator<List<String>> externalPlaceholders) {
        final List<String> raw = existingStringList(key);
        if (raw == null || raw.isEmpty()) {
            return List.of(missingString(key));
        }
        return apply(raw, placeholders, externalPlaceholders);
    }

    public String translate(
            final String key,
            final Map<String, String> replacements,
            final String fallbackMiniMessage) {
        final String raw = string(key);
        final String source = raw == null ? fallbackMiniMessage : raw;
        return apply(source, replacements);
    }

    public static String missingString(final String key) {
        return MISSING_STRING_PREFIX + key;
    }

    public static String missingStringList(final String key) {
        return MISSING_STRING_LIST_PREFIX + key;
    }

    public static String apply(final String miniMessage, final Map<String, String> replacements) {
        String result = miniMessage == null ? "" : miniMessage;
        if (replacements != null && !replacements.isEmpty()) {
            final LinkedHashMap<String, Supplier<String>> suppliers = new LinkedHashMap<>();
            for (final Map.Entry<String, String> entry : replacements.entrySet()) {
                suppliers.put(entry.getKey(), () -> entry.getValue() == null ? "" : entry.getValue());
            }
            result = UtilManager.replaceFromMap(
                    result,
                    suppliers);
        }
        return NotQuestsMiniMessage.applySpecialTags(result);
    }

    public static String apply(
            final String miniMessage,
            final Placeholders placeholders,
            final UnaryOperator<String> externalPlaceholders) {
        String result = miniMessage == null ? "" : miniMessage;
        if (placeholders != null) {
            result = placeholders.apply(result);
        }
        if (externalPlaceholders != null) {
            result = externalPlaceholders.apply(result);
        }
        return NotQuestsMiniMessage.applySpecialTags(result);
    }

    public static List<String> apply(
            final List<String> miniMessages,
            final Placeholders placeholders,
            final UnaryOperator<List<String>> externalPlaceholders) {
        List<String> result = miniMessages == null ? List.of() : List.copyOf(miniMessages);
        if (placeholders != null) {
            result = placeholders.apply(result);
        }
        if (externalPlaceholders != null) {
            result = externalPlaceholders.apply(result);
        }
        if (result == null || result.isEmpty()) {
            return List.of();
        }
        return result.stream()
                .map(NotQuestsMiniMessage::applySpecialTags)
                .toList();
    }

    private List<String> existingStringList(final String key) {
        if (key == null || key.isBlank() || !language.isList(key)) {
            return null;
        }
        return language.getStringList(key);
    }

    public interface Logger {
        void info(String message);
    }

    public static final class Placeholders {
        private final Map<String, Supplier<String>> replacements = new LinkedHashMap<>();

        private Placeholders() {
            putLiteral("%QUESTPOINTS%", "0");
        }

        public static Placeholders create() {
            return new Placeholders();
        }

        public Placeholders put(final String key, final Supplier<String> value) {
            if (key != null && value != null) {
                replacements.put(key, value);
            }
            return this;
        }

        public Placeholders putLiteral(final String key, final Object value) {
            return put(key, () -> String.valueOf(value == null ? "" : value));
        }

        public Placeholders putAll(final Map<?, ?> values) {
            if (values == null) {
                return this;
            }
            for (final Map.Entry<?, ?> entry : values.entrySet()) {
                putLiteral(String.valueOf(entry.getKey()), entry.getValue());
            }
            return this;
        }

        public Placeholders quest(
                final Supplier<String> displayNameOrIdentifier,
                final Supplier<String> identifier,
                final Supplier<String> description,
                final Supplier<Integer> allObjectivesCount) {
            put("%QUESTNAME%", displayNameOrIdentifier);
            put("%QUESTID%", identifier);
            put("%QUESTDESCRIPTION%", description);
            put("%ALLOBJECTIVESCOUNT%", () -> String.valueOf(allObjectivesCount.get()));
            return this;
        }

        public Placeholders questPlayer(final Supplier<Number> questPoints, final Supplier<String> profile) {
            put("%QUESTPOINTS%", () -> String.valueOf(questPoints.get()));
            put("%PROFILENAME%", profile);
            return this;
        }

        public Placeholders activeHolderCounts(
                final Supplier<Integer> completedObjectivesCount,
                final Supplier<Integer> allObjectivesCount) {
            put("%COMPLETEDOBJECTIVESCOUNT%", () -> String.valueOf(completedObjectivesCount.get()));
            put("%ALLOBJECTIVESCOUNT%", () -> String.valueOf(allObjectivesCount.get()));
            return this;
        }

        public Placeholders activeQuestCompletedObjectives(final Supplier<Integer> completedObjectivesCount) {
            put("%COMPLETEDOBJECTIVESCOUNT%", () -> String.valueOf(completedObjectivesCount.get()));
            return this;
        }

        public Placeholders objective(final Supplier<Integer> objectiveId, final Supplier<String> objectiveName) {
            put("%OBJECTIVEID%", () -> String.valueOf(objectiveId.get()));
            put("%OBJECTIVENAME%", objectiveName);
            return this;
        }

        public Placeholders activeObjective(
                final Supplier<Integer> objectiveId,
                final Supplier<String> objectiveName,
                final Supplier<Number> currentProgress,
                final Supplier<Number> neededProgress,
                final Supplier<String> taskDescription,
                final Supplier<String> completedTaskDescription,
                final Supplier<String> objectiveDescription) {
            objective(objectiveId, objectiveName);
            put("%ACTIVEOBJECTIVEID%", () -> String.valueOf(objectiveId.get()));
            put("%ACTIVEOBJECTIVEPROGRESS%", () -> formatProgress(currentProgress.get()));
            put("%OBJECTIVEPROGRESSNEEDED%", () -> formatProgress(neededProgress.get()));
            put("%OBJECTIVEPROGRESSPERCENTAGE%", () -> {
                final double needed = neededProgress.get().doubleValue();
                if (needed == 0d) {
                    return "0";
                }
                return String.valueOf((int) ((currentProgress.get().doubleValue() / needed) * 100));
            });
            put("%OBJECTIVETASKDESCRIPTION%", taskDescription);
            put("%COMPLETEDOBJECTIVETASKDESCRIPTION%", completedTaskDescription);
            put("%OBJECTIVEDESCRIPTION%", objectiveDescription);
            return this;
        }

        public Placeholders category(final Supplier<String> finalName, final Supplier<String> categoryId) {
            put("%CATEGORYNAME%", finalName);
            put("%CATEGORYID%", categoryId);
            return this;
        }

        public Placeholders npcId(final Supplier<String> npcId) {
            put("%NPCID%", npcId);
            return this;
        }

        public Placeholders questCooldownLeftFormatted(final Supplier<String> cooldownText) {
            put("%QUESTCOOLDOWNLEFTFORMATTED%", cooldownText);
            return this;
        }

        public String apply(final String message) {
            return UtilManager.replaceFromMap(message, replacements);
        }

        public List<String> apply(final List<String> messages) {
            final List<String> result = new ArrayList<>();
            for (final String message : messages) {
                result.add(apply(message));
            }
            return result;
        }

        public static String formatProgress(final Number value) {
            String formatted = String.format("%.2f", value == null ? 0d : value.doubleValue());
            if (formatted.endsWith(".00") || formatted.endsWith(",00")) {
                formatted = formatted.substring(0, formatted.length() - 3);
            }
            return formatted;
        }
    }
}
