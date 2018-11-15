package com.android.commands.svc;

import android.hardware.usb.IUsbManager;
import android.hardware.usb.IUsbManager.Stub;
import android.hardware.usb.UsbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc.Command;
import java.io.PrintStream;

public class UsbCommand extends Command {
    public UsbCommand() {
        super("usb");
    }

    public String shortHelp() {
        return "Control Usb state";
    }

    public String longHelp() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(shortHelp());
        stringBuilder.append("\n\nusage: svc usb setFunctions [function]\n         Set the current usb function. If function is blank, sets to charging.\n       svc usb setScreenUnlockedFunctions [function]\n         Sets the functions which, if the device was charging, become current onscreen unlock. If function is blank, turn off this feature.\n       svc usb getFunctions\n          Gets the list of currently enabled functions\n\npossible values of [function] are any of 'mtp', 'ptp', 'rndis', 'midi'\n");
        return stringBuilder.toString();
    }

    public void run(String[] args) {
        PrintStream printStream;
        StringBuilder stringBuilder;
        if (args.length >= 2) {
            IUsbManager usbMgr = Stub.asInterface(ServiceManager.getService("usb"));
            if ("setFunctions".equals(args[1])) {
                try {
                    usbMgr.setCurrentFunctions(UsbManager.usbFunctionsFromString(args.length >= 3 ? args[2] : ""));
                } catch (RemoteException e) {
                    printStream = System.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error communicating with UsbManager: ");
                    stringBuilder.append(e);
                    printStream.println(stringBuilder.toString());
                }
                return;
            } else if ("getFunctions".equals(args[1])) {
                try {
                    System.err.println(UsbManager.usbFunctionsToString(usbMgr.getCurrentFunctions()));
                } catch (RemoteException e2) {
                    printStream = System.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error communicating with UsbManager: ");
                    stringBuilder.append(e2);
                    printStream.println(stringBuilder.toString());
                }
                return;
            } else if ("setScreenUnlockedFunctions".equals(args[1])) {
                try {
                    usbMgr.setScreenUnlockedFunctions(UsbManager.usbFunctionsFromString(args.length >= 3 ? args[2] : ""));
                } catch (RemoteException e22) {
                    printStream = System.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error communicating with UsbManager: ");
                    stringBuilder.append(e22);
                    printStream.println(stringBuilder.toString());
                }
                return;
            }
        }
        System.err.println(longHelp());
    }
}
