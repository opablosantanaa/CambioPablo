package com.opablosantanaa.cambiopablo.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.opablosantanaa.cambiopablo.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

final class DarkVeilRenderer extends Thread {
    private static final long FRAME_MS = 33;
    private final Context context;
    private final SurfaceTexture texture;
    private final Object stateLock = new Object();
    private final FloatBuffer triangle = ByteBuffer.allocateDirect(6 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private volatile boolean alive = true;
    private volatile boolean running;
    private volatile int requestedWidth, requestedHeight;
    private long surfaceRevision;
    private final int[] surfaceSize = new int[2];
    private int width, height, program;
    private EGLConfig windowConfig;
    private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    DarkVeilRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super("DarkVeil-renderer");
        this.context = context; this.texture = texture;
        resize(width, height);
        triangle.put(new float[]{-1, -1, 3, -1, -1, 3}).position(0);
    }

    void resize(int width, int height) {
        // Cap the long edge: the soft background does not need display-resolution shading.
        float scale = Math.min(1f, 640f / Math.max(1, Math.max(width, height)));
        synchronized (stateLock) {
            requestedWidth = Math.max(1, Math.round(width * scale));
            requestedHeight = Math.max(1, Math.round(height * scale));
            // TextureView can reset its buffer even when the view dimensions did not change.
            surfaceRevision++;
            stateLock.notifyAll();
        }
    }

    void setRunning(boolean value) {
        synchronized (stateLock) {
            if (value && !running) surfaceRevision++;
            running = value;
            stateLock.notifyAll();
        }
    }

    void shutdown() {
        alive = false;
        synchronized (stateLock) { stateLock.notifyAll(); }
        interrupt();
    }

    @Override public void run() {
        try {
            initializeEgl();
            program = link(readAsset("darkveil.vert"), readAsset("darkveil.frag"));
            GLES20.glUseProgram(program);
            setColor("uBackground", R.color.background);
            setColor("uPrimary", R.color.primary);
            setColor("uAccent", R.color.accent);
            int position = GLES20.glGetAttribLocation(program, "position");
            GLES20.glEnableVertexAttribArray(position);
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, triangle);
            int resolution = GLES20.glGetUniformLocation(program, "uResolution");
            int timeUniform = GLES20.glGetUniformLocation(program, "uTime");
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uWarp"), .35f);
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean animate = true, wasAnimating = true, drawn = false;
            long policyCheck = 0, previous = SystemClock.uptimeMillis();
            long appliedRevision = -1;
            float time = 0;
            while (alive) {
                long revision;
                int targetWidth, targetHeight;
                synchronized (stateLock) {
                    while (alive && !running) {
                        stateLock.wait(); previous = SystemClock.uptimeMillis();
                    }
                    revision = surfaceRevision;
                    targetWidth = requestedWidth;
                    targetHeight = requestedHeight;
                }
                if (!alive) break;
                long frameStart = SystemClock.uptimeMillis();
                if (frameStart >= policyCheck) {
                    animate = MotionPolicy.enabled(context) && (power == null || !power.isPowerSaveMode());
                    policyCheck = frameStart + 1000;
                }
                boolean refreshed = appliedRevision != revision;
                if (refreshed) {
                    recreateWindowSurface(targetWidth, targetHeight);
                    appliedRevision = revision;
                }
                boolean resized = updateViewport(resolution);
                if (animate) time += Math.min(.1f, (frameStart - previous) / 1000f) * .5f;
                previous = frameStart;
                if (!drawn || refreshed || resized || animate || animate != wasAnimating) {
                    GLES20.glUniform1f(timeUniform, animate ? time : 0);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
                    if (!EGL14.eglSwapBuffers(display, eglSurface)) throw new IllegalStateException("EGL swap failed");
                    drawn = true;
                }
                wasAnimating = animate;
                long delay = animate ? Math.max(1, FRAME_MS - (SystemClock.uptimeMillis() - frameStart)) : 1000;
                synchronized (stateLock) { if (alive && running) stateLock.wait(delay); }
            }
        } catch (InterruptedException ignored) {
            // Normal disposal when Android destroys the texture.
        } catch (IOException | RuntimeException error) {
            Log.w("DarkVeil", "Background unavailable; keeping the solid theme background", error);
        } finally {
            release();
            texture.release();
        }
    }

    private void setColor(String uniform, int resource) {
        int color = ContextCompat.getColor(context, resource);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, uniform),
                android.graphics.Color.red(color) / 255f,
                android.graphics.Color.green(color) / 255f,
                android.graphics.Color.blue(color) / 255f);
    }

    private void initializeEgl() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        if (display == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(display, version, 0, version, 1))
            throw new IllegalStateException("EGL initialization failed");
        int[] attributes = {EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT, EGL14.EGL_NONE};
        EGLConfig[] config = new EGLConfig[1]; int[] count = new int[1];
        if (!EGL14.eglChooseConfig(display, attributes, 0, config, 0, 1, count, 0) || count[0] == 0)
            throw new IllegalStateException("EGL configuration unavailable");
        eglContext = EGL14.eglCreateContext(display, config[0], EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
        windowConfig = config[0];
        recreateWindowSurface(requestedWidth, requestedHeight);
    }

    private void recreateWindowSurface(int bufferWidth, int bufferHeight) {
        // Keep the context/program, replacing only the EGL window so buffer sizing takes effect.
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(display, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        texture.setDefaultBufferSize(bufferWidth, bufferHeight);
        eglSurface = EGL14.eglCreateWindowSurface(display, windowConfig, texture, new int[]{EGL14.EGL_NONE}, 0);
        if (eglContext == EGL14.EGL_NO_CONTEXT || eglSurface == EGL14.EGL_NO_SURFACE
                || !EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext))
            throw new IllegalStateException("EGL surface unavailable");
    }

    private boolean updateViewport(int resolution) {
        // Use the actual drawing buffer, not a cached assumption about its dimensions.
        if (!EGL14.eglQuerySurface(display, eglSurface, EGL14.EGL_WIDTH, surfaceSize, 0)
                || !EGL14.eglQuerySurface(display, eglSurface, EGL14.EGL_HEIGHT, surfaceSize, 1))
            throw new IllegalStateException("EGL dimensions unavailable");
        int actualWidth = Math.max(1, surfaceSize[0]);
        int actualHeight = Math.max(1, surfaceSize[1]);
        boolean changed = width != actualWidth || height != actualHeight;
        width = actualWidth; height = actualHeight;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUniform2f(resolution, width, height);
        return changed;
    }

    private String readAsset(String name) throws IOException {
        StringBuilder source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(name), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) source.append(line).append('\n');
        }
        return source.toString();
    }

    private int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source); GLES20.glCompileShader(shader);
        int[] status = new int[1]; GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String reason = GLES20.glGetShaderInfoLog(shader); GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compilation: " + reason);
        }
        return shader;
    }

    private int link(String vertexSource, String fragmentSource) {
        int vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragment = 0, linked = 0;
        try {
            fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            linked = GLES20.glCreateProgram();
            GLES20.glAttachShader(linked, vertex); GLES20.glAttachShader(linked, fragment);
            GLES20.glLinkProgram(linked);
            int[] status = new int[1]; GLES20.glGetProgramiv(linked, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == 0) {
                String reason = GLES20.glGetProgramInfoLog(linked); GLES20.glDeleteProgram(linked);
                throw new IllegalStateException("Shader linking: " + reason);
            }
            return linked;
        } finally {
            GLES20.glDeleteShader(vertex); if (fragment != 0) GLES20.glDeleteShader(fragment);
        }
    }

    private void release() {
        if (display == EGL14.EGL_NO_DISPLAY) return;
        if (program != 0) GLES20.glDeleteProgram(program);
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface);
        if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext);
        EGL14.eglReleaseThread(); EGL14.eglTerminate(display);
    }
}
