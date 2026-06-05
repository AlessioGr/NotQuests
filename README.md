[![GitHub issues](https://img.shields.io/github/issues/AlessioGr/NotQuests)](https://github.com/AlessioGr/NotQuests/issues)
[![GitHub license](https://img.shields.io/github/license/AlessioGr/NotQuests)](https://github.com/AlessioGr/NotQuests/blob/main/LICENSE)
[![CodeFactor](https://www.codefactor.io/repository/github/alessiogr/notquests/badge)](https://www.codefactor.io/repository/github/alessiogr/notquests)

![Banner](https://user-images.githubusercontent.com/70709113/133943253-af271d49-441b-473e-8b95-6053fe5d09cb.png)

# NotQuests - Best Quest Plugin

Download: https://modrinth.com/plugin/notquests/versions

NotQuests allows you to create powerful quests for pretty much any type of server.

Please note: updates and bug fixes are not guaranteed, and support should not be expected.

- The latest version of NotQuests will be available **exclusively on Modrinth**, a platform I'm happy to support!
- NotQuests has reached more than **27.000 downloads** across all resource sites! Thank you!
- If you like NotQuests and want more updates, please give it a **follow** on modrinth to support me! It's just one click :)

![Front image](https://github.com/AlessioGr/NotQuests/blob/main/Logo/spigotbg.jpg?raw=true)

**Attention: Please use [Paper](https://papermc.io/downloads/paper) or a fork of Paper, like [Purpur](https://purpurmc.org/download/purpur) or [Pufferfish](https://pufferfish.host/downloads). Spigot/Bukkit is not supported.**

[=> Getting started guide <=](https://www.notquests.com/docs/tutorials/getting-started)

## Requirements to run NotQuests
- Minecraft 26.1.2 with Paper 26.1.2 (only the latest Minecraft version is actively developed. Older Minecraft versions only work with older NotQuests versions)
- Java 25

## Helpful links
- [Wiki](https://www.notquests.com/)
- [Getting started guide](https://www.notquests.com/docs/tutorials/getting-started/)
- [GitHub support](https://github.com/AlessioGr/NotQuests/issues) and [Discord support](https://discord.gg/7br638S5Ex)
- [Source code](https://github.com/AlessioGr/NotQuests)


## Integrations / optional plugins:
- MySQL Database - strongly recommended. Otherwise, SQLite will be used.
- Citizens - needed for Citizens NPC stuff to work. You can also use Armor Stands without Citizens, or FancyNPCs
- FancyNPCs - an alternative to Citizens
- Vault - for Economy stuff
- PlaceholderAPI - needed for placeholders
- MythicMobs - you can use MythicMobs in KillMobs objectives
- EliteMobs - to use the KillEliteMobs objective
- Slimefun - to use the SlimefunResearch objective
- LuckPerms - to use the Permission Reward
- Towny - to use various Requirements and Objectives
- Jobs Reborn
- EcoMobs
- Infinite other plugins through placeholder and command objectives/conditions/actions.

NotQuests is an incredibly easy-to-use, powerful and open-source minecraft Quests plugin for Paper which allows you to create detailed quests.

A powerful and flexible quest plugin does not have to be a pain to use. NotQuests goal is to take the simplicity of "easy" Quest plugins and the power & flexibility of more advanced plugins - and combine it!
It allows you to create powerful quests in a logical and simple way.

Some quest plugins want to be super powerful and flexible - but they are too hard to use. Some want to be easy-to-use, but they aren't powerful and only suitable for simple pump-and-dump quests.

NotQuests strives to be easy-to-use & intuitive while being super flexible & powerful at the same time! You can create both simple pump-and-dump farming quests, as well as complex omni-path storylines.

The command system is much faster, easier & flexible than what other Quest plugins have to offer, and I did not notice any performance issues either. Do note that one goal of NotQuests is to be modern. Until an abstract version is finished, do not expect for old Minecraft versions to be supported for ages. NotQuests is evolving quick!

## Features

- Proxy support (like Velocity, Waterfall & BungeeCord)
- **Custom YAML-based GUIs:** The entire GUI system is configurable via YAML files, supporting tabbed interfaces, paged item lists, action buttons, custom skull textures, and dynamic placeholders.
- **Quest Profiles:** Every player with the permission node "notquests.user.profiles" can create profiles in notquests! Each profile has their own quest points, tags active & completed quests etc. This would allow players to start over if they want to, in order to choose a different path, do a speedrun or just to experience your RPG again - or whatever else!
- Sub-objectives! Each objective can have unlimited sub-objectives. And each sub-objective can also have unlimited sub-sub-objectives.. and so on!
- Variable system for registering flexible Actions & Conditions at the same time
- Actions, Conditions, Objectives & Conditions are registered dynamically. This makes it easy to add your own via the API or by contributing.
- Tag System (save data per-player)
- Flexible Conversation system, tightly integrated with actions & conditions
- Multiple integrations with other plugins (if not, you can achieve a lot with Placeholder Conditions/Objectives & Console Command Actions)
- Great, extensive API
- Complete GUI for the User Interface
- Beautiful GUI & Chat Design
- Modern coloring using MiniMessage (legacy is unsupported)
- Player data is saved in MySQL or SQLite, Quest configuration data in a configuration file
- Quest Point System (You can create your own currency via tags)
- Smart Command Tab Completions for all User and Admin commands
- Translation System (https://translate.notquests.com/)
- Many usable Objectives, Conditions (= Requirements) and Actions (= Rewards). Full list of objectives: https://www.notquests.com/docs/documentation/types/objectives
- Flexible NPC system - this would allow you to easily integrate other NPC plugins with notquests in the future

## Sub-Objectives
![Sub Objectives](https://raw.githubusercontent.com/AlessioGr/NotQuests/main/Logo/assets/sub-objectives.png)


## PlaceholderAPI Placeholders

**Player Placeholders:**

%notquests_player_has_completed_quest_QUESTNAME%
%notquests_player_has_current_active_quest_QUESTNAME%
%notquests_player_is_objective_unlocked_and_active_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_is_objective_unlocked_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_is_objective_completed_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_questpoints%
%notquests_player_active_quests_list_horizontal%
%notquests_player_active_quests_list_vertical%
%notquests_player_completed_quests_amount%
%notquests_player_active_quests_amount%
%notquests_player_tag_TAGNAME%
%notquests_player_variable_VARIABLENAME%
%notquests_player_expression_EXPRESSION%
%notquests_player_rounded_expression_EXPRESSION%
%notquests_player_quest_cooldown_left_formatted_QUESTNAME%
