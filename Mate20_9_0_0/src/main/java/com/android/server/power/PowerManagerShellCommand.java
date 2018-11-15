package com.android.server.power;

import android.content.Intent;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import java.io.PrintWriter;

class PowerManagerShellCommand extends ShellCommand {
    private static final int LOW_POWER_MODE_ON = 1;
    final IPowerManager mInterface;

    PowerManagerShellCommand(IPowerManager service) {
        this.mInterface = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int i = (cmd.hashCode() == 1369181230 && cmd.equals("set-mode")) ? 0 : -1;
            if (i != 0) {
                return handleDefaultCommands(cmd);
            }
            return runSetMode();
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private int runSetMode() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int mode = -1;
        try {
            int mode2 = Integer.parseInt(getNextArgRequired());
            IPowerManager iPowerManager = this.mInterface;
            boolean z = true;
            if (mode2 != 1) {
                z = false;
            }
            iPowerManager.setPowerSaveMode(z);
            return 0;
        } catch (RuntimeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(ex.toString());
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Power manager (power) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-mode MODE");
        pw.println("    sets the power mode of the device to MODE.");
        pw.println("    1 turns low power mode on and 0 turns low power mode off.");
        pw.println();
        Intent.printIntentArgsHelp(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
