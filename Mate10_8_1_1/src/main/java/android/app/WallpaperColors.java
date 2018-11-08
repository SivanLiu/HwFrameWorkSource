package android.app;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Size;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.graphics.palette.Palette;
import com.android.internal.graphics.palette.Palette.Swatch;
import com.android.internal.graphics.palette.VariationalKMeansQuantizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WallpaperColors implements Parcelable {
    private static final float BRIGHT_IMAGE_MEAN_LUMINANCE = 0.75f;
    public static final Creator<WallpaperColors> CREATOR = new Creator<WallpaperColors>() {
        public WallpaperColors createFromParcel(Parcel in) {
            return new WallpaperColors(in);
        }

        public WallpaperColors[] newArray(int size) {
            return new WallpaperColors[size];
        }
    };
    private static final float DARK_PIXEL_LUMINANCE = 0.45f;
    private static final float DARK_THEME_MEAN_LUMINANCE = 0.25f;
    public static final int HINT_FROM_BITMAP = 4;
    public static final int HINT_SUPPORTS_DARK_TEXT = 1;
    public static final int HINT_SUPPORTS_DARK_THEME = 2;
    private static final int MAX_BITMAP_SIZE = 112;
    private static final float MAX_DARK_AREA = 0.05f;
    private static final int MAX_WALLPAPER_EXTRACTION_AREA = 12544;
    private static final float MIN_COLOR_OCCURRENCE = 0.05f;
    private int mColorHints;
    private final ArrayList<Color> mMainColors;

    public WallpaperColors(Parcel parcel) {
        this.mMainColors = new ArrayList();
        int count = parcel.readInt();
        for (int i = 0; i < count; i++) {
            this.mMainColors.add(Color.valueOf(parcel.readInt()));
        }
        this.mColorHints = parcel.readInt();
    }

    public static WallpaperColors fromDrawable(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            width = 112;
            height = 112;
        }
        Size optimalSize = calculateOptimalSize(width, height);
        Bitmap bitmap = Bitmap.createBitmap(optimalSize.getWidth(), optimalSize.getHeight(), Config.ARGB_8888);
        Canvas bmpCanvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(bmpCanvas);
        WallpaperColors colors = fromBitmap(bitmap);
        bitmap.recycle();
        return colors;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static WallpaperColors fromBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap can't be null");
        }
        int hints;
        boolean shouldRecycle = false;
        if (bitmap.getWidth() * bitmap.getHeight() > MAX_WALLPAPER_EXTRACTION_AREA) {
            shouldRecycle = true;
            Size optimalSize = calculateOptimalSize(bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, optimalSize.getWidth(), optimalSize.getHeight(), true);
        }
        ArrayList<Swatch> swatches = new ArrayList(Palette.from(bitmap).setQuantizer(new VariationalKMeansQuantizer()).maximumColorCount(5).clearFilters().resizeBitmapArea(MAX_WALLPAPER_EXTRACTION_AREA).generate().getSwatches());
        swatches.removeIf(new android.app.-$Lambda$xNlQtks0cIOkqsInCE_AAmZWgcY.AnonymousClass1(((float) (bitmap.getWidth() * bitmap.getHeight())) * 0.05f));
        swatches.sort(-$Lambda$xNlQtks0cIOkqsInCE_AAmZWgcY.$INST$0);
        int swatchesSize = swatches.size();
        Color primary = null;
        Color secondary = null;
        Color tertiary = null;
        int i = 0;
        while (i < swatchesSize) {
            Color color = Color.valueOf(((Swatch) swatches.get(i)).getRgb());
            switch (i) {
                case 0:
                    primary = color;
                    continue;
                case 1:
                    secondary = color;
                    continue;
                case 2:
                    tertiary = color;
                    continue;
                default:
                    break;
            }
            hints = calculateDarkHints(bitmap);
            if (shouldRecycle) {
                bitmap.recycle();
            }
            return new WallpaperColors(primary, secondary, tertiary, hints | 4);
        }
        hints = calculateDarkHints(bitmap);
        if (shouldRecycle) {
            bitmap.recycle();
        }
        return new WallpaperColors(primary, secondary, tertiary, hints | 4);
    }

    static /* synthetic */ boolean lambda$-android_app_WallpaperColors_6256(float minColorArea, Swatch s) {
        return ((float) s.getPopulation()) < minColorArea;
    }

    public WallpaperColors(Color primaryColor, Color secondaryColor, Color tertiaryColor) {
        this(primaryColor, secondaryColor, tertiaryColor, 0);
    }

    public WallpaperColors(Color primaryColor, Color secondaryColor, Color tertiaryColor, int colorHints) {
        if (primaryColor == null) {
            throw new IllegalArgumentException("Primary color should never be null.");
        }
        this.mMainColors = new ArrayList(3);
        this.mMainColors.add(primaryColor);
        if (secondaryColor != null) {
            this.mMainColors.add(secondaryColor);
        }
        if (tertiaryColor != null) {
            if (secondaryColor == null) {
                throw new IllegalArgumentException("tertiaryColor can't be specified when secondaryColor is null");
            }
            this.mMainColors.add(tertiaryColor);
        }
        this.mColorHints = colorHints;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        List<Color> mainColors = getMainColors();
        int count = mainColors.size();
        dest.writeInt(count);
        for (int i = 0; i < count; i++) {
            dest.writeInt(((Color) mainColors.get(i)).toArgb());
        }
        dest.writeInt(this.mColorHints);
    }

    public Color getPrimaryColor() {
        return (Color) this.mMainColors.get(0);
    }

    public Color getSecondaryColor() {
        return this.mMainColors.size() < 2 ? null : (Color) this.mMainColors.get(1);
    }

    public Color getTertiaryColor() {
        return this.mMainColors.size() < 3 ? null : (Color) this.mMainColors.get(2);
    }

    public List<Color> getMainColors() {
        return Collections.unmodifiableList(this.mMainColors);
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WallpaperColors other = (WallpaperColors) o;
        if (this.mMainColors.equals(other.mMainColors) && this.mColorHints == other.mColorHints) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (this.mMainColors.hashCode() * 31) + this.mColorHints;
    }

    public int getColorHints() {
        return this.mColorHints;
    }

    public void setColorHints(int colorHints) {
        this.mColorHints = colorHints;
    }

    private static int calculateDarkHints(Bitmap source) {
        if (source == null) {
            return 0;
        }
        int[] pixels = new int[(source.getWidth() * source.getHeight())];
        double totalLuminance = 0.0d;
        int maxDarkPixels = (int) (((float) pixels.length) * 0.05f);
        int darkPixels = 0;
        source.getPixels(pixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
        float[] tmpHsl = new float[3];
        for (int i = 0; i < pixels.length; i++) {
            ColorUtils.colorToHSL(pixels[i], tmpHsl);
            float luminance = tmpHsl[2];
            int alpha = Color.alpha(pixels[i]);
            if (luminance < DARK_PIXEL_LUMINANCE && alpha != 0) {
                darkPixels++;
            }
            totalLuminance += (double) luminance;
        }
        int hints = 0;
        double meanLuminance = totalLuminance / ((double) pixels.length);
        if (meanLuminance > 0.75d && darkPixels < maxDarkPixels) {
            hints = 1;
        }
        if (meanLuminance < 0.25d) {
            hints |= 2;
        }
        return hints;
    }

    private static Size calculateOptimalSize(int width, int height) {
        int requestedArea = width * height;
        double scale = 1.0d;
        if (requestedArea > MAX_WALLPAPER_EXTRACTION_AREA) {
            scale = Math.sqrt(12544.0d / ((double) requestedArea));
        }
        int newWidth = (int) (((double) width) * scale);
        int newHeight = (int) (((double) height) * scale);
        if (newWidth == 0) {
            newWidth = 1;
        }
        if (newHeight == 0) {
            newHeight = 1;
        }
        return new Size(newWidth, newHeight);
    }

    public String toString() {
        StringBuilder colors = new StringBuilder();
        for (int i = 0; i < this.mMainColors.size(); i++) {
            colors.append(Integer.toHexString(((Color) this.mMainColors.get(i)).toArgb())).append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        }
        return "[WallpaperColors: " + colors.toString() + "h: " + this.mColorHints + "]";
    }
}
