package com.otaliastudios.cameraview.gesture;

import android.content.res.TypedArray;

import android.support.annotation.NonNull;
import com.otaliastudios.cameraview.R;


/**
 * Parses gestures from XML attributes.
 */
public class GestureParser {

    private int tapAction;
    private int longTapAction;
    private int pinchAction;
    private int horizontalScrollAction;
    private int verticalScrollAction;

    public GestureParser(@NonNull TypedArray array) {
        this.tapAction = array.getInteger(R.styleable.CameraView_cameraGestureTap, GestureAction.DEFAULT_TAP.value());
        this.longTapAction = array.getInteger(R.styleable.CameraView_cameraGestureLongTap, GestureAction.DEFAULT_LONG_TAP.value());
        this.pinchAction = array.getInteger(R.styleable.CameraView_cameraGesturePinch, GestureAction.DEFAULT_PINCH.value());
        this.horizontalScrollAction = array.getInteger(R.styleable.CameraView_cameraGestureScrollHorizontal, GestureAction.DEFAULT_SCROLL_HORIZONTAL.value());
        this.verticalScrollAction = array.getInteger(R.styleable.CameraView_cameraGestureScrollVertical, GestureAction.DEFAULT_SCROLL_VERTICAL.value());
    }

    private GestureAction get(int which) {
        return GestureAction.fromValue(which);
    }

    public GestureAction getTapAction() {
        return get(tapAction);
    }

    public GestureAction getLongTapAction() {
        return get(longTapAction);
    }

    public GestureAction getPinchAction() {
        return get(pinchAction);
    }

    public GestureAction getHorizontalScrollAction() {
        return get(horizontalScrollAction);
    }

    public GestureAction getVerticalScrollAction() {
        return get(verticalScrollAction);
    }

}
