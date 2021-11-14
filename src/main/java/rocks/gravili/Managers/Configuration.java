/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.Managers;

import org.bukkit.Particle;

/**
 * This is the Configuration Class which contains the settings which can be configured in the General.conf
 *
 * @author Alessio Gravili
 */
public class Configuration {

    /**
     * MYSQL Database Connection Information
     */
    private String host, database, username, password;
    /**
     * MYSQL Database Connection Information
     */
    private int port;

    private boolean questPreviewUseGUI = true;
    private boolean userCommandsUseGUI = true;
    private boolean mySQLEnabled = false;

    public String placeholder_player_active_quests_list_horizontal_separator = " | ";

    public int placeholder_player_active_quests_list_horizontal_limit = -1;
    public int placeholder_player_active_quests_list_vertical_limit = -1;

    public boolean placeholder_player_active_quests_list_horizontal_use_displayname_if_available = true;
    public boolean placeholder_player_active_quests_list_vertical_use_displayname_if_available = true;

    private int maxActiveQuestsPerPlayer = -1;


    private boolean armorStandPreventEditing = true;


    //Particles
    private int citizensNPCQuestGiverIndicatorParticleSpawnInterval = 10;
    private int citizensNPCQuestGiverIndicatorParticleCount = 1;
    private Particle citizensNPCQuestGiverIndicatorParticleType = Particle.VILLAGER_ANGRY;
    private boolean citizensNPCQuestGiverIndicatorParticleEnabled = true;
    private double citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow = -1;

    private int armorStandQuestGiverIndicatorParticleSpawnInterval = 10;
    private int armorStandQuestGiverIndicatorParticleCount = 1;
    private Particle armorStandQuestGiverIndicatorParticleType = Particle.VILLAGER_ANGRY;
    private boolean armorStandQuestGiverIndicatorParticleEnabled = true;
    private double armorStandQuestGiverIndicatorParticleDisableIfTPSBelow = -1;


    private String languageCode = "en";

    //Integrations
    private boolean integrationCitizensEnabled = true;
    private boolean integrationVaultEnabled = true;
    private boolean integrationPlaceholderAPIEnabled = true;
    private boolean integrationMythicMobsEnabled = true;
    private boolean integrationEliteMobsEnabled = true;
    private boolean integrationBetonQuestEnabled = true;
    private boolean integrationWorldEditEnabled = true;


    //Other
    private boolean actionBarFancyCommandCompletionEnabled = true;
    private boolean titleFancyCommandCompletionEnabled = false;
    private boolean bossBarFancyCommandCompletionEnabled = false;
    private int fancyCommandCompletionMaxPreviousArgumentsDisplayed = 2;
    private boolean moveEventEnabled = true;


    //GUI
    private boolean guiQuestPreviewDescription_enabled = true;
    private boolean guiQuestPreviewRewards_enabled = true;
    private boolean guiQuestPreviewRequirements_enabled = true;
    private char guiQuestPreviewDescription_slot = '1';
    private char guiQuestPreviewRewards_slot = '3';
    private char guiQuestPreviewRequirements_slot = '5';
    public boolean showQuestItemAmount = true;
    public boolean showObjectiveItemAmount = true;

    //Visual
    public boolean visualTitleQuestSuccessfullyAccepted_enabled = true;
    public boolean visualTitleQuestFailed_enabled = true;
    public boolean visualTitleQuestCompleted_enabled = true;


    public boolean supportPlaceholderAPIInTranslationStrings = false;


    public Configuration() {

    }

    public final String getDatabaseHost() {
        return host;
    }

    public void setDatabaseHost(final String host) {
        this.host = host;
    }

    public final int getDatabasePort() {
        return port;
    }

    public void setDatabasePort(final int port) {
        this.port = port;
    }

    public final String getDatabaseName() {
        return database;
    }

    public void setDatabaseName(final String database) {
        this.database = database;
    }

    public final String getDatabaseUsername() {
        return username;
    }

    public void setDatabaseUsername(final String username) {
        this.username = username;
    }

    public final String getDatabasePassword() {
        return password;
    }

    public void setDatabasePassword(final String password) {
        this.password = password;
    }

    public final boolean isQuestPreviewUseGUI() {
        return questPreviewUseGUI;
    }

    public void setQuestPreviewUseGUI(final boolean questPreviewUseGUI) {
        this.questPreviewUseGUI = questPreviewUseGUI;
    }

    public final boolean isMySQLEnabled(){
        return mySQLEnabled;
    }

    public void setMySQLEnabled(final boolean mySQLEnabled){
        this.mySQLEnabled = mySQLEnabled;
    }


    public final boolean isUserCommandsUseGUI() {
        return userCommandsUseGUI;
    }

    public void setUserCommandsUseGUI(final boolean userCommandsUseGUI) {
        this.userCommandsUseGUI = userCommandsUseGUI;
    }

    public final int getMaxActiveQuestsPerPlayer() {
        return maxActiveQuestsPerPlayer;
    }

    public void setMaxActiveQuestsPerPlayer(int maxActiveQuestsPerPlayer) {
        this.maxActiveQuestsPerPlayer = maxActiveQuestsPerPlayer;
    }


    //Particles Citizens
    public final int getCitizensNPCQuestGiverIndicatorParticleSpawnInterval() {
        return citizensNPCQuestGiverIndicatorParticleSpawnInterval;
    }

    public void setCitizensNPCQuestGiverIndicatorParticleSpawnInterval(final int citizensNPCQuestGiverIndicatorParticleSpawnInterval) {
        this.citizensNPCQuestGiverIndicatorParticleSpawnInterval = citizensNPCQuestGiverIndicatorParticleSpawnInterval;
    }

    public final Particle getCitizensNPCQuestGiverIndicatorParticleType() {
        return citizensNPCQuestGiverIndicatorParticleType;
    }

    public void setCitizensNPCQuestGiverIndicatorParticleType(final Particle citizensNPCQuestGiverIndicatorParticleType) {
        this.citizensNPCQuestGiverIndicatorParticleType = citizensNPCQuestGiverIndicatorParticleType;
    }

    public final int getCitizensNPCQuestGiverIndicatorParticleCount() {
        return citizensNPCQuestGiverIndicatorParticleCount;
    }

    public void setCitizensNPCQuestGiverIndicatorParticleCount(final int citizensNPCQuestGiverIndicatorParticleCount) {
        this.citizensNPCQuestGiverIndicatorParticleCount = citizensNPCQuestGiverIndicatorParticleCount;
    }

    public final boolean isCitizensNPCQuestGiverIndicatorParticleEnabled() {
        return citizensNPCQuestGiverIndicatorParticleEnabled;
    }

    public final void setCitizensNPCQuestGiverIndicatorParticleEnabled(boolean citizensNPCQuestGiverIndicatorParticleEnabled) {
        this.citizensNPCQuestGiverIndicatorParticleEnabled = citizensNPCQuestGiverIndicatorParticleEnabled;
    }

    public final double getCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow() {
        return citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow;
    }

    public final void setCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow(double disableIfTPSBelow) {
        this.citizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow = disableIfTPSBelow;
    }


    //Particles ArmorStands
    public final int getArmorStandQuestGiverIndicatorParticleSpawnInterval() {
        return armorStandQuestGiverIndicatorParticleSpawnInterval;
    }

    public void setArmorStandQuestGiverIndicatorParticleSpawnInterval(final int armorStandQuestGiverIndicatorParticleSpawnInterval) {
        this.armorStandQuestGiverIndicatorParticleSpawnInterval = armorStandQuestGiverIndicatorParticleSpawnInterval;
    }

    public final Particle getArmorStandQuestGiverIndicatorParticleType() {
        return armorStandQuestGiverIndicatorParticleType;
    }

    public void setArmorStandQuestGiverIndicatorParticleType(final Particle armorStandQuestGiverIndicatorParticleType) {
        this.armorStandQuestGiverIndicatorParticleType = armorStandQuestGiverIndicatorParticleType;
    }

    public final int getArmorStandQuestGiverIndicatorParticleCount() {
        return armorStandQuestGiverIndicatorParticleCount;
    }

    public void setArmorStandQuestGiverIndicatorParticleCount(final int armorStandQuestGiverIndicatorParticleCount) {
        this.armorStandQuestGiverIndicatorParticleCount = armorStandQuestGiverIndicatorParticleCount;
    }

    public final boolean isArmorStandQuestGiverIndicatorParticleEnabled() {
        return armorStandQuestGiverIndicatorParticleEnabled;
    }

    public final void setArmorStandQuestGiverIndicatorParticleEnabled(boolean armorStandQuestGiverIndicatorParticleEnabled) {
        this.armorStandQuestGiverIndicatorParticleEnabled = armorStandQuestGiverIndicatorParticleEnabled;
    }

    public final double getArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow() {
        return armorStandQuestGiverIndicatorParticleDisableIfTPSBelow;
    }

    public final void setArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow(double disableIfTPSBelow) {
        this.armorStandQuestGiverIndicatorParticleDisableIfTPSBelow = disableIfTPSBelow;
    }


    public final String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    public final boolean isArmorStandPreventEditing() {
        return armorStandPreventEditing;
    }

    public void setArmorStandPreventEditing(final boolean armorStandPreventEditing) {
        this.armorStandPreventEditing = armorStandPreventEditing;
    }


    //Integrations
    public final boolean isIntegrationCitizensEnabled() {
        return integrationCitizensEnabled;
    }

    public void setIntegrationCitizensEnabled(final boolean integrationCitizensEnabled) {
        this.integrationCitizensEnabled = integrationCitizensEnabled;
    }

    public final boolean isIntegrationVaultEnabled() {
        return integrationVaultEnabled;
    }

    public void setIntegrationVaultEnabled(final boolean integrationVaultEnabled) {
        this.integrationVaultEnabled = integrationVaultEnabled;
    }

    public final boolean isIntegrationPlaceholderAPIEnabled() {
        return integrationPlaceholderAPIEnabled;
    }

    public void setIntegrationPlaceholderAPIEnabled(final boolean integrationPlaceholderAPIEnabled) {
        this.integrationPlaceholderAPIEnabled = integrationPlaceholderAPIEnabled;
    }

    public final boolean isIntegrationMythicMobsEnabled() {
        return integrationMythicMobsEnabled;
    }

    public void setIntegrationMythicMobsEnabled(final boolean integrationMythicMobsEnabled) {
        this.integrationMythicMobsEnabled = integrationMythicMobsEnabled;
    }

    public final boolean isIntegrationEliteMobsEnabled() {
        return integrationEliteMobsEnabled;
    }

    public void setIntegrationEliteMobsEnabled(final boolean integrationEliteMobsEnabled) {
        this.integrationEliteMobsEnabled = integrationEliteMobsEnabled;
    }

    public final boolean isIntegrationBetonQuestEnabled() {
        return integrationBetonQuestEnabled;
    }

    public void setIntegrationBetonQuestEnabled(final boolean integrationBetonQuestEnabled) {
        this.integrationBetonQuestEnabled = integrationBetonQuestEnabled;
    }

    public final boolean isIntegrationWorldEditEnabled() {
        return integrationWorldEditEnabled;
    }

    public void setIntegrationWorldEditEnabled(final boolean integrationWorldEditEnabled) {
        this.integrationWorldEditEnabled = integrationWorldEditEnabled;
    }

    public final boolean isActionBarFancyCommandCompletionEnabled() {
        return actionBarFancyCommandCompletionEnabled;
    }

    public void setActionBarFancyCommandCompletionEnabled(final boolean actionBarFancyCommandCompletionEnabled) {
        this.actionBarFancyCommandCompletionEnabled = actionBarFancyCommandCompletionEnabled;
    }

    public final boolean isTitleFancyCommandCompletionEnabled() {
        return titleFancyCommandCompletionEnabled;
    }

    public void setTitleFancyCommandCompletionEnabled(final boolean titleFancyCommandCompletionEnabled) {
        this.titleFancyCommandCompletionEnabled = titleFancyCommandCompletionEnabled;
    }

    public final boolean isBossBarFancyCommandCompletionEnabled() {
        return bossBarFancyCommandCompletionEnabled;
    }

    public void setBossBarFancyCommandCompletionEnabled(final boolean bossBarFancyCommandCompletionEnabled) {
        this.bossBarFancyCommandCompletionEnabled = bossBarFancyCommandCompletionEnabled;
    }


    public final int getFancyCommandCompletionMaxPreviousArgumentsDisplayed() {
        return fancyCommandCompletionMaxPreviousArgumentsDisplayed;
    }

    public void setFancyCommandCompletionMaxPreviousArgumentsDisplayed(final int fancyCommandCompletionMaxPreviousArgumentsDisplayed) {
        this.fancyCommandCompletionMaxPreviousArgumentsDisplayed = fancyCommandCompletionMaxPreviousArgumentsDisplayed;
    }

    public final boolean isMoveEventEnabled() {
        return moveEventEnabled;
    }

    public void setMoveEventEnabled(final boolean moveEventEnabled) {
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

    public char getGuiQuestPreviewDescription_slot() {
        return guiQuestPreviewDescription_slot;
    }

    public void setGuiQuestPreviewDescription_slot(char guiQuestPreviewDescription_slot) {
        this.guiQuestPreviewDescription_slot = guiQuestPreviewDescription_slot;
    }

    public char getGuiQuestPreviewRewards_slot() {
        return guiQuestPreviewRewards_slot;
    }

    public void setGuiQuestPreviewRewards_slot(char guiQuestPreviewRewards_slot) {
        this.guiQuestPreviewRewards_slot = guiQuestPreviewRewards_slot;
    }

    public char getGuiQuestPreviewRequirements_slot() {
        return guiQuestPreviewRequirements_slot;
    }

    public void setGuiQuestPreviewRequirements_slot(char guiQuestPreviewRequirements_slot) {
        this.guiQuestPreviewRequirements_slot = guiQuestPreviewRequirements_slot;
    }
}
