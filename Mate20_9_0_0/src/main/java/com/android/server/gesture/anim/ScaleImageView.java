package com.android.server.gesture.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.TaskSnapshot;
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
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.PathInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.server.gesture.GestureNavConst;
import com.android.server.wm.PhaseInterpolator;
import java.util.Arrays;
import java.util.List;

public class ScaleImageView extends ImageView {
    public static final int ANIM_TYPE_RECOVER = 1;
    public static final int ANIM_TYPE_TRANSLATE = 0;
    private static final int ERROR_ID = -1;
    private static final float FOLLOW_TRANSLATE_RATIO = 0.1f;
    private static final int LANDSCAPE_FINAL_WIDTH = 316;
    private static final String LEFT = "left";
    private static final float MAX_SCALE_DISTANCE = 500.0f;
    private static final float MAX_SCALE_RATIO = 0.7f;
    private static final int NAVIGATIONBAR_LAYER = 231000;
    private static final int PICTURE_ROUND = 2;
    private static final int PORTRAIT_FINAL_WIDTH = 328;
    private static final long RECOVER_ANIMATION_DURATION = 100;
    private static final String RIGHT = "right";
    private static final float SCALE = 0.75f;
    private static final float SCALE_X_PIVOT = 0.5f;
    private static final float SCALE_Y_PIVOT = 0.625f;
    private static final int STATE_LEFT = 1;
    private static final int STATE_MIDDLE = 0;
    private static final int STATE_RIGHT = 2;
    private static final String TAG = "GestureScale";
    private static final long TRANSLATE_ANIMATION_DURATION = 300;
    private static final int TYPE_LANDSCAPE = 1;
    private static final int TYPE_PORTRAIT = 0;
    private TimeInterpolator[] mAlphaInterpolators;
    private ActivityManager mAm;
    private TranslateAnimationListener mAnimationListener;
    private TimeInterpolator mConstantInterpolator;
    private Context mContext;
    private boolean mFollowStart;
    private float mFollowStartX;
    private float mFollowStartY;
    private float mHeight;
    private int mScreenOrientation;
    private TimeInterpolator[] mSizeBigInterpolators;
    private TimeInterpolator[] mSizeSmallInterpolators;
    private float mWidth;

    public interface TranslateAnimationListener {
        void onAnimationEnd(int i);
    }

    public ScaleImageView(Context context) {
        super(context);
        this.mConstantInterpolator = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return 1.0f;
            }
        };
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        init(context);
    }

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mConstantInterpolator = /* anonymous class already generated */;
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        init(context);
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mConstantInterpolator = /* anonymous class already generated */;
        this.mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
        this.mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, 0.3f, 1.0f)};
        this.mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.6f, 1.0f), this.mConstantInterpolator};
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "init");
        this.mFollowStart = false;
        this.mContext = context;
        this.mAm = (ActivityManager) context.getSystemService(ActivityManager.class);
        initWms();
    }

    private void initWms() {
    }

    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        this.mWidth = (float) w;
        this.mHeight = (float) h;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("view width is ");
        stringBuilder.append(this.mWidth);
        stringBuilder.append(", height is ");
        stringBuilder.append(this.mHeight);
        Log.d(str, stringBuilder.toString());
        float pivotX = this.mWidth * 0.5f;
        float pivotY = this.mHeight * SCALE_Y_PIVOT;
        setPivotX(pivotX);
        setPivotY(pivotY);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("view pivotX is ");
        stringBuilder2.append(pivotX);
        stringBuilder2.append(", pivotY is ");
        stringBuilder2.append(pivotY);
        Log.d(str2, stringBuilder2.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mScreenOrientation ");
        stringBuilder.append(this.mScreenOrientation);
        Log.d(str, stringBuilder.toString());
    }

    public void setFollowPosition(float x, float y) {
        if (this.mFollowStart) {
            doScale(this.mFollowStartY < y ? GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO : this.mFollowStartY - y);
            doTranslate(x - this.mFollowStartX, this.mFollowStartY - y, 0.1f);
            return;
        }
        this.mFollowStartX = x;
        this.mFollowStartY = y;
        this.mFollowStart = true;
    }

    private void doScale(float y) {
        float scale;
        if (y >= MAX_SCALE_DISTANCE) {
            scale = 0.7f;
        } else {
            scale = 1.0f - (0.3f * (y / MAX_SCALE_DISTANCE));
        }
        setScaleX(scale);
        setScaleY(scale);
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
        setBackgroundColor(-7829368);
        refreshBySurfaceControl();
        setVisibility(0);
        long newT = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("refresh content cost time ");
        stringBuilder.append(newT - oldT);
        Log.d(str, stringBuilder.toString());
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

    private void refreshContentByActivityManager() {
        Log.d(TAG, "refreshContentByActivityManager");
        int topTaskId = getTopTaskId();
        if (topTaskId == -1) {
            Log.e(TAG, "error top task id");
            return;
        }
        Bitmap snapshot = getTaskSnapshot(topTaskId);
        if (snapshot == null) {
            Log.w(TAG, "task snapshot is null, try to get task thumbnail");
            snapshot = getTaskThumbnail(topTaskId);
        }
        if (snapshot != null) {
            Log.d(TAG, "set snapshot bitmap into ImageView");
            setScaleType(ScaleType.FIT_CENTER);
            setImageBitmap(addRoundOnBitmap(snapshot, (float) dp2px(this.mContext, 2)));
        } else {
            Log.d(TAG, "no snapshot, use color.");
            setBackgroundColor(-7829368);
        }
    }

    private int getTopTaskId() {
        List<RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            Log.d(TAG, "tasks count is 0");
            return -1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("tasks count is ");
        stringBuilder.append(tasks.size());
        Log.d(str, stringBuilder.toString());
        return ((RunningTaskInfo) tasks.get(0)).id;
    }

    private Bitmap getTaskSnapshot(int topTaskId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTaskSnapshot with top task id ");
        stringBuilder.append(topTaskId);
        Log.d(str, stringBuilder.toString());
        try {
            TaskSnapshot taskSnapshot = ActivityManager.getService().getTaskSnapshot(topTaskId, false);
            if (taskSnapshot == null) {
                Log.e(TAG, "error, taskSnapshot is null");
                return null;
            }
            Bitmap snapshot = Bitmap.createHardwareBitmap(taskSnapshot.getSnapshot());
            if (snapshot == null) {
                Log.e(TAG, "error, snapshot is null");
            }
            return snapshot;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException");
            return null;
        }
    }

    private Bitmap getTaskThumbnail(int topTaskId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTaskThumbnail with top task id ");
        stringBuilder.append(topTaskId);
        Log.d(str, stringBuilder.toString());
        return null;
    }

    private void refreshBySurfaceControl() {
        Log.d(TAG, "refreshBySurfaceControl");
        setScaleType(ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        matrix.setTranslate(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, (float) getStatusBarHeight(this.mContext));
        setImageMatrix(matrix);
        Bitmap bitmap = screenShotBitmap(this.mContext, 0, Integer.MAX_VALUE);
        if (bitmap == null) {
            setBackgroundColor(-7829368);
            return;
        }
        setScaleType(ScaleType.FIT_CENTER);
        setImageBitmap(addRoundOnBitmap(bitmap, (float) dp2px(this.mContext, 2)));
    }

    public static Bitmap screenShotBitmap(Context ctx, int minLayer, int maxLayer) {
        return screenShotBitmap(ctx, 1.0f);
    }

    public static Bitmap screenShotBitmap(Context ctx, float scale) {
        Rect screenRect;
        Bitmap bitmap;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        DisplayMetrics displayMetricsBody = new DisplayMetrics();
        Display display = ((WindowManager) ctx.getSystemService("window")).getDefaultDisplay();
        display.getMetrics(displayMetricsBody);
        display.getRealMetrics(displayMetrics);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display getMetrics w:");
        stringBuilder.append(displayMetricsBody.widthPixels);
        stringBuilder.append(", h:");
        stringBuilder.append(displayMetricsBody.heightPixels);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Display getRealMetrics w:");
        stringBuilder.append(displayMetrics.widthPixels);
        stringBuilder.append(", h:");
        stringBuilder.append(displayMetrics.heightPixels);
        Log.d(str, stringBuilder.toString());
        int[] dims = new int[]{(((int) (((float) displayMetrics.widthPixels) * scale)) / 2) * 2, (((int) (((float) displayMetrics.heightPixels) * scale)) / 2) * 2};
        int[] dimsBody = new int[]{(((int) (((float) displayMetricsBody.widthPixels) * scale)) / 2) * 2, (((int) (((float) displayMetricsBody.heightPixels) * scale)) / 2) * 2};
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mFingerViewParams, dims[0] =");
        stringBuilder2.append(dims[0]);
        stringBuilder2.append(", dims[1] =");
        stringBuilder2.append(dims[1]);
        Log.d(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mFingerViewParams, dimsBody[0] =");
        stringBuilder2.append(dimsBody[0]);
        stringBuilder2.append(", dimsBody[1] =");
        stringBuilder2.append(dimsBody[1]);
        Log.d(str2, stringBuilder2.toString());
        if (isLazyMode(ctx)) {
            screenRect = getScreenshotRect(ctx);
        } else {
            screenRect = new Rect();
        }
        screenRect = new Rect(0, getStatusBarHeight(ctx), dimsBody[0], dimsBody[1]);
        int rotation = display.getRotation();
        if (rotation == 0 || 2 == rotation) {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("SurfaceControl.screenshot_ext_hw with rotation ");
            stringBuilder3.append(rotation);
            Log.d(str, stringBuilder3.toString());
            bitmap = SurfaceControl.screenshot_ext_hw(screenRect, dimsBody[0], dimsBody[1], 0, NAVIGATIONBAR_LAYER, false, converseRotation(rotation));
        } else {
            bitmap = rotationScreenBitmap(screenRect, rotation, dimsBody, 0, NAVIGATIONBAR_LAYER);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, dimsBody[0], dimsBody[1]);
        if (bitmap == null) {
            Log.e(TAG, "screenShotBitmap error bitmap is null");
            return null;
        }
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

    private static Bitmap rotationScreenBitmap(Rect rect, int rotation, int[] srcDims, int minLayer, int maxLayer) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("rotationScreenBitmap with rotation ");
        stringBuilder.append(rotation);
        stringBuilder.append(", srcDims ");
        stringBuilder.append(Arrays.toString(srcDims));
        stringBuilder.append(", layer ");
        int i = minLayer;
        stringBuilder.append(i);
        stringBuilder.append(",");
        int i2 = maxLayer;
        stringBuilder.append(i2);
        Log.d(str, stringBuilder.toString());
        float degrees = convertRotationToDegrees(rotation);
        float[] dims = new float[]{(float) srcDims[0], (float) srcDims[1]};
        Matrix metrics = new Matrix();
        metrics.reset();
        metrics.preRotate(-degrees);
        metrics.mapPoints(dims);
        dims[0] = Math.abs(dims[0]);
        dims[1] = Math.abs(dims[1]);
        Bitmap bitmap = SurfaceControl.screenshot(rect, (int) dims[0], (int) dims[1], i, i2, false, 0);
        Bitmap ss = Bitmap.createBitmap(srcDims[0], srcDims[1], Config.ARGB_8888);
        Canvas c = new Canvas(ss);
        c.translate(((float) srcDims[0]) * 0.5f, ((float) srcDims[1]) * 0.5f);
        c.rotate(degrees);
        c.translate((-dims[0]) * 0.5f, (-dims[1]) * 0.5f);
        c.drawBitmap(bitmap, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, null);
        bitmap.recycle();
        return ss;
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
        Rect sourceCrop = null;
        int state = getLazyState(context);
        if (1 == state) {
            sourceCrop = new Rect(0, (int) (((float) displayMetrics.heightPixels) * 0.25f), (int) (((float) displayMetrics.widthPixels) * 0.75f), displayMetrics.heightPixels);
        } else if (2 == state) {
            sourceCrop = new Rect((int) (((float) displayMetrics.widthPixels) * 0.25f), (int) (((float) displayMetrics.heightPixels) * 0.25f), displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        return sourceCrop;
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0076, element type: float, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void playTranslateAnimation(int topDistance) {
        int finalWidth;
        this.mFollowStart = false;
        float oldX = getX();
        float oldY = getY();
        float oldScaleX = getScaleX();
        float oldScaleY = getScaleY();
        if (this.mScreenOrientation == 0) {
            finalWidth = dp2px(this.mContext, PORTRAIT_FINAL_WIDTH);
        } else {
            finalWidth = dp2px(this.mContext, LANDSCAPE_FINAL_WIDTH);
        }
        float newY = ((float) topDistance) - ((this.mHeight - (this.mHeight * (((float) finalWidth) / this.mWidth))) / 2.0f);
        PropertyValuesHolder xProperty = PropertyValuesHolder.ofFloat("x", new float[]{oldX, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        PropertyValuesHolder yProperty = PropertyValuesHolder.ofFloat("y", new float[]{oldY, newY});
        PropertyValuesHolder xScaleProperty = PropertyValuesHolder.ofFloat("scaleX", new float[]{oldScaleX, newScale});
        PropertyValuesHolder yScaleProperty = PropertyValuesHolder.ofFloat("scaleY", new float[]{oldScaleY, newScale});
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat("alpha", new float[]{1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        ObjectAnimator transAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{xProperty, yProperty, xScaleProperty, yScaleProperty, alphaProperty});
        transAnimator.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563661));
        transAnimator.setDuration(300);
        transAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Log.d(ScaleImageView.TAG, "onAnimationEnd translate");
                ScaleImageView.this.setVisibility(8);
                ScaleImageView.this.setScaleX(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setScaleY(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setX(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setY(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                ScaleImageView.this.setAlpha(1.0f);
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationEnd(0);
                }
            }
        });
        transAnimator.start();
    }

    public void playRecoverAnimation() {
        this.mFollowStart = false;
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
            public void onAnimationEnd(Animator animation) {
                Log.d(ScaleImageView.TAG, "onAnimationEnd recover");
                ScaleImageView.this.setVisibility(8);
                if (ScaleImageView.this.mAnimationListener != null) {
                    ScaleImageView.this.mAnimationListener.onAnimationEnd(1);
                }
            }
        });
        recover.start();
    }

    public void playGestureToLauncherIconAnimation(boolean isIconView) {
    }

    private float[] calculateScalePivot(float px, float py, float fromWidth, float fromHeight, float toWidth, float toHeight, int[] fromLocInWindow) {
        float scaleX = toWidth / fromWidth;
        float scaleY = toHeight / fromHeight;
        float pivotX = ((px - (toWidth / 2.0f)) - (((float) fromLocInWindow[0]) * scaleX)) / (1.0f - scaleX);
        float pivotY = ((py - (toHeight / 2.0f)) - (((float) fromLocInWindow[1]) * scaleY)) / (1.0f - scaleY);
        return new float[]{pivotX, pivotY};
    }

    private Animation createGestureToLauncherIconAnimation(float targetPx, float targetPy, int iconWidth, int iconHeight, float fromAlpha, float toAlpha) {
        TimeInterpolator[] sizeXInterpolators;
        TimeInterpolator[] sizeYInterpolators;
        AnimationSet contentAnimSet = new AnimationSet(false);
        long duration = SystemProperties.getLong("to_launcher_dur", 350);
        AlphaAnimation contentAlphaAnim = new AlphaAnimation(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f);
        contentAlphaAnim.setDuration(duration);
        float[] alphaInValues = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.16f, 0.32f, 1.0f};
        float[] contentAlphaOutValues = new float[]{fromAlpha, fromAlpha, toAlpha, toAlpha};
        PhaseInterpolator contentAlphaInterpolator = new PhaseInterpolator(alphaInValues, contentAlphaOutValues, this.mAlphaInterpolators);
        contentAlphaAnim.setInterpolator(contentAlphaInterpolator);
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        int windowWidth = wm.getDefaultDisplay().getWidth();
        int windowHeight = wm.getDefaultDisplay().getHeight();
        int viewHeight = (int) (((float) getMeasuredHeight()) * getScaleY());
        int viewWidth = (int) (((float) getMeasuredWidth()) * getScaleX());
        Object viewLocInWindow = new int[2];
        getLocationInWindow(viewLocInWindow);
        String str = TAG;
        int windowWidth2 = windowWidth;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[viewWidth, viewHeight, viewLocX, viewLocY] = [");
        stringBuilder.append(viewWidth);
        stringBuilder.append(", ");
        stringBuilder.append(viewHeight);
        stringBuilder.append(", ");
        stringBuilder.append(viewLocInWindow[0]);
        stringBuilder.append(", ");
        stringBuilder.append(viewLocInWindow[1]);
        stringBuilder.append("]");
        Log.d(str, stringBuilder.toString());
        float fromWidth = (float) viewWidth;
        float fromHeight = (float) viewHeight;
        float toWidth = (float) iconWidth;
        AnimationSet contentAnimSet2 = contentAnimSet;
        float toHeight = (float) iconHeight;
        float scaleToX = toWidth / fromWidth;
        float scaleToY = toHeight / fromHeight;
        boolean isHorizontal = viewWidth > viewHeight;
        float middleYRatio = 0.44f;
        float middleXRatio = isHorizontal ? 0.54f : 0.44f;
        if (!isHorizontal) {
            middleYRatio = 0.54f;
        }
        float middleX = 1.0f - (((fromWidth - toWidth) * middleXRatio) / fromWidth);
        float middleY = 1.0f - (((fromHeight - toHeight) * middleYRatio) / fromHeight);
        int windowWidth3 = windowWidth2;
        int windowHeight2 = windowHeight;
        int viewHeight2 = viewHeight;
        float[] pivot = calculateScalePivot(targetPx, targetPy, fromWidth, fromHeight, toWidth, toHeight, viewLocInWindow);
        float pivotX = pivot[0];
        fromHeight = pivot[1];
        contentAlphaOutValues = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.16f, 1.0f};
        float[] scaleOutValuesX = new float[]{1.0f, middleX, scaleToX};
        float[] scaleOutValuesY = new float[]{1.0f, middleY, scaleToY};
        if (isHorizontal) {
            sizeXInterpolators = this.mSizeBigInterpolators;
        } else {
            sizeXInterpolators = this.mSizeSmallInterpolators;
        }
        if (isHorizontal) {
            sizeYInterpolators = this.mSizeSmallInterpolators;
        } else {
            sizeYInterpolators = this.mSizeBigInterpolators;
        }
        PhaseInterpolator interpolatorX = new PhaseInterpolator(contentAlphaOutValues, scaleOutValuesX, sizeXInterpolators);
        PhaseInterpolator interpolatorY = new PhaseInterpolator(contentAlphaOutValues, scaleOutValuesY, sizeYInterpolators);
        float f = pivotX;
        float f2 = fromHeight;
        ScaleAnimation contentScaleAnimX = new ScaleAnimation(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f, 1.0f, 1.0f, f, f2);
        contentScaleAnimX.setDuration(duration);
        contentScaleAnimX.setInterpolator(interpolatorX);
        ScaleAnimation contentScaleAnimY = new ScaleAnimation(1.0f, 1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f, f, f2);
        contentScaleAnimY.setDuration(duration);
        contentScaleAnimY.setInterpolator(interpolatorY);
        AnimationSet contentAnimSet3 = contentAnimSet2;
        contentAnimSet3.addAnimation(contentAlphaAnim);
        contentAnimSet3.addAnimation(contentScaleAnimX);
        contentAnimSet3.addAnimation(contentScaleAnimY);
        viewHeight = windowWidth3;
        contentAnimSet3.initialize(viewWidth, viewHeight2, viewHeight, windowHeight2);
        contentAnimSet3.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                ScaleImageView.this.setVisibility(8);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        return contentAnimSet3;
    }

    public void setAnimationListener(TranslateAnimationListener listener) {
        this.mAnimationListener = listener;
    }
}
