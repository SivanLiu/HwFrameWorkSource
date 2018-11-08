package android.os;

import android.os.BatteryStats.IntToString;

final /* synthetic */ class -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE implements IntToString {
    public static final /* synthetic */ -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE $INST$0 = new -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE((byte) 0);
    public static final /* synthetic */ -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE $INST$1 = new -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE((byte) 1);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ String $m$0(int arg0) {
        return UserHandle.formatUid(arg0);
    }

    private final /* synthetic */ String $m$1(int arg0) {
        return Integer.toString(arg0);
    }

    private /* synthetic */ -$Lambda$-dncxFEc2F2bgG2fsIoC6FC6WNE(byte b) {
        this.$id = b;
    }

    public final String applyAsString(int i) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(i);
            case (byte) 1:
                return $m$1(i);
            default:
                throw new AssertionError();
        }
    }
}
