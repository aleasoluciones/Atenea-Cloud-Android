package com.ateneacloud.drive.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.cameraupload.GalleryBucketUtils;
import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncGalleryProvider;
import com.ateneacloud.drive.util.GlideApp;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Buckets fragment
 */
public class BucketsActivity extends AppCompatActivity {
    private TextView mTitle;
    private RadioGroup mRadioGroup;
    private Button mDoneBtn;
    private GridView mGridView;
    private List<GalleryBucketUtils.Bucket> buckets;
    private boolean[] selectedBuckets;
    private ImageAdapter imageAdapter;
    private Bitmap[] thumbnails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cuc_local_directory_fragment);

        mTitle = findViewById(R.id.cuc_local_library_header_tv);
        mGridView = findViewById(R.id.cuc_bucket_selection_grid);
        mRadioGroup = findViewById(R.id.cuc_local_directory_radio_group);
        mRadioGroup.setVisibility(View.GONE);
        mDoneBtn = findViewById(R.id.cuc_local_directory_btn);
        mDoneBtn.setOnClickListener(onClickListener);
        mDoneBtn.setVisibility(View.VISIBLE);

        mTitle.setText(getResources().getString(R.string.select_album_sync));


        mGridView.setVisibility(View.VISIBLE);
        mGridView.setEnabled(true);
        mRadioGroup.check(R.id.cuc_local_directory_pick_folders_rb);


        buckets = GalleryBucketUtils.getMediaBuckets(getApplicationContext());
        selectedBuckets = new boolean[buckets.size()];

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            thumbnails = new Bitmap[buckets.size()];
            for (int i = 0; i < buckets.size(); i++) {
                GalleryBucketUtils.Bucket b = buckets.get(i);
                if (b.image_id > 0) {
                    thumbnails[i] = MediaStore.Images.Thumbnails.getThumbnail(
                            getContentResolver(), b.image_id,
                            MediaStore.Images.Thumbnails.MINI_KIND, null);
                }

                selectedBuckets[i] = b.isCameraBucket;

            }
        } else {
            for (int i = 0; i < buckets.size(); i++) {
                GalleryBucketUtils.Bucket b = buckets.get(i);
                if (b.isImages != null && b.isImages.equals(GalleryBucketUtils.IMAGES)) {
                    Uri image_uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, b.imageId);
                    String image_path = Utils.getRealPathFromURI(SeadroidApplication.getAppContext(), image_uri, "images");
                    b.imagePath = image_path;
                } else {
                    Uri video_uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, b.videoId);
                    String videoPath = Utils.getRealPathFromURI(SeadroidApplication.getAppContext(), video_uri, "video");
                    b.videoPath = videoPath;
                }
                selectedBuckets[i] = b.isCameraBucket;
            }
        }

        imageAdapter = new ImageAdapter();
        mGridView.setAdapter(imageAdapter);
        mGridView.setClickable(true);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Si solo se permite la selección única, desmarca los demás álbumes
                for (int i = 0; i < selectedBuckets.length; i++) {
                    selectedBuckets[i] = (i == position); // Marca el álbum actual, desmarca los demás
                }

                // Actualiza la interfaz de usuario para reflejar la selección
                imageAdapter.notifyDataSetChanged();
            }
        });
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finishWithResult();
        }
    };

    public class ImageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public ImageAdapter() {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return buckets.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.bucket_item, null);
                holder.imageview = (ImageView) convertView.findViewById(R.id.bucket_item_thumbImage);
                holder.text = (TextView) convertView.findViewById(R.id.bucket_item_name);
                holder.marking = (ImageView) convertView.findViewById(R.id.bucket_item_marking);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.imageview.setId(position);
            holder.text.setText(buckets.get(position).name);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                holder.imageview.setImageBitmap(thumbnails[position]);
            } else {
                if (buckets.get(position).isImages != null && buckets.get(position).isImages.equals(GalleryBucketUtils.IMAGES)) {
                    GlideApp.with(getApplicationContext()).load(buckets.get(position).imagePath).into(holder.imageview);
                } else {
                    GlideApp.with(getApplicationContext()).load(Uri.fromFile(new File(buckets.get(position).videoPath))).into(holder.imageview);
                }
            }

            if (selectedBuckets[position])
                holder.marking.setBackgroundResource(R.drawable.checkbox_checked);
            else
                holder.marking.setBackgroundResource(R.drawable.checkbox_unchecked);

            holder.id = position;
            return convertView;
        }
    }

    static class ViewHolder {
        ImageView imageview;
        ImageView marking;
        TextView text;
        int id;
    }

    public List<String> getSelectedBuckets() {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            if (selectedBuckets[i]) {
                ret.add(buckets.get(i).id);
            }
        }

        return ret;
    }

    private void finishWithResult() {
        Intent intent = new Intent();
        SeafSyncGalleryProvider provider = new SeafSyncGalleryProvider();
        String albumPath = provider.getAlbumPath(getSelectedBuckets().get(0));
        intent.putExtra("album_path", albumPath);
        setResult(RESULT_OK, intent);
        finish();
    }

}
