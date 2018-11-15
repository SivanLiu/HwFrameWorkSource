package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

class WindowSurfaceController {
    private static final boolean IS_DEBUG_VERSION;
    static final String TAG = "WindowManager";
    final WindowStateAnimator mAnimator;
    private float mBlurAlpha;
    private Rect mBlurBlank;
    private int mBlurRadius;
    private Region mBlurRegion;
    private int mBlurRoundx;
    private int mBlurRoundy;
    private boolean mHiddenForCrop = false;
    private boolean mHiddenForOtherReasons = true;
    private float mLastDsdx = 1.0f;
    private float mLastDsdy = 0.0f;
    private float mLastDtdx = 0.0f;
    private float mLastDtdy = 1.0f;
    private final WindowManagerService mService;
    private float mSurfaceAlpha = 0.0f;
    SurfaceControl mSurfaceControl;
    private int mSurfaceH = 0;
    int mSurfaceLayer = 0;
    boolean mSurfaceShown = false;
    private int mSurfaceW = 0;
    private float mSurfaceX = 0.0f;
    private float mSurfaceY = 0.0f;
    private final Transaction mTmpTransaction = new Transaction();
    private final Session mWindowSession;
    private final int mWindowType;
    private final String title;

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
    }

    public WindowSurfaceController(SurfaceSession s, String name, int w, int h, int format, int flags, WindowStateAnimator animator, int windowType, int ownerUid) {
        this.mAnimator = animator;
        this.mSurfaceW = w;
        this.mSurfaceH = h;
        this.title = name;
        this.mService = animator.mService;
        WindowState win = animator.mWin;
        this.mWindowType = windowType;
        this.mWindowSession = win.mSession;
        Trace.traceBegin(32, "new SurfaceControl");
        this.mSurfaceControl = win.makeSurface().setParent(win.getSurfaceControl()).setName(name).setSize(w, h).setFormat(format).setFlags(flags).setMetadata(windowType, ownerUid).build();
        Trace.traceEnd(32);
    }

    private void logSurface(String msg, RuntimeException where) {
        String str = new StringBuilder();
        str.append("  SURFACE ");
        str.append(msg);
        str.append(": ");
        str.append(this.title);
        str = str.toString();
        if (where != null) {
            Slog.i(TAG, str, where);
        } else {
            Slog.i(TAG, str);
        }
    }

    void reparentChildrenInTransaction(WindowSurfaceController other) {
        if (this.mSurfaceControl != null && other.mSurfaceControl != null) {
            this.mSurfaceControl.reparentChildren(other.getHandle());
        }
    }

    void detachChildren() {
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.detachChildren();
        }
    }

    void hide(Transaction transaction, String reason) {
        this.mHiddenForOtherReasons = true;
        this.mAnimator.destroyPreservedSurfaceLocked();
        if (this.mSurfaceShown) {
            hideSurface(transaction);
        }
    }

    private void hideSurface(Transaction transaction) {
        if (this.mSurfaceControl != null) {
            setShown(false);
            try {
                transaction.hide(this.mSurfaceControl);
                synchronized (this.mAnimator.mInsetSurfaceLock) {
                    if (this.mAnimator.mInsetSurfaceOverlay != null && this.mService.mPolicy.isInputMethodMovedUp()) {
                        this.mAnimator.mInsetSurfaceOverlay.hide(transaction);
                    }
                }
            } catch (RuntimeException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception hiding surface in ");
                stringBuilder.append(this);
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    void destroyNotInTransaction() {
        if (IS_DEBUG_VERSION) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Destroying surface ");
            stringBuilder.append(this);
            stringBuilder.append(" called by ");
            stringBuilder.append(Debug.getCallers(8));
            Slog.i(str, stringBuilder.toString());
        }
        try {
            this.mAnimator.mWin.mClient.updateSurfaceStatus(false);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception thrown when updateSurfaceStatus");
            stringBuilder2.append(this);
            stringBuilder2.append(": ");
            stringBuilder2.append(e);
            Slog.w(str2, stringBuilder2.toString());
        }
        try {
            if (this.mSurfaceControl != null) {
                this.mSurfaceControl.destroy();
            }
            setShown(false);
            this.mSurfaceControl = null;
            synchronized (this.mAnimator.mInsetSurfaceLock) {
                if (this.mAnimator.mInsetSurfaceOverlay != null) {
                    this.mAnimator.mInsetSurfaceOverlay.destroy();
                    this.mAnimator.mInsetSurfaceOverlay = null;
                }
            }
        } catch (RuntimeException e2) {
            try {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error destroying surface in: ");
                stringBuilder3.append(this);
                Slog.w(str3, stringBuilder3.toString(), e2);
                setShown(false);
                this.mSurfaceControl = null;
                synchronized (this.mAnimator.mInsetSurfaceLock) {
                    if (this.mAnimator.mInsetSurfaceOverlay != null) {
                        this.mAnimator.mInsetSurfaceOverlay.destroy();
                        this.mAnimator.mInsetSurfaceOverlay = null;
                    }
                }
            } catch (Throwable th) {
                setShown(false);
                this.mSurfaceControl = null;
                synchronized (this.mAnimator.mInsetSurfaceLock) {
                    if (this.mAnimator.mInsetSurfaceOverlay != null) {
                        this.mAnimator.mInsetSurfaceOverlay.destroy();
                        this.mAnimator.mInsetSurfaceOverlay = null;
                    }
                }
            }
        }
    }

    void disconnectInTransaction() {
        try {
            if (this.mSurfaceControl != null) {
                this.mSurfaceControl.disconnect();
            }
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error disconnecting surface in: ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    void setCropInTransaction(Rect clipRect, boolean recoveringMemory) {
        try {
            if (clipRect.width() <= 0 || clipRect.height() <= 0) {
                this.mHiddenForCrop = true;
                this.mAnimator.destroyPreservedSurfaceLocked();
                updateVisibility();
                return;
            }
            this.mSurfaceControl.setWindowCrop(clipRect);
            this.mHiddenForCrop = false;
            updateVisibility();
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error setting crop surface of ");
            stringBuilder.append(this);
            stringBuilder.append(" crop=");
            stringBuilder.append(clipRect.toShortString());
            Slog.w(str, stringBuilder.toString(), e);
            if (!recoveringMemory) {
                this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
            }
        }
    }

    void clearCropInTransaction(boolean recoveringMemory) {
        try {
            this.mSurfaceControl.setWindowCrop(new Rect(0, 0, -1, -1));
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error setting clearing crop of ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            if (!recoveringMemory) {
                this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
            }
        }
    }

    void setFinalCropInTransaction(Rect clipRect) {
        try {
            this.mSurfaceControl.setFinalCrop(clipRect);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error disconnecting surface in: ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    void setLayerStackInTransaction(int layerStack) {
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.setLayerStack(layerStack);
        }
    }

    void setPositionInTransaction(float left, float top, boolean recoveringMemory) {
        setPosition(null, left, top, recoveringMemory);
    }

    void setPosition(Transaction t, float left, float top, boolean recoveringMemory) {
        boolean surfaceMoved = (this.mSurfaceX == left && this.mSurfaceY == top) ? false : true;
        if (surfaceMoved) {
            this.mSurfaceX = left;
            this.mSurfaceY = top;
            if (t == null) {
                try {
                    this.mSurfaceControl.setPosition(left, top);
                    return;
                } catch (RuntimeException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error positioning surface of ");
                    stringBuilder.append(this);
                    stringBuilder.append(" pos=(");
                    stringBuilder.append(left);
                    stringBuilder.append(",");
                    stringBuilder.append(top);
                    stringBuilder.append(")");
                    Slog.w(str, stringBuilder.toString(), e);
                    if (!recoveringMemory) {
                        this.mAnimator.reclaimSomeSurfaceMemory("position", true);
                        return;
                    }
                    return;
                }
            }
            t.setPosition(this.mSurfaceControl, left, top);
        }
    }

    void setGeometryAppliesWithResizeInTransaction(boolean recoveringMemory) {
        this.mSurfaceControl.setGeometryAppliesWithResize();
    }

    void setMatrixInTransaction(float dsdx, float dtdx, float dtdy, float dsdy, boolean recoveringMemory) {
        setMatrix(null, dsdx, dtdx, dtdy, dsdy, false);
    }

    void setMatrix(Transaction t, float dsdx, float dtdx, float dtdy, float dsdy, boolean recoveringMemory) {
        boolean matrixChanged = (this.mLastDsdx == dsdx && this.mLastDtdx == dtdx && this.mLastDtdy == dtdy && this.mLastDsdy == dsdy) ? false : true;
        if (matrixChanged) {
            this.mLastDsdx = dsdx;
            this.mLastDtdx = dtdx;
            this.mLastDtdy = dtdy;
            this.mLastDsdy = dsdy;
            if (t == null) {
                try {
                    this.mSurfaceControl.setMatrix(dsdx, dtdx, dtdy, dsdy);
                } catch (RuntimeException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error setting matrix on surface surface");
                    stringBuilder.append(this.title);
                    stringBuilder.append(" MATRIX [");
                    stringBuilder.append(dsdx);
                    stringBuilder.append(",");
                    stringBuilder.append(dtdx);
                    stringBuilder.append(",");
                    stringBuilder.append(dtdy);
                    stringBuilder.append(",");
                    stringBuilder.append(dsdy);
                    stringBuilder.append("]");
                    Slog.e(str, stringBuilder.toString(), null);
                    if (!recoveringMemory) {
                        this.mAnimator.reclaimSomeSurfaceMemory("matrix", true);
                    }
                }
            } else {
                t.setMatrix(this.mSurfaceControl, dsdx, dtdx, dtdy, dsdy);
            }
        }
    }

    boolean setSizeInTransaction(int width, int height, boolean recoveringMemory) {
        boolean surfaceResized = (this.mSurfaceW == width && this.mSurfaceH == height) ? false : true;
        if (!surfaceResized) {
            return false;
        }
        this.mSurfaceW = width;
        this.mSurfaceH = height;
        try {
            if (this.mAnimator.mWin.mAppToken != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SIZE ");
                stringBuilder.append(width);
                stringBuilder.append("x");
                stringBuilder.append(height);
                logSurface(stringBuilder.toString(), null);
            }
            this.mSurfaceControl.setSize(width, height);
            synchronized (this.mAnimator.mInsetSurfaceLock) {
                if (this.mAnimator.mInsetSurfaceOverlay != null && this.mService.mPolicy.isInputMethodMovedUp()) {
                    this.mAnimator.mInsetSurfaceOverlay.updateSurface(this.mAnimator.mWin);
                }
            }
            return true;
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error resizing surface of ");
            stringBuilder2.append(this.title);
            stringBuilder2.append(" size=(");
            stringBuilder2.append(width);
            stringBuilder2.append("x");
            stringBuilder2.append(height);
            stringBuilder2.append(")");
            Slog.e(str, stringBuilder2.toString(), e);
            if (!recoveringMemory) {
                this.mAnimator.reclaimSomeSurfaceMemory("size", true);
            }
            return false;
        }
    }

    boolean prepareToShowInTransaction(float alpha, float dsdx, float dtdx, float dsdy, float dtdy, boolean recoveringMemory) {
        if (this.mSurfaceControl != null) {
            try {
                this.mSurfaceAlpha = alpha;
                this.mSurfaceControl.setAlpha(alpha);
                this.mLastDsdx = dsdx;
                this.mLastDtdx = dtdx;
                this.mLastDsdy = dsdy;
                this.mLastDtdy = dtdy;
                this.mSurfaceControl.setMatrix(dsdx, dtdx, dsdy, dtdy);
            } catch (RuntimeException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error updating surface in ");
                stringBuilder.append(this.title);
                Slog.w(str, stringBuilder.toString(), e);
                if (!recoveringMemory) {
                    this.mAnimator.reclaimSomeSurfaceMemory("update", true);
                }
                return false;
            }
        }
        return true;
    }

    void setTransparentRegionHint(Region region) {
        if (this.mSurfaceControl == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
            return;
        }
        this.mService.openSurfaceTransaction();
        try {
            this.mSurfaceControl.setTransparentRegionHint(region);
        } finally {
            this.mService.closeSurfaceTransaction("setTransparentRegion");
        }
    }

    void setOpaque(boolean isOpaque) {
        if (this.mSurfaceControl != null) {
            this.mService.openSurfaceTransaction();
            try {
                this.mSurfaceControl.setOpaque(isOpaque);
            } finally {
                this.mService.closeSurfaceTransaction("setOpaqueLocked");
            }
        }
    }

    void setSecure(boolean isSecure) {
        if (this.mSurfaceControl != null) {
            this.mService.openSurfaceTransaction();
            try {
                this.mSurfaceControl.setSecure(isSecure);
            } finally {
                this.mService.closeSurfaceTransaction("setSecure");
            }
        }
    }

    void getContainerRect(Rect rect) {
        this.mAnimator.getContainerRect(rect);
    }

    boolean showRobustlyInTransaction() {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Showing ");
            stringBuilder.append(this);
            stringBuilder.append(" during relayout");
            Slog.v(str, stringBuilder.toString());
        }
        this.mHiddenForOtherReasons = false;
        return updateVisibility();
    }

    private boolean updateVisibility() {
        if (this.mHiddenForCrop || this.mHiddenForOtherReasons) {
            if (this.mSurfaceShown) {
                hideSurface(this.mTmpTransaction);
                SurfaceControl.mergeToGlobalTransaction(this.mTmpTransaction);
            }
            return false;
        } else if (this.mSurfaceShown) {
            return true;
        } else {
            return showSurface();
        }
    }

    private boolean showSurface() {
        try {
            setShown(true);
            this.mSurfaceControl.show();
            synchronized (this.mAnimator.mInsetSurfaceLock) {
                if (this.mAnimator.mInsetSurfaceOverlay != null && this.mService.mPolicy.isInputMethodMovedUp()) {
                    this.mAnimator.mInsetSurfaceOverlay.show();
                }
            }
            return true;
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure showing surface ");
            stringBuilder.append(this.mSurfaceControl);
            stringBuilder.append(" in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("show", true);
            return false;
        }
    }

    void deferTransactionUntil(IBinder handle, long frame) {
        this.mSurfaceControl.deferTransactionUntil(handle, frame);
    }

    void forceScaleableInTransaction(boolean force) {
        this.mSurfaceControl.setOverrideScalingMode(force ? 1 : -1);
    }

    boolean clearWindowContentFrameStats() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.clearContentFrameStats();
    }

    boolean getWindowContentFrameStats(WindowContentFrameStats outStats) {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.getContentFrameStats(outStats);
    }

    boolean hasSurface() {
        return this.mSurfaceControl != null;
    }

    IBinder getHandle() {
        if (this.mSurfaceControl == null) {
            return null;
        }
        return this.mSurfaceControl.getHandle();
    }

    void getSurface(Surface outSurface) {
        outSurface.copyFrom(this.mSurfaceControl);
    }

    int getLayer() {
        return this.mSurfaceLayer;
    }

    boolean getShown() {
        return this.mSurfaceShown;
    }

    void setShown(boolean surfaceShown) {
        this.mSurfaceShown = surfaceShown;
        this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mAnimator.mWin, surfaceShown);
        if (this.mWindowSession != null) {
            this.mWindowSession.onWindowSurfaceVisibilityChanged(this, this.mSurfaceShown, this.mWindowType);
        }
    }

    float getX() {
        return this.mSurfaceX;
    }

    float getY() {
        return this.mSurfaceY;
    }

    int getWidth() {
        return this.mSurfaceW;
    }

    int getHeight() {
        return this.mSurfaceH;
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mSurfaceShown);
        proto.write(1120986464258L, this.mSurfaceLayer);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mSurface=");
            pw.println(this.mSurfaceControl);
        }
        pw.print(prefix);
        pw.print("Surface: shown=");
        pw.print(this.mSurfaceShown);
        pw.print(" layer=");
        pw.print(this.mSurfaceLayer);
        pw.print(" alpha=");
        pw.print(this.mSurfaceAlpha);
        pw.print(" rect=(");
        pw.print(this.mSurfaceX);
        pw.print(",");
        pw.print(this.mSurfaceY);
        pw.print(") ");
        pw.print(this.mSurfaceW);
        pw.print(" x ");
        pw.print(this.mSurfaceH);
        pw.print(" transform=(");
        pw.print(this.mLastDsdx);
        pw.print(", ");
        pw.print(this.mLastDtdx);
        pw.print(", ");
        pw.print(this.mLastDsdy);
        pw.print(", ");
        pw.print(this.mLastDtdy);
        pw.println(")");
        pw.print(prefix);
        pw.print("blurRadius=");
        pw.print(this.mBlurRadius);
        pw.print(" blurRound=(");
        pw.print(this.mBlurRoundx);
        pw.print(",");
        pw.print(this.mBlurRoundy);
        pw.print(")");
        pw.print(" blurAlpha=");
        pw.print(this.mBlurAlpha);
        pw.print(" blurRegion=");
        pw.print(this.mBlurRegion);
        pw.print(" blurBlank=");
        pw.println(this.mBlurBlank);
    }

    public String toString() {
        return this.mSurfaceControl.toString();
    }

    public void setBlurRadius(int radius) {
        SurfaceControl.openTransaction();
        try {
            this.mBlurRadius = radius;
            this.mSurfaceControl.setBlurRadius(radius);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating surface in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("blur-radius", true);
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
        }
        SurfaceControl.closeTransaction();
    }

    public void setBlurRound(int rx, int ry) {
        SurfaceControl.openTransaction();
        try {
            this.mBlurRoundx = rx;
            this.mBlurRoundy = ry;
            this.mSurfaceControl.setBlurRound(rx, ry);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating surface in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("blur-round", true);
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
        }
        SurfaceControl.closeTransaction();
    }

    public void setBlurAlpha(float alpha) {
        SurfaceControl.openTransaction();
        try {
            this.mBlurAlpha = alpha;
            this.mSurfaceControl.setBlurAlpha(alpha);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating surface in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("blur-alpha", true);
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
        }
        SurfaceControl.closeTransaction();
    }

    public void setBlurRegion(Region region) {
        SurfaceControl.openTransaction();
        try {
            this.mBlurRegion = region;
            this.mSurfaceControl.setBlurRegion(region);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating surface in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("blur-region", true);
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
        }
        SurfaceControl.closeTransaction();
    }

    public void setBlurBlank(Rect rect) {
        SurfaceControl.openTransaction();
        try {
            this.mBlurBlank = rect;
            this.mSurfaceControl.setBlurBlank(rect);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating surface in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString(), e);
            this.mAnimator.reclaimSomeSurfaceMemory("blur-blank", true);
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
        }
        SurfaceControl.closeTransaction();
    }

    public void clearWindowClipFlag() {
        if (this.mSurfaceControl == null) {
            Slog.e(TAG, "mSurfaceControl is null!!!");
            return;
        }
        this.mSurfaceControl.setWindowClipFlag(0);
        this.mSurfaceControl.setWindowClipRound(0.0f, 0.0f);
    }

    public void setWindowClipFlag(int flag) {
        if (this.mSurfaceControl == null) {
            Slog.e(TAG, "mSurfaceControl is null!!!");
        } else {
            this.mSurfaceControl.setWindowClipFlag(flag);
        }
    }

    public void setWindowClipRound(float roundx, float roundy) {
        if (this.mSurfaceControl == null) {
            Slog.e(TAG, "mSurfaceControl is null!!!");
        } else {
            this.mSurfaceControl.setWindowClipRound(roundx, roundy);
        }
    }

    public void setWindowClipIcon(int iconViewWidth, int iconViewHeight, Bitmap icon) {
        if (this.mSurfaceControl == null) {
            Slog.e(TAG, "mSurfaceControl is null!!!");
        } else if (icon == null) {
            Slog.e(TAG, "icon is null!!");
        } else {
            int width = icon.getWidth();
            int height = icon.getHeight();
            int byteCount = icon.getRowBytes() * icon.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteCount);
            icon.copyPixelsToBuffer(byteBuffer);
            this.mSurfaceControl.setWindowClipIcon(iconViewWidth, iconViewHeight, byteBuffer.array(), byteCount, width, height);
        }
    }

    void setSurfaceLowResolutionInfo(float ratio, int mode) {
        this.mSurfaceControl.setSurfaceLowResolutionInfo(ratio, mode);
    }

    void setSecureScreenShot(boolean isSecureScreenShot) {
        if (this.mSurfaceControl != null) {
            this.mService.openSurfaceTransaction();
            try {
                this.mSurfaceControl.setSecureScreenShot(isSecureScreenShot);
            } finally {
                this.mService.closeSurfaceTransaction("setSecureScreenShot");
            }
        }
    }

    void setSecureScreenRecord(boolean isSecureScreenRecord) {
        if (this.mSurfaceControl != null) {
            this.mService.openSurfaceTransaction();
            try {
                this.mSurfaceControl.setSecureScreenRecord(isSecureScreenRecord);
            } finally {
                this.mService.closeSurfaceTransaction("setSecureScreenRecord");
            }
        }
    }
}
