package rocks.gravili.notquests.paper.managers.tags;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.Locale;

public class Tag {
    private final NotQuests main;
    private final TagType tagType;
    private final String tagName;
    private Category category;

    public Tag(final NotQuests main, final String tagName, final TagType tagType){
        this.main = main;
        this.tagName = tagName.toLowerCase(Locale.ROOT);
        this.tagType = tagType;
        category = main.getDataManager().getDefaultCategory();
    }

    public final TagType getTagType(){
        return tagType;
    }

    public final String getTagName(){
        return tagName;
    }

    public final Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }
}
