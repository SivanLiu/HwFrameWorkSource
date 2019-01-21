package com.android.okhttp;

import com.android.okhttp.ConnectionSpec.Builder;

public class ConnectionSpecs {
    private ConnectionSpecs() {
    }

    public static Builder builder(boolean tls) {
        return new Builder(tls);
    }
}
