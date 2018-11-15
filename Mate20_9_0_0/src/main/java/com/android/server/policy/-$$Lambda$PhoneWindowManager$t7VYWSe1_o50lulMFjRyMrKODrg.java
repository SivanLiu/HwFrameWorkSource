package com.android.server.policy;

import android.os.Looper;
import android.view.InputChannel;
import android.view.InputEventReceiver;
import android.view.InputEventReceiver.Factory;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PhoneWindowManager$t7VYWSe1_o50lulMFjRyMrKODrg implements Factory {
    private final /* synthetic */ PhoneWindowManager f$0;

    public /* synthetic */ -$$Lambda$PhoneWindowManager$t7VYWSe1_o50lulMFjRyMrKODrg(PhoneWindowManager phoneWindowManager) {
        this.f$0 = phoneWindowManager;
    }

    public final InputEventReceiver createInputEventReceiver(InputChannel inputChannel, Looper looper) {
        return new HideNavInputEventReceiver(inputChannel, looper);
    }
}
