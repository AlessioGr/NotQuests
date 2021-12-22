package rocks.gravili.notquests.managers;

import rocks.gravili.notquests.NotQuests;

public class GUIManager {
    private final NotQuests notQuests;

    public GUIManager(final NotQuests notQuests) {
        this.notQuests = notQuests;

        /*ChestInterface infoInterface = ChestInterface.builder()
                // This interface will have one row.
                .rows(1)
                // This interface will update every five ticks.
                .updates(true, 5)
                // Cancel all inventory click events
                .clickHandler(ClickHandler.cancel())
                // Fill the background with black stained glass panes
                .addTransform(PaperTransform.chestFill(
                        ItemStackElement.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                ))
                // Add some information to the pane
                .addTransform((pane, view) -> {
                    // Get the view arguments
                    // (Keep in mind - these arguments may be coming from a Supplier, so their values can change!)
                    final @NonNull String time = view.argument().get("time");
                    final @NonNull Player player = view.argument().get("player");

                    // Return a pane with
                    return pane.element(ItemStackElement.of(PaperItemBuilder.paper(Material.PAPER)
                                    // Add icon name
                                    .name(Component.text()
                                            .append(player.displayName())
                                            .append(Component.text("'s info"))
                                            .decoration(TextDecoration.ITALIC, false)
                                            .asComponent())
                                    // Add icon lore
                                    .loreComponents(
                                            Component.text()
                                                    .append(Component.text("Current time: "))
                                                    .append(Component.text(time))
                                                    .color(NamedTextColor.GRAY)
                                                    .decoration(TextDecoration.ITALIC, false)
                                                    .asComponent(),
                                            Component.text()
                                                    .append(Component.text("Health: "))
                                                    .append(Component.text(Double.toString(player.getHealth())))
                                                    .color(NamedTextColor.GRAY)
                                                    .decoration(TextDecoration.ITALIC, false)
                                                    .asComponent())
                                    .build(),
                            // Handle click
                            (clickEvent, clickView) -> {
                                final @NonNull InterfaceArgument argument = clickView.argument();
                                argument.set("clicks", ((Integer) argument.get("clicks")) + 1);
                                clickView.parent().open(clickView.viewer(), argument);
                            }
                    ), 4, 0);
                })
                // Set the title
                .title(Component.text("interfaces demo"))
                // Build the interface
                .build()*/
    }
}
