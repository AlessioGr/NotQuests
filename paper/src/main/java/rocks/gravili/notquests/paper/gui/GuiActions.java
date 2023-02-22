package rocks.gravili.notquests.paper.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.gui.propertytype.ActionPropertyType;
import rocks.gravili.notquests.paper.gui.propertytype.ConditionPropertyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiActions {

    private final NotQuests notQuests;

    public GuiActions(NotQuests notQuests) {
        this.notQuests = notQuests;
    }

    public void cancelClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    public void executeAction(InventoryClickEvent event, boolean close, ActionPropertyType actionExpressions, ConditionPropertyType conditionExpressions) {

        notQuests.getLogManager().debug("Actions called: ");
        var actionStrings = cleanUpStrings(actionExpressions.toStringList());
        notQuests.getLogManager().debug("Witch conditions: ");
        var conditionStrings = cleanUpStrings(conditionExpressions.toStringList());

        if (notQuests.getConversationManager() == null) {
            return;
        }

        if (event.getWhoClicked() instanceof Player player) {
            var questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());

            var actions = notQuests.getConversationManager().parseActionString(actionStrings);
            var conditions = notQuests.getConversationManager().parseConditionsString(conditionStrings);

            actions.forEach(action -> {
                if (conditions != null) {
                    conditions.forEach(condition -> action.addCondition(condition, false, null, null));
                }
                notQuests.getActionManager().executeActionWithConditions(action, questPlayer, player, true);
            });
            if (close) {
                player.closeInventory();
            }
        }
    }

    private List<String> cleanUpStrings(List<String> toClean) {
        var newStrings = new ArrayList<String>();
        toClean.forEach(
                expression -> {
                    if (!expression.isBlank()) {
                        notQuests.getLogManager().debug(" -> after: " + expression);
                        var cleanedExpression = expression.replaceAll("^\\s*", "");
                        newStrings.add(cleanedExpression);
                        notQuests.getLogManager().debug(" -> after: " + cleanedExpression);
                    }
                }
        );
        return newStrings;
    }
}
