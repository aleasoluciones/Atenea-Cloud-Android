package com.ateneacloud.drive.gallery;

import android.app.Activity;

public class NoSearchActivity extends Activity {
    @Override
    public boolean onSearchRequested() {
        return false;
    }
}
