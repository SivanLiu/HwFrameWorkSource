package sun.security.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;

public class ManifestDigester {
    public static final String MF_MAIN_ATTRS = "Manifest-Main-Attributes";
    private HashMap<String, Entry> entries = new HashMap();
    private byte[] rawBytes;

    public static class Entry {
        int length;
        int lengthWithBlankLine;
        int offset;
        boolean oldStyle;
        byte[] rawBytes;

        public Entry(int offset, int length, int lengthWithBlankLine, byte[] rawBytes) {
            this.offset = offset;
            this.length = length;
            this.lengthWithBlankLine = lengthWithBlankLine;
            this.rawBytes = rawBytes;
        }

        public byte[] digest(MessageDigest md) {
            md.reset();
            if (this.oldStyle) {
                doOldStyle(md, this.rawBytes, this.offset, this.lengthWithBlankLine);
            } else {
                md.update(this.rawBytes, this.offset, this.lengthWithBlankLine);
            }
            return md.digest();
        }

        private void doOldStyle(MessageDigest md, byte[] bytes, int offset, int length) {
            int i = offset;
            int start = offset;
            int max = offset + length;
            int prev = -1;
            while (i < max) {
                if (bytes[i] == (byte) 13 && prev == 32) {
                    md.update(bytes, start, (i - start) - 1);
                    start = i;
                }
                prev = bytes[i];
                i++;
            }
            md.update(bytes, start, i - start);
        }

        public byte[] digestWorkaround(MessageDigest md) {
            md.reset();
            md.update(this.rawBytes, this.offset, this.length);
            return md.digest();
        }
    }

    static class Position {
        int endOfFirstLine;
        int endOfSection;
        int startOfNext;

        Position() {
        }
    }

    private boolean findSection(int offset, Position pos) {
        int i = offset;
        int len = this.rawBytes.length;
        int last = offset;
        boolean allBlank = true;
        pos.endOfFirstLine = -1;
        while (i < len) {
            byte b = this.rawBytes[i];
            if (b != (byte) 10) {
                if (b != (byte) 13) {
                    allBlank = false;
                    i++;
                } else {
                    if (pos.endOfFirstLine == -1) {
                        pos.endOfFirstLine = i - 1;
                    }
                    if (i < len && this.rawBytes[i + 1] == (byte) 10) {
                        i++;
                    }
                }
            }
            if (pos.endOfFirstLine == -1) {
                pos.endOfFirstLine = i - 1;
            }
            if (allBlank || i == len - 1) {
                if (i == len - 1) {
                    pos.endOfSection = i;
                } else {
                    pos.endOfSection = last;
                }
                pos.startOfNext = i + 1;
                return true;
            }
            last = i;
            allBlank = true;
            i++;
        }
        return false;
    }

    public ManifestDigester(byte[] bytes) {
        this.rawBytes = bytes;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Position pos = new Position();
        if (findSection(0, pos)) {
            this.entries.put(MF_MAIN_ATTRS, new Entry(0, pos.endOfSection + 1, pos.startOfNext, this.rawBytes));
            int start = pos.startOfNext;
            while (findSection(start, pos)) {
                int len = (pos.endOfFirstLine - start) + 1;
                int sectionLen = (pos.endOfSection - start) + 1;
                int sectionLenWithBlank = pos.startOfNext - start;
                if (len > 6 && isNameAttr(bytes, start)) {
                    StringBuilder nameBuf = new StringBuilder(sectionLen);
                    try {
                        nameBuf.append(new String(bytes, start + 6, len - 6, "UTF8"));
                        int i = start + len;
                        if (i - start < sectionLen) {
                            if (bytes[i] == (byte) 13) {
                                i += 2;
                            } else {
                                i++;
                            }
                        }
                        while (i - start < sectionLen) {
                            int i2 = i + 1;
                            if (bytes[i] != (byte) 32) {
                                break;
                            }
                            int i3;
                            i = i2;
                            while (i - start < sectionLen) {
                                i3 = i + 1;
                                if (bytes[i] == 10) {
                                    i = i3;
                                    break;
                                }
                                i = i3;
                            }
                            if (bytes[i - 1] == (byte) 10) {
                                if (bytes[i - 2] == (byte) 13) {
                                    i3 = (i - i2) - 2;
                                } else {
                                    i3 = (i - i2) - 1;
                                }
                                nameBuf.append(new String(bytes, i2, i3, "UTF8"));
                            } else {
                                return;
                            }
                        }
                        this.entries.put(nameBuf.toString(), new Entry(start, sectionLen, sectionLenWithBlank, this.rawBytes));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("UTF8 not available on platform");
                    }
                }
                start = pos.startOfNext;
            }
        }
    }

    private boolean isNameAttr(byte[] bytes, int start) {
        return (bytes[start] == (byte) 78 || bytes[start] == (byte) 110) && ((bytes[start + 1] == (byte) 97 || bytes[start + 1] == (byte) 65) && ((bytes[start + 2] == (byte) 109 || bytes[start + 2] == (byte) 77) && ((bytes[start + 3] == (byte) 101 || bytes[start + 3] == (byte) 69) && bytes[start + 4] == (byte) 58 && bytes[start + 5] == (byte) 32)));
    }

    public Entry get(String name, boolean oldStyle) {
        Entry e = (Entry) this.entries.get(name);
        if (e != null) {
            e.oldStyle = oldStyle;
        }
        return e;
    }

    public byte[] manifestDigest(MessageDigest md) {
        md.reset();
        md.update(this.rawBytes, 0, this.rawBytes.length);
        return md.digest();
    }
}
