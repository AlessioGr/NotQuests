# NotQuests

[![GitHub issues](https://img.shields.io/github/issues/AlessioGr/NotQuests)](https://github.com/AlessioGr/NotQuests/issues)
[![GitHub stars](https://img.shields.io/github/stars/AlessioGr/NotQuests)](https://github.com/AlessioGr/NotQuests/stargazers)
[![GitHub license](https://img.shields.io/github/license/AlessioGr/NotQuests)](https://github.com/AlessioGr/NotQuests/blob/main/LICENSE)
<a href="https://quests.notnot.pro/"><img src="https://img.shields.io/badge/JavaDocs-Read%20now-yellow" alt="javadics"></a>

![Banner](https://user-images.githubusercontent.com/70709113/133943253-af271d49-441b-473e-8b95-6053fe5d09cb.png)

NotQuests is a flexible, Minecraft 1.17.1 Quest plugin, featuring a complete GUI for player interactions, open & trusted source code and flexibility.
---

I created NotQuests for my own server, mc.notnot.pro and planned to keep it private. However, I don't have enough time to maintain it myself, so I just made it public.

You can find the releases here: https://github.com/AlessioGr/NotQuests/releases. A MySQL database connection is recommended and can be specified in the general.yml. Otherwise, SQLite will be used.



![Banner_Images](https://user-images.githubusercontent.com/70709113/133997397-fbe14d0b-22fe-4ede-98e2-4d7a2cbcb489.png)

Quest Giver NPC:

![image](https://user-images.githubusercontent.com/70709113/131539574-ef73ddfb-1dcd-4ab8-a85c-9b38d2f95a8d.png)

What happens when you right click it:

![image](https://user-images.githubusercontent.com/70709113/131539663-5bd12479-3bc8-4958-81a2-de12a541820f.png)

Quest Accepting GUI:

![image](https://user-images.githubusercontent.com/70709113/131539715-b055e4cd-2a7e-4a78-8d7a-dc840399c0c0.png)

Quest User GUI:

![image](https://user-images.githubusercontent.com/70709113/131539761-66be66c2-26d9-4636-bbd5-d69fd11bfeaf.png)

Preview Quests GUI:

![image](https://user-images.githubusercontent.com/70709113/131539815-48c7de30-a5af-499c-b5c7-8316da5e186b.png)

Admin Commands (for quest creation):

![image](https://i.imgur.com/mAyL08w.png)

Admin Commands - Quest Editing:

![image](https://i.imgur.com/WKYvJ4V.png)



![Banner_Features](https://user-images.githubusercontent.com/70709113/133997198-bbc020b6-69c5-454b-b5b5-5f1bec0bab0d.png)
- Quests can be bound to Citizens NPCs
- **Quests can also be bound to Armor Stands ⇒ 10000x better performance because Citizens is a laggy plugin**. Especially if your armor stands are optimized in your paper configuration.
- Complete GUI for the user interface
- Player data is saved in MySQL, quest configuration data in a configuration file
- Quest Points
- Smart Command Tab Completions for all User and Admin commands
- Smart translation system
- Re-usable Actions for Triggers
- Multiple Quest Objectives:
  - Break Blocks
  - Collect Items
  - Consume Items
  - Craft Items
  - Deliver Items to NPC
  - Escort NPC
  - Kill Mobs
  - Complete Other Quest
  - Talk to NPC
  - Command Triggers (to finish the objective)
- Multiple Quest Accept Requirements:
  -  Money
  -  Other Quest
  -  Permission
  -  Quest Points
- Multiple Quest Completion Rewards:
  - Command
  - Quest Points
  - Item
  - Money
- Multiple Triggers: (Triggers run console commands (= Actions) when activated. Triggers are set per-quest)
  - Begin Quest or Objective
  - Complete Quest or Objective
  - Death
  - Disconnect
  - Fail Quest
  - NPC dies (good for Escort NPC Quests)
  - Enter World
  - Leave World
- There's a lot more, I'll add that later. Triggers are extremely flexible by the way. Each trigger has an option to trigger only for certain objectives if they are active, quests, or worlds.


**Requirements to run this:**
- Paper or Spigot 1.17.1 (Paper is recommended)
- Java 16


**Optional:**
- Citizens (Needed for Citizens NPC stuff to work. You can also use Armor Stands without Citizens, though)
- Vault
- MySQL Database (strongly recommended)
- PlaceholderAPI



![Banner_Placeholders](https://user-images.githubusercontent.com/70709113/133997368-44c2bdb3-4ad9-483b-b2db-a5221d3d8a5a.png)

Player Placeholders:

%notquests_player_has_completed_quest_QUESTNAME%
%notquests_player_has_current_active_quest_QUESTNAME%
%notquests_player_is_objective_unlocked_and_active_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_is_objective_unlocked_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_is_objective_completed_OBJECTIVEID_from_active_quest_QUESTNAME%
%notquests_player_questpoints%
%notquests_player_active_quests_list_horizontal%
%notquests_player_active_quests_list_vertical%


![Banner_Future_Plans](https://user-images.githubusercontent.com/70709113/133997163-a98072a9-db72-4bf4-a0eb-b27ec67ad566.png)

- Load Player Data only when the player joins
- Complete GUI for Admin Commands
- Make Citizens optional
- Customized Armor Stands instead of Citizen NPCs for better performance. They will also contain the quests assigned to them directly in a PDC. Both Armor Stands and NPCs will be usable.
- Quest Timer Trigger
- MythicMobs Integration (kinda works already if you use Triggers)
- "Interactions" Plugin Integration
- Placeholder Requirements
- Technical: Give Quest NPCs the quests they offer directly (attach the quest names to the npc/to the trait) for performance boost

I'm not a professional programmer and made this just for fun. You won't find many comments or javadocs either - I will add them gradually.
