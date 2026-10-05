package com.opablosantanaa.cambiopablo.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public final class DarkVeilView extends TextureView implements TextureView.SurfaceTextureListener {
    private DarkVeilRenderer renderer;
    private boolean hostActive;
    private boolean windowVisible;

    public DarkVeilView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpaque(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setSurfaceTextureListener(this);
    }

    public void setRunning(boolean active) {
        hostActive = active;
        updateRunning();
    }

    private void updateRunning() {
        if (renderer != null) renderer.setRunning(hostActive && windowVisible);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        windowVisible = visibility == View.VISIBLE;
        updateRunning();
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderer = new DarkVeilRenderer(getContext().getApplicationContext(), surface, width, height);
        updateRunning();
        renderer.start();
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (renderer != null) renderer.resize(width, height);
    }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (renderer != null) { renderer.shutdown(); renderer = null; }
        // The GL thread releases this texture after releasing its EGL window.
        return false;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
}
