package rocks.gravili.notquests.paper.managers.data;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;

import java.io.File;
import java.util.ArrayList;

public class Category {
    private final NotQuests main;
    private final String categoryName;
    private final File categoryFolder;

    private File categoryFile, questsFile, actionsFile, conditionsFile, conversationsFolder;
    private FileConfiguration categoryConfig, questsConfig, actionsConfig, conditionsConfig;
    private ArrayList<FileConfiguration> conversationsConfigs;

    private Category parentCategory = null;


    public Category(final NotQuests main, final String categoryName, final File categoryFolder) {
        this.main = main;
        this.categoryName = categoryName;
        this.categoryFolder = categoryFolder;
        conversationsConfigs = new ArrayList<>();
    }


    public final File getCategoryFolder() {
        return categoryFolder;
    }

    public final String getCategoryName() {
        return categoryName;
    }

    public final String getCategoryFullName() {
        String fullCategoryName = null;
        if(getParentCategory() != null){
            fullCategoryName = getParentCategory().getCategoryFullName() + ".";
        }
        if(fullCategoryName != null){
            return fullCategoryName + categoryName;
        }else {
            return categoryName;
        }
    }

    public final Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(final Category parentCategory) {
        this.parentCategory = parentCategory;
    }

    public final File getCategoryFile() {
        return categoryFile;
    }

    public void setCategoryFile(final File categoryFile) {
        this.categoryFile = categoryFile;
    }

    public final File getQuestsFile() {
        return questsFile;
    }

    public void setQuestsFile(final File questsFile) {
        this.questsFile = questsFile;
    }

    public final File getActionsFile() {
        return actionsFile;
    }

    public void setActionsFile(final File actionsFile) {
        this.actionsFile = actionsFile;
    }

    public final File getConditionsFile() {
        return conditionsFile;
    }

    public void setConditionsFile(final File conditionsFile) {
        this.conditionsFile = conditionsFile;
    }

    public final File getConversationsFolder() {
        return conversationsFolder;
    }

    public void setConversationsFolder(final File conversationsFolder) {
        this.conversationsFolder = conversationsFolder;
    }

    public void initializeConfigurations() {

        categoryConfig = loadConfig(categoryFile, categoryConfig);
        questsConfig = loadConfig(questsFile, questsConfig);
        actionsConfig = loadConfig(actionsFile, actionsConfig);
        conditionsConfig = loadConfig(conditionsFile, conditionsConfig);

        if (!conversationsConfigs.isEmpty()) {
            return;
        }
        for (File conversationFile : main.getUtilManager().listFilesRecursively(conversationsFolder)) {
            conversationsConfigs.add(loadConfig(conversationFile, null));
        }
    }

    private FileConfiguration loadConfig(File file, FileConfiguration fileConfiguration) {
        if (file != null && fileConfiguration == null) {
            main.getLogManager().info("Loading " + file.getName() + " of category " + getCategoryName() + "...");
            try {
                return main.getDataManager().loadYAMLConfiguration(file);
            } catch (Exception e) {
                main.getDataManager().disablePluginAndSaving("There was an error loading the " + file.getName() + " configuration of category <highlight>" + getCategoryName() + "</highlight>. It either doesn't exist, is invalid or has an error.", e);
            }
        }
        return null;
    }

    public FileConfiguration getCategoryConfig() {
        return categoryConfig;
    }

    public FileConfiguration getQuestsConfig() {
        return questsConfig;
    }

    public FileConfiguration getActionsConfig() {
        return actionsConfig;
    }

    public FileConfiguration getConditionsConfig() {
        return conditionsConfig;
    }

    public final ArrayList<FileConfiguration> getConversationsConfigs() {
        return conversationsConfigs;
    }
}
