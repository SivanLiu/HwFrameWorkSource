package com.android.commands.svc;

import android.net.wifi.IWifiManager;
import android.net.wifi.IWifiManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc.Command;
import java.io.PrintStream;

public class WifiCommand extends Command {
    public WifiCommand() {
        super("wifi");
    }

    public String shortHelp() {
        return "Control the Wi-Fi manager";
    }

    public String longHelp() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(shortHelp());
        stringBuilder.append("\n\nusage: svc wifi [enable|disable]\n         Turn Wi-Fi on or off.\n\n");
        return stringBuilder.toString();
    }

    public void run(String[] args) {
        boolean validCommand = false;
        if (args.length >= 2) {
            boolean flag = false;
            if ("enable".equals(args[1])) {
                flag = true;
                validCommand = true;
            } else if ("disable".equals(args[1])) {
                flag = false;
                validCommand = true;
            }
            if (validCommand) {
                IWifiManager wifiMgr = Stub.asInterface(ServiceManager.getService("wifi"));
                if (wifiMgr == null) {
                    System.err.println("Wi-Fi service is not ready");
                    return;
                }
                try {
                    wifiMgr.setWifiEnabled("com.android.shell", flag);
                } catch (RemoteException e) {
                    PrintStream printStream = System.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Wi-Fi operation failed: ");
                    stringBuilder.append(e);
                    printStream.println(stringBuilder.toString());
                }
                return;
            }
        }
        System.err.println(longHelp());
    }
}
