package com.ateneacloud.drive.play;

/**
 * get video link state listener
 */
public interface VideoLinkStateListener {
    void onSuccess(String fileLink);

    void onError(String errMsg);
}
