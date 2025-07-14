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

    // Variables for smoothing the path
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
        // Initialize the paint and set its color to white
        paint = new Paint();
        paint.setColor(Color.WHITE);  // Set the pen color to white
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);  // Enable anti-aliasing for smoother edges
        paint.setDither(true);  // Enable dithering for better color blending
        paint.setStrokeJoin(Paint.Join.ROUND);  // Smooth stroke joins
        paint.setStrokeCap(Paint.Cap.ROUND);  // Smooth stroke ends

        paths = new ArrayList<>();
        undonePaths = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }
        if (currentPath != null) {
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
            currentPath = null;
        }
    }

    // Undo the last action
    public void undo() {
        if (!paths.isEmpty()) {
            undonePaths.add(paths.remove(paths.size() - 1));
            invalidate();
        }
    }

    // Redo the last undone action
    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            invalidate();
        }
    }

    // Clear the canvas
    public void clear() {
        paths.clear();
        undonePaths.clear();
        invalidate();  // Refresh the view to clear everything
    }
}
