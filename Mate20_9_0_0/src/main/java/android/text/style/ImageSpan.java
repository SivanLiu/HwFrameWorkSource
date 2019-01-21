package android.text.style;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;

public class ImageSpan extends DynamicDrawableSpan {
    private Uri mContentUri;
    private Context mContext;
    private Drawable mDrawable;
    private int mResourceId;
    private String mSource;

    @Deprecated
    public ImageSpan(Bitmap b) {
        this(null, b, 0);
    }

    @Deprecated
    public ImageSpan(Bitmap b, int verticalAlignment) {
        this(null, b, verticalAlignment);
    }

    public ImageSpan(Context context, Bitmap bitmap) {
        this(context, bitmap, 0);
    }

    public ImageSpan(Context context, Bitmap bitmap, int verticalAlignment) {
        Drawable bitmapDrawable;
        super(verticalAlignment);
        this.mContext = context;
        if (context != null) {
            bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
        } else {
            bitmapDrawable = new BitmapDrawable(bitmap);
        }
        this.mDrawable = bitmapDrawable;
        int width = this.mDrawable.getIntrinsicWidth();
        int height = this.mDrawable.getIntrinsicHeight();
        this.mDrawable.setBounds(0, 0, width > 0 ? width : 0, height > 0 ? height : 0);
    }

    public ImageSpan(Drawable drawable) {
        this(drawable, 0);
    }

    public ImageSpan(Drawable drawable, int verticalAlignment) {
        super(verticalAlignment);
        this.mDrawable = drawable;
    }

    public ImageSpan(Drawable drawable, String source) {
        this(drawable, source, 0);
    }

    public ImageSpan(Drawable drawable, String source, int verticalAlignment) {
        super(verticalAlignment);
        this.mDrawable = drawable;
        this.mSource = source;
    }

    public ImageSpan(Context context, Uri uri) {
        this(context, uri, 0);
    }

    public ImageSpan(Context context, Uri uri, int verticalAlignment) {
        super(verticalAlignment);
        this.mContext = context;
        this.mContentUri = uri;
        this.mSource = uri.toString();
    }

    public ImageSpan(Context context, int resourceId) {
        this(context, resourceId, 0);
    }

    public ImageSpan(Context context, int resourceId, int verticalAlignment) {
        super(verticalAlignment);
        this.mContext = context;
        this.mResourceId = resourceId;
    }

    public Drawable getDrawable() {
        Drawable drawable = null;
        if (this.mDrawable != null) {
            return this.mDrawable;
        }
        if (this.mContentUri != null) {
            try {
                InputStream is = this.mContext.getContentResolver().openInputStream(this.mContentUri);
                drawable = new BitmapDrawable(this.mContext.getResources(), BitmapFactory.decodeStream(is));
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                is.close();
                return drawable;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to loaded content ");
                stringBuilder.append(this.mContentUri);
                Log.e("ImageSpan", stringBuilder.toString(), e);
                return drawable;
            }
        }
        try {
            drawable = this.mContext.getDrawable(this.mResourceId);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        } catch (Exception e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to find resource: ");
            stringBuilder2.append(this.mResourceId);
            Log.e("ImageSpan", stringBuilder2.toString());
            return drawable;
        }
    }

    public String getSource() {
        return this.mSource;
    }
}
