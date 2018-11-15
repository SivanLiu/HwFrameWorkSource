package org.bouncycastle.pqc.crypto.rainbow;

import org.bouncycastle.crypto.CipherParameters;

public class RainbowParameters implements CipherParameters {
    private final int[] DEFAULT_VI;
    private int[] vi;

    public RainbowParameters() {
        this.DEFAULT_VI = new int[]{6, 12, 17, 22, 33};
        this.vi = this.DEFAULT_VI;
    }

    public RainbowParameters(int[] iArr) {
        this.DEFAULT_VI = new int[]{6, 12, 17, 22, 33};
        this.vi = iArr;
        try {
            checkParams();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkParams() throws Exception {
        if (this.vi == null) {
            throw new Exception("no layers defined.");
        } else if (this.vi.length > 1) {
            int i = 0;
            while (i < this.vi.length - 1) {
                int i2 = this.vi[i];
                i++;
                if (i2 >= this.vi[i]) {
                    throw new Exception("v[i] has to be smaller than v[i+1]");
                }
            }
        } else {
            throw new Exception("Rainbow needs at least 1 layer, such that v1 < v2.");
        }
    }

    public int getDocLength() {
        return this.vi[this.vi.length - 1] - this.vi[0];
    }

    public int getNumOfLayers() {
        return this.vi.length - 1;
    }

    public int[] getVi() {
        return this.vi;
    }
}
