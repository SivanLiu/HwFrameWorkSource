package android.util.proto;

import android.net.wifi.WifiEnterpriseConfig;
import android.opengl.GLES32;
import android.provider.Telephony.BaseMmsColumns;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;
import android.view.KeyEvent;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public final class ProtoOutputStream {
    public static final long FIELD_COUNT_MASK = 16492674416640L;
    public static final long FIELD_COUNT_PACKED = 5497558138880L;
    public static final long FIELD_COUNT_REPEATED = 2199023255552L;
    public static final int FIELD_COUNT_SHIFT = 40;
    public static final long FIELD_COUNT_SINGLE = 1099511627776L;
    public static final long FIELD_COUNT_UNKNOWN = 0;
    public static final int FIELD_ID_MASK = -8;
    public static final int FIELD_ID_SHIFT = 3;
    public static final long FIELD_TYPE_BOOL = 34359738368L;
    public static final long FIELD_TYPE_BYTES = 51539607552L;
    public static final long FIELD_TYPE_DOUBLE = 4294967296L;
    public static final long FIELD_TYPE_ENUM = 60129542144L;
    public static final long FIELD_TYPE_FIXED32 = 30064771072L;
    public static final long FIELD_TYPE_FIXED64 = 25769803776L;
    public static final long FIELD_TYPE_FLOAT = 8589934592L;
    public static final long FIELD_TYPE_INT32 = 21474836480L;
    public static final long FIELD_TYPE_INT64 = 12884901888L;
    public static final long FIELD_TYPE_MASK = 1095216660480L;
    public static final long FIELD_TYPE_MESSAGE = 47244640256L;
    private static final String[] FIELD_TYPE_NAMES = new String[]{"Double", "Float", "Int64", "UInt64", "Int32", "Fixed64", "Fixed32", "Bool", "String", "Group", "Message", "Bytes", "UInt32", "Enum", "SFixed32", "SFixed64", "SInt32", "SInt64"};
    public static final long FIELD_TYPE_SFIXED32 = 64424509440L;
    public static final long FIELD_TYPE_SFIXED64 = 68719476736L;
    public static final int FIELD_TYPE_SHIFT = 32;
    public static final long FIELD_TYPE_SINT32 = 73014444032L;
    public static final long FIELD_TYPE_SINT64 = 77309411328L;
    public static final long FIELD_TYPE_STRING = 38654705664L;
    public static final long FIELD_TYPE_UINT32 = 55834574848L;
    public static final long FIELD_TYPE_UINT64 = 17179869184L;
    public static final long FIELD_TYPE_UNKNOWN = 0;
    public static final String TAG = "ProtoOutputStream";
    public static final int WIRE_TYPE_END_GROUP = 4;
    public static final int WIRE_TYPE_FIXED32 = 5;
    public static final int WIRE_TYPE_FIXED64 = 1;
    public static final int WIRE_TYPE_LENGTH_DELIMITED = 2;
    public static final int WIRE_TYPE_MASK = 7;
    public static final int WIRE_TYPE_START_GROUP = 3;
    public static final int WIRE_TYPE_VARINT = 0;
    private EncodedBuffer mBuffer;
    private boolean mCompacted;
    private int mCopyBegin;
    private int mDepth;
    private long mExpectedObjectToken;
    private int mNextObjectId;
    private OutputStream mStream;

    public ProtoOutputStream() {
        this(0);
    }

    public ProtoOutputStream(int chunkSize) {
        this.mNextObjectId = -1;
        this.mBuffer = new EncodedBuffer(chunkSize);
    }

    public ProtoOutputStream(OutputStream stream) {
        this();
        this.mStream = stream;
    }

    public ProtoOutputStream(FileDescriptor fd) {
        this(new FileOutputStream(fd));
    }

    /* JADX WARNING: Missing block: B:9:0x003f, code skipped:
            writeRepeatedSInt64Impl(r0, (long) r9);
     */
    /* JADX WARNING: Missing block: B:10:0x0045, code skipped:
            writeRepeatedSInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:11:0x004b, code skipped:
            writeRepeatedSFixed64Impl(r0, (long) r9);
     */
    /* JADX WARNING: Missing block: B:12:0x0051, code skipped:
            writeRepeatedSFixed32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:13:0x0057, code skipped:
            writeRepeatedEnumImpl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:14:0x005d, code skipped:
            writeRepeatedUInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:16:0x0065, code skipped:
            if (r9 == 0.0d) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:17:0x0067, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:18:0x0069, code skipped:
            writeRepeatedBoolImpl(r0, r2);
     */
    /* JADX WARNING: Missing block: B:19:0x006e, code skipped:
            writeRepeatedFixed32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:20:0x0074, code skipped:
            writeRepeatedFixed64Impl(r0, (long) r9);
     */
    /* JADX WARNING: Missing block: B:21:0x007a, code skipped:
            writeRepeatedInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:22:0x0080, code skipped:
            writeRepeatedUInt64Impl(r0, (long) r9);
     */
    /* JADX WARNING: Missing block: B:23:0x0086, code skipped:
            writeRepeatedInt64Impl(r0, (long) r9);
     */
    /* JADX WARNING: Missing block: B:24:0x008b, code skipped:
            writeRepeatedFloatImpl(r0, (float) r9);
     */
    /* JADX WARNING: Missing block: B:25:0x0090, code skipped:
            writeRepeatedDoubleImpl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:52:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void write(long fieldId, double val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        boolean z = false;
        switch (i) {
            case 257:
                writeDoubleImpl(id, val);
                return;
            case 258:
                writeFloatImpl(id, (float) val);
                return;
            case 259:
                writeInt64Impl(id, (long) val);
                return;
            case 260:
                writeUInt64Impl(id, (long) val);
                return;
            case 261:
                writeInt32Impl(id, (int) val);
                return;
            case 262:
                writeFixed64Impl(id, (long) val);
                return;
            case 263:
                writeFixed32Impl(id, (int) val);
                return;
            case 264:
                if (val != 0.0d) {
                    z = true;
                }
                writeBoolImpl(id, z);
                return;
            default:
                switch (i) {
                    case 269:
                        writeUInt32Impl(id, (int) val);
                        return;
                    case 270:
                        writeEnumImpl(id, (int) val);
                        return;
                    case 271:
                        writeSFixed32Impl(id, (int) val);
                        return;
                    case 272:
                        writeSFixed64Impl(id, (long) val);
                        return;
                    case 273:
                        writeSInt32Impl(id, (int) val);
                        return;
                    case 274:
                        writeSInt64Impl(id, (long) val);
                        return;
                    default:
                        switch (i) {
                            case 513:
                                break;
                            case 514:
                                break;
                            case 515:
                                break;
                            case 516:
                                break;
                            case 517:
                                break;
                            case 518:
                                break;
                            case 519:
                                break;
                            case ImsReasonInfo.CODE_USER_IGNORE_WITH_CAUSE /*520*/:
                                break;
                            default:
                                switch (i) {
                                    case 525:
                                        break;
                                    case 526:
                                        break;
                                    case 527:
                                        break;
                                    case 528:
                                        break;
                                    case 529:
                                        break;
                                    case 530:
                                        break;
                                    default:
                                        switch (i) {
                                            case 1281:
                                                break;
                                            case 1282:
                                                break;
                                            case 1283:
                                                break;
                                            case 1284:
                                                break;
                                            case 1285:
                                                break;
                                            case 1286:
                                                break;
                                            case GLES32.GL_CONTEXT_LOST /*1287*/:
                                                break;
                                            case 1288:
                                                break;
                                            default:
                                                switch (i) {
                                                    case 1293:
                                                        break;
                                                    case 1294:
                                                        break;
                                                    case 1295:
                                                        break;
                                                    case 1296:
                                                        break;
                                                    case 1297:
                                                        break;
                                                    case 1298:
                                                        break;
                                                    default:
                                                        StringBuilder stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Attempt to call write(long, double) with ");
                                                        stringBuilder.append(getFieldIdString(fieldId));
                                                        throw new IllegalArgumentException(stringBuilder.toString());
                                                }
                                        }
                                }
                        }
                }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x003e, code skipped:
            writeRepeatedSInt64Impl(r0, (long) r8);
     */
    /* JADX WARNING: Missing block: B:10:0x0044, code skipped:
            writeRepeatedSInt32Impl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:11:0x004a, code skipped:
            writeRepeatedSFixed64Impl(r0, (long) r8);
     */
    /* JADX WARNING: Missing block: B:12:0x0050, code skipped:
            writeRepeatedSFixed32Impl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:13:0x0056, code skipped:
            writeRepeatedEnumImpl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:14:0x005c, code skipped:
            writeRepeatedUInt32Impl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:16:0x0064, code skipped:
            if (r8 == 0.0f) goto L_0x0068;
     */
    /* JADX WARNING: Missing block: B:17:0x0066, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:18:0x0068, code skipped:
            writeRepeatedBoolImpl(r0, r2);
     */
    /* JADX WARNING: Missing block: B:19:0x006d, code skipped:
            writeRepeatedFixed32Impl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:20:0x0073, code skipped:
            writeRepeatedFixed64Impl(r0, (long) r8);
     */
    /* JADX WARNING: Missing block: B:21:0x0079, code skipped:
            writeRepeatedInt32Impl(r0, (int) r8);
     */
    /* JADX WARNING: Missing block: B:22:0x007f, code skipped:
            writeRepeatedUInt64Impl(r0, (long) r8);
     */
    /* JADX WARNING: Missing block: B:23:0x0085, code skipped:
            writeRepeatedInt64Impl(r0, (long) r8);
     */
    /* JADX WARNING: Missing block: B:24:0x008a, code skipped:
            writeRepeatedFloatImpl(r0, r8);
     */
    /* JADX WARNING: Missing block: B:25:0x008e, code skipped:
            writeRepeatedDoubleImpl(r0, (double) r8);
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:52:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void write(long fieldId, float val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        boolean z = false;
        switch (i) {
            case 257:
                writeDoubleImpl(id, (double) val);
                return;
            case 258:
                writeFloatImpl(id, val);
                return;
            case 259:
                writeInt64Impl(id, (long) val);
                return;
            case 260:
                writeUInt64Impl(id, (long) val);
                return;
            case 261:
                writeInt32Impl(id, (int) val);
                return;
            case 262:
                writeFixed64Impl(id, (long) val);
                return;
            case 263:
                writeFixed32Impl(id, (int) val);
                return;
            case 264:
                if (val != 0.0f) {
                    z = true;
                }
                writeBoolImpl(id, z);
                return;
            default:
                switch (i) {
                    case 269:
                        writeUInt32Impl(id, (int) val);
                        return;
                    case 270:
                        writeEnumImpl(id, (int) val);
                        return;
                    case 271:
                        writeSFixed32Impl(id, (int) val);
                        return;
                    case 272:
                        writeSFixed64Impl(id, (long) val);
                        return;
                    case 273:
                        writeSInt32Impl(id, (int) val);
                        return;
                    case 274:
                        writeSInt64Impl(id, (long) val);
                        return;
                    default:
                        switch (i) {
                            case 513:
                                break;
                            case 514:
                                break;
                            case 515:
                                break;
                            case 516:
                                break;
                            case 517:
                                break;
                            case 518:
                                break;
                            case 519:
                                break;
                            case ImsReasonInfo.CODE_USER_IGNORE_WITH_CAUSE /*520*/:
                                break;
                            default:
                                switch (i) {
                                    case 525:
                                        break;
                                    case 526:
                                        break;
                                    case 527:
                                        break;
                                    case 528:
                                        break;
                                    case 529:
                                        break;
                                    case 530:
                                        break;
                                    default:
                                        switch (i) {
                                            case 1281:
                                                break;
                                            case 1282:
                                                break;
                                            case 1283:
                                                break;
                                            case 1284:
                                                break;
                                            case 1285:
                                                break;
                                            case 1286:
                                                break;
                                            case GLES32.GL_CONTEXT_LOST /*1287*/:
                                                break;
                                            case 1288:
                                                break;
                                            default:
                                                switch (i) {
                                                    case 1293:
                                                        break;
                                                    case 1294:
                                                        break;
                                                    case 1295:
                                                        break;
                                                    case 1296:
                                                        break;
                                                    case 1297:
                                                        break;
                                                    case 1298:
                                                        break;
                                                    default:
                                                        StringBuilder stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Attempt to call write(long, float) with ");
                                                        stringBuilder.append(getFieldIdString(fieldId));
                                                        throw new IllegalArgumentException(stringBuilder.toString());
                                                }
                                        }
                                }
                        }
                }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x003d, code skipped:
            writeRepeatedSInt64Impl(r0, (long) r7);
     */
    /* JADX WARNING: Missing block: B:10:0x0043, code skipped:
            writeRepeatedSInt32Impl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:11:0x0048, code skipped:
            writeRepeatedSFixed64Impl(r0, (long) r7);
     */
    /* JADX WARNING: Missing block: B:12:0x004e, code skipped:
            writeRepeatedSFixed32Impl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:13:0x0053, code skipped:
            writeRepeatedEnumImpl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:14:0x0058, code skipped:
            writeRepeatedUInt32Impl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:15:0x005d, code skipped:
            if (r7 == 0) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:16:0x005f, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:17:0x0061, code skipped:
            writeRepeatedBoolImpl(r0, r2);
     */
    /* JADX WARNING: Missing block: B:18:0x0066, code skipped:
            writeRepeatedFixed32Impl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:19:0x006b, code skipped:
            writeRepeatedFixed64Impl(r0, (long) r7);
     */
    /* JADX WARNING: Missing block: B:20:0x0071, code skipped:
            writeRepeatedInt32Impl(r0, r7);
     */
    /* JADX WARNING: Missing block: B:21:0x0076, code skipped:
            writeRepeatedUInt64Impl(r0, (long) r7);
     */
    /* JADX WARNING: Missing block: B:22:0x007b, code skipped:
            writeRepeatedInt64Impl(r0, (long) r7);
     */
    /* JADX WARNING: Missing block: B:23:0x0080, code skipped:
            writeRepeatedFloatImpl(r0, (float) r7);
     */
    /* JADX WARNING: Missing block: B:24:0x0085, code skipped:
            writeRepeatedDoubleImpl(r0, (double) r7);
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:43:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:52:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void write(long fieldId, int val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        boolean z = false;
        switch (i) {
            case 257:
                writeDoubleImpl(id, (double) val);
                return;
            case 258:
                writeFloatImpl(id, (float) val);
                return;
            case 259:
                writeInt64Impl(id, (long) val);
                return;
            case 260:
                writeUInt64Impl(id, (long) val);
                return;
            case 261:
                writeInt32Impl(id, val);
                return;
            case 262:
                writeFixed64Impl(id, (long) val);
                return;
            case 263:
                writeFixed32Impl(id, val);
                return;
            case 264:
                if (val != 0) {
                    z = true;
                }
                writeBoolImpl(id, z);
                return;
            default:
                switch (i) {
                    case 269:
                        writeUInt32Impl(id, val);
                        return;
                    case 270:
                        writeEnumImpl(id, val);
                        return;
                    case 271:
                        writeSFixed32Impl(id, val);
                        return;
                    case 272:
                        writeSFixed64Impl(id, (long) val);
                        return;
                    case 273:
                        writeSInt32Impl(id, val);
                        return;
                    case 274:
                        writeSInt64Impl(id, (long) val);
                        return;
                    default:
                        switch (i) {
                            case 513:
                                break;
                            case 514:
                                break;
                            case 515:
                                break;
                            case 516:
                                break;
                            case 517:
                                break;
                            case 518:
                                break;
                            case 519:
                                break;
                            case ImsReasonInfo.CODE_USER_IGNORE_WITH_CAUSE /*520*/:
                                break;
                            default:
                                switch (i) {
                                    case 525:
                                        break;
                                    case 526:
                                        break;
                                    case 527:
                                        break;
                                    case 528:
                                        break;
                                    case 529:
                                        break;
                                    case 530:
                                        break;
                                    default:
                                        switch (i) {
                                            case 1281:
                                                break;
                                            case 1282:
                                                break;
                                            case 1283:
                                                break;
                                            case 1284:
                                                break;
                                            case 1285:
                                                break;
                                            case 1286:
                                                break;
                                            case GLES32.GL_CONTEXT_LOST /*1287*/:
                                                break;
                                            case 1288:
                                                break;
                                            default:
                                                switch (i) {
                                                    case 1293:
                                                        break;
                                                    case 1294:
                                                        break;
                                                    case 1295:
                                                        break;
                                                    case 1296:
                                                        break;
                                                    case 1297:
                                                        break;
                                                    case 1298:
                                                        break;
                                                    default:
                                                        StringBuilder stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Attempt to call write(long, int) with ");
                                                        stringBuilder.append(getFieldIdString(fieldId));
                                                        throw new IllegalArgumentException(stringBuilder.toString());
                                                }
                                        }
                                }
                        }
                }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x003f, code skipped:
            writeRepeatedSInt64Impl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:10:0x0044, code skipped:
            writeRepeatedSInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:11:0x004a, code skipped:
            writeRepeatedSFixed64Impl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:12:0x004f, code skipped:
            writeRepeatedSFixed32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:13:0x0055, code skipped:
            writeRepeatedEnumImpl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:14:0x005b, code skipped:
            writeRepeatedUInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:16:0x0063, code skipped:
            if (r9 == 0) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:17:0x0065, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:18:0x0067, code skipped:
            writeRepeatedBoolImpl(r0, r2);
     */
    /* JADX WARNING: Missing block: B:19:0x006c, code skipped:
            writeRepeatedFixed32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:20:0x0072, code skipped:
            writeRepeatedFixed64Impl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:21:0x0077, code skipped:
            writeRepeatedInt32Impl(r0, (int) r9);
     */
    /* JADX WARNING: Missing block: B:22:0x007d, code skipped:
            writeRepeatedUInt64Impl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:23:0x0081, code skipped:
            writeRepeatedInt64Impl(r0, r9);
     */
    /* JADX WARNING: Missing block: B:24:0x0085, code skipped:
            writeRepeatedFloatImpl(r0, (float) r9);
     */
    /* JADX WARNING: Missing block: B:25:0x008a, code skipped:
            writeRepeatedDoubleImpl(r0, (double) r9);
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:52:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void write(long fieldId, long val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        boolean z = false;
        switch (i) {
            case 257:
                writeDoubleImpl(id, (double) val);
                return;
            case 258:
                writeFloatImpl(id, (float) val);
                return;
            case 259:
                writeInt64Impl(id, val);
                return;
            case 260:
                writeUInt64Impl(id, val);
                return;
            case 261:
                writeInt32Impl(id, (int) val);
                return;
            case 262:
                writeFixed64Impl(id, val);
                return;
            case 263:
                writeFixed32Impl(id, (int) val);
                return;
            case 264:
                if (val != 0) {
                    z = true;
                }
                writeBoolImpl(id, z);
                return;
            default:
                switch (i) {
                    case 269:
                        writeUInt32Impl(id, (int) val);
                        return;
                    case 270:
                        writeEnumImpl(id, (int) val);
                        return;
                    case 271:
                        writeSFixed32Impl(id, (int) val);
                        return;
                    case 272:
                        writeSFixed64Impl(id, val);
                        return;
                    case 273:
                        writeSInt32Impl(id, (int) val);
                        return;
                    case 274:
                        writeSInt64Impl(id, val);
                        return;
                    default:
                        switch (i) {
                            case 513:
                                break;
                            case 514:
                                break;
                            case 515:
                                break;
                            case 516:
                                break;
                            case 517:
                                break;
                            case 518:
                                break;
                            case 519:
                                break;
                            case ImsReasonInfo.CODE_USER_IGNORE_WITH_CAUSE /*520*/:
                                break;
                            default:
                                switch (i) {
                                    case 525:
                                        break;
                                    case 526:
                                        break;
                                    case 527:
                                        break;
                                    case 528:
                                        break;
                                    case 529:
                                        break;
                                    case 530:
                                        break;
                                    default:
                                        switch (i) {
                                            case 1281:
                                                break;
                                            case 1282:
                                                break;
                                            case 1283:
                                                break;
                                            case 1284:
                                                break;
                                            case 1285:
                                                break;
                                            case 1286:
                                                break;
                                            case GLES32.GL_CONTEXT_LOST /*1287*/:
                                                break;
                                            case 1288:
                                                break;
                                            default:
                                                switch (i) {
                                                    case 1293:
                                                        break;
                                                    case 1294:
                                                        break;
                                                    case 1295:
                                                        break;
                                                    case 1296:
                                                        break;
                                                    case 1297:
                                                        break;
                                                    case 1298:
                                                        break;
                                                    default:
                                                        StringBuilder stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Attempt to call write(long, long) with ");
                                                        stringBuilder.append(getFieldIdString(fieldId));
                                                        throw new IllegalArgumentException(stringBuilder.toString());
                                                }
                                        }
                                }
                        }
                }
        }
    }

    public void write(long fieldId, boolean val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        if (i == 264) {
            writeBoolImpl(id, val);
        } else if (i == ImsReasonInfo.CODE_USER_IGNORE_WITH_CAUSE || i == 1288) {
            writeRepeatedBoolImpl(id, val);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempt to call write(long, boolean) with ");
            stringBuilder.append(getFieldIdString(fieldId));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void write(long fieldId, String val) {
        assertNotCompacted();
        int id = (int) fieldId;
        int i = (int) ((17587891077120L & fieldId) >> 32);
        if (i == 265) {
            writeStringImpl(id, val);
        } else if (i == ImsReasonInfo.CODE_USER_DECLINE_WITH_CAUSE || i == 1289) {
            writeRepeatedStringImpl(id, val);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempt to call write(long, String) with ");
            stringBuilder.append(getFieldIdString(fieldId));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void write(long fieldId, byte[] val) {
        assertNotCompacted();
        int id = (int) fieldId;
        switch ((int) ((17587891077120L & fieldId) >> 32)) {
            case 267:
                writeObjectImpl(id, val);
                return;
            case 268:
                writeBytesImpl(id, val);
                return;
            case 523:
            case 1291:
                writeRepeatedObjectImpl(id, val);
                return;
            case 524:
            case 1292:
                writeRepeatedBytesImpl(id, val);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Attempt to call write(long, byte[]) with ");
                stringBuilder.append(getFieldIdString(fieldId));
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public long start(long fieldId) {
        assertNotCompacted();
        int id = (int) fieldId;
        if ((FIELD_TYPE_MASK & fieldId) == FIELD_TYPE_MESSAGE) {
            long count = FIELD_COUNT_MASK & fieldId;
            if (count == FIELD_COUNT_SINGLE) {
                return startObjectImpl(id, false);
            }
            if (count == FIELD_COUNT_REPEATED || count == FIELD_COUNT_PACKED) {
                return startObjectImpl(id, true);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempt to call start(long) with ");
        stringBuilder.append(getFieldIdString(fieldId));
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void end(long token) {
        endObjectImpl(token, getRepeatedFromToken(token));
    }

    @Deprecated
    public void writeDouble(long fieldId, double val) {
        assertNotCompacted();
        writeDoubleImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeDoubleImpl(int id, double val) {
        if (val != 0.0d) {
            writeTag(id, 1);
            this.mBuffer.writeRawFixed64(Double.doubleToLongBits(val));
        }
    }

    @Deprecated
    public void writeRepeatedDouble(long fieldId, double val) {
        assertNotCompacted();
        writeRepeatedDoubleImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedDoubleImpl(int id, double val) {
        writeTag(id, 1);
        this.mBuffer.writeRawFixed64(Double.doubleToLongBits(val));
    }

    @Deprecated
    public void writePackedDouble(long fieldId, double[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 8);
            while (i < N) {
                this.mBuffer.writeRawFixed64(Double.doubleToLongBits(val[i]));
                i++;
            }
        }
    }

    @Deprecated
    public void writeFloat(long fieldId, float val) {
        assertNotCompacted();
        writeFloatImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeFloatImpl(int id, float val) {
        if (val != 0.0f) {
            writeTag(id, 5);
            this.mBuffer.writeRawFixed32(Float.floatToIntBits(val));
        }
    }

    @Deprecated
    public void writeRepeatedFloat(long fieldId, float val) {
        assertNotCompacted();
        writeRepeatedFloatImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedFloatImpl(int id, float val) {
        writeTag(id, 5);
        this.mBuffer.writeRawFixed32(Float.floatToIntBits(val));
    }

    @Deprecated
    public void writePackedFloat(long fieldId, float[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 4);
            while (i < N) {
                this.mBuffer.writeRawFixed32(Float.floatToIntBits(val[i]));
                i++;
            }
        }
    }

    private void writeUnsignedVarintFromSignedInt(int val) {
        if (val >= 0) {
            this.mBuffer.writeRawVarint32(val);
        } else {
            this.mBuffer.writeRawVarint64((long) val);
        }
    }

    @Deprecated
    public void writeInt32(long fieldId, int val) {
        assertNotCompacted();
        writeInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeInt32Impl(int id, int val) {
        if (val != 0) {
            writeTag(id, 0);
            writeUnsignedVarintFromSignedInt(val);
        }
    }

    @Deprecated
    public void writeRepeatedInt32(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedInt32Impl(int id, int val) {
        writeTag(id, 0);
        writeUnsignedVarintFromSignedInt(val);
    }

    @Deprecated
    public void writePackedInt32(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                int v = val[i2];
                size += v >= 0 ? EncodedBuffer.getRawVarint32Size(v) : 10;
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                writeUnsignedVarintFromSignedInt(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeInt64(long fieldId, long val) {
        assertNotCompacted();
        writeInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeInt64Impl(int id, long val) {
        if (val != 0) {
            writeTag(id, 0);
            this.mBuffer.writeRawVarint64(val);
        }
    }

    @Deprecated
    public void writeRepeatedInt64(long fieldId, long val) {
        assertNotCompacted();
        writeRepeatedInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedInt64Impl(int id, long val) {
        writeTag(id, 0);
        this.mBuffer.writeRawVarint64(val);
    }

    @Deprecated
    public void writePackedInt64(long fieldId, long[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                size += EncodedBuffer.getRawVarint64Size(val[i2]);
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                this.mBuffer.writeRawVarint64(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeUInt32(long fieldId, int val) {
        assertNotCompacted();
        writeUInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeUInt32Impl(int id, int val) {
        if (val != 0) {
            writeTag(id, 0);
            this.mBuffer.writeRawVarint32(val);
        }
    }

    @Deprecated
    public void writeRepeatedUInt32(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedUInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedUInt32Impl(int id, int val) {
        writeTag(id, 0);
        this.mBuffer.writeRawVarint32(val);
    }

    @Deprecated
    public void writePackedUInt32(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                size += EncodedBuffer.getRawVarint32Size(val[i2]);
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                this.mBuffer.writeRawVarint32(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeUInt64(long fieldId, long val) {
        assertNotCompacted();
        writeUInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeUInt64Impl(int id, long val) {
        if (val != 0) {
            writeTag(id, 0);
            this.mBuffer.writeRawVarint64(val);
        }
    }

    @Deprecated
    public void writeRepeatedUInt64(long fieldId, long val) {
        assertNotCompacted();
        writeRepeatedUInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedUInt64Impl(int id, long val) {
        writeTag(id, 0);
        this.mBuffer.writeRawVarint64(val);
    }

    @Deprecated
    public void writePackedUInt64(long fieldId, long[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                size += EncodedBuffer.getRawVarint64Size(val[i2]);
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                this.mBuffer.writeRawVarint64(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeSInt32(long fieldId, int val) {
        assertNotCompacted();
        writeSInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeSInt32Impl(int id, int val) {
        if (val != 0) {
            writeTag(id, 0);
            this.mBuffer.writeRawZigZag32(val);
        }
    }

    @Deprecated
    public void writeRepeatedSInt32(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedSInt32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedSInt32Impl(int id, int val) {
        writeTag(id, 0);
        this.mBuffer.writeRawZigZag32(val);
    }

    @Deprecated
    public void writePackedSInt32(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                size += EncodedBuffer.getRawZigZag32Size(val[i2]);
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                this.mBuffer.writeRawZigZag32(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeSInt64(long fieldId, long val) {
        assertNotCompacted();
        writeSInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeSInt64Impl(int id, long val) {
        if (val != 0) {
            writeTag(id, 0);
            this.mBuffer.writeRawZigZag64(val);
        }
    }

    @Deprecated
    public void writeRepeatedSInt64(long fieldId, long val) {
        assertNotCompacted();
        writeRepeatedSInt64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedSInt64Impl(int id, long val) {
        writeTag(id, 0);
        this.mBuffer.writeRawZigZag64(val);
    }

    @Deprecated
    public void writePackedSInt64(long fieldId, long[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                size += EncodedBuffer.getRawZigZag64Size(val[i2]);
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                this.mBuffer.writeRawZigZag64(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeFixed32(long fieldId, int val) {
        assertNotCompacted();
        writeFixed32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeFixed32Impl(int id, int val) {
        if (val != 0) {
            writeTag(id, 5);
            this.mBuffer.writeRawFixed32(val);
        }
    }

    @Deprecated
    public void writeRepeatedFixed32(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedFixed32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedFixed32Impl(int id, int val) {
        writeTag(id, 5);
        this.mBuffer.writeRawFixed32(val);
    }

    @Deprecated
    public void writePackedFixed32(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 4);
            while (i < N) {
                this.mBuffer.writeRawFixed32(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeFixed64(long fieldId, long val) {
        assertNotCompacted();
        writeFixed64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeFixed64Impl(int id, long val) {
        if (val != 0) {
            writeTag(id, 1);
            this.mBuffer.writeRawFixed64(val);
        }
    }

    @Deprecated
    public void writeRepeatedFixed64(long fieldId, long val) {
        assertNotCompacted();
        writeRepeatedFixed64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedFixed64Impl(int id, long val) {
        writeTag(id, 1);
        this.mBuffer.writeRawFixed64(val);
    }

    @Deprecated
    public void writePackedFixed64(long fieldId, long[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 8);
            while (i < N) {
                this.mBuffer.writeRawFixed64(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeSFixed32(long fieldId, int val) {
        assertNotCompacted();
        writeSFixed32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeSFixed32Impl(int id, int val) {
        if (val != 0) {
            writeTag(id, 5);
            this.mBuffer.writeRawFixed32(val);
        }
    }

    @Deprecated
    public void writeRepeatedSFixed32(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedSFixed32Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedSFixed32Impl(int id, int val) {
        writeTag(id, 5);
        this.mBuffer.writeRawFixed32(val);
    }

    @Deprecated
    public void writePackedSFixed32(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 4);
            while (i < N) {
                this.mBuffer.writeRawFixed32(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeSFixed64(long fieldId, long val) {
        assertNotCompacted();
        writeSFixed64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeSFixed64Impl(int id, long val) {
        if (val != 0) {
            writeTag(id, 1);
            this.mBuffer.writeRawFixed64(val);
        }
    }

    @Deprecated
    public void writeRepeatedSFixed64(long fieldId, long val) {
        assertNotCompacted();
        writeRepeatedSFixed64Impl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedSFixed64Impl(int id, long val) {
        writeTag(id, 1);
        this.mBuffer.writeRawFixed64(val);
    }

    @Deprecated
    public void writePackedSFixed64(long fieldId, long[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N * 8);
            while (i < N) {
                this.mBuffer.writeRawFixed64(val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeBool(long fieldId, boolean val) {
        assertNotCompacted();
        writeBoolImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeBoolImpl(int id, boolean val) {
        if (val) {
            writeTag(id, 0);
            this.mBuffer.writeRawByte((byte) 1);
        }
    }

    @Deprecated
    public void writeRepeatedBool(long fieldId, boolean val) {
        assertNotCompacted();
        writeRepeatedBoolImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedBoolImpl(int id, boolean val) {
        writeTag(id, 0);
        this.mBuffer.writeRawByte((byte) val);
    }

    @Deprecated
    public void writePackedBool(long fieldId, boolean[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            writeKnownLengthHeader(id, N);
            while (i < N) {
                this.mBuffer.writeRawByte((byte) val[i]);
                i++;
            }
        }
    }

    @Deprecated
    public void writeString(long fieldId, String val) {
        assertNotCompacted();
        writeStringImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeStringImpl(int id, String val) {
        if (val != null && val.length() > 0) {
            writeUtf8String(id, val);
        }
    }

    @Deprecated
    public void writeRepeatedString(long fieldId, String val) {
        assertNotCompacted();
        writeRepeatedStringImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedStringImpl(int id, String val) {
        if (val == null || val.length() == 0) {
            writeKnownLengthHeader(id, 0);
        } else {
            writeUtf8String(id, val);
        }
    }

    private void writeUtf8String(int id, String val) {
        try {
            byte[] buf = val.getBytes("UTF-8");
            writeKnownLengthHeader(id, buf.length);
            this.mBuffer.writeRawBuffer(buf);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("not possible");
        }
    }

    @Deprecated
    public void writeBytes(long fieldId, byte[] val) {
        assertNotCompacted();
        writeBytesImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeBytesImpl(int id, byte[] val) {
        if (val != null && val.length > 0) {
            writeKnownLengthHeader(id, val.length);
            this.mBuffer.writeRawBuffer(val);
        }
    }

    @Deprecated
    public void writeRepeatedBytes(long fieldId, byte[] val) {
        assertNotCompacted();
        writeRepeatedBytesImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedBytesImpl(int id, byte[] val) {
        writeKnownLengthHeader(id, val == null ? 0 : val.length);
        this.mBuffer.writeRawBuffer(val);
    }

    @Deprecated
    public void writeEnum(long fieldId, int val) {
        assertNotCompacted();
        writeEnumImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeEnumImpl(int id, int val) {
        if (val != 0) {
            writeTag(id, 0);
            writeUnsignedVarintFromSignedInt(val);
        }
    }

    @Deprecated
    public void writeRepeatedEnum(long fieldId, int val) {
        assertNotCompacted();
        writeRepeatedEnumImpl(checkFieldId(fieldId, 0), val);
    }

    private void writeRepeatedEnumImpl(int id, int val) {
        writeTag(id, 0);
        writeUnsignedVarintFromSignedInt(val);
    }

    @Deprecated
    public void writePackedEnum(long fieldId, int[] val) {
        assertNotCompacted();
        int id = checkFieldId(fieldId, 0);
        int i = 0;
        int N = val != null ? val.length : 0;
        if (N > 0) {
            int size = 0;
            for (int i2 = 0; i2 < N; i2++) {
                int v = val[i2];
                size += v >= 0 ? EncodedBuffer.getRawVarint32Size(v) : 10;
            }
            writeKnownLengthHeader(id, size);
            while (i < N) {
                writeUnsignedVarintFromSignedInt(val[i]);
                i++;
            }
        }
    }

    public static long makeToken(int tagSize, boolean repeated, int depth, int objectId, int sizePos) {
        return (((((((long) tagSize) & 7) << 61) | (repeated ? 1152921504606846976L : 0)) | ((511 & ((long) depth)) << 51)) | ((524287 & ((long) objectId)) << 32)) | (4294967295L & ((long) sizePos));
    }

    public static int getTagSizeFromToken(long token) {
        return (int) ((token >> 61) & 7);
    }

    public static boolean getRepeatedFromToken(long token) {
        return ((token >> 60) & 1) != 0;
    }

    public static int getDepthFromToken(long token) {
        return (int) ((token >> 51) & 511);
    }

    public static int getObjectIdFromToken(long token) {
        return (int) ((token >> 32) & 524287);
    }

    public static int getSizePosFromToken(long token) {
        return (int) token;
    }

    public static int convertObjectIdToOrdinal(int objectId) {
        return 524287 - objectId;
    }

    public static String token2String(long token) {
        if (token == 0) {
            return "Token(0)";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Token(val=0x");
        stringBuilder.append(Long.toHexString(token));
        stringBuilder.append(" depth=");
        stringBuilder.append(getDepthFromToken(token));
        stringBuilder.append(" object=");
        stringBuilder.append(convertObjectIdToOrdinal(getObjectIdFromToken(token)));
        stringBuilder.append(" tagSize=");
        stringBuilder.append(getTagSizeFromToken(token));
        stringBuilder.append(" sizePos=");
        stringBuilder.append(getSizePosFromToken(token));
        stringBuilder.append(')');
        return stringBuilder.toString();
    }

    @Deprecated
    public long startObject(long fieldId) {
        assertNotCompacted();
        return startObjectImpl(checkFieldId(fieldId, 0), false);
    }

    @Deprecated
    public void endObject(long token) {
        assertNotCompacted();
        endObjectImpl(token, false);
    }

    @Deprecated
    public long startRepeatedObject(long fieldId) {
        assertNotCompacted();
        return startObjectImpl(checkFieldId(fieldId, 0), true);
    }

    @Deprecated
    public void endRepeatedObject(long token) {
        assertNotCompacted();
        endObjectImpl(token, true);
    }

    private long startObjectImpl(int id, boolean repeated) {
        writeTag(id, 2);
        int sizePos = this.mBuffer.getWritePos();
        this.mDepth++;
        this.mNextObjectId--;
        this.mBuffer.writeRawFixed32((int) (this.mExpectedObjectToken >> 32));
        this.mBuffer.writeRawFixed32((int) this.mExpectedObjectToken);
        long old = this.mExpectedObjectToken;
        this.mExpectedObjectToken = makeToken(getTagSize(id), repeated, this.mDepth, this.mNextObjectId, sizePos);
        return this.mExpectedObjectToken;
    }

    private void endObjectImpl(long token, boolean repeated) {
        int depth = getDepthFromToken(token);
        boolean expectedRepeated = getRepeatedFromToken(token);
        int sizePos = getSizePosFromToken(token);
        int childRawSize = (this.mBuffer.getWritePos() - sizePos) - 8;
        if (repeated != expectedRepeated) {
            if (repeated) {
                throw new IllegalArgumentException("endRepeatedObject called where endObject should have been");
            }
            throw new IllegalArgumentException("endObject called where endRepeatedObject should have been");
        } else if ((this.mDepth & KeyEvent.KEYCODE_FINGERPRINT_UP) == depth && this.mExpectedObjectToken == token) {
            this.mExpectedObjectToken = (((long) this.mBuffer.getRawFixed32At(sizePos)) << 32) | (4294967295L & ((long) this.mBuffer.getRawFixed32At(sizePos + 4)));
            this.mDepth--;
            if (childRawSize > 0) {
                this.mBuffer.editRawFixed32(sizePos, -childRawSize);
                this.mBuffer.editRawFixed32(sizePos + 4, -1);
            } else if (repeated) {
                this.mBuffer.editRawFixed32(sizePos, 0);
                this.mBuffer.editRawFixed32(sizePos + 4, 0);
            } else {
                this.mBuffer.rewindWriteTo(sizePos - getTagSizeFromToken(token));
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Mismatched startObject/endObject calls. Current depth ");
            stringBuilder.append(this.mDepth);
            stringBuilder.append(" token=");
            stringBuilder.append(token2String(token));
            stringBuilder.append(" expectedToken=");
            stringBuilder.append(token2String(this.mExpectedObjectToken));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @Deprecated
    public void writeObject(long fieldId, byte[] value) {
        assertNotCompacted();
        writeObjectImpl(checkFieldId(fieldId, 0), value);
    }

    void writeObjectImpl(int id, byte[] value) {
        if (value != null && value.length != 0) {
            writeKnownLengthHeader(id, value.length);
            this.mBuffer.writeRawBuffer(value);
        }
    }

    @Deprecated
    public void writeRepeatedObject(long fieldId, byte[] value) {
        assertNotCompacted();
        writeRepeatedObjectImpl(checkFieldId(fieldId, 0), value);
    }

    void writeRepeatedObjectImpl(int id, byte[] value) {
        writeKnownLengthHeader(id, value == null ? 0 : value.length);
        this.mBuffer.writeRawBuffer(value);
    }

    public static long makeFieldId(int id, long fieldFlags) {
        return (((long) id) & 4294967295L) | fieldFlags;
    }

    public static int checkFieldId(long fieldId, long expectedFlags) {
        long j = fieldId;
        long fieldCount = j & FIELD_COUNT_MASK;
        long fieldType = j & FIELD_TYPE_MASK;
        long expectedCount = expectedFlags & FIELD_COUNT_MASK;
        long expectedType = expectedFlags & FIELD_TYPE_MASK;
        if (((int) j) == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid proto field ");
            stringBuilder.append((int) j);
            stringBuilder.append(" fieldId=");
            stringBuilder.append(Long.toHexString(fieldId));
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (fieldType == expectedType && (fieldCount == expectedCount || (fieldCount == FIELD_COUNT_PACKED && expectedCount == FIELD_COUNT_REPEATED))) {
            return (int) j;
        } else {
            String countString = getFieldCountString(fieldCount);
            String typeString = getFieldTypeString(fieldType);
            if (typeString == null || countString == null) {
                StringBuilder sb = new StringBuilder();
                if (expectedType == FIELD_TYPE_MESSAGE) {
                    sb.append(BaseMmsColumns.START);
                } else {
                    sb.append("write");
                }
                sb.append(getFieldCountString(expectedCount));
                sb.append(getFieldTypeString(expectedType));
                sb.append(" called with an invalid fieldId: 0x");
                sb.append(Long.toHexString(fieldId));
                sb.append(". The proto field ID might be ");
                sb.append((int) j);
                sb.append('.');
                throw new IllegalArgumentException(sb.toString());
            }
            StringBuilder sb2 = new StringBuilder();
            if (expectedType == FIELD_TYPE_MESSAGE) {
                sb2.append(BaseMmsColumns.START);
            } else {
                sb2.append("write");
            }
            sb2.append(getFieldCountString(expectedCount));
            sb2.append(getFieldTypeString(expectedType));
            sb2.append(" called for field ");
            sb2.append((int) j);
            sb2.append(" which should be used with ");
            if (fieldType == FIELD_TYPE_MESSAGE) {
                sb2.append(BaseMmsColumns.START);
            } else {
                sb2.append("write");
            }
            sb2.append(countString);
            sb2.append(typeString);
            if (fieldCount == FIELD_COUNT_PACKED) {
                sb2.append(" or writeRepeated");
                sb2.append(typeString);
            }
            sb2.append('.');
            throw new IllegalArgumentException(sb2.toString());
        }
    }

    private static String getFieldTypeString(long fieldType) {
        int index = ((int) ((FIELD_TYPE_MASK & fieldType) >>> 32)) - 1;
        if (index < 0 || index >= FIELD_TYPE_NAMES.length) {
            return null;
        }
        return FIELD_TYPE_NAMES[index];
    }

    private static String getFieldCountString(long fieldCount) {
        if (fieldCount == FIELD_COUNT_SINGLE) {
            return "";
        }
        if (fieldCount == FIELD_COUNT_REPEATED) {
            return "Repeated";
        }
        if (fieldCount == FIELD_COUNT_PACKED) {
            return "Packed";
        }
        return null;
    }

    private String getFieldIdString(long fieldId) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        long fieldCount = FIELD_COUNT_MASK & fieldId;
        String countString = getFieldCountString(fieldCount);
        if (countString == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("fieldCount=");
            stringBuilder.append(fieldCount);
            countString = stringBuilder.toString();
        }
        if (countString.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(countString);
            stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            countString = stringBuilder.toString();
        }
        long fieldType = FIELD_TYPE_MASK & fieldId;
        String typeString = getFieldTypeString(fieldType);
        if (typeString == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("fieldType=");
            stringBuilder2.append(fieldType);
            typeString = stringBuilder2.toString();
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(countString);
        stringBuilder2.append(typeString);
        stringBuilder2.append(" tag=");
        stringBuilder2.append((int) fieldId);
        stringBuilder2.append(" fieldId=0x");
        stringBuilder2.append(Long.toHexString(fieldId));
        return stringBuilder2.toString();
    }

    private static int getTagSize(int id) {
        return EncodedBuffer.getRawVarint32Size(id << 3);
    }

    public void writeTag(int id, int wireType) {
        this.mBuffer.writeRawVarint32((id << 3) | wireType);
    }

    private void writeKnownLengthHeader(int id, int size) {
        writeTag(id, 2);
        this.mBuffer.writeRawFixed32(size);
        this.mBuffer.writeRawFixed32(size);
    }

    private void assertNotCompacted() {
        if (this.mCompacted) {
            throw new IllegalArgumentException("write called after compact");
        }
    }

    public byte[] getBytes() {
        compactIfNecessary();
        return this.mBuffer.getBytes(this.mBuffer.getReadableSize());
    }

    private void compactIfNecessary() {
        if (!this.mCompacted) {
            if (this.mDepth == 0) {
                this.mBuffer.startEditing();
                int readableSize = this.mBuffer.getReadableSize();
                editEncodedSize(readableSize);
                this.mBuffer.rewindRead();
                compactSizes(readableSize);
                if (this.mCopyBegin < readableSize) {
                    this.mBuffer.writeFromThisBuffer(this.mCopyBegin, readableSize - this.mCopyBegin);
                }
                this.mBuffer.startEditing();
                this.mCompacted = true;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to compact with ");
            stringBuilder.append(this.mDepth);
            stringBuilder.append(" missing calls to endObject");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:20:0x00da, code skipped:
            if ((r12.mBuffer.readRawByte() & 128) == 0) goto L_0x00df;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int editEncodedSize(int rawSize) {
        int objectEnd = this.mBuffer.getReadPos() + rawSize;
        int encodedSize = 0;
        while (true) {
            int readPos = this.mBuffer.getReadPos();
            int tagPos = readPos;
            if (readPos >= objectEnd) {
                return encodedSize;
            }
            readPos = readRawTag();
            encodedSize += EncodedBuffer.getRawVarint32Size(readPos);
            int wireType = readPos & 7;
            StringBuilder stringBuilder;
            switch (wireType) {
                case 0:
                    while (true) {
                        encodedSize++;
                        break;
                    }
                case 1:
                    encodedSize += 8;
                    this.mBuffer.skipRead(8);
                    break;
                case 2:
                    int childRawSize = this.mBuffer.readRawFixed32();
                    int childEncodedSizePos = this.mBuffer.getReadPos();
                    int childEncodedSize = this.mBuffer.readRawFixed32();
                    if (childRawSize < 0) {
                        childEncodedSize = editEncodedSize(-childRawSize);
                        this.mBuffer.editRawFixed32(childEncodedSizePos, childEncodedSize);
                    } else if (childEncodedSize == childRawSize) {
                        this.mBuffer.skipRead(childRawSize);
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pre-computed size where the precomputed size and the raw size in the buffer don't match! childRawSize=");
                        stringBuilder2.append(childRawSize);
                        stringBuilder2.append(" childEncodedSize=");
                        stringBuilder2.append(childEncodedSize);
                        stringBuilder2.append(" childEncodedSizePos=");
                        stringBuilder2.append(childEncodedSizePos);
                        throw new RuntimeException(stringBuilder2.toString());
                    }
                    encodedSize += EncodedBuffer.getRawVarint32Size(childEncodedSize) + childEncodedSize;
                    break;
                case 3:
                case 4:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("groups not supported at index ");
                    stringBuilder.append(tagPos);
                    throw new RuntimeException(stringBuilder.toString());
                case 5:
                    encodedSize += 4;
                    this.mBuffer.skipRead(4);
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("editEncodedSize Bad tag tag=0x");
                    stringBuilder.append(Integer.toHexString(readPos));
                    stringBuilder.append(" wireType=");
                    stringBuilder.append(wireType);
                    stringBuilder.append(" -- ");
                    stringBuilder.append(this.mBuffer.getDebugString());
                    throw new ProtoParseException(stringBuilder.toString());
            }
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void compactSizes(int rawSize) {
        int objectEnd = this.mBuffer.getReadPos() + rawSize;
        while (true) {
            int readPos = this.mBuffer.getReadPos();
            int tagPos = readPos;
            if (readPos < objectEnd) {
                readPos = readRawTag();
                int wireType = readPos & 7;
                StringBuilder stringBuilder;
                switch (wireType) {
                    case 0:
                        while ((this.mBuffer.readRawByte() & 128) != 0) {
                        }
                        break;
                    case 1:
                        this.mBuffer.skipRead(8);
                        break;
                    case 2:
                        this.mBuffer.writeFromThisBuffer(this.mCopyBegin, this.mBuffer.getReadPos() - this.mCopyBegin);
                        int childRawSize = this.mBuffer.readRawFixed32();
                        int childEncodedSize = this.mBuffer.readRawFixed32();
                        this.mBuffer.writeRawVarint32(childEncodedSize);
                        this.mCopyBegin = this.mBuffer.getReadPos();
                        if (childRawSize < 0) {
                            compactSizes(-childRawSize);
                            break;
                        } else {
                            this.mBuffer.skipRead(childEncodedSize);
                            break;
                        }
                    case 3:
                    case 4:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("groups not supported at index ");
                        stringBuilder.append(tagPos);
                        throw new RuntimeException(stringBuilder.toString());
                    case 5:
                        this.mBuffer.skipRead(4);
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("compactSizes Bad tag tag=0x");
                        stringBuilder.append(Integer.toHexString(readPos));
                        stringBuilder.append(" wireType=");
                        stringBuilder.append(wireType);
                        stringBuilder.append(" -- ");
                        stringBuilder.append(this.mBuffer.getDebugString());
                        throw new ProtoParseException(stringBuilder.toString());
                }
            }
            return;
        }
    }

    public void flush() {
        if (this.mStream != null && this.mDepth == 0 && !this.mCompacted) {
            compactIfNecessary();
            try {
                this.mStream.write(this.mBuffer.getBytes(this.mBuffer.getReadableSize()));
                this.mStream.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Error flushing proto to stream", ex);
            }
        }
    }

    private int readRawTag() {
        if (this.mBuffer.getReadPos() == this.mBuffer.getReadableSize()) {
            return 0;
        }
        return (int) this.mBuffer.readRawUnsigned();
    }

    public void dump(String tag) {
        Log.d(tag, this.mBuffer.getDebugString());
        this.mBuffer.dumpBuffers(tag);
    }
}
