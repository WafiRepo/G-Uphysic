package de.rwth_aachen.phyphox.Helper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Paint paint;
    private Path currentPath;
    private List<Path> paths;
    private List<Path> undonePaths;
    private List<Integer> pathColors;
    private List<Integer> undonePathColors;
    private int currentColor = Color.BLACK;
    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 4;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paths = new ArrayList<>();
        undonePaths = new ArrayList<>();
        pathColors = new ArrayList<>();
        undonePathColors = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < paths.size(); i++) {
            paint.setColor(pathColors.get(i));
            canvas.drawPath(paths.get(i), paint);
        }

        if (currentPath != null) {
            paint.setColor(currentColor);
            canvas.drawPath(currentPath, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }

    private void touchStart(float x, float y) {
        currentPath = new Path();
        currentPath.moveTo(x, y);
        lastX = x;
        lastY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - lastX);
        float dy = Math.abs(y - lastY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            // Use quadTo to smooth the drawing by using control points between the last point and the current one
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
            lastX = x;
            lastY = y;
        }
    }

    private void touchUp() {
        if (currentPath != null) {
            currentPath.lineTo(lastX, lastY);
            paths.add(currentPath);
            pathColors.add(currentColor);
            currentPath = null;
        }
    }

    public void undo() {
        if (!paths.isEmpty()) {
            undonePaths.add(paths.remove(paths.size() - 1));
            undonePathColors.add(pathColors.remove(pathColors.size() - 1));
            invalidate();
        }
    }

    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            pathColors.add(undonePathColors.remove(undonePathColors.size() - 1));
            invalidate();
        }
    }

    public void clear() {
        paths.clear();
        undonePaths.clear();
        pathColors.clear();
        undonePathColors.clear();
        invalidate();
    }

    public void setColor(int color) {
        this.currentColor = color;
    }

    public int getCurrentColor() {
        return currentColor;
    }
}
