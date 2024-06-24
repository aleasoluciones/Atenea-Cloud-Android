package com.ateneacloud.drive.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.ui.fragment.SyncManagementFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Adapter for displaying synchronization settings in a RecyclerView.
 */
public class SyncAdapter extends RecyclerView.Adapter<SyncAdapter.SettingsHolder> {
    private List<SeafSyncSettings> seafSyncSettings;
    private FragmentActivity activity;
    private int touchSlop;

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param seafSyncSettings List<SeafSyncSettings> containing the data to populate views to be used
     *                         by RecyclerView.
     * @param activity         The FragmentActivity associated with the adapter.
     */
    public SyncAdapter(List<SeafSyncSettings> seafSyncSettings, FragmentActivity activity) {
        this.activity = activity;
        this.seafSyncSettings = seafSyncSettings;
        touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class SettingsHolder extends RecyclerView.ViewHolder {
        private final LinearLayout syncItemLinearLayout;
        private final TextView syncItemType;
        private final TextView syncItemSource;
        private final TextView syncItemTarget;
        private final TextView syncItemStatus;
        private final TextView syncItemLastRun;
        private final TextView syncItemSourceText;
        private final TextView syncItemTargetText;
        private final TextView syncItemStatusText;
        private final TextView syncItemLastRunText;
        private final ImageView syncItemArrow;


        public SettingsHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            syncItemLinearLayout = (LinearLayout) view.findViewById(R.id.sync_item_linear_layout);

            syncItemType = (TextView) view.findViewById(R.id.sync_item_type);
            syncItemSource = (TextView) view.findViewById(R.id.sync_item_source);
            syncItemTarget = (TextView) view.findViewById(R.id.sync_item_target);
            syncItemStatus = (TextView) view.findViewById(R.id.sync_item_status);
            syncItemLastRun = (TextView) view.findViewById(R.id.sync_item_last_run);

            syncItemSourceText = (TextView) view.findViewById(R.id.sync_item_source_text);
            syncItemTargetText = (TextView) view.findViewById(R.id.sync_item_target_text);
            syncItemStatusText = (TextView) view.findViewById(R.id.sync_item_status_text);
            syncItemLastRunText = (TextView) view.findViewById(R.id.sync_item_last_run_text);

            syncItemArrow = (ImageView) view.findViewById(R.id.sync_item_arrow);
        }

        public LinearLayout getSyncItemLinearLayout() {
            return syncItemLinearLayout;
        }

        public TextView getSyncItemType() {
            return syncItemType;
        }

        public TextView getSyncItemSource() {
            return syncItemSource;
        }

        public TextView getSyncItemTarget() {
            return syncItemTarget;
        }

        public TextView getSyncItemStatus() {
            return syncItemStatus;
        }

        public TextView getSyncItemLastRun() {
            return syncItemLastRun;
        }

        public TextView getSyncItemSourceText() {
            return syncItemSourceText;
        }

        public TextView getSyncItemTargetText() {
            return syncItemTargetText;
        }

        public TextView getSyncItemStatusText() {
            return syncItemStatusText;
        }

        public TextView getSyncItemLastRunText() {
            return syncItemLastRunText;
        }

        public ImageView getSyncItemArrow() {
            return syncItemArrow;
        }
    }

    /**
     * Initializes a new ViewHolder by inflating the layout for a list item view.
     *
     * @param viewGroup The parent ViewGroup.
     * @param viewType  The type of view to create.
     * @return A new SettingsHolder with the inflated view.
     */
    @Override
    public SettingsHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.sync_item, viewGroup, false);

        return new SettingsHolder(view);
    }

    /**
     * Binds synchronization settings to a ViewHolder for a specific position in the dataset.
     *
     * @param viewHolder The ViewHolder to bind the data to.
     * @param position   The position of the item in the dataset.
     */
    @Override
    public void onBindViewHolder(SettingsHolder viewHolder, final int position) {

        SeafSyncSettings seafSyncSetting = seafSyncSettings.get(position);

        viewHolder.getSyncItemType().setText(getType(seafSyncSetting.getType()));
        viewHolder.getSyncItemSource().setText(getSource(seafSyncSetting));
        viewHolder.getSyncItemTarget().setText(seafSyncSetting.getRepoPath());
        viewHolder.getSyncItemStatus().setText(getStatusText(seafSyncSetting.getStatus()));
        viewHolder.getSyncItemLastRun().setText(formatDate(seafSyncSetting.getLastExecution()));
        viewHolder.getSyncItemLinearLayout().setOnTouchListener(currentsOptionTouchListener(viewHolder, seafSyncSetting));

    }

    /**
     * Returns the number of items in the dataset.
     *
     * @return The number of synchronization settings in the dataset.
     */
    @Override
    public int getItemCount() {
        return seafSyncSettings.size();
    }

    /**
     * Formats a date to a string in the "yyyy-MM-dd HH:mm:ss" format.
     *
     * @param executionDate The date to be formatted.
     * @return The formatted date as a string.
     */
    private String formatDate(Date executionDate) {
        String date = "";


        if (executionDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.format(executionDate);

        }

        return date;
    }

    /**
     * Returns the source of the synchronization settings, which can be a folder, album, or "gallery."
     *
     * @param settings The synchronization settings to determine the source for.
     * @return The source description as a string.
     */
    private String getSource(SeafSyncSettings settings) {

        if (settings.getType() == SeafSyncType.Folder || settings.getType() == SeafSyncType.Album) {

            return new File(settings.getResourceUri().getPath()).getName();

        } else if (settings.getType() == SeafSyncType.Gallery) {

            return activity.getResources().getString(R.string.gallery);

        }

        return "";
    }

    /**
     * Returns a touch listener for handling user interaction with synchronization items.
     *
     * @param settingsHolder The ViewHolder associated with the synchronization item.
     * @param settings       The synchronization settings for the item.
     * @return A touch listener to handle user interactions.
     */
    private View.OnTouchListener currentsOptionTouchListener(SettingsHolder settingsHolder, SeafSyncSettings settings) {

        return new View.OnTouchListener() {
            float initialX, initialY;
            boolean isClick = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isClick = true;
                        initialX = event.getX();
                        initialY = event.getY();

                        break;
                    case MotionEvent.ACTION_MOVE:
                        isClick = wantToClick(event, initialX, initialY);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        isClick = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        isClick = false;
                        navigateToCreateSync(settings);
                        break;
                }

                if (isClick) {
                    settingsHolder.getSyncItemLinearLayout().setBackgroundColor(activity.getColor(R.color.fancy_dark_gray));
                    changeColorImageView(settingsHolder.getSyncItemArrow(), Color.WHITE);
                } else {
                    settingsHolder.getSyncItemLinearLayout().setBackgroundColor(Color.WHITE);
                    changeColorImageView(settingsHolder.getSyncItemArrow(), activity.getColor(R.color.light_grey));
                }

                return true;
            }
        };

    }

    /**
     * Checks if a click action occurred based on the touch event.
     *
     * @param event    The touch event to analyze.
     * @param initialX The initial X coordinate of the touch.
     * @param initialY The initial Y coordinate of the touch.
     * @return True if it's a click action, false otherwise.
     */
    private boolean wantToClick(MotionEvent event, float initialX, float initialY) {
        float x = event.getX();
        float y = event.getY();
        float dx = x - initialX;
        float dy = y - initialY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        return distance < touchSlop;
    }

    /**
     * Changes the color of an ImageView's drawable to the specified color.
     *
     * @param imageView The ImageView to change the drawable color.
     * @param color     The color to set for the drawable.
     */
    private void changeColorImageView(ImageView imageView, int color) {
        Drawable drawable = imageView.getDrawable();

        Drawable.ConstantState constantState = drawable.getConstantState();

        Drawable wrappedDrawable = constantState.newDrawable();
        DrawableCompat.setTint(wrappedDrawable, color);

        imageView.setImageDrawable(wrappedDrawable);
    }

    /**
     * Navigates to the CreateSyncsFragment for editing the synchronization settings.
     *
     * @param settings The synchronization settings to edit.
     */
    private void navigateToCreateSync(SeafSyncSettings settings) {
        SyncManagementFragment mSyncsFragment = new SyncManagementFragment(settings);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.syncs_fragment_container, mSyncsFragment);
        fragmentTransaction.commit();
    }

    /**
     * Returns the human-readable status text based on the synchronization status.
     *
     * @param syncStatus The synchronization status to get the status text for.
     * @return The human-readable status text as a string.
     */
    private String getStatusText(Enum<SeafSyncStatus> syncStatus) {

        String status;

        if (syncStatus == SeafSyncStatus.Complete) {
            status = activity.getResources().getString(R.string.sync_status_complete);
        } else if (syncStatus == SeafSyncStatus.Pending) {
            status = activity.getResources().getString(R.string.sync_status_pending);
        } else if (syncStatus == SeafSyncStatus.Running) {
            status = activity.getResources().getString(R.string.sync_status_running);
        } else {
            status = activity.getResources().getString(R.string.sync_status_error);
        }

        return status;

    }

    private String getType(Enum<SeafSyncType> syncType) {
        String type;

        if (syncType == SeafSyncType.Gallery) {
            type = activity.getResources().getString(R.string.gallery);
        } else if (syncType == SeafSyncType.Album) {
            type = activity.getResources().getString(R.string.album);
        } else {
            type = activity.getResources().getString(R.string.folder);
        }

        return type;
    }

}
