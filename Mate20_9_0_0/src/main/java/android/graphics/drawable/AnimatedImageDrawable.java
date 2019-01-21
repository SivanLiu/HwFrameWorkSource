package android.graphics.drawable;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.Source;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2.AnimationCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.internal.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import libcore.util.NativeAllocationRegistry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatedImageDrawable extends Drawable implements Animatable2 {
    private static final int FINISHED = -1;
    @Deprecated
    public static final int LOOP_INFINITE = -1;
    public static final int REPEAT_INFINITE = -1;
    private static final int REPEAT_UNDEFINED = -2;
    private ArrayList<AnimationCallback> mAnimationCallbacks;
    private ColorFilter mColorFilter;
    private Handler mHandler;
    private int mIntrinsicHeight;
    private int mIntrinsicWidth;
    private Runnable mRunnable;
    private boolean mStarting;
    private State mState;

    private class State {
        private final AssetFileDescriptor mAssetFd;
        boolean mAutoMirrored = false;
        private final InputStream mInputStream;
        final long mNativePtr;
        int mRepeatCount = -2;
        int[] mThemeAttrs = null;

        State(long nativePtr, InputStream is, AssetFileDescriptor afd) {
            this.mNativePtr = nativePtr;
            this.mInputStream = is;
            this.mAssetFd = afd;
        }
    }

    private static native long nCreate(long j, ImageDecoder imageDecoder, int i, int i2, Rect rect) throws IOException;

    private static native long nDraw(long j, long j2);

    private static native int nGetAlpha(long j);

    private static native long nGetNativeFinalizer();

    private static native int nGetRepeatCount(long j);

    private static native boolean nIsRunning(long j);

    private static native long nNativeByteSize(long j);

    private static native void nSetAlpha(long j, int i);

    private static native void nSetColorFilter(long j, long j2);

    private static native void nSetMirrored(long j, boolean z);

    private static native void nSetOnAnimationEndListener(long j, AnimatedImageDrawable animatedImageDrawable);

    private static native void nSetRepeatCount(long j, int i);

    private static native boolean nStart(long j);

    private static native boolean nStop(long j);

    public void setRepeatCount(int repeatCount) {
        if (repeatCount < -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid value passed to setRepeatCount");
            stringBuilder.append(repeatCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.mState.mRepeatCount != repeatCount) {
            this.mState.mRepeatCount = repeatCount;
            if (this.mState.mNativePtr != 0) {
                nSetRepeatCount(this.mState.mNativePtr, repeatCount);
            }
        }
    }

    @Deprecated
    public void setLoopCount(int loopCount) {
        setRepeatCount(loopCount);
    }

    public int getRepeatCount() {
        if (this.mState.mNativePtr != 0) {
            if (this.mState.mRepeatCount == -2) {
                this.mState.mRepeatCount = nGetRepeatCount(this.mState.mNativePtr);
            }
            return this.mState.mRepeatCount;
        }
        throw new IllegalStateException("called getRepeatCount on empty AnimatedImageDrawable");
    }

    @Deprecated
    public int getLoopCount(int loopCount) {
        return getRepeatCount();
    }

    public AnimatedImageDrawable() {
        this.mAnimationCallbacks = null;
        this.mState = new State(0, null, null);
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        updateStateFromTypedArray(Drawable.obtainAttributes(r, theme, attrs, R.styleable.AnimatedImageDrawable), this.mSrcDensityOverride);
    }

    private void updateStateFromTypedArray(TypedArray a, int srcDensityOverride) throws XmlPullParserException {
        StringBuilder stringBuilder;
        State oldState = this.mState;
        Resources r = a.getResources();
        int srcResId = a.getResourceId(0, 0);
        if (srcResId != 0) {
            TypedValue value = new TypedValue();
            r.getValueForDensity(srcResId, srcDensityOverride, value, true);
            if (srcDensityOverride > 0 && value.density > 0 && value.density != 65535) {
                if (value.density == srcDensityOverride) {
                    value.density = r.getDisplayMetrics().densityDpi;
                } else {
                    value.density = (value.density * r.getDisplayMetrics().densityDpi) / srcDensityOverride;
                }
            }
            int density = 0;
            if (value.density == 0) {
                density = 160;
            } else if (value.density != 65535) {
                density = value.density;
            }
            Drawable drawable = null;
            try {
                drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(r, r.openRawResource(srcResId, value), density), -$$Lambda$AnimatedImageDrawable$Cgt3NliB7ZYUONyDd-eQGdYbEKc.INSTANCE);
                if (drawable instanceof AnimatedImageDrawable) {
                    int repeatCount = this.mState.mRepeatCount;
                    AnimatedImageDrawable other = (AnimatedImageDrawable) drawable;
                    this.mState = other.mState;
                    other.mState = null;
                    this.mIntrinsicWidth = other.mIntrinsicWidth;
                    this.mIntrinsicHeight = other.mIntrinsicHeight;
                    if (repeatCount != -2) {
                        setRepeatCount(repeatCount);
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(a.getPositionDescription());
                    stringBuilder.append(": <animated-image> did not decode animated");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            } catch (IOException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(a.getPositionDescription());
                stringBuilder2.append(": <animated-image> requires a valid 'src' attribute");
                throw new XmlPullParserException(stringBuilder2.toString(), null, e);
            }
        }
        this.mState.mThemeAttrs = a.extractThemeAttrs();
        if (this.mState.mNativePtr == 0 && (this.mState.mThemeAttrs == null || this.mState.mThemeAttrs[0] == 0)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(a.getPositionDescription());
            stringBuilder.append(": <animated-image> requires a valid 'src' attribute");
            throw new XmlPullParserException(stringBuilder.toString());
        }
        this.mState.mAutoMirrored = a.getBoolean(3, oldState.mAutoMirrored);
        int repeatCount2 = a.getInt(1, -2);
        if (repeatCount2 != -2) {
            setRepeatCount(repeatCount2);
        }
        if (a.getBoolean(2, false) && this.mState.mNativePtr != 0) {
            start();
        }
    }

    static /* synthetic */ void lambda$updateStateFromTypedArray$0(ImageDecoder decoder, ImageInfo info, Source src) {
        if (!info.isAnimated()) {
            throw new IllegalArgumentException("image is not animated");
        }
    }

    public AnimatedImageDrawable(long nativeImageDecoder, ImageDecoder decoder, int width, int height, int srcDensity, int dstDensity, Rect cropRect, InputStream inputStream, AssetFileDescriptor afd) throws IOException {
        int i = srcDensity;
        int i2 = dstDensity;
        Rect rect = cropRect;
        this.mAnimationCallbacks = null;
        int width2 = Bitmap.scaleFromDensity(width, i, i2);
        int height2 = Bitmap.scaleFromDensity(height, srcDensity, dstDensity);
        if (rect == null) {
            this.mIntrinsicWidth = width2;
            this.mIntrinsicHeight = height2;
        } else {
            rect.set(Bitmap.scaleFromDensity(rect.left, i, i2), Bitmap.scaleFromDensity(rect.top, i, i2), Bitmap.scaleFromDensity(rect.right, i, i2), Bitmap.scaleFromDensity(rect.bottom, i, i2));
            this.mIntrinsicWidth = cropRect.width();
            this.mIntrinsicHeight = cropRect.height();
        }
        this.mState = new State(nCreate(nativeImageDecoder, decoder, width2, height2, rect), inputStream, afd);
        new NativeAllocationRegistry(AnimatedImageDrawable.class.getClassLoader(), nGetNativeFinalizer(), nNativeByteSize(this.mState.mNativePtr)).registerNativeAllocation(this.mState, this.mState.mNativePtr);
    }

    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    public void draw(Canvas canvas) {
        if (this.mState.mNativePtr != 0) {
            if (this.mStarting) {
                this.mStarting = false;
                postOnAnimationStart();
            }
            long nextUpdate = nDraw(this.mState.mNativePtr, canvas.getNativeCanvasWrapper());
            if (nextUpdate > 0) {
                if (this.mRunnable == null) {
                    this.mRunnable = new -$$Lambda$AlQeVq8Y-kfuQeb-JLZ0ueV4DE8(this);
                }
                scheduleSelf(this.mRunnable, SystemClock.uptimeMillis() + nextUpdate);
                return;
            } else if (nextUpdate == -1) {
                postOnAnimationEnd();
                return;
            } else {
                return;
            }
        }
        throw new IllegalStateException("called draw on empty AnimatedImageDrawable");
    }

    public void setAlpha(int alpha) {
        if (alpha < 0 || alpha > 255) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alpha must be between 0 and 255! provided ");
            stringBuilder.append(alpha);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.mState.mNativePtr != 0) {
            nSetAlpha(this.mState.mNativePtr, alpha);
            invalidateSelf();
        } else {
            throw new IllegalStateException("called setAlpha on empty AnimatedImageDrawable");
        }
    }

    public int getAlpha() {
        if (this.mState.mNativePtr != 0) {
            return nGetAlpha(this.mState.mNativePtr);
        }
        throw new IllegalStateException("called getAlpha on empty AnimatedImageDrawable");
    }

    public void setColorFilter(ColorFilter colorFilter) {
        long j = 0;
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called setColorFilter on empty AnimatedImageDrawable");
        } else if (colorFilter != this.mColorFilter) {
            this.mColorFilter = colorFilter;
            if (colorFilter != null) {
                j = colorFilter.getNativeInstance();
            }
            nSetColorFilter(this.mState.mNativePtr, j);
            invalidateSelf();
        }
    }

    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    public int getOpacity() {
        return -3;
    }

    public void setAutoMirrored(boolean mirrored) {
        if (this.mState.mAutoMirrored != mirrored) {
            this.mState.mAutoMirrored = mirrored;
            if (getLayoutDirection() == 1 && this.mState.mNativePtr != 0) {
                nSetMirrored(this.mState.mNativePtr, mirrored);
                invalidateSelf();
            }
        }
    }

    public boolean onLayoutDirectionChanged(int layoutDirection) {
        boolean mirror = false;
        if (!this.mState.mAutoMirrored || this.mState.mNativePtr == 0) {
            return false;
        }
        if (layoutDirection == 1) {
            mirror = true;
        }
        nSetMirrored(this.mState.mNativePtr, mirror);
        return true;
    }

    public final boolean isAutoMirrored() {
        return this.mState.mAutoMirrored;
    }

    public boolean isRunning() {
        if (this.mState.mNativePtr != 0) {
            return nIsRunning(this.mState.mNativePtr);
        }
        throw new IllegalStateException("called isRunning on empty AnimatedImageDrawable");
    }

    public void start() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called start on empty AnimatedImageDrawable");
        } else if (nStart(this.mState.mNativePtr)) {
            this.mStarting = true;
            invalidateSelf();
        }
    }

    public void stop() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called stop on empty AnimatedImageDrawable");
        } else if (nStop(this.mState.mNativePtr)) {
            postOnAnimationEnd();
        }
    }

    public void registerAnimationCallback(AnimationCallback callback) {
        if (callback != null) {
            if (this.mAnimationCallbacks == null) {
                this.mAnimationCallbacks = new ArrayList();
                nSetOnAnimationEndListener(this.mState.mNativePtr, this);
            }
            if (!this.mAnimationCallbacks.contains(callback)) {
                this.mAnimationCallbacks.add(callback);
            }
        }
    }

    public boolean unregisterAnimationCallback(AnimationCallback callback) {
        if (callback == null || this.mAnimationCallbacks == null || !this.mAnimationCallbacks.remove(callback)) {
            return false;
        }
        if (this.mAnimationCallbacks.isEmpty()) {
            clearAnimationCallbacks();
        }
        return true;
    }

    public void clearAnimationCallbacks() {
        if (this.mAnimationCallbacks != null) {
            this.mAnimationCallbacks = null;
            nSetOnAnimationEndListener(this.mState.mNativePtr, null);
        }
    }

    private void postOnAnimationStart() {
        if (this.mAnimationCallbacks != null) {
            getHandler().post(new -$$Lambda$AnimatedImageDrawable$6aWLU8OYhdfACSejz5_iGirYxUk(this));
        }
    }

    public static /* synthetic */ void lambda$postOnAnimationStart$1(AnimatedImageDrawable animatedImageDrawable) {
        Iterator it = animatedImageDrawable.mAnimationCallbacks.iterator();
        while (it.hasNext()) {
            ((AnimationCallback) it.next()).onAnimationStart(animatedImageDrawable);
        }
    }

    private void postOnAnimationEnd() {
        if (this.mAnimationCallbacks != null) {
            getHandler().post(new -$$Lambda$AnimatedImageDrawable$dGAkP-tKNvqn_qCWdrQRL806ExQ(this));
        }
    }

    public static /* synthetic */ void lambda$postOnAnimationEnd$2(AnimatedImageDrawable animatedImageDrawable) {
        Iterator it = animatedImageDrawable.mAnimationCallbacks.iterator();
        while (it.hasNext()) {
            ((AnimationCallback) it.next()).onAnimationEnd(animatedImageDrawable);
        }
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
        return this.mHandler;
    }

    private void onAnimationEnd() {
        if (this.mAnimationCallbacks != null) {
            Iterator it = this.mAnimationCallbacks.iterator();
            while (it.hasNext()) {
                ((AnimationCallback) it.next()).onAnimationEnd(this);
            }
        }
    }
}
