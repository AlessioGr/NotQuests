
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

package rocks.gravili.notquests;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;


/**
 * This is the entry point of NotQuests. All kinds of managers, commands and other shit is reistered here.
 *
 * @author Alessio Gravili
 */
public final class Main extends JavaPlugin {

    private static Main instance;

    private rocks.gravili.notquests.paper.NotQuests notQuests;
    private rocks.gravili.notquests.spigot.NotQuests notQuestsSpigot;


    @Override
    public void onLoad() {
        instance = this;
    }

    public final rocks.gravili.notquests.paper.NotQuests getNotQuests(){
        return notQuests;
    }
    public final rocks.gravili.notquests.spigot.NotQuests getNotQuestsSpigot(){
        return notQuestsSpigot;
    }

    public static Main getInstance() {
        return instance;
    }


    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    @Override
    public void onEnable() {

        if(PaperLib.isPaper()){
            getLogger().log(Level.INFO, "Loading NotQuests Paper...");
            notQuests = new rocks.gravili.notquests.paper.NotQuests(instance);
            notQuests.onLoad();
        }else{
            getLogger().log(Level.INFO, "Loading NotQuests Spigot...");

            notQuestsSpigot = new rocks.gravili.notquests.spigot.NotQuests(instance);
            notQuestsSpigot.onLoad();
        }

        if(notQuests != null){
            getLogger().log(Level.INFO, "Enabling NotQuests Paper...");
            notQuests.onEnable();
        }else{
            getLogger().log(Level.INFO, "Enabling NotQuests Spigot...");
            notQuestsSpigot.onEnable();
        }

        /*getLogger().log(Level.INFO, "NotQuests has started. It will start loading in 5 seconds. Why the delay? Because spigot's load order system is broken. It does not work correctly. Without the delay, some integrations will stop working.");

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if(PaperLib.isPaper()){
                    getLogger().log(Level.INFO, "Loading NotQuests Paper...");
                    notQuests = new rocks.gravili.notquests.paper.NotQuests(instance);
                    notQuests.onLoad();
                }else{
                    getLogger().log(Level.INFO, "Loading NotQuests Spigot...");

                    notQuestsSpigot = new rocks.gravili.notquests.spigot.NotQuests(instance);
                    notQuestsSpigot.onLoad();
                }
            }
        }, 0L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if(notQuests != null){
                    getLogger().log(Level.INFO, "Enabling NotQuests Paper...");
                    notQuests.onEnable();
                }else{
                    getLogger().log(Level.INFO, "Enabling NotQuests Spigot...");
                    notQuestsSpigot.onEnable();
                }
            }
        }, 0L);
*/

    }

    /**
     * Called when the plugin is disabled or reloaded via ServerUtils / PlugMan
     */
    @Override
    public void onDisable() {
        if(notQuests != null){
            notQuests.onDisable();
        }else{
            notQuestsSpigot.onDisable();
        }

    }


}

