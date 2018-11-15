package com.android.server.policy;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.display.DisplayManagerInternal;
import android.os.SystemClock;
import android.view.Display;
import android.view.animation.LinearInterpolator;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class BurnInProtectionHelper implements DisplayListener, AnimatorListener, AnimatorUpdateListener {
    private static final String ACTION_BURN_IN_PROTECTION = "android.internal.policy.action.BURN_IN_PROTECTION";
    private static final long BURNIN_PROTECTION_FIRST_WAKEUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long BURNIN_PROTECTION_MINIMAL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);
    private static final long BURNIN_PROTECTION_SUBSEQUENT_WAKEUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);
    public static final int BURN_IN_MAX_RADIUS_DEFAULT = -1;
    private static final int BURN_IN_SHIFT_STEP = 2;
    private static final long CENTERING_ANIMATION_DURATION_MS = 100;
    private static final boolean DEBUG = false;
    private static final String TAG = "BurnInProtection";
    private final AlarmManager mAlarmManager;
    private int mAppliedBurnInXOffset = 0;
    private int mAppliedBurnInYOffset = 0;
    private boolean mBurnInProtectionActive;
    private final PendingIntent mBurnInProtectionIntent;
    private BroadcastReceiver mBurnInProtectionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BurnInProtectionHelper.this.updateBurnInProtection();
        }
    };
    private final int mBurnInRadiusMaxSquared;
    private final ValueAnimator mCenteringAnimator;
    private final Display mDisplay;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private boolean mFirstUpdate;
    private int mLastBurnInXOffset = 0;
    private int mLastBurnInYOffset = 0;
    private final int mMaxHorizontalBurnInOffset;
    private final int mMaxVerticalBurnInOffset;
    private final int mMinHorizontalBurnInOffset;
    private final int mMinVerticalBurnInOffset;
    private int mXOffsetDirection = 1;
    private int mYOffsetDirection = 1;

    public BurnInProtectionHelper(Context context, int minHorizontalOffset, int maxHorizontalOffset, int minVerticalOffset, int maxVerticalOffset, int maxOffsetRadius) {
        this.mMinHorizontalBurnInOffset = minHorizontalOffset;
        this.mMaxHorizontalBurnInOffset = maxHorizontalOffset;
        this.mMinVerticalBurnInOffset = minVerticalOffset;
        this.mMaxVerticalBurnInOffset = maxVerticalOffset;
        if (maxOffsetRadius != -1) {
            this.mBurnInRadiusMaxSquared = maxOffsetRadius * maxOffsetRadius;
        } else {
            this.mBurnInRadiusMaxSquared = -1;
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        context.registerReceiver(this.mBurnInProtectionReceiver, new IntentFilter(ACTION_BURN_IN_PROTECTION));
        Intent intent = new Intent(ACTION_BURN_IN_PROTECTION);
        intent.setPackage(context.getPackageName());
        intent.setFlags(1073741824);
        this.mBurnInProtectionIntent = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        this.mDisplay = displayManager.getDisplay(0);
        displayManager.registerDisplayListener(this, null);
        this.mCenteringAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        this.mCenteringAnimator.setDuration(CENTERING_ANIMATION_DURATION_MS);
        this.mCenteringAnimator.setInterpolator(new LinearInterpolator());
        this.mCenteringAnimator.addListener(this);
        this.mCenteringAnimator.addUpdateListener(this);
    }

    public void startBurnInProtection() {
        if (!this.mBurnInProtectionActive) {
            this.mBurnInProtectionActive = true;
            this.mFirstUpdate = true;
            this.mCenteringAnimator.cancel();
            updateBurnInProtection();
        }
    }

    private void updateBurnInProtection() {
        if (this.mBurnInProtectionActive) {
            long interval;
            if (this.mFirstUpdate) {
                interval = BURNIN_PROTECTION_FIRST_WAKEUP_INTERVAL_MS;
            } else {
                interval = BURNIN_PROTECTION_SUBSEQUENT_WAKEUP_INTERVAL_MS;
            }
            if (this.mFirstUpdate) {
                this.mFirstUpdate = false;
            } else {
                adjustOffsets();
                this.mAppliedBurnInXOffset = this.mLastBurnInXOffset;
                this.mAppliedBurnInYOffset = this.mLastBurnInYOffset;
                this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), this.mLastBurnInXOffset, this.mLastBurnInYOffset);
            }
            long nowWall = System.currentTimeMillis();
            long nextWall = BURNIN_PROTECTION_MINIMAL_INTERVAL_MS + nowWall;
            this.mAlarmManager.setExact(3, (((nextWall - (nextWall % interval)) + interval) - nowWall) + SystemClock.elapsedRealtime(), this.mBurnInProtectionIntent);
            return;
        }
        this.mAlarmManager.cancel(this.mBurnInProtectionIntent);
        this.mCenteringAnimator.start();
    }

    public void cancelBurnInProtection() {
        if (this.mBurnInProtectionActive) {
            this.mBurnInProtectionActive = false;
            updateBurnInProtection();
        }
    }

    private void adjustOffsets() {
        do {
            int xChange = this.mXOffsetDirection * 2;
            this.mLastBurnInXOffset += xChange;
            if (this.mLastBurnInXOffset > this.mMaxHorizontalBurnInOffset || this.mLastBurnInXOffset < this.mMinHorizontalBurnInOffset) {
                this.mLastBurnInXOffset -= xChange;
                this.mXOffsetDirection *= -1;
                int yChange = this.mYOffsetDirection * 2;
                this.mLastBurnInYOffset += yChange;
                if (this.mLastBurnInYOffset > this.mMaxVerticalBurnInOffset || this.mLastBurnInYOffset < this.mMinVerticalBurnInOffset) {
                    this.mLastBurnInYOffset -= yChange;
                    this.mYOffsetDirection *= -1;
                }
            }
            if (this.mBurnInRadiusMaxSquared == -1) {
                return;
            }
        } while ((this.mLastBurnInXOffset * this.mLastBurnInXOffset) + (this.mLastBurnInYOffset * this.mLastBurnInYOffset) > this.mBurnInRadiusMaxSquared);
    }

    public void dump(String prefix, PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(TAG);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        prefix = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mBurnInProtectionActive=");
        stringBuilder.append(this.mBurnInProtectionActive);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mHorizontalBurnInOffsetsBounds=(");
        stringBuilder.append(this.mMinHorizontalBurnInOffset);
        stringBuilder.append(", ");
        stringBuilder.append(this.mMaxHorizontalBurnInOffset);
        stringBuilder.append(")");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mVerticalBurnInOffsetsBounds=(");
        stringBuilder.append(this.mMinVerticalBurnInOffset);
        stringBuilder.append(", ");
        stringBuilder.append(this.mMaxVerticalBurnInOffset);
        stringBuilder.append(")");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mBurnInRadiusMaxSquared=");
        stringBuilder.append(this.mBurnInRadiusMaxSquared);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mLastBurnInOffset=(");
        stringBuilder.append(this.mLastBurnInXOffset);
        stringBuilder.append(", ");
        stringBuilder.append(this.mLastBurnInYOffset);
        stringBuilder.append(")");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mOfsetChangeDirections=(");
        stringBuilder.append(this.mXOffsetDirection);
        stringBuilder.append(", ");
        stringBuilder.append(this.mYOffsetDirection);
        stringBuilder.append(")");
        pw.println(stringBuilder.toString());
    }

    public void onDisplayAdded(int i) {
    }

    public void onDisplayRemoved(int i) {
    }

    public void onDisplayChanged(int displayId) {
        if (displayId != this.mDisplay.getDisplayId()) {
            return;
        }
        if (this.mDisplay.getState() == 3 || this.mDisplay.getState() == 4 || this.mDisplay.getState() == 6) {
            startBurnInProtection();
        } else {
            cancelBurnInProtection();
        }
    }

    public void onAnimationStart(Animator animator) {
    }

    public void onAnimationEnd(Animator animator) {
        if (animator == this.mCenteringAnimator && !this.mBurnInProtectionActive) {
            this.mAppliedBurnInXOffset = 0;
            this.mAppliedBurnInYOffset = 0;
            this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), 0, 0);
        }
    }

    public void onAnimationCancel(Animator animator) {
    }

    public void onAnimationRepeat(Animator animator) {
    }

    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        if (!this.mBurnInProtectionActive) {
            float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), (int) (((float) this.mAppliedBurnInXOffset) * value), (int) (((float) this.mAppliedBurnInYOffset) * value));
        }
    }
}
