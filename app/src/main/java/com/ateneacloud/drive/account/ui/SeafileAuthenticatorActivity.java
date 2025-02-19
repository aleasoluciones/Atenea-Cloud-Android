package com.ateneacloud.drive.account.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.Authenticator;
import com.ateneacloud.drive.cameraupload.CameraUploadManager;
import com.ateneacloud.drive.ui.BaseAuthenticatorActivity;

/**
 * The Authenticator activity.
 *
 * Called by the Authenticator and in charge of identifing the user.
 *
 * It sends back to the Authenticator the result.
 */
public class SeafileAuthenticatorActivity extends BaseAuthenticatorActivity {

    public static final int SEACLOUD_CC = 0;
    public static final int SINGLE_SIGN_ON_LOGIN = 1;
    public static final int OTHER_SERVER = 2;

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_SERVER_URI = "SERVER_URI";
    public final static String ARG_EDIT_OLD_ACCOUNT_NAME = "EDIT_OLD_ACCOUNT";
    public final static String ARG_EMAIL = "EMAIL";
    public final static String ARG_NAME = "NAME";
    public final static String ARG_SHIB = "SHIB";
    public final static String ARG_AUTH_SESSION_KEY = "TWO_FACTOR_AUTH";
    public final static String ARG_IS_EDITING = "isEdited";

    private static final int REQ_SIGNUP = 1;

    private final String DEBUG_TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_create_type_select);

        String[] array = getResources().getStringArray(R.array.choose_server_array);
        String[] strArray = new String[1 + array.length];
        strArray[0] = getString(R.string.server_name_top);
        for (int i = 0; i < array.length; i++) {
            strArray[i + 1] = array[i];
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, R.layout.list_item_authenticator, strArray);
        ListView listView = (ListView)findViewById(R.id.account_create_list);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch ((int) id) {
                    case SEACLOUD_CC:
                        intent = new Intent(SeafileAuthenticatorActivity.this, AccountDetailActivity.class);
                        intent.putExtras(getIntent());
                        intent.putExtra(SeafileAuthenticatorActivity.ARG_SERVER_URI, getString(R.string.server_url_seacloud));
                        startActivityForResult(intent, SeafileAuthenticatorActivity.REQ_SIGNUP);
                        break;
                    case SINGLE_SIGN_ON_LOGIN:
                        intent = new Intent(SeafileAuthenticatorActivity.this, SingleSignOnActivity.class);
                        intent.putExtras(getIntent());
                        startActivityForResult(intent, SeafileAuthenticatorActivity.REQ_SIGNUP);
                        break;
                    case OTHER_SERVER:
                        launchAccountDetailActivity();
                        break;
                    default:
                        return;
                }
            }
        });

        mAccountManager = AccountManager.get(getBaseContext());

        if (getIntent().getBooleanExtra(ARG_SHIB, false)) {
            Intent intent = new Intent(this, SingleSignOnAuthorizeActivity.class);
            android.accounts.Account account = new android.accounts.Account(getIntent().getStringExtra(SeafileAuthenticatorActivity.ARG_ACCOUNT_NAME), com.ateneacloud.drive.account.Account.ACCOUNT_TYPE);
            intent.putExtra(SingleSignOnActivity.SINGLE_SIGN_ON_SERVER_URL, mAccountManager.getUserData(account, Authenticator.KEY_SERVER_URI));
            intent.putExtras(getIntent().getExtras());
            startActivityForResult(intent, SeafileAuthenticatorActivity.REQ_SIGNUP);
        } else if (getIntent().getBooleanExtra(ARG_IS_EDITING, false)) {
            Intent intent = new Intent(this, AccountDetailActivity.class);
            intent.putExtras(getIntent().getExtras());
            startActivityForResult(intent, SeafileAuthenticatorActivity.REQ_SIGNUP);
        }

        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.choose_server);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(SeafileAuthenticatorActivity.this, null);
            }
        });

        // When this activity starts, always start Detail Ativity
        // This is because Atenea Cloud only supports that type of login
        launchAccountDetailActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(DEBUG_TAG, "onActivityResult");

        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(data);
        } else {
            finish();
        }
    }

    private void finishLogin(Intent intent) {
        Log.d(DEBUG_TAG, "finishLogin");

        String newAccountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        final Account newAccount = new Account(newAccountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String serveruri = intent.getStringExtra(ARG_SERVER_URI);
        String email = intent.getStringExtra(ARG_EMAIL);
        String name = intent.getStringExtra(ARG_NAME);
        String sessionKey = intent.getStringExtra(ARG_AUTH_SESSION_KEY);
        boolean shib = intent.getBooleanExtra(ARG_SHIB, false);

        int cameraIsSyncable = 0;
        boolean cameraSyncAutomatically = true;

        if (intent.getBooleanExtra(ARG_IS_EDITING, false)) {

            String oldAccountName = intent.getStringExtra(ARG_EDIT_OLD_ACCOUNT_NAME);
            final Account oldAccount = new Account(oldAccountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

            // serverUri and mail stay the same. so just update the token and exit
            if (oldAccount.equals(newAccount)) {

                mAccountManager.setAuthToken(newAccount, Authenticator.AUTHTOKEN_TYPE, authtoken);
                mAccountManager.setUserData(newAccount, Authenticator.SESSION_KEY, sessionKey);
                mAccountManager.setUserData(newAccount, Authenticator.KEY_NAME, name);

                Bundle result = new Bundle();
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                result.putString(AccountManager.KEY_ACCOUNT_NAME, newAccountName);
                setAccountAuthenticatorResult(result);
                setResult(RESULT_OK, intent);
                finish();
                return;
            }

            Log.d(DEBUG_TAG, "removing old account " + oldAccountName);

            cameraIsSyncable = ContentResolver.getIsSyncable(oldAccount, CameraUploadManager.AUTHORITY);
            cameraSyncAutomatically = ContentResolver.getSyncAutomatically(oldAccount, CameraUploadManager.AUTHORITY);

            mAccountManager.removeAccount(oldAccount, null, null);
        }

        Log.d(DEBUG_TAG, "adding new account "+newAccountName);
        mAccountManager.addAccountExplicitly(newAccount, null, null);

        mAccountManager.setAuthToken(newAccount, Authenticator.AUTHTOKEN_TYPE, authtoken);
        mAccountManager.setUserData(newAccount, Authenticator.KEY_SERVER_URI, serveruri);
        mAccountManager.setUserData(newAccount, Authenticator.KEY_EMAIL, email);
        mAccountManager.setUserData(newAccount, Authenticator.KEY_NAME, name);
        mAccountManager.setUserData(newAccount, Authenticator.SESSION_KEY, sessionKey);
        if (shib) {
            mAccountManager.setUserData(newAccount, Authenticator.KEY_SHIB, "shib");
        }

        // set sync settings
        ContentResolver.setIsSyncable(newAccount, CameraUploadManager.AUTHORITY, cameraIsSyncable);
        ContentResolver.setSyncAutomatically(newAccount, CameraUploadManager.AUTHORITY, cameraSyncAutomatically);

        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        result.putString(AccountManager.KEY_ACCOUNT_NAME, newAccountName);
        setAccountAuthenticatorResult(result);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void launchAccountDetailActivity() {
        Intent intent = new Intent(SeafileAuthenticatorActivity.this, AccountDetailActivity.class);
        intent.putExtra(SeafileAuthenticatorActivity.ARG_SERVER_URI, getString(R.string.server_atenea_cloud));
        intent.putExtras(getIntent());
        startActivityForResult(intent, SeafileAuthenticatorActivity.REQ_SIGNUP);
    }
}
