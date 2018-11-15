package com.android.commands.getfeimafilelist;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

final class CfgFileListShellCommand extends ShellCommand {
    private static final String TAG = "CfgFileListShellCommand";

    CfgFileListShellCommand() {
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            System.out.println("cmd is null.");
            return -1;
        }
        String[] args = cmd.split(";");
        int i = 0;
        try {
            String str = args[0];
            if (!(str.hashCode() == 3198785 && str.equals("help"))) {
                i = -1;
            }
            if (i != 0) {
                return getFeimaFileList(args);
            }
            System.out.println("get file list from the Feima cfg path which exists the given file name by slot Id.");
            System.out.println("getFeimaFileList [file name] [type] [slotId]");
            System.out.println("[file name] like 'prop/local.prop'");
            System.out.println("[type] like 0 or 1. 0 == CUST_TYPE_CONFIG ,1 == CUST_TYPE_MEDIA. If type is null, type == 0");
            System.out.println("[slotId] like 0 or 1. If slotId is null, slotId == 0");
            return -1;
        } catch (RemoteException e) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            printStream.println(stringBuilder.toString());
        }
    }

    int getFeimaFileList(String[] args) throws RemoteException {
        ArrayList<File> cfgFileList = null;
        String type = null;
        String slotId = null;
        try {
            if (args.length >= 3) {
                type = args[1];
                slotId = args[2];
            } else if (args.length >= 2) {
                type = args[1];
            }
            if (type == null) {
                try {
                    cfgFileList = HwCfgFilePolicy.getCfgFileList(args[0], 0);
                } catch (NoClassDefFoundError e) {
                    Log.e(TAG, "class HwCfgFilePolicy not found error");
                }
            } else {
                cfgFileList = slotId == null ? HwCfgFilePolicy.getCfgFileList(args[0], Integer.parseInt(type)) : HwCfgFilePolicy.getCfgFileList(args[0], Integer.parseInt(type), Integer.parseInt(slotId));
            }
        } catch (Exception e2) {
        }
        if (cfgFileList == null) {
            System.out.println("Not found file.");
            return -1;
        }
        if (cfgFileList.size() >= 1) {
            for (int i = cfgFileList.size() - 1; i >= 0; i--) {
                System.out.println(((File) cfgFileList.get(i)).getAbsolutePath());
            }
        }
        return 0;
    }

    public void onHelp() {
    }
}
