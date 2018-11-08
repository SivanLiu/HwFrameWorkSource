package org.ifaa.android.manager;

import android.content.Context;

public abstract class IFAAManagerV2 extends IFAAManager {
    public static final int IFAA_AUTH_FINGERPRINT = 1;

    public abstract byte[] processCmdV2(Context context, byte[] bArr);
}
