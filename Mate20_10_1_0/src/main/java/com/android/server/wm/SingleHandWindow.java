package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import huawei.android.provider.FrontFingerPrintSettings;

/* access modifiers changed from: package-private */
public final class SingleHandWindow {
    private static final int ALP_BLACK_COLOR = -1728053248;
    private static final int BLACK_COLOR = 0;
    private static final String BLURPAPER = "blurpaper";
    private static final String BLURPAPER_TOP = "blurpapertop";
    private static final int DOUBLE_VALUE = 2;
    private static final int FREEZE_ROTATION = -1;
    private static final float INITIAL_SCALE = 0.75f;
    private static final boolean IS_DEBUG = false;
    public static final int LAZY_MODE_OFF = 0;
    public static final int LAZY_MODE_ON_LEFT = 1;
    public static final int LAZY_MODE_ON_LEFT_HINT = 3;
    public static final int LAZY_MODE_ON_RIGHT = 2;
    public static final int LAZY_MODE_ON_RIGHT_HINT = 4;
    private static final float MAX_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.3f;
    private static final int NINTH_DENOMINATOR = 9;
    private static final int QUARTER_DENOMINATOR = 4;
    private static final String SINGLE_HAND_MODE_GUIDE_SHOWN = "single_hand_mode_guide_shown";
    private static final String SINGLE_HAND_MODE_HINT_SHOWN = "single_hand_mode_hint_shown";
    private static final int SLEEP_TIME = 100;
    private static final String TAG = "SingleHand";
    private static final float WINDOW_ALPHA = 1.0f;
    private static final String YES = "yes";
    private View.OnClickListener mActionClickListener = new View.OnClickListener() {
        /* class com.android.server.wm.SingleHandWindow.AnonymousClass3 */

        public void onClick(View v) {
            SingleHandWindow.this.showHint(true);
            Settings.Global.putString(SingleHandWindow.this.mContext.getContentResolver(), SingleHandWindow.SINGLE_HAND_MODE_GUIDE_SHOWN, SingleHandWindow.YES);
        }
    };
    private Configuration mConfiguration = new Configuration();
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final Display mDefaultDisplay;
    private DisplayInfo mDefaultDisplayInfo = new DisplayInfo();
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        /* class com.android.server.wm.SingleHandWindow.AnonymousClass4 */

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayChanged(int displayId) {
            if (displayId != SingleHandWindow.this.mDefaultDisplay.getDisplayId()) {
                return;
            }
            if (!SingleHandWindow.this.updateDefaultDisplayInfo()) {
                SingleHandWindow.this.dismiss();
            } else if (SingleHandWindow.this.mIsNeedRelayout) {
                SingleHandWindow.this.relayout();
            }
        }

        public void onDisplayRemoved(int displayId) {
            if (displayId == SingleHandWindow.this.mDefaultDisplay.getDisplayId()) {
                SingleHandWindow.this.dismiss();
            }
        }
    };
    private final DisplayManager mDisplayManager;
    private Handler mHandler;
    private int mHeight;
    private float mHeightScale;
    /* access modifiers changed from: private */
    public ImageView mImageView;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /* class com.android.server.wm.SingleHandWindow.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                    SingleHandWindow.this.updateLocale();
                }
                if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                    SingleHandWindow.this.updateConfiguration();
                }
            }
        }
    };
    private boolean mIsAttachedToWindow = false;
    private boolean mIsHintShowing;
    private final boolean mIsLeft;
    /* access modifiers changed from: private */
    public boolean mIsNeedRelayout = false;
    /* access modifiers changed from: private */
    public boolean mIsPointDownOuter = false;
    private boolean mIsWindowVisible;
    private ViewGroup.LayoutParams mLayoutParams;
    private int mLazyMode = 0;
    private final String mName;
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        /* class com.android.server.wm.SingleHandWindow.AnonymousClass2 */

        public boolean onTouch(View view, MotionEvent event) {
            boolean inRegion = SingleHandWindow.this.singlehandRegionContainsPoint((int) event.getRawX(), (int) event.getRawY());
            int actionMasked = event.getActionMasked();
            if (actionMasked == 0) {
                boolean unused = SingleHandWindow.this.mIsPointDownOuter = !inRegion;
            } else if (actionMasked == 1) {
                ImageView imageView = (ImageView) SingleHandWindow.this.mWindowContent.findViewById(34603162);
                if (imageView == null || imageView.getVisibility() != 0) {
                    SingleHandWindow.this.showHint(false);
                    Settings.Global.putString(SingleHandWindow.this.mContext.getContentResolver(), SingleHandWindow.SINGLE_HAND_MODE_HINT_SHOWN, SingleHandWindow.YES);
                } else if (!inRegion && SingleHandWindow.this.mIsPointDownOuter) {
                    Settings.Global.putString(SingleHandWindow.this.mContext.getContentResolver(), "single_hand_mode", "");
                }
                boolean unused2 = SingleHandWindow.this.mIsPointDownOuter = false;
            } else if (actionMasked == 3) {
                boolean unused3 = SingleHandWindow.this.mIsPointDownOuter = false;
            }
            return true;
        }
    };
    private DisplayInfo mPreDisplayInfo = new DisplayInfo();
    private RelativeLayout mRelateViewbottom;
    private RelativeLayout mRelateViewtop;
    /* access modifiers changed from: private */
    public final WindowManagerService mService;
    private int mWidth;
    private float mWidthScale;
    /* access modifiers changed from: private */
    public View mWindowContent;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private TextView overlayDisplayWindow = null;
    private TextView overlayGuideWindow = null;
    private TextView slideHintText1 = null;
    private TextView slideHintText2 = null;
    private TextView slideHintTextFp = null;

    SingleHandWindow(boolean isLeft, String name, int width, int height, WindowManagerService service) {
        this.mContext = service.mContext;
        this.mName = name;
        this.mWidth = width;
        this.mHeight = height;
        this.mIsLeft = isLeft;
        this.mHandler = new Handler();
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mService = service;
        this.mDefaultDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDefaultDisplayInfo = this.mService.getDefaultDisplayContentLocked().getDisplayInfo();
        this.mConfiguration = this.mContext.getResources().getConfiguration();
        this.mPreDisplayInfo.copyFrom(this.mDefaultDisplayInfo);
        if (this.mName.contains(BLURPAPER)) {
            createWindow();
        }
    }

    private void show(View v, boolean isVis) {
        if (v == null) {
            return;
        }
        if (isVis) {
            v.setVisibility(0);
        } else {
            v.setVisibility(4);
        }
    }

    public void show() {
        if (!this.mIsWindowVisible) {
            if (!this.mName.contains(BLURPAPER)) {
                this.mService.freezeOrThawRotation(0);
                this.mLazyMode = this.mIsLeft ? 1 : 2;
                this.mService.setLazyMode(this.mLazyMode, this.mIsHintShowing, this.mName);
            }
            this.mDisplayManager.registerDisplayListener(this.mDisplayListener, null);
            if (!updateDefaultDisplayInfo()) {
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
                return;
            }
            if (BLURPAPER_TOP.equals(this.mName)) {
                WindowManager.LayoutParams layoutParams = this.mWindowParams;
                layoutParams.x = 0;
                layoutParams.y = 0;
                layoutParams.width = this.mWidth;
                layoutParams.height = this.mHeight;
                this.mWindowContent.setOnTouchListener(this.mOnTouchListener);
                this.mWindowManager.addView(this.mWindowContent, this.mWindowParams);
            }
            this.mIsWindowVisible = true;
        }
    }

    public void dismiss() {
        if (this.mIsAttachedToWindow) {
            this.mIsAttachedToWindow = false;
            this.mContext.unregisterReceiver(this.mIntentReceiver);
        }
        if (this.mIsWindowVisible) {
            if (updateDefaultDisplayInfo()) {
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            }
            if (!this.mName.contains(BLURPAPER)) {
                this.mHandler.postDelayed(new Runnable() {
                    /* class com.android.server.wm.SingleHandWindow.AnonymousClass5 */

                    public void run() {
                        SingleHandWindow.this.mService.freezeOrThawRotation(-1);
                    }
                }, 100);
                this.mLazyMode = 0;
                this.mIsHintShowing = false;
                this.mService.setLazyMode(this.mLazyMode, this.mIsHintShowing, this.mName);
            } else {
                this.mWindowManager.removeView(this.mWindowContent);
            }
            this.mIsWindowVisible = false;
        }
    }

    public void relayout() {
        if (this.mIsWindowVisible && this.mName.contains(BLURPAPER)) {
            this.mWindowManager.removeView(this.mWindowContent);
            this.mIsWindowVisible = false;
            createWindow();
            updateWindowParams();
            this.mWindowContent.setOnTouchListener(this.mOnTouchListener);
            this.mWindowManager.addView(this.mWindowContent, this.mWindowParams);
            this.mIsWindowVisible = true;
        }
    }

    private Bitmap cropwallpaper(boolean isTop) {
        if (SingleHandAdapter.scaleWallpaper == null) {
            Slog.e(TAG, "cropwallpaper fail");
            return null;
        }
        int w = SingleHandAdapter.scaleWallpaper.getWidth();
        int h = SingleHandAdapter.scaleWallpaper.getHeight();
        if (isTop) {
            return Bitmap.createBitmap(SingleHandAdapter.scaleWallpaper, 0, 0, w, (int) (((float) h) * 0.25f));
        }
        if (this.mIsLeft) {
            return Bitmap.createBitmap(SingleHandAdapter.scaleWallpaper, (int) (((float) w) * 0.75f), (int) (((float) h) * 0.25f), (int) (((float) w) - (((float) w) * 0.75f)), (int) (((float) h) * 0.75f));
        }
        return Bitmap.createBitmap(SingleHandAdapter.scaleWallpaper, 0, (int) (((float) h) * 0.25f), (int) (((float) w) - (((float) w) * 0.75f)), (int) (((float) h) * 0.75f));
    }

    /* access modifiers changed from: package-private */
    public void updateLocale() {
        Slog.d(TAG, "updateLocale");
        TextView textView = this.overlayDisplayWindow;
        if (textView != null) {
            textView.setText(this.mContext.getResources().getString(33685760));
        }
        TextView textView2 = this.overlayGuideWindow;
        if (textView2 != null) {
            textView2.setText(this.mContext.getResources().getString(33686194));
            this.overlayGuideWindow.setBackgroundDrawable(this.mContext.getResources().getDrawable(33752027));
        }
        TextView textView3 = this.slideHintText1;
        if (textView3 != null) {
            textView3.setText(this.mContext.getResources().getString(33686195, 1));
        }
        TextView textView4 = this.slideHintText2;
        if (textView4 != null) {
            textView4.setText(this.mContext.getResources().getString(33686196, 2));
        }
        TextView textView5 = this.slideHintTextFp;
        if (textView5 != null) {
            textView5.setText(this.mContext.getResources().getString(33686202));
        }
    }

    /* access modifiers changed from: package-private */
    public void updateConfiguration() {
        Configuration newConfiguration = this.mContext.getResources().getConfiguration();
        int diff = this.mConfiguration.diff(newConfiguration);
        this.mConfiguration = newConfiguration;
        if ((diff & 128) != 0) {
            Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
        }
    }

    /* access modifiers changed from: private */
    public boolean updateDefaultDisplayInfo() {
        boolean isValue = this.mDefaultDisplay.getDisplayInfo(this.mDefaultDisplayInfo);
        this.mIsNeedRelayout = false;
        if (!isValue) {
            Slog.w(TAG, "there is no default display");
            return false;
        }
        DisplayInfo displayInfo = this.mPreDisplayInfo;
        if (displayInfo == null) {
            return false;
        }
        if (!displayInfo.equals(this.mDefaultDisplayInfo)) {
            this.mWidthScale = ((float) this.mDefaultDisplayInfo.logicalWidth) / ((float) this.mPreDisplayInfo.logicalWidth);
            this.mHeightScale = ((float) this.mDefaultDisplayInfo.logicalHeight) / ((float) this.mPreDisplayInfo.logicalHeight);
            if (!(this.mDefaultDisplayInfo.logicalWidth == this.mPreDisplayInfo.logicalWidth && this.mDefaultDisplayInfo.logicalHeight == this.mPreDisplayInfo.logicalHeight && this.mDefaultDisplayInfo.logicalDensityDpi == this.mPreDisplayInfo.logicalDensityDpi)) {
                this.mIsNeedRelayout = true;
            }
            this.mPreDisplayInfo.copyFrom(this.mDefaultDisplayInfo);
        }
        return true;
    }

    public void updateLayoutParams() {
        int quarterLogicalHeight = this.mDefaultDisplayInfo.logicalHeight / 4;
        this.mLayoutParams = this.mRelateViewtop.getLayoutParams();
        ViewGroup.LayoutParams layoutParams = this.mLayoutParams;
        layoutParams.height = quarterLogicalHeight;
        this.mRelateViewtop.setLayoutParams(layoutParams);
        this.mLayoutParams = this.mRelateViewbottom.getLayoutParams();
        this.mLayoutParams.height = this.mDefaultDisplayInfo.logicalHeight - quarterLogicalHeight;
        this.mLayoutParams.width = this.mDefaultDisplayInfo.logicalWidth / 4;
        if (this.mIsLeft) {
            ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(11);
        } else {
            ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(9);
        }
        ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(12);
        this.mRelateViewbottom.setLayoutParams(this.mLayoutParams);
    }

    private void handleBlurpaper() {
        synchronized (SingleHandAdapter.mLock) {
            this.mWindowContent.setBackgroundColor(0);
            int quarterLogicalHeight = this.mDefaultDisplayInfo.logicalHeight / 4;
            this.mRelateViewtop = (RelativeLayout) this.mWindowContent.findViewById(34603160);
            this.mLayoutParams = this.mRelateViewtop.getLayoutParams();
            this.mLayoutParams.height = quarterLogicalHeight;
            this.mRelateViewtop.setLayoutParams(this.mLayoutParams);
            this.mRelateViewtop.setBackground(new BitmapDrawable(this.mRelateViewtop.getResources(), cropwallpaper(true)));
            this.mRelateViewbottom = (RelativeLayout) this.mWindowContent.findViewById(34603161);
            this.mLayoutParams = this.mRelateViewbottom.getLayoutParams();
            this.mLayoutParams.height = this.mDefaultDisplayInfo.logicalHeight - quarterLogicalHeight;
            this.mLayoutParams.width = this.mDefaultDisplayInfo.logicalWidth / 4;
            if (!this.mIsLeft) {
                if (this.mLayoutParams instanceof RelativeLayout.LayoutParams) {
                    ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(9);
                }
                if (HwDisplaySizeUtil.hasSideInScreen()) {
                    this.mLayoutParams.width -= HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth() * 2;
                }
            } else if (this.mLayoutParams instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(11);
            }
            if (this.mLayoutParams instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) this.mLayoutParams).addRule(12);
            }
            this.mRelateViewbottom.setLayoutParams(this.mLayoutParams);
            this.mRelateViewbottom.setBackground(new BitmapDrawable(this.mRelateViewbottom.getResources(), cropwallpaper(false)));
        }
    }

    private void createWindow() {
        ViewGroup.MarginLayoutParams marginParams;
        this.mWindowContent = LayoutInflater.from(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null))).inflate(34013238, (ViewGroup) null);
        if (!this.mIsAttachedToWindow) {
            this.mIsAttachedToWindow = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.LOCALE_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        }
        if (this.mName.contains(BLURPAPER)) {
            handleBlurpaper();
        }
        this.mWindowParams = new WindowManager.LayoutParams((int) HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL);
        this.mWindowParams.flags |= 16778024;
        this.mWindowParams.privateFlags |= 2;
        WindowManager.LayoutParams layoutParams = this.mWindowParams;
        layoutParams.alpha = 1.0f;
        layoutParams.gravity = 8388659;
        layoutParams.format = -3;
        layoutParams.layoutInDisplaySideMode = 2;
        layoutParams.hwFlags = 64;
        boolean hintShown = isSingleHandModeHintShown(SINGLE_HAND_MODE_HINT_SHOWN);
        if (this.mName.contains(BLURPAPER)) {
            this.mIsHintShowing = !hintShown;
            showHint(!hintShown);
            this.mWidthScale = 1.0f;
            this.mHeightScale = 1.0f;
        }
        RelativeLayout layout = (RelativeLayout) this.mWindowContent.findViewById(34603238);
        int top = this.mContext.getApplicationContext().getResources().getDimensionPixelSize(17105443);
        ViewGroup.LayoutParams lp = layout.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            marginParams = (ViewGroup.MarginLayoutParams) lp;
        } else {
            marginParams = new ViewGroup.MarginLayoutParams(lp);
        }
        marginParams.setMargins(0, top, -2, -2);
        layout.setLayoutParams(marginParams);
        updateLocale();
    }

    private void updateWindowParams() {
        WindowManager.LayoutParams layoutParams = this.mWindowParams;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = this.mDefaultDisplayInfo.logicalWidth;
        this.mWindowParams.height = this.mDefaultDisplayInfo.logicalHeight;
        this.mWidth = (int) (((float) this.mWidth) * this.mWidthScale);
        this.mHeight = (int) (((float) this.mHeight) * this.mHeightScale);
    }

    /* access modifiers changed from: package-private */
    public boolean isSingleHandModeHintShown(String tag) {
        String value = Settings.Global.getString(this.mContext.getContentResolver(), tag);
        Slog.d(TAG, "tag " + tag + "value " + value);
        if (value == null || !value.equals(YES)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void handleImageView(boolean isVisible, boolean isVirtualNaviVisiable, boolean isFpTrikeyNaviVisiable) {
        ImageView imageView = (ImageView) this.mWindowContent.findViewById(34603464);
        if (imageView != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imageView.getLayoutParams();
            params.width = (this.mDefaultDisplayInfo.logicalWidth * 4) / 9;
            params.height = params.width;
            imageView.setLayoutParams(params);
        }
        show(imageView, isVirtualNaviVisiable);
        ImageView imageView2 = (ImageView) this.mWindowContent.findViewById(34603465);
        if (imageView2 != null) {
            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) imageView2.getLayoutParams();
            params2.width = (this.mDefaultDisplayInfo.logicalWidth * 4) / 9;
            params2.height = params2.width;
            imageView2.setLayoutParams(params2);
        }
        show(imageView2, isVirtualNaviVisiable);
        show((ImageView) this.mWindowContent.findViewById(34603466), isFpTrikeyNaviVisiable);
        setWindowTitle(isVisible);
        if (this.mIsWindowVisible) {
            this.mWindowManager.updateViewLayout(this.mWindowContent, this.mWindowParams);
        }
    }

    /* access modifiers changed from: package-private */
    public void handleOtherView(boolean isVisible) {
        boolean isFpTrikeyNaviVisiable = true;
        int i = 0;
        boolean isVirtualNaviVisiable = isVisible && !isFpTrikeyDevice();
        this.slideHintText1 = (TextView) this.mWindowContent.findViewById(34603461);
        show(this.slideHintText1, isVirtualNaviVisiable);
        this.slideHintText2 = (TextView) this.mWindowContent.findViewById(34603462);
        show(this.slideHintText2, isVirtualNaviVisiable);
        if (!isVisible || !isFpTrikeyDevice()) {
            isFpTrikeyNaviVisiable = false;
        }
        this.slideHintTextFp = (TextView) this.mWindowContent.findViewById(34603463);
        show(this.slideHintTextFp, isFpTrikeyNaviVisiable);
        LinearLayout viewSlideHint = (LinearLayout) this.mWindowContent.findViewById(34603166);
        if (viewSlideHint != null) {
            if (isVisible) {
                viewSlideHint.setVisibility(isVirtualNaviVisiable ? 0 : 8);
            } else {
                viewSlideHint.setVisibility(4);
            }
        }
        LinearLayout viewSlideHintFpNavi = (LinearLayout) this.mWindowContent.findViewById(34603467);
        if (viewSlideHintFpNavi != null) {
            if (isVisible) {
                if (!isFpTrikeyNaviVisiable) {
                    i = 8;
                }
                viewSlideHintFpNavi.setVisibility(i);
            } else {
                viewSlideHintFpNavi.setVisibility(4);
            }
        }
        handleImageView(isVisible, isVirtualNaviVisiable, isFpTrikeyNaviVisiable);
    }

    /* access modifiers changed from: private */
    public void showHint(boolean isVisible) {
        ImageView imageView;
        this.mImageView = (ImageView) this.mWindowContent.findViewById(34603162);
        ((FrameLayout) this.mWindowContent.findViewById(34603163)).setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wm.SingleHandWindow.AnonymousClass6 */

            public void onClick(View v) {
                SingleHandWindow.this.mImageView.performClick();
            }
        });
        if (!isVisible && (imageView = this.mImageView) != null) {
            imageView.setOnClickListener(this.mActionClickListener);
        }
        show(this.mImageView, !isVisible);
        if (!isVisible) {
            this.mWindowContent.setBackgroundColor(0);
        } else {
            this.mWindowContent.setBackgroundColor(ALP_BLACK_COLOR);
        }
        this.overlayDisplayWindow = (TextView) this.mWindowContent.findViewById(34603164);
        this.overlayGuideWindow = (TextView) this.mWindowContent.findViewById(34603406);
        if (!isSingleHandModeHintShown(SINGLE_HAND_MODE_GUIDE_SHOWN)) {
            show(this.overlayGuideWindow, !isVisible);
        } else {
            show(this.overlayGuideWindow, false);
        }
        show(this.overlayDisplayWindow, !isVisible);
        handleOtherView(isVisible);
    }

    private void setWindowTitle(boolean isVis) {
        if (isVis) {
            this.mIsHintShowing = true;
            this.mWindowParams.setTitle("hwSingleMode_windowbg_hint");
        } else {
            this.mIsHintShowing = false;
            if (this.mIsLeft) {
                this.mWindowParams.setTitle("hwSingleMode_windowbg_left");
            } else {
                this.mWindowParams.setTitle("hwSingleMode_windowbg_right");
            }
        }
        WindowManagerService windowManagerService = this.mService;
        windowManagerService.setLazyMode(windowManagerService.getLazyMode(), this.mIsHintShowing, this.mName);
    }

    /* access modifiers changed from: package-private */
    public boolean singlehandRegionContainsPoint(int x, int y) {
        int right;
        int left;
        int top = (int) (((float) this.mDefaultDisplayInfo.logicalHeight) * 0.25f);
        int bottom = this.mDefaultDisplayInfo.logicalHeight;
        if (this.mIsLeft) {
            left = 0;
            right = (int) (((float) this.mDefaultDisplayInfo.logicalWidth) * 0.75f);
        } else {
            left = (int) (((float) this.mDefaultDisplayInfo.logicalWidth) * 0.25f);
            right = this.mDefaultDisplayInfo.logicalWidth;
        }
        if (y < top || y >= bottom || x < left || x >= right) {
            return false;
        }
        return true;
    }

    public void onBlurWallpaperChanged() {
        View view = this.mWindowContent.findViewById(34603160);
        if (view != null && (view instanceof RelativeLayout)) {
            this.mRelateViewtop = (RelativeLayout) view;
            this.mRelateViewtop.setBackground(new BitmapDrawable(this.mRelateViewtop.getResources(), cropwallpaper(true)));
        }
        View view2 = this.mWindowContent.findViewById(34603161);
        if (view2 != null && (view2 instanceof RelativeLayout)) {
            this.mRelateViewbottom = (RelativeLayout) view2;
            this.mRelateViewbottom.setBackground(new BitmapDrawable(this.mRelateViewbottom.getResources(), cropwallpaper(false)));
        }
    }

    public static int getExpandLazyMode(int mode, boolean isHintShowing) {
        if (isHintShowing) {
            if (mode == 1) {
                return 3;
            }
            if (mode == 2) {
                return 4;
            }
        }
        return mode;
    }

    private boolean isFpTrikeyDevice() {
        return FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1;
    }
}
