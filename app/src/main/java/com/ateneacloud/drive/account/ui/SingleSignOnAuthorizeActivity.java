package com.ateneacloud.drive.account.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.ssl.CertsManager;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.dialog.SslConfirmDialog;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.util.DeviceIdManager;
import com.ateneacloud.drive.util.Utils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

/**
 * Single sign on Authorize page
 * use cookie to get authorized data
 * <p/>
 */
public class SingleSignOnAuthorizeActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {
    public static final String DEBUG_TAG = SingleSignOnAuthorizeActivity.class.getSimpleName();

    public static final String SEAHUB_SHIB_COOKIE_NAME = "seahub_auth";
    private WebView mWebview;
    private LinearLayout mloadingAnimation;
    public String serverUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_sign_on_authorize_layout);
        mWebview = (WebView) findViewById(R.id.single_sign_on_authorize_wv);
        mloadingAnimation = (LinearLayout) findViewById(R.id.single_sign_on_loading_ll);

        mWebview.getSettings().setLoadsImagesAutomatically(true);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CustomWebviewClient client = new CustomWebviewClient();
        mWebview.setWebViewClient(client);

        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.shib_login_title);

        String url = getIntent().getStringExtra(SingleSignOnActivity.SINGLE_SIGN_ON_SERVER_URL);
        CookieManager.getInstance().removeAllCookie();
        openAuthorizePage(url);
    }

    private void openAuthorizePage(String url) {
        Log.d(DEBUG_TAG, "server url is " + url);

        serverUrl = url;

        if (!Utils.isNetworkOn()) {
            showShortToast(this, getString(R.string.network_down));
            return;
        }

        String appVersion = "";
        Context context = SeadroidApplication.getAppContext();
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        if (!url.endsWith("/")) {
            url += "/shib-login";
        } else
            url += "shib-login";

        //local device id
        String deviceId = DeviceIdManager.getInstance().getOrSet();

        try {
            url += String.format("?shib_platform_version=%s&shib_device_name=%s&shib_platform=%s&shib_device_id=%s&shib_client_version=%s",
                    URLEncoder.encode(Build.VERSION.RELEASE, "UTF-8"),
                    URLEncoder.encode(Build.MODEL, "UTF-8"),
                    "android",
                    deviceId,
                    appVersion);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.d(DEBUG_TAG, "url " + url);

        mWebview.loadUrl(url);

        showPageLoading(true);
    }


    private void showPageLoading(boolean pageLoading) {

        if (!pageLoading) {
            mloadingAnimation.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_out));
            mWebview.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in));
            mloadingAnimation.setVisibility(View.GONE);
            mWebview.setVisibility(View.VISIBLE);
        } else {
            mloadingAnimation.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in));
            mWebview.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_out));

            mloadingAnimation.setVisibility(View.VISIBLE);
            mWebview.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return true;
    }

    private void displaySSLError() {
        showPageLoading(false);
        showShortToast(this, R.string.ssl_error);
    }

    class CustomWebviewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // Display error messages
            showShortToast(SingleSignOnAuthorizeActivity.this,
                    String.format((R.string.shib_load_page_error) + description));

            showPageLoading(false);
        }


        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            Log.d(DEBUG_TAG, "onReceivedSslError " + error.getCertificate().toString());
            final Account account = new Account(serverUrl, null, null, null, false);
            SslCertificate sslCert = error.getCertificate();
            X509Certificate savedCert = CertsManager.instance().getCertificate(account);

            if (Utils.isSameCert(sslCert, savedCert)) {
                Log.d(DEBUG_TAG, "trust this cert");
                handler.proceed();
            } else {
                Log.d(DEBUG_TAG, "cert is not trusted");
                SslConfirmDialog dialog = new SslConfirmDialog(account,
                        Utils.getX509CertFromSslCertHack(sslCert),
                        new SslConfirmDialog.Listener() {
                            @Override
                            public void onAccepted(boolean rememberChoice) {
                                CertsManager.instance().saveCertForAccount(account, rememberChoice);
                                // Ignore SSL certificate validate
                                handler.proceed();
                            }

                            @Override
                            public void onRejected() {
                                displaySSLError();
                                handler.cancel();
                            }
                        });
                dialog.show(getSupportFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
            }
        }

        @Override
        public void onPageFinished(WebView webView, String url) {
            Log.d(DEBUG_TAG, "onPageFinished " + serverUrl);
            showPageLoading(false);

            String cookie = getCookie(serverUrl, SEAHUB_SHIB_COOKIE_NAME);
            if (cookie == null)
                return;

            Account account = null;
            try {
                account = parseAccount(Utils.cleanServerURL(serverUrl), cookie);
                if (account == null) {
                    return;
                }
                ConcurrentAsyncTask.execute(new SingleSignOnAuthorizeActivity.AccountInfoTask(account, account.getToken()));
            } catch (MalformedURLException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
        }
    }

    /**
     * The cookie value is like seahub_shib="foo@test.com@bd8cc1138", where
     * foo@test.com is username and bd8cc1138 is api token"
     */

    private Account parseAccount(String url, String cookie) {
        if (url == null || cookie.isEmpty())
            return null;

        if (cookie.startsWith("\"")) {
            cookie = cookie.substring(1, cookie.length() - 1);
        }

        String email = cookie.substring(0, cookie.lastIndexOf("@"));
        String token = cookie.substring(cookie.lastIndexOf("@") + 1);

        if (email.isEmpty() || token.isEmpty())
            return null;

        Log.d(DEBUG_TAG, "email: " + email);
        Log.d(DEBUG_TAG, "token: " + token);

        return new Account(url, email, "", token, true);
    }

    private class AccountInfoTask extends AsyncTask<Void, Void, String> {
        Account loginAccount;
        String authToken;
        SeafException err = null;

        public AccountInfoTask(Account loginAccount, String authToken) {
            this.loginAccount = loginAccount;
            this.authToken = authToken;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            return getAccountInfo();
        }

        @Override
        protected void onPostExecute(final String result) {
            if (err == SeafException.sslException) {
                SslConfirmDialog dialog = new SslConfirmDialog(loginAccount,
                        new SslConfirmDialog.Listener() {
                            @Override
                            public void onAccepted(boolean rememberChoice) {
                                CertsManager.instance().saveCertForAccount(loginAccount, rememberChoice);
                                ConcurrentAsyncTask.execute(new SingleSignOnAuthorizeActivity.AccountInfoTask(loginAccount, authToken));
                            }

                            @Override
                            public void onRejected() {

                            }
                        });
                dialog.show(getSupportFragmentManager(), SslConfirmDialog.FRAGMENT_TAG);
            }

            if (result != null && result.equals("Success")) {
                returnAccount(loginAccount);
            }
        }

        private void returnAccount(Account account) {
            Intent retData = new Intent();
            retData.putExtras(getIntent());
            retData.putExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME, account.getSignature());
            retData.putExtra(android.accounts.AccountManager.KEY_AUTHTOKEN, account.getToken());
            retData.putExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, getIntent().getStringExtra(SeafileAuthenticatorActivity.ARG_ACCOUNT_TYPE));
            retData.putExtra(SeafileAuthenticatorActivity.ARG_EMAIL, account.getEmail());
            retData.putExtra(SeafileAuthenticatorActivity.ARG_NAME, account.getName());
            retData.putExtra(SeafileAuthenticatorActivity.ARG_SHIB, account.is_shib);
            retData.putExtra(SeafileAuthenticatorActivity.ARG_SERVER_URI, account.getServer());
            setResult(RESULT_OK, retData);
            finish();
        }

        private String getAccountInfo() {
            try {
                DataManager manager = new DataManager(loginAccount);
                AccountInfo accountInfo = manager.getAccountInfo();
                if (accountInfo == null)
                    return "Unknown error";
                loginAccount = new Account(accountInfo.getName(), loginAccount.server, accountInfo.getEmail(), loginAccount.token, loginAccount.is_shib, loginAccount.sessionKey);
                return "Success";

            } catch (SeafException e) {
                err = e;
                if (e == SeafException.sslException) {
                    return getString(R.string.ssl_error);
                } else {
                    showShortToast(SingleSignOnAuthorizeActivity.this, e.getMessage());
                    return e.getMessage();
                }
            } catch (JSONException e) {
                showShortToast(SingleSignOnAuthorizeActivity.this, e.getMessage());
                return e.getMessage();
            }
        }
    }


    public String getCookie(String url, String key) {
        String CookieValue = "";

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager cookieManager = CookieManager.getInstance();

        String cookies = cookieManager.getCookie(url);
        if (cookies == null)
            return null;

        Log.d(DEBUG_TAG, "All the cookies in a string:" + cookies);

        String[] allCookies = cookies.split(";");

        for (String cookie : allCookies) {
            if (cookie.contains(key)) {
                String[] pair = cookie.split("=");
                CookieValue = pair[1];
            }
        }
        return CookieValue;
    }

}
