package com.huawei.nearbysdk;

import com.huawei.nearbysdk.NearbyAdapter.NAdapterGetCallback;

public interface NearbyAdapterCallback extends NAdapterGetCallback {
    void onAdapterGet(NearbyAdapter nearbyAdapter);

    void onBinderDied();
}
