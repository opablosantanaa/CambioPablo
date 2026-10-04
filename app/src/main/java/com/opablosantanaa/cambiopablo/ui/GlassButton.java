package com.opablosantanaa.cambiopablo.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.widget.AppCompatButton;
import com.opablosantanaa.cambiopablo.R;


public final class GlassButton extends AppCompatButton {
    private GlassSurfaceDrawable glass;
    private ValueAnimator fade;
    private float pointerX, pointerY, intensity;
    private boolean touching, hovering;

    public GlassButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.GlassButton);
        boolean accent = attributes.getBoolean(R.styleable.GlassButton_glassAccent, true);
        attributes.recycle();
        glass = new GlassSurfaceDrawable(getResources().getDisplayMetrics().density, accent);
        setBackground(glass);
        // Keep theme tinting from flattening the custom material.
        setBackgroundTintList(null);
        setStateListAnimator(null);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touching = true; setPointer(event.getX(), event.getY()); fadeTo(0.6f, 180); break;
                case MotionEvent.ACTION_MOVE:
                    setPointer(event.getX(), event.getY()); break;
                case MotionEvent.ACTION_UP:
                    touching = false;
                    // Keep feedback visible even for a tap shorter than the first animation frame.
                    setIntensity(0.6f);
                    fadeTo(isFocused() || hovering ? 0.6f : 0, 500); break;
                case MotionEvent.ACTION_CANCEL:
                    touching = false; fadeTo(isFocused() || hovering ? 0.6f : 0, 500); break;
                default: break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override public boolean performClick() {
        return super.performClick(); // Retain standard accessibility and keyboard click behavior.
    }

    @Override public boolean onHoverEvent(MotionEvent event) {
        if (isEnabled()) {
            hovering = event.getActionMasked() != MotionEvent.ACTION_HOVER_EXIT;
            if (hovering) setPointer(event.getX(), event.getY());
            fadeTo(hovering || isFocused() ? 0.6f : 0, 500);
        }
        return super.onHoverEvent(event);
    }

    @Override protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        if (glass == null) return;
        if (focused) setPointer(getWidth() / 2f, getHeight() / 2f);
        fadeTo(focused || touching || hovering ? 0.6f : 0, 500);
    }

    private void setPointer(float x, float y) {
        pointerX = Math.max(0, Math.min(getWidth(), x));
        pointerY = Math.max(0, Math.min(getHeight(), y));
        glass.setSpotlight(pointerX, pointerY, intensity);
    }

    private void setIntensity(float value) {
        intensity = value;
        glass.setSpotlight(pointerX, pointerY, intensity);
    }

    private boolean motionEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1) != 0;
    }

    private void fadeTo(float target, long duration) {
        if (fade != null) { fade.cancel(); fade = null; }
        if (!motionEnabled()) { setIntensity(target); return; }
        if (Math.abs(target - intensity) < 0.001f) return;
        fade = ValueAnimator.ofFloat(intensity, target);
        fade.setDuration(duration);
        fade.setInterpolator(new DecelerateInterpolator());
        fade.addUpdateListener(animation -> setIntensity((float) animation.getAnimatedValue()));
        fade.start();
    }

    private void stopLight() {
        if (fade != null) { fade.cancel(); fade = null; }
        touching = hovering = false;
        if (glass != null) setIntensity(0);
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) stopLight();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) stopLight();
    }

    @Override protected void onDetachedFromWindow() {
        stopLight();
        super.onDetachedFromWindow();
    }
}
