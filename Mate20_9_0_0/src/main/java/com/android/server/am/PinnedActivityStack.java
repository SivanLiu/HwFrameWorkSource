package com.android.server.am;

import android.app.RemoteAction;
import android.content.res.Configuration;
import android.graphics.Rect;
import com.android.server.wm.PinnedStackWindowController;
import com.android.server.wm.PinnedStackWindowListener;
import java.util.ArrayList;
import java.util.List;

class PinnedActivityStack extends ActivityStack<PinnedStackWindowController> implements PinnedStackWindowListener {
    PinnedActivityStack(ActivityDisplay display, int stackId, ActivityStackSupervisor supervisor, boolean onTop) {
        super(display, stackId, supervisor, 2, 1, onTop);
    }

    PinnedStackWindowController createStackWindowController(int displayId, boolean onTop, Rect outBounds) {
        return new PinnedStackWindowController(this.mStackId, this, displayId, onTop, outBounds, this.mStackSupervisor.mWindowManager);
    }

    Rect getDefaultPictureInPictureBounds(float aspectRatio) {
        return ((PinnedStackWindowController) getWindowContainerController()).getPictureInPictureBounds(aspectRatio, null);
    }

    void animateResizePinnedStack(Rect sourceHintBounds, Rect toBounds, int animationDuration, boolean fromFullscreen) {
        if (skipResizeAnimation(toBounds == null)) {
            this.mService.moveTasksToFullscreenStack(this.mStackId, true);
        } else {
            ((PinnedStackWindowController) getWindowContainerController()).animateResizePinnedStack(toBounds, sourceHintBounds, animationDuration, fromFullscreen);
        }
    }

    private boolean skipResizeAnimation(boolean toFullscreen) {
        boolean z = false;
        if (!toFullscreen) {
            return false;
        }
        Configuration parentConfig = getParent().getConfiguration();
        ActivityRecord top = topRunningNonOverlayTaskActivity();
        if (!(top == null || top.isConfigurationCompatible(parentConfig))) {
            z = true;
        }
        return z;
    }

    void setPictureInPictureAspectRatio(float aspectRatio) {
        ((PinnedStackWindowController) getWindowContainerController()).setPictureInPictureAspectRatio(aspectRatio);
    }

    void setPictureInPictureActions(List<RemoteAction> actions) {
        ((PinnedStackWindowController) getWindowContainerController()).setPictureInPictureActions(actions);
    }

    boolean isAnimatingBoundsToFullscreen() {
        return ((PinnedStackWindowController) getWindowContainerController()).isAnimatingBoundsToFullscreen();
    }

    boolean deferScheduleMultiWindowModeChanged() {
        return ((PinnedStackWindowController) this.mWindowContainerController).deferScheduleMultiWindowModeChanged();
    }

    public void updatePictureInPictureModeForPinnedStackAnimation(Rect targetStackBounds, boolean forceUpdate) {
        synchronized (this) {
            ArrayList<TaskRecord> tasks = getAllTasks();
            for (int i = 0; i < tasks.size(); i++) {
                this.mStackSupervisor.updatePictureInPictureMode((TaskRecord) tasks.get(i), targetStackBounds, forceUpdate);
            }
        }
    }
}
