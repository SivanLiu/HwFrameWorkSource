package huawei.com.android.server.fingerprint;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.android.server.fingerprint.fingerprintAnimation.WaterEffectView;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.systemui.shared.system.MetricsLoggerCompat;

public class HighLightMaskView extends FrameLayout {
    private static final String TAG = "HighLightMaskView";
    private static int[] sSampleAlpha = new int[]{234, 229, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED, 211, HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, 205, 187, 176, HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW, 163, 159, CPUFeature.MSG_SET_BG_UIDS, 140, 140, CPUFeature.MSG_SET_CPUSETCONFIG_VR, 121, HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START, 101, 92, 81, 81, 69, 68, 58, 56, 46, 44, 35, 34, 30, 22, 23, 18, 0, 0};
    private static int[] sSampleBrightness = new int[]{4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 30, 34, 40, 46, 50, 56, 64, 74, 84, 94, 104, 114, 124, 134, CPUFeature.MSG_RESET_VIP_THREAD, CPUFeature.MSG_RESET_ON_FIRE, 164, 174, 184, 194, 204, HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED, MetricsLoggerCompat.OVERVIEW_ACTIVITY, 234, 244, 248, 255};
    private View mBlackMaskView;
    private int mCenterX;
    private int mCenterY;
    private View mCircleMaskView;
    private int mCircleOnDrawCounter;
    private int mHighLightShowType = -1;
    private boolean mIsCircleViewVisible;
    private final Paint mPaint = new Paint(1);
    private String mPkgName;
    private int mRadius;
    private float mScale;
    private WaterEffectView mWaterEffectView;

    public HighLightMaskView(Context context, int brightness, int radius) {
        super(context);
        init(context, brightness, radius);
    }

    public void setAlpha(int alpha) {
        if (this.mBlackMaskView != null) {
            this.mBlackMaskView.getBackground().setAlpha(alpha);
        }
    }

    public float getAlpha() {
        if (this.mBlackMaskView != null) {
            return (float) this.mBlackMaskView.getBackground().getAlpha();
        }
        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (FingerViewController.PKGNAME_OF_KEYGUARD.equals(this.mPkgName) && 1 == this.mHighLightShowType) {
            this.mWaterEffectView.onResume();
            this.mWaterEffectView.playAnim(((float) this.mCenterX) * this.mScale, ((float) this.mCenterY) * this.mScale);
        }
        this.mCircleOnDrawCounter = 0;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (FingerViewController.PKGNAME_OF_KEYGUARD.equals(this.mPkgName) && 1 == this.mHighLightShowType) {
            this.mWaterEffectView.onPause();
        }
        this.mCircleOnDrawCounter = 0;
    }

    private void init(Context context, int brightness, int radius) {
        this.mPaint.setDither(true);
        this.mPaint.setStyle(Style.FILL);
        this.mPaint.setColor(-16711681);
        this.mPaint.setMaskFilter(new BlurMaskFilter(20.0f, Blur.SOLID));
        this.mRadius = radius;
        this.mWaterEffectView = new WaterEffectView(context);
        addView(this.mWaterEffectView, -1, -1);
        this.mBlackMaskView = new View(this.mContext);
        this.mBlackMaskView.setBackgroundColor(-16777216);
        int alphaNew = 0;
        if (brightness > 0) {
            alphaNew = getMaskAlpha(brightness);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("alphaNew:");
        stringBuilder.append(alphaNew);
        stringBuilder.append("radius = ");
        stringBuilder.append(radius);
        Log.i(str, stringBuilder.toString());
        this.mBlackMaskView.getBackground().setAlpha(alphaNew);
        addView(this.mBlackMaskView, -1, -1);
        this.mCircleMaskView = new View(this.mContext) {
            public void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                Log.i(HighLightMaskView.TAG, " mCircleMaskView onDraw");
                canvas.drawCircle(((float) HighLightMaskView.this.mCenterX) * HighLightMaskView.this.mScale, ((float) HighLightMaskView.this.mCenterY) * HighLightMaskView.this.mScale, ((float) HighLightMaskView.this.mRadius) * HighLightMaskView.this.mScale, HighLightMaskView.this.mPaint);
                if (HighLightMaskView.this.mHighLightShowType == 1) {
                    if (HighLightMaskView.this.mCircleOnDrawCounter == 1 && HighLightMaskView.this.mIsCircleViewVisible) {
                        Log.i(HighLightMaskView.TAG, " mCircleMaskView notifyCaptureImage 1");
                        FingerViewController.getInstance(this.mContext).notifyCaptureImage();
                        HighLightMaskView.this.mCircleOnDrawCounter = 0;
                        return;
                    }
                    HighLightMaskView.this.mCircleOnDrawCounter = HighLightMaskView.this.mCircleOnDrawCounter + 1;
                } else if (HighLightMaskView.this.mIsCircleViewVisible) {
                    Log.i(HighLightMaskView.TAG, " mCircleMaskView notifyCaptureImage 2");
                    FingerViewController.getInstance(this.mContext).notifyCaptureImage();
                }
            }
        };
        addView(this.mCircleMaskView, -1, -1);
    }

    public int getMaskAlpha(int currentLight) {
        int alpha = 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" currentLight:");
        stringBuilder.append(currentLight);
        Log.d(str, stringBuilder.toString());
        if (currentLight > sSampleBrightness[sSampleBrightness.length - 1]) {
            return 0;
        }
        int i = 0;
        while (i < sSampleBrightness.length) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" sSampleBrightness :[");
            stringBuilder2.append(i);
            stringBuilder2.append("]");
            stringBuilder2.append(sSampleBrightness[i]);
            Log.d(str2, stringBuilder2.toString());
            if (currentLight == sSampleBrightness[i]) {
                alpha = sSampleAlpha[i];
                break;
            } else if (currentLight >= sSampleBrightness[i]) {
                i++;
            } else if (i == 0) {
                alpha = sSampleAlpha[0];
            } else {
                alpha = queryAlphaImpl(currentLight, sSampleBrightness[i - 1], sSampleAlpha[i - 1], sSampleBrightness[i], sSampleAlpha[i]);
            }
        }
        if (alpha > sSampleAlpha[0] || alpha < 0) {
            alpha = 0;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(" alpha:");
        stringBuilder.append(alpha);
        Log.d(str, stringBuilder.toString());
        return alpha;
    }

    private int queryAlphaImpl(int currLight, int preLevelLight, int preLevelAlpha, int lastLevelLight, int lastLevelAlpha) {
        return (((currLight - preLevelLight) * (lastLevelAlpha - preLevelAlpha)) / (lastLevelLight - preLevelLight)) + preLevelAlpha;
    }

    public void setCenterPoints(int centerX, int centerY) {
        this.mCenterX = centerX;
        this.mCenterY = centerY;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCenterX = ");
        stringBuilder.append(this.mCenterX);
        stringBuilder.append(",mCenterY=");
        stringBuilder.append(this.mCenterY);
        Log.d(str, stringBuilder.toString());
    }

    public void setScale(float scale) {
        this.mScale = scale;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mScale = ");
        stringBuilder.append(this.mScale);
        Log.d(str, stringBuilder.toString());
    }

    public void setCircleVisibility(int visibility) {
        if (this.mCircleMaskView != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" mCircleMaskView setVisibility:");
            stringBuilder.append(visibility);
            Log.i(str, stringBuilder.toString());
            if (visibility == 0 || this.mCircleMaskView.getVisibility() != visibility) {
                this.mCircleMaskView.setVisibility(visibility);
                if (visibility == 0) {
                    this.mIsCircleViewVisible = true;
                } else {
                    Log.i(TAG, " mCircleMaskView notifyDismissBlueSpot ");
                    FingerViewController.getInstance(this.mContext).notifyDismissBlueSpot();
                    this.mIsCircleViewVisible = false;
                }
            } else {
                Log.i(TAG, " visibility is already INVISIBLE skip");
            }
        }
    }

    public int getCircleVisibility() {
        if (this.mCircleMaskView != null) {
            return this.mCircleMaskView.getVisibility();
        }
        return 4;
    }

    public void setType(int type) {
        this.mHighLightShowType = type;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mHighLightShowType = ");
        stringBuilder.append(this.mScale);
        Log.i(str, stringBuilder.toString());
    }

    public void setPackageName(String pkgName) {
        this.mPkgName = pkgName;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPkgName = ");
        stringBuilder.append(this.mPkgName);
        Log.i(str, stringBuilder.toString());
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaint.setXfermode(null);
    }
}
