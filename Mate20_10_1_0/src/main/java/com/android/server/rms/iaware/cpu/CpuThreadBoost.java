package com.android.server.rms.iaware.cpu;

import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature;
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
    private static final Object SLOCK = new Object();
    private static final String STR_SYSTEM_SERVER_PROC_NAME = "system_server";
    private static final String TAG = "CpuThreadBoost";
    private static CpuThreadBoost sInstance;
    private List<String> mBoostThreadsList = new ArrayList();
    private CPUFeature.CPUFeatureHandler mCPUFeatureHandler;
    private CPUFeature mCPUFeatureInstance;
    private boolean mEnable = false;
    private int mMyPid = 0;

    private CpuThreadBoost() {
    }

    public static CpuThreadBoost getInstance() {
        CpuThreadBoost cpuThreadBoost;
        synchronized (SLOCK) {
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
        int index = 0;
        while (index < len) {
            if (ThreadBoostConfig.GAP_IDENTIFIER.equals(threadsBoostInfo[index])) {
                index = obtainBoostProcInfo(threadsBoostInfo, index + 1, len);
            }
            index++;
        }
    }

    public void notifyCommChange(int pid, int tgid) {
        if (this.mEnable) {
            int isBoost = 0;
            if (tgid == this.mMyPid) {
                isBoost = 1;
            }
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putInt(151);
            buffer.putInt(tgid);
            buffer.putInt(pid);
            buffer.putInt(isBoost);
            CPUFeature cPUFeature = this.mCPUFeatureInstance;
            if (cPUFeature != null) {
                cPUFeature.sendPacket(buffer);
            }
        }
    }

    private int obtainBoostProcInfo(String[] threadsBoostInfo, int start, int len) {
        int index = start;
        if (index >= len) {
            return index;
        }
        if (STR_SYSTEM_SERVER_PROC_NAME.equals(threadsBoostInfo[index])) {
            index++;
            while (index < len && !ThreadBoostConfig.GAP_IDENTIFIER.equals(threadsBoostInfo[index])) {
                this.mBoostThreadsList.add(threadsBoostInfo[index]);
                index++;
            }
        }
        return index - 1;
    }

    public void start(CPUFeature feature, CPUFeature.CPUFeatureHandler handler) {
        this.mEnable = true;
        this.mCPUFeatureInstance = feature;
        this.mCPUFeatureHandler = handler;
        this.mMyPid = Process.myPid();
        List<String> tidStrArray = new ArrayList<>();
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
                AwareLog.e(TAG, "closeBufferedReader exception " + e.getMessage());
            }
        }
    }

    private void closeInputStreamReader(InputStreamReader isr) {
        if (isr != null) {
            try {
                isr.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeInputStreamReader exception " + e.getMessage());
            }
        }
    }

    private void closeFileInputStream(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeFileInputStream exception " + e.getMessage());
            }
        }
    }

    private String getThreadName(String tidPath) {
        String commFilePath = tidPath + "/comm";
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
            AwareLog.e(TAG, "exception file not found, file path: " + commFilePath);
        } catch (UnsupportedEncodingException e2) {
            AwareLog.e(TAG, "UnsupportedEncodingException ");
        } catch (IOException e3) {
            AwareLog.e(TAG, "getSystemServerThreads failed!");
        } catch (Throwable th) {
            closeBufferedReader(bufReader);
            closeInputStreamReader(inputStreamReader);
            closeFileInputStream(input);
            throw th;
        }
        closeBufferedReader(bufReader);
        closeInputStreamReader(inputStreamReader);
        closeFileInputStream(input);
        return tidName;
    }

    private void getSystemThreads(int pid, List<String> tidStrArray) {
        String tidStr;
        File[] subFiles = new File("/proc/" + pid + "/task/").listFiles();
        if (subFiles != null) {
            for (File eachTidFile : subFiles) {
                try {
                    String tidPath = eachTidFile.getCanonicalPath();
                    String tidName = getThreadName(tidPath);
                    if (tidName != null) {
                        int boostThreadsListSize = this.mBoostThreadsList.size();
                        for (int i = 0; i < boostThreadsListSize; i++) {
                            if (tidName.contains(this.mBoostThreadsList.get(i)) && (tidStr = getTidStr(tidPath)) != null) {
                                tidStrArray.add(tidStr);
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
        AwareLog.e(TAG, "getTid failed, error path is " + tidPath);
        return null;
    }

    private void sendPacket(List<String> tidStrArray) {
        int num = tidStrArray.size();
        int[] tids = new int[num];
        int i = 0;
        while (i < num) {
            try {
                tids[i] = Integer.parseInt(tidStrArray.get(i));
                i++;
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "parseInt failed!");
                return;
            }
        }
        int len = tids.length;
        ByteBuffer buffer = ByteBuffer.allocate((len + 2) * 4);
        buffer.putInt(115);
        buffer.putInt(len);
        for (int i2 : tids) {
            buffer.putInt(i2);
        }
        CPUFeature cPUFeature = this.mCPUFeatureInstance;
        if (cPUFeature != null) {
            cPUFeature.sendPacket(buffer);
        }
    }

    private void removeCpusMsg() {
        CPUFeature.CPUFeatureHandler cPUFeatureHandler = this.mCPUFeatureHandler;
        if (cPUFeatureHandler != null) {
            cPUFeatureHandler.removeMessages(CPUFeature.MSG_SET_BOOST_CPUS);
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
