package com.android.internal.telephony.uicc;

public class IccFileNotFound extends IccException {
    IccFileNotFound() {
    }

    IccFileNotFound(String s) {
        super(s);
    }

    IccFileNotFound(int ef) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ICC EF Not Found 0x");
        stringBuilder.append(Integer.toHexString(ef));
        super(stringBuilder.toString());
    }
}
