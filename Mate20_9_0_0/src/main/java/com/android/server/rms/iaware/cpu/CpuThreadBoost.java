package com.android.server.rms.iaware.cpu;

import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature.CPUFeatureHandler;
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

public class CpuThreadBoost {
    private static final String STR_SYSTEM_SERVER_PROC_NAME = "system_server";
    private static final String TAG = "CpuThreadBoost";
    private static CpuThreadBoost sInstance;
    private List<String> mBoostThreadsList = new ArrayList();
    private CPUFeatureHandler mCPUFeatureHandler;
    private CPUFeature mCPUFeatureInstance;
    private boolean mEnable = false;
    private int mMyPid = 0;

    private CpuThreadBoost() {
    }

    public static synchronized CpuThreadBoost getInstance() {
        CpuThreadBoost cpuThreadBoost;
        synchronized (CpuThreadBoost.class) {
            if (sInstance == null) {
                sInstance = new CpuThreadBoost();
            }
            cpuThreadBoost = sInstance;
        }
        return cpuThreadBoost;
    }

    public void setBoostThreadsList(String[] threadsBoostInfo) {
        if (threadsBoostInfo == null) {
            AwareLog.i(TAG, "threadBootInfo is empty");
            return;
        }
        int len = threadsBoostInfo.length;
        this.mBoostThreadsList.clear();
        int i = 0;
        while (i < len) {
            if (ThreadBoostConfig.GAP_IDENTIFIER.equals(threadsBoostInfo[i])) {
                i = obtainBoostProcInfo(threadsBoostInfo, i + 1, len);
            }
            i++;
        }
    }

    public void notifyCommChange(int pid, int tgid) {
        if (this.mEnable) {
            int isBoost = 0;
            if (tgid == this.mMyPid) {
                isBoost = 1;
            }
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putInt(CPUFeature.MSG_BINDER_THREAD_CREATE);
            buffer.putInt(tgid);
            buffer.putInt(pid);
            buffer.putInt(isBoost);
            if (this.mCPUFeatureInstance != null) {
                this.mCPUFeatureInstance.sendPacket(buffer);
            }
        }
    }

    private int obtainBoostProcInfo(String[] threadsBoostInfo, int start, int len) {
        int i = start;
        if (i >= len) {
            return i;
        }
        if (STR_SYSTEM_SERVER_PROC_NAME.equals(threadsBoostInfo[i])) {
            i++;
            while (i < len && !ThreadBoostConfig.GAP_IDENTIFIER.equals(threadsBoostInfo[i])) {
                int i2 = i + 1;
                this.mBoostThreadsList.add(threadsBoostInfo[i]);
                i = i2;
            }
        }
        return i - 1;
    }

    public void start(CPUFeature feature, CPUFeatureHandler handler) {
        this.mEnable = true;
        this.mCPUFeatureInstance = feature;
        this.mCPUFeatureHandler = handler;
        this.mMyPid = Process.myPid();
        List<String> tidStrArray = new ArrayList();
        getSystemThreads(this.mMyPid, tidStrArray);
        sendPacket(tidStrArray);
    }

    public void stop() {
        this.mEnable = false;
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

    private String getThreadName(String tidPath) {
        String commFilePath = new StringBuilder();
        commFilePath.append(tidPath);
        commFilePath.append("/comm");
        commFilePath = commFilePath.toString();
        FileInputStream input = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufReader = null;
        String tidName = null;
        try {
            input = new FileInputStream(commFilePath);
            inputStreamReader = new InputStreamReader(input, "UTF-8");
            bufReader = new BufferedReader(inputStreamReader);
            tidName = bufReader.readLine();
        } catch (FileNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception file not found, file path: ");
            stringBuilder.append(commFilePath);
            AwareLog.e(str, stringBuilder.toString());
        } catch (UnsupportedEncodingException e2) {
            AwareLog.e(TAG, "UnsupportedEncodingException ");
        } catch (IOException e3) {
            AwareLog.e(TAG, "getSystemServerThreads failed!");
        } catch (Throwable th) {
            closeBufferedReader(bufReader);
            closeInputStreamReader(inputStreamReader);
            closeFileInputStream(input);
        }
        closeBufferedReader(bufReader);
        closeInputStreamReader(inputStreamReader);
        closeFileInputStream(input);
        return tidName;
    }

    private void getSystemThreads(int pid, List<String> tidStrArray) {
        String filePath = new StringBuilder();
        filePath.append("/proc/");
        filePath.append(pid);
        filePath.append("/task/");
        File[] subFiles = new File(filePath.toString()).listFiles();
        if (subFiles != null) {
            for (File eachTidFile : subFiles) {
                String tidPath = "";
                try {
                    tidPath = eachTidFile.getCanonicalPath();
                    String tidName = getThreadName(tidPath);
                    if (tidName != null) {
                        int boostThreadsListSize = this.mBoostThreadsList.size();
                        String tidStr = null;
                        for (int i = 0; i < boostThreadsListSize; i++) {
                            if (tidName.contains((CharSequence) this.mBoostThreadsList.get(i))) {
                                tidStr = getTidStr(tidPath);
                                if (tidStr != null) {
                                    tidStrArray.add(tidStr);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    private String getTidStr(String tidPath) {
        String[] subStr = tidPath.split("task/");
        if (subStr.length == 2) {
            return subStr[1];
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTid failed, error path is ");
        stringBuilder.append(tidPath);
        AwareLog.e(str, stringBuilder.toString());
        return null;
    }

    private void sendPacket(List<String> tidStrArray) {
        int num = tidStrArray.size();
        int[] tids = new int[num];
        int i = 0;
        int i2 = 0;
        while (i2 < num) {
            try {
                tids[i2] = Integer.parseInt((String) tidStrArray.get(i2));
                i2++;
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "parseInt failed!");
                return;
            }
        }
        i2 = tids.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 * (i2 + 2));
        buffer.putInt(115);
        buffer.putInt(i2);
        while (i < i2) {
            buffer.putInt(tids[i]);
            i++;
        }
        if (this.mCPUFeatureInstance != null) {
            this.mCPUFeatureInstance.sendPacket(buffer);
        }
    }

    private void removeCpusMsg() {
        if (this.mCPUFeatureHandler != null) {
            this.mCPUFeatureHandler.removeMessages(CPUFeature.MSG_SET_BOOST_CPUS);
            this.mCPUFeatureHandler.removeMessages(CPUFeature.MSG_RESET_BOOST_CPUS);
        }
    }

    public void setBoostCpus() {
        if (this.mEnable && this.mCPUFeatureHandler != null) {
            removeCpusMsg();
            this.mCPUFeatureHandler.sendEmptyMessage(CPUFeature.MSG_SET_BOOST_CPUS);
        }
    }

    public void resetBoostCpus() {
        if (this.mEnable && this.mCPUFeatureHandler != null) {
            removeCpusMsg();
            this.mCPUFeatureHandler.sendEmptyMessage(CPUFeature.MSG_RESET_BOOST_CPUS);
        }
    }
}
