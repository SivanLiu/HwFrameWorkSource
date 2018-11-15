package org.bouncycastle.pqc.crypto.xmss;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import org.bouncycastle.util.Integers;

public class BDSStateMap implements Serializable {
    private final Map<Integer, BDS> bdsState = new TreeMap();

    BDSStateMap() {
    }

    BDSStateMap(BDSStateMap bDSStateMap, XMSSMTParameters xMSSMTParameters, long j, byte[] bArr, byte[] bArr2) {
        for (Integer num : bDSStateMap.bdsState.keySet()) {
            this.bdsState.put(num, bDSStateMap.bdsState.get(num));
        }
        updateState(xMSSMTParameters, j, bArr, bArr2);
    }

    BDSStateMap(XMSSMTParameters xMSSMTParameters, long j, byte[] bArr, byte[] bArr2) {
        for (long j2 = 0; j2 < j; j2++) {
            updateState(xMSSMTParameters, j2, bArr, bArr2);
        }
    }

    private void updateState(XMSSMTParameters xMSSMTParameters, long j, byte[] bArr, byte[] bArr2) {
        XMSSParameters xMSSParameters = xMSSMTParameters.getXMSSParameters();
        int height = xMSSParameters.getHeight();
        long treeIndex = XMSSUtil.getTreeIndex(j, height);
        int leafIndex = XMSSUtil.getLeafIndex(j, height);
        OTSHashAddress oTSHashAddress = (OTSHashAddress) ((Builder) new Builder().withTreeAddress(treeIndex)).withOTSAddress(leafIndex).build();
        int i = 1;
        int i2 = (1 << height) - 1;
        if (leafIndex < i2) {
            if (get(0) == null || leafIndex == 0) {
                put(0, new BDS(xMSSParameters, bArr, bArr2, oTSHashAddress));
            }
            update(0, bArr, bArr2, oTSHashAddress);
        }
        while (i < xMSSMTParameters.getLayers()) {
            int leafIndex2 = XMSSUtil.getLeafIndex(treeIndex, height);
            treeIndex = XMSSUtil.getTreeIndex(treeIndex, height);
            OTSHashAddress oTSHashAddress2 = (OTSHashAddress) ((Builder) ((Builder) new Builder().withLayerAddress(i)).withTreeAddress(treeIndex)).withOTSAddress(leafIndex2).build();
            if (leafIndex2 < i2 && XMSSUtil.isNewAuthenticationPathNeeded(j, height, i)) {
                if (get(i) == null) {
                    put(i, new BDS(xMSSMTParameters.getXMSSParameters(), bArr, bArr2, oTSHashAddress2));
                }
                update(i, bArr, bArr2, oTSHashAddress2);
            }
            i++;
        }
    }

    public BDS get(int i) {
        return (BDS) this.bdsState.get(Integers.valueOf(i));
    }

    public boolean isEmpty() {
        return this.bdsState.isEmpty();
    }

    public void put(int i, BDS bds) {
        this.bdsState.put(Integers.valueOf(i), bds);
    }

    void setXMSS(XMSSParameters xMSSParameters) {
        for (Integer num : this.bdsState.keySet()) {
            BDS bds = (BDS) this.bdsState.get(num);
            bds.setXMSS(xMSSParameters);
            bds.validate();
        }
    }

    public BDS update(int i, byte[] bArr, byte[] bArr2, OTSHashAddress oTSHashAddress) {
        return (BDS) this.bdsState.put(Integers.valueOf(i), ((BDS) this.bdsState.get(Integers.valueOf(i))).getNextState(bArr, bArr2, oTSHashAddress));
    }
}
