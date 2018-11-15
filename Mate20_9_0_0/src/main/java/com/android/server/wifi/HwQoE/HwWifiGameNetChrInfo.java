package com.android.server.wifi.HwQoE;

import android.util.Log;

public class HwWifiGameNetChrInfo {
    private static final String TAG = "HiDATA_GameNetChrInfo";
    public boolean mAP24gBTCoexist;
    public boolean mAP5gOnly;
    public short mArpRttGeneralDuration;
    public short mArpRttPoorDuration;
    public short mArpRttSmoothDuration;
    public short mBTScan24GCounter;
    public short mNetworkGeneralAvgRtt;
    public short mNetworkGeneralDuration;
    public short mNetworkPoorAvgRtt;
    public short mNetworkPoorDuration;
    public short mNetworkSmoothAvgRtt;
    public short mNetworkSmoothDuration;
    public short mTcpRttBadDuration;
    public short mTcpRttGeneralDuration;
    public short mTcpRttPoorDuration;
    public short mTcpRttSmoothDuration;
    public short mWifiDisCounter;
    public short mWifiRoamingCounter;
    public short mWifiScanCounter;

    public void clean() {
        this.mArpRttSmoothDuration = (short) 0;
        this.mArpRttGeneralDuration = (short) 0;
        this.mArpRttPoorDuration = (short) 0;
        this.mNetworkSmoothDuration = (short) 0;
        this.mNetworkGeneralDuration = (short) 0;
        this.mNetworkPoorDuration = (short) 0;
        this.mTcpRttSmoothDuration = (short) 0;
        this.mTcpRttGeneralDuration = (short) 0;
        this.mTcpRttPoorDuration = (short) 0;
        this.mTcpRttBadDuration = (short) 0;
        this.mWifiRoamingCounter = (short) 0;
        this.mWifiScanCounter = (short) 0;
        this.mWifiDisCounter = (short) 0;
        this.mBTScan24GCounter = (short) 0;
        this.mAP24gBTCoexist = false;
        this.mAP5gOnly = false;
        this.mNetworkSmoothAvgRtt = (short) 0;
        this.mNetworkGeneralAvgRtt = (short) 0;
        this.mNetworkPoorAvgRtt = (short) 0;
    }

    public void setNetworkSmoothAvgRtt(long mNetworkSmoothAvgRtt) {
        if (mNetworkSmoothAvgRtt >= 32767) {
            this.mNetworkSmoothAvgRtt = Short.MAX_VALUE;
        } else {
            this.mNetworkSmoothAvgRtt = (short) ((int) mNetworkSmoothAvgRtt);
        }
    }

    public void setNetworkGeneralAvgRtt(long mNetworkGeneralAvgRtt) {
        if (mNetworkGeneralAvgRtt >= 32767) {
            this.mNetworkGeneralAvgRtt = Short.MAX_VALUE;
        } else {
            this.mNetworkGeneralAvgRtt = (short) ((int) mNetworkGeneralAvgRtt);
        }
    }

    public void setNetworkPoorAvgRtt(long mNetworkPoorAvgRtt) {
        if (mNetworkPoorAvgRtt >= 32767) {
            this.mNetworkPoorAvgRtt = Short.MAX_VALUE;
        } else {
            this.mNetworkPoorAvgRtt = (short) ((int) mNetworkPoorAvgRtt);
        }
    }

    public void setArpRttSmoothDuration(long mArpRttSmoothDuration) {
        if (mArpRttSmoothDuration >= 32767) {
            this.mArpRttSmoothDuration = Short.MAX_VALUE;
        } else {
            this.mArpRttSmoothDuration = (short) ((int) mArpRttSmoothDuration);
        }
    }

    public void setArpRttGeneralDuration(long mArpRttGeneralDuration) {
        if (mArpRttGeneralDuration >= 32767) {
            this.mArpRttGeneralDuration = Short.MAX_VALUE;
        } else {
            this.mArpRttGeneralDuration = (short) ((int) mArpRttGeneralDuration);
        }
    }

    public void setArpRttPoorDuration(long mArpRttPoorDuration) {
        if (mArpRttPoorDuration >= 32767) {
            this.mArpRttPoorDuration = Short.MAX_VALUE;
        } else {
            this.mArpRttPoorDuration = (short) ((int) mArpRttPoorDuration);
        }
    }

    public void setNetworkSmoothDuration(long mNetworkSmoothDuration) {
        if (mNetworkSmoothDuration >= 32767) {
            this.mNetworkSmoothDuration = Short.MAX_VALUE;
        } else {
            this.mNetworkSmoothDuration = (short) ((int) mNetworkSmoothDuration);
        }
    }

    public void setNetworkGeneralDuration(long mNetworkGeneralDuration) {
        if (mNetworkGeneralDuration >= 32767) {
            this.mNetworkGeneralDuration = Short.MAX_VALUE;
        } else {
            this.mNetworkGeneralDuration = (short) ((int) mNetworkGeneralDuration);
        }
    }

    public void setNetworkPoorDuration(long mNetworkPoorDuration) {
        if (mNetworkPoorDuration >= 32767) {
            this.mNetworkPoorDuration = Short.MAX_VALUE;
        } else {
            this.mNetworkPoorDuration = (short) ((int) mNetworkPoorDuration);
        }
    }

    public void setTcpRttSmoothDuration(long mTcpRttSmoothDuration) {
        if (mTcpRttSmoothDuration >= 32767) {
            this.mTcpRttSmoothDuration = Short.MAX_VALUE;
        } else {
            this.mTcpRttSmoothDuration = (short) ((int) mTcpRttSmoothDuration);
        }
    }

    public void setTcpRttGeneralDuration(long mTcpRttGeneralDuration) {
        if (mTcpRttGeneralDuration >= 32767) {
            this.mTcpRttGeneralDuration = Short.MAX_VALUE;
        } else {
            this.mTcpRttGeneralDuration = (short) ((int) mTcpRttGeneralDuration);
        }
    }

    public void setTcpRttPoorDuration(long mTcpRttPoorDuration) {
        if (mTcpRttPoorDuration >= 32767) {
            this.mTcpRttPoorDuration = Short.MAX_VALUE;
        } else {
            this.mTcpRttPoorDuration = (short) ((int) mTcpRttPoorDuration);
        }
    }

    public void setTcpRttBadDuration(long mTcpRttBadDuration) {
        if (mTcpRttBadDuration >= 32767) {
            this.mTcpRttBadDuration = Short.MAX_VALUE;
        } else {
            this.mTcpRttBadDuration = (short) ((int) mTcpRttBadDuration);
        }
    }

    public void setWifiRoamingCounter(short mWifiRoamingCounter) {
        this.mWifiRoamingCounter = mWifiRoamingCounter;
    }

    public void setWifiScanCounter(short mWifiScanCounter) {
        this.mWifiScanCounter = mWifiScanCounter;
    }

    public void setWifiDisCounter(short mWifiDisCounter) {
        this.mWifiDisCounter = mWifiDisCounter;
    }

    public void setBTScan24GCounter(short mBTScan24GCounter) {
        this.mBTScan24GCounter = mBTScan24GCounter;
    }

    public void setAP5gOnly(boolean mAP5gOnly) {
        this.mAP5gOnly = mAP5gOnly;
    }

    public void setAP24gBTCoexist(boolean mAP24gBTCoexist) {
        this.mAP24gBTCoexist = mAP24gBTCoexist;
    }

    public void chrInfoDump() {
        StringBuffer buffer = new StringBuffer("Game CHR Info : ");
        buffer.append("mAP24gBTCoexist: ");
        buffer.append(this.mAP24gBTCoexist);
        buffer.append("mAP5gOnly: ");
        buffer.append(this.mAP5gOnly);
        buffer.append(", GameRttSmoothDuration: ");
        buffer.append(this.mNetworkSmoothDuration);
        buffer.append(" s, GameRttGeneralDuration: ");
        buffer.append(this.mNetworkGeneralDuration);
        buffer.append(" s, GameRttPoorDuration: ");
        buffer.append(this.mNetworkPoorDuration);
        buffer.append(" s, ArpRttSmoothDuration: ");
        buffer.append(this.mArpRttSmoothDuration);
        buffer.append(" s, ArpRttGeneralDuration: ");
        buffer.append(this.mArpRttGeneralDuration);
        buffer.append(" s, ArpRttPoorDuration: ");
        buffer.append(this.mArpRttPoorDuration);
        buffer.append(" s, TcpRttSmoothDuration: ");
        buffer.append(this.mTcpRttSmoothDuration);
        buffer.append(" s, TcpRttGeneralDuration: ");
        buffer.append(this.mTcpRttGeneralDuration);
        buffer.append(" s, TcpRttPoorDuration: ");
        buffer.append(this.mTcpRttPoorDuration);
        buffer.append(" s, TcpRttBadDuration: ");
        buffer.append(this.mTcpRttBadDuration);
        buffer.append(" s, WifiDisCounter: ");
        buffer.append(this.mWifiDisCounter);
        buffer.append(" , WifiScanCounter: ");
        buffer.append(this.mWifiScanCounter);
        buffer.append(" , WifiRoamingCounter: ");
        buffer.append(this.mWifiRoamingCounter);
        buffer.append(", GameRttSmoothAvgRtt: ");
        buffer.append(this.mNetworkSmoothAvgRtt);
        buffer.append(" ms, GameRttGeneralAvgRtt: ");
        buffer.append(this.mNetworkGeneralAvgRtt);
        buffer.append(" ms, GameRttPoorAvgRtt: ");
        buffer.append(this.mNetworkPoorAvgRtt);
        logD(buffer.toString());
    }

    private void logD(String info) {
        Log.d(TAG, info);
    }
}
