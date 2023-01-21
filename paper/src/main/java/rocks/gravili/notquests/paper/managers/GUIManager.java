/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.incendo.interfaces.core.arguments.ArgumentKey;
import org.incendo.interfaces.core.arguments.HashMapInterfaceArguments;
import org.incendo.interfaces.core.click.ClickHandler;
import org.incendo.interfaces.core.transform.types.PaginatedTransform;
import org.incendo.interfaces.core.util.Vector2;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PaperInterfaceListeners;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.incendo.interfaces.paper.pane.ChestPane;
import org.incendo.interfaces.paper.transform.PaperTransform;
import org.incendo.interfaces.paper.type.ChestInterface;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GUIManager {
  private final NotQuests main;
  private final ItemStack chest_closed,
      chest_open,
      abort_closed,
      abort_open,
      books_closed,
      books_open,
      coins;
  private ChestInterface mainInterface;
  private ChestInterface previewQuestInterface,
      abortQuestInterface,
      questProgressInterface,
      selectiveTakeQuestsInterface;

  public GUIManager(final NotQuests main) {
    this.main = main;
    // chest = new ItemStack(Material.CHEST);

    chest_open = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) chest_open.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTcxNDUxNmU2NTY1YjgxMmZmNWIzOWVhMzljZDI4N2FmZWM4ZmNjMDZkOGYzMDUyMWNjZDhmMWI0Y2JmZGM2YiJ9fX0="));
      meta.setPlayerProfile(prof);
      chest_open.setItemMeta(meta);
    }

    chest_closed = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) chest_closed.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNkM2M0NWQ3YjgzODRlOGExOTYzZTRkYTBhZTZiMmRhZWIyYTNlOTdhYzdhMjhmOWViM2QzOTU5NzI1Nzk5ZiJ9fX0="));
      meta.setPlayerProfile(prof);
      chest_closed.setItemMeta(meta);
    }

    abort_closed = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) abort_closed.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmRhOTExNDM3YjRlY2ZhYTNjMTg5NDE2MjIxN2MwMWI2OGE1NWM4OWJiMmY0ZDQ5MjczNDVjZTVjNzk0In19fQ=="));
      meta.setPlayerProfile(prof);
      abort_closed.setItemMeta(meta);
    }

    abort_open = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) abort_open.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmE1ZTM4MjlkNzM2MWM5ZDNmOTAxYjc5ODMxMGJhYmU5MDgxZjY4NWU4NDcwZjE3ZGE5MzRmZjIzNDAwOWExNyJ9fX0="));
      meta.setPlayerProfile(prof);
      abort_open.setItemMeta(meta);
    }

    coins = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) coins.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODM4MWM1MjlkNTJlMDNjZDc0YzNiZjM4YmI2YmEzZmRlMTMzN2FlOWJmNTAzMzJmYWE4ODllMGEyOGU4MDgxZiJ9fX0="));
      meta.setPlayerProfile(prof);
      coins.setItemMeta(meta);
    }

    books_closed = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) books_closed.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBiMDY4NzA5NzkwZDQxYjg5MjdiODQyMmQyMWJiNTI0MDRiNTViNGNhMzUyY2RiN2M2OGU0YjM2NTkyNzIxIn19fQ=="));
      meta.setPlayerProfile(prof);
      books_closed.setItemMeta(meta);
    }

    books_open = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) books_open.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzBjZjZjZGMxMWRiNzRjMGQ3N2JhMzc1NmM2ZmRlMzQ1ZmU1NDQzZWNmN2VhNGE0MWQxNjI1NGU2NTk1ODRjZiJ9fX0="));
      meta.setPlayerProfile(prof);
      books_open.setItemMeta(meta);
    }

    constructInterfaces();
  }

  public void showMainQuestsGUI(QuestPlayer questPlayer) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    mainInterface.open(
        PlayerViewer.of(questPlayer.getPlayer()),
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .build());
  }

  public void showPreviewQuestGUI(QuestPlayer questPlayer, final Quest quest) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    previewQuestInterface.open(
        PlayerViewer.of(questPlayer.getPlayer()),
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("quest", Quest.class), quest)
            .build());
  }

  public void showAbortQuestGUI(QuestPlayer questPlayer, final ActiveQuest activeQuest) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    abortQuestInterface.open(
        PlayerViewer.of(questPlayer.getPlayer()),
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("activeQuest", ActiveQuest.class), activeQuest)
            .build());
  }

  public void showQuestProgressGUI(QuestPlayer questPlayer, final ActiveQuest activeQuest) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    questProgressInterface.open(
        PlayerViewer.of(questPlayer.getPlayer()),
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("activeQuest", ActiveQuest.class), activeQuest)
            .build());
  }

  //This should ONLY be used for NPC-induced GUIs. Otherwise, takeEnabled woud nreal
  public void showTakeQuestsGUI(QuestPlayer questPlayer, final ArrayList<Quest> quests) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    selectiveTakeQuestsInterface.open(
        PlayerViewer.of(questPlayer.getPlayer()),
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("quests", ArrayList.class), quests)
            .build());
  }

  public void showTakeQuestsGUI(QuestPlayer questPlayer) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    final HashMapInterfaceArguments arguments =
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("paneType", String.class), "takequest")
            .build();
    constructMainInterface(
            main.getLanguageManager().getComponent("gui.takeQuestChoose.title", null))
        .open(PlayerViewer.of(questPlayer.getPlayer()), arguments);
  }

  public void showTakeQuestsGUIOfCategory(final QuestPlayer questPlayer, final Category category) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    final HashMapInterfaceArguments arguments =
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("paneType", String.class), "takequest")
            .with(ArgumentKey.of("category", Category.class), category)
            .build();
    constructMainInterface(
        main.getLanguageManager().getComponent("gui.takeQuestChoose.title", null))
        .open(PlayerViewer.of(questPlayer.getPlayer()), arguments);
  }

  public void showActiveQuestsGUI(QuestPlayer questPlayer) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    final HashMapInterfaceArguments arguments =
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("paneType", String.class), "activequests")
            .build();
    constructMainInterface(main.getLanguageManager().getComponent("gui.activeQuests.title", null))
        .open(PlayerViewer.of(questPlayer.getPlayer()), arguments);
  }

  public void showAbortQuestsGUI(QuestPlayer questPlayer) {
    if (main.getDataManager().isDisabled()) {
      main.getDataManager().sendPluginDisabledMessage(questPlayer.getPlayer());
      return;
    }
    final HashMapInterfaceArguments arguments =
        HashMapInterfaceArguments.with(
                ArgumentKey.of("player", Player.class), questPlayer.getPlayer())
            .with(ArgumentKey.of("questPlayer", QuestPlayer.class), questPlayer)
            .with(ArgumentKey.of("paneType", String.class), "abortquest")
            .build();
    constructMainInterface(
            main.getLanguageManager().getComponent("gui.abortQuestChoose.title", null))
        .open(PlayerViewer.of(questPlayer.getPlayer()), arguments);
  }

  public void constructInterfaces() {
    PaperInterfaceListeners.install(main.getMain());

    mainInterface =
        constructMainInterface(main.getLanguageManager().getComponent("gui.main.title", null));

    ItemStack separatorItemStack1 = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta separatorItemStack1Meta = separatorItemStack1.getItemMeta();
    separatorItemStack1Meta.displayName(Component.text(" "));
    separatorItemStack1.setItemMeta(separatorItemStack1Meta);

    ItemStack separatorItemStack2 = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
    ItemMeta separatorItemStack2Meta = separatorItemStack2.getItemMeta();
    separatorItemStack2Meta.displayName(Component.text(" "));
    separatorItemStack2.setItemMeta(separatorItemStack2Meta);

    ItemStack separatorItemStack3 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta separatorItemStack3Meta = separatorItemStack3.getItemMeta();
    separatorItemStack3Meta.displayName(Component.text(" "));
    separatorItemStack3.setItemMeta(separatorItemStack3Meta);

    previewQuestInterface =
        ChestInterface.builder()
            // This interface will have one row.
            .rows(3)
            // This interface will update every five ticks.
            // .updates(true, 5)
            // Cancel all inventory click events
            .clickHandler(ClickHandler.cancel())
            // Fill the background with black stained glass panes
            .addTransform(PaperTransform.chestFill(ItemStackElement.of(separatorItemStack3)))
            .addTransform(
                (pane, view) -> {
                  ChestPane result = pane;
                  // Get the view arguments
                  // (Keep in mind - these arguments may be coming from a Supplier, so their values
                  // can change!)
                  final Player player =
                      view.arguments().get(ArgumentKey.of("player", Player.class));
                  QuestPlayer questPlayer = null;
                  if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
                    questPlayer =
                        view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
                  }
                  final Quest quest = view.arguments().get(ArgumentKey.of("quest", Quest.class));

                  if (main.getConfiguration().isGuiQuestPreviewDescription_enabled()) {
                    ItemStack itemStack = new ItemStack(Material.BOOKSHELF);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.previewQuest.button.description.name",
                                player,
                                questPlayer,
                                quest));

                    if (!quest.getObjectiveHolderDescription().isBlank()) {
                      List<String> loreStringList =
                          main.getLanguageManager()
                              .getStringList(
                                  "gui.previewQuest.button.description.lore",
                                  player,
                                  questPlayer,
                                  quest);
                      List<Component> lore = new ArrayList<>();

                      for (String loreString : loreStringList) {
                        if (loreString.contains("%WRAPPEDQUESTDESCRIPTION%")) {
                          for (String questDescriptionLine :
                              quest.getQuestDescriptionList(
                                  main.getConfiguration().guiQuestDescriptionMaxLineLength)) {
                            lore.add(
                                main.parse(
                                        loreString.replace("%WRAPPEDQUESTDESCRIPTION%", "")
                                            + questDescriptionLine)
                                    .decoration(TextDecoration.ITALIC, false));
                          }
                        } else {
                          lore.add(main.parse(loreString).decoration(TextDecoration.ITALIC, false));
                        }
                      }
                      itemMeta.lore(lore);
                    } else {
                      itemMeta.lore(
                          main.getLanguageManager()
                              .getComponentList(
                                  "gui.previewQuest.button.description.lore-if-empty",
                                  player,
                                  questPlayer,
                                  quest));
                    }

                    itemStack.setItemMeta(itemMeta);

                    result =
                        result.element(
                            ItemStackElement.of(itemStack),
                            main.getLanguageManager()
                                .getInt("gui.previewQuest.button.description.x"),
                            main.getLanguageManager()
                                .getInt("gui.previewQuest.button.description.y"));
                  }

                  if (main.getConfiguration().isGuiQuestPreviewRewards_enabled()) {
                    ItemStack itemStack = new ItemStack(Material.EMERALD);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.previewQuest.button.rewards.name",
                                player,
                                questPlayer,
                                quest));

                    if (!quest.getRewards().isEmpty()) {
                      List<String> loreStringList =
                          main.getLanguageManager()
                              .getStringList(
                                  "gui.previewQuest.button.rewards.lore",
                                  player,
                                  questPlayer,
                                  quest);
                      List<Component> lore = new ArrayList<>();

                      for (String loreString : loreStringList) {
                        if (loreString.contains("%QUESTREWARDS%")) {
                          for (String rewardLine :
                              main.getQuestManager().getQuestRewardsList(quest, questPlayer)) {
                            lore.add(
                                main.parse(loreString.replace("%QUESTREWARDS%", "") + rewardLine)
                                    .decoration(TextDecoration.ITALIC, false));
                          }
                        } else {
                          lore.add(main.parse(loreString).decoration(TextDecoration.ITALIC, false));
                        }
                      }
                      itemMeta.lore(lore);
                    } else {
                      itemMeta.lore(
                          main.getLanguageManager()
                              .getComponentList(
                                  "gui.previewQuest.button.rewards.lore-if-empty",
                                  player,
                                  questPlayer,
                                  quest));
                    }

                    itemStack.setItemMeta(itemMeta);

                    result =
                        result.element(
                            ItemStackElement.of(itemStack),
                            main.getLanguageManager().getInt("gui.previewQuest.button.rewards.x"),
                            main.getLanguageManager().getInt("gui.previewQuest.button.rewards.y"));
                  }

                  if (main.getConfiguration().isGuiQuestPreviewRequirements_enabled()) {
                    ItemStack itemStack = new ItemStack(Material.IRON_BARS);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.previewQuest.button.requirements.name",
                                player,
                                questPlayer,
                                quest));

                    if (!quest.getRequirements().isEmpty()) {
                      List<String> loreStringList =
                          main.getLanguageManager()
                              .getStringList(
                                  "gui.previewQuest.button.requirements.lore",
                                  player,
                                  questPlayer,
                                  quest);
                      List<Component> lore = new ArrayList<>();

                      for (String loreString : loreStringList) {
                        if (loreString.contains("%QUESTREQUIREMENTS%")) {
                          for (final String requirementLine :
                              main.getQuestManager().getQuestRequirementsList(quest, questPlayer)) {
                            lore.add(
                                main.parse(
                                        loreString.replace("%QUESTREQUIREMENTS%", "")
                                            + requirementLine)
                                    .decoration(TextDecoration.ITALIC, false));
                          }
                        } else {
                          lore.add(main.parse(loreString).decoration(TextDecoration.ITALIC, false));
                        }
                      }
                      itemMeta.lore(lore);
                    } else {
                      itemMeta.lore(
                          main.getLanguageManager()
                              .getComponentList(
                                  "gui.previewQuest.button.requirements.lore-if-empty",
                                  player,
                                  questPlayer,
                                  quest));
                    }

                    itemStack.setItemMeta(itemMeta);

                    result =
                        result.element(
                            ItemStackElement.of(itemStack),
                            main.getLanguageManager()
                                .getInt("gui.previewQuest.button.requirements.x"),
                            main.getLanguageManager()
                                .getInt("gui.previewQuest.button.requirements.y"));
                  }

                  ItemStack confirmTakeItemStack = new ItemStack(Material.GREEN_CONCRETE);
                  ItemMeta confirmTakeItemMeta = confirmTakeItemStack.getItemMeta();
                  confirmTakeItemMeta.displayName(
                      main.getLanguageManager()
                          .getComponent(
                              "gui.previewQuest.button.confirmTake.name",
                              player,
                              questPlayer,
                              quest));
                  confirmTakeItemMeta.lore(
                      main.getLanguageManager()
                          .getComponentList(
                              "gui.previewQuest.button.confirmTake.lore",
                              player,
                              questPlayer,
                              quest));
                  confirmTakeItemStack.setItemMeta(confirmTakeItemMeta);

                  result =
                      result.element(
                          ItemStackElement.of(
                              confirmTakeItemStack,
                              (clickHandler) -> {
                                final String takeQuestResult =
                                        main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                                if (!takeQuestResult.equals("accepted")) {
                                  main.sendMessage(player, takeQuestResult);
                                }
                                clickHandler.viewer().close();
                              }),
                          main.getLanguageManager().getInt("gui.previewQuest.button.confirmTake.x"),
                          main.getLanguageManager()
                              .getInt("gui.previewQuest.button.confirmTake.y"));

                  ItemStack cancelTakeItemStack = new ItemStack(Material.RED_CONCRETE);
                  ItemMeta cancelTakeItemMeta = cancelTakeItemStack.getItemMeta();
                  cancelTakeItemMeta.displayName(
                      main.getLanguageManager()
                          .getComponent(
                              "gui.previewQuest.button.cancelTake.name",
                              player,
                              questPlayer,
                              quest));
                  cancelTakeItemMeta.lore(
                      main.getLanguageManager()
                          .getComponentList(
                              "gui.previewQuest.button.cancelTake.lore",
                              player,
                              questPlayer,
                              quest));
                  cancelTakeItemStack.setItemMeta(cancelTakeItemMeta);

                  result =
                      result.element(
                          ItemStackElement.of(
                              cancelTakeItemStack,
                              (clickHandler) -> {
                                clickHandler.viewer().close();
                              }),
                          main.getLanguageManager().getInt("gui.previewQuest.button.cancelTake.x"),
                          main.getLanguageManager().getInt("gui.previewQuest.button.cancelTake.y"));

                  return result;
                })
            // Set the title
            .title(main.getLanguageManager().getComponent("gui.previewQuest.title", null))
            // Build the interface
            .build();

    abortQuestInterface =
        ChestInterface.builder()
            // This interface will have one row.
            .rows(3)
            // This interface will update every five ticks.
            // .updates(true, 5)
            // Cancel all inventory click events
            .clickHandler(ClickHandler.cancel())
            // Fill the background with black stained glass panes
            .addTransform(PaperTransform.chestFill(ItemStackElement.of(separatorItemStack3)))
            .addTransform(
                (pane, view) -> {
                  ChestPane result = pane;
                  // Get the view arguments
                  // (Keep in mind - these arguments may be coming from a Supplier, so their values
                  // can change!)
                  final Player player =
                      view.arguments().get(ArgumentKey.of("player", Player.class));
                  final QuestPlayer questPlayer =
                      view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));

                  final ActiveQuest activeQuest =
                      view.arguments().get(ArgumentKey.of("activeQuest", ActiveQuest.class));

                  ItemStack confirmAbortItemStack = new ItemStack(Material.GREEN_CONCRETE);
                  ItemMeta confirmAbortItemMeta = confirmAbortItemStack.getItemMeta();
                  confirmAbortItemMeta.displayName(
                      main.getLanguageManager()
                          .getComponent(
                              "gui.abortQuest.button.confirmAbort.name",
                              player,
                              questPlayer,
                              activeQuest));
                  confirmAbortItemMeta.lore(
                      main.getLanguageManager()
                          .getComponentList(
                              "gui.abortQuest.button.confirmAbort.lore",
                              player,
                              questPlayer,
                              activeQuest));
                  confirmAbortItemStack.setItemMeta(confirmAbortItemMeta);

                  result =
                      result.element(
                          ItemStackElement.of(
                              confirmAbortItemStack,
                              (clickHandler) -> {
                                if(!activeQuest.getQuest().isAbortEnabled()){
                                  main.sendMessage(
                                          player,
                                          main.getLanguageManager()
                                                  .getString("chat.abort-disabled", player, activeQuest));

                                  return;
                                }
                                questPlayer.failQuest(activeQuest);
                                main.sendMessage(
                                    player,
                                    main.getLanguageManager()
                                        .getString("chat.quest-aborted", player, activeQuest));
                                clickHandler.viewer().close();
                              }),
                          main.getLanguageManager().getInt("gui.abortQuest.button.confirmAbort.x"),
                          main.getLanguageManager().getInt("gui.abortQuest.button.confirmAbort.y"));

                  ItemStack cancelAbortItemStack = new ItemStack(Material.RED_CONCRETE);
                  ItemMeta cancelAbortItemMeta = cancelAbortItemStack.getItemMeta();
                  cancelAbortItemMeta.displayName(
                      main.getLanguageManager()
                          .getComponent(
                              "gui.abortQuest.button.cancelAbort.name",
                              player,
                              questPlayer,
                              activeQuest));
                  cancelAbortItemMeta.lore(
                      main.getLanguageManager()
                          .getComponentList(
                              "gui.abortQuest.button.cancelAbort.lore",
                              player,
                              questPlayer,
                              activeQuest));
                  cancelAbortItemStack.setItemMeta(cancelAbortItemMeta);

                  result =
                      result.element(
                          ItemStackElement.of(
                              cancelAbortItemStack,
                              (clickHandler) -> {
                                clickHandler.viewer().close();
                              }),
                          main.getLanguageManager().getInt("gui.abortQuest.button.cancelAbort.x"),
                          main.getLanguageManager().getInt("gui.abortQuest.button.cancelAbort.y"));

                  return result;
                })
            // Set the title
            .title(main.getLanguageManager().getComponent("gui.abortQuest.title", null))
            // Build the interface
            .build();

    questProgressInterface =
        ChestInterface.builder()
            // This interface will have one row.
            .rows(6)
            // This interface will update every five ticks.
            // .updates(true, 5)
            // Cancel all inventory click events
            .clickHandler(ClickHandler.cancel())
            // Fill the background with black stained glass panes
            .addTransform(PaperTransform.chestFill(ItemStackElement.of(separatorItemStack3)))
            .addTransform(
                (pane, view) -> {
                  ChestPane result = pane;
                  // Get the view arguments
                  // (Keep in mind - these arguments may be coming from a Supplier, so their values
                  // can change!)
                  final Player player =
                      view.arguments().get(ArgumentKey.of("player", Player.class));
                  final QuestPlayer questPlayer =
                      view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
                  final ActiveQuest activeQuest =
                      view.arguments().get(ArgumentKey.of("activeQuest", ActiveQuest.class));

                  int counterX = 0;
                  int counterY = 1;
                  for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                    final Material materialToUse = Material.PAPER;

                    int count = activeObjective.getObjectiveID();
                    if (!main.getConfiguration().showObjectiveItemAmount) {
                      count = 1;
                    }
                    ItemStack itemStack = new ItemStack(materialToUse);
                    itemStack.setAmount(count);
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (activeObjective.isUnlocked()) {
                      itemMeta.displayName(
                          main.getLanguageManager()
                              .getComponent(
                                  "gui.progress.button.unlockedObjective.name",
                                  player,
                                  questPlayer,
                                  activeQuest,
                                  activeObjective));

                      if (!activeObjective.getObjective().getObjectiveHolderDescription().isBlank()) {
                        List<String> loreStringList =
                            main.getLanguageManager()
                                .getStringList(
                                    "gui.progress.button.unlockedObjective.lore",
                                    player,
                                    questPlayer,
                                    activeQuest,
                                    activeObjective);
                        List<Component> lore = new ArrayList<>();

                        for (String loreString : loreStringList) {
                          for (String loreStringSplit : loreString.split("\n")) {
                            if (loreStringSplit.contains("%WRAPPEDOBJECTIVEDESCRIPTION%")) {
                              for (String objectiveDescriptionLine :
                                  activeObjective
                                      .getObjective()
                                      .getDescriptionLines(
                                          main.getConfiguration()
                                              .guiQuestDescriptionMaxLineLength)) {
                                lore.add(
                                    main.parse(
                                            loreStringSplit.replace(
                                                    "%WRAPPEDOBJECTIVEDESCRIPTION%", "")
                                                + objectiveDescriptionLine)
                                        .decoration(TextDecoration.ITALIC, false));
                              }
                            } else {
                              lore.add(
                                  main.parse(loreStringSplit)
                                      .decoration(TextDecoration.ITALIC, false));
                            }
                          }
                        }
                        itemMeta.lore(lore);
                      } else {
                        itemMeta.lore(
                            main.getLanguageManager()
                                .getComponentList(
                                    "gui.progress.button.unlockedObjective.lore-if-description-empty",
                                    player,
                                    questPlayer,
                                    activeQuest,
                                    activeObjective));
                      }

                      itemStack.setItemMeta(itemMeta);

                      result = result.element(ItemStackElement.of(itemStack), counterX++, counterY);

                    } else {
                      itemMeta.displayName(
                          main.getLanguageManager()
                              .getComponent(
                                  "gui.progress.button.lockedObjective.name",
                                  player,
                                  questPlayer,
                                  activeQuest,
                                  activeObjective));
                      itemMeta.lore(
                          main.getLanguageManager()
                              .getComponentList(
                                  "gui.progress.button.lockedObjective.lore",
                                  player,
                                  questPlayer,
                                  activeQuest,
                                  activeObjective));
                      itemStack.setItemMeta(itemMeta);

                      result = result.element(ItemStackElement.of(itemStack), counterX++, counterY);
                    }
                    if (counterX > 8) {
                      counterX = 0;
                      counterY++;
                    }

                    itemStack.setItemMeta(itemMeta);
                  }

                  counterX = 0;
                  counterY++;

                  for (final ActiveObjective activeObjective :
                      activeQuest.getCompletedObjectives()) {
                    final Material materialToUse = Material.FILLED_MAP;

                    int count = activeObjective.getObjectiveID();
                    if (!main.getConfiguration().showObjectiveItemAmount) {
                      count = 1;
                    }
                    ItemStack itemStack = new ItemStack(materialToUse);
                    itemStack.setAmount(count);
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.progress.button.completedObjective.name",
                                player,
                                questPlayer,
                                activeQuest,
                                activeObjective));

                    if (!activeObjective.getObjective().getObjectiveHolderDescription().isBlank()) {
                      List<String> loreStringList =
                          main.getLanguageManager()
                              .getStringList(
                                  "gui.progress.button.completedObjective.lore",
                                  player,
                                  questPlayer,
                                  activeQuest,
                                  activeObjective);
                      List<Component> lore = new ArrayList<>();

                      for (String loreString : loreStringList) {
                        for (String loreStringSplit : loreString.split("\n")) {
                          if (loreStringSplit.contains("</strikethrough>")) {
                            loreStringSplit = "<strikethrough><unimportant>" + loreStringSplit;
                          }
                          if (loreStringSplit.contains("%WRAPPEDOBJECTIVEDESCRIPTION%")) {
                            for (String objectiveDescriptionLine :
                                activeObjective
                                    .getObjective()
                                    .getDescriptionLines(
                                        main.getConfiguration().guiQuestDescriptionMaxLineLength)) {
                              lore.add(
                                  main.parse(
                                          loreStringSplit.replace(
                                                  "%WRAPPEDOBJECTIVEDESCRIPTION%", "")
                                              + objectiveDescriptionLine)
                                      .decoration(TextDecoration.ITALIC, false));
                            }
                          } else {
                            lore.add(
                                main.parse(loreStringSplit)
                                    .decoration(TextDecoration.ITALIC, false));
                          }
                        }
                      }
                      itemMeta.lore(lore);
                    } else {
                      itemMeta.lore(
                          main.getLanguageManager()
                              .getComponentList(
                                  "gui.progress.button.completedObjective.lore-if-description-empty",
                                  player,
                                  questPlayer,
                                  activeQuest,
                                  activeObjective));
                    }

                    itemStack.setItemMeta(itemMeta);

                    result =
                        result.element(
                            ItemStackElement.of(itemStack),
                            counterX++,
                            counterY); // TODO: Positions

                    if (counterX > 8) {
                      counterX = 0;
                      counterY++;
                    }
                    itemStack.setItemMeta(itemMeta);
                  }

                  return result;
                })
            // Set the title
            .title(main.getLanguageManager().getComponent("gui.progress.title", null))
            // Build the interface
            .build();

    selectiveTakeQuestsInterface =
        ChestInterface.builder()
            // This interface will have one row.
            .rows(6)
            // This interface will update every five ticks.
            // .updates(true, 5)
            // Cancel all inventory click events
            .clickHandler(ClickHandler.cancel())
            // Fill the background with black stained glass panes
            .addTransform(PaperTransform.chestFill(ItemStackElement.of(separatorItemStack3)))
            .addTransform(
                (pane, view) -> {
                  // Get the view arguments
                  // (Keep in mind - these arguments may be coming from a Supplier, so their values
                  // can change!)
                  final Player player =
                      view.arguments().get(ArgumentKey.of("player", Player.class));
                  final QuestPlayer questPlayer =
                      view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
                  final ArrayList<Quest> selectedQuests =
                      view.arguments().get(ArgumentKey.of("quests", ArrayList.class));

                  ArrayList<ItemStackElement<ChestPane>> list =
                      new ArrayList<>() {
                        {
                          int count = 1;
                          for (final Quest quest :
                              main.getQuestManager()
                                  .getQuestsFromListWithVisibilityEvaluations(
                                      questPlayer, selectedQuests)) {
                            final ItemStack materialToUse = quest.getTakeItem();

                            ItemStack itemStack = new ItemStack(materialToUse);
                            ItemMeta itemMeta = itemStack.getItemMeta();

                            itemStack.setAmount(count);

                            List<String> loreStringList =
                                main.getLanguageManager()
                                    .getStringList(
                                        "gui.availableQuests.button.questPreview.lore",
                                        player,
                                        quest);
                            List<Component> lore = new ArrayList<>();

                            for (String loreString : loreStringList) {
                              // main.getLogManager().info("Found line: " + loreString);

                              if (loreString.contains("%WRAPPEDQUESTDESCRIPTION%")) {
                                if (!quest.getObjectiveHolderDescription().isBlank()) {
                                  int counter = 0;
                                  for (String questDescriptionLine :
                                      quest.getQuestDescriptionList(
                                          main.getConfiguration()
                                              .guiQuestDescriptionMaxLineLength)) {
                                    // main.getLogManager().info("Found d line: " +
                                    // questDescriptionLine);
                                    counter++;
                                    lore.add(
                                        main.parse(
                                                loreString.replace("%WRAPPEDQUESTDESCRIPTION%", "")
                                                    + questDescriptionLine)
                                            .decoration(TextDecoration.ITALIC, false));
                                  }
                                }
                              } else {
                                lore.add(
                                    main.parse(loreString)
                                        .decoration(TextDecoration.ITALIC, false));
                              }
                            }

                            if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                              itemMeta.displayName(
                                  main.getLanguageManager()
                                      .getComponent(
                                          "gui.availableQuests.button.questPreview.name-if-accepted",
                                          player,
                                          quest));
                            } else {
                              itemMeta.displayName(
                                  main.getLanguageManager()
                                      .getComponent(
                                          "gui.availableQuests.button.questPreview.name-if-not-accepted",
                                          player,
                                          quest));
                            }

                            itemMeta.lore(lore);

                            itemStack.setItemMeta(itemMeta);

                            add(
                                ItemStackElement.of(
                                    itemStack,
                                    (clickHandler) -> {
                                      showPreviewQuestGUI(questPlayer, quest);
                                    }));

                            if (main.getConfiguration().showQuestItemAmount) {
                              count++;
                            }
                          }
                        }
                      };
                  PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer>
                      paginatedTransform =
                          new PaginatedTransform<>(Vector2.at(1, 1), Vector2.at(7, 4), list);
                  return paginatedTransform.apply(pane, view);
                })
            // Set the title
            .title(main.getLanguageManager().getComponent("gui.availableQuests.title", null))
            // Build the interface
            .build();
  }

  public final ChestInterface constructMainInterface(final Component title) {
    final ItemStack separatorItemStack1 = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    final ItemMeta separatorItemStack1Meta = separatorItemStack1.getItemMeta();
    separatorItemStack1Meta.displayName(Component.text(" "));
    separatorItemStack1.setItemMeta(separatorItemStack1Meta);

    final ItemStack separatorItemStack2 = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
    final ItemMeta separatorItemStack2Meta = separatorItemStack2.getItemMeta();
    separatorItemStack2Meta.displayName(Component.text(" "));
    separatorItemStack2.setItemMeta(separatorItemStack2Meta);

    final ItemStack separatorItemStack3 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    final ItemMeta separatorItemStack3Meta = separatorItemStack3.getItemMeta();
    separatorItemStack3Meta.displayName(Component.text(" "));
    separatorItemStack3.setItemMeta(separatorItemStack3Meta);



    final ItemStack backgroundItemStack = new ItemStack(main.getLanguageManager().getMaterialOrAir("gui.main.backgroundFill.material"));
    final ItemMeta backgroundItemStackMeta = backgroundItemStack.getItemMeta();
    if(backgroundItemStackMeta != null) {
      backgroundItemStackMeta.displayName(Component.text(" "));
      backgroundItemStack.setItemMeta(backgroundItemStackMeta);
    }


    final ConfigurationSection mainBackgroundConfigurationSection =
        main.getLanguageManager()
            .getLanguageConfig()
            .getConfigurationSection("gui.main.background");
    final Set<String> keys =
        mainBackgroundConfigurationSection != null
            ? mainBackgroundConfigurationSection.getKeys(false)
            : null;

    return ChestInterface.builder()
        // This interface will have one row.
        .rows(6)
        // This interface will update every five ticks.
        // .updates(true, 5)
        // Cancel all inventory click events
        .clickHandler(ClickHandler.cancel())
        // Fill the background with black stained glass panes
        .addTransform(PaperTransform.chestFill(ItemStackElement.of(backgroundItemStack)))
        .addTransform(
            (pane, view) -> {
              ChestPane result = pane;
              if (mainBackgroundConfigurationSection != null) {
                int counter = 0;
                for (final String backgroundID : keys) { // TODO: Improve performance
                  counter++; // TODO: make this clean and let them actually customize the block
                  final int xStart =
                      mainBackgroundConfigurationSection.getInt(backgroundID + ".minX", -1);
                  final int xEnd =
                      mainBackgroundConfigurationSection.getInt(backgroundID + ".maxX", -1);
                  final int yStart =
                      mainBackgroundConfigurationSection.getInt(backgroundID + ".minY", -1);
                  final int yEnd =
                      mainBackgroundConfigurationSection.getInt(backgroundID + ".maxY", -1);
                  for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                      if (counter == 3) {
                        result = result.element(ItemStackElement.of(separatorItemStack2), x, y);
                      } else {
                        result = result.element(ItemStackElement.of(separatorItemStack1), x, y);
                      }
                    }
                  }
                }
              }

              return result;
            })
        .addTransform(
            (pane, view) -> {
              ChestPane result;
              // Get the view arguments
              // (Keep in mind - these arguments may be coming from a Supplier, so their values can
              // change!)
              final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));
              QuestPlayer questPlayer = null;
              if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
                questPlayer =
                    view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
              }

              final String paneType =
                  view.arguments().getOrDefault(ArgumentKey.of("paneType", String.class), "");

              ItemMeta coinsItemMeta = coins.getItemMeta();
              coinsItemMeta.displayName(
                  main.getLanguageManager()
                      .getComponent("gui.main.button.questpoints.name", player, questPlayer));
              coinsItemMeta.lore(
                  main.getLanguageManager()
                      .getComponentList("gui.main.button.questpoints.lore", player, questPlayer));
              coins.setItemMeta(coinsItemMeta);

              result =
                  pane.element(
                      ItemStackElement.of(coins),
                      main.getLanguageManager().getInt("gui.main.button.questpoints.x"),
                      main.getLanguageManager().getInt("gui.main.button.questpoints.y"));

              ItemStack takeQuestItemStack = chest_closed;
              if (paneType.equalsIgnoreCase("takequest")) {
                takeQuestItemStack = chest_open;
              }
              ItemMeta takeItemMeta = takeQuestItemStack.getItemMeta();
              takeItemMeta.displayName(
                  main.getLanguageManager()
                      .getComponent("gui.main.button.takequest.name", player, questPlayer));
              takeItemMeta.lore(
                  main.getLanguageManager()
                      .getComponentList("gui.main.button.takequest.lore", player, questPlayer));

              takeQuestItemStack.setItemMeta(takeItemMeta);

              result =
                  result.element(
                      ItemStackElement.of(
                          takeQuestItemStack,
                          // Handle click
                          (clickHandler) -> {
                            showTakeQuestsGUI(
                                clickHandler
                                    .view()
                                    .arguments()
                                    .get(ArgumentKey.of("questPlayer", QuestPlayer.class)));
                          }),
                      main.getLanguageManager().getInt("gui.main.button.takequest.x"),
                      main.getLanguageManager().getInt("gui.main.button.takequest.y"));

              ItemStack abortQuestItemStack = abort_closed;
              if (paneType.equalsIgnoreCase("abortquest")) {
                abortQuestItemStack = abort_open;
              }
              ItemMeta abortItemMeta = abortQuestItemStack.getItemMeta();
              abortItemMeta.displayName(
                  main.getLanguageManager()
                      .getComponent("gui.main.button.abortquest.name", player, questPlayer));
              abortItemMeta.lore(
                  main.getLanguageManager()
                      .getComponentList("gui.main.button.abortquest.lore", player, questPlayer));

              abortQuestItemStack.setItemMeta(abortItemMeta);

              result =
                  result.element(
                      ItemStackElement.of(
                          abortQuestItemStack,
                          // Handle click
                          (clickHandler) -> {
                            showAbortQuestsGUI(
                                clickHandler
                                    .view()
                                    .arguments()
                                    .get(ArgumentKey.of("questPlayer", QuestPlayer.class)));
                          }),
                      main.getLanguageManager().getInt("gui.main.button.abortquest.x"),
                      main.getLanguageManager().getInt("gui.main.button.abortquest.y"));

              ItemStack activeQuestsItemStack = books_closed;
              if (paneType.equalsIgnoreCase("activequests")) {
                activeQuestsItemStack = books_open;
              }
              ItemMeta activeQuestsItemMeta = activeQuestsItemStack.getItemMeta();
              activeQuestsItemMeta.displayName(
                  main.getLanguageManager()
                      .getComponent("gui.main.button.activequests.name", player, questPlayer));
              activeQuestsItemMeta.lore(
                  main.getLanguageManager()
                      .getComponentList("gui.main.button.activequests.lore", player, questPlayer));
              activeQuestsItemStack.setItemMeta(activeQuestsItemMeta);

              result =
                  result.element(
                      ItemStackElement.of(
                          activeQuestsItemStack,
                          // Handle click
                          (clickHandler) -> {
                            showActiveQuestsGUI(
                                clickHandler
                                    .view()
                                    .arguments()
                                    .get(ArgumentKey.of("questPlayer", QuestPlayer.class)));
                          }),
                      main.getLanguageManager().getInt("gui.main.button.activequests.x"),
                      main.getLanguageManager().getInt("gui.main.button.activequests.y"));

              return result;
            })
        .addTransform(
            (pane, view) -> { // For the actual content
              final String paneType =
                  view.arguments().getOrDefault(ArgumentKey.of("paneType", String.class), "");
              if (paneType.equalsIgnoreCase("takequest")) {
                if (view.arguments().contains(ArgumentKey.of("category", Category.class))) {
                  final Category category =
                      view.arguments().get(ArgumentKey.of("category", Category.class));
                  return getTakeQuestPaneOfCategory(pane, view, category);
                } else {
                  return getTakeQuestPane(pane, view);
                }
              } else if (paneType.equalsIgnoreCase("abortquest")) {
                return getAbortQuestPane(pane, view);
              } else if (paneType.equalsIgnoreCase("activequests")) {
                return getActiveQuestsPane(pane, view);
              }
              return pane;
            })
        // Set the title
        .title(title)
        // Build the interface
        .build();
  }

  public ChestPane getActiveQuestsPane(
      ChestPane pane, InterfaceView<ChestPane, PlayerViewer> view) {
    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));

    if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
      final QuestPlayer questPlayer =
          view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
      if (questPlayer == null) {
        return pane;
      }
      ArrayList<ItemStackElement<ChestPane>> list =
          new ArrayList<>() {
            {
              for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                ItemStack itemStack = new ItemStack(Material.BOOK);
                ItemMeta itemMeta = itemStack.getItemMeta();

                itemMeta.displayName(
                    main.getLanguageManager()
                        .getComponent(
                            "gui.activeQuests.button.activeQuestButton.name", player, activeQuest));
                itemMeta.lore(
                    main.getLanguageManager()
                        .getComponentList(
                            "gui.activeQuests.button.activeQuestButton.lore", player, activeQuest));

                itemStack.setItemMeta(itemMeta);

                add(
                    ItemStackElement.of(
                        itemStack,
                        (clickHandler) -> {
                          showQuestProgressGUI(questPlayer, activeQuest);
                        }));
              }
            }
          };
      PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> paginatedTransform =
          new PaginatedTransform<>(Vector2.at(3, 1), Vector2.at(8, 5), list);
      return paginatedTransform.apply(pane, view);
    }
    return pane;
  }

  public ChestPane getTakeQuestPane(ChestPane pane, InterfaceView<ChestPane, PlayerViewer> view) {
    if (main.getDataManager().getCategories().size() == 1) {
      return getTakeQuestPaneOfCategory(pane, view, main.getDataManager().getDefaultCategory());
    }

    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));
    QuestPlayer questPlayer = null;
    if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
      questPlayer = view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
    }
    if (questPlayer != null) {
      ArrayList<ItemStackElement<ChestPane>> list =
          new ArrayList<>() {
            {
              for (final Category category : main.getDataManager().getCategories()) {

                final ItemStack itemStack = category.getGuiItem();

                ItemMeta itemMeta = itemStack.getItemMeta();

                itemMeta.displayName(
                    main.parse(category.getFinalName()).decoration(TextDecoration.ITALIC, false));

                itemStack.setItemMeta(itemMeta);

                add(
                    ItemStackElement.of(
                        itemStack,
                        (clickHandler) -> {
                          final HashMapInterfaceArguments arguments =
                              HashMapInterfaceArguments.with(
                                      ArgumentKey.of("player", Player.class),
                                      clickHandler
                                          .view()
                                          .arguments()
                                          .get(ArgumentKey.of("player", Player.class)))
                                  .with(
                                      ArgumentKey.of("questPlayer", QuestPlayer.class),
                                      clickHandler
                                          .view()
                                          .arguments()
                                          .get(ArgumentKey.of("questPlayer", QuestPlayer.class)))
                                  .with(ArgumentKey.of("paneType", String.class), "takequest")
                                  .with(ArgumentKey.of("category", Category.class), category)
                                  .build();
                          constructMainInterface(
                                  main.getLanguageManager()
                                      .getComponent("gui.takeQuestChoose.title", null))
                              .open(clickHandler.viewer(), arguments);
                        }));
              }
            }
          };
      PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> paginatedTransform =
          new PaginatedTransform<>(
              Vector2.at(
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.minX"),
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.minY")),
              Vector2.at(
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.maxX"),
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.maxY")),
              list);
      return paginatedTransform.apply(pane, view);
    }
    return pane;
  }

  public ChestPane getTakeQuestPaneOfCategory(
      ChestPane pane, InterfaceView<ChestPane, PlayerViewer> view, Category category) {

    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));

    if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
      final QuestPlayer questPlayer =
          view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
      ArrayList<ItemStackElement<ChestPane>> list =
          new ArrayList<>() {
            {
              int count = 1;
              for (final Quest quest :
                  main.getQuestManager().getAllQuestsWithVisibilityEvaluations(questPlayer)) {
                if (quest.isTakeEnabled()
                    && quest
                        .getCategory()
                        .getCategoryFullName()
                        .equalsIgnoreCase(category.getCategoryFullName())) {
                  final ItemStack materialToUse = quest.getTakeItem();

                  ItemStack itemStack = new ItemStack(materialToUse);
                  ItemMeta itemMeta = itemStack.getItemMeta();

                  itemStack.setAmount(count);

                  List<String> loreStringList =
                      main.getLanguageManager()
                          .getStringList(
                              "gui.takeQuestChoose.button.questPreview.lore", player, quest);
                  List<Component> lore = new ArrayList<>();

                  for (String loreString : loreStringList) {
                    // main.getLogManager().info("Found line: " + loreString);

                    if (loreString.contains("%WRAPPEDQUESTDESCRIPTION%")) {
                      if (!quest.getObjectiveHolderDescription().isBlank()) {
                        int counter = 0;
                        for (String questDescriptionLine :
                            quest.getQuestDescriptionList(
                                main.getConfiguration().guiQuestDescriptionMaxLineLength)) {
                          // main.getLogManager().info("Found d line: " + questDescriptionLine);
                          counter++;
                          lore.add(
                              main.parse(
                                      loreString.replace("%WRAPPEDQUESTDESCRIPTION%", "")
                                          + questDescriptionLine)
                                  .decoration(TextDecoration.ITALIC, false));
                        }
                      }
                    } else {
                      lore.add(main.parse(loreString).decoration(TextDecoration.ITALIC, false));
                    }
                  }

                  /*if (!quest.getQuestDescription().isBlank()) {
                      description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.lore", player, quest)
                              + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength
                      );
                  }*/

                  if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.takeQuestChoose.button.questPreview.name-if-accepted",
                                player,
                                quest));
                  } else {
                    itemMeta.displayName(
                        main.getLanguageManager()
                            .getComponent(
                                "gui.takeQuestChoose.button.questPreview.name-if-not-accepted",
                                player,
                                quest));
                  }

                  itemMeta.lore(lore);

                  itemStack.setItemMeta(itemMeta);

                  add(
                      ItemStackElement.of(
                          itemStack,
                          (clickHandler) -> {
                            showPreviewQuestGUI(questPlayer, quest);
                          }));

                  if (main.getConfiguration().showQuestItemAmount) {
                    count++;
                  }
                }
              }
            }
          };
      PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> paginatedTransform =
          new PaginatedTransform<>(
              Vector2.at(
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.minX"),
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.minY")),
              Vector2.at(
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.maxX"),
                  main.getLanguageManager().getInt("gui.takeQuestChoose.size.maxY")),
              list);
      return paginatedTransform.apply(pane, view);
    }
    return pane;
  }

  public ChestPane getAbortQuestPane(ChestPane pane, InterfaceView<ChestPane, PlayerViewer> view) {
    final Player player = view.arguments().get(ArgumentKey.of("player", Player.class));

    if (view.arguments().contains(ArgumentKey.of("questPlayer", QuestPlayer.class))) {
      final QuestPlayer questPlayer =
          view.arguments().get(ArgumentKey.of("questPlayer", QuestPlayer.class));
      if (questPlayer == null) {
        return pane;
      }
      ArrayList<ItemStackElement<ChestPane>> list =
          new ArrayList<>() {
            {
              int count = 0;
              for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                final ItemStack materialToUse;
                if (!activeQuest.isCompleted()) {
                  materialToUse = activeQuest.getQuest().getTakeItem();
                } else {
                  materialToUse = new ItemStack(Material.EMERALD_BLOCK);
                }

                if (main.getConfiguration().showQuestItemAmount) {
                  count++;
                  materialToUse.setAmount(count);
                }

                ItemMeta itemMeta = materialToUse.getItemMeta();

                itemMeta.displayName(
                    main.getLanguageManager()
                        .getComponent(
                            "gui.abortQuestChoose.button.abortQuestPreview.name",
                            player,
                            activeQuest));
                itemMeta.lore(
                    main.getLanguageManager()
                        .getComponentList(
                            "gui.abortQuestChoose.button.abortQuestPreview.lore",
                            player,
                            activeQuest));

                materialToUse.setItemMeta(itemMeta);

                add(
                    ItemStackElement.of(
                        materialToUse,
                        // Handle click
                        (clickHandler) -> {
                          // final @NonNull InterfaceArguments argument = view.arguments();
                          showAbortQuestGUI(questPlayer, activeQuest);
                        }));
              }
            }
          };
      PaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> paginatedTransform =
          new PaginatedTransform<>(Vector2.at(3, 1), Vector2.at(8, 5), list);
      return paginatedTransform.apply(pane, view);
    }
    return pane;
  }
}
