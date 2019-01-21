package com.android.internal.telephony.ims;

import android.content.Context;
import com.android.internal.telephony.ims.ImsResolver.ImsDynamicQueryManagerFactory;
import com.android.internal.telephony.ims.ImsServiceFeatureQueryManager.Listener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WamP7BPq0j01TgYE3GvUqU3b-rs implements ImsDynamicQueryManagerFactory {
    public static final /* synthetic */ -$$Lambda$WamP7BPq0j01TgYE3GvUqU3b-rs INSTANCE = new -$$Lambda$WamP7BPq0j01TgYE3GvUqU3b-rs();

    private /* synthetic */ -$$Lambda$WamP7BPq0j01TgYE3GvUqU3b-rs() {
    }

    public final ImsServiceFeatureQueryManager create(Context context, Listener listener) {
        return new ImsServiceFeatureQueryManager(context, listener);
    }
}
