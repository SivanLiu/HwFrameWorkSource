package android.widget.sr;

import android.util.Log;

public class SRMemoryRecorder {
    private static final int MAX_SIZE = 53477376;
    private static final String SR_TAG = "SuperResolution";
    private int mMemoryCount = 0;

    public synchronized int getMemoryCount() {
        return this.mMemoryCount;
    }

    public synchronized boolean enoughRoomForSize(int size) {
        boolean ret;
        ret = this.mMemoryCount + size < MAX_SIZE;
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enoughRoomForSize: size = ");
        stringBuilder.append(size);
        stringBuilder.append("  mem = ");
        stringBuilder.append(this.mMemoryCount);
        stringBuilder.append("MAX_SIZE = ");
        stringBuilder.append(MAX_SIZE);
        stringBuilder.append(" ret = ");
        stringBuilder.append(ret);
        Log.d(str, stringBuilder.toString());
        return ret;
    }

    public synchronized void add(int size) {
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("add: size = ");
        stringBuilder.append(size);
        stringBuilder.append(" before add mem = ");
        stringBuilder.append(this.mMemoryCount);
        Log.d(str, stringBuilder.toString());
        this.mMemoryCount += size;
    }

    public synchronized void remove(int size) {
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove: size = ");
        stringBuilder.append(size);
        stringBuilder.append(" before remove mem = ");
        stringBuilder.append(this.mMemoryCount);
        Log.d(str, stringBuilder.toString());
        this.mMemoryCount -= size;
    }
}
