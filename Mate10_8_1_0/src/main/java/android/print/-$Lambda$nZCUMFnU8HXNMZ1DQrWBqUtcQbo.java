package android.print;

import java.util.function.IntConsumer;

final /* synthetic */ class -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo implements IntConsumer {
    public static final /* synthetic */ -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo $INST$0 = new -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo((byte) 0);
    public static final /* synthetic */ -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo $INST$1 = new -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo((byte) 1);
    public static final /* synthetic */ -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo $INST$2 = new -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo((byte) 2);
    public static final /* synthetic */ -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo $INST$3 = new -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo((byte) 3);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(int arg0) {
        PrintAttributes.enforceValidColorMode(arg0);
    }

    private final /* synthetic */ void $m$1(int arg0) {
        PrintAttributes.enforceValidDuplexMode(arg0);
    }

    private final /* synthetic */ void $m$2(int arg0) {
        PrintAttributes.enforceValidColorMode(arg0);
    }

    private final /* synthetic */ void $m$3(int arg0) {
        PrintAttributes.enforceValidDuplexMode(arg0);
    }

    private /* synthetic */ -$Lambda$nZCUMFnU8HXNMZ1DQrWBqUtcQbo(byte b) {
        this.$id = b;
    }

    public final void accept(int i) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(i);
                return;
            case (byte) 1:
                $m$1(i);
                return;
            case (byte) 2:
                $m$2(i);
                return;
            case (byte) 3:
                $m$3(i);
                return;
            default:
                throw new AssertionError();
        }
    }
}
