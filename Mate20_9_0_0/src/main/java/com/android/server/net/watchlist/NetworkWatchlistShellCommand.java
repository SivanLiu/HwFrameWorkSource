package com.android.server.net.watchlist;

import android.content.Context;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.provider.Settings.Global;
import java.io.FileInputStream;
import java.io.PrintWriter;

class NetworkWatchlistShellCommand extends ShellCommand {
    final Context mContext;
    final NetworkWatchlistService mService;

    NetworkWatchlistShellCommand(NetworkWatchlistService service, Context context) {
        this.mContext = context;
        this.mService = service;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0034 A:{Catch:{ Exception -> 0x0044 }} */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x003e A:{Catch:{ Exception -> 0x0044 }} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0039 A:{Catch:{ Exception -> 0x0044 }} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0034 A:{Catch:{ Exception -> 0x0044 }} */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x003e A:{Catch:{ Exception -> 0x0044 }} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0039 A:{Catch:{ Exception -> 0x0044 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int hashCode = cmd.hashCode();
            if (hashCode == 1757613042) {
                if (cmd.equals("set-test-config")) {
                    hashCode = 0;
                    switch (hashCode) {
                        case 0:
                            break;
                        case 1:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 1854202282) {
                if (cmd.equals("force-generate-report")) {
                    hashCode = 1;
                    switch (hashCode) {
                        case 0:
                            return runSetTestConfig();
                        case 1:
                            return runForceGenerateReport();
                        default:
                            return handleDefaultCommands(cmd);
                    }
                }
            }
            hashCode = -1;
            switch (hashCode) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private int runSetTestConfig() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            ParcelFileDescriptor pfd = openFileForSystem(getNextArgRequired(), "r");
            if (pfd != null) {
                WatchlistConfig.getInstance().setTestMode(new FileInputStream(pfd.getFileDescriptor()));
            }
            pw.println("Success!");
            return 0;
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(ex.toString());
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private int runForceGenerateReport() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        long ident = Binder.clearCallingIdentity();
        try {
            if (WatchlistConfig.getInstance().isConfigSecure()) {
                pw.println("Error: Cannot force generate report under production config");
                return -1;
            }
            Global.putLong(this.mContext.getContentResolver(), "network_watchlist_last_report_time", 0);
            this.mService.forceReportWatchlistForTest(System.currentTimeMillis());
            pw.println("Success!");
            Binder.restoreCallingIdentity(ident);
            return 0;
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(ex);
            pw.println(stringBuilder.toString());
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Network watchlist manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-test-config your_watchlist_config.xml");
        pw.println("    Set network watchlist test config file.");
        pw.println("  force-generate-report");
        pw.println("    Force generate watchlist test report.");
    }
}
