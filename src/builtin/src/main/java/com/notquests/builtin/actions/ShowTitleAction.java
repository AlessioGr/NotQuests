package com.notquests.builtin.actions;

import com.notquests.core.managers.UtilManager;
import com.notquests.core.platform.NotQuestsAdapter;

import java.time.Duration;

public final class ShowTitleAction {
    private static final String TITLE = "title";
    private static final String FADE_IN = "fadeIn";
    private static final String STAY = "stay";
    private static final String FADE_OUT = "fadeOut";

    private static final Duration DEFAULT_FADE_IN = Duration.ofMillis(500);
    private static final Duration DEFAULT_STAY = Duration.ofMillis(3000);
    private static final Duration DEFAULT_FADE_OUT = Duration.ofMillis(500);

    private ShowTitleAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("ShowTitle")
                .displayName("Show Title")
                .description("Shows a title overlay in the center of the target player's screen.")
                .field(
                        TITLE,
                        adapter.fields().greedyText().config("specifics.title"),
                        "Title text shown in the center of the target player's screen. Use a vertical bar (`|`) to add a subtitle.")
                .flag(
                        FADE_IN,
                        adapter.fields().duration(DEFAULT_FADE_IN).config("specifics.fadeInMillis"),
                        "How long the title should fade in. Examples: 250ms, 1s.")
                .flag(
                        STAY,
                        adapter.fields().duration(DEFAULT_STAY).config("specifics.stayMillis"),
                        "How long the title should stay fully visible. Examples: 3s, 1500ms.")
                .flag(
                        FADE_OUT,
                        adapter.fields().duration(DEFAULT_FADE_OUT).config("specifics.fadeOutMillis"),
                        "How long the title should fade out. Examples: 500ms, 1s.")
                .singleLine((action, arguments) -> {
                    if (arguments.size() >= 5 && arguments.get(0).equalsIgnoreCase("timed")) {
                        action.setValue(FADE_IN, UtilManager.parseDuration(arguments.get(1)));
                        action.setValue(STAY, UtilManager.parseDuration(arguments.get(2)));
                        action.setValue(FADE_OUT, UtilManager.parseDuration(arguments.get(3)));
                        action.setValue(TITLE, String.join(" ", arguments.subList(4, arguments.size())));
                        return;
                    }
                    action.setValue(TITLE, String.join(" ", arguments));
                })
                .execute((action, questPlayer, objects) -> {
                    final String rawTitle = action.text(TITLE);
                    if (questPlayer == null || !questPlayer.hasPlayer() || rawTitle.isBlank()) {
                        return;
                    }
                    final String resolved = adapter.resolveActionText(action, questPlayer, rawTitle, objects);
                    final String[] parts = resolved.split("\\|", 2);
                    questPlayer.showTitle(
                            parts[0],
                            parts.length > 1 ? parts[1] : "",
                            action.duration(FADE_IN, DEFAULT_FADE_IN),
                            action.duration(STAY, DEFAULT_STAY),
                            action.duration(FADE_OUT, DEFAULT_FADE_OUT));
                })
                .actionDescription((action, questPlayer, objects) -> "Shows title: " + action.text(TITLE))
                .register();
    }
}
