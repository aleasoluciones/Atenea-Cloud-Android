package com.ateneacloud.drive.ui.adapter;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.config.GlideLoadConfig;
import com.ateneacloud.drive.data.SeafEvent;
import com.ateneacloud.drive.data.SeafItem;
import com.ateneacloud.drive.ui.activity.BrowserActivity;
import com.ateneacloud.drive.ui.widget.CircleImageView;
import com.ateneacloud.drive.util.GlideApp;
import com.ateneacloud.drive.util.SystemSwitchUtils;
import com.ateneacloud.drive.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for Activities tab
 */
public class ActivitiesItemAdapter extends BaseAdapter {
    public static final String DEBUG_TAG = ActivitiesItemAdapter.class.getSimpleName();

    public static final int REFRESH_ON_NONE = 0;
    public static final int REFRESH_ON_PULL_DOWN = 1;
    public static final int REFRESH_ON_PULL_UP = 2;
    private int state = REFRESH_ON_NONE;

    private ArrayList<SeafEvent> items;
    private BrowserActivity mActivity;

    private boolean useNewActivity;

    public ActivitiesItemAdapter(BrowserActivity activity) {
        this.mActivity = activity;
        items = Lists.newArrayList();
    }

    @Override
    public int getCount() {
        return items.size() + 1;
    }

    public void clear() {
        items.clear();
    }

    public void add(SeafEvent entry) {
        items.add(entry);
    }

    public void notifyChanged() {
        notifyDataSetChanged();
    }

    @Override
    public SeafItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setItems(List<SeafEvent> events, boolean useNewActivity) {
        this.useNewActivity = useNewActivity;
        items.clear();
        items.addAll(events);
    }

    private LinearLayout mFooterView;

    public void setFooterViewLoading(boolean more) {
        ProgressBar progress = (ProgressBar) mFooterView.findViewById(R.id.progressbar);
        TextView text = (TextView) mFooterView.findViewById(R.id.text);
        if (more) {
            mFooterView.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
            mFooterView.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
        }
    }

    public void setState(int state) {
        this.state = state;
    }

    public View getFooterView() {
        return this.mFooterView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == getCount() - 1) {
            this.mFooterView = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.footer_load_more, null);
            switch (state) {
                case REFRESH_ON_NONE:
                case REFRESH_ON_PULL_DOWN:
                    setFooterViewLoading(false);
                    break;
                case REFRESH_ON_PULL_UP:
                    setFooterViewLoading(true);
                    break;
            }
            return mFooterView;
        }
        if (position < 0) {
            position = 0;
        }

        final SeafEvent item = items.get(position);
        View view = convertView;
        // TODO optimize by setting tags
        final ViewHolder viewHolder;

        view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_activities, null);
        RelativeLayout rl_old = view.findViewById(R.id.rl_activities_old);

        TextView title = view.findViewById(R.id.tv_activities_mod_desc);
        TextView nick = view.findViewById(R.id.tv_activities_nick);
        TextView date = view.findViewById(R.id.tv_activities_date);
        TextView repoName = view.findViewById(R.id.tv_activities_repo_name);
        CircleImageView icon = view.findViewById(R.id.iv_activities_avatar);

        View rl_new = view.findViewById(R.id.rl_activities_new);
        CircleImageView icon_url = view.findViewById(R.id.iv_activities_avatar_url);
        TextView tv_name = view.findViewById(R.id.tv_activities_name);
        TextView tv_state = view.findViewById(R.id.tv_activities_state);
        TextView tv_desc = view.findViewById(R.id.tv_activities_desc);
        TextView tv_time = view.findViewById(R.id.tv_activities_time);
        TextView tv_mod = view.findViewById(R.id.tv_activities_mod);
        viewHolder = new ViewHolder(title, nick, date, repoName, icon, tv_name, tv_state, tv_desc, tv_time, tv_mod, icon_url, rl_old, rl_new);
        view.setTag(viewHolder);
        if (useNewActivity) {
            rl_old.setVisibility(View.GONE);
            rl_new.setVisibility(View.VISIBLE);
            item.setAvatar(item.getAvatar_url());
            viewHolder.tv_name.setText(item.getAuthor_name());
            viewHolder.tv_time.setText(SystemSwitchUtils.parseDateTime(item.getV_time()));
            viewHolder.tv_mod.setText(item.getRepo_name());
            viewHolder.tv_desc.setText(item.getPath());
            viewHolder.tv_state.setText(SystemSwitchUtils.obj_type(mActivity, item.getObj_type(), item.getOp_type()));
            //
            GlideApp.with(viewHolder.icon_url)
                    .load(GlideLoadConfig.getGlideUrl(item.getAvatar_url()))
                    .apply(GlideLoadConfig.getDefaultAvatarOptions())
                    .into(viewHolder.icon_url);

        } else {
            rl_old.setVisibility(View.VISIBLE);
            rl_new.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(item.getAvatar())) {
            final String avatar = parseAvatar(item.getAvatar());

            GlideApp.with(viewHolder.icon)
                    .load(GlideLoadConfig.getGlideUrl(avatar))
                    .apply(GlideLoadConfig.getDefaultAvatarOptions())
                    .into(viewHolder.icon);
        }

        viewHolder.title.setText(item.getDesc());
        viewHolder.nick.setText(item.getNick());

        if (!TextUtils.isEmpty(item.getTime_relative())) {
            final String relative = parseRelativeTime(item.getTime_relative());
            viewHolder.date.setText(relative);
            viewHolder.date.setVisibility(View.VISIBLE);
        } else {
            viewHolder.date.setVisibility(View.GONE);
        }
        viewHolder.repoName.setText(item.getRepo_name());
        return view;
    }

    private String parseAvatar(@NonNull String avatar) {
        // <img src="/seahub/image-view/avatars/7/9/dc411b7a64a20963ccff32563e38d6/resized/36/bamboo_5.png" width="36" height="36" class="avatar" />
        String re1 = ".*?";   // Non-greedy match on filler
        String re2 = "(src)"; // Variable Name 1
        String re3 = ".*?";   // Non-greedy match on filler
        String re4 = "((?:\\/[\\w\\.\\-]+)+)";    // Unix Path 1

        Pattern p = Pattern.compile(re1 + re2 + re3 + re4, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(avatar);
        if (m.find()) {
            String avatarPath = m.group(2);
            return Utils.pathJoin(mActivity.getAccount().getServer(), avatarPath);
        } else return avatar;
    }

    private String parseRelativeTime(@NonNull String relativeTime) {
        String regex = "(<[^>]+>)";
        final String[] split = relativeTime.split(regex);
        if (split.length > 1) {
            return split[1];
        } else return relativeTime;
    }

    private static class ViewHolder {
        TextView title, nick, date, repoName;
        ImageView icon, icon_url;
        TextView tv_name, tv_state, tv_desc, tv_mod, tv_time;
        View rl_old, rl_new;

        public ViewHolder(TextView title, TextView nick, TextView date, TextView repoName, ImageView icon, TextView tv_name,
                          TextView tv_state, TextView tv_desc, TextView tv_time, TextView tv_mod, ImageView icon_url, RelativeLayout rl_old, View rl_new) {
            super();
            this.icon = icon;
            this.title = title;
            this.nick = nick;
            this.date = date;
            this.repoName = repoName;
            this.icon_url = icon_url;
            this.tv_name = tv_name;
            this.tv_state = tv_state;
            this.tv_desc = tv_desc;
            this.tv_time = tv_time;
            this.tv_mod = tv_mod;
            this.rl_old = rl_old;
            this.rl_new = rl_new;
        }
    }

}
