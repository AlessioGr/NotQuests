package com.notquests.core.managers.tags;

import com.notquests.core.managers.PlayerDatabase;
import com.notquests.core.structs.QuestPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TagManager {
    public record Tag(String tagName, TagType tagType, String category) {
        public Tag(final String tagName, final TagType tagType) {
            this(tagName, tagType, "");
        }

        public Tag {
            if (tagName == null || tagName.isBlank()) {
                throw new IllegalArgumentException("Tag name cannot be blank.");
            }
            tagName = tagName.toLowerCase(Locale.ROOT);
            tagType = Objects.requireNonNull(tagType, "tagType");
            category = category == null ? "" : category;
        }
    }

    public static Object parse(final TagType type, final String value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case BOOLEAN -> Boolean.parseBoolean(value);
            case INTEGER -> Integer.parseInt(value);
            case FLOAT -> Float.parseFloat(value);
            case DOUBLE -> Double.parseDouble(value);
            case STRING -> value;
        };
    }

    public static boolean matchesType(final Object value, final TagType type) {
        if (value == null || type == null) {
            return false;
        }
        return switch (type) {
            case BOOLEAN -> value instanceof Boolean;
            case INTEGER -> value instanceof Integer;
            case FLOAT -> value instanceof Float;
            case DOUBLE -> value instanceof Double;
            case STRING -> value instanceof String;
        };
    }

    public static Optional<EncodedTagValue> encode(final Object value) {
        if (value instanceof final Boolean booleanValue) {
            return Optional.of(new EncodedTagValue(booleanValue.toString(), TagType.BOOLEAN));
        }
        if (value instanceof final Integer integerValue) {
            return Optional.of(new EncodedTagValue(integerValue.toString(), TagType.INTEGER));
        }
        if (value instanceof final Float floatValue) {
            return Optional.of(new EncodedTagValue(floatValue.toString(), TagType.FLOAT));
        }
        if (value instanceof final Double doubleValue) {
            return Optional.of(new EncodedTagValue(doubleValue.toString(), TagType.DOUBLE));
        }
        if (value instanceof final String stringValue) {
            return Optional.of(new EncodedTagValue(stringValue, TagType.STRING));
        }
        return Optional.empty();
    }

    public record EncodedTagValue(String value, TagType type) {}

    public void loadAllOnlinePlayers(
            final ConnectionSource connections,
            final Iterable<OnlinePlayerTags> players,
            final BatchLogger logger,
            final boolean verbose) {
        if (logger != null) {
            logger.info("Loading tags of all online players...");
        }
        for (final OnlinePlayerTags player : players) {
            if (logger != null) {
                logger.info("Loading tags of all online player " + player.playerName());
            }
            if (player.player() == null) {
                if (logger != null) {
                    logger.info("Loading Saving tags of all online player "
                            + player.playerName()
                            + " because they have no questplayer.");
                }
                continue;
            }
            loadOnlinePlayer(connections, player, logger, verbose);
        }
    }

    public void saveAllOnlinePlayers(
            final ConnectionSource connections,
            final Iterable<OnlinePlayerTags> players,
            final BatchLogger logger,
            final boolean verbose) {
        if (logger != null) {
            logger.info("Saving tags of all online players...");
        }
        for (final OnlinePlayerTags player : players) {
            if (logger != null) {
                logger.info("Saving tags of all online player " + player.playerName());
            }
            if (player.player() == null) {
                if (logger != null) {
                    logger.info("Skip Saving tags of all online player "
                            + player.playerName()
                            + " because they have no questplayer.");
                }
                continue;
            }
            saveOnlinePlayer(connections, player, logger, verbose);
        }
    }

    public LoadSummary loadOnlinePlayer(
            final ConnectionSource connections,
            final OnlinePlayerTags player,
            final BatchLogger logger,
            final boolean verbose) {
        try (Connection connection = connections.connection()) {
            return loadOnJoin(connection, player.player(), player.playerName(), logger, verbose);
        } catch (final Exception exception) {
            if (logger != null) {
                logger.severe(
                        "ERROR: Could not load tags for player with uuid <highlight>%s</highlight>. Error: ",
                        player.playerIdentifier());
                logger.stackTrace(exception);
            }
            return new LoadSummary(0, true);
        }
    }

    public SaveSummary saveOnlinePlayer(
            final ConnectionSource connections,
            final OnlinePlayerTags player,
            final BatchLogger logger,
            final boolean verbose) {
        try (Connection connection = connections.connection()) {
            return saveOnQuit(connection, player.player(), player.playerName(), logger, verbose);
        } catch (final Exception exception) {
            if (logger != null) {
                logger.severe(
                        "There was an error saving the tag data of player with UUID <highlight>%s</highlight>! Stacktrace:",
                        player.playerIdentifier());
                logger.stackTrace(exception);
            }
            return new SaveSummary(0, true);
        }
    }

    public LoadSummary loadOnJoin(
            final Connection connection,
            final QuestPlayer player,
            final String playerName,
            final Logger logger,
            final boolean verbose) throws SQLException {
        if (player == null) {
            return new LoadSummary(0, true);
        }
        if (!player.getTags().isEmpty()) {
            if (verbose && logger != null) {
                logger.info("Skip Loading tags for %s! Size: %s", playerName, player.getTags().size());
            }
            return new LoadSummary(player.getTags().size(), true);
        }
        if (verbose && logger != null) {
            logger.info("Loading tags for %s (Profile: %s) ...", playerName, player.getProfile());
        }

        final int loaded = load(connection, player, playerName, logger, verbose);
        player.setFinishedLoadingTags(true);

        if (verbose && logger != null) {
            logger.info("  Loaded %s tags for %s:", player.getTags().size(), playerName);
            for (final Map.Entry<String, Object> tag : player.getTags().entrySet()) {
                logger.info(
                        "    %s: %s (%s)",
                        tag.getKey(),
                        tag.getValue(),
                        tag.getValue() == null ? "null" : tag.getValue().getClass().getName());
            }
        }
        return new LoadSummary(loaded, false);
    }

    public SaveSummary saveOnQuit(
            final Connection connection,
            final QuestPlayer player,
            final String playerName,
            final Logger logger,
            final boolean verbose) throws SQLException {
        if (player == null) {
            return new SaveSummary(0, true);
        }
        if (!player.isFinishedLoadingTags()) {
            if (logger != null) {
                logger.info("Saving of tags has been skipped, because tags didn't even finish loading yet.");
            }
            return new SaveSummary(0, true);
        }
        if (player.getTags().isEmpty()) {
            return new SaveSummary(0, true);
        }
        return new SaveSummary(save(connection, player, playerName, logger, verbose), false);
    }

    public int load(
            final Connection connection,
            final QuestPlayer player,
            final String playerName,
            final Logger logger,
            final boolean verbose) throws SQLException {
        if (connection == null || player == null) {
            return 0;
        }
        int loaded = 0;
        try (PreparedStatement tagsStatement = connection.prepareStatement(PlayerDatabase.SELECT_TAGS)) {
            tagsStatement.setString(1, player.getPlayerIdentifier());
            tagsStatement.setString(2, player.getProfile());

            try (ResultSet result = tagsStatement.executeQuery()) {
                while (result.next()) {
                    final String tagIdentifier = result.getString("TagIdentifier");
                    final String tagValue = result.getString("TagValue");
                    final String tagType = result.getString("TagType");

                    if (tagValue == null) {
                        player.setTagValue(tagIdentifier, null);
                        continue;
                    }
                    final TagType parsedType;
                    try {
                        parsedType = TagType.valueOf(tagType);
                    } catch (final RuntimeException exception) {
                        if (logger != null) {
                            logger.warn(
                                    "Skipping tag %s for player %s because it has unknown tag type %s.",
                                    tagIdentifier,
                                    playerName,
                                    tagType);
                        }
                        continue;
                    }
                    if (verbose && logger != null) {
                        logger.info(
                                "  Loaded <highlight>%s</highlight> %s tag for player <highlight2>%s</highlight2> with the value <highlight2>%s</highlight2>.",
                                tagIdentifier,
                                tagType,
                                playerName,
                                tagValue);
                    }
                    player.setTagValue(tagIdentifier, parse(parsedType, tagValue));
                    loaded++;
                }
            }
        }
        return loaded;
    }

    public int save(
            final Connection connection,
            final QuestPlayer player,
            final String playerName,
            final Logger logger,
            final boolean verbose) throws SQLException {
        if (connection == null || player == null) {
            return 0;
        }
        int saved = 0;
        try (PreparedStatement deleteTags = connection.prepareStatement(PlayerDatabase.DELETE_TAGS);
                PreparedStatement insertTag = connection.prepareStatement(PlayerDatabase.INSERT_TAG)) {
            deleteTags.setString(1, player.getPlayerIdentifier());
            deleteTags.setString(2, player.getProfile());
            deleteTags.executeUpdate();

            for (final Map.Entry<String, Object> tag : player.getTags().entrySet()) {
                final Object tagValue = tag.getValue();
                if (verbose && logger != null) {
                    logger.info(
                            "Saving the %s tag <highlight>%s</highlight> with value <highlight>%s</highlight> for player <highlight2>%s</highlight2>...",
                            tagValue != null ? tagValue.getClass().getName() : "null",
                            tag.getKey(),
                            tagValue != null ? tagValue : "null",
                            playerName);
                }
                if (tagValue == null) {
                    if (logger != null) {
                        logger.info("Null tag => removing the tag");
                    }
                    continue;
                }

                final var encodedValue = encode(tagValue);
                if (encodedValue.isEmpty()) {
                    if (logger != null) {
                        logger.warn(
                                "Encountered an unknown tag value type when saving tag %s. Tag value type: %s",
                                tag.getKey(),
                                tagValue.getClass().toString());
                    }
                    continue;
                }

                insertTag.setString(1, player.getPlayerIdentifier());
                insertTag.setString(2, tag.getKey());
                insertTag.setString(3, encodedValue.get().value());
                insertTag.setString(4, encodedValue.get().type().name());
                insertTag.setString(5, player.getProfile());
                insertTag.executeUpdate();
                saved++;
            }
        }
        return saved;
    }

    public interface Logger {
        void info(String message, Object... args);

        void warn(String message, Object... args);
    }

    public interface BatchLogger extends Logger {
        void severe(String message, Object... args);

        void stackTrace(Throwable throwable);
    }

    public interface ConnectionSource {
        Connection connection() throws SQLException;
    }

    public record LoadSummary(int loaded, boolean skipped) {}

    public record SaveSummary(int saved, boolean skipped) {}

    public record OnlinePlayerTags(QuestPlayer player, String playerIdentifier, String playerName) {}
}
