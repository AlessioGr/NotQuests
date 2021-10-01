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

package notquests.notquests.Managers;

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


    //Other
    private boolean actionBarCommandCompletionEnabled = true;
    private int actionBarCommandCompletionMaxPreviousArgumentsDisplayed = 2;


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

    public final boolean isActionBarCommandCompletionEnabled() {
        return actionBarCommandCompletionEnabled;
    }

    public void setActionBarCommandCompletionEnabled(final boolean actionBarCommandCompletionEnabled) {
        this.actionBarCommandCompletionEnabled = actionBarCommandCompletionEnabled;
    }

    public final int getActionBarCommandCompletionMaxPreviousArgumentsDisplayed() {
        return actionBarCommandCompletionMaxPreviousArgumentsDisplayed;
    }

    public void setActionBarCommandCompletionMaxPreviousArgumentsDisplayed(final int actionBarCommandCompletionMaxPreviousArgumentsDisplayed) {
        this.actionBarCommandCompletionMaxPreviousArgumentsDisplayed = actionBarCommandCompletionMaxPreviousArgumentsDisplayed;
    }
}
