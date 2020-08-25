package com.android.server.hidata.channelqoe;

public class HwChannelQoEParmStatistics {
    private static final int TPU_ARRAY_LENGTH = 10;
    public int mNetworkType = -1;
    public Rst mRst = new Rst();
    public Rtt mRtt = new Rtt();
    public Svr mSvr = new Svr();
    public Tput mTput = new Tput();
    public DRtt mdRtt = new DRtt();

    public void reset() {
        this.mRst.reset();
        this.mSvr.reset();
        this.mRtt.reset();
        this.mdRtt.reset();
        this.mTput.reset();
    }

    public static class Rst {
        public int[] mRst = new int[10];

        public Rst() {
            int i = 0;
            while (true) {
                int[] iArr = this.mRst;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }

        public void reset() {
            int i = 0;
            while (true) {
                int[] iArr = this.mRst;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    public static class Svr {
        public int[] mSvr = new int[5];

        public Svr() {
            int i = 0;
            while (true) {
                int[] iArr = this.mSvr;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }

        public void reset() {
            int i = 0;
            while (true) {
                int[] iArr = this.mSvr;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    public static class Rtt {
        public int[] mRtt = new int[10];

        public Rtt() {
            int i = 0;
            while (true) {
                int[] iArr = this.mRtt;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }

        public void reset() {
            int i = 0;
            while (true) {
                int[] iArr = this.mRtt;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    public static class DRtt {
        public int[] reserved = new int[10];

        public void reset() {
            int i = 0;
            while (true) {
                int[] iArr = this.reserved;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    public static class Tput {
        public int[] mTput = new int[10];

        public void reset() {
            int i = 0;
            while (true) {
                int[] iArr = this.mTput;
                if (i < iArr.length) {
                    iArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }
}
