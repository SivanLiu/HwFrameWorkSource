package com.android.server.hidata.wavemapping.entity;

import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SpaceExpInfo$JzzV2KLKzGPchCYgN8JC_0Y1reM implements BiConsumer {
    private final /* synthetic */ SpaceExpInfo f$0;

    public /* synthetic */ -$$Lambda$SpaceExpInfo$JzzV2KLKzGPchCYgN8JC_0Y1reM(SpaceExpInfo spaceExpInfo) {
        this.f$0 = spaceExpInfo;
    }

    public final void accept(Object obj, Object obj2) {
        ((Long) this.f$0.duration_app.merge((String) obj, (Long) obj2, -$$Lambda$SpaceExpInfo$UNDzBq9YY5vB0tVYGv7BxBnAJ8Y.INSTANCE));
    }
}
