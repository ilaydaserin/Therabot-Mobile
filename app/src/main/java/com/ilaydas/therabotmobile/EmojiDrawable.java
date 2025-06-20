package com.ilaydas.therabotmobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class EmojiDrawable extends Drawable {

    private final Paint paint;
    private final int color;

    public EmojiDrawable(Context context, int emojiResId, int color) {
        this.color = color;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        // Dairenin merkezi ve yarıçapı
        int width = getBounds().width();
        int height = getBounds().height();
        float radius = Math.min(width, height) / 4f;

        float cx = width / 2f;
        float cy = height / 2f;

        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
