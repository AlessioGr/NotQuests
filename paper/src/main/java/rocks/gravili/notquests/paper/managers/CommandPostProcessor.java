package rocks.gravili.notquests.paper.managers;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import cloud.commandframework.services.types.ConsumerService;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;

public class CommandPostProcessor<C> implements CommandPostprocessor<C> {
    private final NotQuests main;

    public CommandPostProcessor(final NotQuests main){
        this.main = main;
    }

    @Override
    public void accept(@NotNull final CommandPostprocessingContext<C> context) {
        if(main.getDataManager().isDisabled() && !(context.getCommand().getArguments().size() >= 3 && (context.getCommand().getArguments().get(2).getName().equalsIgnoreCase("enablePluginAndSaving") || context.getCommand().getArguments().get(2).getName().equalsIgnoreCase("disablePluginAndSaving") )) ){
            if(context.getCommandContext().getSender() instanceof final CommandSender commandSender){
                main.getDataManager().sendPluginDisabledMessage(commandSender);
            }
            ConsumerService.interrupt();
        }
    }

}