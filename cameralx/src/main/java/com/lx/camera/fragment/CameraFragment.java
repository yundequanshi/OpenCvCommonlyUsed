package com.lx.camera.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.lx.camera.R;
import com.lx.camera.entiy.PreviewAndTakeBitmapCallback;
import com.lx.camera.entiy.PreviewAndTakeStringCallback;
import com.lx.camera.preview.CameraSurfacePreview;
import com.lx.camera.utils.GallerySaveExpert;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class CameraFragment extends Fragment {


    private static final int RC_CAMERA_PERM = 123;

    private static final int RC_WRITE_PERM = 125;

    static {
        System.loadLibrary("opencv_java3");
    }

    private View mView = null;

    private FrameLayout preview;

    private CameraSurfacePreview surfacePreview = null;

    private FrameLayout mProgressBar;

    private View focusMarker;

    private PreviewAndTakeBitmapCallback mCallback = null;

    private PreviewAndTakeStringCallback mStringCallback;

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (mView == null) {
            initView(inflater, container);
        } else {
            if (container != null) {
                container.removeView(mView);
            }
        }
        return mView;
    }

    private void initView(LayoutInflater inflater, ViewGroup container) {
        mView = inflater.inflate(R.layout.fragment_camera, container, false);
        preview = mView.findViewById(R.id.camera_preview);
        mProgressBar = mView.findViewById(R.id.flProgressBar);
        focusMarker = mView.findViewById(R.id.focus_marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (surfacePreview != null) {
            surfacePreview.releaseCameraAndPreview();
            surfacePreview = null;
            preview.removeAllViews();
        }
        requestCameraPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (surfacePreview != null) {
            surfacePreview.releaseCameraAndPreview();
            surfacePreview = null;
            preview.removeAllViews();
        }
    }

    @AfterPermissionGranted(RC_CAMERA_PERM)
    private void requestCameraPermission() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(getContext(), perms)) {
            ImageView canvasFrame = mView.findViewById(R.id.camera_drawing_pane);
            surfacePreview = new CameraSurfacePreview(getContext(), canvasFrame, focusMarker, mProgressBar);
            preview.addView(surfacePreview);
            surfacePreview.setPreviewAndTakeCallback(new PreviewAndTakeBitmapCallback() {
                @Override
                public void onPreviewBitmapCallback(final Bitmap bitmap) {
                    if (mCallback != null) {
                        mCallback.onPreviewBitmapCallback(bitmap);
                    }
                    if (mStringCallback != null) {
                        requestWritePermission(bitmap, true);
                    }
                }

                @Override
                public void onTakeBitmapCallback(final Bitmap bitmap) {
                    if (mCallback != null) {
                        mCallback.onTakeBitmapCallback(bitmap);
                    }
                    if (mStringCallback != null) {
                        requestWritePermission(bitmap, false);
                    }
                }
            });
        } else {
            EasyPermissions.requestPermissions(this, "您拒绝了我们的权限，功能没法使用，请设置权限",
                    RC_CAMERA_PERM, perms);
        }
    }

    @AfterPermissionGranted(RC_WRITE_PERM)
    private void requestWritePermission(Bitmap bitmap, boolean isPreview) {
        mProgressBar.setVisibility(View.VISIBLE);
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(getContext(), perms)) {
            if (bitmap != null) {
                saveBitmap(bitmap, isPreview);
            }
        } else {
            EasyPermissions.requestPermissions(this, "您拒绝了储存权限，功能没法使用，请设置权限",
                    RC_WRITE_PERM, perms);
        }
    }

    @SuppressLint("CheckResult")
    private void saveBitmap(final Bitmap bitmap, final boolean isPreview) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                String result = GallerySaveExpert
                        .writePhotoFile(bitmap, "photo", getString(R.string.app_name), Bitmap.CompressFormat.JPEG,
                                true, getActivity());
                emitter.onNext(result);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(final String bitmapString) {
                        mProgressBar.setVisibility(View.GONE);
                        if (mStringCallback != null) {
                            if (isPreview) {
                                mStringCallback.onPreviewStringCallback(bitmapString);
                            } else {
                                mStringCallback.onTakeStringCallback(bitmapString);
                            }
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 拍照/自动预览获取文档
     */
    public void setPreviewAndTakeBitmapCallback(PreviewAndTakeBitmapCallback callback) {
        mCallback = callback;
    }

    /**
     * 拍照/自动预览获取文档储存地址
     */
    public void setPreviewAndTakeStringCallback(final PreviewAndTakeStringCallback stringCallback) {
        mStringCallback = stringCallback;
    }

    /**
     * 闪光灯开关（默认是关）
     */
    public void setFlashOpenClose() {
        if (surfacePreview != null) {
            surfacePreview.setFlashOpenClose();
        }
    }

    /**
     * 是否自动扫描文档
     */
    public void setScanDocument(boolean isHandTake) {
        if (surfacePreview != null) {
            surfacePreview.setScanDocument(isHandTake);
        }
    }

    /**
     * 手动拍照
     */
    public void takeHandPhoto() {
        if (surfacePreview != null) {
            surfacePreview.takeHandPhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
