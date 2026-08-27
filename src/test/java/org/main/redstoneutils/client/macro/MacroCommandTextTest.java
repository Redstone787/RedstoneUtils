package org.main.redstoneutils.client.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroCommandTextTest {

    @Test
    void normalizesCommandsAndAliases() {
        assertEquals("say hello", MacroCommandText.normalizeCommand("/// say hello"));
        assertEquals("build-fast", MacroCommandText.normalizeAlias("/Build-Fast"));
    }

    @Test
    void validatesOnlySingleCommandRoots() {
        assertTrue(MacroCommandText.isValidAliasInput("/build_fast"));
        assertFalse(MacroCommandText.isValidAliasInput("build fast"));
        assertFalse(MacroCommandText.isValidAliasInput("build.fast"));
    }

    @Test
    void appendsArgumentsWithoutExtraWhitespace() {
        assertEquals("time set day", MacroCommandText.appendArguments("/time", " set day "));
    }
}
