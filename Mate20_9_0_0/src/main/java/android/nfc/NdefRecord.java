package android.nfc;

import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.webkit.WebView;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public final class NdefRecord implements Parcelable {
    public static final Creator<NdefRecord> CREATOR = new Creator<NdefRecord>() {
        public NdefRecord createFromParcel(Parcel in) {
            short tnf = (short) in.readInt();
            byte[] type = new byte[in.readInt()];
            in.readByteArray(type);
            byte[] id = new byte[in.readInt()];
            in.readByteArray(id);
            byte[] payload = new byte[in.readInt()];
            in.readByteArray(payload);
            return new NdefRecord(tnf, type, id, payload);
        }

        public NdefRecord[] newArray(int size) {
            return new NdefRecord[size];
        }
    };
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final byte FLAG_CF = (byte) 32;
    private static final byte FLAG_IL = (byte) 8;
    private static final byte FLAG_MB = Byte.MIN_VALUE;
    private static final byte FLAG_ME = (byte) 64;
    private static final byte FLAG_SR = (byte) 16;
    private static final int MAX_PAYLOAD_SIZE = 10485760;
    public static final byte[] RTD_ALTERNATIVE_CARRIER = new byte[]{(byte) 97, (byte) 99};
    public static final byte[] RTD_ANDROID_APP = "android.com:pkg".getBytes();
    public static final byte[] RTD_HANDOVER_CARRIER = new byte[]{(byte) 72, (byte) 99};
    public static final byte[] RTD_HANDOVER_REQUEST = new byte[]{(byte) 72, (byte) 114};
    public static final byte[] RTD_HANDOVER_SELECT = new byte[]{(byte) 72, (byte) 115};
    public static final byte[] RTD_SMART_POSTER = new byte[]{(byte) 83, (byte) 112};
    public static final byte[] RTD_TEXT = new byte[]{(byte) 84};
    public static final byte[] RTD_URI = new byte[]{(byte) 85};
    public static final short TNF_ABSOLUTE_URI = (short) 3;
    public static final short TNF_EMPTY = (short) 0;
    public static final short TNF_EXTERNAL_TYPE = (short) 4;
    public static final short TNF_MIME_MEDIA = (short) 2;
    public static final short TNF_RESERVED = (short) 7;
    public static final short TNF_UNCHANGED = (short) 6;
    public static final short TNF_UNKNOWN = (short) 5;
    public static final short TNF_WELL_KNOWN = (short) 1;
    private static final String[] URI_PREFIX_MAP = new String[]{"", "http://www.", "https://www.", "http://", "https://", WebView.SCHEME_TEL, WebView.SCHEME_MAILTO, "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"};
    private final byte[] mId;
    private final byte[] mPayload;
    private final short mTnf;
    private final byte[] mType;

    public static NdefRecord createApplicationRecord(String packageName) {
        if (packageName == null) {
            throw new NullPointerException("packageName is null");
        } else if (packageName.length() != 0) {
            return new NdefRecord((short) 4, RTD_ANDROID_APP, null, packageName.getBytes(StandardCharsets.UTF_8));
        } else {
            throw new IllegalArgumentException("packageName is empty");
        }
    }

    public static NdefRecord createUri(Uri uri) {
        if (uri != null) {
            String uriString = uri.normalizeScheme().toString();
            if (uriString.length() != 0) {
                byte prefix = (byte) 0;
                for (int i = 1; i < URI_PREFIX_MAP.length; i++) {
                    if (uriString.startsWith(URI_PREFIX_MAP[i])) {
                        prefix = (byte) i;
                        uriString = uriString.substring(URI_PREFIX_MAP[i].length());
                        break;
                    }
                }
                byte[] uriBytes = uriString.getBytes(StandardCharsets.UTF_8);
                byte[] recordBytes = new byte[(uriBytes.length + 1)];
                recordBytes[0] = prefix;
                System.arraycopy(uriBytes, 0, recordBytes, 1, uriBytes.length);
                return new NdefRecord((short) 1, RTD_URI, null, recordBytes);
            }
            throw new IllegalArgumentException("uri is empty");
        }
        throw new NullPointerException("uri is null");
    }

    public static NdefRecord createUri(String uriString) {
        return createUri(Uri.parse(uriString));
    }

    public static NdefRecord createMime(String mimeType, byte[] mimeData) {
        if (mimeType != null) {
            mimeType = Intent.normalizeMimeType(mimeType);
            if (mimeType.length() != 0) {
                int slashIndex = mimeType.indexOf(47);
                if (slashIndex == 0) {
                    throw new IllegalArgumentException("mimeType must have major type");
                } else if (slashIndex != mimeType.length() - 1) {
                    return new NdefRecord((short) 2, mimeType.getBytes(StandardCharsets.US_ASCII), null, mimeData);
                } else {
                    throw new IllegalArgumentException("mimeType must have minor type");
                }
            }
            throw new IllegalArgumentException("mimeType is empty");
        }
        throw new NullPointerException("mimeType is null");
    }

    public static NdefRecord createExternal(String domain, String type, byte[] data) {
        if (domain == null) {
            throw new NullPointerException("domain is null");
        } else if (type != null) {
            domain = domain.trim().toLowerCase(Locale.ROOT);
            type = type.trim().toLowerCase(Locale.ROOT);
            if (domain.length() == 0) {
                throw new IllegalArgumentException("domain is empty");
            } else if (type.length() != 0) {
                byte[] byteDomain = domain.getBytes(StandardCharsets.UTF_8);
                byte[] byteType = type.getBytes(StandardCharsets.UTF_8);
                byte[] b = new byte[((byteDomain.length + 1) + byteType.length)];
                System.arraycopy(byteDomain, 0, b, 0, byteDomain.length);
                b[byteDomain.length] = (byte) 58;
                System.arraycopy(byteType, 0, b, byteDomain.length + 1, byteType.length);
                return new NdefRecord((short) 4, b, null, data);
            } else {
                throw new IllegalArgumentException("type is empty");
            }
        } else {
            throw new NullPointerException("type is null");
        }
    }

    public static NdefRecord createTextRecord(String languageCode, String text) {
        if (text != null) {
            byte[] languageCodeBytes;
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            if (languageCode == null || languageCode.isEmpty()) {
                languageCodeBytes = Locale.getDefault().getLanguage().getBytes(StandardCharsets.US_ASCII);
            } else {
                languageCodeBytes = languageCode.getBytes(StandardCharsets.US_ASCII);
            }
            if (languageCodeBytes.length < 64) {
                ByteBuffer buffer = ByteBuffer.allocate((languageCodeBytes.length + 1) + textBytes.length);
                buffer.put((byte) (languageCodeBytes.length & 255));
                buffer.put(languageCodeBytes);
                buffer.put(textBytes);
                return new NdefRecord((short) 1, RTD_TEXT, null, buffer.array());
            }
            throw new IllegalArgumentException("language code is too long, must be <64 bytes.");
        }
        throw new NullPointerException("text is null");
    }

    public NdefRecord(short tnf, byte[] type, byte[] id, byte[] payload) {
        if (type == null) {
            type = EMPTY_BYTE_ARRAY;
        }
        if (id == null) {
            id = EMPTY_BYTE_ARRAY;
        }
        if (payload == null) {
            payload = EMPTY_BYTE_ARRAY;
        }
        String message = validateTnf(tnf, type, id, payload);
        if (message == null) {
            this.mTnf = tnf;
            this.mType = type;
            this.mId = id;
            this.mPayload = payload;
            return;
        }
        throw new IllegalArgumentException(message);
    }

    @Deprecated
    public NdefRecord(byte[] data) throws FormatException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        NdefRecord[] rs = parse(buffer, 1);
        if (buffer.remaining() <= 0) {
            this.mTnf = rs[0].mTnf;
            this.mType = rs[0].mType;
            this.mId = rs[0].mId;
            this.mPayload = rs[0].mPayload;
            return;
        }
        throw new FormatException("data too long");
    }

    public short getTnf() {
        return this.mTnf;
    }

    public byte[] getType() {
        return (byte[]) this.mType.clone();
    }

    public byte[] getId() {
        return (byte[]) this.mId.clone();
    }

    public byte[] getPayload() {
        return (byte[]) this.mPayload.clone();
    }

    @Deprecated
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(getByteLength());
        writeToByteBuffer(buffer, true, true);
        return buffer.array();
    }

    public String toMimeType() {
        switch (this.mTnf) {
            case (short) 1:
                if (Arrays.equals(this.mType, RTD_TEXT)) {
                    return "text/plain";
                }
                break;
            case (short) 2:
                return Intent.normalizeMimeType(new String(this.mType, StandardCharsets.US_ASCII));
        }
        return null;
    }

    public Uri toUri() {
        return toUri(false);
    }

    private Uri toUri(boolean inSmartPoster) {
        short s = this.mTnf;
        Uri uri = null;
        if (s != (short) 1) {
            switch (s) {
                case (short) 3:
                    return Uri.parse(new String(this.mType, StandardCharsets.UTF_8)).normalizeScheme();
                case (short) 4:
                    if (!inSmartPoster) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("vnd.android.nfc://ext/");
                        stringBuilder.append(new String(this.mType, StandardCharsets.US_ASCII));
                        return Uri.parse(stringBuilder.toString());
                    }
                    break;
            }
        } else if (Arrays.equals(this.mType, RTD_SMART_POSTER) && !inSmartPoster) {
            try {
                for (NdefRecord nestedRecord : new NdefMessage(this.mPayload).getRecords()) {
                    Uri uri2 = nestedRecord.toUri(true);
                    if (uri2 != null) {
                        return uri2;
                    }
                }
            } catch (FormatException e) {
            }
        } else if (Arrays.equals(this.mType, RTD_URI)) {
            Uri wktUri = parseWktUri();
            if (wktUri != null) {
                uri = wktUri.normalizeScheme();
            }
            return uri;
        }
        return null;
    }

    private Uri parseWktUri() {
        if (this.mPayload.length < 2) {
            return null;
        }
        int prefixIndex = this.mPayload[0] & -1;
        if (prefixIndex < 0 || prefixIndex >= URI_PREFIX_MAP.length) {
            return null;
        }
        String prefix = URI_PREFIX_MAP[prefixIndex];
        String suffix = new String(Arrays.copyOfRange(this.mPayload, 1, this.mPayload.length), StandardCharsets.UTF_8);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(suffix);
        return Uri.parse(stringBuilder.toString());
    }

    static NdefRecord[] parse(ByteBuffer buffer, boolean ignoreMbMe) throws FormatException {
        ByteBuffer byteBuffer = buffer;
        ArrayList records = new ArrayList();
        try {
            ArrayList<byte[]> chunks = new ArrayList();
            short chunkTnf = (short) -1;
            boolean inChunk = false;
            byte[] payload = null;
            byte[] id = null;
            byte[] type = null;
            boolean me = false;
            while (!me) {
                byte[] type2;
                byte[] id2;
                short chunkTnf2;
                long payloadLength;
                boolean me2;
                byte[] type3;
                short chunkTnf3;
                ArrayList<byte[]> chunks2;
                short tnf;
                byte flag = buffer.get();
                boolean il = true;
                boolean mb = (flag & WifiNetworkScoreCache.INVALID_NETWORK_SCORE) != 0;
                me = (flag & 64) != 0;
                boolean cf = (flag & 32) != 0;
                boolean sr = (flag & 16) != 0;
                if ((flag & 8) == 0) {
                    il = false;
                }
                short tnf2 = (short) (flag & 7);
                if (mb || records.size() != 0 || inChunk) {
                    type2 = type;
                    id2 = id;
                } else if (ignoreMbMe) {
                    type2 = type;
                    id2 = id;
                } else {
                    throw new FormatException("expected MB flag");
                }
                if (mb && (records.size() != 0 || inChunk)) {
                    if (!ignoreMbMe) {
                        throw new FormatException("unexpected MB flag");
                    }
                }
                if (inChunk) {
                    if (il) {
                        throw new FormatException("unexpected IL flag in non-leading chunk");
                    }
                }
                if (cf) {
                    if (me) {
                        throw new FormatException("unexpected ME flag in non-trailing chunk");
                    }
                }
                if (inChunk) {
                    if (tnf2 != (short) 6) {
                        throw new FormatException("expected TNF_UNCHANGED in non-leading chunk");
                    }
                }
                if (!inChunk) {
                    if (tnf2 == (short) 6) {
                        throw new FormatException("unexpected TNF_UNCHANGED in first chunk or unchunked record");
                    }
                }
                int typeLength = buffer.get() & 255;
                if (sr) {
                    chunkTnf2 = chunkTnf;
                    payloadLength = (long) (buffer.get() & 255);
                } else {
                    chunkTnf2 = chunkTnf;
                    byte b = flag;
                    payloadLength = ((long) buffer.getInt()) & 4294967295L;
                }
                int idLength = il ? buffer.get() & 255 : 0;
                byte[] bArr;
                if (!inChunk) {
                    me2 = me;
                    bArr = payload;
                } else if (typeLength == 0) {
                    me2 = me;
                    bArr = payload;
                } else {
                    throw new FormatException("expected zero-length type in non-leading chunk");
                }
                if (inChunk) {
                    type3 = type2;
                    payload = id2;
                } else {
                    type3 = typeLength > 0 ? new byte[typeLength] : EMPTY_BYTE_ARRAY;
                    payload = idLength > 0 ? new byte[idLength] : EMPTY_BYTE_ARRAY;
                    byteBuffer.get(type3);
                    byteBuffer.get(payload);
                }
                ensureSanePayloadSize(payloadLength);
                if (payloadLength > 0) {
                    id = new byte[((int) payloadLength)];
                } else {
                    id = EMPTY_BYTE_ARRAY;
                }
                byteBuffer.get(id);
                if (!cf || inChunk) {
                    chunkTnf3 = chunkTnf2;
                } else {
                    if (typeLength != 0) {
                    } else if (tnf2 == (short) 5) {
                        int i = typeLength;
                    } else {
                        throw new FormatException("expected non-zero type length in first chunk");
                    }
                    chunks.clear();
                    chunkTnf3 = tnf2;
                }
                if (cf || inChunk) {
                    chunks.add(id);
                }
                boolean z;
                if (cf || !inChunk) {
                    chunks2 = chunks;
                    z = inChunk;
                    type = id;
                    tnf = tnf2;
                } else {
                    short tnf3;
                    payloadLength = 0;
                    Iterator it = chunks.iterator();
                    while (it.hasNext()) {
                        Iterator it2 = it;
                        type = (byte[]) it.next();
                        payloadLength += (long) type.length;
                        it = it2;
                        id = id;
                    }
                    ensureSanePayloadSize(payloadLength);
                    type = new byte[((int) payloadLength)];
                    int i2 = null;
                    id = chunks.iterator();
                    chunks2 = chunks;
                    chunks = i2;
                    while (id.hasNext()) {
                        byte[] bArr2 = id;
                        z = inChunk;
                        id = (byte[]) id.next();
                        tnf3 = tnf2;
                        System.arraycopy(id, (short) 0, type, chunks, id.length);
                        chunks += id.length;
                        id = bArr2;
                        inChunk = z;
                        tnf2 = tnf3;
                    }
                    tnf3 = tnf2;
                    tnf = chunkTnf3;
                }
                if (cf) {
                    inChunk = true;
                } else {
                    inChunk = false;
                    String error = validateTnf(tnf, type3, payload, type);
                    if (error == null) {
                        records.add(new NdefRecord(tnf, type3, payload, type));
                        if (ignoreMbMe) {
                            break;
                        }
                    } else {
                        throw new FormatException(error);
                    }
                }
                chunkTnf = chunkTnf3;
                id = payload;
                chunks = chunks2;
                byteBuffer = buffer;
                payload = type;
                type = type3;
                me = me2;
            }
            return (NdefRecord[]) records.toArray(new NdefRecord[records.size()]);
        } catch (BufferUnderflowException e) {
            throw new FormatException("expected more data", e);
        }
    }

    private static void ensureSanePayloadSize(long size) throws FormatException {
        if (size > 10485760) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("payload above max limit: ");
            stringBuilder.append(size);
            stringBuilder.append(" > ");
            stringBuilder.append(MAX_PAYLOAD_SIZE);
            throw new FormatException(stringBuilder.toString());
        }
    }

    static String validateTnf(short tnf, byte[] type, byte[] id, byte[] payload) {
        switch (tnf) {
            case (short) 0:
                if (type.length == 0 && id.length == 0 && payload.length == 0) {
                    return null;
                }
                return "unexpected data in TNF_EMPTY record";
            case (short) 1:
            case (short) 2:
            case (short) 3:
            case (short) 4:
                return null;
            case (short) 5:
            case (short) 7:
                if (type.length != 0) {
                    return "unexpected type field in TNF_UNKNOWN or TNF_RESERVEd record";
                }
                return null;
            case (short) 6:
                return "unexpected TNF_UNCHANGED in first chunk or logical record";
            default:
                return String.format("unexpected tnf value: 0x%02x", new Object[]{Short.valueOf(tnf)});
        }
    }

    void writeToByteBuffer(ByteBuffer buffer, boolean mb, boolean me) {
        boolean il = true;
        int i = 0;
        boolean sr = this.mPayload.length < 256;
        if (this.mTnf != (short) 0 && this.mId.length <= 0) {
            il = false;
        }
        int i2 = ((mb ? WifiNetworkScoreCache.INVALID_NETWORK_SCORE : 0) | (me ? 64 : 0)) | (sr ? 16 : 0);
        if (il) {
            i = 8;
        }
        buffer.put((byte) ((i | i2) | this.mTnf));
        buffer.put((byte) this.mType.length);
        if (sr) {
            buffer.put((byte) this.mPayload.length);
        } else {
            buffer.putInt(this.mPayload.length);
        }
        if (il) {
            buffer.put((byte) this.mId.length);
        }
        buffer.put(this.mType);
        buffer.put(this.mId);
        buffer.put(this.mPayload);
    }

    int getByteLength() {
        int length = ((3 + this.mType.length) + this.mId.length) + this.mPayload.length;
        boolean z = true;
        boolean sr = this.mPayload.length < 256;
        if (this.mTnf != (short) 0 && this.mId.length <= 0) {
            z = false;
        }
        boolean il = z;
        if (!sr) {
            length += 3;
        }
        if (il) {
            return length + 1;
        }
        return length;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mTnf);
        dest.writeInt(this.mType.length);
        dest.writeByteArray(this.mType);
        dest.writeInt(this.mId.length);
        dest.writeByteArray(this.mId);
        dest.writeInt(this.mPayload.length);
        dest.writeByteArray(this.mPayload);
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * 1) + Arrays.hashCode(this.mId))) + Arrays.hashCode(this.mPayload))) + this.mTnf)) + Arrays.hashCode(this.mType);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NdefRecord other = (NdefRecord) obj;
        if (Arrays.equals(this.mId, other.mId) && Arrays.equals(this.mPayload, other.mPayload) && this.mTnf == other.mTnf) {
            return Arrays.equals(this.mType, other.mType);
        }
        return false;
    }

    public String toString() {
        StringBuilder b = new StringBuilder(String.format("NdefRecord tnf=%X", new Object[]{Short.valueOf(this.mTnf)}));
        if (this.mType.length > 0) {
            b.append(" type=");
            b.append(bytesToString(this.mType));
        }
        if (this.mId.length > 0) {
            b.append(" id=");
            b.append(bytesToString(this.mId));
        }
        if (this.mPayload.length > 0) {
            b.append(" payload=");
            b.append(bytesToString(this.mPayload));
        }
        return b.toString();
    }

    private static StringBuilder bytesToString(byte[] bs) {
        StringBuilder s = new StringBuilder();
        int length = bs.length;
        for (int i = 0; i < length; i++) {
            s.append(String.format("%02X", new Object[]{Byte.valueOf(bs[i])}));
        }
        return s;
    }
}
