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

package rocks.gravili.notquests.paper.managers;

import lombok.Data;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * This is the Configuration Class which contains the settings which can be configured in the General.conf
 *
 * @author Alessio Gravili
 */
@Data
public class Configuration {

    private final String beamMode = "end_gateway"; //end_gateway, beacon, end_crystal


    public String configurationVersion = "";

    public boolean debug = false;

    /**
     * MYSQL Database Connection Information
     */
    private String databaseHost, databaseName, databaseUsername, databasePassword;
    private int databasePort;

    public boolean loadPlayerData = true;
    public boolean savePlayerData = true;


    private boolean questPreviewUseGUI = true;
    private boolean userCommandsUseGUI = true;
    private boolean mySQLEnabled = false;

    public boolean storageCreateBackupsWhenServerShutsDown = true;

    public String placeholder_player_active_quests_list_horizontal_separator = " | ";

    public int placeholder_player_active_quests_list_horizontal_limit = -1;
    public int placeholder_player_active_quests_list_vertical_limit = -1;

    public boolean placeholder_player_active_quests_list_horizontal_use_displayname_if_available = true;
    public boolean placeholder_player_active_quests_list_vertical_use_displayname_if_available = true;

    private int maxActiveQuestsPerPlayer = -1;


    private boolean armorStandPreventEditing = true;

    public List<String> journalItemEnabledWorlds;
    public int journalInventorySlot = 8;
    public ItemStack journalItem = null;


    public boolean packetMagic = false;
    public boolean usePacketEvents = false;
    public boolean packetMagicUnsafeDisregardVersion = false;
    public boolean deletePreviousConversations = false;
    public int previousConversationsHistorySize = 20;

    public boolean updateCheckerNotifyOpsInChat = true;


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
    private boolean integrationSlimeFunEnabled = true;
    private boolean integrationLuckPermsEnabled = true;
    private boolean integrationUltimateClansEnabled = true;
    private boolean integrationTownyEnabled = true;
    private boolean integrationJobsRebornEnabled = true;
    private boolean integrationProjectKorraEnabled = true;
    private boolean integrationEcoBossesEnabled = true;


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
    public boolean showQuestItemAmount = false;
    public boolean showObjectiveItemAmount = true;

    //Visual
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
}
