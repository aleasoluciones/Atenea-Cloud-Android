package com.ateneacloud.drive.ui.dialog;

import static com.google.common.io.Resources.getResource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.clock.SyncWokerManager;

public class TimePreferenceDialog extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private int default_value = 2;
    private static final int MAX_VALUE = 4;
    private static final int MIN_VALUE = 0;

    private SeekBar seekBar;
    private TextView textView;
    private int currentValue;

    private final SharedPreferences preferences = SeadroidApplication.getAppContext().getSharedPreferences("SeafileSyncPreferences", Context.MODE_PRIVATE);

    public TimePreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.time_preference_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        int syncTime = preferences.getInt("syncTime", SyncWokerManager.DEFAULT_VALUE);
        default_value = syncTime;
        currentValue = default_value;
        seekBar = view.findViewById(R.id.seekBar);
        textView = view.findViewById(R.id.textView);

        seekBar.setMax(MAX_VALUE - MIN_VALUE);
        seekBar.setProgress(default_value - MIN_VALUE);
        seekBar.setOnSeekBarChangeListener(this);

        updateTextView(default_value);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(currentValue);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("syncTime", currentValue);
            editor.apply();

            if (SyncWokerManager.isSyncWorkerScheduled(SeadroidApplication.getAppContext())) {
                SyncWokerManager.deleteSyncWoker(SeadroidApplication.getAppContext());
                SyncWokerManager.createSyncWoker(SeadroidApplication.getAppContext());
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, default_value);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        currentValue = restorePersistedValue ? getPersistedInt(default_value) : (Integer) defaultValue;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        currentValue = progress + MIN_VALUE;
        updateTextView(currentValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void updateTextView(int value) {
        switch (value) {
            case 0:
                textView.setText("30 " + getContext().getResources().getString(R.string.minutes));
                break;
            case 1:
                textView.setText(value + " " + getContext().getResources().getString(R.string.hour));
                break;
            default:
                textView.setText(value + " " + getContext().getResources().getString(R.string.hours));
                break;
        }
    }
}
