package org.bouncycastle.pqc.crypto.xmss;

import java.io.Serializable;
import java.util.Stack;

class BDSTreeHash implements Serializable {
    private static final long serialVersionUID = 1;
    private boolean finished = false;
    private int height;
    private final int initialHeight;
    private boolean initialized = false;
    private int nextIndex;
    private XMSSNode tailNode;

    BDSTreeHash(int i) {
        this.initialHeight = i;
    }

    int getHeight() {
        return (!this.initialized || this.finished) ? Integer.MAX_VALUE : this.height;
    }

    int getIndexLeaf() {
        return this.nextIndex;
    }

    public XMSSNode getTailNode() {
        return this.tailNode.clone();
    }

    void initialize(int i) {
        this.tailNode = null;
        this.height = this.initialHeight;
        this.nextIndex = i;
        this.initialized = true;
        this.finished = false;
    }

    boolean isFinished() {
        return this.finished;
    }

    boolean isInitialized() {
        return this.initialized;
    }

    void setNode(XMSSNode xMSSNode) {
        this.tailNode = xMSSNode;
        this.height = xMSSNode.getHeight();
        if (this.height == this.initialHeight) {
            this.finished = true;
        }
    }

    void update(Stack<XMSSNode> stack, WOTSPlus wOTSPlus, byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        if (oTSHashAddress == null) {
            throw new NullPointerException("otsHashAddress == null");
        } else if (this.finished || !this.initialized) {
            throw new IllegalStateException("finished or not initialized");
        } else {
            oTSHashAddress = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(this.nextIndex).withChainAddress(oTSHashAddress.getChainAddress()).withHashAddress(oTSHashAddress.getHashAddress()).withKeyAndMask(oTSHashAddress.getKeyAndMask())).build();
            LTreeAddress lTreeAddress = (LTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withLTreeAddress(this.nextIndex).build();
            HashTreeAddress hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withTreeIndex(this.nextIndex).build();
            wOTSPlus.importKeys(wOTSPlus.getWOTSPlusSecretKey(bArr2, oTSHashAddress), bArr);
            XMSSNode lTree = XMSSNodeUtil.lTree(wOTSPlus, wOTSPlus.getPublicKey(oTSHashAddress), lTreeAddress);
            while (!stack.isEmpty() && ((XMSSNode) stack.peek()).getHeight() == lTree.getHeight() && ((XMSSNode) stack.peek()).getHeight() != this.initialHeight) {
                HashTreeAddress hashTreeAddress2 = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex((hashTreeAddress.getTreeIndex() - 1) / 2).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                lTree = XMSSNodeUtil.randomizeHash(wOTSPlus, (XMSSNode) stack.pop(), lTree, hashTreeAddress2);
                hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress2.getLayerAddress())).withTreeAddress(hashTreeAddress2.getTreeAddress())).withTreeHeight(hashTreeAddress2.getTreeHeight() + 1).withTreeIndex(hashTreeAddress2.getTreeIndex()).withKeyAndMask(hashTreeAddress2.getKeyAndMask())).build();
                lTree = new XMSSNode(lTree.getHeight() + 1, lTree.getValue());
            }
            if (this.tailNode == null) {
                this.tailNode = lTree;
            } else if (this.tailNode.getHeight() == lTree.getHeight()) {
                HashTreeAddress hashTreeAddress3 = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex((hashTreeAddress.getTreeIndex() - 1) / 2).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                lTree = new XMSSNode(this.tailNode.getHeight() + 1, XMSSNodeUtil.randomizeHash(wOTSPlus, this.tailNode, lTree, hashTreeAddress3).getValue());
                this.tailNode = lTree;
                hashTreeAddress3 = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress3.getLayerAddress())).withTreeAddress(hashTreeAddress3.getTreeAddress())).withTreeHeight(hashTreeAddress3.getTreeHeight() + 1).withTreeIndex(hashTreeAddress3.getTreeIndex()).withKeyAndMask(hashTreeAddress3.getKeyAndMask())).build();
            } else {
                stack.push(lTree);
            }
            if (this.tailNode.getHeight() == this.initialHeight) {
                this.finished = true;
                return;
            }
            this.height = lTree.getHeight();
            this.nextIndex++;
        }
    }
}
