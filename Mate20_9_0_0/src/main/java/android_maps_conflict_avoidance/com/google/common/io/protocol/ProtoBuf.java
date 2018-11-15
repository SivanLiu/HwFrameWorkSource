package android_maps_conflict_avoidance.com.google.common.io.protocol;

import android_maps_conflict_avoidance.com.google.common.io.IoUtil;
import android_maps_conflict_avoidance.com.google.common.io.MarkedOutputStream;
import android_maps_conflict_avoidance.com.google.common.util.IntMap;
import android_maps_conflict_avoidance.com.google.common.util.IntMap.KeyIterator;
import android_maps_conflict_avoidance.com.google.common.util.Primitives;
import com.google.android.maps.MapView.LayoutParams;
import com.google.android.maps.OverlayItem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

public class ProtoBuf {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final Boolean FALSE = new Boolean(false);
    private static final SimpleCounter NULL_COUNTER = new SimpleCounter();
    public static final Boolean TRUE = new Boolean(true);
    private int cachedSize = Integer.MIN_VALUE;
    private ProtoBufType msgType;
    private final IntMap values;
    private IntMap wireTypes;

    private static class SimpleCounter {
        public int count;

        private SimpleCounter() {
            this.count = 0;
        }
    }

    public ProtoBuf(ProtoBufType type) {
        this.msgType = type;
        this.values = new IntMap();
    }

    public void clear() {
        this.values.clear();
        this.wireTypes = null;
    }

    public void addProtoBuf(int tag, ProtoBuf value) {
        addObject(tag, value);
    }

    public boolean getBool(int tag) {
        return ((Boolean) getObject(tag, 24)).booleanValue();
    }

    public byte[] getBytes(int tag) {
        return (byte[]) getObject(tag, 25);
    }

    public int getInt(int tag) {
        return (int) ((Long) getObject(tag, 21)).longValue();
    }

    public long getLong(int tag) {
        return ((Long) getObject(tag, 19)).longValue();
    }

    public ProtoBuf getProtoBuf(int tag) {
        return (ProtoBuf) getObject(tag, 26);
    }

    public ProtoBuf getProtoBuf(int tag, int index) {
        return (ProtoBuf) getObject(tag, index, 26);
    }

    public String getString(int tag) {
        return (String) getObject(tag, 28);
    }

    public String getString(int tag, int index) {
        return (String) getObject(tag, index, 28);
    }

    public boolean has(int tag) {
        return getCount(tag) > 0 || getDefault(tag) != null;
    }

    public ProtoBuf parse(byte[] data) throws IOException {
        parse(new ByteArrayInputStream(data), data.length);
        return this;
    }

    public ProtoBuf parse(InputStream is) throws IOException {
        parse(is, Integer.MAX_VALUE);
        return this;
    }

    public int parse(InputStream is, int available) throws IOException {
        return parseInternal(is, available, true, new SimpleCounter());
    }

    /* JADX WARNING: Removed duplicated region for block: B:59:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x0152 A:{RETURN} */
    /* JADX WARNING: Missing block: B:45:0x0115, code:
            r9 = r22;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseInternal(InputStream is, int available, boolean clear, SimpleCounter counter) throws IOException {
        InputStream inputStream = is;
        SimpleCounter simpleCounter = counter;
        if (clear) {
            clear();
        }
        long v = 0;
        Object value = null;
        int shift = 0;
        int total = 0;
        int available2 = available;
        while (available2 > 0) {
            long tagAndType = readVarInt(inputStream, 1, simpleCounter);
            if (tagAndType != -1) {
                available2 -= simpleCounter.count;
                int wireType = ((int) tagAndType) & 7;
                if (wireType != 4) {
                    int available3;
                    int available4;
                    Object obj;
                    int count;
                    int available5 = available2;
                    int tag = (int) (tagAndType >>> 3);
                    available2 = getType(tag);
                    if (available2 == 16) {
                        if (this.wireTypes == null) {
                            this.wireTypes = new IntMap();
                        }
                        this.wireTypes.put(tag, Primitives.toInteger(wireType));
                        available2 = wireType;
                    } else {
                        int i = available2;
                    }
                    if (wireType != 5) {
                        int shift2;
                        ProtoBuf group;
                        switch (wireType) {
                            case LayoutParams.MODE_MAP /*0*/:
                                shift2 = shift;
                                available3 = available5;
                                v = readVarInt(inputStream, false, simpleCounter);
                                available4 = available3 - simpleCounter.count;
                                if (isZigZagEncodedType(tag)) {
                                    v = zigZagDecode(v);
                                }
                                value = Primitives.toLong(v);
                                break;
                            case 1:
                                break;
                            case OverlayItem.ITEM_STATE_SELECTED_MASK /*2*/:
                                obj = value;
                                shift2 = shift;
                                available4 = (int) readVarInt(inputStream, false, simpleCounter);
                                value = (available5 - simpleCounter.count) - available4;
                                if (available2 != 27) {
                                    group = available4 == 0 ? EMPTY_BYTE_ARRAY : new byte[available4];
                                    shift = 0;
                                    while (shift < available4) {
                                        count = inputStream.read(group, shift, available4 - shift);
                                        if (count > 0) {
                                            shift += count;
                                        } else {
                                            throw new IOException("Unexp.EOF");
                                        }
                                    }
                                    int total2 = available4;
                                    available4 = group;
                                    shift = shift2;
                                    total = total2;
                                    break;
                                }
                                group = new ProtoBuf((ProtoBufType) this.msgType.getData(tag));
                                group.parseInternal(inputStream, available4, 0, simpleCounter);
                                total = available4;
                                available4 = value;
                                value = group;
                                break;
                            case LayoutParams.LEFT /*3*/:
                                ProtoBuf group2 = new ProtoBuf(this.msgType == null ? null : (ProtoBufType) this.msgType.getData(tag));
                                ProtoBuf value2 = group2;
                                group = group2;
                                available4 = group2.parseInternal(inputStream, available5, false, simpleCounter);
                                value = value2;
                                break;
                            default:
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown wire type ");
                                stringBuilder.append(wireType);
                                stringBuilder.append(", reading garbage data?");
                                throw new IOException(stringBuilder.toString());
                        }
                    }
                    obj = value;
                    available3 = available5;
                    v = 0;
                    count = 0;
                    available4 = wireType == 5 ? 4 : 8;
                    value = available3 - available4;
                    while (true) {
                        int count2 = available4 - 1;
                        if (available4 > 0) {
                            v |= ((long) is.read()) << count;
                            count += 8;
                            available4 = count2;
                            total = total;
                        } else {
                            available4 = Primitives.toLong(v);
                            shift = count;
                            Object obj2 = value;
                            value = available4;
                            available4 = obj2;
                            addObject(tag, value);
                            available2 = available4;
                        }
                    }
                }
            }
            if (available2 < 0) {
                return available2;
            }
            throw new IOException();
        }
        if (available2 < 0) {
        }
    }

    private static int getCount(Object o) {
        if (o == null) {
            return 0;
        }
        return o instanceof Vector ? ((Vector) o).size() : 1;
    }

    public int getCount(int tag) {
        return getCount(this.values.get(tag));
    }

    public int getType(int tag) {
        int tagType = 16;
        if (this.msgType != null) {
            tagType = this.msgType.getType(tag);
        }
        if (tagType == 16) {
            Integer tagTypeObj = this.wireTypes != null ? (Integer) this.wireTypes.get(tag) : null;
            if (tagTypeObj != null) {
                tagType = tagTypeObj.intValue();
            }
        }
        if (tagType != 16 || getCount(tag) <= 0) {
            return tagType;
        }
        int i = 0;
        Object o = getObject(tag, 0, 16);
        if (!((o instanceof Long) || (o instanceof Boolean))) {
            i = 2;
        }
        return i;
    }

    private static int getVarIntSize(long i) {
        if (i < 0) {
            return 10;
        }
        int size = 1;
        while (i >= 128) {
            size++;
            i >>= 7;
        }
        return size;
    }

    public void outputWithSizeTo(OutputStream os) throws IOException {
        outputTo(os, true);
    }

    public void outputTo(OutputStream os) throws IOException {
        outputTo(os, false);
    }

    private void outputTo(OutputStream os, boolean addSize) throws IOException {
        MarkedOutputStream mos = new MarkedOutputStream();
        int size = outputToInternal(mos);
        if (addSize) {
            ((DataOutput) os).writeInt(size);
        }
        int previous = 0;
        int n = mos.numMarkers();
        for (int i = 0; i < n; i += 2) {
            int current = mos.getMarker(i);
            mos.writeContentsTo(os, previous, current - previous);
            writeVarInt(os, (long) mos.getMarker(i + 1));
            previous = current;
        }
        if (previous < mos.availableContent()) {
            mos.writeContentsTo(os, previous, mos.availableContent() - previous);
        }
    }

    private int outputToInternal(MarkedOutputStream os) throws IOException {
        KeyIterator itr = this.values.keys();
        int totalSize = 0;
        while (itr.hasNext()) {
            totalSize += outputField(itr.next(), os);
        }
        return totalSize;
    }

    /* JADX WARNING: Missing block: B:8:0x0054, code:
            r9 = r17;
            r10 = r18;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int outputField(int tag, MarkedOutputStream os) throws IOException {
        int i = tag;
        OutputStream outputStream = os;
        int size = getCount(tag);
        int wireType = getWireType(tag);
        int wireTypeTag = (i << 3) | wireType;
        long v = 0;
        int cnt = 0;
        Object o = null;
        int totalSize = 0;
        for (int i2 = 0; i2 < size; i2++) {
            Object o2;
            long v2;
            totalSize += writeVarInt(outputStream, (long) wireTypeTag);
            boolean added = false;
            int contentStart = os.availableContent();
            int i3 = 4;
            if (wireType != 5) {
                int cnt2;
                long j;
                switch (wireType) {
                    case LayoutParams.MODE_MAP /*0*/:
                        o2 = o;
                        cnt2 = cnt;
                        j = v;
                        v2 = ((Long) getObject(i, i2, 19)).longValue();
                        if (isZigZagEncodedType(tag)) {
                            v2 = zigZagEncode(v2);
                        }
                        writeVarInt(outputStream, v2);
                        v = v2;
                        break;
                    case 1:
                        break;
                    case OverlayItem.ITEM_STATE_SELECTED_MASK /*2*/:
                        cnt2 = cnt;
                        Object o3 = getObject(i, i2, getType(tag) == 27 ? 16 : 25);
                        if ((o3 instanceof byte[]) != null) {
                            byte[] o4 = (byte[]) o3;
                            j = v;
                            writeVarInt(outputStream, (long) o4.length);
                            outputStream.write(o4);
                        } else {
                            j = v;
                            outputStream.addMarker(os.availableContent());
                            o = os.numMarkers();
                            outputStream.addMarker(-1);
                            cnt = ((ProtoBuf) o3).outputToInternal(outputStream);
                            outputStream.setMarker(o, cnt);
                            totalSize += getVarIntSize((long) cnt) + cnt;
                            added = true;
                        }
                        o = o3;
                        cnt = cnt2;
                        v = j;
                        break;
                    case LayoutParams.LEFT /*3*/:
                        o2 = o;
                        cnt2 = cnt;
                        totalSize = (totalSize + ((ProtoBuf) getObject(i, i2, 26)).outputToInternal(outputStream)) + writeVarInt(outputStream, (long) ((i << 3) | 4));
                        added = true;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
            o2 = o;
            v2 = ((Long) getObject(i, i2, 19)).longValue();
            if (wireType != 5) {
                i3 = 8;
            }
            int cnt3 = i3;
            long v3 = v2;
            for (int b = 0; b < cnt3; b++) {
                outputStream.write((int) (v3 & 255));
                v3 >>= 8;
            }
            cnt = cnt3;
            v = v3;
            o = o2;
            if (!added) {
                totalSize += os.availableContent() - contentStart;
            }
        }
        return totalSize;
    }

    private boolean isZigZagEncodedType(int tag) {
        int declaredType = getType(tag);
        return declaredType == 33 || declaredType == 34;
    }

    private static long zigZagEncode(long v) {
        return (v << 1) ^ (-(v >>> 63));
    }

    private static long zigZagDecode(long v) {
        return (v >>> 1) ^ (-(1 & v));
    }

    public void setInt(int tag, int value) {
        setLong(tag, (long) value);
    }

    public void setLong(int tag, long value) {
        setObject(tag, Primitives.toLong(value));
    }

    public void setString(int tag, String value) {
        setObject(tag, value);
    }

    private void assertTypeMatch(int tag, Object object) {
    }

    private Object getDefault(int tag) {
        int type = getType(tag);
        if (type != 16) {
            switch (type) {
                case 26:
                case 27:
                    break;
                default:
                    return this.msgType.getData(tag);
            }
        }
        return null;
    }

    private static void checkTag(int tag) {
    }

    private Object getObject(int tag, int desiredType) {
        checkTag(tag);
        Object o = this.values.get(tag);
        int count = getCount(o);
        if (count == 0) {
            return getDefault(tag);
        }
        if (count <= 1) {
            return getObjectWithoutArgChecking(tag, 0, desiredType, o);
        }
        throw new IllegalArgumentException();
    }

    private Object getObject(int tag, int index, int desiredType) {
        checkTag(tag);
        Object o = this.values.get(tag);
        if (index < getCount(o)) {
            return getObjectWithoutArgChecking(tag, index, desiredType, o);
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    private Object getObjectWithoutArgChecking(int tag, int index, int desiredType, Object o) {
        Vector v = null;
        if (o instanceof Vector) {
            v = (Vector) o;
            o = v.elementAt(index);
        }
        Object o2 = convert(o, desiredType);
        if (!(o2 == o || o == null)) {
            if (v == null) {
                setObject(tag, o2);
            } else {
                v.setElementAt(o2, index);
            }
        }
        return o2;
    }

    private final int getWireType(int tag) {
        int tagType = getType(tag);
        if (tagType != 5) {
            switch (tagType) {
                case LayoutParams.MODE_MAP /*0*/:
                case 1:
                case OverlayItem.ITEM_STATE_SELECTED_MASK /*2*/:
                case LayoutParams.LEFT /*3*/:
                    break;
                default:
                    switch (tagType) {
                        case LayoutParams.CENTER_VERTICAL /*16*/:
                            break;
                        case LayoutParams.CENTER /*17*/:
                        case 22:
                        case 32:
                            return 1;
                        case 18:
                        case 23:
                        case 31:
                            return 5;
                        case 19:
                        case 20:
                        case 21:
                        case 24:
                        case 29:
                        case 30:
                        case 33:
                        case 34:
                            return 0;
                        case 25:
                        case 27:
                        case 28:
                        case 35:
                        case 36:
                            return 2;
                        case 26:
                            return 3;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupp.Type:");
                            stringBuilder.append(this.msgType);
                            stringBuilder.append('/');
                            stringBuilder.append(tag);
                            stringBuilder.append('/');
                            stringBuilder.append(tagType);
                            throw new RuntimeException(stringBuilder.toString());
                    }
            }
        }
        return tagType;
    }

    private void insertObject(int tag, int index, Object o, boolean appendToEnd) {
        checkTag(tag);
        Vector current = this.values.get(tag);
        Vector v = null;
        if (current instanceof Vector) {
            v = current;
        }
        if (current == null || (v != null && v.size() == 0)) {
            setObject(tag, o);
            return;
        }
        assertTypeMatch(tag, o);
        if (v == null) {
            v = new Vector();
            v.addElement(current);
            this.values.put(tag, v);
        }
        if (appendToEnd) {
            v.addElement(o);
        } else {
            v.insertElementAt(o, index);
        }
    }

    private void addObject(int tag, Object o) {
        insertObject(tag, 0, o, true);
    }

    private static Object convert(Object obj, int tagType) {
        switch (tagType) {
            case LayoutParams.CENTER_VERTICAL /*16*/:
                return obj;
            case 19:
            case 21:
            case 22:
            case 23:
            case 31:
            case 32:
            case 33:
            case 34:
                if (!(obj instanceof Boolean)) {
                    return obj;
                }
                return Primitives.toLong(((Boolean) obj).booleanValue() ? 1 : 0);
            case 24:
                if (obj instanceof Boolean) {
                    return obj;
                }
                switch ((int) ((Long) obj).longValue()) {
                    case LayoutParams.MODE_MAP /*0*/:
                        return FALSE;
                    case 1:
                        return TRUE;
                    default:
                        throw new IllegalArgumentException("Type mismatch");
                }
            case 25:
            case 35:
                if (obj instanceof String) {
                    return IoUtil.encodeUtf8((String) obj);
                }
                if (!(obj instanceof ProtoBuf)) {
                    return obj;
                }
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try {
                    ((ProtoBuf) obj).outputTo(buf);
                    return buf.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException(e.toString());
                }
            case 26:
            case 27:
                if (!(obj instanceof byte[])) {
                    return obj;
                }
                try {
                    return new ProtoBuf(null).parse((byte[]) obj);
                } catch (IOException e2) {
                    throw new RuntimeException(e2.toString());
                }
            case 28:
            case 36:
                if (!(obj instanceof byte[])) {
                    return obj;
                }
                byte[] data = (byte[]) obj;
                return IoUtil.decodeUtf8(data, 0, data.length, true);
            default:
                throw new RuntimeException("Unsupp.Type");
        }
    }

    private static long readVarInt(InputStream is, boolean permitEOF, SimpleCounter counter) throws IOException {
        long result = 0;
        int shift = 0;
        int i = 0;
        counter.count = 0;
        while (i < 10) {
            int in = is.read();
            if (in != -1) {
                result |= ((long) (in & 127)) << shift;
                if ((in & 128) == 0) {
                    break;
                }
                shift += 7;
                i++;
            } else if (i == 0 && permitEOF) {
                return -1;
            } else {
                throw new IOException("EOF");
            }
        }
        counter.count = i + 1;
        return result;
    }

    private void setObject(int tag, Object o) {
        if (tag >= 0) {
            if (o != null) {
                assertTypeMatch(tag, o);
            }
            this.values.put(tag, o);
            return;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    static int writeVarInt(OutputStream os, long value) throws IOException {
        int i = 0;
        while (i < 10) {
            int toWrite = (int) (127 & value);
            value >>>= 7;
            if (value == 0) {
                os.write(toWrite);
                return i + 1;
            }
            os.write(toWrite | 128);
            i++;
        }
        return i;
    }
}
