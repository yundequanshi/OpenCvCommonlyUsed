package com.lx.used.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

public class OpenCvRectangle extends View {

    private Paint mLinePaint = new Paint();

    private Path mLinePath = new Path();

    public OpenCvRectangle(final Context context) {
        super(context);
        init();
    }

    public OpenCvRectangle(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OpenCvRectangle(final Context context, final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public OpenCvRectangle(final Context context, final AttributeSet attrs,
            final int defStyleAttr,
            final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        mLinePaint.setColor(Color.parseColor("#3D94FD"));
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);
        mLinePaint.setAlpha(80);
        mLinePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mLinePath, mLinePaint);
    }

    public void onCornersDetected(Point[] point) {
        resetPointPath(point);
        invalidate();
    }

    public void onCornersNotDetected() {
        mLinePath.reset();
        invalidate();
    }

    private void resetPointPath(Point[] point) {
        mLinePath.reset();
        Point lt = point[0];
        Point rt = point[1];
        Point rb = point[2];
        Point lb = point[3];
        mLinePath.moveTo(lt.x, lt.y);
        mLinePath.lineTo(rt.x, rt.y);
        mLinePath.lineTo(rb.x, rb.y);
        mLinePath.lineTo(lb.x, lb.y);
        mLinePath.close();
    }
}
