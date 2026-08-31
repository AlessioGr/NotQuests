package com.notquests.core.managers;

import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the authoritative configured quest and category repositories. */
public final class QuestManager {
    private final ConcurrentHashMap<String, Quest> quests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Category> categories = new ConcurrentHashMap<>();

    public QuestManager() {
        ensureDefaultCategory();
    }

    public List<Quest> getAllQuests() {
        return quests.values().stream()
                .sorted(Comparator.comparing(Quest::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> getQuestNames() {
        return getAllQuests().stream().map(Quest::getIdentifier).toList();
    }

    public List<String> getQuestNamesInCategory(final String categoryName) {
        final String checkedCategory = categoryName == null || categoryName.isBlank()
                ? Category.DEFAULT_NAME
                : categoryName;
        return getAllQuests().stream()
                .filter(quest -> Category.same(quest.getCategory(), checkedCategory))
                .map(Quest::getIdentifier)
                .toList();
    }

    public int getQuestCount() {
        return quests.size();
    }

    public Quest getQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        return quests.get(questKey(questName));
    }

    public Quest getOrCreateQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            throw new IllegalArgumentException("Quest name cannot be blank.");
        }
        ensureDefaultCategory();
        return quests.computeIfAbsent(questKey(questName), ignored -> new Quest(questName));
    }

    public Quest createQuest(final String questName, final String categoryName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        final Quest created = new Quest(questName);
        if (categoryName != null && !categoryName.isBlank()) {
            created.setCategory(categoryName);
        }
        if (quests.putIfAbsent(questKey(questName), created) != null) {
            return null;
        }
        getOrCreateCategory(created.getCategory());
        return created;
    }

    public Quest removeQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            return null;
        }
        return quests.remove(questKey(questName));
    }

    public List<Category> getAllCategories() {
        return categories.values().stream()
                .sorted(Comparator.comparing(Category::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> getCategoryNames() {
        return getAllCategories().stream().map(Category::getIdentifier).toList();
    }

    public List<String> getTopLevelCategoryNames() {
        return getCategoryNames().stream().filter(Category::isTopLevel).toList();
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public String getDefaultCategoryName() {
        return Category.DEFAULT_NAME;
    }

    public Category getCategory(final String categoryName) {
        return categories.get(Category.key(categoryName));
    }

    public Category getOrCreateCategory(final String categoryName) {
        final String canonicalName = Category.canonical(categoryName);
        return categories.computeIfAbsent(Category.key(canonicalName), ignored -> new Category(canonicalName));
    }

    public boolean createCategory(final String categoryName) {
        final String canonicalName = Category.canonical(categoryName);
        return categories.putIfAbsent(Category.key(canonicalName), new Category(canonicalName)) == null;
    }

    public boolean createCategory(final String categoryName, final String parentCategoryName) {
        return createCategory(getCategoryIdentifier(categoryName, parentCategoryName));
    }

    public Category removeCategory(final String categoryName) {
        if (Category.same(Category.DEFAULT_NAME, categoryName)) {
            return null;
        }
        return categories.remove(Category.key(categoryName));
    }

    public String getCategoryIdentifier(final String categoryName, final String parentCategoryName) {
        return parentCategoryName == null || parentCategoryName.isBlank()
                ? Category.canonical(categoryName)
                : Category.join(parentCategoryName, categoryName);
    }

    public String getCategoryDisplayNameOrIdentifier(final String categoryName) {
        final Category category = getCategory(categoryName);
        return category == null ? Category.canonical(categoryName) : category.getDisplayNameOrIdentifier();
    }

    public void ensureDefaultCategory() {
        categories.computeIfAbsent(
                Category.key(Category.DEFAULT_NAME),
                ignored -> new Category(Category.DEFAULT_NAME));
    }

    public void clear() {
        quests.clear();
        categories.clear();
        ensureDefaultCategory();
    }

    private static String questKey(final String questName) {
        return questName.toLowerCase(Locale.ROOT);
    }
}
