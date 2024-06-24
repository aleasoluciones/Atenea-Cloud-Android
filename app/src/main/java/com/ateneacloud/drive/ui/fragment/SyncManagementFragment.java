package com.ateneacloud.drive.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.account.AccountPlans;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.sync.clock.SyncWokerManager;
import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.enums.SeafSyncNetwork;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.ateneacloud.drive.ui.activity.BucketsActivity;
import com.ateneacloud.drive.ui.activity.SeafileSyncPathChooserActivity;
import com.ateneacloud.drive.ui.activity.SelectSyncFolderActivity;
import com.ateneacloud.drive.ui.activity.SyncsActivity;
import com.ateneacloud.drive.ui.dialog.DeleteSyncDialog;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for creating and editing Seafile synchronization settings. This fragment allows users to configure
 * synchronization settings, including the source and target folders, synchronization mode, network preferences,
 * and expiration date.
 */
public class SyncManagementFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

    private int CHOOSE_SYNC_FOLDER = 8748, CHOOSE_LOCAL_FOLDER = 5526, CHOOSE_ALBUM = 5456;
    private String DEBUG_TAG = "SyncManagementFragment";

    //Header
    private TextView syncManagementErrorMessage;
    private TextView syncManagementSyncStatusText;
    private Switch syncManagementSyncStatus;

    //Source and Target
    private ConstraintLayout syncManagementSourceConstraint, syncManagementTargetConstraint;
    private TextView syncManagementSource, syncManagementTarget, syncManagementSourceFolder, syncManagementTargetFolder;
    private ImageView syncManagementSourceArrow, syncManagementTargetArrow;

    //Backup Settings
    private Switch syncManagementSwitchWifi, syncManagementSwitchVideo;
    private Button syncManagementButtonFull, syncManagementButtonIncremental, syncManagementSave, syncManagementDelete;


    //Backup expirations
    ////General
    private CardView syncManagementUnavailableExpirations, //CardView para mensaje de no disponible
            syncManagementCardviewExpirations; //CardView para la seleccion del tipo se sincronizacion
    private Button syncManagementTypeTemporary, syncManagementTypePermanet;

    ////Temporary
    private ConstraintLayout syncManagementExpirationTemporaryConstraint;
    private TextView syncManagementTextDate, syncManagementTextTime;

    private CheckBox syncManagementDeleteAllFiles;

    ////Permanent
    private ConstraintLayout syncManagementExpirationPermanetConstraint;
    private Spinner syncManagementExpirationFileSpinner;
    private EditText syncManagementExpirationFileCustomNumber;
    private Spinner syncManagementExpirationFileCustomSpinner;

    //Others
    private AccountManager accountManager;
    private SeafSyncSettings settings;
    private int touchSlop;
    private boolean isEdit;
    private boolean actionPending;
    private SeafSyncSettingsService settingsService;
    private static final int EXPIRATION_IN_WEEK = 0, EXPIRATION_IN_MONTH = 1, EXPIRATION_IN_YEAR = 2, EXPIRATION_INDEFINITE = 3, EXPIRATION_CUSTOM = 4, EXPIRATION_IN_DAY = 5;
    private static final int ACTION_CREATE_SYNC = 0, ACTION_UPDATE_SYNC = 1, ACTION_DELETE_SYNC = 2;
    private String[] expirationOptions, customExpirationOptions;

    public SyncManagementFragment() {

    }

    /**
     * Constructor for creating a new sync settings fragment for a specific sync type.
     *
     * @param type The type of synchronization settings to create.
     */
    public SyncManagementFragment(Enum<SeafSyncType> type) {
        Date currentDate = new Date();
        settings = new SeafSyncSettings();
        settings.setType(type);
        settings.setNetwork(SeafSyncNetwork.Wifi);
        settings.setUploadVideos(true);
        settings.setMode(SeafSyncMode.Complete);
        settings.setCreationDate(currentDate);
        settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_WEEK));
        isEdit = false;

    }

    /**
     * Constructor for editing existing synchronization settings.
     *
     * @param settings The synchronization settings to edit.
     */
    public SyncManagementFragment(SeafSyncSettings settings) {
        this.settings = settings.clone();
        isEdit = true;
    }

    /**
     * Called when the fragment is created. This method is called after the fragment's
     * UI is created and ready for user interaction.
     *
     * @param savedInstanceState A Bundle containing the saved state information, if any.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Called to create the view hierarchy associated with the fragment.
     *
     * @param inflater           The LayoutInflater object used to inflate the layout.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState A Bundle containing the saved state information, if any.
     * @return The root View of the fragment's layout.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.management_syncs, container, false);
    }

    /**
     * Called immediately after onCreateView(View, Bundle) has returned, allowing
     * further initialization of the fragment's view.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState A Bundle containing the saved state information, if any.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expirationOptions = new String[]{getResources().getString(R.string.one_week), getResources().getString(R.string.one_month), getResources().getString(R.string.one_year), getResources().getString(R.string.indefinite), getResources().getString(R.string.customized)};

        customExpirationOptions = new String[]{getResources().getString(R.string.days), getResources().getString(R.string.weeks), getResources().getString(R.string.months), getResources().getString(R.string.years),};

        accountManager = new AccountManager(getActivity());

        initSettingsServiceAndAccount();
        actionPending = false;

        if (savedInstanceState != null) {
            // Restaurar el objeto desde el Bundle
            settings = savedInstanceState.getParcelable("syncSettings");
            isEdit = savedInstanceState.getBoolean("edit");
        }

        if (getActivity() != null && getActivity() instanceof SyncsActivity) {
            if (isEdit) {
                ((SyncsActivity) getActivity()).changeTitle(getResources().getString(R.string.select_album_sync));
            } else {
                ((SyncsActivity) getActivity()).changeTitle(getResources().getString(R.string.sync_management_new_sync));
            }
        }

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        initialComponents();
        adjustComponents();
    }

    /**
     * Handles clicks on various UI elements.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.sync_management_mode_full:
                syncManagementButtonFull.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
                syncManagementButtonIncremental.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
                settings.setMode(SeafSyncMode.Complete);
                break;
            case R.id.sync_management_mode_incremental:
                syncManagementButtonFull.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
                syncManagementButtonIncremental.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
                settings.setMode(SeafSyncMode.Incremental);
                break;
            case R.id.sync_management_text_date:
                showDatePickerDialog();
                break;
            case R.id.sync_management_text_time:
                showTimePickerDialog();
                break;
            case R.id.sync_management_save:
                if (isEdit) {
                    executeActionForSynchronization(ACTION_UPDATE_SYNC);
                } else {
                    executeActionForSynchronization(ACTION_CREATE_SYNC);
                }
                break;
            case R.id.sync_management_delete:
                executeActionForSynchronization(ACTION_DELETE_SYNC);
                break;
            case R.id.sync_management_type_temporary:

                if (settings.getExpireDate() == null) {
                    settings.setExpireDate(Utils.addTimeToDate(new Date(), getTimeExpire(EXPIRATION_IN_MONTH)));
                }

                settings.setTimeExpirationFiles(-1);

                syncManagementTypeTemporary.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
                syncManagementTypePermanet.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
                syncManagementExpirationTemporaryConstraint.setVisibility(View.VISIBLE);
                syncManagementExpirationPermanetConstraint.setVisibility(View.GONE);
                textDateSetExpireSetting(settings.getExpireDate());
                textTimeSetTime(settings.getExpireDate().getHours(), settings.getExpireDate().getMinutes());
                break;
            case R.id.sync_management_type_permanent:

                settings.setExpireDate(null);

                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_WEEK));
                setExpirationTypeInSpinner(settings.getTimeExpirationFiles());

                syncManagementTypeTemporary.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
                syncManagementTypePermanet.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
                syncManagementExpirationTemporaryConstraint.setVisibility(View.GONE);
                syncManagementExpirationPermanetConstraint.setVisibility(View.VISIBLE);
                break;
        }

    }

    /**
     * Handles changes in state for Switch and CheckBox components.
     *
     * @param buttonView The CompoundButton whose state has changed.
     * @param isChecked  The new state of the button.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()) {
            case R.id.sync_management_wifi_switch:
                if (isChecked) {
                    settings.setNetwork(SeafSyncNetwork.Wifi);
                } else {
                    settings.setNetwork(SeafSyncNetwork.WifiAndData);
                }
                break;
            case R.id.sync_management_video_switch:
                settings.setUploadVideos(isChecked);
                break;
            case R.id.sync_management_sync_status:
                settings.setActive(isChecked);
                if (isChecked) {
                    syncManagementSyncStatusText.setText(getResources().getString(R.string.synchronization_enabled));
                } else {
                    syncManagementSyncStatusText.setText(getResources().getString(R.string.synchronization_disabled));
                }
                break;
        }

    }

    /**
     * Called when the fragment needs to save its state. This is used to preserve fragment state across configuration changes.
     *
     * @param outState The Bundle in which to save the fragment's state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("edit", isEdit);
        outState.putParcelable("syncSettings", settings);
    }

    /**
     * Handles the result of activities launched with startActivityForResult().
     *
     * @param requestCode The code that was used to identify the request.
     * @param resultCode  The result code returned by the activity.
     * @param data        Additional data from the activity result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_SYNC_FOLDER) {

            if (resultCode == getActivity().RESULT_OK) {
                Intent dstData = data;
                String dstRepoId, dstRepoName, dstDir;

                dstRepoName = dstData.getStringExtra(SeafileSyncPathChooserActivity.DATA_REPO_NAME);
                dstRepoId = dstData.getStringExtra(SeafileSyncPathChooserActivity.DATA_REPO_ID);
                dstDir = dstData.getStringExtra(SeafileSyncPathChooserActivity.DATA_DIR);

                settings.setRepoId(dstRepoId);
                settings.setRepoPath(dstDir);

                syncManagementTargetFolder.setText(dstDir);
                syncManagementTargetFolder.setVisibility(View.VISIBLE);

            }

        } else if (requestCode == CHOOSE_LOCAL_FOLDER) {
            if (resultCode == getActivity().RESULT_OK) {
                if (data != null) {
                    String selectedFolderPath = data.getStringExtra("selected_folder_path");
                    setResourceSetting(selectedFolderPath);
                }
            }
        } else if (requestCode == CHOOSE_ALBUM) {
            if (resultCode == getActivity().RESULT_OK) {
                if (data != null) {
                    String albumPath = data.getStringExtra("album_path");
                    setResourceSetting(albumPath);
                }
            }
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.sync_management_expiration_file_spinner) {
            updateExpirationFilesInSettings(position);
        } else if (parent.getId() == R.id.sync_management_expiration_file_custom_spinner) {
            updateCustomExpirationFilesInSettings(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Displays a date picker dialog and sets a callback for when a date is selected.
     */
    private void showDatePickerDialog() {
        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.datePickerDateSet = new DatePickerFragment.DatePickerDateSet() {
            @Override
            public void onDateSet(Date date) {
                textDateSetExpireSetting(date);
            }
        };
        datePickerFragment.show(requireActivity().getSupportFragmentManager(), "datePicker");
    }

    /**
     * Displays a time picker dialog and sets a callback for when a time is selected.
     */
    private void showTimePickerDialog() {
        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTimePickerTimeSet(new TimePickerFragment.TimePickerTimeSet() {
            @Override
            public void onTimeSet(int hourOfDay, int minute) {
                textTimeSetTime(hourOfDay, minute);
            }
        });
        timePickerFragment.show(requireActivity().getSupportFragmentManager(), "timePicker");
    }

    private String formattedDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    /**
     * Updates the text with the selected date in the specified format.
     *
     * @param date The selected date.
     */
    private void textDateSetExpireSetting(Date date) {
        updateDayExpireDate(date);
        syncManagementTextDate.setText(formattedDate(date));
    }

    /**
     * Updates the text with the selected time in the specified format.
     *
     * @param hourOfDay The selected hour of the day.
     * @param minute    The selected minute.
     */
    private void textTimeSetTime(int hourOfDay, int minute) {
        String min = minute < 10 ? "0" + minute : minute + "";
        String hour = hourOfDay == 0 ? "0" + hourOfDay : hourOfDay + "";
        updateTimeExpireDate(hourOfDay, minute);
        syncManagementTextTime.setText(hour + ":" + min);
    }

    /**
     * Updates the day part of the expiration date with the new date.
     *
     * @param newDate The new date to update to.
     */
    private void updateDayExpireDate(Date newDate) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(settings.getExpireDate());
        int hora = calendar1.get(Calendar.HOUR_OF_DAY);
        int minutos = calendar1.get(Calendar.MINUTE);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(newDate);
        calendar2.set(Calendar.HOUR_OF_DAY, hora);
        calendar2.set(Calendar.MINUTE, minutos);

        settings.setExpireDate(calendar2.getTime());
    }

    /**
     * Updates the time part of the expiration date with the new hour and minute.
     *
     * @param hourOfDay The new hour of the day.
     * @param minute    The new minute.
     */
    private void updateTimeExpireDate(int hourOfDay, int minute) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(settings.getExpireDate());
        calendar1.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar1.set(Calendar.MINUTE, minute);

        settings.setExpireDate(calendar1.getTime());
    }

    /**
     * Initializes source and target UI components.
     */
    private void inititalSourceAndTarget() {

        syncManagementSourceConstraint = getView().findViewById(R.id.sync_management_source_constraint);
        syncManagementTargetConstraint = getView().findViewById(R.id.sync_management_target_constraint);

        syncManagementSource = getView().findViewById(R.id.sync_management_source);
        syncManagementTarget = getView().findViewById(R.id.sync_management_target);

        syncManagementSourceFolder = getView().findViewById(R.id.sync_management_source_folder);
        syncManagementTargetFolder = getView().findViewById(R.id.sync_management_target_folder);

        syncManagementSourceArrow = getView().findViewById(R.id.sync_management_source_arrow);
        syncManagementTargetArrow = getView().findViewById(R.id.sync_management_target_arrow);

        changeColorImageView(syncManagementSourceArrow, getContext().getColor(R.color.light_grey));
        changeColorImageView(syncManagementTargetArrow, getContext().getColor(R.color.light_grey));

    }

    /**
     * Initializes backup settings UI components and sets listeners.
     */
    private void initialBackupSetting() {
        syncManagementButtonFull = getView().findViewById(R.id.sync_management_mode_full);
        syncManagementButtonIncremental = getView().findViewById(R.id.sync_management_mode_incremental);

        syncManagementSwitchWifi = getView().findViewById(R.id.sync_management_wifi_switch);
        syncManagementSwitchVideo = getView().findViewById(R.id.sync_management_video_switch);
    }

    /**
     * Initializes backup expiration UI components and sets listeners.
     */
    private void initialBackupExpiration() {

        //Unavailable
        syncManagementUnavailableExpirations = getView().findViewById(R.id.sync_management_unavailable_expirations);

        //General
        syncManagementTypeTemporary = getView().findViewById(R.id.sync_management_type_temporary);
        syncManagementTypePermanet = getView().findViewById(R.id.sync_management_type_permanent);

        //Expiration
        syncManagementCardviewExpirations = getView().findViewById(R.id.sync_management_cardview_expirations);

        //Permanent
        syncManagementExpirationPermanetConstraint = getView().findViewById(R.id.sync_management_expiration_permanent_constraint);

        syncManagementExpirationFileSpinner = getView().findViewById(R.id.sync_management_expiration_file_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, expirationOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        syncManagementExpirationFileSpinner.setAdapter(adapter);

        syncManagementExpirationFileCustomNumber = getView().findViewById(R.id.sync_management_expiration_file_custom_number);

        syncManagementExpirationFileCustomSpinner = getView().findViewById(R.id.sync_management_expiration_file_custom_spinner);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, customExpirationOptions);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        syncManagementExpirationFileCustomSpinner.setAdapter(adapter2);

        //Temporary
        syncManagementExpirationTemporaryConstraint = getView().findViewById(R.id.sync_management_expiration_temporary_constraint);

        syncManagementTextDate = getView().findViewById(R.id.sync_management_text_date);
        syncManagementTextTime = getView().findViewById(R.id.sync_management_text_time);

        syncManagementDeleteAllFiles = getView().findViewById(R.id.sync_management_delete_all_files);

    }

    /**
     * Initializes UI components.
     */
    private void initialComponents() {
        //Header
        syncManagementErrorMessage = getView().findViewById(R.id.sync_management_error_message);
        syncManagementSyncStatusText = getView().findViewById(R.id.sync_management_sync_status_text);
        syncManagementSyncStatus = getView().findViewById(R.id.sync_management_sync_status);


        //Body
        inititalSourceAndTarget();
        initialBackupSetting();
        initialBackupExpiration();

        //Footer
        syncManagementSave = getView().findViewById(R.id.sync_management_save);
        syncManagementDelete = getView().findViewById(R.id.sync_management_delete);


    }

    /**
     * Creates and returns a View.OnTouchListener to handle touch events for UI components.
     *
     * @param constraint The ConstraintLayout to listen for touch events on.
     * @param text       The TextView for the text portion.
     * @param folder     The TextView for the folder portion.
     * @param arrow      The ImageView to change color when touched.
     * @param isSource   A flag indicating whether the component is a source.
     * @return A View.OnTouchListener instance.
     */
    private View.OnTouchListener createOptionTouchListener(ConstraintLayout constraint, TextView text, TextView folder, ImageView arrow, Boolean isSource) {

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
                        if (isClick) {
                            isClick = wantToClick(event, initialX, initialY);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        isClick = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        isClick = false;
                        if (isSource) {

                            if (settings.getType() == SeafSyncType.Album) {
                                Intent intent = new Intent(getActivity(), BucketsActivity.class);
                                startActivityForResult(intent, CHOOSE_ALBUM);
                            } else {
                                Intent intent = new Intent(getActivity(), SelectSyncFolderActivity.class);
                                startActivityForResult(intent, CHOOSE_LOCAL_FOLDER);
                            }

                        } else {
                            Intent chooserIntent = new Intent(getContext(), SeafileSyncPathChooserActivity.class);
                            chooserIntent.putExtra("account", accountManager.getCurrentAccount());
                            startActivityForResult(chooserIntent, CHOOSE_SYNC_FOLDER);
                        }
                        break;
                }

                if (isClick) {
                    constraint.setBackgroundColor(getContext().getColor(R.color.fancy_dark_gray));
//                    text.setTextColor(Color.WHITE);
//                    folder.setTextColor(Color.WHITE);
                    changeColorImageView(arrow, Color.WHITE);
                } else {
                    constraint.setBackgroundColor(Color.WHITE);
//                    text.setTextColor(Color.BLACK);
//                    folder.setTextColor(Color.BLACK);
                    changeColorImageView(arrow, getContext().getColor(R.color.light_grey));
                }

                return true;
            }
        };

    }

    /**
     * Changes the color of an ImageView by tinting it with the specified color.
     *
     * @param imageView The ImageView to change the color of.
     * @param color     The color to apply to the ImageView.
     */
    private void changeColorImageView(ImageView imageView, int color) {
        Drawable drawable = imageView.getDrawable();

        Drawable.ConstantState constantState = drawable.getConstantState();

        Drawable wrappedDrawable = constantState.newDrawable();
        DrawableCompat.setTint(wrappedDrawable, color);

        imageView.setImageDrawable(wrappedDrawable);
    }

    /**
     * Determines whether the touch event should be considered a click based on initial and current positions.
     *
     * @param event    The MotionEvent object.
     * @param initialX The initial X position.
     * @param initialY The initial Y position.
     * @return True if the event should be treated as a click; false otherwise.
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
     * Adjusts the settings and UI components based on the current synchronization settings.
     */
    private void adjustComponents() {

        syncManagementSyncStatus.setOnCheckedChangeListener(this);

        //Error messages
        adjustErrorMessages();

        //Sync status
        syncManagementSyncStatus.setChecked(settings.isActive());

        //SourceAndTarget
        adjustSourceAndTarget();

        //Backup settings
        adjustBackupSettings();

        //Backup expiration
        adjustBackupExpirations();

        if (isEdit) {
            syncManagementDelete.setVisibility(View.VISIBLE);
            syncManagementSave.setVisibility(View.VISIBLE);
            syncManagementSave.setText(getResources().getText(R.string.general_update));
        } else {
            syncManagementDelete.setVisibility(View.GONE);
        }

        syncManagementSave.setOnClickListener(this);
        syncManagementDelete.setOnClickListener(this);

    }

    private void adjustErrorMessages() {
        if (settings.getStatus() == SeafSyncStatus.Error) {
            syncManagementErrorMessage.setText(getResources().getString(R.string.unknow_error));
        } else if (settings.getStatus() == SeafSyncStatus.ErrorNotExistLocalFolder) {
            syncManagementErrorMessage.setText(getResources().getString(R.string.sync_status_error_not_exist_local_folder));
        } else if (settings.getStatus() == SeafSyncStatus.ErrorNotExistRepoDir) {
            syncManagementErrorMessage.setText(getResources().getString(R.string.sync_status_error_not_exist_repo_dir));
        } else if (settings.getStatus() == SeafSyncStatus.ErrorOutSpace) {
            syncManagementErrorMessage.setText(getResources().getString(R.string.sync_status_error_out_space));
        } else if (settings.getStatus() == SeafSyncStatus.ErrorNotValidAllFile) {
            syncManagementErrorMessage.setText(getResources().getString(R.string.sync_status_error_not_valid_files));
        } else {
            syncManagementErrorMessage.setVisibility(View.GONE);
        }

    }

    private void adjustSourceAndTarget() {
        if (settings.getType() == SeafSyncType.Gallery) {
            syncManagementSource.setText(getResources().getString(R.string.gallery));
            syncManagementSourceFolder.setVisibility(View.GONE);
            syncManagementSourceArrow.setVisibility(View.INVISIBLE);
            syncManagementSourceConstraint.setOnTouchListener(null);
        } else if (settings.getType() == SeafSyncType.Album) {
            syncManagementSource.setText(getResources().getString(R.string.album));
        } else if (settings.getType() == SeafSyncType.Folder) {
            syncManagementSource.setText(getResources().getString(R.string.folder));
        }

        if (settings.getResourceUri() == null) {
            syncManagementSourceFolder.setVisibility(View.GONE);
        } else {
            syncManagementSourceFolder.setText(settings.getDirNameOfResource());
        }

        if (settings.getRepoPath() == null) {
            syncManagementTargetFolder.setVisibility(View.GONE);
        } else {
            syncManagementTargetFolder.setText(settings.getRepoPath());
        }

        if (settings.getType() != SeafSyncType.Gallery) {
            syncManagementSourceConstraint.setOnTouchListener(createOptionTouchListener(syncManagementSourceConstraint, syncManagementSource, syncManagementSourceFolder, syncManagementSourceArrow, true));
        }

        syncManagementTargetConstraint.setOnTouchListener(createOptionTouchListener(syncManagementTargetConstraint, syncManagementTarget, syncManagementTargetFolder, syncManagementTargetArrow, false));

    }

    private void adjustBackupSettings() {

        syncManagementButtonFull.setOnClickListener(this);
        syncManagementButtonIncremental.setOnClickListener(this);

        syncManagementSwitchWifi.setOnCheckedChangeListener(this);
        syncManagementSwitchVideo.setOnCheckedChangeListener(this);

        if (settings.getNetwork() == SeafSyncNetwork.Wifi) {
            syncManagementSwitchWifi.setChecked(true);
        } else if (settings.getNetwork() == SeafSyncNetwork.WifiAndData) {
            syncManagementSwitchWifi.setChecked(false);
        }

        syncManagementSwitchVideo.setChecked(settings.isUploadVideos());

        if (settings.getMode() == SeafSyncMode.Incremental) {
            syncManagementButtonFull.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
            syncManagementButtonIncremental.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
        } else if (settings.getMode() == SeafSyncMode.Complete) {
            syncManagementButtonFull.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
            syncManagementButtonIncremental.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
        }
    }

    private void adjustBackupExpirations() {
        syncManagementCardviewExpirations.setVisibility(View.GONE);

        syncManagementTextDate.setOnClickListener(this);
        syncManagementTextTime.setOnClickListener(this);

        syncManagementExpirationFileSpinner.setOnItemSelectedListener(this);
        syncManagementExpirationFileCustomSpinner.setOnItemSelectedListener(this);

        syncManagementExpirationFileCustomNumber.setEnabled(true);
        syncManagementDeleteAllFiles.setEnabled(true);


        if (!isEdit) {
            syncManagementTypeTemporary.setOnClickListener(this);
            syncManagementTypePermanet.setOnClickListener(this);
        }

        if (settings.getExpireDate() != null) {
            adjustTemporaryExpiration();
        } else {
            adjustPermanetExpiration();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {

            AccountInfo accountInfo = getAccountInfo();

            if (accountInfo != null) {
                handler.post(() -> {
                    if (accountInfo.getPlan() != AccountPlans.Basic) {
                        syncManagementCardviewExpirations.setVisibility(View.VISIBLE);
                        syncManagementUnavailableExpirations.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void adjustTemporaryExpiration() {
        syncManagementTypeTemporary.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);
        syncManagementTypePermanet.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);

        syncManagementExpirationTemporaryConstraint.setVisibility(View.VISIBLE);
        textDateSetExpireSetting(settings.getExpireDate());
        textTimeSetTime(settings.getExpireDate().getHours(), settings.getExpireDate().getMinutes());

        syncManagementExpirationPermanetConstraint.setVisibility(View.GONE);

        syncManagementDeleteAllFiles.setChecked(settings.isDeletedAllFiles());

        if (!isEdit) {
            settings.setTimeExpirationFiles(-1);
            setExpirationTypeInSpinner(-1);
        } else {
            syncManagementTypeTemporary.setBackgroundResource(R.drawable.button_shape_block_sync_mode);
        }

    }

    private void adjustPermanetExpiration() {
        syncManagementTypeTemporary.setBackgroundResource(R.drawable.button_shape_disable_sync_mode);
        syncManagementTypePermanet.setBackgroundResource(R.drawable.button_shape_enable_sync_mode);

        syncManagementExpirationPermanetConstraint.setVisibility(View.VISIBLE);
        setExpirationTypeInSpinner(settings.getTimeExpirationFiles());

        syncManagementExpirationTemporaryConstraint.setVisibility(View.GONE);

        if (isEdit) {
            int index = syncManagementExpirationFileSpinner.getSelectedItemPosition();
            updateExpirationFilesInSettings(index);
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{expirationOptions[index]});
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            syncManagementExpirationFileSpinner.setAdapter(adapter);

//            index = syncManagementExpirationFileCustomSpinner.getSelectedItemPosition();
//            ArrayAdapter<String> adapter2 = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{customExpirationOptions[index]});
//            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            syncManagementExpirationFileCustomSpinner.setAdapter(adapter2);

            syncManagementExpirationFileCustomNumber.setText(settings.getNumberExpiration() + "");
            syncManagementTypePermanet.setBackgroundResource(R.drawable.button_shape_block_sync_mode);
        }
    }

    /**
     * Initializes the settings service and account for synchronization.
     */
    private void initSettingsServiceAndAccount() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            settings.setAccountId(accountManager.getCurrentAccount().getEmail());
            settingsService = new SeafSyncSettingsService();
        });
    }

    /**
     * Deletes the synchronization settings.
     */
    private void deleteSettings(Handler handler) {
        handler.post(() -> {
            DeleteSyncDialog.build(getContext()).setResponse(
                    new DeleteSyncDialog.DeleteSyncDialogResponse() {
                        @Override
                        public void deleteSynchronization() {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                settingsService.remove(settings);
                                handler.post(() -> {
                                    getActivity().onBackPressed();
                                });
                            });
                        }
                    }).show();
        });

    }

    /**
     * Saves the synchronization settings.
     */
    private void saveSettings(Handler handler) {
        if (!(settings.getType() == SeafSyncType.Gallery || settings.getResourceUri() != null)) {
            handler.post(() -> {
                Toast.makeText(getContext(), getResources().getString(R.string.settings_select_origin), Toast.LENGTH_SHORT).show();
            });
        } else if (settings.getRepoPath() == null && settings.getRepoId() == null) {
            handler.post(() -> {
                Toast.makeText(getContext(), getResources().getString(R.string.settings_select_destination), Toast.LENGTH_SHORT).show();
            });

        } else {

            boolean err = false;

            if (settings.getExpireDate() != null) {
                settings.setDeletedAllFiles(syncManagementDeleteAllFiles.isChecked());
            } else if (syncManagementExpirationFileCustomSpinner.getVisibility() == View.VISIBLE && syncManagementUnavailableExpirations.getVisibility() == View.GONE) {
                try {
                    int number = Integer.parseInt(syncManagementExpirationFileCustomNumber.getText().toString());
                    settings.setNumberExpiration(number);
                } catch (Exception e) {
                    err = true;
                    e.printStackTrace();
                }
            }

            if (!err) {
                if (settingsService.isSameOriginDestination(settings)) {
                    handler.post(() -> {
                        Toast.makeText(getContext(), getResources().getString(R.string.exists_same_source_target), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    settingsService.add(settings);
                    SyncWokerManager.createSyncWoker(getContext());
                    handler.post(() -> {
                        getActivity().onBackPressed();
                    });
                }
            }

        }
    }

    /**
     * Updates the synchronization settings.
     */
    private void updateSettings(Handler handler) {

        boolean err = false;

        if (settings.getExpireDate() != null) {
            settings.setDeletedAllFiles(syncManagementDeleteAllFiles.isChecked());
        } else if (syncManagementExpirationFileCustomSpinner.getVisibility() == View.VISIBLE && syncManagementUnavailableExpirations.getVisibility() == View.GONE) {
            try {
                int number = Integer.parseInt(syncManagementExpirationFileCustomNumber.getText().toString());
                settings.setNumberExpiration(number);
            } catch (Exception e) {
                err = true;
                e.printStackTrace();
            }
        } else if (syncManagementExpirationFileCustomSpinner.getVisibility() != View.VISIBLE && isEdit) {
            settings.setNumberExpiration(-1);
        }

        if (!err) {
            if (settingsService.isSameOriginDestination(settings)) {
                handler.post(() -> {
                    Toast.makeText(getContext(), getResources().getString(R.string.exists_same_source_target), Toast.LENGTH_SHORT).show();
                });
            } else {
                settingsService.update(settings);
                SyncWokerManager.createSyncWoker(getContext());
                handler.post(() -> {
                    getActivity().onBackPressed();
                });
            }
        }
    }

    private void executeActionForSynchronization(int actionSync) {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {

            while (settingsService == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!actionPending) {
                try {
                    actionPending = true;
                    switch (actionSync) {
                        case ACTION_CREATE_SYNC:
                            saveSettings(handler);
                            break;
                        case ACTION_DELETE_SYNC:
                            deleteSettings(handler);
                            break;
                        case ACTION_UPDATE_SYNC:
                            updateSettings(handler);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                actionPending = false;
            }

        });


    }

    /**
     * Sets the resource URI for synchronization based on the provided path.
     *
     * @param path The path to the resource.
     */
    private void setResourceSetting(String path) {
        File f = new File(path);
        if (f.exists()) {
            settings.setResourceUri(Uri.fromFile(f));
            syncManagementSourceFolder.setText(f.getName());
            syncManagementSourceFolder.setVisibility(View.VISIBLE);
        }
    }

    private AccountInfo getAccountInfo() {
        try {
            AccountManager accountManager = new AccountManager(getContext());
            DataManager manager = new DataManager(accountManager.getCurrentAccount());
            AccountInfo accountInfo = manager.getAccountInfo();
            return accountInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private long getTimeExpire(int typeExpiration) {
        switch (typeExpiration) {
            case EXPIRATION_IN_DAY:
                return 86400000L;
            case EXPIRATION_IN_WEEK:
                return 604800000L;
            case EXPIRATION_IN_MONTH:
                return 2629800000L;
            case EXPIRATION_IN_YEAR:
                return 31557600000L;
            default:
                return -1L;
        }
    }

    private int getTypeExpiration(long expiration) {

        if (expiration == 86400000L) {
            return EXPIRATION_IN_DAY;
        } else if (expiration == 604800000L) {
            return EXPIRATION_IN_WEEK;
        } else if (expiration == 2629800000L) {
            return EXPIRATION_IN_MONTH;
        } else if (expiration == 31557600000L) {
            return EXPIRATION_IN_YEAR;
        } else if (expiration == -1) {
            return EXPIRATION_INDEFINITE;
        } else {
            return EXPIRATION_CUSTOM;
        }

    }

    private void setExpirationTypeInSpinner(long expiration) {

        if (settings.getNumberExpiration() != -1) {

            syncManagementExpirationFileSpinner.setSelection(4);

        } else if (settings.getTimeExpirationFiles() != -1) {

            int type = getTypeExpiration(expiration);

            switch (type) {
                case EXPIRATION_IN_WEEK:
                    syncManagementExpirationFileSpinner.setSelection(0);
                    break;
                case EXPIRATION_IN_MONTH:
                    syncManagementExpirationFileSpinner.setSelection(1);
                    break;
                case EXPIRATION_IN_YEAR:
                    syncManagementExpirationFileSpinner.setSelection(2);
                    break;
            }

        } else {
            syncManagementExpirationFileSpinner.setSelection(3);
        }

    }

    private void updateExpirationFilesInSettings(int position) {

        switch (position) {
            case EXPIRATION_IN_WEEK:
                syncManagementExpirationFileCustomNumber.setVisibility(View.GONE);
                syncManagementExpirationFileCustomSpinner.setVisibility(View.GONE);
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_WEEK));
                break;

            case EXPIRATION_IN_MONTH:
                syncManagementExpirationFileCustomNumber.setVisibility(View.GONE);
                syncManagementExpirationFileCustomSpinner.setVisibility(View.GONE);
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_MONTH));
                break;

            case EXPIRATION_IN_YEAR:
                syncManagementExpirationFileCustomNumber.setVisibility(View.GONE);
                syncManagementExpirationFileCustomSpinner.setVisibility(View.GONE);
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_YEAR));
                break;

            case EXPIRATION_INDEFINITE:
                syncManagementExpirationFileCustomNumber.setVisibility(View.GONE);
                syncManagementExpirationFileCustomSpinner.setVisibility(View.GONE);
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_INDEFINITE));
                break;

            case EXPIRATION_CUSTOM:
                syncManagementExpirationFileCustomNumber.setVisibility(View.VISIBLE);
                syncManagementExpirationFileCustomSpinner.setVisibility(View.VISIBLE);

                if (isEdit) {
                    int number = settings.getNumberExpiration() == -1 ? 1 : settings.getNumberExpiration();
                    syncManagementExpirationFileCustomNumber.setText(number + "");
                    setTypeExpirationCustomSpinner(getTypeExpiration(settings.getTimeExpirationFiles()));
                } else {
                    syncManagementExpirationFileCustomNumber.setText("1");
                    syncManagementExpirationFileCustomSpinner.setSelection(1);
                    settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_WEEK));
                }

                break;
        }
    }

    private void updateCustomExpirationFilesInSettings(int position) {

        switch (position) {
            case 0:
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_DAY));
                break;
            case 1:
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_WEEK));
                break;
            case 2:
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_MONTH));
                break;
            case 3:
                settings.setTimeExpirationFiles(getTimeExpire(EXPIRATION_IN_YEAR));
                break;
        }
    }

    private void setTypeExpirationCustomSpinner(long type) {

        if (type == EXPIRATION_IN_DAY) {
            syncManagementExpirationFileCustomSpinner.setSelection(0);
        } else if (type == EXPIRATION_IN_WEEK) {
            syncManagementExpirationFileCustomSpinner.setSelection(1);
        } else if (type == EXPIRATION_IN_MONTH) {
            syncManagementExpirationFileCustomSpinner.setSelection(2);
        } else if (type == EXPIRATION_IN_YEAR) {
            syncManagementExpirationFileCustomSpinner.setSelection(3);
        }

    }

}