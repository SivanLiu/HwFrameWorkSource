package com.android.server.pm;

import android.content.pm.IOtaDexopt;
import android.os.RemoteException;
import android.os.ShellCommand;
import java.io.PrintWriter;
import java.util.Locale;

class OtaDexoptShellCommand extends ShellCommand {
    final IOtaDexopt mInterface;

    OtaDexoptShellCommand(OtaDexoptService service) {
        this.mInterface = service;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int i;
            switch (cmd.hashCode()) {
                case -1001078227:
                    if (cmd.equals("progress")) {
                        i = 5;
                        break;
                    }
                case -318370553:
                    if (cmd.equals("prepare")) {
                        i = 0;
                        break;
                    }
                case 3089282:
                    if (cmd.equals("done")) {
                        i = 2;
                        break;
                    }
                case 3377907:
                    if (cmd.equals("next")) {
                        i = 4;
                        break;
                    }
                case 3540684:
                    if (cmd.equals("step")) {
                        i = 3;
                        break;
                    }
                case 856774308:
                    if (cmd.equals("cleanup")) {
                        i = 1;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    return runOtaPrepare();
                case 1:
                    return runOtaCleanup();
                case 2:
                    return runOtaDone();
                case 3:
                    return runOtaStep();
                case 4:
                    return runOtaNext();
                case 5:
                    return runOtaProgress();
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

    private int runOtaPrepare() throws RemoteException {
        this.mInterface.prepare();
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runOtaCleanup() throws RemoteException {
        this.mInterface.cleanup();
        return 0;
    }

    private int runOtaDone() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        if (this.mInterface.isDone()) {
            pw.println("OTA complete.");
        } else {
            pw.println("OTA incomplete.");
        }
        return 0;
    }

    private int runOtaStep() throws RemoteException {
        this.mInterface.dexoptNextPackage();
        return 0;
    }

    private int runOtaNext() throws RemoteException {
        getOutPrintWriter().println(this.mInterface.nextDexoptCommand());
        return 0;
    }

    private int runOtaProgress() throws RemoteException {
        float progress = this.mInterface.getProgress();
        getOutPrintWriter().format(Locale.ROOT, "%.2f", new Object[]{Float.valueOf(progress)});
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("OTA Dexopt (ota) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  prepare");
        pw.println("    Prepare an OTA dexopt pass, collecting all packages.");
        pw.println("  done");
        pw.println("    Replies whether the OTA is complete or not.");
        pw.println("  step");
        pw.println("    OTA dexopt the next package.");
        pw.println("  next");
        pw.println("    Get parameters for OTA dexopt of the next package.");
        pw.println("  cleanup");
        pw.println("    Clean up internal states. Ends an OTA session.");
    }
}
