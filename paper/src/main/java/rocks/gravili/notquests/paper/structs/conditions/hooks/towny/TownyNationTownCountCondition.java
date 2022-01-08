package rocks.gravili.notquests.paper.structs.conditions.hooks.towny;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.ConditionFor;

public class TownyNationTownCountCondition extends Condition {

    private int minTownCount = 1;

    public TownyNationTownCountCondition(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return;
        }

        manager.command(builder
                .argument(IntegerArgument.<CommandSender>newBuilder("min Town Count").withMin(1), ArgumentDescription.of("Minimum Town count"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new TownyNationTownCount Condition")
                .handler((context) -> {
                    final int minTownCount = context.get("min Town Count");

                    TownyNationTownCountCondition townyNationTownCountCondition = new TownyNationTownCountCondition(main);
                    townyNationTownCountCondition.setMinTownCount(minTownCount);


                    main.getConditionsManager().addCondition(townyNationTownCountCondition, context);
                }));
    }

    public final int getMinTownCount() {
        return minTownCount;
    }

    public void setMinTownCount(final int minTownCount) {
        this.minTownCount = minTownCount;
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return "<YELLOW>Error: The server does not have Towny enabled. Please ask the Owner to install Towny for Towny stuff to work.";
        }

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUUID());
            if (resident != null && resident.getTownOrNull() != null && resident.hasNation() && resident.getNationOrNull() != null) {
                Nation nation = resident.getNationOrNull();
                if (nation.getNumTowns() >= getMinTownCount()) {
                    return "";
                } else {
                    return "<YELLOW>Your nation needs to have at least <highlight>" + getMinTownCount() + "</highlight> towns.";
                }
            } else {
                return "<YELLOW>You need to be in a nation";
            }


        } else {
            return "<YELLOW>Error reading TownyTownResidentCount requirement...";

        }
    }

    @Override
    public String getConditionDescription() {
        return "<GRAY>-- Minimum towns in nation: " + getMinTownCount();
    }


    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minTownCount", getMinTownCount());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.minTownCount = configuration.getInt(initialPath + ".specifics.minTownCount");

    }
}