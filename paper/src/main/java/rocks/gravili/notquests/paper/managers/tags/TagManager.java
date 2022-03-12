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

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

public class TagManager {
    private final NotQuests main;
    private final HashMap<String, Tag> identifiersAndTags;

    final NamespacedKey booleanTagsNestedPDCKey, integerTagsNestedPDCKey, floatTagsNestedPDCKey, doubleTagsNestedPDCKey, stringTagsNestedPDC;

    public TagManager(final NotQuests main) {
        this.main = main;
        this.identifiersAndTags = new HashMap<>();
        booleanTagsNestedPDCKey = new NamespacedKey(main.getMain(), "notquests_tags_boolean");
        integerTagsNestedPDCKey = new NamespacedKey(main.getMain(), "notquests_tags_integer");
        floatTagsNestedPDCKey = new NamespacedKey(main.getMain(), "notquests_tags_float");
        doubleTagsNestedPDCKey = new NamespacedKey(main.getMain(), "notquests_tags_double");
        stringTagsNestedPDC = new NamespacedKey(main.getMain(), "notquests_tags_string");

        loadTags();
    }

    public void loadAllOnlinePlayerTags() {
        main.getLogManager().info("Loading tags of all online players...");
        for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager().info("Loading tags of all online player " + player.getName());
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                onJoin(questPlayer, player);
            } else {
                main.getLogManager().info("Loading Saving tags of all online player " + player.getName() + " because they have no questplayer.");
            }
        }
    }

    public void saveAllOnlinePlayerTags() {
        main.getLogManager().info("Saving tags of all online players...");
        for (final Player player : Bukkit.getOnlinePlayers()) {
            main.getLogManager().info("Saving tags of all online player " + player.getName());
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                onQuit(questPlayer, player);
            } else {
                main.getLogManager().info("Skip Saving tags of all online player " + player.getName() + " because they have no questplayer.");
            }
        }
    }

    //TODO: test if hashmap => bytestream serialization is faster
    public void onJoin(final QuestPlayer questPlayer, final Player player) {
        if (questPlayer.getTags().size() > 0) {
            main.getLogManager().info("Skip Loading tags for " + player.getName() + "! Size: " + questPlayer.getTags().size());
            return;
        }
        main.getLogManager().info("Loading tags for " + player.getName() + "...");

        final PersistentDataContainer persistentDataContainer = player.getPersistentDataContainer();
        final PersistentDataContainer booleanTagsContainer = persistentDataContainer.get(booleanTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        final PersistentDataContainer integerTagsContainer = persistentDataContainer.get(integerTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        final PersistentDataContainer floatTagsContainer = persistentDataContainer.get(floatTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        final PersistentDataContainer doubleTagsContainer = persistentDataContainer.get(doubleTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        final PersistentDataContainer stringTagsContainer = persistentDataContainer.get(stringTagsNestedPDC, PersistentDataType.TAG_CONTAINER);

        if (booleanTagsContainer != null) {
            final ArrayList<NamespacedKey> keysToRemove = new ArrayList<>();
            main.getLogManager().info("Loading <highlight>" + booleanTagsContainer.getKeys().size() + "</highlight> boolean tags for player <highlight2>" + player.getName() + "</highlight2>...");

            for (final NamespacedKey key : booleanTagsContainer.getKeys()) {
                if (booleanTagsContainer.has(key, PersistentDataType.BYTE)) {
                    final Object value = booleanTagsContainer.get(key, PersistentDataType.BYTE);
                    main.getLogManager().info("Loaded <highlight>" + key.getKey() + "</highlight> boolean tag for player <highlight2>" + player.getName() + "</highlight2>.");
                    if (value == null) {
                        questPlayer.setTagValue(key.getKey(), null);
                        continue;
                    }

                    questPlayer.setTagValue(key.getKey(), (byte) value != 0);
                } else {
                    main.getLogManager().warn("Cannot load the tag <highlight>" + key.getKey() + "</highlight> for player <highlight2>" + player.getName() + "</highlight2> because the tag's value is incorrect (should be byte). Removing tag...");
                    keysToRemove.add(key);
                }
            }
            for (final NamespacedKey namespacedKey : keysToRemove) {
                booleanTagsContainer.remove(namespacedKey);
                persistentDataContainer.set(booleanTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, booleanTagsContainer);  //TODO: Check if needed
            }
        }

        if (integerTagsContainer != null) {
            final ArrayList<NamespacedKey> keysToRemove = new ArrayList<>();
            main.getLogManager().info("Loading <highlight>" + integerTagsContainer.getKeys().size() + "</highlight> integer tags for player <highlight2>" + player.getName() + "</highlight2>...");

            for (final NamespacedKey key : integerTagsContainer.getKeys()) {
                if (integerTagsContainer.has(key, PersistentDataType.INTEGER)) {
                    questPlayer.setTagValue(key.getKey(), integerTagsContainer.get(key, PersistentDataType.INTEGER));
                    main.getLogManager().info("Loaded <highlight>" + key.getKey() + "</highlight> integer tag for player <highlight2>" + player.getName() + "</highlight2>.");
                } else {
                    main.getLogManager().warn("Cannot load the tag <highlight>" + key.getKey() + "</highlight> for player <highlight2>" + player.getName() + "</highlight2> because the tag's value is incorrect (should be integer). Removing tag...");
                    keysToRemove.add(key);
                }
            }
            for (final NamespacedKey namespacedKey : keysToRemove) {
                integerTagsContainer.remove(namespacedKey);
                persistentDataContainer.set(integerTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, integerTagsContainer);  //TODO: Check if needed
            }
        }

        if (floatTagsContainer != null) {
            final ArrayList<NamespacedKey> keysToRemove = new ArrayList<>();
            main.getLogManager().info("Loading <highlight>" + floatTagsContainer.getKeys().size() + "</highlight> float tags for player <highlight2>" + player.getName() + "</highlight2>...");

            for (final NamespacedKey key : floatTagsContainer.getKeys()) {
                if (floatTagsContainer.has(key, PersistentDataType.FLOAT)) {
                    questPlayer.setTagValue(key.getKey(), floatTagsContainer.get(key, PersistentDataType.FLOAT));
                    main.getLogManager().info("Loaded <highlight>" + key.getKey() + "</highlight> float tag for player <highlight2>" + player.getName() + "</highlight2>.");
                } else {
                    main.getLogManager().warn("Cannot load the tag <highlight>" + key.getKey() + "</highlight> for player <highlight2>" + player.getName() + "</highlight2> because the tag's value is incorrect (should be float). Removing tag...");
                    keysToRemove.add(key);
                }
            }
            for (final NamespacedKey namespacedKey : keysToRemove) {
                floatTagsContainer.remove(namespacedKey);
                persistentDataContainer.set(floatTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, floatTagsContainer);  //TODO: Check if needed
            }
        }

        if (doubleTagsContainer != null) {
            final ArrayList<NamespacedKey> keysToRemove = new ArrayList<>();
            main.getLogManager().info("Loading <highlight>" + doubleTagsContainer.getKeys().size() + "</highlight> double tags for player <highlight2>" + player.getName() + "</highlight2>...");

            for (final NamespacedKey key : doubleTagsContainer.getKeys()) {
                if (doubleTagsContainer.has(key, PersistentDataType.DOUBLE)) {
                    questPlayer.setTagValue(key.getKey(), doubleTagsContainer.get(key, PersistentDataType.DOUBLE));
                    main.getLogManager().info("Loaded <highlight>" + key.getKey() + "</highlight> double tag for player <highlight2>" + player.getName() + "</highlight2>.");
                } else {
                    main.getLogManager().warn("Cannot load the tag <highlight>" + key.getKey() + "</highlight> for player <highlight2>" + player.getName() + "</highlight2> because the tag's value is incorrect (should be double). Removing tag...");
                    keysToRemove.add(key);
                }
            }
            for (final NamespacedKey namespacedKey : keysToRemove) {
                doubleTagsContainer.remove(namespacedKey);
                persistentDataContainer.set(doubleTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, doubleTagsContainer);  //TODO: Check if needed
            }
        }

        if (stringTagsContainer != null) {
            final ArrayList<NamespacedKey> keysToRemove = new ArrayList<>();
            main.getLogManager().info("Loading <highlight>" + stringTagsContainer.getKeys().size() + "</highlight> string tags for player <highlight2>" + player.getName() + "</highlight2>...");

            for (final NamespacedKey key : stringTagsContainer.getKeys()) {
                if (stringTagsContainer.has(key, PersistentDataType.STRING)) {
                    questPlayer.setTagValue(key.getKey(), stringTagsContainer.get(key, PersistentDataType.STRING));
                    main.getLogManager().info("Loaded <highlight>" + key.getKey() + "</highlight> string tag for player <highlight2>" + player.getName() + "</highlight2>.");
                } else {
                    main.getLogManager().warn("Cannot load the tag <highlight>" + key.getKey() + "</highlight> for player <highlight2>" + player.getName() + "</highlight2> because the tag's value is incorrect (should be string). Removing tag...");
                    keysToRemove.add(key);
                }
            }
            for (final NamespacedKey namespacedKey : keysToRemove) {
                stringTagsContainer.remove(namespacedKey);
                persistentDataContainer.set(stringTagsNestedPDC, PersistentDataType.TAG_CONTAINER, stringTagsContainer);  //TODO: Check if needed
            }
        }

        main.getLogManager().info("Loaded " + questPlayer.getTags().size() + " tags for " + player.getName() + ":");
        if (questPlayer.getTags().size() > 0) {
            for (final String tagIdentifier : questPlayer.getTags().keySet()) {
                main.getLogManager().info("   " + tagIdentifier + ": " + questPlayer.getTagValue(tagIdentifier) + " (" + questPlayer.getTagValue(tagIdentifier).getClass().getName() + ")");
            }
        }


    }

    public void onQuit(final QuestPlayer questPlayer, final Player player) {
        final PersistentDataContainer persistentDataContainer = player.getPersistentDataContainer();
        @Nullable PersistentDataContainer booleanTagsContainer = persistentDataContainer.get(booleanTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        @Nullable PersistentDataContainer integerTagsContainer = persistentDataContainer.get(integerTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        @Nullable PersistentDataContainer floatTagsContainer = persistentDataContainer.get(floatTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        @Nullable PersistentDataContainer doubleTagsContainer = persistentDataContainer.get(doubleTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER);
        @Nullable PersistentDataContainer stringTagsContainer = persistentDataContainer.get(stringTagsNestedPDC, PersistentDataType.TAG_CONTAINER);

        for (final String tagIdentifier : questPlayer.getTags().keySet()) {
            @Nullable final Object tagValue = questPlayer.getTagValue(tagIdentifier);

            main.getLogManager().info("Saving the " + (tagValue != null ? tagValue.getClass().getName() : "null") + " tag <highlight>" + tagIdentifier + "</highlight> with value <highlight>" + (tagValue != null ? tagValue : "null") + "</highlight> for player <highlight2>" + player.getName() + "</highlight2>...");


            //Remove tag from the player's pdc if it's null
            if (tagValue == null) {
                main.getLogManager().info("Null tag => removing the tag");
                if (booleanTagsContainer != null) {
                    booleanTagsContainer.remove(new NamespacedKey(main.getMain(), tagIdentifier));
                    persistentDataContainer.set(booleanTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, booleanTagsContainer);  //TODO: Check if needed
                }
                if (integerTagsContainer != null) {
                    integerTagsContainer.remove(new NamespacedKey(main.getMain(), tagIdentifier));
                    persistentDataContainer.set(integerTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, integerTagsContainer);  //TODO: Check if needed
                }
                if (floatTagsContainer != null) {
                    floatTagsContainer.remove(new NamespacedKey(main.getMain(), tagIdentifier));
                    persistentDataContainer.set(floatTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, floatTagsContainer);  //TODO: Check if needed
                }
                if (doubleTagsContainer != null) {
                    doubleTagsContainer.remove(new NamespacedKey(main.getMain(), tagIdentifier));
                    persistentDataContainer.set(doubleTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, doubleTagsContainer);  //TODO: Check if needed
                }
                if (stringTagsContainer != null) {
                    stringTagsContainer.remove(new NamespacedKey(main.getMain(), tagIdentifier));
                    persistentDataContainer.set(stringTagsNestedPDC, PersistentDataType.TAG_CONTAINER, stringTagsContainer);  //TODO: Check if needed
                }
                continue;
            }

            if (tagValue instanceof final Boolean booleanTagValue) {
                if (booleanTagsContainer == null) {
                    booleanTagsContainer = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
                }
                booleanTagsContainer.set(new NamespacedKey(main.getMain(), tagIdentifier), PersistentDataType.BYTE, (byte) (booleanTagValue ? 1 : 0));

                persistentDataContainer.set(booleanTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, booleanTagsContainer);  //TODO: Check if needed
                main.getLogManager().info("Saved boolean tag!");
            } else if (tagValue instanceof final Integer integerTagValue) {
                if (integerTagsContainer == null) {
                    integerTagsContainer = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
                }
                integerTagsContainer.set(new NamespacedKey(main.getMain(), tagIdentifier), PersistentDataType.INTEGER, integerTagValue);

                persistentDataContainer.set(integerTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, integerTagsContainer); //TODO: Check if needed
                main.getLogManager().info("Saved integer tag!");
            } else if (tagValue instanceof final Float floatValue) {
                if (floatTagsContainer == null) {
                    floatTagsContainer = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
                }
                floatTagsContainer.set(new NamespacedKey(main.getMain(), tagIdentifier), PersistentDataType.FLOAT, floatValue);

                persistentDataContainer.set(floatTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, floatTagsContainer); //TODO: Check if needed
                main.getLogManager().info("Saved float tag! Keys size: " + floatTagsContainer.getKeys().size());
            } else if (tagValue instanceof final Double doubleValue) {
                if (doubleTagsContainer == null) {
                    doubleTagsContainer = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
                }
                doubleTagsContainer.set(new NamespacedKey(main.getMain(), tagIdentifier), PersistentDataType.DOUBLE, doubleValue);

                persistentDataContainer.set(doubleTagsNestedPDCKey, PersistentDataType.TAG_CONTAINER, doubleTagsContainer); //TODO: Check if needed
                main.getLogManager().info("Saved double tag!");
            } else if (tagValue instanceof final String stringTagValue) {
                if (stringTagsContainer == null) {
                    stringTagsContainer = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
                }
                stringTagsContainer.set(new NamespacedKey(main.getMain(), tagIdentifier), PersistentDataType.STRING, stringTagValue);

                persistentDataContainer.set(stringTagsNestedPDC, PersistentDataType.TAG_CONTAINER, stringTagsContainer); //TODO: Check if needed
                main.getLogManager().info("Saved string tag!");
            }
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
        main.getLogManager().info("Scheduled Tags Data load for following categories: <highlight>" + categoriesStringList);

        for (final Category category : main.getDataManager().getCategories()) {
            loadTags(category);
            main.getLogManager().info("Loading tags for category <highlight>" + category.getCategoryFullName());
        }
    }

    public void loadTags(final Category category) {
        //First load from tags.yml:
        if (category.getTagsConfig() == null) {
            main.getLogManager().severe("Error: Cannot load tags of category <highlight>" + category.getCategoryFullName() + "</highlight>, because it doesn't have a tags config. This category has been skipped.");
            return;
        }

        final ConfigurationSection tagsConfigurationSection = category.getTagsConfig().getConfigurationSection("tags");
        if (tagsConfigurationSection != null) {
            for (final String tagIdentifier : tagsConfigurationSection.getKeys(false)) {
                if (identifiersAndTags.get(tagIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading tags.yml tag data: The tag " + tagIdentifier + " already exists.");
                    return;
                }
                main.getLogManager().info("Loading tag <highlight>" + tagIdentifier);

                final String tagTypeString = tagsConfigurationSection.getString(tagIdentifier + ".tagType", "");

                TagType tagType;
                try {
                    tagType = TagType.valueOf(tagTypeString);
                } catch (Exception e) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading tags.yml tag data: The tag " + tagIdentifier + " has an invalid tag type which could not be loaded: " + tagTypeString);
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
