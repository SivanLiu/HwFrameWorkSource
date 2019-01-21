package android.view;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable.VectorDrawableAnimatorRT;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.TimeUtils;
import android.view.IGraphicsStatsCallback.Stub;
import android.view.Surface.OutOfResourcesException;
import android.view.animation.AnimationUtils;
import com.android.internal.R;
import com.android.internal.util.VirtualRefBasePtr;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ThreadedRenderer {
    private static final String CACHE_PATH_SHADERS = "com.android.opengl.shaders_cache";
    private static final String CACHE_PATH_SKIASHADERS = "com.android.skia.shaders_cache";
    public static final String DEBUG_DIRTY_REGIONS_PROPERTY = "debug.hwui.show_dirty_regions";
    public static final String DEBUG_FPS_DIVISOR = "debug.hwui.fps_divisor";
    public static final String DEBUG_OVERDRAW_PROPERTY = "debug.hwui.overdraw";
    public static final String DEBUG_SHOW_LAYERS_UPDATES_PROPERTY = "debug.hwui.show_layers_updates";
    public static final String DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY = "debug.hwui.show_non_rect_clip";
    public static int EGL_CONTEXT_PRIORITY_HIGH_IMG = 12545;
    public static int EGL_CONTEXT_PRIORITY_LOW_IMG = 12547;
    public static int EGL_CONTEXT_PRIORITY_MEDIUM_IMG = 12546;
    private static final int FLAG_DUMP_ALL = 1;
    private static final int FLAG_DUMP_FRAMESTATS = 1;
    private static final int FLAG_DUMP_RESET = 2;
    private static final String LOG_TAG = "ThreadedRenderer";
    public static final String OVERDRAW_PROPERTY_SHOW = "show";
    static final String PRINT_CONFIG_PROPERTY = "debug.hwui.print_config";
    static final String PROFILE_MAXFRAMES_PROPERTY = "debug.hwui.profile.maxframes";
    public static final String PROFILE_PROPERTY = "debug.hwui.profile";
    public static final String PROFILE_PROPERTY_VISUALIZE_BARS = "visual_bars";
    private static final int SYNC_CONTEXT_IS_STOPPED = 4;
    private static final int SYNC_FRAME_DROPPED = 8;
    private static final int SYNC_INVALIDATE_REQUIRED = 1;
    private static final int SYNC_LOST_SURFACE_REWARD_IF_FOUND = 2;
    private static final int SYNC_OK = 0;
    private static final String[] VISUALIZERS = new String[]{PROFILE_PROPERTY_VISUALIZE_BARS};
    public static boolean sRendererDisabled = false;
    private static Boolean sSupportsOpenGL;
    public static boolean sSystemRendererDisabled = false;
    public static boolean sTrimForeground = false;
    private final int mAmbientShadowAlpha;
    private HwCustRenderThreadMonitor mCustMonitor;
    private boolean mEnabled;
    private boolean mHasInsets;
    private int mHeight;
    private boolean mInitialized = false;
    private int mInsetLeft;
    private int mInsetTop;
    private boolean mIsOpaque = false;
    private long[] mJankDrawData = new long[4];
    private final float mLightRadius;
    private final float mLightY;
    private final float mLightZ;
    private long mNativeProxy;
    private boolean mRequested = true;
    private RenderNode mRootNode;
    private boolean mRootNodeNeedsUpdate;
    private final int mSpotShadowAlpha;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private int mWidth;

    interface DrawCallbacks {
        void onPostDraw(DisplayListCanvas displayListCanvas);

        void onPreDraw(DisplayListCanvas displayListCanvas);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DumpFlags {
    }

    public interface FrameCompleteCallback {
        void onFrameComplete(long j);
    }

    public interface FrameDrawingCallback {
        void onFrameDraw(long j);
    }

    private static class ProcessInitializer {
        static ProcessInitializer sInstance = new ProcessInitializer();
        private Context mAppContext;
        private IGraphicsStatsCallback mGraphicsStatsCallback = new Stub() {
            public void onRotateGraphicsStatsBuffer() throws RemoteException {
                ProcessInitializer.this.rotateBuffer();
            }
        };
        private IGraphicsStats mGraphicsStatsService;
        private boolean mInitialized = false;

        private ProcessInitializer() {
        }

        /* JADX WARNING: Missing block: B:12:0x001b, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        synchronized void init(Context context, long renderProxy) {
            if (!this.mInitialized) {
                this.mInitialized = true;
                this.mAppContext = context.getApplicationContext();
                initSched(renderProxy);
                if (this.mAppContext != null) {
                    initGraphicsStats();
                }
            }
        }

        private void initSched(long renderProxy) {
            try {
                ActivityManager.getService().setRenderThread(ThreadedRenderer.nGetRenderThreadTid(renderProxy));
            } catch (Throwable t) {
                Log.w(ThreadedRenderer.LOG_TAG, "Failed to set scheduler for RenderThread", t);
            }
        }

        private void initGraphicsStats() {
            try {
                IBinder binder = ServiceManager.getService("graphicsstats");
                if (binder != null) {
                    this.mGraphicsStatsService = IGraphicsStats.Stub.asInterface(binder);
                    requestBuffer();
                }
            } catch (Throwable t) {
                Log.w(ThreadedRenderer.LOG_TAG, "Could not acquire gfx stats buffer", t);
            }
        }

        private void rotateBuffer() {
            ThreadedRenderer.nRotateProcessStatsBuffer();
            requestBuffer();
        }

        private void requestBuffer() {
            try {
                ParcelFileDescriptor pfd = this.mGraphicsStatsService.requestBufferForProcess(this.mAppContext.getApplicationInfo().packageName, this.mGraphicsStatsCallback);
                ThreadedRenderer.nSetProcessStatsBuffer(pfd.getFd());
                pfd.close();
            } catch (Throwable t) {
                Log.w(ThreadedRenderer.LOG_TAG, "Could not acquire gfx stats buffer", t);
            }
        }
    }

    public static class SimpleRenderer {
        private final FrameInfo mFrameInfo = new FrameInfo();
        private final float mLightY;
        private final float mLightZ;
        private long mNativeProxy;
        private final RenderNode mRootNode;
        private Surface mSurface;

        public SimpleRenderer(Context context, String name, Surface surface) {
            TypedArray a = context.obtainStyledAttributes(null, R.styleable.Lighting, 0, 0);
            this.mLightY = a.getDimension(3, 0.0f);
            this.mLightZ = a.getDimension(4, 0.0f);
            float lightRadius = a.getDimension(2.8E-45f, 0.0f);
            int ambientShadowAlpha = (int) ((a.getFloat(0, 0.0f) * 255.0f) + 1056964608);
            int spotShadowAlpha = (int) ((255.0f * a.getFloat(1, 0.0f)) + 0.5f);
            a.recycle();
            long rootNodePtr = ThreadedRenderer.nCreateRootRenderNode();
            this.mRootNode = RenderNode.adopt(rootNodePtr);
            this.mRootNode.setClipToBounds(false);
            this.mNativeProxy = ThreadedRenderer.nCreateProxy(true, rootNodePtr);
            ThreadedRenderer.nSetName(this.mNativeProxy, name);
            ProcessInitializer.sInstance.init(context, this.mNativeProxy);
            ThreadedRenderer.nLoadSystemProperties(this.mNativeProxy);
            ThreadedRenderer.nSetup(this.mNativeProxy, lightRadius, ambientShadowAlpha, spotShadowAlpha);
            this.mSurface = surface;
            ThreadedRenderer.nUpdateSurface(this.mNativeProxy, surface);
        }

        public void setLightCenter(Display display, int windowLeft, int windowTop) {
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            ThreadedRenderer.nSetLightCenter(this.mNativeProxy, (((float) displaySize.x) / 2.0f) - ((float) windowLeft), this.mLightY - ((float) windowTop), this.mLightZ);
        }

        public RenderNode getRootNode() {
            return this.mRootNode;
        }

        public void draw(FrameDrawingCallback callback) {
            long vsync = AnimationUtils.currentAnimationTimeMillis() * TimeUtils.NANOS_PER_MS;
            this.mFrameInfo.setVsync(vsync, vsync);
            this.mFrameInfo.addFlags(4);
            if (callback != null) {
                ThreadedRenderer.nSetFrameCallback(this.mNativeProxy, callback);
            }
            ThreadedRenderer.nSyncAndDrawFrame(this.mNativeProxy, this.mFrameInfo.mFrameInfo, this.mFrameInfo.mFrameInfo.length);
        }

        public void destroy() {
            this.mSurface = null;
            ThreadedRenderer.nDestroy(this.mNativeProxy, this.mRootNode.mNativeRenderNode);
        }

        protected void finalize() throws Throwable {
            try {
                ThreadedRenderer.nDeleteProxy(this.mNativeProxy);
                this.mNativeProxy = 0;
            } finally {
                super.finalize();
            }
        }
    }

    public static native void disableVsync();

    private static native long nAddFrameMetricsObserver(long j, FrameMetricsObserver frameMetricsObserver);

    private static native void nAddRenderNode(long j, long j2, boolean z);

    private static native void nAllocateBuffers(long j, Surface surface);

    private static native void nBuildLayer(long j, long j2);

    private static native void nCancelLayerUpdate(long j, long j2);

    private static native boolean nCopyLayerInto(long j, long j2, Bitmap bitmap);

    private static native int nCopySurfaceInto(Surface surface, int i, int i2, int i3, int i4, Bitmap bitmap);

    private static native Bitmap nCreateHardwareBitmap(long j, int i, int i2);

    private static native long nCreateProxy(boolean z, long j);

    private static native long nCreateRootRenderNode();

    private static native long nCreateTextureLayer(long j);

    private static native void nDeleteProxy(long j);

    private static native void nDestroy(long j, long j2);

    private static native void nDestroyHardwareResources(long j);

    private static native void nDetachSurfaceTexture(long j, long j2);

    private static native void nDrawRenderNode(long j, long j2);

    private static native void nDumpProfileInfo(long j, FileDescriptor fileDescriptor, int i);

    private static native void nFence(long j);

    private static native int nGetRenderThreadTid(long j);

    private static native void nHackySetRTAnimationsEnabled(boolean z);

    private static native void nInitialize(long j, Surface surface);

    private static native void nInvokeFunctor(long j, boolean z);

    private static native boolean nLoadSystemProperties(long j);

    private static native void nNotifyFramePending(long j);

    private static native void nOverrideProperty(String str, String str2);

    private static native boolean nPauseSurface(long j, Surface surface);

    private static native void nPushLayerUpdate(long j, long j2);

    private static native void nRegisterAnimatingRenderNode(long j, long j2);

    private static native void nRegisterVectorDrawableAnimator(long j, long j2);

    private static native void nRemoveFrameMetricsObserver(long j, long j2);

    private static native void nRemoveRenderNode(long j, long j2);

    private static native void nRotateProcessStatsBuffer();

    private static native void nSerializeDisplayListTree(long j);

    private static native void nSetContentDrawBounds(long j, int i, int i2, int i3, int i4);

    private static native void nSetContextPriority(int i);

    private static native void nSetDebuggingEnabled(boolean z);

    private static native void nSetFrameCallback(long j, FrameDrawingCallback frameDrawingCallback);

    private static native void nSetFrameCompleteCallback(long j, FrameCompleteCallback frameCompleteCallback);

    private static native void nSetHighContrastText(boolean z);

    private static native void nSetIsolatedProcess(boolean z);

    private static native void nSetLightCenter(long j, float f, float f2, float f3);

    private static native void nSetName(long j, String str);

    private static native void nSetOpaque(long j, boolean z);

    private static native void nSetProcessStatsBuffer(int i);

    private static native void nSetStopped(long j, boolean z);

    private static native void nSetWideGamut(long j, boolean z);

    private static native void nSetup(long j, float f, int i, int i2);

    private static native void nStopDrawing(long j);

    private static native int nSyncAndDrawFrame(long j, long[] jArr, int i);

    private static native void nTrimMemory(int i);

    private static native void nUpdateSurface(long j, Surface surface);

    static native void setupShadersDiskCache(String str, String str2);

    static {
        isAvailable();
    }

    public static void disable(boolean system) {
        sRendererDisabled = true;
        if (system) {
            sSystemRendererDisabled = true;
        }
    }

    public static void enableForegroundTrimming() {
        sTrimForeground = true;
    }

    public static boolean isAvailable() {
        if (sSupportsOpenGL != null) {
            return sSupportsOpenGL.booleanValue();
        }
        boolean z = false;
        if (SystemProperties.getInt("ro.kernel.qemu", 0) == 0) {
            sSupportsOpenGL = Boolean.valueOf(true);
            return true;
        }
        int qemu_gles = SystemProperties.getInt("qemu.gles", -1);
        if (qemu_gles == -1) {
            return false;
        }
        if (qemu_gles > 0) {
            z = true;
        }
        sSupportsOpenGL = Boolean.valueOf(z);
        return sSupportsOpenGL.booleanValue();
    }

    public static void setupDiskCache(File cacheDir) {
        setupShadersDiskCache(new File(cacheDir, CACHE_PATH_SHADERS).getAbsolutePath(), new File(cacheDir, CACHE_PATH_SKIASHADERS).getAbsolutePath());
    }

    public static ThreadedRenderer create(Context context, boolean translucent, String name) {
        if (isAvailable()) {
            return new ThreadedRenderer(context, translucent, name);
        }
        return null;
    }

    public static void trimMemory(int level) {
        nTrimMemory(level);
    }

    public static void overrideProperty(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value must be non-null");
        }
        nOverrideProperty(name, value);
    }

    ThreadedRenderer(Context context, boolean translucent, String name) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.Lighting, 0, 0);
        this.mLightY = a.getDimension(3, 0.0f);
        this.mLightZ = a.getDimension(4, 0.0f);
        this.mLightRadius = a.getDimension(2, 0.0f);
        this.mAmbientShadowAlpha = (int) ((a.getFloat(0, 0.0f) * 255.0f) + 0.5f);
        this.mSpotShadowAlpha = (int) ((255.0f * a.getFloat(1, 0.0f)) + 0.5f);
        a.recycle();
        long rootNodePtr = nCreateRootRenderNode();
        this.mRootNode = RenderNode.adopt(rootNodePtr);
        this.mRootNode.setClipToBounds(false);
        this.mIsOpaque = translucent ^ 1;
        this.mNativeProxy = nCreateProxy(translucent, rootNodePtr);
        nSetName(this.mNativeProxy, name);
        ProcessInitializer.sInstance.init(context, this.mNativeProxy);
        loadSystemProperties();
        if (HwCustRenderThreadMonitor.shouldStartMonitot(context)) {
            this.mCustMonitor = (HwCustRenderThreadMonitor) HwCustUtils.createObj(HwCustRenderThreadMonitor.class, new Object[]{context});
        }
    }

    void destroy() {
        HwCustRenderThreadMonitor hwCustRenderThreadMonitor;
        HwCustRenderThreadMonitor hwCustRenderThreadMonitor2;
        this.mInitialized = false;
        updateEnabledState(null);
        if (this.mCustMonitor != null) {
            hwCustRenderThreadMonitor = this.mCustMonitor;
            hwCustRenderThreadMonitor2 = this.mCustMonitor;
            hwCustRenderThreadMonitor.renderMonitorStart(1);
        }
        nDestroy(this.mNativeProxy, this.mRootNode.mNativeRenderNode);
        if (this.mCustMonitor != null) {
            hwCustRenderThreadMonitor = this.mCustMonitor;
            hwCustRenderThreadMonitor2 = this.mCustMonitor;
            hwCustRenderThreadMonitor.renderMonitorStop(1);
        }
    }

    boolean isEnabled() {
        return this.mEnabled;
    }

    void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    boolean isRequested() {
        return this.mRequested;
    }

    void setRequested(boolean requested) {
        this.mRequested = requested;
    }

    private void updateEnabledState(Surface surface) {
        if (surface == null || !surface.isValid()) {
            setEnabled(false);
        } else {
            setEnabled(this.mInitialized);
        }
    }

    boolean initialize(Surface surface) throws OutOfResourcesException {
        boolean status = this.mInitialized ^ true;
        this.mInitialized = true;
        updateEnabledState(surface);
        nInitialize(this.mNativeProxy, surface);
        return status;
    }

    void allocateBuffers(Surface surface) throws OutOfResourcesException {
        nAllocateBuffers(this.mNativeProxy, surface);
    }

    boolean initializeIfNeeded(int width, int height, AttachInfo attachInfo, Surface surface, Rect surfaceInsets) throws OutOfResourcesException {
        if (!isRequested() || isEnabled() || !initialize(surface)) {
            return false;
        }
        setup(width, height, attachInfo, surfaceInsets);
        return true;
    }

    void updateSurface(Surface surface) throws OutOfResourcesException {
        updateEnabledState(surface);
        nUpdateSurface(this.mNativeProxy, surface);
    }

    boolean pauseSurface(Surface surface) {
        return nPauseSurface(this.mNativeProxy, surface);
    }

    void setStopped(boolean stopped) {
        nSetStopped(this.mNativeProxy, stopped);
    }

    void destroyHardwareResources(View view) {
        destroyResources(view);
        nDestroyHardwareResources(this.mNativeProxy);
    }

    private static void destroyResources(View view) {
        view.destroyHardwareResources();
    }

    void detachSurfaceTexture(long hardwareLayer) {
        nDetachSurfaceTexture(this.mNativeProxy, hardwareLayer);
    }

    void setup(int width, int height, AttachInfo attachInfo, Rect surfaceInsets) {
        this.mWidth = width;
        this.mHeight = height;
        if (surfaceInsets == null || (surfaceInsets.left == 0 && surfaceInsets.right == 0 && surfaceInsets.top == 0 && surfaceInsets.bottom == 0)) {
            this.mHasInsets = false;
            this.mInsetLeft = 0;
            this.mInsetTop = 0;
            this.mSurfaceWidth = width;
            this.mSurfaceHeight = height;
        } else {
            this.mHasInsets = true;
            this.mInsetLeft = surfaceInsets.left;
            this.mInsetTop = surfaceInsets.top;
            this.mSurfaceWidth = (this.mInsetLeft + width) + surfaceInsets.right;
            this.mSurfaceHeight = (this.mInsetTop + height) + surfaceInsets.bottom;
            setOpaque(false);
        }
        this.mRootNode.setLeftTopRightBottom(-this.mInsetLeft, -this.mInsetTop, this.mSurfaceWidth, this.mSurfaceHeight);
        nSetup(this.mNativeProxy, this.mLightRadius, this.mAmbientShadowAlpha, this.mSpotShadowAlpha);
        setLightCenter(attachInfo);
    }

    void setLightCenter(AttachInfo attachInfo) {
        Point displaySize = attachInfo.mPoint;
        attachInfo.mDisplay.getRealSize(displaySize);
        nSetLightCenter(this.mNativeProxy, (((float) displaySize.x) / 2.0f) - ((float) attachInfo.mWindowLeft), this.mLightY - ((float) attachInfo.mWindowTop), this.mLightZ);
    }

    void setOpaque(boolean opaque) {
        boolean z = opaque && !this.mHasInsets;
        this.mIsOpaque = z;
        nSetOpaque(this.mNativeProxy, this.mIsOpaque);
    }

    boolean isOpaque() {
        return this.mIsOpaque;
    }

    void setWideGamut(boolean wideGamut) {
        nSetWideGamut(this.mNativeProxy, wideGamut);
    }

    int getWidth() {
        return this.mWidth;
    }

    int getHeight() {
        return this.mHeight;
    }

    long getJankDrawData(int index) {
        if (index < 0 || index > 3 || this.mJankDrawData == null) {
            return -1;
        }
        return this.mJankDrawData[index];
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0057 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0051  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0057 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0051  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0057 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0051  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x004f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void dumpGfxInfo(PrintWriter pw, FileDescriptor fd, String[] args) {
        pw.flush();
        int flags = (args == null || args.length == 0) ? 1 : 0;
        if (args != null) {
            int flags2 = flags;
            for (String str : args) {
                Object obj;
                int hashCode = str.hashCode();
                if (hashCode != -252053678) {
                    if (hashCode != 1492) {
                        if (hashCode == 108404047 && str.equals("reset")) {
                            obj = 1;
                            switch (obj) {
                                case null:
                                    flags2 |= 1;
                                    break;
                                case 1:
                                    flags2 |= 2;
                                    break;
                                case 2:
                                    flags2 = 1;
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (str.equals("-a")) {
                        obj = 2;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (str.equals("framestats")) {
                    obj = null;
                    switch (obj) {
                        case null:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        default:
                            break;
                    }
                }
                obj = -1;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
            flags = flags2;
        }
        nDumpProfileInfo(this.mNativeProxy, fd, flags);
    }

    boolean loadSystemProperties() {
        boolean changed = nLoadSystemProperties(this.mNativeProxy);
        if (changed) {
            invalidateRoot();
        }
        return changed;
    }

    private void updateViewTreeDisplayList(View view) {
        view.mPrivateFlags |= 32;
        view.mRecreateDisplayList = (view.mPrivateFlags & Integer.MIN_VALUE) == Integer.MIN_VALUE;
        view.mPrivateFlags &= Integer.MAX_VALUE;
        view.updateDisplayListIfDirty();
        view.mRecreateDisplayList = false;
    }

    private void updateRootDisplayList(View view, DrawCallbacks callbacks) {
        Trace.traceBegin(8, "Record View#draw()");
        updateViewTreeDisplayList(view);
        if (this.mRootNodeNeedsUpdate || !this.mRootNode.isValid()) {
            DisplayListCanvas canvas = this.mRootNode.start(this.mSurfaceWidth, this.mSurfaceHeight);
            try {
                int saveCount = canvas.save();
                canvas.translate((float) this.mInsetLeft, (float) this.mInsetTop);
                callbacks.onPreDraw(canvas);
                canvas.insertReorderBarrier();
                canvas.drawRenderNode(view.updateDisplayListIfDirty());
                canvas.insertInorderBarrier();
                callbacks.onPostDraw(canvas);
                canvas.restoreToCount(saveCount);
                this.mRootNodeNeedsUpdate = false;
            } finally {
                this.mRootNode.end(canvas);
            }
        }
        Trace.traceEnd(8);
    }

    public void addRenderNode(RenderNode node, boolean placeFront) {
        nAddRenderNode(this.mNativeProxy, node.mNativeRenderNode, placeFront);
    }

    public void removeRenderNode(RenderNode node) {
        nRemoveRenderNode(this.mNativeProxy, node.mNativeRenderNode);
    }

    public void drawRenderNode(RenderNode node) {
        nDrawRenderNode(this.mNativeProxy, node.mNativeRenderNode);
    }

    public void setContentDrawBounds(int left, int top, int right, int bottom) {
        nSetContentDrawBounds(this.mNativeProxy, left, top, right, bottom);
    }

    void invalidateRoot() {
        this.mRootNodeNeedsUpdate = true;
    }

    void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks, FrameDrawingCallback frameDrawingCallback) {
        HwCustRenderThreadMonitor hwCustRenderThreadMonitor;
        boolean z;
        AttachInfo attachInfo2 = attachInfo;
        FrameDrawingCallback frameDrawingCallback2 = frameDrawingCallback;
        attachInfo2.mIgnoreDirtyState = true;
        long startdraw = System.nanoTime();
        Choreographer choreographer = attachInfo2.mViewRootImpl.mChoreographer;
        choreographer.mFrameInfo.markDrawStart();
        updateRootDisplayList(view, callbacks);
        long step1time = System.nanoTime();
        attachInfo2.mIgnoreDirtyState = false;
        if (attachInfo2.mPendingAnimatingRenderNodes != null) {
            int count = attachInfo2.mPendingAnimatingRenderNodes.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= count) {
                    break;
                }
                int count2 = count;
                registerAnimatingRenderNode((RenderNode) attachInfo2.mPendingAnimatingRenderNodes.get(i2));
                i = i2 + 1;
                count = count2;
            }
            attachInfo2.mPendingAnimatingRenderNodes.clear();
            attachInfo2.mPendingAnimatingRenderNodes = null;
        }
        long[] frameInfo = choreographer.mFrameInfo.mFrameInfo;
        long step2time;
        if (frameDrawingCallback2 != null) {
            step2time = 0;
            nSetFrameCallback(this.mNativeProxy, frameDrawingCallback2);
        } else {
            step2time = 0;
        }
        if (this.mCustMonitor != null) {
            HwCustRenderThreadMonitor hwCustRenderThreadMonitor2 = this.mCustMonitor;
            hwCustRenderThreadMonitor = this.mCustMonitor;
            hwCustRenderThreadMonitor2.renderMonitorStart(0);
        }
        int syncResult = nSyncAndDrawFrame(this.mNativeProxy, frameInfo, frameInfo.length);
        if (this.mCustMonitor != null) {
            hwCustRenderThreadMonitor = this.mCustMonitor;
            HwCustRenderThreadMonitor hwCustRenderThreadMonitor3 = this.mCustMonitor;
            z = false;
            hwCustRenderThreadMonitor.renderMonitorStop(0);
        } else {
            z = false;
        }
        if ((syncResult & 2) != 0) {
            setEnabled(z);
            attachInfo2.mViewRootImpl.mSurface.release();
            attachInfo2.mViewRootImpl.invalidate();
        }
        long step2time2 = System.nanoTime();
        if ((syncResult & 1) != 0) {
            attachInfo2.mViewRootImpl.invalidate();
        }
        long step3time = System.nanoTime();
        this.mJankDrawData[0] = step1time - startdraw;
        this.mJankDrawData[1] = step2time2 - step1time;
        this.mJankDrawData[2] = step3time - step2time2;
        this.mJankDrawData[3] = step3time - startdraw;
    }

    void setFrameCompleteCallback(FrameCompleteCallback callback) {
        nSetFrameCompleteCallback(this.mNativeProxy, callback);
    }

    static void invokeFunctor(long functor, boolean waitForCompletion) {
        nInvokeFunctor(functor, waitForCompletion);
    }

    TextureLayer createTextureLayer() {
        return TextureLayer.adoptTextureLayer(this, nCreateTextureLayer(this.mNativeProxy));
    }

    void buildLayer(RenderNode node) {
        nBuildLayer(this.mNativeProxy, node.getNativeDisplayList());
    }

    boolean copyLayerInto(TextureLayer layer, Bitmap bitmap) {
        return nCopyLayerInto(this.mNativeProxy, layer.getDeferredLayerUpdater(), bitmap);
    }

    void pushLayerUpdate(TextureLayer layer) {
        nPushLayerUpdate(this.mNativeProxy, layer.getDeferredLayerUpdater());
    }

    void onLayerDestroyed(TextureLayer layer) {
        nCancelLayerUpdate(this.mNativeProxy, layer.getDeferredLayerUpdater());
    }

    void fence() {
        nFence(this.mNativeProxy);
    }

    void stopDrawing() {
        nStopDrawing(this.mNativeProxy);
    }

    public void notifyFramePending() {
        nNotifyFramePending(this.mNativeProxy);
    }

    void registerAnimatingRenderNode(RenderNode animator) {
        nRegisterAnimatingRenderNode(this.mRootNode.mNativeRenderNode, animator.mNativeRenderNode);
    }

    void registerVectorDrawableAnimator(VectorDrawableAnimatorRT animator) {
        nRegisterVectorDrawableAnimator(this.mRootNode.mNativeRenderNode, animator.getAnimatorNativePtr());
    }

    public void serializeDisplayListTree() {
        nSerializeDisplayListTree(this.mNativeProxy);
    }

    public static int copySurfaceInto(Surface surface, Rect srcRect, Bitmap bitmap) {
        if (srcRect == null) {
            return nCopySurfaceInto(surface, 0, 0, 0, 0, bitmap);
        }
        return nCopySurfaceInto(surface, srcRect.left, srcRect.top, srcRect.right, srcRect.bottom, bitmap);
    }

    public static Bitmap createHardwareBitmap(RenderNode node, int width, int height) {
        return nCreateHardwareBitmap(node.getNativeDisplayList(), width, height);
    }

    public static void setHighContrastText(boolean highContrastText) {
        nSetHighContrastText(highContrastText);
    }

    public static void setIsolatedProcess(boolean isIsolated) {
        nSetIsolatedProcess(isIsolated);
    }

    public static void setDebuggingEnabled(boolean enable) {
        nSetDebuggingEnabled(enable);
    }

    protected void finalize() throws Throwable {
        try {
            nDeleteProxy(this.mNativeProxy);
            this.mNativeProxy = 0;
        } finally {
            super.finalize();
        }
    }

    void addFrameMetricsObserver(FrameMetricsObserver observer) {
        observer.mNative = new VirtualRefBasePtr(nAddFrameMetricsObserver(this.mNativeProxy, observer));
    }

    void removeFrameMetricsObserver(FrameMetricsObserver observer) {
        nRemoveFrameMetricsObserver(this.mNativeProxy, observer.mNative.get());
        observer.mNative = null;
    }

    public static void setFPSDivisor(int divisor) {
        boolean z = true;
        if (divisor > 1) {
            z = false;
        }
        nHackySetRTAnimationsEnabled(z);
    }

    public static void setContextPriority(int priority) {
        nSetContextPriority(priority);
    }
}
