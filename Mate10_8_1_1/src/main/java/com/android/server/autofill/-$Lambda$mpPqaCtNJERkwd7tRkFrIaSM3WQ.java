package com.android.server.autofill;

import com.android.internal.os.IResultReceiver;

final /* synthetic */ class -$Lambda$mpPqaCtNJERkwd7tRkFrIaSM3WQ implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((AutofillManagerServiceShellCommand) this.-$f1).lambda$-com_android_server_autofill_AutofillManagerServiceShellCommand_6061(this.-$f0, (IResultReceiver) this.-$f2);
    }

    private final /* synthetic */ void $m$1() {
        ((AutofillManagerServiceShellCommand) this.-$f1).lambda$-com_android_server_autofill_AutofillManagerServiceShellCommand_6868(this.-$f0, (IResultReceiver) this.-$f2);
    }

    public /* synthetic */ -$Lambda$mpPqaCtNJERkwd7tRkFrIaSM3WQ(byte b, int i, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
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
