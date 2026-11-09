/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public class KiwigramConfig {

    private static final String PREFS_NAME = "kiwigram_prefs";
    private static final Object sync = new Object();
    private static boolean configLoaded = false;

    private static final int MAX_ACCOUNTS = UserConfig.MAX_ACCOUNT_COUNT;

    // Per-account Ghost Mode modular states
    private static final boolean[] ghostMode = new boolean[MAX_ACCOUNTS];
    private static final boolean[] seenGhost = new boolean[MAX_ACCOUNTS];
    private static final boolean[] onlineGhost = new boolean[MAX_ACCOUNTS];
    private static final boolean[] typingGhost = new boolean[MAX_ACCOUNTS];
    private static final boolean[] recordingGhost = new boolean[MAX_ACCOUNTS];
    private static final boolean[] storyGhost = new boolean[MAX_ACCOUNTS];

    // Per-account VC Ghost settings
    private static final boolean[] vcInvisibleListener = new boolean[MAX_ACCOUNTS];
    private static final boolean[] vcAutoMute = new boolean[MAX_ACCOUNTS];
    private static final boolean[] vcReceiveAudio = new boolean[MAX_ACCOUNTS];
    private static final boolean[] vcHideRaiseHand = new boolean[MAX_ACCOUNTS];
    private static final boolean[] vcHideSpeaking = new boolean[MAX_ACCOUNTS];

    // Per-account God Mode settings
    private static final boolean[] godMode = new boolean[MAX_ACCOUNTS];
    private static final boolean[] preserveDeletedMessages = new boolean[MAX_ACCOUNTS];

    // UI Customization & Gestures (App-wide)
    public static int customFontIndex = 0;
    public static int customBubbleStyle = 0;
    public static int customAccentColor = 0;
    public static boolean compactChatList = false;
    public static boolean showDirectShare = true;

    // Multi-Action Swipe UI Actions
    // 0: default/forward, 1: reply, 2: context-action (copy/save)
    public static int messageSwipeUpAction = 0;
    public static int messageSwipeCenterAction = 1;
    public static int messageSwipeDownAction = 2;

    static {
        for (int i = 0; i < MAX_ACCOUNTS; i++) {
            onlineGhost[i] = true;
            typingGhost[i] = true;
            recordingGhost[i] = true;
            vcInvisibleListener[i] = true;
            vcAutoMute[i] = true;
            vcReceiveAudio[i] = true;
            vcHideRaiseHand[i] = true;
            vcHideSpeaking[i] = true;
        }
        loadConfig();
    }

    private static int sanitizeAccount(int account) {
        if (account < 0 || account >= MAX_ACCOUNTS) {
            return UserConfig.selectedAccount >= 0 && UserConfig.selectedAccount < MAX_ACCOUNTS
                    ? UserConfig.selectedAccount : 0;
        }
        return account;
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            if (ApplicationLoader.applicationContext == null) {
                return;
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Legacy fallback defaults
            boolean legacyGhost = prefs.getBoolean("ghostMode", false);
            boolean legacyOnline = prefs.getBoolean("hideOnline", true);
            boolean legacyTyping = prefs.getBoolean("hideTyping", true);
            boolean legacySeen = prefs.getBoolean("stealthRead", false);
            boolean legacyStories = prefs.getBoolean("stealthStories", false);
            boolean legacyGod = prefs.getBoolean("godMode", false);
            boolean legacyPreserve = prefs.getBoolean("preserveDeletedMessages", false);

            for (int i = 0; i < MAX_ACCOUNTS; i++) {
                String prefix = "acc_" + i + "_";
                ghostMode[i] = prefs.getBoolean(prefix + "ghostMode", i == 0 ? legacyGhost : false);
                seenGhost[i] = prefs.getBoolean(prefix + "seenGhost", i == 0 ? legacySeen : false);
                onlineGhost[i] = prefs.getBoolean(prefix + "onlineGhost", i == 0 ? legacyOnline : true);
                typingGhost[i] = prefs.getBoolean(prefix + "typingGhost", i == 0 ? legacyTyping : true);
                recordingGhost[i] = prefs.getBoolean(prefix + "recordingGhost", true);
                storyGhost[i] = prefs.getBoolean(prefix + "storyGhost", i == 0 ? legacyStories : false);

                vcInvisibleListener[i] = prefs.getBoolean(prefix + "vcInvisibleListener", true);
                vcAutoMute[i] = prefs.getBoolean(prefix + "vcAutoMute", true);
                vcReceiveAudio[i] = prefs.getBoolean(prefix + "vcReceiveAudio", true);
                vcHideRaiseHand[i] = prefs.getBoolean(prefix + "vcHideRaiseHand", true);
                vcHideSpeaking[i] = prefs.getBoolean(prefix + "vcHideSpeaking", true);

                godMode[i] = prefs.getBoolean(prefix + "godMode", i == 0 ? legacyGod : false);
                preserveDeletedMessages[i] = prefs.getBoolean(prefix + "preserveDeletedMessages", i == 0 ? legacyPreserve : false);
            }

            customFontIndex = prefs.getInt("customFontIndex", 0);
            customBubbleStyle = prefs.getInt("customBubbleStyle", 0);
            customAccentColor = prefs.getInt("customAccentColor", 0);
            compactChatList = prefs.getBoolean("compactChatList", false);
            showDirectShare = prefs.getBoolean("showDirectShare", true);

            messageSwipeUpAction = prefs.getInt("messageSwipeUpAction", 0);
            messageSwipeCenterAction = prefs.getInt("messageSwipeCenterAction", 1);
            messageSwipeDownAction = prefs.getInt("messageSwipeDownAction", 2);

            configLoaded = true;
        }
    }

    public static void saveConfig() {
        synchronized (sync) {
            if (ApplicationLoader.applicationContext == null) {
                return;
            }
            SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

            for (int i = 0; i < MAX_ACCOUNTS; i++) {
                String prefix = "acc_" + i + "_";
                editor.putBoolean(prefix + "ghostMode", ghostMode[i]);
                editor.putBoolean(prefix + "seenGhost", seenGhost[i]);
                editor.putBoolean(prefix + "onlineGhost", onlineGhost[i]);
                editor.putBoolean(prefix + "typingGhost", typingGhost[i]);
                editor.putBoolean(prefix + "recordingGhost", recordingGhost[i]);
                editor.putBoolean(prefix + "storyGhost", storyGhost[i]);

                editor.putBoolean(prefix + "vcInvisibleListener", vcInvisibleListener[i]);
                editor.putBoolean(prefix + "vcAutoMute", vcAutoMute[i]);
                editor.putBoolean(prefix + "vcReceiveAudio", vcReceiveAudio[i]);
                editor.putBoolean(prefix + "vcHideRaiseHand", vcHideRaiseHand[i]);
                editor.putBoolean(prefix + "vcHideSpeaking", vcHideSpeaking[i]);

                editor.putBoolean(prefix + "godMode", godMode[i]);
                editor.putBoolean(prefix + "preserveDeletedMessages", preserveDeletedMessages[i]);
            }

            // Sync legacy keys for backward compatibility
            int sel = sanitizeAccount(UserConfig.selectedAccount);
            editor.putBoolean("ghostMode", ghostMode[sel]);
            editor.putBoolean("hideOnline", onlineGhost[sel]);
            editor.putBoolean("hideTyping", typingGhost[sel]);
            editor.putBoolean("stealthRead", seenGhost[sel]);
            editor.putBoolean("stealthStories", storyGhost[sel]);
            editor.putBoolean("godMode", godMode[sel]);
            editor.putBoolean("preserveDeletedMessages", preserveDeletedMessages[sel]);

            editor.putInt("customFontIndex", customFontIndex);
            editor.putInt("customBubbleStyle", customBubbleStyle);
            editor.putInt("customAccentColor", customAccentColor);
            editor.putBoolean("compactChatList", compactChatList);
            editor.putBoolean("showDirectShare", showDirectShare);

            editor.putInt("messageSwipeUpAction", messageSwipeUpAction);
            editor.putInt("messageSwipeCenterAction", messageSwipeCenterAction);
            editor.putInt("messageSwipeDownAction", messageSwipeDownAction);

            editor.apply();
        }
    }

    // ==========================================
    // Ghost Mode (Master & Modular)
    // ==========================================

    public static boolean isGhostMode(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc];
        }
    }

    public static boolean isGhostMode() {
        return isGhostMode(UserConfig.selectedAccount);
    }

    public static void setGhostMode(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            ghostMode[acc] = enabled;
        }
        saveConfig();
        if (UserConfig.getInstance(acc).isClientActivated()) {
            Utilities.stageQueue.postRunnable(() -> {
                MessagesController.getInstance(acc).updateTimerProc();
            });
            NotificationCenter.getInstance(acc).postNotificationName(NotificationCenter.mainUserInfoChanged);
        }
    }

    public static void setGhostMode(boolean enabled) {
        setGhostMode(UserConfig.selectedAccount, enabled);
    }

    public static void toggleGhostMode(int account) {
        setGhostMode(account, !isGhostMode(account));
    }

    public static void toggleGhostMode() {
        toggleGhostMode(UserConfig.selectedAccount);
    }

    public static boolean isSeenGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc] && seenGhost[acc];
        }
    }

    public static boolean isSeenGhost() {
        return isSeenGhost(UserConfig.selectedAccount);
    }

    public static boolean isStealthRead() {
        return isSeenGhost();
    }

    public static void setSeenGhost(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            seenGhost[acc] = enabled;
        }
        saveConfig();
    }

    public static void setSeenGhost(boolean enabled) {
        setSeenGhost(UserConfig.selectedAccount, enabled);
    }

    public static boolean isOnlineGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc] && onlineGhost[acc];
        }
    }

    public static boolean isOnlineGhost() {
        return isOnlineGhost(UserConfig.selectedAccount);
    }

    public static boolean isHideOnline() {
        return isOnlineGhost();
    }

    public static void setOnlineGhost(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            onlineGhost[acc] = enabled;
        }
        saveConfig();
        if (UserConfig.getInstance(acc).isClientActivated()) {
            Utilities.stageQueue.postRunnable(() -> {
                MessagesController.getInstance(acc).updateTimerProc();
            });
            NotificationCenter.getInstance(acc).postNotificationName(NotificationCenter.mainUserInfoChanged);
        }
    }

    public static void setOnlineGhost(boolean enabled) {
        setOnlineGhost(UserConfig.selectedAccount, enabled);
    }

    public static boolean isTypingGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc] && typingGhost[acc];
        }
    }

    public static boolean isTypingGhost() {
        return isTypingGhost(UserConfig.selectedAccount);
    }

    public static boolean isHideTyping() {
        return isTypingGhost();
    }

    public static void setTypingGhost(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            typingGhost[acc] = enabled;
        }
        saveConfig();
    }

    public static void setTypingGhost(boolean enabled) {
        setTypingGhost(UserConfig.selectedAccount, enabled);
    }

    public static boolean isRecordingGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc] && recordingGhost[acc];
        }
    }

    public static boolean isRecordingGhost() {
        return isRecordingGhost(UserConfig.selectedAccount);
    }

    public static void setRecordingGhost(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            recordingGhost[acc] = enabled;
        }
        saveConfig();
    }

    public static void setRecordingGhost(boolean enabled) {
        setRecordingGhost(UserConfig.selectedAccount, enabled);
    }

    public static boolean isStoryGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return ghostMode[acc] && storyGhost[acc];
        }
    }

    public static boolean isStoryGhost() {
        return isStoryGhost(UserConfig.selectedAccount);
    }

    public static boolean isStealthStories() {
        return isStoryGhost();
    }

    public static void setStoryGhost(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            storyGhost[acc] = enabled;
        }
        saveConfig();
    }

    public static void setStoryGhost(boolean enabled) {
        setStoryGhost(UserConfig.selectedAccount, enabled);
    }

    // Direct toggle getters (returns raw toggle position regardless of master toggle)
    public static boolean getRawSeenGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return seenGhost[acc];
        }
    }

    public static boolean getRawOnlineGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return onlineGhost[acc];
        }
    }

    public static boolean getRawTypingGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return typingGhost[acc];
        }
    }

    public static boolean getRawRecordingGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return recordingGhost[acc];
        }
    }

    public static boolean getRawStoryGhost(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return storyGhost[acc];
        }
    }

    // ==========================================
    // VC Ghost
    // ==========================================

    public static boolean isVcInvisibleListener(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return vcInvisibleListener[acc];
        }
    }

    public static boolean isVcInvisibleListener() {
        return isVcInvisibleListener(UserConfig.selectedAccount);
    }

    public static void setVcInvisibleListener(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            vcInvisibleListener[acc] = enabled;
        }
        saveConfig();
    }

    public static void setVcInvisibleListener(boolean enabled) {
        setVcInvisibleListener(UserConfig.selectedAccount, enabled);
    }

    public static void toggleVcInvisibleListener(int account) {
        setVcInvisibleListener(account, !isVcInvisibleListener(account));
    }

    public static void toggleVcInvisibleListener() {
        toggleVcInvisibleListener(UserConfig.selectedAccount);
    }

    public static boolean isVcAutoMute(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return vcAutoMute[acc];
        }
    }

    public static boolean isVcAutoMute() {
        return isVcAutoMute(UserConfig.selectedAccount);
    }

    public static void setVcAutoMute(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            vcAutoMute[acc] = enabled;
        }
        saveConfig();
    }

    public static void setVcAutoMute(boolean enabled) {
        setVcAutoMute(UserConfig.selectedAccount, enabled);
    }

    public static boolean isVcReceiveAudio(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return vcReceiveAudio[acc];
        }
    }

    public static boolean isVcReceiveAudio() {
        return isVcReceiveAudio(UserConfig.selectedAccount);
    }

    public static void setVcReceiveAudio(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            vcReceiveAudio[acc] = enabled;
        }
        saveConfig();
    }

    public static void setVcReceiveAudio(boolean enabled) {
        setVcReceiveAudio(UserConfig.selectedAccount, enabled);
    }

    public static boolean isVcHideRaiseHand(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return vcHideRaiseHand[acc];
        }
    }

    public static boolean isVcHideRaiseHand() {
        return isVcHideRaiseHand(UserConfig.selectedAccount);
    }

    public static void setVcHideRaiseHand(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            vcHideRaiseHand[acc] = enabled;
        }
        saveConfig();
    }

    public static void setVcHideRaiseHand(boolean enabled) {
        setVcHideRaiseHand(UserConfig.selectedAccount, enabled);
    }

    public static boolean isVcHideSpeaking(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return vcHideSpeaking[acc];
        }
    }

    public static boolean isVcHideSpeaking() {
        return isVcHideSpeaking(UserConfig.selectedAccount);
    }

    public static void setVcHideSpeaking(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            vcHideSpeaking[acc] = enabled;
        }
        saveConfig();
    }

    public static void setVcHideSpeaking(boolean enabled) {
        setVcHideSpeaking(UserConfig.selectedAccount, enabled);
    }

    // ==========================================
    // God Mode
    // ==========================================

    public static boolean isGodMode(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return godMode[acc];
        }
    }

    public static boolean isGodMode() {
        return isGodMode(UserConfig.selectedAccount);
    }

    public static void setGodMode(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            godMode[acc] = enabled;
        }
        saveConfig();
        if (UserConfig.getInstance(acc).isClientActivated()) {
            NotificationCenter.getInstance(acc).postNotificationName(NotificationCenter.mainUserInfoChanged);
        }
    }

    public static void setGodMode(boolean enabled) {
        setGodMode(UserConfig.selectedAccount, enabled);
    }

    public static void toggleGodMode(int account) {
        setGodMode(account, !isGodMode(account));
    }

    public static void toggleGodMode() {
        toggleGodMode(UserConfig.selectedAccount);
    }

    public static boolean isPreserveDeletedMessages(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return godMode[acc] && preserveDeletedMessages[acc];
        }
    }

    public static boolean isPreserveDeletedMessages() {
        return isPreserveDeletedMessages(UserConfig.selectedAccount);
    }

    public static boolean getRawPreserveDeletedMessages(int account) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            return preserveDeletedMessages[acc];
        }
    }

    public static void setPreserveDeletedMessages(int account, boolean enabled) {
        int acc = sanitizeAccount(account);
        synchronized (sync) {
            preserveDeletedMessages[acc] = enabled;
        }
        saveConfig();
    }

    public static void setPreserveDeletedMessages(boolean enabled) {
        setPreserveDeletedMessages(UserConfig.selectedAccount, enabled);
    }
}
