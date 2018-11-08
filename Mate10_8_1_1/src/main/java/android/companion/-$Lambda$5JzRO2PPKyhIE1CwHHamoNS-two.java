package android.companion;

import android.app.PendingIntent;

final /* synthetic */ class -$Lambda$5JzRO2PPKyhIE1CwHHamoNS-two implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((CallbackProxy) this.-$f0).lambda$-android_companion_CompanionDeviceManager$CallbackProxy_11398((CharSequence) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((CallbackProxy) this.-$f0).lambda$-android_companion_CompanionDeviceManager$CallbackProxy_11025((PendingIntent) this.-$f1);
    }

    public /* synthetic */ -$Lambda$5JzRO2PPKyhIE1CwHHamoNS-two(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            default:
                throw new AssertionError();
        }
    }
}
