package com.opablosantanaa.cambiopablo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

public final class JellyTabView extends AppCompatTextView {
    private float labelReveal;
    private int iconColor;
    private final TextPaint labelPaint = new TextPaint();
    private StaticLayout labelLayout;
    private int layoutWidth = -1;
    private float layoutTextSize;

    public JellyTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMaxLines(2);
        ViewCompat.setTooltipText(this, getText());
    }

    public float getLabelReveal() { return labelReveal; }

    public void setLabelReveal(float value) {
        labelReveal = Math.max(0, Math.min(1, value));
        invalidate();
    }

    public float preferredExpandedWidth() {
        return dp(28 + 8 + 24) + getPaint().measureText(getText().toString());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMinimumHeight(Math.max(dp(64), Math.round(getTextSize() * 2 + dp(16))));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private StaticLayout labelLayout(int width) {
        labelPaint.set(getPaint());
        if (labelLayout == null || layoutWidth != width || layoutTextSize != getTextSize()) {
            layoutWidth = width; layoutTextSize = getTextSize();
            labelLayout = StaticLayout.Builder.obtain(getText(), 0, getText().length(), labelPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
                    .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build();
        }
        return labelLayout;
    }
    @Override protected void onDraw(Canvas canvas) {
        Drawable icon = getCompoundDrawablesRelative()[0];
        if (icon == null) return;
        int iconSize = dp(28);
        int color = getCurrentTextColor();
        if (iconColor != color) { iconColor = color; DrawableCompat.setTint(icon, color); }
        int available = Math.max(1, getWidth() - dp(24) - iconSize - dp(8));
        int labelWidth = Math.max(1, Math.min(available,
                (int) Math.ceil(getPaint().measureText(getText().toString()))));
        StaticLayout label = labelLayout(labelWidth);
        labelPaint.setColor(color); labelPaint.setAlpha(Math.round(255 * labelReveal));
        float groupWidth = iconSize + labelReveal * (dp(8) + labelWidth);
        float left = (getWidth() - groupWidth) / 2f;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        float iconLeft = rtl ? left + groupWidth - iconSize : left;
        int saved = canvas.save();
        canvas.translate(iconLeft, (getHeight() - iconSize) / 2f);
        icon.setBounds(0, 0, iconSize, iconSize); icon.draw(canvas);
        canvas.restoreToCount(saved);
        if (labelReveal > 0) {
            saved = canvas.save();
            canvas.clipRect(dp(8), 0, getWidth() - dp(8), getHeight());
            canvas.translate(rtl ? left : left + iconSize + dp(8), (getHeight() - label.getHeight()) / 2f);
            label.draw(canvas); canvas.restoreToCount(saved);
        }
    }
}
