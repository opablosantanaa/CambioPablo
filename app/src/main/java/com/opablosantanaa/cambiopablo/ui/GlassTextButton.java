package com.opablosantanaa.cambiopablo.ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;


public final class GlassTextButton extends AppCompatTextView {
    public GlassTextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundTintList(null);
        setBackground(new GlassSurfaceDrawable(getResources().getDisplayMetrics().density, false));
    }
}
