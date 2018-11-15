package com.android.server.rms.iaware.cpu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.InnerUtils;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CPUKeyBackground {
    private static final String GRP_BACKGROUND = "/background";
    public static final String GRP_KEY_BACKGROUND = "/key-background";
    private static final String KEY_BUNDLE_GRP = "KEY_GRP";
    private static final String KEY_BUNDLE_PID = "KEY_PID";
    private static final String KEY_BUNDLE_UID = "KEY_UID";
    private static final String TAG = "CPUKeyBackground";
    private static CPUKeyBackground sInstance;
    private AwareStateCallback mAwareStateCallback;
    private CPUFeature mCPUFeatureInstance;
    private KeyBackgroundHandler mKeyBackgroundHandler;

    private class KeyBackgroundHandler extends Handler {
        private KeyBackgroundHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 104:
                    CPUKeyBackground.this.doKBackground(104, msg.arg1);
                    break;
                case 105:
                    CPUKeyBackground.this.doKBackground(105, msg.arg1);
                    break;
                case 106:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        int pid = bundle.getInt(CPUKeyBackground.KEY_BUNDLE_PID);
                        int uid = bundle.getInt(CPUKeyBackground.KEY_BUNDLE_UID);
                        if (bundle.getInt(CPUKeyBackground.KEY_BUNDLE_GRP) == 0) {
                            CPUKeyBackground.this.checkKeyBackgroundAndSendMsg(pid, uid);
                            break;
                        }
                    }
                    AwareLog.e(CPUKeyBackground.TAG, "handleMessage inavlid params null bundle!");
                    return;
                    break;
            }
        }
    }

    private class AwareStateCallback implements IAwareStateCallback {
        private AwareStateCallback() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            if (stateType == 5) {
                NetManager.getInstance().sendMsgToNetMng(uid, eventType, 3);
            } else if (stateType == 2) {
                NetManager.getInstance().sendMsgToNetMng(uid, eventType, 4);
            }
            if (stateType == 0 && CPUFeature.isCpusetEnable()) {
                String pkgName = CPUKeyBackground.this.getAppPkgName(pid, uid);
                if (!CPUKeyBackground.this.checkIsNativeKeyBackgroupApp(pkgName)) {
                    if (CPUKeyBackground.this.isProtectPackage(pkgName)) {
                        if (1 == eventType) {
                            CPUKeyBackground.this.setProcessGroupByUid(uid);
                        }
                    } else if (1 == eventType) {
                        CPUKeyBackground.this.sendMessagesByUid(104, uid, CPUKeyBackground.GRP_BACKGROUND);
                    } else if (2 == eventType) {
                        CPUKeyBackground.this.sendMessagesByUid(105, uid, CPUKeyBackground.GRP_KEY_BACKGROUND);
                    }
                }
            }
        }
    }

    private CPUKeyBackground() {
    }

    public static synchronized CPUKeyBackground getInstance() {
        CPUKeyBackground cPUKeyBackground;
        synchronized (CPUKeyBackground.class) {
            if (sInstance == null) {
                sInstance = new CPUKeyBackground();
            }
            cPUKeyBackground = sInstance;
        }
        return cPUKeyBackground;
    }

    public void start(CPUFeature feature) {
        initHandler();
        this.mCPUFeatureInstance = feature;
        registerStateCallback();
    }

    private void registerStateCallback() {
        if (this.mAwareStateCallback == null) {
            this.mAwareStateCallback = new AwareStateCallback();
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 0);
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 5);
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 2);
        }
    }

    public void destroy() {
        unregisterStateCallback();
    }

    private void unregisterStateCallback() {
        if (this.mAwareStateCallback != null) {
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 0);
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 5);
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 2);
            this.mAwareStateCallback = null;
        }
    }

    private void initHandler() {
        if (this.mKeyBackgroundHandler == null) {
            this.mKeyBackgroundHandler = new KeyBackgroundHandler();
        }
    }

    private void checkKeyBackgroundAndSendMsg(int pid, int uid) {
        boolean isKeyBG = AwareAppKeyBackgroup.getInstance().checkIsKeyBackgroup(pid, uid);
        if (isProtectPackage(getAppPkgName(pid, uid)) && isKeyBG) {
            setProcessGroup(pid, -1);
            return;
        }
        if (isKeyBG && this.mKeyBackgroundHandler != null) {
            Message msg = this.mKeyBackgroundHandler.obtainMessage(104);
            msg.arg1 = pid;
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    public void sendSwitchGroupMessage(int pid, int messgaeId) {
        if (this.mKeyBackgroundHandler != null) {
            Message msg = this.mKeyBackgroundHandler.obtainMessage(messgaeId);
            msg.arg1 = pid;
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    private void closeBufferedReader(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeBufferedReader exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private void closeInputStreamReader(InputStreamReader isr) {
        if (isr != null) {
            try {
                isr.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeInputStreamReader exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private void closeFileInputStream(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeFileInputStream exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private void moveForkToBackground(int uid, int pid) {
        StringBuilder stringBuilder;
        if (this.mKeyBackgroundHandler != null) {
            StringBuilder strPath = new StringBuilder();
            strPath.append("/acct/uid_");
            strPath.append(uid);
            strPath.append("/pid_");
            strPath.append(pid);
            strPath.append("/cgroup.procs");
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            String readLine;
            try {
                fis = new FileInputStream(strPath.toString());
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);
                String line = "";
                while (true) {
                    readLine = br.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    int heriPid = Integer.parseInt(line.trim());
                    if (heriPid != pid) {
                        if (checkIsTargetGroup(heriPid, GRP_KEY_BACKGROUND)) {
                            Message msg = this.mKeyBackgroundHandler.obtainMessage(105);
                            msg.arg1 = heriPid;
                            this.mKeyBackgroundHandler.sendMessage(msg);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(readLine, stringBuilder.toString());
            } catch (FileNotFoundException e2) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("FileNotFoundException ");
                stringBuilder.append(e2.getMessage());
                AwareLog.e(readLine, stringBuilder.toString());
            } catch (UnsupportedEncodingException e3) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("UnsupportedEncodingException ");
                stringBuilder.append(e3.getMessage());
                AwareLog.e(readLine, stringBuilder.toString());
            } catch (IOException e4) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IOException ");
                stringBuilder.append(e4.getMessage());
                AwareLog.e(readLine, stringBuilder.toString());
            } catch (Throwable th) {
                closeBufferedReader(null);
                closeInputStreamReader(null);
                closeFileInputStream(null);
            }
            closeBufferedReader(br);
            closeInputStreamReader(isr);
            closeFileInputStream(fis);
        }
    }

    private void sendMessagesByUid(int code, int uid, String grp) {
        List<ProcessInfo> procs = getProcessesByUid(uid);
        if (!procs.isEmpty() && this.mKeyBackgroundHandler != null) {
            int procsSize = procs.size();
            for (int i = 0; i < procsSize; i++) {
                ProcessInfo info = (ProcessInfo) procs.get(i);
                if (info != null && checkIsTargetGroup(info.mPid, grp)) {
                    Message msg = this.mKeyBackgroundHandler.obtainMessage(code);
                    msg.arg1 = info.mPid;
                    this.mKeyBackgroundHandler.sendMessage(msg);
                    if (105 == code) {
                        moveForkToBackground(uid, info.mPid);
                    }
                }
            }
        }
    }

    private String getAppPkgName(int pid, int uid) {
        if (pid > 0) {
            return InnerUtils.getAwarePkgName(pid);
        }
        return InnerUtils.getPackageNameByUid(uid);
    }

    private boolean checkIsNativeKeyBackgroupApp(String pkgName) {
        if (pkgName != null && AwareDefaultConfigList.getInstance().getKeyHabitAppList().contains(pkgName)) {
            return true;
        }
        return false;
    }

    private List<ProcessInfo> getProcessesByUid(int uid) {
        List<ProcessInfo> procs = new ArrayList();
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.isEmpty()) {
            AwareLog.e(TAG, "getProcessesByUid procList is null!");
            return procs;
        }
        int procListSize = procList.size();
        for (int i = 0; i < procListSize; i++) {
            ProcessInfo info = (ProcessInfo) procList.get(i);
            if (info != null && uid == info.mUid) {
                procs.add(info);
            }
        }
        return procs;
    }

    public void notifyProcessGroupChange(int pid, int uid, int grp) {
        if (CPUFeature.isCpusetEnable() && this.mKeyBackgroundHandler != null) {
            Message msg = this.mKeyBackgroundHandler.obtainMessage(106);
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_BUNDLE_PID, pid);
            bundle.putInt(KEY_BUNDLE_UID, uid);
            bundle.putInt(KEY_BUNDLE_GRP, grp);
            msg.setData(bundle);
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    private int doKBackground(int msg, int pid) {
        if (!CPUFeature.isCpusetEnable()) {
            return 0;
        }
        if (this.mCPUFeatureInstance == null) {
            AwareLog.e(TAG, "doKBackground mCPUFeatureInstance = null!");
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(msg);
        buffer.putInt(pid);
        int resCode = this.mCPUFeatureInstance.sendPacket(buffer);
        if (resCode != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("doKBackground sendPacked failed, send error code:");
            stringBuilder.append(resCode);
            AwareLog.e(str, stringBuilder.toString());
        }
        return 0;
    }

    public boolean checkIsTargetGroup(int pid, String grp) {
        String str;
        StringBuilder stringBuilder;
        if (grp == null) {
            AwareLog.e(TAG, "checkIsTargetGroup invalid params");
            return false;
        }
        String filePath = new StringBuilder();
        filePath.append("/proc/");
        filePath.append(pid);
        filePath.append("/cpuset");
        File file = new File(filePath.toString());
        FileInputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        int temp = 0;
        try {
            inputStream = new FileInputStream(file);
            while (true) {
                int read = inputStream.read();
                temp = read;
                if (read != -1) {
                    char ch = (char) temp;
                    if (!(ch == 10 || ch == 13)) {
                        sb.append(ch);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("checkIsTargetGroup close catch IOException msg = ");
                        stringBuilder.append(e.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                    }
                }
            }
            inputStream.close();
            if (sb.length() == 0) {
                AwareLog.e(TAG, "checkIsTargetGroup read fail");
                return false;
            } else if (grp.equals(sb.toString())) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkIsTargetGroup read catch IOException msg = ");
            stringBuilder.append(e2.getMessage());
            AwareLog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkIsTargetGroup close catch IOException msg = ");
                    stringBuilder2.append(e3.getMessage());
                    AwareLog.e(str2, stringBuilder2.toString());
                }
            }
            return false;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e22) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("checkIsTargetGroup close catch IOException msg = ");
                    stringBuilder3.append(e22.getMessage());
                    AwareLog.e(TAG, stringBuilder3.toString());
                }
            }
        }
    }

    private boolean isProtectPackage(String pkgName) {
        if (pkgName == null || !pkgName.contains("com.hicloud.android.clone")) {
            return false;
        }
        return true;
    }

    private void setProcessGroupByUid(int uid) {
        List<ProcessInfo> procs = getProcessesByUid(uid);
        int procsSize = procs.size();
        for (int i = 0; i < procsSize; i++) {
            ProcessInfo info = (ProcessInfo) procs.get(i);
            if (info != null && checkIsTargetGroup(info.mPid, GRP_BACKGROUND)) {
                setProcessGroup(info.mPid, -1);
            }
        }
    }

    private void setProcessGroup(int pid, int schedGroup) {
        String str;
        StringBuilder stringBuilder;
        try {
            Process.setProcessGroup(pid, schedGroup);
        } catch (IllegalArgumentException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setProcessGroup pid ");
            stringBuilder.append(pid);
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        } catch (SecurityException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setProcessGroup pid ");
            stringBuilder.append(pid);
            stringBuilder.append(e2.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        } catch (RuntimeException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setProcessGroup pid ");
            stringBuilder.append(pid);
            stringBuilder.append(e3.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        }
    }
}
