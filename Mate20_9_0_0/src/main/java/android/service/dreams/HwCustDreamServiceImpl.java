package android.service.dreams;

import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.service.dreams.IDreamManager.Stub;
import android.util.Slog;
import android.view.WindowManager.LayoutParams;

public class HwCustDreamServiceImpl extends HwCustDreamService {
    private static final String TAG = "HwCustDreamServiceImpl";
    private static final boolean mChargingAlbumSupported = SystemProperties.getBoolean("ro.config.ChargingAlbum", false);
    private boolean mAlbumMode = false;
    private DreamService mFather;
    private final IDreamManager mSandman;

    public HwCustDreamServiceImpl(DreamService service) {
        super(service);
        Slog.w(TAG, TAG);
        this.mSandman = Stub.asInterface(ServiceManager.getService("dreams"));
        this.mFather = service;
    }

    public boolean isChargingAlbumEnabled() {
        if (mChargingAlbumSupported) {
            return this.mAlbumMode;
        }
        return super.isChargingAlbumEnabled();
    }

    public void enableChargingAlbum() {
        if (mChargingAlbumSupported) {
            try {
                if (this.mSandman == null) {
                    this.mAlbumMode = false;
                    Slog.w(TAG, "No dream manager found");
                } else if (this.mSandman.isChargingAlbumEnabled()) {
                    this.mAlbumMode = true;
                } else {
                    this.mAlbumMode = false;
                }
            } catch (Throwable t) {
                this.mAlbumMode = false;
                Slog.w(TAG, "Crashed in isChargingAlbumEnabled()", t);
            }
        }
    }

    public void setAlbumLayoutParams(LayoutParams lp, IBinder windowToken) {
        if (mChargingAlbumSupported) {
            int i;
            String str = TAG;
            r2 = new Object[2];
            int i2 = 0;
            r2[0] = windowToken;
            r2[1] = Integer.valueOf(2102);
            Slog.v(str, String.format("Attaching window token: %s to window of type %s", r2));
            lp.type = 2102;
            lp.token = windowToken;
            lp.windowAnimations = 16974574;
            int i3 = lp.flags;
            if (this.mFather.isFullscreen()) {
                i = 1024;
            } else {
                i = 0;
            }
            int i4 = -2146893567 | i;
            if (this.mFather.isScreenBright()) {
                i2 = 128;
            }
            lp.flags = i3 | (i4 | i2);
        }
    }
}
