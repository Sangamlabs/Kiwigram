/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.KiwigramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class VcGhostActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int rowCount;
    private int vcHeaderRow;
    private int invisibleListenerRow;
    private int autoMuteRow;
    private int receiveAudioRow;
    private int hideRaiseHandRow;
    private int hideSpeakingRow;
    private int vcDetailRow;

    private int mtprotoHeaderRow;
    private int mtprotoStatusRow;
    private int mtprotoDetailRow;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHECK = 1;
    private static final int TYPE_DETAIL = 2;
    private static final int TYPE_INFO = 3;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mainUserInfoChanged);
        updateRows();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
    }

    private void updateRows() {
        rowCount = 0;

        vcHeaderRow = rowCount++;
        invisibleListenerRow = rowCount++;
        autoMuteRow = rowCount++;
        receiveAudioRow = rowCount++;
        hideRaiseHandRow = rowCount++;
        hideSpeakingRow = rowCount++;
        vcDetailRow = rowCount++;

        mtprotoHeaderRow = rowCount++;
        mtprotoStatusRow = rowCount++;
        mtprotoDetailRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.VcGhost));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == invisibleListenerRow) {
                boolean current = KiwigramConfig.isVcInvisibleListener(currentAccount);
                KiwigramConfig.setVcInvisibleListener(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == autoMuteRow) {
                boolean current = KiwigramConfig.isVcAutoMute(currentAccount);
                KiwigramConfig.setVcAutoMute(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == receiveAudioRow) {
                boolean current = KiwigramConfig.isVcReceiveAudio(currentAccount);
                KiwigramConfig.setVcReceiveAudio(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == hideRaiseHandRow) {
                boolean current = KiwigramConfig.isVcHideRaiseHand(currentAccount);
                KiwigramConfig.setVcHideRaiseHand(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == hideSpeakingRow) {
                boolean current = KiwigramConfig.isVcHideSpeaking(currentAccount);
                KiwigramConfig.setVcHideSpeaking(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.mainUserInfoChanged && account == currentAccount) {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == invisibleListenerRow
                    || position == autoMuteRow
                    || position == receiveAudioRow
                    || position == hideRaiseHandRow
                    || position == hideSpeakingRow;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == vcHeaderRow || position == mtprotoHeaderRow) {
                return TYPE_HEADER;
            } else if (position == invisibleListenerRow || position == autoMuteRow || position == receiveAudioRow
                    || position == hideRaiseHandRow || position == hideSpeakingRow) {
                return TYPE_CHECK;
            } else if (position == mtprotoStatusRow) {
                return TYPE_DETAIL;
            } else {
                return TYPE_INFO;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_DETAIL:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_INFO:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == vcHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.VcGhost));
                    } else if (position == mtprotoHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.VcCompatibilityNotice));
                    }
                    break;

                case TYPE_CHECK:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == invisibleListenerRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.VcInvisibleListener), KiwigramConfig.isVcInvisibleListener(currentAccount), true);
                    } else if (position == autoMuteRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.VcAutoMute), KiwigramConfig.isVcAutoMute(currentAccount), true);
                    } else if (position == receiveAudioRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.VcReceiveAudio), KiwigramConfig.isVcReceiveAudio(currentAccount), true);
                    } else if (position == hideRaiseHandRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.VcHideRaiseHand), KiwigramConfig.isVcHideRaiseHand(currentAccount), true);
                    } else if (position == hideSpeakingRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.VcHideSpeaking), KiwigramConfig.isVcHideSpeaking(currentAccount), false);
                    }
                    break;

                case TYPE_DETAIL:
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    if (position == mtprotoStatusRow) {
                        VoIPService service = VoIPService.getSharedInstance();
                        org.telegram.tgnet.TLRPC.GroupCall activeCall = service != null ? service.getGroupCall() : null;
                        if (activeCall != null && activeCall.listeners_hidden) {
                            detailCell.setTextAndValue(LocaleController.getString(R.string.VcCompatibilityActive),
                                    "Call administrator has enabled hidden listeners. Your account identity is concealed on the server.", false);
                        } else if (activeCall != null) {
                            detailCell.setTextAndValue("Visible to Participants",
                                    "The current voice chat administrator has not hidden listeners. Telegram MTProto registers listeners server-side. Auto-mute and stealth speaking suppression remain active.", false);
                        } else {
                            detailCell.setTextAndValue("Protocol Architecture",
                                    "In Telegram MTProto, listener lists are hidden only when group/channel administrators enable groupCall.listeners_hidden. Kiwigram enforces auto-mute, suppresses speaking indicators, and avoids raise-hand actions.", false);
                        }
                    }
                    break;

                case TYPE_INFO:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == vcDetailRow) {
                        infoCell.setText(LocaleController.getString(R.string.VcInvisibleListenerDetail));
                    } else if (position == mtprotoDetailRow) {
                        infoCell.setText(LocaleController.getString(R.string.VcCompatibilityNoticeDetail));
                    }
                    break;
            }
        }
    }
}
