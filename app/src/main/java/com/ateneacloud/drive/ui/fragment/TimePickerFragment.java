package com.ateneacloud.drive.ui.fragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import androidx.fragment.app.DialogFragment;

import com.ateneacloud.drive.R;

import java.util.Calendar;

/**
 * A DialogFragment for picking a time using a TimePickerDialog.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    /**
     * Callback interface to notify the time selection.
     */
    private TimePickerTimeSet timePickerTimeSet;

    /**
     * Sets the callback interface to handle the time selection.
     *
     * @param timePickerTimeSet The callback interface to handle time selection.
     */
    public void setTimePickerTimeSet(TimePickerTimeSet timePickerTimeSet) {
        this.timePickerTimeSet = timePickerTimeSet;
    }

    /**
     * Creates and returns a new TimePickerDialog with the current time as the default.
     *
     * @param savedInstanceState The saved instance state, if any.
     * @return A new TimePickerDialog instance.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), R.style.CustomPickerDialog, this, hour, minute, true);
    }

    /**
     * Called when a time is set in the TimePickerDialog.
     *
     * @param view      The TimePicker view.
     * @param hourOfDay The selected hour of the day.
     * @param minute    The selected minute.
     */
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (timePickerTimeSet != null) {
            timePickerTimeSet.onTimeSet(hourOfDay, minute);
        }
    }

    /**
     * Callback interface to notify the time selection.
     */
    public interface TimePickerTimeSet {

        /**
         * Called when a time is set.
         *
         * @param hourOfDay The selected hour of the day.
         * @param minute    The selected minute.
         */
        void onTimeSet(int hourOfDay, int minute);
    }
}