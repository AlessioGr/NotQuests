/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers.tags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class TagManager {
    private final NotQuests main;
    private final HashMap<String, Tag> identifiersAndTags;

    public TagManager(final NotQuests main) {
        this.main = main;
        this.identifiersAndTags = new HashMap<>();

        loadTags();
    }

    public void loadAllOnlinePlayerTags() {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), this::loadAllOnlinePlayersAsync);
        }else{
            loadAllOnlinePlayersAsync();
        }
    }

    private void loadAllOnlinePlayersAsync() {
        main.getLogManager().info("Loading tags of all online players...");
        for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager().info("Loading tags of all online player " + player.getName());
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                onJoin(questPlayer, player);
            } else {
                main.getLogManager().info("Loading Saving tags of all online player " + player.getName() + " because they have no questplayer.");
            }
        }
    }

/**
* Saves tags of all online players
 * @param preventNewThreadCreation if true, the method will not create a new thread in order to make it async. This is useful if this method is called when shutting down the server, as you cannot create new threads during shut down
*/
    public void saveAllOnlinePlayerTags(final boolean preventNewThreadCreation) {
        if(preventNewThreadCreation){
            saveAllOnlinePlayerTagsAsync();
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), this::saveAllOnlinePlayerTagsAsync);
        }else{
            saveAllOnlinePlayerTagsAsync();
        }
    }

    private void saveAllOnlinePlayerTagsAsync() {
        main.getLogManager().info("Saving tags of all online players...");
        for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager().info("Saving tags of all online player " + player.getName());
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                onQuit(questPlayer, player);
            } else {
                main.getLogManager().info("Skip Saving tags of all online player " + player.getName() + " because they have no questplayer.");
            }
        }
    }



    public void onJoin(final QuestPlayer questPlayer, final Player player) {
        if (!questPlayer.getTags().isEmpty()) {
            if (main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("Skip Loading tags for " + player.getName() + "! Size: " + questPlayer.getTags().size());
            }
            return;
        }
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Loading tags for " + player.getName() + " (Profile: %s) ...",
                questPlayer.getProfile()
            );
        }
        final UUID uuid = player.getUniqueId();

        try (Connection connection = main.getDataManager().getConnection();
             final PreparedStatement tagsStatement = connection.prepareStatement("""
                SELECT TagIdentifier, TagValue, TagType FROM Tags
                WHERE PlayerUUID = ? AND Profile = ?;
             """)
        ) {
            tagsStatement.setString(1, uuid.toString());
            tagsStatement.setString(2, questPlayer.getProfile());

            try(final ResultSet result = tagsStatement.executeQuery()) {
                while (result.next()) {

                    final String tagIdentifier = result.getString("TagIdentifier");
                    final String tagValue = result.getString("TagValue");
                    final String tagType = result.getString("TagType");


                    if (tagValue == null) {
                        questPlayer.setTagValue(tagIdentifier, null);
                        continue;
                    }
                    if (main.getConfiguration().isVerboseStartupMessages()) {
                        main.getLogManager().info("  Loaded <highlight>%s</highlight> %s tag for player <highlight2>%s</highlight2> with the value <highlight2>%s</highlight2>.",
                                tagIdentifier,
                                tagType,
                                player.getName(),
                                tagValue
                        );
                    }


                    switch (tagType) {
                        case "INTEGER" -> questPlayer.setTagValue(tagIdentifier, Integer.parseInt(tagValue));
                        case "FLOAT" -> questPlayer.setTagValue(tagIdentifier, Float.parseFloat(tagValue));
                        case "BOOLEAN" -> questPlayer.setTagValue(tagIdentifier, Boolean.parseBoolean(tagValue));
                        case "STRING" -> questPlayer.setTagValue(tagIdentifier, tagValue);
                        case "DOUBLE" -> questPlayer.setTagValue(tagIdentifier, Double.parseDouble(tagValue));
                    }

                }
            }


        } catch (Exception e) {
            main.getLogManager().severe("ERROR: Could not load tags for player with uuid <highlight>%s</highlight>. Error: ", uuid);
            e.printStackTrace();
            return;
        }


        questPlayer.setFinishedLoadingTags(true);


        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("  Loaded %s tags for %s:",
                    questPlayer.getTags().size(),
                    player.getName()
            );

        }
        if (!questPlayer.getTags().isEmpty()) {
            for (final String tagIdentifier : questPlayer.getTags().keySet()) {
                main.getLogManager().info("    %s: %s (%s)",
                        tagIdentifier,
                        questPlayer.getTagValue(tagIdentifier),
                        questPlayer.getTagValue(tagIdentifier).getClass().getName()
                );
            }
        }
    }

    public void onQuit(final QuestPlayer questPlayer, final Player player) {
        if(!questPlayer.isFinishedLoadingTags()){
            main.getLogManager().info("Saving of tags has been skipped, because tags didn't even finish loading yet.");
            return;
        }
        if(questPlayer.getTags().isEmpty()){
            return;
        }
        final String uuidString = player.getUniqueId().toString();

        try (final Connection connection = main.getDataManager().getConnection();
             final PreparedStatement deleteTagsPS = connection.prepareStatement("""
                DELETE FROM Tags WHERE PlayerUUID = ?;
             """);
             final PreparedStatement insertIntoTagsPS = connection.prepareStatement("""
                INSERT INTO Tags (PlayerUUID, TagIdentifier, TagValue, TagType, Profile) VALUES (?, ?, ?, ?, ?);
             """)
             ) {


            for (final String tagIdentifier : questPlayer.getTags().keySet()) {
                @Nullable final Object tagValue = questPlayer.getTagValue(tagIdentifier);

                if (main.getConfiguration().isVerboseStartupMessages()) {
                    main.getLogManager().info("Saving the " + (tagValue != null ? tagValue.getClass().getName() : "null") + " tag <highlight>" + tagIdentifier + "</highlight> with value <highlight>" + (tagValue != null ? tagValue : "null") + "</highlight> for player <highlight2>" + player.getName() + "</highlight2>...");
                }

                //Remove all tags first before adding all fresh and updated ones
                deleteTagsPS.setString(1, uuidString);
                deleteTagsPS.executeUpdate();

                //Skip over adding the tag if it's null (= removing it)
                if (tagValue == null) {
                    main.getLogManager().info("Null tag => removing the tag");
                    //statement.executeUpdate("DELETE FROM Tags WHERE PlayerUUID = '" + uuidString + "' AND TagIdentifier = '" + tagIdentifier + "';");
                    continue;
                }

                insertIntoTagsPS.setString(1, uuidString);
                insertIntoTagsPS.setString(2, tagIdentifier);
                insertIntoTagsPS.setString(5, questPlayer.getProfile());

                if (tagValue instanceof final Boolean booleanTagValue) {
                    insertIntoTagsPS.setString(3, booleanTagValue.toString());
                    insertIntoTagsPS.setString(4, "BOOLEAN");
                    insertIntoTagsPS.executeUpdate();
                } else if (tagValue instanceof final Integer integerTagValue) {
                    insertIntoTagsPS.setString(3, integerTagValue.toString());
                    insertIntoTagsPS.setString(4, "INTEGER");
                    insertIntoTagsPS.executeUpdate();
                } else if (tagValue instanceof final Float floatValue) {
                    insertIntoTagsPS.setString(3, floatValue.toString());
                    insertIntoTagsPS.setString(4, "FLOAT");
                    insertIntoTagsPS.executeUpdate();
                } else if (tagValue instanceof final Double doubleValue) {
                    insertIntoTagsPS.setString(3, doubleValue.toString());
                    insertIntoTagsPS.setString(4, "DOUBLE");
                    insertIntoTagsPS.executeUpdate();
                } else if (tagValue instanceof final String stringTagValue) {
                    insertIntoTagsPS.setString(3, stringTagValue);
                    insertIntoTagsPS.setString(4, "STRING");
                    insertIntoTagsPS.executeUpdate();
                } else{
                    main.getLogManager().warn("Encountered an unknown tag value type when saving tag %s. Tag value type: %s",
                            tagIdentifier,
                            tagValue.getClass().toString()
                    );
                }
            }

        } catch (Exception e) {
            main.getLogManager().severe("There was an error saving the tag data of player with UUID <highlight>%s</highlight>! Stacktrace:", questPlayer.getUniqueId());
            e.printStackTrace();
        }
    }


    public final Tag getTag(final String tagIdentifier) {
        return identifiersAndTags.get(tagIdentifier.toLowerCase(Locale.ROOT));
    }


    public final Collection<Tag> getTags() {
        return identifiersAndTags.values();
    }

    public final Collection<String> getTagIdentifiers() {
        return identifiersAndTags.keySet();
    }


    public void loadTags() {
        final ArrayList<String> categoriesStringList = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            categoriesStringList.add(category.getCategoryFullName());
        }
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Scheduled Tags Data load for following categories: <highlight>%s", categoriesStringList);
        }

        for (final Category category : main.getDataManager().getCategories()) {
            loadTags(category);
            if (main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Loading tags for category <highlight>%s", category.getCategoryFullName());
            }
        }
    }

    public void loadTags(final Category category) {
        //First load from tags.yml:
        if (category.getTagsConfig() == null) {
            main.getLogManager().severe("Error: Cannot load tags of category <highlight>%s</highlight>, because it doesn't have a tags config. This category has been skipped.",
                    category.getCategoryFullName()
            );
            return;
        }

        final ConfigurationSection tagsConfigurationSection = category.getTagsConfig().getConfigurationSection("tags");
        if (tagsConfigurationSection != null) {
            for (final String tagIdentifier : tagsConfigurationSection.getKeys(false)) {
                if (identifiersAndTags.get(tagIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading tags.yml tag data: The tag %s already exists.",
                            tagIdentifier
                    );
                    return;
                }
                if (main.getConfiguration().isVerboseStartupMessages()) {
                    main.getLogManager().info("Loading tag <highlight>%s", tagIdentifier);
                }

                final String tagTypeString = tagsConfigurationSection.getString(tagIdentifier + ".tagType", "");

                TagType tagType;
                try {
                    tagType = TagType.valueOf(tagTypeString);
                } catch (Exception e) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading tags.yml tag data: The tag %s has an invalid tag type which could not be loaded: %s",
                            tagIdentifier,
                            tagTypeString
                    );
                    return;
                }

                final Tag tag = new Tag(main, tagIdentifier, tagType);
                tag.setCategory(category);


                identifiersAndTags.put(tagIdentifier.toLowerCase(Locale.ROOT), tag);
            }
        }

    }

    public void addTag(final Tag newTag) {
        if (identifiersAndTags.get(newTag.getTagName()) != null) {
            return;
        }

        identifiersAndTags.put(newTag.getTagName(), newTag);

        newTag.getCategory().getTagsConfig().set("tags." + newTag.getTagName() + ".tagType", newTag.getTagType().name());

        newTag.getCategory().saveTagsConfig();
    }

    public void deleteTag(final Tag foundTag) {
        if (identifiersAndTags.get(foundTag.getTagName()) == null) {
            return;
        }

        identifiersAndTags.remove(foundTag.getTagName());
        foundTag.getCategory().getTagsConfig().set("tags." + foundTag.getTagName(), null);
        foundTag.getCategory().saveTagsConfig();
    }
}
