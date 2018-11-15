package org.bouncycastle.pqc.crypto.gmss;

import org.bouncycastle.util.Arrays;

public class GMSSParameters {
    private int[] K;
    private int[] heightOfTrees;
    private int numOfLayers;
    private int[] winternitzParameter;

    public GMSSParameters(int i) throws IllegalArgumentException {
        if (i <= 10) {
            int[] iArr = new int[]{10};
            init(iArr.length, iArr, new int[]{3}, new int[]{2});
        } else if (i <= 20) {
            int[] iArr2 = new int[]{10, 10};
            init(iArr2.length, iArr2, new int[]{5, 4}, new int[]{2, 2});
        } else {
            int[] iArr3 = new int[]{10, 10, 10, 10};
            init(iArr3.length, iArr3, new int[]{9, 9, 9, 3}, new int[]{2, 2, 2, 2});
        }
    }

    public GMSSParameters(int i, int[] iArr, int[] iArr2, int[] iArr3) throws IllegalArgumentException {
        init(i, iArr, iArr2, iArr3);
    }

    private void init(int i, int[] iArr, int[] iArr2, int[] iArr3) throws IllegalArgumentException {
        Object obj;
        String str = "";
        this.numOfLayers = i;
        if (this.numOfLayers == iArr2.length && this.numOfLayers == iArr.length && this.numOfLayers == iArr3.length) {
            obj = 1;
        } else {
            str = "Unexpected parameterset format";
            obj = null;
        }
        String str2 = str;
        Object obj2 = obj;
        i = 0;
        while (i < this.numOfLayers) {
            if (iArr3[i] < 2 || (iArr[i] - iArr3[i]) % 2 != 0) {
                str2 = "Wrong parameter K (K >= 2 and H-K even required)!";
                obj2 = null;
            }
            if (iArr[i] < 4 || iArr2[i] < 2) {
                str2 = "Wrong parameter H or w (H > 3 and w > 1 required)!";
                obj2 = null;
            }
            i++;
        }
        if (obj2 != null) {
            this.heightOfTrees = Arrays.clone(iArr);
            this.winternitzParameter = Arrays.clone(iArr2);
            this.K = Arrays.clone(iArr3);
            return;
        }
        throw new IllegalArgumentException(str2);
    }

    public int[] getHeightOfTrees() {
        return Arrays.clone(this.heightOfTrees);
    }

    public int[] getK() {
        return Arrays.clone(this.K);
    }

    public int getNumOfLayers() {
        return this.numOfLayers;
    }

    public int[] getWinternitzParameter() {
        return Arrays.clone(this.winternitzParameter);
    }
}
