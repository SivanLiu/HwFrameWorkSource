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
    static final /* synthetic */ boolean -assertionsDisabled = (CollationDataReader.class.desiredAssertionStatus() ^ 1);
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
        tailoring.version = ICUBinary.readHeader(inBytes, DATA_FORMAT, IS_ACCEPTABLE);
        if (base == null || base.getUCAVersion() == tailoring.getUCAVersion()) {
            int inLength = inBytes.remaining();
            if (inLength < 8) {
                throw new ICUException("not enough bytes");
            }
            int indexesLength = inBytes.getInt();
            if (indexesLength < 2 || inLength < indexesLength * 4) {
                throw new ICUException("not enough indexes");
            }
            int length;
            int[] inIndexes = new int[20];
            inIndexes[0] = indexesLength;
            int i = 1;
            while (i < indexesLength && i < inIndexes.length) {
                inIndexes[i] = inBytes.getInt();
                i++;
            }
            for (i = indexesLength; i < inIndexes.length; i++) {
                inIndexes[i] = -1;
            }
            if (indexesLength > inIndexes.length) {
                ICUBinary.skipBytes(inBytes, (indexesLength - inIndexes.length) * 4);
            }
            if (indexesLength > 19) {
                length = inIndexes[19];
            } else if (indexesLength > 5) {
                length = inIndexes[indexesLength - 1];
            } else {
                length = 0;
            }
            if (inLength < length) {
                throw new ICUException("not enough bytes");
            }
            int[] reorderCodes;
            int reorderCodesLength;
            CollationData collationData = base == null ? null : base.data;
            length = inIndexes[6] - inIndexes[5];
            if (length < 4) {
                reorderCodes = new int[0];
                reorderCodesLength = 0;
                ICUBinary.skipBytes(inBytes, length);
            } else if (collationData == null) {
                throw new ICUException("Collation base data must not reorder scripts");
            } else {
                reorderCodesLength = length / 4;
                reorderCodes = ICUBinary.getInts(inBytes, reorderCodesLength, length & 3);
                int reorderRangesLength = 0;
                while (reorderRangesLength < reorderCodesLength && (reorderCodes[(reorderCodesLength - reorderRangesLength) - 1] & -65536) != 0) {
                    reorderRangesLength++;
                }
                if (-assertionsDisabled || reorderRangesLength < reorderCodesLength) {
                    reorderCodesLength -= reorderRangesLength;
                } else {
                    throw new AssertionError();
                }
            }
            byte[] bArr = null;
            length = inIndexes[7] - inIndexes[6];
            if (length >= 256) {
                if (reorderCodesLength == 0) {
                    throw new ICUException("Reordering table without reordering codes");
                }
                bArr = new byte[256];
                inBytes.get(bArr);
                length -= 256;
            }
            ICUBinary.skipBytes(inBytes, length);
            if (collationData == null || collationData.numericPrimary == (((long) inIndexes[1]) & 4278190080L)) {
                CollationData collationData2 = null;
                length = inIndexes[8] - inIndexes[7];
                if (length >= 8) {
                    tailoring.ensureOwnedData();
                    collationData2 = tailoring.ownedData;
                    collationData2.base = collationData;
                    collationData2.numericPrimary = ((long) inIndexes[1]) & 4278190080L;
                    Trie2_32 createFromSerialized = Trie2_32.createFromSerialized(inBytes);
                    tailoring.trie = createFromSerialized;
                    collationData2.trie = createFromSerialized;
                    int trieLength = collationData2.trie.getSerializedLength();
                    if (trieLength > length) {
                        throw new ICUException("Not enough bytes for the mappings trie");
                    }
                    length -= trieLength;
                } else if (collationData != null) {
                    tailoring.data = collationData;
                } else {
                    throw new ICUException("Missing collation data mappings");
                }
                ICUBinary.skipBytes(inBytes, length);
                ICUBinary.skipBytes(inBytes, inIndexes[9] - inIndexes[8]);
                length = inIndexes[10] - inIndexes[9];
                if (length < 8) {
                    ICUBinary.skipBytes(inBytes, length);
                } else if (collationData2 == null) {
                    throw new ICUException("Tailored ces without tailored trie");
                } else {
                    collationData2.ces = ICUBinary.getLongs(inBytes, length / 8, length & 7);
                }
                ICUBinary.skipBytes(inBytes, inIndexes[11] - inIndexes[10]);
                length = inIndexes[12] - inIndexes[11];
                if (length < 4) {
                    ICUBinary.skipBytes(inBytes, length);
                } else if (collationData2 == null) {
                    throw new ICUException("Tailored ce32s without tailored trie");
                } else {
                    collationData2.ce32s = ICUBinary.getInts(inBytes, length / 4, length & 3);
                }
                int jamoCE32sStart = inIndexes[4];
                if (jamoCE32sStart >= 0) {
                    if (collationData2 == null || collationData2.ce32s == null) {
                        throw new ICUException("JamoCE32sStart index into non-existent ce32s[]");
                    }
                    collationData2.jamoCE32s = new int[67];
                    System.arraycopy(collationData2.ce32s, jamoCE32sStart, collationData2.jamoCE32s, 0, 67);
                } else if (collationData2 != null) {
                    if (collationData != null) {
                        collationData2.jamoCE32s = collationData.jamoCE32s;
                    } else {
                        throw new ICUException("Missing Jamo CE32s for Hangul processing");
                    }
                }
                length = inIndexes[13] - inIndexes[12];
                if (length >= 4) {
                    int rootElementsLength = length / 4;
                    if (collationData2 == null) {
                        throw new ICUException("Root elements but no mappings");
                    } else if (rootElementsLength <= 4) {
                        throw new ICUException("Root elements array too short");
                    } else {
                        collationData2.rootElements = new long[rootElementsLength];
                        for (i = 0; i < rootElementsLength; i++) {
                            collationData2.rootElements[i] = ((long) inBytes.getInt()) & 4294967295L;
                        }
                        if (collationData2.rootElements[3] != 83887360) {
                            throw new ICUException("Common sec/ter weights in base data differ from the hardcoded value");
                        } else if ((collationData2.rootElements[4] >>> 24) < 69) {
                            throw new ICUException("[fixed last secondary common byte] is too low");
                        } else {
                            length &= 3;
                        }
                    }
                }
                ICUBinary.skipBytes(inBytes, length);
                length = inIndexes[14] - inIndexes[13];
                if (length < 2) {
                    ICUBinary.skipBytes(inBytes, length);
                } else if (collationData2 == null) {
                    throw new ICUException("Tailored contexts without tailored trie");
                } else {
                    collationData2.contexts = ICUBinary.getString(inBytes, length / 2, length & 1);
                }
                length = inIndexes[15] - inIndexes[14];
                if (length >= 2) {
                    if (collationData2 == null) {
                        throw new ICUException("Unsafe-backward-set but no mappings");
                    }
                    if (collationData == null) {
                        tailoring.unsafeBackwardSet = new UnicodeSet((int) UTF16.TRAIL_SURROGATE_MIN_VALUE, 57343);
                        collationData2.nfcImpl.addLcccChars(tailoring.unsafeBackwardSet);
                    } else {
                        tailoring.unsafeBackwardSet = collationData.unsafeBackwardSet.cloneAsThawed();
                    }
                    USerializedSet sset = new USerializedSet();
                    char[] unsafeData = ICUBinary.getChars(inBytes, length / 2, length & 1);
                    length = 0;
                    sset.getSet(unsafeData, 0);
                    int count = sset.countRanges();
                    int[] range = new int[2];
                    for (i = 0; i < count; i++) {
                        sset.getRange(i, range);
                        tailoring.unsafeBackwardSet.add(range[0], range[1]);
                    }
                    int c = 65536;
                    int lead = 55296;
                    while (lead < 56320) {
                        if (!tailoring.unsafeBackwardSet.containsNone(c, c + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                            tailoring.unsafeBackwardSet.add(lead);
                        }
                        lead++;
                        c += 1024;
                    }
                    tailoring.unsafeBackwardSet.freeze();
                    collationData2.unsafeBackwardSet = tailoring.unsafeBackwardSet;
                } else if (collationData2 != null) {
                    if (collationData != null) {
                        collationData2.unsafeBackwardSet = collationData.unsafeBackwardSet;
                    } else {
                        throw new ICUException("Missing unsafe-backward-set");
                    }
                }
                ICUBinary.skipBytes(inBytes, length);
                length = inIndexes[16] - inIndexes[15];
                if (collationData2 != null) {
                    collationData2.fastLatinTable = null;
                    collationData2.fastLatinTableHeader = null;
                    if (((inIndexes[1] >> 16) & 255) == 2) {
                        if (length >= 2) {
                            char header0 = inBytes.getChar();
                            int headerLength = header0 & 255;
                            collationData2.fastLatinTableHeader = new char[headerLength];
                            collationData2.fastLatinTableHeader[0] = header0;
                            for (i = 1; i < headerLength; i++) {
                                collationData2.fastLatinTableHeader[i] = inBytes.getChar();
                            }
                            collationData2.fastLatinTable = ICUBinary.getChars(inBytes, (length / 2) - headerLength, length & 1);
                            length = 0;
                            if ((header0 >> 8) != 2) {
                                throw new ICUException("Fast-Latin table version differs from version in data header");
                            }
                        } else if (collationData != null) {
                            collationData2.fastLatinTable = collationData.fastLatinTable;
                            collationData2.fastLatinTableHeader = collationData.fastLatinTableHeader;
                        }
                    }
                }
                ICUBinary.skipBytes(inBytes, length);
                length = inIndexes[17] - inIndexes[16];
                if (length >= 2) {
                    if (collationData2 == null) {
                        throw new ICUException("Script order data but no mappings");
                    }
                    int scriptsLength = length / 2;
                    CharBuffer inChars = inBytes.asCharBuffer();
                    collationData2.numScripts = inChars.get();
                    int scriptStartsLength = scriptsLength - ((collationData2.numScripts + 1) + 16);
                    if (scriptStartsLength <= 2) {
                        throw new ICUException("Script order data too short");
                    }
                    char[] cArr = new char[(collationData2.numScripts + 16)];
                    collationData2.scriptsIndex = cArr;
                    inChars.get(cArr);
                    cArr = new char[scriptStartsLength];
                    collationData2.scriptStarts = cArr;
                    inChars.get(cArr);
                    if (!(collationData2.scriptStarts[0] == '\u0000' && collationData2.scriptStarts[1] == '̀' && collationData2.scriptStarts[scriptStartsLength - 1] == '＀')) {
                        throw new ICUException("Script order data not valid");
                    }
                } else if (!(collationData2 == null || collationData == null)) {
                    collationData2.numScripts = collationData.numScripts;
                    collationData2.scriptsIndex = collationData.scriptsIndex;
                    collationData2.scriptStarts = collationData.scriptStarts;
                }
                ICUBinary.skipBytes(inBytes, length);
                length = inIndexes[18] - inIndexes[17];
                if (length >= 256) {
                    if (collationData2 == null) {
                        throw new ICUException("Data for compressible primary lead bytes but no mappings");
                    }
                    collationData2.compressibleBytes = new boolean[256];
                    for (i = 0; i < 256; i++) {
                        collationData2.compressibleBytes[i] = inBytes.get() != (byte) 0;
                    }
                    length -= 256;
                } else if (collationData2 != null) {
                    if (collationData != null) {
                        collationData2.compressibleBytes = collationData.compressibleBytes;
                    } else {
                        throw new ICUException("Missing data for compressible primary lead bytes");
                    }
                }
                ICUBinary.skipBytes(inBytes, length);
                ICUBinary.skipBytes(inBytes, inIndexes[19] - inIndexes[18]);
                CollationSettings ts = (CollationSettings) tailoring.settings.readOnly();
                int options = inIndexes[1] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                char[] fastLatinPrimaries = new char[CollationFastLatin.LATIN_LIMIT];
                int fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, ts, fastLatinPrimaries);
                if (options == ts.options && ts.variableTop != 0) {
                    if (Arrays.equals(reorderCodes, ts.reorderCodes) && fastLatinOptions == ts.fastLatinOptions && (fastLatinOptions < 0 || Arrays.equals(fastLatinPrimaries, ts.fastLatinPrimaries))) {
                        return;
                    }
                }
                CollationSettings settings = (CollationSettings) tailoring.settings.copyOnWrite();
                settings.options = options;
                settings.variableTop = tailoring.data.getLastPrimaryForGroup(settings.getMaxVariable() + 4096);
                if (settings.variableTop == 0) {
                    throw new ICUException("The maxVariable could not be mapped to a variableTop");
                }
                if (reorderCodesLength != 0) {
                    settings.aliasReordering(collationData, reorderCodes, reorderCodesLength, bArr);
                }
                settings.fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, settings, settings.fastLatinPrimaries);
                return;
            }
            throw new ICUException("Tailoring numeric primary weight differs from base data");
        }
        throw new ICUException("Tailoring UCA version differs from base data UCA version");
    }

    private CollationDataReader() {
    }
}
