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

    // Ghost Mode
    public static boolean ghostMode = false;
    public static boolean hideTyping = true;
    public static boolean hideOnline = true;
    public static boolean stealthRead = false;
    public static boolean stealthStories = false;

    // God Mode
    public static boolean godMode = false;
    public static boolean preserveDeletedMessages = false;

    // Kiwigram Plus (Themes & Typography & Customization)
    public static int customFontIndex = 0;
    public static int customBubbleStyle = 0;
    public static int customAccentColor = 0;
    public static boolean compactChatList = false;
    public static boolean showDirectShare = true;

    // Swipe UI Actions
    // 0: default/forward, 1: reply, 2: context-action (copy/save)
    public static int messageSwipeUpAction = 0;
    public static int messageSwipeCenterAction = 1;
    public static int messageSwipeDownAction = 2;

    static {
        loadConfig();
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
            ghostMode = prefs.getBoolean("ghostMode", false);
            hideTyping = prefs.getBoolean("hideTyping", true);
            hideOnline = prefs.getBoolean("hideOnline", true);
            stealthRead = prefs.getBoolean("stealthRead", false);
            stealthStories = prefs.getBoolean("stealthStories", false);

            godMode = prefs.getBoolean("godMode", false);
            preserveDeletedMessages = prefs.getBoolean("preserveDeletedMessages", false);

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
            editor.putBoolean("ghostMode", ghostMode);
            editor.putBoolean("hideTyping", hideTyping);
            editor.putBoolean("hideOnline", hideOnline);
            editor.putBoolean("stealthRead", stealthRead);
            editor.putBoolean("stealthStories", stealthStories);

            editor.putBoolean("godMode", godMode);
            editor.putBoolean("preserveDeletedMessages", preserveDeletedMessages);

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

    public static void toggleGhostMode() {
        setGhostMode(!ghostMode);
    }

    public static void setGhostMode(boolean enabled) {
        ghostMode = enabled;
        saveConfig();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                final int account = i;
                Utilities.stageQueue.postRunnable(() -> {
                    MessagesController.getInstance(account).updateTimerProc();
                });
                NotificationCenter.getInstance(i).postNotificationName(NotificationCenter.mainUserInfoChanged);
            }
        }
    }

    public static void toggleGodMode() {
        setGodMode(!godMode);
    }

    public static void setGodMode(boolean enabled) {
        godMode = enabled;
        saveConfig();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                NotificationCenter.getInstance(i).postNotificationName(NotificationCenter.mainUserInfoChanged);
            }
        }
    }

    public static boolean isGhostMode() {
        return ghostMode;
    }

    public static boolean isGodMode() {
        return godMode;
    }

    public static boolean isHideOnline() {
        return ghostMode && hideOnline;
    }

    public static boolean isHideTyping() {
        return ghostMode && hideTyping;
    }

    public static boolean isStealthRead() {
        return ghostMode && stealthRead;
    }

    public static boolean isStealthStories() {
        return ghostMode && stealthStories;
    }

    public static boolean isPreserveDeletedMessages() {
        return godMode && preserveDeletedMessages;
    }
}
