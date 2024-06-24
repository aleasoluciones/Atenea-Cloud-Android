package com.ateneacloud.drive.ui.adapter;

import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.config.GlideLoadConfig;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafCachedFile;
import com.ateneacloud.drive.data.SeafDirent;
import com.ateneacloud.drive.data.SeafDirentTrash;
import com.ateneacloud.drive.data.SeafGroup;
import com.ateneacloud.drive.data.SeafItem;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.data.SeafRepoTrash;
import com.ateneacloud.drive.transfer.DownloadTaskInfo;
import com.ateneacloud.drive.ui.NavContext;
import com.ateneacloud.drive.ui.activity.BrowserActivity;
import com.ateneacloud.drive.util.GlideApp;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeafItemAdapter extends BaseAdapter {

    private static final String DEBUG_TAG = "SeafItemAdapter";
    private ArrayList<SeafItem> items;
    private BrowserActivity mActivity;
    private boolean repoIsEncrypted;
    private boolean actionModeOn;
    private NavContext nav;
    private DataManager dataManager;

    private SparseBooleanArray mSelectedItemsIds;
    private List<Integer> mSelectedItemsPositions = Lists.newArrayList();
    private List<SeafDirent> mSelectedItemsValues = Lists.newArrayList();


    private SparseBooleanArray mSelectedTrashItemsIds;
    private List<Integer> mSelectedTrashItemsPositions = Lists.newArrayList();
    private List<SeafDirentTrash> mSelectedTrashItemsValues = Lists.newArrayList();

    private SparseBooleanArray mSelectedTrashReposIds;
    private List<Integer> mSelectedTrashReposPositions = Lists.newArrayList();
    private List<SeafRepoTrash> mSelectedTrashReposValues = Lists.newArrayList();

    /**
     * DownloadTask instance container
     **/
    private List<DownloadTaskInfo> mDownloadTaskInfos;

    public SeafItemAdapter(BrowserActivity activity) {
        mActivity = activity;
        items = Lists.newArrayList();
        mSelectedItemsIds = new SparseBooleanArray();
        mSelectedTrashItemsIds = new SparseBooleanArray();
        mSelectedTrashReposIds = new SparseBooleanArray();
        nav = mActivity.getNavContext();
        dataManager = mActivity.getDataManager();
    }

    /**
     * sort files type
     */
    public static final int SORT_BY_NAME = 9;
    /**
     * sort files type
     */
    public static final int SORT_BY_LAST_MODIFIED_TIME = 10;
    /**
     * sort files order
     */
    public static final int SORT_ORDER_ASCENDING = 11;
    /**
     * sort files order
     */
    public static final int SORT_ORDER_DESCENDING = 12;

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * To refresh downloading status of {@link com.ateneacloud.drive.ui.fragment.ReposFragment#mListView},
     * use this method to update data set.
     * <p>
     * This method should be called after the "Download folder" menu was clicked.
     *
     * @param newList
     */
    public void setDownloadTaskList(List<DownloadTaskInfo> newList) {
        if (!equalLists(newList, mDownloadTaskInfos)) {
            this.mDownloadTaskInfos = newList;
            // redraw the list
            notifyDataSetChanged();
        }
    }

    /**
     * Compare two lists
     *
     * @param newList
     * @param oldList
     * @return true if the two lists are equal,
     * false, otherwise.
     */
    private boolean equalLists(List<DownloadTaskInfo> newList, List<DownloadTaskInfo> oldList) {
        if (newList == null && oldList == null)
            return true;

        if ((newList == null && oldList != null)
                || newList != null && oldList == null
                || newList.size() != oldList.size())
            return false;

        return newList.equals(oldList);
    }

    public void addEntry(SeafItem entry) {
        items.add(entry);
        // Collections.sort(items);
        notifyDataSetChanged();
    }

    public void add(SeafItem entry) {
        items.add(entry);
    }

    public void notifyChanged() {
        notifyDataSetChanged();
    }

    @Override
    public SeafItem getItem(int position) {
        return items.get(position);
    }

    public void setItems(List<SeafDirent> dirents) {
        items.clear();
        items.addAll(dirents);
        this.mSelectedItemsIds.clear();
        this.mSelectedItemsPositions.clear();
        this.mSelectedItemsValues.clear();
    }

    public void setItemsTrash(List<SeafDirentTrash> dirents) {
        items.clear();
        items.addAll(dirents);
        this.mSelectedTrashItemsIds.clear();
        this.mSelectedTrashItemsPositions.clear();
        this.mSelectedTrashItemsValues.clear();
    }

    public void setReposTrash(List<SeafRepoTrash> repos) {
        items.clear();
        items.addAll(repos);
        this.mSelectedTrashReposIds.clear();
        this.mSelectedTrashReposPositions.clear();
        this.mSelectedTrashReposValues.clear();
    }


    public void deselectAllItems() {
        mSelectedItemsIds.clear();
        mSelectedItemsPositions.clear();
        mSelectedItemsValues.clear();
        notifyDataSetChanged();
    }

    public void selectAllItems() {
        mSelectedItemsIds.clear();
        mSelectedItemsPositions.clear();
        mSelectedItemsValues.clear();
        for (int i = 0; i < items.size(); i++) {
            mSelectedItemsIds.put(i, true);
            mSelectedItemsPositions.add(i);
            mSelectedItemsValues.add((SeafDirent) items.get(i));
        }
        notifyDataSetChanged();
    }

    public void deselectAllItemsTrash() {
        mSelectedTrashItemsIds.clear();
        mSelectedTrashItemsPositions.clear();
        mSelectedTrashItemsValues.clear();
        notifyDataSetChanged();
    }

    public void selectAllItemsTrash() {
        mSelectedTrashItemsIds.clear();
        mSelectedTrashItemsPositions.clear();
        mSelectedTrashItemsValues.clear();
        for (int i = 0; i < items.size(); i++) {
            mSelectedTrashItemsIds.put(i, true);
            mSelectedTrashItemsPositions.add(i);
            mSelectedTrashItemsValues.add((SeafDirentTrash) items.get(i));
        }
        notifyDataSetChanged();
    }

    public void deselectAllRepoTrash() {
        mSelectedTrashReposIds.clear();
        mSelectedTrashReposPositions.clear();
        mSelectedTrashReposValues.clear();
        notifyDataSetChanged();
    }

    public void selectAllReposTrash() {
        mSelectedTrashReposIds.clear();
        mSelectedTrashReposPositions.clear();
        mSelectedTrashReposValues.clear();
        for (int i = 0; i < items.size(); i++) {
            mSelectedTrashReposIds.put(i, true);
            mSelectedTrashReposPositions.add(i);
            mSelectedTrashReposValues.add((SeafRepoTrash) items.get(i));
        }
        notifyDataSetChanged();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        items.clear();
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    public boolean isEnable(int position) {
        SeafItem item = items.get(position);
        return !(item instanceof SeafGroup);
    }

    public boolean isClickable(int position) {
        SeafItem item = items.get(position);
        return !(item instanceof SeafGroup);
    }

    public int getViewTypeCount() {
        return 2;
    }

    public int getItemViewType(int position) {
        SeafItem item = items.get(position);
        if (item instanceof SeafGroup)
            return 0;
        else
            return 1;
    }

    private View getRepoView(final SeafRepo repo, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            View action = view.findViewById(R.id.expandable_toggle_button);
            ImageView downloadStatusIcon = (ImageView) view.findViewById(R.id.list_item_download_status_icon);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.list_item_download_status_progressbar);
            viewHolder = new ViewHolder(title, subtitle, multiSelect, icon, action, downloadStatusIcon, progressBar);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showRepoBottomSheet(repo);
            }
        });

        viewHolder.multiSelect.setVisibility(View.GONE);
        viewHolder.downloadStatusIcon.setVisibility(View.GONE);
        viewHolder.progressBar.setVisibility(View.GONE);
        viewHolder.title.setText(repo.getTitle());
        viewHolder.subtitle.setText(repo.getSubtitle());
        viewHolder.icon.setImageResource(repo.getIcon());
        if (repo.hasWritePermission() && !repo.isSharedRepo) {
            viewHolder.action.setVisibility(View.VISIBLE);
        } else {
            viewHolder.action.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    /**
     * Get a view for displaying a deleted Seafile repository.
     *
     * @param repo        The SeafRepoTrash object representing the repository.
     * @param convertView The recycled view to be reused, if available.
     * @param parent      The parent view that this view will be attached to.
     * @return A view displaying information about the deleted repository.
     */
    private View getRepoTrashView(final SeafRepoTrash repo, View convertView, ViewGroup parent, final int position) {
        View view = convertView;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            View action = view.findViewById(R.id.expandable_toggle_button);
            ImageView downloadStatusIcon = (ImageView) view.findViewById(R.id.list_item_download_status_icon);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.list_item_download_status_progressbar);
            viewHolder = new ViewHolder(title, subtitle, multiSelect, icon, action, downloadStatusIcon, progressBar);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showRepoTrashBottomSheet(repo);
            }
        });

        if (actionModeOn) {
            viewHolder.multiSelect.setVisibility(View.VISIBLE);
            if (mSelectedTrashReposIds.get(position)) {
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
            } else
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);

            viewHolder.multiSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSelectedTrashReposIds.get(position)) {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
                        mSelectedTrashReposIds.put(position, true);
                        mSelectedTrashReposPositions.add(position);
                        mSelectedTrashReposValues.add(repo);
                    } else {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);
                        mSelectedTrashReposIds.delete(position);
                        mSelectedTrashReposPositions.remove(Integer.valueOf(position));
                        mSelectedTrashReposValues.remove(repo);
                    }

                    mActivity.onItemSelected();
                }
            });
        } else
            viewHolder.multiSelect.setVisibility(View.GONE);

        //viewHolder.multiSelect.setVisibility(View.GONE);
        viewHolder.downloadStatusIcon.setVisibility(View.GONE);
        viewHolder.progressBar.setVisibility(View.GONE);
        viewHolder.title.setText(repo.getTitle());
        viewHolder.subtitle.setText(repo.getSubtitle());
        viewHolder.icon.setImageResource(repo.getIcon());
        if (true) {
            viewHolder.action.setVisibility(View.VISIBLE);
        } else {
            viewHolder.action.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    private View getGroupView(SeafGroup group) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.group_item, null);
        TextView tv = (TextView) view.findViewById(R.id.textview_groupname);
        String groupTitle = group.getTitle();
        if ("Organization".equals(groupTitle)) {
            groupTitle = mActivity.getString(R.string.shared_with_all);
        }
        tv.setText(groupTitle);
        return view;
    }

    private View getDirentView(final SeafDirent dirent, View convertView, ViewGroup parent, final int position) {
        View view = convertView;
        final ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            View action = view.findViewById(R.id.expandable_toggle_button);
            ImageView downloadStatusIcon = (ImageView) view.findViewById(R.id.list_item_download_status_icon);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.list_item_download_status_progressbar);
            viewHolder = new ViewHolder(title, subtitle, multiSelect, icon, action, downloadStatusIcon, progressBar);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dirent.isDir())
                    mActivity.showDirBottomSheet(dirent.getTitle(), (SeafDirent) getItem(position));
                else
                    mActivity.showFileBottomSheet(dirent.getTitle(), (SeafDirent) getItem(position));
            }
        });

        if (actionModeOn) {
            viewHolder.multiSelect.setVisibility(View.VISIBLE);
            if (mSelectedItemsIds.get(position)) {
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
            } else
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);

            viewHolder.multiSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSelectedItemsIds.get(position)) {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
                        mSelectedItemsIds.put(position, true);
                        mSelectedItemsPositions.add(position);
                        mSelectedItemsValues.add(dirent);
                    } else {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);
                        mSelectedItemsIds.delete(position);
                        mSelectedItemsPositions.remove(Integer.valueOf(position));
                        mSelectedItemsValues.remove(dirent);
                    }

                    mActivity.onItemSelected();
                }
            });
        } else
            viewHolder.multiSelect.setVisibility(View.GONE);

        viewHolder.title.setText(dirent.getTitle());
        viewHolder.icon.setTag(R.id.imageloader_uri, dirent.getTitle());
        if (dirent.isDir()) {
            viewHolder.downloadStatusIcon.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);

            viewHolder.subtitle.setText(dirent.getSubtitle());

            if (repoIsEncrypted) {
                viewHolder.action.setVisibility(View.GONE);
            } else
                viewHolder.action.setVisibility(View.VISIBLE);

            viewHolder.icon.setImageResource(dirent.getIcon());
        } else {
            viewHolder.downloadStatusIcon.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);
            viewHolder.action.setVisibility(View.VISIBLE);
            setFileView(dirent, viewHolder, position);
        }

        return view;
    }

    /**
     * use to refresh view of {@link com.ateneacloud.drive.ui.fragment.ReposFragment #mPullRefreshListView}
     * <p>
     * <h5>when to show download status icons</h5>
     * if the dirent is a file and already cached, show cached icon.</br>
     * if the dirent is a file and waiting to download, show downloading icon.</br>
     * if the dirent is a file and is downloading, show indeterminate progressbar.</br>
     * ignore directories and repos.</br>
     *
     * @param dirent
     * @param viewHolder
     * @param position
     */
    private void setFileView(SeafDirent dirent, ViewHolder viewHolder, int position) {
        String repoName = nav.getRepoName();
        String repoID = nav.getRepoID();
        String filePath = Utils.pathJoin(nav.getDirPath(), dirent.name);
        if (repoName == null || repoID == null)
            return;

        File file = null;
        try {
            file = dataManager.getLocalRepoFile(repoName, repoID, filePath);
        } catch (RuntimeException e) {
            mActivity.showShortToast(mActivity, mActivity.getResources().getString(R.string.storage_space_insufficient));
            e.printStackTrace();
            return;
        }
        boolean cacheExists = false;

        if (file.exists() && file.length() == dirent.getFileSize()) {
            SeafCachedFile cf = dataManager.getCachedFile(repoName, repoID, filePath);
            String subtitle = null;
            subtitle = dirent.getSubtitle();
            if (cf != null) {
                cacheExists = true;
            }
            // show file download finished
            viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
            viewHolder.downloadStatusIcon.setImageResource(R.drawable.list_item_download_finished);
            viewHolder.subtitle.setText(subtitle);
            viewHolder.progressBar.setVisibility(View.GONE);

        } else {
            int downloadStatusIcon = R.drawable.list_item_download_waiting;
            if (mDownloadTaskInfos != null) {
                for (DownloadTaskInfo downloadTaskInfo : mDownloadTaskInfos) {
                    // use repoID and path to identify the task
                    if (downloadTaskInfo.repoID.equals(repoID)
                            && downloadTaskInfo.pathInRepo.equals(filePath)) {
                        switch (downloadTaskInfo.state) {
                            case INIT:
                            case FAILED:
                                downloadStatusIcon = R.drawable.list_item_download_waiting;
                                viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            case CANCELLED:
                                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            case TRANSFERRING:
                                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                                viewHolder.progressBar.setVisibility(View.VISIBLE);
                                break;
                            case FINISHED:
                                downloadStatusIcon = R.drawable.list_item_download_finished;
                                viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            default:
                                downloadStatusIcon = R.drawable.list_item_download_waiting;
                                break;
                        }
                    }
                }
            } else {
                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
            }

            viewHolder.downloadStatusIcon.setImageResource(downloadStatusIcon);
            viewHolder.subtitle.setText(dirent.getSubtitle());
        }
        if (Utils.isViewableImage(file.getName())) {
            String url = dataManager.getImageThumbnailLink(repoName, repoID, filePath, getThumbnailWidth());
            if (url == null) {
                viewHolder.icon.setImageResource(dirent.getIcon());
            } else {
                GlideApp.with(viewHolder.action)
                        .load(GlideLoadConfig.getGlideUrl(url))
                        .apply(GlideLoadConfig.getOptions(dirent.size + ""))
                        .thumbnail(0.1f)
                        .into(viewHolder.icon);

            }
        } else {
            viewHolder.icon.setImageResource(dirent.getIcon());
        }

    }

    /**
     * Get a view for displaying a deleted Seafile file or directory.
     *
     * @param dirent      The SeafDirentTrash object representing the file or directory.
     * @param convertView The recycled view to be reused, if available.
     * @param parent      The parent view that this view will be attached to.
     * @param position    The position of the item in the list.
     * @return A view displaying information about the deleted file or directory.
     */
    private View getDirentTrashView(final SeafDirentTrash dirent, View convertView, ViewGroup parent, final int position) {
        View view = convertView;
        final ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            View action = view.findViewById(R.id.expandable_toggle_button);
            ImageView downloadStatusIcon = (ImageView) view.findViewById(R.id.list_item_download_status_icon);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.list_item_download_status_progressbar);
            viewHolder = new ViewHolder(title, subtitle, multiSelect, icon, action, downloadStatusIcon, progressBar);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showDirentTrashBottomSheet(dirent.getTitle(), (SeafDirentTrash) getItem(position));
            }
        });


        if (actionModeOn) {
            viewHolder.multiSelect.setVisibility(View.VISIBLE);
            if (mSelectedTrashItemsIds.get(position)) {
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
            } else
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);

            viewHolder.multiSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSelectedTrashItemsIds.get(position)) {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
                        mSelectedTrashItemsIds.put(position, true);
                        mSelectedTrashItemsPositions.add(position);
                        mSelectedTrashItemsValues.add(dirent);
                    } else {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);
                        mSelectedTrashItemsIds.delete(position);
                        mSelectedTrashItemsPositions.remove(Integer.valueOf(position));
                        mSelectedTrashItemsValues.remove(dirent);
                    }

                    mActivity.onItemSelected();
                }
            });
        } else
            viewHolder.multiSelect.setVisibility(View.GONE);

        viewHolder.title.setText(dirent.getTitle());
        viewHolder.icon.setTag(R.id.imageloader_uri, dirent.getTitle());

        if (dirent.isDir()) {
            viewHolder.downloadStatusIcon.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);

            if (!dirent.getSubtitle().equals("")) {
                viewHolder.subtitle.setVisibility(View.VISIBLE);
                viewHolder.subtitle.setText(dirent.getSubtitle());
            } else {
                viewHolder.subtitle.setVisibility(View.GONE);
            }

            if (repoIsEncrypted || !dirent.isRoot) {
                viewHolder.action.setVisibility(View.GONE);
            } else
                viewHolder.action.setVisibility(View.VISIBLE);

            viewHolder.icon.setImageResource(dirent.getIcon());
        } else {
            viewHolder.downloadStatusIcon.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);
            if (dirent.isRoot) {
                viewHolder.action.setVisibility(View.VISIBLE);
            } else {
                viewHolder.action.setVisibility(View.GONE);
            }

            setFileTrashView(dirent, viewHolder, position);
        }


        return view;
    }

    /**
     * Set the view for displaying a deleted Seafile file, including its download status and icon.
     *
     * @param dirent     The SeafDirentTrash object representing the file.
     * @param viewHolder The ViewHolder containing views for displaying file information.
     * @param position   The position of the item in the list.
     */
    private void setFileTrashView(SeafDirentTrash dirent, ViewHolder viewHolder, int position) {
        if (nav.getDirPath() == null) {
            return;
        }
        String repoName = nav.getRepoName();
        String repoID = nav.getRepoID();

        String filePath = Utils.pathJoin(nav.getDirPath(), dirent.name);

        if (repoName == null || repoID == null)
            return;

        File file = null;
        try {
            file = dataManager.getLocalRepoFile(repoName, repoID, filePath);
        } catch (RuntimeException e) {
            mActivity.showShortToast(mActivity, mActivity.getResources().getString(R.string.storage_space_insufficient));
            e.printStackTrace();
            return;
        }
        boolean cacheExists = false;

        if (file.exists() && file.length() == dirent.getFileSize()) {
            SeafCachedFile cf = dataManager.getCachedFile(repoName, repoID, filePath);
            String subtitle = null;
            subtitle = dirent.getSubtitle();
            if (cf != null) {
                cacheExists = true;
            }
            // show file download finished
            viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
            viewHolder.downloadStatusIcon.setImageResource(R.drawable.list_item_download_finished);

            if (!subtitle.equals("")) {
                viewHolder.subtitle.setVisibility(View.VISIBLE);
                viewHolder.subtitle.setText(subtitle);
            } else {
                viewHolder.subtitle.setVisibility(View.GONE);
            }

            viewHolder.progressBar.setVisibility(View.GONE);

        } else {
            int downloadStatusIcon = R.drawable.list_item_download_waiting;
            if (mDownloadTaskInfos != null) {
                for (DownloadTaskInfo downloadTaskInfo : mDownloadTaskInfos) {
                    // use repoID and path to identify the task
                    if (downloadTaskInfo.repoID.equals(repoID)
                            && downloadTaskInfo.pathInRepo.equals(filePath)) {
                        switch (downloadTaskInfo.state) {
                            case INIT:
                            case FAILED:
                                downloadStatusIcon = R.drawable.list_item_download_waiting;
                                viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            case CANCELLED:
                                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            case TRANSFERRING:
                                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                                viewHolder.progressBar.setVisibility(View.VISIBLE);
                                break;
                            case FINISHED:
                                downloadStatusIcon = R.drawable.list_item_download_finished;
                                viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
                                viewHolder.progressBar.setVisibility(View.GONE);
                                break;
                            default:
                                downloadStatusIcon = R.drawable.list_item_download_waiting;
                                break;
                        }
                    }
                }
            } else {
                viewHolder.downloadStatusIcon.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
            }

            viewHolder.downloadStatusIcon.setImageResource(downloadStatusIcon);
            viewHolder.subtitle.setText(dirent.getSubtitle());
        }
        if (Utils.isViewableImage(file.getName())) {
            String url = dataManager.getImageThumbnailLink(repoName, repoID, filePath, getThumbnailWidth());
            if (url == null) {
                viewHolder.icon.setImageResource(dirent.getIcon());
            } else {
                GlideApp.with(viewHolder.action)
                        .load(GlideLoadConfig.getGlideUrl(url))
                        .apply(GlideLoadConfig.getOptions(dirent.size + ""))
                        .thumbnail(0.1f)
                        .into(viewHolder.icon);

            }
        } else {
            viewHolder.icon.setImageResource(dirent.getIcon());
        }

    }

    private View getCacheView(SeafCachedFile item, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            View action = view.findViewById(R.id.expandable_toggle_button);
            ImageView downloadStatusIcon = (ImageView) view.findViewById(R.id.list_item_download_status_icon);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.list_item_download_status_progressbar);
            viewHolder = new ViewHolder(title, subtitle, multiSelect, icon, action, downloadStatusIcon, progressBar);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.downloadStatusIcon.setVisibility(View.VISIBLE);
        viewHolder.downloadStatusIcon.setImageResource(R.drawable.list_item_download_finished);
        viewHolder.progressBar.setVisibility(View.GONE);
        viewHolder.title.setText(item.getTitle());
        viewHolder.subtitle.setText(item.getSubtitle());
        viewHolder.icon.setImageResource(item.getIcon());
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SeafItem item = items.get(position);
        if (item instanceof SeafRepo) {
            return getRepoView((SeafRepo) item, convertView, parent);
        } else if (item instanceof SeafRepoTrash) {
            return getRepoTrashView((SeafRepoTrash) item, convertView, parent, position);
        } else if (item instanceof SeafGroup) {
            return getGroupView((SeafGroup) item);
        } else if (item instanceof SeafCachedFile) {
            return getCacheView((SeafCachedFile) item, convertView, parent);
        } else if (item instanceof SeafDirentTrash) {
            return getDirentTrashView((SeafDirentTrash) item, convertView, parent, position);
        } else {
            return getDirentView((SeafDirent) item, convertView, parent, position);
        }
    }

    public void setActionModeOn(boolean actionModeOn) {
        this.actionModeOn = actionModeOn;
    }

    public void toggleSelection(int position) {
        if (mSelectedItemsIds.get(position)) {
            // unselected
            mSelectedItemsIds.delete(position);
            mSelectedItemsPositions.remove(Integer.valueOf(position));
            mSelectedItemsValues.remove(items.get(position));
        } else {
            mSelectedItemsIds.put(position, true);
            mSelectedItemsPositions.add(position);
            mSelectedItemsValues.add((SeafDirent) items.get(position));
        }

        mActivity.onItemSelected();
        notifyDataSetChanged();
    }

    public void toggleTrashSelection(int position) {
        if (mSelectedTrashItemsIds.get(position)) {
            // unselected
            mSelectedTrashItemsIds.delete(position);
            mSelectedTrashItemsPositions.remove(Integer.valueOf(position));
            mSelectedTrashItemsValues.remove(items.get(position));
        } else {
            mSelectedTrashItemsIds.put(position, true);
            mSelectedTrashItemsPositions.add(position);
            mSelectedTrashItemsValues.add((SeafDirentTrash) items.get(position));
        }

        mActivity.onItemSelected();
        notifyDataSetChanged();
    }

    public void toggleTrashRepoSelection(int position) {
        if (mSelectedTrashReposIds.get(position)) {
            // unselected
            mSelectedTrashReposIds.delete(position);
            mSelectedTrashReposPositions.remove(Integer.valueOf(position));
            mSelectedTrashReposValues.remove(items.get(position));
        } else {
            mSelectedTrashReposIds.put(position, true);
            mSelectedTrashReposPositions.add(position);
            mSelectedTrashReposValues.add((SeafRepoTrash) items.get(position));
        }

        mActivity.onItemSelected();
        notifyDataSetChanged();
    }


    public int getCheckedItemCount() {
        return mSelectedItemsIds.size();
    }

    public List<SeafDirent> getSelectedItemsValues() {
        return mSelectedItemsValues;
    }

    public int getCheckedTrashItemCount() {
        return mSelectedTrashItemsIds.size();
    }

    public List<SeafDirentTrash> getSelectedTrashItemsValues() {
        return mSelectedTrashItemsValues;
    }

    public int getCheckedTrashRepoCount() {
        return mSelectedTrashReposIds.size();
    }

    public List<SeafRepoTrash> getSelectedTrashRepoValues() {
        return mSelectedTrashReposValues;
    }

    private static class ViewHolder {
        TextView title, subtitle;
        ImageView icon, multiSelect, downloadStatusIcon; // downloadStatusIcon used to show file downloading status, it is invisible by
        // default
        ProgressBar progressBar;
        View action;

        public ViewHolder(TextView title,
                          TextView subtitle,
                          ImageView multiSelect,
                          ImageView icon,
                          View action,
                          ImageView downloadStatusIcon,
                          ProgressBar progressBar
        ) {
            super();
            this.icon = icon;
            this.multiSelect = multiSelect;
            this.action = action;
            this.title = title;
            this.subtitle = subtitle;
            this.downloadStatusIcon = downloadStatusIcon;
            this.progressBar = progressBar;
        }

    }

    private int getThumbnailWidth() {
        return (int) SeadroidApplication.getAppContext().getResources().getDimension(R.dimen.lv_icon_width);
    }

    public void setEncryptedRepo(boolean encrypted) {
        repoIsEncrypted = encrypted;
    }

    /**
     * Sorts the given list by type of {@link #SORT_BY_NAME} or {@link #SORT_BY_LAST_MODIFIED_TIME},
     * and by order of {@link #SORT_ORDER_ASCENDING} or {@link #SORT_ORDER_DESCENDING}
     */
    public void sortFiles(int type, int order) {
        List<SeafGroup> groups = Lists.newArrayList();
        List<SeafCachedFile> cachedFiles = Lists.newArrayList();
        List<SeafDirent> folders = Lists.newArrayList();
        List<SeafDirent> files = Lists.newArrayList();
        List<SeafDirentTrash> foldersTrash = Lists.newArrayList();
        List<SeafDirentTrash> filesTrash = Lists.newArrayList();
        List<SeafRepoTrash> repoTrashes = Lists.newArrayList();
        SeafGroup group = null;

        for (SeafItem item : items) {
            if (item instanceof SeafGroup) {
                group = (SeafGroup) item;
                groups.add(group);
            } else if (item instanceof SeafRepo) {
                if (group == null)
                    continue;
                group.addIfAbsent((SeafRepo) item);
            } else if (item instanceof SeafCachedFile) {
                cachedFiles.add(((SeafCachedFile) item));
            } else if (item instanceof SeafRepoTrash) {
                repoTrashes.add((SeafRepoTrash) item);
            } else if (item instanceof SeafDirentTrash) {
                if (((SeafDirentTrash) item).isDir())
                    foldersTrash.add(((SeafDirentTrash) item));
                else
                    filesTrash.add(((SeafDirentTrash) item));
            } else {
                if (((SeafDirent) item).isDir())
                    folders.add(((SeafDirent) item));
                else
                    files.add(((SeafDirent) item));
            }
        }

        items.clear();

        // sort SeafGroups and SeafRepos
        for (SeafGroup sg : groups) {
            sg.sortByType(type, order);
            items.add(sg);
            items.addAll(sg.getRepos());
        }

        // sort SeafDirents and SeafReposTrash
        if (type == SORT_BY_NAME) {
            // sort by name, in ascending order
            Collections.sort(foldersTrash, new SeafDirentTrash.DirentNameComparator());
            Collections.sort(filesTrash, new SeafDirentTrash.DirentNameComparator());
            Collections.sort(folders, new SeafDirent.DirentNameComparator());
            Collections.sort(files, new SeafDirent.DirentNameComparator());
            Collections.sort(repoTrashes, new SeafRepoTrash.RepoNameComparator());
            if (order == SORT_ORDER_DESCENDING) {
                Collections.reverse(foldersTrash);
                Collections.reverse(filesTrash);
                Collections.reverse(folders);
                Collections.reverse(files);
                Collections.reverse(repoTrashes);
            }
        } else if (type == SORT_BY_LAST_MODIFIED_TIME) {
            // sort by last modified time, in ascending order
            Collections.sort(foldersTrash, new SeafDirentTrash.DirentLastMTimeComparator());
            Collections.sort(filesTrash, new SeafDirentTrash.DirentLastMTimeComparator());
            Collections.sort(folders, new SeafDirent.DirentLastMTimeComparator());
            Collections.sort(files, new SeafDirent.DirentLastMTimeComparator());
            Collections.sort(repoTrashes, new SeafRepoTrash.RepoLastMTimeComparator());
            if (order == SORT_ORDER_DESCENDING) {
                Collections.reverse(foldersTrash);
                Collections.reverse(filesTrash);
                Collections.reverse(folders);
                Collections.reverse(files);
                Collections.reverse(repoTrashes);
            }
        }
        // Adds the objects in the specified collection to this ArrayList
        items.addAll(cachedFiles);
        items.addAll(folders);
        items.addAll(files);
        items.addAll(foldersTrash);
        items.addAll(filesTrash);
        items.addAll(repoTrashes);
    }

    public ArrayList<SeafItem> getItems() {
        return items;
    }
}

