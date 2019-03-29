package com.lx.app.test;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.lx.camera.entiy.PreviewAndTakeStringCallback;
import com.lx.camera.fragment.CameraFragment;

public class MainActivity extends AppCompatActivity {

    private Button btnTake;

    private Button btnFlash;

    private Button btnAuto;

    private boolean isAuto = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnTake = findViewById(R.id.btnTake);
        btnFlash = findViewById(R.id.btnFlash);
        btnAuto = findViewById(R.id.btnAuto);
        final CameraFragment cameraFragment = new CameraFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.flMain, cameraFragment).commit();
        cameraFragment.setPreviewAndTakeStringCallback(new PreviewAndTakeStringCallback() {
            @Override
            public void onPreviewStringCallback(final String result) {
                Intent intent = new Intent(getBaseContext(), CropActivity.class);
                intent.putExtra("path", result);
                intent.putExtra("isFullPhoto", false);
                startActivity(intent);
            }

            @Override
            public void onTakeStringCallback(final String result) {
                Intent intent = new Intent(getBaseContext(), CropActivity.class);
                intent.putExtra("path", result);
                intent.putExtra("isFullPhoto", true);
                startActivity(intent);
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

    @Override
    protected void onResume() {
        super.onResume();
        isAuto = true;
        btnAuto.setText("手动");
    }
}
