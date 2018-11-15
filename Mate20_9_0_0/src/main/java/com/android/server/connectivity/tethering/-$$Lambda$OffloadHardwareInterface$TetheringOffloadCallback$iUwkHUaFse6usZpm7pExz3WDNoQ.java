package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.NatTimeoutUpdate;
import com.android.internal.util.BitUtils;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$TetheringOffloadCallback$iUwkHUaFse6usZpm7pExz3WDNoQ implements Runnable {
    private final /* synthetic */ TetheringOffloadCallback f$0;
    private final /* synthetic */ NatTimeoutUpdate f$1;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$TetheringOffloadCallback$iUwkHUaFse6usZpm7pExz3WDNoQ(TetheringOffloadCallback tetheringOffloadCallback, NatTimeoutUpdate natTimeoutUpdate) {
        this.f$0 = tetheringOffloadCallback;
        this.f$1 = natTimeoutUpdate;
    }

    public final void run() {
        this.f$0.controlCb.onNatTimeoutUpdate(OffloadHardwareInterface.networkProtocolToOsConstant(this.f$1.proto), this.f$1.src.addr, BitUtils.uint16(this.f$1.src.port), this.f$1.dst.addr, BitUtils.uint16(this.f$1.dst.port));
    }
}
