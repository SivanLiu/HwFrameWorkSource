package com.huawei.systemmanager.optimize;

import com.android.internal.util.MemInfoReader;

class HwMemInfoReaderImpl implements IHwMemInfoReader {
    private static IHwMemInfoReader sInstance;
    private MemInfoReader mMemInfoReader = new MemInfoReader();

    private HwMemInfoReaderImpl() {
    }

    public static synchronized IHwMemInfoReader getIsntance() {
        IHwMemInfoReader iHwMemInfoReader;
        synchronized (HwMemInfoReaderImpl.class) {
            if (sInstance == null) {
                sInstance = new HwMemInfoReaderImpl();
            }
            iHwMemInfoReader = sInstance;
        }
        return iHwMemInfoReader;
    }

    public long getFreeSize() {
        return this.mMemInfoReader.getFreeSize();
    }

    public long getCachedSize() {
        return this.mMemInfoReader.getCachedSize();
    }

    public void readMemInfo() {
        this.mMemInfoReader.readMemInfo();
    }
}
