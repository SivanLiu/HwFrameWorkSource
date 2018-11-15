package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.eap.EAPMethod;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NAIRealmData {
    @VisibleForTesting
    public static final int NAI_ENCODING_UTF8_MASK = 1;
    @VisibleForTesting
    public static final String NAI_REALM_STRING_SEPARATOR = ";";
    private final List<EAPMethod> mEAPMethods;
    private final List<String> mRealms;

    @VisibleForTesting
    public NAIRealmData(List<String> realms, List<EAPMethod> eapMethods) {
        this.mRealms = realms;
        this.mEAPMethods = eapMethods;
    }

    public static NAIRealmData parse(ByteBuffer payload) throws ProtocolException {
        int length = ((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK;
        if (length <= payload.remaining()) {
            boolean z = true;
            if ((payload.get() & 1) == 0) {
                z = false;
            }
            List<String> realmList = Arrays.asList(ByteBufferReader.readStringWithByteLength(payload, z ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII).split(NAI_REALM_STRING_SEPARATOR));
            List<EAPMethod> eapMethodList = new ArrayList();
            for (int methodCount = payload.get() & Constants.BYTE_MASK; methodCount > 0; methodCount--) {
                eapMethodList.add(EAPMethod.parse(payload));
            }
            return new NAIRealmData(realmList, eapMethodList);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid data length: ");
        stringBuilder.append(length);
        throw new ProtocolException(stringBuilder.toString());
    }

    public List<String> getRealms() {
        return Collections.unmodifiableList(this.mRealms);
    }

    public List<EAPMethod> getEAPMethods() {
        return Collections.unmodifiableList(this.mEAPMethods);
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof NAIRealmData)) {
            return false;
        }
        NAIRealmData that = (NAIRealmData) thatObject;
        if (!(this.mRealms.equals(that.mRealms) && this.mEAPMethods.equals(that.mEAPMethods))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (this.mRealms.hashCode() * 31) + this.mEAPMethods.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NAIRealmElement{mRealms=");
        stringBuilder.append(this.mRealms);
        stringBuilder.append(" mEAPMethods=");
        stringBuilder.append(this.mEAPMethods);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
