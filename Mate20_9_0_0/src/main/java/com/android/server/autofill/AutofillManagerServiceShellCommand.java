package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.service.autofill.AutofillFieldClassificationService.Scores;
import com.android.internal.os.IResultReceiver.Stub;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AutofillManagerServiceShellCommand extends ShellCommand {
    private final AutofillManagerService mService;

    public AutofillManagerServiceShellCommand(AutofillManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        Object obj = -1;
        switch (cmd.hashCode()) {
            case 102230:
                if (cmd.equals("get")) {
                    obj = 3;
                    break;
                }
                break;
            case 113762:
                if (cmd.equals("set")) {
                    obj = 4;
                    break;
                }
                break;
            case 3322014:
                if (cmd.equals("list")) {
                    obj = null;
                    break;
                }
                break;
            case 108404047:
                if (cmd.equals("reset")) {
                    obj = 2;
                    break;
                }
                break;
            case 1557372922:
                if (cmd.equals("destroy")) {
                    obj = 1;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                return requestList(pw);
            case 1:
                return requestDestroy(pw);
            case 2:
                return requestReset();
            case 3:
                return requestGet(pw);
            case 4:
                return requestSet(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x00f5, code skipped:
            if (r0 != null) goto L_0x00f7;
     */
    /* JADX WARNING: Missing block: B:10:0x00f7, code skipped:
            if (r1 != null) goto L_0x00f9;
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:13:0x00fd, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:14:0x00fe, code skipped:
            r1.addSuppressed(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x0102, code skipped:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("AutoFill Service (autofill) commands:");
        pw.println("  help");
        pw.println("    Prints this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get log_level ");
        pw.println("    Gets the Autofill log level (off | debug | verbose).");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get max_partitions");
        pw.println("    Gets the maximum number of partitions per session.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get max_visible_datasets");
        pw.println("    Gets the maximum number of visible datasets in the UI.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get full_screen_mode");
        pw.println("    Gets the Fill UI full screen mode");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get fc_score [--algorithm ALGORITHM] value1 value2");
        pw.println("    Gets the field classification score for 2 fields.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get bind-instant-service-allowed");
        pw.println("    Gets whether binding to services provided by instant apps is allowed");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set log_level [off | debug | verbose]");
        pw.println("    Sets the Autofill log level.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set max_partitions number");
        pw.println("    Sets the maximum number of partitions per session.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set max_visible_datasets number");
        pw.println("    Sets the maximum number of visible datasets in the UI.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set full_screen_mode [true | false | default]");
        pw.println("    Sets the Fill UI full screen mode");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set bind-instant-service-allowed [true | false]");
        pw.println("    Sets whether binding to services provided by instant apps is allowed");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list sessions [--user USER_ID]");
        pw.println("    Lists all pending sessions.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  destroy sessions [--user USER_ID]");
        pw.println("    Destroys all pending sessions.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  reset");
        pw.println("    Resets all pending sessions and cached service connections.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (pw != null) {
            pw.close();
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int requestGet(PrintWriter pw) {
        int i;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case -2124387184:
                if (what.equals("fc_score")) {
                    i = 3;
                    break;
                }
            case -2006901047:
                if (what.equals("log_level")) {
                    i = 0;
                    break;
                }
            case -1298810906:
                if (what.equals("full_screen_mode")) {
                    i = 4;
                    break;
                }
            case 809633044:
                if (what.equals("bind-instant-service-allowed")) {
                    i = 5;
                    break;
                }
            case 1393110435:
                if (what.equals("max_visible_datasets")) {
                    i = 2;
                    break;
                }
            case 1772188804:
                if (what.equals("max_partitions")) {
                    i = 1;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return getLogLevel(pw);
            case 1:
                return getMaxPartitions(pw);
            case 2:
                return getMaxVisibileDatasets(pw);
            case 3:
                return getFieldClassificationScore(pw);
            case 4:
                return getFullScreenMode(pw);
            case 5:
                return getBindInstantService(pw);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid set: ");
                stringBuilder.append(what);
                pw.println(stringBuilder.toString());
                return -1;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int requestSet(PrintWriter pw) {
        int i;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case -2006901047:
                if (what.equals("log_level")) {
                    i = 0;
                    break;
                }
            case -1298810906:
                if (what.equals("full_screen_mode")) {
                    i = 3;
                    break;
                }
            case 809633044:
                if (what.equals("bind-instant-service-allowed")) {
                    i = 4;
                    break;
                }
            case 1393110435:
                if (what.equals("max_visible_datasets")) {
                    i = 2;
                    break;
                }
            case 1772188804:
                if (what.equals("max_partitions")) {
                    i = 1;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return setLogLevel(pw);
            case 1:
                return setMaxPartitions();
            case 2:
                return setMaxVisibileDatasets();
            case 3:
                return setFullScreenMode(pw);
            case 4:
                return setBindInstantService(pw);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid set: ");
                stringBuilder.append(what);
                pw.println(stringBuilder.toString());
                return -1;
        }
    }

    private int getLogLevel(PrintWriter pw) {
        int logLevel = this.mService.getLogLevel();
        if (logLevel == 0) {
            pw.println("off");
            return 0;
        } else if (logLevel == 2) {
            pw.println("debug");
            return 0;
        } else if (logLevel != 4) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknow (");
            stringBuilder.append(logLevel);
            stringBuilder.append(")");
            pw.println(stringBuilder.toString());
            return 0;
        } else {
            pw.println("verbose");
            return 0;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0043  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0058  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0043  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0058  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0043  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0058  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int setLogLevel(PrintWriter pw) {
        int i;
        String logLevel = getNextArgRequired();
        String toLowerCase = logLevel.toLowerCase();
        int hashCode = toLowerCase.hashCode();
        if (hashCode == 109935) {
            if (toLowerCase.equals("off")) {
                i = 2;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 95458899) {
            if (toLowerCase.equals("debug")) {
                i = 1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 351107458 && toLowerCase.equals("verbose")) {
            i = 0;
            switch (i) {
                case 0:
                    this.mService.setLogLevel(4);
                    return 0;
                case 1:
                    this.mService.setLogLevel(2);
                    return 0;
                case 2:
                    this.mService.setLogLevel(0);
                    return 0;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid level: ");
                    stringBuilder.append(logLevel);
                    pw.println(stringBuilder.toString());
                    return -1;
            }
        }
        i = -1;
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    private int getMaxPartitions(PrintWriter pw) {
        pw.println(this.mService.getMaxPartitions());
        return 0;
    }

    private int setMaxPartitions() {
        this.mService.setMaxPartitions(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    private int getMaxVisibileDatasets(PrintWriter pw) {
        pw.println(this.mService.getMaxVisibleDatasets());
        return 0;
    }

    private int setMaxVisibileDatasets() {
        this.mService.setMaxVisibleDatasets(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    private int getFieldClassificationScore(PrintWriter pw) {
        String algorithm;
        String value1;
        String nextArg = getNextArgRequired();
        if ("--algorithm".equals(nextArg)) {
            algorithm = getNextArgRequired();
            value1 = getNextArgRequired();
        } else {
            algorithm = null;
            value1 = nextArg;
        }
        String value2 = getNextArgRequired();
        CountDownLatch latch = new CountDownLatch(1);
        this.mService.getScore(algorithm, value1, value2, new RemoteCallback(new -$$Lambda$AutofillManagerServiceShellCommand$3WCRplTGFh_xsmb8tmAG8x-Pn5A(pw, latch)));
        return waitForLatch(pw, latch);
    }

    static /* synthetic */ void lambda$getFieldClassificationScore$0(PrintWriter pw, CountDownLatch latch, Bundle result) {
        Scores scores = (Scores) result.getParcelable("scores");
        if (scores == null) {
            pw.println("no score");
        } else {
            pw.println(scores.scores[0][0]);
        }
        latch.countDown();
    }

    private int getFullScreenMode(PrintWriter pw) {
        Boolean mode = this.mService.getFullScreenMode();
        if (mode == null) {
            pw.println(HealthServiceWrapper.INSTANCE_VENDOR);
        } else if (mode.booleanValue()) {
            pw.println("true");
        } else {
            pw.println("false");
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0056  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int setFullScreenMode(PrintWriter pw) {
        int i;
        String mode = getNextArgRequired();
        String toLowerCase = mode.toLowerCase();
        int hashCode = toLowerCase.hashCode();
        if (hashCode == 3569038) {
            if (toLowerCase.equals("true")) {
                i = 0;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 97196323) {
            if (toLowerCase.equals("false")) {
                i = 1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 1544803905 && toLowerCase.equals(HealthServiceWrapper.INSTANCE_VENDOR)) {
            i = 2;
            switch (i) {
                case 0:
                    this.mService.setFullScreenMode(Boolean.TRUE);
                    return 0;
                case 1:
                    this.mService.setFullScreenMode(Boolean.FALSE);
                    return 0;
                case 2:
                    this.mService.setFullScreenMode(null);
                    return 0;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid mode: ");
                    stringBuilder.append(mode);
                    pw.println(stringBuilder.toString());
                    return -1;
            }
        }
        i = -1;
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    private int getBindInstantService(PrintWriter pw) {
        if (this.mService.getAllowInstantService()) {
            pw.println("true");
        } else {
            pw.println("false");
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0033  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0033  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0048  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int setBindInstantService(PrintWriter pw) {
        boolean z;
        String mode = getNextArgRequired();
        String toLowerCase = mode.toLowerCase();
        int hashCode = toLowerCase.hashCode();
        if (hashCode == 3569038) {
            if (toLowerCase.equals("true")) {
                z = false;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 97196323 && toLowerCase.equals("false")) {
            z = true;
            switch (z) {
                case false:
                    this.mService.setAllowInstantService(true);
                    return 0;
                case true:
                    this.mService.setAllowInstantService(false);
                    return 0;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid mode: ");
                    stringBuilder.append(mode);
                    pw.println(stringBuilder.toString());
                    return -1;
            }
        }
        z = true;
        switch (z) {
            case false:
                break;
            case true:
                break;
            default:
                break;
        }
    }

    private int requestDestroy(PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }
        int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        return requestSessionCommon(pw, latch, new -$$Lambda$AutofillManagerServiceShellCommand$ww56nbkJspkRdVJ0yMdT4sroSiY(this, userId, new Stub() {
            public void send(int resultCode, Bundle resultData) {
                latch.countDown();
            }
        }));
    }

    private int requestList(final PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }
        int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        return requestSessionCommon(pw, latch, new -$$Lambda$AutofillManagerServiceShellCommand$WrWpLlZPawytZji8-6Dx9_p70Dw(this, userId, new Stub() {
            public void send(int resultCode, Bundle resultData) {
                Iterator it = resultData.getStringArrayList("sessions").iterator();
                while (it.hasNext()) {
                    pw.println((String) it.next());
                }
                latch.countDown();
            }
        }));
    }

    private boolean isNextArgSessions(PrintWriter pw) {
        if (getNextArgRequired().equals("sessions")) {
            return true;
        }
        pw.println("Error: invalid list type");
        return false;
    }

    private int requestSessionCommon(PrintWriter pw, CountDownLatch latch, Runnable command) {
        command.run();
        return waitForLatch(pw, latch);
    }

    private int waitForLatch(PrintWriter pw, CountDownLatch latch) {
        try {
            if (latch.await(true, TimeUnit.SECONDS)) {
                return 0;
            }
            pw.println("Timed out after 5 seconds");
            return -1;
        } catch (InterruptedException e) {
            pw.println("System call interrupted");
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private int requestReset() {
        this.mService.reset();
        return 0;
    }

    private int getUserIdFromArgsOrAllUsers() {
        if ("--user".equals(getNextArg())) {
            return UserHandle.parseUserArg(getNextArgRequired());
        }
        return -1;
    }
}
