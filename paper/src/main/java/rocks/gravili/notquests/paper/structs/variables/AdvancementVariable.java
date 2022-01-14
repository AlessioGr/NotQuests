package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdvancementVariable extends Variable<Boolean>{
    public AdvancementVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);


        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Advancement").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Advancement Name]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            Iterator<Advancement> advancements = Bukkit.getServer().advancementIterator();
                            while (advancements.hasNext()){
                                Advancement advancement = advancements.next();
                                suggestions.add(advancement.getKey().getKey());
                            }
                            return suggestions;

                        }
                ).single().build()
        );
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(getRequiredStringValue("Advancement"));
        if(namespacedKey == null){
            return false;
        }
        Advancement advancement = Bukkit.getAdvancement(namespacedKey);
        if(advancement == null){
            return false;
        }
        return player != null && player.getAdvancementProgress(
                advancement
        ).isDone();
    }

    @Override
    public boolean setValue(Boolean newValue, Player player, Object... objects) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(getRequiredStringValue("Advancement"));
        if(namespacedKey == null){
            return false;
        }
        Advancement advancement = Bukkit.getAdvancement(namespacedKey);
        if(advancement == null){
            return false;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);

        if(newValue){
            for (String criteria : progress.getRemainingCriteria()){
                progress.awardCriteria(criteria);
            }
        }else{
            for (String criteria : progress.getAwardedCriteria()){
                progress.revokeCriteria(criteria);
            }
        }

        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Advancements";
    }

    @Override
    public String getSingular() {
        return "Advancement";
    }
}
