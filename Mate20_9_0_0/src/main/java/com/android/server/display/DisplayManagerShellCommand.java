package com.android.server.display;

import android.content.Intent;
import android.os.ShellCommand;
import java.io.PrintWriter;

class DisplayManagerShellCommand extends ShellCommand {
    private static final String TAG = "DisplayManagerShellCommand";
    private final BinderService mService;

    DisplayManagerShellCommand(BinderService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        Object obj = -1;
        int hashCode = cmd.hashCode();
        if (hashCode != -1505467592) {
            if (hashCode == 1604823708 && cmd.equals("set-brightness")) {
                obj = null;
            }
        } else if (cmd.equals("reset-brightness-configuration")) {
            obj = 1;
        }
        switch (obj) {
            case null:
                return setBrightness();
            case 1:
                return resetBrightnessConfiguration();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Display manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  set-brightness BRIGHTNESS");
        pw.println("    Sets the current brightness to BRIGHTNESS (a number between 0 and 1).");
        pw.println("  reset-brightness-configuration");
        pw.println("    Reset the brightness to its default configuration.");
        pw.println();
        Intent.printIntentArgsHelp(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private int setBrightness() {
        String brightnessText = getNextArg();
        if (brightnessText == null) {
            getErrPrintWriter().println("Error: no brightness specified");
            return 1;
        }
        float brightness = -1.0f;
        try {
            brightness = Float.parseFloat(brightnessText);
        } catch (NumberFormatException e) {
        }
        if (brightness < 0.0f || brightness > 1.0f) {
            getErrPrintWriter().println("Error: brightness should be a number between 0 and 1");
            return 1;
        }
        this.mService.setBrightness(((int) brightness) * 255);
        return 0;
    }

    private int resetBrightnessConfiguration() {
        this.mService.resetBrightnessConfiguration();
        return 0;
    }
}
