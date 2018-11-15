package com.android.server.om;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

final class OverlayManagerShellCommand extends ShellCommand {
    private final IOverlayManager mInterface;

    OverlayManagerShellCommand(IOverlayManager iom) {
        this.mInterface = iom;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        StringBuilder stringBuilder;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter err = getErrPrintWriter();
        try {
            boolean z;
            switch (cmd.hashCode()) {
                case -1361113425:
                    if (cmd.equals("set-priority")) {
                        z = true;
                        break;
                    }
                case -1298848381:
                    if (cmd.equals("enable")) {
                        z = true;
                        break;
                    }
                case -794624300:
                    if (cmd.equals("enable-exclusive")) {
                        z = true;
                        break;
                    }
                case 3322014:
                    if (cmd.equals("list")) {
                        z = false;
                        break;
                    }
                case 1671308008:
                    if (cmd.equals("disable")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return runList();
                case true:
                    return runEnableDisable(true);
                case true:
                    return runEnableDisable(false);
                case true:
                    return runEnableExclusive();
                case true:
                    return runSetPriority();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(e.getMessage());
            err.println(stringBuilder.toString());
            return -1;
        } catch (RemoteException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e2);
            err.println(stringBuilder.toString());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter out = getOutPrintWriter();
        out.println("Overlay manager (overlay) commands:");
        out.println("  help");
        out.println("    Print this help text.");
        out.println("  dump [--verbose] [--user USER_ID] [PACKAGE [PACKAGE [...]]]");
        out.println("    Print debugging information about the overlay manager.");
        out.println("  list [--user USER_ID] [PACKAGE [PACKAGE [...]]]");
        out.println("    Print information about target and overlay packages.");
        out.println("    Overlay packages are printed in priority order. With optional");
        out.println("    parameters PACKAGEs, limit output to the specified packages");
        out.println("    but include more information about each package.");
        out.println("  enable [--user USER_ID] PACKAGE");
        out.println("    Enable overlay package PACKAGE.");
        out.println("  disable [--user USER_ID] PACKAGE");
        out.println("    Disable overlay package PACKAGE.");
        out.println("  enable-exclusive [--user USER_ID] [--category] PACKAGE");
        out.println("    Enable overlay package PACKAGE and disable all other overlays for");
        out.println("    its target package. If the --category option is given, only disables");
        out.println("    other overlays in the same category.");
        out.println("  set-priority [--user USER_ID] PACKAGE PARENT|lowest|highest");
        out.println("    Change the priority of the overlay PACKAGE to be just higher than");
        out.println("    the priority of PACKAGE_PARENT If PARENT is the special keyword");
        out.println("    'lowest', change priority of PACKAGE to the lowest priority.");
        out.println("    If PARENT is the special keyword 'highest', change priority of");
        out.println("    PACKAGE to the highest priority.");
    }

    private int runList() throws RemoteException {
        PrintWriter out = getOutPrintWriter();
        PrintWriter err = getErrPrintWriter();
        int i = 0;
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int i2 = 1;
            if (nextOption != null) {
                int i3 = -1;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    i3 = 0;
                }
                if (i3 != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return 1;
                }
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                Map<String, List<OverlayInfo>> allOverlays = this.mInterface.getAllOverlays(userId);
                for (String targetPackageName : allOverlays.keySet()) {
                    out.println(targetPackageName);
                    List<OverlayInfo> overlaysForTarget = (List) allOverlays.get(targetPackageName);
                    int N = overlaysForTarget.size();
                    int i4 = i;
                    while (i4 < N) {
                        String status;
                        int i5 = ((OverlayInfo) overlaysForTarget.get(i4)).state;
                        if (i5 != 6) {
                            switch (i5) {
                                case 2:
                                    status = "[ ]";
                                    break;
                                case 3:
                                    break;
                                default:
                                    status = "---";
                                    break;
                            }
                        }
                        status = "[x]";
                        out.println(String.format("%s %s", new Object[]{status, oi.packageName}));
                        i4++;
                        i2 = 1;
                        i = 0;
                    }
                    int i6 = i2;
                    out.println();
                    i = 0;
                }
                return 0;
            }
        }
    }

    private int runEnableDisable(boolean enable) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                Object obj = -1;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    obj = null;
                }
                if (obj != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return 1;
                }
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                return this.mInterface.setEnabled(getNextArgRequired(), enable, userId) ^ 1;
            }
        }
    }

    private int runEnableExclusive() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        boolean inCategory = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                boolean z = true;
                int hashCode = opt.hashCode();
                if (hashCode != 66265758) {
                    if (hashCode == 1333469547 && opt.equals("--user")) {
                        z = false;
                    }
                } else if (opt.equals("--category")) {
                    z = true;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case true:
                        inCategory = true;
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: Unknown option: ");
                        stringBuilder.append(opt);
                        err.println(stringBuilder.toString());
                        return 1;
                }
            }
            String overlay = getNextArgRequired();
            if (inCategory) {
                return this.mInterface.setEnabledExclusiveInCategory(overlay, userId) ^ 1;
            }
            return this.mInterface.setEnabledExclusive(overlay, true, userId) ^ 1;
        }
    }

    private int runSetPriority() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                Object obj = -1;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    obj = null;
                }
                if (obj != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    err.println(stringBuilder.toString());
                    return 1;
                }
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                String packageName = getNextArgRequired();
                nextOption = getNextArgRequired();
                if ("highest".equals(nextOption)) {
                    return 1 ^ this.mInterface.setHighestPriority(packageName, userId);
                }
                if ("lowest".equals(nextOption)) {
                    return 1 ^ this.mInterface.setLowestPriority(packageName, userId);
                }
                return 1 ^ this.mInterface.setPriority(packageName, nextOption, userId);
            }
        }
    }
}
