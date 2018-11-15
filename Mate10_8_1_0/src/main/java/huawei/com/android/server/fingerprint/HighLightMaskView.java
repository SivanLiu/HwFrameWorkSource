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
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.com.android.server.policy.HwGlobalActionsData;

public class HighLightMaskView extends FrameLayout {
    private static final int DEFAULT_CIRCLE_RADIUS = 125;
    private static final String TAG = "HighLightMaskView";
    private static int[] sSampleAlpha = new int[]{234, 229, 219, 220, 216, 211, 208, 205, 187, 176, HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW, 163, 159, CPUFeature.MSG_SET_BG_UIDS, CPUFeature.MSG_CPUCTL_SUBSWITCH, CPUFeature.MSG_CPUCTL_SUBSWITCH, 125, 121, 111, 101, 92, 81, 81, 69, 68, 58, 56, 46, 44, 35, 34, 30, 22, 23, 18, 0, 0};
    private static int[] sSampleBrightness = new int[]{4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 30, 34, 40, 46, 50, 56, 64, 74, 84, 94, 104, CPUFeature.MSG_CPUFEATURE_OFF, 124, 134, CPUFeature.MSG_RESET_VIP_THREAD, CPUFeature.MSG_RESET_ON_FIRE, 164, 174, 184, 194, WifiProCommonUtils.HTTP_REACHALBE_GOOLE, 214, 224, 234, 244, 248, 255};
    private View mBlackMaskView;
    private int mCenterX;
    private int mCenterY;
    private View mCircleMaskView;
    private int mHighLightShowType = -1;
    private final Paint mPaint = new Paint(1);
    private String mPkgName;
    private int mRadius;
    private float mScale;
    private WaterEffectView mWaterEffectView;

    public HighLightMaskView(Context context, int brightness) {
        super(context);
        init(context, brightness);
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
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (FingerViewController.PKGNAME_OF_KEYGUARD.equals(this.mPkgName) && 1 == this.mHighLightShowType) {
            this.mWaterEffectView.onPause();
        }
    }

    private void init(Context context, int brightness) {
        this.mPaint.setDither(true);
        this.mPaint.setStyle(Style.FILL);
        this.mPaint.setColor(HwGlobalActionsData.MASK_FLAG_REBOOT);
        this.mPaint.setMaskFilter(new BlurMaskFilter(20.0f, Blur.SOLID));
        this.mRadius = 125;
        this.mWaterEffectView = new WaterEffectView(context);
        addView(this.mWaterEffectView, -1, -1);
        this.mBlackMaskView = new View(this.mContext);
        this.mBlackMaskView.setBackgroundColor(-16777216);
        int alphaNew = 0;
        if (brightness > 0) {
            alphaNew = getMaskAlpha(brightness);
        }
        Log.i(TAG, "alphaNew:" + alphaNew);
        this.mBlackMaskView.getBackground().setAlpha(alphaNew);
        addView(this.mBlackMaskView, -1, -1);
        this.mCircleMaskView = new View(this.mContext) {
            public void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                canvas.drawCircle(((float) HighLightMaskView.this.mCenterX) * HighLightMaskView.this.mScale, ((float) HighLightMaskView.this.mCenterY) * HighLightMaskView.this.mScale, ((float) HighLightMaskView.this.mRadius) * HighLightMaskView.this.mScale, HighLightMaskView.this.mPaint);
            }
        };
        addView(this.mCircleMaskView, -1, -1);
    }

    public int getMaskAlpha(int currentLight) {
        int alpha = 0;
        Log.d(TAG, " currentLight:" + currentLight);
        if (currentLight > sSampleBrightness[sSampleBrightness.length - 1]) {
            return 0;
        }
        int i = 0;
        while (i < sSampleBrightness.length) {
            Log.d(TAG, " sSampleBrightness :[" + i + "]" + sSampleBrightness[i]);
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
        Log.d(TAG, " alpha:" + alpha);
        return alpha;
    }

    private int queryAlphaImpl(int currLight, int preLevelLight, int preLevelAlpha, int lastLevelLight, int lastLevelAlpha) {
        return (((currLight - preLevelLight) * (lastLevelAlpha - preLevelAlpha)) / (lastLevelLight - preLevelLight)) + preLevelAlpha;
    }

    public void setCenterPoints(int centerX, int centerY) {
        this.mCenterX = centerX;
        this.mCenterY = centerY;
        Log.d(TAG, "mCenterX = " + this.mCenterX + ",mCenterY=" + this.mCenterY);
    }

    public void setScale(float scale) {
        this.mScale = scale;
        Log.d(TAG, "mScale = " + this.mScale);
    }

    public void setCircleVisibility(int visibility) {
        if (this.mCircleMaskView != null) {
            this.mCircleMaskView.setVisibility(visibility);
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
        Log.i(TAG, "mHighLightShowType = " + this.mScale);
    }

    public void setPackageName(String pkgName) {
        this.mPkgName = pkgName;
        Log.i(TAG, "mPkgName = " + this.mPkgName);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaint.setXfermode(null);
    }
}
