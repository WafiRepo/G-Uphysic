package de.rwth_aachen.phyphox.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawView extends View {

    private Paint paint;
    private Path currentPath;
    private List<Path> paths; // List to store paths for undo
    private List<Path> undonePaths; // List to store undone paths for redo

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize Paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK); // Color of the drawing stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);

        // Initialize paths
        paths = new ArrayList<>();
        undonePaths = new ArrayList<>();
        currentPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw all paths in the list
        for (Path p : paths) {
            canvas.drawPath(p, paint);
        }

        // Draw the current path being drawn
        canvas.drawPath(currentPath, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true); // Minta ScrollView tidak intercept
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Start a new path and move to the initial position
                currentPath = new Path();
                currentPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                // Draw a line following the finger/stylus movement
                currentPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                // Add the current path to the paths list and reset undone paths
                paths.add(currentPath);
                undonePaths.clear();
                currentPath = new Path(); // Reset the current path
                break;
            default:
                return false;
        }

        // Redraw the view
        invalidate();
        return true;
    }

    public void clearCanvas() {
        // Clear all paths and reset the canvas
        paths.clear();
        undonePaths.clear();
        invalidate(); // Redraw the view
    }

    public void undo() {
        if (paths.size() > 0) {
            // Remove the last path from the paths list and add it to the undonePaths list
            undonePaths.add(paths.remove(paths.size() - 1));
            invalidate(); // Redraw the view
        }
    }

    public void redo() {
        if (undonePaths.size() > 0) {
            // Re-add the last undone path back to the paths list
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            invalidate(); // Redraw the view
        }
    }
}
