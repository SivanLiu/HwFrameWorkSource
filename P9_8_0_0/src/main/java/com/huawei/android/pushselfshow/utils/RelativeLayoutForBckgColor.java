package com.huawei.android.pushselfshow.utils;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.huawei.android.pushagent.a.a.c;

public class RelativeLayoutForBckgColor extends RelativeLayout {
    private WallpaperManager a;
    private Drawable b;

    public RelativeLayoutForBckgColor(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        a();
    }

    public RelativeLayoutForBckgColor(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        a();
    }

    private static Object a(WallpaperManager wallpaperManager, Rect rect) {
        Object -l_2_R = null;
        Object -l_3_R;
        try {
            -l_3_R = Class.forName("com.huawei.android.app.WallpaperManagerEx");
            -l_2_R = -l_3_R.getDeclaredMethod("getBlurBitmap", new Class[]{WallpaperManager.class, Rect.class}).invoke(-l_3_R, new Object[]{wallpaperManager, rect});
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", " WallpaperManagerEx getBlurBitmap wrong " + -l_3_R2.toString());
        } catch (Object -l_3_R22) {
            c.d("PushSelfShowLog", " WallpaperManagerEx getBlurBitmap wrong " + -l_3_R22.toString());
        } catch (Object -l_3_R222) {
            c.d("PushSelfShowLog", " WallpaperManagerEx getBlurBitmap wrong " + -l_3_R222.toString());
        } catch (Object -l_3_R2222) {
            c.d("PushSelfShowLog", " WallpaperManagerEx getBlurBitmap wrong " + -l_3_R2222.toString());
        } catch (Object -l_3_R22222) {
            c.d("PushSelfShowLog", " WallpaperManagerEx getBlurBitmap wrong " + -l_3_R22222.toString());
        }
        return -l_2_R;
    }

    private void b() {
        int -l_1_I = getContext().getResources().getColor(d.f(getContext(), "hwpush_bgcolor"));
        try {
            Object -l_2_R = new int[2];
            getLocationOnScreen(-l_2_R);
            Object -l_3_R = new Rect(-l_2_R[0], -l_2_R[1], -l_2_R[0] + getWidth(), -l_2_R[1] + getHeight());
            if (-l_3_R.width() <= 0 || -l_3_R.height() <= 0) {
                return;
            }
            if (a.d()) {
                int -l_4_I = a.j(getContext());
                if (-l_4_I != 0) {
                    setBackgroundColor(-l_4_I);
                } else {
                    setBackgroundColor(-l_1_I);
                }
            } else if (a(this.a, -l_3_R) == null) {
                c.d("PushSelfShowLog", "not emui 3.0,can not use wallpaper as background");
                setBackgroundColor(-l_1_I);
            } else {
                this.b = new BitmapDrawable((Bitmap) a(this.a, -l_3_R));
                setBackgroundDrawable(this.b);
            }
        } catch (NotFoundException e) {
            c.d("PushSelfShowLog", "setBlurWallpaperBackground error, use default backgroud");
            setBackgroundColor(-l_1_I);
        } catch (Exception e2) {
            c.d("PushSelfShowLog", "setBlurWallpaperBackground error, use default backgroud");
            setBackgroundColor(-l_1_I);
        }
    }

    @SuppressLint({"ServiceCast"})
    public final void a() {
        this.a = (WallpaperManager) getContext().getSystemService("wallpaper");
    }

    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        b();
    }
}
