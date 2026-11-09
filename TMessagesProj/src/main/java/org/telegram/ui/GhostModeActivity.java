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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.KiwigramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class GhostModeActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int rowCount;
    private int masterHeaderRow;
    private int masterGhostRow;
    private int masterGhostDetailRow;

    private int featuresHeaderRow;
    private int seenGhostRow;
    private int onlineGhostRow;
    private int typingGhostRow;
    private int recordingGhostRow;
    private int storyGhostRow;
    private int featuresDetailRow;

    private int vcHeaderRow;
    private int vcGhostSubmenuRow;
    private int vcGhostDetailRow;

    private static final int TYPE_SETTINGS = 0;
    private static final int TYPE_INFO = 1;
    private static final int TYPE_HEADER = 2;
    private static final int TYPE_CHECK = 3;
    private static final int TYPE_SHADOW = 4;

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

        masterHeaderRow = rowCount++;
        masterGhostRow = rowCount++;
        masterGhostDetailRow = rowCount++;

        featuresHeaderRow = rowCount++;
        seenGhostRow = rowCount++;
        onlineGhostRow = rowCount++;
        typingGhostRow = rowCount++;
        recordingGhostRow = rowCount++;
        storyGhostRow = rowCount++;
        featuresDetailRow = rowCount++;

        vcHeaderRow = rowCount++;
        vcGhostSubmenuRow = rowCount++;
        vcGhostDetailRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.GhostMode));
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
            if (position == masterGhostRow) {
                KiwigramConfig.toggleGhostMode(currentAccount);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KiwigramConfig.isGhostMode(currentAccount));
                }
                listAdapter.notifyDataSetChanged();
            } else if (position == seenGhostRow) {
                boolean current = KiwigramConfig.getRawSeenGhost(currentAccount);
                KiwigramConfig.setSeenGhost(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == onlineGhostRow) {
                boolean current = KiwigramConfig.getRawOnlineGhost(currentAccount);
                KiwigramConfig.setOnlineGhost(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == typingGhostRow) {
                boolean current = KiwigramConfig.getRawTypingGhost(currentAccount);
                KiwigramConfig.setTypingGhost(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == recordingGhostRow) {
                boolean current = KiwigramConfig.getRawRecordingGhost(currentAccount);
                KiwigramConfig.setRecordingGhost(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == storyGhostRow) {
                boolean current = KiwigramConfig.getRawStoryGhost(currentAccount);
                KiwigramConfig.setStoryGhost(currentAccount, !current);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!current);
                }
            } else if (position == vcGhostSubmenuRow) {
                presentFragment(new VcGhostActivity());
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
            return position == masterGhostRow
                    || position == seenGhostRow
                    || position == onlineGhostRow
                    || position == typingGhostRow
                    || position == recordingGhostRow
                    || position == storyGhostRow
                    || position == vcGhostSubmenuRow;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == masterHeaderRow || position == featuresHeaderRow || position == vcHeaderRow) {
                return TYPE_HEADER;
            } else if (position == masterGhostRow || position == seenGhostRow || position == onlineGhostRow
                    || position == typingGhostRow || position == recordingGhostRow || position == storyGhostRow) {
                return TYPE_CHECK;
            } else if (position == vcGhostSubmenuRow) {
                return TYPE_SETTINGS;
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
                case TYPE_SETTINGS:
                    view = new TextSettingsCell(mContext);
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
                    if (position == masterHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.GhostMode));
                    } else if (position == featuresHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.GhostModeDescription));
                    } else if (position == vcHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.VcGhost));
                    }
                    break;

                case TYPE_CHECK:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == masterGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.GhostMode), KiwigramConfig.isGhostMode(currentAccount), false);
                    } else if (position == seenGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.SeenGhost), KiwigramConfig.getRawSeenGhost(currentAccount), true);
                    } else if (position == onlineGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.OnlineGhost), KiwigramConfig.getRawOnlineGhost(currentAccount), true);
                    } else if (position == typingGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.TypingGhost), KiwigramConfig.getRawTypingGhost(currentAccount), true);
                    } else if (position == recordingGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.RecordingGhost), KiwigramConfig.getRawRecordingGhost(currentAccount), true);
                    } else if (position == storyGhostRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.StoryGhost), KiwigramConfig.getRawStoryGhost(currentAccount), false);
                    }
                    break;

                case TYPE_SETTINGS:
                    TextSettingsCell settingsCell = (TextSettingsCell) holder.itemView;
                    if (position == vcGhostSubmenuRow) {
                        boolean invisible = KiwigramConfig.isVcInvisibleListener(currentAccount);
                        settingsCell.setTextAndValue(LocaleController.getString(R.string.VcGhost),
                                invisible ? LocaleController.getString(R.string.NotificationsOn) : LocaleController.getString(R.string.NotificationsOff), false);
                    }
                    break;

                case TYPE_INFO:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == masterGhostDetailRow) {
                        infoCell.setText(LocaleController.getString(R.string.GhostModeDescription));
                    } else if (position == featuresDetailRow) {
                        infoCell.setText(LocaleController.getString(R.string.SeenGhostDetail));
                    } else if (position == vcGhostDetailRow) {
                        infoCell.setText(LocaleController.getString(R.string.VcGhostDetail));
                    }
                    break;
            }
        }
    }
}
