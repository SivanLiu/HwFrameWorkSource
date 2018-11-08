package com.android.server.policy;

final /* synthetic */ class -$Lambda$pV_TcBBXJOcgD8CpVRVZuDc_ff8 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((GlobalActions) this.-$f0).-com_android_server_policy_GlobalActions-mthref-0();
    }

    private final /* synthetic */ void $m$1() {
        ((PhoneWindowManager) this.-$f0).lambda$-com_android_server_policy_PhoneWindowManager_65488();
    }

    private final /* synthetic */ void $m$2() {
        ((PhoneWindowManager) this.-$f0).lambda$-com_android_server_policy_PhoneWindowManager_65756();
    }

    public /* synthetic */ -$Lambda$pV_TcBBXJOcgD8CpVRVZuDc_ff8(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            case (byte) 2:
                $m$2();
                return;
            default:
                throw new AssertionError();
        }
    }
}
