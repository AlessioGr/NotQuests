package rocks.gravili.notquests.managers;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.structs.ActiveQuest;
import rocks.gravili.notquests.structs.QuestPlayer;

public class GUIManager {
    private final NotQuests main;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GUIManager(final NotQuests main) {
        this.main = main;
    }

    public final String convert(final String old) { //Converts MiniMessage to legacy
        return main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old);
    }

    public void showActiveQuests(QuestPlayer questPlayer, Player player) {
        if (questPlayer != null) {
            String[] guiSetup = {
                    "zxxxxxxxx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "pxxxxxxxn"
            };
            InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.activeQuests.title", player)), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1));

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                final ItemStack materialToUse;
                if (!activeQuest.isCompleted()) {
                    materialToUse = activeQuest.getQuest().getTakeItem();
                } else {
                    materialToUse = new ItemStack(Material.EMERALD_BLOCK);
                }

                if (main.getConfiguration().showQuestItemAmount) {
                    count++;
                }

                group.addElement(new StaticGuiElement('e',
                        materialToUse,
                        count,
                        click -> {
                            player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                            return true;
                        },
                        convert(main.getLanguageManager().getString("gui.activeQuests.button.activeQuestButton.text", player, activeQuest))
                ));
            }

            gui.addElement(group);

            // Previous page
            gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
            // Next page
            gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

            gui.show(player);
        } else {
            player.sendMessage(miniMessage.parse(
                    main.getLanguageManager().getString("chat.no-quests-accepted", player)
            ));
        }
    }
}
