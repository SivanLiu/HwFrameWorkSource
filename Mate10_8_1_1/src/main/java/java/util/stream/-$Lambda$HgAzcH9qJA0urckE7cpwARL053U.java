package java.util.stream;

import java.util.function.LongFunction;

final /* synthetic */ class -$Lambda$HgAzcH9qJA0urckE7cpwARL053U implements LongFunction {
    public static final /* synthetic */ -$Lambda$HgAzcH9qJA0urckE7cpwARL053U $INST$0 = new -$Lambda$HgAzcH9qJA0urckE7cpwARL053U((byte) 0);
    public static final /* synthetic */ -$Lambda$HgAzcH9qJA0urckE7cpwARL053U $INST$1 = new -$Lambda$HgAzcH9qJA0urckE7cpwARL053U((byte) 1);
    public static final /* synthetic */ -$Lambda$HgAzcH9qJA0urckE7cpwARL053U $INST$2 = new -$Lambda$HgAzcH9qJA0urckE7cpwARL053U((byte) 2);
    public static final /* synthetic */ -$Lambda$HgAzcH9qJA0urckE7cpwARL053U $INST$3 = new -$Lambda$HgAzcH9qJA0urckE7cpwARL053U((byte) 3);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(long arg0) {
        return Long.valueOf(arg0);
    }

    private final /* synthetic */ Object $m$1(long arg0) {
        return Nodes.doubleBuilder(arg0);
    }

    private final /* synthetic */ Object $m$2(long arg0) {
        return Nodes.intBuilder(arg0);
    }

    private final /* synthetic */ Object $m$3(long arg0) {
        return Nodes.longBuilder(arg0);
    }

    private /* synthetic */ -$Lambda$HgAzcH9qJA0urckE7cpwARL053U(byte b) {
        this.$id = b;
    }

    public final Object apply(long j) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(j);
            case (byte) 1:
                return $m$1(j);
            case (byte) 2:
                return $m$2(j);
            case (byte) 3:
                return $m$3(j);
            default:
                throw new AssertionError();
        }
    }
}
