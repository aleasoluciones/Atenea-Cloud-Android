package com.ateneacloud.drive.sync;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.sync.observers.SeafSyncObserverFactory;
import com.ateneacloud.drive.sync.observers.SeafSyncObserverProtocol;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.ateneacloud.drive.sync.uploaders.SeafSyncUploaderFactory;
import com.ateneacloud.drive.sync.uploaders.SeafUploaderProtocol;
import com.ateneacloud.drive.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class responsible for managing synchronization settings and synchronization activities.
 */
public class SeafSyncronizer {

    public List<SeafSyncSettings> settings;

    private List<Account> accounts;

    private static List<SeafSyncObserverProtocol> observers;

    private SeafSyncSettingsService settingsService;


    /**
     * Constructs a new SeafSyncronizer
     */
    public SeafSyncronizer() {
        this.settingsService = new SeafSyncSettingsService();
        this.observers = new ArrayList<>();
        loadSettings();
        loadAccounts();
        //initializeObservers();
    }

    /**
     * Loads the active synchronization settings from the SeafSyncSettingsService.
     */
    private void loadSettings() {
        settings = settingsService.activeSettings();
    }

    private void loadAccounts() {
        AccountManager manager = new AccountManager(SeadroidApplication.getAppContext());
        accounts = manager.getSignedInAccountList();
    }

    /**
     * Initializes synchronization observers for each active synchronization setting.
     * Observers are created based on the settings using the SeafSyncObserverFactory.
     * Additionally, synchronization is started for each setting.
     */
    private void initializeObservers() {
        for (SeafSyncSettings setting : settings) {
            SeafSyncObserverProtocol observer = SeafSyncObserverFactory.createFor(setting);
            observer.start(() -> startSyncForSetting(setting));
            observers.add(observer);
        }
    }

    /**
     * Reloads synchronization observers and starts monitoring changes in synchronization settings.
     */
    public void reloadObservers() {
        stopObservers();
        observers.clear();
        initializeObservers();
    }

    /**
     * Stops all active synchronization observers.
     */
    public void stopObservers() {
        for (SeafSyncObserverProtocol observer : observers) {
            observer.stop();
        }
    }

    /**
     * Initiates synchronization for all active synchronization settings.
     */
    public void sync() {
        if (!NetworkUtils.isNotConnected()) {
            for (SeafSyncSettings setting : settings) {
                startSyncForSetting(setting);
            }
        }
    }

    /**
     * Handles changes in the network status for synchronization and updates observers accordingly.
     */
    public void startSync() {
        sync();
        //reloadObservers();
    }

    /**
     * Initiates synchronization for a specific synchronization setting.
     *
     * @param setting The SeafSyncSettings to be synchronized.
     */
    public void startSyncForSetting(SeafSyncSettings setting) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            for (Account account : accounts) {
                if (account.getEmail().toString().equals(setting.getAccountId())) {
                    SeafConnection connection = new SeafConnection(account);
                    SeafUploaderProtocol uploader = SeafSyncUploaderFactory.getUploaderFor(connection, setting);
                    if(uploader != null){
                        uploader.upload();
                    }
                    break;
                }
            }

        });
    }

    /**
     * Initiates synchronization for all active synchronization settings.
     */
    public void stopSync() {
        stopObservers();
        observers.clear();
    }

}
