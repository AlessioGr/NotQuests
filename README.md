# NotQuests
NotQuests is a Minecraft 1.17.1 plugin for Paper.

I created it for my own server, mc.notnot.pro and planned to keep it private. However, I don't have enough time to maintain it myself, so I just made it public.

You have to build this yourself and change the MySQL Database details (required) in Datamanager - it's hard-coded at the moment.

**Features**
- Bind Quests to NPCs
- Complete GUI for the user interface
- Player data is saved in MySQL, quest configuration data in a configuration file
- Quest Points
- Smart Command Tab Completions for all User and Admin commands
- Re-usable Actions for Triggers
- Multiple Quest Objectives:
  - Break Blocks
  - Collect Items
  - Consume Items
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
- Multiple Triggers: (Triggers run console commands (= Actions) when activated. Triggers are set per-quest)
  - Begin Quest or Objective
  - Complete Quest or Objective
  - Death
  - Disconnect
  - Fail Quest
  - NPC dies (good for Escort NPC Quests)
  - Enter World
  - Leave World
- Probably plenty of more features which I forgot to add


**Requirements to run this:**
- Paper (Won't work on just spigot)
- PlaceholderAPI
- Vault
- Citizens

**Future Plans**
- Complete GUI for Admin Commands
- Make Citizens optional
- Customized Armor Stands instead of Citizen NPCs for better performance. They will also contain the quests assigned to them directly in a PDC. Both Armor Stands and NPCs will be usable.

I'm not a professional programmer and made this just for fun. Code quality is close to 0. You won't find any comments or javadocs either. I just decided to publish it because, why not.
