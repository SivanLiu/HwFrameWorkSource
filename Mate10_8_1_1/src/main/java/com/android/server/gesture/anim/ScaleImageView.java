package com.android.server.gesture.anim;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.server.gesture.GestureNavConst;
import com.android.server.wifipro.WifiProCHRManager;
import com.android.server.wm.PhaseInterpolator;
import com.android.server.wm.WindowManagerService;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScaleImageView extends ImageView {
    public static final int ANIM_TYPE_HOME = 2;
    public static final int ANIM_TYPE_RECOVER = 1;
    public static final int ANIM_TYPE_TRANSLATE = 0;
    private static final float DY_COMPENSATION_FACTOR = 0.125f;
    private static final int ERROR_ID = -1;
    private static final float FOLLOW_TRANSLATE_RATIO = 0.1f;
    private static final long HANDLE_TOUCH_WAIT_TIME = 15;
    private static final long HOME_FADE_OUT_ALPHA_DURATION = 280;
    private static final long HOME_FADE_OUT_SCALE_DURATION = 280;
    private static final long HOME_TO_ICON_DURATION = 350;
    private static final boolean IS_NOTCH_PROP = ("".equals(SystemProperties.get("ro.config.hw_notch_size", "")) ^ 1);
    private static final float LAZY_MODE_SCALE = 0.75f;
    private static final String LEFT = "left";
    private static final float MAX_POINT_DISTANCE = 100.0f;
    private static final float MAX_SCALE_DISTANCE = 500.0f;
    private static final float MAX_SCALE_RATIO = 0.7f;
    private static final int NAVIGATIONBAR_LAYER = 231000;
    private static final int PICTURE_ROUND = 4;
    private static final long RECOVER_ANIMATION_DURATION = 100;
    private static final String RIGHT = "right";
    private static final float SCALE_X_PIVOT = 0.5f;
    private static final float SCALE_Y_PIVOT = 0.625f;
    private static final int STATE_LEFT = 1;
    private static final int STATE_MIDDLE = 0;
    private static final int STATE_RIGHT = 2;
    private static final String TAG = "GestureScale";
    private static final long TRANSLATE_ALPHA_DURATION = 75;
    private static final long TRANSLATE_ANIMATION_DURATION = 150;
    private static final int TYPE_LANDSCAPE = 1;
    private static final int TYPE_PORTRAIT = 0;
    private TimeInterpolator[] mAlphaInterpolators;
    private ActivityManager mAm;
    private TranslateAnimationListener mAnimationListener;
    private TimeInterpolator mConstantInterpolator;
    private Context mContext;
    private boolean mControlVisibleByProxy;
    private float mHeight;
    private TimeInterpolator[] mIconAlphaInterpolators;
    private int mScreenOrientation;
    private TimeInterpolator[] mSizeBigInterpolators;
    private TimeInterpolator[] mSizeSmallInterpolators;
    private TouchHandler mTouchHandler;
    private float mWidth;
    private WindowManagerService mWms;

    public interface TranslateAnimationListener {
        void onAnimationEnd(int i, boolean z);

        void onAnimationStart(int i, boolean z);
    }

    private static class TouchHandler extends Handler {
        private static final int MSG_HANDLE_NEXT = 1;
        private TouchPoint mLastHandlePoint = new TouchPoint(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        private ConcurrentLinkedQueue<TouchPoint> mTouchQueue = new ConcurrentLinkedQueue();
        private WeakReference<ScaleImageView> mViewRef;

        TouchHandler(ScaleImageView context) {
            this.mViewRef = new WeakReference(context);
        }

        public void handleMessage(Message msg) {
            ScaleImageView view = (ScaleImageView) this.mViewRef.get();
            if (view == null) {
                Log.e(ScaleImageView.TAG, "context is null error.");
                return;
            }
            if (msg.what == 1) {
                if (this.mTouchQueue.isEmpty()) {
                    Log.i(ScaleImageView.TAG, "no touch point");
                    return;
                }
                TouchPoint tp = (TouchPoint) this.mTouchQueue.poll();
                view.handleTouchPoint(tp);
                this.mLastHandlePoint.x = tp.x;
                this.mLastHandlePoint.y = tp.y;
                sendEmptyMessageDelayed(1, ScaleImageView.HANDLE_TOUCH_WAIT_TIME);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void putTouchPoint(TouchPoint p) {
            removeMessages(1);
            this.mTouchQueue.clear();
            TouchPoint np = new TouchPoint(this.mLastHandlePoint.x, this.mLastHandlePoint.y);
            while (true) {
                if (np.x == p.x && np.y == p.y) {
                    break;
                }
                double d = Math.sqrt((double) (((p.x - np.x) * (p.x - np.x)) + ((p.y - np.y) * (p.y - np.y))));
                if (d <= 100.0d) {
                    break;
                }
                double ratio = 100.0d / d;
                np.x = (float) (((double) np.x) + (((double) (p.x - np.x)) * ratio));
                np.y = (float) (((double) np.y) + (((double) (p.y - np.y)) * ratio));
                this.mTouchQueue.add(np);
                np = new TouchPoint(np.x, np.y);
            }
            this.mTouchQueue.add(np);
            sendEmptyMessage(1);
        }

        void stopHandler() {
            removeMessages(1);
            this.mLastHandlePoint = new TouchPoint(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        }
    }

    private static class TouchPoint {
        float x;
        float y;

        TouchPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public ScaleImageView(Context context) {
        super(context);
        this.mTouchHandler = new TouchHandler(this);
        this.mConstantInterpolator = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return 1.0f;
            }
        };
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        this.mIconAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f)};
        init(context);
    }

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTouchHandler = new TouchHandler(this);
        this.mConstantInterpolator = /* anonymous class already generated */;
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        this.mIconAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f)};
        init(context);
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mTouchHandler = new TouchHandler(this);
        this.mConstantInterpolator = /* anonymous class already generated */;
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        this.mIconAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f)};
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "init");
        this.mContext = context;
        this.mAm = (ActivityManager) context.getSystemService(ActivityManager.class);
        initWms();
    }

    private void initWms() {
        try {
            Class wmsClazz = Class.forName("com.android.server.wm.WindowManagerService");
            if (wmsClazz == null) {
                Log.w(TAG, "wmsClazz is null, just return!");
                return;
            }
            try {
                Method getInstanceMethod = wmsClazz.getDeclaredMethod(WifiProCHRManager.LOG_GET_INSTANCE_API_NAME, new Class[0]);
                if (getInstanceMethod == null) {
                    Log.w(TAG, "getInstanceMethod is null, just return!");
                    return;
                }
                getInstanceMethod.setAccessible(true);
                try {
                    Object wmsObj = getInstanceMethod.invoke(null, new Object[0]);
                    if (wmsObj == null) {
                        Log.w(TAG, "wmsObj is null, just return!");
                    } else {
                        this.mWms = (WindowManagerService) wmsObj;
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "access getInstanceMethod failed!");
                } catch (InvocationTargetException e2) {
                    Log.e(TAG, "invoke getInstanceMethod failed!");
                }
            } catch (NoSuchMethodException e3) {
                Log.e(TAG, "get getInstanceMethod failed!");
            }
        } catch (ClassNotFoundException e4) {
            Log.e(TAG, "get wmsClazz failed!");
        }
    }

    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        this.mWidth = (float) w;
        this.mHeight = (float) h;
        Log.d(TAG, "view width is " + this.mWidth + ", height is " + this.mHeight);
        float pivotX = this.mWidth * 0.5f;
        float pivotY = this.mHeight * SCALE_Y_PIVOT;
        setPivotX(pivotX);
        setPivotY(pivotY);
        Log.d(TAG, "view pivotX is " + pivotX + ", pivotY is " + pivotY);
    }

    private static int dp2px(Context context, int dp) {
        if (context != null && dp > 0) {
            return (int) TypedValue.applyDimension(1, (float) dp, context.getResources().getDisplayMetrics());
        }
        Log.e(TAG, "dp2px parameters error.");
        return 0;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == 1) {
            this.mScreenOrientation = 0;
        } else {
            this.mScreenOrientation = 1;
        }
        Log.d(TAG, "mScreenOrientation " + this.mScreenOrientation);
    }

    private void handleTouchPoint(TouchPoint tp) {
        if (tp != null) {
            doScale(-tp.y);
            doTranslate(tp.x, -tp.y, 0.1f);
        }
    }

    private void doScale(float y) {
        float scale;
        if (y >= MAX_SCALE_DISTANCE) {
            scale = 0.7f;
        } else {
            scale = 1.0f - ((y / MAX_SCALE_DISTANCE) * 0.3f);
        }
        setScaleX(scale);
        setScaleY(scale);
    }

    public void setFollowPosition(float x, float y) {
        this.mTouchHandler.putTouchPoint(new TouchPoint(x, y));
    }

    private void doTranslate(float x, float y, float transRatio) {
        float moveX = x * transRatio;
        float moveY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (y > MAX_SCALE_DISTANCE) {
            moveY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO - ((y - MAX_SCALE_DISTANCE) * transRatio);
        } else if (y < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            moveY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO + ((-y) * transRatio);
        }
        setX(moveX);
        setY(moveY);
    }

    public void refreshContent() {
        Log.d(TAG, "refreshContent");
        long oldT = System.currentTimeMillis();
        refreshBySurfaceControl();
        setVisibility(0);
        Log.d(TAG, "refresh content cost time " + (System.currentTimeMillis() - oldT));
    }

    private static Bitmap addRoundOnBitmap(Bitmap source, float round) {
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight());
        RectF rectf = new RectF(rect);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectf, round, round, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);
        return bitmap;
    }

    private void refreshBySurfaceControl() {
        Log.d(TAG, "refreshBySurfaceControl");
        Bitmap bitmap = screenshot(this.mContext);
        if (bitmap == null) {
            setBackgroundColor(-7829368);
        } else {
            setImageBitmap(bitmap);
        }
    }

    private Bitmap screenshot(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService("window");
        if (wm == null) {
            Log.e(TAG, "screenshot error WindowManager is null");
            return null;
        }
        Bitmap bitmap;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        DisplayMetrics displayMetricsBody = new DisplayMetrics();
        Display display = wm.getDefaultDisplay();
        display.getMetrics(displayMetricsBody);
        display.getRealMetrics(displayMetrics);
        int rotation = display.getRotation();
        Rect screenRect = getScreenshotRect(ctx, rotation, displayMetrics, displayMetricsBody);
        if (rotation == 0 || 2 == rotation) {
            Log.d(TAG, "SurfaceControl.screenshot_ext_hw with rotation " + rotation);
            bitmap = SurfaceControl.screenshot_ext_hw(screenRect, screenRect.width(), screenRect.height(), 0, NAVIGATIONBAR_LAYER, false, converseRotation(rotation));
        } else {
            bitmap = rotationScreenBitmap(screenRect, rotation, screenRect.height(), screenRect.width(), 0, NAVIGATIONBAR_LAYER);
        }
        if (bitmap == null) {
            Log.e(TAG, "screenshot error bitmap is null");
            return null;
        }
        Log.i(TAG, "screenshot bitmap info: width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        bitmap.prepareToDraw();
        return bitmap;
    }

    private static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private static int getCurrentStatusBarHeight(Context context) {
        return getStatusBarHeight(context);
    }

    private static int getBlackHeight(Context context) {
        return IS_NOTCH_PROP ? getStatusBarHeight(context) : 0;
    }

    private static int converseRotation(int rotation) {
        switch (rotation) {
            case 1:
                return 3;
            case 2:
                return 2;
            case 3:
                return 1;
            default:
                return 0;
        }
    }

    private static float convertRotationToDegrees(int rotation) {
        switch (rotation) {
            case 1:
                return 270.0f;
            case 2:
                return 180.0f;
            case 3:
                return 90.0f;
            default:
                return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static Bitmap rotationScreenBitmap(Rect rect, int rotation, int width, int height, int minLayer, int maxLayer) {
        Log.d(TAG, "rotationScreenBitmap with rotation " + rotation + ", width " + width + ", height " + height + ", layer " + minLayer + "," + maxLayer);
        float degrees = convertRotationToDegrees(rotation);
        float[] dims = new float[]{(float) width, (float) height};
        Log.d(TAG, "debug rotationScreenBitmap point 1: dims[0] " + dims[0] + ", dims[1] " + dims[1]);
        Matrix metrics = new Matrix();
        metrics.reset();
        metrics.preRotate(-degrees);
        metrics.mapPoints(dims);
        dims[0] = Math.abs(dims[0]);
        dims[1] = Math.abs(dims[1]);
        Log.d(TAG, "debug rotationScreenBitmap point 2: dims[0] " + dims[0] + ", dims[1] " + dims[1]);
        Bitmap bitmap = SurfaceControl.screenshot(rect, (int) dims[0], (int) dims[1], minLayer, maxLayer, false, 0);
        if (bitmap == null) {
            Log.w(TAG, "bitmap is null when rotationScreenBitmap!");
            return null;
        }
        Bitmap ss = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas c = new Canvas(ss);
        c.translate(((float) width) * 0.5f, ((float) height) * 0.5f);
        c.rotate(degrees);
        c.translate((-dims[0]) * 0.5f, (-dims[1]) * 0.5f);
        c.drawBitmap(bitmap, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, null);
        bitmap.recycle();
        bitmap = ss;
        return ss;
    }

    private Rect getScreenshotRect(Context ctx, int rotation, DisplayMetrics dm, DisplayMetrics dmBody) {
        Rect screenshotRect;
        Log.d(TAG, "getScreenshotRect dm width " + dm.widthPixels + ", height " + dm.heightPixels);
        Log.d(TAG, "getScreenshotRect dmBody width " + dmBody.widthPixels + ", height " + dmBody.heightPixels);
        int sbHeight = getCurrentStatusBarHeight(ctx);
        if (1 == rotation || 3 == rotation) {
            screenshotRect = getLandscapeRect(ctx, rotation, dm, dmBody);
        } else if (isLazyMode(ctx)) {
            screenshotRect = getLazyModeRect(ctx, dm, dmBody);
        } else {
            screenshotRect = getPortraitRect(dmBody, sbHeight);
        }
        Log.d(TAG, "getScreenshotRect, left " + screenshotRect.left + ", top " + screenshotRect.top + ", right " + screenshotRect.right + ", bottom " + screenshotRect.bottom);
        return screenshotRect;
    }

    private Rect getPortraitRect(DisplayMetrics dmBody, int sbHeight) {
        Log.d(TAG, "getScreenshotRect rotation portrait");
        setScaleType(ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        matrix.setTranslate(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, (float) sbHeight);
        setImageMatrix(matrix);
        return new Rect(0, sbHeight, dmBody.widthPixels, dmBody.heightPixels);
    }

    private Rect getLazyModeRect(Context ctx, DisplayMetrics dm, DisplayMetrics dmBody) {
        int sbHeight = getCurrentStatusBarHeight(ctx);
        int state = getLazyState(ctx);
        setScaleType(ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        matrix.setTranslate(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, ((float) sbHeight) * 0.75f);
        matrix.setScale(1.3333334f, 1.3333334f);
        setImageMatrix(matrix);
        if (1 == state) {
            Log.d(TAG, "getScreenshotRect lazy state left");
            return new Rect(0, (int) ((((float) dm.heightPixels) * 0.25f) + (((float) sbHeight) * 0.75f)), (int) (((float) dm.widthPixels) * 0.75f), (int) (((float) dmBody.heightPixels) + (((float) (dm.heightPixels - dmBody.heightPixels)) * 0.25f)));
        }
        Log.d(TAG, "getScreenshotRect lazy state right");
        return new Rect((int) (((float) dm.widthPixels) * 0.25f), (int) ((((float) dm.heightPixels) * 0.25f) + (((float) sbHeight) * 0.75f)), dmBody.widthPixels, (int) (((float) dmBody.heightPixels) + (((float) (dm.heightPixels - dmBody.heightPixels)) * 0.25f)));
    }

    private Rect getLandscapeRect(Context ctx, int rotation, DisplayMetrics dm, DisplayMetrics dmBody) {
        Rect screenshotRect;
        setScaleType(ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        int sbHeight = getCurrentStatusBarHeight(ctx);
        int blackHeight = getBlackHeight(ctx);
        if (1 == rotation) {
            Log.d(TAG, "getScreenshotRect rotation landscape rotation 90");
            int top = blackHeight;
            screenshotRect = new Rect(0, blackHeight, dmBody.heightPixels - sbHeight, dmBody.widthPixels + blackHeight);
            matrix.setTranslate(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, (float) sbHeight);
        } else {
            Log.d(TAG, "getScreenshotRect rotation landscape rotation 270");
            int i = sbHeight;
            screenshotRect = new Rect(sbHeight, blackHeight + (dm.widthPixels - dmBody.widthPixels), dmBody.heightPixels, dm.widthPixels + blackHeight);
            matrix.setTranslate(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, (float) sbHeight);
        }
        setImageMatrix(matrix);
        return screenshotRect;
    }

    private static int getLazyState(Context context) {
        String str = Global.getString(context.getContentResolver(), "single_hand_mode");
        if (str == null || "".equals(str)) {
            return 0;
        }
        if (str.contains(LEFT)) {
            return 1;
        }
        if (str.contains(RIGHT)) {
            return 2;
        }
        return 0;
    }

    private static boolean isLazyMode(Context context) {
        return getLazyState(context) != 0;
    }

    private static Rect getScreenshotRect(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        if (windowManager == null) {
            return new Rect();
        }
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        Rect rect = null;
        int state = getLazyState(context);
        if (1 == state) {
            rect = new Rect(0, (int) (((float) displayMetrics.heightPixels) * 0.25f), (int) (((float) displayMetrics.widthPixels) * 0.75f), displayMetrics.heightPixels);
        } else if (2 == state) {
            rect = new Rect((int) (((float) displayMetrics.widthPixels) * 0.25f), (int) (((float) displayMetrics.heightPixels) * 0.25f), displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        return rect;
    }

    public void playTranslateAnimation(int leftDistance, int topDistance, int cardWidth, int cardHeight) {
        Log.d(TAG, "playTranslateAnimation with left " + leftDistance + ", top " + topDistance + ", cardWidth " + cardWidth + ", cardHeight " + cardHeight);
        this.mTouchHandler.stopHandler();
        float oldX = getX();
        float oldY = getY();
        float oldScaleX = getScaleX();
        float oldScaleY = getScaleY();
        float newScale = ((float) cardWidth) / this.mWidth;
        float newX = ((float) leftDistance) - ((this.mWidth - (this.mWidth * newScale)) / 2.0f);
        float newY = ((float) topDistance) - ((this.mHeight - (this.mHeight * newScale)) / 2.0f);
        PropertyValuesHolder xProperty = PropertyValuesHolder.ofFloat("x", new float[]{oldX, newX});
        PropertyValuesHolder yProperty = PropertyValuesHolder.ofFloat("y", new float[]{oldY, newY});
        PropertyValuesHolder xScaleProperty = PropertyValuesHolder.ofFloat("scaleX", new float[]{oldScaleX, newScale});
        PropertyValuesHolder yScaleProperty = PropertyValuesHolder.ofFloat("scaleY", new float[]{oldScaleY, newScale});
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat("alpha", new float[]{1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        ObjectAnimator transAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{xProperty, yProperty, xScaleProperty, yScaleProperty});
        transAnimator.setInterpolator(new DecelerateInterpolator());
        transAnimator.setDuration(TRANSLATE_ANIMATION_DURATION);
        final ObjectAnimator transAlpha = ObjectAnimator.ofFloat(this, "alpha", new float[]{1.0f, 0.0f});
        transAlpha.setInterpolator(new DecelerateInterpolator());
        transAlpha.setDuration(TRANSLATE_ALPHA_DURATION);
        transAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                transAlpha.start();
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationStart(0, true);
                }
            }

            public void onAnimationEnd(Animator animation) {
                Log.d(ScaleImageView.TAG, "onAnimationEnd translate");
                ScaleImageView.this.setVisibility(8);
                ScaleImageView.this.setScaleX(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setScaleY(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setX(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setY(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setAlpha(1.0f);
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationEnd(0, true);
                }
            }
        });
        transAnimator.start();
    }

    public void playRecoverAnimation() {
        Log.d(TAG, "playRecoverAnimation");
        this.mTouchHandler.stopHandler();
        float oldX = getX();
        float oldY = getY();
        float oldScaleX = getScaleX();
        float oldScaleY = getScaleY();
        PropertyValuesHolder xp = PropertyValuesHolder.ofFloat("x", new float[]{oldX, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        PropertyValuesHolder yp = PropertyValuesHolder.ofFloat("y", new float[]{oldY, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        PropertyValuesHolder scaleXp = PropertyValuesHolder.ofFloat("scaleX", new float[]{oldScaleX, 1.0f});
        PropertyValuesHolder scaleYp = PropertyValuesHolder.ofFloat("scaleY", new float[]{oldScaleY, 1.0f});
        ObjectAnimator recover = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{xp, yp, scaleXp, scaleYp});
        recover.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563661));
        recover.setDuration(RECOVER_ANIMATION_DURATION);
        recover.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationStart(1, true);
                }
            }

            public void onAnimationEnd(Animator animation) {
                Log.d(ScaleImageView.TAG, "onAnimationEnd recover");
                if (!ScaleImageView.this.mControlVisibleByProxy) {
                    ScaleImageView.this.setVisibility(8);
                }
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationEnd(1, true);
                }
            }
        });
        recover.start();
    }

    public void setControlVisibleByProxy(boolean byProxy) {
        this.mControlVisibleByProxy = byProxy;
    }

    public void playGestureToLauncherIconAnimation(boolean isIconView) {
        float fromAlpha;
        float toAlpha;
        Animator animator;
        Log.d(TAG, "do go home anim: view visibility: " + getVisibility() + ", window visibility: " + getWindowVisibility());
        this.mTouchHandler.stopHandler();
        if (!(this.mWms == null || this.mContext == null)) {
            boolean isLauncherLandscape = this.mWms.isLauncherLandscape();
            Log.d(TAG, "orientation = " + this.mContext.getResources().getConfiguration().orientation + ", launcher orientation is landscape = " + isLauncherLandscape);
            if (!isLauncherLandscape && this.mContext.getResources().getConfiguration().orientation == 2) {
                setScaleY(1.0f);
                setScaleX(1.0f);
                setVisibility(8);
                if (this.mAnimationListener != null) {
                    this.mAnimationListener.onAnimationStart(2, false);
                    this.mAnimationListener.onAnimationEnd(2, false);
                }
                Log.d(TAG, "first case!");
                return;
            } else if (isLauncherLandscape && this.mContext.getResources().getConfiguration().orientation == 1) {
                setScaleY(1.0f);
                setScaleX(1.0f);
                setVisibility(8);
                if (this.mAnimationListener != null) {
                    this.mAnimationListener.onAnimationStart(2, false);
                    this.mAnimationListener.onAnimationEnd(2, false);
                }
                Log.d(TAG, "second case!");
                return;
            }
        }
        boolean doFadeOutAnimation = false;
        float px = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float py = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        int iconWidth = 0;
        int iconHeight = 0;
        Bitmap iconBitmap = null;
        int flag = 0;
        if (this.mWms != null) {
            px = this.mWms.getExitPivotX();
            py = this.mWms.getExitPivotY();
            iconWidth = this.mWms.getExitIconWidth();
            iconHeight = this.mWms.getExitIconHeight();
            iconBitmap = this.mWms.getExitIconBitmap();
            flag = this.mWms.getExitFlag();
        } else {
            Log.w(TAG, "mWms is null, doFadeOutAnimation!");
            doFadeOutAnimation = true;
        }
        if (iconBitmap == null) {
            doFadeOutAnimation = true;
        }
        Log.d(TAG, "do gesture to home anim: [px, py, iconWidth, iconHeight, iconBitmap, view's px, view's py, flag] = [" + px + ", " + py + ", " + iconWidth + ", " + iconHeight + ", " + iconBitmap + ", " + getPivotX() + ", " + getPivotY() + ", " + flag + "]");
        if (!isIconView) {
            fromAlpha = 1.0f;
            toAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (iconBitmap != null) {
            setScaleType(ScaleType.FIT_XY);
            setImageBitmap(iconBitmap);
            fromAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            toAlpha = 1.0f;
        } else {
            fromAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            toAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (doFadeOutAnimation) {
            animator = createGestureToLauncherFadeOutAnimator(fromAlpha, toAlpha);
        } else {
            animator = createGestureToLauncherIconAnimator(px, py, iconWidth, iconHeight, fromAlpha, toAlpha, (float) flag, isIconView);
        }
        animator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationStart(2, true);
                }
            }

            public void onAnimationEnd(Animator animation) {
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationEnd(2, true);
                }
            }

            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.setStartDelay(TRANSLATE_ANIMATION_DURATION);
        animator.start();
    }

    private Animator createGestureToLauncherFadeOutAnimator(final float fromAlpha, float toAlpha) {
        AnimatorSet animatorSet = new AnimatorSet();
        PathInterpolator alphaInterpolator = new PathInterpolator(0.5f, 0.2f, 0.6f, 1.0f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", new float[]{fromAlpha, toAlpha});
        alphaAnimator.setDuration(280);
        alphaAnimator.setInterpolator(alphaInterpolator);
        PathInterpolator scaleInterpolator = new PathInterpolator(0.5f, 0.2f, 0.6f, 1.0f);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", new float[]{getScaleX(), GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        scaleXAnimator.setDuration(280);
        scaleXAnimator.setInterpolator(scaleInterpolator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", new float[]{getScaleY(), GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        scaleYAnimator.setDuration(280);
        scaleYAnimator.setInterpolator(scaleInterpolator);
        animatorSet.playTogether(new Animator[]{alphaAnimator, scaleXAnimator, scaleYAnimator});
        animatorSet.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                ScaleImageView.this.setVisibility(8);
                ScaleImageView.this.setScaleX(1.0f);
                ScaleImageView.this.setScaleY(1.0f);
                ScaleImageView.this.setAlpha(fromAlpha);
                Log.d(ScaleImageView.TAG, "go home fade out animation end!");
            }

            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        return animatorSet;
    }

    private Animator createGestureToLauncherIconAnimator(float targetPx, float targetPy, int iconWidth, int iconHeight, float fromAlpha, float toAlpha, float flag, boolean isIconView) {
        PhaseInterpolator alphaInterpolator;
        TimeInterpolator[] sizeXInterpolators;
        TimeInterpolator[] sizeYInterpolators;
        TimeInterpolator[] transXInterpolators;
        TimeInterpolator[] transYInterpolators;
        AnimatorSet animatorSet = new AnimatorSet();
        if (flag == 1.0f && isIconView) {
            alphaInterpolator = new PhaseInterpolator(new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.12307692f, 0.24615385f, 0.7692308f, 1.0f}, new float[]{fromAlpha, fromAlpha, toAlpha, toAlpha, fromAlpha}, this.mIconAlphaInterpolators);
        } else {
            alphaInterpolator = new PhaseInterpolator(new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.16f, 0.32f, 1.0f}, new float[]{fromAlpha, fromAlpha, toAlpha, toAlpha}, this.mAlphaInterpolators);
        }
        PhaseInterpolator finalAlphaInterpolator = alphaInterpolator;
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(new float[]{fromAlpha, toAlpha});
        alphaAnimator.setDuration(HOME_TO_ICON_DURATION);
        final PhaseInterpolator phaseInterpolator = finalAlphaInterpolator;
        alphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScaleImageView.this.setAlpha(phaseInterpolator.getInterpolation(animation.getAnimatedFraction()));
            }
        });
        int viewHeight = (int) (((float) getMeasuredHeight()) * getScaleY());
        int viewWidth = (int) (((float) getMeasuredWidth()) * getScaleX());
        getLocationInWindow(new int[2]);
        float fromWidth = (float) viewWidth;
        float fromHeight = (float) viewHeight;
        float toWidth = ((float) iconWidth) * getScaleX();
        float toHeight = ((float) iconHeight) * getScaleY();
        if (flag == 1.0f) {
            toWidth *= 0.4f;
            toHeight *= 0.4f;
        }
        float scaleFromX = getScaleX();
        float scaleFromY = getScaleY();
        float scaleToX = toWidth / fromWidth;
        float scaleToY = toHeight / fromHeight;
        boolean isLandscape = viewWidth > viewHeight;
        float middleX = 1.0f - (((fromWidth - toWidth) * (isLandscape ? 0.54f : 0.44f)) / fromWidth);
        float middleY = 1.0f - (((fromHeight - toHeight) * (isLandscape ? 0.44f : 0.54f)) / fromHeight);
        r35 = new float[3];
        r35 = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.16f, 1.0f};
        float[] scaleOutValuesX = new float[]{scaleFromX, middleX, scaleToX};
        float[] scaleOutValuesY = new float[]{scaleFromY, middleY, scaleToY};
        if (isLandscape) {
            sizeXInterpolators = this.mSizeBigInterpolators;
        } else {
            sizeXInterpolators = this.mSizeSmallInterpolators;
        }
        if (isLandscape) {
            sizeYInterpolators = this.mSizeSmallInterpolators;
        } else {
            sizeYInterpolators = this.mSizeBigInterpolators;
        }
        PhaseInterpolator phaseInterpolator2 = new PhaseInterpolator(r35, scaleOutValuesX, sizeXInterpolators);
        phaseInterpolator2 = new PhaseInterpolator(r35, scaleOutValuesY, sizeYInterpolators);
        ValueAnimator scaleXAnimator = ValueAnimator.ofFloat(new float[]{scaleFromX, scaleToX});
        scaleXAnimator.setDuration(HOME_TO_ICON_DURATION);
        phaseInterpolator = phaseInterpolator2;
        scaleXAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScaleImageView.this.setScaleX(phaseInterpolator.getInterpolation(animation.getAnimatedFraction()));
            }
        });
        ValueAnimator scaleYAnimator = ValueAnimator.ofFloat(new float[]{scaleFromY, scaleToY});
        scaleYAnimator.setDuration(HOME_TO_ICON_DURATION);
        phaseInterpolator = phaseInterpolator2;
        scaleYAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScaleImageView.this.setScaleY(phaseInterpolator.getInterpolation(animation.getAnimatedFraction()));
            }
        });
        final float originalPivotX = getPivotX();
        final float originalPivotY = getPivotY();
        final float transFromX = getTranslationX();
        final float transFromY = getTranslationY();
        float middleTransX = transFromX - ((transFromX - transToX) * (isLandscape ? 0.54f : 0.44f));
        float middleTransY = transFromY - ((transFromY - ((targetPy - originalPivotY) + (((float) iconHeight) * DY_COMPENSATION_FACTOR))) * (isLandscape ? 0.44f : 0.54f));
        r48 = new float[3];
        r48 = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.16f, 1.0f};
        float[] transOutValuesX = new float[]{transFromX, middleTransX, targetPx - originalPivotX};
        float[] transOutValuesY = new float[]{transFromY, middleTransY, (targetPy - originalPivotY) + (((float) iconHeight) * DY_COMPENSATION_FACTOR)};
        if (isLandscape) {
            transXInterpolators = this.mSizeBigInterpolators;
        } else {
            transXInterpolators = this.mSizeSmallInterpolators;
        }
        if (isLandscape) {
            transYInterpolators = this.mSizeSmallInterpolators;
        } else {
            transYInterpolators = this.mSizeBigInterpolators;
        }
        phaseInterpolator2 = new PhaseInterpolator(r48, transOutValuesX, transXInterpolators);
        phaseInterpolator2 = new PhaseInterpolator(r48, transOutValuesY, transYInterpolators);
        ValueAnimator transXAnimator = ValueAnimator.ofFloat(new float[]{transFromX, transToX});
        transXAnimator.setDuration(HOME_TO_ICON_DURATION);
        phaseInterpolator = phaseInterpolator2;
        transXAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScaleImageView.this.setTranslationX(phaseInterpolator.getInterpolation(animation.getAnimatedFraction()));
            }
        });
        ValueAnimator transYAnimator = ValueAnimator.ofFloat(new float[]{transFromY, transToY});
        transYAnimator.setDuration(HOME_TO_ICON_DURATION);
        phaseInterpolator = phaseInterpolator2;
        transYAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScaleImageView.this.setTranslationY(phaseInterpolator.getInterpolation(animation.getAnimatedFraction()));
            }
        });
        animatorSet.playTogether(new Animator[]{alphaAnimator, scaleXAnimator, scaleYAnimator, transXAnimator, transYAnimator});
        final float f = fromAlpha;
        animatorSet.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                ScaleImageView.this.setVisibility(8);
                ScaleImageView.this.setScaleX(1.0f);
                ScaleImageView.this.setScaleY(1.0f);
                ScaleImageView.this.setAlpha(f);
                ScaleImageView.this.setPivotX(originalPivotX);
                ScaleImageView.this.setPivotY(originalPivotY);
                ScaleImageView.this.setTranslationX(transFromX);
                ScaleImageView.this.setTranslationY(transFromY);
                Log.d(ScaleImageView.TAG, "go home animation end!");
            }

            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        return animatorSet;
    }

    public void setAnimationListener(TranslateAnimationListener listener) {
        this.mAnimationListener = listener;
    }
}
