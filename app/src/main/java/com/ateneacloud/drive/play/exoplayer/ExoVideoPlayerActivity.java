package com.ateneacloud.drive.play.exoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountPlans;
import com.ateneacloud.drive.play.VideoLinkStateListener;
import com.ateneacloud.drive.play.VideoLinkTask;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.dialog.ChangePlanDialog;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.util.Utils;


public class ExoVideoPlayerActivity extends BaseActivity implements VideoLinkStateListener {
    private ExoPlayer player;
    private PlayerView playerView;

    private ConstraintLayout playerCurtain;

    private ProgressBar playerCurtainBar;

    private Account mAccount;
    private AccountInfo mAccountInfo;
    private String fileName;
    private String mRepoID;
    private String mFilePath;
    private String mFileLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.hideSystemNavigationBar(this);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        mAccount = intent.getParcelableExtra("account");
        fileName = intent.getStringExtra("fileName");
        mRepoID = intent.getStringExtra("repoID");
        mFilePath = intent.getStringExtra("filePath");
        mAccountInfo = intent.getParcelableExtra("accountInfo");
        if (mAccountInfo == null) {
            Toast.makeText(ExoVideoPlayerActivity.this, R.string.error_not_account_info, Toast.LENGTH_SHORT);
            finish();
        }
        VideoLinkTask task = new VideoLinkTask(mAccount, mRepoID, mFilePath, this);
        ConcurrentAsyncTask.execute(task);
    }

    @Override
    public void onSuccess(String fileLink) {
        mFileLink = fileLink;
        init();
    }

    private void init() {
        playerView = findViewById(R.id.player_view);
        playerCurtain = findViewById(R.id.player_curtain);
        playerCurtainBar = findViewById(R.id.player_curtain_progress_bar);

        if (player != null) {
            return;
        }


        player = new ExoPlayer.Builder(this).build();

        playerView.setPlayer(player);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);

        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        player.setPlayWhenReady(true);
        player.setMediaSource(getMediaSource(mFileLink));
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    playerCurtainBar.setVisibility(View.GONE);
                    if (isVideo4K(player) && mAccountInfo.getPlan() != AccountPlans.Platinum) {
                        player.stop();
                        ChangePlanDialog
                                .build(ExoVideoPlayerActivity.this, ChangePlanDialog.NOT_SUPPORT_4K)
                                .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                    @Override
                                    public void changePlanDialogResponseYes() {
                                        Utils.openWebPlans(ExoVideoPlayerActivity.this);
                                        finish();
                                    }

                                    @Override
                                    public void changePlanDialogResponseNo() {
                                        finish();
                                    }

                                    @Override
                                    public void changePlanDialogResponseNeither() {
                                        finish();
                                    }
                                }).show();
                    } else {
                        playerCurtain.setVisibility(View.GONE);
                    }
                }
            }
        });
        player.prepare();

    }

    @Override
    public void onError(String errMsg) {
        ToastUtils.showLong(errMsg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
        }

        super.onDestroy();
    }

    private MediaSource getMediaSource(String url) {
        String userAgent = Util.getUserAgent(this, AppUtils.getAppName());
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
                .setAllowCrossProtocolRedirects(true);
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this,
                httpDataSourceFactory
        );

        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url));
    }

    private boolean isVideo4K(ExoPlayer player) {
        if (player != null) {
            int videoWidth = player.getVideoFormat().width;
            int videoHeight = player.getVideoFormat().height;
            return videoWidth >= 3840 && videoHeight >= 2160;
        }
        return false;
    }
}
