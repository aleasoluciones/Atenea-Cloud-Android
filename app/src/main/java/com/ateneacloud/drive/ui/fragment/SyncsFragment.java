package com.ateneacloud.drive.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.ateneacloud.drive.ui.activity.SyncsActivity;
import com.ateneacloud.drive.ui.adapter.SyncAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A fragment that displays synchronization options and a list of current sync tasks.
 */
public class SyncsFragment extends Fragment {

    private ConstraintLayout createSyncGalleryConstraint, createSyncAlbumConstraint, createSyncFolderConstraint;
    private TextView createSyncGalleryText, createSyncAlbumText, createSyncFolderText;
    private ImageView createSyncGalleryArrow, createSyncAlbumArrow, createSyncFolderArrow;
    private RecyclerView currentSyncsRecyclerView;
    private int touchSlop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.syncs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && getActivity() instanceof SyncsActivity) {
            ((SyncsActivity) getActivity()).changeTitle(R.string.synchronizations);
        }

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        initialCreateElements();
    }

    /**
     * Initializes and configures UI elements for creating synchronization options.
     */
    private void initialCreateElements() {
        createSyncGalleryConstraint = getView().findViewById(R.id.create_sync_gallery_constraint);
        createSyncAlbumConstraint = getView().findViewById(R.id.create_sync_album_constraint);
        createSyncFolderConstraint = getView().findViewById(R.id.create_sync_folder_constraint);

        createSyncGalleryText = getView().findViewById(R.id.create_sync_gallery_text);
        createSyncAlbumText = getView().findViewById(R.id.create_sync_album_text);
        createSyncFolderText = getView().findViewById(R.id.create_sync_folder_text);

        createSyncGalleryArrow = getView().findViewById(R.id.create_sync_gallery_arrow);
        createSyncAlbumArrow = getView().findViewById(R.id.create_sync_album_arrow);
        createSyncFolderArrow = getView().findViewById(R.id.create_sync_folder_arrow);

        createSyncGalleryConstraint.setOnTouchListener(createOptionTouchListener(createSyncGalleryConstraint, createSyncGalleryText, createSyncGalleryArrow));
        createSyncAlbumConstraint.setOnTouchListener(createOptionTouchListener(createSyncAlbumConstraint, createSyncAlbumText, createSyncAlbumArrow));
        createSyncFolderConstraint.setOnTouchListener(createOptionTouchListener(createSyncFolderConstraint, createSyncFolderText, createSyncFolderArrow));

        currentSyncsRecyclerView = getView().findViewById(R.id.current_syncs_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        currentSyncsRecyclerView.setLayoutManager(layoutManager);
        currentSyncsRecyclerView.setNestedScrollingEnabled(false);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            try {
                AccountManager accountManager = new AccountManager(getActivity());
                SeafSyncSettingsService settingsService = new SeafSyncSettingsService();
                List<SeafSyncSettings> settings = settingsService.allToAccount(accountManager.getCurrentAccount());
                SyncAdapter syncAdapter = new SyncAdapter(settings, getActivity());

                handler.post(() -> {
                    currentSyncsRecyclerView.setAdapter(syncAdapter);
                    syncAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Changes the color of an ImageView's drawable and updates it.
     *
     * @param imageView The ImageView whose drawable color is to be changed.
     * @param color The new color to be applied to the drawable.
     */
    private void changeColorImageView(ImageView imageView, int color) {
        Drawable drawable = imageView.getDrawable();

        Drawable.ConstantState constantState = drawable.getConstantState();

        Drawable wrappedDrawable = constantState.newDrawable();
        DrawableCompat.setTint(wrappedDrawable, color);

        imageView.setImageDrawable(wrappedDrawable);
    }

    /**
     * Creates a touch listener for synchronization option elements.
     *
     * @param constraintLayout The ConstraintLayout element to be touched.
     * @param textView The TextView element associated with the option.
     * @param imageView The ImageView element associated with the option.
     * @return The touch listener for the option element.
     */
    private View.OnTouchListener createOptionTouchListener(ConstraintLayout constraintLayout, TextView textView, ImageView imageView) {


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
                        launchCreateSync(v);
                        break;
                }

                if (isClick) {
                    constraintLayout.setBackgroundColor(getContext().getColor(R.color.fancy_dark_gray));
//                    textView.setTextColor(Color.WHITE);
                    changeColorImageView(imageView, Color.WHITE);
                } else {
                    constraintLayout.setBackgroundColor(Color.WHITE);
//                    textView.setTextColor(Color.BLACK);
                    changeColorImageView(imageView, getContext().getColor(R.color.light_grey));
                }

                return true;
            }
        };

    }

    /**
     * Determines if a touch event corresponds to a click based on the touch distance.
     *
     * @param event The MotionEvent being handled.
     * @param initialX The initial X-coordinate of the touch.
     * @param initialY The initial Y-coordinate of the touch.
     * @return True if the event corresponds to a click, false otherwise.
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
     * Launches the appropriate synchronization creation action based on the touched element.
     *
     * @param v The View element that was touched.
     */
    private void launchCreateSync(View v) {
        switch (v.getId()) {
            case R.id.create_sync_gallery_constraint:
                navigateToCreateSync(SeafSyncType.Gallery);
                break;
            case R.id.create_sync_album_constraint:
                navigateToCreateSync(SeafSyncType.Album);
                break;
            case R.id.create_sync_folder_constraint:
                navigateToCreateSync(SeafSyncType.Folder);
                break;

        }
    }

    /**
     * Navigates to the CreateSyncsFragment based on the provided synchronization type.
     *
     * @param type The type of synchronization (e.g., Gallery, Album, Folder).
     */
    private void navigateToCreateSync(Enum<SeafSyncType> type) {
        try {
            SyncManagementFragment mSyncsFragment = new SyncManagementFragment(type);
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.replace(R.id.syncs_fragment_container, mSyncsFragment);
            fragmentTransaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}