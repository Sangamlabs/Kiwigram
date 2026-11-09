/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.messenger;

import org.junit.Assert;
import org.junit.Test;

public class KiwigramConfigTest {

    @Test
    public void testMultiAccountIsolation() {
        // Verify account 0 and account 1 maintain distinct, independent preferences
        KiwigramConfig.setGhostMode(0, true);
        KiwigramConfig.setGhostMode(1, false);

        Assert.assertTrue("Account 0 ghost mode should be enabled", KiwigramConfig.isGhostMode(0));
        Assert.assertFalse("Account 1 ghost mode should be disabled", KiwigramConfig.isGhostMode(1));

        KiwigramConfig.setGodMode(0, false);
        KiwigramConfig.setGodMode(1, true);

        Assert.assertFalse("Account 0 god mode should be false", KiwigramConfig.isGodMode(0));
        Assert.assertTrue("Account 1 god mode should be true", KiwigramConfig.isGodMode(1));
    }

    @Test
    public void testGhostModeModularSwitches() {
        int account = 0;
        KiwigramConfig.setGhostMode(account, true);

        KiwigramConfig.setSeenGhost(account, true);
        KiwigramConfig.setOnlineGhost(account, false);
        KiwigramConfig.setTypingGhost(account, true);
        KiwigramConfig.setRecordingGhost(account, false);
        KiwigramConfig.setStoryGhost(account, true);

        Assert.assertTrue("Seen ghost should be active", KiwigramConfig.isSeenGhost(account));
        Assert.assertFalse("Online ghost should be inactive", KiwigramConfig.isOnlineGhost(account));
        Assert.assertTrue("Typing ghost should be active", KiwigramConfig.isTypingGhost(account));
        Assert.assertFalse("Recording ghost should be inactive", KiwigramConfig.isRecordingGhost(account));
        Assert.assertTrue("Story ghost should be active", KiwigramConfig.isStoryGhost(account));

        // When master Ghost Mode is disabled, modular active getters should report false while raw values are preserved
        KiwigramConfig.setGhostMode(account, false);
        Assert.assertFalse("Seen ghost should be inactive when master is OFF", KiwigramConfig.isSeenGhost(account));
        Assert.assertFalse("Online ghost should be inactive when master is OFF", KiwigramConfig.isOnlineGhost(account));
        Assert.assertFalse("Typing ghost should be inactive when master is OFF", KiwigramConfig.isTypingGhost(account));
        Assert.assertTrue("Raw seen ghost state should be preserved", KiwigramConfig.getRawSeenGhost(account));
    }

    @Test
    public void testVcGhostPreferences() {
        int account = 0;
        // Verify default preferences
        Assert.assertTrue("Invisible listener should default to ON", KiwigramConfig.isVcInvisibleListener(account));
        Assert.assertTrue("Auto mute should default to ON", KiwigramConfig.isVcAutoMute(account));
        Assert.assertTrue("Receive audio should default to ON", KiwigramConfig.isVcReceiveAudio(account));
        Assert.assertTrue("Hide raise hand should default to ON", KiwigramConfig.isVcHideRaiseHand(account));
        Assert.assertTrue("Hide speaking should default to ON", KiwigramConfig.isVcHideSpeaking(account));

        // Toggle each independently
        KiwigramConfig.setVcInvisibleListener(account, false);
        Assert.assertFalse("Invisible listener toggled off", KiwigramConfig.isVcInvisibleListener(account));

        KiwigramConfig.setVcAutoMute(account, false);
        Assert.assertFalse("Auto mute toggled off", KiwigramConfig.isVcAutoMute(account));

        KiwigramConfig.setVcHideSpeaking(account, false);
        Assert.assertFalse("Hide speaking toggled off", KiwigramConfig.isVcHideSpeaking(account));
    }

    @Test
    public void testGodModeAndRemoteHistoryPreservation() {
        int account = 0;
        KiwigramConfig.setGodMode(account, true);
        KiwigramConfig.setPreserveDeletedMessages(account, true);

        Assert.assertTrue("God mode should be active", KiwigramConfig.isGodMode(account));
        Assert.assertTrue("Preserve deleted messages should be active", KiwigramConfig.isPreserveDeletedMessages(account));

        // Disabling God mode turns off active preservation
        KiwigramConfig.setGodMode(account, false);
        Assert.assertFalse("Preserve deleted messages should be inactive when God mode is OFF", KiwigramConfig.isPreserveDeletedMessages(account));
        Assert.assertTrue("Raw preserve deleted messages preference preserved", KiwigramConfig.getRawPreserveDeletedMessages(account));
    }
}
