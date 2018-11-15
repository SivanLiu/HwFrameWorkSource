package com.android.server.security.trustcircle.tlv.core;

import com.android.server.security.trustcircle.utils.ByteUtil;
import com.android.server.security.trustcircle.utils.LogHelper;

public class TLVByteArrayInvoker extends TLVInvoker<Byte[]> {
    public TLVByteArrayInvoker(short tag) {
        super(tag);
    }

    public TLVByteArrayInvoker(short tag, Byte[] t) {
        super(tag, t);
    }

    public <T> T byteArray2Type(Byte[] raw) {
        if (raw == null) {
            LogHelper.e(ICommand.TAG, "error_tlv in TLVByteArrayInvoker.byteArray2Type:input byte array is null");
            return null;
        }
        this.mType = raw;
        return raw;
    }

    public <T> Byte[] type2ByteArray(T t) {
        if (t == null || !(t instanceof Byte[])) {
            return new Byte[0];
        }
        if (t instanceof Byte[]) {
            return (Byte[]) t;
        }
        String str = ICommand.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("type2ByteArray: unsupported type ");
        stringBuilder.append(t.getClass().getSimpleName());
        LogHelper.e(str, stringBuilder.toString());
        return new Byte[0];
    }

    public boolean isTypeExists(int id) {
        return this.mID == id;
    }

    public boolean isTypeExists(short tag) {
        return this.mTag == tag;
    }

    protected short getTagByType(Byte[] type) {
        return this.mTag;
    }

    public String byteArray2ServerHexString() {
        return ByteUtil.byteArray2ServerHexString(this.mOriginalByteArray);
    }

    public <T> T findTypeByTag(short tag) {
        if (this.mTag == tag) {
            return this.mType;
        }
        return null;
    }

    public int getID() {
        return 0;
    }
}
