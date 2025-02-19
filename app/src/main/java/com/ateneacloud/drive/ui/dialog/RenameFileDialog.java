package com.ateneacloud.drive.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafDirentTrash;
import com.ateneacloud.drive.util.Utils;

import java.util.List;

class RenameTask extends TaskDialog.Task {
    String repoID;
    String path;
    String newName;
    boolean isdir;
    DataManager dataManager;

    public RenameTask(String repoID, String path,
                      String newName, boolean isdir, DataManager dataManager) {
        this.repoID = repoID;
        this.path = path;
        this.newName = newName;
        this.isdir = isdir;
        this.dataManager = dataManager;
    }

    @Override
    protected void runTask() {
        if (newName.equals(Utils.fileNameFromPath(path))) {
            return;
        }
        try {
            dataManager.rename(repoID, path, newName, isdir);
        } catch (SeafException e) {
            setTaskException(e);
        }
    }
}

public class RenameFileDialog extends TaskDialog {
    private EditText fileNameText;
    private String originalName;
    private String repoID;
    private String path;
    private boolean isdir;
    private String extension;
    private DataManager dataManager;
    private Account account;
    private List<SeafDirentTrash> direntTrashList;

    private static final String STATE_REPO_ID = "rename_task.repo_name";
    private static final String STATE_PATH = "rename_task.repo_id";
    private static final String STATE_ISDIR = "rename_task.account";
    private static final String STATE_ACCOUNT = "rename_task.account";

    public void init(String repoID, String path, boolean isdir, Account account, List<SeafDirentTrash> direntTrashList) {
        this.repoID = repoID;
        this.path = path;
        this.isdir = isdir;
        this.account = account;
        this.direntTrashList = direntTrashList;
    }

    private DataManager getDataManager() {
        if (dataManager == null) {
            dataManager = new DataManager(account);
        }

        return dataManager;
    }

    public String getNewFileName() {
        return fileNameText.getText().toString().trim();
    }

    @Override
    protected View createDialogContentView(LayoutInflater inflater, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_new_file, null);
        fileNameText = (EditText) view.findViewById(R.id.new_file_name);

        if (savedInstanceState != null) {
            repoID = savedInstanceState.getString(STATE_REPO_ID);
            path = savedInstanceState.getString(STATE_PATH);
            isdir = savedInstanceState.getBoolean(STATE_ISDIR);
            account = (Account) savedInstanceState.getParcelable(STATE_ACCOUNT);
        }

        String fileName = Utils.fileNameFromPath(path);
        originalName = fileName;
        extension = "";

        try {
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex > 0 && !isdir) {

                extension = fileName.substring(lastDotIndex + 1);
                if (!extension.isEmpty()) {
                    fileName = fileName.substring(0, lastDotIndex);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(fileName)) {
            fileNameText.setText(fileName);
            fileNameText.setSelection(fileName.length());
        }

        return view;
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        String str = getActivity().getString(isdir ? R.string.rename_dir : R.string.rename_file);
        dialog.setTitle(str);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    protected void onValidateUserInput() throws Exception {
        String fileName = fileNameText.getText().toString().trim();

        if (fileName.length() == 0) {
            String err = getActivity().getResources().getString(R.string.file_name_empty);
            throw new Exception(err);
        }
    }

    @Override
    protected RenameTask prepareTask() {
        RenameTask task = new RenameTask(repoID, path, getNewNameFile(), isdir, getDataManager());
        return task;
    }

    @Override
    protected void disableInput() {
        super.disableInput();
        fileNameText.setEnabled(false);
    }

    @Override
    protected void enableInput() {
        super.enableInput();
        fileNameText.setEnabled(true);
    }

    @Override
    protected void onSaveDialogContentState(Bundle outState) {
        outState.putString(STATE_REPO_ID, repoID);
        outState.putString(STATE_PATH, path);
        outState.putBoolean(STATE_ISDIR, isdir);
        outState.putParcelable(STATE_ACCOUNT, account);
    }

    private String getNewNameFile() {

        final String[] finalName = {""};
        String newName = extension.isEmpty() ? fileNameText.getText().toString().trim() : fileNameText.getText().toString().trim() + "." + extension;
        finalName[0] = newName;

        boolean existsInGarbage = false;

        for (SeafDirentTrash dirent : direntTrashList) {
            String parentPath = Utils.getParentPath(path).endsWith("/") ? Utils.getParentPath(path) : Utils.getParentPath(path) + "/";
            if (dirent.getTitle().equals(newName) && isdir == dirent.isDir() && dirent.path.equals(parentPath)) {
                existsInGarbage = true;
                break;
            }
        }

        if (existsInGarbage) {

            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message mesg) {
                    throw new RuntimeException();
                }
            };

            StepRecycleBin.build(getContext()).setOnReplaceTrash(new StepRecycleBin.ReplaceTrashTask() {
                @Override
                public void continueTask() {
                    finalName[0] = newName;
                    handler.sendMessage(handler.obtainMessage());
                }

                @Override
                public void cancelTask() {
                    finalName[0] = originalName;
                    handler.sendMessage(handler.obtainMessage());
                }
            }).show();


            try {
                Looper.loop();
            } catch (RuntimeException e) {
            }

        }

        return finalName[0];
    }
}
