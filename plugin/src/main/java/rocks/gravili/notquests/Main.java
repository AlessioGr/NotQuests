
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
import org.bukkit.plugin.java.JavaPlugin;


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
        if(PaperLib.isPaper()){
            notQuests = new rocks.gravili.notquests.paper.NotQuests(this);
            notQuests.onLoad();
        }else{
            notQuestsSpigot = new rocks.gravili.notquests.spigot.NotQuests(this);
            notQuestsSpigot.onLoad();
        }
    }

    public rocks.gravili.notquests.paper.NotQuests getNotQuests(){
        return notQuests;
    }
    public rocks.gravili.notquests.spigot.NotQuests getNotQuestsSpigot(){
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
        instance = this;
        if(notQuests != null){
            notQuests.onEnable();
        }else{
            notQuestsSpigot.onEnable();
        }
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

