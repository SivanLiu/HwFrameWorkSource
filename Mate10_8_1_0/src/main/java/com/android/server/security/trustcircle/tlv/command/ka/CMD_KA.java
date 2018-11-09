package com.android.server.security.trustcircle.tlv.command.ka;

import com.android.server.security.trustcircle.tlv.core.TLVByteArrayInvoker;
import com.android.server.security.trustcircle.tlv.core.TLVNumberInvoker.TLVIntegerInvoker;
import com.android.server.security.trustcircle.tlv.core.TLVNumberInvoker.TLVLongInvoker;
import com.android.server.security.trustcircle.tlv.core.TLVTree.TLVRootTree;
import com.android.server.security.trustcircle.tlv.core.TLVTreeInvoker.TLVChildTreeInvoker;
import java.util.Vector;

public class CMD_KA extends TLVRootTree {
    public static final int ID = -2147483616;
    public static final int TAG_CMD_KA = 32;
    public static final short TAG_EE_AES_TMP_KEY = (short) 8194;
    public static final short TAG_KA_VERSION = (short) 8192;
    public static final short TAG_USER_ID = (short) 8193;
    public TLVByteArrayInvoker eeAesTmpKey;
    public TLVChildTreeInvoker kaInfo;
    public TLVIntegerInvoker kaVersion;
    public TLVLongInvoker userId;

    public CMD_KA() {
        this.kaVersion = new TLVIntegerInvoker(TAG_KA_VERSION);
        this.userId = new TLVLongInvoker(TAG_USER_ID);
        this.eeAesTmpKey = new TLVByteArrayInvoker(TAG_EE_AES_TMP_KEY);
        this.kaInfo = new TLVChildTreeInvoker(8195);
        this.mNodeList = new Vector();
        this.mNodeList.add(this.kaVersion);
        this.mNodeList.add(this.userId);
        this.mNodeList.add(this.eeAesTmpKey);
        this.mNodeList.add(this.kaInfo);
    }

    public int getCmdID() {
        return ID;
    }

    public short getTreeTag() {
        return (short) 32;
    }
}
