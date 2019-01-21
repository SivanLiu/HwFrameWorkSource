package android.opengl;

import android.content.Context;
import android.os.Process;
import android.telephony.SubscriptionPlan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback2;
import android.view.SurfaceView;
import com.huawei.pgmng.log.LogPower;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class GLSurfaceView extends SurfaceView implements Callback2 {
    public static final int DEBUG_CHECK_GL_ERROR = 1;
    public static final int DEBUG_LOG_GL_CALLS = 2;
    private static final boolean LOG_ATTACH_DETACH = false;
    private static final boolean LOG_EGL = false;
    private static final boolean LOG_PAUSE_RESUME = false;
    private static final boolean LOG_RENDERER = false;
    private static final boolean LOG_RENDERER_DRAW_FRAME = false;
    private static final boolean LOG_SURFACE = false;
    private static final boolean LOG_THREADS = false;
    public static final int RENDERMODE_CONTINUOUSLY = 1;
    public static final int RENDERMODE_WHEN_DIRTY = 0;
    private static final String TAG = "GLSurfaceView";
    private static final long VSYNC_SPAN = 16666667;
    private static final GLThreadManager sGLThreadManager = new GLThreadManager();
    private int mDebugFlags;
    private boolean mDetached;
    private EGLConfigChooser mEGLConfigChooser;
    private int mEGLContextClientVersion;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLThread mGLThread;
    private GLWrapper mGLWrapper;
    private boolean mPreserveEGLContextOnPause;
    private Renderer mRenderer;
    private final WeakReference<GLSurfaceView> mThisWeakRef = new WeakReference(this);

    public interface EGLConfigChooser {
        EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eGLDisplay);
    }

    public interface EGLContextFactory {
        EGLContext createContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig);

        void destroyContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLContext eGLContext);
    }

    public interface EGLWindowSurfaceFactory {
        EGLSurface createWindowSurface(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj);

        void destroySurface(EGL10 egl10, EGLDisplay eGLDisplay, EGLSurface eGLSurface);
    }

    private static class EglHelper {
        EGL10 mEgl;
        EGLConfig mEglConfig;
        EGLContext mEglContext;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;

        public EglHelper(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        public void start() {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay != EGL10.EGL_NO_DISPLAY) {
                if (this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
                    GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                    if (view == null) {
                        this.mEglConfig = null;
                        this.mEglContext = null;
                    } else {
                        this.mEglConfig = view.mEGLConfigChooser.chooseConfig(this.mEgl, this.mEglDisplay);
                        this.mEglContext = view.mEGLContextFactory.createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
                    }
                    if (this.mEglContext == null || this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                        this.mEglContext = null;
                        throwEglException("createContext");
                    }
                    this.mEglSurface = null;
                    return;
                }
                throw new RuntimeException("eglInitialize failed");
            }
            throw new RuntimeException("eglGetDisplay failed");
        }

        public boolean createSurface() {
            if (this.mEgl == null) {
                throw new RuntimeException("egl not initialized");
            } else if (this.mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            } else if (this.mEglConfig != null) {
                destroySurfaceImp();
                GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    this.mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(this.mEgl, this.mEglDisplay, this.mEglConfig, view.getHolder());
                } else {
                    this.mEglSurface = null;
                }
                if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                    if (this.mEgl.eglGetError() == EGL14.EGL_BAD_NATIVE_WINDOW) {
                        Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                    }
                    return false;
                } else if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                    return true;
                } else {
                    logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                    return false;
                }
            } else {
                throw new RuntimeException("mEglConfig not initialized");
            }
        }

        GL createGL() {
            GL gl = this.mEglContext.getGL();
            GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
            if (view == null) {
                return gl;
            }
            if (view.mGLWrapper != null) {
                gl = view.mGLWrapper.wrap(gl);
            }
            if ((view.mDebugFlags & 3) == 0) {
                return gl;
            }
            int configFlags = 0;
            Writer log = null;
            if ((view.mDebugFlags & 1) != 0) {
                configFlags = 0 | 1;
            }
            if ((view.mDebugFlags & 2) != 0) {
                log = new LogWriter();
            }
            return GLDebugHelper.wrap(gl, configFlags, log);
        }

        public int swap() {
            if (this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
                return 12288;
            }
            return this.mEgl.eglGetError();
        }

        public void destroySurface() {
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (this.mEglSurface != null && this.mEglSurface != EGL10.EGL_NO_SURFACE) {
                this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLWindowSurfaceFactory.destroySurface(this.mEgl, this.mEglDisplay, this.mEglSurface);
                }
                this.mEglSurface = null;
            }
        }

        public void finish() {
            if (this.mEglContext != null) {
                GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLContextFactory.destroyContext(this.mEgl, this.mEglDisplay, this.mEglContext);
                }
                this.mEglContext = null;
            }
            if (this.mEglDisplay != null) {
                this.mEgl.eglTerminate(this.mEglDisplay);
                this.mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, this.mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            throw new RuntimeException(formatEglError(function, error));
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(function);
            stringBuilder.append(" failed: ");
            stringBuilder.append(EGLLogWrapper.getErrorString(error));
            return stringBuilder.toString();
        }
    }

    static class GLThread extends Thread {
        private EglHelper mEglHelper;
        private ArrayList<Runnable> mEventQueue = new ArrayList();
        private boolean mExited;
        private Runnable mFinishDrawingRunnable = null;
        private boolean mFinishedCreatingEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;
        private boolean mHasSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private int mHeight = 0;
        private boolean mPaused;
        private boolean mRenderComplete;
        private int mRenderMode = 1;
        private boolean mRequestPaused;
        private boolean mRequestRender = true;
        private boolean mShouldExit;
        private boolean mShouldReleaseEglContext;
        private boolean mSizeChanged = true;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mWantRenderNotification = false;
        private int mWidth = 0;

        GLThread(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        public void run() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GLThread ");
            stringBuilder.append(getId());
            setName(stringBuilder.toString());
            try {
                guardedRun();
            } catch (InterruptedException e) {
            } catch (Throwable th) {
                GLSurfaceView.sGLThreadManager.threadExiting(this);
            }
            GLSurfaceView.sGLThreadManager.threadExiting(this);
        }

        private void stopEglSurfaceLocked() {
            if (this.mHaveEglSurface) {
                this.mHaveEglSurface = false;
                this.mEglHelper.destroySurface();
            }
        }

        private void stopEglContextLocked() {
            if (this.mHaveEglContext) {
                this.mEglHelper.finish();
                this.mHaveEglContext = false;
                GLSurfaceView.sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:253:0x0161 A:{SYNTHETIC} */
        /* JADX WARNING: Missing block: B:106:0x0184, code skipped:
            if (r13 == null) goto L_0x018c;
     */
        /* JADX WARNING: Missing block: B:108:?, code skipped:
            r13.run();
            r13 = null;
     */
        /* JADX WARNING: Missing block: B:109:0x018c, code skipped:
            if (r4 == false) goto L_0x01c9;
     */
        /* JADX WARNING: Missing block: B:111:0x0194, code skipped:
            if (r1.mEglHelper.createSurface() == false) goto L_0x01b2;
     */
        /* JADX WARNING: Missing block: B:112:0x0196, code skipped:
            r20 = android.opengl.GLSurfaceView.access$800();
     */
        /* JADX WARNING: Missing block: B:113:0x019a, code skipped:
            monitor-enter(r20);
     */
        /* JADX WARNING: Missing block: B:116:?, code skipped:
            r1.mFinishedCreatingEglSurface = true;
            android.opengl.GLSurfaceView.access$800().notifyAll();
            r16 = java.lang.System.nanoTime();
     */
        /* JADX WARNING: Missing block: B:117:0x01ab, code skipped:
            monitor-exit(r20);
     */
        /* JADX WARNING: Missing block: B:118:0x01ac, code skipped:
            r4 = false;
     */
        /* JADX WARNING: Missing block: B:123:0x01b2, code skipped:
            r20 = android.opengl.GLSurfaceView.access$800();
     */
        /* JADX WARNING: Missing block: B:124:0x01b6, code skipped:
            monitor-enter(r20);
     */
        /* JADX WARNING: Missing block: B:127:?, code skipped:
            r1.mFinishedCreatingEglSurface = true;
            r1.mSurfaceIsBad = true;
            android.opengl.GLSurfaceView.access$800().notifyAll();
     */
        /* JADX WARNING: Missing block: B:128:0x01c3, code skipped:
            monitor-exit(r20);
     */
        /* JADX WARNING: Missing block: B:134:0x01c9, code skipped:
            if (r5 == false) goto L_0x01d8;
     */
        /* JADX WARNING: Missing block: B:135:0x01cb, code skipped:
            r20 = false;
            r5 = (javax.microedition.khronos.opengles.GL10) r1.mEglHelper.createGL();
     */
        /* JADX WARNING: Missing block: B:136:0x01d8, code skipped:
            r20 = r5;
            r5 = r24;
     */
        /* JADX WARNING: Missing block: B:137:0x01dc, code skipped:
            r28 = r9;
            r29 = r10;
     */
        /* JADX WARNING: Missing block: B:138:0x01e0, code skipped:
            if (r3 == false) goto L_0x0221;
     */
        /* JADX WARNING: Missing block: B:140:0x01ea, code skipped:
            r9 = (android.opengl.GLSurfaceView) r1.mGLSurfaceViewWeakRef.get();
     */
        /* JADX WARNING: Missing block: B:141:0x01ee, code skipped:
            if (r9 == null) goto L_0x021a;
     */
        /* JADX WARNING: Missing block: B:144:0x01f3, code skipped:
            r31 = r3;
            r32 = r4;
     */
        /* JADX WARNING: Missing block: B:146:?, code skipped:
            android.os.Trace.traceBegin(true, "onSurfaceCreated");
            android.opengl.GLSurfaceView.access$1000(r9).onSurfaceCreated(r5, r1.mEglHelper.mEglConfig);
     */
        /* JADX WARNING: Missing block: B:149:?, code skipped:
            android.os.Trace.traceEnd(8);
     */
        /* JADX WARNING: Missing block: B:150:0x020d, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:152:0x020f, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:153:0x0210, code skipped:
            r31 = r3;
            r32 = r4;
     */
        /* JADX WARNING: Missing block: B:154:0x0214, code skipped:
            android.os.Trace.traceEnd(8);
     */
        /* JADX WARNING: Missing block: B:155:0x0219, code skipped:
            throw r0;
     */
        /* JADX WARNING: Missing block: B:156:0x021a, code skipped:
            r31 = r3;
            r32 = r4;
     */
        /* JADX WARNING: Missing block: B:157:0x021e, code skipped:
            r3 = false;
     */
        /* JADX WARNING: Missing block: B:158:0x0221, code skipped:
            r31 = r3;
            r32 = r4;
     */
        /* JADX WARNING: Missing block: B:159:0x0225, code skipped:
            if (r7 == false) goto L_0x024e;
     */
        /* JADX WARNING: Missing block: B:161:0x022f, code skipped:
            r4 = (android.opengl.GLSurfaceView) r1.mGLSurfaceViewWeakRef.get();
     */
        /* JADX WARNING: Missing block: B:162:0x0230, code skipped:
            if (r4 == null) goto L_0x024c;
     */
        /* JADX WARNING: Missing block: B:164:?, code skipped:
            android.os.Trace.traceBegin(8, "onSurfaceChanged");
            android.opengl.GLSurfaceView.access$1000(r4).onSurfaceChanged(r5, r11, r12);
     */
        /* JADX WARNING: Missing block: B:166:?, code skipped:
            android.os.Trace.traceEnd(8);
     */
        /* JADX WARNING: Missing block: B:170:0x024c, code skipped:
            r7 = false;
     */
        /* JADX WARNING: Missing block: B:171:0x024e, code skipped:
            r9 = java.lang.System.nanoTime();
     */
        /* JADX WARNING: Missing block: B:172:0x025a, code skipped:
            r4 = (android.opengl.GLSurfaceView) r1.mGLSurfaceViewWeakRef.get();
     */
        /* JADX WARNING: Missing block: B:173:0x025b, code skipped:
            if (r4 == null) goto L_0x0289;
     */
        /* JADX WARNING: Missing block: B:176:0x0260, code skipped:
            r33 = r6;
            r34 = r7;
     */
        /* JADX WARNING: Missing block: B:178:?, code skipped:
            android.os.Trace.traceBegin(true, "onDrawFrame");
            android.opengl.GLSurfaceView.access$1000(r4).onDrawFrame(r5);
     */
        /* JADX WARNING: Missing block: B:179:0x0270, code skipped:
            if (r14 == null) goto L_0x0276;
     */
        /* JADX WARNING: Missing block: B:180:0x0272, code skipped:
            r14.run();
     */
        /* JADX WARNING: Missing block: B:181:0x0275, code skipped:
            r14 = null;
     */
        /* JADX WARNING: Missing block: B:184:?, code skipped:
            android.os.Trace.traceEnd(8);
     */
        /* JADX WARNING: Missing block: B:185:0x027c, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:187:0x027e, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:188:0x027f, code skipped:
            r33 = r6;
            r34 = r7;
     */
        /* JADX WARNING: Missing block: B:189:0x0283, code skipped:
            android.os.Trace.traceEnd(8);
     */
        /* JADX WARNING: Missing block: B:190:0x0288, code skipped:
            throw r0;
     */
        /* JADX WARNING: Missing block: B:191:0x0289, code skipped:
            r33 = r6;
            r34 = r7;
     */
        /* JADX WARNING: Missing block: B:192:0x028d, code skipped:
            r4 = r1.mEglHelper.swap();
     */
        /* JADX WARNING: Missing block: B:193:0x0296, code skipped:
            if (r4 == 12288) goto L_0x02bb;
     */
        /* JADX WARNING: Missing block: B:195:0x029a, code skipped:
            if (r4 == android.opengl.EGL14.EGL_CONTEXT_LOST) goto L_0x02b7;
     */
        /* JADX WARNING: Missing block: B:196:0x029c, code skipped:
            android.opengl.GLSurfaceView.EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", r4);
            r6 = android.opengl.GLSurfaceView.access$800();
     */
        /* JADX WARNING: Missing block: B:197:0x02a7, code skipped:
            monitor-enter(r6);
     */
        /* JADX WARNING: Missing block: B:200:?, code skipped:
            r1.mSurfaceIsBad = true;
            android.opengl.GLSurfaceView.access$800().notifyAll();
     */
        /* JADX WARNING: Missing block: B:201:0x02b2, code skipped:
            monitor-exit(r6);
     */
        /* JADX WARNING: Missing block: B:207:0x02b7, code skipped:
            r6 = true;
     */
        /* JADX WARNING: Missing block: B:209:0x02bc, code skipped:
            r6 = r33;
     */
        /* JADX WARNING: Missing block: B:210:0x02be, code skipped:
            r35 = java.lang.System.nanoTime();
     */
        /* JADX WARNING: Missing block: B:211:0x02c4, code skipped:
            if (r15 != false) goto L_0x02e7;
     */
        /* JADX WARNING: Missing block: B:212:0x02c6, code skipped:
            r37 = r3;
            r38 = r4;
     */
        /* JADX WARNING: Missing block: B:213:0x02d5, code skipped:
            if ((r35 - r16) < 60000000000L) goto L_0x02d9;
     */
        /* JADX WARNING: Missing block: B:214:0x02d7, code skipped:
            r15 = true;
     */
        /* JADX WARNING: Missing block: B:215:0x02d9, code skipped:
            r41 = r5;
            r42 = r6;
            r43 = r11;
            r44 = r12;
            r45 = r13;
            r7 = r21;
     */
        /* JADX WARNING: Missing block: B:216:0x02e7, code skipped:
            r37 = r3;
            r38 = r4;
            r3 = r35;
            r41 = r5;
            r42 = r6;
            r5 = (r3 - r9) / android.opengl.GLSurfaceView.VSYNC_SPAN;
     */
        /* JADX WARNING: Missing block: B:217:0x0300, code skipped:
            if (r5 < 30) goto L_0x038e;
     */
        /* JADX WARNING: Missing block: B:218:0x0302, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("jank_event_sync: start_ts=");
            r0.append(r9);
            r0.append(",end_ts=");
            r0.append(r3);
            r0.append(", appid=");
            r7 = r21;
            r0.append(r7);
            r0 = r0.toString();
            r43 = r11;
            r44 = r12;
            android.os.Trace.traceBegin(8, r0);
            android.os.Trace.traceEnd(8);
            r11 = new java.lang.StringBuilder();
            r11.append("#P:");
            r11.append(r2);
            r11.append("#SK:");
            r11.append(r5);
            r11.append("#IP:0#FRT:");
            r45 = r13;
            r11.append(r9 / 10000);
            r11.append("#DNT:");
            r11.append(r3 / 10000);
            android.util.Jlog.d(android.util.JlogConstants.JLID_OPENGL_JANK_FRAME_SKIP, r11.toString());
            r12 = android.opengl.GLSurfaceView.TAG;
            r13 = new java.lang.StringBuilder();
            r46 = r0;
            r13.append("OpenGL:");
            r13.append(r2);
            r13.append(" Skipped ");
            r13.append(r5);
            r13.append(" frames!");
            android.util.Log.i(r12, r13.toString());
     */
        /* JADX WARNING: Missing block: B:219:0x038e, code skipped:
            r43 = r11;
            r44 = r12;
            r45 = r13;
            r7 = r21;
     */
        /* JADX WARNING: Missing block: B:220:0x0396, code skipped:
            if (r8 == false) goto L_0x039d;
     */
        /* JADX WARNING: Missing block: B:221:0x0398, code skipped:
            r28 = true;
            r8 = false;
     */
        /* JADX WARNING: Missing block: B:222:0x039d, code skipped:
            r21 = r7;
            r18 = r9;
            r5 = r20;
            r9 = r28;
            r10 = r29;
            r4 = r32;
            r7 = r34;
            r3 = r37;
            r24 = r41;
            r6 = r42;
            r11 = r43;
            r12 = r44;
            r13 = r45;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void guardedRun() throws InterruptedException {
            Throwable th;
            this.mEglHelper = new EglHelper(this.mGLSurfaceViewWeakRef);
            this.mHaveEglContext = false;
            this.mHaveEglSurface = false;
            this.mWantRenderNotification = false;
            boolean createEglContext = false;
            boolean createEglSurface = false;
            boolean createGlInterface = false;
            boolean lostEglContext = false;
            boolean sizeChanged = false;
            boolean wantRenderNotification = false;
            boolean doRenderNotification = false;
            boolean askedToReleaseEglContext = false;
            int w = 0;
            int h = 0;
            Runnable event = null;
            Runnable finishDrawingRunnable = null;
            boolean checkskipframe = false;
            long SurfaceCreateTime = SubscriptionPlan.BYTES_UNLIMITED;
            try {
                int mPackageId = Process.myPid();
                String mPackageName = "";
                GL10 gl = null;
                GLSurfaceView mPackageName2 = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                if (mPackageName2 != null) {
                    Context jankcontext = mPackageName2.getContext();
                    if (jankcontext != null) {
                        mPackageName = jankcontext.getPackageName();
                    }
                }
                GLSurfaceView jankview = mPackageName2;
                String mPackageName3 = mPackageName;
                while (true) {
                    synchronized (GLSurfaceView.sGLThreadManager) {
                        while (!this.mShouldExit) {
                            boolean createEglContext2;
                            try {
                                createEglContext2 = createEglContext;
                                if (this.mEventQueue.isEmpty()) {
                                    boolean pausing = false;
                                    if (this.mPaused != this.mRequestPaused) {
                                        boolean pausing2 = this.mRequestPaused;
                                        this.mPaused = this.mRequestPaused;
                                        GLSurfaceView.sGLThreadManager.notifyAll();
                                        pausing = pausing2;
                                    }
                                    if (this.mShouldReleaseEglContext) {
                                        stopEglSurfaceLocked();
                                        stopEglContextLocked();
                                        this.mShouldReleaseEglContext = false;
                                        askedToReleaseEglContext = true;
                                    }
                                    if (lostEglContext) {
                                        stopEglSurfaceLocked();
                                        stopEglContextLocked();
                                        lostEglContext = false;
                                    }
                                    if (pausing && this.mHaveEglSurface) {
                                        stopEglSurfaceLocked();
                                    }
                                    if (pausing && this.mHaveEglContext) {
                                        GLSurfaceView view = (GLSurfaceView) this.mGLSurfaceViewWeakRef.get();
                                        if (!(view == null ? false : view.mPreserveEGLContextOnPause)) {
                                            stopEglContextLocked();
                                        }
                                    }
                                    if (!(this.mHasSurface || this.mWaitingForSurface)) {
                                        if (this.mHaveEglSurface) {
                                            stopEglSurfaceLocked();
                                        }
                                        this.mWaitingForSurface = true;
                                        this.mSurfaceIsBad = false;
                                        GLSurfaceView.sGLThreadManager.notifyAll();
                                    }
                                    if (this.mHasSurface && this.mWaitingForSurface) {
                                        this.mWaitingForSurface = false;
                                        GLSurfaceView.sGLThreadManager.notifyAll();
                                    }
                                    if (doRenderNotification) {
                                        this.mWantRenderNotification = false;
                                        doRenderNotification = false;
                                        this.mRenderComplete = true;
                                        GLSurfaceView.sGLThreadManager.notifyAll();
                                    }
                                    if (this.mFinishDrawingRunnable != null) {
                                        finishDrawingRunnable = this.mFinishDrawingRunnable;
                                        this.mFinishDrawingRunnable = null;
                                    }
                                    if (readyToDraw()) {
                                        if (!this.mHaveEglContext) {
                                            if (askedToReleaseEglContext) {
                                                askedToReleaseEglContext = false;
                                            } else {
                                                this.mEglHelper.start();
                                                this.mHaveEglContext = true;
                                                createEglContext = true;
                                                GLSurfaceView.sGLThreadManager.notifyAll();
                                                if (this.mHaveEglContext && !this.mHaveEglSurface) {
                                                    this.mHaveEglSurface = true;
                                                    createEglSurface = true;
                                                    createGlInterface = true;
                                                    sizeChanged = true;
                                                }
                                                if (this.mHaveEglSurface) {
                                                    if (this.mSizeChanged) {
                                                        sizeChanged = true;
                                                        w = this.mWidth;
                                                        h = this.mHeight;
                                                        this.mWantRenderNotification = true;
                                                        createEglSurface = true;
                                                        this.mSizeChanged = false;
                                                    }
                                                    this.mRequestRender = false;
                                                    GLSurfaceView.sGLThreadManager.notifyAll();
                                                    if (this.mWantRenderNotification) {
                                                        wantRenderNotification = true;
                                                    }
                                                }
                                            }
                                        }
                                        createEglContext = createEglContext2;
                                        this.mHaveEglSurface = true;
                                        createEglSurface = true;
                                        createGlInterface = true;
                                        sizeChanged = true;
                                        if (this.mHaveEglSurface) {
                                        }
                                    } else {
                                        if (finishDrawingRunnable != null) {
                                            Log.w(GLSurfaceView.TAG, "Warning, !readyToDraw() but waiting for draw finished! Early reporting draw finished.");
                                            finishDrawingRunnable.run();
                                            finishDrawingRunnable = null;
                                        }
                                        createEglContext = createEglContext2;
                                    }
                                    GLSurfaceView.sGLThreadManager.wait();
                                } else {
                                    try {
                                        event = (Runnable) this.mEventQueue.remove(0);
                                        createEglContext = createEglContext2;
                                    } catch (RuntimeException t) {
                                        GLSurfaceView.sGLThreadManager.releaseEglContextLocked(this);
                                        throw t;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        createEglContext = createEglContext2;
                                        throw th;
                                    }
                                }
                                try {
                                } catch (Throwable th3) {
                                    th = th3;
                                    boolean z = createEglContext;
                                    boolean z2 = lostEglContext;
                                    boolean z3 = doRenderNotification;
                                    boolean z4 = askedToReleaseEglContext;
                                    int i = w;
                                    int i2 = h;
                                    Runnable runnable = event;
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        synchronized (GLSurfaceView.sGLThreadManager) {
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                        }
                        return;
                    }
                }
            } catch (Throwable th5) {
                synchronized (GLSurfaceView.sGLThreadManager) {
                    stopEglSurfaceLocked();
                    stopEglContextLocked();
                }
            }
        }

        public boolean ableToDraw() {
            return this.mHaveEglContext && this.mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return !this.mPaused && this.mHasSurface && !this.mSurfaceIsBad && this.mWidth > 0 && this.mHeight > 0 && (this.mRequestRender || this.mRenderMode == 1);
        }

        public void setRenderMode(int renderMode) {
            if (renderMode < 0 || renderMode > 1) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRenderMode = renderMode;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public int getRenderMode() {
            int i;
            synchronized (GLSurfaceView.sGLThreadManager) {
                i = this.mRenderMode;
            }
            return i;
        }

        public void requestRender() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRequestRender = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (Thread.currentThread() == this) {
                    return;
                }
                this.mWantRenderNotification = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                this.mFinishDrawingRunnable = finishDrawing;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mHasSurface = true;
                this.mFinishedCreatingEglSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (this.mWaitingForSurface && !this.mFinishedCreatingEglSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mHasSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mWaitingForSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRequestPaused = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRequestPaused = false;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && this.mPaused && !this.mRenderComplete) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:23:0x0044, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onWindowResize(int w, int h) {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mWidth = w;
                this.mHeight = h;
                this.mSizeChanged = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                if (Thread.currentThread() == this) {
                    return;
                }
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused && !this.mRenderComplete && ableToDraw()) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mShouldExit = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            this.mShouldReleaseEglContext = true;
            GLSurfaceView.sGLThreadManager.notifyAll();
        }

        public void queueEvent(Runnable r) {
            if (r != null) {
                synchronized (GLSurfaceView.sGLThreadManager) {
                    this.mEventQueue.add(r);
                    GLSurfaceView.sGLThreadManager.notifyAll();
                }
                return;
            }
            throw new IllegalArgumentException("r must not be null");
        }
    }

    private static class GLThreadManager {
        private static String TAG = "GLThreadManager";

        private GLThreadManager() {
        }

        public synchronized void threadExiting(GLThread thread) {
            thread.mExited = true;
            notifyAll();
        }

        public void releaseEglContextLocked(GLThread thread) {
            notifyAll();
        }
    }

    public interface GLWrapper {
        GL wrap(GL gl);
    }

    static class LogWriter extends Writer {
        private StringBuilder mBuilder = new StringBuilder();

        LogWriter() {
        }

        public void close() {
            flushBuilder();
        }

        public void flush() {
            flushBuilder();
        }

        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == 10) {
                    flushBuilder();
                } else {
                    this.mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (this.mBuilder.length() > 0) {
                Log.v(GLSurfaceView.TAG, this.mBuilder.toString());
                this.mBuilder.delete(0, this.mBuilder.length());
            }
        }
    }

    public interface Renderer {
        void onDrawFrame(GL10 gl10);

        void onSurfaceChanged(GL10 gl10, int i, int i2);

        void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig);
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {
        protected int[] mConfigSpec;

        abstract EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr);

        public BaseConfigChooser(int[] configSpec) {
            this.mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (egl.eglChooseConfig(display, this.mConfigSpec, null, 0, num_config)) {
                int numConfigs = num_config[0];
                if (numConfigs > 0) {
                    EGLConfig[] configs = new EGLConfig[numConfigs];
                    if (egl.eglChooseConfig(display, this.mConfigSpec, configs, numConfigs, num_config)) {
                        EGLConfig config = chooseConfig(egl, display, configs);
                        if (config != null) {
                            return config;
                        }
                        throw new IllegalArgumentException("No config chosen");
                    }
                    throw new IllegalArgumentException("eglChooseConfig#2 failed");
                }
                throw new IllegalArgumentException("No configs match configSpec");
            }
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        private int[] filterConfigSpec(int[] configSpec) {
            if (GLSurfaceView.this.mEGLContextClientVersion != 2 && GLSurfaceView.this.mEGLContextClientVersion != 3) {
                return configSpec;
            }
            int len = configSpec.length;
            int[] newConfigSpec = new int[(len + 2)];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = EGL14.EGL_RENDERABLE_TYPE;
            if (GLSurfaceView.this.mEGLContextClientVersion == 2) {
                newConfigSpec[len] = 4;
            } else {
                newConfigSpec[len] = 64;
            }
            newConfigSpec[len + 1] = EGL14.EGL_NONE;
            return newConfigSpec;
        }
    }

    private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION;

        private DefaultContextFactory() {
            this.EGL_CONTEXT_CLIENT_VERSION = 12440;
        }

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] iArr;
            int[] attrib_list = new int[]{this.EGL_CONTEXT_CLIENT_VERSION, GLSurfaceView.this.mEGLContextClientVersion, EGL14.EGL_NONE};
            EGLContext eGLContext = EGL10.EGL_NO_CONTEXT;
            if (GLSurfaceView.this.mEGLContextClientVersion != 0) {
                iArr = attrib_list;
            } else {
                iArr = null;
            }
            return egl.eglCreateContext(display, config, eGLContext, iArr);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("display:");
                stringBuilder.append(display);
                stringBuilder.append(" context: ");
                stringBuilder.append(context);
                Log.e("DefaultContextFactory", stringBuilder.toString());
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
        private DefaultWindowSurfaceFactory() {
        }

        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                return egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
                Log.e(GLSurfaceView.TAG, "eglCreateWindowSurface", e);
                return result;
            }
        }

        public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];

        public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{EGL14.EGL_RED_SIZE, redSize, EGL14.EGL_GREEN_SIZE, greenSize, EGL14.EGL_BLUE_SIZE, blueSize, EGL14.EGL_ALPHA_SIZE, alphaSize, EGL14.EGL_DEPTH_SIZE, depthSize, EGL14.EGL_STENCIL_SIZE, stencilSize, EGL14.EGL_NONE});
            this.mRedSize = redSize;
            this.mGreenSize = greenSize;
            this.mBlueSize = blueSize;
            this.mAlphaSize = alphaSize;
            this.mDepthSize = depthSize;
            this.mStencilSize = stencilSize;
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                EGL10 egl10 = egl;
                EGLDisplay eGLDisplay = display;
                EGLConfig eGLConfig = config;
                int d = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_STENCIL_SIZE, 0);
                if (d >= this.mDepthSize && s >= this.mStencilSize) {
                    egl10 = egl;
                    eGLDisplay = display;
                    eGLConfig = config;
                    int r = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_BLUE_SIZE, 0);
                    int thisR = findConfigAttrib(egl10, eGLDisplay, eGLConfig, EGL14.EGL_ALPHA_SIZE, 0);
                    if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && thisR == this.mAlphaSize) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, this.mValue)) {
                return this.mValue[0];
            }
            return defaultValue;
        }
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    public GLSurfaceView(Context context) {
        super(context);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGLThread != null) {
                this.mGLThread.requestExitAndWait();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    private void init() {
        getHolder().addCallback(this);
    }

    public void setGLWrapper(GLWrapper glWrapper) {
        this.mGLWrapper = glWrapper;
    }

    public void setDebugFlags(int debugFlags) {
        this.mDebugFlags = debugFlags;
    }

    public int getDebugFlags() {
        return this.mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        this.mPreserveEGLContextOnPause = preserveOnPause;
    }

    public boolean getPreserveEGLContextOnPause() {
        return this.mPreserveEGLContextOnPause;
    }

    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (this.mEGLConfigChooser == null) {
            this.mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (this.mEGLContextFactory == null) {
            this.mEGLContextFactory = new DefaultContextFactory();
        }
        if (this.mEGLWindowSurfaceFactory == null) {
            this.mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        this.mRenderer = renderer;
        this.mGLThread = new GLThread(this.mThisWeakRef);
        this.mGLThread.start();
    }

    public void setEGLContextFactory(EGLContextFactory factory) {
        checkRenderThreadState();
        this.mEGLContextFactory = factory;
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        checkRenderThreadState();
        this.mEGLWindowSurfaceFactory = factory;
    }

    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        checkRenderThreadState();
        this.mEGLConfigChooser = configChooser;
    }

    public void setEGLConfigChooser(boolean needDepth) {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
    }

    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        this.mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode) {
        this.mGLThread.setRenderMode(renderMode);
    }

    public int getRenderMode() {
        return this.mGLThread.getRenderMode();
    }

    public void requestRender() {
        this.mGLThread.requestRender();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        this.mGLThread.surfaceCreated();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mGLThread.surfaceDestroyed();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        this.mGLThread.onWindowResize(w, h);
    }

    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        if (this.mGLThread != null) {
            this.mGLThread.requestRenderAndNotify(finishDrawing);
        }
    }

    @Deprecated
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
    }

    public void onPause() {
        LogPower.push(107, this.mContext.getPackageName());
        this.mGLThread.onPause();
    }

    public void onResume() {
        this.mGLThread.onResume();
        LogPower.push(106, this.mContext.getPackageName());
    }

    public void queueEvent(Runnable r) {
        this.mGLThread.queueEvent(r);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mDetached && this.mRenderer != null) {
            int renderMode = 1;
            if (this.mGLThread != null) {
                renderMode = this.mGLThread.getRenderMode();
            }
            this.mGLThread = new GLThread(this.mThisWeakRef);
            if (renderMode != 1) {
                this.mGLThread.setRenderMode(renderMode);
            }
            this.mGLThread.start();
        }
        this.mDetached = false;
    }

    protected void onDetachedFromWindow() {
        if (this.mGLThread != null) {
            this.mGLThread.requestExitAndWait();
        }
        this.mDetached = true;
        super.onDetachedFromWindow();
    }

    private void checkRenderThreadState() {
        if (this.mGLThread != null) {
            throw new IllegalStateException("setRenderer has already been called for this instance.");
        }
    }
}
