package com.android.internal.app;

import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import com.android.internal.colorextraction.types.Tonal;

public class PlatLogoActivity extends Activity {
    TimeAnimator anim;
    PBackground bg;
    FrameLayout layout;

    private class PBackground extends Drawable {
        private int darkest;
        private float dp;
        private float maxRadius;
        private float offset;
        private int[] palette;
        private float radius;
        private float x;
        private float y;

        public PBackground() {
            randomizePalette();
        }

        public void setRadius(float r) {
            this.radius = Math.max(48.0f * this.dp, r);
        }

        public void setPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void setOffset(float o) {
            this.offset = o;
        }

        public float lum(int rgb) {
            return (((((float) Color.red(rgb)) * 299.0f) + (((float) Color.green(rgb)) * 587.0f)) + (((float) Color.blue(rgb)) * 114.0f)) / 1000.0f;
        }

        public void randomizePalette() {
            int slots = ((int) (Math.random() * 2.0d)) + 2;
            float[] color = new float[]{((float) Math.random()) * 360.0f, 1.0f, 1.0f};
            this.palette = new int[slots];
            this.darkest = 0;
            for (int i = 0; i < slots; i++) {
                this.palette[i] = Color.HSVToColor(color);
                color[0] = color[0] + (360.0f / ((float) slots));
                if (lum(this.palette[i]) < lum(this.palette[this.darkest])) {
                    this.darkest = i;
                }
            }
            StringBuilder str = new StringBuilder();
            int length = this.palette.length;
            for (int i2 = 0; i2 < length; i2++) {
                str.append(String.format("#%08x ", new Object[]{Integer.valueOf(r3[i2])}));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("color palette: ");
            stringBuilder.append(str);
            Log.v("PlatLogoActivity", stringBuilder.toString());
        }

        public void draw(Canvas canvas) {
            Canvas canvas2 = canvas;
            if (this.dp == 0.0f) {
                this.dp = PlatLogoActivity.this.getResources().getDisplayMetrics().density;
            }
            float width = (float) canvas.getWidth();
            float height = (float) canvas.getHeight();
            float p = 2.0f;
            if (this.radius == 0.0f) {
                setPosition(width / 2.0f, height / 2.0f);
                setRadius(width / 6.0f);
            }
            float inner_w = this.radius * 0.667f;
            Paint paint = new Paint();
            paint.setStrokeCap(Cap.BUTT);
            canvas2.translate(this.x, this.y);
            Path p2 = new Path();
            p2.moveTo(-this.radius, height);
            p2.lineTo(-this.radius, 0.0f);
            p2.arcTo(-this.radius, -this.radius, this.radius, this.radius, -180.0f, 270.0f, false);
            p2.lineTo(-this.radius, this.radius);
            float w = ((float) Math.max(canvas.getWidth(), canvas.getHeight())) * 1.414f;
            paint.setStyle(Style.FILL);
            int i = 0;
            float w2 = w;
            while (true) {
                int i2 = i;
                Path p3;
                if (w2 > (this.radius * p) + (inner_w * p)) {
                    paint.setColor(this.palette[i2 % this.palette.length] | Tonal.MAIN_COLOR_DARK);
                    float f = (-w2) / p;
                    float f2 = (-w2) / p;
                    float f3 = w2 / p;
                    float f4 = w2 / p;
                    p3 = p2;
                    canvas2.drawOval(f, f2, f3, f4, paint);
                    w2 = (float) (((double) w2) - (((double) inner_w) * (1.100000023841858d + Math.sin((double) (((((float) i2) / 20.0f) + this.offset) * 3.14159f)))));
                    i = i2 + 1;
                    p2 = p3;
                    p = 2.0f;
                } else {
                    p3 = p2;
                    paint.setColor(this.palette[(this.darkest + 1) % this.palette.length] | Tonal.MAIN_COLOR_DARK);
                    canvas2.drawOval(-this.radius, -this.radius, this.radius, this.radius, paint);
                    p3.reset();
                    p3.moveTo(-this.radius, height);
                    p3.lineTo(-this.radius, 0.0f);
                    p3.arcTo(-this.radius, -this.radius, this.radius, this.radius, -180.0f, 270.0f, false);
                    p3.lineTo((-this.radius) + inner_w, this.radius);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(2.0f * inner_w);
                    paint.setColor(this.palette[this.darkest]);
                    canvas2.drawPath(p3, paint);
                    paint.setStrokeWidth(inner_w);
                    paint.setColor(-1);
                    canvas2.drawPath(p3, paint);
                    return;
                }
            }
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public int getOpacity() {
            return 0;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.layout = new FrameLayout(this);
        setContentView(this.layout);
        this.bg = new PBackground();
        this.layout.setBackground(this.bg);
        this.layout.setOnTouchListener(new OnTouchListener() {
            final PointerCoords pc0 = new PointerCoords();
            final PointerCoords pc1 = new PointerCoords();

            public boolean onTouch(View v, MotionEvent event) {
                int actionMasked = event.getActionMasked();
                if ((actionMasked == 0 || actionMasked == 2) && event.getPointerCount() > 1) {
                    event.getPointerCoords(0, this.pc0);
                    event.getPointerCoords(1, this.pc1);
                    PlatLogoActivity.this.bg.setRadius(((float) Math.hypot((double) (this.pc0.x - this.pc1.x), (double) (this.pc0.y - this.pc1.y))) / 2.0f);
                }
                return true;
            }
        });
    }

    public void onStart() {
        super.onStart();
        this.bg.randomizePalette();
        this.anim = new TimeAnimator();
        this.anim.setTimeListener(new TimeListener() {
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                PlatLogoActivity.this.bg.setOffset(((float) totalTime) / 60000.0f);
                PlatLogoActivity.this.bg.invalidateSelf();
            }
        });
        this.anim.start();
    }

    public void onStop() {
        if (this.anim != null) {
            this.anim.cancel();
            this.anim = null;
        }
        super.onStop();
    }
}
