package com.android.server.security.trustcircle.tlv.command.ka;

import com.android.server.security.trustcircle.tlv.core.TLVByteArrayInvoker;
import com.android.server.security.trustcircle.tlv.core.TLVTree.TLVRootTree;
import java.util.Vector;

public class RET_KA extends TLVRootTree {
    public static final int ID = -2147483615;
    public static final short TAG_IV = (short) 8449;
    public static final short TAG_PAYLOAD = (short) 8448;
    public static final short TAG_RET_KA = (short) 33;
    public TLVByteArrayInvoker iv;
    public TLVByteArrayInvoker payload;

    public RET_KA() {
        this.payload = new TLVByteArrayInvoker(TAG_PAYLOAD);
        this.iv = new TLVByteArrayInvoker(TAG_IV);
        this.mNodeList = new Vector();
        this.mNodeList.add(this.payload);
        this.mNodeList.add(this.iv);
    }

    public int getCmdID() {
        return ID;
    }

    public short getTreeTag() {
        return (short) 33;
    }
}
