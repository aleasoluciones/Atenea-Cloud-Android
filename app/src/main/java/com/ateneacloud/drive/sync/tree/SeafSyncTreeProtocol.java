package com.ateneacloud.drive.sync.tree;

import android.net.Uri;

import java.util.List;
import java.util.function.Predicate;

public interface SeafSyncTreeProtocol {

    String getId();
    void setId(String identifier);

    String getSyncSettingId();
    void setSyncSettingId(String syncSettingId);

    Enum<SeafSyncTreeType> getType();
    void setType(Enum<SeafSyncTreeType> type);

    Uri getUri();
    void setUri(Uri uri);

    void addChildren(SeafSyncTreeProtocol children);

    List<SeafSyncTreeProtocol> getChildrens();

    void clearChildrens();

    List<SeafSyncTreeProtocol> getAllChildrensOfType(Enum<SeafSyncTreeType> treeType);

    List<SeafSyncTreeProtocol> getChildrensOfType(Enum<SeafSyncTreeType> treeType);

    List<SeafSyncTreeProtocol> getChildrensFrom(SeafSyncTreeProtocol parent, Predicate predicate);

    String getFullHash();
    void setFullHash(String hash);

    String getRelativeHash();
    void setRelativeHash(String hash);
}
