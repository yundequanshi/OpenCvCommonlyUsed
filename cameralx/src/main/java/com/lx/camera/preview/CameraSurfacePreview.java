package com.lx.camera.preview;

import static android.content.Context.WINDOW_SERVICE;
import static android.graphics.ImageFormat.NV21;
import static com.lx.camera.utils.FocusAreaSpecialist.convert;
import static com.lx.camera.utils.FocusAreaSpecialist.getBoundingBox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.lx.camera.R;
import com.lx.camera.entiy.PreviewAndTakeBitmapCallback;
import com.lx.camera.utils.AffineTransformator;
import com.lx.camera.utils.BestPreviewSizeTool;
import com.lx.camera.utils.ExcelentRotator;
import com.lx.camera.utils.ViewUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class CameraSurfacePreview extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.PictureCallback, AutoFocusCallback {

    private static final String TAG = "CameraSurfacePreview";

    private static final int ANGLE_BETWEEN_ROTATION_STATES = 90;

    static {
        System.loadLibrary("native-lib");
    }

    @SuppressWarnings("JniMissingFunction")
    private static native void decode(byte[] yuv420sp, int width, int height, int[] arr);

    private SurfaceHolder mHolder;

    private Camera mCamera;

    private Parameters mParameters;

    private PreviewAndTakeBitmapCallback mPreviewCallback = null;

    private ImageView canvasFrame;

    private View focusMarker;

    private FrameLayout mProgressBar;

    private Canvas canvas;

    private Paint paint;

    private Path mLinePath;

    private int correctCameraOrientation = 0;

    private int previewFrameWidth = 0;

    private int previewFrameHeight = 0;

    private boolean isHandTake = true;

    private int canvasFrameHeight = 0;

    private int canvasFrameWidth = 0;

    private boolean processFrame = false;

    private final int[] sheetXCoords = new int[4];

    private final int[] sheetYCoords = new int[4];

    private String currentFocusMode = null;

    private boolean isPreviewTakePhoto = false;

    private int delayTime = 4000;

    private int delayTipsTime = 2000;

    private int delayWhat = 4000;

    private int delayAutoTime = 10000;

    private int delayAutoWhat = 10000;

    private MediaPlayer mediaPlayer = null;

    private boolean isScan = true;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == delayWhat) {
                isHandTake = true;
                processFrame = true;
            }
            if (msg.what == delayAutoWhat) {
                isHandTake = false;
                mHandler.removeMessages(delayWhat);
                if (!isPreviewTakePhoto) {
                    takePhoto();
                }
            }
        }
    };

    public CameraSurfacePreview(final Context context, ImageView canvasFrame, View focusMarker,
            FrameLayout mProgressBar) {
        super(context);
        this.canvasFrame = canvasFrame;
        this.focusMarker = focusMarker;
        this.mProgressBar = mProgressBar;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.setKeepScreenOn(true);
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.take);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        mCamera = getCameraInstance();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        if (holder.getSurface() == null) {
            return;
        }
        stopCameraPreview();
        updateCorrectCameraOrientation();
        initPreview();
        initDrawingTools(width, height);
        setFocus();
    }

    /**
     * 设置摄像头角度
     */
    private void updateCorrectCameraOrientation() {
        int FULL_ANGLE = 360;
        int rotation = 0, degrees, result;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);

        WindowManager windowManager = (WindowManager) getContext().getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            if (display != null) {
                rotation = display.getRotation();
            }
        }

        degrees = rotation * ANGLE_BETWEEN_ROTATION_STATES;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % FULL_ANGLE;
            result = (FULL_ANGLE - result) % FULL_ANGLE;
        } else {
            result = (info.orientation - degrees + FULL_ANGLE) % FULL_ANGLE;
        }
        correctCameraOrientation = result;
    }

    /**
     * 初始化预览
     */
    private void initPreview() {
        if (mHolder == null) {
            return;
        }
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mParameters = mCamera.getParameters();
            previewFrameWidth = mParameters.getPreviewSize().width;
            previewFrameHeight = mParameters.getPreviewSize().height;
            BestPreviewSizeTool.SizePair calculatedPair = BestPreviewSizeTool
                    .generateValidPreviewSize(mParameters, previewFrameWidth, previewFrameHeight);
            if (calculatedPair != null) {
                mParameters.setPictureSize(calculatedPair.getPictureSize().width,
                        calculatedPair.getPictureSize().height);
                mParameters.setPreviewSize(calculatedPair.getPreviewSize().width,
                        calculatedPair.getPreviewSize().height);
            }
            mParameters.setRotation(correctCameraOrientation);
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mParameters.setPreviewFormat(NV21);
            mCamera.setParameters(mParameters);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setDisplayOrientation(correctCameraOrientation);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            isHandTake = true;
            mHandler.sendEmptyMessageDelayed(delayWhat, delayTime);
            mHandler.sendEmptyMessageDelayed(delayAutoWhat, delayAutoTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化绘制相关
     */
    private void initDrawingTools(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        canvasFrameWidth = width;
        canvasFrameHeight = height;
        canvas = new Canvas(bitmap);
        canvasFrame.setScaleType(ImageView.ScaleType.FIT_XY);
        canvasFrame.setImageBitmap(bitmap);
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.colorSurfaceDraw));
        paint.setStyle(Style.FILL_AND_STROKE);
        paint.setAlpha(80);
        paint.setStrokeWidth(10f);
        mLinePath = new Path();
    }

    /**
     * 绘制扫面的文档边框
     */
    private void drawLinesWithScale(int srcWidth, int srcHeight, int dstWidth, int dstHeight, int[] arr) {
        final float wScale = (float) dstWidth / srcWidth;
        final float hScale = (float) dstHeight / srcHeight;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mLinePath.reset();
        int width = (int) (Math.abs((wScale * arr[0])) - (wScale * arr[2]));
        int height = (int) (Math.abs((hScale * arr[5])) - (hScale * arr[3]));
        if (width > 1000 || height > 1000) {
            mHandler.sendEmptyMessageDelayed(delayWhat, delayTipsTime);
            return;
        }
        int widthHalf = ViewUtils.getScreenWidth(getContext()) / 2;
        int heightHalf = ViewUtils.getScreenWidth(getContext()) / 2;
        double point1 = Math.abs(wScale * arr[6] - wScale * arr[0]);
        double point2 = Math.abs(wScale * arr[4] - wScale * arr[2]);
        double point3 = Math.abs(hScale * arr[7] - hScale * arr[5]);
        double point4 = Math.abs(hScale * arr[3] - hScale * arr[1]);
        if (point1 < widthHalf || point2 < widthHalf || point3 < widthHalf || point4 < widthHalf) {
            Toast.makeText(getContext(), getContext().getString(R.string.string_preview_tips_distance), Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessageDelayed(delayWhat, delayTipsTime);
            return;
        }
        mLinePath.moveTo((int) (wScale * arr[0]), (int) (hScale * arr[1]));
        mLinePath.lineTo((int) (wScale * arr[2]), (int) (hScale * arr[3]));
        mLinePath.lineTo((int) (wScale * arr[4]), (int) (hScale * arr[5]));
        mLinePath.lineTo((int) (wScale * arr[6]), (int) (hScale * arr[7]));
        mLinePath.close();
        canvas.drawPath(mLinePath, paint);
        canvasFrame.draw(canvas);
        canvasFrame.invalidate();
        sheetXCoords[0] = arr[0];
        sheetYCoords[0] = arr[1];
        sheetXCoords[1] = arr[2];
        sheetYCoords[1] = arr[3];
        sheetXCoords[2] = arr[4];
        sheetYCoords[2] = arr[5];
        sheetXCoords[3] = arr[6];
        sheetYCoords[3] = arr[7];
        isPreviewTakePhoto = true;
        mHandler.removeMessages(delayAutoWhat);
        takePhoto();
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        if (mCamera == null) {
            return;
        }
        if (mParameters == null) {
            return;
        }
        if (setUpFocus()) {
            mCamera.autoFocus(this);
        } else {
            mCamera.takePicture(null, null, this);
        }
    }

    /**
     * 手动拍照
     */
    public void takeHandPhoto() {
        isHandTake = false;
        mHandler.removeMessages(delayWhat);
        mHandler.removeMessages(delayAutoWhat);
        if (!isPreviewTakePhoto) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    mediaPlayer.pause();
                }
            });
            takePhoto();
        }
    }

    /**
     * 是否自动扫描文档
     */
    public boolean setScanDocument(boolean isScan) {
        boolean isSuccess = true;
        if (isScan) {
            this.isScan = isScan;
            currentFocusMode = null;
            mHandler.sendEmptyMessageDelayed(delayWhat, delayTime);
            mHandler.sendEmptyMessageDelayed(delayAutoWhat, delayAutoTime);
        } else {
            if (isPreviewTakePhoto) {
                this.isScan = true;
                currentFocusMode = null;
                isSuccess = false;
            } else {
                this.isScan = isScan;
                processFrame = false;
                mHandler.removeMessages(delayWhat);
                mHandler.removeMessages(delayAutoWhat);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mLinePath.reset();
            }
        }
        return isSuccess;
    }

    /**
     * 设置焦点
     */
    private void setFocus() {
        if (mCamera == null) {
            return;
        }
        if (mParameters == null) {
            return;
        }
        List<String> focusModes = mParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(mParameters);
        mCamera.autoFocus(this);
    }

    @Override
    public void onAutoFocus(final boolean success, final Camera camera) {
        if (isPreviewTakePhoto || !isHandTake) {
            mCamera.takePicture(null, null, this);
        }
        if (currentFocusMode != null) {
            mParameters.setFocusMode(currentFocusMode);
            camera.setParameters(mParameters);
            currentFocusMode = null;
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        stopCameraPreview();
    }

    /**
     * 停止预览/释放绘制
     */
    public void stopCameraPreview() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
            if (canvas != null && canvasFrame != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvasFrame.draw(canvas);
                canvasFrame.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机/停止预览
     */
    public void releaseCameraAndPreview() {
        if (mCamera == null) {
            return;
        }
        if (mHolder == null) {
            return;
        }
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 处理三星 索尼的图像问题
     */
    private int samsungAndSonyDevicesRotationBugAdditionalRotateCompute(byte[] bytes, int width, int height) {
        int rotationDegrees = 0;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface exifInterface = new ExifInterface(inputStream);
                orientation = exifInterface
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } else {
                if (width > height) {
                    orientation = ExifInterface.ORIENTATION_ROTATE_90;
                }
            }

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotationDegrees;
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (processFrame && isHandTake && currentFocusMode == null) {
            processFrame = false;
            previewFrame(data);
        }
    }

    /**
     * 预览
     */
    @SuppressLint("CheckResult")
    private void previewFrame(final byte[] data) {
        Observable.create(new ObservableOnSubscribe<int[]>() {
            @Override
            public void subscribe(final ObservableEmitter<int[]> emitter) throws Exception {
                final int[] arr = new int[8];
                decode(data, previewFrameWidth, previewFrameHeight, arr);
                emitter.onNext(arr);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<int[]>() {
                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(final int[] ints) {
                        for (int i = 0; i < correctCameraOrientation / ANGLE_BETWEEN_ROTATION_STATES; i++) {
                            ExcelentRotator.rotate90(ints, previewFrameWidth, previewFrameHeight);
                        }
                        drawLinesWithScale(previewFrameWidth, previewFrameHeight, canvasFrameWidth, canvasFrameHeight,
                                ints);
                    }

                    @Override
                    public void onError(final Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onPictureTaken(final byte[] data, final Camera camera) {
        mProgressBar.setVisibility(VISIBLE);
        savePreviewDocument(data);
    }

    /**
     * 生成扫描得到的文档
     */
    @SuppressLint("CheckResult")
    private void savePreviewDocument(final byte[] data) {
        Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(final ObservableEmitter<Bitmap> emitter) throws Exception {
                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
                int additionalRotate = samsungAndSonyDevicesRotationBugAdditionalRotateCompute(data,
                        picture.getWidth(), picture.getHeight());
                picture = ExcelentRotator.rotateBitmap(picture, additionalRotate);
                if (isHandTake) {
                    int[] xsy = sheetXCoords;
                    int[] yki = sheetYCoords;
                    ExcelentRotator.scaleCoords(xsy, yki, previewFrameWidth, previewFrameHeight, picture.getWidth(),
                            picture.getHeight());
                    Vector<Point> sheetCoords = new Vector<>();
                    for (int i = 0; i < xsy.length; i++) {
                        sheetCoords.add(new Point(xsy[i], yki[i]));
                    }
                    Mat source = new Mat();
                    Utils.bitmapToMat(picture, source);
//                    Imgproc.cvtColor(source, source, Imgproc.COLOR_BGR2RGB);
                    Mat resultImg = AffineTransformator.iso216ratioTransform(source, sheetCoords);
//                    EffectiveMagician.realMagic(resultImg.getNativeObjAddr());
                    Bitmap bitmapResult = Bitmap
                            .createBitmap(resultImg.cols(), resultImg.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(resultImg, bitmapResult);
                    emitter.onNext(bitmapResult);
                } else {
                    emitter.onNext(picture);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onSubscribe(final Disposable d) {

                    }

                    @Override
                    public void onNext(final Bitmap bitmap) {
                        if (mPreviewCallback != null) {
                            if (isHandTake) {
                                mPreviewCallback.onPreviewBitmapCallback(bitmap);
                            } else {
                                mPreviewCallback.onTakeBitmapCallback(bitmap);
                            }
                        }
                        isPreviewTakePhoto = false;
                        if (isScan) {
                            mHandler.sendEmptyMessageDelayed(delayWhat, delayTime);
                            mHandler.sendEmptyMessageDelayed(delayAutoWhat, delayAutoTime);
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            mLinePath.reset();
                        } else {
                            isHandTake = true;
                        }
                        mCamera.startPreview();
                        mProgressBar.setVisibility(GONE);
                    }

                    @Override
                    public void onError(final Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getPointerCount() == 1) {
            try {
                if (!isPreviewTakePhoto) {
                    if (setUpFocus()) {
                        mCamera.autoFocus(this);
                    } else {
                        handleFocus(event);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 触摸聚焦
     */
    private void handleFocus(MotionEvent event) {
        try {
            if (mCamera == null) {
                return;
            }
            Parameters parameters = mCamera.getParameters();
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int dp25 = ViewUtils.dp2px(25);
            focusMarker.setX(event.getX() - dp25);
            focusMarker.setY(event.getY() - dp25);
            focusMarker.setVisibility(VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    focusMarker.setVisibility(GONE);
                }
            }, 500);
            currentFocusMode = parameters.getFocusMode();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, viewWidth, viewHeight);
                Camera.Area area = new Camera.Area(focusRect, 1000);
                if (parameters.getMaxNumFocusAreas() > 0) {
                    parameters.setFocusAreas(Collections.singletonList(area));
                }
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setMeteringAreas(Collections.singletonList(area));
                }
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
        }
        mCamera.autoFocus(this);
    }

    private Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int halfAreaSize = areaSize / 2;
        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000)
                , clamp(centerY - halfAreaSize, -1000, 1000)
                , clamp(centerX + halfAreaSize, -1000, 1000)
                , clamp(centerY + halfAreaSize, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right),
                Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 聚焦文档
     */
    private boolean setUpFocus() {
        try {
            Parameters parameters = mCamera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                Rect rArea = getBoundingBox(Arrays.copyOf(sheetXCoords, sheetXCoords.length),
                        Arrays.copyOf(sheetYCoords, sheetYCoords.length));
                rArea = convert(rArea, previewFrameWidth, previewFrameHeight);
                Camera.Area area = new Camera.Area(rArea, 1000);
                if (parameters.getMaxNumFocusAreas() > 0) {
                    parameters.setFocusAreas(Collections.singletonList(area));
                }

                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setMeteringAreas(Collections.singletonList(area));
                }
                mCamera.setParameters(parameters);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置闪光灯
     */
    public void setFlashOpenClose() {
        if (mCamera == null) {
            return;
        }
        if (mParameters == null) {
            return;
        }
        if (mParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCamera.setParameters(mParameters);
    }

    public void setPreviewAndTakeCallback(final PreviewAndTakeBitmapCallback callback) {
        mPreviewCallback = callback;
    }
}
