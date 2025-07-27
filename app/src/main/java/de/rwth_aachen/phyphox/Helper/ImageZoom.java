package de.rwth_aachen.phyphox.Helper;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.appcompat.widget.AppCompatImageView;

public class ImageZoom extends View {

    private Drawable drawable;
    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private final float minScale = 1f;
    private final float maxScale = 4f;

    private PointF lastTouch = new PointF();
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ImageZoom(Context context) {
        super(context);
        init(context);
    }

    public ImageZoom(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageZoom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                showZoomDialog();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                resetZoom();
                return true;
            }
        });

        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawable = getBackground();
        if (drawable == null) return;

        canvas.save();
        canvas.concat(matrix);
        drawable.setBounds(0, 0, getWidth(), getHeight());
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        PointF currentTouch = new PointF(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(currentTouch);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentTouch.x - lastTouch.x;
                float dy = currentTouch.y - lastTouch.y;
                matrix.postTranslate(dx, dy);
                invalidate();
                lastTouch.set(currentTouch.x, currentTouch.y);
                break;
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = scale * scaleFactor;

            if (newScale >= minScale && newScale <= maxScale) {
                scale = newScale;
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                invalidate();
            }

            return true;
        }
    }

    private void resetZoom() {
        matrix.reset();
        invalidate();
        scale = 1f;
    }

    private void showZoomDialog() {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout rootLayout = new FrameLayout(getContext());

        ImageZoom zoomImage = new ImageZoom(getContext());
        Drawable bg = getBackground();
        if (bg != null) {
            zoomImage.setBackground(bg);
        }

        zoomImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ImageButton btnClose = new ImageButton(getContext());
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        closeParams.topMargin = 50;
        closeParams.rightMargin = 50;
        closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        btnClose.setLayoutParams(closeParams);
        btnClose.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        rootLayout.addView(zoomImage);
        rootLayout.addView(btnClose);

        dialog.setContentView(rootLayout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }

        dialog.show();
    }
}
