package com.lx.used.opencv;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.lx.used.utils.OpenCvRectangle;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayOutputStream;
import java.util.List;
import me.pqpo.smartcropperlib.SmartCropper;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

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
        Observable.create(new ObservableOnSubscribe<android.graphics.Point[]>() {
            @Override
            public void subscribe(final ObservableEmitter<android.graphics.Point[]> emitter) {
                Log.d("预览", size.toString());
                int width = size.getWidth();
                int height = size.getHeight();
                double rotate =  ((double) ScreenUtils.getScreenHeight()) / width;
//                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) openCvRectangle.getLayoutParams();
//                params.width = ScreenUtils.getScreenHeight();
//                params.height = (int) (height * rotate);
//                openCvRectangle.setLayoutParams(params);
                YuvImage yuv = new YuvImage(data, format, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
                byte[] bytes = out.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Bitmap bitmapRotate = ImageUtils.rotate(bitmap, 90, 0, 0);
//                float rotate = (float) ((double)width  / ScreenUtils.getScreenHeight());
//                Log.d("图片rotate", rotate+ "");
//                Bitmap bitmap1 = changeBitmapSize(bitmapRotate, rotate, rotate, height, width);
//                Log.d("图片", bitmap1.getWidth() + "*" + bitmap1.getHeight());
//                Mat source = new Mat();
//                Utils.bitmapToMat(bitmap1, source);
//                Core.rotate(source, source, Core.ROTATE_90_CLOCKWISE);
                android.graphics.Point[] points = SmartCropper.scan(bitmapRotate);
                if (points == null) {
                    emitter.onError(new Throwable(""));
                } else {
////                    Mat mat = new Mat(source.rows(), source.cols(), source.type());
////                    Imgproc.line(mat, points[0], points[1], new Scalar(255, 255, 255), 20);
////                    Imgproc.line(mat, points[1], points[2], new Scalar(255, 255, 255), 20);
////                    Imgproc.line(mat, points[2], points[3], new Scalar(255, 255, 255), 20);
////                    Imgproc.line(mat, points[3], points[0], new Scalar(255, 255, 255), 20);
////                    List<Point> corners = new ArrayList<>();
////                    corners.add(points[0]);
////                    corners.add(points[1]);
////                    corners.add(points[2]);
////                    corners.add(points[3]);
////                    Bitmap bitmapLast = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
////                    Utils.matToBitmap(source, bitmapLast);
////                    emitter.onNext(bitmapLast);
                    emitter.onNext(points);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<android.graphics.Point[]>() {

                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(android.graphics.Point[] bitmap) {
//                        ivResult.setImageBitmap(bitmap);
//                        android.graphics.Point[] points = new android.graphics.Point[4];
//                        double rotate = (double) size.getHeight() / ScreenUtils.getScreenHeight() + 1;

//                        Log.d("比例值", rotate + "");

//                        int x0 = (int) (bitmap[0].x);
//                        int y0 = (int) (bitmap[0].y);
//                        points[0] = new android.graphics.Point(x0, y0);
//
//                        int x1 = (int) (bitmap[1].x);
//                        int y1 = (int) (bitmap[1].y);
//                        points[1] = new android.graphics.Point(x1, y1);
//
//                        int x2 = (int) (bitmap[2].x);
//                        int y2 = (int) (bitmap[2].y);
//                        points[2] = new android.graphics.Point(x2, y2);
//
//                        int x3 = (int) (bitmap[3].x);
//                        int y3 = (int) (bitmap[3].y);
//                        points[3] = new android.graphics.Point(x3, y3);

//                        openCvRectangle.onCornersDetected(bitmap);
                        isBusy = false;
                    }

                    @Override
                    public void onError(final Throwable e) {
                        isBusy = false;
//                        openCvRectangle.onCornersNotDetected();
//                        ivResult.setImageBitmap(null);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    private Bitmap changeBitmapSize(Bitmap bitmap, float scaleWidth, float scaleHeight, int oldWidth, int oldHeight) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, matrix, true);
        return bitmap;
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
