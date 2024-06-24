package com.ateneacloud.drive.sync.tree.services;

import android.net.Uri;

import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.sync.fileProvider.SeafSyncFileProviderFactory;
import com.ateneacloud.drive.sync.enums.SeafSyncItemType;
import com.ateneacloud.drive.sync.fileProvider.SeafSyncProviderProtocol;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.SeafSyncTreeType;

import java.util.List;

public class SeafSyncTreeBuilder {

    private SeafSyncTree root;
    private Uri sourceURI;
    private SeafSyncSettings settings;

    public SeafSyncTreeBuilder(Uri sourceURI, SeafSyncSettings settings) {
        this.root = new SeafSyncTree();
        this.sourceURI = sourceURI;
        this.settings = settings;
    }

    public SeafSyncTree build() {
        root = new SeafSyncTree();
        root.setUri(sourceURI);
        root.setSyncSettingId(settings.getId());
        readContentsRecursivelyFrom(sourceURI, root);
        return root;
    }

    private SeafSyncTree readContentsRecursivelyFrom(Uri folderUri, SeafSyncTree parent) {
        SeafSyncProviderProtocol fileProvider =  SeafSyncFileProviderFactory.getProviderForURI(folderUri, settings);
        List<SeafSyncFileItem> items = fileProvider.getFiles();

        for (SeafSyncFileItem syncItem : items) {
            if (syncItem.getItemType() == SeafSyncItemType.Folder) {
                parent.addChildren(readContentsRecursivelyFrom(Uri.fromFile(syncItem.getPath()), createTreeNodeWith(syncItem, SeafSyncTreeType.Folder)));
            } else {
                parent.addChildren(createTreeNodeWith(syncItem, SeafSyncTreeType.File));
            }
        }

        return parent;
    }

    private SeafSyncTree createTreeNodeWith(SeafSyncFileItem syncItem, SeafSyncTreeType type) {
        SeafSyncTree node = new SeafSyncTree();
        node.setUri(Uri.fromFile(syncItem.getPath()));
        node.setId(syncItem.getIdentifier());
        node.setType(type);
        node.setSyncSettingId(settings.getId());
        return node;
    }
}
