package com.opablosantanaa.cambiopablo.ui;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.animation.Animator;

public final class JellyTabsLayout extends LinearLayout {
    private int selectedIndex = -1;
    private int measuredWidth;
    private AnimatorSet motion;

    public JellyTabsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= getChildCount() || index == selectedIndex) return;
        boolean animate = selectedIndex >= 0 && getWidth() > 0 && MotionPolicy.enabled(getContext());
        selectedIndex = index;
        applySelection(animate);
    }

    private void applySelection(boolean animate) {
        if (motion != null) motion.cancel();
        ArrayList<Animator> animations = new ArrayList<>();
        float density = getResources().getDisplayMetrics().density;
        float push = getChildAt(selectedIndex).getWidth() * .04f + 2 * density;
        float[] initialWeights = new float[getChildCount()];
        float[] targetWeights = new float[getChildCount()];
        float available = Math.max(1, getWidth() - getPaddingLeft() - getPaddingRight());
        float minimum = 48 * density;
        float expanded = Math.max(available * .5f,
                ((JellyTabView) getChildAt(selectedIndex)).preferredExpandedWidth());
        expanded = Math.min(expanded, Math.max(minimum, available - minimum * (getChildCount() - 1)));
        float collapsed = Math.max(minimum, (available - expanded) / Math.max(1, getChildCount() - 1));
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            initialWeights[i] = params.weight;
            targetWeights[i] = i == selectedIndex ? expanded / available : collapsed / available;
        }
        float total = 0;
        for (float weight : initialWeights) total += weight;
        for (int i = 0; i < initialWeights.length; i++) initialWeights[i] /= Math.max(.001f, total);
        if (animate) {
            ValueAnimator widths = ValueAnimator.ofFloat(0, 1);
            widths.setDuration(360);
            widths.setInterpolator(new android.view.animation.DecelerateInterpolator(2f));
            widths.addUpdateListener(a -> {
                float progress = (float) a.getAnimatedValue();
                for (int i = 0; i < getChildCount(); i++) {
                    LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
                    params.weight = initialWeights[i] + (targetWeights[i] - initialWeights[i]) * progress;
                    getChildAt(i).setLayoutParams(params);
                }
            });
            animations.add(widths);
        }
        int direction = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? -1 : 1;
        for (int i = 0; i < getChildCount(); i++) {
            View chip = getChildAt(i);
            JellyTabView tab = (JellyTabView) chip;
            float reveal = i == selectedIndex ? 1 : 0;
            if (animate) {
                ValueAnimator label = ValueAnimator.ofFloat(tab.getLabelReveal(), reveal);
                label.addUpdateListener(a -> tab.setLabelReveal((float) a.getAnimatedValue()));
                label.setDuration(240);
                animations.add(label);
            } else {
                tab.setLabelReveal(reveal);
                LayoutParams params = (LayoutParams) chip.getLayoutParams();
                params.weight = targetWeights[i]; chip.setLayoutParams(params);
            }
            float scale = i == selectedIndex ? 1.08f : .98f;
            float x = Integer.signum(i - selectedIndex) * direction * push;
            if (!animate) {
                chip.setTranslationX(x); chip.setScaleX(scale); chip.setScaleY(scale);
                continue;
            }
            long delay = Math.abs(i - selectedIndex) * 22L;
            ObjectAnimator shift = ObjectAnimator.ofFloat(chip, View.TRANSLATION_X, x);
            ObjectAnimator horizontal = ObjectAnimator.ofFloat(chip, View.SCALE_X, scale);
            ObjectAnimator vertical = ObjectAnimator.ofFloat(chip, View.SCALE_Y, scale);
            shift.setInterpolator(new OvershootInterpolator(.8f));
            horizontal.setInterpolator(new OvershootInterpolator(1.5f));
            vertical.setInterpolator(new OvershootInterpolator(.7f));
            shift.setDuration(420); horizontal.setDuration(420); vertical.setDuration(480);
            shift.setStartDelay(delay); horizontal.setStartDelay(delay); vertical.setStartDelay(delay + 35);
            animations.add(shift); animations.add(horizontal); animations.add(vertical);
        }
        if (animate) { motion = new AnimatorSet(); motion.playTogether(animations); motion.start(); }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (selectedIndex >= 0 && (measuredWidth != getWidth() || changed)) {
            measuredWidth = getWidth(); applySelection(false);
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (motion != null) motion.cancel();
        super.onDetachedFromWindow();
    }
}
