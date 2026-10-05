package com.opablosantanaa.cambiopablo.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;
import com.opablosantanaa.cambiopablo.R;

public final class GlideCurrencyPicker {
    public interface OnCurrencySelected { void onSelected(int index); }
    private PopupWindow popup;

    public void dismiss() {
        if (popup != null) { popup.dismiss(); popup = null; }
    }

    public void show(TextView anchor, String[] codes, String[] names, int selected,
                     OnCurrencySelected listener) {
        dismiss();
        Context context = anchor.getContext();
        Menu menu = new Menu(context, codes, names, selected, index -> {
            dismiss();
            if (index != selected) listener.onSelected(index);
        });
        ScrollView surface = new ScrollView(context);
        surface.setFillViewport(false);
        surface.addView(menu, new ViewGroup.LayoutParams(-1, -2));
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(context, R.color.panel)); background.setCornerRadius(dp(context, 16));
        background.setStroke(dp(context, 1), ContextCompat.getColor(context, R.color.border));
        surface.setBackground(background); surface.setClipToOutline(true);
        Rect frame = new Rect(); anchor.getWindowVisibleDisplayFrame(frame);
        int[] location = new int[2]; anchor.getLocationOnScreen(location);
        int above = location[1] - frame.top;
        int below = frame.bottom - location[1] - anchor.getHeight();
        int width = Math.min(dp(context, 300), frame.width() - dp(context, 24));
        int height = Math.min(menu.rowHeight * codes.length + dp(context, 8),
                Math.max(dp(context, 96), Math.max(above, below) - dp(context, 12)));
        popup = new PopupWindow(surface, width, height, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true); popup.setElevation(dp(context, 8));
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setAnimationStyle(0);
        menu.cancel = this::dismiss;
        popup.setOnDismissListener(() -> {
            surface.animate().cancel(); menu.stopAnimation(); anchor.setActivated(false);
            anchor.requestFocus();
        });
        anchor.setActivated(true);
        popup.showAsDropDown(anchor, 0, dp(context, 6), Gravity.START);
        boolean aboveAnchor = popup.isAboveAnchor();
        surface.post(() -> {
            if (!surface.isAttachedToWindow()) return;
            menu.getChildAt(selected).requestFocus();
            surface.smoothScrollTo(0, Math.max(0, menu.getChildAt(selected).getTop() - height / 2));
            if (MotionPolicy.enabled(context)) {
                surface.setPivotX(0); surface.setPivotY(aboveAnchor ? surface.getHeight() : 0);
                surface.setAlpha(0); surface.setScaleX(.95f); surface.setScaleY(.95f);
                surface.animate().alpha(1).scaleX(1).scaleY(1).setDuration(180)
                        .setInterpolator(new DecelerateInterpolator(2f)).start();
            }
        });
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Menu extends LinearLayout {
        private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String[] codes, names;
        private final OnCurrencySelected listener;
        private final int rowHeight;
        private int active;
        private float highlightTop;
        private ValueAnimator glide;
        private boolean tracking;
        private Runnable cancel;

        Menu(Context context, String[] codes, String[] names, int selected, OnCurrencySelected listener) {
            super(context);
            this.codes = codes; this.names = names; this.listener = listener; active = selected;
            rowHeight = dp(context, Math.max(56, Math.round(40 * getResources().getConfiguration().fontScale)));
            setOrientation(VERTICAL); setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
            highlight.setColor(ContextCompat.getColor(context, R.color.menu_highlight));
            for (int i = 0; i < codes.length; i++) {
                final int index = i;
                TextView row = new TextView(context);
                row.setText(context.getString(R.string.currency_option, codes[i], names[i]));
                row.setTextSize(15); row.setTextColor(ContextCompat.getColor(context, R.color.white));
                row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(context, 12), 0, dp(context, 12), 0);
                row.setMaxLines(2); row.setFocusableInTouchMode(true); row.setFocusable(true); row.setClickable(true); row.setSelected(i == selected);
                row.setOnClickListener(v -> listener.onSelected(index));
                row.setOnFocusChangeListener((v, focused) -> { if (focused) setActive(index, true); });
                addView(row, new LayoutParams(-1, rowHeight));
            }
            highlightTop = getPaddingTop() + rowHeight * selected;
        }

        private void setActive(int index, boolean animate) {
            if (index < 0 || index >= getChildCount()) return;
            if (active == index) return;
            active = index; stopAnimation();
            float target = getPaddingTop() + rowHeight * index;
            if (!animate || !MotionPolicy.enabled(getContext())) { highlightTop = target; invalidate(); return; }
            glide = ValueAnimator.ofFloat(highlightTop, target); glide.setDuration(220);
            glide.setInterpolator(new DecelerateInterpolator(2f));
            glide.addUpdateListener(a -> { highlightTop = (float) a.getAnimatedValue(); invalidate(); });
            glide.start();
        }

        private void stopAnimation() { if (glide != null) glide.cancel(); }

        @Override protected void dispatchDraw(Canvas canvas) {
            canvas.drawRoundRect(getPaddingLeft(), highlightTop, getWidth() - getPaddingRight(),
                    highlightTop + rowHeight, dp(getContext(), 12), dp(getContext(), 12), highlight);
            super.dispatchDraw(canvas);
        }

        private int rowAt(float y) {
            int index = (int) Math.floor((y - getPaddingTop()) / rowHeight);
            return index >= 0 && index < getChildCount() ? index : -1;
        }

        @Override public boolean dispatchTouchEvent(MotionEvent event) {
            int index = event.getX() >= 0 && event.getX() < getWidth() ? rowAt(event.getY()) : -1;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    tracking = true; getParent().requestDisallowInterceptTouchEvent(true);
                    setActive(index, true); return true;
                case MotionEvent.ACTION_MOVE:
                    if (tracking) {
                        ScrollView scroll = (ScrollView) getParent();
                        float visibleY = event.getY() - scroll.getScrollY();
                        int edge = dp(getContext(), 24);
                        if (visibleY < edge) scroll.scrollBy(0, -dp(getContext(), 12));
                        else if (visibleY > scroll.getHeight() - edge) scroll.scrollBy(0, dp(getContext(), 12));
                        setActive(index, true); return true;
                    } break;
                case MotionEvent.ACTION_UP:
                    if (tracking) {
                        tracking = false; getParent().requestDisallowInterceptTouchEvent(false);
                        if (index >= 0) getChildAt(index).performClick();
                        return true;
                    } break;
                case MotionEvent.ACTION_CANCEL:
                    tracking = false; getParent().requestDisallowInterceptTouchEvent(false); return true;
            }
            return super.dispatchTouchEvent(event);
        }

        @Override public boolean dispatchHoverEvent(MotionEvent event) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) setActive(rowAt(event.getY()), true);
            return super.dispatchHoverEvent(event);
        }

        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int next = active;
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_DOWN: next = (active + 1) % codes.length; break;
                    case KeyEvent.KEYCODE_DPAD_UP: next = (active + codes.length - 1) % codes.length; break;
                    case KeyEvent.KEYCODE_MOVE_HOME: next = 0; break;
                    case KeyEvent.KEYCODE_MOVE_END: next = codes.length - 1; break;
                    case KeyEvent.KEYCODE_ESCAPE: if (cancel != null) cancel.run(); return true;
                    default:
                        int unicode = event.getUnicodeChar();
                        if (unicode != 0 && Character.isLetter(unicode)) {
                            String prefix = String.valueOf((char) unicode).toLowerCase(Locale.ROOT);
                            for (int k = 1; k <= codes.length; k++) {
                                int index = (active + k) % codes.length;
                                if (codes[index].toLowerCase(Locale.ROOT).startsWith(prefix)
                                        || names[index].toLowerCase(Locale.ROOT).startsWith(prefix)) { next = index; break; }
                            }
                        } else return super.dispatchKeyEvent(event);
                }
                setActive(next, true); getChildAt(next).requestFocus(); return true;
            }
            return super.dispatchKeyEvent(event);
        }

        @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
    }
}
