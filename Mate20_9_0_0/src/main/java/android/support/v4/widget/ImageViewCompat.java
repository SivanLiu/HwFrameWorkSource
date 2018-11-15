package android.support.v4.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

public class ImageViewCompat {
    @Nullable
    public static ColorStateList getImageTintList(@NonNull ImageView view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getImageTintList();
        }
        return view instanceof TintableImageSourceView ? ((TintableImageSourceView) view).getSupportImageTintList() : null;
    }

    public static void setImageTintList(@NonNull ImageView view, @Nullable ColorStateList tintList) {
        if (VERSION.SDK_INT >= 21) {
            view.setImageTintList(tintList);
            if (VERSION.SDK_INT == 21) {
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() == null || view.getImageTintMode() == null) ? false : true;
                if (imageViewDrawable != null && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintList(tintList);
        }
    }

    @Nullable
    public static Mode getImageTintMode(@NonNull ImageView view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getImageTintMode();
        }
        return view instanceof TintableImageSourceView ? ((TintableImageSourceView) view).getSupportImageTintMode() : null;
    }

    public static void setImageTintMode(@NonNull ImageView view, @Nullable Mode mode) {
        if (VERSION.SDK_INT >= 21) {
            view.setImageTintMode(mode);
            if (VERSION.SDK_INT == 21) {
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() == null || view.getImageTintMode() == null) ? false : true;
                if (imageViewDrawable != null && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintMode(mode);
        }
    }

    private ImageViewCompat() {
    }
}
