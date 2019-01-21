package org.bouncycastle.pqc.crypto.xmss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public final class BDS implements Serializable {
    private static final long serialVersionUID = 1;
    private List<XMSSNode> authenticationPath;
    private int index;
    private int k;
    private Map<Integer, XMSSNode> keep;
    private Map<Integer, LinkedList<XMSSNode>> retain;
    private XMSSNode root;
    private Stack<XMSSNode> stack;
    private final List<BDSTreeHash> treeHashInstances;
    private final int treeHeight;
    private boolean used;
    private transient WOTSPlus wotsPlus;

    private BDS(BDS bds, byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        this.wotsPlus = bds.wotsPlus;
        this.treeHeight = bds.treeHeight;
        this.k = bds.k;
        this.root = bds.root;
        this.authenticationPath = new ArrayList(bds.authenticationPath);
        this.retain = bds.retain;
        this.stack = (Stack) bds.stack.clone();
        this.treeHashInstances = bds.treeHashInstances;
        this.keep = new TreeMap(bds.keep);
        this.index = bds.index;
        nextAuthenticationPath(bArr, bArr2, oTSHashAddress);
        bds.used = true;
    }

    private BDS(WOTSPlus wOTSPlus, int i, int i2) {
        this.wotsPlus = wOTSPlus;
        this.treeHeight = i;
        this.k = i2;
        if (i2 <= i && i2 >= 2) {
            i -= i2;
            if (i % 2 == 0) {
                this.authenticationPath = new ArrayList();
                this.retain = new TreeMap();
                this.stack = new Stack();
                this.treeHashInstances = new ArrayList();
                for (i2 = 0; i2 < i; i2++) {
                    this.treeHashInstances.add(new BDSTreeHash(i2));
                }
                this.keep = new TreeMap();
                this.index = 0;
                this.used = false;
                return;
            }
        }
        throw new IllegalArgumentException("illegal value for BDS parameter k");
    }

    BDS(XMSSParameters xMSSParameters, int i) {
        this(xMSSParameters.getWOTSPlus(), xMSSParameters.getHeight(), xMSSParameters.getK());
        this.index = i;
        this.used = true;
    }

    BDS(XMSSParameters xMSSParameters, byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        this(xMSSParameters.getWOTSPlus(), xMSSParameters.getHeight(), xMSSParameters.getK());
        initialize(bArr, bArr2, oTSHashAddress);
    }

    BDS(XMSSParameters xMSSParameters, byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress, int i) {
        this(xMSSParameters.getWOTSPlus(), xMSSParameters.getHeight(), xMSSParameters.getK());
        initialize(bArr, bArr2, oTSHashAddress);
        while (this.index < i) {
            nextAuthenticationPath(bArr, bArr2, oTSHashAddress);
            this.used = false;
        }
    }

    private BDSTreeHash getBDSTreeHashInstanceForUpdate() {
        BDSTreeHash bDSTreeHash = null;
        for (BDSTreeHash bDSTreeHash2 : this.treeHashInstances) {
            if (!bDSTreeHash2.isFinished()) {
                if (bDSTreeHash2.isInitialized()) {
                    if (bDSTreeHash == null || bDSTreeHash2.getHeight() < bDSTreeHash.getHeight() || (bDSTreeHash2.getHeight() == bDSTreeHash.getHeight() && bDSTreeHash2.getIndexLeaf() < bDSTreeHash.getIndexLeaf())) {
                        bDSTreeHash = bDSTreeHash2;
                    }
                }
            }
        }
        return bDSTreeHash;
    }

    private void initialize(byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        if (oTSHashAddress != null) {
            LTreeAddress lTreeAddress = (LTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).build();
            HashTreeAddress hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).build();
            for (int i = 0; i < (1 << this.treeHeight); i++) {
                oTSHashAddress = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(i).withChainAddress(oTSHashAddress.getChainAddress()).withHashAddress(oTSHashAddress.getHashAddress()).withKeyAndMask(oTSHashAddress.getKeyAndMask())).build();
                this.wotsPlus.importKeys(this.wotsPlus.getWOTSPlusSecretKey(bArr2, oTSHashAddress), bArr);
                lTreeAddress = (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(i).withTreeHeight(lTreeAddress.getTreeHeight()).withTreeIndex(lTreeAddress.getTreeIndex()).withKeyAndMask(lTreeAddress.getKeyAndMask())).build();
                XMSSNode lTree = XMSSNodeUtil.lTree(this.wotsPlus, this.wotsPlus.getPublicKey(oTSHashAddress), lTreeAddress);
                hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeIndex(i).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                while (!this.stack.isEmpty() && ((XMSSNode) this.stack.peek()).getHeight() == lTree.getHeight()) {
                    int floor = (int) Math.floor((double) (i / (1 << lTree.getHeight())));
                    if (floor == 1) {
                        this.authenticationPath.add(lTree.clone());
                    }
                    if (floor == 3 && lTree.getHeight() < this.treeHeight - this.k) {
                        ((BDSTreeHash) this.treeHashInstances.get(lTree.getHeight())).setNode(lTree.clone());
                    }
                    if (floor >= 3 && (floor & 1) == 1 && lTree.getHeight() >= this.treeHeight - this.k && lTree.getHeight() <= this.treeHeight - 2) {
                        if (this.retain.get(Integer.valueOf(lTree.getHeight())) == null) {
                            LinkedList linkedList = new LinkedList();
                            linkedList.add(lTree.clone());
                            this.retain.put(Integer.valueOf(lTree.getHeight()), linkedList);
                        } else {
                            ((LinkedList) this.retain.get(Integer.valueOf(lTree.getHeight()))).add(lTree.clone());
                        }
                    }
                    hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex((hashTreeAddress.getTreeIndex() - 1) / 2).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                    lTree = XMSSNodeUtil.randomizeHash(this.wotsPlus, (XMSSNode) this.stack.pop(), lTree, hashTreeAddress);
                    hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight() + 1).withTreeIndex(hashTreeAddress.getTreeIndex()).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                    lTree = new XMSSNode(lTree.getHeight() + 1, lTree.getValue());
                }
                this.stack.push(lTree);
            }
            this.root = (XMSSNode) this.stack.pop();
            return;
        }
        throw new NullPointerException("otsHashAddress == null");
    }

    private void nextAuthenticationPath(byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        if (oTSHashAddress == null) {
            throw new NullPointerException("otsHashAddress == null");
        } else if (this.used) {
            throw new IllegalStateException("index already used");
        } else if (this.index <= (1 << this.treeHeight) - 2) {
            LTreeAddress lTreeAddress = (LTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).build();
            HashTreeAddress hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).build();
            int calculateTau = XMSSUtil.calculateTau(this.index, this.treeHeight);
            if (((this.index >> (calculateTau + 1)) & 1) == 0 && calculateTau < this.treeHeight - 1) {
                this.keep.put(Integer.valueOf(calculateTau), ((XMSSNode) this.authenticationPath.get(calculateTau)).clone());
            }
            int i = 0;
            if (calculateTau == 0) {
                oTSHashAddress = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(this.index).withChainAddress(oTSHashAddress.getChainAddress()).withHashAddress(oTSHashAddress.getHashAddress()).withKeyAndMask(oTSHashAddress.getKeyAndMask())).build();
                this.wotsPlus.importKeys(this.wotsPlus.getWOTSPlusSecretKey(bArr2, oTSHashAddress), bArr);
                this.authenticationPath.set(0, XMSSNodeUtil.lTree(this.wotsPlus, this.wotsPlus.getPublicKey(oTSHashAddress), (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(this.index).withTreeHeight(lTreeAddress.getTreeHeight()).withTreeIndex(lTreeAddress.getTreeIndex()).withKeyAndMask(lTreeAddress.getKeyAndMask())).build()));
            } else {
                int i2;
                int i3 = calculateTau - 1;
                XMSSNode randomizeHash = XMSSNodeUtil.randomizeHash(this.wotsPlus, (XMSSNode) this.authenticationPath.get(i3), (XMSSNode) this.keep.get(Integer.valueOf(i3)), (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(i3).withTreeIndex(this.index >> calculateTau).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build());
                this.authenticationPath.set(calculateTau, new XMSSNode(randomizeHash.getHeight() + 1, randomizeHash.getValue()));
                this.keep.remove(Integer.valueOf(i3));
                for (i2 = 0; i2 < calculateTau; i2++) {
                    List list;
                    Object tailNode;
                    if (i2 < this.treeHeight - this.k) {
                        list = this.authenticationPath;
                        tailNode = ((BDSTreeHash) this.treeHashInstances.get(i2)).getTailNode();
                    } else {
                        list = this.authenticationPath;
                        tailNode = ((LinkedList) this.retain.get(Integer.valueOf(i2))).removeFirst();
                    }
                    list.set(i2, tailNode);
                }
                i2 = Math.min(calculateTau, this.treeHeight - this.k);
                for (int i4 = 0; i4 < i2; i4++) {
                    calculateTau = (this.index + 1) + (3 * (1 << i4));
                    if (calculateTau < (1 << this.treeHeight)) {
                        ((BDSTreeHash) this.treeHashInstances.get(i4)).initialize(calculateTau);
                    }
                }
            }
            while (i < ((this.treeHeight - this.k) >> 1)) {
                BDSTreeHash bDSTreeHashInstanceForUpdate = getBDSTreeHashInstanceForUpdate();
                if (bDSTreeHashInstanceForUpdate != null) {
                    bDSTreeHashInstanceForUpdate.update(this.stack, this.wotsPlus, bArr, bArr2, oTSHashAddress);
                }
                i++;
            }
            this.index++;
        } else {
            throw new IllegalStateException("index out of bounds");
        }
    }

    protected List<XMSSNode> getAuthenticationPath() {
        ArrayList arrayList = new ArrayList();
        for (XMSSNode clone : this.authenticationPath) {
            arrayList.add(clone.clone());
        }
        return arrayList;
    }

    protected int getIndex() {
        return this.index;
    }

    public BDS getNextState(byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        return new BDS(this, bArr, bArr2, oTSHashAddress);
    }

    protected XMSSNode getRoot() {
        return this.root.clone();
    }

    protected int getTreeHeight() {
        return this.treeHeight;
    }

    boolean isUsed() {
        return this.used;
    }

    protected void setXMSS(XMSSParameters xMSSParameters) {
        if (this.treeHeight == xMSSParameters.getHeight()) {
            this.wotsPlus = xMSSParameters.getWOTSPlus();
            return;
        }
        throw new IllegalStateException("wrong height");
    }

    protected void validate() {
        if (this.authenticationPath == null) {
            throw new IllegalStateException("authenticationPath == null");
        } else if (this.retain == null) {
            throw new IllegalStateException("retain == null");
        } else if (this.stack == null) {
            throw new IllegalStateException("stack == null");
        } else if (this.treeHashInstances == null) {
            throw new IllegalStateException("treeHashInstances == null");
        } else if (this.keep == null) {
            throw new IllegalStateException("keep == null");
        } else if (!XMSSUtil.isIndexValid(this.treeHeight, (long) this.index)) {
            throw new IllegalStateException("index in BDS state out of bounds");
        }
    }
}
