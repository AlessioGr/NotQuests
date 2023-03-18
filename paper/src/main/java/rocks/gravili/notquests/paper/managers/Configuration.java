/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.managers;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

/**
 * This is the Configuration Class which contains the settings which can be configured in the
 * General.conf
 *
 * @author Alessio Gravili
 */
public class Configuration {

  private final String beamMode = "end_gateway"; // end_gateway, beacon, end_crystal

  public boolean visualObjectiveTrackingShowProgressInActionBar = true;
  public boolean visualObjectiveTrackingShowProgressInBossBar = true;
  public boolean visualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted = false;
  public int visualObjectiveTrackingBossBarTimer = 10;

  private String configurationVersion = "";
  private int configurationVersionMajor;
  private int configurationVersionMinor;
  private int configurationVersionPatch;

  public boolean debug = false;
  public boolean verboseStartupMessages = true;
  public boolean loadPlayerData = true;
  public boolean savePlayerData = true;
  public boolean loadPlayerDataOnJoin = true;
  public boolean savePlayerDataOnQuit = true;
  public boolean storageCreateBackupsWhenServerShutsDown = true;
  public boolean storageCreateDatabaseBackupBeforeDatabaseLoads = true;

  public String placeholder_player_active_quests_list_horizontal_separator = " | ";
  public int placeholder_player_active_quests_list_horizontal_limit = -1;
  public int placeholder_player_active_quests_list_vertical_limit = -1;
  public boolean placeholder_player_active_quests_list_horizontal_use_displayname_if_available =
      true;
  public boolean placeholder_player_active_quests_list_vertical_use_displayname_if_available = true;
  public List<String> journalItemEnabledWorlds;
  public int journalInventorySlot = 8;
  public ItemStack journalItem = null;
  public boolean packetMagic = false;
  public boolean usePacketEvents = false;
  public boolean packetMagicUnsafeDisregardVersion = false;
  public boolean deletePreviousConversations = false;
  public int previousConversationsHistorySize = 20;
  public boolean updateCheckerNotifyOpsInChat = true;
  public boolean showQuestItemAmount = false;
  public boolean showObjectiveItemAmount = true;
  public boolean questVisibilityEvaluationLimits = false;
  public boolean questVisibilityEvaluationAlreadyAccepted = true;
  public boolean questVisibilityEvaluationAcceptCooldown = false;
  public boolean questVisibilityEvaluationConditions = false;
  // Visual
  public boolean visualTitleQuestSuccessfullyAccepted_enabled = true;
  public boolean visualTitleQuestFailed_enabled = true;
  public boolean visualTitleQuestCompleted_enabled = true;
  public boolean supportPlaceholderAPIInTranslationStrings = false;
  public int guiQuestDescriptionMaxLineLength = 50;
  public int guiObjectiveDescriptionMaxLineLength = 50;
  public boolean wrapLongWords = false;
  public boolean hideRewardsWithoutName = true;
  public boolean showRewardsAfterQuestCompletion = true;
  public boolean showRewardsAfterObjectiveCompletion = true;
  /** MYSQL Database Connection Information */
  private String databaseHost, databaseName, databaseUsername, databasePassword;
  private int databasePort;
  private boolean questPreviewUseGUI = true;
  private boolean userCommandsUseGUI = true;
  private boolean mySQLEnabled = false;
  private int maxActiveQuestsPerPlayer = -1;
  private boolean armorStandPreventEditing = true;
  // Particles
  private boolean citizensFocusingEnabled = true;
  private int citizensFocusingRotateTime = 14;
  private boolean citizensFocusingCancelConversationWhenTooFar = true;
  private int citizensNPCQuestGiverIndicatorParticleSpawnInterval = 10;
  private int citizensNPCQuestGiverIndicatorParticleCount = 1;
  private Particle citizensNPCQuestGiverIndicatorParticleType = Particle.VILLAGER_ANGRY;
  private String citizensNPCQuestGiverIndicatorText = "";
  private int citizensNPCQuestGiverIndicatorTextInterval = 100;
  private boolean citizensNPCQuestGiverIndicatorParticleEnabled = true;
  private double citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow = -1;
  private int armorStandQuestGiverIndicatorParticleSpawnInterval = 10;
  private int armorStandQuestGiverIndicatorParticleCount = 1;
  private Particle armorStandQuestGiverIndicatorParticleType = Particle.VILLAGER_ANGRY;
  private boolean armorStandQuestGiverIndicatorParticleEnabled = true;
  private double armorStandQuestGiverIndicatorParticleDisableIfTPSBelow = -1;
  private String languageCode = "en";
  // Integrations
  private boolean integrationCitizensEnabled = true;
  private boolean integrationVaultEnabled = true;
  private boolean integrationPlaceholderAPIEnabled = true;
  private boolean integrationMythicMobsEnabled = true;
  private boolean integrationEliteMobsEnabled = true;
  private boolean integrationBetonQuestEnabled = true;
  private boolean integrationWorldEditEnabled = true;
  private boolean integrationSlimeFunEnabled = true;
  private boolean integrationLuckPermsEnabled = true;
  private boolean integrationUltimateClansEnabled = true;
  private boolean integrationTownyEnabled = true;
  private boolean integrationJobsRebornEnabled = true;
  private boolean integrationProjectKorraEnabled = true;
  private boolean integrationEcoBossesEnabled = true;
  private boolean integrationUltimateJobsEnabled = true;

  private boolean integrationZNPCsEnabled = true;


  private boolean objectiveUnlockConditionsCheckOnAnyAction = true;

  private int objectiveUnlockConditionsCheckRegularInterval = -1;


  public boolean isIntegrationZNPCsEnabled() {
    return integrationZNPCsEnabled;
  }

  public void setIntegrationZNPCsEnabled(boolean integrationZNPCsEnabled) {
    this.integrationZNPCsEnabled = integrationZNPCsEnabled;
  }


  public boolean isIntegrationFloodgateEnabled() {
    return integrationFloodgateEnabled;
  }

  public void setIntegrationFloodgateEnabled(boolean integrationFloodgateEnabled) {
    this.integrationFloodgateEnabled = integrationFloodgateEnabled;
  }

  private boolean integrationFloodgateEnabled = true;

  // Other
  private boolean actionBarFancyCommandCompletionEnabled = true;
  private boolean titleFancyCommandCompletionEnabled = false;
  private boolean bossBarFancyCommandCompletionEnabled = false;
  private int fancyCommandCompletionMaxPreviousArgumentsDisplayed = 2;
  private boolean moveEventEnabled = true;
  // GUI
  private boolean guiQuestPreviewDescription_enabled = true;
  private boolean guiQuestPreviewRewards_enabled = true;
  private boolean guiQuestPreviewRequirements_enabled = true;

  private boolean consoleColorsEnabled = true;
  private boolean consoleColorsDownsampleColors = false;

  public boolean isConversationAllowAnswerNumberInChat() {
    return conversationAllowAnswerNumberInChat;
  }

  public void setConversationAllowAnswerNumberInChat(boolean conversationAllowAnswerNumberInChat) {
    this.conversationAllowAnswerNumberInChat = conversationAllowAnswerNumberInChat;
  }

  private boolean conversationAllowAnswerNumberInChat = true;

  private String colorsConsolePrefixPrefix = "<#393e46>[<gradient:#E0EAFC:#CFDEF3>";
  private String colorsConsolePrefixSuffix = "<#393e46>]<#636c73>: ";
  private String colorsConsoleInfoDefault = "<main>";
  private String colorsConsoleInfoDefaultDownsampled = "<gray>";
  private String colorsConsoleInfoData = "<gradient:#1FA2FF:#12D8FA:#A6FFCB>";
  private String colorsConsoleInfoDataDownsampled = "<blue>";

  private String colorsConsoleInfoLanguage = "<gradient:#AA076B:#61045F>";
  private String colorsConsoleInfoLanguageDownsampled = "<dark_purple>";

  private String colorsConsoleWarnDefault = "<warn>";
  private String colorsConsoleWarnDefaultDownsampled = "<yellow>";

  private String colorsConsoleSevereDefault = "<error>";
  private String colorsConsoleSevereDefaultDownsampled = "<red>";

  private String colorsConsoleDebugDefault = "<unimportant>";
  private String colorsConsoleDebugDownsampled = "<dark_gray>";

  private List<String> colorsMain;
  private List<String> colorsHighlight;
  private List<String> colorsHighlight2;
  private List<String> colorsError;
  private List<String> colorsSuccess;
  private List<String> colorsUnimportant;
  private List<String> colorsVeryUnimportant;
  private List<String> colorsWarn;
  private List<String> colorsPositive;
  private List<String> colorsNegative;

  public String getBeamMode() {
    return beamMode;
  }

  public boolean isQuestVisibilityEvaluationAcceptCooldown() {
    return questVisibilityEvaluationAcceptCooldown;
  }

  public void setQuestVisibilityEvaluationAcceptCooldown(
      boolean questVisibilityEvaluationAcceptCooldown) {
    this.questVisibilityEvaluationAcceptCooldown = questVisibilityEvaluationAcceptCooldown;
  }

  public boolean isVisualObjectiveTrackingShowProgressInActionBar() {
    return visualObjectiveTrackingShowProgressInActionBar;
  }

  public void setVisualObjectiveTrackingShowProgressInActionBar(
      boolean visualObjectiveTrackingShowProgressInActionBar) {
    this.visualObjectiveTrackingShowProgressInActionBar =
        visualObjectiveTrackingShowProgressInActionBar;
  }

  public boolean isVisualObjectiveTrackingShowProgressInBossBar() {
    return visualObjectiveTrackingShowProgressInBossBar;
  }

  public void setVisualObjectiveTrackingShowProgressInBossBar(
      boolean visualObjectiveTrackingShowProgressInBossBar) {
    this.visualObjectiveTrackingShowProgressInBossBar =
        visualObjectiveTrackingShowProgressInBossBar;
  }

  public boolean isVisualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted() {
    return visualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted;
  }

  public void setVisualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted(
      boolean visualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted) {
    this.visualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted =
        visualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted;
  }

  public boolean isQuestVisibilityEvaluationConditions() {
    return questVisibilityEvaluationConditions;
  }

  public void setQuestVisibilityEvaluationConditions(boolean questVisibilityEvaluationConditions) {
    this.questVisibilityEvaluationConditions = questVisibilityEvaluationConditions;
  }

  public int getVisualObjectiveTrackingBossBarTimer() {
    return visualObjectiveTrackingBossBarTimer;
  }

  public void setVisualObjectiveTrackingBossBarTimer(int visualObjectiveTrackingBossBarTimer) {
    this.visualObjectiveTrackingBossBarTimer = visualObjectiveTrackingBossBarTimer;
  }

  public String getConfigurationVersion() {
    return configurationVersion;
  }

  public final int getConfigurationVersionMajor(){
    return this.configurationVersionMajor;
  }
  public final int getConfigurationVersionMinor(){
    return this.configurationVersionMinor;
  }
  public final int getConfigurationVersionPatch(){
    return this.configurationVersionPatch;
  }

  public void setConfigurationVersion(String configurationVersion) {
    this.configurationVersion = configurationVersion;
    final String[] configurationVersionSplit = configurationVersion.split("\\.");
    this.configurationVersionMajor = Integer.parseInt(configurationVersionSplit[0]);
    this.configurationVersionMinor = Integer.parseInt(configurationVersionSplit[1]);
    this.configurationVersionPatch = Integer.parseInt(configurationVersionSplit[2]);
  }

  public boolean isQuestVisibilityEvaluationLimits() {
    return questVisibilityEvaluationLimits;
  }

  public void setQuestVisibilityEvaluationLimits(boolean questVisibilityEvaluationLimits) {
    this.questVisibilityEvaluationLimits = questVisibilityEvaluationLimits;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public boolean isVerboseStartupMessages() {
    return verboseStartupMessages;
  }

  public void setVerboseStartupMessages(boolean verboseStartupMessages) {
    this.verboseStartupMessages = verboseStartupMessages;
  }

  public String getDatabaseHost() {
    return databaseHost;
  }

  public void setDatabaseHost(String databaseHost) {
    this.databaseHost = databaseHost;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getDatabaseUsername() {
    return databaseUsername;
  }

  public void setDatabaseUsername(String databaseUsername) {
    this.databaseUsername = databaseUsername;
  }

  public String getDatabasePassword() {
    return databasePassword;
  }

  public void setDatabasePassword(String databasePassword) {
    this.databasePassword = databasePassword;
  }

  public boolean isQuestVisibilityEvaluationAlreadyAccepted() {
    return questVisibilityEvaluationAlreadyAccepted;
  }

  public void setQuestVisibilityEvaluationAlreadyAccepted(
      boolean questVisibilityEvaluationAlreadyAccepted) {
    this.questVisibilityEvaluationAlreadyAccepted = questVisibilityEvaluationAlreadyAccepted;
  }

  public int getDatabasePort() {
    return databasePort;
  }

  public void setDatabasePort(int databasePort) {
    this.databasePort = databasePort;
  }

  public boolean isLoadPlayerData() {
    return loadPlayerData;
  }

  public void setLoadPlayerData(boolean loadPlayerData) {
    this.loadPlayerData = loadPlayerData;
  }

  public boolean isSavePlayerData() {
    return savePlayerData;
  }

  public void setSavePlayerData(boolean savePlayerData) {
    this.savePlayerData = savePlayerData;
  }

  public boolean isLoadPlayerDataOnJoin() {
    return loadPlayerDataOnJoin;
  }

  public void setLoadPlayerDataOnJoin(boolean loadPlayerDataOnJoin) {
    this.loadPlayerDataOnJoin = loadPlayerDataOnJoin;
  }

  public boolean isSavePlayerDataOnQuit() {
    return savePlayerDataOnQuit;
  }

  public void setSavePlayerDataOnQuit(boolean savePlayerDataOnQuit) {
    this.savePlayerDataOnQuit = savePlayerDataOnQuit;
  }

  public boolean isQuestPreviewUseGUI() {
    return questPreviewUseGUI;
  }

  public void setQuestPreviewUseGUI(boolean questPreviewUseGUI) {
    this.questPreviewUseGUI = questPreviewUseGUI;
  }

  public boolean isUserCommandsUseGUI() {
    return userCommandsUseGUI;
  }

  public void setUserCommandsUseGUI(boolean userCommandsUseGUI) {
    this.userCommandsUseGUI = userCommandsUseGUI;
  }

  public boolean isMySQLEnabled() {
    return mySQLEnabled;
  }

  public void setMySQLEnabled(boolean mySQLEnabled) {
    this.mySQLEnabled = mySQLEnabled;
  }

  public boolean isStorageCreateBackupsWhenServerShutsDown() {
    return storageCreateBackupsWhenServerShutsDown;
  }

  public void setStorageCreateBackupsWhenServerShutsDown(
      boolean storageCreateBackupsWhenServerShutsDown) {
    this.storageCreateBackupsWhenServerShutsDown = storageCreateBackupsWhenServerShutsDown;
  }

  public boolean isStorageCreateDatabaseBackupBeforeDatabaseLoads() {
    return storageCreateDatabaseBackupBeforeDatabaseLoads;
  }

  public void setStorageCreateDatabaseBackupBeforeDatabaseLoads(
          boolean storageCreateDatabaseBackupBeforeDatabaseLoads) {
    this.storageCreateDatabaseBackupBeforeDatabaseLoads = storageCreateDatabaseBackupBeforeDatabaseLoads;
  }

  public String getPlaceholder_player_active_quests_list_horizontal_separator() {
    return placeholder_player_active_quests_list_horizontal_separator;
  }

  public void setPlaceholder_player_active_quests_list_horizontal_separator(
      String placeholder_player_active_quests_list_horizontal_separator) {
    this.placeholder_player_active_quests_list_horizontal_separator =
        placeholder_player_active_quests_list_horizontal_separator;
  }

  public int getPlaceholder_player_active_quests_list_horizontal_limit() {
    return placeholder_player_active_quests_list_horizontal_limit;
  }

  public void setPlaceholder_player_active_quests_list_horizontal_limit(
      int placeholder_player_active_quests_list_horizontal_limit) {
    this.placeholder_player_active_quests_list_horizontal_limit =
        placeholder_player_active_quests_list_horizontal_limit;
  }

  public int getPlaceholder_player_active_quests_list_vertical_limit() {
    return placeholder_player_active_quests_list_vertical_limit;
  }

  public void setPlaceholder_player_active_quests_list_vertical_limit(
      int placeholder_player_active_quests_list_vertical_limit) {
    this.placeholder_player_active_quests_list_vertical_limit =
        placeholder_player_active_quests_list_vertical_limit;
  }

  public boolean isPlaceholder_player_active_quests_list_horizontal_use_displayname_if_available() {
    return placeholder_player_active_quests_list_horizontal_use_displayname_if_available;
  }

  public void setPlaceholder_player_active_quests_list_horizontal_use_displayname_if_available(
      boolean placeholder_player_active_quests_list_horizontal_use_displayname_if_available) {
    this.placeholder_player_active_quests_list_horizontal_use_displayname_if_available =
        placeholder_player_active_quests_list_horizontal_use_displayname_if_available;
  }

  public boolean isPlaceholder_player_active_quests_list_vertical_use_displayname_if_available() {
    return placeholder_player_active_quests_list_vertical_use_displayname_if_available;
  }

  public void setPlaceholder_player_active_quests_list_vertical_use_displayname_if_available(
      boolean placeholder_player_active_quests_list_vertical_use_displayname_if_available) {
    this.placeholder_player_active_quests_list_vertical_use_displayname_if_available =
        placeholder_player_active_quests_list_vertical_use_displayname_if_available;
  }

  public int getMaxActiveQuestsPerPlayer() {
    return maxActiveQuestsPerPlayer;
  }

  public void setMaxActiveQuestsPerPlayer(int maxActiveQuestsPerPlayer) {
    this.maxActiveQuestsPerPlayer = maxActiveQuestsPerPlayer;
  }

  public boolean isArmorStandPreventEditing() {
    return armorStandPreventEditing;
  }

  public void setArmorStandPreventEditing(boolean armorStandPreventEditing) {
    this.armorStandPreventEditing = armorStandPreventEditing;
  }

  public List<String> getJournalItemEnabledWorlds() {
    return journalItemEnabledWorlds;
  }

  public void setJournalItemEnabledWorlds(List<String> journalItemEnabledWorlds) {
    this.journalItemEnabledWorlds = journalItemEnabledWorlds;
  }

  public int getJournalInventorySlot() {
    return journalInventorySlot;
  }

  public void setJournalInventorySlot(int journalInventorySlot) {
    this.journalInventorySlot = journalInventorySlot;
  }

  public ItemStack getJournalItem() {
    return journalItem;
  }

  public void setJournalItem(ItemStack journalItem) {
    this.journalItem = journalItem;
  }

  public boolean isPacketMagic() {
    return packetMagic;
  }

  public void setPacketMagic(boolean packetMagic) {
    this.packetMagic = packetMagic;
  }

  public boolean isUsePacketEvents() {
    return usePacketEvents;
  }

  public void setUsePacketEvents(boolean usePacketEvents) {
    this.usePacketEvents = usePacketEvents;
  }

  public boolean isPacketMagicUnsafeDisregardVersion() {
    return packetMagicUnsafeDisregardVersion;
  }

  public void setPacketMagicUnsafeDisregardVersion(boolean packetMagicUnsafeDisregardVersion) {
    this.packetMagicUnsafeDisregardVersion = packetMagicUnsafeDisregardVersion;
  }

  public boolean isDeletePreviousConversations() {
    return deletePreviousConversations;
  }

  public void setDeletePreviousConversations(boolean deletePreviousConversations) {
    this.deletePreviousConversations = deletePreviousConversations;
  }

  public int getPreviousConversationsHistorySize() {
    return previousConversationsHistorySize;
  }

  public void setPreviousConversationsHistorySize(int previousConversationsHistorySize) {
    this.previousConversationsHistorySize = previousConversationsHistorySize;
  }

  public boolean isUpdateCheckerNotifyOpsInChat() {
    return updateCheckerNotifyOpsInChat;
  }

  public void setUpdateCheckerNotifyOpsInChat(boolean updateCheckerNotifyOpsInChat) {
    this.updateCheckerNotifyOpsInChat = updateCheckerNotifyOpsInChat;
  }

  public boolean isCitizensFocusingEnabled() {
    return this.citizensFocusingEnabled;
  }

  public void setCitizensFocusingEnabled(boolean citizensFocusingEnabled) {
    this.citizensFocusingEnabled = citizensFocusingEnabled;
  }

  public int getCitizensFocusingRotateTime() {
    return this.citizensFocusingRotateTime;
  }

  public void setCitizensFocusingRotateTime(int citizensFocusingRotateTime) {
    this.citizensFocusingRotateTime = citizensFocusingRotateTime;
  }

  public boolean isCitizensFocusingCancelConversationWhenTooFar() {
    return this.citizensFocusingCancelConversationWhenTooFar;
  }

  public void setCitizensFocusingCancelConversationWhenTooFar(boolean citizensFocusingCancelConversationWhenTooFar) {
    this.citizensFocusingCancelConversationWhenTooFar = citizensFocusingCancelConversationWhenTooFar;
  }

  public int getCitizensNPCQuestGiverIndicatorParticleSpawnInterval() {
    return citizensNPCQuestGiverIndicatorParticleSpawnInterval;
  }

  public void setCitizensNPCQuestGiverIndicatorParticleSpawnInterval(
      int citizensNPCQuestGiverIndicatorParticleSpawnInterval) {
    this.citizensNPCQuestGiverIndicatorParticleSpawnInterval =
        citizensNPCQuestGiverIndicatorParticleSpawnInterval;
  }

  public int getCitizensNPCQuestGiverIndicatorParticleCount() {
    return citizensNPCQuestGiverIndicatorParticleCount;
  }

  public void setCitizensNPCQuestGiverIndicatorParticleCount(
      int citizensNPCQuestGiverIndicatorParticleCount) {
    this.citizensNPCQuestGiverIndicatorParticleCount = citizensNPCQuestGiverIndicatorParticleCount;
  }

  public Particle getCitizensNPCQuestGiverIndicatorParticleType() {
    return citizensNPCQuestGiverIndicatorParticleType;
  }

  public String getCitizensNPCQuestGiverIndicatorText() {
    return citizensNPCQuestGiverIndicatorText;
  }

  public void setCitizensNPCQuestGiverIndicatorParticleType(
      Particle citizensNPCQuestGiverIndicatorParticleType) {
    this.citizensNPCQuestGiverIndicatorParticleType = citizensNPCQuestGiverIndicatorParticleType;
  }
  public void setCitizensNPCQuestGiverIndicatorText(
          String citizensNPCQuestGiverIndicatorIndicatorText) {
    this.citizensNPCQuestGiverIndicatorText = citizensNPCQuestGiverIndicatorIndicatorText;
  }
  public void setCitizensNPCQuestGiverIndicatorTextInterval(
          int citizensNPCQuestGiverIndicatorIndicatorTextInterval) {
    this.citizensNPCQuestGiverIndicatorTextInterval = citizensNPCQuestGiverIndicatorIndicatorTextInterval;
  }
  public int getCitizensNPCQuestGiverIndicatorTextInterval() {
    return citizensNPCQuestGiverIndicatorTextInterval;
  }

  public boolean isCitizensNPCQuestGiverIndicatorParticleEnabled() {
    return citizensNPCQuestGiverIndicatorParticleEnabled;
  }

  public void setCitizensNPCQuestGiverIndicatorParticleEnabled(
      boolean citizensNPCQuestGiverIndicatorParticleEnabled) {
    this.citizensNPCQuestGiverIndicatorParticleEnabled =
        citizensNPCQuestGiverIndicatorParticleEnabled;
  }

  public double getCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow() {
    return citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow;
  }

  public void setCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow(
      double citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow) {
    this.citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow =
        citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow;
  }

  public int getArmorStandQuestGiverIndicatorParticleSpawnInterval() {
    return armorStandQuestGiverIndicatorParticleSpawnInterval;
  }

  public void setArmorStandQuestGiverIndicatorParticleSpawnInterval(
      int armorStandQuestGiverIndicatorParticleSpawnInterval) {
    this.armorStandQuestGiverIndicatorParticleSpawnInterval =
        armorStandQuestGiverIndicatorParticleSpawnInterval;
  }

  public int getArmorStandQuestGiverIndicatorParticleCount() {
    return armorStandQuestGiverIndicatorParticleCount;
  }

  public void setArmorStandQuestGiverIndicatorParticleCount(
      int armorStandQuestGiverIndicatorParticleCount) {
    this.armorStandQuestGiverIndicatorParticleCount = armorStandQuestGiverIndicatorParticleCount;
  }

  public Particle getArmorStandQuestGiverIndicatorParticleType() {
    return armorStandQuestGiverIndicatorParticleType;
  }

  public void setArmorStandQuestGiverIndicatorParticleType(
      Particle armorStandQuestGiverIndicatorParticleType) {
    this.armorStandQuestGiverIndicatorParticleType = armorStandQuestGiverIndicatorParticleType;
  }

  public boolean isArmorStandQuestGiverIndicatorParticleEnabled() {
    return armorStandQuestGiverIndicatorParticleEnabled;
  }

  public void setArmorStandQuestGiverIndicatorParticleEnabled(
      boolean armorStandQuestGiverIndicatorParticleEnabled) {
    this.armorStandQuestGiverIndicatorParticleEnabled =
        armorStandQuestGiverIndicatorParticleEnabled;
  }

  public double getArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow() {
    return armorStandQuestGiverIndicatorParticleDisableIfTPSBelow;
  }

  public void setArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow(
      double armorStandQuestGiverIndicatorParticleDisableIfTPSBelow) {
    this.armorStandQuestGiverIndicatorParticleDisableIfTPSBelow =
        armorStandQuestGiverIndicatorParticleDisableIfTPSBelow;
  }

  public String getLanguageCode() {
    return languageCode;
  }

  public void setLanguageCode(String languageCode) {
    this.languageCode = languageCode;
  }

  public boolean isIntegrationCitizensEnabled() {
    return integrationCitizensEnabled;
  }

  public void setIntegrationCitizensEnabled(boolean integrationCitizensEnabled) {
    this.integrationCitizensEnabled = integrationCitizensEnabled;
  }

  public boolean isIntegrationVaultEnabled() {
    return integrationVaultEnabled;
  }

  public void setIntegrationVaultEnabled(boolean integrationVaultEnabled) {
    this.integrationVaultEnabled = integrationVaultEnabled;
  }

  public boolean isIntegrationPlaceholderAPIEnabled() {
    return integrationPlaceholderAPIEnabled;
  }

  public void setIntegrationPlaceholderAPIEnabled(boolean integrationPlaceholderAPIEnabled) {
    this.integrationPlaceholderAPIEnabled = integrationPlaceholderAPIEnabled;
  }

  public boolean isIntegrationMythicMobsEnabled() {
    return integrationMythicMobsEnabled;
  }

  public void setIntegrationMythicMobsEnabled(boolean integrationMythicMobsEnabled) {
    this.integrationMythicMobsEnabled = integrationMythicMobsEnabled;
  }

  public boolean isIntegrationEliteMobsEnabled() {
    return integrationEliteMobsEnabled;
  }

  public void setIntegrationEliteMobsEnabled(boolean integrationEliteMobsEnabled) {
    this.integrationEliteMobsEnabled = integrationEliteMobsEnabled;
  }

  public boolean isIntegrationBetonQuestEnabled() {
    return integrationBetonQuestEnabled;
  }

  public void setIntegrationBetonQuestEnabled(boolean integrationBetonQuestEnabled) {
    this.integrationBetonQuestEnabled = integrationBetonQuestEnabled;
  }

  public boolean isIntegrationWorldEditEnabled() {
    return integrationWorldEditEnabled;
  }

  public void setIntegrationWorldEditEnabled(boolean integrationWorldEditEnabled) {
    this.integrationWorldEditEnabled = integrationWorldEditEnabled;
  }

  public boolean isIntegrationSlimeFunEnabled() {
    return integrationSlimeFunEnabled;
  }

  public void setIntegrationSlimeFunEnabled(boolean integrationSlimeFunEnabled) {
    this.integrationSlimeFunEnabled = integrationSlimeFunEnabled;
  }

  public boolean isIntegrationLuckPermsEnabled() {
    return integrationLuckPermsEnabled;
  }

  public void setIntegrationLuckPermsEnabled(boolean integrationLuckPermsEnabled) {
    this.integrationLuckPermsEnabled = integrationLuckPermsEnabled;
  }

  public boolean isIntegrationUltimateClansEnabled() {
    return integrationUltimateClansEnabled;
  }

  public void setIntegrationUltimateClansEnabled(boolean integrationUltimateClansEnabled) {
    this.integrationUltimateClansEnabled = integrationUltimateClansEnabled;
  }

  public boolean isIntegrationTownyEnabled() {
    return integrationTownyEnabled;
  }

  public void setIntegrationTownyEnabled(boolean integrationTownyEnabled) {
    this.integrationTownyEnabled = integrationTownyEnabled;
  }

  public boolean isIntegrationJobsRebornEnabled() {
    return integrationJobsRebornEnabled;
  }

  public void setIntegrationJobsRebornEnabled(boolean integrationJobsRebornEnabled) {
    this.integrationJobsRebornEnabled = integrationJobsRebornEnabled;
  }

  public boolean isIntegrationProjectKorraEnabled() {
    return integrationProjectKorraEnabled;
  }

  public void setIntegrationProjectKorraEnabled(boolean integrationProjectKorraEnabled) {
    this.integrationProjectKorraEnabled = integrationProjectKorraEnabled;
  }

  public boolean isIntegrationEcoBossesEnabled() {
    return integrationEcoBossesEnabled;
  }

  public void setIntegrationEcoBossesEnabled(boolean integrationEcoBossesEnabled) {
    this.integrationEcoBossesEnabled = integrationEcoBossesEnabled;
  }

  public boolean isIntegrationUltimateJobsEnabled() {
    return integrationUltimateJobsEnabled;
  }

  public void setIntegrationUltimateJobsEnabled(boolean integrationUltimateJobsEnabled) {
    this.integrationUltimateJobsEnabled = integrationUltimateJobsEnabled;
  }

  public boolean isActionBarFancyCommandCompletionEnabled() {
    return actionBarFancyCommandCompletionEnabled;
  }

  public void setActionBarFancyCommandCompletionEnabled(
      boolean actionBarFancyCommandCompletionEnabled) {
    this.actionBarFancyCommandCompletionEnabled = actionBarFancyCommandCompletionEnabled;
  }

  public boolean isTitleFancyCommandCompletionEnabled() {
    return titleFancyCommandCompletionEnabled;
  }

  public void setTitleFancyCommandCompletionEnabled(boolean titleFancyCommandCompletionEnabled) {
    this.titleFancyCommandCompletionEnabled = titleFancyCommandCompletionEnabled;
  }

  public boolean isBossBarFancyCommandCompletionEnabled() {
    return bossBarFancyCommandCompletionEnabled;
  }

  public void setBossBarFancyCommandCompletionEnabled(
      boolean bossBarFancyCommandCompletionEnabled) {
    this.bossBarFancyCommandCompletionEnabled = bossBarFancyCommandCompletionEnabled;
  }

  public int getFancyCommandCompletionMaxPreviousArgumentsDisplayed() {
    return fancyCommandCompletionMaxPreviousArgumentsDisplayed;
  }

  public void setFancyCommandCompletionMaxPreviousArgumentsDisplayed(
      int fancyCommandCompletionMaxPreviousArgumentsDisplayed) {
    this.fancyCommandCompletionMaxPreviousArgumentsDisplayed =
        fancyCommandCompletionMaxPreviousArgumentsDisplayed;
  }

  public boolean isMoveEventEnabled() {
    return moveEventEnabled;
  }

  public void setMoveEventEnabled(boolean moveEventEnabled) {
    this.moveEventEnabled = moveEventEnabled;
  }

  public boolean isGuiQuestPreviewDescription_enabled() {
    return guiQuestPreviewDescription_enabled;
  }

  public void setGuiQuestPreviewDescription_enabled(boolean guiQuestPreviewDescription_enabled) {
    this.guiQuestPreviewDescription_enabled = guiQuestPreviewDescription_enabled;
  }

  public boolean isGuiQuestPreviewRewards_enabled() {
    return guiQuestPreviewRewards_enabled;
  }

  public void setGuiQuestPreviewRewards_enabled(boolean guiQuestPreviewRewards_enabled) {
    this.guiQuestPreviewRewards_enabled = guiQuestPreviewRewards_enabled;
  }

  public boolean isGuiQuestPreviewRequirements_enabled() {
    return guiQuestPreviewRequirements_enabled;
  }

  public void setGuiQuestPreviewRequirements_enabled(boolean guiQuestPreviewRequirements_enabled) {
    this.guiQuestPreviewRequirements_enabled = guiQuestPreviewRequirements_enabled;
  }

  public boolean isShowQuestItemAmount() {
    return showQuestItemAmount;
  }

  public void setShowQuestItemAmount(boolean showQuestItemAmount) {
    this.showQuestItemAmount = showQuestItemAmount;
  }

  public boolean isShowObjectiveItemAmount() {
    return showObjectiveItemAmount;
  }

  public void setShowObjectiveItemAmount(boolean showObjectiveItemAmount) {
    this.showObjectiveItemAmount = showObjectiveItemAmount;
  }

  public boolean isVisualTitleQuestSuccessfullyAccepted_enabled() {
    return visualTitleQuestSuccessfullyAccepted_enabled;
  }

  public void setVisualTitleQuestSuccessfullyAccepted_enabled(
      boolean visualTitleQuestSuccessfullyAccepted_enabled) {
    this.visualTitleQuestSuccessfullyAccepted_enabled =
        visualTitleQuestSuccessfullyAccepted_enabled;
  }

  public boolean isVisualTitleQuestFailed_enabled() {
    return visualTitleQuestFailed_enabled;
  }

  public void setVisualTitleQuestFailed_enabled(boolean visualTitleQuestFailed_enabled) {
    this.visualTitleQuestFailed_enabled = visualTitleQuestFailed_enabled;
  }

  public boolean isVisualTitleQuestCompleted_enabled() {
    return visualTitleQuestCompleted_enabled;
  }

  public void setVisualTitleQuestCompleted_enabled(boolean visualTitleQuestCompleted_enabled) {
    this.visualTitleQuestCompleted_enabled = visualTitleQuestCompleted_enabled;
  }

  public boolean isSupportPlaceholderAPIInTranslationStrings() {
    return supportPlaceholderAPIInTranslationStrings;
  }

  public void setSupportPlaceholderAPIInTranslationStrings(
      boolean supportPlaceholderAPIInTranslationStrings) {
    this.supportPlaceholderAPIInTranslationStrings = supportPlaceholderAPIInTranslationStrings;
  }

  public int getGuiQuestDescriptionMaxLineLength() {
    return guiQuestDescriptionMaxLineLength;
  }

  public void setGuiQuestDescriptionMaxLineLength(int guiQuestDescriptionMaxLineLength) {
    this.guiQuestDescriptionMaxLineLength = guiQuestDescriptionMaxLineLength;
  }

  public int getGuiObjectiveDescriptionMaxLineLength() {
    return guiObjectiveDescriptionMaxLineLength;
  }

  public void setGuiObjectiveDescriptionMaxLineLength(int guiObjectiveDescriptionMaxLineLength) {
    this.guiObjectiveDescriptionMaxLineLength = guiObjectiveDescriptionMaxLineLength;
  }

  public boolean isWrapLongWords() {
    return wrapLongWords;
  }

  public void setWrapLongWords(boolean wrapLongWords) {
    this.wrapLongWords = wrapLongWords;
  }

  public boolean isHideRewardsWithoutName() {
    return hideRewardsWithoutName;
  }

  public void setHideRewardsWithoutName(boolean hideRewardsWithoutName) {
    this.hideRewardsWithoutName = hideRewardsWithoutName;
  }

  public boolean isShowRewardsAfterQuestCompletion() {
    return showRewardsAfterQuestCompletion;
  }

  public void setShowRewardsAfterQuestCompletion(boolean showRewardsAfterQuestCompletion) {
    this.showRewardsAfterQuestCompletion = showRewardsAfterQuestCompletion;
  }

  public boolean isShowRewardsAfterObjectiveCompletion() {
    return showRewardsAfterObjectiveCompletion;
  }

  public void setShowRewardsAfterObjectiveCompletion(boolean showRewardsAfterObjectiveCompletion) {
    this.showRewardsAfterObjectiveCompletion = showRewardsAfterObjectiveCompletion;
  }

  public List<String> getColorsMain() {
    return colorsMain;
  }

  public void setColorsMain(List<String> colorsMain) {
    this.colorsMain = colorsMain;
  }

  public List<String> getColorsHighlight() {
    return colorsHighlight;
  }

  public void setColorsHighlight(List<String> colorsHighlight) {
    this.colorsHighlight = colorsHighlight;
  }

  public List<String> getColorsHighlight2() {
    return colorsHighlight2;
  }

  public void setColorsHighlight2(List<String> colorsHighlight2) {
    this.colorsHighlight2 = colorsHighlight2;
  }

  public List<String> getColorsError() {
    return colorsError;
  }

  public void setColorsError(List<String> colorsError) {
    this.colorsError = colorsError;
  }

  public List<String> getColorsSuccess() {
    return colorsSuccess;
  }

  public void setColorsSuccess(List<String> colorsSuccess) {
    this.colorsSuccess = colorsSuccess;
  }

  public List<String> getColorsUnimportant() {
    return colorsUnimportant;
  }

  public void setColorsUnimportant(List<String> colorsUnimportant) {
    this.colorsUnimportant = colorsUnimportant;
  }

  public List<String> getColorsVeryUnimportant() {
    return colorsVeryUnimportant;
  }

  public void setColorsVeryUnimportant(List<String> colorsVeryUnimportant) {
    this.colorsVeryUnimportant = colorsVeryUnimportant;
  }

  public List<String> getColorsWarn() {
    return colorsWarn;
  }

  public void setColorsWarn(List<String> colorsWarn) {
    this.colorsWarn = colorsWarn;
  }

  public List<String> getColorsPositive() {
    return colorsPositive;
  }

  public void setColorsPositive(List<String> colorsPositive) {
    this.colorsPositive = colorsPositive;
  }

  public List<String> getColorsNegative() {
    return colorsNegative;
  }

  public void setColorsNegative(List<String> colorsNegative) {
    this.colorsNegative = colorsNegative;
  }

  public String getColorsConsoleInfoDefault() {
    return colorsConsoleInfoDefault;
  }

  public void setColorsConsoleInfoDefault(String colorsConsoleInfoDefault) {
    this.colorsConsoleInfoDefault = colorsConsoleInfoDefault;
  }

  public String getColorsConsoleInfoData() {
    return colorsConsoleInfoData;
  }

  public void setColorsConsoleInfoData(String colorsConsoleInfoData) {
    this.colorsConsoleInfoData = colorsConsoleInfoData;
  }

  public String getColorsConsoleInfoLanguage() {
    return colorsConsoleInfoLanguage;
  }

  public void setColorsConsoleInfoLanguage(String colorsConsoleInfoLanguage) {
    this.colorsConsoleInfoLanguage = colorsConsoleInfoLanguage;
  }

  public String getColorsConsoleWarnDefault() {
    return colorsConsoleWarnDefault;
  }

  public void setColorsConsoleWarnDefault(String colorsConsoleWarnDefault) {
    this.colorsConsoleWarnDefault = colorsConsoleWarnDefault;
  }

  public String getColorsConsoleSevereDefault() {
    return colorsConsoleSevereDefault;
  }

  public void setColorsConsoleSevereDefault(String colorsConsoleSevereDefault) {
    this.colorsConsoleSevereDefault = colorsConsoleSevereDefault;
  }

  public String getColorsConsoleDebugDefault() {
    return colorsConsoleDebugDefault;
  }

  public void setColorsConsoleDebugDefault(String colorsConsoleDebugDefault) {
    this.colorsConsoleDebugDefault = colorsConsoleDebugDefault;
  }

  public String getColorsConsolePrefixPrefix() {
    return colorsConsolePrefixPrefix;
  }

  public void setColorsConsolePrefixPrefix(String colorsConsolePrefixPrefix) {
    this.colorsConsolePrefixPrefix = colorsConsolePrefixPrefix;
  }

  public String getColorsConsolePrefixSuffix() {
    return colorsConsolePrefixSuffix;
  }

  public void setColorsConsolePrefixSuffix(String colorsConsolePrefixSuffix) {
    this.colorsConsolePrefixSuffix = colorsConsolePrefixSuffix;
  }

  public boolean isConsoleColorsEnabled() {
    return consoleColorsEnabled;
  }

  public void setConsoleColorsEnabled(boolean consoleColorsEnabled) {
    this.consoleColorsEnabled = consoleColorsEnabled;
  }

  public boolean isConsoleColorsDownsampleColors() {
    return consoleColorsDownsampleColors;
  }

  public void setConsoleColorsDownsampleColors(boolean consoleColorsDownsampleColors) {
    this.consoleColorsDownsampleColors = consoleColorsDownsampleColors;
  }

  public String getColorsConsoleInfoDefaultDownsampled() {
    return colorsConsoleInfoDefaultDownsampled;
  }

  public void setColorsConsoleInfoDefaultDownsampled(String colorsConsoleInfoDefaultDownsampled) {
    this.colorsConsoleInfoDefaultDownsampled = colorsConsoleInfoDefaultDownsampled;
  }

  public String getColorsConsoleInfoDataDownsampled() {
    return colorsConsoleInfoDataDownsampled;
  }

  public void setColorsConsoleInfoDataDownsampled(String colorsConsoleInfoDataDownsampled) {
    this.colorsConsoleInfoDataDownsampled = colorsConsoleInfoDataDownsampled;
  }

  public String getColorsConsoleInfoLanguageDownsampled() {
    return colorsConsoleInfoLanguageDownsampled;
  }

  public void setColorsConsoleInfoLanguageDownsampled(
      String colorsConsoleInfoLanguageDownsampled) {
    this.colorsConsoleInfoLanguageDownsampled = colorsConsoleInfoLanguageDownsampled;
  }

  public String getColorsConsoleWarnDefaultDownsampled() {
    return colorsConsoleWarnDefaultDownsampled;
  }

  public void setColorsConsoleWarnDefaultDownsampled(String colorsConsoleWarnDefaultDownsampled) {
    this.colorsConsoleWarnDefaultDownsampled = colorsConsoleWarnDefaultDownsampled;
  }

  public String getColorsConsoleSevereDefaultDownsampled() {
    return colorsConsoleSevereDefaultDownsampled;
  }

  public void setColorsConsoleSevereDefaultDownsampled(
      String colorsConsoleSevereDefaultDownsampled) {
    this.colorsConsoleSevereDefaultDownsampled = colorsConsoleSevereDefaultDownsampled;
  }

  public String getColorsConsoleDebugDownsampled() {
    return colorsConsoleDebugDownsampled;
  }

  public void setColorsConsoleDebugDownsampled(String colorsConsoleDebugDownsampled) {
    this.colorsConsoleDebugDownsampled = colorsConsoleDebugDownsampled;
  }


  public boolean isObjectiveUnlockConditionsCheckOnAnyAction() {
    return objectiveUnlockConditionsCheckOnAnyAction;
  }

  public void setObjectiveUnlockConditionsCheckOnAnyAction(boolean objectiveUnlockConditionsCheckOnAnyAction) {
    this.objectiveUnlockConditionsCheckOnAnyAction = objectiveUnlockConditionsCheckOnAnyAction;
  }

  public int getObjectiveUnlockConditionsCheckRegularInterval() {
    return objectiveUnlockConditionsCheckRegularInterval;
  }

  public void setObjectiveUnlockConditionsCheckRegularInterval(int objectiveUnlockConditionsCheckRegularInterval) {
    this.objectiveUnlockConditionsCheckRegularInterval = objectiveUnlockConditionsCheckRegularInterval;
  }
}
