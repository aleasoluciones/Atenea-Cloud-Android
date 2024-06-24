package com.ateneacloud.drive.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ateneacloud.drive.R;

public class ChangePlanDialog {

    public static final int NOT_ALLOWED_FILES = 0, CHANGE_PLAN = 1, WITHOUT_STORAGE = 2,
            NOT_SUPPORT_4K = 3, NOT_ALLOWED_FILE = 4, NOT_ALLOWED_IMAGE = 5;

    private ChangePlanDialogResponse changePlanDialogResponse;

    private Context context;
    private int type;

    private ChangePlanDialog(Context context, int type){
        this.context = context;
        this.type = type;
    }

    public static ChangePlanDialog build(Context context, int type){
        return new ChangePlanDialog(context, type);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.plan_change))
                .setMessage(getTypeChangePlan(context, type))
                .setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(changePlanDialogResponse != null){
                            changePlanDialogResponse.changePlanDialogResponseYes();
                        }
                    }
                })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(changePlanDialogResponse != null){
                            changePlanDialogResponse.changePlanDialogResponseNo();
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (changePlanDialogResponse != null) {
                            changePlanDialogResponse.changePlanDialogResponseNeither();
                        }
                    }
                }).show();
    }

    private String getTypeChangePlan(Context context,int type){
        switch (type){
            case NOT_ALLOWED_FILES:
                return context.getResources().getString(R.string.not_allowed_files)+"\n"+context.getResources().getString(R.string.want_plan_change);

            case WITHOUT_STORAGE:
                return context.getResources().getString(R.string.without_storage)+"\n"+context.getResources().getString(R.string.want_plan_change);

            case NOT_SUPPORT_4K:
                return context.getResources().getString(R.string.not_support_4k)+"\n"+context.getResources().getString(R.string.want_plan_change);

            case NOT_ALLOWED_FILE:
                return context.getResources().getString(R.string.not_allowed_file)+"\n"+context.getResources().getString(R.string.want_plan_change);

            case NOT_ALLOWED_IMAGE:
                return context.getResources().getString(R.string.not_allowed_image)+"\n"+context.getResources().getString(R.string.want_plan_change);

            default:
                return context.getResources().getString(R.string.want_plan_change);
        }
    }

    public ChangePlanDialog setResponse(ChangePlanDialogResponse changePlanDialogResponse) {
        this.changePlanDialogResponse = changePlanDialogResponse;
        return this;
    }

    public interface ChangePlanDialogResponse {

        public void changePlanDialogResponseYes();

        public void changePlanDialogResponseNo();

        public void changePlanDialogResponseNeither();
    }

}
