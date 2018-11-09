package com.android.server.wifi.hotspot2.anqp;

import android.net.wifi.WifiSsid;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HSOsuProvidersElement extends ANQPElement {
    public static final int MAXIMUM_OSU_SSID_LENGTH = 32;
    private final WifiSsid mOsuSsid;
    private final List<OsuProviderInfo> mProviders;

    public HSOsuProvidersElement(WifiSsid osuSsid, List<OsuProviderInfo> providers) {
        super(ANQPElementType.HSOSUProviders);
        this.mOsuSsid = osuSsid;
        this.mProviders = providers;
    }

    public static HSOsuProvidersElement parse(ByteBuffer payload) throws ProtocolException {
        int ssidLength = payload.get() & Constants.BYTE_MASK;
        if (ssidLength > 32) {
            throw new ProtocolException("Invalid SSID length: " + ssidLength);
        }
        byte[] ssidBytes = new byte[ssidLength];
        payload.get(ssidBytes);
        List<OsuProviderInfo> providers = new ArrayList();
        for (int numProviders = payload.get() & Constants.BYTE_MASK; numProviders > 0; numProviders--) {
            providers.add(OsuProviderInfo.parse(payload));
        }
        return new HSOsuProvidersElement(WifiSsid.createFromByteArray(ssidBytes), providers);
    }

    public WifiSsid getOsuSsid() {
        return this.mOsuSsid;
    }

    public List<OsuProviderInfo> getProviders() {
        return Collections.unmodifiableList(this.mProviders);
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof HSOsuProvidersElement)) {
            return false;
        }
        HSOsuProvidersElement that = (HSOsuProvidersElement) thatObject;
        if (!this.mOsuSsid != null ? that.mOsuSsid == null : this.mOsuSsid.equals(that.mOsuSsid)) {
            z = false;
        } else if (this.mProviders != null) {
            z = this.mProviders.equals(that.mProviders);
        } else if (that.mProviders != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mOsuSsid, this.mProviders});
    }

    public String toString() {
        return "OSUProviders{mOsuSsid=" + this.mOsuSsid + ", mProviders=" + this.mProviders + "}";
    }
}
