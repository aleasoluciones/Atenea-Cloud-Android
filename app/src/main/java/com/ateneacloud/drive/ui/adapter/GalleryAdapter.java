package com.ateneacloud.drive.ui.adapter;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafPhoto;
import com.ateneacloud.drive.ui.activity.GalleryActivity;
import com.ateneacloud.drive.util.GlideApp;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.util.List;

/**
 * Gallery Adapter
 */
public class GalleryAdapter extends PagerAdapter {
    public static final String DEBUG_TAG = "GalleryAdapter";

    /**
     * unique task id
     */
    public static int taskID;

    private GalleryActivity mActivity;
    private List<SeafPhoto> seafPhotos;
    private LayoutInflater inflater;
    private Account mAccount;
    private DataManager dm;
    private DisplayMetrics displayMetrics;

    public GalleryAdapter(GalleryActivity context, Account account,
                          List<SeafPhoto> photos, DataManager dataManager) {
        mActivity = context;
        seafPhotos = photos;
        inflater = context.getLayoutInflater();
        mAccount = account;
        dm = dataManager;
        displayMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    @Override
    public int getCount() {
        return seafPhotos.size();
    }

    public void setItems(List<SeafPhoto> photos) {
        seafPhotos = photos;
    }

    public void downloadPhoto() {
        notifyDataSetChanged();
    }

    @Override
    public View instantiateItem(ViewGroup container, final int position) {
        View contentView = inflater.inflate(R.layout.gallery_view_item, container, false);
        final PhotoView photoView = (PhotoView) contentView.findViewById(R.id.gallery_photoview);
        final ProgressBar progressBar = (ProgressBar) contentView.findViewById(R.id.gallery_progress_bar);
        final String repoName = seafPhotos.get(position).getRepoName();
        final String repoID = seafPhotos.get(position).getRepoID();
        SeafPhoto seafPhoto = seafPhotos.get(position);
        final String filePath = Utils.pathJoin(seafPhoto.getDirPath(),
                seafPhoto.getName());
        final File file = dm.getLocalRepoFile(repoName, repoID, filePath);
        if (file.exists()) {
            GlideApp.with(mActivity).load("file://" + file.getAbsolutePath().toString()).into(photoView);
            seafPhoto.setDownloaded(true);
        } else {
            String urlicon = dm.getThumbnailLink(repoName, repoID, filePath, Utils.getThumbnailWidth());
            progressBar.setVisibility(View.VISIBLE);
            GlideUrl glideUrl = new GlideUrl(urlicon, new LazyHeaders.Builder()
                    .addHeader("Authorization", "Token " + mAccount.token)
                    .build());
            RequestOptions opt = new RequestOptions()
                    .skipMemoryCache(true)
                    .override(displayMetrics.widthPixels, displayMetrics.heightPixels)
                    .diskCacheStrategy(DiskCacheStrategy.NONE);
            GlideApp.with(mActivity)
                    .asBitmap()
                    .load(glideUrl)
                    .apply(opt)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(photoView);
        }

        photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {
                mActivity.hideOrShowToolBar();
            }
        });

        container.addView(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return contentView;
    }

    /**
     * when you call notifyDataSetChanged(),
     * the view pager will remove all views and reload them all.
     * As so the reload effect is obtained.
     */
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        GlideApp.with(mActivity).clear((View) object);
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
