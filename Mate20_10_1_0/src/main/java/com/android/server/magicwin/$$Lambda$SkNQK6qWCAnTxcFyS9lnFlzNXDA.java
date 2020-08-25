package com.android.server.magicwin;

import com.android.server.magicwin.HwMagicWindowConfig;
import java.util.Map;
import java.util.function.Function;

/* renamed from: com.android.server.magicwin.-$$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA implements Function {
    public static final /* synthetic */ $$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA INSTANCE = new $$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA();

    private /* synthetic */ $$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return (HwMagicWindowConfig.PackageConfig) ((Map.Entry) obj).getValue();
    }
}
