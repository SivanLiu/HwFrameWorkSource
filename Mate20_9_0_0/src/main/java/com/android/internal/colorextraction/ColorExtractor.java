package com.android.internal.colorextraction;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.types.ExtractionType;
import com.android.internal.colorextraction.types.Tonal;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ColorExtractor implements android.app.WallpaperManager.OnColorsChangedListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "ColorExtractor";
    public static final int TYPE_DARK = 1;
    public static final int TYPE_EXTRA_DARK = 2;
    public static final int TYPE_NORMAL = 0;
    private static final int[] sGradientTypes = new int[]{0, 1, 2};
    private final Context mContext;
    private final ExtractionType mExtractionType;
    protected final SparseArray<GradientColors[]> mGradientColors;
    protected WallpaperColors mLockColors;
    private final ArrayList<WeakReference<OnColorsChangedListener>> mOnColorsChangedListeners;
    protected WallpaperColors mSystemColors;

    public static class GradientColors {
        private int mMainColor;
        private int mSecondaryColor;
        private boolean mSupportsDarkText;

        public void setMainColor(int mainColor) {
            this.mMainColor = mainColor;
        }

        public void setSecondaryColor(int secondaryColor) {
            this.mSecondaryColor = secondaryColor;
        }

        public void setSupportsDarkText(boolean supportsDarkText) {
            this.mSupportsDarkText = supportsDarkText;
        }

        public void set(GradientColors other) {
            this.mMainColor = other.mMainColor;
            this.mSecondaryColor = other.mSecondaryColor;
            this.mSupportsDarkText = other.mSupportsDarkText;
        }

        public int getMainColor() {
            return this.mMainColor;
        }

        public int getSecondaryColor() {
            return this.mSecondaryColor;
        }

        public boolean supportsDarkText() {
            return this.mSupportsDarkText;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (o == null || o.getClass() != getClass()) {
                return false;
            }
            GradientColors other = (GradientColors) o;
            if (other.mMainColor == this.mMainColor && other.mSecondaryColor == this.mSecondaryColor && other.mSupportsDarkText == this.mSupportsDarkText) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * ((31 * this.mMainColor) + this.mSecondaryColor)) + (this.mSupportsDarkText ^ 1);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GradientColors(");
            stringBuilder.append(Integer.toHexString(this.mMainColor));
            stringBuilder.append(", ");
            stringBuilder.append(Integer.toHexString(this.mSecondaryColor));
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    public interface OnColorsChangedListener {
        void onBlurWallpaperChanged() throws RemoteException;

        void onColorsChanged(ColorExtractor colorExtractor, int i);
    }

    public ColorExtractor(Context context) {
        this(context, new Tonal(context));
    }

    @VisibleForTesting
    public ColorExtractor(Context context, ExtractionType extractionType) {
        this.mContext = context;
        this.mExtractionType = extractionType;
        this.mGradientColors = new SparseArray();
        for (int which : new int[]{2, 1}) {
            GradientColors[] colors = new GradientColors[sGradientTypes.length];
            this.mGradientColors.append(which, colors);
            for (int type : sGradientTypes) {
                colors[type] = new GradientColors();
            }
        }
        this.mOnColorsChangedListeners = new ArrayList();
        GradientColors[] systemColors = (GradientColors[]) this.mGradientColors.get(1);
        GradientColors[] lockColors = (GradientColors[]) this.mGradientColors.get(2);
        WallpaperManager wallpaperManager = (WallpaperManager) this.mContext.getSystemService(WallpaperManager.class);
        if (wallpaperManager == null) {
            Log.w(TAG, "Can't listen to color changes!");
        } else {
            wallpaperManager.addOnColorsChangedListener(this, null);
            Trace.beginSection("ColorExtractor#getWallpaperColors");
            this.mSystemColors = wallpaperManager.getWallpaperColors(1);
            this.mLockColors = wallpaperManager.getWallpaperColors(2);
            Trace.endSection();
        }
        extractInto(this.mSystemColors, systemColors[0], systemColors[1], systemColors[2]);
        extractInto(this.mLockColors, lockColors[0], lockColors[1], lockColors[2]);
    }

    public GradientColors getColors(int which) {
        return getColors(which, 1);
    }

    public GradientColors getColors(int which, int type) {
        if (type != 0 && type != 1 && type != 2) {
            throw new IllegalArgumentException("type should be TYPE_NORMAL, TYPE_DARK or TYPE_EXTRA_DARK");
        } else if (which == 2 || which == 1) {
            return ((GradientColors[]) this.mGradientColors.get(which))[type];
        } else {
            throw new IllegalArgumentException("which should be FLAG_SYSTEM or FLAG_NORMAL");
        }
    }

    public WallpaperColors getWallpaperColors(int which) {
        if (which == 2) {
            return this.mLockColors;
        }
        if (which == 1) {
            return this.mSystemColors;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid value for which: ");
        stringBuilder.append(which);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void onColorsChanged(WallpaperColors colors, int which) {
        GradientColors[] lockColors;
        boolean changed = false;
        if ((which & 2) != 0) {
            this.mLockColors = colors;
            lockColors = (GradientColors[]) this.mGradientColors.get(2);
            extractInto(colors, lockColors[0], lockColors[1], lockColors[2]);
            changed = true;
        }
        if ((which & 1) != 0) {
            this.mSystemColors = colors;
            lockColors = (GradientColors[]) this.mGradientColors.get(1);
            extractInto(colors, lockColors[0], lockColors[1], lockColors[2]);
            changed = true;
        }
        if (changed) {
            triggerColorsChanged(which);
        }
    }

    protected void triggerColorsChanged(int which) {
        ArrayList<WeakReference<OnColorsChangedListener>> references = new ArrayList(this.mOnColorsChangedListeners);
        int size = references.size();
        for (int i = 0; i < size; i++) {
            WeakReference<OnColorsChangedListener> weakReference = (WeakReference) references.get(i);
            OnColorsChangedListener listener = (OnColorsChangedListener) weakReference.get();
            if (listener == null) {
                this.mOnColorsChangedListeners.remove(weakReference);
            } else {
                listener.onColorsChanged(this, which);
            }
        }
    }

    private void extractInto(WallpaperColors inWallpaperColors, GradientColors outGradientColorsNormal, GradientColors outGradientColorsDark, GradientColors outGradientColorsExtraDark) {
        this.mExtractionType.extractInto(inWallpaperColors, outGradientColorsNormal, outGradientColorsDark, outGradientColorsExtraDark);
    }

    public void destroy() {
        WallpaperManager wallpaperManager = (WallpaperManager) this.mContext.getSystemService(WallpaperManager.class);
        if (wallpaperManager != null) {
            wallpaperManager.removeOnColorsChangedListener(this);
        }
    }

    public void addOnColorsChangedListener(OnColorsChangedListener listener) {
        this.mOnColorsChangedListeners.add(new WeakReference(listener));
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener listener) {
        ArrayList<WeakReference<OnColorsChangedListener>> references = new ArrayList(this.mOnColorsChangedListeners);
        int size = references.size();
        for (int i = 0; i < size; i++) {
            WeakReference<OnColorsChangedListener> weakReference = (WeakReference) references.get(i);
            if (weakReference.get() == listener) {
                this.mOnColorsChangedListeners.remove(weakReference);
                return;
            }
        }
    }
}
