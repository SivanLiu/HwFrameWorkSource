package com.android.server.hidata.channelqoe;

public class HwChannelQoEAppInfo {
    public IChannelQoECallback callback;
    public int goodTimes = 0;
    public int mNetwork;
    public int mQci;
    public int mScence;
    public int mUID;
    public boolean needTputTest = false;

    public HwChannelQoEAppInfo(int uid, int scence, int network, int qci, IChannelQoECallback icallback) {
        this.mUID = uid;
        this.mScence = scence;
        this.mNetwork = network;
        this.callback = icallback;
        this.mQci = qci;
    }

    public float getTput() {
        return HwCHQciManager.getInstance().getChQciConfig(this.mQci).mTput;
    }
}
