package com.android.server;

import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.RouteInfo;

final /* synthetic */ class -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg implements NetworkManagementEventCallback {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg$1 */
    final /* synthetic */ class AnonymousClass1 implements NetworkManagementEventCallback {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(INetworkManagementEventObserver arg0) {
            arg0.addressRemoved((String) this.-$f0, (LinkAddress) this.-$f1);
        }

        private final /* synthetic */ void $m$1(INetworkManagementEventObserver arg0) {
            arg0.addressUpdated((String) this.-$f0, (LinkAddress) this.-$f1);
        }

        private final /* synthetic */ void $m$2(INetworkManagementEventObserver arg0) {
            arg0.limitReached((String) this.-$f0, (String) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(iNetworkManagementEventObserver);
                    return;
                case (byte) 1:
                    $m$1(iNetworkManagementEventObserver);
                    return;
                case (byte) 2:
                    $m$2(iNetworkManagementEventObserver);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg$2 */
    final /* synthetic */ class AnonymousClass2 implements NetworkManagementEventCallback {
        private final /* synthetic */ long -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0(INetworkManagementEventObserver arg0) {
            arg0.interfaceDnsServerInfo((String) this.-$f1, this.-$f0, (String[]) this.-$f2);
        }

        public /* synthetic */ AnonymousClass2(long j, Object obj, Object obj2) {
            this.-$f0 = j;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
            $m$0(iNetworkManagementEventObserver);
        }
    }

    /* renamed from: com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg$3 */
    final /* synthetic */ class AnonymousClass3 implements NetworkManagementEventCallback {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(INetworkManagementEventObserver arg0) {
            arg0.interfaceLinkStateChanged((String) this.-$f1, this.-$f0);
        }

        private final /* synthetic */ void $m$1(INetworkManagementEventObserver arg0) {
            arg0.interfaceStatusChanged((String) this.-$f1, this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(byte b, boolean z, Object obj) {
            this.$id = b;
            this.-$f0 = z;
            this.-$f1 = obj;
        }

        public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(iNetworkManagementEventObserver);
                    return;
                case (byte) 1:
                    $m$1(iNetworkManagementEventObserver);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.-$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg$4 */
    final /* synthetic */ class AnonymousClass4 implements NetworkManagementEventCallback {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ long -$f2;

        private final /* synthetic */ void $m$0(INetworkManagementEventObserver arg0) {
            arg0.interfaceClassDataActivityChanged(Integer.toString(this.-$f1), this.-$f0, this.-$f2);
        }

        public /* synthetic */ AnonymousClass4(boolean z, int i, long j) {
            this.-$f0 = z;
            this.-$f1 = i;
            this.-$f2 = j;
        }

        public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
            $m$0(iNetworkManagementEventObserver);
        }
    }

    private final /* synthetic */ void $m$0(INetworkManagementEventObserver arg0) {
        arg0.interfaceAdded((String) this.-$f0);
    }

    private final /* synthetic */ void $m$1(INetworkManagementEventObserver arg0) {
        arg0.interfaceRemoved((String) this.-$f0);
    }

    private final /* synthetic */ void $m$2(INetworkManagementEventObserver arg0) {
        arg0.routeUpdated((RouteInfo) this.-$f0);
    }

    private final /* synthetic */ void $m$3(INetworkManagementEventObserver arg0) {
        arg0.routeRemoved((RouteInfo) this.-$f0);
    }

    public /* synthetic */ -$Lambda$9jO-pgghrn5IhueuFzPwKVTwWXg(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(iNetworkManagementEventObserver);
                return;
            case (byte) 1:
                $m$1(iNetworkManagementEventObserver);
                return;
            case (byte) 2:
                $m$2(iNetworkManagementEventObserver);
                return;
            case (byte) 3:
                $m$3(iNetworkManagementEventObserver);
                return;
            default:
                throw new AssertionError();
        }
    }
}
