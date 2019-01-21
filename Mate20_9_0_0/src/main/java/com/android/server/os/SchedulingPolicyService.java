package com.android.server.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.ISchedulingPolicyService.Stub;
import android.os.Process;
import android.util.Log;
import com.android.server.SystemServerInitThreadPool;

public class SchedulingPolicyService extends Stub {
    private static final String[] MEDIA_PROCESS_NAMES = new String[]{"media.codec"};
    private static final int PRIORITY_MAX = 3;
    private static final int PRIORITY_MIN = 1;
    private static final String TAG = "SchedulingPolicyService";
    private int mBoostedPid = -1;
    private IBinder mClient;
    private final DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            SchedulingPolicyService.this.requestCpusetBoost(false, null);
        }
    };

    public SchedulingPolicyService() {
        SystemServerInitThreadPool.get().submit(new -$$Lambda$SchedulingPolicyService$ao2OiSvvlyzmJ0li0c0nhHy-IDk(this), "SchedulingPolicyService.<init>");
    }

    public static /* synthetic */ void lambda$new$0(SchedulingPolicyService schedulingPolicyService) {
        synchronized (schedulingPolicyService.mDeathRecipient) {
            if (schedulingPolicyService.mBoostedPid == -1) {
                int[] nativePids = Process.getPidsForCommands(MEDIA_PROCESS_NAMES);
                if (nativePids != null && nativePids.length == 1) {
                    schedulingPolicyService.mBoostedPid = nativePids[0];
                    schedulingPolicyService.disableCpusetBoost(nativePids[0]);
                }
            }
        }
    }

    public int requestPriority(int pid, int tid, int prio, boolean isForApp) {
        String str;
        StringBuilder stringBuilder;
        if (!isPermitted() || prio < 1 || prio > 3 || Process.getThreadGroupLeader(tid) != pid) {
            return -1;
        }
        if (Binder.getCallingUid() != 1002) {
            try {
                Process.setThreadGroup(tid, !isForApp ? 4 : 6);
            } catch (RuntimeException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed setThreadGroup: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return -1;
            }
        }
        try {
            Process.setThreadScheduler(tid, 1073741825, prio);
            return 0;
        } catch (RuntimeException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed setThreadScheduler: ");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int requestCpusetBoost(boolean enable, IBinder client) {
        if (Binder.getCallingPid() != Process.myPid() && Binder.getCallingUid() != 1013) {
            return -1;
        }
        int[] nativePids = Process.getPidsForCommands(MEDIA_PROCESS_NAMES);
        if (nativePids == null || nativePids.length != 1) {
            Log.e(TAG, "requestCpusetBoost: can't find media.codec process");
            return -1;
        }
        synchronized (this.mDeathRecipient) {
            int enableCpusetBoost;
            if (enable) {
                try {
                    enableCpusetBoost = enableCpusetBoost(nativePids[0], client);
                    return enableCpusetBoost;
                } catch (Throwable th) {
                }
            } else {
                enableCpusetBoost = disableCpusetBoost(nativePids[0]);
                return enableCpusetBoost;
            }
        }
    }

    private int enableCpusetBoost(int pid, IBinder client) {
        if (this.mBoostedPid == pid) {
            return 0;
        }
        this.mBoostedPid = -1;
        if (this.mClient != null) {
            try {
                this.mClient.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e) {
            } catch (Throwable th) {
                this.mClient = null;
            }
            this.mClient = null;
        }
        try {
            client.linkToDeath(this.mDeathRecipient, 0);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Moving ");
            stringBuilder.append(pid);
            stringBuilder.append(" to group ");
            stringBuilder.append(5);
            Log.i(str, stringBuilder.toString());
            Process.setProcessGroup(pid, 5);
            this.mBoostedPid = pid;
            this.mClient = client;
            return 0;
        } catch (Exception e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed enableCpusetBoost: ");
            stringBuilder2.append(e2);
            Log.e(str2, stringBuilder2.toString());
            try {
                client.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e3) {
            }
            return -1;
        }
    }

    private int disableCpusetBoost(int pid) {
        int boostedPid = this.mBoostedPid;
        this.mBoostedPid = -1;
        if (this.mClient != null) {
            try {
                this.mClient.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e) {
            } catch (Throwable th) {
                this.mClient = null;
            }
            this.mClient = null;
        }
        if (boostedPid == pid) {
            String str;
            StringBuilder stringBuilder;
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving ");
                stringBuilder.append(pid);
                stringBuilder.append(" back to group default");
                Log.i(str, stringBuilder.toString());
                Process.setProcessGroup(pid, -1);
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't move pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" back to group default");
                Log.w(str, stringBuilder.toString());
            }
        }
        return 0;
    }

    private boolean isPermitted() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return true;
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1002 || callingUid == 1041 || callingUid == 1047) {
            return true;
        }
        return false;
    }
}
