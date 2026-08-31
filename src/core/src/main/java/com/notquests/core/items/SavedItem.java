package com.notquests.core.items;

public final class SavedItem {
    private final String name;
    private final ItemSelection itemSelection;
    private String category;
    private String displayName;

    public SavedItem(
            final String name,
            final ItemSelection itemSelection,
            final String category,
            final String displayName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Saved item name cannot be blank.");
        }
        if (itemSelection == null) {
            throw new IllegalArgumentException("Saved item selection cannot be null.");
        }
        this.name = name;
        this.itemSelection = itemSelection;
        this.category = clean(category);
        this.displayName = clean(displayName);
    }

    public String getName() {
        return name;
    }

    public ItemSelection getItemSelection() {
        return itemSelection;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = clean(category);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = clean(displayName);
    }

    private static String clean(final String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
