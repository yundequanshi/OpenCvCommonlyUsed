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

public class OpenCvDraw extends View {

    private Paint mLinePaint = new Paint();

    private Path mLinePath = new Path();

    public OpenCvDraw(final Context context) {
        super(context);
        init();
    }

    public OpenCvDraw(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OpenCvDraw(final Context context, final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public OpenCvDraw(final Context context, final AttributeSet attrs,
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

    public void onCornersDetected(Point[] point) {
        resetPointPath(point);
        invalidate();
    }

    public void onCornersNotDetected() {
        mLinePath.reset();
        invalidate();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mLinePath, mLinePaint);
    }

    private void resetPointPath(Point[] point) {
        mLinePath.reset();
        Point lt = point[0];
        Point rt = point[1];
        Point rb = point[2];
        Point lb = point[3];
        mLinePath.moveTo(getViewPointX(lt), getViewPointY(lt));
        mLinePath.lineTo(getViewPointX(rt), getViewPointY(rt));
        mLinePath.lineTo(getViewPointX(rb), getViewPointY(rb));
        mLinePath.lineTo(getViewPointX(lb), getViewPointY(lb));
        mLinePath.close();
    }

    private float getViewPointX(Point point) {
        return (float) point.x;
    }

    private float getViewPointY(Point point) {
        return (float) point.y;
    }
}
