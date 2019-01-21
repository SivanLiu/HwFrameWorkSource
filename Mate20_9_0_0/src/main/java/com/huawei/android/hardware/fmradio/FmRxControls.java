package com.huawei.android.hardware.fmradio;

import android.util.Log;

class FmRxControls {
    static final int FREQ_MUL = 1000;
    static final int SCAN_BACKWARD = 3;
    static final int SCAN_FORWARD = 2;
    static final int SEEK_BACKWARD = 1;
    static final int SEEK_FORWARD = 0;
    private static final String TAG = "FmRxControls";
    private static final int V4L2_CID_AUDIO_MUTE = 9963785;
    private static final int V4L2_CID_BASE = 9963776;
    private static final int V4L2_CID_PRIVATE_BASE = 134217728;
    private static final int V4L2_CID_PRIVATE_TAVARUA_EMPHASIS = 134217740;
    private static final int V4L2_CID_PRIVATE_TAVARUA_LP_MODE = 134217745;
    private static final int V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_MASK = 134217734;
    private static final int V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_PROC = 134217744;
    private static final int V4L2_CID_PRIVATE_TAVARUA_RDSON = 134217743;
    private static final int V4L2_CID_PRIVATE_TAVARUA_RDS_STD = 134217741;
    private static final int V4L2_CID_PRIVATE_TAVARUA_REGION = 134217735;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SCANDWELL = 134217730;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SIGNAL_TH = 134217736;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SPACING = 134217742;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SRCHMODE = 134217729;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SRCHON = 134217731;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_CNT = 134217739;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_PI = 134217738;
    private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY = 134217737;
    private static final int V4L2_CID_PRIVATE_TAVARUA_STATE = 134217732;
    private static final int V4L2_CID_PRIVATE_TAVARUA_TRANSMIT_MODE = 134217733;
    private static final int V4L2_CTRL_CLASS_USER = 9961472;
    private int mFreq;

    FmRxControls() {
    }

    public void fmOn(int fd, int device) {
        FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_STATE, device);
    }

    public void fmOff(int fd) {
        FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_STATE, 0);
    }

    public void muteControl(int fd, boolean on) {
        if (on) {
            FmReceiverWrapper.setControlNative(fd, V4L2_CID_AUDIO_MUTE, 3);
        } else {
            FmReceiverWrapper.setControlNative(fd, V4L2_CID_AUDIO_MUTE, 0);
        }
    }

    public int setStation(int fd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("** Tune Using: ");
        stringBuilder.append(fd);
        Log.d(str, stringBuilder.toString());
        int ret = FmReceiverWrapper.setFreqNative(fd, this.mFreq);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("** Returned: ");
        stringBuilder2.append(ret);
        Log.d(str2, stringBuilder2.toString());
        return ret;
    }

    public int getTunedFrequency(int fd) {
        int frequency = FmReceiverWrapper.getFreqNative(fd);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTunedFrequency: ");
        stringBuilder.append(frequency);
        Log.d(str, stringBuilder.toString());
        return frequency;
    }

    public int getFreq() {
        return this.mFreq;
    }

    public void setFreq(int f) {
        this.mFreq = f;
    }

    public int searchStationList(int fd, int mode, int preset_num, int dir, int pty) {
        int re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCHMODE, mode);
        if (re != 0) {
            return re;
        }
        re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_CNT, preset_num);
        if (re != 0) {
            return re;
        }
        if (pty > 0) {
            re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY, pty);
        }
        if (re != 0) {
            return re;
        }
        re = FmReceiverWrapper.startSearchNative(fd, dir);
        if (re != 0) {
            return re;
        }
        return 0;
    }

    public int[] stationList(int fd) {
        byte[] sList = new byte[100];
        float lowBand = (float) (((double) FmReceiverWrapper.getLowerBandNative(fd)) / 1000.0d);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lowBand: ");
        stringBuilder.append(lowBand);
        Log.d(str, stringBuilder.toString());
        FmReceiverWrapper.getBufferNative(fd, sList, 0);
        int station_num = sList[0];
        int[] stationList = new int[(station_num + 1)];
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("station_num: ");
        stringBuilder2.append(station_num);
        Log.d(str2, stringBuilder2.toString());
        for (int i = 0; i < station_num; i++) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" Byte1 = ");
            stringBuilder2.append(sList[(i * 2) + 1]);
            Log.d(str2, stringBuilder2.toString());
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" Byte2 = ");
            stringBuilder2.append(sList[(i * 2) + 2]);
            Log.d(str2, stringBuilder2.toString());
            int tmpFreqByte1 = sList[(i * 2) + 1] & 255;
            int tmpFreqByte2 = sList[(i * 2) + 2] & 255;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" tmpFreqByte1 = ");
            stringBuilder2.append(tmpFreqByte1);
            Log.d(str2, stringBuilder2.toString());
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" tmpFreqByte2 = ");
            stringBuilder2.append(tmpFreqByte2);
            Log.d(str2, stringBuilder2.toString());
            int freq = ((tmpFreqByte1 & 3) << 8) | tmpFreqByte2;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" freq: ");
            stringBuilder2.append(freq);
            Log.d(str2, stringBuilder2.toString());
            float real_freq = ((float) (freq * 50)) + (1000.0f * lowBand);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" real_freq: ");
            stringBuilder2.append(real_freq);
            Log.d(str2, stringBuilder2.toString());
            stationList[i] = (int) real_freq;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" stationList: ");
            stringBuilder2.append(stationList[i]);
            Log.d(str2, stringBuilder2.toString());
        }
        try {
            stationList[station_num] = 0;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, "ArrayIndexOutOfBoundsException !!");
        }
        return stationList;
    }

    public void searchStations(int fd, int mode, int dwell, int dir, int pty, int pi) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mode is ");
        stringBuilder.append(mode);
        stringBuilder.append(" Dwell is ");
        stringBuilder.append(dwell);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("dir is ");
        stringBuilder.append(dir);
        stringBuilder.append(" PTY is ");
        stringBuilder.append(pty);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("pi is ");
        stringBuilder.append(pi);
        stringBuilder.append(" id ");
        stringBuilder.append(V4L2_CID_PRIVATE_TAVARUA_SRCHMODE);
        Log.d(str, stringBuilder.toString());
        int re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCHMODE, mode);
        re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SCANDWELL, dwell);
        if (pty != 0) {
            re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY, pty);
        }
        if (pi != 0) {
            re = FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PI, pi);
        }
        re = FmReceiverWrapper.startSearchNative(fd, dir);
    }

    public int stereoControl(int fd, boolean stereo) {
        if (stereo) {
            return FmReceiverWrapper.setMonoStereoNative(fd, 1);
        }
        return FmReceiverWrapper.setMonoStereoNative(fd, 0);
    }

    public void searchRdsStations(int mode, int dwelling, int direction, int RdsSrchPty, int RdsSrchPI) {
    }

    public void cancelSearch(int fd) {
        FmReceiverWrapper.cancelSearchNative(fd);
    }

    public int setLowPwrMode(int fd, boolean lpmOn) {
        if (lpmOn) {
            return FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE, 1);
        }
        return FmReceiverWrapper.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE, 0);
    }

    public int getPwrMode(int fd) {
        return FmReceiverWrapper.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE);
    }
}
