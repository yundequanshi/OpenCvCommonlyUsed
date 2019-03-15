package com.lx.used.opencv;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.lx.opencvuilts.ScannerUtils;
import com.lx.used.utils.OpenCvRectangle;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.Size;
import com.otaliastudios.cameraview.SizeSelector;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import me.pqpo.smartcropperlib.SmartCropper;
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

    private OpenCvRectangle openCvRectangle;

    private boolean isBusy = false;

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mCameraView.takePicture();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.cameraView);
        ivResult = findViewById(R.id.ivResult);
        openCvRectangle = findViewById(R.id.openCvRectangle);
        mCameraView.setLifecycleOwner(this);
        mCameraView.setPreviewStreamSize(new SizeSelector() {
            @NonNull
            @Override
            public List<Size> select(@NonNull final List<Size> source) {
                return source;
            }
        });
//        mCameraView.setPreviewStreamSize(new SizeSelector() {
//            @NonNull
//            @Override
//            public List<Size> select(@NonNull final List<Size> source) {
//                for (Size size:source){
//                    Log.d("预览", size.toString());
//                }
//                return source;
//            }
//        });
        mCameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull final Frame frame) {
                previewFrame(frame.getData(), frame.getSize(), frame.getFormat());
            }
        });
    }

    @SuppressLint("CheckResult")
    private void previewFrame2(final Bitmap bitmap) {
        if (isBusy) {
            return;
        }
        isBusy = true;
        Observable.create(new ObservableOnSubscribe<android.graphics.Point[]>() {
            @Override
            public void subscribe(final ObservableEmitter<android.graphics.Point[]> emitter) {
                Mat source = new Mat();
                Utils.bitmapToMat(bitmap, source);
                android.graphics.Point[] points = SmartCropper.scan(bitmap);
                if (points == null) {
                    emitter.onError(new Throwable(""));
                } else {
                    emitter.onNext(points);
                }
//                ivResult.setImageBitmap(bitmap);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<android.graphics.Point[]>() {

                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(android.graphics.Point[] bitmap) {
                        openCvRectangle.onCornersDetected(bitmap);
                        isBusy = false;
                        mHandler.postDelayed(mRunnable, 5000);
                    }

                    @Override
                    public void onError(final Throwable e) {
                        mHandler.postDelayed(mRunnable, 3000);
                        isBusy = false;
                        openCvRectangle.onCornersNotDetected();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    @SuppressLint("CheckResult")
    private void previewFrame(final byte[] data, final Size size, final int format) {
        if (isBusy) {
            return;
        }
        isBusy = true;
        Observable.create(new ObservableOnSubscribe<Point[]>() {
            @Override
            public void subscribe(final ObservableEmitter<Point[]> emitter) {
                Log.d("预览", size.toString());
                int width = size.getWidth();
                int height = size.getHeight();
                YuvImage yuv = new YuvImage(data, format, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
                byte[] bytes = out.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                Bitmap bitmap1 = ImageUtils.compressByScale(bitmap, bitmap.getWidth() * 2, bitmap.getHeight() * 2);
                Mat source = new Mat();
                Utils.bitmapToMat(bitmap, source);
                Core.rotate(source, source, Core.ROTATE_90_CLOCKWISE);
                Point[] points = ScannerUtils.scanPoint(source);
                if (points == null) {
                    emitter.onError(new Throwable(""));
                } else {
                    Mat mat = new Mat(source.rows(), source.cols(), source.type());
                    Imgproc.line(mat, points[0], points[1], new Scalar(255, 255, 255), 20);
                    Imgproc.line(mat, points[1], points[2], new Scalar(255, 255, 255), 20);
                    Imgproc.line(mat, points[2], points[3], new Scalar(255, 255, 255), 20);
                    Imgproc.line(mat, points[3], points[0], new Scalar(255, 255, 255), 20);
//                    List<Point> corners = new ArrayList<>();
//                    corners.add(points[0]);
//                    corners.add(points[1]);
//                    corners.add(points[2]);
//                    corners.add(points[3]);
//                    Bitmap bitmapLast = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(source, bitmapLast);
//                    emitter.onNext(bitmapLast);
                    emitter.onNext(points);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Point[]>() {

                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(Point[] bitmap) {
//                        ivResult.setImageBitmap(bitmap);
                        android.graphics.Point[] points = new android.graphics.Point[4];
//                        double rotate = (double) size.getHeight() / ScreenUtils.getScreenHeight() + 1;

//                        Log.d("比例值", rotate + "");

                        int x0 = (int) (bitmap[0].x);
                        int y0 = (int) (bitmap[0].y);
                        points[0] = new android.graphics.Point(x0, y0);

                        int x1 = (int) (bitmap[1].x);
                        int y1 = (int) (bitmap[1].y);
                        points[1] = new android.graphics.Point(x1, y1);

                        int x2 = (int) (bitmap[2].x);
                        int y2 = (int) (bitmap[2].y);
                        points[2] = new android.graphics.Point(x2, y2);

                        int x3 = (int) (bitmap[3].x);
                        int y3 = (int) (bitmap[3].y);
                        points[3] = new android.graphics.Point(x3, y3);

                        openCvRectangle.onCornersDetected(points);
                        isBusy = false;
                    }

                    @Override
                    public void onError(final Throwable e) {
                        isBusy = false;
//                        ivResult.setImageBitmap(null);
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
