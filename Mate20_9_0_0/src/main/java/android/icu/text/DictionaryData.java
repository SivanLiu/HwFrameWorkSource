package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.nio.ByteBuffer;

final class DictionaryData {
    private static final int DATA_FORMAT_ID = 1147757428;
    public static final int IX_COUNT = 8;
    public static final int IX_RESERVED1_OFFSET = 1;
    public static final int IX_RESERVED2_OFFSET = 2;
    public static final int IX_RESERVED6 = 6;
    public static final int IX_RESERVED7 = 7;
    public static final int IX_STRING_TRIE_OFFSET = 0;
    public static final int IX_TOTAL_SIZE = 3;
    public static final int IX_TRANSFORM = 5;
    public static final int IX_TRIE_TYPE = 4;
    public static final int TRANSFORM_NONE = 0;
    public static final int TRANSFORM_OFFSET_MASK = 2097151;
    public static final int TRANSFORM_TYPE_MASK = 2130706432;
    public static final int TRANSFORM_TYPE_OFFSET = 16777216;
    public static final int TRIE_HAS_VALUES = 8;
    public static final int TRIE_TYPE_BYTES = 0;
    public static final int TRIE_TYPE_MASK = 7;
    public static final int TRIE_TYPE_UCHARS = 1;

    private DictionaryData() {
    }

    public static DictionaryMatcher loadDictionaryFor(String dictType) throws IOException {
        int i;
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dictionaries/");
        stringBuilder.append(dictType);
        String dictFileName = rb.getStringWithFallback(stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("brkitr/");
        stringBuilder2.append(dictFileName);
        ByteBuffer bytes = ICUBinary.getRequiredData(stringBuilder2.toString());
        ICUBinary.readHeader(bytes, DATA_FORMAT_ID, null);
        int[] indexes = new int[8];
        boolean z = false;
        for (i = 0; i < 8; i++) {
            indexes[i] = bytes.getInt();
        }
        int offset = indexes[0];
        Assert.assrt(offset >= 32);
        if (offset > 32) {
            ICUBinary.skipBytes(bytes, offset - 32);
        }
        i = indexes[4] & 7;
        int totalSize = indexes[3] - offset;
        if (i == 0) {
            int transform = indexes[5];
            byte[] data = new byte[totalSize];
            bytes.get(data);
            return new BytesDictionaryMatcher(data, transform);
        } else if (i != 1) {
            return null;
        } else {
            if (totalSize % 2 == 0) {
                z = true;
            }
            Assert.assrt(z);
            return new CharsDictionaryMatcher(ICUBinary.getString(bytes, totalSize / 2, totalSize & 1));
        }
    }
}
