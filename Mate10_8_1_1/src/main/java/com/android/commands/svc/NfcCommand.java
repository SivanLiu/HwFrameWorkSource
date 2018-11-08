package com.android.commands.svc;

import android.nfc.INfcAdapter;
import android.nfc.INfcAdapter.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc.Command;

public class NfcCommand extends Command {
    public NfcCommand() {
        super("nfc");
    }

    public String shortHelp() {
        return "Control NFC functions";
    }

    public String longHelp() {
        return shortHelp() + "\n" + "\n" + "usage: svc nfc [enable|disable]\n" + "         Turn NFC on or off.\n\n";
    }

    public void run(String[] args) {
        INfcAdapter adapter = Stub.asInterface(ServiceManager.getService("nfc"));
        if (adapter == null) {
            System.err.println("Got a null NfcAdapter, is the system running?");
            return;
        }
        try {
            if (args.length == 2 && "enable".equals(args[1])) {
                adapter.enable();
            } else if (args.length == 2 && "disable".equals(args[1])) {
                adapter.disable(true);
            } else {
                System.err.println(longHelp());
            }
        } catch (RemoteException e) {
            System.err.println("NFC operation failed: " + e);
        }
    }
}
