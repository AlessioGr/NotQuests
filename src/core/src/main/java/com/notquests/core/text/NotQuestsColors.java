package com.notquests.core.text;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public final class NotQuestsColors {
  public static final TextColor main = TextColor.color(0x1985ff);
  public static final String mainMM = "<#1985ff>";
  public static final TextColor highlight = TextColor.color(0xff45ae);
  public static final String highlightMM = "<#ff45ae>";
  public static final TextColor highlight2 = TextColor.color(0xff004c);
  public static final TextColor lightHighlight = TextColor.color(0x4da0ff);
  public static final String lightHighlightMM = "<#4da0ff>";

  public static final String debugTitleGradient = "<gradient:#abfff5:#c7fff8>";
  public static final String debugGradient = "<gradient:#8a98c2:#8ac2bb>";
  public static final String debugHighlightGradient = "<gradient:#009dff:#00e5ff>";

  public static String formatDebug(final String message) {
    return debugTitleGradient + "[NotQuests Debug]</gradient> "
            + debugGradient + (message == null ? "" : message) + "</gradient>";
  }

  private NotQuestsColors() {}

  @FunctionalInterface
  public interface Palette {
    List<String> colors(String tagName);
  }

  public static final class Messages {
    private final MiniMessage miniMessage;
    private final TagResolver tagResolver;

    public Messages(final Palette colorsForTag) {
      this.tagResolver = NotQuestsMiniMessage.tagResolver(colorsForTag);
      this.miniMessage = MiniMessage.builder()
              .preProcessor(NotQuestsMiniMessage::normalizeTags)
              .tags(tagResolver)
              .build();
    }

    public MiniMessage miniMessage() {
      return miniMessage;
    }

    public TagResolver tagResolver() {
      return tagResolver;
    }
  }
}
