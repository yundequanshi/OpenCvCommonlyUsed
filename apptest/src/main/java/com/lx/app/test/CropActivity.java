package com.lx.app.test;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import me.pqpo.smartcropperlib.SmartCropper;
import me.pqpo.smartcropperlib.view.CropImageView;

public class CropActivity extends AppCompatActivity {

    private ImageView ivResult;

    private Button btnCancel;

    private Button btnSure;

    private CropImageView cropImageView;

    private String path = "";

    private boolean isFullPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        SmartCropper.buildImageDetector(this);
        ivResult = findViewById(R.id.ivResult);
        btnCancel = findViewById(R.id.btnCancel);
        btnSure = findViewById(R.id.btnSure);
        cropImageView = findViewById(R.id.cropImageView);
        path = (String) getIntent().getExtras().get("path");
        isFullPhoto = (Boolean) getIntent().getExtras().get("isFullPhoto");
        Glide.with(this)
                .load(path)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull final Drawable resource,
                            @Nullable final Transition<? super Drawable> transition) {
                        Bitmap bitmap = drawable2Bitmap(resource);
                        if (!isFullPhoto) {
                            cropImageView.setAutoScanEnable(false);
                        }
                        cropImageView.setImageToCrop(bitmap);
                    }
                });
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                onBackPressed();
            }
        });
        btnSure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                Bitmap bitmap = cropImageView.crop();
                ivResult.setImageBitmap(bitmap);
            }
        });
    }

    private Bitmap drawable2Bitmap(final Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1,
                    drawable.getOpacity() != PixelFormat.OPAQUE
                            ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE
                            ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
