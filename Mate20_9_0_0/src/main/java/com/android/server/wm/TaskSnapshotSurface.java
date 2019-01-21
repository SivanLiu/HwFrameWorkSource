package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityThread;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.IWindowSession;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceSession;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.policy.DecorView;
import com.android.internal.view.BaseIWindow;
import com.android.server.policy.WindowManagerPolicy.StartingSurface;
import java.util.Objects;

class TaskSnapshotSurface implements StartingSurface {
    private static final int FLAG_INHERIT_EXCLUDES = 830922808;
    private static final int MSG_REPORT_DRAW = 0;
    private static final int PRIVATE_FLAG_INHERITS = 131072;
    private static final long SIZE_MISMATCH_MINIMUM_TIME_MS = 450;
    private static final String TAG = "WindowManager";
    private static final String TITLE_FORMAT = "SnapshotStartingWindow for taskId=%s";
    private static Handler sHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                boolean hasDrawn;
                TaskSnapshotSurface surface = msg.obj;
                synchronized (surface.mService.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        hasDrawn = surface.mHasDrawn;
                    } finally {
                        while (true) {
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
                if (hasDrawn) {
                    surface.reportDrawn();
                }
            }
        }
    };
    private final Paint mBackgroundPaint = new Paint();
    private SurfaceControl mChildSurfaceControl;
    private final Rect mContentInsets = new Rect();
    private final Rect mFrame = new Rect();
    private final Handler mHandler;
    private boolean mHasDrawn;
    private final int mOrientationOnCreation;
    private final WindowManagerService mService;
    private final IWindowSession mSession;
    private long mShownTime;
    private boolean mSizeMismatch;
    private TaskSnapshot mSnapshot;
    private final Rect mStableInsets = new Rect();
    private final int mStatusBarColor;
    private final Surface mSurface;
    @VisibleForTesting
    final SystemBarBackgroundPainter mSystemBarBackgroundPainter;
    private final Rect mTaskBounds;
    private final CharSequence mTitle;
    private final Window mWindow;

    static class SystemBarBackgroundPainter {
        private final Rect mContentInsets = new Rect();
        private final int mNavigationBarColor;
        private final Paint mNavigationBarPaint = new Paint();
        private final Rect mStableInsets = new Rect();
        private final int mStatusBarColor;
        private final Paint mStatusBarPaint = new Paint();
        private final int mSysUiVis;
        private final int mWindowFlags;
        private final int mWindowPrivateFlags;

        SystemBarBackgroundPainter(int windowFlags, int windowPrivateFlags, int sysUiVis, int statusBarColor, int navigationBarColor) {
            this.mWindowFlags = windowFlags;
            this.mWindowPrivateFlags = windowPrivateFlags;
            this.mSysUiVis = sysUiVis;
            this.mStatusBarColor = DecorView.calculateStatusBarColor(windowFlags, ActivityThread.currentActivityThread().getSystemUiContext().getColor(17170783), statusBarColor);
            this.mNavigationBarColor = navigationBarColor;
            this.mStatusBarPaint.setColor(this.mStatusBarColor);
            this.mNavigationBarPaint.setColor(navigationBarColor);
        }

        void setInsets(Rect contentInsets, Rect stableInsets) {
            this.mContentInsets.set(contentInsets);
            this.mStableInsets.set(stableInsets);
        }

        int getStatusBarColorViewHeight() {
            if (DecorView.STATUS_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mSysUiVis, this.mStatusBarColor, this.mWindowFlags, (this.mWindowPrivateFlags & 131072) != 0)) {
                return DecorView.getColorViewTopInset(this.mStableInsets.top, this.mContentInsets.top);
            }
            return 0;
        }

        private boolean isNavigationBarColorViewVisible() {
            return DecorView.NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mSysUiVis, this.mNavigationBarColor, this.mWindowFlags, false);
        }

        void drawDecors(Canvas c, Rect alreadyDrawnFrame) {
            drawStatusBarBackground(c, alreadyDrawnFrame, getStatusBarColorViewHeight());
            drawNavigationBarBackground(c);
        }

        @VisibleForTesting
        void drawStatusBarBackground(Canvas c, Rect alreadyDrawnFrame, int statusBarHeight) {
            if (statusBarHeight > 0 && Color.alpha(this.mStatusBarColor) != 0) {
                if (alreadyDrawnFrame == null || c.getWidth() > alreadyDrawnFrame.right) {
                    c.drawRect((float) (alreadyDrawnFrame != null ? alreadyDrawnFrame.right : 0), 0.0f, (float) (c.getWidth() - DecorView.getColorViewRightInset(this.mStableInsets.right, this.mContentInsets.right)), (float) statusBarHeight, this.mStatusBarPaint);
                }
            }
        }

        @VisibleForTesting
        void drawNavigationBarBackground(Canvas c) {
            Rect navigationBarRect = new Rect();
            DecorView.getNavigationBarRect(c.getWidth(), c.getHeight(), this.mStableInsets, this.mContentInsets, navigationBarRect);
            if (isNavigationBarColorViewVisible() && Color.alpha(this.mNavigationBarColor) != 0 && !navigationBarRect.isEmpty()) {
                c.drawRect(navigationBarRect, this.mNavigationBarPaint);
            }
        }
    }

    @VisibleForTesting
    static class Window extends BaseIWindow {
        private TaskSnapshotSurface mOuter;

        Window() {
        }

        public void setOuter(TaskSnapshotSurface outer) {
            this.mOuter = outer;
        }

        public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, MergedConfiguration mergedConfiguration, Rect backDropFrame, boolean forceLayout, boolean alwaysConsumeNavBar, int displayId, ParcelableWrapper displayCutout) {
            if (!(mergedConfiguration == null || this.mOuter == null || this.mOuter.mOrientationOnCreation == mergedConfiguration.getMergedConfiguration().orientation)) {
                Handler access$400 = TaskSnapshotSurface.sHandler;
                TaskSnapshotSurface taskSnapshotSurface = this.mOuter;
                Objects.requireNonNull(taskSnapshotSurface);
                access$400.post(new -$$Lambda$-OevXHSXgaSE351ZqRnMoA024MM(taskSnapshotSurface));
            }
            if (reportDraw) {
                TaskSnapshotSurface.sHandler.obtainMessage(0, this.mOuter).sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:56:0x0197, code skipped:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:59:0x01a6, code skipped:
            r54 = r4;
            r51 = r7;
            r56 = r8;
            r57 = r10;
            r58 = r11;
            r59 = r13;
     */
    /* JADX WARNING: Missing block: B:61:?, code skipped:
            r0 = r13.addToDisplay(r14, r14.mSeq, r15, 8, r62.getDisplayContent().getDisplayId(), r11, r9, r9, r9, r12, null);
     */
    /* JADX WARNING: Missing block: B:62:0x01c3, code skipped:
            if (r0 >= null) goto L_0x01ec;
     */
    /* JADX WARNING: Missing block: B:63:0x01c5, code skipped:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Failed to add snapshot starting window res=");
            r3.append(r0);
            android.util.Slog.w(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:64:0x01db, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:67:0x01e0, code skipped:
            r54 = r4;
            r51 = r7;
            r56 = r8;
            r57 = r10;
            r58 = r11;
            r59 = r13;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static TaskSnapshotSurface create(WindowManagerService service, AppWindowToken token, TaskSnapshot snapshot) {
        int backgroundColor;
        int statusBarColor;
        int navigationBarColor;
        Throwable mainWindow;
        IWindowSession iWindowSession;
        LayoutParams layoutParams;
        int i;
        Rect rect;
        Window window;
        int windowFlags;
        int windowPrivateFlags;
        int currentOrientation;
        AppWindowToken appWindowToken = token;
        LayoutParams layoutParams2 = new LayoutParams();
        Window window2 = new Window();
        IWindowSession session = WindowManagerGlobal.getWindowSession();
        window2.setSession(session);
        Surface surface = new Surface();
        Rect tmpRect = new Rect();
        ParcelableWrapper tmpCutout = new ParcelableWrapper();
        Rect tmpFrame = new Rect();
        Rect tmpContentInsets = new Rect();
        Rect tmpStableInsets = new Rect();
        MergedConfiguration tmpMergedConfiguration = new MergedConfiguration();
        synchronized (service.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState mainWindow2 = token.findMainWindow();
                Task task = token.getTask();
                int navigationBarColor2;
                int i2;
                if (task == null) {
                    String str;
                    StringBuilder stringBuilder;
                    backgroundColor = -1;
                    try {
                        str = TAG;
                        statusBarColor = 0;
                        try {
                            stringBuilder = new StringBuilder();
                            navigationBarColor = 0;
                        } catch (Throwable th) {
                            mainWindow = th;
                            navigationBarColor = 0;
                            iWindowSession = session;
                            navigationBarColor2 = window2;
                            layoutParams = layoutParams2;
                            i2 = backgroundColor;
                            i = statusBarColor;
                            rect = tmpFrame;
                            tmpFrame = tmpStableInsets;
                            tmpStableInsets = rect;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    mainWindow = th2;
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw mainWindow;
                        }
                    } catch (Throwable th3) {
                        mainWindow = th3;
                        statusBarColor = 0;
                        navigationBarColor = 0;
                        iWindowSession = session;
                        navigationBarColor2 = window2;
                        layoutParams = layoutParams2;
                        i2 = backgroundColor;
                        rect = tmpFrame;
                        tmpFrame = tmpStableInsets;
                        tmpStableInsets = rect;
                        while (true) {
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw mainWindow;
                    }
                    try {
                        stringBuilder.append("TaskSnapshotSurface.create: Failed to find task for token=");
                        stringBuilder.append(appWindowToken);
                        Slog.w(str, stringBuilder.toString());
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } catch (Throwable th4) {
                        mainWindow = th4;
                        navigationBarColor2 = window2;
                        i2 = backgroundColor;
                        rect = tmpFrame;
                        tmpFrame = tmpStableInsets;
                        tmpStableInsets = rect;
                        while (true) {
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw mainWindow;
                    }
                }
                backgroundColor = -1;
                statusBarColor = 0;
                navigationBarColor = 0;
                try {
                    AppWindowToken topFullscreenToken = token.getTask().getTopFullscreenAppToken();
                    if (topFullscreenToken == null) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("TaskSnapshotSurface.create: Failed to find top fullscreen for task=");
                        stringBuilder2.append(task);
                        Slog.w(str2, stringBuilder2.toString());
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    WindowState topFullscreenWindow = topFullscreenToken.getTopFullscreenWindow();
                    AppWindowToken appWindowToken2;
                    if (mainWindow2 == null) {
                        appWindowToken2 = topFullscreenToken;
                        iWindowSession = session;
                        window = window2;
                        layoutParams = layoutParams2;
                        rect = tmpFrame;
                        tmpFrame = tmpStableInsets;
                        tmpStableInsets = rect;
                    } else if (topFullscreenWindow == null) {
                        WindowState windowState = mainWindow2;
                        appWindowToken2 = topFullscreenToken;
                        iWindowSession = session;
                        window = window2;
                        layoutParams = layoutParams2;
                        rect = tmpFrame;
                        tmpFrame = tmpStableInsets;
                        tmpStableInsets = rect;
                    } else {
                        int sysUiVis = topFullscreenWindow.getSystemUiVisibility();
                        windowFlags = topFullscreenWindow.getAttrs().flags;
                        windowPrivateFlags = topFullscreenWindow.getAttrs().privateFlags;
                        layoutParams2.packageName = mainWindow2.getAttrs().packageName;
                        layoutParams2.windowAnimations = mainWindow2.getAttrs().windowAnimations;
                        layoutParams2.dimAmount = mainWindow2.getAttrs().dimAmount;
                        layoutParams2.type = 3;
                        layoutParams2.format = snapshot.getSnapshot().getFormat();
                        layoutParams2.flags = ((windowFlags & -830922809) | 8) | 16;
                        layoutParams2.privateFlags = windowPrivateFlags & 131072;
                        layoutParams2.token = appWindowToken.token;
                        layoutParams2.width = -1;
                        layoutParams2.height = -1;
                        layoutParams2.systemUiVisibility = sysUiVis;
                        String str3 = TITLE_FORMAT;
                        mainWindow2 = new Object[1];
                        mainWindow2[0] = Integer.valueOf(task.mTaskId);
                        layoutParams2.setTitle(String.format(str3, mainWindow2));
                        mainWindow2 = task.getTaskDescription();
                        if (mainWindow2 != null) {
                            i2 = mainWindow2.getBackgroundColor();
                            try {
                                navigationBarColor2 = mainWindow2.getStatusBarColor();
                                try {
                                    backgroundColor = i2;
                                    statusBarColor = navigationBarColor2;
                                    navigationBarColor = mainWindow2.getNavigationBarColor();
                                } catch (Throwable th5) {
                                    mainWindow = th5;
                                    i = navigationBarColor2;
                                    iWindowSession = session;
                                    navigationBarColor2 = window2;
                                    layoutParams = layoutParams2;
                                    rect = tmpFrame;
                                    tmpFrame = tmpStableInsets;
                                    tmpStableInsets = rect;
                                    while (true) {
                                        break;
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw mainWindow;
                                }
                            } catch (Throwable th6) {
                                mainWindow = th6;
                                iWindowSession = session;
                                window = window2;
                                layoutParams = layoutParams2;
                                rect = tmpFrame;
                                tmpFrame = tmpStableInsets;
                                tmpStableInsets = rect;
                                while (true) {
                                    break;
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw mainWindow;
                            }
                        }
                        Rect taskBounds = new Rect();
                        task.getBounds(taskBounds);
                        currentOrientation = topFullscreenWindow.getConfiguration().orientation;
                    }
                    try {
                        String str4 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("TaskSnapshotSurface.create: Failed to find main window for token=");
                        stringBuilder3.append(appWindowToken);
                        Slog.w(str4, stringBuilder3.toString());
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } catch (Throwable th7) {
                        mainWindow = th7;
                        i2 = backgroundColor;
                        i = statusBarColor;
                        while (true) {
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw mainWindow;
                    }
                } catch (Throwable th8) {
                    mainWindow = th8;
                    iWindowSession = session;
                    navigationBarColor2 = window2;
                    layoutParams = layoutParams2;
                    rect = tmpFrame;
                    tmpFrame = tmpStableInsets;
                    tmpStableInsets = rect;
                    while (true) {
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw mainWindow;
                }
            } catch (Throwable th9) {
                mainWindow = th9;
                backgroundColor = -1;
                statusBarColor = 0;
                navigationBarColor = 0;
                iWindowSession = session;
                layoutParams = layoutParams2;
                rect = tmpFrame;
                tmpStableInsets = rect;
                while (true) {
                    break;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                throw mainWindow;
            }
        }
        WindowContainer topFullscreenToken2;
        topFullscreenToken2 = new TaskSnapshotSurface(service, window2, surface, snapshot, layoutParams2.getTitle(), backgroundColor, statusBarColor, navigationBarColor, sysUiVis, windowFlags, windowPrivateFlags, taskBounds, currentOrientation);
        window2.setOuter(topFullscreenToken2);
        try {
            try {
                iWindowSession.relayout(window2, window2.mSeq, layoutParams2, -1, -1, 0, 0, -1, tmpFrame, tmpRect, tmpContentInsets, tmpRect, tmpStableInsets, tmpRect, tmpRect, tmpCutout, tmpMergedConfiguration, surface);
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
            window = window2;
            layoutParams = layoutParams2;
        }
        tmpStableInsets = tmpFrame;
        topFullscreenToken2.setFrames(tmpStableInsets, tmpContentInsets, tmpStableInsets);
        topFullscreenToken2.drawSnapshot();
        return topFullscreenToken2;
        tmpStableInsets = tmpFrame;
        topFullscreenToken2.setFrames(tmpStableInsets, tmpContentInsets, tmpStableInsets);
        topFullscreenToken2.drawSnapshot();
        return topFullscreenToken2;
    }

    @VisibleForTesting
    TaskSnapshotSurface(WindowManagerService service, Window window, Surface surface, TaskSnapshot snapshot, CharSequence title, int backgroundColor, int statusBarColor, int navigationBarColor, int sysUiVis, int windowFlags, int windowPrivateFlags, Rect taskBounds, int currentOrientation) {
        this.mService = service;
        this.mHandler = new Handler(this.mService.mH.getLooper());
        this.mSession = WindowManagerGlobal.getWindowSession();
        this.mWindow = window;
        this.mSurface = surface;
        this.mSnapshot = snapshot;
        this.mTitle = title;
        this.mBackgroundPaint.setColor(backgroundColor != 0 ? backgroundColor : -1);
        this.mTaskBounds = taskBounds;
        this.mSystemBarBackgroundPainter = new SystemBarBackgroundPainter(windowFlags, windowPrivateFlags, sysUiVis, statusBarColor, navigationBarColor);
        this.mStatusBarColor = statusBarColor;
        this.mOrientationOnCreation = currentOrientation;
    }

    /* JADX WARNING: Missing block: B:12:0x002d, code skipped:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r9.mSession.remove(r9.mWindow);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void remove() {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long now = SystemClock.uptimeMillis();
                if (this.mSizeMismatch && now - this.mShownTime < SIZE_MISMATCH_MINIMUM_TIME_MS) {
                    this.mHandler.postAtTime(new -$$Lambda$-OevXHSXgaSE351ZqRnMoA024MM(this), this.mShownTime + SIZE_MISMATCH_MINIMUM_TIME_MS);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    @VisibleForTesting
    void setFrames(Rect frame, Rect contentInsets, Rect stableInsets) {
        this.mFrame.set(frame);
        this.mContentInsets.set(contentInsets);
        this.mStableInsets.set(stableInsets);
        boolean z = (this.mFrame.width() == this.mSnapshot.getSnapshot().getWidth() && this.mFrame.height() == this.mSnapshot.getSnapshot().getHeight()) ? false : true;
        this.mSizeMismatch = z;
        this.mSystemBarBackgroundPainter.setInsets(contentInsets, stableInsets);
    }

    private void drawSnapshot() {
        GraphicBuffer buffer = this.mSnapshot.getSnapshot();
        if (this.mSizeMismatch) {
            drawSizeMismatchSnapshot(buffer);
        } else {
            drawSizeMatchSnapshot(buffer);
        }
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mShownTime = SystemClock.uptimeMillis();
                this.mHasDrawn = true;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        reportDrawn();
        this.mSnapshot = null;
    }

    private void drawSizeMatchSnapshot(GraphicBuffer buffer) {
        this.mSurface.attachAndQueueBuffer(buffer);
        this.mSurface.release();
    }

    private void drawSizeMismatchSnapshot(GraphicBuffer buffer) {
        Builder builder = new Builder(new SurfaceSession(this.mSurface));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mTitle);
        stringBuilder.append(" - task-snapshot-surface");
        this.mChildSurfaceControl = builder.setName(stringBuilder.toString()).setSize(buffer.getWidth(), buffer.getHeight()).setFormat(buffer.getFormat()).build();
        Surface surface = new Surface();
        surface.copyFrom(this.mChildSurfaceControl);
        Rect crop = calculateSnapshotCrop();
        Rect frame = calculateSnapshotFrame(crop);
        SurfaceControl.openTransaction();
        try {
            this.mChildSurfaceControl.show();
            this.mChildSurfaceControl.setWindowCrop(crop);
            this.mChildSurfaceControl.setPosition((float) frame.left, (float) frame.top);
            float scale = 1.0f / this.mSnapshot.getScale();
            this.mChildSurfaceControl.setMatrix(scale, 0.0f, 0.0f, scale);
            surface.attachAndQueueBuffer(buffer);
            surface.release();
            Canvas c = this.mSurface.lockCanvas(null);
            drawBackgroundAndBars(c, frame);
            this.mSurface.unlockCanvasAndPost(c);
            this.mSurface.release();
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    @VisibleForTesting
    Rect calculateSnapshotCrop() {
        Rect rect = new Rect();
        int i = 0;
        rect.set(0, 0, this.mSnapshot.getSnapshot().getWidth(), this.mSnapshot.getSnapshot().getHeight());
        Rect insets = this.mSnapshot.getContentInsets();
        boolean isTop = this.mTaskBounds.top == 0 && this.mFrame.top == 0;
        int scale = (int) (((float) insets.left) * this.mSnapshot.getScale());
        if (!isTop) {
            i = (int) (((float) insets.top) * this.mSnapshot.getScale());
        }
        rect.inset(scale, i, (int) (((float) insets.right) * this.mSnapshot.getScale()), (int) (((float) insets.bottom) * this.mSnapshot.getScale()));
        return rect;
    }

    @VisibleForTesting
    Rect calculateSnapshotFrame(Rect crop) {
        Rect frame = new Rect(crop);
        float scale = this.mSnapshot.getScale();
        frame.scale(1.0f / scale);
        frame.offsetTo((int) (((float) (-crop.left)) / scale), (int) (((float) (-crop.top)) / scale));
        frame.offset(DecorView.getColorViewLeftInset(this.mStableInsets.left, this.mContentInsets.left), 0);
        return frame;
    }

    @VisibleForTesting
    void drawBackgroundAndBars(Canvas c, Rect frame) {
        Rect rect = frame;
        int statusBarHeight = this.mSystemBarBackgroundPainter.getStatusBarColorViewHeight();
        boolean z = false;
        boolean fillHorizontally = c.getWidth() > rect.right;
        if (c.getHeight() > rect.bottom) {
            z = true;
        }
        boolean fillVertically = z;
        if (fillHorizontally) {
            float f;
            float f2 = (float) rect.right;
            float f3 = Color.alpha(this.mStatusBarColor) == 255 ? (float) statusBarHeight : 0.0f;
            float width = (float) c.getWidth();
            if (fillVertically) {
                f = (float) rect.bottom;
            } else {
                f = (float) c.getHeight();
            }
            c.drawRect(f2, f3, width, f, this.mBackgroundPaint);
        }
        if (fillVertically) {
            c.drawRect(0.0f, (float) rect.bottom, (float) c.getWidth(), (float) c.getHeight(), this.mBackgroundPaint);
        }
        this.mSystemBarBackgroundPainter.drawDecors(c, rect);
    }

    private void reportDrawn() {
        try {
            this.mSession.finishDrawing(this.mWindow);
        } catch (RemoteException e) {
        }
    }
}
