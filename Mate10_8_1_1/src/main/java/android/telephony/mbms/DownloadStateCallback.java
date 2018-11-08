package android.telephony.mbms;

public class DownloadStateCallback {
    public static final int ALL_UPDATES = 0;
    public static final int PROGRESS_UPDATES = 1;
    public static final int STATE_UPDATES = 2;
    private final int mCallbackFilterFlags;

    public DownloadStateCallback() {
        this.mCallbackFilterFlags = 0;
    }

    public DownloadStateCallback(int filterFlags) {
        this.mCallbackFilterFlags = filterFlags;
    }

    public int getCallbackFilterFlags() {
        return this.mCallbackFilterFlags;
    }

    public final boolean isFilterFlagSet(int flag) {
        boolean z = true;
        if (this.mCallbackFilterFlags == 0) {
            return true;
        }
        if ((this.mCallbackFilterFlags & flag) <= 0) {
            z = false;
        }
        return z;
    }

    public void onProgressUpdated(DownloadRequest request, FileInfo fileInfo, int currentDownloadSize, int fullDownloadSize, int currentDecodedSize, int fullDecodedSize) {
    }

    public void onStateUpdated(DownloadRequest request, FileInfo fileInfo, int state) {
    }
}
