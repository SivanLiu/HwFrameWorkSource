package com.android.server.magicwin;

import com.android.server.magicwin.HwMagicWindowConfig;
import java.util.function.BiConsumer;

/* renamed from: com.android.server.magicwin.-$$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk implements BiConsumer {
    public static final /* synthetic */ $$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk INSTANCE = new $$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk();

    private /* synthetic */ $$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((HwMagicWindowConfig.OpenCapAppConfig) obj2).updateRationAndBound();
    }
}
