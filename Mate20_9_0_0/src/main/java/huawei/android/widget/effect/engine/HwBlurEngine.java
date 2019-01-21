package huawei.android.widget.effect.engine;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import com.huawei.iimagekit.blur.BlurAlgorithm;
import com.huawei.iimagekit.blur.util.SystemUtil;
import huawei.android.widget.loader.ResLoader;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class HwBlurEngine {
    private static final String ADVANCED_VISUAL_EFFECT_KEY = "advanced_visual_effect";
    private static final int ADVANCED_VISUAL_EFFECT_OFF = 0;
    private static final int ADVANCED_VISUAL_EFFECT_ON = 1;
    private static final int BLUR_RADIUS = 120;
    private static final int COUNT = 4;
    private static final int DOWN_FACTOR = 15;
    private static final int HANDLER_MASSAGE_BLUR = 1;
    private static final String HANDLER_MASSAGE_KEY = "blur";
    private static final int HANDLER_MASSAGE_STOP = 2;
    private static final boolean IS_BLUR_EFFECT_ENABLED = SystemUtil.getSystemProperty("ro.config.hw_blur_effect", true);
    private static final boolean IS_EMUI_LITE = SystemUtil.getSystemProperty("ro.build.hw_emui_lite.enable", false);
    private static final int MAX_FRAME_INTERVAL = 16666666;
    private static final int MIN_DIMEN = 1;
    private static final String TAG = HwBlurEngine.class.getSimpleName();
    private static HwBlurEngine mBlurEngine = new HwBlurEngine();
    private static BlurAlgorithm mStaticAlgorithm = new BlurAlgorithm(null, 6, true);
    private long lastFrameTime = System.nanoTime();
    private BlurAlgorithm mAlgorithm;
    private Bitmap mBalanceBitmap;
    private Canvas mBalanceCanvas;
    private Bitmap mBitmapForBlur;
    private Bitmap mBitmapForDraw;
    private Canvas mBlurCanvas;
    private Rect mBlurUnionRect = new Rect();
    private Bitmap mBlurredBitmap;
    private View mDecorView;
    private Rect mDecorViewRect = new Rect();
    private boolean mIsBitmapCopying = false;
    private boolean mIsBlurEffectEnabled = true;
    private boolean mIsDecorViewChanged = false;
    private boolean mIsDrawingViewSelf = false;
    private boolean mIsLastFrameTimeout = false;
    private boolean mIsSettingFetched = false;
    private boolean mIsSettingOpened = false;
    private boolean mIsSkipDrawFrame = false;
    private boolean mIsThemeAttrFetched = false;
    private boolean mIsThemeSupported = false;
    private boolean mIsUnionAreaChanged = true;
    private OnGlobalLayoutListener mLayoutListener = new -$$Lambda$HwBlurEngine$mT50yEF0clUy9ydDQRLag1Qem98(this);
    private int mMethod = 10;
    private OnPreDrawListener mOnPreDrawListener = new -$$Lambda$HwBlurEngine$x-UbPOu5PkLUPGbsHOyr-UY88qw(this);
    private SubThread mSubThread;
    private SubThreadHandler mSubThreadHandler;
    private ConcurrentHashMap<View, HwBlurEntity> mTargetViews = new ConcurrentHashMap();

    public enum BlurType {
        Blur,
        DarkBlur,
        LightBlur,
        LightBlurWithGray
    }

    private class SubThread extends Thread {
        private Looper mLooper;

        SubThread() {
            start();
        }

        public void run() {
            super.run();
            Looper.prepare();
            this.mLooper = Looper.myLooper();
            HwBlurEngine.this.mSubThreadHandler = new SubThreadHandler();
            Looper.loop();
        }

        void quit() {
            if (this.mLooper != null) {
                this.mLooper.quitSafely();
                this.mLooper = null;
            }
        }
    }

    private static class SubThreadHandler extends Handler {
        private boolean destroyed;

        private SubThreadHandler() {
            this.destroyed = false;
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int value = msg.getData().getInt(HwBlurEngine.HANDLER_MASSAGE_KEY);
            if (value == 1) {
                HwBlurEngine.getInstance().prepareBlurredBitmap();
            } else if (value == 2) {
                HwBlurEngine.getInstance().destroyResources();
                this.destroyed = false;
            }
        }

        private void sendMessage(int value) {
            Message message = obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt(HwBlurEngine.HANDLER_MASSAGE_KEY, value);
            message.setData(bundle);
            sendMessage(message);
        }

        void execute(int messageType) {
            if (this.destroyed) {
                Log.d(HwBlurEngine.TAG, "SubThreadHandler execute: destroyed");
                return;
            }
            if (messageType == 1) {
                removeCallbacksAndMessages(null);
                sendMessage(1);
            } else if (messageType == 2) {
                this.destroyed = true;
                sendMessage(2);
            }
        }
    }

    public static HwBlurEngine getInstance() {
        return mBlurEngine;
    }

    private HwBlurEngine() {
        Log.d(TAG, "create blur engine instance: version 1.5");
    }

    public void addBlurTargetView(View targetView, BlurType blurType) {
        if (targetView == null) {
            Log.e(TAG, "addBlurTargetView: targetView can not be null");
        } else if (blurType == null) {
            Log.e(TAG, "addBlurTargetView: blurType can not be null");
        } else if (!isThemeSupportedAndSettingOpened(targetView)) {
        } else {
            if (!isShowHwBlur()) {
                Log.w(TAG, "addBlurTargetView: isShowHwBlur = false");
            } else if (this.mTargetViews.containsKey(targetView)) {
                Log.w(TAG, "addBlurTargetView: target view is existed in blur engine.");
            } else {
                if (this.mAlgorithm == null) {
                    this.mAlgorithm = new BlurAlgorithm(targetView.getContext(), this.mMethod);
                }
                this.mTargetViews.put(targetView, new HwBlurEntity(targetView, blurType));
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addBlurTargetView: [");
                stringBuilder.append(this.mTargetViews.size());
                stringBuilder.append("], view: ");
                stringBuilder.append(targetView);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public void removeBlurTargetView(View targetView) {
        if (targetView == null) {
            Log.e(TAG, "removeTargetView: targetView can not be null");
            return;
        }
        if (this.mTargetViews.size() == 0) {
            this.mIsThemeAttrFetched = false;
            this.mIsSettingFetched = false;
        }
        if (this.mTargetViews.containsKey(targetView)) {
            this.mTargetViews.remove(targetView);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeTargetView: [");
            stringBuilder.append(this.mTargetViews.size());
            stringBuilder.append("], view = ");
            stringBuilder.append(targetView);
            Log.d(str, stringBuilder.toString());
            if (this.mTargetViews.size() == 0) {
                this.mTargetViews = new ConcurrentHashMap();
                destroyDecorView();
            }
            return;
        }
        Log.w(TAG, "removeTargetView: target view do not contained in blur engine");
    }

    public boolean isDrawingViewSelf() {
        return this.mIsDrawingViewSelf;
    }

    public void setBlurEnable(boolean enabled) {
        this.mIsBlurEffectEnabled = enabled;
    }

    public boolean isBlurEnable() {
        return this.mIsBlurEffectEnabled;
    }

    public boolean isShowHwBlur() {
        return isBlurEnable() && isDeviceSupport() && isConfigurationOpen();
    }

    public boolean isShowHwBlur(View targetView) {
        boolean z = false;
        if (targetView == null) {
            Log.e(TAG, "isShowHwBlur: targetView can not be null");
            return false;
        } else if (!isThemeSupportedAndSettingOpened(targetView) || !this.mTargetViews.containsKey(targetView)) {
            return false;
        } else {
            if (isShowHwBlur() && ((HwBlurEntity) this.mTargetViews.get(targetView)).isEnabled()) {
                z = true;
            }
            return z;
        }
    }

    public void setTargetViewBlurEnable(View targetView, boolean isBlurEnable) {
        if (targetView == null) {
            Log.e(TAG, "setBlurEnable: targetView can not be null");
            return;
        }
        if (this.mTargetViews.containsKey(targetView)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBlurEnable: [");
            stringBuilder.append(isBlurEnable);
            stringBuilder.append("], view = ");
            stringBuilder.append(targetView);
            Log.d(str, stringBuilder.toString());
            ((HwBlurEntity) this.mTargetViews.get(targetView)).setBlurEnable(isBlurEnable);
            if (isBlurEnable) {
                updateDecorView(targetView);
            }
            updateUnionArea();
        } else {
            Log.w(TAG, "setBlurEnable: target view is not contained in blur engine.");
        }
    }

    public void setTargetViewCornerRadius(View targetView, int cornerRadius) {
        if (targetView == null) {
            Log.e(TAG, "setCornerRadius: targetView can not be null");
        } else if (cornerRadius < 0) {
            Log.e(TAG, "setCornerRadius: corner radius must >= 0");
        } else {
            if (this.mTargetViews.containsKey(targetView)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setCornerRadius: view = ");
                stringBuilder.append(targetView);
                stringBuilder.append(", radius = ");
                stringBuilder.append(cornerRadius);
                Log.d(str, stringBuilder.toString());
                ((HwBlurEntity) this.mTargetViews.get(targetView)).setCornerRadius(cornerRadius);
                targetView.invalidate();
            } else {
                Log.w(TAG, "setCornerRadius: target view is not contained in blur engine.");
            }
        }
    }

    public void setTargetViewOverlayColor(View targetView, int overlayColor) {
        if (targetView == null) {
            Log.e(TAG, "setOverlayColor: targetView can not be null");
            return;
        }
        if (this.mTargetViews.containsKey(targetView)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setOverlayColor: view = ");
            stringBuilder.append(targetView);
            stringBuilder.append(", color = ");
            stringBuilder.append(overlayColor);
            Log.d(str, stringBuilder.toString());
            ((HwBlurEntity) this.mTargetViews.get(targetView)).setOverlayColor(overlayColor);
            targetView.invalidate();
        } else {
            Log.w(TAG, "setOverlayColor: target view is not contained in blur engine.");
        }
    }

    public void onWindowVisibilityChanged(View targetView, boolean isViewVisible) {
        if (!isViewVisible) {
            removeBlurTargetView(targetView);
        }
    }

    public void draw(Canvas canvas, View targetView) {
        if (canvas == null || targetView == null || !targetView.isShown()) {
            Log.e(TAG, "draw: canvas is null or target view is null or view is not shown");
        } else if (!isShowHwBlur()) {
            Log.w(TAG, "draw: isShowHwBlur = false");
        } else if (this.mIsDrawingViewSelf) {
            throw new RuntimeException("");
        } else {
            if (this.mTargetViews.containsKey(targetView) && ((HwBlurEntity) this.mTargetViews.get(targetView)).isEnabled() && this.mBitmapForDraw != null) {
                ((HwBlurEntity) this.mTargetViews.get(targetView)).drawBlurredBitmap(canvas, this.mBitmapForDraw, this.mBlurUnionRect, 15);
            }
        }
    }

    private static boolean isDeviceSupport() {
        return IS_EMUI_LITE ^ 1;
    }

    private static boolean isConfigurationOpen() {
        return IS_BLUR_EFFECT_ENABLED;
    }

    private boolean isThemeSupportedAndSettingOpened(View targetView) {
        if (!this.mIsThemeAttrFetched) {
            this.mIsThemeSupported = isThemeSupport(targetView);
        }
        if (!this.mIsThemeSupported) {
            return false;
        }
        if (!this.mIsSettingFetched) {
            this.mIsSettingOpened = isSettingOpened(targetView);
        }
        if (this.mIsSettingOpened) {
            return true;
        }
        return false;
    }

    private boolean isThemeSupport(View targetView) {
        if (targetView.getContext() == null) {
            Log.e(TAG, "isThemeSupport: context of targetView is null.");
            return false;
        }
        TypedArray typedArray = targetView.getContext().obtainStyledAttributes(new int[]{ResLoader.getInstance().getIdentifier(targetView.getContext(), "attr", "hwBlurEffectEnable")});
        boolean hwBlurEffectEnable = typedArray.getBoolean(0, false);
        typedArray.recycle();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isThemeSupport: fetec theme property success, ");
        stringBuilder.append(hwBlurEffectEnable);
        Log.d(str, stringBuilder.toString());
        this.mIsThemeAttrFetched = true;
        return hwBlurEffectEnable;
    }

    private boolean isSettingOpened(View targetView) {
        boolean z = false;
        if (targetView.getContext() == null || targetView.getContext().getContentResolver() == null) {
            Log.e(TAG, "isSettingOpen: context of targetView is null.");
            return false;
        }
        int result = System.getInt(targetView.getContext().getContentResolver(), ADVANCED_VISUAL_EFFECT_KEY, 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSettingOpened: fetch setting data success, ");
        stringBuilder.append(result);
        Log.d(str, stringBuilder.toString());
        this.mIsSettingFetched = true;
        if (result == 1) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:26:0x0066, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean onPreDraw() {
        boolean z = isDrawFrameTimeout() && !this.mIsUnionAreaChanged;
        this.mIsSkipDrawFrame = z;
        if (this.mBlurUnionRect.width() == 0 || this.mBlurUnionRect.height() == 0 || !isShowHwBlur() || this.mDecorView == null) {
            return true;
        }
        if (this.mIsUnionAreaChanged) {
            getBitmapForBlur();
            prepareBlurredBitmap();
            this.mIsUnionAreaChanged = false;
        } else {
            this.mDecorView.post(new -$$Lambda$HwBlurEngine$4jYJ818cTpPpo1oCogbANZGF_dc(this));
        }
        this.mIsBitmapCopying = true;
        if (!(this.mBlurredBitmap == null || this.mBitmapForDraw == null)) {
            this.mBitmapForDraw = this.mBlurredBitmap.copy(this.mBlurredBitmap.getConfig(), this.mBlurredBitmap.isMutable());
        }
        this.mIsBitmapCopying = false;
        return true;
    }

    public static /* synthetic */ void lambda$onPreDraw$0(HwBlurEngine hwBlurEngine) {
        hwBlurEngine.getBitmapForBlur();
        if (hwBlurEngine.mSubThreadHandler != null) {
            hwBlurEngine.mSubThreadHandler.execute(1);
        }
    }

    private boolean isDrawFrameTimeout() {
        long currentTime = System.nanoTime();
        long frameInterval = currentTime - this.lastFrameTime;
        this.lastFrameTime = currentTime;
        boolean z = frameInterval > 16666666 && !this.mIsLastFrameTimeout;
        this.mIsLastFrameTimeout = z;
        return this.mIsLastFrameTimeout;
    }

    private void prepareBlurredBitmap() {
        if (this.mIsSkipDrawFrame) {
            invalidateTargetViewsInSubThread();
            return;
        }
        if (!(this.mBalanceCanvas == null || this.mBitmapForBlur == null)) {
            this.mBalanceCanvas.drawBitmap(this.mBitmapForBlur, 0.0f, 0.0f, null);
        }
        for (Entry<View, HwBlurEntity> entityEntry : this.mTargetViews.entrySet()) {
            HwBlurEntity blurEntity = (HwBlurEntity) entityEntry.getValue();
            if (((View) entityEntry.getKey()).isShown() && blurEntity.isEnabled() && this.mBalanceCanvas != null && this.mBitmapForBlur != null) {
                blurEntity.drawBitmapForBlur(this.mBalanceCanvas, this.mBitmapForBlur, this.mBlurUnionRect, 15);
            }
        }
        if (!this.mIsBitmapCopying) {
            int result = this.mAlgorithm.blur(this.mBalanceBitmap, this.mBlurredBitmap, 8);
            if (result != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" mAlgorithm.blur occurred some error, error code= ");
                stringBuilder.append(result);
                Log.w(str, stringBuilder.toString());
            }
            if (this.mBlurredBitmap.sameAs(this.mBitmapForDraw)) {
                return;
            }
        }
        if (this.mIsDecorViewChanged) {
            this.mIsDecorViewChanged = false;
        }
        invalidateTargetViewsInSubThread();
    }

    private void invalidateTargetViewsInSubThread() {
        if (!Looper.getMainLooper().isCurrentThread()) {
            for (Entry<View, HwBlurEntity> entityEntry : this.mTargetViews.entrySet()) {
                if (((View) entityEntry.getKey()).isShown() && ((HwBlurEntity) entityEntry.getValue()).isEnabled()) {
                    ((View) entityEntry.getKey()).postInvalidate();
                }
            }
        }
    }

    private void getBitmapForBlur() {
        if (!this.mIsSkipDrawFrame && this.mDecorView != null) {
            if (this.mBlurCanvas != null || this.mTargetViews.size() != 0) {
                this.mDecorView.getGlobalVisibleRect(this.mDecorViewRect);
                if (this.mBlurCanvas == null) {
                    initBitmapAndCanvas();
                }
                int rc = this.mBlurCanvas.save();
                this.mBlurCanvas.scale(0.06666667f, 0.06666667f);
                this.mBlurCanvas.translate((float) (this.mDecorViewRect.left - this.mBlurUnionRect.left), (float) (this.mDecorViewRect.top - this.mBlurUnionRect.top));
                this.mIsDrawingViewSelf = true;
                try {
                    this.mDecorView.draw(this.mBlurCanvas);
                } catch (Exception e) {
                }
                this.mIsDrawingViewSelf = false;
                this.mBlurCanvas.restoreToCount(rc);
            }
        }
    }

    private void updateDecorView(View targetView) {
        Context mContext = targetView.getContext();
        for (int i = 0; i < 4 && !(mContext instanceof Activity) && (mContext instanceof ContextWrapper); i++) {
            mContext = ((ContextWrapper) mContext).getBaseContext();
        }
        if (mContext instanceof Activity) {
            View decorView = ((Activity) mContext).getWindow().getDecorView();
            if (decorView != this.mDecorView) {
                String str;
                StringBuilder stringBuilder;
                if (this.mSubThread == null) {
                    this.mSubThread = new SubThread();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateDecorView: create new sub-thread ");
                    stringBuilder.append(this.mSubThread.getId());
                    Log.d(str, stringBuilder.toString());
                }
                if (this.mDecorView != null) {
                    this.mDecorView.getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
                    this.mDecorView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mLayoutListener);
                }
                this.mDecorView = decorView;
                this.mDecorView.getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
                this.mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(this.mLayoutListener);
                this.mIsDecorViewChanged = true;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateDecorView: ");
                stringBuilder.append(this.mDecorView);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private void destroyResources() {
        if (this.mBitmapForBlur != null) {
            this.mBlurCanvas = null;
            this.mBitmapForBlur.recycle();
            this.mBitmapForBlur = null;
            Log.d(TAG, "destroyDecorView: destroyed mBlurCanvas and mBitmapForBlur");
        }
        if (this.mBalanceBitmap != null) {
            this.mBalanceCanvas = null;
            this.mBalanceBitmap.recycle();
            this.mBalanceBitmap = null;
            Log.d(TAG, "destroyDecorView: destroyed mBalanceCanvas and mBalanceBitmap");
        }
        if (this.mBlurredBitmap != null) {
            this.mBlurredBitmap.recycle();
            this.mBlurredBitmap = null;
            Log.d(TAG, "destroyDecorView: destroyed mBlurredBitmap");
        }
        if (this.mBitmapForDraw != null) {
            this.mBitmapForDraw.recycle();
            this.mBitmapForDraw = null;
            Log.d(TAG, "destroyDecorView: destroyed mBitmapForDraw");
        }
        if (this.mSubThread != null) {
            this.mSubThread.quit();
            this.mSubThread = null;
            Log.d(TAG, "destroyResources: sub-thread Looper quited safely");
        }
    }

    private void destroyDecorView() {
        if (this.mDecorView != null) {
            this.mDecorView.getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
            this.mDecorView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mLayoutListener);
            this.mDecorView = null;
            Log.d(TAG, "destroyDecorView: removed listeners and destroy decor view");
        }
        if (this.mSubThreadHandler != null) {
            this.mSubThreadHandler.execute(2);
            Log.d(TAG, "destroyDecorView: send message to destroy resources");
        }
    }

    private void updateUnionArea() {
        this.mBlurUnionRect.setEmpty();
        for (Entry<View, HwBlurEntity> entityEntry : this.mTargetViews.entrySet()) {
            View targetView = (View) entityEntry.getKey();
            HwBlurEntity blurEntity = (HwBlurEntity) entityEntry.getValue();
            if (targetView.isShown() && blurEntity.isEnabled()) {
                Rect targetViewRect = new Rect();
                targetView.getGlobalVisibleRect(targetViewRect);
                blurEntity.setTargetViewRect(targetViewRect);
                this.mBlurUnionRect.union(targetViewRect);
            }
            targetView.invalidate();
        }
        initBitmapAndCanvas();
    }

    private void initBitmapAndCanvas() {
        int targetWidth = this.mBlurUnionRect.width() / 15;
        int targetHeight = this.mBlurUnionRect.height() / 15;
        int scaledW = 1 >= targetWidth ? 1 : targetWidth;
        int scaledH = 1 >= targetHeight ? 1 : targetHeight;
        if (this.mBitmapForBlur == null || this.mBlurredBitmap == null || this.mBlurCanvas == null || this.mBalanceBitmap == null || this.mBalanceCanvas == null || this.mBitmapForDraw == null || this.mBitmapForBlur.getWidth() != scaledW || this.mBitmapForBlur.getHeight() != scaledH) {
            this.mBitmapForBlur = Bitmap.createBitmap(scaledW, scaledH, Config.ARGB_4444);
            this.mBalanceBitmap = Bitmap.createBitmap(scaledW, scaledH, Config.ARGB_4444);
            this.mBlurredBitmap = Bitmap.createBitmap(scaledW, scaledH, Config.ARGB_4444);
            this.mBitmapForDraw = Bitmap.createBitmap(scaledW, scaledH, Config.ARGB_4444);
            this.mBlurCanvas = new Canvas(this.mBitmapForBlur);
            this.mBalanceCanvas = new Canvas(this.mBalanceBitmap);
            this.mIsUnionAreaChanged = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initBitmapAndCanvas: bitmap initialed. [");
            stringBuilder.append(scaledW);
            stringBuilder.append(", ");
            stringBuilder.append(scaledH);
            stringBuilder.append("]");
            Log.d(str, stringBuilder.toString());
        }
    }

    public static Bitmap blur(Bitmap input, int radius, int downscale) {
        if (input == null || input.getWidth() == 0 || input.getHeight() == 0) {
            Log.e(TAG, "blur: input bitmap is null or bitmap size is 0");
            return null;
        } else if (radius <= 0 || downscale <= 0) {
            Log.e(TAG, "blur: blur radius and downscale must > 0");
            return null;
        } else {
            int blurRadius = (int) (((float) radius) / ((float) downscale));
            if (blurRadius != 0) {
                return getBlurredBitmap(getBitmapForBlur(input, downscale), blurRadius);
            }
            Log.e(TAG, "blur: blur radius downscale must < radius");
            return null;
        }
    }

    public static Bitmap blur(View view, int radius, int downscale) {
        if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
            Log.e(TAG, "blur: input view is null or view size is 0");
            return null;
        } else if (radius <= 0 || downscale <= 0) {
            Log.e(TAG, "blur: blur radius and downscale must > 0");
            return null;
        } else {
            int blurRadius = (int) (((float) radius) / ((float) downscale));
            if (blurRadius != 0) {
                return getBlurredBitmap(getBitmapForBlur(view, downscale), blurRadius);
            }
            Log.e(TAG, "blur: blur radius downscale must < radius");
            return null;
        }
    }

    private static Bitmap getBitmapForBlur(View view, int downscale) {
        Bitmap bitmapForBlur = Bitmap.createBitmap(Math.max(1, view.getWidth() / downscale), Math.max(1, view.getHeight() / downscale), Config.ARGB_4444);
        Canvas blurCanvas = new Canvas(bitmapForBlur);
        blurCanvas.scale(1.0f / ((float) downscale), 1.0f / ((float) downscale));
        view.draw(blurCanvas);
        return bitmapForBlur;
    }

    private static Bitmap getBitmapForBlur(Bitmap input, int downscale) {
        int scaledW = Math.max(1, input.getWidth() / downscale);
        int scaledH = Math.max(1, input.getHeight() / downscale);
        Bitmap bitmapForBlur = Bitmap.createBitmap(scaledW, scaledH, Config.ARGB_4444);
        Canvas blurCanvas = new Canvas(bitmapForBlur);
        RectF rectF = new RectF(0.0f, 0.0f, (float) scaledW, (float) scaledH);
        Paint paint = new Paint();
        paint.setShader(getBitmapScaleShader(input, bitmapForBlur));
        blurCanvas.drawRect(rectF, paint);
        return bitmapForBlur;
    }

    private static BitmapShader getBitmapScaleShader(Bitmap in, Bitmap out) {
        float scaleRateX = ((float) out.getWidth()) / ((float) in.getWidth());
        float scaleRateY = ((float) out.getHeight()) / ((float) in.getHeight());
        Matrix matrix = new Matrix();
        matrix.postScale(scaleRateX, scaleRateY);
        TileMode tileMode = TileMode.CLAMP;
        BitmapShader shader = new BitmapShader(in, tileMode, tileMode);
        shader.setLocalMatrix(matrix);
        return shader;
    }

    private static Bitmap getBlurredBitmap(Bitmap bitmapForBlur, int radius) {
        Bitmap blurredBitmap = Bitmap.createBitmap(bitmapForBlur.getWidth(), bitmapForBlur.getHeight(), Config.ARGB_4444);
        if (mStaticAlgorithm != null) {
            mStaticAlgorithm.blur(bitmapForBlur, blurredBitmap, radius);
        }
        return blurredBitmap;
    }

    @Deprecated
    public static HwBlurEngine getInstance(View targetView, BlurType blurType) {
        Log.w(TAG, "getInstance: function deprecated!!!");
        mBlurEngine.addBlurTargetView(targetView, blurType);
        return mBlurEngine;
    }

    @Deprecated
    public void onWindowVisibilityChanged(View targetView, boolean isViewVisible, boolean isBlurEnabled) {
        onWindowVisibilityChanged(targetView, isViewVisible);
        setTargetViewBlurEnable(targetView, isBlurEnabled);
    }

    @Deprecated
    public HwBlurEngine(View targetView, BlurType blurType) {
        Log.e(TAG, "This constructor is deprecated and will remove later.");
    }

    @Deprecated
    public static boolean isEnable() {
        Log.e(TAG, "isEnable() is deprecated and will remove later.");
        return false;
    }

    @Deprecated
    public static void setGlobalEnable(boolean isEnable) {
        Log.e(TAG, "setGlobalEnable() is deprecated and will remove later.");
    }

    @Deprecated
    public void setEnable(boolean isEnable) {
        Log.e(TAG, "setEnable() is deprecated and will remove later.");
        boolean z = isDeviceSupport() && isEnable;
        this.mIsBlurEffectEnabled = z;
    }

    @Deprecated
    public void onAttachedToWindow() {
        Log.e(TAG, "onAttachedToWindow() will remove later and call this method will take no effect");
    }

    @Deprecated
    public void onDetachedFromWindow() {
        Log.e(TAG, "onDetachedFromWindow() will remove later and call this method will take no effect");
    }

    @Deprecated
    public void draw(Canvas canvas) {
        Log.e(TAG, "draw(Canvas canvas) is deprecated pls use draw(Canvas, View) instead.");
    }

    @Deprecated
    public void onWindowFocusChanged(View targetView, boolean hasWindowFocus, boolean isTargetViewEnableBlur) {
        onWindowVisibilityChanged(targetView, hasWindowFocus, isTargetViewEnableBlur);
    }

    @Deprecated
    public void onWindowFocusChanged(View targetView, boolean hasWindowFocus) {
        onWindowFocusChanged(targetView, hasWindowFocus, true);
    }
}
