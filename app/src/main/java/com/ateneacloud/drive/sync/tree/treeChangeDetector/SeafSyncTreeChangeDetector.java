package com.ateneacloud.drive.sync.tree.treeChangeDetector;

import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import com.ateneacloud.drive.sync.tree.treeFolderState.repository.coreData.SeafSyncTreeFolderStateCoreDataRepository;
import com.ateneacloud.drive.sync.tree.treeFolderState.services.SeafSyncFolderStateService;

import java.util.List;

public class SeafSyncTreeChangeDetector {

    private SeafSyncFolderStateService stateService;

    public SeafSyncTreeChangeDetector() {
        stateService = new SeafSyncFolderStateService(new SeafSyncTreeFolderStateCoreDataRepository());
    }

    public SeafSyncTree changes(SeafSyncTree tree) {
        List<SeafSyncTreeState> storedStates = stateService.findBySyncSetting(tree.getSyncSettingId());
        return detectChanges(tree, storedStates);
    }

    private SeafSyncTree detectChanges(SeafSyncTree tree, List<SeafSyncTreeState> storedStates) {
        SeafSyncTree treeWithChangedFolderOnly = new SeafSyncTree();

        for (SeafSyncTree children : tree.getChildrens()) {
            SeafSyncTree changedChildren = detectChanges(children, storedStates);
            if (changedChildren != null) {
                treeWithChangedFolderOnly.addChildren(changedChildren);
            }
        }

        SeafSyncTreeState matchingState = findMatching(tree, storedStates);

        if (matchingState == null) {
            return tree;
        }

        if (matchingState.getFullHash().equals(tree.getFullHash())) {
            return null;
        }

        return treeWithChangedFolderOnly;
    }

    private SeafSyncTreeState findMatching(SeafSyncTree treeNode, List<SeafSyncTreeState> storedStates) {
        for (SeafSyncTreeState state : storedStates) {
            if (state.getSyncSettingId().equals(treeNode.getSyncSettingId()) &&
                    state.getUri().equals(treeNode.getUri())) {
                return state;
            }
        }
        return null;
    }
}
