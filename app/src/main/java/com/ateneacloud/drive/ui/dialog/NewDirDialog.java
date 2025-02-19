package com.ateneacloud.drive.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafDirentTrash;

import java.util.ArrayList;
import java.util.List;

class NewDirTask extends TaskDialog.Task {
    String repoID;
    String parentDir;
    String dirName;
    DataManager dataManager;
    boolean run;

    public NewDirTask(String repoID, String parentDir,
                      String dirName, DataManager dataManager, boolean run) {
        this.repoID = repoID;
        this.parentDir = parentDir;
        this.dirName = dirName;
        this.dataManager = dataManager;
        this.run = run;
    }

    @Override
    protected void runTask() {
        if (run) {
            try {
                dataManager.createNewDir(repoID, parentDir, dirName);
            } catch (SeafException e) {
                setTaskException(e);
            }
        }
    }
}

public class NewDirDialog extends TaskDialog {

    private static final String STATE_TASK_REPO_ID = "new_dir_task.repo_id";
    private static final String STATE_TASK_PARENT_DIR = "new_dir_task.parent_dir";
    private static final String STATE_ACCOUNT = "new_dir_task.account.account";

    private EditText dirNameText;
    private DataManager dataManager;
    private Account account;

    private String repoID;
    private String parentDir;
    private List<SeafDirentTrash> direntTrashList;
    private boolean runTask = true;

    public String getNewDirName() {
        return dirNameText.getText().toString().trim();
    }

    public void init(String repoID, String parentDir, Account account, List<SeafDirentTrash> direntTrashList) {
        this.repoID = repoID;
        this.parentDir = parentDir;
        this.account = account;
        this.direntTrashList = direntTrashList;
    }

    public void init(String repoID, String parentDir, Account account) {
        this.repoID = repoID;
        this.parentDir = parentDir;
        this.account = account;
        this.direntTrashList = new ArrayList<>();
    }

    private DataManager getDataManager() {
        if (dataManager == null) {
            dataManager = new DataManager(account);
        }

        return dataManager;
    }

    @Override
    protected View createDialogContentView(LayoutInflater inflater, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_new_dir, null);
        dirNameText = (EditText) view.findViewById(R.id.new_dir_name);

        if (savedInstanceState != null) {
            repoID = savedInstanceState.getString(STATE_TASK_REPO_ID);
            parentDir = savedInstanceState.getString(STATE_TASK_PARENT_DIR);
            account = (Account) savedInstanceState.getParcelable(STATE_ACCOUNT);
        }

        return view;
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        dialog.setTitle(getResources().getString(R.string.create_new_dir));
        dirNameText.setFocusable(true);
        dirNameText.setFocusableInTouchMode(true);
        dirNameText.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    protected void onValidateUserInput() throws Exception {
        String dirName = dirNameText.getText().toString().trim();

        if (dirName.length() == 0) {
            String err = getActivity().getResources().getString(R.string.dir_name_empty);
            throw new Exception(err);
        }
    }

    @Override
    protected void onSaveDialogContentState(Bundle outState) {
        outState.putString(STATE_TASK_PARENT_DIR, parentDir);
        outState.putString(STATE_TASK_REPO_ID, repoID);
        outState.putParcelable(STATE_ACCOUNT, account);
    }

    @Override
    protected void disableInput() {
        super.disableInput();
        dirNameText.setEnabled(false);
    }

    @Override
    protected void enableInput() {
        super.enableInput();
        dirNameText.setEnabled(true);
    }

    @Override
    protected NewDirTask prepareTask() {
        EditText dirNameText = (EditText) getContentView().findViewById(R.id.new_dir_name);
        String dirName = dirNameText.getText().toString().trim();
        //NewDirTask task = new NewDirTask(repoID, parentDir, dirName, getDataManager());
        return preparedTask(dirName);
    }

    private NewDirTask preparedTask(String dirName) {
        boolean existsInGarbage = false;
        final boolean[] run = {true};

        for (SeafDirentTrash dirent : direntTrashList) {
            String parentPath = parentDir.endsWith("/") ? parentDir : parentDir + "/";
            if (dirent.getTitle().equals(dirName) && true == dirent.isDir() && dirent.path.equals(parentPath)) {
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
                    handler.sendMessage(handler.obtainMessage());
                }

                @Override
                public void cancelTask() {
                    run[0] = false;
                    runTask = false;
                    handler.sendMessage(handler.obtainMessage());
                }
            }).show();


            try {
                Looper.loop();
            } catch (RuntimeException e) {
            }

        }

        return new NewDirTask(repoID, parentDir, dirName, getDataManager(), run[0]);
    }

    public boolean isRunTask() {
        return runTask;
    }
}