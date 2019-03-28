package com.lx.camera.entiy;

import android.graphics.Bitmap;

public interface PreviewAndTakeBitmapCallback {

    void onPreviewBitmapCallback(Bitmap bitmap);

    void onTakeBitmapCallback(Bitmap bitmap);
}
