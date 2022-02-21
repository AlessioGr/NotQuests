package rocks.gravili.notquests.paper.structs.variables.hooks;

import cloud.commandframework.arguments.standard.StringArgument;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.config.QuestPackage;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.ConditionID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class BetonQuestConditionVariable extends Variable<Boolean> {
    private ConditionID cachedConditionID = null;


    public BetonQuestConditionVariable(NotQuests main) {
        super(main);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("package").withSuggestionsProvider((context, lastString) -> {

                    final ArrayList<String> completions = new ArrayList<>(Config.getPackages().keySet());

                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Package Name]", "[Event Name]");

                    return completions;
                }).build()
        );

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("condition").withSuggestionsProvider((context, lastString) -> {
                    String packageName = context.get("package");
                    final QuestPackage configPack = Config.getPackages().get(packageName);
                    ConfigurationSection conditionsFileConfiguration = configPack.getConfig().getConfigurationSection("conditions");
                    if(conditionsFileConfiguration == null){
                        return new ArrayList<>();
                    }
                    final ArrayList<String> completions = new ArrayList<>(conditionsFileConfiguration.getKeys(false));

                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition Name]", "[...]");

                    return completions;
                }).build()
        );
    }

    public final ConditionID getConditionID(){
        if(cachedConditionID == null){
            final QuestPackage configPack = Config.getPackages().get(getRequiredStringValue("package"));
            try{
                cachedConditionID = new ConditionID(configPack, getRequiredStringValue("condition"));
            }catch (final ObjectNotFoundException e) {
                main.getLogManager().warn("Tried to check BetonQuestCondition Variable, but the BetonQuest condition was not found: " + e.getMessage());
                return null;
            }
        }
        return cachedConditionID;
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        return player != null && BetonQuest.condition(player.getUniqueId().toString(), getConditionID());
    }

    @Override
    public boolean setValueInternally(Boolean newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
        if(together.equalsIgnoreCase(".")){
            return "BetonQuest Conditions";
        }else{
            return "BetonQuest " + together + " Conditions";
        }
    }

    @Override
    public String getSingular() {
        String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
        if(together.equalsIgnoreCase(".")){
            return "BetonQuest Condition";
        }else{
            return "BetonQuest " + together + " Condition";
        }
    }
}
