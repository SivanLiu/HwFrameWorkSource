package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.view.IWindowManager;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowManagerShellCommand extends ShellCommand {
    private final IWindowManager mInterface;
    private final WindowManagerService mInternal;

    public WindowManagerShellCommand(WindowManagerService service) {
        this.mInterface = service;
        this.mInternal = service;
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
                case -1067396926:
                    if (cmd.equals("tracing")) {
                        i = 5;
                        break;
                    }
                case -229462135:
                    if (cmd.equals("dismiss-keyguard")) {
                        i = 4;
                        break;
                    }
                case 3530753:
                    if (cmd.equals("size")) {
                        i = 0;
                        break;
                    }
                case 530020689:
                    if (cmd.equals("overscan")) {
                        i = 2;
                        break;
                    }
                case 1552717032:
                    if (cmd.equals("density")) {
                        i = 1;
                        break;
                    }
                case 1910897543:
                    if (cmd.equals("scaling")) {
                        i = 3;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    return runDisplaySize(pw);
                case 1:
                    return runDisplayDensity(pw);
                case 2:
                    return runDisplayOverscan(pw);
                case 3:
                    return runDisplayScaling(pw);
                case 4:
                    return runDismissKeyguard(pw);
                case 5:
                    return this.mInternal.mWindowTracing.onShellCommand(this, getNextArgRequired());
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

    private int runDisplaySize(PrintWriter pw) throws RemoteException {
        String size = getNextArg();
        StringBuilder stringBuilder;
        if (size == null) {
            Point initialSize = new Point();
            Point baseSize = new Point();
            try {
                this.mInterface.getInitialDisplaySize(0, initialSize);
                this.mInterface.getBaseDisplaySize(0, baseSize);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Physical size: ");
                stringBuilder.append(initialSize.x);
                stringBuilder.append("x");
                stringBuilder.append(initialSize.y);
                pw.println(stringBuilder.toString());
                if (!initialSize.equals(baseSize)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Override size: ");
                    stringBuilder.append(baseSize.x);
                    stringBuilder.append("x");
                    stringBuilder.append(baseSize.y);
                    pw.println(stringBuilder.toString());
                }
            } catch (RemoteException e) {
            }
            return 0;
        }
        int div;
        int w = -1;
        if ("reset".equals(size)) {
            div = -1;
        } else {
            div = size.indexOf(120);
            if (div <= 0 || div >= size.length() - 1) {
                PrintWriter errPrintWriter = getErrPrintWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error: bad size ");
                stringBuilder.append(size);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
            String wstr = size.substring(0, div);
            try {
                div = parseDimension(size.substring(div + 1));
                w = parseDimension(wstr);
            } catch (NumberFormatException e2) {
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: bad number ");
                stringBuilder2.append(e2);
                errPrintWriter2.println(stringBuilder2.toString());
                return -1;
            }
        }
        if (w < 0 || div < 0) {
            this.mInterface.clearForcedDisplaySize(0);
        } else {
            this.mInterface.setForcedDisplaySize(0, w, div);
        }
        return 0;
    }

    private int runDisplayDensity(PrintWriter pw) throws RemoteException {
        String densityStr = getNextArg();
        int initialDensity;
        int baseDensity;
        StringBuilder stringBuilder;
        if (densityStr == null) {
            try {
                initialDensity = this.mInterface.getInitialDisplayDensity(0);
                baseDensity = this.mInterface.getBaseDisplayDensity(0);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Physical density: ");
                stringBuilder.append(initialDensity);
                pw.println(stringBuilder.toString());
                if (initialDensity != baseDensity) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Override density: ");
                    stringBuilder.append(baseDensity);
                    pw.println(stringBuilder.toString());
                }
            } catch (RemoteException e) {
            }
            return 0;
        }
        if ("reset".equals(densityStr)) {
            initialDensity = -1;
        } else {
            try {
                baseDensity = Integer.parseInt(densityStr);
                if (baseDensity < 72) {
                    getErrPrintWriter().println("Error: density must be >= 72");
                    return -1;
                }
                initialDensity = baseDensity;
            } catch (NumberFormatException e2) {
                PrintWriter errPrintWriter = getErrPrintWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error: bad number ");
                stringBuilder.append(e2);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
        if (initialDensity > 0) {
            this.mInterface.setForcedDisplayDensityForUser(0, initialDensity, -2);
        } else {
            this.mInterface.clearForcedDisplayDensityForUser(0, -2);
        }
        return 0;
    }

    private int runDisplayOverscan(PrintWriter pw) throws RemoteException {
        String overscanStr = getNextArgRequired();
        Rect rect = new Rect();
        if ("reset".equals(overscanStr)) {
            rect.set(0, 0, 0, 0);
        } else {
            Matcher matcher = Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)").matcher(overscanStr);
            if (matcher.matches()) {
                rect.left = Integer.parseInt(matcher.group(1));
                rect.top = Integer.parseInt(matcher.group(2));
                rect.right = Integer.parseInt(matcher.group(3));
                rect.bottom = Integer.parseInt(matcher.group(4));
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: bad rectangle arg: ");
                stringBuilder.append(overscanStr);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
        this.mInterface.setOverscan(0, rect.left, rect.top, rect.right, rect.bottom);
        return 0;
    }

    private int runDisplayScaling(PrintWriter pw) throws RemoteException {
        String scalingStr = getNextArgRequired();
        if (Shell.NIGHT_MODE_STR_AUTO.equals(scalingStr)) {
            this.mInterface.setForcedDisplayScalingMode(0, 0);
        } else if ("off".equals(scalingStr)) {
            this.mInterface.setForcedDisplayScalingMode(0, 1);
        } else {
            getErrPrintWriter().println("Error: scaling must be 'auto' or 'off'");
            return -1;
        }
        return 0;
    }

    private int runDismissKeyguard(PrintWriter pw) throws RemoteException {
        this.mInterface.dismissKeyguard(null, null);
        return 0;
    }

    private int parseDimension(String s) throws NumberFormatException {
        if (s.endsWith("px")) {
            return Integer.parseInt(s.substring(0, s.length() - 2));
        }
        if (!s.endsWith("dp")) {
            return Integer.parseInt(s);
        }
        int density;
        try {
            density = this.mInterface.getBaseDisplayDensity(0);
        } catch (RemoteException e) {
            density = 160;
        }
        return (Integer.parseInt(s.substring(0, s.length() - 2)) * density) / 160;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Window manager (window) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  size [reset|WxH|WdpxHdp]");
        pw.println("    Return or override display size.");
        pw.println("    width and height in pixels unless suffixed with 'dp'.");
        pw.println("  density [reset|DENSITY]");
        pw.println("    Return or override display density.");
        pw.println("  overscan [reset|LEFT,TOP,RIGHT,BOTTOM]");
        pw.println("    Set overscan area for display.");
        pw.println("  scaling [off|auto]");
        pw.println("    Set display scaling mode.");
        pw.println("  dismiss-keyguard");
        pw.println("    Dismiss the keyguard, prompting user for auth if necessary.");
        if (!Build.IS_USER) {
            pw.println("  tracing (start | stop)");
            pw.println("    Start or stop window tracing.");
        }
    }
}
