/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.DialogInterface;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

public class KiwigramDeleteHelper {

    private static final int BATCH_SIZE = 100;

    public static class TaskContext {
        public volatile boolean isCancelled = false;
        public AlertDialog activeDialog;
        public int scannedCount = 0;
        public int foundCount = 0;
        public final ArrayList<Integer> mainPeerMessageIds = new ArrayList<>();
        public final ArrayList<Integer> mergedPeerMessageIds = new ArrayList<>();
    }

    public static void startDeleteAllMyMessages(BaseFragment fragment, long dialogId, long mergeDialogId, int topicId) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        int currentAccount = fragment.getCurrentAccount();
        long myUserId = UserConfig.getInstance(currentAccount).getClientUserId();

        TLRPC.Chat chat = null;
        if (DialogObject.isChatDialog(dialogId)) {
            chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null && chat.broadcast && !ChatObject.hasAdminRights(chat)) {
                BulletinFactory.of(fragment).createSimpleBulletin(
                    R.drawable.outline_shield_plain_24,
                    LocaleController.getString(R.string.ErrorOccurred)
                ).show();
                return;
            }
        }

        TaskContext task = new TaskContext();

        AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));
        progressDialog.setMessage(LocaleController.getString(R.string.DeleteAllMyMessagesProgress));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(di -> task.isCancelled = true);
        progressDialog.setNegativeButton(LocaleController.getString(R.string.Cancel), (di, w) -> {
            task.isCancelled = true;
            try {
                di.dismiss();
            } catch (Exception ignored) {}
        });

        fragment.showDialog(progressDialog);
        task.activeDialog = progressDialog;

        final TLRPC.Chat finalChat = chat;
        Utilities.globalQueue.postRunnable(() -> {
            scanHistoryForUserMessages(task, fragment, currentAccount, dialogId, mergeDialogId, topicId, myUserId, finalChat);
        });
    }

    private static void scanHistoryForUserMessages(TaskContext task, BaseFragment fragment, int currentAccount, long dialogId, long mergeDialogId, int topicId, long myUserId, TLRPC.Chat chat) {
        scanPeerHistory(task, fragment, currentAccount, dialogId, topicId, myUserId, chat, task.mainPeerMessageIds);

        if (!task.isCancelled && mergeDialogId != 0) {
            TLRPC.Chat mergedChat = MessagesController.getInstance(currentAccount).getChat(-mergeDialogId);
            scanPeerHistory(task, fragment, currentAccount, mergeDialogId, 0, myUserId, mergedChat, task.mergedPeerMessageIds);
        }

        AndroidUtilities.runOnUIThread(() -> {
            if (task.activeDialog != null) {
                try {
                    task.activeDialog.dismiss();
                } catch (Exception ignored) {}
                task.activeDialog = null;
            }

            if (task.isCancelled || fragment.getParentActivity() == null) {
                return;
            }

            final int totalFound = task.mainPeerMessageIds.size() + task.mergedPeerMessageIds.size();
            if (totalFound == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));
                builder.setMessage(LocaleController.getString(R.string.NoResult));
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                fragment.showDialog(builder.create());
                return;
            }

            // Show confirmation dialog before performing deletion
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(fragment.getParentActivity());
            confirmBuilder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessagesTitle));
            String desc = LocaleController.getString(R.string.DeleteAllMyMessagesDesc) + "\n\n" +
                          LocaleController.formatPluralString("messages", totalFound);
            confirmBuilder.setMessage(desc);
            confirmBuilder.setPositiveButton(LocaleController.getString(R.string.DeleteAllMyMessagesConfirm) + " (" + totalFound + ")", (di, w) -> {
                executeBatchDeletion(task, fragment, currentAccount, dialogId, mergeDialogId, topicId, totalFound);
            });
            confirmBuilder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);

            AlertDialog confirmDialog = confirmBuilder.create();
            fragment.showDialog(confirmDialog);
            TextView posButton = (TextView) confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (posButton != null) {
                posButton.setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }
        });
    }

    private static void scanPeerHistory(TaskContext task, BaseFragment fragment, int currentAccount, long targetDialogId, int topicId, long myUserId, TLRPC.Chat chat, ArrayList<Integer> collectedIds) {
        int offsetId = 0;
        boolean hasMore = true;

        while (hasMore && !task.isCancelled) {
            final Object syncObj = new Object();
            final boolean[] requestFinished = new boolean[1];
            final TLRPC.messages_Messages[] resultHolder = new TLRPC.messages_Messages[1];

            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(targetDialogId);
            if (req.peer == null) {
                break;
            }
            req.limit = BATCH_SIZE;
            req.offset_id = offsetId;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                synchronized (syncObj) {
                    if (response instanceof TLRPC.messages_Messages) {
                        resultHolder[0] = (TLRPC.messages_Messages) response;
                    }
                    requestFinished[0] = true;
                    syncObj.notifyAll();
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors);

            synchronized (syncObj) {
                while (!requestFinished[0]) {
                    try {
                        syncObj.wait(5000);
                    } catch (InterruptedException ignored) {
                        task.isCancelled = true;
                        break;
                    }
                }
            }

            if (task.isCancelled || resultHolder[0] == null || resultHolder[0].messages.isEmpty()) {
                hasMore = false;
                break;
            }

            ArrayList<TLRPC.Message> messages = resultHolder[0].messages;
            for (int a = 0; a < messages.size(); a++) {
                TLRPC.Message msg = messages.get(a);
                task.scannedCount++;

                if (msg instanceof TLRPC.TL_messageEmpty || msg instanceof TLRPC.TL_messageService) {
                    continue;
                }
                if (msg.action != null && !(msg.action instanceof TLRPC.TL_messageActionEmpty)) {
                    continue;
                }

                // Authorship verification: verify outgoing status and matching user ID
                boolean isMine = msg.out;
                if (isMine && msg.from_id != null) {
                    if (msg.from_id instanceof TLRPC.TL_peerUser) {
                        isMine = (((TLRPC.TL_peerUser) msg.from_id).user_id == myUserId);
                    } else {
                        isMine = false;
                    }
                }

                if (isMine && msg.id > 0) {
                    collectedIds.add(msg.id);
                    task.foundCount++;
                }

                offsetId = msg.id;
            }

            if (messages.size() < BATCH_SIZE) {
                hasMore = false;
            }

            // Update scanning progress on UI
            final int scanned = task.scannedCount;
            final int found = task.foundCount;
            AndroidUtilities.runOnUIThread(() -> {
                if (task.activeDialog != null && !task.isCancelled) {
                    try {
                        task.activeDialog.setMessage(LocaleController.getString(R.string.DeleteAllMyMessagesProgress) +
                            "\nScanned: " + scanned + " | Found: " + found);
                    } catch (Exception ignored) {}
                }
            });

            // Prevent UI starvation and excessive flood
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                task.isCancelled = true;
                break;
            }
        }
    }

    private static void executeBatchDeletion(TaskContext task, BaseFragment fragment, int currentAccount, long dialogId, long mergeDialogId, int topicId, int totalTarget) {
        if (fragment.getParentActivity() == null) {
            return;
        }

        task.isCancelled = false;
        AlertDialog deleteProgress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
        deleteProgress.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));
        deleteProgress.setMessage(LocaleController.getString(R.string.DeleteAllMyMessagesProgress) + "\n0 / " + totalTarget);
        deleteProgress.setCanceledOnTouchOutside(false);
        deleteProgress.setCancelable(true);
        deleteProgress.setOnCancelListener(di -> task.isCancelled = true);
        deleteProgress.setNegativeButton(LocaleController.getString(R.string.Cancel), (di, w) -> {
            task.isCancelled = true;
            try {
                di.dismiss();
            } catch (Exception ignored) {}
        });

        fragment.showDialog(deleteProgress);
        task.activeDialog = deleteProgress;

        Utilities.globalQueue.postRunnable(() -> {
            int[] progress = new int[2]; // [0] = deleted, [1] = failed

            deleteMessageIdBatches(task, fragment, currentAccount, dialogId, topicId, task.mainPeerMessageIds, progress, totalTarget);

            if (!task.isCancelled && mergeDialogId != 0 && !task.mergedPeerMessageIds.isEmpty()) {
                deleteMessageIdBatches(task, fragment, currentAccount, mergeDialogId, 0, task.mergedPeerMessageIds, progress, totalTarget);
            }

            final int deleted = progress[0];
            final int failed = progress[1];
            final int remaining = totalTarget - deleted - failed;

            AndroidUtilities.runOnUIThread(() -> {
                if (task.activeDialog != null) {
                    try {
                        task.activeDialog.dismiss();
                    } catch (Exception ignored) {}
                    task.activeDialog = null;
                }

                if (fragment.getParentActivity() != null) {
                    String summary = LocaleController.formatString("DeleteAllMessagesSummary", R.string.DeleteAllMessagesSummary, deleted, failed, remaining);
                    AlertDialog.Builder resultBuilder = new AlertDialog.Builder(fragment.getParentActivity());
                    resultBuilder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));
                    resultBuilder.setMessage(summary);
                    resultBuilder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                    fragment.showDialog(resultBuilder.create());
                }
            });
        });
    }

    private static void deleteMessageIdBatches(TaskContext task, BaseFragment fragment, int currentAccount, long targetDialogId, int topicId, ArrayList<Integer> ids, int[] progress, int totalTarget) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        TLRPC.Chat chat = null;
        if (DialogObject.isChatDialog(targetDialogId)) {
            chat = MessagesController.getInstance(currentAccount).getChat(-targetDialogId);
        }
        final boolean isChannel = ChatObject.isChannel(chat);
        final long channelId = isChannel ? chat.id : 0;

        for (int i = 0; i < ids.size() && !task.isCancelled; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            ArrayList<Integer> chunk = new ArrayList<>(ids.subList(i, end));

            final Object syncObj = new Object();
            final boolean[] requestFinished = new boolean[1];
            final boolean[] requestSuccess = new boolean[1];
            final int[] floodWaitSec = new int[1];

            if (channelId != 0) {
                TLRPC.TL_channels_deleteMessages req = new TLRPC.TL_channels_deleteMessages();
                req.channel = MessagesController.getInstance(currentAccount).getInputChannel(channelId);
                req.id = chunk;

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    synchronized (syncObj) {
                        if (error == null) {
                            requestSuccess[0] = true;
                        } else {
                            if (error.text != null && error.text.startsWith("FLOOD_WAIT_")) {
                                floodWaitSec[0] = Utilities.parseInt(error.text);
                            }
                        }
                        requestFinished[0] = true;
                        syncObj.notifyAll();
                    }
                });
            } else {
                TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
                req.id = chunk;
                req.revoke = true;

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    synchronized (syncObj) {
                        if (error == null) {
                            requestSuccess[0] = true;
                        } else {
                            if (error.text != null && error.text.startsWith("FLOOD_WAIT_")) {
                                floodWaitSec[0] = Utilities.parseInt(error.text);
                            }
                        }
                        requestFinished[0] = true;
                        syncObj.notifyAll();
                    }
                });
            }

            synchronized (syncObj) {
                while (!requestFinished[0]) {
                    try {
                        syncObj.wait(10000);
                    } catch (InterruptedException ignored) {
                        task.isCancelled = true;
                        break;
                    }
                }
            }

            if (task.isCancelled) {
                break;
            }

            if (floodWaitSec[0] > 0) {
                if (floodWaitSec[0] <= 30) {
                    try {
                        Thread.sleep(floodWaitSec[0] * 1000L);
                    } catch (InterruptedException ignored) {
                        task.isCancelled = true;
                        break;
                    }
                    // Retry this chunk
                    i -= BATCH_SIZE;
                    continue;
                } else {
                    progress[1] += chunk.size();
                    break;
                }
            }

            if (requestSuccess[0]) {
                // Server confirmed deletion!
                progress[0] += chunk.size();

                // Synchronize Telegram's local storage and post UI notifications
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesStorage.getInstance(currentAccount).markMessagesAsDeleted(targetDialogId, chunk, true, true, 0, topicId);
                    MessagesStorage.getInstance(currentAccount).updateDialogsWithDeletedMessages(targetDialogId, channelId, chunk, null);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messagesDeleted, chunk, channelId, false, false, false, 0);

                    if (task.activeDialog != null && !task.isCancelled) {
                        try {
                            task.activeDialog.setMessage(LocaleController.getString(R.string.DeleteAllMyMessagesProgress) +
                                "\n" + progress[0] + " / " + totalTarget);
                        } catch (Exception ignored) {}
                    }
                });
            } else {
                progress[1] += chunk.size();
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                task.isCancelled = true;
                break;
            }
        }
    }
}
