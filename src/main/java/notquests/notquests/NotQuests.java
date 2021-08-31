package notquests.notquests;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import notquests.notquests.Commands.CommandNotQuests;
import notquests.notquests.Commands.CommandNotQuestsAdmin;
import notquests.notquests.Events.QuestEvents;
import notquests.notquests.Managers.DataManager;
import notquests.notquests.Managers.QuestManager;
import notquests.notquests.Managers.QuestPlayerManager;
import notquests.notquests.Placeholders.QuestPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;


public final class NotQuests extends JavaPlugin {

    private DataManager dataManager;
    private QuestManager questManager;
    private QuestPlayerManager questPlayerManager;
    private Economy econ = null;
    private Permission perms = null;
    private Chat chat = null;


    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "§aNotQuests > NotQuests is starting...");


        if (!setupEconomy()) {
            getLogger().log(Level.SEVERE, String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));

            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();


        dataManager = new DataManager(this);
        questManager = new QuestManager(this);
        getDataManager().setAlreadyLoadedNPCs(false);
        questPlayerManager = new QuestPlayerManager(this);


        if (getServer().getPluginManager().getPlugin("Citizens") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
            getLogger().log(Level.SEVERE, "§cNotQuests > Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));


        CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(this);
        this.getCommand("notquestsadmin").setExecutor(commandNotQuestsAdmin);
        this.getCommand("notquestsadmin").setTabCompleter(commandNotQuestsAdmin);

        CommandNotQuests commandNotQuests = new CommandNotQuests(this);
        this.getCommand("notquests").setExecutor(commandNotQuests);
        this.getCommand("notquests").setTabCompleter(commandNotQuests);

        getServer().getPluginManager().registerEvents(new QuestEvents(this), this);

        dataManager.reloadData();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new QuestPlaceholders(this).register();
        }


    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "§aNotQuests > NotQuests is shutting down...");

        dataManager.saveData();
        getDataManager().setAlreadyLoadedNPCs(false);
        final ArrayList<Trait> traitsToRemove = new ArrayList<>();
        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
            for (final Trait trait : npc.getTraits()) {
                if (trait.getName().equalsIgnoreCase("nquestgiver")) {
                    traitsToRemove.add(trait);

                }
            }
            for (final Trait traitToRemove : traitsToRemove) {
                npc.removeTrait(traitToRemove.getClass());
                getLogger().log(Level.INFO, "§aNotQuests > Removed nquestgiver trait from NPC with the ID §b" + npc.getId());

            }

        }
        getLogger().log(Level.INFO, "§aNotQuests > Deregistering nquestgiver trait...");


        final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
        for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
            if (traitInfo.getTraitName().equals("nquestgiver")) {
                toDeregister.add(traitInfo);

            }
        }
        for (final TraitInfo traitInfo : toDeregister) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
        }

    }


    public final QuestManager getQuestManager() {
        return questManager;
    }

    public final QuestPlayerManager getQuestPlayerManager() {
        return questPlayerManager;
    }

    public final DataManager getDataManager() {
        return dataManager;
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public final Economy getEconomy() {
        return econ;
    }
}
