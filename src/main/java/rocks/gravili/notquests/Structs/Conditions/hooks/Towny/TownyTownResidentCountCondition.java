package rocks.gravili.notquests.Structs.Conditions.hooks.Towny;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Conditions.ConditionFor;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class TownyTownResidentCountCondition extends Condition {

    private int minTownResidentCount = 1;

    public TownyTownResidentCountCondition(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.isTownyEnabled()) {
            return;
        }

        manager.command(builder.literal("TownyTownResidentCount")
                .argument(IntegerArgument.<CommandSender>newBuilder("min Resident Count").withMin(1), ArgumentDescription.of("Minimum Town resident count"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new TownyTownResidentCount Condition")
                .handler((context) -> {
                    final int minResidentCount = context.get("min Resident Count");

                    TownyTownResidentCountCondition townyTownResidentCountCondition = new TownyTownResidentCountCondition(main);
                    townyTownResidentCountCondition.setMinTownResidentCount(minResidentCount);


                    main.getConditionsManager().addCondition(townyTownResidentCountCondition, context);
                }));
    }

    public final int getMinTownResidentCount() {
        return minTownResidentCount;
    }

    public void setMinTownResidentCount(final int minTownResidentCount) {
        this.minTownResidentCount = minTownResidentCount;
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        if (!main.isTownyEnabled()) {
            return "<YELLOW>Error: The server does not have Towny enabled. Please ask the Owner to install Towny for Towny stuff to work.";
        }

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUUID());
            if (resident != null && resident.getTownOrNull() != null && resident.hasTown()) {
                Town town = resident.getTownOrNull();
                if (town.getNumResidents() >= getMinTownResidentCount()) {
                    return "";
                } else {
                    return "<YELLOW>Your town needs to have at least <AQUA>" + getMinTownResidentCount() + "</AQUA> residents.";
                }
            } else {
                return "<YELLOW>You need to be in a town";
            }


        } else {
            return "<YELLOW>Error reading TownyTownResidentCount requirement...";

        }
    }

    @Override
    public String getConditionDescription() {
        return "<GRAY>-- Minimum town residents: " + getMinTownResidentCount();
    }


    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minTownResidentCount", getMinTownResidentCount());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.minTownResidentCount = configuration.getInt(initialPath + ".specifics.minTownResidentCount");

    }
}