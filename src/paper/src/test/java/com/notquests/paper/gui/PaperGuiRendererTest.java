package com.notquests.paper.gui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.gui.GuiService.GuiAction;
import com.notquests.core.gui.GuiService.GuiSlot;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import xyz.xenondevs.invui.Click;

class PaperGuiRendererTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void clickingAGuiItemRunsItsCoreActions() {
        final NotQuests notQuests = mock(NotQuests.class);
        final NotQuestsPlugin core = mock(NotQuestsPlugin.class);
        final PaperPlayer questPlayer = mock(PaperPlayer.class);
        final Player player = mock(Player.class);
        final List<GuiAction> actions = List.of(GuiAction.close());
        final GuiSlot slot = new GuiSlot(0, "stone", "", "", List.of(), actions);
        final ResolvedGui resolvedGui = new ResolvedGui("test", "Test", 1, List.of(slot));
        when(notQuests.getCorePlugin()).thenReturn(core);

        final var gui = new PaperGuiRenderer(notQuests).buildGui(resolvedGui, questPlayer);
        gui.handleClick(0, new Click(player, ClickType.LEFT));

        verify(core).runGuiActions(eq(questPlayer), eq(actions), any());
    }
}
