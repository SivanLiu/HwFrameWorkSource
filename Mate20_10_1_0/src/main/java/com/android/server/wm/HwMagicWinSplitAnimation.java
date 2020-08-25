package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.GraphicBuffer;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import com.android.server.magicwin.HwMagicWinAnimation;
import com.android.server.multiwin.HwBlur;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

class HwMagicWinSplitAnimation implements SurfaceAnimator.Animatable {
    private static final int ANIMATION_LAYER = 2009999;
    private static final int ANIMATION_LAYER_BACKGROUND = 2009997;
    private static final int ANIMATION_LAYER_BASE = 2010000;
    private static final int ANIMATION_LAYER_EXIT = 2009998;
    private static final int BG_COLOR = -987148;
    private static final int BLUR_DOWNSCALE = 1;
    private static final int BLUR_RADIUS = 10;
    private static final float BLUR_SCALE = 0.05f;
    private static final int COVER_COLOR = Integer.MAX_VALUE;
    private static final float SCALE_FULL = 1.0f;
    private static final String TAG = "HwMagicWinSplitAnimation";
    protected GraphicBuffer mBackgourndBuffer;
    protected SurfaceControl mBackgourndSurface;
    protected GraphicBuffer mGraphicBuffer;
    protected final int mHeigth;
    private final SurfaceAnimator mSurfaceAnimator;
    protected SurfaceControl mSurfaceControl;
    protected WindowContainer mTaskStackContainers;
    protected final int mWidth;

    HwMagicWinSplitAnimation(WindowContainer container, GraphicBuffer buffer) {
        this(container, buffer, buffer.getWidth(), buffer.getHeight());
    }

    HwMagicWinSplitAnimation(WindowContainer container, GraphicBuffer buffer, int width, int height) {
        this.mWidth = width;
        this.mHeigth = height;
        this.mTaskStackContainers = container;
        this.mSurfaceAnimator = new SurfaceAnimator(this, new Runnable() {
            /* class com.android.server.wm.$$Lambda$anjUVzawOhcJamm9PgAXPIHRBI8 */

            public final void run() {
                HwMagicWinSplitAnimation.this.onAnimationFinished();
            }
        }, container.mWmService);
        this.mSurfaceControl = makeSurfaceController(TAG, this.mWidth, this.mHeigth);
        this.mGraphicBuffer = buffer;
    }

    /* access modifiers changed from: package-private */
    public void showAnimationSurface(float cornerRadius) {
        this.mSurfaceControl.setWindowCrop(new Rect(0, 0, this.mWidth, this.mHeigth));
        this.mSurfaceControl.setCornerRadius(cornerRadius);
        applyBufferToSurface(this.mSurfaceControl, this.mGraphicBuffer);
        SurfaceControl.Transaction transaction = getPendingTransaction();
        transaction.show(this.mSurfaceControl);
        transaction.apply();
    }

    /* access modifiers changed from: package-private */
    public void applyBufferToSurface(SurfaceControl surfaceControl, GraphicBuffer buffer) {
        attachBufferToSurface(surfaceControl, buffer);
    }

    /* access modifiers changed from: package-private */
    public void showBgBufferSurface(GraphicBuffer buffer, Point position) {
        this.mBackgourndBuffer = buffer;
        this.mBackgourndSurface = makeSurfaceController(TAG, buffer.getWidth(), buffer.getHeight());
        attachBufferToSurface(this.mBackgourndSurface, this.mBackgourndBuffer);
        showBackgroundSurface(position);
    }

    /* access modifiers changed from: package-private */
    public void showBgColoredSurface(int color, Rect bounds) {
        this.mBackgourndSurface = makeSurfaceController(TAG, bounds.width(), bounds.height());
        Surface surface = new Surface();
        surface.copyFrom(this.mBackgourndSurface);
        Canvas canvas = null;
        try {
            canvas = surface.lockCanvas(bounds);
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
            Slog.i(TAG, "lockCanvas exception");
        }
        if (canvas != null) {
            canvas.drawColor(color);
            surface.unlockCanvasAndPost(canvas);
            surface.destroy();
            showBackgroundSurface(new Point(bounds.left, bounds.top));
        }
    }

    private void destroyGraphicBuffer(GraphicBuffer buffer) {
        if (buffer != null && !buffer.isDestroyed()) {
            buffer.destroy();
        }
    }

    private void destroySurfaceControl(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        if (surfaceControl != null) {
            transaction.remove(surfaceControl);
        }
    }

    private SurfaceControl makeSurfaceController(String surfaceName, int width, int height) {
        return this.mTaskStackContainers.makeChildSurface((WindowContainer) null).setParent(this.mTaskStackContainers.getSurfaceControl()).setName(surfaceName).setFormat(-3).setBufferSize(width, height).build();
    }

    private void attachBufferToSurface(SurfaceControl sc, GraphicBuffer buffer) {
        Surface surface = new Surface();
        surface.copyFrom(sc);
        surface.attachAndQueueBuffer(buffer);
        surface.release();
    }

    private void showBackgroundSurface(Point position) {
        SurfaceControl.Transaction transaction = getPendingTransaction();
        transaction.setPosition(this.mBackgourndSurface, (float) position.x, (float) position.y);
        transaction.setLayer(this.mBackgourndSurface, ANIMATION_LAYER_BACKGROUND);
        transaction.show(this.mBackgourndSurface);
        transaction.apply();
    }

    private void destroyAnimation(SurfaceControl.Transaction transaction) {
        destroyGraphicBuffer(this.mGraphicBuffer);
        this.mGraphicBuffer = null;
        destroySurfaceControl(transaction, this.mSurfaceControl);
        this.mSurfaceControl = null;
        destroyGraphicBuffer(this.mBackgourndBuffer);
        this.mBackgourndBuffer = null;
        destroySurfaceControl(transaction, this.mBackgourndSurface);
        this.mBackgourndSurface = null;
    }

    public SurfaceControl.Transaction getPendingTransaction() {
        return this.mTaskStackContainers.getPendingTransaction();
    }

    public void commitPendingTransaction() {
        this.mTaskStackContainers.commitPendingTransaction();
    }

    public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        t.setLayer(leash, ANIMATION_LAYER);
    }

    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            t.hide(surfaceControl);
        }
        SurfaceControl surfaceControl2 = this.mBackgourndSurface;
        if (surfaceControl2 != null) {
            t.hide(surfaceControl2);
        }
    }

    public SurfaceControl.Builder makeAnimationLeash() {
        return this.mTaskStackContainers.makeAnimationLeash();
    }

    public SurfaceControl getAnimationLeashParent() {
        return this.mTaskStackContainers.getSurfaceControl();
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public SurfaceControl getParentSurfaceControl() {
        return this.mTaskStackContainers.getSurfaceControl();
    }

    public int getSurfaceWidth() {
        return this.mWidth;
    }

    public int getSurfaceHeight() {
        return this.mHeigth;
    }

    /* access modifiers changed from: package-private */
    public void onAnimationFinished() {
        destroyAnimation(getPendingTransaction());
    }

    /* access modifiers changed from: package-private */
    public void stopAnimation() {
        this.mSurfaceAnimator.cancelAnimation();
        destroyAnimation(getPendingTransaction());
    }

    /* access modifiers changed from: package-private */
    public void startAnimation(AnimationAdapter anim, boolean isHidden) {
        this.mSurfaceAnimator.startAnimation(getPendingTransaction(), anim, isHidden);
    }

    public static class BlurWindowAnimation extends HwMagicWinSplitAnimation {
        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ void commitPendingTransaction() {
            HwMagicWinSplitAnimation.super.commitPendingTransaction();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeashParent() {
            return HwMagicWinSplitAnimation.super.getAnimationLeashParent();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ SurfaceControl getParentSurfaceControl() {
            return HwMagicWinSplitAnimation.super.getParentSurfaceControl();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ SurfaceControl.Transaction getPendingTransaction() {
            return HwMagicWinSplitAnimation.super.getPendingTransaction();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ SurfaceControl getSurfaceControl() {
            return HwMagicWinSplitAnimation.super.getSurfaceControl();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ int getSurfaceHeight() {
            return HwMagicWinSplitAnimation.super.getSurfaceHeight();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ int getSurfaceWidth() {
            return HwMagicWinSplitAnimation.super.getSurfaceWidth();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ SurfaceControl.Builder makeAnimationLeash() {
            return HwMagicWinSplitAnimation.super.makeAnimationLeash();
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
            HwMagicWinSplitAnimation.super.onAnimationLeashCreated(transaction, surfaceControl);
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public /* bridge */ /* synthetic */ void onAnimationLeashLost(SurfaceControl.Transaction transaction) {
            HwMagicWinSplitAnimation.super.onAnimationLeashLost(transaction);
        }

        BlurWindowAnimation(WindowContainer container, GraphicBuffer buffer, Rect surfaceBounds) {
            super(container, buffer, surfaceBounds.width(), surfaceBounds.height());
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public void applyBufferToSurface(SurfaceControl sc, GraphicBuffer buffer) {
            Surface surface = new Surface();
            surface.copyFrom(sc);
            Rect bounds = new Rect(0, 0, this.mWidth, this.mHeigth);
            Canvas canvas = null;
            try {
                canvas = surface.lockCanvas(bounds);
            } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
                Slog.i(HwMagicWinSplitAnimation.TAG, "lockCanvas exception");
            }
            if (canvas != null) {
                Bitmap blurBitmap = null;
                Bitmap hwBitmap = null;
                try {
                    canvas.drawColor(-1);
                    hwBitmap = Bitmap.wrapHardwareBuffer(buffer, null);
                    blurBitmap = HwBlur.blur(hwBitmap, 10, 1, false);
                    canvas.drawBitmap(blurBitmap, (Rect) null, bounds, new Paint(5));
                    canvas.drawColor(HwMagicWinSplitAnimation.COVER_COLOR);
                    surface.unlockCanvasAndPost(canvas);
                    surface.destroy();
                } finally {
                    if (blurBitmap != null && !blurBitmap.isRecycled()) {
                        blurBitmap.recycle();
                    }
                    if (hwBitmap != null && !hwBitmap.isRecycled()) {
                        hwBitmap.recycle();
                    }
                    if (!buffer.isDestroyed()) {
                        buffer.destroy();
                    }
                }
            }
        }
    }

    public static class SplitScreenAnimation {
        private final Rect mTmpRect = new Rect();

        SplitScreenAnimation() {
        }

        private GraphicBuffer captureLayers(Task captrueTask, float scale) {
            captrueTask.getBounds(this.mTmpRect);
            this.mTmpRect.offsetTo(0, 0);
            return SurfaceControl.captureLayers(captrueTask.getSurfaceControl().getHandle(), this.mTmpRect, scale).getGraphicBuffer();
        }

        private HwMagicWinSplitAnimation createEnterAnimation(Task enterTask, boolean isDrawGaussBlur) {
            DisplayContent displayContent = enterTask.getDisplayContent();
            if (isDrawGaussBlur) {
                return new BlurWindowAnimation(displayContent.mTaskStackContainers, captureLayers(enterTask, 0.05f), enterTask.getBounds());
            }
            return new HwMagicWinSplitAnimation(displayContent.mTaskStackContainers, captureLayers(enterTask, 1.0f));
        }

        private HwMagicWinSplitAnimation createExitAnimation(Task exitTask) {
            return new ExitTaskAnimation(exitTask.getDisplayContent().mTaskStackContainers, captureLayers(exitTask, 1.0f));
        }

        private void showBackgroundSurface(HwMagicWinSplitAnimation splitAnimation, DisplayContent displayContent, boolean isScreenshot) {
            Bitmap screenShot = null;
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            Rect screenBounds = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
            if (isScreenshot) {
                try {
                    screenShot = SurfaceControl.screenshot(screenBounds, screenBounds.width(), screenBounds.height(), displayContent.getRotation());
                    splitAnimation.showBgBufferSurface(screenShot.createGraphicBufferHandle(), new Point(screenBounds.left, screenBounds.top));
                } catch (Throwable th) {
                    if (0 != 0 && !screenShot.isRecycled()) {
                        screenShot.recycle();
                    }
                    throw th;
                }
            } else {
                splitAnimation.showBgColoredSurface(HwMagicWinSplitAnimation.BG_COLOR, screenBounds);
            }
            if (screenShot != null && !screenShot.isRecycled()) {
                screenShot.recycle();
            }
        }

        public void startSplitScreenAnimation(AppWindowToken appToken, HwMagicWinAnimation.AnimationParams animParams, boolean isScreenshot, float cornerRadius) {
            if (appToken != null && appToken.getTask() != null) {
                HwMagicWinSplitAnimation splitScreenAnimation = createEnterAnimation(appToken.getTask(), true);
                if (!isScreenshot) {
                    splitScreenAnimation.showAnimationSurface(cornerRadius);
                }
                showBackgroundSurface(splitScreenAnimation, appToken.mDisplayContent, isScreenshot);
                if (isScreenshot) {
                    splitScreenAnimation.showAnimationSurface(cornerRadius);
                }
                splitScreenAnimation.startAnimation(new LocalAnimationAdapter(new SplitScreenAnimationSpec(animParams.getAnimation(), null, splitScreenAnimation.mBackgourndSurface, animParams.getHideThreshold()), appToken.mWmService.mSurfaceAnimationRunner), false);
            }
        }

        public void startMultiTaskAnimation(Task enterTask, Task exitTask, HwMagicWinAnimation.AnimationParams enterAnimParams, HwMagicWinAnimation.AnimationParams exitAnimParams, boolean isDrawGaussBlur) {
            if (enterTask != null && exitTask != null) {
                HwMagicWinSplitAnimation enterTaskAnim = createEnterAnimation(enterTask, isDrawGaussBlur);
                HwMagicWinSplitAnimation exitTaskAnimation = createExitAnimation(exitTask);
                enterTaskAnim.showAnimationSurface(0.0f);
                exitTaskAnimation.showAnimationSurface(0.0f);
                showBackgroundSurface(enterTaskAnim, enterTask.getDisplayContent(), false);
                enterTaskAnim.startAnimation(new LocalAnimationAdapter(new SplitScreenAnimationSpec(enterAnimParams.getAnimation(), null, enterTaskAnim.mBackgourndSurface, enterAnimParams.getHideThreshold()), enterTask.mWmService.mSurfaceAnimationRunner), false);
                exitTaskAnimation.startAnimation(new LocalAnimationAdapter(new SplitScreenAnimationSpec(exitAnimParams.getAnimation(), new Point(exitTask.getBounds().left, exitTask.getBounds().top)), exitTask.mWmService.mSurfaceAnimationRunner), false);
            }
        }
    }

    private static class ExitTaskAnimation extends HwMagicWinSplitAnimation {
        ExitTaskAnimation(WindowContainer container, GraphicBuffer buffer) {
            super(container, buffer);
        }

        @Override // com.android.server.wm.HwMagicWinSplitAnimation
        public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
            t.setLayer(leash, HwMagicWinSplitAnimation.ANIMATION_LAYER_EXIT);
        }
    }

    /* access modifiers changed from: private */
    public static class TmpValues {
        private static final int MATRIX_LENGTH = 9;
        final float[] floats;
        final Transformation transformation;

        private TmpValues() {
            this.transformation = new Transformation();
            this.floats = new float[9];
        }
    }

    /* access modifiers changed from: private */
    public static class SplitScreenAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
        private long mAnimDuration;
        private final Point mAnimPosition;
        private long mAnimStartOffset;
        private long mAnimStartTime;
        private final Animation mAnimation;
        private final WeakReference<SurfaceControl> mHideSurface;
        private float mHideThreshold;
        private boolean mIsHasHidedSurface;
        private final ThreadLocal<TmpValues> mThreadLocalTmps;

        static /* synthetic */ TmpValues lambda$new$0() {
            return new TmpValues();
        }

        SplitScreenAnimationSpec(Animation animation) {
            this(animation, null, null, 0.0f);
        }

        SplitScreenAnimationSpec(Animation animation, Point animPos) {
            this(animation, animPos, null, 0.0f);
        }

        SplitScreenAnimationSpec(Animation animation, Point animPos, SurfaceControl hideSurface, float hideThreshold) {
            this.mAnimStartTime = 0;
            this.mAnimStartOffset = 0;
            this.mAnimDuration = 0;
            this.mAnimPosition = new Point();
            this.mIsHasHidedSurface = false;
            this.mThreadLocalTmps = ThreadLocal.withInitial($$Lambda$HwMagicWinSplitAnimation$SplitScreenAnimationSpec$CZkToanMKGprdMH6KQBXhW4Ufzw.INSTANCE);
            this.mHideThreshold = 0.0f;
            this.mAnimation = animation;
            if (animPos != null) {
                this.mAnimPosition.set(animPos.x, animPos.y);
            }
            this.mHideSurface = new WeakReference<>(hideSurface);
            if (hideSurface != null) {
                this.mAnimStartTime = animation.getStartTime();
                this.mAnimStartOffset = animation.getStartOffset();
                this.mAnimDuration = animation.computeDurationHint();
                this.mHideThreshold = hideThreshold;
            }
        }

        public void dump(PrintWriter pw, String prefix) {
        }

        public void writeToProtoInner(ProtoOutputStream proto) {
        }

        public long getDuration() {
            return this.mAnimation.computeDurationHint();
        }

        public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
            TmpValues tmp = this.mThreadLocalTmps.get();
            tmp.transformation.clear();
            this.mAnimation.getTransformation(currentPlayTime, tmp.transformation);
            tmp.transformation.getMatrix().postTranslate((float) this.mAnimPosition.x, (float) this.mAnimPosition.y);
            t.setMatrix(leash, tmp.transformation.getMatrix(), tmp.floats);
            t.setAlpha(leash, tmp.transformation.getAlpha());
            if (tmp.transformation.hasClipRect()) {
                t.setWindowCrop(leash, tmp.transformation.getClipRect());
            }
            hideSurface(t, currentPlayTime);
        }

        private void hideSurface(SurfaceControl.Transaction transaction, long currentPlayTime) {
            if (this.mHideSurface.get() != null && !this.mIsHasHidedSurface) {
                long j = this.mAnimDuration;
                if (j > 0) {
                    float f = this.mHideThreshold;
                    if (f + 1.0f > 1.0f && (((float) (currentPlayTime - (this.mAnimStartTime + this.mAnimStartOffset))) * 1.0f) / ((float) j) >= f) {
                        transaction.hide(this.mHideSurface.get());
                        this.mIsHasHidedSurface = true;
                    }
                }
            }
        }
    }
}
