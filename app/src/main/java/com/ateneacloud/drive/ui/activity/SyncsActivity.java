package com.ateneacloud.drive.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MenuItem;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.ui.fragment.SyncManagementFragment;
import com.ateneacloud.drive.ui.fragment.SyncsFragment;

public class SyncsActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private Toolbar mActionBarToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syncs);

        loadFragment(savedInstanceState);

        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        changeTitle(R.string.synchronizations);

    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                // Depending on which version of Android you are on the Toolbar or the ActionBar may be
                // active so the a11y description is set here.
                mActionBarToolbar.setNavigationContentDescription(getResources().getString(R.string
                        .navdrawer_description_a11y));
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    private void loadFragment(Bundle saveInstance) {
        if (saveInstance != null) {
            String currentFragmentTag = saveInstance.getString("currentFragmentTag");
            if (currentFragmentTag != null) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                if (currentFragment != null) {
                    lauchFragment(currentFragment);
                }
            }
        } else {
            lauchFragment(new SyncsFragment());
        }
    }

    private void lauchFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.syncs_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.syncs_fragment_container);
                if (currentFragment instanceof SyncManagementFragment) {
                    lauchFragment(new SyncsFragment());
                } else {
                    this.finish();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.syncs_fragment_container);


        if (currentFragment != null) {
            outState.putString("currentFragmentTag", currentFragment.getTag());
        }
    }

    public void changeTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    public void changeTitle(int title) {
        getSupportActionBar().setTitle(title);
    }
}