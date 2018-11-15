package com.android.server.am;

import android.app.IInstrumentationWatcher;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Process;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;

public class InstrumentationReporter {
    static final boolean DEBUG = false;
    static final int REPORT_TYPE_FINISHED = 1;
    static final int REPORT_TYPE_STATUS = 0;
    static final String TAG = "ActivityManager";
    final Object mLock = new Object();
    ArrayList<Report> mPendingReports;
    Thread mThread;

    final class MyThread extends Thread {
        public MyThread() {
            super("InstrumentationReporter");
        }

        /* JADX WARNING: Removed duplicated region for block: B:18:0x0047 A:{Splitter: B:13:0x002b, ExcHandler: android.os.RemoteException (r5_4 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:9:0x001d, code:
            r1 = false;
            r2 = 0;
     */
        /* JADX WARNING: Missing block: B:11:0x0023, code:
            if (r2 >= r3.size()) goto L_0x006d;
     */
        /* JADX WARNING: Missing block: B:12:0x0025, code:
            r4 = (com.android.server.am.InstrumentationReporter.Report) r3.get(r2);
     */
        /* JADX WARNING: Missing block: B:15:0x002d, code:
            if (r4.mType != 0) goto L_0x003b;
     */
        /* JADX WARNING: Missing block: B:16:0x002f, code:
            r4.mWatcher.instrumentationStatus(r4.mName, r4.mResultCode, r4.mResults);
     */
        /* JADX WARNING: Missing block: B:17:0x003b, code:
            r4.mWatcher.instrumentationFinished(r4.mName, r4.mResultCode, r4.mResults);
     */
        /* JADX WARNING: Missing block: B:18:0x0047, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:19:0x0048, code:
            r7 = new java.lang.StringBuilder();
            r7.append("Failure reporting to instrumentation watcher: comp=");
            r7.append(r4.mName);
            r7.append(" results=");
            r7.append(r4.mResults);
            android.util.Slog.i("ActivityManager", r7.toString(), r5);
     */
        /* JADX WARNING: Missing block: B:20:0x006a, code:
            r2 = r2 + 1;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            Process.setThreadPriority(0);
            boolean waited = false;
            while (true) {
                synchronized (InstrumentationReporter.this.mLock) {
                    ArrayList<Report> reports = InstrumentationReporter.this.mPendingReports;
                    InstrumentationReporter.this.mPendingReports = null;
                    if (reports == null || reports.isEmpty()) {
                        if (waited) {
                            InstrumentationReporter.this.mThread = null;
                            return;
                        }
                        try {
                            InstrumentationReporter.this.mLock.wait(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                        } catch (InterruptedException e) {
                        }
                        waited = true;
                    }
                }
            }
        }
    }

    final class Report {
        final ComponentName mName;
        final int mResultCode;
        final Bundle mResults;
        final int mType;
        final IInstrumentationWatcher mWatcher;

        Report(int type, IInstrumentationWatcher watcher, ComponentName name, int resultCode, Bundle results) {
            this.mType = type;
            this.mWatcher = watcher;
            this.mName = name;
            this.mResultCode = resultCode;
            this.mResults = results;
        }
    }

    public void reportStatus(IInstrumentationWatcher watcher, ComponentName name, int resultCode, Bundle results) {
        report(new Report(0, watcher, name, resultCode, results));
    }

    public void reportFinished(IInstrumentationWatcher watcher, ComponentName name, int resultCode, Bundle results) {
        report(new Report(1, watcher, name, resultCode, results));
    }

    private void report(Report report) {
        synchronized (this.mLock) {
            if (this.mThread == null) {
                this.mThread = new MyThread();
                this.mThread.start();
            }
            if (this.mPendingReports == null) {
                this.mPendingReports = new ArrayList();
            }
            this.mPendingReports.add(report);
            this.mLock.notifyAll();
        }
    }
}
