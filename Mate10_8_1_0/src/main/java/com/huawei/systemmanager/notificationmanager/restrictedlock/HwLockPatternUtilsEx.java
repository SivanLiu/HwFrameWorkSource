package com.huawei.systemmanager.notificationmanager.restrictedlock;

import android.content.Context;
import com.android.internal.widget.LockPatternUtils;

public class HwLockPatternUtilsEx {
    private LockPatternUtils mInnerLockPatternUtils;

    public HwLockPatternUtilsEx(Context context) {
        this.mInnerLockPatternUtils = new LockPatternUtils(context);
    }

    public boolean isSeparateProfileChallengeEnabled(int userHandle) {
        return this.mInnerLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle);
    }
}
