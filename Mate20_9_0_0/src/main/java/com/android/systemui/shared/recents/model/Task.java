package com.android.systemui.shared.recents.model;

import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewDebug.ExportedProperty;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class Task {
    public static final String TAG = "Task";
    @ExportedProperty(category = "recents")
    public int colorBackground;
    @ExportedProperty(category = "recents")
    public int colorPrimary;
    public Drawable icon;
    @ExportedProperty(category = "recents")
    public boolean isDockable;
    @ExportedProperty(category = "recents")
    public boolean isHwTaskLocked;
    @ExportedProperty(category = "recents")
    public boolean isLaunchTarget;
    @ExportedProperty(category = "recents")
    public boolean isLocked;
    @ExportedProperty(category = "recents")
    public boolean isStackTask;
    @ExportedProperty(category = "recents")
    public boolean isSystemApp;
    @ExportedProperty(deepExport = true, prefix = "key_")
    public TaskKey key;
    private ArrayList<TaskCallbacks> mCallbacks = new ArrayList();
    @ExportedProperty(category = "recents")
    public String packageName;
    @ExportedProperty(category = "recents")
    public int resizeMode;
    public TaskDescription taskDescription;
    public int temporarySortIndexInStack;
    public ThumbnailData thumbnail;
    @ExportedProperty(category = "recents")
    public String title;
    @ExportedProperty(category = "recents")
    public String titleDescription;
    @ExportedProperty(category = "recents")
    public ComponentName topActivity;
    @ExportedProperty(category = "recents")
    public boolean useLightOnPrimaryColor;

    public interface TaskCallbacks {
        void onTaskDataLoaded(Task task, ThumbnailData thumbnailData);

        void onTaskDataUnloaded();

        void onTaskWindowingModeChanged();
    }

    public static class TaskKey {
        @ExportedProperty(category = "recents")
        public final Intent baseIntent;
        @ExportedProperty(category = "recents")
        public final int id;
        @ExportedProperty(category = "recents")
        public long lastActiveTime;
        private int mHashCode;
        @ExportedProperty(category = "recents")
        public final int userId;
        @ExportedProperty(category = "recents")
        public int windowingMode;

        public TaskKey(int id, int windowingMode, Intent intent, int userId, long lastActiveTime) {
            this.id = id;
            this.windowingMode = windowingMode;
            this.baseIntent = intent;
            this.userId = userId;
            this.lastActiveTime = lastActiveTime;
            updateHashCode();
        }

        public void setWindowingMode(int windowingMode) {
            this.windowingMode = windowingMode;
            updateHashCode();
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        public String getPackageName() {
            if (this.baseIntent.getComponent() != null) {
                return this.baseIntent.getComponent().getPackageName();
            }
            return this.baseIntent.getPackage();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof TaskKey)) {
                return false;
            }
            TaskKey otherKey = (TaskKey) o;
            if (this.id == otherKey.id && this.windowingMode == otherKey.windowingMode && this.userId == otherKey.userId) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("id=");
            stringBuilder.append(this.id);
            stringBuilder.append(" windowingMode=");
            stringBuilder.append(this.windowingMode);
            stringBuilder.append(" user=");
            stringBuilder.append(this.userId);
            stringBuilder.append(" lastActiveTime=");
            stringBuilder.append(this.lastActiveTime);
            return stringBuilder.toString();
        }

        private void updateHashCode() {
            this.mHashCode = Objects.hash(new Object[]{Integer.valueOf(this.id), Integer.valueOf(this.windowingMode), Integer.valueOf(this.userId)});
        }
    }

    public Task(TaskKey key, Drawable icon, ThumbnailData thumbnail, String title, String titleDescription, int colorPrimary, int colorBackground, boolean isLaunchTarget, boolean isStackTask, boolean isSystemApp, boolean isDockable, TaskDescription taskDescription, int resizeMode, ComponentName topActivity, boolean isLocked) {
        this.key = key;
        this.icon = icon;
        this.thumbnail = thumbnail;
        this.title = title;
        this.titleDescription = titleDescription;
        this.colorPrimary = colorPrimary;
        this.colorBackground = colorBackground;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) > 3.0f;
        this.taskDescription = taskDescription;
        this.isLaunchTarget = isLaunchTarget;
        this.isStackTask = isStackTask;
        this.isSystemApp = isSystemApp;
        this.isDockable = isDockable;
        this.resizeMode = resizeMode;
        this.topActivity = topActivity;
        this.isLocked = isLocked;
    }

    public void setPakcageName(String packageName) {
        this.packageName = packageName;
        this.isHwTaskLocked = HwRecentsTaskUtils.isHwTaskLocked(packageName, false);
    }

    public void copyFrom(Task o) {
        this.key = o.key;
        this.icon = o.icon;
        this.thumbnail = o.thumbnail;
        this.title = o.title;
        this.titleDescription = o.titleDescription;
        this.colorPrimary = o.colorPrimary;
        this.colorBackground = o.colorBackground;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.taskDescription = o.taskDescription;
        this.isLaunchTarget = o.isLaunchTarget;
        this.isStackTask = o.isStackTask;
        this.isSystemApp = o.isSystemApp;
        this.isDockable = o.isDockable;
        this.resizeMode = o.resizeMode;
        this.isLocked = o.isLocked;
        this.topActivity = o.topActivity;
        this.packageName = o.packageName;
        this.isHwTaskLocked = o.isHwTaskLocked;
    }

    public void addCallback(TaskCallbacks cb) {
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    public void removeCallback(TaskCallbacks cb) {
        this.mCallbacks.remove(cb);
    }

    public void setWindowingMode(int windowingMode) {
        this.key.setWindowingMode(windowingMode);
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            ((TaskCallbacks) this.mCallbacks.get(i)).onTaskWindowingModeChanged();
        }
    }

    public void notifyTaskDataLoaded(ThumbnailData thumbnailData, Drawable applicationIcon) {
        this.icon = applicationIcon;
        this.thumbnail = thumbnailData;
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            ((TaskCallbacks) this.mCallbacks.get(i)).onTaskDataLoaded(this, thumbnailData);
        }
    }

    public void notifyTaskDataUnloaded(Drawable defaultApplicationIcon) {
        this.icon = defaultApplicationIcon;
        this.thumbnail = null;
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            ((TaskCallbacks) this.mCallbacks.get(i)).onTaskDataUnloaded();
        }
    }

    public ComponentName getTopComponent() {
        if (this.topActivity != null) {
            return this.topActivity;
        }
        return this.key.baseIntent.getComponent();
    }

    public boolean equals(Object o) {
        return this.key.equals(((Task) o).key);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.key.toString());
        stringBuilder.append("] ");
        stringBuilder.append(this.title);
        return stringBuilder.toString();
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.print(prefix);
        writer.print(this.key);
        if (!this.isDockable) {
            writer.print(" dockable=N");
        }
        if (this.isLaunchTarget) {
            writer.print(" launchTarget=Y");
        }
        if (this.isLocked) {
            writer.print(" locked=Y");
        }
        writer.print(" ");
        writer.print(this.title);
        writer.println();
    }

    public void recycle() {
        if (this.thumbnail != null) {
            Bitmap b = this.thumbnail.thumbnail;
            if (!(b == null || b.isRecycled())) {
                b.recycle();
                this.thumbnail = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Task recycle ");
                stringBuilder.append(this.key);
                Log.i(str, stringBuilder.toString());
            }
        }
    }
}
