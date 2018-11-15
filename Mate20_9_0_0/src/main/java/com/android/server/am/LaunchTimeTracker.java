package com.android.server.am;

import android.os.SystemClock;
import android.os.Trace;
import android.util.Jlog;
import android.util.SparseArray;

class LaunchTimeTracker {
    private final SparseArray<Entry> mWindowingModeLaunchTime = new SparseArray();

    static class Entry {
        long mFullyDrawnStartTime;
        long mLaunchStartTime;

        Entry() {
        }

        void setLaunchTime(ActivityRecord r) {
            long uptimeMillis;
            if (r.displayStartTime == 0) {
                if (r.getStack() != null) {
                    r.getStack().mshortComponentName = r.shortComponentName;
                    if (r.app != null) {
                        Jlog.d(43, r.shortComponentName, r.app.pid, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    } else {
                        Jlog.d(43, r.shortComponentName, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                    if (r.task != null) {
                        r.task.isLaunching = true;
                    }
                }
                uptimeMillis = SystemClock.uptimeMillis();
                r.displayStartTime = uptimeMillis;
                r.fullyDrawnStartTime = uptimeMillis;
                if (this.mLaunchStartTime == 0) {
                    startLaunchTraces(r.packageName);
                    uptimeMillis = r.displayStartTime;
                    this.mFullyDrawnStartTime = uptimeMillis;
                    this.mLaunchStartTime = uptimeMillis;
                }
            } else if (this.mLaunchStartTime == 0) {
                startLaunchTraces(r.packageName);
                uptimeMillis = SystemClock.uptimeMillis();
                this.mFullyDrawnStartTime = uptimeMillis;
                this.mLaunchStartTime = uptimeMillis;
            }
        }

        private void startLaunchTraces(String packageName) {
            if (this.mFullyDrawnStartTime != 0) {
                Trace.asyncTraceEnd(64, "drawing", 0);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("launching: ");
            stringBuilder.append(packageName);
            Trace.asyncTraceBegin(64, stringBuilder.toString(), 0);
            Trace.asyncTraceBegin(64, "drawing", 0);
        }

        private void stopFullyDrawnTraceIfNeeded() {
            if (this.mFullyDrawnStartTime != 0 && this.mLaunchStartTime == 0) {
                Trace.asyncTraceEnd(64, "drawing", 0);
                this.mFullyDrawnStartTime = 0;
            }
        }
    }

    LaunchTimeTracker() {
    }

    void setLaunchTime(ActivityRecord r) {
        Entry entry = (Entry) this.mWindowingModeLaunchTime.get(r.getWindowingMode());
        if (entry == null) {
            entry = new Entry();
            this.mWindowingModeLaunchTime.append(r.getWindowingMode(), entry);
        }
        entry.setLaunchTime(r);
    }

    void stopFullyDrawnTraceIfNeeded(int windowingMode) {
        Entry entry = (Entry) this.mWindowingModeLaunchTime.get(windowingMode);
        if (entry != null) {
            entry.stopFullyDrawnTraceIfNeeded();
        }
    }

    Entry getEntry(int windowingMode) {
        return (Entry) this.mWindowingModeLaunchTime.get(windowingMode);
    }
}
