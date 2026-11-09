/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

public class KiwigramDeleteHelper {

    private static final int BATCH_SIZE = 100;

    public static class TaskContext {
        public volatile boolean isCancelled = false;
        public AlertDialog activeDialog;
        public TextView statusTextView;
        public TextView statsTextView;
        public ProgressBar progressBar;

        public int scannedCount = 0;
        public int foundCount = 0;
        public int deletedCount = 0;
        public int failedCount = 0;

        public final LinkedHashSet<Integer> mainPeerMessageIds = new LinkedHashSet<>();
        public final LinkedHashSet<Integer> mergedPeerMessageIds = new LinkedHashSet<>();

        public void updateUi(String status, int progressPercent, int totalTarget) {
            AndroidUtilities.runOnUIThread(() -> {
                if (activeDialog == null || isCancelled) {
                    return;
                }
                if (statusTextView != null) {
                    statusTextView.setText(status);
                }
                if (statsTextView != null) {
                    statsTextView.setText(String.format("Searched: %d | Found: %d | Deleted: %d | Failed: %d",
                            scannedCount, foundCount, deletedCount, failedCount));
                }
                if (progressBar != null) {
                    if (progressPercent >= 0) {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(progressPercent);
                    } else {
                        progressBar.setIndeterminate(true);
                    }
                }
            });
        }
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
        createProgressDialog(activity, task, "Searching your messages server-side...", () -> task.isCancelled = true);
        fragment.showDialog(task.activeDialog);

        final TLRPC.Chat finalChat = chat;
        Utilities.globalQueue.postRunnable(() -> {
            searchUserMessages(task, fragment, currentAccount, dialogId, mergeDialogId, topicId, myUserId, finalChat);
        });
    }

    private static void createProgressDialog(Context context, TaskContext task, String initialStatus, Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        task.statusTextView = new TextView(context);
        task.statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        task.statusTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        task.statusTextView.setText(initialStatus);
        container.addView(task.statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        task.statsTextView = new TextView(context);
        task.statsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        task.statsTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        task.statsTextView.setText("Searched: 0 | Found: 0 | Deleted: 0 | Failed: 0");
        container.addView(task.statsTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        task.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        task.progressBar.setIndeterminate(true);
        task.progressBar.setMax(100);
        container.addView(task.progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(container);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (di, w) -> {
            task.isCancelled = true;
            if (onCancel != null) {
                onCancel.run();
            }
            try {
                di.dismiss();
            } catch (Exception ignored) {}
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(di -> {
            task.isCancelled = true;
            if (onCancel != null) {
                onCancel.run();
            }
        });

        task.activeDialog = dialog;
    }

    private static void searchUserMessages(TaskContext task, BaseFragment fragment, int currentAccount, long dialogId, long mergeDialogId, int topicId, long myUserId, TLRPC.Chat chat) {
        searchPeerMessages(task, currentAccount, dialogId, topicId, myUserId, chat, task.mainPeerMessageIds);

        if (!task.isCancelled && mergeDialogId != 0) {
            TLRPC.Chat mergedChat = MessagesController.getInstance(currentAccount).getChat(-mergeDialogId);
            searchPeerMessages(task, currentAccount, mergeDialogId, 0, myUserId, mergedChat, task.mergedPeerMessageIds);
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
                builder.setMessage("No eligible messages sent by your account were found in this chat.");
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                fragment.showDialog(builder.create());
                return;
            }

            // Confirmation Dialog with total message count and clear warning
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(fragment.getParentActivity());
            confirmBuilder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessagesTitle));
            String desc = String.format("Found %d of your messages in this chat.\n\nAre you sure you want to permanently delete them for everyone? This operation cannot be undone.", totalFound);
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

    private static void searchPeerMessages(TaskContext task, int currentAccount, long targetDialogId, int topicId, long myUserId, TLRPC.Chat chat, LinkedHashSet<Integer> collectedIds) {
        boolean useServerSideSearch = true;
        int offsetId = 0;
        int lastOffsetId = -1;
        boolean hasMore = true;

        while (hasMore && !task.isCancelled) {
            final Object syncObj = new Object();
            final boolean[] requestFinished = new boolean[1];
            final TLRPC.messages_Messages[] resultHolder = new TLRPC.messages_Messages[1];
            final TLRPC.TL_error[] errorHolder = new TLRPC.TL_error[1];

            TLObject request;
            TLRPC.InputPeer targetPeer = MessagesController.getInstance(currentAccount).getInputPeer(targetDialogId);
            if (targetPeer == null) {
                break;
            }

            if (useServerSideSearch) {
                // Server-side filtered search by from_id = current user
                TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.peer = targetPeer;
                req.q = "";
                req.from_id = MessagesController.getInstance(currentAccount).getInputPeer(myUserId);
                req.flags |= 1;
                req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
                req.offset_id = offsetId;
                req.limit = BATCH_SIZE;
                if (topicId != 0) {
                    req.top_msg_id = topicId;
                    req.flags |= 2;
                }
                request = req;
            } else {
                // Fallback sequential scan if server returns RPC error for peer/query
                TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                req.peer = targetPeer;
                req.offset_id = offsetId;
                req.limit = BATCH_SIZE;
                request = req;
            }

            ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) -> {
                synchronized (syncObj) {
                    if (response instanceof TLRPC.messages_Messages) {
                        resultHolder[0] = (TLRPC.messages_Messages) response;
                    }
                    if (error != null) {
                        errorHolder[0] = error;
                    }
                    requestFinished[0] = true;
                    syncObj.notifyAll();
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors);

            synchronized (syncObj) {
                while (!requestFinished[0]) {
                    try {
                        syncObj.wait(6000);
                    } catch (InterruptedException ignored) {
                        task.isCancelled = true;
                        break;
                    }
                }
            }

            if (task.isCancelled) {
                break;
            }

            // Handle RPC error fallback for server-side search
            if (errorHolder[0] != null) {
                if (useServerSideSearch) {
                    // Fall back to getHistory if server does not support empty query search with from_id on this chat type
                    useServerSideSearch = false;
                    offsetId = 0;
                    lastOffsetId = -1;
                    continue;
                } else {
                    // Fatal getHistory error, abort
                    break;
                }
            }

            if (resultHolder[0] == null || resultHolder[0].messages.isEmpty()) {
                hasMore = false;
                break;
            }

            ArrayList<TLRPC.Message> messages = resultHolder[0].messages;
            int previousFound = collectedIds.size();

            for (int a = 0; a < messages.size(); a++) {
                TLRPC.Message msg = messages.get(a);
                task.scannedCount++;

                if (msg instanceof TLRPC.TL_messageEmpty || msg instanceof TLRPC.TL_messageService) {
                    continue;
                }
                if (msg.action != null && !(msg.action instanceof TLRPC.TL_messageActionEmpty)) {
                    continue;
                }

                // Strict Authorship Verification: only collect messages sent by the logged-in user
                boolean isMine = msg.out;
                if (isMine && msg.from_id != null) {
                    if (msg.from_id instanceof TLRPC.TL_peerUser) {
                        isMine = (((TLRPC.TL_peerUser) msg.from_id).user_id == myUserId);
                    } else {
                        isMine = false;
                    }
                } else if (!isMine && msg.from_id != null) {
                    if (msg.from_id instanceof TLRPC.TL_peerUser) {
                        isMine = (((TLRPC.TL_peerUser) msg.from_id).user_id == myUserId);
                    }
                }

                if (isMine && msg.id > 0) {
                    collectedIds.add(msg.id);
                }

                offsetId = msg.id;
            }

            task.foundCount = collectedIds.size();

            // Prevent infinite pagination loop
            if (offsetId == lastOffsetId || messages.size() < BATCH_SIZE) {
                hasMore = false;
            }
            lastOffsetId = offsetId;

            task.updateUi("Searching your messages...", -1, 0);

            try {
                Thread.sleep(120);
            } catch (InterruptedException ignored) {
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
        createProgressDialog(fragment.getParentActivity(), task, "Deleting messages...", () -> task.isCancelled = true);
        fragment.showDialog(task.activeDialog);

        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<Integer> mainList = new ArrayList<>(task.mainPeerMessageIds);
            deleteMessageIdBatches(task, currentAccount, dialogId, topicId, mainList, totalTarget);

            if (!task.isCancelled && mergeDialogId != 0 && !task.mergedPeerMessageIds.isEmpty()) {
                ArrayList<Integer> mergedList = new ArrayList<>(task.mergedPeerMessageIds);
                deleteMessageIdBatches(task, currentAccount, mergeDialogId, 0, mergedList, totalTarget);
            }

            final int deleted = task.deletedCount;
            final int failed = task.failedCount;
            final int remaining = Math.max(0, totalTarget - deleted - failed);

            AndroidUtilities.runOnUIThread(() -> {
                if (task.activeDialog != null) {
                    try {
                        task.activeDialog.dismiss();
                    } catch (Exception ignored) {}
                    task.activeDialog = null;
                }

                if (fragment.getParentActivity() != null) {
                    String summary = String.format("Deletion completed.\nDeleted: %d\nFailed: %d\nRemaining: %d", deleted, failed, remaining);
                    AlertDialog.Builder resultBuilder = new AlertDialog.Builder(fragment.getParentActivity());
                    resultBuilder.setTitle(LocaleController.getString(R.string.DeleteAllMyMessages));
                    resultBuilder.setMessage(summary);
                    resultBuilder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                    fragment.showDialog(resultBuilder.create());
                }
            });
        });
    }

    private static void deleteMessageIdBatches(TaskContext task, int currentAccount, long targetDialogId, int topicId, ArrayList<Integer> ids, int totalTarget) {
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
                    task.failedCount += chunk.size();
                    break;
                }
            }

            if (requestSuccess[0]) {
                task.deletedCount += chunk.size();

                // Synchronize Telegram's local storage and post UI notifications
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesStorage.getInstance(currentAccount).markMessagesAsDeleted(targetDialogId, chunk, true, true, 0, topicId);
                    MessagesStorage.getInstance(currentAccount).updateDialogsWithDeletedMessages(targetDialogId, channelId, chunk, null);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messagesDeleted, chunk, channelId, false, false, false, 0);
                });
            } else {
                task.failedCount += chunk.size();
            }

            int percent = totalTarget > 0 ? (int) (((task.deletedCount + task.failedCount) * 100.0f) / totalTarget) : 0;
            task.updateUi("Deleting messages (" + percent + "%)...", percent, totalTarget);

            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
                task.isCancelled = true;
                break;
            }
        }
    }
}
