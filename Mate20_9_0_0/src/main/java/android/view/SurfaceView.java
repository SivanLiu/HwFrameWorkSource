package android.view;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.res.CompatibilityInfo.Translator;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceHolder.Callback;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import com.android.internal.view.SurfaceCallbackHelper;
import com.huawei.pgmng.log.LogPower;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SurfaceView extends View implements WindowStoppedCallback {
    private static final boolean DEBUG = false;
    private static final String GALLERY = "com.android.gallery3d";
    private static final String TAG = "SurfaceView";
    private boolean mAttachedToWindow;
    final ArrayList<Callback> mCallbacks;
    final Configuration mConfiguration;
    SurfaceControl mDeferredDestroySurfaceControl;
    boolean mDelayDestory;
    boolean mDrawFinished;
    private final OnPreDrawListener mDrawListener;
    boolean mDrawingStopped;
    int mFormat;
    boolean mFromStopped;
    private boolean mGlobalListenersAdded;
    boolean mHaveFrame;
    boolean mIsCreating;
    long mLastLockTime;
    int mLastSurfaceHeight;
    int mLastSurfaceWidth;
    boolean mLastWindowVisibility;
    final int[] mLocation;
    private int mPendingReportDraws;
    private Rect mRTLastReportedPosition;
    int mRequestedFormat;
    int mRequestedHeight;
    boolean mRequestedVisible;
    int mRequestedWidth;
    private volatile boolean mRtHandlingPositionUpdates;
    private Transaction mRtTransaction;
    final Rect mScreenRect;
    private final OnScrollChangedListener mScrollChangedListener;
    int mSubLayer;
    final Surface mSurface;
    SurfaceControlWithBackground mSurfaceControl;
    boolean mSurfaceCreated;
    private int mSurfaceFlags;
    final Rect mSurfaceFrame;
    int mSurfaceHeight;
    private final SurfaceHolder mSurfaceHolder;
    final ReentrantLock mSurfaceLock;
    SurfaceSession mSurfaceSession;
    int mSurfaceWidth;
    final Rect mTmpRect;
    private Translator mTranslator;
    boolean mViewVisibility;
    boolean mVisible;
    boolean mVisiblityChangeState;
    int mWindowSpaceLeft;
    int mWindowSpaceTop;
    boolean mWindowStopped;
    boolean mWindowVisibility;
    private Object sObjectLock;

    class SurfaceControlWithBackground extends SurfaceControl {
        SurfaceControl mBackgroundControl;
        private boolean mOpaque = true;
        public boolean mVisible = false;

        public SurfaceControlWithBackground(String name, boolean opaque, Builder b) throws Exception {
            super(b.setName(name).build());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Background for -");
            stringBuilder.append(name);
            this.mBackgroundControl = b.setName(stringBuilder.toString()).setFormat(1024).setColorLayer(true).build();
            this.mOpaque = opaque;
        }

        public void setAlpha(float alpha) {
            super.setAlpha(alpha);
            this.mBackgroundControl.setAlpha(alpha);
        }

        public void setLayer(int zorder) {
            super.setLayer(zorder);
            this.mBackgroundControl.setLayer(-3);
        }

        public void setPosition(float x, float y) {
            super.setPosition(x, y);
            this.mBackgroundControl.setPosition(x, y);
        }

        public void setSize(int w, int h) {
            super.setSize(w, h);
            this.mBackgroundControl.setSize(w, h);
        }

        public void setWindowCrop(Rect crop) {
            super.setWindowCrop(crop);
            this.mBackgroundControl.setWindowCrop(crop);
        }

        public void setFinalCrop(Rect crop) {
            super.setFinalCrop(crop);
            this.mBackgroundControl.setFinalCrop(crop);
        }

        public void setLayerStack(int layerStack) {
            super.setLayerStack(layerStack);
            this.mBackgroundControl.setLayerStack(layerStack);
        }

        public void setOpaque(boolean isOpaque) {
            super.setOpaque(isOpaque);
            this.mOpaque = isOpaque;
            updateBackgroundVisibility();
        }

        public void setSecure(boolean isSecure) {
            super.setSecure(isSecure);
        }

        public void setMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
            super.setMatrix(dsdx, dtdx, dsdy, dtdy);
            this.mBackgroundControl.setMatrix(dsdx, dtdx, dsdy, dtdy);
        }

        public void hide() {
            super.hide();
            this.mVisible = false;
            updateBackgroundVisibility();
        }

        public void show() {
            super.show();
            this.mVisible = true;
            updateBackgroundVisibility();
        }

        public void destroy() {
            super.destroy();
            this.mBackgroundControl.destroy();
        }

        public void release() {
            super.release();
            this.mBackgroundControl.release();
        }

        public void setTransparentRegionHint(Region region) {
            super.setTransparentRegionHint(region);
            this.mBackgroundControl.setTransparentRegionHint(region);
        }

        public void deferTransactionUntil(IBinder handle, long frame) {
            super.deferTransactionUntil(handle, frame);
            this.mBackgroundControl.deferTransactionUntil(handle, frame);
        }

        public void deferTransactionUntil(Surface barrier, long frame) {
            super.deferTransactionUntil(barrier, frame);
            this.mBackgroundControl.deferTransactionUntil(barrier, frame);
        }

        private void setBackgroundColor(int bgColor) {
            float[] colorComponents = new float[]{((float) Color.red(bgColor)) / 255.0f, ((float) Color.green(bgColor)) / 255.0f, ((float) Color.blue(bgColor)) / 255.0f};
            SurfaceControl.openTransaction();
            try {
                this.mBackgroundControl.setColor(colorComponents);
            } finally {
                SurfaceControl.closeTransaction();
            }
        }

        void updateBackgroundVisibility() {
            if (this.mOpaque && this.mVisible) {
                this.mBackgroundControl.show();
            } else {
                this.mBackgroundControl.hide();
            }
        }
    }

    public SurfaceView(Context context) {
        this(context, null);
    }

    public SurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCallbacks = new ArrayList();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock();
        this.mSurface = new Surface();
        this.mDrawingStopped = true;
        this.mDrawFinished = false;
        this.mScreenRect = new Rect();
        this.sObjectLock = new Object();
        this.mTmpRect = new Rect();
        this.mConfiguration = new Configuration();
        this.mSubLayer = -2;
        this.mIsCreating = false;
        this.mRtHandlingPositionUpdates = false;
        this.mScrollChangedListener = new OnScrollChangedListener() {
            public void onScrollChanged() {
                SurfaceView.this.updateSurface();
            }
        };
        this.mDrawListener = new OnPreDrawListener() {
            public boolean onPreDraw() {
                SurfaceView surfaceView = SurfaceView.this;
                boolean z = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                surfaceView.mHaveFrame = z;
                SurfaceView.this.updateSurface();
                return true;
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mVisiblityChangeState = false;
        this.mDelayDestory = false;
        this.mFromStopped = false;
        this.mLastWindowVisibility = false;
        this.mViewVisibility = false;
        this.mWindowStopped = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0;
        this.mVisible = false;
        this.mWindowSpaceLeft = -1;
        this.mWindowSpaceTop = -1;
        this.mSurfaceWidth = -1;
        this.mSurfaceHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mSurfaceFlags = 4;
        this.mRtTransaction = new Transaction();
        this.mRTLastReportedPosition = new Rect();
        this.mSurfaceHolder = new SurfaceHolder() {
            private static final String LOG_TAG = "SurfaceHolder";

            public boolean isCreating() {
                return SurfaceView.this.mIsCreating;
            }

            public void addCallback(Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    if (!SurfaceView.this.mCallbacks.contains(callback)) {
                        SurfaceView.this.mCallbacks.add(callback);
                    }
                }
            }

            public void removeCallback(Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    SurfaceView.this.mCallbacks.remove(callback);
                }
            }

            public void setFixedSize(int width, int height) {
                if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                    SurfaceView.this.mRequestedWidth = width;
                    SurfaceView.this.mRequestedHeight = height;
                    SurfaceView.this.requestLayout();
                }
            }

            public void setSizeFromLayout() {
                if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                    SurfaceView surfaceView = SurfaceView.this;
                    SurfaceView.this.mRequestedHeight = -1;
                    surfaceView.mRequestedWidth = -1;
                    SurfaceView.this.requestLayout();
                }
            }

            public void setFormat(int format) {
                if (format == -1) {
                    format = 4;
                }
                SurfaceView.this.mRequestedFormat = format;
                if (SurfaceView.this.mSurfaceControl != null) {
                    SurfaceView.this.updateSurface();
                }
            }

            @Deprecated
            public void setType(int type) {
            }

            public void setKeepScreenOn(boolean screenOn) {
                SurfaceView.this.runOnUiThread(new -$$Lambda$SurfaceView$3$XvaZSTTyv1kHN4GtX5NDdmQTRp8(this, screenOn));
            }

            public Canvas lockCanvas() {
                return internalLockCanvas(null, false);
            }

            public Canvas lockCanvas(Rect inOutDirty) {
                return internalLockCanvas(inOutDirty, false);
            }

            public Canvas lockHardwareCanvas() {
                return internalLockCanvas(null, true);
            }

            private Canvas internalLockCanvas(Rect dirty, boolean hardware) {
                SurfaceView.this.mSurfaceLock.lock();
                Canvas c = null;
                if (!(SurfaceView.this.mDrawingStopped || SurfaceView.this.mSurfaceControl == null)) {
                    if (hardware) {
                        try {
                            c = SurfaceView.this.mSurface.lockHardwareCanvas();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Exception locking surface", e);
                        }
                    } else {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    }
                }
                if (c != null) {
                    SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                    return c;
                }
                long now = SystemClock.uptimeMillis();
                long nextTime = SurfaceView.this.mLastLockTime + 100;
                if (nextTime > now) {
                    try {
                        Thread.sleep(nextTime - now);
                    } catch (InterruptedException e2) {
                    }
                    now = SystemClock.uptimeMillis();
                }
                SurfaceView.this.mLastLockTime = now;
                SurfaceView.this.mSurfaceLock.unlock();
                return null;
            }

            public void unlockCanvasAndPost(Canvas canvas) {
                SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
                SurfaceView.this.mSurfaceLock.unlock();
                IHwApsImpl hwApsImpl = HwFrameworkFactory.getHwApsImpl();
                if (hwApsImpl.isSupportAps() && hwApsImpl.isAPSReady()) {
                    hwApsImpl.powerCtroll();
                }
            }

            public Surface getSurface() {
                return SurfaceView.this.mSurface;
            }

            public Rect getSurfaceFrame() {
                return SurfaceView.this.mSurfaceFrame;
            }
        };
        this.mRenderNode.requestPositionUpdates(this);
        setWillNotDraw(true);
    }

    public SurfaceHolder getHolder() {
        return this.mSurfaceHolder;
    }

    private void updateRequestedVisibility() {
        boolean z = this.mViewVisibility && this.mWindowVisibility && !this.mWindowStopped;
        this.mRequestedVisible = z;
    }

    public void windowStopped(boolean stopped) {
        this.mWindowStopped = stopped;
        updateRequestedVisibility();
        this.mFromStopped = true;
        updateSurface();
        this.mFromStopped = false;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewRootImpl().addWindowStoppedCallback(this);
        boolean z = false;
        this.mWindowStopped = false;
        if (getVisibility() == 0) {
            z = true;
        }
        this.mViewVisibility = z;
        updateRequestedVisibility();
        this.mAttachedToWindow = true;
        this.mParent.requestTransparentRegion(this);
        if (!this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnScrollChangedListener(this.mScrollChangedListener);
            observer.addOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = true;
        }
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mWindowVisibility = visibility == 0;
        updateRequestedVisibility();
        if (!this.mWindowVisibility) {
            this.mVisiblityChangeState = true;
        }
        updateSurface();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean newRequestedVisible = false;
        this.mViewVisibility = visibility == 0;
        if (this.mWindowVisibility && this.mViewVisibility && !this.mWindowStopped) {
            newRequestedVisible = true;
        }
        if (newRequestedVisible != this.mRequestedVisible) {
            requestLayout();
        }
        this.mRequestedVisible = newRequestedVisible;
        updateSurface();
    }

    private void performDrawFinished() {
        if (this.mPendingReportDraws > 0) {
            this.mDrawFinished = true;
            if (this.mAttachedToWindow) {
                notifyDrawFinished();
                invalidate();
                return;
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.identityHashCode(this));
        stringBuilder.append("finished drawing but no pending report draw (extra call to draw completion runnable?)");
        Log.e(str, stringBuilder.toString());
    }

    void notifyDrawFinished() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.pendingDrawFinished();
        }
        this.mPendingReportDraws--;
    }

    protected void onDetachedFromWindow() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.removeWindowStoppedCallback(this);
        }
        this.mAttachedToWindow = false;
        if (this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.removeOnScrollChangedListener(this.mScrollChangedListener);
            observer.removeOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = false;
        }
        while (this.mPendingReportDraws > 0) {
            notifyDrawFinished();
        }
        this.mRequestedVisible = false;
        updateSurface();
        synchronized (this.sObjectLock) {
            if (this.mSurfaceControl != null) {
                this.mSurfaceControl.destroy();
            }
            this.mSurfaceControl = null;
        }
        this.mHaveFrame = false;
        super.onDetachedFromWindow();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        if (this.mRequestedWidth >= 0) {
            width = View.resolveSizeAndState(this.mRequestedWidth, widthMeasureSpec, 0);
        } else {
            width = View.getDefaultSize(0, widthMeasureSpec);
        }
        if (this.mRequestedHeight >= 0) {
            height = View.resolveSizeAndState(this.mRequestedHeight, heightMeasureSpec, 0);
        } else {
            height = View.getDefaultSize(0, heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean result = super.setFrame(left, top, right, bottom);
        updateSurface();
        return result;
    }

    public boolean gatherTransparentRegion(Region region) {
        if (isAboveParent() || !this.mDrawFinished) {
            return super.gatherTransparentRegion(region);
        }
        boolean opaque = true;
        if ((this.mPrivateFlags & 128) == 0) {
            opaque = super.gatherTransparentRegion(region);
        } else if (region != null) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                getLocationInWindow(this.mLocation);
                int l = this.mLocation[0];
                int t = this.mLocation[1];
                region.op(l, t, l + w, t + h, Op.UNION);
            }
        }
        if (PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            opaque = false;
        }
        return opaque;
    }

    public void draw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 0) {
            canvas.drawColor(0, Mode.CLEAR);
        }
        super.draw(canvas);
    }

    protected void dispatchDraw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 128) {
            canvas.drawColor(0, Mode.CLEAR);
        }
        super.dispatchDraw(canvas);
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        this.mSubLayer = isMediaOverlay ? -1 : -2;
    }

    public void setZOrderOnTop(boolean onTop) {
        if (onTop) {
            this.mSubLayer = 1;
        } else {
            this.mSubLayer = -2;
        }
    }

    public void setSecure(boolean isSecure) {
        if (isSecure) {
            this.mSurfaceFlags |= 128;
        } else {
            this.mSurfaceFlags &= -129;
        }
    }

    private void updateOpaqueFlag() {
        if (PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            this.mSurfaceFlags &= -1025;
        } else {
            this.mSurfaceFlags |= 1024;
        }
    }

    private Rect getParentSurfaceInsets() {
        ViewRootImpl root = getViewRootImpl();
        if (root == null) {
            return null;
        }
        return root.mWindowAttributes.surfaceInsets;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:300:0x04bc=Splitter:B:300:0x04bc, B:346:0x052c=Splitter:B:346:0x052c} */
    /* JADX WARNING: Removed duplicated region for block: B:267:0x0461 A:{Catch:{ all -> 0x0469 }} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x03d1 A:{Catch:{ all -> 0x03c6, all -> 0x03d9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x03f7 A:{SYNTHETIC, Splitter:B:232:0x03f7} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x03d1 A:{Catch:{ all -> 0x03c6, all -> 0x03d9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x03f7 A:{SYNTHETIC, Splitter:B:232:0x03f7} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x0379  */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x03d1 A:{Catch:{ all -> 0x03c6, all -> 0x03d9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x03f7 A:{SYNTHETIC, Splitter:B:232:0x03f7} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0364 A:{SYNTHETIC, Splitter:B:191:0x0364} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x0379  */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x03d1 A:{Catch:{ all -> 0x03c6, all -> 0x03d9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x03f7 A:{SYNTHETIC, Splitter:B:232:0x03f7} */
    /* JADX WARNING: Removed duplicated region for block: B:289:0x04aa A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x04bd A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0504 A:{SYNTHETIC, Splitter:B:329:0x0504} */
    /* JADX WARNING: Removed duplicated region for block: B:318:0x04f1 A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:318:0x04f1 A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0504 A:{SYNTHETIC, Splitter:B:329:0x0504} */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0504 A:{SYNTHETIC, Splitter:B:329:0x0504} */
    /* JADX WARNING: Removed duplicated region for block: B:318:0x04f1 A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:318:0x04f1 A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0504 A:{SYNTHETIC, Splitter:B:329:0x0504} */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0504 A:{SYNTHETIC, Splitter:B:329:0x0504} */
    /* JADX WARNING: Removed duplicated region for block: B:318:0x04f1 A:{Catch:{ Exception -> 0x01c7 }} */
    /* JADX WARNING: Missing block: B:133:0x0269, code skipped:
            if (r1.mRtHandlingPositionUpdates == false) goto L_0x026b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void updateSurface() {
        Exception ex;
        boolean redrawNeeded;
        Throwable th;
        if (this.mHaveFrame) {
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot == null || viewRoot.mSurface == null || !viewRoot.mSurface.isValid()) {
                if (!(viewRoot == null || viewRoot.mSurface == null || viewRoot.mSurface.isValid() || this.mSurfaceControl == null || !this.mFromStopped || !this.mDelayDestory)) {
                    Log.i(TAG, "need destroy surface control");
                    synchronized (this.sObjectLock) {
                        this.mSurfaceControl.destroy();
                        this.mSurfaceControl = null;
                    }
                    this.mDelayDestory = false;
                }
                return;
            }
            boolean positionChanged;
            boolean layoutSizeChanged;
            ViewRootImpl impl;
            this.mTranslator = viewRoot.mTranslator;
            if (this.mTranslator != null) {
                this.mSurface.setCompatibilityTranslator(this.mTranslator);
            }
            int myWidth = this.mRequestedWidth;
            if (myWidth <= 0) {
                myWidth = getWidth();
            }
            int myWidth2 = myWidth;
            myWidth = this.mRequestedHeight;
            if (myWidth <= 0) {
                myWidth = getHeight();
            }
            int myHeight = myWidth;
            boolean formatChanged = this.mFormat != this.mRequestedFormat;
            boolean visibleChanged = this.mVisible != this.mRequestedVisible;
            boolean z = (this.mSurfaceControl == null || formatChanged || visibleChanged) && this.mRequestedVisible;
            boolean creating = z;
            z = (this.mSurfaceWidth == myWidth2 && this.mSurfaceHeight == myHeight) ? false : true;
            boolean sizeChanged = z;
            boolean windowVisibleChanged = this.mWindowVisibility != this.mLastWindowVisibility;
            boolean z2;
            if (creating || formatChanged || sizeChanged || visibleChanged) {
            } else if (windowVisibleChanged) {
                z2 = windowVisibleChanged;
            } else {
                getLocationInSurface(this.mLocation);
                z = (this.mWindowSpaceLeft == this.mLocation[0] && this.mWindowSpaceTop == this.mLocation[1]) ? false : true;
                positionChanged = z;
                z = (getWidth() == this.mScreenRect.width() && getHeight() == this.mScreenRect.height()) ? false : true;
                layoutSizeChanged = z;
                if (positionChanged || layoutSizeChanged) {
                    this.mWindowSpaceLeft = this.mLocation[0];
                    this.mWindowSpaceTop = this.mLocation[1];
                    this.mLocation[0] = getWidth();
                    this.mLocation[1] = getHeight();
                    this.mScreenRect.set(this.mWindowSpaceLeft, this.mWindowSpaceTop, this.mWindowSpaceLeft + this.mLocation[0], this.mWindowSpaceTop + this.mLocation[1]);
                    if (getDisplay() != null && HwPCUtils.isValidExtDisplayId(getDisplay().getDisplayId())) {
                        impl = getViewRootImpl();
                        if (!(impl == null || impl.mBasePackageName == null || !impl.mBasePackageName.equals(GALLERY))) {
                            this.mScreenRect.set(this.mWindowSpaceLeft, this.mWindowSpaceTop, this.mWindowSpaceLeft + this.mLocation[0], this.mWindowSpaceTop + this.mLocation[1]);
                        }
                    }
                    if (this.mTranslator != null) {
                        this.mTranslator.translateRectInAppWindowToScreen(this.mScreenRect);
                    }
                    if (this.mSurfaceControl != null) {
                        if (!(isHardwareAccelerated() && this.mRtHandlingPositionUpdates)) {
                            try {
                                setParentSpaceRectangle(this.mScreenRect, -1);
                            } catch (Exception ex2) {
                                Log.e(TAG, "Exception configuring surface", ex2);
                            }
                        }
                    }
                    return;
                }
                z2 = windowVisibleChanged;
            }
            getLocationInWindow(this.mLocation);
            try {
                boolean z3;
                z = this.mRequestedVisible;
                this.mVisible = z;
                boolean visible = z;
                this.mWindowSpaceLeft = this.mLocation[0];
                this.mWindowSpaceTop = this.mLocation[1];
                this.mSurfaceWidth = myWidth2;
                this.mSurfaceHeight = myHeight;
                this.mFormat = this.mRequestedFormat;
                this.mLastWindowVisibility = this.mWindowVisibility;
                this.mScreenRect.left = this.mWindowSpaceLeft;
                this.mScreenRect.top = this.mWindowSpaceTop;
                this.mScreenRect.right = this.mWindowSpaceLeft + getWidth();
                this.mScreenRect.bottom = this.mWindowSpaceTop + getHeight();
                if (this.mTranslator != null) {
                    try {
                        this.mTranslator.translateRectInAppWindowToScreen(this.mScreenRect);
                    } catch (Exception e) {
                        ex2 = e;
                    }
                }
                Rect surfaceInsets = getParentSurfaceInsets();
                this.mScreenRect.offset(surfaceInsets.left, surfaceInsets.top);
                if (creating) {
                    this.mSurfaceSession = new SurfaceSession(viewRoot.mSurface);
                    this.mDeferredDestroySurfaceControl = this.mSurfaceControl;
                    updateOpaqueFlag();
                    String name = new StringBuilder();
                    name.append("SurfaceView - ");
                    name.append(viewRoot.getTitle().toString());
                    redrawNeeded = false;
                    try {
                        this.mSurfaceControl = new SurfaceControlWithBackground(name.toString(), (this.mSurfaceFlags & 1024) != 0, new Builder(this.mSurfaceSession).setSize(this.mSurfaceWidth, this.mSurfaceHeight).setFormat(this.mFormat).setFlags(this.mSurfaceFlags));
                    } catch (Exception e2) {
                        ex2 = e2;
                        z3 = redrawNeeded;
                        Log.e(TAG, "Exception configuring surface", ex2);
                    }
                }
                redrawNeeded = false;
                if (this.mSurfaceControl == null) {
                    return;
                }
                this.mSurfaceLock.lock();
                Rect rect;
                try {
                    float resolutionRatio;
                    this.mDrawingStopped = !visible;
                    SurfaceControl.openTransaction();
                    try {
                        this.mSurfaceControl.setLayer(this.mSubLayer);
                        if (this.mViewVisibility) {
                            try {
                                this.mSurfaceControl.show();
                            } catch (Throwable th2) {
                                th = th2;
                                rect = surfaceInsets;
                            }
                        } else {
                            this.mSurfaceControl.hide();
                        }
                        if (!(sizeChanged || creating)) {
                        }
                        this.mSurfaceControl.setPosition((float) this.mScreenRect.left, (float) this.mScreenRect.top);
                        this.mSurfaceControl.setMatrix(((float) this.mScreenRect.width()) / ((float) this.mSurfaceWidth), 0.0f, 0.0f, ((float) this.mScreenRect.height()) / ((float) this.mSurfaceHeight));
                        if (getDisplay() != null) {
                            if (HwPCUtils.isValidExtDisplayId(getDisplay().getDisplayId())) {
                                impl = getViewRootImpl();
                                if (!(impl == null || impl.mBasePackageName == null || !impl.mBasePackageName.equals(GALLERY))) {
                                    setWindowCrop(this.mScreenRect, this.mSurfaceControl, surfaceInsets);
                                }
                            }
                        }
                        resolutionRatio = -1.0f;
                        if (HwFrameworkFactory.getApsManager() != null) {
                            resolutionRatio = HwFrameworkFactory.getApsManager().getResolution(viewRoot.mBasePackageName);
                        }
                        if (0.0f < resolutionRatio && resolutionRatio < 1.0f) {
                            this.mSurfaceControl.setSurfaceLowResolutionInfo(1.0f / resolutionRatio, 2);
                        }
                    } catch (Exception ex22) {
                        Slog.e(TAG, "Exception is thrown when get/set resolution ratio", ex22);
                    } catch (Throwable th3) {
                        th = th3;
                        rect = surfaceInsets;
                        try {
                            SurfaceControl.closeTransaction();
                            throw th;
                        } catch (Throwable th4) {
                            th = th4;
                            z3 = redrawNeeded;
                            this.mSurfaceLock.unlock();
                            throw th;
                        }
                    }
                    if (sizeChanged) {
                        this.mSurfaceControl.setSize(this.mSurfaceWidth, this.mSurfaceHeight);
                    }
                    SurfaceControl.closeTransaction();
                    if (sizeChanged || creating) {
                        z3 = true;
                    } else {
                        z3 = redrawNeeded;
                    }
                    try {
                        Callback[] callbacks;
                        boolean z4;
                        this.mSurfaceFrame.left = 0;
                        this.mSurfaceFrame.top = 0;
                        if (this.mTranslator == null) {
                            try {
                                this.mSurfaceFrame.right = this.mSurfaceWidth;
                                this.mSurfaceFrame.bottom = this.mSurfaceHeight;
                            } catch (Throwable th5) {
                                th = th5;
                                rect = surfaceInsets;
                            }
                        } else {
                            resolutionRatio = this.mTranslator.applicationInvertedScale;
                            this.mSurfaceFrame.right = (int) ((((float) this.mSurfaceWidth) * resolutionRatio) + 0.5f);
                            this.mSurfaceFrame.bottom = (int) ((((float) this.mSurfaceHeight) * resolutionRatio) + 0.5f);
                        }
                        myWidth = this.mSurfaceFrame.right;
                        int surfaceHeight = this.mSurfaceFrame.bottom;
                        if (this.mLastSurfaceWidth == myWidth) {
                            if (this.mLastSurfaceHeight == surfaceHeight) {
                                layoutSizeChanged = false;
                                positionChanged = layoutSizeChanged;
                                this.mLastSurfaceWidth = myWidth;
                                this.mLastSurfaceHeight = surfaceHeight;
                                this.mSurfaceLock.unlock();
                                if (visible) {
                                    try {
                                        if (!this.mDrawFinished) {
                                            myWidth = 1;
                                            z3 |= myWidth;
                                            callbacks = null;
                                            windowVisibleChanged = creating;
                                            if (this.mSurfaceCreated) {
                                                if (windowVisibleChanged || (!visible && visibleChanged)) {
                                                    try {
                                                        this.mSurfaceCreated = false;
                                                        if (this.mSurface.isValid()) {
                                                            Callback[] callbacks2;
                                                            callbacks = getSurfaceCallbacks();
                                                            int length = callbacks.length;
                                                            int i = 0;
                                                            while (i < length) {
                                                                callbacks2 = callbacks;
                                                                rect = surfaceInsets;
                                                                callbacks[i].surfaceDestroyed(this.mSurfaceHolder);
                                                                i++;
                                                                callbacks = callbacks2;
                                                                surfaceInsets = rect;
                                                            }
                                                            callbacks2 = callbacks;
                                                            rect = surfaceInsets;
                                                            if (this.mSurface.isValid()) {
                                                                this.mSurface.forceScopedDisconnect();
                                                            }
                                                            LogPower.push(142);
                                                            callbacks = callbacks2;
                                                            if (creating) {
                                                                this.mSurface.copyFrom(this.mSurfaceControl);
                                                            }
                                                            if (sizeChanged && getContext().getApplicationInfo().targetSdkVersion < 26) {
                                                                this.mSurface.createFrom(this.mSurfaceControl);
                                                            }
                                                            if (visible) {
                                                                try {
                                                                    if (this.mSurface.isValid()) {
                                                                        int length2;
                                                                        if (!this.mSurfaceCreated && (windowVisibleChanged || visibleChanged)) {
                                                                            Callback[] callbacks3;
                                                                            this.mSurfaceCreated = true;
                                                                            this.mIsCreating = true;
                                                                            if (callbacks == null) {
                                                                                callbacks = getSurfaceCallbacks();
                                                                            }
                                                                            length2 = callbacks.length;
                                                                            length = 0;
                                                                            while (length < length2) {
                                                                                callbacks3 = callbacks;
                                                                                callbacks[length].surfaceCreated(this.mSurfaceHolder);
                                                                                length++;
                                                                                callbacks = callbacks3;
                                                                            }
                                                                            callbacks3 = callbacks;
                                                                            LogPower.push(141);
                                                                            callbacks = callbacks3;
                                                                        }
                                                                        if (!(creating || formatChanged || sizeChanged || visibleChanged)) {
                                                                            if (!positionChanged) {
                                                                                z4 = positionChanged;
                                                                                if (z3) {
                                                                                    if (callbacks == null) {
                                                                                        callbacks = getSurfaceCallbacks();
                                                                                    }
                                                                                    this.mPendingReportDraws++;
                                                                                    viewRoot.drawPending();
                                                                                    new SurfaceCallbackHelper(new -$$Lambda$SurfaceView$SyyzxOgxKwZMRgiiTGcRYbOU5JY(this)).dispatchSurfaceRedrawNeededAsync(this.mSurfaceHolder, callbacks);
                                                                                }
                                                                                this.mIsCreating = false;
                                                                                if (!(this.mSurfaceControl == null || this.mSurfaceCreated)) {
                                                                                    this.mSurface.release();
                                                                                    if (!this.mWindowStopped || (this.mFromStopped && this.mDelayDestory)) {
                                                                                        if (this.mVisiblityChangeState) {
                                                                                            Log.i(TAG, "delay destroy surface control");
                                                                                            this.mDelayDestory = true;
                                                                                        } else {
                                                                                            synchronized (this.sObjectLock) {
                                                                                                try {
                                                                                                    this.mSurfaceControl.destroy();
                                                                                                    this.mSurfaceControl = null;
                                                                                                } catch (Throwable th6) {
                                                                                                    while (true) {
                                                                                                        th = th6;
                                                                                                    }
                                                                                                    throw th;
                                                                                                }
                                                                                            }
                                                                                            this.mDelayDestory = false;
                                                                                        }
                                                                                        this.mVisiblityChangeState = false;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        if (callbacks == null) {
                                                                            callbacks = getSurfaceCallbacks();
                                                                        }
                                                                        length2 = callbacks.length;
                                                                        length = 0;
                                                                        while (length < length2) {
                                                                            Callback[] callbacks4 = callbacks;
                                                                            z4 = positionChanged;
                                                                            try {
                                                                                callbacks[length].surfaceChanged(this.mSurfaceHolder, this.mFormat, myWidth2, myHeight);
                                                                                length++;
                                                                                callbacks = callbacks4;
                                                                                positionChanged = z4;
                                                                            } catch (Throwable th7) {
                                                                                th = th7;
                                                                                this.mIsCreating = false;
                                                                                this.mSurface.release();
                                                                                if (this.mVisiblityChangeState) {
                                                                                }
                                                                                this.mVisiblityChangeState = false;
                                                                                throw th;
                                                                            }
                                                                        }
                                                                        z4 = positionChanged;
                                                                        if (z3) {
                                                                        }
                                                                        this.mIsCreating = false;
                                                                        this.mSurface.release();
                                                                        if (this.mVisiblityChangeState) {
                                                                        }
                                                                        this.mVisiblityChangeState = false;
                                                                    }
                                                                } catch (Throwable th8) {
                                                                    th = th8;
                                                                    z4 = positionChanged;
                                                                    this.mIsCreating = false;
                                                                    if (!(this.mSurfaceControl == null || this.mSurfaceCreated)) {
                                                                        this.mSurface.release();
                                                                        if (!this.mWindowStopped || (this.mFromStopped && this.mDelayDestory)) {
                                                                            if (this.mVisiblityChangeState) {
                                                                                synchronized (this.sObjectLock) {
                                                                                    try {
                                                                                        this.mSurfaceControl.destroy();
                                                                                        this.mSurfaceControl = null;
                                                                                    } catch (Throwable th9) {
                                                                                        while (true) {
                                                                                            th = th9;
                                                                                        }
                                                                                        throw th;
                                                                                    }
                                                                                }
                                                                                this.mDelayDestory = false;
                                                                            } else {
                                                                                Log.i(TAG, "delay destroy surface control");
                                                                                this.mDelayDestory = true;
                                                                            }
                                                                            this.mVisiblityChangeState = false;
                                                                        }
                                                                    }
                                                                    throw th;
                                                                }
                                                            }
                                                            this.mIsCreating = false;
                                                            this.mSurface.release();
                                                            if (this.mVisiblityChangeState) {
                                                            }
                                                            this.mVisiblityChangeState = false;
                                                        }
                                                    } catch (Throwable th10) {
                                                        th = th10;
                                                        z4 = positionChanged;
                                                        this.mIsCreating = false;
                                                        this.mSurface.release();
                                                        if (this.mVisiblityChangeState) {
                                                        }
                                                        this.mVisiblityChangeState = false;
                                                        throw th;
                                                    }
                                                }
                                                rect = surfaceInsets;
                                                if (creating) {
                                                }
                                                this.mSurface.createFrom(this.mSurfaceControl);
                                                if (visible) {
                                                }
                                                this.mIsCreating = false;
                                                this.mSurface.release();
                                                if (this.mVisiblityChangeState) {
                                                }
                                                this.mVisiblityChangeState = false;
                                            }
                                            if (creating) {
                                            }
                                            this.mSurface.createFrom(this.mSurfaceControl);
                                            if (visible) {
                                            }
                                            this.mIsCreating = false;
                                            this.mSurface.release();
                                            if (this.mVisiblityChangeState) {
                                            }
                                            this.mVisiblityChangeState = false;
                                        }
                                    } catch (Throwable th11) {
                                        th = th11;
                                        z4 = positionChanged;
                                        rect = surfaceInsets;
                                        this.mIsCreating = false;
                                        this.mSurface.release();
                                        if (this.mVisiblityChangeState) {
                                        }
                                        this.mVisiblityChangeState = false;
                                        throw th;
                                    }
                                }
                                myWidth = 0;
                                z3 |= myWidth;
                                callbacks = null;
                                windowVisibleChanged = creating;
                                if (this.mSurfaceCreated) {
                                }
                                if (creating) {
                                }
                                this.mSurface.createFrom(this.mSurfaceControl);
                                if (visible) {
                                }
                                this.mIsCreating = false;
                                this.mSurface.release();
                                if (this.mVisiblityChangeState) {
                                }
                                this.mVisiblityChangeState = false;
                            }
                        }
                        layoutSizeChanged = true;
                        positionChanged = layoutSizeChanged;
                        try {
                            this.mLastSurfaceWidth = myWidth;
                            this.mLastSurfaceHeight = surfaceHeight;
                            this.mSurfaceLock.unlock();
                            if (visible) {
                            }
                            myWidth = 0;
                            z3 |= myWidth;
                            callbacks = null;
                            windowVisibleChanged = creating;
                            try {
                                if (this.mSurfaceCreated) {
                                }
                                if (creating) {
                                }
                                this.mSurface.createFrom(this.mSurfaceControl);
                                if (visible) {
                                }
                                this.mIsCreating = false;
                                this.mSurface.release();
                                if (this.mVisiblityChangeState) {
                                }
                                this.mVisiblityChangeState = false;
                            } catch (Throwable th12) {
                                th = th12;
                                z4 = positionChanged;
                                rect = surfaceInsets;
                                this.mIsCreating = false;
                                this.mSurface.release();
                                if (this.mVisiblityChangeState) {
                                }
                                this.mVisiblityChangeState = false;
                                throw th;
                            }
                        } catch (Throwable th13) {
                            th = th13;
                            z4 = positionChanged;
                            rect = surfaceInsets;
                            this.mSurfaceLock.unlock();
                            throw th;
                        }
                    } catch (Throwable th14) {
                        th = th14;
                        rect = surfaceInsets;
                        this.mSurfaceLock.unlock();
                        throw th;
                    }
                } catch (Throwable th15) {
                    th = th15;
                    rect = surfaceInsets;
                    z3 = redrawNeeded;
                }
            } catch (Exception e3) {
                ex22 = e3;
                redrawNeeded = false;
                Log.e(TAG, "Exception configuring surface", ex22);
            }
        }
    }

    private void onDrawFinished() {
        if (this.mDeferredDestroySurfaceControl != null) {
            this.mDeferredDestroySurfaceControl.destroy();
            this.mDeferredDestroySurfaceControl = null;
        }
        runOnUiThread(new -$$Lambda$SurfaceView$Cs7TGTdA1lXf9qW8VOJAfEsMjdk(this));
    }

    protected void applyChildSurfaceTransaction_renderWorker(Transaction t, Surface viewRootSurface, long nextViewRootFrameNumber) {
    }

    private void applySurfaceTransforms(SurfaceControl surface, Rect position, long frameNumber) {
        if (frameNumber > 0) {
            this.mRtTransaction.deferTransactionUntilSurface(surface, getViewRootImpl().mSurface, frameNumber);
        }
        synchronized (this.sObjectLock) {
            this.mRtTransaction.setPosition(surface, (float) position.left, (float) position.top);
        }
        this.mRtTransaction.setMatrix(surface, ((float) position.width()) / ((float) this.mSurfaceWidth), 0.0f, 0.0f, ((float) position.height()) / ((float) this.mSurfaceHeight));
    }

    private void setParentSpaceRectangle(Rect position, long frameNumber) {
        ViewRootImpl viewRoot = getViewRootImpl();
        applySurfaceTransforms(this.mSurfaceControl, position, frameNumber);
        applySurfaceTransforms(this.mSurfaceControl.mBackgroundControl, position, frameNumber);
        applyChildSurfaceTransaction_renderWorker(this.mRtTransaction, viewRoot.mSurface, frameNumber);
        this.mRtTransaction.apply();
    }

    private void setWindowCrop(Rect position, SurfaceControl surfaceControl, Rect surfaceInsets) {
        this.mTmpRect.set(0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
        if (position.left < surfaceInsets.left) {
            if (position.left + this.mSurfaceWidth > surfaceInsets.left) {
                this.mTmpRect.left = surfaceInsets.left - position.left;
                this.mTmpRect.right = this.mSurfaceWidth;
            } else {
                this.mTmpRect.left = 0;
                this.mTmpRect.right = this.mSurfaceWidth - surfaceInsets.left;
            }
        } else if (position.left > this.mSurfaceWidth + surfaceInsets.left) {
            this.mTmpRect.left = surfaceInsets.right;
            this.mTmpRect.right = this.mSurfaceWidth;
        } else {
            this.mTmpRect.left = 0;
            this.mTmpRect.right = (this.mSurfaceWidth + surfaceInsets.left) - position.left;
        }
        surfaceControl.setWindowCrop(this.mTmpRect);
    }

    public final void updateSurfacePosition_renderWorker(long frameNumber, int left, int top, int right, int bottom) {
        if (this.mSurfaceControl != null) {
            this.mRtHandlingPositionUpdates = true;
            if (this.mRTLastReportedPosition.left != left || this.mRTLastReportedPosition.top != top || this.mRTLastReportedPosition.right != right || this.mRTLastReportedPosition.bottom != bottom) {
                try {
                    this.mRTLastReportedPosition.set(left, top, right, bottom);
                    setParentSpaceRectangle(this.mRTLastReportedPosition, frameNumber);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception from repositionChild", ex);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002a, code skipped:
            if (r4.mRtHandlingPositionUpdates == false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:11:0x002c, code skipped:
            r4.mRtHandlingPositionUpdates = false;
     */
    /* JADX WARNING: Missing block: B:12:0x0035, code skipped:
            if (r4.mScreenRect.isEmpty() != false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:14:0x003f, code skipped:
            if (r4.mScreenRect.equals(r4.mRTLastReportedPosition) != false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            setParentSpaceRectangle(r4.mScreenRect, r5);
     */
    /* JADX WARNING: Missing block: B:17:0x0047, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x0048, code skipped:
            android.util.Log.e(TAG, "Exception configuring surface", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final void surfacePositionLost_uiRtSync(long frameNumber) {
        this.mRTLastReportedPosition.setEmpty();
        synchronized (this.sObjectLock) {
            if (this.mSurfaceControl == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("surfacePositionLost_uiRtSync  mSurfaceControl = ");
                stringBuilder.append(this.mSurfaceControl);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private Callback[] getSurfaceCallbacks() {
        Callback[] callbacks;
        synchronized (this.mCallbacks) {
            callbacks = new Callback[this.mCallbacks.size()];
            this.mCallbacks.toArray(callbacks);
        }
        return callbacks;
    }

    private void runOnUiThread(Runnable runnable) {
        Handler handler = getHandler();
        if (handler == null || handler.getLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    public boolean isFixedSize() {
        return (this.mRequestedWidth == -1 && this.mRequestedHeight == -1) ? false : true;
    }

    private boolean isAboveParent() {
        return this.mSubLayer >= 0;
    }

    public void setResizeBackgroundColor(int bgColor) {
        this.mSurfaceControl.setBackgroundColor(bgColor);
    }
}
