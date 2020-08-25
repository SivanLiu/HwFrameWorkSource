package com.android.server.magicwin;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.IWallpaperManagerCallback;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.HwMwUtils;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.HwMultiWindowSplitUI;
import java.util.Locale;
import libcore.io.IoUtils;

public class HwMagicWindowUIController {
    private static final int ANIMATION_DELAY = 500;
    private static final int BITMAP_SCALSE = 4;
    private static final String BLACK_COLOR = "#000000";
    private static final int BLACK_TRANSPARENCY_30 = 1291845632;
    private static final int BLACK_TRANSPARENCY_70 = -16777216;
    private static final int BLUR_RADIS = 10;
    private static final float CIRCLE_ALPHA_1 = 0.5f;
    private static final float CIRCLE_ALPHA_2 = 1.0f;
    private static final float CIRCLE_ALPHA_3 = 0.0f;
    private static final int CIRCLE_ALPHA_TIME = 600;
    private static final float CIRCLE_SCALE_1 = 1.0f;
    private static final float CIRCLE_SCALE_2 = 0.75f;
    private static final float CIRCLE_SCALE_3 = 1.25f;
    private static final int CIRCLE_SCALE_TIME = 600;
    private static final float CIRCLE_TRANS_END = -64.0f;
    private static final float CIRCLE_TRANS_START = 0.0f;
    private static final int CIRCLE_TRANS_TIME = 1000;
    private static final int CONTENT_POSITION_ERROR = 0;
    private static final String CONTENT_RELPACE_LEFT_CHARACTER = "<a>";
    private static final String CONTENT_RELPACE_RIGHT_CHARACTER = "</a>";
    private static final float DIP2PX_REF = 0.5f;
    private static final int DURATION_ADD_SPLIT_BAR = 50;
    private static final int DURATION_BACKGROUND_ANIMATION = 200;
    private static final int DURATION_BACKGROUND_ANIMATION_FIRST_STEP = 150;
    private static final int DURATION_BACKGROUND_ANIMATION_SECOND_STEP = 50;
    private static final int DURATION_BACKGROUND_CHANGE = 250;
    private static final int DURATION_BACKGROUND_DELAY = 100;
    private static final long DURATION_DOUBLE_TO_SINGLE = 10;
    private static final long DURATION_SINGLE_TO_DOUBLE = 750;
    private static final float FULLY_TRANSPARENT = 0.0f;
    private static final String GESTURE_HOME_ANIMATOR = "gesture_home_animator";
    private static final float HALF_FACTOR = 2.0f;
    private static final int HOME_ANIMATION_END = 0;
    private static final int HOME_ANIMATION_START = 1;
    private static final int IS_REMINDER = 1;
    private static final int KEYWORD_DEFAULT_LENGHT = 3;
    private static final int KEYWORD_POSITION_ERROR = -1;
    private static final String KEY_NO_MORE_REMINDER = "key_no_more_reminder";
    private static final String LAUNCHER_PACKAGE_NAME = "com.huawei.android.launcher";
    private static final int MAGICWINDOW_WALLPAPER_DELAY_TIME = 250;
    private static final int MAGIC_WINDOW_TYPE = 103;
    private static final int MSG_DISMISS_DIALOG = 4;
    private static final int MSG_FORCE_UPDATE_SPLIT_BAR = 5;
    private static final int MSG_SET_WALLPAPER = 1;
    private static final int MSG_SHOW_DIALOG = 2;
    private static final int MSG_UPDATE_BG_COLOR = 6;
    private static final int MSG_UPDATE_DRAG_VIEW_VISIBILE = 3;
    private static final int MSG_UPDATE_WALLPAPER_VISIBILITY = 0;
    private static final int NOT_REMINDER = 0;
    private static final float OPAQUE = 1.0f;
    private static final int PAGE_ANIMATION_TIME = 600;
    private static final float PAGE_LEFT_SCALE_END = 0.95f;
    private static final float PAGE_LEFT_SCALE_START = 1.0f;
    private static final float PAGE_LEFT_TRANS_END = -52.0f;
    private static final float PAGE_LEFT_TRANS_START = 0.0f;
    private static final float PAGE_RIGHT_ALPHA_END = 1.0f;
    private static final float PAGE_RIGHT_ALPHA_START = 0.0f;
    private static final float PAGE_RIGHT_SCALE_END = 1.0f;
    private static final float PAGE_RIGHT_SCALE_START = 0.4f;
    private static final float PIVOT_COMPENSATION = 0.5f;
    private static final int RECENT_ANIMATION_CANCEL = -1;
    private static final String SETTINGS_INTNET_ACTION = "android.settings.MAGICWINDOW_SETTINGS";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "HwMagicWindowUIController";
    private static final int TAH_DELAY_ANIMATION = 1;
    private static final int TAH_ENTER_BACKGROUND_DURATION = 750;
    private static final int THEME_EMUI_DIALOG_ALERT = 33947691;
    private static final float TRANSPARENCY_30 = 0.3f;
    private static final float TRANSPARENCY_70 = 1.0f;
    private static final int UPDATE_SYSTEMUI_VISIBILITY = 1000;
    private ObjectAnimator alphaAnimator = null;
    private boolean isNeedUpdateWallPaperSize = false;
    /* access modifiers changed from: private */
    public FrameLayout mBackgroundLayout;
    /* access modifiers changed from: private */
    public ImageView mBackgroundView;
    /* access modifiers changed from: private */
    public Bitmap mBmpGauss = null;
    /* access modifiers changed from: private */
    public boolean mCheckBoxStatus = false;
    private ImageView mCircleIv = null;
    private ClickableSpan mClickableSpan = new ClickableSpan() {
        /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass5 */

        public void onClick(View view) {
            if (HwMagicWindowUIController.this.mDialog != null) {
                HwMagicWindowUIController.this.mDialog.dismiss();
            }
            Intent intent = new Intent(HwMagicWindowUIController.SETTINGS_INTNET_ACTION);
            intent.setPackage("com.android.settings");
            intent.addFlags(268435456);
            try {
                HwMagicWindowUIController.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException ex) {
                Slog.v(HwMagicWindowUIController.TAG, "startActivity failed! message : " + ex.getMessage());
            }
        }

        public void updateDrawState(TextPaint ds) {
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public AlertDialog mDialog;
    private DisplayMetrics mDisplayMetrics;
    /* access modifiers changed from: private */
    public HwMagicWindowService mHwMagicWinService = null;
    /* access modifiers changed from: private */
    public HwMultiWindowSplitUI mHwMultiWindowSplitUI = null;
    /* access modifiers changed from: private */
    public boolean mIsEnterMwWallpaperAnimating = false;
    /* access modifiers changed from: private */
    public boolean mIsMiddle = true;
    /* access modifiers changed from: private */
    public String mPackageName = null;
    private ImageView mPageLeftIv = null;
    private ImageView mPageRightIv = null;
    /* access modifiers changed from: private */
    public Handler mUIHandler = new Handler(ActivityThread.currentActivityThread().getLooper()) {
        /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass1 */

        public void handleMessage(Message msg) {
            boolean isDelayAnimation = false;
            switch (msg.what) {
                case 0:
                    boolean isGestureBackHome = msg.obj != null ? ((Boolean) msg.obj).booleanValue() : false;
                    if (msg.arg1 == 1) {
                        isDelayAnimation = true;
                    }
                    HwMagicWindowUIController hwMagicWindowUIController = HwMagicWindowUIController.this;
                    hwMagicWindowUIController.changeMagicWindowWallpaper(hwMagicWindowUIController.mIsMiddle, true, isGestureBackHome, isDelayAnimation);
                    return;
                case 1:
                    HwMagicWindowUIController.this.changeMagicWindowWallpaper(((Boolean) msg.obj).booleanValue(), false, false, false);
                    return;
                case 2:
                    HwMagicWindowUIController.this.showDialog((String) msg.obj);
                    return;
                case 3:
                    HwMagicWindowUIController.this.updateDragViewVisibility();
                    return;
                case 4:
                    if (HwMagicWindowUIController.this.mDialog != null && HwMagicWindowUIController.this.mDialog.isShowing()) {
                        HwMagicWindowUIController.this.mDialog.dismiss();
                    }
                    AlertDialog unused = HwMagicWindowUIController.this.mDialog = null;
                    return;
                case 5:
                    HwMagicWindowUIController.this.forceUpdateSplitBar(((Boolean) msg.obj).booleanValue());
                    return;
                case 6:
                    HwMagicWindowUIController.this.setBgColor();
                    return;
                default:
                    Slog.e(HwMagicWindowUIController.TAG, "msg.what error : " + msg.what);
                    return;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mVisible = false;
    private IWallpaperManagerCallback mWallpaperCallback;
    private WindowManager mWindowManager;

    public HwMagicWindowUIController(HwMagicWindowService service, Context context, ActivityTaskManagerService atms) {
        this.mContext = context;
        this.mHwMagicWinService = service;
        this.mWallpaperCallback = new WallpaperCallback(service);
        HwMultiWindowSplitUI hwMultiWindowSplitUI = this.mHwMultiWindowSplitUI;
        this.mHwMultiWindowSplitUI = HwMultiWindowSplitUI.getInstance(context, atms);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(GESTURE_HOME_ANIMATOR), false, new ContentObserver(this.mUIHandler) {
            /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass2 */

            public void onChange(boolean selfChange) {
                boolean isTopAPPInMagicwinMode = HwMagicWindowUIController.this.mHwMagicWinService.getAmsPolicy().isStackInHwMagicWindowMode();
                int animStatus = Settings.Secure.getInt(HwMagicWindowUIController.this.mContext.getContentResolver(), HwMagicWindowUIController.GESTURE_HOME_ANIMATOR, 0);
                boolean isAnimBeginWhenBgVisible = animStatus == 1 && HwMagicWindowUIController.this.mVisible;
                boolean isAnimCancleWhenBgInVisible = animStatus == -1 && !HwMagicWindowUIController.this.mVisible;
                if (isTopAPPInMagicwinMode && isAnimBeginWhenBgVisible) {
                    boolean unused = HwMagicWindowUIController.this.mVisible = false;
                    Message msg = HwMagicWindowUIController.this.mUIHandler.obtainMessage(0);
                    msg.obj = true;
                    HwMagicWindowUIController.this.mUIHandler.sendMessage(msg);
                }
                if (isTopAPPInMagicwinMode && isAnimCancleWhenBgInVisible) {
                    boolean unused2 = HwMagicWindowUIController.this.mVisible = true;
                    HwMagicWindowUIController.this.mUIHandler.sendEmptyMessage(0);
                }
                if (animStatus == 1) {
                    Settings.Secure.putInt(HwMagicWindowUIController.this.mContext.getContentResolver(), HwMagicWindowUIController.GESTURE_HOME_ANIMATOR, 0);
                }
            }
        });
        getDisplayMetrics();
    }

    private void getDisplayMetrics() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        Display display = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        display.getRealMetrics(this.mDisplayMetrics);
    }

    /* access modifiers changed from: private */
    public void creatMagicWindowBg() {
        this.mBackgroundLayout = (FrameLayout) LayoutInflater.from(this.mContext).inflate(34013415, (ViewGroup) null);
        FrameLayout frameLayout = this.mBackgroundLayout;
        if (frameLayout == null) {
            Slog.w(TAG, "mBackgroundLayout = null");
            return;
        }
        frameLayout.setVisibility(this.mVisible ? 0 : 4);
        WindowManager.LayoutParams mLp = new WindowManager.LayoutParams();
        mLp.setTitle("MagicWindow");
        mLp.type = 2103;
        int width = this.mDisplayMetrics.widthPixels;
        int height = this.mDisplayMetrics.heightPixels;
        if (HwMwUtils.IS_TABLET && height > width) {
            width = this.mDisplayMetrics.heightPixels;
            height = this.mDisplayMetrics.widthPixels;
        }
        mLp.width = width;
        mLp.height = height;
        mLp.flags = 264;
        mLp.format = -3;
        mLp.privateFlags |= 80;
        mLp.gravity = 8388659;
        this.mBackgroundLayout.setSystemUiVisibility(5894);
        this.mWindowManager.addView(this.mBackgroundLayout, mLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
        this.mBackgroundView = new ImageView(this.mContext);
        this.mBackgroundLayout.addView(this.mBackgroundView, lp);
    }

    public void whetherShowDialog(String packageName) {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            boolean isNeedShowForPackage = true;
            boolean hasReminder = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_NO_MORE_REMINDER, 0, ActivityManager.getCurrentUser()) == 1;
            AlertDialog alertDialog = this.mDialog;
            boolean isDialogShowing = alertDialog != null && alertDialog.isShowing();
            if (packageName == null || this.mHwMagicWinService.getDialogShownForApp(packageName) || !this.mHwMagicWinService.getHwMagicWinEnabled(packageName)) {
                isNeedShowForPackage = false;
            }
            if (!hasReminder && !isDialogShowing && isNeedShowForPackage) {
                Message msg = this.mUIHandler.obtainMessage();
                msg.what = 2;
                msg.obj = packageName;
                this.mUIHandler.sendMessage(msg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void showDialog(String packageName) {
        this.mPackageName = packageName;
        if (this.mHwMagicWinService.getAppSupportMode(this.mPackageName) != 0) {
            AlertDialog alertDialog = this.mDialog;
            if (!(alertDialog != null && alertDialog.isShowing())) {
                AlertDialog.Builder buider = new AlertDialog.Builder(this.mContext, THEME_EMUI_DIALOG_ALERT);
                ScrollView magicWinTipsViewRoot = (ScrollView) LayoutInflater.from(buider.getContext()).inflate(34013416, (ViewGroup) null);
                if (magicWinTipsViewRoot != null) {
                    this.mCircleIv = (ImageView) magicWinTipsViewRoot.findViewById(34603500);
                    this.mPageLeftIv = (ImageView) magicWinTipsViewRoot.findViewById(34603502);
                    this.mPageRightIv = (ImageView) magicWinTipsViewRoot.findViewById(34603503);
                    TextView goTv = (TextView) magicWinTipsViewRoot.findViewById(34603501);
                    CheckBox cb = (CheckBox) magicWinTipsViewRoot.findViewById(34603499);
                    this.mDialog = buider.setCancelable(false).setView(magicWinTipsViewRoot).setPositiveButton(33685961, new DialogInterface.OnClickListener() {
                        /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass3 */

                        public void onClick(DialogInterface dialog, int which) {
                            if (HwMagicWindowUIController.this.mCheckBoxStatus) {
                                Settings.System.putIntForUser(HwMagicWindowUIController.this.mContext.getContentResolver(), HwMagicWindowUIController.KEY_NO_MORE_REMINDER, 1, ActivityManager.getCurrentUser());
                            }
                            HwMagicWindowUIController.this.mHwMagicWinService.setDialogShownForApp(HwMagicWindowUIController.this.mPackageName, true);
                        }
                    }).create();
                    if (this.mCircleIv != null && this.mPageLeftIv != null && this.mPageRightIv != null && goTv != null && cb != null && this.mDialog != null) {
                        goTv.setHighlightColor(this.mContext.getResources().getColor(17170445));
                        setClickableSpanForTextView(goTv, getKeywords(goTv.getText().toString()), this.mClickableSpan, this.mContext);
                        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass4 */

                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                boolean unused = HwMagicWindowUIController.this.mCheckBoxStatus = isChecked;
                            }
                        });
                        this.mDialog.getWindow().getAttributes().privateFlags |= 16;
                        this.mDialog.getWindow().setType(2003);
                        this.mDialog.getWindow().getAttributes().setTitle("MagicWindowGuideDialog");
                        this.mCheckBoxStatus = true;
                        this.mDialog.show();
                        startDialogAnimation(this.mCircleIv, this.mPageLeftIv, this.mPageRightIv);
                    }
                }
            }
        } else if (!this.mHwMagicWinService.getDialogShownForApp(this.mPackageName)) {
            Toast toast = Toast.makeText(this.mContext, 33685970, 0);
            toast.getWindowParams().privateFlags |= 16;
            toast.show();
            this.mHwMagicWinService.setDialogShownForApp(this.mPackageName, true);
        }
    }

    public void dismissDialog() {
        this.mUIHandler.sendMessage(this.mUIHandler.obtainMessage(4));
    }

    private void startDialogAnimation(ImageView circleIv, ImageView pageLeftIv, ImageView pageRightIv) {
        ObjectAnimator circleIvTranslate = ObjectAnimator.ofFloat(circleIv, "translationY", 0.0f, (float) dip2px(CIRCLE_TRANS_END));
        ObjectAnimator circleIvScaleX = ObjectAnimator.ofFloat(circleIv, "scaleX", 1.0f, 0.75f, CIRCLE_SCALE_3);
        ObjectAnimator circleIvScaleY = ObjectAnimator.ofFloat(circleIv, "scaleY", 1.0f, 0.75f, CIRCLE_SCALE_3);
        circleIvTranslate.setDuration(1000L);
        circleIvScaleX.setDuration(600L);
        circleIvScaleY.setDuration(600L);
        ObjectAnimator circleIvAlpha = ObjectAnimator.ofFloat(circleIv, "alpha", 0.5f, 1.0f, 0.0f);
        circleIvAlpha.setDuration(600L);
        ObjectAnimator pageLeftIvTranslate = ObjectAnimator.ofFloat(pageLeftIv, "translationX", 0.0f, (float) dip2px(PAGE_LEFT_TRANS_END));
        ObjectAnimator pageLeftIvScaleX = ObjectAnimator.ofFloat(pageLeftIv, "scaleX", 1.0f, PAGE_LEFT_SCALE_END);
        ObjectAnimator pageLeftIvScaleY = ObjectAnimator.ofFloat(pageLeftIv, "scaleY", 1.0f, PAGE_LEFT_SCALE_END);
        pageLeftIvTranslate.setDuration(600L);
        pageLeftIvScaleX.setDuration(600L);
        pageLeftIvScaleY.setDuration(600L);
        ObjectAnimator pageRightIvAlpha = ObjectAnimator.ofFloat(pageRightIv, "alpha", 0.0f, 1.0f);
        ObjectAnimator pageRightIvScaleX = ObjectAnimator.ofFloat(pageRightIv, "scaleX", 0.4f, 1.0f);
        ObjectAnimator pageRightIvScaleY = ObjectAnimator.ofFloat(pageRightIv, "scaleY", 0.4f, 1.0f);
        pageRightIvAlpha.setDuration(600L);
        pageRightIvScaleX.setDuration(600L);
        pageRightIvScaleY.setDuration(600L);
        AnimatorSet animSetPage = new AnimatorSet();
        animSetPage.play(pageLeftIvTranslate).with(pageLeftIvScaleX).with(pageLeftIvScaleY).with(pageRightIvAlpha).with(pageRightIvScaleX).with(pageRightIvScaleY);
        AnimatorSet animSetCircle = new AnimatorSet();
        animSetCircle.play(circleIvScaleX).with(circleIvScaleY).with(circleIvAlpha).after(circleIvTranslate).before(animSetPage).after(500);
        animSetCircle.start();
    }

    private int dip2px(float dpValue) {
        return (int) ((dpValue * this.mContext.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private String getKeywords(String information) {
        int keywordStartPosition = information.indexOf(CONTENT_RELPACE_LEFT_CHARACTER);
        int keywordEndPosition = information.indexOf(CONTENT_RELPACE_RIGHT_CHARACTER);
        if (keywordStartPosition == -1 || keywordEndPosition == -1 || keywordEndPosition < keywordStartPosition) {
            return "";
        }
        return information.substring(keywordStartPosition + 3, keywordEndPosition);
    }

    private void setClickableSpanForTextView(TextView tv, String linkStr, ClickableSpan clickableSpan, Context mContext2) {
        if (tv != null && !TextUtils.isEmpty(linkStr)) {
            String content = tv.getText().toString();
            Locale defaultLocale = Locale.getDefault();
            if (!content.toLowerCase(defaultLocale).contains(linkStr.toLowerCase(defaultLocale))) {
                content = content + " " + linkStr;
            }
            String content2 = content.replaceAll(CONTENT_RELPACE_LEFT_CHARACTER, "").replaceAll(CONTENT_RELPACE_RIGHT_CHARACTER, "");
            int start = content2.toLowerCase(defaultLocale).lastIndexOf(linkStr.toLowerCase(defaultLocale));
            int end = linkStr.length() + start;
            if (start >= 0 && start < end && end <= content2.length()) {
                SpannableStringBuilder sp = new SpannableStringBuilder(content2);
                sp.setSpan(clickableSpan, start, end, 33);
                sp.setSpan(new ForegroundColorSpan(mContext2.getResources().getColor(33882525)), start, end, 34);
                tv.setText(sp);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private void updateWallpaperSize() {
        WindowManager.LayoutParams layoutParams;
        FrameLayout frameLayout = this.mBackgroundLayout;
        if (frameLayout != null && (layoutParams = (WindowManager.LayoutParams) frameLayout.getLayoutParams()) != null) {
            layoutParams.width = this.mDisplayMetrics.widthPixels;
            layoutParams.height = this.mDisplayMetrics.heightPixels;
            this.mWindowManager.updateViewLayout(this.mBackgroundLayout, layoutParams);
        }
    }

    public void setNeedUpdateWallpaperSize(boolean isUpdateWallPaper) {
        this.isNeedUpdateWallPaperSize = isUpdateWallPaper;
    }

    public void changeWallpaper(boolean isMiddle) {
        if (isMiddle != this.mIsMiddle) {
            Message msg = this.mUIHandler.obtainMessage(1);
            msg.obj = Boolean.valueOf(isMiddle);
            this.mUIHandler.removeMessages(1);
            this.mUIHandler.sendMessage(msg);
        } else if (isMiddle) {
            updateSplitBarVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void changeMagicWindowWallpaper(boolean isMiddle, boolean changeVisible, boolean isGestureBackHome, boolean isDelayAnimation) {
        this.mIsMiddle = isMiddle;
        if (this.mBackgroundView == null) {
            creatMagicWindowBg();
        }
        boolean z = false;
        if (this.isNeedUpdateWallPaperSize) {
            Slog.i(TAG, "need change magicwindow wallpaper size");
            updateWallpaperSize();
            this.isNeedUpdateWallPaperSize = false;
        }
        if ((changeVisible || !this.mVisible) && this.mBackgroundView != null) {
            if (this.mVisible) {
                if (this.mHwMagicWinService.getAmsPolicy().getActvityByPosition(2) == null) {
                    z = true;
                }
                this.mIsMiddle = z;
            }
            startBackgroundVisibleAnimation(this.mVisible, this.mIsMiddle, isGestureBackHome, isDelayAnimation);
        } else if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
        } else {
            if (isMiddle) {
                startAlphaChangeAnimator(1.0f, TRANSPARENCY_30, DURATION_DOUBLE_TO_SINGLE);
            } else {
                startAlphaChangeAnimator(TRANSPARENCY_30, 1.0f, DURATION_SINGLE_TO_DOUBLE);
            }
        }
    }

    public void updateSplitBarVisibility(boolean isVisible) {
        Message msg = this.mUIHandler.obtainMessage(5);
        msg.obj = Boolean.valueOf(isVisible);
        this.mUIHandler.removeMessages(5);
        if (!isVisible) {
            this.mUIHandler.sendMessage(msg);
        } else {
            this.mUIHandler.sendMessageDelayed(msg, 50);
        }
    }

    /* access modifiers changed from: private */
    public void forceUpdateSplitBar(boolean isVisible) {
        if (!isVisible) {
            Slog.i(TAG, "force update, remove split bar.");
            this.mHwMultiWindowSplitUI.removeSplit(103, false);
        } else if (shouldAddSplitBar()) {
            Slog.i(TAG, "force update, add split bar.");
            this.mHwMultiWindowSplitUI.addDividerBarWindow(0, 103);
        }
    }

    private boolean shouldAddSplitBar() {
        if (!this.mHwMagicWinService.getConfig().isDragable(this.mHwMagicWinService.getAmsPolicy().getFocusedStackPackageName()) || !this.mHwMagicWinService.getAmsPolicy().isInHwDoubleWindow()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void updateDragViewVisibility() {
        if (shouldAddSplitBar()) {
            Slog.i(TAG, "update Visibility, add split bar.");
            this.mHwMultiWindowSplitUI.addDividerBarWindow(0, 103);
        } else if (!this.mHwMagicWinService.getAmsPolicy().isInHwDoubleWindow()) {
            Slog.i(TAG, "update Visibility, remove split bar.");
            this.mHwMultiWindowSplitUI.removeSplit(103, false);
        }
    }

    private void startAlphaChangeAnimator(float from, float to, long time) {
        if (this.mBackgroundView != null) {
            ObjectAnimator objectAnimator = this.alphaAnimator;
            if (objectAnimator != null && objectAnimator.isRunning()) {
                this.alphaAnimator.end();
            }
            this.alphaAnimator = ObjectAnimator.ofFloat(this.mBackgroundView, "alpha", from, to);
            this.alphaAnimator.setDuration(time);
            this.alphaAnimator.start();
        }
    }

    public void getWallpaperBitmap() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this.mContext);
        if (wallpaperManager == null || wallpaperManager.getIWallpaperManager() == null) {
            Slog.e(TAG, "wallpaperManager is a null object or wallpaperManager.getIWallpaperManager() = null ");
            return;
        }
        try {
            ParcelFileDescriptor fd = wallpaperManager.getIWallpaperManager().getBlurWallpaper(this.mWallpaperCallback);
            if (fd == null) {
                Slog.e(TAG, "getBlurWallpaper(), fd = null");
                return;
            }
            try {
                this.mBmpGauss = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, new BitmapFactory.Options());
            } catch (OutOfMemoryError e) {
                Slog.w(TAG, "Can't decode file", e);
            } finally {
                IoUtils.closeQuietly(fd);
            }
        } catch (RemoteException re) {
            Slog.w(TAG, "Can't getWallpaper", re);
        }
    }

    public Bitmap getWallpaperScreenShot() {
        int color;
        Bitmap bitmap = this.mBmpGauss;
        if (bitmap == null) {
            Slog.w(TAG, "getWallpaperScreenShot failed, cause mBmpGauss is null!");
            return null;
        }
        Bitmap wallpaperScreenShot = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            color = BLACK_TRANSPARENCY_70;
        } else {
            color = this.mIsMiddle ? BLACK_TRANSPARENCY_30 : BLACK_TRANSPARENCY_70;
        }
        return getBlurAndBlackWallpaper(wallpaperScreenShot, color);
    }

    private Bitmap getBlurAndBlackWallpaper(Bitmap bitmap, int color) {
        if (bitmap == null) {
            return bitmap;
        }
        Bitmap bm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bm);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
        canvas.drawColor(color);
        return bm;
    }

    public void setWallpaperToVisiableForTah() {
        if (!this.mVisible) {
            if (this.mBmpGauss == null) {
                Slog.w(TAG, "updateMagicWindowWallpaperVisibility(), bmpGauss = null");
                this.mHwMagicWinService.mHandler.sendEmptyMessage(1);
            }
            this.mVisible = true;
            Slog.d(TAG, "setWallpaperToVisiableForTah to visalbe");
            this.mUIHandler.post(new Runnable() {
                /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass6 */

                public void run() {
                    if (HwMagicWindowUIController.this.mBackgroundView == null) {
                        HwMagicWindowUIController.this.creatMagicWindowBg();
                    }
                    HwMagicWindowUIController.this.mBackgroundView.clearAnimation();
                    HwMagicWindowUIController.this.mBackgroundView.setBackgroundColor(0);
                    HwMagicWindowUIController.this.mBackgroundLayout.setBackgroundColor(0);
                    HwMagicWindowUIController.this.mBackgroundLayout.setVisibility(0);
                }
            });
            Message message = this.mUIHandler.obtainMessage(0);
            message.arg1 = 1;
            this.mUIHandler.sendMessageDelayed(message, 250);
        }
    }

    public void updateMagicWindowWallpaperVisibility(Boolean isVisible) {
        if (this.mVisible != isVisible.booleanValue()) {
            if (this.mBmpGauss == null) {
                Slog.w(TAG, "updateMagicWindowWallpaperVisibility(), bmpGauss = null");
                this.mHwMagicWinService.mHandler.sendEmptyMessage(1);
            }
            this.mVisible = isVisible.booleanValue();
            Slog.d(TAG, "updateMagicWindowWallpaperVisibility, isVisible = " + isVisible);
            this.mUIHandler.sendEmptyMessage(0);
        }
    }

    public void hideMwWallpaperInNeed() {
        FrameLayout frameLayout = this.mBackgroundLayout;
        if (frameLayout != null && frameLayout.getVisibility() == 0) {
            this.mVisible = true;
            updateMagicWindowWallpaperVisibility(false);
        }
    }

    private void startBackgroundVisibleAnimation(boolean visible, final boolean isMiddle, boolean isGestureBackHome, boolean isDelayAnimation) {
        ObjectAnimator animator;
        if (visible) {
            this.mIsEnterMwWallpaperAnimating = true;
            this.mBackgroundLayout.setVisibility(0);
            animator = ObjectAnimator.ofFloat(this.mBackgroundView, "alpha", 0.0f, 1.0f);
            animator.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 34078724));
            this.mBackgroundLayout.setBackground(null);
            changeBackGroundColorIfNeed(isMiddle, true);
            this.mBackgroundView.setAlpha(1.0f);
            animator.addListener(new Animator.AnimatorListener() {
                /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass7 */

                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    boolean unused = HwMagicWindowUIController.this.mIsEnterMwWallpaperAnimating = false;
                    if (!HwMagicWindowUIController.this.isNeedChangeBgColor()) {
                        HwMagicWindowUIController.this.mUIHandler.sendEmptyMessage(3);
                        HwMagicWindowUIController.this.mBackgroundLayout.setBackground(new BitmapDrawable(HwMagicWindowUIController.this.mBmpGauss));
                        HwMagicWindowUIController.this.mBackgroundView.setBackgroundColor(Color.parseColor(HwMagicWindowUIController.BLACK_COLOR));
                        float f = 1.0f;
                        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                            HwMagicWindowUIController.this.mBackgroundView.setAlpha(1.0f);
                            return;
                        }
                        ImageView access$1300 = HwMagicWindowUIController.this.mBackgroundView;
                        if (isMiddle) {
                            f = HwMagicWindowUIController.TRANSPARENCY_30;
                        }
                        access$1300.setAlpha(f);
                        return;
                    }
                    HwMagicWindowUIController.this.setBgColor();
                    HwMagicWindowUIController.this.startBackgroundVisibleAnimationWhite();
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else {
            String focusPackageName = this.mHwMagicWinService.getAmsPolicy().getFocusedStackPackageName();
            if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || ((focusPackageName == null || focusPackageName.indexOf("com.huawei.android.launcher") < 0) && !isGestureBackHome)) {
                animator = ObjectAnimator.ofFloat(this.mBackgroundView, "alpha", 1.0f, 0.0f);
                this.mBackgroundLayout.setBackground(null);
                changeBackGroundColorIfNeed(isMiddle, false);
                this.mBackgroundView.setAlpha(1.0f);
                animator.addListener(new Animator.AnimatorListener() {
                    /* class com.android.server.magicwin.HwMagicWindowUIController.AnonymousClass8 */

                    public void onAnimationStart(Animator animation) {
                        Slog.i(HwMagicWindowUIController.TAG, "wallpaper changed, will remove split bar.");
                        HwMagicWindowUIController.this.mHwMultiWindowSplitUI.removeSplit(103, false);
                    }

                    public void onAnimationEnd(Animator animation) {
                        if (!HwMagicWindowUIController.this.mIsEnterMwWallpaperAnimating) {
                            HwMagicWindowUIController.this.mBackgroundLayout.setVisibility(4);
                        }
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            } else {
                this.mBackgroundLayout.setBackground(null);
                if (isNeedChangeBgColor()) {
                    this.mBackgroundView.setAlpha(1.0f);
                } else {
                    this.mBackgroundView.setAlpha(0.0f);
                }
                this.mBackgroundLayout.setVisibility(4);
                return;
            }
        }
        int duration = (!visible || !isDelayAnimation) ? 200 : TAH_ENTER_BACKGROUND_DURATION;
        if (visible && isNeedChangeBgColor()) {
            duration = 150;
        }
        animator.setDuration((long) duration);
        animator.start();
    }

    /* access modifiers changed from: private */
    public void startBackgroundVisibleAnimationWhite() {
        int setColor = getSplitBgColor();
        if (setColor != -1) {
            TransitionDrawable trans = new TransitionDrawable(new BitmapDrawable[]{new BitmapDrawable(getBlurAndBlackWallpaper(this.mBmpGauss, BLACK_TRANSPARENCY_70)), new BitmapDrawable(getBlurAndBlackWallpaper(this.mBmpGauss, setColor))});
            this.mBackgroundView.setBackgroundDrawable(trans);
            this.mBackgroundView.setAlpha(1.0f);
            trans.startTransition(50);
        }
    }

    public void updateBgColor() {
        Message msg = this.mUIHandler.obtainMessage(6);
        this.mUIHandler.removeMessages(6);
        this.mUIHandler.sendMessage(msg);
    }

    private void changeBackGroundColorIfNeed(boolean isMiddle, boolean isForceToBlack) {
        int color;
        if (!isNeedChangeBgColor() || isForceToBlack) {
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                color = BLACK_TRANSPARENCY_70;
            } else {
                color = isMiddle ? BLACK_TRANSPARENCY_30 : BLACK_TRANSPARENCY_70;
            }
            this.mBackgroundView.setBackground(new BitmapDrawable(getBlurAndBlackWallpaper(this.mBmpGauss, color)));
            return;
        }
        setBgColor();
    }

    private int getSplitBgColor() {
        String pkgName = this.mHwMagicWinService.getAmsPolicy().getFocusedStackPackageName();
        if (this.mBackgroundView == null || !this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName)) {
            return -1;
        }
        HwMagicWindowService hwMagicWindowService = this.mHwMagicWinService;
        if (hwMagicWindowService.isInAppSplitWinMode(hwMagicWindowService.getAmsPolicy().getTopActivity())) {
            Slog.d(TAG, "update wallpaper for split window");
            return this.mHwMagicWinService.getConfig().getSplitLineBgColor(pkgName);
        }
        Slog.d(TAG, "update wallpaper for normal magic");
        return this.mHwMagicWinService.getConfig().getSplitBarBgColor(pkgName);
    }

    /* access modifiers changed from: private */
    public void setBgColor() {
        int setColor = getSplitBgColor();
        if (setColor != -1 && !this.mIsEnterMwWallpaperAnimating) {
            this.mBackgroundView.setBackground(new BitmapDrawable(getBlurAndBlackWallpaper(this.mBmpGauss, setColor)));
            this.mBackgroundView.setAlpha(1.0f);
        }
    }

    private class WallpaperCallback extends IWallpaperManagerCallback.Stub {
        private HwMagicWindowService mMws;

        WallpaperCallback(HwMagicWindowService service) {
            this.mMws = service;
        }

        public void onWallpaperChanged() throws RemoteException {
        }

        public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) throws RemoteException {
        }

        public void onBlurWallpaperChanged() throws RemoteException {
            if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
                Slog.d(HwMagicWindowUIController.TAG, "onBlurWallpaperChanged()");
            }
            this.mMws.mHandler.sendEmptyMessage(1);
        }
    }

    /* access modifiers changed from: private */
    public boolean isNeedChangeBgColor() {
        String focusPackageName = this.mHwMagicWinService.getAmsPolicy().getFocusedStackPackageName();
        int splitLineBgColor = this.mHwMagicWinService.getConfig().getSplitLineBgColor(focusPackageName);
        int splitBarBgColor = this.mHwMagicWinService.getConfig().getSplitBarBgColor(focusPackageName);
        if ((splitLineBgColor == -1 && splitBarBgColor == -1) || !this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(focusPackageName)) {
            return false;
        }
        Slog.d(TAG, "isNeedChangeBgColor for special cast");
        return true;
    }
}
