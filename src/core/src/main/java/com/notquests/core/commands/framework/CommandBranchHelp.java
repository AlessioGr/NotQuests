package com.notquests.core.commands.framework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class CommandBranchHelp {
    private CommandBranchHelp() {}

    public static CommandMessage noPermission() {
        return CommandMessage.error("<error>You do not have permission to use this command.");
    }

    public static CommandMessage wrongSender(final String senderType) {
        return CommandMessage.error("<error>This command can only be used by a " + senderType + ".");
    }

    public static void send(
            final NQCommandSchema.Node node,
            final List<? extends NQCommandSchema.Node> path,
            final Consumer<CommandMessage> messageOutput,
            final Consumer<RichUsageLine> usageOutput) {
        messageOutput.accept(header(path));
        for (final RichUsageLine line : richUsageLines(node, path)) {
            usageOutput.accept(line);
        }
    }

    public static CommandMessage header(final List<? extends NQCommandSchema.Node> path) {
        return CommandMessage.success(
                "<main>/" + NQCommandSchema.pathSyntax(path) + " <unimportant>- available subcommands:");
    }

    public static List<UsageLine> usageLines(
            final NQCommandSchema.Node node,
            final List<? extends NQCommandSchema.Node> path) {
        final ArrayList<NQCommandSchema.Node> children = new ArrayList<>(node.children());
        children.sort(Comparator.comparing(NQCommandSchema::displayToken));
        final ArrayList<UsageLine> lines = new ArrayList<>(children.size());
        for (final NQCommandSchema.Node child : children) {
            final List<NQCommandSchema.Node> childPath = append(path, child);
            final boolean hasMore = !child.children().isEmpty();
            final String syntax = "/" + NQCommandSchema.pathSyntax(childPath);
            lines.add(new UsageLine(
                    childPath,
                    hasMore,
                    syntax + (hasMore ? " ..." : NQCommandSchema.flagsSyntax(child)),
                    syntax + (hasMore ? " " : NQCommandSchema.flagsSyntax(child))));
        }
        return List.copyOf(lines);
    }

    public static List<RichUsageLine> richUsageLines(
            final NQCommandSchema.Node node,
            final List<? extends NQCommandSchema.Node> path) {
        final ArrayList<RichUsageLine> lines = new ArrayList<>();
        for (final UsageLine usageLine : usageLines(node, path)) {
            lines.add(richUsageLine(usageLine));
        }
        return List.copyOf(lines);
    }

    public static RichUsageLine richUsageLine(final UsageLine usageLine) {
        final ArrayList<RichPart> parts = new ArrayList<>();
        parts.add(new RichPart(
                "/",
                RichStyle.MUTED,
                usageLine.clickSyntax(),
                new RichHover(List.of(new RichHoverLine(
                        RichStyle.DESCRIPTION,
                        "Click to insert this command shape"))),
                false));
        final List<NQCommandSchema.Node> path = usageLine.path();
        for (int index = 0; index < path.size(); index++) {
            final NQCommandSchema.Node node = path.get(index);
            parts.add(segmentPart(node, path.subList(0, index + 1), index > 0));
        }
        if (usageLine.hasMore()) {
            parts.add(new RichPart(
                    " ...",
                    RichStyle.MUTED,
                    "",
                    new RichHover(List.of(new RichHoverLine(
                            RichStyle.DESCRIPTION,
                            "More arguments or subcommands follow"))),
                    false));
        } else if (!path.isEmpty()) {
            final NQCommandSchema.Node executable = path.get(path.size() - 1);
            for (final NQCommandSchema.Flag flag : executable.flags()) {
                parts.add(flagPart(flag, usageLine.displaySyntax()));
            }
        }
        return new RichUsageLine(List.copyOf(parts));
    }

    public static List<CommandMessage> messages(
            final NQCommandSchema.Node node,
            final List<? extends NQCommandSchema.Node> path) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(header(path));
        for (final UsageLine line : usageLines(node, path)) {
            messages.add(CommandMessage.success(line.displaySyntax()));
        }
        return List.copyOf(messages);
    }

    private static RichPart segmentPart(
            final NQCommandSchema.Node node,
            final List<? extends NQCommandSchema.Node> pathPrefix,
            final boolean leadingSpace) {
        final boolean argument = !"LITERAL".equals(node.kindName());
        final String token = NQCommandSchema.displayToken(node);
        final String suggestedPrefix = "/" + NQCommandSchema.pathSyntax(pathPrefix) + (node.children().isEmpty() ? "" : " ");
        final ArrayList<RichHoverLine> hover = new ArrayList<>();
        hover.add(new RichHoverLine(
                argument ? RichStyle.ARGUMENT : RichStyle.LITERAL,
                argument ? "Argument " + token : "Command " + node.name()));
        final String description = NQCommandSchema.descriptionText(node.description());
        if (!description.isBlank()) {
            hover.add(new RichHoverLine(RichStyle.DESCRIPTION, description));
        }
        if (argument && node.argumentInfo() != null) {
            hover.add(new RichHoverLine(
                    RichStyle.MUTED,
                    "Accepts: " + node.argumentInfo().valueTypeName()));
        }
        hover.add(new RichHoverLine(
                RichStyle.MUTED,
                "Syntax: /" + NQCommandSchema.pathSyntax(pathPrefix)));
        return new RichPart(
                token,
                argument ? RichStyle.ARGUMENT : RichStyle.LITERAL,
                suggestedPrefix,
                new RichHover(List.copyOf(hover)),
                leadingSpace);
    }

    private static RichPart flagPart(final NQCommandSchema.Flag flag, final String syntax) {
        final String token = flag.isPresence() ? "[--" + flag.name() + "]" : "[--" + flag.name() + " <value>]";
        final ArrayList<RichHoverLine> hover = new ArrayList<>();
        hover.add(new RichHoverLine(RichStyle.FLAG, "Flag --" + flag.name()));
        final String description = NQCommandSchema.descriptionText(flag.description());
        if (!description.isBlank()) {
            hover.add(new RichHoverLine(RichStyle.DESCRIPTION, description));
        }
        if (!flag.isPresence()) {
            hover.add(new RichHoverLine(
                    RichStyle.MUTED,
                    "Accepts: " + flag.valueArgumentInfo().valueTypeName()));
        }
        return new RichPart(
                token,
                RichStyle.FLAG,
                syntax,
                new RichHover(List.copyOf(hover)),
                true);
    }

    private static List<NQCommandSchema.Node> append(
            final List<? extends NQCommandSchema.Node> path,
            final NQCommandSchema.Node child) {
        final ArrayList<NQCommandSchema.Node> copy = new ArrayList<>(path.size() + 1);
        copy.addAll(path);
        copy.add(child);
        return List.copyOf(copy);
    }

    public record UsageLine(
            List<NQCommandSchema.Node> path,
            boolean hasMore,
            String displaySyntax,
            String clickSyntax) {}

    public enum RichStyle {
        LITERAL,
        ARGUMENT,
        FLAG,
        DESCRIPTION,
        MUTED
    }

    public record RichUsageLine(List<RichPart> parts) {}

    public record RichPart(
            String text,
            RichStyle style,
            String clickSyntax,
            RichHover hover,
            boolean leadingSpace) {}

    public record RichHover(List<RichHoverLine> lines) {}

    public record RichHoverLine(RichStyle style, String text) {}
}
