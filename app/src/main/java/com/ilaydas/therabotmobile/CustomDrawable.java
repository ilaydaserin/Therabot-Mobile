package com.ilaydas.therabotmobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class CustomDrawable extends Drawable {

    private Drawable emojiDrawable;

    public CustomDrawable(@NonNull Context context, int emojiResId) {
        emojiDrawable = ContextCompat.getDrawable(context, emojiResId);
        if (emojiDrawable != null) {
            // Drawable'ın bounds'unu burda set et, bu sayede null drawable'ın oluşturduğu problemleri azaltır
            emojiDrawable.setBounds(0, 0, emojiDrawable.getIntrinsicWidth(), emojiDrawable.getIntrinsicHeight());
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (emojiDrawable == null) return;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // İkon boyutunu ayarla
        int size = canvasWidth / 3; // Daha da küçültmek için /4 olabilir
        int left = 0;
        int top = canvasHeight - size;

        emojiDrawable.setBounds(left, top, left + size, top + size);
        emojiDrawable.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        if (emojiDrawable != null) emojiDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
        if (emojiDrawable != null) emojiDrawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return emojiDrawable != null ? emojiDrawable.getOpacity() : PixelFormat.TRANSLUCENT;
    }
}
