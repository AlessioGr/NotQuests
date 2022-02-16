package rocks.gravili.notquests.paper.managers;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import cloud.commandframework.services.types.ConsumerService;
import rocks.gravili.notquests.paper.NotQuests;

public class CommandPostProcessor<C> implements CommandPostprocessor<C> {
    private final NotQuests main;

    public CommandPostProcessor(final NotQuests main){
        this.main = main;
    }

    @Override
    public void accept(final CommandPostprocessingContext<C> context) {
        if(main)
        context.getCommandContext().get()
        /* Act on the context */
        if (yourCondition) {
            /* Filter out the context so that it is never passed to the executor */
            ConsumerService.interrupt();
        }
    }

}