
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

package rocks.gravili.notquests;

import io.papermc.lib.PaperLib;
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


    @Override
    public void onLoad() {
        instance = this;
    }

    public rocks.gravili.notquests.paper.NotQuests getNotQuests(){
        return notQuests;
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
            getLogger().log(Level.SEVERE, "NotQuests version v5.15.0 or higher is no longer compatible with Spigot. In order to use Spigot, please use NotQuests v5.14.0 or lower. The reason for that is that Spigot is missing a lot of features that are required for NotQuests to work. Please use Paper instead. There is no reason to use Spigot. No good server nowadays uses Spigot. The only reason to use Spigot is because you don't know about Paper. No support is given to Spigot servers.");
            return;
        }

        if(notQuests != null){
            getLogger().log(Level.INFO, "Enabling NotQuests Paper...");
            notQuests.onEnable();
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
        }

    }


}

