package com.huawei.zxing.oned.rss.expanded;

import com.huawei.zxing.common.BitArray;
import java.util.List;

final class BitArrayBuilder {
    private BitArrayBuilder() {
    }

    static BitArray buildBitArray(List<ExpandedPair> pairs) {
        int i;
        int charNumber = (pairs.size() << 1) - 1;
        if (((ExpandedPair) pairs.get(pairs.size() - 1)).getRightChar() == null) {
            charNumber--;
        }
        BitArray binary = new BitArray(12 * charNumber);
        int firstValue = ((ExpandedPair) pairs.get(0)).getRightChar().getValue();
        int accPos = 0;
        for (i = 11; i >= 0; i--) {
            if (((1 << i) & firstValue) != 0) {
                binary.set(accPos);
            }
            accPos++;
        }
        for (i = 1; i < pairs.size(); i++) {
            ExpandedPair currentPair = (ExpandedPair) pairs.get(i);
            int leftValue = currentPair.getLeftChar().getValue();
            int accPos2 = accPos;
            for (accPos = 11; accPos >= 0; accPos--) {
                if (((1 << accPos) & leftValue) != 0) {
                    binary.set(accPos2);
                }
                accPos2++;
            }
            if (currentPair.getRightChar() != null) {
                accPos = currentPair.getRightChar().getValue();
                int accPos3 = accPos2;
                for (accPos2 = 11; accPos2 >= 0; accPos2--) {
                    if (((1 << accPos2) & accPos) != 0) {
                        binary.set(accPos3);
                    }
                    accPos3++;
                }
                accPos = accPos3;
            } else {
                accPos = accPos2;
            }
        }
        return binary;
    }
}
