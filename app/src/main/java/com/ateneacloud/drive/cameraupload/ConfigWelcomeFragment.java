package com.ateneacloud.drive.cameraupload;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ateneacloud.drive.R;

/**
 * Welcome fragment for camera upload configuration helper
 */
public class ConfigWelcomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = getActivity().getLayoutInflater().inflate(R.layout.cuc_welcome_fragment, null);

        return rootView;
    }

}

