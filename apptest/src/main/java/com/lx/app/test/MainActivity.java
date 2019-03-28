package com.lx.app.test;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import com.lx.camera.entiy.PreviewAndTakeBitmapCallback;
import com.lx.camera.entiy.PreviewAndTakeStringCallback;
import com.lx.camera.fragment.CameraFragment;

public class MainActivity extends AppCompatActivity {

    private ImageView ivResult;

    private Button btnTake;

    private Button btnFlash;

    private Button btnAuto;

    private int delayTime = 2000;

    private boolean isAuto = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivResult = findViewById(R.id.ivResult);
        btnTake = findViewById(R.id.btnTake);
        btnFlash = findViewById(R.id.btnFlash);
        btnAuto = findViewById(R.id.btnAuto);
        final CameraFragment cameraFragment = new CameraFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.flMain, cameraFragment).commit();
        cameraFragment.setPreviewAndTakeBitmapCallback(new PreviewAndTakeBitmapCallback() {
            @Override
            public void onPreviewBitmapCallback(final Bitmap bitmap) {
                ivResult.setImageBitmap(bitmap);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ivResult.setImageBitmap(null);
                    }
                }, delayTime);
            }

            @Override
            public void onTakeBitmapCallback(final Bitmap bitmap) {
                ivResult.setImageBitmap(bitmap);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ivResult.setImageBitmap(null);
                    }
                }, delayTime);
            }
        });
        cameraFragment.setPreviewAndTakeStringCallback(new PreviewAndTakeStringCallback() {
            @Override
            public void onPreviewStringCallback(final String result) {

            }

            @Override
            public void onTakeStringCallback(final String result) {

            }
        });
        btnTake.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cameraFragment.takeHandPhoto();
            }
        });
        btnFlash.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cameraFragment.setFlashOpenClose();
            }
        });
        btnAuto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                boolean isSuccess = cameraFragment.setScanDocument(!isAuto);
                if (isSuccess) {
                    if (isAuto) {
                        isAuto = false;
                        btnAuto.setText("自动");
                    } else {
                        isAuto = true;
                        btnAuto.setText("手动");
                    }
                }
            }
        });
    }
}
