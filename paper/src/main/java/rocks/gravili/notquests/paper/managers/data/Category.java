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

package rocks.gravili.notquests.paper.managers.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.PredefinedProgressOrder;
import rocks.gravili.notquests.paper.structs.Quest;

public class Category {
  private final NotQuests main;
  private final String categoryName;
  private final File categoryFolder;
  private final ArrayList<FileConfiguration> conversationsConfigs;

  private PredefinedProgressOrder predefinedProgressOrder;

  private int conversationDelayInMS = 0;

  private File categoryFile,
      questsFile,
      actionsFile,
      conditionsFile,
      tagsFile,
      itemsFile,
      conversationsFolder;
  private FileConfiguration categoryConfig,
      questsConfig,
      actionsConfig,
      conditionsConfig,
      tagsConfig,
      itemsConfig;
  private Category parentCategory = null;
  private String displayName = "";

  private ItemStack guiItem = new ItemStack(Material.CHEST);


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
    if (getParentCategory() != null) {
      fullCategoryName = getParentCategory().getCategoryFullName() + ".";
    }
    if (fullCategoryName != null) {
      return fullCategoryName + categoryName;
    } else {
      return categoryName;
    }
  }

  public final String getFinalName() {
    if (!displayName.isBlank()) {
      return getDisplayName();
    } else {
      return getCategoryName();
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

  public final File getTagsFile() {
    return tagsFile;
  }

  public void setTagsFile(final File tagsFile) {
    this.tagsFile = tagsFile;
  }

  public final File getItemsFile() {
    return itemsFile;
  }

  public void setItemsFile(final File itemsFile) {
    this.itemsFile = itemsFile;
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
    tagsConfig = loadConfig(tagsFile, tagsConfig);
    itemsConfig = loadConfig(itemsFile, itemsConfig);

    if (!conversationsConfigs.isEmpty()) {
      return;
    }
    if (conversationsFolder != null) {
      for (File conversationFile :
          main.getUtilManager().listFilesRecursively(conversationsFolder)) {
        conversationsConfigs.add(loadConfig(conversationFile, null));
      }
    }

    //Setup default values
    setupDefaults();
  }

  private FileConfiguration loadConfig(File file, FileConfiguration fileConfiguration) {
    if (file != null && fileConfiguration == null) {
      main.getLogManager()
          .debug(
              "    Loading <highlight>"
                  + file.getName()
                  + "</highlight> of category <highlight>"
                  + getCategoryName()
                  + "</highlight>...");
      try {
        return main.getDataManager().loadYAMLConfiguration(file);
      } catch (Exception e) {
        main.getDataManager()
            .disablePluginAndSaving(
                "There was an error loading the "
                    + file.getName()
                    + " configuration of category <highlight>"
                    + getCategoryName()
                    + "</highlight>. It either doesn't exist, is invalid or has an error. Please carefully read the error below and try to fix it:",
                e);
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

  public FileConfiguration getTagsConfig() {
    return tagsConfig;
  }

  public FileConfiguration getItemsConfig() {
    return itemsConfig;
  }

  public final ArrayList<FileConfiguration> getConversationsConfigs() {
    return conversationsConfigs;
  }

  public void saveCategoryConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        categoryConfig.save(categoryFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveQuestsConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        questsConfig.save(questsFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveActionsConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        actionsConfig.save(actionsFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveConditionsConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        conditionsConfig.save(conditionsFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveTagsConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        tagsConfig.save(tagsFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveItemsConfig() {
    if (main.getDataManager().isSavingEnabled()) {
      try {
        itemsConfig.save(itemsFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public final PredefinedProgressOrder getPredefinedProgressOrder() {
    return predefinedProgressOrder;
  }

  public void setPredefinedProgressOrder(final PredefinedProgressOrder predefinedProgressOrder, final boolean save) {
    this.predefinedProgressOrder = predefinedProgressOrder;
    for(final Quest quest : getQuests()){
      quest.updateConditionsWithSpecial();
    }
    if (save) {
      if(predefinedProgressOrder != null) {
        predefinedProgressOrder.saveToConfiguration(getCategoryConfig(),  "predefinedProgressOrder");
      }else{
        getQuestsConfig()
            .set(
                "predefinedProgressOrder",
                null);
      }
      saveCategoryConfig();
    }
  }

  public final String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String newDisplayName, boolean save) {
    this.displayName = newDisplayName;
    if (save) {
      getCategoryConfig().set("displayName", newDisplayName);
      saveCategoryConfig();
    }
  }


  public void removeDisplayName(boolean save) {
    this.displayName = "";
    if (save) {
      getCategoryConfig().set("displayName", null);
      saveCategoryConfig();
    }
  }

  public final ItemStack getGuiItem() {
    return guiItem;
  }

  public void setGuiItem(final ItemStack guiItem, final boolean save) {
    this.guiItem = guiItem;
    if (save) {
      getCategoryConfig().set("guiItem", guiItem);
      saveCategoryConfig();
    }
  }



  public final ArrayList<Quest> getQuests(){
    return main.getQuestManager().getAllQuests().stream()
        .filter(quest -> quest.getCategory() == this)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public String toString() {
    return "Category{"
        + "categoryName='"
        + categoryName
        + '\''
        + ", categoryFolder="
        + categoryFolder
        + ", categoryFile="
        + categoryFile
        + ", parentCategory="
        + parentCategory
        + '}';
  }

  public final int getConversationDelayInMS(){
    return conversationDelayInMS;
  }

  public void setConversationDelayInMS(final int conversationDelayInMS, final boolean save){
    this.conversationDelayInMS = conversationDelayInMS;

    if (save) {
      getCategoryConfig()
              .set(
                      "conversations.delay",
                      conversationDelayInMS);
      saveCategoryConfig();
    }
  }

  public void setupDefaults(){
    if(!getCategoryConfig().contains("conversations.delay")){
      getCategoryConfig()
              .set(
                      "conversations.delay",
                      conversationDelayInMS);
      saveCategoryConfig();
    }

    if(!getCategoryConfig().contains("displayName")){
      getCategoryConfig()
              .set(
                      "displayName",
                      "");
      saveCategoryConfig();
    }
  }

  public void loadDataFromCategoryConfig() {
    this.displayName = getCategoryConfig().getString("displayName", "");
    this.predefinedProgressOrder = PredefinedProgressOrder.fromConfiguration(getCategoryConfig(), "predefinedProgressOrder");

    this.conversationDelayInMS = getCategoryConfig().getInt("conversations.delay", 0);

    this.guiItem = getCategoryConfig().getItemStack("guiItem", new ItemStack(Material.CHEST));

  }
}
