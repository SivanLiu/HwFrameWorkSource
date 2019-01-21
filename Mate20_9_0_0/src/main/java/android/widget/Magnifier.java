package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DisplayListCanvas;
import android.view.PixelCopy;
import android.view.PixelCopy.OnPixelCopyFinishedListener;
import android.view.RenderNode;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceHolder;
import android.view.SurfaceSession;
import android.view.SurfaceView;
import android.view.ThreadedRenderer.FrameDrawingCallback;
import android.view.ThreadedRenderer.SimpleRenderer;
import android.view.View;
import android.view.ViewRootImpl;
import com.android.internal.util.Preconditions;

public final class Magnifier {
    private static final int NONEXISTENT_PREVIOUS_CONFIG_VALUE = -1;
    private static final HandlerThread sPixelCopyHandlerThread = new HandlerThread("magnifier pixel copy result handler");
    private final int mBitmapHeight;
    private final int mBitmapWidth;
    private Callback mCallback;
    private final Point mCenterZoomCoords = new Point();
    private final Point mClampedCenterZoomCoords = new Point();
    private SurfaceInfo mContentCopySurface;
    private final Object mLock = new Object();
    private SurfaceInfo mParentSurface;
    private final Rect mPixelCopyRequestRect = new Rect();
    private final PointF mPrevPosInView = new PointF(-1.0f, -1.0f);
    private final Point mPrevStartCoordsInSurface = new Point(-1, -1);
    private final View mView;
    private final int[] mViewCoordinatesInSurface;
    private InternalPopupWindow mWindow;
    private final Point mWindowCoords = new Point();
    private final float mWindowCornerRadius;
    private final float mWindowElevation;
    private final int mWindowHeight;
    private final int mWindowWidth;
    private final float mZoom;

    public interface Callback {
        void onOperationComplete();
    }

    private static class InternalPopupWindow {
        private static final int CONTENT_BITMAP_ALPHA = 242;
        private static final int SURFACE_Z = 5;
        private Bitmap mBitmap;
        private final RenderNode mBitmapRenderNode;
        private Callback mCallback;
        private final int mContentHeight;
        private final int mContentWidth;
        private final Object mDestroyLock = new Object();
        private final Display mDisplay;
        private boolean mFirstDraw = true;
        private boolean mFrameDrawScheduled;
        private final Handler mHandler;
        private int mLastDrawContentPositionX;
        private int mLastDrawContentPositionY;
        private final Object mLock;
        private final Runnable mMagnifierUpdater;
        private final int mOffsetX;
        private final int mOffsetY;
        private boolean mPendingWindowPositionUpdate;
        private final SimpleRenderer mRenderer;
        private final Surface mSurface;
        private final SurfaceControl mSurfaceControl;
        private final int mSurfaceHeight;
        private final SurfaceSession mSurfaceSession;
        private final int mSurfaceWidth;
        private int mWindowPositionX;
        private int mWindowPositionY;

        InternalPopupWindow(Context context, Display display, Surface parentSurface, int width, int height, float elevation, float cornerRadius, Handler handler, Object lock, Callback callback) {
            this.mDisplay = display;
            this.mLock = lock;
            this.mCallback = callback;
            this.mContentWidth = width;
            this.mContentHeight = height;
            this.mOffsetX = (int) (((float) width) * 0.1f);
            this.mOffsetY = (int) (0.1f * ((float) height));
            this.mSurfaceWidth = this.mContentWidth + (this.mOffsetX * 2);
            this.mSurfaceHeight = this.mContentHeight + (2 * this.mOffsetY);
            this.mSurfaceSession = new SurfaceSession(parentSurface);
            this.mSurfaceControl = new Builder(this.mSurfaceSession).setFormat(-3).setSize(this.mSurfaceWidth, this.mSurfaceHeight).setName("magnifier surface").setFlags(4).build();
            this.mSurface = new Surface();
            this.mSurface.copyFrom(this.mSurfaceControl);
            this.mRenderer = new SimpleRenderer(context, "magnifier renderer", this.mSurface);
            this.mBitmapRenderNode = createRenderNodeForBitmap("magnifier content", elevation, cornerRadius);
            DisplayListCanvas canvas = this.mRenderer.getRootNode().start(width, height);
            try {
                canvas.insertReorderBarrier();
                canvas.drawRenderNode(this.mBitmapRenderNode);
                canvas.insertInorderBarrier();
                this.mHandler = handler;
                this.mMagnifierUpdater = new -$$Lambda$Magnifier$InternalPopupWindow$t9Cn2sIi2LBUhAVikvRPKKoAwIU(this);
                this.mFrameDrawScheduled = false;
            } finally {
                this.mRenderer.getRootNode().end(canvas);
            }
        }

        private RenderNode createRenderNodeForBitmap(String name, float elevation, float cornerRadius) {
            RenderNode bitmapRenderNode = RenderNode.create(name, null);
            bitmapRenderNode.setLeftTopRightBottom(this.mOffsetX, this.mOffsetY, this.mOffsetX + this.mContentWidth, this.mOffsetY + this.mContentHeight);
            bitmapRenderNode.setElevation(elevation);
            Outline outline = new Outline();
            outline.setRoundRect(0, 0, this.mContentWidth, this.mContentHeight, cornerRadius);
            outline.setAlpha(1.0f);
            bitmapRenderNode.setOutline(outline);
            bitmapRenderNode.setClipToOutline(true);
            DisplayListCanvas canvas = bitmapRenderNode.start(this.mContentWidth, this.mContentHeight);
            try {
                canvas.drawColor(-16711936);
                return bitmapRenderNode;
            } finally {
                bitmapRenderNode.end(canvas);
            }
        }

        public void setContentPositionForNextDraw(int contentX, int contentY) {
            this.mWindowPositionX = contentX - this.mOffsetX;
            this.mWindowPositionY = contentY - this.mOffsetY;
            this.mPendingWindowPositionUpdate = true;
            requestUpdate();
        }

        public void updateContent(Bitmap bitmap) {
            if (this.mBitmap != null) {
                this.mBitmap.recycle();
            }
            this.mBitmap = bitmap;
            requestUpdate();
        }

        private void requestUpdate() {
            if (!this.mFrameDrawScheduled) {
                Message request = Message.obtain(this.mHandler, this.mMagnifierUpdater);
                request.setAsynchronous(true);
                request.sendToTarget();
                this.mFrameDrawScheduled = true;
            }
        }

        public void destroy() {
            synchronized (this.mDestroyLock) {
                this.mSurface.destroy();
            }
            synchronized (this.mLock) {
                this.mRenderer.destroy();
                this.mSurfaceControl.destroy();
                this.mSurfaceSession.kill();
                this.mBitmapRenderNode.destroy();
                this.mHandler.removeCallbacks(this.mMagnifierUpdater);
                if (this.mBitmap != null) {
                    this.mBitmap.recycle();
                }
            }
        }

        /* JADX WARNING: Missing block: B:20:0x0080, code skipped:
            r12.mRenderer.draw(r2);
     */
        /* JADX WARNING: Missing block: B:21:0x0087, code skipped:
            if (r12.mCallback == null) goto L_0x008e;
     */
        /* JADX WARNING: Missing block: B:22:0x0089, code skipped:
            r12.mCallback.onOperationComplete();
     */
        /* JADX WARNING: Missing block: B:23:0x008e, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void doDraw() {
            synchronized (this.mLock) {
                if (this.mSurface.isValid()) {
                    DisplayListCanvas canvas = this.mBitmapRenderNode.start(this.mContentWidth, this.mContentHeight);
                    try {
                        canvas.drawColor(-1);
                        boolean z = false;
                        Rect srcRect = new Rect(z, z, this.mBitmap.getWidth(), this.mBitmap.getHeight());
                        Rect dstRect = new Rect(z, z, this.mContentWidth, this.mContentHeight);
                        Paint paint = new Paint();
                        paint.setFilterBitmap(true);
                        paint.setAlpha(242);
                        canvas.drawBitmap(this.mBitmap, srcRect, dstRect, paint);
                        if (!this.mPendingWindowPositionUpdate) {
                            if (!this.mFirstDraw) {
                                FrameDrawingCallback callback = null;
                                this.mLastDrawContentPositionX = this.mWindowPositionX + this.mOffsetX;
                                this.mLastDrawContentPositionY = this.mWindowPositionY + this.mOffsetY;
                                this.mFrameDrawScheduled = z;
                            }
                        }
                        boolean firstDraw = this.mFirstDraw;
                        this.mFirstDraw = z;
                        boolean updateWindowPosition = this.mPendingWindowPositionUpdate;
                        this.mPendingWindowPositionUpdate = z;
                        FrameDrawingCallback -__lambda_magnifier_internalpopupwindow_vzthyvjdqhg2j1gaeowcnqy2iiw = new -$$Lambda$Magnifier$InternalPopupWindow$vZThyvjDQhg2J1GAeOWCNqy2iiw(this, this.mWindowPositionX, this.mWindowPositionY, updateWindowPosition, firstDraw);
                        this.mLastDrawContentPositionX = this.mWindowPositionX + this.mOffsetX;
                        this.mLastDrawContentPositionY = this.mWindowPositionY + this.mOffsetY;
                        this.mFrameDrawScheduled = z;
                    } finally {
                        this.mBitmapRenderNode.end(canvas);
                    }
                }
            }
        }

        public static /* synthetic */ void lambda$doDraw$0(InternalPopupWindow internalPopupWindow, int pendingX, int pendingY, boolean updateWindowPosition, boolean firstDraw, long frame) {
            synchronized (internalPopupWindow.mDestroyLock) {
                if (internalPopupWindow.mSurface.isValid()) {
                    synchronized (internalPopupWindow.mLock) {
                        internalPopupWindow.mRenderer.setLightCenter(internalPopupWindow.mDisplay, pendingX, pendingY);
                        SurfaceControl.openTransaction();
                        internalPopupWindow.mSurfaceControl.deferTransactionUntil(internalPopupWindow.mSurface, frame);
                        if (updateWindowPosition) {
                            internalPopupWindow.mSurfaceControl.setPosition((float) pendingX, (float) pendingY);
                        }
                        if (firstDraw) {
                            internalPopupWindow.mSurfaceControl.setLayer(5);
                            internalPopupWindow.mSurfaceControl.show();
                        }
                        SurfaceControl.closeTransaction();
                    }
                    return;
                }
            }
        }
    }

    private static class SurfaceInfo {
        public static final SurfaceInfo NULL = new SurfaceInfo(null, 0, 0, false);
        private int mHeight;
        private boolean mIsMainWindowSurface;
        private Surface mSurface;
        private int mWidth;

        SurfaceInfo(Surface surface, int width, int height, boolean isMainWindowSurface) {
            this.mSurface = surface;
            this.mWidth = width;
            this.mHeight = height;
            this.mIsMainWindowSurface = isMainWindowSurface;
        }
    }

    static {
        sPixelCopyHandlerThread.start();
    }

    public Magnifier(View view) {
        this.mView = (View) Preconditions.checkNotNull(view);
        Context context = this.mView.getContext();
        this.mWindowWidth = context.getResources().getDimensionPixelSize(17105169);
        this.mWindowHeight = context.getResources().getDimensionPixelSize(17105167);
        this.mWindowElevation = context.getResources().getDimension(17105166);
        this.mWindowCornerRadius = getDeviceDefaultDialogCornerRadius();
        this.mZoom = context.getResources().getFloat(17105170);
        this.mBitmapWidth = Math.round(((float) this.mWindowWidth) / this.mZoom);
        this.mBitmapHeight = Math.round(((float) this.mWindowHeight) / this.mZoom);
        this.mViewCoordinatesInSurface = new int[2];
    }

    private float getDeviceDefaultDialogCornerRadius() {
        TypedArray ta = new ContextThemeWrapper(this.mView.getContext(), 16974120).obtainStyledAttributes(new int[]{16844145});
        float dialogCornerRadius = ta.getDimension(0, 0.0f);
        ta.recycle();
        return dialogCornerRadius;
    }

    public void show(float xPosInView, float yPosInView) {
        Throwable th;
        float xPosInView2 = Math.max(0.0f, Math.min(xPosInView, (float) this.mView.getWidth()));
        float yPosInView2 = Math.max(0.0f, Math.min(yPosInView, (float) this.mView.getHeight()));
        obtainSurfaces();
        obtainContentCoordinates(xPosInView2, yPosInView2);
        obtainWindowCoordinates();
        int startX = this.mClampedCenterZoomCoords.x - (this.mBitmapWidth / 2);
        int startY = this.mClampedCenterZoomCoords.y - (this.mBitmapHeight / 2);
        if (xPosInView2 != this.mPrevPosInView.x || yPosInView2 != this.mPrevPosInView.y) {
            float yPosInView3;
            if (this.mWindow == null) {
                synchronized (this.mLock) {
                    try {
                        yPosInView3 = yPosInView2;
                        this.mWindow = new InternalPopupWindow(this.mView.getContext(), this.mView.getDisplay(), this.mParentSurface.mSurface, this.mWindowWidth, this.mWindowHeight, this.mWindowElevation, this.mWindowCornerRadius, Handler.getMain(), this.mLock, this.mCallback);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
            yPosInView3 = yPosInView2;
            performPixelCopy(startX, startY, true);
            this.mPrevPosInView.x = xPosInView2;
            this.mPrevPosInView.y = yPosInView3;
        }
    }

    public void dismiss() {
        if (this.mWindow != null) {
            synchronized (this.mLock) {
                this.mWindow.destroy();
                this.mWindow = null;
            }
            this.mPrevPosInView.x = -1.0f;
            this.mPrevPosInView.y = -1.0f;
            this.mPrevStartCoordsInSurface.x = -1;
            this.mPrevStartCoordsInSurface.y = -1;
        }
    }

    public void update() {
        if (this.mWindow != null) {
            obtainSurfaces();
            performPixelCopy(this.mPrevStartCoordsInSurface.x, this.mPrevStartCoordsInSurface.y, false);
        }
    }

    public int getWidth() {
        return this.mWindowWidth;
    }

    public int getHeight() {
        return this.mWindowHeight;
    }

    public float getZoom() {
        return this.mZoom;
    }

    public Point getWindowCoords() {
        if (this.mWindow == null) {
            return null;
        }
        Rect surfaceInsets = this.mView.getViewRootImpl().mWindowAttributes.surfaceInsets;
        return new Point(this.mWindow.mLastDrawContentPositionX - surfaceInsets.left, this.mWindow.mLastDrawContentPositionY - surfaceInsets.top);
    }

    private void obtainSurfaces() {
        SurfaceInfo validMainWindowSurface = SurfaceInfo.NULL;
        if (this.mView.getViewRootImpl() != null) {
            ViewRootImpl viewRootImpl = this.mView.getViewRootImpl();
            Surface mainWindowSurface = viewRootImpl.mSurface;
            if (mainWindowSurface != null && mainWindowSurface.isValid()) {
                Rect surfaceInsets = viewRootImpl.mWindowAttributes.surfaceInsets;
                validMainWindowSurface = new SurfaceInfo(mainWindowSurface, (viewRootImpl.getWidth() + surfaceInsets.left) + surfaceInsets.right, (viewRootImpl.getHeight() + surfaceInsets.top) + surfaceInsets.bottom, true);
            }
        }
        SurfaceInfo validSurfaceViewSurface = SurfaceInfo.NULL;
        if (this.mView instanceof SurfaceView) {
            SurfaceHolder surfaceHolder = ((SurfaceView) this.mView).getHolder();
            Surface surfaceViewSurface = surfaceHolder.getSurface();
            if (surfaceViewSurface != null && surfaceViewSurface.isValid()) {
                Rect surfaceFrame = surfaceHolder.getSurfaceFrame();
                validSurfaceViewSurface = new SurfaceInfo(surfaceViewSurface, surfaceFrame.right, surfaceFrame.bottom, false);
            }
        }
        this.mParentSurface = validMainWindowSurface != SurfaceInfo.NULL ? validMainWindowSurface : validSurfaceViewSurface;
        this.mContentCopySurface = this.mView instanceof SurfaceView ? validSurfaceViewSurface : validMainWindowSurface;
    }

    private void obtainContentCoordinates(float xPosInView, float yPosInView) {
        float posX;
        float posY;
        this.mView.getLocationInSurface(this.mViewCoordinatesInSurface);
        if (this.mView instanceof SurfaceView) {
            posX = xPosInView;
            posY = yPosInView;
        } else {
            posX = ((float) this.mViewCoordinatesInSurface[0]) + xPosInView;
            posY = ((float) this.mViewCoordinatesInSurface[1]) + yPosInView;
        }
        this.mCenterZoomCoords.x = Math.round(posX);
        this.mCenterZoomCoords.y = Math.round(posY);
        Rect viewVisibleRegion = new Rect();
        this.mView.getGlobalVisibleRect(viewVisibleRegion);
        if (this.mView.getViewRootImpl() != null) {
            Rect surfaceInsets = this.mView.getViewRootImpl().mWindowAttributes.surfaceInsets;
            viewVisibleRegion.offset(surfaceInsets.left, surfaceInsets.top);
        }
        if (this.mView instanceof SurfaceView) {
            viewVisibleRegion.offset(-this.mViewCoordinatesInSurface[0], -this.mViewCoordinatesInSurface[1]);
        }
        this.mClampedCenterZoomCoords.x = Math.max(viewVisibleRegion.left + (this.mBitmapWidth / 2), Math.min(this.mCenterZoomCoords.x, viewVisibleRegion.right - (this.mBitmapWidth / 2)));
        this.mClampedCenterZoomCoords.y = this.mCenterZoomCoords.y;
    }

    private void obtainWindowCoordinates() {
        int verticalOffset = this.mView.getContext().getResources().getDimensionPixelSize(17105168);
        this.mWindowCoords.x = this.mCenterZoomCoords.x - (this.mWindowWidth / 2);
        this.mWindowCoords.y = (this.mCenterZoomCoords.y - (this.mWindowHeight / 2)) - verticalOffset;
        if (this.mParentSurface != this.mContentCopySurface) {
            Point point = this.mWindowCoords;
            point.x += this.mViewCoordinatesInSurface[0];
            point = this.mWindowCoords;
            point.y += this.mViewCoordinatesInSurface[1];
        }
    }

    private void performPixelCopy(int startXInSurface, int startYInSurface, boolean updateWindowPosition) {
        int i = startXInSurface;
        int i2 = startYInSurface;
        if (this.mContentCopySurface.mSurface != null && this.mContentCopySurface.mSurface.isValid()) {
            Rect systemInsets;
            int clampedStartXInSurface = Math.max(0, Math.min(i, this.mContentCopySurface.mWidth - this.mBitmapWidth));
            int clampedStartYInSurface = Math.max(0, Math.min(i2, this.mContentCopySurface.mHeight - this.mBitmapHeight));
            if (this.mParentSurface.mIsMainWindowSurface) {
                systemInsets = this.mView.getRootWindowInsets().getSystemWindowInsets();
                systemInsets = new Rect(systemInsets.left, systemInsets.top, this.mParentSurface.mWidth - systemInsets.right, this.mParentSurface.mHeight - systemInsets.bottom);
            } else {
                systemInsets = new Rect(0, 0, this.mParentSurface.mWidth, this.mParentSurface.mHeight);
            }
            Rect windowBounds = systemInsets;
            int windowCoordsX = Math.max(windowBounds.left, Math.min(windowBounds.right - this.mWindowWidth, this.mWindowCoords.x));
            int windowCoordsY = Math.max(windowBounds.top, Math.min(windowBounds.bottom - this.mWindowHeight, this.mWindowCoords.y));
            this.mPixelCopyRequestRect.set(clampedStartXInSurface, clampedStartYInSurface, this.mBitmapWidth + clampedStartXInSurface, this.mBitmapHeight + clampedStartYInSurface);
            InternalPopupWindow currentWindowInstance = this.mWindow;
            Bitmap bitmap = Bitmap.createBitmap(this.mBitmapWidth, this.mBitmapHeight, Config.ARGB_8888);
            Surface access$000 = this.mContentCopySurface.mSurface;
            OnPixelCopyFinishedListener onPixelCopyFinishedListener = r0;
            Rect rect = this.mPixelCopyRequestRect;
            Surface surface = access$000;
            Bitmap bitmap2 = bitmap;
            -$$Lambda$Magnifier$1ctRJdojBZQzahoS7og5wm1FKM4 -__lambda_magnifier_1ctrjdojbzqzahos7og5wm1fkm4 = new -$$Lambda$Magnifier$1ctRJdojBZQzahoS7og5wm1FKM4(this, currentWindowInstance, updateWindowPosition, windowCoordsX, windowCoordsY, bitmap);
            PixelCopy.request(surface, rect, bitmap2, onPixelCopyFinishedListener, sPixelCopyHandlerThread.getThreadHandler());
            this.mPrevStartCoordsInSurface.x = i;
            this.mPrevStartCoordsInSurface.y = i2;
        }
    }

    public static /* synthetic */ void lambda$performPixelCopy$0(Magnifier magnifier, InternalPopupWindow currentWindowInstance, boolean updateWindowPosition, int windowCoordsX, int windowCoordsY, Bitmap bitmap, int result) {
        synchronized (magnifier.mLock) {
            if (magnifier.mWindow != currentWindowInstance) {
                return;
            }
            if (updateWindowPosition) {
                magnifier.mWindow.setContentPositionForNextDraw(windowCoordsX, windowCoordsY);
            }
            magnifier.mWindow.updateContent(bitmap);
        }
    }

    public void setOnOperationCompleteCallback(Callback callback) {
        this.mCallback = callback;
        if (this.mWindow != null) {
            this.mWindow.mCallback = callback;
        }
    }

    public Bitmap getContent() {
        if (this.mWindow == null) {
            return null;
        }
        Bitmap createScaledBitmap;
        synchronized (this.mWindow.mLock) {
            createScaledBitmap = Bitmap.createScaledBitmap(this.mWindow.mBitmap, this.mWindowWidth, this.mWindowHeight, true);
        }
        return createScaledBitmap;
    }

    public Rect getWindowPositionOnScreen() {
        int[] viewLocationOnScreen = new int[2];
        this.mView.getLocationOnScreen(viewLocationOnScreen);
        int[] viewLocationInSurface = new int[2];
        this.mView.getLocationInSurface(viewLocationInSurface);
        int left = (this.mWindowCoords.x + viewLocationOnScreen[0]) - viewLocationInSurface[0];
        int top = (this.mWindowCoords.y + viewLocationOnScreen[1]) - viewLocationInSurface[1];
        return new Rect(left, top, this.mWindowWidth + left, this.mWindowHeight + top);
    }

    public static PointF getMagnifierDefaultSize() {
        Resources resources = Resources.getSystem();
        float density = resources.getDisplayMetrics().density;
        PointF size = new PointF();
        size.x = resources.getDimension(17105169) / density;
        size.y = resources.getDimension(17105167) / density;
        return size;
    }
}
