package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ActionSelector;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.NQItemSelector;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.items.NQItem;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class AdminItemsCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> editBuilder;


    public AdminItemsCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> editBuilder) {
        this.main = main;
        this.manager = manager;
        this.editBuilder = editBuilder;

        manager.command(editBuilder.literal("create")
                .argument(StringArgument.of("name"), ArgumentDescription.of("Item Name"))
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of what this item should be based on. If you use 'hand', the item you are holding in your main hand will be used."))
                .meta(CommandMeta.DESCRIPTION, "Creates a new Item.")
                .handler((context) -> {
                    final String itemName = context.get("name");

                    for(Material material : Material.values()){
                        if(itemName.equalsIgnoreCase(material.name())){
                            context.getSender().sendMessage(main.parse(
                                    "<error>Error: The item <highlight>" + itemName + "</highlight> already exists! You cannot use item names identical to vanilla Minecraft item names."
                            ));
                            return;
                        }
                    }

                    if(main.getItemsManager().getItem(itemName) != null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: The item <highlight>" + itemName + "</highlight> already exists!"
                        ));
                        return;
                    }

                    final MaterialOrHand materialOrHand = context.get("material");

                    ItemStack itemStack;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemStack = player.getInventory().getItemInMainHand().clone();
                            itemStack.setAmount(1);
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            context.getSender().sendMessage(main.parse(
                                    "<error>You cannot use <highlight>'any'</highlight> here!"
                            ));
                            return;
                        }
                        itemStack = main.getItemsManager().getItemStack(materialOrHand.material);
                    }

                    NQItem nqItem = new NQItem(main, itemName, itemStack);


                    if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                        nqItem.setCategory(category);
                    }
                    main.getItemsManager().addItem(nqItem);

                    context.getSender().sendMessage(main.parse(
                            "<success>The item <highlight>" + itemName + "</highlight> has been added successfully!"
                    ));
                }));

        manager.command(editBuilder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all items")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse("<highlight>All Items:"));
                    int counter = 1;

                    for(NQItem nqItem : main.getItemsManager().getItems()){
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + nqItem.getItemName() + "</main> <highlight2>Type: <main>" + nqItem.getItemStack().getType().name()));
                        counter++;
                    }
                }));


        Command.Builder<CommandSender>  admitItemsEditBuilder = editBuilder
                .literal("edit", "e")
                .argument(NQItemSelector.of("item", main), ArgumentDescription.of("NotQuests Item which you want to edit."));

        handleEditCommands(admitItemsEditBuilder);
    }

    public void handleEditCommands(Command.Builder<CommandSender> builder){
        manager.command(builder.literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes a NotQuests Item.")
                .handler((context) -> {
                    NQItem nqItem = context.get("item");

                    main.getItemsManager().deleteItem(nqItem);

                    context.getSender().sendMessage(main.parse(
                            "<success>The item <highlight>" + nqItem.getItemName() + "</highlight> has been deleted successfully!"
                    ));
                }));
    }
}
