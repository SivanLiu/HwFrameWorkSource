package com.android.systemui.shared.system;

import android.content.ComponentName;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.shared.recents.model.ThumbnailData;

public abstract class TaskStackChangeListener {
    public void onTaskStackChangedBackground() {
    }

    public void onTaskStackChanged() {
    }

    public void onTaskSnapshotChanged(int taskId, ThumbnailData snapshot) {
    }

    public void onActivityPinned(String packageName, int userId, int taskId, int stackId) {
    }

    public void onActivityUnpinned() {
    }

    public void onPinnedActivityRestartAttempt(boolean clearedTask) {
    }

    public void onPinnedStackAnimationStarted() {
    }

    public void onPinnedStackAnimationEnded() {
    }

    public void onActivityForcedResizable(String packageName, int taskId, int reason) {
    }

    public void onActivityDismissingDockedStack() {
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() {
    }

    public void onTaskProfileLocked(int taskId, int userId) {
    }

    public void onTaskCreated(int taskId, ComponentName componentName) {
    }

    public void onTaskRemoved(int taskId) {
    }

    public void onTaskMovedToFront(int taskId) {
    }

    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation) {
    }

    protected final boolean checkCurrentUserId(int currentUserId, boolean debug) {
        int processUserId = UserHandle.myUserId();
        if (processUserId == currentUserId) {
            return true;
        }
        if (debug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UID mismatch. Process is uid=");
            stringBuilder.append(processUserId);
            stringBuilder.append(" and the current user is uid=");
            stringBuilder.append(currentUserId);
            Log.d("TaskStackChangeListener", stringBuilder.toString());
        }
        return false;
    }
}
