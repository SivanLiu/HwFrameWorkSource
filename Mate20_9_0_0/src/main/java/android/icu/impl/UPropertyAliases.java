package android.icu.impl;

import android.icu.impl.ICUBinary.Authenticate;
import android.icu.util.BytesTrie;
import android.icu.util.BytesTrie.Result;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class UPropertyAliases {
    private static final int DATA_FORMAT = 1886282093;
    public static final UPropertyAliases INSTANCE;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    private static final int IX_BYTE_TRIES_OFFSET = 1;
    private static final int IX_NAME_GROUPS_OFFSET = 2;
    private static final int IX_RESERVED3_OFFSET = 3;
    private static final int IX_VALUE_MAPS_OFFSET = 0;
    private byte[] bytesTries;
    private String nameGroups;
    private int[] valueMaps;

    private static final class IsAcceptable implements Authenticate {
        private IsAcceptable() {
        }

        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == (byte) 2;
        }
    }

    static {
        try {
            INSTANCE = new UPropertyAliases();
        } catch (IOException e) {
            MissingResourceException mre = new MissingResourceException("Could not construct UPropertyAliases. Missing pnames.icu", "", "");
            mre.initCause(e);
            throw mre;
        }
    }

    private void load(ByteBuffer bytes) throws IOException {
        ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
        int indexesLength = bytes.getInt() / 4;
        if (indexesLength >= 8) {
            int i;
            int[] inIndexes = new int[indexesLength];
            int i2 = 0;
            inIndexes[0] = indexesLength * 4;
            for (i = 1; i < indexesLength; i++) {
                inIndexes[i] = bytes.getInt();
            }
            i = inIndexes[0];
            int nextOffset = inIndexes[1];
            this.valueMaps = ICUBinary.getInts(bytes, (nextOffset - i) / 4, 0);
            i = nextOffset;
            nextOffset = inIndexes[2];
            this.bytesTries = new byte[(nextOffset - i)];
            bytes.get(this.bytesTries);
            int numBytes = inIndexes[3] - nextOffset;
            StringBuilder sb = new StringBuilder(numBytes);
            while (i2 < numBytes) {
                sb.append((char) bytes.get());
                i2++;
            }
            this.nameGroups = sb.toString();
            return;
        }
        throw new IOException("pnames.icu: not enough indexes");
    }

    private UPropertyAliases() throws IOException {
        load(ICUBinary.getRequiredData("pnames.icu"));
    }

    private int findProperty(int property) {
        int i = 1;
        int numRanges = this.valueMaps[0];
        while (numRanges > 0) {
            int start = this.valueMaps[i];
            int limit = this.valueMaps[i + 1];
            i += 2;
            if (property < start) {
                break;
            } else if (property < limit) {
                return ((property - start) * 2) + i;
            } else {
                i += (limit - start) * 2;
                numRanges--;
            }
        }
        return 0;
    }

    private int findPropertyValueNameGroup(int valueMapIndex, int value) {
        if (valueMapIndex == 0) {
            return 0;
        }
        valueMapIndex++;
        int valueMapIndex2 = valueMapIndex + 1;
        valueMapIndex = this.valueMaps[valueMapIndex];
        int valuesStart;
        int v;
        if (valueMapIndex >= 16) {
            valuesStart = valueMapIndex2;
            int nameGroupOffsetsStart = (valueMapIndex2 + valueMapIndex) - 16;
            do {
                v = this.valueMaps[valueMapIndex2];
                if (value < v) {
                    break;
                } else if (value == v) {
                    return this.valueMaps[(nameGroupOffsetsStart + valueMapIndex2) - valuesStart];
                } else {
                    valueMapIndex2++;
                }
            } while (valueMapIndex2 < nameGroupOffsetsStart);
        } else {
            while (valueMapIndex > 0) {
                v = this.valueMaps[valueMapIndex2];
                valuesStart = this.valueMaps[valueMapIndex2 + 1];
                valueMapIndex2 += 2;
                if (value < v) {
                    break;
                } else if (value < valuesStart) {
                    return this.valueMaps[(valueMapIndex2 + value) - v];
                } else {
                    valueMapIndex2 += valuesStart - v;
                    valueMapIndex--;
                }
            }
        }
        return 0;
    }

    private String getName(int numNames, int nameIndex) {
        int nameGroupsIndex = numNames + 1;
        numNames = this.nameGroups.charAt(numNames);
        if (nameIndex < 0 || numNames <= nameIndex) {
            throw new IllegalIcuArgumentException("Invalid property (value) name choice");
        }
        while (nameIndex > 0) {
            int nameGroupsIndex2;
            while (true) {
                nameGroupsIndex2 = nameGroupsIndex + 1;
                if (this.nameGroups.charAt(nameGroupsIndex) == 0) {
                    break;
                }
                nameGroupsIndex = nameGroupsIndex2;
            }
            nameIndex--;
            nameGroupsIndex = nameGroupsIndex2;
        }
        int nameGroupsIndex3 = nameGroupsIndex;
        while (this.nameGroups.charAt(nameGroupsIndex3) != 0) {
            nameGroupsIndex3++;
        }
        if (nameGroupsIndex == nameGroupsIndex3) {
            return null;
        }
        return this.nameGroups.substring(nameGroupsIndex, nameGroupsIndex3);
    }

    private static int asciiToLowercase(int c) {
        return (65 > c || c > 90) ? c : c + 32;
    }

    private boolean containsName(BytesTrie trie, CharSequence name) {
        Result result = Result.NO_VALUE;
        for (int i = 0; i < name.length(); i++) {
            int c = name.charAt(i);
            if (!(c == 45 || c == 95 || c == 32 || (9 <= c && c <= 13))) {
                if (!result.hasNext()) {
                    return false;
                }
                result = trie.next(asciiToLowercase(c));
            }
        }
        return result.hasValue();
    }

    public String getPropertyName(int property, int nameChoice) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex != 0) {
            return getName(this.valueMaps[valueMapIndex], nameChoice);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid property enum ");
        stringBuilder.append(property);
        stringBuilder.append(" (0x");
        stringBuilder.append(Integer.toHexString(property));
        stringBuilder.append(")");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String getPropertyValueName(int property, int value, int nameChoice) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex != 0) {
            int nameGroupOffset = findPropertyValueNameGroup(this.valueMaps[valueMapIndex + 1], value);
            if (nameGroupOffset != 0) {
                return getName(nameGroupOffset, nameChoice);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Property ");
            stringBuilder.append(property);
            stringBuilder.append(" (0x");
            stringBuilder.append(Integer.toHexString(property));
            stringBuilder.append(") does not have named values");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid property enum ");
        stringBuilder2.append(property);
        stringBuilder2.append(" (0x");
        stringBuilder2.append(Integer.toHexString(property));
        stringBuilder2.append(")");
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private int getPropertyOrValueEnum(int bytesTrieOffset, CharSequence alias) {
        BytesTrie trie = new BytesTrie(this.bytesTries, bytesTrieOffset);
        if (containsName(trie, alias)) {
            return trie.getValue();
        }
        return -1;
    }

    public int getPropertyEnum(CharSequence alias) {
        return getPropertyOrValueEnum(0, alias);
    }

    public int getPropertyValueEnum(int property, CharSequence alias) {
        int valueMapIndex = findProperty(property);
        StringBuilder stringBuilder;
        if (valueMapIndex != 0) {
            valueMapIndex = this.valueMaps[valueMapIndex + 1];
            if (valueMapIndex != 0) {
                return getPropertyOrValueEnum(this.valueMaps[valueMapIndex], alias);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Property ");
            stringBuilder.append(property);
            stringBuilder.append(" (0x");
            stringBuilder.append(Integer.toHexString(property));
            stringBuilder.append(") does not have named values");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid property enum ");
        stringBuilder.append(property);
        stringBuilder.append(" (0x");
        stringBuilder.append(Integer.toHexString(property));
        stringBuilder.append(")");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getPropertyValueEnumNoThrow(int property, CharSequence alias) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex == 0) {
            return -1;
        }
        valueMapIndex = this.valueMaps[valueMapIndex + 1];
        if (valueMapIndex == 0) {
            return -1;
        }
        return getPropertyOrValueEnum(this.valueMaps[valueMapIndex], alias);
    }

    public static int compare(String stra, String strb) {
        int cstra = 0;
        int istrb = 0;
        int istra = 0;
        int cstrb = 0;
        while (true) {
            if (istra < stra.length()) {
                cstra = stra.charAt(istra);
                if (!(cstra == 32 || cstra == 45 || cstra == 95)) {
                    switch (cstra) {
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                            break;
                    }
                }
                istra++;
            }
            while (istrb < strb.length()) {
                cstrb = strb.charAt(istrb);
                if (!(cstrb == 32 || cstrb == 45 || cstrb == 95)) {
                    switch (cstrb) {
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                            break;
                        default:
                            break;
                    }
                }
                istrb++;
            }
            boolean endstrb = true;
            boolean endstra = istra == stra.length();
            if (istrb != strb.length()) {
                endstrb = false;
            }
            if (endstra) {
                if (endstrb) {
                    return 0;
                }
                cstra = 0;
            } else if (endstrb) {
                cstrb = 0;
            }
            int rc = asciiToLowercase(cstra) - asciiToLowercase(cstrb);
            if (rc != 0) {
                return rc;
            }
            istra++;
            istrb++;
            continue;
        }
    }
}
