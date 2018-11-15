package com.android.server.wifi.hotspot2.anqp;

import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import java.nio.ByteBuffer;

public class GenericBlobElement extends ANQPElement {
    private final byte[] mData;

    public GenericBlobElement(ANQPElementType infoID, ByteBuffer payload) {
        super(infoID);
        this.mData = new byte[payload.remaining()];
        payload.get(this.mData);
    }

    public byte[] getData() {
        return this.mData;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Element ID ");
        stringBuilder.append(getID());
        stringBuilder.append(": ");
        stringBuilder.append(Utils.toHexString(this.mData));
        return stringBuilder.toString();
    }
}
