package com.ateneacloud.drive.sync.tree;

import android.net.Uri;

import com.ateneacloud.drive.sync.SeafSyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class SeafSyncTree {

    private String identifier;
    private Enum<SeafSyncTreeType> type;
    private String syncSettingId;
    private String fullHash;
    private String relativeHash;
    private List<SeafSyncTree> childrens;

    private Uri uri;

    public SeafSyncTree() {
        identifier = UUID.randomUUID().toString();
        type = SeafSyncTreeType.Root;
        childrens = new ArrayList<>();
    }

    public String getId() {
        return identifier;
    }

    public void setId(String identifier) {
        this.identifier = identifier;
    }

    public String getSyncSettingId() {
        return syncSettingId;
    }

    public void setSyncSettingId(String syncSettingId) {
        this.syncSettingId = syncSettingId;
    }

    public void setType(Enum<SeafSyncTreeType> type) {
        this.type = type;
    }

    public Enum<SeafSyncTreeType> getType() {
        return type;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getFullHash() {
        if (identifier == null) {
            identifier = "";
        }

        StringBuilder hash = new StringBuilder(identifier);

        for (SeafSyncTree child : childrens) {
            hash.append(".").append(child.getFullHash());
        }

        return SeafSyncUtils.calculateHash(hash.toString());
    }

    public void setFullHash(String fullHash) {
        this.fullHash = fullHash;
    }

    public String getRelativeHash() {
        if (identifier == null) {
            identifier = "";
        }

        StringBuilder hash = new StringBuilder(identifier);

        for (SeafSyncTree child : childrens) {
            hash.append(".").append(child.identifier);
        }

        return SeafSyncUtils.calculateHash(hash.toString());
    }

    public void setRelativeHash(String relativeHash) {
        this.relativeHash = relativeHash;
    }

    public void addChildren(SeafSyncTree children) {
        childrens.add(children);
    }

    public void clearChildrens() {
        childrens.clear();
    }

    public List<SeafSyncTree> getChildrens() {
        return childrens;
    }

    public List<SeafSyncTree> getAllChildrensOfType(Enum<SeafSyncTreeType> fileType) {
        List<SeafSyncTree> filteredChildren = new ArrayList<>();
        for (SeafSyncTree tree : childrens) {
            if (tree.getType() == fileType) {
                filteredChildren.add(tree);
            }
            filteredChildren.addAll(tree.getAllChildrensOfType(fileType));
        }
        return filteredChildren;
    }

    public List<SeafSyncTree> getChildrensOfType(Enum<SeafSyncTreeType> fileType) {
        List<SeafSyncTree> filteredChildren = new ArrayList<>();
        for (SeafSyncTree tree : childrens) {
            if (tree.getType() == fileType) {
                filteredChildren.add(tree);
            }
        }
        return filteredChildren;
    }

    public List<SeafSyncTree> getChildrensFrom(SeafSyncTree parent, Predicate<SeafSyncTree> predicate) {
        List<SeafSyncTree> filteredChildren = new ArrayList<>();
        for (SeafSyncTree tree : parent.childrens) {
            if (predicate.test(tree)) {
                filteredChildren.add(tree);
            }
            filteredChildren.addAll(getChildrensFrom(tree, predicate));
        }
        return filteredChildren;
    }

    public SeafSyncTree copy() {
        SeafSyncTree tree = new SeafSyncTree();
        tree.setUri(this.uri);
        tree.setId(this.identifier);
        tree.setSyncSettingId(this.syncSettingId);
        tree.setRelativeHash(this.relativeHash);
        tree.setFullHash(this.fullHash);
        tree.setType(this.type);
        tree.childrens = new ArrayList<>(this.childrens);
        return tree;
    }
}

