package com.android.server.am;

import android.app.IInstrumentationWatcher;
import android.app.IUiAutomationConnection;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.PrintWriterPrinter;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

class ActiveInstrumentation {
    Bundle mArguments;
    ComponentName mClass;
    Bundle mCurResults;
    boolean mFinished;
    String mProfileFile;
    ComponentName mResultClass;
    final ArrayList<ProcessRecord> mRunningProcesses = new ArrayList();
    final ActivityManagerService mService;
    ApplicationInfo mTargetInfo;
    String[] mTargetProcesses;
    IUiAutomationConnection mUiAutomationConnection;
    IInstrumentationWatcher mWatcher;

    ActiveInstrumentation(ActivityManagerService service) {
        this.mService = service;
    }

    void removeProcess(ProcessRecord proc) {
        this.mFinished = true;
        this.mRunningProcesses.remove(proc);
        if (this.mRunningProcesses.size() == 0) {
            this.mService.mActiveInstrumentation.remove(this);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ActiveInstrumentation{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mClass.toShortString());
        if (this.mFinished) {
            sb.append(" FINISHED");
        }
        sb.append(" ");
        sb.append(this.mRunningProcesses.size());
        sb.append(" procs");
        sb.append('}');
        return sb.toString();
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mClass=");
        pw.print(this.mClass);
        pw.print(" mFinished=");
        pw.println(this.mFinished);
        pw.print(prefix);
        pw.println("mRunningProcesses:");
        for (int i = 0; i < this.mRunningProcesses.size(); i++) {
            pw.print(prefix);
            pw.print("  #");
            pw.print(i);
            pw.print(": ");
            pw.println(this.mRunningProcesses.get(i));
        }
        pw.print(prefix);
        pw.print("mTargetProcesses=");
        pw.println(Arrays.toString(this.mTargetProcesses));
        pw.print(prefix);
        pw.print("mTargetInfo=");
        pw.println(this.mTargetInfo);
        if (this.mTargetInfo != null) {
            ApplicationInfo applicationInfo = this.mTargetInfo;
            PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(pw);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            applicationInfo.dump(printWriterPrinter, stringBuilder.toString(), 0);
        }
        if (this.mProfileFile != null) {
            pw.print(prefix);
            pw.print("mProfileFile=");
            pw.println(this.mProfileFile);
        }
        if (this.mWatcher != null) {
            pw.print(prefix);
            pw.print("mWatcher=");
            pw.println(this.mWatcher);
        }
        if (this.mUiAutomationConnection != null) {
            pw.print(prefix);
            pw.print("mUiAutomationConnection=");
            pw.println(this.mUiAutomationConnection);
        }
        pw.print(prefix);
        pw.print("mArguments=");
        pw.println(this.mArguments);
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mClass.writeToProto(proto, 1146756268033L);
        proto.write(1133871366146L, this.mFinished);
        int i = 0;
        for (int i2 = 0; i2 < this.mRunningProcesses.size(); i2++) {
            ((ProcessRecord) this.mRunningProcesses.get(i2)).writeToProto(proto, 2246267895811L);
        }
        String[] strArr = this.mTargetProcesses;
        int length = strArr.length;
        while (i < length) {
            proto.write(2237677961220L, strArr[i]);
            i++;
        }
        if (this.mTargetInfo != null) {
            this.mTargetInfo.writeToProto(proto, 1146756268037L);
        }
        proto.write(1138166333446L, this.mProfileFile);
        proto.write(1138166333447L, this.mWatcher.toString());
        proto.write(1138166333448L, this.mUiAutomationConnection.toString());
        proto.write(1138166333449L, this.mArguments.toString());
        proto.end(token);
    }
}
