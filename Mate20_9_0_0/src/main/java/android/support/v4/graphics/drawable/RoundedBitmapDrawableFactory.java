package android.support.v4.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.BitmapCompat;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import java.io.InputStream;

public final class RoundedBitmapDrawableFactory {
    private static final String TAG = "RoundedBitmapDrawableFa";

    private static class DefaultRoundedBitmapDrawable extends RoundedBitmapDrawable {
        DefaultRoundedBitmapDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }

        public void setMipMap(boolean mipMap) {
            if (this.mBitmap != null) {
                BitmapCompat.setHasMipMap(this.mBitmap, mipMap);
                invalidateSelf();
            }
        }

        public boolean hasMipMap() {
            return this.mBitmap != null && BitmapCompat.hasMipMap(this.mBitmap);
        }

        void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight, Rect bounds, Rect outRect) {
            GravityCompat.apply(gravity, bitmapWidth, bitmapHeight, bounds, outRect, 0);
        }
    }

    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @Nullable Bitmap bitmap) {
        if (VERSION.SDK_INT >= 21) {
            return new RoundedBitmapDrawable21(res, bitmap);
        }
        return new DefaultRoundedBitmapDrawable(res, bitmap);
    }

    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @NonNull String filepath) {
        RoundedBitmapDrawable drawable = create(res, BitmapFactory.decodeFile(filepath));
        if (drawable.getBitmap() == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RoundedBitmapDrawable cannot decode ");
            stringBuilder.append(filepath);
            Log.w(str, stringBuilder.toString());
        }
        return drawable;
    }

    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @NonNull InputStream is) {
        RoundedBitmapDrawable drawable = create(res, BitmapFactory.decodeStream(is));
        if (drawable.getBitmap() == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RoundedBitmapDrawable cannot decode ");
            stringBuilder.append(is);
            Log.w(str, stringBuilder.toString());
        }
        return drawable;
    }

    private RoundedBitmapDrawableFactory() {
    }
}
