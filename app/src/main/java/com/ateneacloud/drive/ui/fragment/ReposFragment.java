package com.ateneacloud.drive.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.ListFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.view.ActionMode;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cocosw.bottomsheet.BottomSheet;
import com.google.common.collect.Maps;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafCachedFile;
import com.ateneacloud.drive.data.SeafDirent;
import com.ateneacloud.drive.data.SeafDirentTrash;
import com.ateneacloud.drive.data.SeafGroup;
import com.ateneacloud.drive.data.SeafItem;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.data.SeafRepoTrash;
import com.ateneacloud.drive.ssl.CertsManager;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.ui.CopyMoveContext;
import com.ateneacloud.drive.ui.NavContext;
import com.ateneacloud.drive.ui.activity.BrowserActivity;
import com.ateneacloud.drive.ui.adapter.SeafItemAdapter;
import com.ateneacloud.drive.ui.dialog.SslConfirmDialog;
import com.ateneacloud.drive.ui.dialog.TaskDialog;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.util.Utils;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ReposFragment extends ListFragment {

    private static final String DEBUG_TAG = "ReposFragment";
    private static final String KEY_REPO_SCROLL_POSITION = "repo_scroll_position";

    private static final int REFRESH_ON_RESUME = 0;
    private static final int REFRESH_ON_PULL = 1;
    private static final int REFRESH_ON_CLICK = 2;
    private static final int REFRESH_ON_OVERFLOW_MENU = 3;
    private static int mRefreshType = -1;
    /**
     * flag to stop refreshing when nav to other directory
     */
    private static int mPullToRefreshStopRefreshing = 0;

    private SeafItemAdapter adapter;
    private BrowserActivity mActivity = null;
    private ActionMode mActionMode;
    private CopyMoveContext copyMoveContext;
    private Map<String, ScrollState> scrollPostions;

    public static final int FILE_ACTION_EXPORT = 0;
    public static final int FILE_ACTION_COPY = 1;
    public static final int FILE_ACTION_MOVE = 2;
    public static final int FILE_ACTION_STAR = 3;

    private SwipeRefreshLayout refreshLayout;
    private ListView mListView;
    private ImageView mEmptyView;
    private View mProgressContainer;
    private View mListContainer;
    private TextView mErrorText;

    private boolean isTimerStarted;
    private final Handler mTimer = new Handler();

    private DataManager getDataManager() {
        return mActivity.getDataManager();
    }

    private NavContext getNavContext() {
        return mActivity.getNavContext();
    }

    public SeafItemAdapter getAdapter() {
        return adapter;
    }

    public ImageView getEmptyView() {
        return mEmptyView;
    }

    public interface OnFileSelectedListener {
        void onFileSelected(SeafDirent fileName);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "ReposFragment Attached");
        mActivity = (BrowserActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.repos_fragment, container, false);
        refreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh);
        mListView = (ListView) root.findViewById(android.R.id.list);
        mEmptyView = (ImageView) root.findViewById(R.id.empty);
        mListContainer = root.findViewById(R.id.listContainer);
        mErrorText = (TextView) root.findViewById(R.id.error_message);
        mProgressContainer = root.findViewById(R.id.progressContainer);

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                startContextualActionMode(position);
                return true;
            }
        });

        refreshLayout.setColorSchemeResources(R.color.fancy_blue);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshType = REFRESH_ON_PULL;
                refreshView(true, true);
            }
        });

        return root;
    }

    /**
     * Start action mode for selecting and process multiple files/folders.
     * The contextual action mode is a system implementation of ActionMode
     * that focuses user interaction toward performing contextual actions.
     * When a user enables this mode by selecting an item,
     * a contextual action bar appears at the top of the screen
     * to present actions the user can perform on the currently selected item(s).
     * <p>
     * While this mode is enabled,
     * the user can select multiple items (if you allow it), deselect items,
     * and continue to navigate within the activity (as much as you're willing to allow).
     * <p>
     * The action mode is disabled and the contextual action bar disappears
     * when the user deselects all items, presses the BACK button, or selects the Done action on the left side of the bar.
     * <p>
     * see http://developer.android.com/guide/topics/ui/menus.html#CAB
     */
    public void startContextualActionMode(int position) {
        startContextualActionMode();

        NavContext nav = getNavContext();
        if (((adapter == null || !nav.inRepo()) && !nav.inRepoTrash()) || nav.isNavigateToDirentsTrash()) return;

        if (nav.inDirentsTrash()) {
            adapter.toggleTrashSelection(position);
        } else if (nav.inRepoTrash()) {
            adapter.toggleTrashRepoSelection(position);
        } else {
            adapter.toggleSelection(position);
        }

        updateContextualActionBar();

    }

    public void startContextualActionMode() {
        NavContext nav = getNavContext();
        if ((!nav.inRepo() && !nav.inRepoTrash()) || nav.isNavigateToDirentsTrash()) return;

        if (mActionMode == null) {
            // start the actionMode

            if (nav.inDirentsTrash()) {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallbackTrash());
            } else if (nav.inRepoTrash()) {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallbackTrashRepos());
            } else {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallback());
            }


        }

    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    public void clearActionMode(){
        if (adapter == null) return;

        adapter.setActionModeOn(false);

        adapter.deselectAllRepoTrash();
        adapter.deselectAllItemsTrash();
        adapter.deselectAllItems();

        if(mActionMode!=null) {
            mActionMode.finish();
        }
        // Here you can make any necessary updates to the activity when
        // the contextual action bar (CAB) is removed. By default, selected items are deselected/unchecked.
        mActionMode = null;
    }

    public void showRepoBottomSheet(final SeafRepo repo) {
        final BottomSheet.Builder builder = new BottomSheet.Builder(mActivity);
        builder.title(repo.getName()).sheet(R.menu.bottom_sheet_op_repo).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.rename_repo:
                        mActivity.renameRepo(repo.getID(), repo.getName());
                        break;
                    case R.id.delete_repo:
                        mActivity.deleteRepo(repo.getID());
                        break;
                }
            }
        }).show();
    }

    /**
     * Displays a bottom sheet with options for a deleted Seafile repository.
     *
     * @param repo The SeafRepoTrash object representing the deleted repository.
     */
    public void showRepoTrashBottomSheet(final SeafRepoTrash repo) {
        final BottomSheet.Builder builder = new BottomSheet.Builder(mActivity);
        builder.title(repo.getName()).sheet(R.menu.bottom_sheet_recover).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.recover_item:
                        mActivity.retrieveRepository(repo);
                        break;
                }
            }
        }).show();
    }

    public void showFileBottomSheet(String title, final SeafDirent dirent) {
        final String repoName = getNavContext().getRepoName();
        final String repoID = getNavContext().getRepoID();
        final String dir = getNavContext().getDirPath();
        final String path = Utils.pathJoin(dir, dirent.name);
        final String filename = dirent.name;
        final String localPath = getDataManager().getLocalRepoFile(repoName, repoID, path).getPath();
        final BottomSheet.Builder builder = new BottomSheet.Builder(mActivity);
        builder.title(title).sheet(R.menu.bottom_sheet_op_file).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.share:
                        mActivity.showShareDialog(repoID, path, false, dirent.size, dirent.name);
                        break;
                    case R.id.open:
                        mActivity.onFileSelected(dirent, true);
                        break;
                    case R.id.delete:
                        mActivity.deleteFile(repoID, repoName, path);
                        break;
                    case R.id.copy:
                        mActivity.copyFile(repoID, repoName, dir, filename, false);
                        break;
                    case R.id.move:
                        mActivity.moveFile(repoID, repoName, dir, filename, false);
                        break;
                    case R.id.rename:
                        mActivity.renameFile(repoID, repoName, path);
                        break;
                    case R.id.update:
                        mActivity.addUpdateTask(repoID, repoName, dir, localPath);
                        break;
                    case R.id.download:
                        mActivity.downloadFile(dirent, repoID, dir, repoName);
                        break;
                    case R.id.export:
                        mActivity.exportFile(dirent.name, dirent.size);
                        break;
                    case R.id.star:
                        mActivity.starFile(repoID, dir, filename);
                        break;
                }
            }
        });

        SeafRepo repo = getDataManager().getCachedRepoByID(repoID);
        if (!dirent.hasWritePermission()) {
            Menu menu = builder.build().getMenu();
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
        }
        if (!Utils.isTextMimeType(filename)) {
            Menu menu = builder.build().getMenu();
            menu.findItem(R.id.open).setVisible(false);
        }

        if (repo.isSharedRepo) {
            Menu menu = builder.build().getMenu();
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.open).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.update).setVisible(false);
        }

        builder.show();

        try {
            if (repo != null && repo.encrypted) {
                builder.remove(R.id.share);
            }

            SeafCachedFile cf = getDataManager().getCachedFile(repoName, repoID, path);
            if (cf != null) {
                builder.remove(R.id.download);
            } else {
                builder.remove(R.id.update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showDirBottomSheet(String title, final SeafDirent dirent) {
        final String repoName = getNavContext().getRepoName();
        final String repoID = getNavContext().getRepoID();
        final String dir = getNavContext().getDirPath();
        final String path = Utils.pathJoin(dir, dirent.name);
        final String filename = dirent.name;
        final BottomSheet.Builder builder = new BottomSheet.Builder(mActivity);
        builder.title(title).sheet(R.menu.bottom_sheet_op_dir).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.share:
                        mActivity.showShareDialog(repoID, path, true, dirent.size, dirent.name);
                        break;
                    case R.id.delete:
                        mActivity.deleteDir(repoID, repoName, path);
                        break;
                    case R.id.copy:
                        mActivity.copyFile(repoID, repoName, dir, filename, false);
                        break;
                    case R.id.move:
                        mActivity.moveFile(repoID, repoName, dir, filename, false);
                        break;
                    case R.id.rename:
                        mActivity.renameDir(repoID, repoName, path);
                        break;
                    case R.id.download:
                        mActivity.downloadDir(dirent);
                        break;
                }
            }
        });
        SeafRepo repo = getDataManager().getCachedRepoByID(repoID);
        Menu menu = builder.build().getMenu();
        if (!dirent.hasWritePermission()) {
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
        }

        if (repo.isSharedRepo) {
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
        }

        builder.show();
        try {
            if (repo != null && repo.encrypted) {
                builder.remove(R.id.share);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Displays a bottom sheet with options for a deleted Seafile directory or file.
     *
     * @param title  The title to display on the bottom sheet.
     * @param dirent The SeafDirentTrash object representing the deleted directory or file.
     */
    public void showDirentTrashBottomSheet(String title, final SeafDirentTrash dirent) {
        final BottomSheet.Builder builder = new BottomSheet.Builder(mActivity);
        builder.title(title).sheet(R.menu.bottom_sheet_recover).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.recover_item:
                        mActivity.retrieveDirent(dirent);
                        break;
                }
            }
        }).show();
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(DEBUG_TAG, "ReposFragment onActivityCreated");
        scrollPostions = Maps.newHashMap();
        adapter = new SeafItemAdapter(mActivity);

        mListView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        // Log.d(DEBUG_TAG, "ReposFragment onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        // Log.d(DEBUG_TAG, "ReposFragment onStop");
        super.onStop();
        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log.d(DEBUG_TAG, "ReposFragment onResume");
        // refresh the view (loading data)
        refreshView(true);
        mRefreshType = REFRESH_ON_RESUME;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        mActivity = null;
        // Log.d(DEBUG_TAG, "ReposFragment detached");
        super.onDetach();
    }

    public void refresh() {
        mRefreshType = REFRESH_ON_OVERFLOW_MENU;
        refreshView(true, false);
    }

    public void refreshView() {
        refreshView(false, false);
    }

    public void refreshView(boolean restorePosition) {
        refreshView(false, restorePosition);
    }

    public void refreshView(boolean forceRefresh, boolean restorePosition) {
        if (mActivity == null)
            return;

        mErrorText.setVisibility(View.GONE);
        mListContainer.setVisibility(View.VISIBLE);

        NavContext navContext = getNavContext();
        if (navContext.inRepoTrash()) {
            if (mActivity.getCurrentPosition() == BrowserActivity.INDEX_LIBRARY_TAB) {
                mActivity.enableUpButton();
            }
            navToReposTrashView(forceRefresh, restorePosition);
        } else if (navContext.inRepo() && navContext.inDirentsTrash()) {
            if (mActivity.getCurrentPosition() == BrowserActivity.INDEX_TRASH_TAB && navContext.isNavigateToDirentsTrash()) {
                mActivity.enableUpButton();
            }
            navToDirentsTrash(forceRefresh, restorePosition);
        } else if (navContext.inRepo()) {
            if (!navContext.inDirentsTrash()) {
                if (mActivity.getCurrentPosition() == BrowserActivity.INDEX_TRASH_TAB) {
                    mActivity.enableUpButton();
                }
                navToDirectory(forceRefresh, restorePosition);
            }
        } else {
            if (!navContext.inRepoTrash()) {
                mActivity.disableUpButton();
                navToReposView(forceRefresh, restorePosition);
            }
        }
       mActivity.supportInvalidateOptionsMenu();
    }


    public void navToReposView(boolean forceRefresh, boolean restorePosition) {
        //stopTimer();

        mPullToRefreshStopRefreshing++;

        if (mPullToRefreshStopRefreshing > 1) {
            refreshLayout.setRefreshing(false);
            mPullToRefreshStopRefreshing = 0;
        }

        forceRefresh = forceRefresh || isReposRefreshTimeOut();
        if ((!Utils.isNetworkOn() || !forceRefresh) && !getNavContext().inRepoTrash()) {
            List<SeafRepo> repos = getDataManager().getReposFromCache();
            if (repos != null) {
                if (mRefreshType == REFRESH_ON_PULL) {
                    refreshLayout.setRefreshing(false);
                    mPullToRefreshStopRefreshing = 0;
                }

                updateAdapterWithRepos(repos, restorePosition);
                return;
            }
        }

        ConcurrentAsyncTask.execute(new LoadTask(getDataManager()));
    }

    public void navToDirectory(boolean forceRefresh, boolean restorePosition) {

        if (getNavContext().inDirentsTrash()) {
            return;
        }

        startTimer();

        mPullToRefreshStopRefreshing++;

        if (mPullToRefreshStopRefreshing > 1) {
            refreshLayout.setRefreshing(false);
            mPullToRefreshStopRefreshing = 0;
        }

        NavContext nav = getNavContext();
        DataManager dataManager = getDataManager();

        SeafRepo repo = getDataManager().getCachedRepoByID(nav.getRepoID());
        if (repo != null) {
            adapter.setEncryptedRepo(repo.encrypted);
            if (nav.getDirPath().equals(BrowserActivity.ACTIONBAR_PARENT_PATH)) {
                mActivity.setUpButtonTitle(nav.getRepoName());
            } else

                mActivity.setUpButtonTitle(nav.getDirPath().substring(
                        nav.getDirPath().lastIndexOf(BrowserActivity.ACTIONBAR_PARENT_PATH) + 1));
        }

        forceRefresh = forceRefresh || isDirentsRefreshTimeOut(nav.getRepoID(), nav.getDirPath());
        if ((!Utils.isNetworkOn() || !forceRefresh) && !getNavContext().inDirentsTrash()) {
            List<SeafDirent> dirents = dataManager.getCachedDirents(nav.getRepoID(), nav.getDirPath());
            if (dirents != null) {
                if (mRefreshType == REFRESH_ON_PULL) {
                    refreshLayout.setRefreshing(false);
                    mPullToRefreshStopRefreshing = 0;
                }

                updateAdapterWithDirents(dirents, restorePosition);
                return;
            }
        }

        ConcurrentAsyncTask.execute(new LoadDirTask(getDataManager()),
                nav.getRepoName(),
                nav.getRepoID(),
                nav.getDirPath());
    }

    /**
     * Navigates to the repository trash view, allowing for optional force refresh and restoration of the scroll position.
     *
     * @param forceRefresh    True to force a refresh of the repository trash data, false otherwise.
     * @param restorePosition True to restore the scroll position after navigation, false otherwise.
     */
    public void navToReposTrashView(boolean forceRefresh, boolean restorePosition) {
        if (!getNavContext().inRepoTrash()) {
            return;
        }

        mPullToRefreshStopRefreshing++;

        if (mPullToRefreshStopRefreshing > 1) {
            refreshLayout.setRefreshing(false);
            mPullToRefreshStopRefreshing = 0;
        }

        forceRefresh = forceRefresh || isReposRefreshTimeOut();
        if ((!Utils.isNetworkOn() || !forceRefresh) && getNavContext().inRepoTrash()) {
            List<SeafRepoTrash> repos = getDataManager().getReposTrashFromCache();
            if (repos != null) {
                if (mRefreshType == REFRESH_ON_PULL) {
                    refreshLayout.setRefreshing(false);
                    mPullToRefreshStopRefreshing = 0;
                }

                updateAdapterWithReposTrash(repos, restorePosition);
                return;
            }
        }

        loadTaskReposTrash(getDataManager());

    }

    /**
     * Navigates to the directory trash view, allowing for optional force refresh and restoration of the scroll position.
     *
     * @param forceRefresh    True to force a refresh of the directory trash data, false otherwise.
     * @param restorePosition True to restore the scroll position after navigation, false otherwise.
     */
    public void navToDirentsTrash(boolean forceRefresh, boolean restorePosition) {
        if (!getNavContext().inDirentsTrash()) {
            return;
        }

        mPullToRefreshStopRefreshing++;

        if (mPullToRefreshStopRefreshing > 1) {
            refreshLayout.setRefreshing(false);
            mPullToRefreshStopRefreshing = 0;
        }

        NavContext nav = getNavContext();
        DataManager dataManager = getDataManager();

        forceRefresh = forceRefresh || isDirentsRefreshTimeOut(nav.getRepoID(), nav.getDirPath());
        if ((!Utils.isNetworkOn() || !forceRefresh) && getNavContext().inDirentsTrash()) {
            List<SeafDirentTrash> dirents = dataManager.getCachedDirentsTrash(nav.getRepoID(), nav.getDirPath());
            if (dirents != null) {
                if (mRefreshType == REFRESH_ON_PULL) {
                    refreshLayout.setRefreshing(false);
                    mPullToRefreshStopRefreshing = 0;
                }

                updateAdapterWithDirentsTrash(dirents, restorePosition);
                return;
            }
        }

        if (!nav.isNavigateToDirentsTrash()) {
            ConcurrentAsyncTask.execute(new LoadDirTrashTask(getDataManager()),
                    nav.getRepoName(),
                    nav.getRepoID(),
                    nav.getDirPath());
        } else {
            ConcurrentAsyncTask.execute(new LoadDirTrashTask(getDataManager()),
                    nav.getRepoName(),
                    nav.getRepoID(),
                    nav.getDirPath(),
                    nav.getDirCommitID());
        }


    }

    // refresh list by mTimer
    public void startTimer() {
        if (isTimerStarted)
            return;

        isTimerStarted = true;
        Log.d(DEBUG_TAG, "timer started");
        mTimer.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mActivity == null) return;

                TransferService ts = mActivity.getTransferService();
                String repoID = getNavContext().getRepoID();
                String repoName = getNavContext().getRepoName();
                String currentDir = getNavContext().getDirPath();
                if (ts != null) {
                    adapter.setDownloadTaskList(ts.getDownloadTaskInfosByPath(repoID, currentDir));
                }
                // Log.d(DEBUG_TAG, "timer post refresh signal " + System.currentTimeMillis());
                mTimer.postDelayed(this, 1 * 3500);
            }
        }, 1 * 3500);
    }

    public void stopTimer() {
        Log.d(DEBUG_TAG, "timer stopped");
        mTimer.removeCallbacksAndMessages(null);
        isTimerStarted = false;
    }

    /**
     * calculate if repo refresh time is expired, the expiration is 10 mins
     */
    private boolean isReposRefreshTimeOut() {
        if (getDataManager().isReposRefreshTimeout()) {
            return true;
        }

        return false;

    }

    /**
     * calculate if dirent refresh time is expired, the expiration is 10 mins
     *
     * @param repoID
     * @param path
     * @return true if refresh time expired, false otherwise
     */
    private boolean isDirentsRefreshTimeOut(String repoID, String path) {
        if (getDataManager().isDirentsRefreshTimeout(repoID, path)) {
            return true;
        }

        return false;
    }

    public void sortFiles(int type, int order) {
        adapter.sortFiles(type, order);
        adapter.notifyDataSetChanged();
        // persist sort settings
        SettingsManager.instance().saveSortFilesPref(type, order);
    }

    private void updateAdapterWithRepos(List<SeafRepo> repos, boolean restoreScrollPosition) {
        adapter.clear();
        if (repos.size() > 0) {
            addReposToAdapter(repos);
            adapter.sortFiles(SettingsManager.instance().getSortFilesTypePref(),
                    SettingsManager.instance().getSortFilesOrderPref());
            adapter.notifyChanged();
            mListView.setVisibility(View.VISIBLE);
            restoreRepoScrollPosition(restoreScrollPosition);
            mEmptyView.setVisibility(View.GONE);
        } else {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
        // Collapses the currently open view
        //mListView.collapse();
    }

    private void updateAdapterWithDirents(final List<SeafDirent> dirents, boolean restoreScrollPosition) {
        adapter.clear();
        if (dirents.size() > 0) {
            for (SeafDirent dirent : dirents) {
                adapter.add(dirent);
            }
            NavContext nav = getNavContext();
            final String repoName = nav.getRepoName();
            final String repoID = nav.getRepoID();
            final String dirPath = nav.getDirPath();

            adapter.sortFiles(SettingsManager.instance().getSortFilesTypePref(),
                    SettingsManager.instance().getSortFilesOrderPref());
            adapter.notifyChanged();
            mListView.setVisibility(View.VISIBLE);
            restoreDirentScrollPosition(restoreScrollPosition, repoID, dirPath);
            mEmptyView.setVisibility(View.GONE);
        } else {
            // Directory is empty
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
        // Collapses the currently open view
        //mListView.collapse();
    }

    /**
     * Updates the adapter with a list of deleted Seafile repositories and optionally restores the scroll position.
     *
     * @param repos                 The list of deleted SeafRepoTrash objects to update the adapter with.
     * @param restoreScrollPosition True to restore the scroll position after updating the adapter, false otherwise.
     */
    private void updateAdapterWithReposTrash(List<SeafRepoTrash> repos, boolean restoreScrollPosition) {
        adapter.clear();
        if (repos.size() > 0) {
            addReposTrashToAdapter(repos);
            adapter.sortFiles(SettingsManager.instance().getSortFilesTypePref(),
                    SettingsManager.instance().getSortFilesOrderPref());
            adapter.notifyChanged();
            mListView.setVisibility(View.VISIBLE);
            restoreRepoScrollPosition(restoreScrollPosition);
            mEmptyView.setVisibility(View.GONE);
        } else {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
        // Collapses the currently open view
        //mListView.collapse();
    }

    /**
     * Updates the adapter with a list of deleted Seafile directory or file entries and optionally restores the scroll position.
     *
     * @param dirents               The list of deleted SeafDirentTrash objects to update the adapter with.
     * @param restoreScrollPosition True to restore the scroll position after updating the adapter, false otherwise.
     */
    private void updateAdapterWithDirentsTrash(final List<SeafDirentTrash> dirents, boolean restoreScrollPosition) {
        adapter.clear();
        if (dirents.size() > 0) {
            for (SeafDirentTrash dirent : dirents) {
                adapter.add(dirent);
            }
            NavContext nav = getNavContext();
            final String repoID = nav.getRepoID();
            final String dirPath = nav.getDirPath();

            adapter.sortFiles(SettingsManager.instance().getSortFilesTypePref(),
                    SettingsManager.instance().getSortFilesOrderPref());
            adapter.notifyChanged();
            mListView.setVisibility(View.VISIBLE);
            restoreDirentScrollPosition(restoreScrollPosition, repoID, dirPath);
            mEmptyView.setVisibility(View.GONE);
        } else {
            // Directory is empty
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }

        // Collapses the currently open view
        //mListView.collapse();
    }

    /**
     * update state of contextual action bar (CAB)
     */
    public void updateContextualActionBar() {

        if (mActionMode == null) {
            // there are some selected items, start the actionMode
            if (getNavContext().inDirentsTrash()) {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallbackTrash());
            } else if (getNavContext().inRepoTrash()) {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallbackTrashRepos());
            } else {
                mActionMode = mActivity.startSupportActionMode(new ActionModeCallback());
            }


        } else {
            String text;
            if (getNavContext().inDirentsTrash()) {
                text = getResources().getQuantityString(
                        R.plurals.transfer_list_items_selected,
                        adapter.getCheckedTrashItemCount(),
                        adapter.getCheckedTrashItemCount());
            } else if (getNavContext().inRepoTrash()) {
                text = getResources().getQuantityString(
                        R.plurals.transfer_list_items_selected,
                        adapter.getCheckedTrashRepoCount(),
                        adapter.getCheckedTrashRepoCount());
            } else {
                text = getResources().getQuantityString(
                        R.plurals.transfer_list_items_selected,
                        adapter.getCheckedItemCount(),
                        adapter.getCheckedItemCount());
            }

            mActionMode.setTitle(text);

        }

    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        if (Utils.isFastTapping()) return;

        // handle action mode selections
        if (mActionMode != null) {
            // add or remove selection for current list item
            if (adapter == null) return;

            if (getNavContext().inDirentsTrash()) {
                adapter.toggleTrashSelection(position);
            } else if (getNavContext().inRepoTrash()) {
                adapter.toggleTrashRepoSelection(position);
            } else {
                adapter.toggleSelection(position);
            }

            updateContextualActionBar();
            return;
        }

        SeafRepo repo = null;
        final NavContext nav = getNavContext();
        if (nav.inRepo()) {
            if (!nav.inDirentsTrash()) {
                repo = getDataManager().getCachedRepoByID(nav.getRepoID());
                mActivity.setUpButtonTitle(repo.getName());
            } else {
                repo = getDataManager().getCachedRepoByID(nav.getRepoID());
            }
        } else {
            SeafItem item = adapter.getItem(position);
            if (item instanceof SeafRepo) {
                repo = (SeafRepo) item;
            }
        }

        if (repo == null) {
            return;
        }

        if(repo.isSharedRepo || !repo.hasWritePermission()){
            mActivity.getmTabLayout().getTabAt(2).view.setVisibility(View.GONE);
            //mActivity.getmTabLayout().removeTabAt(2);
        }else {
            mActivity.getmTabLayout().getTabAt(2).view.setVisibility(View.VISIBLE);
            //mActivity.getmTabLayout().addTab(mActivity.getmTabLayout().newTab().setText(mActivity.getResources().getString(R.string.tabs_activity).toUpperCase()));
        }

        if (repo.encrypted && !getDataManager().getRepoPasswordSet(repo.id)) {
            String password = getDataManager().getRepoPassword(repo.id);
            mActivity.showPasswordDialog(repo.name, repo.id,
                    new TaskDialog.TaskDialogListener() {
                        @Override
                        public void onTaskSuccess() {
                            onListItemClick(l, v, position, id);
                        }
                    }, password);

            return;
        }

        mRefreshType = REFRESH_ON_CLICK;
        if (nav.inRepo()) {
            if (adapter.getItem(position) instanceof SeafDirentTrash) {
                final SeafDirentTrash dirent = (SeafDirentTrash) adapter.getItem(position);
                if (dirent.isDir()) {
                    String newPath = dirent.path.endsWith("/") ? dirent.path + dirent.name : dirent.path + "/" + dirent.name;
                    nav.setDir(newPath, dirent.id);
                    nav.setDirCommitID(dirent.commitID);
                    if (nav.getDirNavigatePath() == null) {
                        String path = dirent.path.endsWith("/") && dirent.path.length() > 1 ? dirent.path.substring(0, dirent.path.length() - 1) : dirent.path;
                        nav.setDirNavigatePath(path);
                        nav.setNavigateToDirentsTrash(true);
                    }
                    nav.setDirPermission(repo.permission);
                    saveDirentScrollPosition(repo.getID(), dirent.path);
                    refreshView();
                }
            } else if (adapter.getItem(position) instanceof SeafDirent) {
                final SeafDirent dirent = (SeafDirent) adapter.getItem(position);
                if (dirent.isDir()) {
                    String currentPath = nav.getDirPath();
                    String newPath = currentPath.endsWith("/") ?
                            currentPath + dirent.name : currentPath + "/" + dirent.name;
                    nav.setDir(newPath, dirent.id);
                    nav.setDirPermission(dirent.permission);
                    saveDirentScrollPosition(repo.getID(), currentPath);
                    refreshView();
                    mActivity.setUpButtonTitle(dirent.name);
                } else {
                    String currentPath = nav.getDirPath();
                    saveDirentScrollPosition(repo.getID(), currentPath);
                    mActivity.onFileSelected(dirent);
                }
            } else
                return;
        } else {
            nav.setDirPermission(repo.permission);
            nav.setRepoID(repo.id);
            nav.setRepoName(repo.getName());
            nav.setDir("/", repo.root);
            saveRepoScrollPosition();
            mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            refreshView();

        }
    }

    private class ScrollState {
        public int index;
        public int top;

        public ScrollState(int index, int top) {
            this.index = index;
            this.top = top;
        }
    }

    private void saveDirentScrollPosition(String repoId, String currentPath) {
        final String pathJoin = Utils.pathJoin(repoId, currentPath);
        final int index = mListView.getFirstVisiblePosition();
        final View v = mListView.getChildAt(0);
        final int top = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
        final ScrollState state = new ScrollState(index, top);
        scrollPostions.put(pathJoin, state);
    }

    private void saveRepoScrollPosition() {
        final int index = mListView.getFirstVisiblePosition();
        final View v = mListView.getChildAt(0);
        final int top = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
        final ScrollState state = new ScrollState(index, top);
        scrollPostions.put(KEY_REPO_SCROLL_POSITION, state);
    }

    private void restoreDirentScrollPosition(boolean restore, String repoId, String dirPath) {
        final String pathJoin = Utils.pathJoin(repoId, dirPath);
        if (restore) {
            ScrollState state = scrollPostions.get(pathJoin);
            if (state != null) {
                mListView.setSelectionFromTop(state.index, state.top);
            } else {
                mListView.setSelectionAfterHeaderView();
            }
        } else {
            mListView.setSelectionAfterHeaderView();
        }
    }

    private void restoreRepoScrollPosition(boolean restore) {
        if (restore) {
            ScrollState state = scrollPostions.get(KEY_REPO_SCROLL_POSITION);
            if (state != null) {
                mListView.setSelectionFromTop(state.index, state.top);
            } else {
                mListView.setSelectionAfterHeaderView();
            }
        } else {
            mListView.setSelectionAfterHeaderView();
        }
    }

    private void addReposToAdapter(List<SeafRepo> repos) {
        if (repos == null)
            return;
        Map<String, List<SeafRepo>> map = Utils.groupRepos(repos);
        List<SeafRepo> personalRepos = map.get(Utils.PERSONAL_REPO);
        if (personalRepos != null) {
            SeafGroup personalGroup = new SeafGroup(mActivity.getResources().getString(R.string.personal));
            adapter.add(personalGroup);
            for (SeafRepo repo : personalRepos)
                adapter.add(repo);
        }

        List<SeafRepo> sharedRepos = map.get(Utils.SHARED_REPO);
        if (sharedRepos != null) {
            SeafGroup sharedGroup = new SeafGroup(mActivity.getResources().getString(R.string.shared));
            adapter.add(sharedGroup);
            for (SeafRepo repo : sharedRepos)
                adapter.add(repo);
        }

        for (Map.Entry<String, List<SeafRepo>> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(Utils.PERSONAL_REPO)
                    && !key.endsWith(Utils.SHARED_REPO)) {
                SeafGroup group = new SeafGroup(key);
                adapter.add(group);
                for (SeafRepo repo : entry.getValue()) {
                    adapter.add(repo);
                }
            }
        }
    }

    /**
     * Adds a list of deleted Seafile repositories to the adapter.
     *
     * @param repos The list of SeafRepoTrash objects to add to the adapter.
     */
    private void addReposTrashToAdapter(List<SeafRepoTrash> repos) {
        if (repos == null)
            return;

        for (SeafRepoTrash repo : repos) {
            adapter.add(repo);
        }
    }


    private class LoadTask extends AsyncTask<Void, Void, List<SeafRepo>> {
        SeafException err = null;
        DataManager dataManager;

        public LoadTask(DataManager dataManager) {
            this.dataManager = dataManager;
        }

        @Override
        protected void onPreExecute() {
            if (getNavContext().inRepoTrash()) {
                return;
            }

            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(true);
            } else if (mRefreshType == REFRESH_ON_PULL) {

            }
        }

        @Override
        protected List<SeafRepo> doInBackground(Void... params) {
            try {
                return dataManager.getReposFromServer();
            } catch (SeafException e) {
                err = e;
                return null;
            }
        }

        private void displaySSLError() {
            if (mActivity == null)
                return;

            if (getNavContext().inRepo()) {
                return;
            }

            showError(R.string.ssl_error);
        }

        private void resend() {
            if (mActivity == null)
                return;

            if (getNavContext().inRepo()) {
                return;
            }
            ConcurrentAsyncTask.execute(new LoadTask(dataManager));
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafRepo> rs) {
            if (getNavContext().inRepoTrash()) {
                return;
            }

            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(false);
            } else if (mRefreshType == REFRESH_ON_PULL) {
                //String lastUpdate = getDataManager().getLastPullToRefreshTime(DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                //mListView.onRefreshComplete(lastUpdate);
                refreshLayout.setRefreshing(false);
                getDataManager().saveLastPullToRefreshTime(System.currentTimeMillis(), DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                mPullToRefreshStopRefreshing = 0;
            }

            if (getNavContext().inRepo()) {
                // this occurs if user already navigate into a repo
                return;
            }

            // Prompt the user to accept the ssl certificate
            if (err == SeafException.sslException) {
                SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                        new SslConfirmDialog.Listener() {
                            @Override
                            public void onAccepted(boolean rememberChoice) {
                                Account account = dataManager.getAccount();
                                CertsManager.instance().saveCertForAccount(account, rememberChoice);
                                resend();
                            }

                            @Override
                            public void onRejected() {
                                displaySSLError();
                            }
                        });
                dialog.show(getFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
                return;
            } else if (err == SeafException.remoteWipedException) {
                mActivity.completeRemoteWipe();
            }

            if (err != null) {
                if (err.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Token expired, should login again
                    mActivity.showShortToast(mActivity, R.string.err_token_expired);
                    mActivity.logoutWhenTokenExpired();
                } else {
                    Log.e(DEBUG_TAG, "failed to load repos: " + err.getMessage());
                    showError(R.string.error_when_load_repos);
                    return;
                }
            }

            if (rs != null) {
                getDataManager().setReposRefreshTimeStamp();
                updateAdapterWithRepos(rs, false);
            } else {
                Log.i(DEBUG_TAG, "failed to load repos");
                showError(R.string.error_when_load_repos);
            }
        }
    }

    private void showError(int strID) {
        showError(mActivity.getResources().getString(strID));
    }

    private void showError(String msg) {
        mProgressContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);

        adapter.clear();
        adapter.notifyChanged();

        mErrorText.setText(msg);
        mErrorText.setVisibility(View.VISIBLE);
        mErrorText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
    }

    public void showLoading(boolean show) {
        mErrorText.setVisibility(View.GONE);
        if (show) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));

            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        } else {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));

            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        }
    }

    private class LoadDirTask extends AsyncTask<String, Void, List<SeafDirent>> {

        SeafException err = null;
        String myRepoName;
        String myRepoID;
        String myPath;

        DataManager dataManager;

        public LoadDirTask(DataManager dataManager) {
            this.dataManager = dataManager;
        }

        @Override
        protected void onPreExecute() {
            if (getNavContext().inDirentsTrash()) {
                return;
            }

            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(true);
            } else if (mRefreshType == REFRESH_ON_PULL) {
                // mHeadProgress.setVisibility(ProgressBar.VISIBLE);
            }
        }

        @Override
        protected List<SeafDirent> doInBackground(String... params) {
            if (params.length != 3) {
                Log.d(DEBUG_TAG, "Wrong params to LoadDirTask");
                return null;
            }

            myRepoName = params[0];
            myRepoID = params[1];
            myPath = params[2];
            try {
                List<SeafDirent> dirents = dataManager.getDirentsFromServer(myRepoID, myPath);
                String repoName = getNavContext().getRepoName();
                String repoID = getNavContext().getRepoID();
                for (SeafDirent sd : dirents) {
                    if (!sd.isDir()) {
                        String path = Utils.pathJoin(getNavContext().getDirPath(), sd.name);
                        SeafCachedFile scf = dataManager.getCachedFile(repoName, repoID, path);
                        if (scf != null && scf.getSize() != sd.getFileSize()) {
                            dataManager.removeCachedFile(scf);
                        }
                    }
                }
                return dirents;
            } catch (SeafException e) {
                err = e;
                return null;
            }

        }

        private void resend() {
            if (mActivity == null)
                return;
            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }

            ConcurrentAsyncTask.execute(new LoadDirTask(dataManager), myRepoName, myRepoID, myPath);
        }

        private void displaySSLError() {
            if (mActivity == null)
                return;

            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }
            showError(R.string.ssl_error);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafDirent> dirents) {
            if (getNavContext().inDirentsTrash()) {
                return;
            }

            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(false);
            } else if (mRefreshType == REFRESH_ON_PULL) {
                //String lastUpdate = getDataManager().getLastPullToRefreshTime(DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                //mListView.onRefreshComplete(lastUpdate);
                refreshLayout.setRefreshing(false);
                getDataManager().saveLastPullToRefreshTime(System.currentTimeMillis(), DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                mPullToRefreshStopRefreshing = 0;
            }

            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }

            if (err == SeafException.sslException) {
                SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                        new SslConfirmDialog.Listener() {
                            @Override
                            public void onAccepted(boolean rememberChoice) {
                                Account account = dataManager.getAccount();
                                CertsManager.instance().saveCertForAccount(account, rememberChoice);
                                resend();
                            }

                            @Override
                            public void onRejected() {
                                displaySSLError();
                            }
                        });
                dialog.show(getFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
                return;
            } else if (err == SeafException.remoteWipedException) {
                mActivity.completeRemoteWipe();
            }

            if (err != null) {
                if (err.getCode() == SeafConnection.HTTP_STATUS_REPO_PASSWORD_REQUIRED) {
                    showPasswordDialog();
                } else if (err.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Token expired, should login again
                    mActivity.showShortToast(mActivity, R.string.err_token_expired);
                    mActivity.logoutWhenTokenExpired();
                } else if (err.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    final String message = String.format(getString(R.string.op_exception_folder_deleted), myPath);
                    mActivity.showShortToast(mActivity, message);
                } else {
                    Log.d(DEBUG_TAG, "failed to load dirents: " + err.getMessage());
                    err.printStackTrace();
                    showError(R.string.error_when_load_dirents);
                }
                return;
            }

            if (dirents == null) {
                showError(R.string.error_when_load_dirents);
                Log.i(DEBUG_TAG, "failed to load dir");
                return;
            }
            getDataManager().setDirsRefreshTimeStamp(myRepoID, myPath);
            updateAdapterWithDirents(dirents, false);
        }
    }

    /**
     * AsyncTask for loading deleted directory contents from the server.
     */
    private class LoadDirTrashTask extends AsyncTask<String, Void, List<SeafDirentTrash>> {

        SeafException err = null;
        String myRepoName;
        String myRepoID;
        String myPath;
        String myCommitID;
        DataManager dataManager;

        /**
         * Constructor for LoadDirTrashTask.
         *
         * @param dataManager The DataManager instance used for data operations.
         */
        public LoadDirTrashTask(DataManager dataManager) {
            this.dataManager = dataManager;
        }

        @Override
        protected void onPreExecute() {
            if (!getNavContext().inDirentsTrash()) {
                return;
            }
            // Pre-execution setup, such as showing loading indicators.
            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(true);
            } else if (mRefreshType == REFRESH_ON_PULL) {
                // mHeadProgress.setVisibility(ProgressBar.VISIBLE);
            }
        }

        @Override
        protected List<SeafDirentTrash> doInBackground(String... params) {
            // Background task to fetch deleted directory contents from the server.
            if (params.length != 3 && params.length != 4) {
                Log.d(DEBUG_TAG, "Wrong params to LoadDirTask");
                return null;
            }

            myRepoName = params[0];
            myRepoID = params[1];
            myPath = params[2];
            if (params.length == 4) {
                myCommitID = params[3];
            }
            try {
                List<SeafDirentTrash> dirents;

                if (params.length == 4) {
                    dirents = dataManager.getTrashDirentsFromServer(myRepoID, myPath, myCommitID);
                } else {
                    dirents = dataManager.getTrashDirentsFromServer(myRepoID, myPath);
                }

                return dirents;
            } catch (SeafException e) {
                err = e;
                return null;
            }

        }

        private void resend() {
            // Resends the request if necessary.
            if (mActivity == null)
                return;
            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }

            ConcurrentAsyncTask.execute(new LoadDirTrashTask(dataManager), myRepoName, myRepoID, myPath);
        }

        private void displaySSLError() {
            // Displays an SSL error dialog if needed.
            if (mActivity == null)
                return;

            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }
            showError(R.string.ssl_error);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafDirentTrash> dirents) {
            if (!getNavContext().inDirentsTrash()) {
                return;
            }
            // Post-execution handling of retrieved data or errors.
            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            if (mRefreshType == REFRESH_ON_CLICK
                    || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                    || mRefreshType == REFRESH_ON_RESUME) {
                showLoading(false);
            } else if (mRefreshType == REFRESH_ON_PULL) {
                String lastUpdate = getDataManager().getLastPullToRefreshTime(DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                //mListView.onRefreshComplete(lastUpdate);
                refreshLayout.setRefreshing(false);
                getDataManager().saveLastPullToRefreshTime(System.currentTimeMillis(), DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                mPullToRefreshStopRefreshing = 0;
            }

            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }

            if (err == SeafException.sslException) {
                SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                        new SslConfirmDialog.Listener() {
                            @Override
                            public void onAccepted(boolean rememberChoice) {
                                Account account = dataManager.getAccount();
                                CertsManager.instance().saveCertForAccount(account, rememberChoice);
                                resend();
                            }

                            @Override
                            public void onRejected() {
                                displaySSLError();
                            }
                        });
                dialog.show(getFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
                return;
            } else if (err == SeafException.remoteWipedException) {
                mActivity.completeRemoteWipe();
            }

            if (err != null) {
                if (err.getCode() == SeafConnection.HTTP_STATUS_REPO_PASSWORD_REQUIRED) {
                    showPasswordDialog();
                } else if (err.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Token expired, should login again
                    mActivity.showShortToast(mActivity, R.string.err_token_expired);
                    mActivity.logoutWhenTokenExpired();
                } else if (err.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    final String message = String.format(getString(R.string.op_exception_folder_deleted), myPath);
                    mActivity.showShortToast(mActivity, message);
                } else {
                    Log.d(DEBUG_TAG, "failed to load dirents: " + err.getMessage());
                    err.printStackTrace();
                    showError(R.string.error_when_load_dirents);
                }
                return;
            }

            if (dirents == null) {
                showError(R.string.error_when_load_dirents);
                Log.i(DEBUG_TAG, "failed to load dir");
                return;
            }

            getDataManager().setDirsRefreshTimeStamp(myRepoID, myPath);
            updateAdapterWithDirentsTrash(dirents, false);
        }
    }

    private void showPasswordDialog() {
        NavContext nav = mActivity.getNavContext();
        String repoName = nav.getRepoName();
        String repoID = nav.getRepoID();

        mActivity.showPasswordDialog(repoName, repoID, new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                refreshView();
            }
        });
    }

    /**
     * Represents a contextual mode of the user interface.
     * Action modes can be used to provide alternative interaction modes and replace parts of the normal UI until finished.
     * A Callback configures and handles events raised by a user's interaction with an action mode.
     */
    class ActionModeCallback implements ActionMode.Callback {
        private boolean allItemsSelected;

        public ActionModeCallback() {
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the contextual action bar (CAB)
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.repos_fragment_menu, menu);
            if (adapter == null) return true;
            adapter.setActionModeOn(true);
            // to hidden  "r" permissions  files or folder
            String permission = getNavContext().getDirPermission();
            if (permission != null && permission.indexOf("w") == -1) {
                menu.findItem(R.id.action_mode_delete).setVisible(false);
                menu.findItem(R.id.action_mode_move).setVisible(false);
            }
            if (getNavContext().inRepo()) {
                SeafRepo repo = getDataManager().getCachedRepoByID(getNavContext().getRepoID());
                if (repo.isSharedRepo) {
                    menu.findItem(R.id.action_mode_delete).setVisible(false);
                    menu.findItem(R.id.action_mode_move).setVisible(false);
                    menu.findItem(R.id.action_mode_copy).setVisible(false);
                }
            }
            adapter.notifyDataSetChanged();
            return true;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            /*
             * The ActionBarPolicy determines how many action button to place in the ActionBar
             * and the default amount is 2.
             */
            menu.findItem(R.id.action_mode_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_mode_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_mode_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            // Here you can perform updates to the contextual action bar (CAB) due to
            // an invalidate() request
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the contextual action bar (CAB)
            NavContext nav = mActivity.getNavContext();
            String repoID = nav.getRepoID();
            String repoName = nav.getRepoName();
            String dirPath = nav.getDirPath();
            final List<SeafDirent> selectedDirents = adapter.getSelectedItemsValues();
            if (selectedDirents.size() == 0
                    || repoID == null
                    || dirPath == null) {
                if (item.getItemId() != R.id.action_mode_select_all) {
                    mActivity.showShortToast(mActivity, R.string.action_mode_no_items_selected);
                    return true;
                }
            }

            switch (item.getItemId()) {
                case R.id.action_mode_select_all:
                    if (!allItemsSelected) {
                        if (adapter == null) return true;

                        adapter.selectAllItems();
                        updateContextualActionBar();
                    } else {
                        if (adapter == null) return true;

                        adapter.deselectAllItems();
                        updateContextualActionBar();
                    }

                    allItemsSelected = !allItemsSelected;
                    break;
                case R.id.action_mode_delete:
                    mActivity.deleteFiles(repoID, dirPath, selectedDirents);
                    break;
                case R.id.action_mode_copy:
                    mActivity.copyFiles(repoID, repoName, dirPath, selectedDirents);
                    break;
                case R.id.action_mode_move:
                    mActivity.moveFiles(repoID, repoName, dirPath, selectedDirents);
                    break;
                case R.id.action_mode_download:
                    mActivity.downloadFiles(repoID, repoName, dirPath, selectedDirents);
                    break;

                default:
                    return false;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (adapter == null) return;

            adapter.setActionModeOn(false);
            if (getNavContext().inDirentsTrash()) {
                adapter.deselectAllItemsTrash();
            } else {
                adapter.deselectAllItems();
            }

            // Here you can make any necessary updates to the activity when
            // the contextual action bar (CAB) is removed. By default, selected items are deselected/unchecked.
            mActionMode = null;
        }

    }

    /**
     * Represents a contextual mode of the user interface.
     * Action modes can be used to provide alternative interaction modes and replace parts of the normal UI until finished.
     * A Callback configures and handles events raised by a user's interaction with an action mode.
     */
    class ActionModeCallbackTrash implements ActionMode.Callback {
        private boolean allItemsSelected;

        public ActionModeCallbackTrash() {
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the contextual action bar (CAB)
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.repos_trash_fragment_menu, menu);
            if (adapter == null) return true;
            adapter.setActionModeOn(true);
            adapter.notifyDataSetChanged();
            return true;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            /*
             * The ActionBarPolicy determines how many action button to place in the ActionBar
             * and the default amount is 2.
             */
            menu.findItem(R.id.action_mode_recover).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_mode_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            // Here you can perform updates to the contextual action bar (CAB) due to
            // an invalidate() request
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the contextual action bar (CAB)
            NavContext nav = mActivity.getNavContext();
            String repoID = nav.getRepoID();
            String repoName = nav.getRepoName();
            String dirPath = nav.getDirPath();
            final List<SeafDirentTrash> selectedDirents = adapter.getSelectedTrashItemsValues();
            if (selectedDirents.size() == 0
                    || repoID == null
                    || dirPath == null) {
                if (item.getItemId() != R.id.action_mode_select_all) {
                    mActivity.showShortToast(mActivity, R.string.action_mode_no_items_selected);
                    return true;
                }
            }

            switch (item.getItemId()) {
                case R.id.action_mode_select_all:
                    if (!mActivity.isVisibleLoadingContainer()) {
                        if (!allItemsSelected) {
                            if (adapter == null) return true;

                            adapter.selectAllItemsTrash();
                            updateContextualActionBar();
                        } else {
                            if (adapter == null) return true;

                            adapter.deselectAllItemsTrash();
                            updateContextualActionBar();
                        }

                        allItemsSelected = !allItemsSelected;
                    }
                    break;
                case R.id.action_mode_recover:
                    if (!mActivity.isVisibleLoadingContainer()) {
                        mActivity.recoverFiles(selectedDirents);
                    }
                    break;

                default:
                    return false;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (adapter == null) return;

            adapter.setActionModeOn(false);

            if (getNavContext().inDirentsTrash()) {
                adapter.deselectAllItemsTrash();
            } else {
                adapter.deselectAllItems();
            }


            // Here you can make any necessary updates to the activity when
            // the contextual action bar (CAB) is removed. By default, selected items are deselected/unchecked.
            mActionMode = null;
        }


    }

    /**
     * Represents a contextual mode of the user interface.
     * Action modes can be used to provide alternative interaction modes and replace parts of the normal UI until finished.
     * A Callback configures and handles events raised by a user's interaction with an action mode.
     */
    class ActionModeCallbackTrashRepos implements ActionMode.Callback {
        private boolean allItemsSelected;

        public ActionModeCallbackTrashRepos() {
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the contextual action bar (CAB)
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.repos_trash_fragment_menu, menu);
            if (adapter == null) return true;
            adapter.setActionModeOn(true);
            adapter.notifyDataSetChanged();
            return true;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            /*
             * The ActionBarPolicy determines how many action button to place in the ActionBar
             * and the default amount is 2.
             */
            menu.findItem(R.id.action_mode_recover).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_mode_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            // Here you can perform updates to the contextual action bar (CAB) due to
            // an invalidate() request
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the contextual action bar (CAB)
            final List<SeafRepoTrash> selectedTrashRepos = adapter.getSelectedTrashRepoValues();
            if (selectedTrashRepos.isEmpty()) {
                if (item.getItemId() != R.id.action_mode_select_all) {
                    mActivity.showShortToast(mActivity, R.string.action_mode_no_items_selected);
                    return true;
                }
            }

            switch (item.getItemId()) {
                case R.id.action_mode_select_all:
                    if (!mActivity.isVisibleLoadingContainer()) {
                        if (!allItemsSelected) {
                            if (adapter == null) return true;

                            adapter.selectAllReposTrash();
                            updateContextualActionBar();
                        } else {
                            if (adapter == null) return true;

                            adapter.deselectAllRepoTrash();
                            updateContextualActionBar();
                        }

                        allItemsSelected = !allItemsSelected;
                    }
                    break;
                case R.id.action_mode_recover:
                    if (!mActivity.isVisibleLoadingContainer()) {
                        mActivity.retrieveRepos(selectedTrashRepos);
                    }
                    break;

                default:
                    return false;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (adapter == null) return;

            adapter.setActionModeOn(false);
            adapter.deselectAllRepoTrash();

            // Here you can make any necessary updates to the activity when
            // the contextual action bar (CAB) is removed. By default, selected items are deselected/unchecked.
            mActionMode = null;
        }


    }

    public void clearAdapterData() {
        if (adapter != null && mListView != null) {
            adapter.clear();
            mListView.setAdapter(adapter);
        }
    }

    public void closeActionMode(){
        if(mActionMode != null){
            mActionMode.finish();
        }

    }

    /**
     * Loads deleted Seafile repositories asynchronously and updates the UI accordingly.
     *
     * @param dataManager The DataManager instance used for data operations.
     */
    private void loadTaskReposTrash(DataManager dataManager) {
        if (!getNavContext().inRepoTrash()) {
            return;
        }
        ExecutorService service = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!getNavContext().inRepoTrash()) {
                        return;
                    }
                    SeafException err = null;

                    if (mRefreshType == REFRESH_ON_CLICK
                            || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                            || mRefreshType == REFRESH_ON_RESUME) {
                        handler.post(() -> showLoading(true));
                    } else if (mRefreshType == REFRESH_ON_PULL) {

                    }

                    List<SeafRepoTrash> rs = null;

                    try {
                        rs = dataManager.getReposTrashFromServer();
                    } catch (SeafException e) {
                        err = e;
                    }
                    SeafException finalErr = err;
                    List<SeafRepoTrash> finalRs = rs;
                    handler.post(() -> {


                        if (!getNavContext().inRepoTrash()) {
                            return;
                        }

                        if (mActivity == null)
                            // this occurs if user navigation to another activity
                            return;

                        if (mRefreshType == REFRESH_ON_CLICK
                                || mRefreshType == REFRESH_ON_OVERFLOW_MENU
                                || mRefreshType == REFRESH_ON_RESUME) {
                            showLoading(false);
                        } else if (mRefreshType == REFRESH_ON_PULL) {
                            String lastUpdate = getDataManager().getLastPullToRefreshTime(DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                            //mListView.onRefreshComplete(lastUpdate);
                            refreshLayout.setRefreshing(false);
                            getDataManager().saveLastPullToRefreshTime(System.currentTimeMillis(), DataManager.PULL_TO_REFRESH_LAST_TIME_FOR_REPOS_FRAGMENT);
                            mPullToRefreshStopRefreshing = 0;
                        }

                        if (getNavContext().inRepo()) {
                            // this occurs if user already navigate into a repo
                            return;
                        }

                        // Prompt the user to accept the ssl certificate
                        if (finalErr == SeafException.sslException) {
                            SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                                    new SslConfirmDialog.Listener() {
                                        @Override
                                        public void onAccepted(boolean rememberChoice) {
                                            Account account = dataManager.getAccount();
                                            CertsManager.instance().saveCertForAccount(account, rememberChoice);
                                            resend();
                                        }

                                        @Override
                                        public void onRejected() {
                                            displaySSLError();
                                        }
                                    });
                            dialog.show(getFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
                            return;
                        } else if (finalErr == SeafException.remoteWipedException) {
                            mActivity.completeRemoteWipe();
                        }

                        if (finalErr != null) {
                            if (finalErr.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                // Token expired, should login again
                                mActivity.showShortToast(mActivity, R.string.err_token_expired);
                                mActivity.logoutWhenTokenExpired();
                            } else {
                                Log.e(DEBUG_TAG, "failed to load repos: " + finalErr.getMessage());
                                showError(R.string.error_when_load_repos);
                                return;
                            }
                        }

                        if (finalRs != null) {
                            getDataManager().setReposRefreshTimeStamp();
                            updateAdapterWithReposTrash(finalRs, false);
                        } else {
                            Log.i(DEBUG_TAG, "failed to load repos");
                            showError(R.string.error_when_load_repos);
                        }

                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            private void displaySSLError() {
                if (mActivity == null)
                    return;

                if (getNavContext().inRepo()) {
                    return;
                }

                showError(R.string.ssl_error);
            }

            private void resend() {
                if (mActivity == null)
                    return;

                if (getNavContext().inRepo()) {
                    return;
                }

                loadTaskReposTrash(dataManager);
            }

        });
    }

    public void onClickRefreshReposTrash() {
        mRefreshType = REFRESH_ON_CLICK;
        mActivity.setUpButtonTitle(getResources().getString(R.string.deleted_repositories).toUpperCase());
        getNavContext().setRepoTrash(true);
        getNavContext().setDirPermission(null);
        getNavContext().setRepoID(null);
        getNavContext().setRepoName(null);
        getNavContext().setDir(null, null);
        saveRepoScrollPosition();
        refreshView(true,false);
    }

    public void onClickRefreshDirentsTrash() {
        mRefreshType = REFRESH_ON_CLICK;
        mActivity.setUpButtonTitle(getResources().getString(R.string.deleted_files).toUpperCase());
        getNavContext().setRepoTrash(false);
        getNavContext().setDirentsTrash(true);
        getNavContext().setDirNavigateRoot(getNavContext().getDirPath());
        saveRepoScrollPosition();
        refreshView(true, false);
    }


}