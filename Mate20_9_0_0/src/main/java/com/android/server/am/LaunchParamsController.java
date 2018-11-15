package com.android.server.am;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo.WindowLayout;
import android.graphics.Rect;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

class LaunchParamsController {
    private final List<LaunchParamsModifier> mModifiers = new ArrayList();
    private final ActivityManagerService mService;
    private final LaunchParams mTmpCurrent = new LaunchParams();
    private final LaunchParams mTmpParams = new LaunchParams();
    private final LaunchParams mTmpResult = new LaunchParams();

    static class LaunchParams {
        final Rect mBounds = new Rect();
        int mPreferredDisplayId;
        int mWindowingMode;

        LaunchParams() {
        }

        void reset() {
            this.mBounds.setEmpty();
            this.mPreferredDisplayId = -1;
            this.mWindowingMode = 0;
        }

        void set(LaunchParams params) {
            this.mBounds.set(params.mBounds);
            this.mPreferredDisplayId = params.mPreferredDisplayId;
            this.mWindowingMode = params.mWindowingMode;
        }

        boolean isEmpty() {
            return this.mBounds.isEmpty() && this.mPreferredDisplayId == -1 && this.mWindowingMode == 0;
        }

        boolean hasWindowingMode() {
            return this.mWindowingMode != 0;
        }

        boolean hasPreferredDisplay() {
            return this.mPreferredDisplayId != -1;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LaunchParams that = (LaunchParams) o;
            if (this.mPreferredDisplayId != that.mPreferredDisplayId || this.mWindowingMode != that.mWindowingMode) {
                return false;
            }
            if (this.mBounds != null) {
                z = this.mBounds.equals(that.mBounds);
            } else if (that.mBounds != null) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * ((31 * (this.mBounds != null ? this.mBounds.hashCode() : 0)) + this.mPreferredDisplayId)) + this.mWindowingMode;
        }
    }

    interface LaunchParamsModifier {
        public static final int RESULT_CONTINUE = 2;
        public static final int RESULT_DONE = 1;
        public static final int RESULT_SKIP = 0;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Result {
        }

        int onCalculate(TaskRecord taskRecord, WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, LaunchParams launchParams, LaunchParams launchParams2);
    }

    LaunchParamsController(ActivityManagerService service) {
        this.mService = service;
    }

    void registerDefaultModifiers(ActivityStackSupervisor supervisor) {
        registerModifier(new TaskLaunchParamsModifier());
        registerModifier(new ActivityLaunchParamsModifier(supervisor));
    }

    void calculate(TaskRecord task, WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, LaunchParams result) {
        LaunchParams launchParams = result;
        result.reset();
        for (int i = this.mModifiers.size() - 1; i >= 0; i--) {
            this.mTmpCurrent.set(launchParams);
            this.mTmpResult.reset();
            switch (((LaunchParamsModifier) this.mModifiers.get(i)).onCalculate(task, layout, activity, source, options, this.mTmpCurrent, this.mTmpResult)) {
                case 1:
                    launchParams.set(this.mTmpResult);
                    return;
                case 2:
                    launchParams.set(this.mTmpResult);
                    break;
                default:
                    break;
            }
        }
    }

    boolean layoutTask(TaskRecord task, WindowLayout layout) {
        return layoutTask(task, layout, null, null, null);
    }

    boolean layoutTask(TaskRecord task, WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options) {
        calculate(task, layout, activity, source, options, this.mTmpParams);
        if (this.mTmpParams.isEmpty()) {
            return false;
        }
        this.mService.mWindowManager.deferSurfaceLayout();
        try {
            if (this.mTmpParams.hasPreferredDisplay() && this.mTmpParams.mPreferredDisplayId != task.getStack().getDisplay().mDisplayId) {
                this.mService.moveStackToDisplay(task.getStackId(), this.mTmpParams.mPreferredDisplayId);
            }
            if (this.mTmpParams.hasWindowingMode() && this.mTmpParams.mWindowingMode != task.getStack().getWindowingMode()) {
                task.getStack().setWindowingMode(this.mTmpParams.mWindowingMode);
            }
            if (this.mTmpParams.mBounds.isEmpty()) {
                this.mService.mWindowManager.continueSurfaceLayout();
                return false;
            }
            task.updateOverrideConfiguration(this.mTmpParams.mBounds);
            return true;
        } finally {
            this.mService.mWindowManager.continueSurfaceLayout();
        }
    }

    void registerModifier(LaunchParamsModifier modifier) {
        if (!this.mModifiers.contains(modifier)) {
            this.mModifiers.add(modifier);
        }
    }
}
