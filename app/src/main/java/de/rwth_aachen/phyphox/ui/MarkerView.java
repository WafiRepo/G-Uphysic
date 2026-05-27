package de.rwth_aachen.phyphox.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class MarkerView extends View {
    private Paint objectPaint;
    private Paint centerPaint;
    private PointF objectPoint;
    private PointF centerPoint;
    private boolean markingObject = true;
    private OnPointMarkedListener listener;

    public interface OnPointMarkedListener {
        void onObjectMarked(float x, float y);
        void onCenterMarked(float x, float y);
    }

    public MarkerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        objectPaint = new Paint();
        objectPaint.setColor(Color.RED);
        objectPaint.setStrokeWidth(5);
        objectPaint.setStyle(Paint.Style.STROKE);

        centerPaint = new Paint();
        centerPaint.setColor(Color.BLUE);
        centerPaint.setStrokeWidth(5);
        centerPaint.setStyle(Paint.Style.STROKE);
    }

    public void setOnPointMarkedListener(OnPointMarkedListener listener) {
        this.listener = listener;
    }

    public void setMarkingObject(boolean markingObject) {
        this.markingObject = markingObject;
    }

    public void reset() {
        objectPoint = null;
        centerPoint = null;
        markingObject = true;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (markingObject) {
                objectPoint = new PointF(event.getX(), event.getY());
                if (listener != null) listener.onObjectMarked(event.getX(), event.getY());
            } else {
                centerPoint = new PointF(event.getX(), event.getY());
                if (listener != null) listener.onCenterMarked(event.getX(), event.getY());
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (objectPoint != null) {
            drawCrosshair(canvas, objectPoint.x, objectPoint.y, objectPaint);
        }
        if (centerPoint != null) {
            drawCrosshair(canvas, centerPoint.x, centerPoint.y, centerPaint);
        }
    }

    private void drawCrosshair(Canvas canvas, float x, float y, Paint paint) {
        float size = 30;
        canvas.drawLine(x - size, y, x + size, y, paint);
        canvas.drawLine(x, y - size, x, y + size, paint);
        canvas.drawCircle(x, y, 10, paint);
    }

    public PointF getObjectPoint() {
        return objectPoint;
    }

    public PointF getCenterPoint() {
        return centerPoint;
    }
}
