package com.android.server.content;

import android.content.IContentService;
import android.os.RemoteException;
import android.os.ShellCommand;
import java.io.PrintWriter;

public class ContentShellCommand extends ShellCommand {
    final IContentService mInterface;

    ContentShellCommand(IContentService service) {
        this.mInterface = service;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0029 A:{Catch:{ RemoteException -> 0x002e }} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0024 A:{Catch:{ RemoteException -> 0x002e }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int i;
            if (cmd.hashCode() == -796331115) {
                if (cmd.equals("reset-today-stats")) {
                    i = 0;
                    if (i == 0) {
                        return handleDefaultCommands(cmd);
                    }
                    return runResetTodayStats();
                }
            }
            i = -1;
            if (i == 0) {
            }
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private int runResetTodayStats() throws RemoteException {
        this.mInterface.resetTodayStats();
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Content service commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  reset-today-stats");
        pw.println("    Reset 1-day sync stats.");
        pw.println();
    }
}
