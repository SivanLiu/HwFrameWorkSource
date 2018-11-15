package com.android.server.webkit;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.webkit.IWebViewUpdateService;
import java.io.PrintWriter;

class WebViewUpdateServiceShellCommand extends ShellCommand {
    final IWebViewUpdateService mInterface;

    WebViewUpdateServiceShellCommand(IWebViewUpdateService service) {
        this.mInterface = service;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            boolean z;
            switch (cmd.hashCode()) {
                case -1857752288:
                    if (cmd.equals("enable-multiprocess")) {
                        z = true;
                        break;
                    }
                case -1381305903:
                    if (cmd.equals("set-webview-implementation")) {
                        z = true;
                        break;
                    }
                case 436183515:
                    if (cmd.equals("disable-multiprocess")) {
                        z = true;
                        break;
                    }
                case 823481554:
                    if (cmd.equals("disable-redundant-packages")) {
                        z = true;
                        break;
                    }
                case 2070404695:
                    if (cmd.equals("enable-redundant-packages")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return enableFallbackLogic(false);
                case true:
                    return enableFallbackLogic(true);
                case true:
                    return setWebViewImplementation();
                case true:
                    return enableMultiProcess(true);
                case true:
                    return enableMultiProcess(false);
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

    private int enableFallbackLogic(boolean enable) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        this.mInterface.enableFallbackLogic(enable);
        pw.println("Success");
        return 0;
    }

    private int setWebViewImplementation() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String shellChosenPackage = getNextArg();
        if (shellChosenPackage.equals(this.mInterface.changeProviderAndSetting(shellChosenPackage))) {
            pw.println("Success");
            return 0;
        }
        pw.println(String.format("Failed to switch to %s, the WebView implementation is now provided by %s.", new Object[]{shellChosenPackage, this.mInterface.changeProviderAndSetting(shellChosenPackage)}));
        return 1;
    }

    private int enableMultiProcess(boolean enable) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        this.mInterface.enableMultiProcess(enable);
        pw.println("Success");
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("WebView updater commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  enable-redundant-packages");
        pw.println("    Allow a fallback package to be installed and enabled even when a");
        pw.println("    more-preferred package is available. This command is useful when testing");
        pw.println("    fallback packages.");
        pw.println("  disable-redundant-packages");
        pw.println("    Disallow installing and enabling fallback packages when a more-preferred");
        pw.println("    package is available.");
        pw.println("  set-webview-implementation PACKAGE");
        pw.println("    Set the WebView implementation to the specified package.");
        pw.println("  enable-multiprocess");
        pw.println("    Enable multi-process mode for WebView");
        pw.println("  disable-multiprocess");
        pw.println("    Disable multi-process mode for WebView");
        pw.println();
    }
}
