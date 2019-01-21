package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IStopUserCallback;
import android.app.IUidObserver;
import android.app.KeyguardManager;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.app.usage.AppStandbyInfo;
import android.app.usage.ConfigurationStats;
import android.app.usage.IUsageStatsManager;
import android.content.ComponentName;
import android.content.IIntentReceiver.Stub;
import android.content.Intent;
import android.content.Intent.CommandOptionHandler;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.UserInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.util.NetworkConstants;
import android.opengl.GLES10;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.DisplayMetrics;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.util.HexDump;
import com.android.internal.util.MemInfoReader;
import com.android.internal.util.Preconditions;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.pm.DumpState;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.WindowManagerService.H;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

final class ActivityManagerShellCommand extends ShellCommand {
    public static final String NO_CLASS_ERROR_CODE = "Error type 3";
    private static final String SHELL_PACKAGE_NAME = "com.android.shell";
    private int mActivityType;
    private String mAgent;
    private boolean mAttachAgentDuringBind;
    private boolean mAutoStop;
    private int mDisplayId;
    final boolean mDumping;
    final IActivityManager mInterface;
    final ActivityManagerService mInternal;
    private boolean mIsLockTask;
    private boolean mIsTaskOverlay;
    final IPackageManager mPm;
    private String mProfileFile;
    private String mReceiverPermission;
    private int mRepeat = 0;
    private int mSamplingInterval;
    private int mStartFlags = 0;
    private boolean mStopOption = false;
    private boolean mStreaming;
    private int mTaskId;
    private int mUserId;
    private boolean mWaitOption = false;
    private int mWindowingMode;

    static final class IntentReceiver extends Stub {
        private boolean mFinished = false;
        private final PrintWriter mPw;

        IntentReceiver(PrintWriter pw) {
            this.mPw = pw;
        }

        public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
            StringBuilder stringBuilder;
            String line = new StringBuilder();
            line.append("Broadcast completed: result=");
            line.append(resultCode);
            line = line.toString();
            if (data != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(line);
                stringBuilder.append(", data=\"");
                stringBuilder.append(data);
                stringBuilder.append("\"");
                line = stringBuilder.toString();
            }
            if (extras != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(line);
                stringBuilder.append(", extras: ");
                stringBuilder.append(extras);
                line = stringBuilder.toString();
            }
            this.mPw.println(line);
            this.mPw.flush();
            synchronized (this) {
                this.mFinished = true;
                notifyAll();
            }
        }

        public synchronized void waitForFinish() {
            while (!this.mFinished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    static final class MyActivityController extends IActivityController.Stub {
        static final int RESULT_ANR_DIALOG = 0;
        static final int RESULT_ANR_KILL = 1;
        static final int RESULT_ANR_WAIT = 1;
        static final int RESULT_CRASH_DIALOG = 0;
        static final int RESULT_CRASH_KILL = 1;
        static final int RESULT_DEFAULT = 0;
        static final int RESULT_EARLY_ANR_CONTINUE = 0;
        static final int RESULT_EARLY_ANR_KILL = 1;
        static final int STATE_ANR = 3;
        static final int STATE_CRASHED = 1;
        static final int STATE_EARLY_ANR = 2;
        static final int STATE_NORMAL = 0;
        final String mGdbPort;
        Process mGdbProcess;
        Thread mGdbThread;
        boolean mGotGdbPrint;
        final InputStream mInput;
        final IActivityManager mInterface;
        final boolean mMonkey;
        final PrintWriter mPw;
        int mResult;
        int mState;

        MyActivityController(IActivityManager iam, PrintWriter pw, InputStream input, String gdbPort, boolean monkey) {
            this.mInterface = iam;
            this.mPw = pw;
            this.mInput = input;
            this.mGdbPort = gdbPort;
            this.mMonkey = monkey;
        }

        public boolean activityResuming(String pkg) {
            synchronized (this) {
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("** Activity resuming: ");
                stringBuilder.append(pkg);
                printWriter.println(stringBuilder.toString());
                this.mPw.flush();
            }
            return true;
        }

        public boolean activityStarting(Intent intent, String pkg) {
            synchronized (this) {
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("** Activity starting: ");
                stringBuilder.append(pkg);
                printWriter.println(stringBuilder.toString());
                this.mPw.flush();
            }
            return true;
        }

        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
            boolean z;
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS CRASHED");
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processName: ");
                stringBuilder.append(processName);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processPid: ");
                stringBuilder.append(pid);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("shortMsg: ");
                stringBuilder.append(shortMsg);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("longMsg: ");
                stringBuilder.append(longMsg);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("timeMillis: ");
                stringBuilder.append(timeMillis);
                printWriter.println(stringBuilder.toString());
                this.mPw.println("stack:");
                this.mPw.print(stackTrace);
                this.mPw.println("#");
                this.mPw.flush();
                z = true;
                if (waitControllerLocked(pid, 1) == 1) {
                    z = false;
                }
            }
            return z;
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) {
            synchronized (this) {
                this.mPw.println("** ERROR: EARLY PROCESS NOT RESPONDING");
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processName: ");
                stringBuilder.append(processName);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processPid: ");
                stringBuilder.append(pid);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("annotation: ");
                stringBuilder.append(annotation);
                printWriter.println(stringBuilder.toString());
                this.mPw.flush();
                if (waitControllerLocked(pid, 2) == 1) {
                    return -1;
                }
                return 0;
            }
        }

        public int appNotResponding(String processName, int pid, String processStats) {
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS NOT RESPONDING");
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processName: ");
                stringBuilder.append(processName);
                printWriter.println(stringBuilder.toString());
                printWriter = this.mPw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processPid: ");
                stringBuilder.append(pid);
                printWriter.println(stringBuilder.toString());
                this.mPw.println("processStats:");
                this.mPw.print(processStats);
                this.mPw.println("#");
                this.mPw.flush();
                int result = waitControllerLocked(pid, 3);
                if (result == 1) {
                    return -1;
                } else if (result == 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        public int systemNotResponding(String message) {
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS NOT RESPONDING");
                PrintWriter printWriter = this.mPw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("message: ");
                stringBuilder.append(message);
                printWriter.println(stringBuilder.toString());
                this.mPw.println("#");
                this.mPw.println("Allowing system to die.");
                this.mPw.flush();
            }
            return -1;
        }

        void killGdbLocked() {
            this.mGotGdbPrint = false;
            if (this.mGdbProcess != null) {
                this.mPw.println("Stopping gdbserver");
                this.mPw.flush();
                this.mGdbProcess.destroy();
                this.mGdbProcess = null;
            }
            if (this.mGdbThread != null) {
                this.mGdbThread.interrupt();
                this.mGdbThread = null;
            }
        }

        int waitControllerLocked(int pid, int state) {
            if (this.mGdbPort != null) {
                killGdbLocked();
                try {
                    PrintWriter printWriter = this.mPw;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Starting gdbserver on port ");
                    stringBuilder.append(this.mGdbPort);
                    printWriter.println(stringBuilder.toString());
                    this.mPw.println("Do the following:");
                    printWriter = this.mPw;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  adb forward tcp:");
                    stringBuilder.append(this.mGdbPort);
                    stringBuilder.append(" tcp:");
                    stringBuilder.append(this.mGdbPort);
                    printWriter.println(stringBuilder.toString());
                    printWriter = this.mPw;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  gdbclient app_process :");
                    stringBuilder.append(this.mGdbPort);
                    printWriter.println(stringBuilder.toString());
                    this.mPw.flush();
                    Runtime runtime = Runtime.getRuntime();
                    String[] strArr = new String[4];
                    strArr[0] = "gdbserver";
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(":");
                    stringBuilder2.append(this.mGdbPort);
                    strArr[1] = stringBuilder2.toString();
                    strArr[2] = "--attach";
                    strArr[3] = Integer.toString(pid);
                    this.mGdbProcess = runtime.exec(strArr);
                    final InputStreamReader converter = new InputStreamReader(this.mGdbProcess.getInputStream());
                    this.mGdbThread = new Thread() {
                        /* JADX WARNING: Missing block: B:13:?, code skipped:
            r2 = r0.readLine();
     */
                        /* JADX WARNING: Missing block: B:14:0x0025, code skipped:
            if (r2 != null) goto L_0x0028;
     */
                        /* JADX WARNING: Missing block: B:15:0x0027, code skipped:
            return;
     */
                        /* JADX WARNING: Missing block: B:16:0x0028, code skipped:
            r3 = r6.this$0.mPw;
            r4 = new java.lang.StringBuilder();
            r4.append("GDB: ");
            r4.append(r2);
            r3.println(r4.toString());
            r6.this$0.mPw.flush();
     */
                        /* JADX WARNING: Missing block: B:19:0x004b, code skipped:
            return;
     */
                        /* Code decompiled incorrectly, please refer to instructions dump. */
                        public void run() {
                            BufferedReader in = new BufferedReader(converter);
                            int count = 0;
                            while (true) {
                                synchronized (MyActivityController.this) {
                                    if (MyActivityController.this.mGdbThread == null) {
                                        return;
                                    } else if (count == 2) {
                                        MyActivityController.this.mGotGdbPrint = true;
                                        MyActivityController.this.notifyAll();
                                    }
                                }
                                count++;
                            }
                            while (true) {
                            }
                        }
                    };
                    this.mGdbThread.start();
                    try {
                        wait(500);
                    } catch (InterruptedException e) {
                    }
                } catch (IOException e2) {
                    PrintWriter printWriter2 = this.mPw;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failure starting gdbserver: ");
                    stringBuilder3.append(e2);
                    printWriter2.println(stringBuilder3.toString());
                    this.mPw.flush();
                    killGdbLocked();
                }
            }
            this.mState = state;
            this.mPw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printMessageForState();
            this.mPw.flush();
            while (this.mState != 0) {
                try {
                    wait();
                } catch (InterruptedException e3) {
                }
            }
            killGdbLocked();
            return this.mResult;
        }

        void resumeController(int result) {
            synchronized (this) {
                this.mState = 0;
                this.mResult = result;
                notifyAll();
            }
        }

        void printMessageForState() {
            switch (this.mState) {
                case 0:
                    this.mPw.println("Monitoring activity manager...  available commands:");
                    break;
                case 1:
                    this.mPw.println("Waiting after crash...  available commands:");
                    this.mPw.println("(c)ontinue: show crash dialog");
                    this.mPw.println("(k)ill: immediately kill app");
                    break;
                case 2:
                    this.mPw.println("Waiting after early ANR...  available commands:");
                    this.mPw.println("(c)ontinue: standard ANR processing");
                    this.mPw.println("(k)ill: immediately kill app");
                    break;
                case 3:
                    this.mPw.println("Waiting after ANR...  available commands:");
                    this.mPw.println("(c)ontinue: show ANR dialog");
                    this.mPw.println("(k)ill: immediately kill app");
                    this.mPw.println("(w)ait: wait some more");
                    break;
            }
            this.mPw.println("(q)uit: finish monitoring");
        }

        void run() throws RemoteException {
            try {
                printMessageForState();
                this.mPw.flush();
                this.mInterface.setActivityController(this, this.mMonkey);
                this.mState = 0;
                BufferedReader in = new BufferedReader(new InputStreamReader(this.mInput));
                while (true) {
                    String readLine = in.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    boolean addNewline = true;
                    PrintWriter printWriter;
                    StringBuilder stringBuilder;
                    if (line.length() <= 0) {
                        addNewline = false;
                    } else if ("q".equals(line)) {
                        break;
                    } else if ("quit".equals(line)) {
                        break;
                    } else if (this.mState == 1) {
                        if (!"c".equals(line)) {
                            if (!"continue".equals(line)) {
                                if (!"k".equals(line)) {
                                    if (!"kill".equals(line)) {
                                        printWriter = this.mPw;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Invalid command: ");
                                        stringBuilder.append(line);
                                        printWriter.println(stringBuilder.toString());
                                    }
                                }
                                resumeController(1);
                            }
                        }
                        resumeController(0);
                    } else if (this.mState == 3) {
                        if (!"c".equals(line)) {
                            if (!"continue".equals(line)) {
                                if (!"k".equals(line)) {
                                    if (!"kill".equals(line)) {
                                        if (!"w".equals(line)) {
                                            if (!"wait".equals(line)) {
                                                printWriter = this.mPw;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Invalid command: ");
                                                stringBuilder.append(line);
                                                printWriter.println(stringBuilder.toString());
                                            }
                                        }
                                        resumeController(1);
                                    }
                                }
                                resumeController(1);
                            }
                        }
                        resumeController(0);
                    } else if (this.mState == 2) {
                        if (!"c".equals(line)) {
                            if (!"continue".equals(line)) {
                                if (!"k".equals(line)) {
                                    if (!"kill".equals(line)) {
                                        printWriter = this.mPw;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Invalid command: ");
                                        stringBuilder.append(line);
                                        printWriter.println(stringBuilder.toString());
                                    }
                                }
                                resumeController(1);
                            }
                        }
                        resumeController(0);
                    } else {
                        printWriter = this.mPw;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid command: ");
                        stringBuilder.append(line);
                        printWriter.println(stringBuilder.toString());
                    }
                    synchronized (this) {
                        if (addNewline) {
                            this.mPw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        }
                        printMessageForState();
                        this.mPw.flush();
                    }
                }
                resumeController(0);
            } catch (IOException e) {
                try {
                    e.printStackTrace(this.mPw);
                    this.mPw.flush();
                } catch (Throwable th) {
                    this.mInterface.setActivityController(null, this.mMonkey);
                }
            } catch (Throwable th2) {
            }
            this.mInterface.setActivityController(null, this.mMonkey);
        }
    }

    static final class StopUserCallback extends IStopUserCallback.Stub {
        private boolean mFinished = false;

        StopUserCallback() {
        }

        public synchronized void waitForFinish() {
            while (!this.mFinished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        public synchronized void userStopped(int userId) {
            this.mFinished = true;
            notifyAll();
        }

        public synchronized void userStopAborted(int userId) {
            this.mFinished = true;
            notifyAll();
        }
    }

    static final class MyUidObserver extends IUidObserver.Stub implements OomAdjObserver {
        static final int STATE_NORMAL = 0;
        final InputStream mInput;
        final IActivityManager mInterface;
        final ActivityManagerService mInternal;
        final PrintWriter mPw;
        int mState;
        final int mUid;

        MyUidObserver(ActivityManagerService service, PrintWriter pw, InputStream input, int uid) {
            this.mInterface = service;
            this.mInternal = service;
            this.mPw = pw;
            this.mInput = input;
            this.mUid = uid;
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq) throws RemoteException {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(uid);
                    this.mPw.print(" procstate ");
                    this.mPw.print(ProcessList.makeProcStateString(procState));
                    this.mPw.print(" seq ");
                    this.mPw.println(procStateSeq);
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(uid);
                    this.mPw.print(" gone");
                    if (disabled) {
                        this.mPw.print(" disabled");
                    }
                    this.mPw.println();
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        public void onUidActive(int uid) throws RemoteException {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(uid);
                    this.mPw.println(" active");
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(uid);
                    this.mPw.print(" idle");
                    if (disabled) {
                        this.mPw.print(" disabled");
                    }
                    this.mPw.println();
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(uid);
                    this.mPw.println(cached ? " cached" : " uncached");
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        public void onOomAdjMessage(String msg) {
            synchronized (this) {
                ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print("# ");
                    this.mPw.println(msg);
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
        }

        void printMessageForState() {
            if (this.mState == 0) {
                this.mPw.println("Watching uid states...  available commands:");
            }
            this.mPw.println("(q)uit: finish watching");
        }

        /*  JADX ERROR: NullPointerException in pass: ProcessVariables
            java.lang.NullPointerException
            	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:271)
            	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
            	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:152)
            	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
            	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
            	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
            	at jadx.core.ProcessClass.process(ProcessClass.java:32)
            	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
            	at java.lang.Iterable.forEach(Iterable.java:75)
            	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
            	at jadx.core.ProcessClass.process(ProcessClass.java:37)
            	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
            	at jadx.api.JavaClass.decompile(JavaClass.java:62)
            	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
            */
        /* JADX WARNING: Removed duplicated region for block: B:36:0x00a3  */
        /* JADX WARNING: Missing block: B:36:0x00a3, code skipped:
            r7.mInternal.clearOomAdjObserver();
     */
        void run() throws android.os.RemoteException {
            /*
            r7 = this;
            r7.printMessageForState();	 Catch:{ IOException -> 0x0084 }
            r0 = r7.mPw;	 Catch:{ IOException -> 0x0084 }
            r0.flush();	 Catch:{ IOException -> 0x0084 }
            r0 = r7.mInterface;	 Catch:{ IOException -> 0x0084 }
            r1 = 31;	 Catch:{ IOException -> 0x0084 }
            r2 = -1;	 Catch:{ IOException -> 0x0084 }
            r3 = 0;	 Catch:{ IOException -> 0x0084 }
            r0.registerUidObserver(r7, r1, r2, r3);	 Catch:{ IOException -> 0x0084 }
            r0 = r7.mUid;	 Catch:{ IOException -> 0x0084 }
            if (r0 < 0) goto L_0x001c;	 Catch:{ IOException -> 0x0084 }
        L_0x0015:
            r0 = r7.mInternal;	 Catch:{ IOException -> 0x0084 }
            r1 = r7.mUid;	 Catch:{ IOException -> 0x0084 }
            r0.setOomAdjObserver(r1, r7);	 Catch:{ IOException -> 0x0084 }
        L_0x001c:
            r0 = 0;	 Catch:{ IOException -> 0x0084 }
            r7.mState = r0;	 Catch:{ IOException -> 0x0084 }
            r0 = new java.io.InputStreamReader;	 Catch:{ IOException -> 0x0084 }
            r1 = r7.mInput;	 Catch:{ IOException -> 0x0084 }
            r0.<init>(r1);	 Catch:{ IOException -> 0x0084 }
            r1 = new java.io.BufferedReader;	 Catch:{ IOException -> 0x0084 }
            r1.<init>(r0);	 Catch:{ IOException -> 0x0084 }
        L_0x002b:
            r2 = r1.readLine();	 Catch:{ IOException -> 0x0084 }
            r3 = r2;	 Catch:{ IOException -> 0x0084 }
            if (r2 == 0) goto L_0x007d;	 Catch:{ IOException -> 0x0084 }
        L_0x0032:
            r2 = 1;	 Catch:{ IOException -> 0x0084 }
            r4 = r3.length();	 Catch:{ IOException -> 0x0084 }
            if (r4 > 0) goto L_0x003b;	 Catch:{ IOException -> 0x0084 }
        L_0x0039:
            r2 = 0;	 Catch:{ IOException -> 0x0084 }
            goto L_0x0064;	 Catch:{ IOException -> 0x0084 }
        L_0x003b:
            r4 = "q";	 Catch:{ IOException -> 0x0084 }
            r4 = r4.equals(r3);	 Catch:{ IOException -> 0x0084 }
            if (r4 != 0) goto L_0x007d;	 Catch:{ IOException -> 0x0084 }
        L_0x0044:
            r4 = "quit";	 Catch:{ IOException -> 0x0084 }
            r4 = r4.equals(r3);	 Catch:{ IOException -> 0x0084 }
            if (r4 == 0) goto L_0x004e;	 Catch:{ IOException -> 0x0084 }
        L_0x004d:
            goto L_0x007d;	 Catch:{ IOException -> 0x0084 }
        L_0x004e:
            r4 = r7.mPw;	 Catch:{ IOException -> 0x0084 }
            r5 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0084 }
            r5.<init>();	 Catch:{ IOException -> 0x0084 }
            r6 = "Invalid command: ";	 Catch:{ IOException -> 0x0084 }
            r5.append(r6);	 Catch:{ IOException -> 0x0084 }
            r5.append(r3);	 Catch:{ IOException -> 0x0084 }
            r5 = r5.toString();	 Catch:{ IOException -> 0x0084 }
            r4.println(r5);	 Catch:{ IOException -> 0x0084 }
        L_0x0064:
            monitor-enter(r7);	 Catch:{ IOException -> 0x0084 }
            if (r2 == 0) goto L_0x0071;
        L_0x0067:
            r4 = r7.mPw;	 Catch:{ all -> 0x006f }
            r5 = "";	 Catch:{ all -> 0x006f }
            r4.println(r5);	 Catch:{ all -> 0x006f }
            goto L_0x0071;	 Catch:{ all -> 0x006f }
        L_0x006f:
            r4 = move-exception;	 Catch:{ all -> 0x006f }
            goto L_0x007b;	 Catch:{ all -> 0x006f }
        L_0x0071:
            r7.printMessageForState();	 Catch:{ all -> 0x006f }
            r4 = r7.mPw;	 Catch:{ all -> 0x006f }
            r4.flush();	 Catch:{ all -> 0x006f }
            monitor-exit(r7);	 Catch:{ all -> 0x006f }
            goto L_0x002b;	 Catch:{ all -> 0x006f }
        L_0x007b:
            monitor-exit(r7);	 Catch:{ all -> 0x006f }
            throw r4;	 Catch:{ IOException -> 0x0084 }
        L_0x007d:
            r0 = r7.mUid;
            if (r0 < 0) goto L_0x0098;
        L_0x0081:
            goto L_0x0093;
        L_0x0082:
            r0 = move-exception;
            goto L_0x009f;
        L_0x0084:
            r0 = move-exception;
            r1 = r7.mPw;	 Catch:{ all -> 0x0082 }
            r0.printStackTrace(r1);	 Catch:{ all -> 0x0082 }
            r1 = r7.mPw;	 Catch:{ all -> 0x0082 }
            r1.flush();	 Catch:{ all -> 0x0082 }
            r0 = r7.mUid;
            if (r0 < 0) goto L_0x0098;
        L_0x0093:
            r0 = r7.mInternal;
            r0.clearOomAdjObserver();
        L_0x0098:
            r0 = r7.mInterface;
            r0.unregisterUidObserver(r7);
            return;
        L_0x009f:
            r1 = r7.mUid;
            if (r1 < 0) goto L_0x00a8;
        L_0x00a3:
            r1 = r7.mInternal;
            r1.clearOomAdjObserver();
        L_0x00a8:
            r1 = r7.mInterface;
            r1.unregisterUidObserver(r7);
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityManagerShellCommand$MyUidObserver.run():void");
        }
    }

    static /* synthetic */ int access$076(ActivityManagerShellCommand x0, int x1) {
        int i = x0.mStartFlags | x1;
        x0.mStartFlags = i;
        return i;
    }

    ActivityManagerShellCommand(ActivityManagerService service, boolean dumping) {
        this.mInterface = service;
        this.mInternal = service;
        this.mPm = AppGlobals.getPackageManager();
        this.mDumping = dumping;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int i;
            switch (cmd.hashCode()) {
                case -2121667104:
                    if (cmd.equals("dumpheap")) {
                        i = 14;
                        break;
                    }
                case -1969672196:
                    if (cmd.equals("set-debug-app")) {
                        i = 15;
                        break;
                    }
                case -1719979774:
                    if (cmd.equals("get-inactive")) {
                        i = 49;
                        break;
                    }
                case -1710503333:
                    if (cmd.equals("package-importance")) {
                        i = 32;
                        break;
                    }
                case -1667670943:
                    if (cmd.equals("get-standby-bucket")) {
                        i = 51;
                        break;
                    }
                case -1619282346:
                    if (cmd.equals("start-user")) {
                        i = 38;
                        break;
                    }
                case -1618876223:
                    if (cmd.equals("broadcast")) {
                        i = 10;
                        break;
                    }
                case -1324660647:
                    if (cmd.equals("suppress-resize-config-changes")) {
                        i = 47;
                        break;
                    }
                case -1303445945:
                    if (cmd.equals("send-trim-memory")) {
                        i = 52;
                        break;
                    }
                case -1131287478:
                    if (cmd.equals("start-service")) {
                        i = 3;
                        break;
                    }
                case -1002578147:
                    if (cmd.equals("get-uid-state")) {
                        i = 45;
                        break;
                    }
                case -965273485:
                    if (cmd.equals("stopservice")) {
                        i = 8;
                        break;
                    }
                case -930080590:
                    if (cmd.equals("startfgservice")) {
                        i = 5;
                        break;
                    }
                case -907667276:
                    if (cmd.equals("unlock-user")) {
                        i = 39;
                        break;
                    }
                case -892396682:
                    if (cmd.equals("start-foreground-service")) {
                        i = 6;
                        break;
                    }
                case -870018278:
                    if (cmd.equals("to-uri")) {
                        i = 33;
                        break;
                    }
                case -812219210:
                    if (cmd.equals("get-current-user")) {
                        i = 37;
                        break;
                    }
                case -747637291:
                    if (cmd.equals("set-standby-bucket")) {
                        i = 50;
                        break;
                    }
                case -699625063:
                    if (cmd.equals("get-config")) {
                        i = 46;
                        break;
                    }
                case -606123342:
                    if (cmd.equals("kill-all")) {
                        i = 24;
                        break;
                    }
                case -548621938:
                    if (cmd.equals("is-user-stopped")) {
                        i = 41;
                        break;
                    }
                case -387147436:
                    if (cmd.equals("track-associations")) {
                        i = 43;
                        break;
                    }
                case -354890749:
                    if (cmd.equals("screen-compat")) {
                        i = 31;
                        break;
                    }
                case -309425751:
                    if (cmd.equals("profile")) {
                        i = 13;
                        break;
                    }
                case -170987146:
                    if (cmd.equals("set-inactive")) {
                        i = 48;
                        break;
                    }
                case -146027423:
                    if (cmd.equals("watch-uids")) {
                        i = 27;
                        break;
                    }
                case -100644880:
                    if (cmd.equals("startforegroundservice")) {
                        i = 4;
                        break;
                    }
                case -27715536:
                    if (cmd.equals("make-uid-idle")) {
                        i = 25;
                        break;
                    }
                case 3194994:
                    if (cmd.equals("hang")) {
                        i = 28;
                        break;
                    }
                case 3291998:
                    if (cmd.equals("kill")) {
                        i = 23;
                        break;
                    }
                case 3552645:
                    if (cmd.equals("task")) {
                        i = 55;
                        break;
                    }
                case 88586660:
                    if (cmd.equals("force-stop")) {
                        i = 21;
                        break;
                    }
                case 94921639:
                    if (cmd.equals("crash")) {
                        i = 22;
                        break;
                    }
                case 109757064:
                    if (cmd.equals("stack")) {
                        i = 54;
                        break;
                    }
                case 109757538:
                    if (cmd.equals("start")) {
                        i = 0;
                        break;
                    }
                case 113399775:
                    if (cmd.equals("write")) {
                        i = 56;
                        break;
                    }
                case 185053203:
                    if (cmd.equals("startservice")) {
                        i = 2;
                        break;
                    }
                case 237240942:
                    if (cmd.equals("to-app-uri")) {
                        i = 35;
                        break;
                    }
                case 549617690:
                    if (cmd.equals("start-activity")) {
                        i = 1;
                        break;
                    }
                case 622433197:
                    if (cmd.equals("untrack-associations")) {
                        i = 44;
                        break;
                    }
                case 667014829:
                    if (cmd.equals("bug-report")) {
                        i = 20;
                        break;
                    }
                case 680834441:
                    if (cmd.equals("supports-split-screen-multi-window")) {
                        i = 59;
                        break;
                    }
                case 723112852:
                    if (cmd.equals("trace-ipc")) {
                        i = 12;
                        break;
                    }
                case 764545184:
                    if (cmd.equals("supports-multiwindow")) {
                        i = 58;
                        break;
                    }
                case 808179021:
                    if (cmd.equals("to-intent-uri")) {
                        i = 34;
                        break;
                    }
                case 810242677:
                    if (cmd.equals("set-watch-heap")) {
                        i = 18;
                        break;
                    }
                case 817137578:
                    if (cmd.equals("clear-watch-heap")) {
                        i = 19;
                        break;
                    }
                case 822490030:
                    if (cmd.equals("set-agent-app")) {
                        i = 16;
                        break;
                    }
                case 900455412:
                    if (cmd.equals("start-fg-service")) {
                        i = 7;
                        break;
                    }
                case 1024703869:
                    if (cmd.equals("attach-agent")) {
                        i = 57;
                        break;
                    }
                case 1078591527:
                    if (cmd.equals("clear-debug-app")) {
                        i = 17;
                        break;
                    }
                case 1097506319:
                    if (cmd.equals("restart")) {
                        i = 29;
                        break;
                    }
                case 1129261387:
                    if (cmd.equals("update-appinfo")) {
                        i = 60;
                        break;
                    }
                case 1219773618:
                    if (cmd.equals("get-started-user-state")) {
                        i = 42;
                        break;
                    }
                case 1236319578:
                    if (cmd.equals("monitor")) {
                        i = 26;
                        break;
                    }
                case 1395483623:
                    if (cmd.equals("instrument")) {
                        i = 11;
                        break;
                    }
                case 1583986358:
                    if (cmd.equals("stop-user")) {
                        i = 40;
                        break;
                    }
                case 1618908732:
                    if (cmd.equals("wait-for-broadcast-idle")) {
                        i = 62;
                        break;
                    }
                case 1671764162:
                    if (cmd.equals("display")) {
                        i = 53;
                        break;
                    }
                case 1852789518:
                    if (cmd.equals("no-home-screen")) {
                        i = 61;
                        break;
                    }
                case 1861559962:
                    if (cmd.equals("idle-maintenance")) {
                        i = 30;
                        break;
                    }
                case 1863290858:
                    if (cmd.equals("stop-service")) {
                        i = 9;
                        break;
                    }
                case 2083239620:
                    if (cmd.equals("switch-user")) {
                        i = 36;
                        break;
                    }
                default:
            }
            i = -1;
            switch (i) {
                case 0:
                case 1:
                    return runStartActivity(pw);
                case 2:
                case 3:
                    return runStartService(pw, false);
                case 4:
                case 5:
                case 6:
                case 7:
                    return runStartService(pw, true);
                case 8:
                case 9:
                    return runStopService(pw);
                case 10:
                    return runSendBroadcast(pw);
                case 11:
                    getOutPrintWriter().println("Error: must be invoked through 'am instrument'.");
                    return -1;
                case 12:
                    return runTraceIpc(pw);
                case 13:
                    return runProfile(pw);
                case 14:
                    return runDumpHeap(pw);
                case 15:
                    return runSetDebugApp(pw);
                case 16:
                    return runSetAgentApp(pw);
                case 17:
                    return runClearDebugApp(pw);
                case 18:
                    return runSetWatchHeap(pw);
                case H.REPORT_WINDOWS_CHANGE /*19*/:
                    return runClearWatchHeap(pw);
                case 20:
                    return runBugReport(pw);
                case BackupHandler.MSG_OP_COMPLETE /*21*/:
                    return runForceStop(pw);
                case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                    return runCrash(pw);
                case H.BOOT_TIMEOUT /*23*/:
                    return runKill(pw);
                case 24:
                    return runKillAll(pw);
                case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                    return runMakeIdle(pw);
                case H.DO_ANIMATION_CALLBACK /*26*/:
                    return runMonitor(pw);
                case 27:
                    return runWatchUids(pw);
                case NetworkConstants.ARP_PAYLOAD_LEN /*28*/:
                    return runHang(pw);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_ENTRY_MODE /*29*/:
                    return runRestart(pw);
                case 30:
                    return runIdleMaintenance(pw);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_12 /*31*/:
                    return runScreenCompat(pw);
                case 32:
                    return runPackageImportance(pw);
                case 33:
                    return runToUri(pw, 0);
                case 34:
                    return runToUri(pw, 1);
                case 35:
                    return runToUri(pw, 2);
                case 36:
                    return runSwitchUser(pw);
                case 37:
                    return runGetCurrentUser(pw);
                case 38:
                    return runStartUser(pw);
                case 39:
                    return runUnlockUser(pw);
                case 40:
                    return runStopUser(pw);
                case 41:
                    return runIsUserStopped(pw);
                case HdmiCecKeycode.CEC_KEYCODE_DOT /*42*/:
                    return runGetStartedUserState(pw);
                case HdmiCecKeycode.CEC_KEYCODE_ENTER /*43*/:
                    return runTrackAssociations(pw);
                case HdmiCecKeycode.CEC_KEYCODE_CLEAR /*44*/:
                    return runUntrackAssociations(pw);
                case NetworkPolicyManagerService.TYPE_RAPID /*45*/:
                    return getUidState(pw);
                case H.WINDOW_REPLACEMENT_TIMEOUT /*46*/:
                    return runGetConfig(pw);
                case 47:
                    return runSuppressResizeConfigChanges(pw);
                case 48:
                    return runSetInactive(pw);
                case 49:
                    return runGetInactive(pw);
                case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL /*50*/:
                    return runSetStandbyBucket(pw);
                case 51:
                    return runGetStandbyBucket(pw);
                case 52:
                    return runSendTrimMemory(pw);
                case 53:
                    return runDisplay(pw);
                case 54:
                    return runStack(pw);
                case 55:
                    return runTask(pw);
                case 56:
                    return runWrite(pw);
                case H.NOTIFY_KEYGUARD_TRUSTED_CHANGED /*57*/:
                    return runAttachAgent(pw);
                case H.SET_HAS_OVERLAY_UI /*58*/:
                    return runSupportsMultiwindow(pw);
                case H.SET_RUNNING_REMOTE_ANIMATION /*59*/:
                    return runSupportsSplitScreenMultiwindow(pw);
                case 60:
                    return runUpdateApplicationInfo(pw);
                case H.RECOMPUTE_FOCUS /*61*/:
                    return runNoHomeScreen(pw);
                case 62:
                    return runWaitForBroadcastIdle(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private Intent makeIntent(int defUser) throws URISyntaxException {
        this.mStartFlags = 0;
        this.mWaitOption = false;
        this.mStopOption = false;
        this.mRepeat = 0;
        this.mProfileFile = null;
        this.mSamplingInterval = 0;
        this.mAutoStop = false;
        this.mStreaming = false;
        this.mUserId = defUser;
        this.mDisplayId = -1;
        this.mWindowingMode = 0;
        this.mActivityType = 0;
        this.mTaskId = -1;
        this.mIsTaskOverlay = false;
        this.mIsLockTask = false;
        return Intent.parseCommandArgs(this, new CommandOptionHandler() {
            public boolean handleOption(String opt, ShellCommand cmd) {
                if (opt.equals("-D")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 2);
                } else if (opt.equals("-N")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 8);
                } else if (opt.equals("-W")) {
                    ActivityManagerShellCommand.this.mWaitOption = true;
                } else if (opt.equals("-P")) {
                    ActivityManagerShellCommand.this.mProfileFile = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAutoStop = true;
                } else if (opt.equals("--start-profiler")) {
                    ActivityManagerShellCommand.this.mProfileFile = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAutoStop = false;
                } else if (opt.equals("--sampling")) {
                    ActivityManagerShellCommand.this.mSamplingInterval = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--streaming")) {
                    ActivityManagerShellCommand.this.mStreaming = true;
                } else if (opt.equals("--attach-agent")) {
                    if (ActivityManagerShellCommand.this.mAgent != null) {
                        cmd.getErrPrintWriter().println("Multiple --attach-agent(-bind) not supported");
                        return false;
                    }
                    ActivityManagerShellCommand.this.mAgent = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAttachAgentDuringBind = false;
                } else if (opt.equals("--attach-agent-bind")) {
                    if (ActivityManagerShellCommand.this.mAgent != null) {
                        cmd.getErrPrintWriter().println("Multiple --attach-agent(-bind) not supported");
                        return false;
                    }
                    ActivityManagerShellCommand.this.mAgent = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAttachAgentDuringBind = true;
                } else if (opt.equals("-R")) {
                    ActivityManagerShellCommand.this.mRepeat = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("-S")) {
                    ActivityManagerShellCommand.this.mStopOption = true;
                } else if (opt.equals("--track-allocation")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 4);
                } else if (opt.equals("--user")) {
                    ActivityManagerShellCommand.this.mUserId = UserHandle.parseUserArg(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--receiver-permission")) {
                    ActivityManagerShellCommand.this.mReceiverPermission = ActivityManagerShellCommand.this.getNextArgRequired();
                } else if (opt.equals("--display")) {
                    ActivityManagerShellCommand.this.mDisplayId = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--windowingMode")) {
                    ActivityManagerShellCommand.this.mWindowingMode = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--activityType")) {
                    ActivityManagerShellCommand.this.mActivityType = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--task")) {
                    ActivityManagerShellCommand.this.mTaskId = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (opt.equals("--task-overlay")) {
                    ActivityManagerShellCommand.this.mIsTaskOverlay = true;
                } else if (!opt.equals("--lock-task")) {
                    return false;
                } else {
                    ActivityManagerShellCommand.this.mIsLockTask = true;
                }
                return true;
            }
        });
    }

    int runStartActivity(PrintWriter pw) throws RemoteException {
        PrintWriter printWriter = pw;
        try {
            Intent intent = makeIntent(-2);
            int i = -1;
            int i2 = 1;
            if (this.mUserId == -1) {
                getErrPrintWriter().println("Error: Can't start service with user 'all'");
                return 1;
            }
            PrintWriter errPrintWriter;
            StringBuilder stringBuilder;
            String mimeType = intent.getType();
            if (mimeType == null && intent.getData() != null && "content".equals(intent.getData().getScheme())) {
                mimeType = this.mInterface.getProviderMimeType(intent.getData(), this.mUserId);
            }
            String mimeType2 = mimeType;
            while (true) {
                int i3;
                String mimeType3;
                int i4;
                int i5;
                int res;
                if (this.mStopOption) {
                    if (intent.getComponent() != null) {
                        mimeType = intent.getComponent().getPackageName();
                    } else {
                        List<ResolveInfo> activities = this.mPm.queryIntentActivities(intent, mimeType2, 0, this.mUserId).getList();
                        if (activities == null || activities.size() <= 0) {
                            errPrintWriter = getErrPrintWriter();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: Intent does not match any activities: ");
                            stringBuilder.append(intent);
                            errPrintWriter.println(stringBuilder.toString());
                        } else if (activities.size() > i2) {
                            errPrintWriter = getErrPrintWriter();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: Intent matches multiple activities; can't stop: ");
                            stringBuilder.append(intent);
                            errPrintWriter.println(stringBuilder.toString());
                            return i2;
                        } else {
                            mimeType = ((ResolveInfo) activities.get(0)).activityInfo.packageName;
                        }
                    }
                    String packageName = mimeType;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Stopping: ");
                    stringBuilder2.append(packageName);
                    printWriter.println(stringBuilder2.toString());
                    pw.flush();
                    this.mInterface.forceStopPackage(packageName, this.mUserId);
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                    }
                }
                ProfilerInfo profilerInfo = null;
                if (!(this.mProfileFile == null && this.mAgent == null)) {
                    ParcelFileDescriptor fd = null;
                    if (this.mProfileFile != null) {
                        fd = openFileForSystem(this.mProfileFile, "w");
                        if (fd == null) {
                            return i2;
                        }
                    }
                    profilerInfo = new ProfilerInfo(this.mProfileFile, fd, this.mSamplingInterval, this.mAutoStop, this.mStreaming, this.mAgent, this.mAttachAgentDuringBind);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Starting: ");
                stringBuilder3.append(intent);
                printWriter.println(stringBuilder3.toString());
                pw.flush();
                intent.addFlags(268435456);
                WaitResult result = null;
                long startTime = SystemClock.uptimeMillis();
                ActivityOptions options = null;
                if (this.mDisplayId != i) {
                    options = ActivityOptions.makeBasic();
                    options.setLaunchDisplayId(this.mDisplayId);
                }
                if (this.mWindowingMode != 0) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLaunchWindowingMode(this.mWindowingMode);
                }
                if (this.mActivityType != 0) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLaunchActivityType(this.mActivityType);
                }
                if (this.mTaskId != i) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLaunchTaskId(this.mTaskId);
                    if (this.mIsTaskOverlay) {
                        options.setTaskOverlay(i2, i2);
                    }
                }
                if (this.mIsLockTask) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLockTaskEnabled(i2);
                }
                ActivityOptions options2 = options;
                Bundle bundle = null;
                if (this.mWaitOption) {
                    WaitResult result2 = this.mInterface;
                    int i6 = this.mStartFlags;
                    if (options2 != null) {
                        bundle = options2.toBundle();
                    }
                    i3 = 0;
                    mimeType3 = mimeType2;
                    i4 = i2;
                    i5 = i;
                    result2 = result2.startActivityAndWait(null, null, intent, mimeType2, null, null, 0, i6, profilerInfo, bundle, this.mUserId);
                    res = result2.result;
                    result = result2;
                } else {
                    ActivityOptions options3 = options2;
                    i3 = 0;
                    mimeType3 = mimeType2;
                    i4 = i2;
                    i5 = i;
                    IActivityManager iActivityManager = this.mInterface;
                    int i7 = this.mStartFlags;
                    ActivityOptions options4 = options3;
                    if (options4 != null) {
                        bundle = options4.toBundle();
                    }
                    options3 = options4;
                    res = iActivityManager.startActivityAsUser(null, null, intent, mimeType3, null, null, 0, i7, profilerInfo, bundle, this.mUserId);
                }
                int res2 = res;
                long endTime = SystemClock.uptimeMillis();
                PrintWriter out = this.mWaitOption ? printWriter : getErrPrintWriter();
                boolean launched = false;
                if (res2 != 100) {
                    StringBuilder stringBuilder4;
                    switch (res2) {
                        case -98:
                            out.println("Error: Not allowed to start background user activity that shouldn't be displayed for all users.");
                            break;
                        case -97:
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Error: Activity not started, voice control not allowed for: ");
                            stringBuilder4.append(intent);
                            out.println(stringBuilder4.toString());
                            break;
                        default:
                            switch (res2) {
                                case -94:
                                    out.println("Error: Activity not started, you do not have permission to access it.");
                                    break;
                                case -93:
                                    out.println("Error: Activity not started, you requested to both forward and receive its result");
                                    break;
                                case -92:
                                    out.println(NO_CLASS_ERROR_CODE);
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Error: Activity class ");
                                    stringBuilder4.append(intent.getComponent().toShortString());
                                    stringBuilder4.append(" does not exist.");
                                    out.println(stringBuilder4.toString());
                                    break;
                                case -91:
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Error: Activity not started, unable to resolve ");
                                    stringBuilder4.append(intent.toString());
                                    out.println(stringBuilder4.toString());
                                    break;
                                default:
                                    switch (res2) {
                                        case 0:
                                            launched = true;
                                            break;
                                        case 1:
                                            launched = true;
                                            out.println("Warning: Activity not started because intent should be handled by the caller");
                                            break;
                                        case 2:
                                            launched = true;
                                            out.println("Warning: Activity not started, its current task has been brought to the front");
                                            break;
                                        case 3:
                                            launched = true;
                                            out.println("Warning: Activity not started, intent has been delivered to currently running top-most instance.");
                                            break;
                                        default:
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("Error: Activity not started, unknown error code ");
                                            stringBuilder4.append(res2);
                                            out.println(stringBuilder4.toString());
                                            break;
                                    }
                            }
                    }
                }
                launched = true;
                out.println("Warning: Activity not started because the  current activity is being kept for the user.");
                out.flush();
                if (this.mWaitOption && launched) {
                    WaitResult result3;
                    if (result == null) {
                        result3 = new WaitResult();
                        result3.who = intent.getComponent();
                    } else {
                        result3 = result;
                    }
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Status: ");
                    stringBuilder5.append(result3.timeout ? "timeout" : "ok");
                    printWriter.println(stringBuilder5.toString());
                    if (result3.who != null) {
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Activity: ");
                        stringBuilder5.append(result3.who.flattenToShortString());
                        printWriter.println(stringBuilder5.toString());
                    }
                    if (result3.thisTime >= 0) {
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("ThisTime: ");
                        stringBuilder5.append(result3.thisTime);
                        printWriter.println(stringBuilder5.toString());
                    }
                    if (result3.totalTime >= 0) {
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("TotalTime: ");
                        stringBuilder5.append(result3.totalTime);
                        printWriter.println(stringBuilder5.toString());
                    }
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("WaitTime: ");
                    stringBuilder5.append(endTime - startTime);
                    printWriter.println(stringBuilder5.toString());
                    printWriter.println("Complete");
                    pw.flush();
                    result = result3;
                }
                this.mRepeat--;
                if (this.mRepeat > 0) {
                    this.mInterface.unhandledBack();
                }
                if (this.mRepeat <= 0) {
                    return i3;
                }
                i = i5;
                mimeType2 = mimeType3;
                i2 = i4;
            }
            errPrintWriter = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: Intent does not match any activities: ");
            stringBuilder.append(intent);
            errPrintWriter.println(stringBuilder.toString());
            return i2;
        } catch (URISyntaxException e2) {
            URISyntaxException uRISyntaxException = e2;
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    int runStartService(PrintWriter pw, boolean asForeground) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        try {
            Intent intent = makeIntent(-2);
            if (this.mUserId == -1) {
                err.println("Error: Can't start activity with user 'all'");
                return -1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting service: ");
            stringBuilder.append(intent);
            pw.println(stringBuilder.toString());
            pw.flush();
            ComponentName cn = this.mInterface.startService(null, intent, intent.getType(), asForeground, SHELL_PACKAGE_NAME, this.mUserId);
            StringBuilder stringBuilder2;
            if (cn == null) {
                err.println("Error: Not found; no service started.");
                return -1;
            } else if (cn.getPackageName().equals("!")) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Requires permission ");
                stringBuilder2.append(cn.getClassName());
                err.println(stringBuilder2.toString());
                return -1;
            } else if (cn.getPackageName().equals("!!")) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: ");
                stringBuilder2.append(cn.getClassName());
                err.println(stringBuilder2.toString());
                return -1;
            } else if (!cn.getPackageName().equals("?")) {
                return 0;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: ");
                stringBuilder2.append(cn.getClassName());
                err.println(stringBuilder2.toString());
                return -1;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runStopService(PrintWriter pw) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        try {
            Intent intent = makeIntent(-2);
            if (this.mUserId == -1) {
                err.println("Error: Can't stop activity with user 'all'");
                return -1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stopping service: ");
            stringBuilder.append(intent);
            pw.println(stringBuilder.toString());
            pw.flush();
            int result = this.mInterface.stopService(null, intent, intent.getType(), this.mUserId);
            if (result == 0) {
                err.println("Service not stopped: was not running.");
                return -1;
            } else if (result == 1) {
                err.println("Service stopped");
                return -1;
            } else if (result != -1) {
                return 0;
            } else {
                err.println("Error stopping service");
                return -1;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runSendBroadcast(PrintWriter pw) throws RemoteException {
        PrintWriter printWriter = pw;
        try {
            Intent intent = makeIntent(-2);
            intent.addFlags(DumpState.DUMP_CHANGES);
            IntentReceiver receiver = new IntentReceiver(printWriter);
            String[] requiredPermissions = this.mReceiverPermission == null ? null : new String[]{this.mReceiverPermission};
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting: ");
            stringBuilder.append(intent);
            printWriter.println(stringBuilder.toString());
            pw.flush();
            IntentReceiver receiver2 = receiver;
            this.mInterface.broadcastIntent(null, intent, null, receiver, 0, null, null, requiredPermissions, -1, null, true, false, this.mUserId);
            receiver2.waitForFinish();
            return 0;
        } catch (URISyntaxException e) {
            URISyntaxException uRISyntaxException = e;
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runTraceIpc(PrintWriter pw) throws RemoteException {
        String op = getNextArgRequired();
        if (op.equals("start")) {
            return runTraceIpcStart(pw);
        }
        if (op.equals("stop")) {
            return runTraceIpcStop(pw);
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error: unknown trace ipc command '");
        stringBuilder.append(op);
        stringBuilder.append("'");
        errPrintWriter.println(stringBuilder.toString());
        return -1;
    }

    int runTraceIpcStart(PrintWriter pw) throws RemoteException {
        pw.println("Starting IPC tracing.");
        pw.flush();
        this.mInterface.startBinderTracking();
        return 0;
    }

    int runTraceIpcStop(PrintWriter pw) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        String filename = null;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                if (opt.equals("--dump-file")) {
                    filename = getNextArgRequired();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return -1;
                }
            } else if (filename == null) {
                err.println("Error: Specify filename to dump logs to.");
                return -1;
            } else {
                new File(filename).delete();
                ParcelFileDescriptor fd = openFileForSystem(filename, "w");
                if (fd == null) {
                    return -1;
                }
                if (this.mInterface.stopBinderTrackingAndDump(fd)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Stopped IPC tracing. Dumping logs to: ");
                    stringBuilder2.append(filename);
                    pw.println(stringBuilder2.toString());
                    return 0;
                }
                err.println("STOP TRACE FAILED.");
                return -1;
            }
        }
    }

    static void removeWallOption() {
        String props = SystemProperties.get("dalvik.vm.extra-opts");
        if (props != null && props.contains("-Xprofile:wallclock")) {
            SystemProperties.set("dalvik.vm.extra-opts", props.replace("-Xprofile:wallclock", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).trim());
        }
    }

    private int runProfile(PrintWriter pw) throws RemoteException {
        String process;
        boolean process2;
        PrintWriter err = getErrPrintWriter();
        String profileFile = null;
        String process22 = null;
        String cmd = null;
        int userId = -2;
        this.mSamplingInterval = 0;
        this.mStreaming = false;
        String cmd2 = getNextArgRequired();
        String nextOption;
        String opt;
        StringBuilder stringBuilder;
        if ("start".equals(cmd2)) {
            process22 = true;
            while (true) {
                nextOption = getNextOption();
                opt = nextOption;
                if (nextOption == null) {
                    process = getNextArgRequired();
                    break;
                } else if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (opt.equals("--wall")) {
                    cmd = true;
                } else if (opt.equals("--streaming")) {
                    this.mStreaming = true;
                } else if (opt.equals("--sampling")) {
                    this.mSamplingInterval = Integer.parseInt(getNextArgRequired());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return -1;
                }
            }
        } else if ("stop".equals(cmd2)) {
            while (true) {
                nextOption = getNextOption();
                opt = nextOption;
                if (nextOption == null) {
                    process = getNextArg();
                    break;
                } else if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return -1;
                }
            }
        } else {
            process = cmd2;
            cmd2 = getNextArgRequired();
            if ("start".equals(cmd2)) {
                process22 = true;
            } else if (!"stop".equals(cmd2)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Profile command ");
                stringBuilder2.append(process);
                stringBuilder2.append(" not valid");
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
        boolean start = process22;
        boolean wall = cmd;
        process22 = process;
        boolean wall2 = cmd2;
        if (userId == -1) {
            err.println("Error: Can't profile with user 'all'");
            return -1;
        }
        ParcelFileDescriptor fd = null;
        ProfilerInfo profilerInfo = null;
        if (start) {
            profileFile = getNextArgRequired();
            fd = openFileForSystem(profileFile, "w");
            if (fd == null) {
                return -1;
            }
            profilerInfo = new ProfilerInfo(profileFile, fd, this.mSamplingInterval, false, this.mStreaming, null, false);
        }
        ProfilerInfo profilerInfo2 = profilerInfo;
        if (wall) {
            try {
                profileFile = SystemProperties.get("dalvik.vm.extra-opts");
                if (profileFile == null || !profileFile.contains("-Xprofile:wallclock")) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(profileFile);
                    stringBuilder3.append(" -Xprofile:wallclock");
                    stringBuilder3.toString();
                }
            } catch (Throwable th) {
            }
        }
        if (this.mInterface.profileControl(process22, userId, start, profilerInfo2, 0)) {
            return 0;
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("PROFILE FAILED on process ");
        stringBuilder4.append(process22);
        err.println(stringBuilder4.toString());
        return -1;
    }

    int runDumpHeap(PrintWriter pw) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        boolean managed = true;
        boolean mallocInfo = false;
        int userId = -2;
        boolean runGc = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                String process = getNextArgRequired();
                String nextArgRequired = getNextArgRequired();
                File file = new File(nextArgRequired);
                file.delete();
                ParcelFileDescriptor fd = openFileForSystem(nextArgRequired, "w");
                if (fd == null) {
                    return -1;
                }
                String heapFile = nextArgRequired;
                String process2 = process;
                if (this.mInterface.dumpHeap(process, userId, managed, mallocInfo, runGc, nextArgRequired, fd)) {
                    return 0;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HEAP DUMP FAILED on process ");
                stringBuilder.append(process2);
                err.println(stringBuilder.toString());
                return -1;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
                if (userId == -1) {
                    err.println("Error: Can't dump heap with user 'all'");
                    return -1;
                }
            } else if (opt.equals("-n")) {
                managed = false;
            } else if (opt.equals("-g")) {
                runGc = true;
            } else if (opt.equals("-m")) {
                managed = false;
                mallocInfo = true;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Unknown option: ");
                stringBuilder2.append(opt);
                err.println(stringBuilder2.toString());
                return -1;
            }
        }
    }

    int runSetDebugApp(PrintWriter pw) throws RemoteException {
        boolean wait = false;
        boolean persistent = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mInterface.setDebugApp(getNextArgRequired(), wait, persistent);
                return 0;
            } else if (opt.equals("-w")) {
                wait = true;
            } else if (opt.equals("--persistent")) {
                persistent = true;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runSetAgentApp(PrintWriter pw) throws RemoteException {
        this.mInterface.setAgentApp(getNextArgRequired(), getNextArg());
        return 0;
    }

    int runClearDebugApp(PrintWriter pw) throws RemoteException {
        this.mInterface.setDebugApp(null, false, true);
        return 0;
    }

    int runSetWatchHeap(PrintWriter pw) throws RemoteException {
        String proc = getNextArgRequired();
        String str = proc;
        this.mInterface.setDumpHeapDebugLimit(str, 0, Long.parseLong(getNextArgRequired()), null);
        return 0;
    }

    int runClearWatchHeap(PrintWriter pw) throws RemoteException {
        this.mInterface.setDumpHeapDebugLimit(getNextArgRequired(), 0, -1, null);
        return 0;
    }

    int runBugReport(PrintWriter pw) throws RemoteException {
        int bugreportType = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mInterface.requestBugReport(bugreportType);
                pw.println("Your lovely bug report is being created; please be patient.");
                return 0;
            } else if (opt.equals("--progress")) {
                bugreportType = 1;
            } else if (opt.equals("--telephony")) {
                bugreportType = 4;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runForceStop(PrintWriter pw) throws RemoteException {
        int userId = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mInterface.forceStopPackage(getNextArgRequired(), userId);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runCrash(PrintWriter pw) throws RemoteException {
        int userId = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                int pid = -1;
                nextOption = null;
                String arg = getNextArgRequired();
                try {
                    pid = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    nextOption = arg;
                }
                this.mInterface.crashApplication(-1, pid, nextOption, userId, "shell-induced crash");
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runKill(PrintWriter pw) throws RemoteException {
        int userId = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mInterface.killBackgroundProcesses(getNextArgRequired(), userId);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runKillAll(PrintWriter pw) throws RemoteException {
        this.mInterface.killAllBackgroundProcesses();
        return 0;
    }

    int runMakeIdle(PrintWriter pw) throws RemoteException {
        int userId = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mInterface.makePackageIdle(getNextArgRequired(), userId);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runMonitor(PrintWriter pw) throws RemoteException {
        String gdbPort = null;
        boolean monkey = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                new MyActivityController(this.mInterface, pw, getRawInputStream(), gdbPort, monkey).run();
                return 0;
            } else if (opt.equals("--gdb")) {
                gdbPort = getNextArgRequired();
            } else if (opt.equals("-m")) {
                monkey = true;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runWatchUids(PrintWriter pw) throws RemoteException {
        int uid = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                new MyUidObserver(this.mInternal, pw, getRawInputStream(), uid).run();
                return 0;
            } else if (opt.equals("--oom")) {
                uid = Integer.parseInt(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runHang(PrintWriter pw) throws RemoteException {
        boolean allowRestart = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                pw.println("Hanging the system...");
                pw.flush();
                this.mInterface.hang(new Binder(), allowRestart);
                return 0;
            } else if (opt.equals("--allow-restart")) {
                allowRestart = true;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runRestart(PrintWriter pw) throws RemoteException {
        String nextOption = getNextOption();
        String opt = nextOption;
        if (nextOption != null) {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: Unknown option: ");
            stringBuilder.append(opt);
            errPrintWriter.println(stringBuilder.toString());
            return -1;
        }
        pw.println("Restart the system...");
        pw.flush();
        this.mInterface.restart();
        return 0;
    }

    int runIdleMaintenance(PrintWriter pw) throws RemoteException {
        String nextOption = getNextOption();
        String opt = nextOption;
        if (nextOption != null) {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: Unknown option: ");
            stringBuilder.append(opt);
            errPrintWriter.println(stringBuilder.toString());
            return -1;
        }
        pw.println("Performing idle maintenance...");
        this.mInterface.sendIdleJobTrigger();
        return 0;
    }

    int runScreenCompat(PrintWriter pw) throws RemoteException {
        boolean enabled;
        String mode = getNextArgRequired();
        if ("on".equals(mode)) {
            enabled = true;
        } else if ("off".equals(mode)) {
            enabled = false;
        } else {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: enabled mode must be 'on' or 'off' at ");
            stringBuilder.append(mode);
            errPrintWriter.println(stringBuilder.toString());
            return -1;
        }
        String packageName = getNextArgRequired();
        do {
            try {
                int i;
                IActivityManager iActivityManager = this.mInterface;
                if (enabled) {
                    i = 1;
                } else {
                    i = 0;
                }
                iActivityManager.setPackageScreenCompatMode(packageName, i);
            } catch (RemoteException e) {
            }
            packageName = getNextArg();
        } while (packageName != null);
        return 0;
    }

    int runPackageImportance(PrintWriter pw) throws RemoteException {
        pw.println(RunningAppProcessInfo.procStateToImportance(this.mInterface.getPackageProcessState(getNextArgRequired(), SHELL_PACKAGE_NAME)));
        return 0;
    }

    int runToUri(PrintWriter pw, int flags) throws RemoteException {
        try {
            pw.println(makeIntent(-2).toUri(flags));
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runSwitchUser(PrintWriter pw) throws RemoteException {
        if (((UserManager) this.mInternal.mContext.getSystemService(UserManager.class)).canSwitchUsers()) {
            this.mInterface.switchUser(Integer.parseInt(getNextArgRequired()));
            return 0;
        }
        getErrPrintWriter().println("Error: disallowed switching user");
        return -1;
    }

    int runGetCurrentUser(PrintWriter pw) throws RemoteException {
        pw.println(((UserInfo) Preconditions.checkNotNull(this.mInterface.getCurrentUser(), "Current user not set")).id);
        return 0;
    }

    int runStartUser(PrintWriter pw) throws RemoteException {
        if (this.mInterface.startUserInBackground(Integer.parseInt(getNextArgRequired()))) {
            pw.println("Success: user started");
        } else {
            getErrPrintWriter().println("Error: could not start user");
        }
        return 0;
    }

    private static byte[] argToBytes(String arg) {
        if (arg.equals("!")) {
            return null;
        }
        return HexDump.hexStringToByteArray(arg);
    }

    int runUnlockUser(PrintWriter pw) throws RemoteException {
        if (this.mInterface.unlockUser(Integer.parseInt(getNextArgRequired()), argToBytes(getNextArgRequired()), argToBytes(getNextArgRequired()), null)) {
            pw.println("Success: user unlocked");
        } else {
            getErrPrintWriter().println("Error: could not unlock user");
        }
        return 0;
    }

    int runStopUser(PrintWriter pw) throws RemoteException {
        boolean wait = false;
        boolean force = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                int user = Integer.parseInt(getNextArgRequired());
                StopUserCallback callback = wait ? new StopUserCallback() : null;
                int res = this.mInterface.stopUser(user, force, callback);
                if (res != 0) {
                    String txt = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    StringBuilder stringBuilder;
                    switch (res) {
                        case -4:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" (Can't stop user ");
                            stringBuilder.append(user);
                            stringBuilder.append(" - one of its related users can't be stopped)");
                            txt = stringBuilder.toString();
                            break;
                        case -3:
                            txt = " (System user cannot be stopped)";
                            break;
                        case -2:
                            txt = " (Can't stop current user)";
                            break;
                        case -1:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" (Unknown user ");
                            stringBuilder.append(user);
                            stringBuilder.append(")");
                            txt = stringBuilder.toString();
                            break;
                    }
                    PrintWriter errPrintWriter = getErrPrintWriter();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Switch failed: ");
                    stringBuilder2.append(res);
                    stringBuilder2.append(txt);
                    errPrintWriter.println(stringBuilder2.toString());
                    return -1;
                }
                if (callback != null) {
                    callback.waitForFinish();
                }
                return 0;
            } else if ("-w".equals(opt)) {
                wait = true;
            } else if ("-f".equals(opt)) {
                force = true;
            } else {
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error: unknown option: ");
                stringBuilder3.append(opt);
                errPrintWriter2.println(stringBuilder3.toString());
                return -1;
            }
        }
    }

    int runIsUserStopped(PrintWriter pw) {
        pw.println(this.mInternal.isUserStopped(UserHandle.parseUserArg(getNextArgRequired())));
        return 0;
    }

    int runGetStartedUserState(PrintWriter pw) throws RemoteException {
        this.mInternal.enforceCallingPermission("android.permission.DUMP", "runGetStartedUserState()");
        int userId = Integer.parseInt(getNextArgRequired());
        try {
            pw.println(this.mInternal.getStartedUserState(userId));
        } catch (NullPointerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User is not started: ");
            stringBuilder.append(userId);
            pw.println(stringBuilder.toString());
        }
        return 0;
    }

    int runTrackAssociations(PrintWriter pw) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        synchronized (this.mInternal) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mInternal.mTrackingAssociations) {
                    pw.println("Association tracking already enabled.");
                } else {
                    this.mInternal.mTrackingAssociations = true;
                    pw.println("Association tracking started.");
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return 0;
    }

    int runUntrackAssociations(PrintWriter pw) {
        int i = "registerUidObserver()";
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", i);
        synchronized (this.mInternal) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                i = 0;
                if (this.mInternal.mTrackingAssociations) {
                    this.mInternal.mTrackingAssociations = false;
                    this.mInternal.mAssociations.clear();
                    pw.println("Association tracking stopped.");
                } else {
                    pw.println("Association tracking not running.");
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return i;
    }

    int getUidState(PrintWriter pw) throws RemoteException {
        this.mInternal.enforceCallingPermission("android.permission.DUMP", "getUidState()");
        int state = this.mInternal.getUidState(Integer.parseInt(getNextArgRequired()));
        pw.print(state);
        pw.print(" (");
        pw.printf(DebugUtils.valueToString(ActivityManager.class, "PROCESS_STATE_", state), new Object[0]);
        pw.println(")");
        return 0;
    }

    private List<Configuration> getRecentConfigurations(int days) {
        IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
        long now = System.currentTimeMillis();
        try {
            ParceledListSlice<ConfigurationStats> configStatsSlice = usm.queryConfigurationStats(4, now - ((long) ((((days * 24) * 60) * 60) * 1000)), now, SHELL_PACKAGE_NAME);
            if (configStatsSlice == null) {
                return Collections.emptyList();
            }
            final ArrayMap<Configuration, Integer> recentConfigs = new ArrayMap();
            List<ConfigurationStats> configStatsList = configStatsSlice.getList();
            int configStatsListSize = configStatsList.size();
            for (int i = 0; i < configStatsListSize; i++) {
                ConfigurationStats stats = (ConfigurationStats) configStatsList.get(i);
                int indexOfKey = recentConfigs.indexOfKey(stats.getConfiguration());
                if (indexOfKey < 0) {
                    recentConfigs.put(stats.getConfiguration(), Integer.valueOf(stats.getActivationCount()));
                } else {
                    recentConfigs.setValueAt(indexOfKey, Integer.valueOf(((Integer) recentConfigs.valueAt(indexOfKey)).intValue() + stats.getActivationCount()));
                }
            }
            Comparator<Configuration> comparator = new Comparator<Configuration>() {
                public int compare(Configuration a, Configuration b) {
                    return ((Integer) recentConfigs.get(b)).compareTo((Integer) recentConfigs.get(a));
                }
            };
            ArrayList<Configuration> configs = new ArrayList(recentConfigs.size());
            configs.addAll(recentConfigs.keySet());
            Collections.sort(configs, comparator);
            return configs;
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    private static void addExtensionsForConfig(EGL10 egl, EGLDisplay display, EGLConfig config, int[] surfaceSize, int[] contextAttribs, Set<String> glExtensions) {
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs);
        if (context != EGL10.EGL_NO_CONTEXT) {
            EGLSurface surface = egl.eglCreatePbufferSurface(display, config, surfaceSize);
            if (surface == EGL10.EGL_NO_SURFACE) {
                egl.eglDestroyContext(display, context);
                return;
            }
            egl.eglMakeCurrent(display, surface, surface, context);
            String extensionList = GLES10.glGetString(7939);
            if (!TextUtils.isEmpty(extensionList)) {
                for (String extension : extensionList.split(" ")) {
                    glExtensions.add(extension);
                }
            }
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroySurface(display, surface);
            egl.eglDestroyContext(display, context);
        }
    }

    Set<String> getGlExtensionsFromDriver() {
        Set<String> glExtensions = new HashSet();
        EGL10 egl = (EGL10) EGLContext.getEGL();
        if (egl == null) {
            getErrPrintWriter().println("Warning: couldn't get EGL");
            return glExtensions;
        }
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, new int[2]);
        int i = 1;
        int[] numConfigs = new int[1];
        int i2 = 0;
        if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
            EGLConfig[] configs = new EGLConfig[numConfigs[0]];
            if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                int[] surfaceSize = new int[]{12375, 1, 12374, 1, 12344};
                int[] gles2 = new int[]{12440, 2, 12344};
                int[] attrib = new int[1];
                int i3 = 0;
                while (true) {
                    int i4 = i3;
                    EGLConfig[] configs2;
                    int[] numConfigs2;
                    if (i4 < numConfigs[i2]) {
                        int i5;
                        int[] attrib2;
                        int i6;
                        egl.eglGetConfigAttrib(display, configs[i4], 12327, attrib);
                        if (attrib[i2] != 12368) {
                            egl.eglGetConfigAttrib(display, configs[i4], 12339, attrib);
                            if ((attrib[i2] & i) != 0) {
                                egl.eglGetConfigAttrib(display, configs[i4], 12352, attrib);
                                if ((attrib[i2] & i) != 0) {
                                    i5 = i4;
                                    addExtensionsForConfig(egl, display, configs[i4], surfaceSize, null, glExtensions);
                                } else {
                                    i5 = i4;
                                }
                                if ((attrib[i2] & 4) != 0) {
                                    attrib2 = attrib;
                                    configs2 = configs;
                                    i6 = i2;
                                    EGLConfig eGLConfig = configs[i5];
                                    numConfigs2 = numConfigs;
                                    i4 = i;
                                    addExtensionsForConfig(egl, display, eGLConfig, surfaceSize, gles2, glExtensions);
                                } else {
                                    attrib2 = attrib;
                                    configs2 = configs;
                                    i6 = i2;
                                    numConfigs2 = numConfigs;
                                    i4 = i;
                                }
                                numConfigs = numConfigs2;
                                configs = configs2;
                                i2 = i6;
                                i = i4;
                                i3 = i5 + 1;
                                attrib = attrib2;
                            }
                        }
                        i5 = i4;
                        attrib2 = attrib;
                        configs2 = configs;
                        i6 = i2;
                        numConfigs2 = numConfigs;
                        i4 = i;
                        numConfigs = numConfigs2;
                        configs = configs2;
                        i2 = i6;
                        i = i4;
                        i3 = i5 + 1;
                        attrib = attrib2;
                    } else {
                        configs2 = configs;
                        numConfigs2 = numConfigs;
                        egl.eglTerminate(display);
                        return glExtensions;
                    }
                }
            }
            getErrPrintWriter().println("Warning: couldn't get EGL configs");
            return glExtensions;
        }
        getErrPrintWriter().println("Warning: couldn't get EGL config count");
        return glExtensions;
    }

    private void writeDeviceConfig(ProtoOutputStream protoOutputStream, long fieldId, PrintWriter pw, Configuration config, DisplayManager dm) {
        KeyguardManager kgm;
        ProtoOutputStream protoOutputStream2 = protoOutputStream;
        PrintWriter printWriter = pw;
        Point stableSize = dm.getStableDisplaySize();
        long token = -1;
        if (protoOutputStream2 != null) {
            token = protoOutputStream.start(fieldId);
            protoOutputStream2.write(1155346202625L, stableSize.x);
            protoOutputStream2.write(1155346202626L, stableSize.y);
            protoOutputStream2.write(1155346202627L, DisplayMetrics.DENSITY_DEVICE_STABLE);
        }
        if (printWriter != null) {
            printWriter.print("stable-width-px: ");
            printWriter.println(stableSize.x);
            printWriter.print("stable-height-px: ");
            printWriter.println(stableSize.y);
            printWriter.print("stable-density-dpi: ");
            printWriter.println(DisplayMetrics.DENSITY_DEVICE_STABLE);
        }
        MemInfoReader memreader = new MemInfoReader();
        memreader.readMemInfo();
        KeyguardManager kgm2 = (KeyguardManager) this.mInternal.mContext.getSystemService(KeyguardManager.class);
        if (protoOutputStream2 != null) {
            protoOutputStream2.write(1116691496964L, memreader.getTotalSize());
            protoOutputStream2.write(1133871366149L, ActivityManager.isLowRamDeviceStatic());
            protoOutputStream2.write(1155346202630L, Runtime.getRuntime().availableProcessors());
            protoOutputStream2.write(1133871366151L, kgm2.isDeviceSecure());
        }
        if (printWriter != null) {
            printWriter.print("total-ram: ");
            printWriter.println(memreader.getTotalSize());
            printWriter.print("low-ram: ");
            printWriter.println(ActivityManager.isLowRamDeviceStatic());
            printWriter.print("max-cores: ");
            printWriter.println(Runtime.getRuntime().availableProcessors());
            printWriter.print("has-secure-screen-lock: ");
            printWriter.println(kgm2.isDeviceSecure());
        }
        ConfigurationInfo configInfo = this.mInternal.getDeviceConfigurationInfo();
        if (configInfo.reqGlEsVersion != 0) {
            if (protoOutputStream2 != null) {
                protoOutputStream2.write(1155346202632L, configInfo.reqGlEsVersion);
            }
            if (printWriter != null) {
                printWriter.print("opengl-version: 0x");
                printWriter.println(Integer.toHexString(configInfo.reqGlEsVersion));
            }
        }
        Set<String> glExtensionsSet = getGlExtensionsFromDriver();
        String[] glExtensions = (String[]) glExtensionsSet.toArray(new String[glExtensionsSet.size()]);
        Arrays.sort(glExtensions);
        for (int i = 0; i < glExtensions.length; i++) {
            if (protoOutputStream2 != null) {
                protoOutputStream2.write(2237677961225L, glExtensions[i]);
            }
            if (printWriter != null) {
                printWriter.print("opengl-extensions: ");
                printWriter.println(glExtensions[i]);
            }
        }
        PackageManager pm = this.mInternal.mContext.getPackageManager();
        List<SharedLibraryInfo> slibs = pm.getSharedLibraries(0);
        Collections.sort(slibs, Comparator.comparing(-$$Lambda$ActivityManagerShellCommand$jVSWDZTj55yxOQmZSLdNsbUkMb4.INSTANCE));
        int i2 = 0;
        while (i2 < slibs.size()) {
            PackageManager pm2;
            if (protoOutputStream2 != null) {
                pm2 = pm;
                protoOutputStream2.write(2237677961226L, ((SharedLibraryInfo) slibs.get(i2)).getName());
            } else {
                pm2 = pm;
            }
            if (printWriter != null) {
                printWriter.print("shared-libraries: ");
                printWriter.println(((SharedLibraryInfo) slibs.get(i2)).getName());
            }
            i2++;
            pm = pm2;
        }
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        Arrays.sort(features, -$$Lambda$ActivityManagerShellCommand$yu115wjRB5hvRTjVM9oePAy5cM0.INSTANCE);
        int i3 = 0;
        while (true) {
            i2 = i3;
            if (i2 >= features.length) {
                break;
            }
            MemInfoReader memreader2;
            if (features[i2].name != null) {
                if (protoOutputStream2 != null) {
                    memreader2 = memreader;
                    kgm = kgm2;
                    protoOutputStream2.write(2237677961227L, features[i2].name);
                } else {
                    memreader2 = memreader;
                    kgm = kgm2;
                }
                if (printWriter != null) {
                    printWriter.print("features: ");
                    printWriter.println(features[i2].name);
                }
            } else {
                memreader2 = memreader;
                kgm = kgm2;
            }
            i3 = i2 + 1;
            memreader = memreader2;
            kgm2 = kgm;
        }
        kgm = kgm2;
        if (protoOutputStream2 != null) {
            protoOutputStream2.end(token);
        }
    }

    static /* synthetic */ int lambda$writeDeviceConfig$0(FeatureInfo o1, FeatureInfo o2) {
        if (o1.name == o2.name) {
            return 0;
        }
        return o1.name == null ? -1 : o1.name.compareTo(o2.name);
    }

    int runGetConfig(PrintWriter pw) throws RemoteException {
        PrintWriter printWriter = pw;
        int days = -1;
        boolean asProto = false;
        boolean inclDevice = false;
        while (true) {
            boolean inclDevice2 = inclDevice;
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                if (opt.equals("--days")) {
                    days = Integer.parseInt(getNextArgRequired());
                    if (days <= 0) {
                        throw new IllegalArgumentException("--days must be a positive integer");
                    }
                } else if (opt.equals(PriorityDump.PROTO_ARG)) {
                    asProto = true;
                } else if (opt.equals("--device")) {
                    inclDevice = true;
                } else {
                    PrintWriter errPrintWriter = getErrPrintWriter();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    errPrintWriter.println(stringBuilder.toString());
                    return -1;
                }
                inclDevice = inclDevice2;
            } else {
                Configuration config = this.mInterface.getConfiguration();
                if (config == null) {
                    getErrPrintWriter().println("Activity manager has no configuration");
                    return -1;
                }
                DisplayManager dm = (DisplayManager) this.mInternal.mContext.getSystemService(DisplayManager.class);
                Display display = dm.getDisplay(0);
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                DisplayMetrics metrics2;
                Display display2;
                if (asProto) {
                    ProtoOutputStream proto;
                    ProtoOutputStream proto2 = new ProtoOutputStream(getOutFileDescriptor());
                    config.writeResConfigToProto(proto2, 1146756268033L, metrics);
                    if (inclDevice2) {
                        proto = proto2;
                        metrics2 = metrics;
                        writeDeviceConfig(proto2, 1146756268034L, null, config, dm);
                    } else {
                        proto = proto2;
                        metrics2 = metrics;
                        display2 = display;
                    }
                    proto.flush();
                    DisplayMetrics displayMetrics = metrics2;
                } else {
                    metrics2 = metrics;
                    display2 = display;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("config: ");
                    stringBuilder2.append(Configuration.resourceQualifierString(config, metrics2));
                    printWriter.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("abi: ");
                    stringBuilder2.append(TextUtils.join(",", Build.SUPPORTED_ABIS));
                    printWriter.println(stringBuilder2.toString());
                    if (inclDevice2) {
                        writeDeviceConfig(null, -1, printWriter, config, dm);
                    }
                    if (days >= 0) {
                        List<Configuration> recentConfigs = getRecentConfigurations(days);
                        int recentConfigSize = recentConfigs.size();
                        if (recentConfigSize > 0) {
                            printWriter.println("recentConfigs:");
                            for (int i = 0; i < recentConfigSize; i++) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("  config: ");
                                stringBuilder3.append(Configuration.resourceQualifierString((Configuration) recentConfigs.get(i)));
                                printWriter.println(stringBuilder3.toString());
                            }
                        }
                    }
                }
                return 0;
            }
        }
    }

    int runSuppressResizeConfigChanges(PrintWriter pw) throws RemoteException {
        this.mInterface.suppressResizeConfigChanges(Boolean.valueOf(getNextArgRequired()).booleanValue());
        return 0;
    }

    int runSetInactive(PrintWriter pw) throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats")).setAppInactive(getNextArgRequired(), Boolean.parseBoolean(getNextArgRequired()), userId);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    private int bucketNameToBucketValue(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("ac")) {
            return 10;
        }
        if (lower.startsWith("wo")) {
            return 20;
        }
        if (lower.startsWith("fr")) {
            return 30;
        }
        if (lower.startsWith("ra")) {
            return 40;
        }
        if (lower.startsWith("ne")) {
            return 50;
        }
        try {
            return Integer.parseInt(lower);
        } catch (NumberFormatException e) {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: Unknown bucket: ");
            stringBuilder.append(name);
            errPrintWriter.println(stringBuilder.toString());
            return -1;
        }
    }

    int runSetStandbyBucket(PrintWriter pw) throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                nextOption = getNextArgRequired();
                String value = getNextArgRequired();
                int bucket = bucketNameToBucketValue(value);
                if (bucket < 0) {
                    return -1;
                }
                boolean multiple = peekNextArg() != null;
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
                if (multiple) {
                    ArrayList<AppStandbyInfo> bucketInfoList = new ArrayList();
                    bucketInfoList.add(new AppStandbyInfo(nextOption, bucket));
                    while (true) {
                        String nextArg = getNextArg();
                        nextOption = nextArg;
                        if (nextArg == null) {
                            break;
                        }
                        bucket = bucketNameToBucketValue(getNextArgRequired());
                        if (bucket >= 0) {
                            bucketInfoList.add(new AppStandbyInfo(nextOption, bucket));
                        }
                    }
                    usm.setAppStandbyBuckets(new ParceledListSlice(bucketInfoList), userId);
                } else {
                    usm.setAppStandbyBucket(nextOption, bucketNameToBucketValue(value), userId);
                }
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runGetStandbyBucket(PrintWriter pw) throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                nextOption = getNextArg();
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
                if (nextOption != null) {
                    pw.println(usm.getAppStandbyBucket(nextOption, 0, userId));
                } else {
                    for (AppStandbyInfo bucketInfo : usm.getAppStandbyBuckets(SHELL_PACKAGE_NAME, userId).getList()) {
                        pw.print(bucketInfo.mPackageName);
                        pw.print(": ");
                        pw.println(bucketInfo.mStandbyBucket);
                    }
                }
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    int runGetInactive(PrintWriter pw) throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                boolean isIdle = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats")).isAppInactive(getNextArgRequired(), userId);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Idle=");
                stringBuilder.append(isIdle);
                pw.println(stringBuilder.toString());
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Unknown option: ");
                stringBuilder2.append(opt);
                errPrintWriter.println(stringBuilder2.toString());
                return -1;
            }
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    int runSendTrimMemory(PrintWriter pw) throws RemoteException {
        int userId = -2;
        do {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                int i;
                nextOption = getNextArgRequired();
                String levelArg = getNextArgRequired();
                switch (levelArg.hashCode()) {
                    case -1943119297:
                        if (levelArg.equals("RUNNING_CRITICAL")) {
                            i = 5;
                            break;
                        }
                    case -847101650:
                        if (levelArg.equals("BACKGROUND")) {
                            i = 2;
                            break;
                        }
                    case -219160669:
                        if (levelArg.equals("RUNNING_MODERATE")) {
                            i = 1;
                            break;
                        }
                    case 163769603:
                        if (levelArg.equals("MODERATE")) {
                            i = 4;
                            break;
                        }
                    case 183181625:
                        if (levelArg.equals("COMPLETE")) {
                            i = 6;
                            break;
                        }
                    case 1072631956:
                        if (levelArg.equals("RUNNING_LOW")) {
                            i = 3;
                            break;
                        }
                    case 2130809258:
                        if (levelArg.equals("HIDDEN")) {
                            i = 0;
                            break;
                        }
                    default:
                        i = -1;
                        break;
                }
                switch (i) {
                    case 0:
                        i = 20;
                        break;
                    case 1:
                        i = 5;
                        break;
                    case 2:
                        i = 40;
                        break;
                    case 3:
                        i = 10;
                        break;
                    case 4:
                        i = 60;
                        break;
                    case 5:
                        i = 15;
                        break;
                    case 6:
                        i = 80;
                        break;
                    default:
                        try {
                            i = Integer.parseInt(levelArg);
                            break;
                        } catch (NumberFormatException e) {
                            PrintWriter errPrintWriter = getErrPrintWriter();
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: Unknown level option: ");
                            stringBuilder.append(levelArg);
                            errPrintWriter.println(stringBuilder.toString());
                            return -1;
                        }
                }
                if (this.mInterface.setProcessMemoryTrimLevel(nextOption, userId, i)) {
                    return 0;
                }
                getErrPrintWriter().println("Unknown error: failed to set trim level");
                return -1;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Unknown option: ");
                stringBuilder2.append(opt);
                errPrintWriter2.println(stringBuilder2.toString());
                return -1;
            }
        } while (userId != -1);
        getErrPrintWriter().println("Error: Can't use user 'all'");
        return -1;
    }

    int runDisplay(PrintWriter pw) throws RemoteException {
        String op = getNextArgRequired();
        int i = (op.hashCode() == 1625698700 && op.equals("move-stack")) ? 0 : -1;
        if (i == 0) {
            return runDisplayMoveStack(pw);
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error: unknown command '");
        stringBuilder.append(op);
        stringBuilder.append("'");
        errPrintWriter.println(stringBuilder.toString());
        return -1;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    int runStack(PrintWriter pw) throws RemoteException {
        int i;
        String op = getNextArgRequired();
        switch (op.hashCode()) {
            case -1551426916:
                if (op.equals("resize-animated")) {
                    i = 3;
                    break;
                }
            case -1152140410:
                if (op.equals("resize-docked-stack")) {
                    i = 4;
                    break;
                }
            case -1082080869:
                if (op.equals("move-top-activity-to-pinned-stack")) {
                    i = 8;
                    break;
                }
            case -934610812:
                if (op.equals("remove")) {
                    i = 9;
                    break;
                }
            case -934437708:
                if (op.equals("resize")) {
                    i = 2;
                    break;
                }
            case 3237038:
                if (op.equals("info")) {
                    i = 7;
                    break;
                }
            case 3322014:
                if (op.equals("list")) {
                    i = 6;
                    break;
                }
            case 35000878:
                if (op.equals("positiontask")) {
                    i = 5;
                    break;
                }
            case 109757538:
                if (op.equals("start")) {
                    i = 0;
                    break;
                }
            case 1022285313:
                if (op.equals("move-task")) {
                    i = 1;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return runStackStart(pw);
            case 1:
                return runStackMoveTask(pw);
            case 2:
                return runStackResize(pw);
            case 3:
                return runStackResizeAnimated(pw);
            case 4:
                return runStackResizeDocked(pw);
            case 5:
                return runStackPositionTask(pw);
            case 6:
                return runStackList(pw);
            case 7:
                return runStackInfo(pw);
            case 8:
                return runMoveTopActivityToPinnedStack(pw);
            case 9:
                return runStackRemove(pw);
            default:
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: unknown command '");
                stringBuilder.append(op);
                stringBuilder.append("'");
                errPrintWriter.println(stringBuilder.toString());
                return -1;
        }
    }

    private Rect getBounds() {
        String leftStr = getNextArgRequired();
        int left = Integer.parseInt(leftStr);
        String topStr = getNextArgRequired();
        int top = Integer.parseInt(topStr);
        String rightStr = getNextArgRequired();
        int right = Integer.parseInt(rightStr);
        String bottomStr = getNextArgRequired();
        int bottom = Integer.parseInt(bottomStr);
        PrintWriter errPrintWriter;
        StringBuilder stringBuilder;
        if (left < 0) {
            errPrintWriter = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: bad left arg: ");
            stringBuilder.append(leftStr);
            errPrintWriter.println(stringBuilder.toString());
            return null;
        } else if (top < 0) {
            errPrintWriter = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: bad top arg: ");
            stringBuilder.append(topStr);
            errPrintWriter.println(stringBuilder.toString());
            return null;
        } else if (right <= 0) {
            errPrintWriter = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: bad right arg: ");
            stringBuilder.append(rightStr);
            errPrintWriter.println(stringBuilder.toString());
            return null;
        } else if (bottom > 0) {
            return new Rect(left, top, right, bottom);
        } else {
            errPrintWriter = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: bad bottom arg: ");
            stringBuilder.append(bottomStr);
            errPrintWriter.println(stringBuilder.toString());
            return null;
        }
    }

    int runDisplayMoveStack(PrintWriter pw) throws RemoteException {
        this.mInterface.moveStackToDisplay(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runStackStart(PrintWriter pw) throws RemoteException {
        int displayId = Integer.parseInt(getNextArgRequired());
        try {
            Intent intent = makeIntent(-2);
            int stackId = this.mInterface.createStackOnDisplay(displayId);
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runStackMoveTask(PrintWriter pw) throws RemoteException {
        boolean toTop;
        int taskId = Integer.parseInt(getNextArgRequired());
        int stackId = Integer.parseInt(getNextArgRequired());
        String toTopStr = getNextArgRequired();
        if ("true".equals(toTopStr)) {
            toTop = true;
        } else if ("false".equals(toTopStr)) {
            toTop = false;
        } else {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: bad toTop arg: ");
            stringBuilder.append(toTopStr);
            errPrintWriter.println(stringBuilder.toString());
            return -1;
        }
        this.mInterface.moveTaskToStack(taskId, stackId, toTop);
        return 0;
    }

    int runStackResize(PrintWriter pw) throws RemoteException {
        int stackId = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds != null) {
            return resizeStack(stackId, bounds, 0);
        }
        getErrPrintWriter().println("Error: invalid input bounds");
        return -1;
    }

    int runStackResizeAnimated(PrintWriter pw) throws RemoteException {
        Rect bounds;
        int stackId = Integer.parseInt(getNextArgRequired());
        if ("null".equals(peekNextArg())) {
            bounds = null;
        } else {
            bounds = getBounds();
            if (bounds == null) {
                getErrPrintWriter().println("Error: invalid input bounds");
                return -1;
            }
        }
        return resizeStackUnchecked(stackId, bounds, 0, true);
    }

    int resizeStackUnchecked(int stackId, Rect bounds, int delayMs, boolean animate) throws RemoteException {
        try {
            this.mInterface.resizeStack(stackId, bounds, false, false, animate, -1);
            Thread.sleep((long) delayMs);
        } catch (InterruptedException e) {
        }
        return 0;
    }

    int runStackResizeDocked(PrintWriter pw) throws RemoteException {
        Rect bounds = getBounds();
        Rect taskBounds = getBounds();
        if (bounds == null || taskBounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        this.mInterface.resizeDockedStack(bounds, taskBounds, null, null, null);
        return 0;
    }

    int resizeStack(int stackId, Rect bounds, int delayMs) throws RemoteException {
        if (bounds != null) {
            return resizeStackUnchecked(stackId, bounds, delayMs, false);
        }
        getErrPrintWriter().println("Error: invalid input bounds");
        return -1;
    }

    int runStackPositionTask(PrintWriter pw) throws RemoteException {
        this.mInterface.positionTaskInStack(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runStackList(PrintWriter pw) throws RemoteException {
        for (StackInfo info : this.mInterface.getAllStackInfos()) {
            pw.println(info);
        }
        return 0;
    }

    int runStackInfo(PrintWriter pw) throws RemoteException {
        pw.println(this.mInterface.getStackInfo(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired())));
        return 0;
    }

    int runStackRemove(PrintWriter pw) throws RemoteException {
        this.mInterface.removeStack(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runMoveTopActivityToPinnedStack(PrintWriter pw) throws RemoteException {
        int stackId = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        } else if (this.mInterface.moveTopActivityToPinnedStack(stackId, bounds)) {
            return 0;
        } else {
            getErrPrintWriter().println("Didn't move top activity to pinned stack.");
            return -1;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x005d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setBoundsSide(Rect bounds, String side, int value) {
        Object obj;
        int hashCode = side.hashCode();
        if (hashCode == 98) {
            if (side.equals("b")) {
                obj = 3;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 108) {
            if (side.equals("l")) {
                obj = null;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 114) {
            if (side.equals("r")) {
                obj = 1;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW && side.equals("t")) {
            obj = 2;
            switch (obj) {
                case null:
                    bounds.left = value;
                    return;
                case 1:
                    bounds.right = value;
                    return;
                case 2:
                    bounds.top = value;
                    return;
                case 3:
                    bounds.bottom = value;
                    return;
                default:
                    PrintWriter errPrintWriter = getErrPrintWriter();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown set side: ");
                    stringBuilder.append(side);
                    errPrintWriter.println(stringBuilder.toString());
                    return;
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                break;
        }
    }

    int runTask(PrintWriter pw) throws RemoteException {
        String op = getNextArgRequired();
        if (op.equals("lock")) {
            return runTaskLock(pw);
        }
        if (op.equals("resizeable")) {
            return runTaskResizeable(pw);
        }
        if (op.equals("resize")) {
            return runTaskResize(pw);
        }
        if (op.equals("focus")) {
            return runTaskFocus(pw);
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error: unknown command '");
        stringBuilder.append(op);
        stringBuilder.append("'");
        errPrintWriter.println(stringBuilder.toString());
        return -1;
    }

    int runTaskLock(PrintWriter pw) throws RemoteException {
        String taskIdStr = getNextArgRequired();
        if (taskIdStr.equals("stop")) {
            this.mInterface.stopSystemLockTaskMode();
        } else {
            this.mInterface.startSystemLockTaskMode(Integer.parseInt(taskIdStr));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Activity manager is ");
        stringBuilder.append(this.mInterface.isInLockTaskMode() ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "not ");
        stringBuilder.append("in lockTaskMode");
        pw.println(stringBuilder.toString());
        return 0;
    }

    int runTaskResizeable(PrintWriter pw) throws RemoteException {
        this.mInterface.setTaskResizeable(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runTaskResize(PrintWriter pw) throws RemoteException {
        int taskId = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        taskResize(taskId, bounds, 0, false);
        return 0;
    }

    void taskResize(int taskId, Rect bounds, int delay_ms, boolean pretendUserResize) throws RemoteException {
        this.mInterface.resizeTask(taskId, bounds, pretendUserResize);
        try {
            Thread.sleep((long) delay_ms);
        } catch (InterruptedException e) {
        }
    }

    int moveTask(int taskId, Rect taskRect, Rect stackRect, int stepSize, int maxToTravel, boolean movingForward, boolean horizontal, int delay_ms) throws RemoteException {
        int maxMove;
        if (movingForward) {
            while (maxToTravel > 0 && ((horizontal && taskRect.right < stackRect.right) || (!horizontal && taskRect.bottom < stackRect.bottom))) {
                if (horizontal) {
                    maxMove = Math.min(stepSize, stackRect.right - taskRect.right);
                    maxToTravel -= maxMove;
                    taskRect.right += maxMove;
                    taskRect.left += maxMove;
                } else {
                    maxMove = Math.min(stepSize, stackRect.bottom - taskRect.bottom);
                    maxToTravel -= maxMove;
                    taskRect.top += maxMove;
                    taskRect.bottom += maxMove;
                }
                taskResize(taskId, taskRect, delay_ms, false);
            }
        } else {
            while (maxToTravel < 0 && ((horizontal && taskRect.left > stackRect.left) || (!horizontal && taskRect.top > stackRect.top))) {
                if (horizontal) {
                    maxMove = Math.min(stepSize, taskRect.left - stackRect.left);
                    maxToTravel -= maxMove;
                    taskRect.right -= maxMove;
                    taskRect.left -= maxMove;
                } else {
                    maxMove = Math.min(stepSize, taskRect.top - stackRect.top);
                    maxToTravel -= maxMove;
                    taskRect.top -= maxMove;
                    taskRect.bottom -= maxMove;
                }
                taskResize(taskId, taskRect, delay_ms, false);
            }
        }
        return maxToTravel;
    }

    int getStepSize(int current, int target, int inStepSize, boolean greaterThanTarget) {
        int stepSize = 0;
        if (greaterThanTarget && target < current) {
            current -= inStepSize;
            stepSize = inStepSize;
            if (target > current) {
                stepSize -= target - current;
            }
        }
        if (greaterThanTarget || target <= current) {
            return stepSize;
        }
        current += inStepSize;
        stepSize = inStepSize;
        if (target < current) {
            return stepSize + (current - target);
        }
        return stepSize;
    }

    int runTaskFocus(PrintWriter pw) throws RemoteException {
        int taskId = Integer.parseInt(getNextArgRequired());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting focus to task ");
        stringBuilder.append(taskId);
        pw.println(stringBuilder.toString());
        this.mInterface.setFocusedTask(taskId);
        return 0;
    }

    int runWrite(PrintWriter pw) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        this.mInternal.getRecentTasks().flush();
        pw.println("All tasks persisted.");
        return 0;
    }

    int runAttachAgent(PrintWriter pw) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "attach-agent");
        String process = getNextArgRequired();
        String agent = getNextArgRequired();
        String nextArg = getNextArg();
        String opt = nextArg;
        if (nextArg != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: Unknown option: ");
            stringBuilder.append(opt);
            pw.println(stringBuilder.toString());
            return -1;
        }
        this.mInternal.attachAgent(process, agent);
        return 0;
    }

    int runSupportsMultiwindow(PrintWriter pw) throws RemoteException {
        if (getResources(pw) == null) {
            return -1;
        }
        pw.println(ActivityManager.supportsMultiWindow(this.mInternal.mContext));
        return 0;
    }

    int runSupportsSplitScreenMultiwindow(PrintWriter pw) throws RemoteException {
        if (getResources(pw) == null) {
            return -1;
        }
        pw.println(ActivityManager.supportsSplitScreenMultiWindow(this.mInternal.mContext));
        return 0;
    }

    int runUpdateApplicationInfo(PrintWriter pw) throws RemoteException {
        int userid = UserHandle.parseUserArg(getNextArgRequired());
        ArrayList<String> packages = new ArrayList();
        packages.add(getNextArgRequired());
        while (true) {
            String nextArg = getNextArg();
            String packageName = nextArg;
            if (nextArg != null) {
                packages.add(packageName);
            } else {
                this.mInternal.scheduleApplicationInfoChanged(packages, userid);
                pw.println("Packages updated with most recent ApplicationInfos.");
                return 0;
            }
        }
    }

    int runNoHomeScreen(PrintWriter pw) throws RemoteException {
        Resources res = getResources(pw);
        if (res == null) {
            return -1;
        }
        pw.println(res.getBoolean(17956996));
        return 0;
    }

    int runWaitForBroadcastIdle(PrintWriter pw) throws RemoteException {
        this.mInternal.waitForBroadcastIdle(pw);
        return 0;
    }

    private Resources getResources(PrintWriter pw) throws RemoteException {
        Configuration config = this.mInterface.getConfiguration();
        if (config == null) {
            pw.println("Error: Activity manager has no configuration");
            return null;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        return new Resources(AssetManager.getSystem(), metrics, config);
    }

    public void onHelp() {
        dumpHelp(getOutPrintWriter(), this.mDumping);
    }

    static void dumpHelp(PrintWriter pw, boolean dumping) {
        if (dumping) {
            pw.println("Activity manager dump options:");
            pw.println("  [-a] [-c] [-p PACKAGE] [-h] [WHAT] ...");
            pw.println("  WHAT may be one of:");
            pw.println("    a[ctivities]: activity stack state");
            pw.println("    r[recents]: recent activities state");
            pw.println("    b[roadcasts] [PACKAGE_NAME] [history [-s]]: broadcast state");
            pw.println("    broadcast-stats [PACKAGE_NAME]: aggregated broadcast statistics");
            pw.println("    i[ntents] [PACKAGE_NAME]: pending intent state");
            pw.println("    p[rocesses] [PACKAGE_NAME]: process state");
            pw.println("    o[om]: out of memory management");
            pw.println("    perm[issions]: URI permission grant state");
            pw.println("    prov[iders] [COMP_SPEC ...]: content provider state");
            pw.println("    provider [COMP_SPEC]: provider client-side state");
            pw.println("    s[ervices] [COMP_SPEC ...]: service state");
            pw.println("    as[sociations]: tracked app associations");
            pw.println("    settings: currently applied config settings");
            pw.println("    service [COMP_SPEC]: service client-side state");
            pw.println("    package [PACKAGE_NAME]: all state related to given package");
            pw.println("    all: dump all activities");
            pw.println("    top: dump the top activity");
            pw.println("  WHAT may also be a COMP_SPEC to dump activities.");
            pw.println("  COMP_SPEC may be a component name (com.foo/.myApp),");
            pw.println("    a partial substring in a component name, a");
            pw.println("    hex object identifier.");
            pw.println("  -a: include all available server state.");
            pw.println("  -c: include client state.");
            pw.println("  -p: limit output to given package.");
            pw.println("  --checkin: output checkin format, resetting data.");
            pw.println("  --C: output checkin format, not resetting data.");
            pw.println("  --proto: output dump in protocol buffer format.");
            return;
        }
        pw.println("Activity manager (activity) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  start-activity [-D] [-N] [-W] [-P <FILE>] [--start-profiler <FILE>]");
        pw.println("          [--sampling INTERVAL] [--streaming] [-R COUNT] [-S]");
        pw.println("          [--track-allocation] [--user <USER_ID> | current] <INTENT>");
        pw.println("      Start an Activity.  Options are:");
        pw.println("      -D: enable debugging");
        pw.println("      -N: enable native debugging");
        pw.println("      -W: wait for launch to complete");
        pw.println("      --start-profiler <FILE>: start profiler and send results to <FILE>");
        pw.println("      --sampling INTERVAL: use sample profiling with INTERVAL microseconds");
        pw.println("          between samples (use with --start-profiler)");
        pw.println("      --streaming: stream the profiling output to the specified file");
        pw.println("          (use with --start-profiler)");
        pw.println("      -P <FILE>: like above, but profiling stops when app goes idle");
        pw.println("      --attach-agent <agent>: attach the given agent before binding");
        pw.println("      --attach-agent-bind <agent>: attach the given agent during binding");
        pw.println("      -R: repeat the activity launch <COUNT> times.  Prior to each repeat,");
        pw.println("          the top activity will be finished.");
        pw.println("      -S: force stop the target app before starting the activity");
        pw.println("      --track-allocation: enable tracking of object allocations");
        pw.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        pw.println("          specified then run as the current user.");
        pw.println("      --windowingMode <WINDOWING_MODE>: The windowing mode to launch the activity into.");
        pw.println("      --activityType <ACTIVITY_TYPE>: The activity type to launch the activity as.");
        pw.println("  start-service [--user <USER_ID> | current] <INTENT>");
        pw.println("      Start a Service.  Options are:");
        pw.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        pw.println("          specified then run as the current user.");
        pw.println("  start-foreground-service [--user <USER_ID> | current] <INTENT>");
        pw.println("      Start a foreground Service.  Options are:");
        pw.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        pw.println("          specified then run as the current user.");
        pw.println("  stop-service [--user <USER_ID> | current] <INTENT>");
        pw.println("      Stop a Service.  Options are:");
        pw.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        pw.println("          specified then run as the current user.");
        pw.println("  broadcast [--user <USER_ID> | all | current] <INTENT>");
        pw.println("      Send a broadcast Intent.  Options are:");
        pw.println("      --user <USER_ID> | all | current: Specify which user to send to; if not");
        pw.println("          specified then send to all users.");
        pw.println("      --receiver-permission <PERMISSION>: Require receiver to hold permission.");
        pw.println("  instrument [-r] [-e <NAME> <VALUE>] [-p <FILE>] [-w]");
        pw.println("          [--user <USER_ID> | current] [--no-hidden-api-checks]");
        pw.println("          [--no-window-animation] [--abi <ABI>] <COMPONENT>");
        pw.println("      Start an Instrumentation.  Typically this target <COMPONENT> is in the");
        pw.println("      form <TEST_PACKAGE>/<RUNNER_CLASS> or only <TEST_PACKAGE> if there");
        pw.println("      is only one instrumentation.  Options are:");
        pw.println("      -r: print raw results (otherwise decode REPORT_KEY_STREAMRESULT).  Use with");
        pw.println("          [-e perf true] to generate raw output for performance measurements.");
        pw.println("      -e <NAME> <VALUE>: set argument <NAME> to <VALUE>.  For test runners a");
        pw.println("          common form is [-e <testrunner_flag> <value>[,<value>...]].");
        pw.println("      -p <FILE>: write profiling data to <FILE>");
        pw.println("      -m: Write output as protobuf to stdout (machine readable)");
        pw.println("      -f <Optional PATH/TO/FILE>: Write output as protobuf to a file (machine");
        pw.println("          readable). If path is not specified, default directory and file name will");
        pw.println("          be used: /sdcard/instrument-logs/log-yyyyMMdd-hhmmss-SSS.instrumentation_data_proto");
        pw.println("      -w: wait for instrumentation to finish before returning.  Required for");
        pw.println("          test runners.");
        pw.println("      --user <USER_ID> | current: Specify user instrumentation runs in;");
        pw.println("          current user if not specified.");
        pw.println("      --no-hidden-api-checks: disable restrictions on use of hidden API.");
        pw.println("      --no-window-animation: turn off window animations while running.");
        pw.println("      --abi <ABI>: Launch the instrumented process with the selected ABI.");
        pw.println("          This assumes that the process supports the selected ABI.");
        pw.println("  trace-ipc [start|stop] [--dump-file <FILE>]");
        pw.println("      Trace IPC transactions.");
        pw.println("      start: start tracing IPC transactions.");
        pw.println("      stop: stop tracing IPC transactions and dump the results to file.");
        pw.println("      --dump-file <FILE>: Specify the file the trace should be dumped to.");
        pw.println("  profile [start|stop] [--user <USER_ID> current] [--sampling INTERVAL]");
        pw.println("          [--streaming] <PROCESS> <FILE>");
        pw.println("      Start and stop profiler on a process.  The given <PROCESS> argument");
        pw.println("        may be either a process name or pid.  Options are:");
        pw.println("      --user <USER_ID> | current: When supplying a process name,");
        pw.println("          specify user of process to profile; uses current user if not specified.");
        pw.println("      --sampling INTERVAL: use sample profiling with INTERVAL microseconds");
        pw.println("          between samples");
        pw.println("      --streaming: stream the profiling output to the specified file");
        pw.println("  dumpheap [--user <USER_ID> current] [-n] [-g] <PROCESS> <FILE>");
        pw.println("      Dump the heap of a process.  The given <PROCESS> argument may");
        pw.println("        be either a process name or pid.  Options are:");
        pw.println("      -n: dump native heap instead of managed heap");
        pw.println("      -g: force GC before dumping the heap");
        pw.println("      --user <USER_ID> | current: When supplying a process name,");
        pw.println("          specify user of process to dump; uses current user if not specified.");
        pw.println("  set-debug-app [-w] [--persistent] <PACKAGE>");
        pw.println("      Set application <PACKAGE> to debug.  Options are:");
        pw.println("      -w: wait for debugger when application starts");
        pw.println("      --persistent: retain this value");
        pw.println("  clear-debug-app");
        pw.println("      Clear the previously set-debug-app.");
        pw.println("  set-watch-heap <PROCESS> <MEM-LIMIT>");
        pw.println("      Start monitoring pss size of <PROCESS>, if it is at or");
        pw.println("      above <HEAP-LIMIT> then a heap dump is collected for the user to report.");
        pw.println("  clear-watch-heap");
        pw.println("      Clear the previously set-watch-heap.");
        pw.println("  bug-report [--progress | --telephony]");
        pw.println("      Request bug report generation; will launch a notification");
        pw.println("        when done to select where it should be delivered. Options are:");
        pw.println("     --progress: will launch a notification right away to show its progress.");
        pw.println("     --telephony: will dump only telephony sections.");
        pw.println("  force-stop [--user <USER_ID> | all | current] <PACKAGE>");
        pw.println("      Completely stop the given application package.");
        pw.println("  crash [--user <USER_ID>] <PACKAGE|PID>");
        pw.println("      Induce a VM crash in the specified package or process");
        pw.println("  kill [--user <USER_ID> | all | current] <PACKAGE>");
        pw.println("      Kill all background processes associated with the given application.");
        pw.println("  kill-all");
        pw.println("      Kill all processes that are safe to kill (cached, etc).");
        pw.println("  make-uid-idle [--user <USER_ID> | all | current] <PACKAGE>");
        pw.println("      If the given application's uid is in the background and waiting to");
        pw.println("      become idle (not allowing background services), do that now.");
        pw.println("  monitor [--gdb <port>]");
        pw.println("      Start monitoring for crashes or ANRs.");
        pw.println("      --gdb: start gdbserv on the given port at crash/ANR");
        pw.println("  watch-uids [--oom <uid>]");
        pw.println("      Start watching for and reporting uid state changes.");
        pw.println("      --oom: specify a uid for which to report detailed change messages.");
        pw.println("  hang [--allow-restart]");
        pw.println("      Hang the system.");
        pw.println("      --allow-restart: allow watchdog to perform normal system restart");
        pw.println("  restart");
        pw.println("      Restart the user-space system.");
        pw.println("  idle-maintenance");
        pw.println("      Perform idle maintenance now.");
        pw.println("  screen-compat [on|off] <PACKAGE>");
        pw.println("      Control screen compatibility mode of <PACKAGE>.");
        pw.println("  package-importance <PACKAGE>");
        pw.println("      Print current importance of <PACKAGE>.");
        pw.println("  to-uri [INTENT]");
        pw.println("      Print the given Intent specification as a URI.");
        pw.println("  to-intent-uri [INTENT]");
        pw.println("      Print the given Intent specification as an intent: URI.");
        pw.println("  to-app-uri [INTENT]");
        pw.println("      Print the given Intent specification as an android-app: URI.");
        pw.println("  switch-user <USER_ID>");
        pw.println("      Switch to put USER_ID in the foreground, starting");
        pw.println("      execution of that user if it is currently stopped.");
        pw.println("  get-current-user");
        pw.println("      Returns id of the current foreground user.");
        pw.println("  start-user <USER_ID>");
        pw.println("      Start USER_ID in background if it is currently stopped;");
        pw.println("      use switch-user if you want to start the user in foreground");
        pw.println("  unlock-user <USER_ID> [TOKEN_HEX]");
        pw.println("      Attempt to unlock the given user using the given authorization token.");
        pw.println("  stop-user [-w] [-f] <USER_ID>");
        pw.println("      Stop execution of USER_ID, not allowing it to run any");
        pw.println("      code until a later explicit start or switch to it.");
        pw.println("      -w: wait for stop-user to complete.");
        pw.println("      -f: force stop even if there are related users that cannot be stopped.");
        pw.println("  is-user-stopped <USER_ID>");
        pw.println("      Returns whether <USER_ID> has been stopped or not.");
        pw.println("  get-started-user-state <USER_ID>");
        pw.println("      Gets the current state of the given started user.");
        pw.println("  track-associations");
        pw.println("      Enable association tracking.");
        pw.println("  untrack-associations");
        pw.println("      Disable and clear association tracking.");
        pw.println("  get-uid-state <UID>");
        pw.println("      Gets the process state of an app given its <UID>.");
        pw.println("  attach-agent <PROCESS> <FILE>");
        pw.println("    Attach an agent to the specified <PROCESS>, which may be either a process name or a PID.");
        pw.println("  get-config [--days N] [--device] [--proto]");
        pw.println("      Retrieve the configuration and any recent configurations of the device.");
        pw.println("      --days: also return last N days of configurations that have been seen.");
        pw.println("      --device: also output global device configuration info.");
        pw.println("      --proto: return result as a proto; does not include --days info.");
        pw.println("  supports-multiwindow");
        pw.println("      Returns true if the device supports multiwindow.");
        pw.println("  supports-split-screen-multi-window");
        pw.println("      Returns true if the device supports split screen multiwindow.");
        pw.println("  suppress-resize-config-changes <true|false>");
        pw.println("      Suppresses configuration changes due to user resizing an activity/task.");
        pw.println("  set-inactive [--user <USER_ID>] <PACKAGE> true|false");
        pw.println("      Sets the inactive state of an app.");
        pw.println("  get-inactive [--user <USER_ID>] <PACKAGE>");
        pw.println("      Returns the inactive state of an app.");
        pw.println("  set-standby-bucket [--user <USER_ID>] <PACKAGE> active|working_set|frequent|rare");
        pw.println("      Puts an app in the standby bucket.");
        pw.println("  get-standby-bucket [--user <USER_ID>] <PACKAGE>");
        pw.println("      Returns the standby bucket of an app.");
        pw.println("  send-trim-memory [--user <USER_ID>] <PROCESS>");
        pw.println("          [HIDDEN|RUNNING_MODERATE|BACKGROUND|RUNNING_LOW|MODERATE|RUNNING_CRITICAL|COMPLETE]");
        pw.println("      Send a memory trim event to a <PROCESS>.  May also supply a raw trim int level.");
        pw.println("  display [COMMAND] [...]: sub-commands for operating on displays.");
        pw.println("       move-stack <STACK_ID> <DISPLAY_ID>");
        pw.println("           Move <STACK_ID> from its current display to <DISPLAY_ID>.");
        pw.println("  stack [COMMAND] [...]: sub-commands for operating on activity stacks.");
        pw.println("       start <DISPLAY_ID> <INTENT>");
        pw.println("           Start a new activity on <DISPLAY_ID> using <INTENT>");
        pw.println("       move-task <TASK_ID> <STACK_ID> [true|false]");
        pw.println("           Move <TASK_ID> from its current stack to the top (true) or");
        pw.println("           bottom (false) of <STACK_ID>.");
        pw.println("       resize <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("           Change <STACK_ID> size and position to <LEFT,TOP,RIGHT,BOTTOM>.");
        pw.println("       resize-animated <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("           Same as resize, but allow animation.");
        pw.println("       resize-docked-stack <LEFT,TOP,RIGHT,BOTTOM> [<TASK_LEFT,TASK_TOP,TASK_RIGHT,TASK_BOTTOM>]");
        pw.println("           Change docked stack to <LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("           and supplying temporary different task bounds indicated by");
        pw.println("           <TASK_LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("       move-top-activity-to-pinned-stack: <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("           Moves the top activity from");
        pw.println("           <STACK_ID> to the pinned stack using <LEFT,TOP,RIGHT,BOTTOM> for the");
        pw.println("           bounds of the pinned stack.");
        pw.println("       positiontask <TASK_ID> <STACK_ID> <POSITION>");
        pw.println("           Place <TASK_ID> in <STACK_ID> at <POSITION>");
        pw.println("       list");
        pw.println("           List all of the activity stacks and their sizes.");
        pw.println("       info <WINDOWING_MODE> <ACTIVITY_TYPE>");
        pw.println("           Display the information about activity stack in <WINDOWING_MODE> and <ACTIVITY_TYPE>.");
        pw.println("       remove <STACK_ID>");
        pw.println("           Remove stack <STACK_ID>.");
        pw.println("  task [COMMAND] [...]: sub-commands for operating on activity tasks.");
        pw.println("       lock <TASK_ID>");
        pw.println("           Bring <TASK_ID> to the front and don't allow other tasks to run.");
        pw.println("       lock stop");
        pw.println("           End the current task lock.");
        pw.println("       resizeable <TASK_ID> [0|1|2|3]");
        pw.println("           Change resizeable mode of <TASK_ID> to one of the following:");
        pw.println("           0: unresizeable");
        pw.println("           1: crop_windows");
        pw.println("           2: resizeable");
        pw.println("           3: resizeable_and_pipable");
        pw.println("       resize <TASK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        pw.println("           Makes sure <TASK_ID> is in a stack with the specified bounds.");
        pw.println("           Forces the task to be resizeable and creates a stack if no existing stack");
        pw.println("           has the specified bounds.");
        pw.println("  update-appinfo <USER_ID> <PACKAGE_NAME> [<PACKAGE_NAME>...]");
        pw.println("      Update the ApplicationInfo objects of the listed packages for <USER_ID>");
        pw.println("      without restarting any processes.");
        pw.println("  write");
        pw.println("      Write all pending state to storage.");
        pw.println();
        Intent.printIntentArgsHelp(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
