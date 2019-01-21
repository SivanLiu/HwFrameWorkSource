package com.android.server.wifi;

import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ShellCommand;
import java.io.PrintWriter;

public class WifiShellCommand extends ShellCommand {
    private final IPackageManager mPM = AppGlobals.getPackageManager();
    private final WifiStateMachine mStateMachine;

    WifiShellCommand(WifiStateMachine stateMachine) {
        this.mStateMachine = stateMachine;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0055 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b0 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0095 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0076 A:{SYNTHETIC, Splitter:B:31:0x0076} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x005b A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0055 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b0 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0095 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0076 A:{SYNTHETIC, Splitter:B:31:0x0076} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x005b A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0055 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b0 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0095 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0076 A:{SYNTHETIC, Splitter:B:31:0x0076} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x005b A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0055 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b0 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0095 A:{Catch:{ Exception -> 0x00d6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0076 A:{SYNTHETIC, Splitter:B:31:0x0076} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x005b A:{Catch:{ Exception -> 0x00d6 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        String str;
        StringBuilder stringBuilder;
        int i;
        checkRootPermission();
        PrintWriter pw = getOutPrintWriter();
        if (cmd != null) {
            str = cmd;
        } else {
            try {
                str = "";
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: ");
                stringBuilder.append(e);
                pw.println(stringBuilder.toString());
                return -1;
            }
        }
        int hashCode = str.hashCode();
        if (hashCode != -1861126232) {
            if (hashCode != -1267819290) {
                if (hashCode != -29690534) {
                    if (hashCode == 1120712756) {
                        if (str.equals("get-ipreach-disconnect")) {
                            i = 1;
                            StringBuilder stringBuilder2;
                            switch (i) {
                                case 0:
                                    boolean enabled;
                                    str = getNextArgRequired();
                                    if ("enabled".equals(str)) {
                                        enabled = true;
                                    } else if ("disabled".equals(str)) {
                                        enabled = false;
                                    } else {
                                        pw.println("Invalid argument to 'set-ipreach-disconnect' - must be 'enabled' or 'disabled'");
                                        return -1;
                                    }
                                    this.mStateMachine.setIpReachabilityDisconnectEnabled(enabled);
                                    return 0;
                                case 1:
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("IPREACH_DISCONNECT state is ");
                                    stringBuilder2.append(this.mStateMachine.getIpReachabilityDisconnectEnabled());
                                    pw.println(stringBuilder2.toString());
                                    return 0;
                                case 2:
                                    try {
                                        i = Integer.parseInt(getNextArgRequired());
                                        if (i < 1) {
                                            pw.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                                            return -1;
                                        }
                                        this.mStateMachine.setPollRssiIntervalMsecs(i);
                                        return 0;
                                    } catch (NumberFormatException e2) {
                                        pw.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                                        return -1;
                                    }
                                case 3:
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("WifiStateMachine.mPollRssiIntervalMsecs = ");
                                    stringBuilder2.append(this.mStateMachine.getPollRssiIntervalMsecs());
                                    pw.println(stringBuilder2.toString());
                                    return 0;
                                default:
                                    return handleDefaultCommands(cmd);
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception: ");
                            stringBuilder.append(e);
                            pw.println(stringBuilder.toString());
                            return -1;
                        }
                    }
                } else if (str.equals("set-poll-rssi-interval-msecs")) {
                    i = 2;
                    switch (i) {
                        case 0:
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
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception: ");
                    stringBuilder.append(e);
                    pw.println(stringBuilder.toString());
                    return -1;
                }
            } else if (str.equals("get-poll-rssi-interval-msecs")) {
                i = 3;
                switch (i) {
                    case 0:
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
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: ");
                stringBuilder.append(e);
                pw.println(stringBuilder.toString());
                return -1;
            }
        } else if (str.equals("set-ipreach-disconnect")) {
            i = 0;
            switch (i) {
                case 0:
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
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
        i = -1;
        switch (i) {
            case 0:
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
        stringBuilder = new StringBuilder();
        stringBuilder.append("Exception: ");
        stringBuilder.append(e);
        pw.println(stringBuilder.toString());
        return -1;
    }

    private void checkRootPermission() {
        int uid = Binder.getCallingUid();
        if (uid != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Uid ");
            stringBuilder.append(uid);
            stringBuilder.append(" does not have access to wifi commands");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Wi-Fi (wifi) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-ipreach-disconnect enabled|disabled");
        pw.println("    Sets whether CMD_IP_REACHABILITY_LOST events should trigger disconnects.");
        pw.println("  get-ipreach-disconnect");
        pw.println("    Gets setting of CMD_IP_REACHABILITY_LOST events triggering disconnects.");
        pw.println("  set-poll-rssi-interval-msecs <int>");
        pw.println("    Sets the interval between RSSI polls to <int> milliseconds.");
        pw.println("  get-poll-rssi-interval-msecs");
        pw.println("    Gets current interval between RSSI polls, in milliseconds.");
        pw.println();
    }
}
