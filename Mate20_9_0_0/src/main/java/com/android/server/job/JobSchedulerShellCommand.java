package com.android.server.job;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;

public final class JobSchedulerShellCommand extends ShellCommand {
    public static final int CMD_ERR_CONSTRAINTS = -1002;
    public static final int CMD_ERR_NO_JOB = -1001;
    public static final int CMD_ERR_NO_PACKAGE = -1000;
    JobSchedulerService mInternal;
    IPackageManager mPM = AppGlobals.getPackageManager();

    JobSchedulerShellCommand(JobSchedulerService service) {
        this.mInternal = service;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        String str;
        int i;
        PrintWriter pw = getOutPrintWriter();
        if (cmd != null) {
            str = cmd;
        } else {
            try {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: ");
                stringBuilder.append(e);
                pw.println(stringBuilder.toString());
                return -1;
            }
        }
        switch (str.hashCode()) {
            case -1894245460:
                if (str.equals("trigger-dock-state")) {
                    i = 11;
                    break;
                }
            case -1845752298:
                if (str.equals("get-storage-seq")) {
                    i = 7;
                    break;
                }
            case -1687551032:
                if (str.equals("get-battery-charging")) {
                    i = 5;
                    break;
                }
            case -1367724422:
                if (str.equals("cancel")) {
                    i = 2;
                    break;
                }
            case -1313911455:
                if (str.equals("timeout")) {
                    i = 1;
                    break;
                }
            case 113291:
                if (str.equals("run")) {
                    i = 0;
                    break;
                }
            case 55361425:
                if (str.equals("get-storage-not-low")) {
                    i = 8;
                    break;
                }
            case 200896764:
                if (str.equals("heartbeat")) {
                    i = 10;
                    break;
                }
            case 703160488:
                if (str.equals("get-battery-seq")) {
                    i = 4;
                    break;
                }
            case 1749711139:
                if (str.equals("get-battery-not-low")) {
                    i = 6;
                    break;
                }
            case 1791471818:
                if (str.equals("get-job-state")) {
                    i = 9;
                    break;
                }
            case 1854493850:
                if (str.equals("monitor-battery")) {
                    i = 3;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return runJob(pw);
            case 1:
                return timeout(pw);
            case 2:
                return cancelJob(pw);
            case 3:
                return monitorBattery(pw);
            case 4:
                return getBatterySeq(pw);
            case 5:
                return getBatteryCharging(pw);
            case 6:
                return getBatteryNotLow(pw);
            case 7:
                return getStorageSeq(pw);
            case 8:
                return getStorageNotLow(pw);
            case 9:
                return getJobState(pw);
            case 10:
                return doHeartbeat(pw);
            case 11:
                return triggerDockState(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private void checkPermission(String operation) throws Exception {
        int uid = Binder.getCallingUid();
        if (uid != 0 && this.mPM.checkUidPermission("android.permission.CHANGE_APP_IDLE_STATE", uid) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Uid ");
            stringBuilder.append(uid);
            stringBuilder.append(" not permitted to ");
            stringBuilder.append(operation);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private boolean printError(int errCode, String pkgName, int userId, int jobId) {
        PrintWriter pw;
        switch (errCode) {
            case CMD_ERR_CONSTRAINTS /*-1002*/:
                pw = getErrPrintWriter();
                pw.print("Job ");
                pw.print(jobId);
                pw.print(" in package ");
                pw.print(pkgName);
                pw.print(" / user ");
                pw.print(userId);
                pw.println(" has functional constraints but --force not specified");
                return true;
            case CMD_ERR_NO_JOB /*-1001*/:
                pw = getErrPrintWriter();
                pw.print("Could not find job ");
                pw.print(jobId);
                pw.print(" in package ");
                pw.print(pkgName);
                pw.print(" / user ");
                pw.println(userId);
                return true;
            case CMD_ERR_NO_PACKAGE /*-1000*/:
                pw = getErrPrintWriter();
                pw.print("Package not found: ");
                pw.print(pkgName);
                pw.print(" / user ");
                pw.println(userId);
                return true;
            default:
                return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x0054 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0077  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0054 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0077  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0054 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0077  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0054 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0077  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runJob(PrintWriter pw) throws Exception {
        checkPermission("force scheduled jobs");
        boolean force = false;
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int hashCode;
            if (nextOption != null) {
                hashCode = opt.hashCode();
                if (hashCode == -1626076853) {
                    if (opt.equals("--force")) {
                        hashCode = 1;
                        switch (hashCode) {
                            case 0:
                            case 1:
                                break;
                            case 2:
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1497) {
                    if (opt.equals("-f")) {
                        hashCode = 0;
                        switch (hashCode) {
                            case 0:
                            case 1:
                                break;
                            case 2:
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1512) {
                    if (opt.equals("-u")) {
                        hashCode = 2;
                        switch (hashCode) {
                            case 0:
                            case 1:
                                break;
                            case 2:
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1333469547 && opt.equals("--user")) {
                    hashCode = 3;
                    switch (hashCode) {
                        case 0:
                        case 1:
                            force = true;
                            break;
                        case 2:
                        case 3:
                            userId = Integer.parseInt(getNextArgRequired());
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: unknown option '");
                            stringBuilder.append(opt);
                            stringBuilder.append("'");
                            pw.println(stringBuilder.toString());
                            return -1;
                    }
                }
                hashCode = -1;
                switch (hashCode) {
                    case 0:
                    case 1:
                        break;
                    case 2:
                    case 3:
                        break;
                    default:
                        break;
                }
            }
            String pkgName = getNextArgRequired();
            hashCode = Integer.parseInt(getNextArgRequired());
            long ident = Binder.clearCallingIdentity();
            try {
                int ret = this.mInternal.executeRunCommand(pkgName, userId, hashCode, force);
                if (printError(ret, pkgName, userId, hashCode)) {
                    return ret;
                }
                pw.print("Running job");
                if (force) {
                    pw.print(" [FORCED]");
                }
                pw.println();
                Binder.restoreCallingIdentity(ident);
                return ret;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x0037 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0053 A:{LOOP_END, LOOP:0: B:1:0x0009->B:16:0x0053} */
    /* JADX WARNING: Missing block: B:11:0x0030, code:
            if (r4.equals("-u") != false) goto L_0x0034;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int timeout(PrintWriter pw) throws Exception {
        Throwable th;
        checkPermission("force timeout jobs");
        int i = -1;
        int userId = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            boolean z = false;
            if (nextOption != null) {
                int i2;
                int hashCode = opt.hashCode();
                if (hashCode != 1512) {
                    if (hashCode == 1333469547 && opt.equals("--user")) {
                        i2 = 1;
                        switch (i2) {
                            case 0:
                            case 1:
                                userId = UserHandle.parseUserArg(getNextArgRequired());
                            default:
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Error: unknown option '");
                                stringBuilder.append(opt);
                                stringBuilder.append("'");
                                pw.println(stringBuilder.toString());
                                return -1;
                        }
                    }
                }
                i2 = -1;
                switch (i2) {
                    case 0:
                    case 1:
                        break;
                    default:
                        break;
                }
            }
            PrintWriter printWriter = pw;
            if (userId == -2) {
                userId = ActivityManager.getCurrentUser();
            }
            nextOption = getNextArg();
            String jobIdStr = getNextArg();
            if (jobIdStr != null) {
                i = Integer.parseInt(jobIdStr);
            }
            int jobId = i;
            long ident = Binder.clearCallingIdentity();
            long ident2;
            try {
                JobSchedulerService jobSchedulerService = this.mInternal;
                if (jobIdStr != null) {
                    z = true;
                }
                String str = nextOption;
                ident2 = ident;
                try {
                    i = jobSchedulerService.executeTimeoutCommand(printWriter, str, userId, z, jobId);
                    Binder.restoreCallingIdentity(ident2);
                    return i;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident2);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                String str2 = nextOption;
                String str3 = opt;
                ident2 = ident;
                Binder.restoreCallingIdentity(ident2);
                throw th;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0035 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x004f A:{LOOP_END, LOOP:1: B:1:0x0007->B:16:0x004f} */
    /* JADX WARNING: Missing block: B:8:0x0024, code:
            if (r8.equals("--user") == false) goto L_0x0031;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int cancelJob(PrintWriter pw) throws Exception {
        checkPermission("cancel jobs");
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int i = 1;
            if (nextOption != null) {
                int hashCode = opt.hashCode();
                if (hashCode != 1512) {
                    if (hashCode == 1333469547) {
                    }
                } else if (opt.equals("-u")) {
                    i = 0;
                    switch (i) {
                        case 0:
                        case 1:
                            userId = UserHandle.parseUserArg(getNextArgRequired());
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: unknown option '");
                            stringBuilder.append(opt);
                            stringBuilder.append("'");
                            pw.println(stringBuilder.toString());
                            return -1;
                    }
                }
                i = -1;
                switch (i) {
                    case 0:
                    case 1:
                        break;
                    default:
                        break;
                }
            } else if (userId < 0) {
                pw.println("Error: must specify a concrete user ID");
                return -1;
            } else {
                String pkgName = getNextArg();
                String jobIdStr = getNextArg();
                int jobId = jobIdStr != null ? Integer.parseInt(jobIdStr) : -1;
                long ident = Binder.clearCallingIdentity();
                try {
                    int executeCancelCommand = this.mInternal.executeCancelCommand(pw, pkgName, userId, jobIdStr != null, jobId);
                    return executeCancelCommand;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    private int monitorBattery(PrintWriter pw) throws Exception {
        boolean enabled;
        checkPermission("change battery monitoring");
        String opt = getNextArgRequired();
        if ("on".equals(opt)) {
            enabled = true;
        } else if ("off".equals(opt)) {
            enabled = false;
        } else {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: unknown option ");
            stringBuilder.append(opt);
            errPrintWriter.println(stringBuilder.toString());
            return 1;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mInternal.setMonitorBattery(enabled);
            if (enabled) {
                pw.println("Battery monitoring enabled");
            } else {
                pw.println("Battery monitoring disabled");
            }
            Binder.restoreCallingIdentity(ident);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int getBatterySeq(PrintWriter pw) {
        pw.println(this.mInternal.getBatterySeq());
        return 0;
    }

    private int getBatteryCharging(PrintWriter pw) {
        pw.println(this.mInternal.getBatteryCharging());
        return 0;
    }

    private int getBatteryNotLow(PrintWriter pw) {
        pw.println(this.mInternal.getBatteryNotLow());
        return 0;
    }

    private int getStorageSeq(PrintWriter pw) {
        pw.println(this.mInternal.getStorageSeq());
        return 0;
    }

    private int getStorageNotLow(PrintWriter pw) {
        pw.println(this.mInternal.getStorageNotLow());
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0035 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004f A:{LOOP_END, LOOP:1: B:1:0x0007->B:17:0x004f} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0035 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004f A:{LOOP_END, LOOP:1: B:1:0x0007->B:17:0x004f} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getJobState(PrintWriter pw) throws Exception {
        checkPermission("force timeout jobs");
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                int hashCode = opt.hashCode();
                if (hashCode == 1512) {
                    if (opt.equals("-u")) {
                        hashCode = 0;
                        switch (hashCode) {
                            case 0:
                            case 1:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1333469547 && opt.equals("--user")) {
                    hashCode = 1;
                    switch (hashCode) {
                        case 0:
                        case 1:
                            userId = UserHandle.parseUserArg(getNextArgRequired());
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: unknown option '");
                            stringBuilder.append(opt);
                            stringBuilder.append("'");
                            pw.println(stringBuilder.toString());
                            return -1;
                    }
                }
                hashCode = -1;
                switch (hashCode) {
                    case 0:
                    case 1:
                        break;
                    default:
                        break;
                }
            }
            if (userId == -2) {
                userId = ActivityManager.getCurrentUser();
            }
            String pkgName = getNextArgRequired();
            int jobId = Integer.parseInt(getNextArgRequired());
            long ident = Binder.clearCallingIdentity();
            try {
                int ret = this.mInternal.getJobState(pw, pkgName, userId, jobId);
                printError(ret, pkgName, userId, jobId);
                return ret;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private int doHeartbeat(PrintWriter pw) throws Exception {
        checkPermission("manipulate scheduler heartbeat");
        String arg = getNextArg();
        int numBeats = arg != null ? Integer.parseInt(arg) : 0;
        long ident = Binder.clearCallingIdentity();
        try {
            int executeHeartbeatCommand = this.mInternal.executeHeartbeatCommand(pw, numBeats);
            return executeHeartbeatCommand;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int triggerDockState(PrintWriter pw) throws Exception {
        boolean idleState;
        checkPermission("trigger wireless charging dock state");
        String opt = getNextArgRequired();
        if ("idle".equals(opt)) {
            idleState = true;
        } else if ("active".equals(opt)) {
            idleState = false;
        } else {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: unknown option ");
            stringBuilder.append(opt);
            errPrintWriter.println(stringBuilder.toString());
            return 1;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mInternal.triggerDockState(idleState);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Job scheduler (jobscheduler) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  run [-f | --force] [-u | --user USER_ID] PACKAGE JOB_ID");
        pw.println("    Trigger immediate execution of a specific scheduled job.");
        pw.println("    Options:");
        pw.println("      -f or --force: run the job even if technical constraints such as");
        pw.println("         connectivity are not currently met");
        pw.println("      -u or --user: specify which user's job is to be run; the default is");
        pw.println("         the primary or system user");
        pw.println("  timeout [-u | --user USER_ID] [PACKAGE] [JOB_ID]");
        pw.println("    Trigger immediate timeout of currently executing jobs, as if their.");
        pw.println("    execution timeout had expired.");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's job is to be run; the default is");
        pw.println("         all users");
        pw.println("  cancel [-u | --user USER_ID] PACKAGE [JOB_ID]");
        pw.println("    Cancel a scheduled job.  If a job ID is not supplied, all jobs scheduled");
        pw.println("    by that package will be canceled.  USE WITH CAUTION.");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's job is to be run; the default is");
        pw.println("         the primary or system user");
        pw.println("  heartbeat [num]");
        pw.println("    With no argument, prints the current standby heartbeat.  With a positive");
        pw.println("    argument, advances the standby heartbeat by that number.");
        pw.println("  monitor-battery [on|off]");
        pw.println("    Control monitoring of all battery changes.  Off by default.  Turning");
        pw.println("    on makes get-battery-seq useful.");
        pw.println("  get-battery-seq");
        pw.println("    Return the last battery update sequence number that was received.");
        pw.println("  get-battery-charging");
        pw.println("    Return whether the battery is currently considered to be charging.");
        pw.println("  get-battery-not-low");
        pw.println("    Return whether the battery is currently considered to not be low.");
        pw.println("  get-storage-seq");
        pw.println("    Return the last storage update sequence number that was received.");
        pw.println("  get-storage-not-low");
        pw.println("    Return whether storage is currently considered to not be low.");
        pw.println("  get-job-state [-u | --user USER_ID] PACKAGE JOB_ID");
        pw.println("    Return the current state of a job, may be any combination of:");
        pw.println("      pending: currently on the pending list, waiting to be active");
        pw.println("      active: job is actively running");
        pw.println("      user-stopped: job can't run because its user is stopped");
        pw.println("      backing-up: job can't run because app is currently backing up its data");
        pw.println("      no-component: job can't run because its component is not available");
        pw.println("      ready: job is ready to run (all constraints satisfied or bypassed)");
        pw.println("      waiting: if nothing else above is printed, job not ready to run");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's job is to be run; the default is");
        pw.println("         the primary or system user");
        pw.println("  trigger-dock-state [idle|active]");
        pw.println("    Trigger wireless charging dock state.  Active by default.");
        pw.println();
    }
}
