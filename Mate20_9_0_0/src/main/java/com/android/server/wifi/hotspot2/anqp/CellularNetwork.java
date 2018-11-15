package com.android.server.wifi.hotspot2.anqp;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CellularNetwork {
    @VisibleForTesting
    public static final int IEI_CONTENT_LENGTH_MASK = 127;
    @VisibleForTesting
    public static final int IEI_TYPE_PLMN_LIST = 0;
    private static final int MNC_2DIGIT_VALUE = 15;
    @VisibleForTesting
    public static final int PLMN_DATA_BYTES = 3;
    private static final String TAG = "CellularNetwork";
    private final List<String> mPlmnList;

    @VisibleForTesting
    public CellularNetwork(List<String> plmnList) {
        this.mPlmnList = plmnList;
    }

    public static CellularNetwork parse(ByteBuffer payload) throws ProtocolException {
        int ieiType = payload.get() & Constants.BYTE_MASK;
        int ieiSize = payload.get() & 127;
        if (ieiType != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignore unsupported IEI Type: ");
            stringBuilder.append(ieiType);
            Log.e(str, stringBuilder.toString());
            payload.position(payload.position() + ieiSize);
            return null;
        }
        int plmnCount = payload.get() & Constants.BYTE_MASK;
        if (ieiSize == (plmnCount * 3) + 1) {
            List<String> plmnList = new ArrayList();
            while (plmnCount > 0) {
                plmnList.add(parsePlmn(payload));
                plmnCount--;
            }
            return new CellularNetwork(plmnList);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("IEI size and PLMN count mismatched: IEI Size=");
        stringBuilder2.append(ieiSize);
        stringBuilder2.append(" PLMN Count=");
        stringBuilder2.append(plmnCount);
        throw new ProtocolException(stringBuilder2.toString());
    }

    public List<String> getPlmns() {
        return Collections.unmodifiableList(this.mPlmnList);
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof CellularNetwork)) {
            return false;
        }
        return this.mPlmnList.equals(((CellularNetwork) thatObject).mPlmnList);
    }

    public int hashCode() {
        return this.mPlmnList.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CellularNetwork{mPlmnList=");
        stringBuilder.append(this.mPlmnList);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private static String parsePlmn(ByteBuffer payload) {
        byte[] plmn = new byte[3];
        payload.get(plmn);
        int mcc = (((plmn[0] << 8) & 3840) | (plmn[0] & 240)) | (plmn[1] & 15);
        int mnc = ((plmn[2] << 4) & 240) | ((plmn[2] >> 4) & 15);
        if (((plmn[1] >> 4) & 15) != 15) {
            return String.format("%03x%03x", new Object[]{Integer.valueOf(mcc), Integer.valueOf((mnc << 4) | ((plmn[1] >> 4) & 15))});
        }
        return String.format("%03x%02x", new Object[]{Integer.valueOf(mcc), Integer.valueOf(mnc)});
    }
}
