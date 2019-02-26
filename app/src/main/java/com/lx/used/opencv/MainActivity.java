package com.lx.used.opencv;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import com.lx.opencvuilts.ScannerUtils;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Size;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayOutputStream;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "扫描";

    private CameraView mCameraView;

    private ImageView ivResult;

    private boolean isBusy = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.cameraView);
        ivResult = findViewById(R.id.ivResult);
        mCameraView.setLifecycleOwner(this);
        mCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull final CameraOptions options) {
            }
        });
        mCameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull final Frame frame) {
                previewFrame(frame.getData(), frame.getSize(), frame.getFormat());
            }
        });
    }

    @SuppressLint("CheckResult")
    private void previewFrame(final byte[] data, final Size size, final int format) {
        if (isBusy) {
            return;
        }
        isBusy = true;
        Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(final ObservableEmitter<Bitmap> emitter) {
                int width = size.getWidth();
                int height = size.getHeight();
                YuvImage yuv = new YuvImage(data, format, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
                byte[] bytes = out.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Mat source = new Mat();
                Utils.bitmapToMat(bitmap, source);
                Core.rotate(source, source, Core.ROTATE_90_CLOCKWISE);
                Point[] points = ScannerUtils.scanPoint(source);
                if (points == null) {
                    emitter.onError(new Throwable(""));
                } else {
                    Imgproc.line(source, points[0], points[1], new Scalar(2555, 255, 255),20);
                    Imgproc.line(source, points[1], points[2], new Scalar(2555, 255, 255),20);
                    Imgproc.line(source, points[2], points[3], new Scalar(2555, 255, 255),20);
                    Imgproc.line(source, points[3], points[0], new Scalar(2555, 255, 255),20);
                    Bitmap bitmapLast = Bitmap.createBitmap(source.cols(), source.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(source, bitmapLast);
                    emitter.onNext(bitmapLast);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Bitmap>() {

                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        isBusy = false;
                        ivResult.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(final Throwable e) {
                        isBusy = false;
                        ivResult.setImageBitmap(null);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !mCameraView.isOpened()) {
            mCameraView.open();
        }
    }
}
