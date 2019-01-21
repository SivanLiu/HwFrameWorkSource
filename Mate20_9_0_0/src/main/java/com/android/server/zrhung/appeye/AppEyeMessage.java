package com.android.server.zrhung.appeye;

import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class AppEyeMessage {
    public static final String KILL_MUlTI_PROCESSES = "killMultiTarget";
    public static final String KILL_PROCESS = "killTarget";
    public static final String NOTIFY_USER = "notifyUser";
    private static final String TAG = "AppEyeMessage";
    private String mCommand = "";
    private String mPackageName = "";
    private String mPid = "";
    private String mProcessName = "";
    private int mUid = 0;

    /* JADX WARNING: Missing block: B:28:0x006b, code skipped:
            if (r5.equals("pid") != false) goto L_0x0079;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int parseMsg(ArrayList<String> msgList) {
        if (msgList == null || msgList.isEmpty()) {
            Log.e(TAG, "empty mssage");
            return -1;
        }
        Iterator it = msgList.iterator();
        while (true) {
            int i = 0;
            if (it.hasNext()) {
                String msg = (String) it.next();
                String[] fields = msg.split(":");
                if (fields.length == 2) {
                    String str = fields[0];
                    switch (str.hashCode()) {
                        case 68795:
                            if (str.equals("END")) {
                                i = 6;
                                break;
                            }
                        case 110987:
                            break;
                        case 115792:
                            if (str.equals("uid")) {
                                i = 1;
                                break;
                            }
                        case 79219778:
                            if (str.equals("START")) {
                                i = 5;
                                break;
                            }
                        case 202325402:
                            if (str.equals("processName")) {
                                i = 2;
                                break;
                            }
                        case 908759025:
                            if (str.equals("packageName")) {
                                i = 3;
                                break;
                            }
                        case 950394699:
                            if (str.equals("command")) {
                                i = 4;
                                break;
                            }
                        default:
                            i = -1;
                            break;
                    }
                    switch (i) {
                        case 0:
                            this.mPid = fields[1];
                            break;
                        case 1:
                            this.mUid = parseInt(fields[1]);
                            break;
                        case 2:
                            this.mProcessName = fields[1];
                            break;
                        case 3:
                            this.mPackageName = fields[1];
                            break;
                        case 4:
                            this.mCommand = fields[1];
                            break;
                        case 5:
                        case 6:
                            break;
                        default:
                            String str2 = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unknow received message: ");
                            stringBuilder.append(msg);
                            Log.e(str2, stringBuilder.toString());
                            break;
                    }
                }
                return -1;
            }
            return 0;
        }
    }

    private int parseInt(String string) {
        int value = -1;
        if (string == null || string.length() == 0) {
            return -1;
        }
        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseInt NumberFormatException e = ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return value;
    }

    public String getCommand() {
        return this.mCommand;
    }

    public int getAppPid() {
        return parseInt(this.mPid);
    }

    public ArrayList<Integer> getAppPidList() {
        ArrayList<Integer> list = new ArrayList();
        for (String str : this.mPid.split(",")) {
            int value = parseInt(str);
            if (value >= 0) {
                list.add(Integer.valueOf(value));
            }
        }
        return list;
    }

    public int getAppUid() {
        return this.mUid;
    }

    public String getAppProcessName() {
        return this.mProcessName;
    }

    public String getAppPkgName() {
        return this.mPackageName;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mCommand = ");
        stringBuilder.append(this.mCommand);
        stringBuilder.append(" mPid = ");
        stringBuilder.append(this.mPid);
        stringBuilder.append(" mUid = ");
        stringBuilder.append(this.mUid);
        stringBuilder.append(" mProcessName = ");
        stringBuilder.append(this.mProcessName);
        stringBuilder.append(" mPackageName = ");
        stringBuilder.append(this.mPackageName);
        return stringBuilder.toString();
    }
}
