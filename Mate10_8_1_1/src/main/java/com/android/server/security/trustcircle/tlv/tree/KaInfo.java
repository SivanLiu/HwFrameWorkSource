package com.android.server.security.trustcircle.tlv.tree;

import com.android.server.security.trustcircle.tlv.core.TLVByteArrayInvoker;
import com.android.server.security.trustcircle.tlv.core.TLVTree.TLVChildTree;
import java.util.Vector;

public class KaInfo extends TLVChildTree {
    public static final short TAG_AAD = (short) 8198;
    public static final short TAG_CT = (short) 8196;
    public static final short TAG_KA_INFO = (short) 8195;
    public static final short TAG_NONCE = (short) 8199;
    public static final short TAG_TAG = (short) 8197;
    public static final short TAG_TMP_PK = (short) 8200;
    public TLVByteArrayInvoker aad;
    public TLVByteArrayInvoker ct;
    public TLVByteArrayInvoker nonce;
    public TLVByteArrayInvoker tag;
    public TLVByteArrayInvoker tmpPk;

    public KaInfo() {
        this.ct = new TLVByteArrayInvoker(TAG_CT);
        this.tag = new TLVByteArrayInvoker(TAG_TAG);
        this.aad = new TLVByteArrayInvoker(TAG_AAD);
        this.nonce = new TLVByteArrayInvoker(TAG_NONCE);
        this.tmpPk = new TLVByteArrayInvoker(TAG_TMP_PK);
        this.mNodeList = new Vector();
        this.mNodeList.add(this.ct);
        this.mNodeList.add(this.tag);
        this.mNodeList.add(this.aad);
        this.mNodeList.add(this.nonce);
        this.mNodeList.add(this.tmpPk);
    }

    public short getTreeTag() {
        return TAG_KA_INFO;
    }
}
