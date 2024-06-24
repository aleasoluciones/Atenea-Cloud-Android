package com.ateneacloud.drive.sync.tree.treeFolderState;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.SeafSyncTreeProtocol;
import com.ateneacloud.drive.sync.tree.SeafSyncTreeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Entity
public class SeafSyncTreeState implements SeafSyncTreeProtocol {

    @Ignore
    private List<SeafSyncTreeProtocol> childrens;
    private Enum<SeafSyncTreeType> type;
    private String syncSettingId;
    @PrimaryKey
    @NonNull
    private String identifier;
    private String relativeHash;
    private String fullHash;
    private Uri uri;

    public SeafSyncTreeState() {
        childrens = new ArrayList<>();
    }

    @Override
    public String getId() {
        return identifier;
    }

    @Override
    public void setId(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getSyncSettingId() {
        return syncSettingId;
    }

    @Override
    public void setSyncSettingId(String syncSettingId) {
        this.syncSettingId = syncSettingId;
    }

    @Override
    public Enum<SeafSyncTreeType> getType() {
        return type;
    }

    @Override
    public void setType(Enum<SeafSyncTreeType> type) {
        this.type = type;
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void setUri(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void addChildren(SeafSyncTreeProtocol children) {
        childrens.add(children);
    }

    @Override
    public List<SeafSyncTreeProtocol> getChildrens() {
        return childrens;
    }

    @Override
    public void clearChildrens() {
        childrens.clear();
    }

    @Override
    public List<SeafSyncTreeProtocol> getAllChildrensOfType(Enum<SeafSyncTreeType> treeType) {
        List<SeafSyncTreeProtocol> childrensOfType = new ArrayList<>();

        for (SeafSyncTreeProtocol child : childrens) {
            if (child.getType() == treeType) {
                childrensOfType.add(child);
            }
        }

        return childrensOfType;
    }

    @Override
    public List<SeafSyncTreeProtocol> getChildrensOfType(Enum<SeafSyncTreeType> treeType) {
        return getChildrens()
                .stream()
                .filter(tree -> ((SeafSyncTree) tree).getType() == treeType)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeafSyncTreeProtocol> getChildrensFrom(SeafSyncTreeProtocol parent, Predicate predicate) {
        Predicate<SeafSyncTreeProtocol> filtroPorTipo = tree -> tree == parent;

        for (SeafSyncTreeProtocol tree : childrens) {
            if (filtroPorTipo.test(tree)) {
                return tree.getChildrens();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public String getFullHash() {
        return fullHash;
    }

    @Override
    public void setFullHash(String hash) {
        this.fullHash = hash;
    }

    @Override
    public String getRelativeHash() {
        return relativeHash;
    }

    @Override
    public void setRelativeHash(String hash) {
        this.relativeHash = hash;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setChildrens(List<SeafSyncTreeProtocol> childrens) {
        this.childrens = childrens;
    }

}
