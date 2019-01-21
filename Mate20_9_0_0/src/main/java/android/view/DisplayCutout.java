package android.view;

import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import java.util.ArrayList;
import java.util.List;

public final class DisplayCutout {
    private static final String BOTTOM_MARKER = "@bottom";
    private static final Object CACHE_LOCK = new Object();
    private static final String DP_MARKER = "@dp";
    private static final Region EMPTY_REGION = new Region();
    public static final String EMULATION_OVERLAY_CATEGORY = "com.android.internal.display_cutout_emulation";
    public static final DisplayCutout NO_CUTOUT = new DisplayCutout(ZERO_RECT, EMPTY_REGION, false);
    private static final Pair<Path, DisplayCutout> NULL_PAIR = new Pair(null, null);
    private static final String RIGHT_MARKER = "@right";
    private static final String TAG = "DisplayCutout";
    private static final Rect ZERO_RECT = new Rect();
    @GuardedBy("CACHE_LOCK")
    private static Pair<Path, DisplayCutout> sCachedCutout = NULL_PAIR;
    @GuardedBy("CACHE_LOCK")
    private static float sCachedDensity;
    @GuardedBy("CACHE_LOCK")
    private static int sCachedDisplayHeight;
    @GuardedBy("CACHE_LOCK")
    private static int sCachedDisplayWidth;
    @GuardedBy("CACHE_LOCK")
    private static String sCachedSpec;
    private final Region mBounds;
    private final Rect mSafeInsets;

    public static final class ParcelableWrapper implements Parcelable {
        public static final Creator<ParcelableWrapper> CREATOR = new Creator<ParcelableWrapper>() {
            public ParcelableWrapper createFromParcel(Parcel in) {
                return new ParcelableWrapper(ParcelableWrapper.readCutoutFromParcel(in));
            }

            public ParcelableWrapper[] newArray(int size) {
                return new ParcelableWrapper[size];
            }
        };
        private DisplayCutout mInner;

        public ParcelableWrapper() {
            this(DisplayCutout.NO_CUTOUT);
        }

        public ParcelableWrapper(DisplayCutout cutout) {
            this.mInner = cutout;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            writeCutoutToParcel(this.mInner, out, flags);
        }

        public static void writeCutoutToParcel(DisplayCutout cutout, Parcel out, int flags) {
            if (cutout == null) {
                out.writeInt(-1);
            } else if (cutout == DisplayCutout.NO_CUTOUT) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                out.writeTypedObject(cutout.mSafeInsets, flags);
                out.writeTypedObject(cutout.mBounds, flags);
            }
        }

        public void readFromParcel(Parcel in) {
            this.mInner = readCutoutFromParcel(in);
        }

        public static DisplayCutout readCutoutFromParcel(Parcel in) {
            int variant = in.readInt();
            if (variant == -1) {
                return null;
            }
            if (variant == 0) {
                return DisplayCutout.NO_CUTOUT;
            }
            return new DisplayCutout((Rect) in.readTypedObject(Rect.CREATOR), (Region) in.readTypedObject(Region.CREATOR), false);
        }

        public DisplayCutout get() {
            return this.mInner;
        }

        public void set(ParcelableWrapper cutout) {
            this.mInner = cutout.get();
        }

        public void set(DisplayCutout cutout) {
            this.mInner = cutout;
        }

        public int hashCode() {
            return this.mInner.hashCode();
        }

        public boolean equals(Object o) {
            return (o instanceof ParcelableWrapper) && this.mInner.equals(((ParcelableWrapper) o).mInner);
        }

        public String toString() {
            return String.valueOf(this.mInner);
        }
    }

    public DisplayCutout(Rect safeInsets, List<Rect> boundingRects) {
        this(safeInsets != null ? new Rect(safeInsets) : ZERO_RECT, boundingRectsToRegion(boundingRects), true);
    }

    private DisplayCutout(Rect safeInsets, Region bounds, boolean copyArguments) {
        Rect rect = safeInsets == null ? ZERO_RECT : copyArguments ? new Rect(safeInsets) : safeInsets;
        this.mSafeInsets = rect;
        Region obtain = bounds == null ? Region.obtain() : copyArguments ? Region.obtain(bounds) : bounds;
        this.mBounds = obtain;
    }

    public boolean isEmpty() {
        return this.mSafeInsets.equals(ZERO_RECT);
    }

    public boolean isBoundsEmpty() {
        return this.mBounds.isEmpty();
    }

    public int getSafeInsetTop() {
        return this.mSafeInsets.top;
    }

    public int getSafeInsetBottom() {
        return this.mSafeInsets.bottom;
    }

    public int getSafeInsetLeft() {
        return this.mSafeInsets.left;
    }

    public int getSafeInsetRight() {
        return this.mSafeInsets.right;
    }

    public Rect getSafeInsets() {
        return new Rect(this.mSafeInsets);
    }

    public Region getBounds() {
        return Region.obtain(this.mBounds);
    }

    public List<Rect> getBoundingRects() {
        List<Rect> result = new ArrayList();
        Region bounds = Region.obtain();
        bounds.set(this.mBounds);
        bounds.op(0, 0, Integer.MAX_VALUE, getSafeInsetTop(), Op.INTERSECT);
        if (!bounds.isEmpty()) {
            result.add(bounds.getBounds());
        }
        bounds.set(this.mBounds);
        bounds.op(0, 0, getSafeInsetLeft(), Integer.MAX_VALUE, Op.INTERSECT);
        if (!bounds.isEmpty()) {
            result.add(bounds.getBounds());
        }
        bounds.set(this.mBounds);
        bounds.op(getSafeInsetLeft() + 1, getSafeInsetTop() + 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Op.INTERSECT);
        if (!bounds.isEmpty()) {
            result.add(bounds.getBounds());
        }
        bounds.recycle();
        return result;
    }

    public int hashCode() {
        return (this.mSafeInsets.hashCode() * 31) + this.mBounds.getBounds().hashCode();
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (o == this) {
            return true;
        }
        if (!(o instanceof DisplayCutout)) {
            return false;
        }
        DisplayCutout c = (DisplayCutout) o;
        if (!(this.mSafeInsets.equals(c.mSafeInsets) && this.mBounds.equals(c.mBounds))) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DisplayCutout{insets=");
        stringBuilder.append(this.mSafeInsets);
        stringBuilder.append(" boundingRect=");
        stringBuilder.append(this.mBounds.getBounds());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mSafeInsets.writeToProto(proto, 1146756268033L);
        this.mBounds.getBounds().writeToProto(proto, 1146756268034L);
        proto.end(token);
    }

    public DisplayCutout inset(int insetLeft, int insetTop, int insetRight, int insetBottom) {
        if (this.mBounds.isEmpty() || (insetLeft == 0 && insetTop == 0 && insetRight == 0 && insetBottom == 0)) {
            return this;
        }
        Rect safeInsets = new Rect(this.mSafeInsets);
        Region bounds = Region.obtain(this.mBounds);
        if (insetTop > 0 || safeInsets.top > 0) {
            safeInsets.top = atLeastZero(safeInsets.top - insetTop);
        }
        if (insetBottom > 0 || safeInsets.bottom > 0) {
            safeInsets.bottom = atLeastZero(safeInsets.bottom - insetBottom);
        }
        if (insetLeft > 0 || safeInsets.left > 0) {
            safeInsets.left = atLeastZero(safeInsets.left - insetLeft);
        }
        if (insetRight > 0 || safeInsets.right > 0) {
            safeInsets.right = atLeastZero(safeInsets.right - insetRight);
        }
        bounds.translate(-insetLeft, -insetTop);
        return new DisplayCutout(safeInsets, bounds, false);
    }

    public DisplayCutout replaceSafeInsets(Rect safeInsets) {
        return new DisplayCutout(new Rect(safeInsets), this.mBounds, false);
    }

    private static int atLeastZero(int value) {
        return value < 0 ? 0 : value;
    }

    public static DisplayCutout fromBoundingRect(int left, int top, int right, int bottom) {
        Path path = new Path();
        path.reset();
        path.moveTo((float) left, (float) top);
        path.lineTo((float) left, (float) bottom);
        path.lineTo((float) right, (float) bottom);
        path.lineTo((float) right, (float) top);
        path.close();
        return fromBounds(path);
    }

    public static DisplayCutout fromBounds(Path path) {
        RectF clipRect = new RectF();
        path.computeBounds(clipRect, false);
        Region clipRegion = Region.obtain();
        if (isRogOn()) {
            clipRegion.set((int) (clipRect.left + 0.5f), (int) (clipRect.top + 0.5f), (int) (clipRect.right + 0.5f), (int) (clipRect.bottom + 0.5f));
        } else {
            clipRegion.set((int) clipRect.left, (int) clipRect.top, (int) clipRect.right, (int) clipRect.bottom);
        }
        Region bounds = new Region();
        bounds.setPath(path, clipRegion);
        clipRegion.recycle();
        return new DisplayCutout(ZERO_RECT, bounds, false);
    }

    public static DisplayCutout fromResources(Resources res, int displayWidth, int displayHeight) {
        return fromSpec(res.getString(17039826), displayWidth, displayHeight, ((float) DisplayMetrics.DENSITY_DEVICE_STABLE) / 160.0f);
    }

    public static Path pathFromResources(Resources res, int displayWidth, int displayHeight) {
        return (Path) pathAndDisplayCutoutFromSpec(res.getString(17039826), displayWidth, displayHeight, ((float) DisplayMetrics.DENSITY_DEVICE_STABLE) / 160.0f).first;
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public static DisplayCutout fromSpec(String spec, int displayWidth, int displayHeight, float density) {
        return (DisplayCutout) pathAndDisplayCutoutFromSpec(spec, displayWidth, displayHeight, density).second;
    }

    /* JADX WARNING: Missing block: B:22:0x0074, code skipped:
            r0 = r17.trim();
     */
    /* JADX WARNING: Missing block: B:23:0x007f, code skipped:
            if (r0.endsWith(RIGHT_MARKER) == false) goto L_0x0096;
     */
    /* JADX WARNING: Missing block: B:24:0x0081, code skipped:
            r4 = (float) r1;
            r0 = r0.substring(0, r0.length() - RIGHT_MARKER.length()).trim();
     */
    /* JADX WARNING: Missing block: B:25:0x0096, code skipped:
            r4 = ((float) r1) / 2.0f;
     */
    /* JADX WARNING: Missing block: B:26:0x009a, code skipped:
            r6 = r0.endsWith(DP_MARKER);
     */
    /* JADX WARNING: Missing block: B:27:0x00a0, code skipped:
            if (r6 == false) goto L_0x00b1;
     */
    /* JADX WARNING: Missing block: B:28:0x00a2, code skipped:
            r0 = r0.substring(0, r0.length() - DP_MARKER.length());
     */
    /* JADX WARNING: Missing block: B:29:0x00b1, code skipped:
            r7 = null;
     */
    /* JADX WARNING: Missing block: B:30:0x00b8, code skipped:
            if (r0.contains(BOTTOM_MARKER) == false) goto L_0x00ce;
     */
    /* JADX WARNING: Missing block: B:31:0x00ba, code skipped:
            r8 = r0.split(BOTTOM_MARKER, 2);
            r0 = r8[0].trim();
            r7 = r8[1].trim();
     */
    /* JADX WARNING: Missing block: B:32:0x00ce, code skipped:
            r8 = r7;
            r7 = r0;
     */
    /* JADX WARNING: Missing block: B:35:0x00d4, code skipped:
            r9 = android.util.PathParser.createPathFromPathData(r7);
            r10 = new android.graphics.Matrix();
     */
    /* JADX WARNING: Missing block: B:36:0x00dd, code skipped:
            if (r6 == false) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:37:0x00df, code skipped:
            r10.postScale(r3, r3);
     */
    /* JADX WARNING: Missing block: B:38:0x00e2, code skipped:
            if (r6 != false) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:40:0x00e8, code skipped:
            if (isRogOn() == false) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:41:0x00ea, code skipped:
            r0 = android.view.SurfaceControl.getBuiltInDisplay(0);
     */
    /* JADX WARNING: Missing block: B:42:0x00ee, code skipped:
            if (r0 == null) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:43:0x00f0, code skipped:
            r11 = android.view.SurfaceControl.getDisplayConfigs(r0);
     */
    /* JADX WARNING: Missing block: B:44:0x00f4, code skipped:
            if (r11 == null) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:46:0x00f7, code skipped:
            if (r11.length == 0) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:48:0x00fd, code skipped:
            if (r11[0].width == 0) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:50:0x0103, code skipped:
            if (r11[0].height == 0) goto L_0x0173;
     */
    /* JADX WARNING: Missing block: B:51:0x0105, code skipped:
            r10.postScale((((float) r1) * 1.0f) / ((float) r11[0].width), (((float) r2) * 1.0f) / ((float) r11[0].height));
            r12 = TAG;
            r14 = new java.lang.StringBuilder();
            r14.append("pathAndDisplayCutoutFromSpec ,displayWidth=");
            r14.append(r1);
            r14.append(",realWidth =");
            r14.append(r11[0].width);
            r14.append(",displayWidth =");
            r14.append(r2);
            r14.append(",realHeight =");
            r14.append(r11[0].height);
            r14.append(",scale1 ");
            r14.append((((float) r1) * 1.0f) / ((float) r11[0].width));
            r14.append(",scale2 ");
            r14.append((((float) r2) * 1.0f) / ((float) r11[0].height));
            android.util.Log.d(r12, r14.toString());
     */
    /* JADX WARNING: Missing block: B:52:0x0173, code skipped:
            r10.postTranslate(r4, 0.0f);
            r9.transform(r10);
     */
    /* JADX WARNING: Missing block: B:53:0x017a, code skipped:
            if (r8 == null) goto L_0x0199;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            r5 = android.util.PathParser.createPathFromPathData(r8);
     */
    /* JADX WARNING: Missing block: B:56:0x0180, code skipped:
            r10.postTranslate(0.0f, (float) r2);
            r5.transform(r10);
            r9.addPath(r5);
     */
    /* JADX WARNING: Missing block: B:57:0x018d, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:58:0x018e, code skipped:
            r5 = r0;
            android.util.Log.wtf(TAG, "Could not inflate bottom cutout: ", r0);
     */
    /* JADX WARNING: Missing block: B:59:0x0198, code skipped:
            return NULL_PAIR;
     */
    /* JADX WARNING: Missing block: B:60:0x0199, code skipped:
            r5 = new android.util.Pair(r9, fromBounds(r9));
            r11 = CACHE_LOCK;
     */
    /* JADX WARNING: Missing block: B:61:0x01a5, code skipped:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:63:?, code skipped:
            sCachedSpec = r7;
            sCachedDisplayWidth = r1;
            sCachedDisplayHeight = r2;
            sCachedDensity = r3;
            sCachedCutout = r5;
     */
    /* JADX WARNING: Missing block: B:64:0x01b0, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:65:0x01b1, code skipped:
            return r5;
     */
    /* JADX WARNING: Missing block: B:69:0x01b5, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:70:0x01b6, code skipped:
            r5 = r0;
            android.util.Log.wtf(TAG, "Could not inflate cutout: ", r0);
     */
    /* JADX WARNING: Missing block: B:71:0x01c0, code skipped:
            return NULL_PAIR;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Pair<Path, DisplayCutout> pathAndDisplayCutoutFromSpec(String spec, int displayWidth, int displayHeight, float density) {
        Throwable th;
        int i = displayWidth;
        int i2 = displayHeight;
        float f = density;
        if (TextUtils.isEmpty(spec)) {
            return NULL_PAIR;
        }
        synchronized (CACHE_LOCK) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pathAndDisplayCutoutFromSpec sCachedDensity=");
                stringBuilder.append(sCachedDensity);
                stringBuilder.append(",sCachedDisplayWidth=");
                stringBuilder.append(sCachedDisplayWidth);
                stringBuilder.append(",sCachedDisplayHeight=");
                stringBuilder.append(sCachedDisplayHeight);
                stringBuilder.append(",density=");
                stringBuilder.append(f);
                stringBuilder.append(" displayWidth ");
                stringBuilder.append(i);
                stringBuilder.append(" displayHeight ");
                stringBuilder.append(i2);
                Log.d(str, stringBuilder.toString());
                try {
                    if (spec.equals(sCachedSpec) && sCachedDisplayWidth == i && sCachedDisplayHeight == i2 && sCachedDensity == f) {
                        Pair pair = sCachedCutout;
                        return pair;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                String str2 = spec;
                throw th;
            }
        }
    }

    public static boolean isRogOn() {
        return SystemProperties.getInt("persist.sys.rog.configmode", 0) == 1;
    }

    private static Region boundingRectsToRegion(List<Rect> rects) {
        Region result = Region.obtain();
        if (rects != null) {
            for (Rect r : rects) {
                result.op(r, Op.UNION);
            }
        }
        return result;
    }
}
