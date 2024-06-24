package com.ateneacloud.drive.folderbackup.selectfolder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ateneacloud.drive.R;


public class TabbarFileViewHolder extends RecyclerView.ViewHolder {

    protected TextView tvName;
    protected LinearLayout llRoot;

    public TabbarFileViewHolder(View itemView) {
        super(itemView);
        llRoot = (LinearLayout) itemView.findViewById(R.id.ll_root);
        tvName = (TextView) itemView.findViewById(R.id.btn_item_tabbar);
    }
}
