package com.opablosantanaa.cambiopablo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.view.TextureView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.opablosantanaa.cambiopablo.ui.DarkVeilView;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Reproduces a buffer reset without a change in the visible view's dimensions. */
@RunWith(AndroidJUnit4.class)
public class DarkVeilResumeTest {
    @Test public void resumingAfterBufferResetKeepsBackgroundFullScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicInteger frames = new AtomicInteger();
            scenario.onActivity(activity -> {
                DarkVeilView view = activity.findViewById(R.id.darkVeil);
                TextureView.SurfaceTextureListener delegate = view.getSurfaceTextureListener();
                view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) {
                        delegate.onSurfaceTextureAvailable(s, w, h);
                    }
                    public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {
                        delegate.onSurfaceTextureSizeChanged(s, w, h);
                    }
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture s) {
                        return delegate.onSurfaceTextureDestroyed(s);
                    }
                    public void onSurfaceTextureUpdated(SurfaceTexture s) {
                        frames.incrementAndGet(); delegate.onSurfaceTextureUpdated(s);
                    }
                });
            });
            awaitFrames(frames, 3);
            assertFullCoverage(scenario);
            for (int cycle = 0; cycle < 3; cycle++) {
                scenario.moveToState(Lifecycle.State.STARTED);
                scenario.onActivity(activity -> {
                    DarkVeilView view = activity.findViewById(R.id.darkVeil);
                    SurfaceTexture surface = view.getSurfaceTexture();
                    assertTrue("The retained texture must be available", surface != null);
                    // Android's texture layer resets the default buffer to the view size.
                    surface.setDefaultBufferSize(view.getWidth(), view.getHeight());
                    view.getSurfaceTextureListener().onSurfaceTextureSizeChanged(
                            surface, view.getWidth(), view.getHeight());
                });
                int beforeResume = frames.get();
                scenario.moveToState(Lifecycle.State.RESUMED);
                awaitFrames(frames, beforeResume + 5);
                assertFullCoverage(scenario);
            }
        }
    }

    private void awaitFrames(AtomicInteger frames, int target) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (frames.get() < target && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
        assertTrue("The background must keep presenting frames", frames.get() >= target);
    }

    private void assertFullCoverage(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Bitmap> bitmap = new AtomicReference<>();
        scenario.onActivity(activity -> {
            DarkVeilView view = activity.findViewById(R.id.darkVeil);
            bitmap.set(view.getBitmap(96, 192));
        });
        Bitmap frame = bitmap.get();
        assertTrue("A rendered frame must be available", frame != null);
        try {
            // The shader writes opaque pixels everywhere; a stale viewport leaves empty margins.
            for (int y = 8; y < frame.getHeight(); y += 16) {
                for (int x = 8; x < frame.getWidth(); x += 16) {
                    assertEquals("Unpainted background at " + x + "," + y,
                            255, Color.alpha(frame.getPixel(x, y)));
                }
            }
        } finally { frame.recycle(); }
    }
}
