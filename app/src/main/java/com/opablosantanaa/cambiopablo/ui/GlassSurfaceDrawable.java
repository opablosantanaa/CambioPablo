package com.opablosantanaa.cambiopablo.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;


public final class GlassSurfaceDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final RectF rim = new RectF();
    private final Path clip = new Path();
    private final float density;
    private final boolean accent;
    private Shader base, reflection, border;
    private boolean selected, focused, pressed, enabled = true;
    private float lightX, lightY, lightOpacity;
    private int alpha = 255;

    public GlassSurfaceDrawable(float density, boolean accent) {
        this.density = density;
        this.accent = accent;
    }

    @Override protected void onBoundsChange(Rect bounds) {
        surface.set(bounds);
        rim.set(surface); rim.inset(density * 0.75f, density * 0.75f);
        clip.reset(); clip.addRoundRect(surface, 16 * density, 16 * density, Path.Direction.CW);
        rebuildMaterial();
    }

    private void rebuildMaterial() {
        if (surface.isEmpty()) return;
        boolean violet = accent || selected;
        base = new LinearGradient(surface.left, surface.top, surface.right, surface.bottom,
                violet ? new int[]{0xde7153c6, 0xd34a328e, 0xe139286f}
                        : new int[]{0x704d6388, 0x392f4569, 0x601a2947},
                new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);
        reflection = new LinearGradient(0, surface.top, 0, surface.bottom,
                new int[]{0x3dffffff, 0x12ffffff, 0x00ffffff, 0x08b7dfff, 0x20b7dfff},
                new float[]{0, 0.22f, 0.5f, 0.82f, 1}, Shader.TileMode.CLAMP);
        border = new LinearGradient(surface.left, surface.top, surface.right, surface.bottom,
                new int[]{0xb3e5f9ff, 0x42b4c8ed, 0x20ffffff, 0x88b4a3ed},
                new float[]{0, 0.35f, 0.65f, 1}, Shader.TileMode.CLAMP);
        invalidateSelf();
    }

    public void setSpotlight(float x, float y, float opacity) {
        lightX = x; lightY = y; lightOpacity = opacity;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(Math.round(alpha * (enabled ? 1f : 0.65f)));
        paint.setShader(base);
        canvas.drawRoundRect(surface, 16 * density, 16 * density, paint);
        paint.setShader(reflection);
        canvas.drawRoundRect(surface, 16 * density, 16 * density, paint);
        int save = canvas.save();
        canvas.clipPath(clip);
        if (pressed) {
            paint.setShader(null); paint.setColor(0x12ffffff);
            paint.setAlpha(Math.round(18 * alpha / 255f));
            canvas.drawRect(surface, paint);
        }
        if (lightOpacity > 0) {
            // SpotlightCard: cyan at the pointer, transparent at 80% of the radial field.
            float radius = Math.max(surface.width() * 0.65f, surface.height() * 2);
            paint.setShader(new RadialGradient(lightX, lightY, radius,
                    new int[]{0x3300e5ff, 0x0000e5ff}, new float[]{0, 0.8f}, Shader.TileMode.CLAMP));
            paint.setAlpha(Math.round(alpha * lightOpacity));
            canvas.drawRect(surface, paint);
        }
        canvas.restoreToCount(save);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((focused ? 2 : 1) * density);
        paint.setAlpha(alpha);
        paint.setShader(border);
        canvas.drawRoundRect(rim, 15.25f * density, 15.25f * density, paint);
        paint.setShader(null);
    }

    @Override public boolean isStateful() { return true; }

    @Override protected boolean onStateChange(int[] states) {
        boolean nextSelected = false, nextFocused = false, nextPressed = false, nextEnabled = false;
        for (int state : states) {
            if (state == android.R.attr.state_selected) nextSelected = true;
            if (state == android.R.attr.state_focused) nextFocused = true;
            if (state == android.R.attr.state_pressed) nextPressed = true;
            if (state == android.R.attr.state_enabled) nextEnabled = true;
        }
        boolean changed = selected != nextSelected || focused != nextFocused
                || pressed != nextPressed || enabled != nextEnabled;
        if (changed) {
            selected = nextSelected; focused = nextFocused; pressed = nextPressed; enabled = nextEnabled;
            rebuildMaterial();
        }
        return changed;
    }

    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public int getAlpha() { return alpha; }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @SuppressWarnings("deprecation")
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
