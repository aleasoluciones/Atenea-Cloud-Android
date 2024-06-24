package com.ateneacloud.drive.sync.observers.impl;

import com.ateneacloud.drive.sync.observers.SeafSyncObserverProtocol;
import com.ateneacloud.drive.sync.observers.callbacks.SeafSyncObserverProtocolCallback;

public class SeafSyncBlindObserver implements SeafSyncObserverProtocol {

    @Override
    public String getObserverIdentifier() {
        return "blind_observer";
    }

    @Override
    public void start(SeafSyncObserverProtocolCallback callback) {
    }

    @Override
    public void stop() {
    }
}
