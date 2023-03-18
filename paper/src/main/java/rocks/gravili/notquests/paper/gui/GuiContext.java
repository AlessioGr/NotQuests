package rocks.gravili.notquests.paper.gui;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;

public class GuiContext {
    private Player player;
    private Quest quest;
    private ActiveQuest activeQuest;
    private NQNPC nqnpc;

    private Category category;

    public GuiContext() {

    }

    public GuiContext(Player player, Quest quest, ActiveQuest activeQuest, NQNPC nqnpc, Category category) {
        this.player = player;
        this.quest = quest;
        this.activeQuest = activeQuest;
        this.nqnpc = nqnpc;
        this.category = category;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Quest getQuest() {
        return quest;
    }

    public ActiveQuest getActiveQuest() {
        return activeQuest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public void setActiveQuest(ActiveQuest activeQuest) {
        this.activeQuest = activeQuest;
    }

    public NQNPC getNqnpc() {
        return nqnpc;
    }

    public void setNqnpc(NQNPC nqnpc) {
        this.nqnpc = nqnpc;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Object[] getAsObjectArray() {
        return new Object[]{
                player,
                activeQuest,
                quest,
                nqnpc,
                category
        };
    }

    public GuiContext clone() {
        return new GuiContext(player, quest, activeQuest, nqnpc, category);
    }
}
