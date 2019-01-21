package android.icu.impl.coll;

import android.icu.impl.ICUBinary;
import android.icu.impl.ICUBinary.Authenticate;
import android.icu.impl.Trie2_32;
import android.icu.impl.USerializedSet;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUException;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

final class CollationDataReader {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int DATA_FORMAT = 1430482796;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    static final int IX_CE32S_OFFSET = 11;
    static final int IX_CES_OFFSET = 9;
    static final int IX_COMPRESSIBLE_BYTES_OFFSET = 17;
    static final int IX_CONTEXTS_OFFSET = 13;
    static final int IX_FAST_LATIN_TABLE_OFFSET = 15;
    static final int IX_INDEXES_LENGTH = 0;
    static final int IX_JAMO_CE32S_START = 4;
    static final int IX_OPTIONS = 1;
    static final int IX_REORDER_CODES_OFFSET = 5;
    static final int IX_REORDER_TABLE_OFFSET = 6;
    static final int IX_RESERVED10_OFFSET = 10;
    static final int IX_RESERVED18_OFFSET = 18;
    static final int IX_RESERVED2 = 2;
    static final int IX_RESERVED3 = 3;
    static final int IX_RESERVED8_OFFSET = 8;
    static final int IX_ROOT_ELEMENTS_OFFSET = 12;
    static final int IX_SCRIPTS_OFFSET = 16;
    static final int IX_TOTAL_SIZE = 19;
    static final int IX_TRIE_OFFSET = 7;
    static final int IX_UNSAFE_BWD_OFFSET = 14;

    private static final class IsAcceptable implements Authenticate {
        private IsAcceptable() {
        }

        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == (byte) 5;
        }
    }

    static void read(CollationTailoring base, ByteBuffer inBytes, CollationTailoring tailoring) throws IOException {
        CollationTailoring collationTailoring = base;
        ByteBuffer byteBuffer = inBytes;
        CollationTailoring collationTailoring2 = tailoring;
        collationTailoring2.version = ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
        if (collationTailoring == null || base.getUCAVersion() == tailoring.getUCAVersion()) {
            int inLength = inBytes.remaining();
            if (inLength >= 8) {
                int indexesLength = inBytes.getInt();
                int i;
                if (indexesLength < 2 || inLength < indexesLength * 4) {
                    i = indexesLength;
                    throw new ICUException("not enough indexes");
                }
                int[] inIndexes = new int[20];
                inIndexes[0] = indexesLength;
                int i2 = 1;
                while (i2 < indexesLength && i2 < inIndexes.length) {
                    inIndexes[i2] = inBytes.getInt();
                    i2++;
                }
                for (i2 = indexesLength; i2 < inIndexes.length; i2++) {
                    inIndexes[i2] = -1;
                }
                if (indexesLength > inIndexes.length) {
                    ICUBinary.skipBytes(byteBuffer, (indexesLength - inIndexes.length) * 4);
                }
                if (indexesLength > 19) {
                    i2 = inIndexes[19];
                } else if (indexesLength > 5) {
                    i2 = inIndexes[indexesLength - 1];
                } else {
                    i2 = 0;
                }
                if (inLength >= i2) {
                    int[] reorderCodes;
                    int reorderCodesLength;
                    int i3;
                    int i4;
                    CollationData baseData = collationTailoring == null ? null : collationTailoring.data;
                    i2 = inIndexes[5 + 1] - inIndexes[5];
                    if (i2 < 4) {
                        reorderCodes = new int[0];
                        reorderCodesLength = 0;
                        ICUBinary.skipBytes(byteBuffer, i2);
                    } else if (baseData != null) {
                        int reorderRangesLength;
                        reorderCodesLength = i2 / 4;
                        reorderCodes = ICUBinary.getInts(byteBuffer, reorderCodesLength, i2 & 3);
                        int reorderRangesLength2 = 0;
                        while (true) {
                            reorderRangesLength = reorderRangesLength2;
                            if (reorderRangesLength >= reorderCodesLength || (reorderCodes[(reorderCodesLength - reorderRangesLength) - 1] & -65536) == 0) {
                                reorderCodesLength -= reorderRangesLength;
                            } else {
                                reorderRangesLength2 = reorderRangesLength + 1;
                            }
                        }
                        reorderCodesLength -= reorderRangesLength;
                    } else {
                        throw new ICUException("Collation base data must not reorder scripts");
                    }
                    byte[] reorderTable = null;
                    i2 = inIndexes[6 + 1] - inIndexes[6];
                    if (i2 >= 256) {
                        if (reorderCodesLength != 0) {
                            reorderTable = new byte[256];
                            byteBuffer.get(reorderTable);
                            i2 -= 256;
                        } else {
                            throw new ICUException("Reordering table without reordering codes");
                        }
                    }
                    ICUBinary.skipBytes(byteBuffer, i2);
                    if (baseData != null) {
                        if (baseData.numericPrimary != (((long) inIndexes[1]) & 4278190080L)) {
                            throw new ICUException("Tailoring numeric primary weight differs from base data");
                        }
                    }
                    int i5 = i2;
                    CollationData data = null;
                    int length = inIndexes[7 + 1] - inIndexes[7];
                    if (length >= 8) {
                        tailoring.ensureOwnedData();
                        data = collationTailoring2.ownedData;
                        data.base = baseData;
                        data.numericPrimary = ((long) inIndexes[1]) & 4278190080L;
                        Trie2_32 createFromSerialized = Trie2_32.createFromSerialized(inBytes);
                        collationTailoring2.trie = createFromSerialized;
                        data.trie = createFromSerialized;
                        i2 = data.trie.getSerializedLength();
                        if (i2 <= length) {
                            length -= i2;
                        } else {
                            throw new ICUException("Not enough bytes for the mappings trie");
                        }
                    } else if (baseData != null) {
                        collationTailoring2.data = baseData;
                    } else {
                        throw new ICUException("Missing collation data mappings");
                    }
                    ICUBinary.skipBytes(byteBuffer, length);
                    ICUBinary.skipBytes(byteBuffer, inIndexes[8 + 1] - inIndexes[8]);
                    length = inIndexes[9 + 1] - inIndexes[9];
                    if (length < 8) {
                        ICUBinary.skipBytes(byteBuffer, length);
                    } else if (data != null) {
                        data.ces = ICUBinary.getLongs(byteBuffer, length / 8, length & 7);
                    } else {
                        throw new ICUException("Tailored ces without tailored trie");
                    }
                    ICUBinary.skipBytes(byteBuffer, inIndexes[10 + 1] - inIndexes[10]);
                    length = inIndexes[11 + 1] - inIndexes[11];
                    if (length < 4) {
                        ICUBinary.skipBytes(byteBuffer, length);
                    } else if (data != null) {
                        data.ce32s = ICUBinary.getInts(byteBuffer, length / 4, length & 3);
                    } else {
                        throw new ICUException("Tailored ce32s without tailored trie");
                    }
                    int jamoCE32sStart = inIndexes[4];
                    if (jamoCE32sStart < 0) {
                        if (data != null) {
                            if (baseData != null) {
                                data.jamoCE32s = baseData.jamoCE32s;
                            } else {
                                i3 = jamoCE32sStart;
                                throw new ICUException("Missing Jamo CE32s for Hangul processing");
                            }
                        }
                    } else if (data == null || data.ce32s == null) {
                        throw new ICUException("JamoCE32sStart index into non-existent ce32s[]");
                    } else {
                        data.jamoCE32s = new int[67];
                        System.arraycopy(data.ce32s, jamoCE32sStart, data.jamoCE32s, 0, 67);
                    }
                    indexesLength = inIndexes[12 + 1] - inIndexes[12];
                    if (indexesLength >= 4) {
                        length = indexesLength / 4;
                        if (data == null) {
                            throw new ICUException("Root elements but no mappings");
                        } else if (length > 4) {
                            data.rootElements = new long[length];
                            i4 = 0;
                            while (i4 < length) {
                                i3 = jamoCE32sStart;
                                data.rootElements[i4] = ((long) inBytes.getInt()) & 4294967295L;
                                i4++;
                                jamoCE32sStart = i3;
                            }
                            if (data.rootElements[3] != 83887360) {
                                throw new ICUException("Common sec/ter weights in base data differ from the hardcoded value");
                            } else if ((data.rootElements[4] >>> 24) >= 69) {
                                indexesLength &= 3;
                            } else {
                                throw new ICUException("[fixed last secondary common byte] is too low");
                            }
                        } else {
                            throw new ICUException("Root elements array too short");
                        }
                    }
                    ICUBinary.skipBytes(byteBuffer, indexesLength);
                    i4 = inIndexes[13 + 1] - inIndexes[13];
                    if (i4 < 2) {
                        ICUBinary.skipBytes(byteBuffer, i4);
                    } else if (data != null) {
                        data.contexts = ICUBinary.getString(byteBuffer, i4 / 2, i4 & 1);
                    } else {
                        throw new ICUException("Tailored contexts without tailored trie");
                    }
                    int index = 14;
                    inLength = inIndexes[14];
                    indexesLength = inIndexes[14 + 1] - inLength;
                    int index2;
                    int i6;
                    if (indexesLength < 2) {
                        index2 = 14;
                        i6 = inLength;
                        if (data != null) {
                            if (baseData != null) {
                                data.unsafeBackwardSet = baseData.unsafeBackwardSet;
                            } else {
                                throw new ICUException("Missing unsafe-backward-set");
                            }
                        }
                    } else if (data != null) {
                        int length2;
                        if (baseData == null) {
                            collationTailoring2.unsafeBackwardSet = new UnicodeSet((int) UTF16.TRAIL_SURROGATE_MIN_VALUE, 57343);
                            data.nfcImpl.addLcccChars(collationTailoring2.unsafeBackwardSet);
                        } else {
                            collationTailoring2.unsafeBackwardSet = baseData.unsafeBackwardSet.cloneAsThawed();
                        }
                        USerializedSet sset = new USerializedSet();
                        indexesLength = 0;
                        sset.getSet(ICUBinary.getChars(byteBuffer, indexesLength / 2, indexesLength & 1), 0);
                        jamoCE32sStart = sset.countRanges();
                        int[] range = new int[2];
                        int i7 = 0;
                        while (i7 < jamoCE32sStart) {
                            sset.getRange(i7, range);
                            index2 = index;
                            i6 = inLength;
                            length2 = indexesLength;
                            collationTailoring2.unsafeBackwardSet.add(range[0], range[1]);
                            i7++;
                            index = index2;
                            inLength = i6;
                            indexesLength = length2;
                        }
                        i6 = inLength;
                        length2 = indexesLength;
                        index = 65536;
                        inLength = 55296;
                        while (inLength < UTF16.TRAIL_SURROGATE_MIN_VALUE) {
                            if (!collationTailoring2.unsafeBackwardSet.containsNone(index, index + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                                collationTailoring2.unsafeBackwardSet.add(inLength);
                            }
                            inLength++;
                            index += 1024;
                        }
                        collationTailoring2.unsafeBackwardSet.freeze();
                        data.unsafeBackwardSet = collationTailoring2.unsafeBackwardSet;
                        indexesLength = length2;
                    } else {
                        index2 = 14;
                        i6 = inLength;
                        throw new ICUException("Unsafe-backward-set but no mappings");
                    }
                    ICUBinary.skipBytes(byteBuffer, indexesLength);
                    i4 = inIndexes[15 + 1] - inIndexes[15];
                    if (data != null) {
                        data.fastLatinTable = null;
                        data.fastLatinTableHeader = null;
                        if (((inIndexes[1] >> 16) & 255) == 2) {
                            if (i4 >= 2) {
                                char header0 = inBytes.getChar();
                                length = header0 & 255;
                                data.fastLatinTableHeader = new char[length];
                                data.fastLatinTableHeader[0] = header0;
                                for (i2 = 1; i2 < length; i2++) {
                                    data.fastLatinTableHeader[i2] = inBytes.getChar();
                                }
                                data.fastLatinTable = ICUBinary.getChars(byteBuffer, (i4 / 2) - length, i4 & 1);
                                i4 = 0;
                                if ((header0 >> 8) != 2) {
                                    throw new ICUException("Fast-Latin table version differs from version in data header");
                                }
                            } else if (baseData != null) {
                                data.fastLatinTable = baseData.fastLatinTable;
                                data.fastLatinTableHeader = baseData.fastLatinTableHeader;
                            }
                        }
                    }
                    ICUBinary.skipBytes(byteBuffer, i4);
                    indexesLength = inIndexes[16 + 1] - inIndexes[16];
                    if (indexesLength >= 2) {
                        if (data != null) {
                            i4 = indexesLength / 2;
                            CharBuffer inChars = inBytes.asCharBuffer();
                            data.numScripts = inChars.get();
                            i2 = i4 - ((data.numScripts + 1) + 16);
                            if (i2 > 2) {
                                char[] cArr = new char[(data.numScripts + 16)];
                                data.scriptsIndex = cArr;
                                inChars.get(cArr);
                                cArr = new char[i2];
                                data.scriptStarts = cArr;
                                inChars.get(cArr);
                                if (!(data.scriptStarts[0] == 0 && data.scriptStarts[1] == 768 && data.scriptStarts[i2 - 1] == 65280)) {
                                    throw new ICUException("Script order data not valid");
                                }
                            }
                            throw new ICUException("Script order data too short");
                        }
                        throw new ICUException("Script order data but no mappings");
                    } else if (!(data == null || baseData == null)) {
                        data.numScripts = baseData.numScripts;
                        data.scriptsIndex = baseData.scriptsIndex;
                        data.scriptStarts = baseData.scriptStarts;
                    }
                    ICUBinary.skipBytes(byteBuffer, indexesLength);
                    i4 = inIndexes[17 + 1] - inIndexes[17];
                    if (i4 >= 256) {
                        if (data != null) {
                            data.compressibleBytes = new boolean[256];
                            for (length = 0; length < 256; length++) {
                                data.compressibleBytes[length] = inBytes.get() != (byte) 0;
                            }
                            i4 -= 256;
                        } else {
                            throw new ICUException("Data for compressible primary lead bytes but no mappings");
                        }
                    } else if (data != null) {
                        if (baseData != null) {
                            data.compressibleBytes = baseData.compressibleBytes;
                        } else {
                            throw new ICUException("Missing data for compressible primary lead bytes");
                        }
                    }
                    ICUBinary.skipBytes(byteBuffer, i4);
                    inLength = inIndexes[18];
                    ICUBinary.skipBytes(byteBuffer, inIndexes[18 + 1] - inLength);
                    CollationSettings offset = (CollationSettings) collationTailoring2.settings.readOnly();
                    length = inIndexes[1] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    i2 = new char[CollationFastLatin.LATIN_LIMIT];
                    jamoCE32sStart = CollationFastLatin.getOptions(collationTailoring2.data, offset, i2);
                    int index3;
                    if (length == offset.options) {
                        index3 = 18;
                        if (offset.variableTop != 0 && Arrays.equals(reorderCodes, offset.reorderCodes) && jamoCE32sStart == offset.fastLatinOptions && (jamoCE32sStart < 0 || Arrays.equals(i2, offset.fastLatinPrimaries))) {
                            return;
                        }
                    }
                    index3 = 18;
                    CollationSettings index4 = (CollationSettings) collationTailoring2.settings.copyOnWrite();
                    index4.options = length;
                    int offset2 = inLength;
                    index4.variableTop = collationTailoring2.data.getLastPrimaryForGroup(4096 + index4.getMaxVariable());
                    if (index4.variableTop != 0) {
                        if (reorderCodesLength != 0) {
                            index4.aliasReordering(baseData, reorderCodes, reorderCodesLength, reorderTable);
                        }
                        index4.fastLatinOptions = CollationFastLatin.getOptions(collationTailoring2.data, index4, index4.fastLatinPrimaries);
                        return;
                    }
                    throw new ICUException("The maxVariable could not be mapped to a variableTop");
                }
                i = indexesLength;
                throw new ICUException("not enough bytes");
            }
            throw new ICUException("not enough bytes");
        }
        throw new ICUException("Tailoring UCA version differs from base data UCA version");
    }

    private CollationDataReader() {
    }
}
