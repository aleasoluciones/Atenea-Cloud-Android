package com.ateneacloud.drive.sync.tree.services;

import android.net.Uri;

import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.SeafSyncTreeType;
import com.ateneacloud.drive.sync.tree.treeChangeDetector.SeafSyncTreeChangeDetector;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import com.ateneacloud.drive.sync.tree.treeFolderState.services.SeafSyncFolderStateService;

import java.util.List;

public class SeafSyncTreeService {

    private SeafSyncFolderStateService stateService;
    private SeafSyncTreeChangeDetector changeDetector;


    public SeafSyncTreeService() {
        this.stateService = new SeafSyncFolderStateService();
        this.changeDetector = new SeafSyncTreeChangeDetector();
    }

    public SeafSyncTreeService(SeafSyncFolderStateService stateService, SeafSyncTreeChangeDetector changeDetector) {
        this.stateService = stateService;
        this.changeDetector = changeDetector;
    }

    public void saveTreeRecursively(SeafSyncTree tree) {
        saveTree(tree);

        List<SeafSyncTree> children = tree.getChildrens();
        for (SeafSyncTree childTree : children) {
            saveTree(childTree);
        }
    }

    public void saveTree(SeafSyncTree tree) {
        if (tree.getType() != SeafSyncTreeType.Folder && tree.getType() != SeafSyncTreeType.Root) {
            return;
        }

        SeafSyncTreeState state = new SeafSyncTreeState();
        state.setId(tree.getId());
        state.setRelativeHash(tree.getRelativeHash());
        state.setFullHash(tree.getFullHash());
        state.setUri(tree.getUri());
        state.setSyncSettingId(tree.getSyncSettingId());
        state.setType(tree.getType());

        stateService.insert(state);
    }

    public SeafSyncTree changesSinceLastSyncFor(SeafSyncTree tree) {
        return changeDetector.changes(tree);
    }

    public SeafSyncTree buildFrom(Uri uri, SeafSyncSettings settings) {
        SeafSyncTreeBuilder treeBuilder = new SeafSyncTreeBuilder(uri, settings);
        return treeBuilder.build();
    }
}
