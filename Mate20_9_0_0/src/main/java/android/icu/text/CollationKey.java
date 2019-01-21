package android.icu.text;

public final class CollationKey implements Comparable<CollationKey> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int MERGE_SEPERATOR_ = 2;
    private int m_hashCode_;
    private byte[] m_key_;
    private int m_length_;
    private String m_source_;

    public static final class BoundMode {
        @Deprecated
        public static final int COUNT = 3;
        public static final int LOWER = 0;
        public static final int UPPER = 1;
        public static final int UPPER_LONG = 2;

        private BoundMode() {
        }
    }

    public CollationKey(String source, byte[] key) {
        this(source, key, -1);
    }

    private CollationKey(String source, byte[] key, int length) {
        this.m_source_ = source;
        this.m_key_ = key;
        this.m_hashCode_ = 0;
        this.m_length_ = length;
    }

    public CollationKey(String source, RawCollationKey key) {
        this.m_source_ = source;
        this.m_length_ = key.size - 1;
        this.m_key_ = key.releaseBytes();
        this.m_hashCode_ = 0;
    }

    public String getSourceString() {
        return this.m_source_;
    }

    public byte[] toByteArray() {
        int length = getLength() + 1;
        byte[] result = new byte[length];
        System.arraycopy(this.m_key_, 0, result, 0, length);
        return result;
    }

    public int compareTo(CollationKey target) {
        int i = 0;
        while (true) {
            int l = this.m_key_[i] & 255;
            int r = target.m_key_[i] & 255;
            if (l < r) {
                return -1;
            }
            if (l > r) {
                return 1;
            }
            if (l == 0) {
                return 0;
            }
            i++;
        }
    }

    public boolean equals(Object target) {
        if (target instanceof CollationKey) {
            return equals((CollationKey) target);
        }
        return false;
    }

    public boolean equals(CollationKey target) {
        if (this == target) {
            return true;
        }
        if (target == null) {
            return false;
        }
        CollationKey other = target;
        for (int i = 0; this.m_key_[i] == other.m_key_[i]; i++) {
            if (this.m_key_[i] == (byte) 0) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        if (this.m_hashCode_ == 0) {
            if (this.m_key_ == null) {
                this.m_hashCode_ = 1;
            } else {
                StringBuilder key = new StringBuilder(this.m_key_.length >> 1);
                int i = 0;
                while (this.m_key_[i] != (byte) 0 && this.m_key_[i + 1] != (byte) 0) {
                    key.append((char) ((this.m_key_[i] << 8) | (255 & this.m_key_[i + 1])));
                    i += 2;
                }
                if (this.m_key_[i] != (byte) 0) {
                    key.append((char) (this.m_key_[i] << 8));
                }
                this.m_hashCode_ = key.toString().hashCode();
            }
        }
        return this.m_hashCode_;
    }

    public CollationKey getBound(int boundType, int noOfLevels) {
        int offset = 0;
        int keystrength = 0;
        if (noOfLevels > 0) {
            while (offset < this.m_key_.length && this.m_key_[offset] != (byte) 0) {
                int offset2 = offset + 1;
                if (this.m_key_[offset] == 1) {
                    keystrength++;
                    noOfLevels--;
                    if (noOfLevels == 0 || offset2 == this.m_key_.length || this.m_key_[offset2] == (byte) 0) {
                        offset = offset2 - 1;
                        break;
                    }
                }
                offset = offset2;
            }
        }
        if (noOfLevels <= 0) {
            byte[] resultkey = new byte[((offset + boundType) + 1)];
            System.arraycopy(this.m_key_, 0, resultkey, 0, offset);
            switch (boundType) {
                case 0:
                    break;
                case 1:
                    int offset3 = offset + 1;
                    resultkey[offset] = (byte) 2;
                    offset = offset3;
                    break;
                case 2:
                    int offset4 = offset + 1;
                    resultkey[offset] = (byte) -1;
                    offset = offset4 + 1;
                    resultkey[offset4] = (byte) -1;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal boundType argument");
            }
            resultkey[offset] = (byte) 0;
            return new CollationKey(null, resultkey, offset);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Source collation key has only ");
        stringBuilder.append(keystrength);
        stringBuilder.append(" strength level. Call getBound() again  with noOfLevels < ");
        stringBuilder.append(keystrength);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public CollationKey merge(CollationKey source) {
        if (source == null || source.getLength() == 0) {
            throw new IllegalArgumentException("CollationKey argument can not be null or of 0 length");
        }
        int rindex;
        int i;
        byte[] result = new byte[((getLength() + source.getLength()) + 2)];
        int index = 0;
        int rindex2 = 0;
        int sourceindex = 0;
        while (true) {
            int index2;
            if (this.m_key_[index] < (byte) 0 || this.m_key_[index] >= (byte) 2) {
                rindex = rindex2 + 1;
                index2 = index + 1;
                result[rindex2] = this.m_key_[index];
                rindex2 = rindex;
                index = index2;
            } else {
                rindex = rindex2 + 1;
                result[rindex2] = (byte) 2;
                while (true) {
                    if (source.m_key_[sourceindex] >= (byte) 0 && source.m_key_[sourceindex] < (byte) 2) {
                        break;
                    }
                    rindex2 = rindex + 1;
                    index2 = sourceindex + 1;
                    result[rindex] = source.m_key_[sourceindex];
                    rindex = rindex2;
                    sourceindex = index2;
                }
                if (this.m_key_[index] == (byte) 1 && source.m_key_[sourceindex] == (byte) 1) {
                    index++;
                    sourceindex++;
                    rindex2 = rindex + 1;
                    result[rindex] = (byte) 1;
                } else {
                    i = this.m_length_ - index;
                    rindex2 = i;
                }
            }
        }
        i = this.m_length_ - index;
        rindex2 = i;
        if (i > 0) {
            System.arraycopy(this.m_key_, index, result, rindex, rindex2);
            rindex += rindex2;
        } else {
            i = source.m_length_ - sourceindex;
            rindex2 = i;
            if (i > 0) {
                System.arraycopy(source.m_key_, sourceindex, result, rindex, rindex2);
                rindex += rindex2;
            }
        }
        result[rindex] = (byte) 0;
        return new CollationKey(null, result, rindex);
    }

    private int getLength() {
        if (this.m_length_ >= 0) {
            return this.m_length_;
        }
        int length = this.m_key_.length;
        for (int index = 0; index < length; index++) {
            if (this.m_key_[index] == (byte) 0) {
                length = index;
                break;
            }
        }
        this.m_length_ = length;
        return this.m_length_;
    }
}
