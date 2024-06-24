package com.ateneacloud.drive.ui.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;

import com.ateneacloud.drive.R;

import java.util.Calendar;
import java.util.Date;

/**
 * A DialogFragment for picking a date using a DatePickerDialog.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    /**
     * Callback interface to notify the date selection.
     */
    public DatePickerDateSet datePickerDateSet;

    /**
     * Default constructor for DatePickerFragment.
     * Required for Fragment creation.
     */
    public DatePickerFragment() {

    }

    /**
     * Creates and returns a new DatePickerDialog with the current date as the default.
     *
     * @param savedInstanceState The saved instance state, if any.
     * @return A new DatePickerDialog instance.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Obtiene la fecha actual
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Crea el DatePickerDialog y devuelve la instancia
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), R.style.CustomPickerDialog, this, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        return datePickerDialog;
    }

    /**
     * Called when a date is set in the DatePickerDialog.
     *
     * @param view  The DatePicker view.
     * @param year  The selected year.
     * @param month The selected month.
     * @param day   The selected day of the month.
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        Date date = calendar.getTime();

        if (datePickerDateSet != null) {
            datePickerDateSet.onDateSet(date);
        }
    }

    /**
     * Callback interface to notify the date selection.
     */
    public interface DatePickerDateSet {

        /**
         * Called when a date is set.
         *
         * @param date The selected date.
         */
        void onDateSet(Date date);
    }
}