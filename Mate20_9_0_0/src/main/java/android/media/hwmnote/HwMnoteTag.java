package android.media.hwmnote;

import android.util.Log;

public class HwMnoteTag {
    static final int SIZE_UNDEFINED = 0;
    private static final String TAG = "HwMnoteTag";
    private static final int[] TYPE_TO_SIZE_MAP = new int[11];
    public static final short TYPE_UNDEFINED = (short) 7;
    public static final short TYPE_UNSIGNED_LONG = (short) 4;
    private static final long UNSIGNED_LONG_MAX = 4294967295L;
    private int mComponentCountActual;
    private final short mDataType;
    private boolean mHasDefinedDefaultComponentCount;
    private int mIfd;
    private int mOffset;
    private final short mTagId;
    private Object mValue = null;

    static {
        TYPE_TO_SIZE_MAP[4] = 4;
        TYPE_TO_SIZE_MAP[7] = 1;
    }

    public static boolean isValidIfd(int ifdId) {
        return ifdId == 0 || ifdId == 1 || ifdId == 2;
    }

    public static boolean isValidType(short type) {
        return type == (short) 4 || type == (short) 7;
    }

    HwMnoteTag(short tagId, short type, int componentCount, int ifd, boolean hasDefinedComponentCount) {
        this.mTagId = tagId;
        this.mDataType = type;
        this.mComponentCountActual = componentCount;
        this.mHasDefinedDefaultComponentCount = hasDefinedComponentCount;
        this.mIfd = ifd;
    }

    public int getIfd() {
        return this.mIfd;
    }

    protected void setIfd(int ifdId) {
        this.mIfd = ifdId;
    }

    public short getTagId() {
        return this.mTagId;
    }

    public short getDataType() {
        return this.mDataType;
    }

    public int getDataSize() {
        return getComponentCount() * TYPE_TO_SIZE_MAP[this.mDataType];
    }

    public int getComponentCount() {
        return this.mComponentCountActual;
    }

    protected void forceSetComponentCount(int count) {
        this.mComponentCountActual = count;
    }

    public boolean hasValue() {
        return this.mValue != null;
    }

    public boolean setValue(int[] value) {
        int i = 0;
        if (checkBadComponentCount(value.length) || this.mDataType != (short) 4 || checkOverflowForUnsignedLong(value)) {
            return false;
        }
        long[] data = new long[value.length];
        int length = value.length;
        while (i < length) {
            data[i] = (long) value[i];
            i++;
        }
        this.mValue = data;
        this.mComponentCountActual = length;
        return true;
    }

    public boolean setValue(int value) {
        return setValue(new int[]{value});
    }

    /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setValue(long[] value) {
        if (checkBadComponentCount(value.length) || this.mDataType != (short) 4 || checkOverflowForUnsignedLong(value)) {
            return false;
        }
        this.mValue = value;
        this.mComponentCountActual = value.length;
        return true;
    }

    public boolean setValue(long value) {
        return setValue(new long[]{value});
    }

    public boolean setValue(byte[] value, int offset, int length) {
        if (checkBadComponentCount(length) || this.mDataType != (short) 7) {
            return false;
        }
        this.mValue = new byte[length];
        System.arraycopy(value, offset, this.mValue, 0, length);
        this.mComponentCountActual = length;
        return true;
    }

    public boolean setValue(byte[] value) {
        return setValue(value, 0, value.length);
    }

    public boolean setValue(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof int[]) {
            return setValue((int[]) obj);
        }
        if (obj instanceof long[]) {
            return setValue((long[]) obj);
        }
        if (obj instanceof byte[]) {
            return setValue((byte[]) obj);
        }
        if (obj instanceof Integer) {
            return setValue(((Integer) obj).intValue());
        }
        if (obj instanceof Long) {
            return setValue(((Long) obj).longValue());
        }
        if (obj instanceof Integer[]) {
            return setValue(getInts((Integer[]) obj));
        }
        if (obj instanceof Long[]) {
            return setValue(getLongs((Long[]) obj));
        }
        if (obj instanceof Byte[]) {
            return setValue(getBytes((Byte[]) obj));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set value with error type, tagId:");
        stringBuilder.append(this.mTagId);
        Log.w(str, stringBuilder.toString());
        return false;
    }

    private int[] getInts(Integer[] obj) {
        Integer[] arr = obj;
        int length = arr.length;
        int[] fin = new int[length];
        for (int i = 0; i < length; i++) {
            fin[i] = arr[i] == null ? 0 : arr[i].intValue();
        }
        return fin;
    }

    private long[] getLongs(Long[] obj) {
        Long[] arr = obj;
        int length = arr.length;
        long[] fin = new long[length];
        for (int i = 0; i < length; i++) {
            fin[i] = arr[i] == null ? 0 : arr[i].longValue();
        }
        return fin;
    }

    private byte[] getBytes(Byte[] obj) {
        Byte[] arr = obj;
        int length = arr.length;
        byte[] fin = new byte[length];
        for (int i = 0; i < length; i++) {
            fin[i] = arr[i] == null ? (byte) 0 : arr[i].byteValue();
        }
        return fin;
    }

    public byte[] getValueAsBytes() {
        if (this.mValue instanceof byte[]) {
            return (byte[]) this.mValue;
        }
        return new byte[0];
    }

    public int[] getValueAsInts() {
        if (this.mValue == null) {
            return null;
        }
        int i = 0;
        if (!(this.mValue instanceof long[])) {
            return new int[0];
        }
        long[] val = this.mValue;
        int length = val.length;
        int[] arr = new int[length];
        while (i < length) {
            arr[i] = (int) val[i];
            i++;
        }
        return arr;
    }

    public int getValueAsInt(int defaultValue) {
        int[] i = getValueAsInts();
        if (i == null || i.length < 1) {
            return defaultValue;
        }
        return i[0];
    }

    public long[] getValueAsLongs() {
        if (this.mValue instanceof long[]) {
            return (long[]) this.mValue;
        }
        return new long[0];
    }

    public long getValueAsLong(long defaultValue) {
        long[] l = getValueAsLongs();
        if (l.length < 1) {
            return defaultValue;
        }
        return l[0];
    }

    public Object getValue() {
        return this.mValue;
    }

    protected long getValueAt(int index) {
        if (this.mValue instanceof long[]) {
            return ((long[]) this.mValue)[index];
        }
        if (this.mValue instanceof byte[]) {
            return (long) ((byte[]) this.mValue)[index];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot get integer value from ");
        stringBuilder.append(convertTypeToString(this.mDataType));
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected void getBytes(byte[] buf) {
        getBytes(buf, 0, buf.length);
    }

    protected void getBytes(byte[] buf, int offset, int length) {
        if (this.mDataType == (short) 7) {
            int i;
            Object obj = this.mValue;
            if (length > this.mComponentCountActual) {
                i = this.mComponentCountActual;
            } else {
                i = length;
            }
            System.arraycopy(obj, 0, buf, offset, i);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot get BYTE value from ");
        stringBuilder.append(convertTypeToString(this.mDataType));
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected int getOffset() {
        return this.mOffset;
    }

    protected void setOffset(int offset) {
        this.mOffset = offset;
    }

    protected void setHasDefinedCount(boolean d) {
        this.mHasDefinedDefaultComponentCount = d;
    }

    protected boolean hasDefinedCount() {
        return this.mHasDefinedDefaultComponentCount;
    }

    private boolean checkBadComponentCount(int count) {
        if (!this.mHasDefinedDefaultComponentCount || this.mComponentCountActual == count) {
            return false;
        }
        return true;
    }

    private static String convertTypeToString(short type) {
        if (type == (short) 4) {
            return "UNSIGNED_LONG";
        }
        if (type != (short) 7) {
            return "";
        }
        return "UNDEFINED";
    }

    private boolean checkOverflowForUnsignedLong(long[] value) {
        for (long v : value) {
            if (v < 0 || v > UNSIGNED_LONG_MAX) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForUnsignedLong(int[] value) {
        for (int v : value) {
            if (v < 0) {
                return true;
            }
        }
        return false;
    }
}
