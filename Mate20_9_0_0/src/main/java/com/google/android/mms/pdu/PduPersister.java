package com.google.android.mms.pdu;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.drm.DrmManagerClient;
import android.hardware.radio.V1_0.RadioAccessFamily;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Settings.Secure;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Draft;
import android.provider.Telephony.Mms.Inbox;
import android.provider.Telephony.Mms.Outbox;
import android.provider.Telephony.Mms.Sent;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.HbpcdLookup;
import com.android.mms.pdu.HwCustPduPersister;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.DownloadDrmHelper;
import com.google.android.mms.util.DrmConvertSession;
import com.google.android.mms.util.PduCache;
import com.google.android.mms.util.PduCacheEntry;
import com.google.android.mms.util.SqliteWrapper;
import huawei.cust.HwCustUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

public class PduPersister {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int[] ADDRESS_FIELDS = new int[]{129, 130, 137, 151};
    private static final HashMap<Integer, Integer> CHARSET_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> CHARSET_COLUMN_NAME_MAP = new HashMap();
    private static final boolean DEBUG = false;
    private static final int DEFAULT_SUBSCRIPTION = 0;
    private static final long DUMMY_THREAD_ID = Long.MAX_VALUE;
    private static final HashMap<Integer, Integer> ENCODED_STRING_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> ENCODED_STRING_COLUMN_NAME_MAP = new HashMap();
    public static final int LOAD_MODE_MMS_COMMON = 0;
    public static final int LOAD_MODE_MMS_FAVORITES = 1;
    private static final boolean LOCAL_LOGV = false;
    public static final String LOCAL_NUMBER_FROM_DB = "localNumberFromDb";
    private static final HashMap<Integer, Integer> LONG_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> LONG_COLUMN_NAME_MAP = new HashMap();
    private static final HashMap<Uri, Integer> MESSAGE_BOX_MAP = new HashMap();
    private static final HashMap<Integer, Integer> OCTET_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> OCTET_COLUMN_NAME_MAP = new HashMap();
    private static final int PART_COLUMN_CHARSET = 1;
    private static final int PART_COLUMN_CONTENT_DISPOSITION = 2;
    private static final int PART_COLUMN_CONTENT_ID = 3;
    private static final int PART_COLUMN_CONTENT_LOCATION = 4;
    private static final int PART_COLUMN_CONTENT_TYPE = 5;
    private static final int PART_COLUMN_FILENAME = 6;
    private static final int PART_COLUMN_ID = 0;
    private static final int PART_COLUMN_NAME = 7;
    private static final int PART_COLUMN_TEXT = 8;
    private static final String[] PART_PROJECTION = new String[]{HbpcdLookup.ID, "chset", "cd", "cid", "cl", "ct", "fn", "name", "text"};
    private static final PduCache PDU_CACHE_INSTANCE = PduCache.getInstance();
    private static final int PDU_COLUMN_CONTENT_CLASS = 11;
    private static final int PDU_COLUMN_CONTENT_LOCATION = 5;
    private static final int PDU_COLUMN_CONTENT_TYPE = 6;
    private static final int PDU_COLUMN_DATE = 21;
    private static final int PDU_COLUMN_DELIVERY_REPORT = 12;
    private static final int PDU_COLUMN_DELIVERY_TIME = 22;
    private static final int PDU_COLUMN_EXPIRY = 23;
    private static final int PDU_COLUMN_ID = 0;
    private static final int PDU_COLUMN_MESSAGE_BOX = 1;
    private static final int PDU_COLUMN_MESSAGE_CLASS = 7;
    private static final int PDU_COLUMN_MESSAGE_ID = 8;
    private static final int PDU_COLUMN_MESSAGE_SIZE = 24;
    private static final int PDU_COLUMN_MESSAGE_TYPE = 13;
    private static final int PDU_COLUMN_MMS_VERSION = 14;
    private static final int PDU_COLUMN_PRIORITY = 15;
    private static final int PDU_COLUMN_READ_REPORT = 16;
    private static final int PDU_COLUMN_READ_STATUS = 17;
    private static final int PDU_COLUMN_REPORT_ALLOWED = 18;
    private static final int PDU_COLUMN_RESPONSE_TEXT = 9;
    private static final int PDU_COLUMN_RETRIEVE_STATUS = 19;
    private static final int PDU_COLUMN_RETRIEVE_TEXT = 3;
    private static final int PDU_COLUMN_RETRIEVE_TEXT_CHARSET = 26;
    private static final int PDU_COLUMN_STATUS = 20;
    private static final int PDU_COLUMN_SUBJECT = 4;
    private static final int PDU_COLUMN_SUBJECT_CHARSET = 25;
    private static final int PDU_COLUMN_THREAD_ID = 2;
    private static final int PDU_COLUMN_TRANSACTION_ID = 10;
    private static final String[] PDU_PROJECTION = new String[]{HbpcdLookup.ID, "msg_box", "thread_id", "retr_txt", "sub", "ct_l", "ct_t", "m_cls", "m_id", "resp_txt", "tr_id", "ct_cls", "d_rpt", "m_type", "v", "pri", "rr", "read_status", "rpt_a", "retr_st", "st", "date", "d_tm", "exp", "m_size", "sub_cs", "retr_txt_cs"};
    public static final int PROC_STATUS_COMPLETED = 3;
    public static final int PROC_STATUS_PERMANENTLY_FAILURE = 2;
    public static final int PROC_STATUS_TRANSIENT_FAILURE = 1;
    private static final String TAG = "PduPersister";
    public static final String TEMPORARY_DRM_OBJECT_URI = "content://mms/9223372036854775807/part";
    private static final HashMap<Integer, Integer> TEXT_STRING_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> TEXT_STRING_COLUMN_NAME_MAP = new HashMap();
    private static PduPersister sPersister;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final DrmManagerClient mDrmManagerClient;
    HwCustPduPersister mHwCustPduPersister = ((HwCustPduPersister) HwCustUtils.createObj(HwCustPduPersister.class, new Object[0]));
    private final TelephonyManager mTelephonyManager;

    static {
        MESSAGE_BOX_MAP.put(Inbox.CONTENT_URI, Integer.valueOf(1));
        MESSAGE_BOX_MAP.put(Sent.CONTENT_URI, Integer.valueOf(2));
        MESSAGE_BOX_MAP.put(Draft.CONTENT_URI, Integer.valueOf(3));
        MESSAGE_BOX_MAP.put(Outbox.CONTENT_URI, Integer.valueOf(4));
        CHARSET_COLUMN_INDEX_MAP.put(Integer.valueOf(150), Integer.valueOf(25));
        CHARSET_COLUMN_INDEX_MAP.put(Integer.valueOf(154), Integer.valueOf(26));
        CHARSET_COLUMN_NAME_MAP.put(Integer.valueOf(150), "sub_cs");
        CHARSET_COLUMN_NAME_MAP.put(Integer.valueOf(154), "retr_txt_cs");
        ENCODED_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(154), Integer.valueOf(3));
        ENCODED_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(150), Integer.valueOf(4));
        ENCODED_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(154), "retr_txt");
        ENCODED_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(150), "sub");
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(131), Integer.valueOf(5));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(132), Integer.valueOf(6));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(138), Integer.valueOf(7));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(139), Integer.valueOf(8));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(147), Integer.valueOf(9));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(152), Integer.valueOf(10));
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(131), "ct_l");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(132), "ct_t");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(138), "m_cls");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(139), "m_id");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(147), "resp_txt");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(152), "tr_id");
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), Integer.valueOf(11));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(134), Integer.valueOf(12));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(140), Integer.valueOf(13));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(141), Integer.valueOf(14));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(143), Integer.valueOf(15));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(144), Integer.valueOf(16));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(155), Integer.valueOf(17));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(145), Integer.valueOf(18));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(153), Integer.valueOf(19));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(149), Integer.valueOf(20));
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), "ct_cls");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(134), "d_rpt");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(140), "m_type");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(141), "v");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(143), "pri");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(144), "rr");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(155), "read_status");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(145), "rpt_a");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(153), "retr_st");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(149), "st");
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(133), Integer.valueOf(21));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(135), Integer.valueOf(22));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(136), Integer.valueOf(23));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(142), Integer.valueOf(24));
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(133), "date");
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(135), "d_tm");
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(136), "exp");
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(142), "m_size");
    }

    private PduPersister(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mDrmManagerClient = new DrmManagerClient(context);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public static PduPersister getPduPersister(Context context) {
        ActivityThread actThread = ActivityThread.currentActivityThread();
        Context priorContext = actThread != null ? actThread.getApplication() : null;
        if (priorContext == null) {
            priorContext = context.getApplicationContext();
        }
        if (sPersister == null) {
            sPersister = new PduPersister(priorContext);
        } else if (!priorContext.equals(sPersister.mContext)) {
            Log.w(TAG, "PduPersister create a new one. may cause memory leak");
            sPersister.release();
            sPersister = new PduPersister(priorContext);
        }
        return sPersister;
    }

    private void setEncodedStringValueToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s != null && s.length() > 0) {
            headers.setEncodedStringValue(new EncodedStringValue(c.getInt(((Integer) CHARSET_COLUMN_INDEX_MAP.get(Integer.valueOf(mapColumn))).intValue()), getBytes(s)), mapColumn);
        }
    }

    private void setTextStringToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s != null) {
            headers.setTextString(getBytes(s), mapColumn);
        }
    }

    private void setOctetToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) throws InvalidHeaderValueException {
        if (!c.isNull(columnIndex)) {
            headers.setOctet(c.getInt(columnIndex), mapColumn);
        }
    }

    private void setLongToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        if (!c.isNull(columnIndex)) {
            headers.setLongInteger(c.getLong(columnIndex), mapColumn);
        }
    }

    private Integer getIntegerFromPartColumn(Cursor c, int columnIndex) {
        if (c.isNull(columnIndex)) {
            return null;
        }
        return Integer.valueOf(c.getInt(columnIndex));
    }

    private byte[] getByteArrayFromPartColumn(Cursor c, int columnIndex) {
        if (c.isNull(columnIndex)) {
            return null;
        }
        return getBytes(c.getString(columnIndex));
    }

    /* JADX WARNING: Removed duplicated region for block: B:103:0x01f8 A:{Catch:{ IOException -> 0x0194, all -> 0x02aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01e6 A:{SYNTHETIC, Splitter:B:96:0x01e6} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01e6 A:{SYNTHETIC, Splitter:B:96:0x01e6} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x01f8 A:{Catch:{ IOException -> 0x0194, all -> 0x02aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x01f8 A:{Catch:{ IOException -> 0x0194, all -> 0x02aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01e6 A:{SYNTHETIC, Splitter:B:96:0x01e6} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01e6 A:{SYNTHETIC, Splitter:B:96:0x01e6} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x01f8 A:{Catch:{ IOException -> 0x0194, all -> 0x02aa }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private PduPart[] loadParts(long msgId, int loadType) throws MmsException {
        Cursor c;
        Throwable e;
        Throwable th;
        PduPersister pduPersister = this;
        long j = msgId;
        byte[] bArr = loadType;
        int i = 1;
        Context context;
        ContentResolver contentResolver;
        StringBuilder stringBuilder;
        if (bArr == 1) {
            context = pduPersister.mContext;
            contentResolver = pduPersister.mContentResolver;
            stringBuilder = new StringBuilder();
            stringBuilder.append("content://fav-mms/");
            stringBuilder.append(j);
            stringBuilder.append("/part");
            c = SqliteWrapper.query(context, contentResolver, Uri.parse(stringBuilder.toString()), PART_PROJECTION, null, null, null);
        } else {
            context = pduPersister.mContext;
            contentResolver = pduPersister.mContentResolver;
            stringBuilder = new StringBuilder();
            stringBuilder.append("content://mms/");
            stringBuilder.append(j);
            stringBuilder.append("/part");
            c = SqliteWrapper.query(context, contentResolver, Uri.parse(stringBuilder.toString()), PART_PROJECTION, null, null, null);
        }
        Cursor c2 = c;
        PduPart[] parts = null;
        if (c2 != null) {
            try {
                if (c2.getCount() != 0) {
                    parts = new PduPart[c2.getCount()];
                    int partIdx = 0;
                    while (c2.moveToNext()) {
                        PduPart part = new PduPart();
                        Integer charset = pduPersister.getIntegerFromPartColumn(c2, i);
                        if (charset != null) {
                            part.setCharset(charset.intValue());
                        }
                        byte[] contentDisposition = pduPersister.getByteArrayFromPartColumn(c2, 2);
                        if (contentDisposition != null) {
                            part.setContentDisposition(contentDisposition);
                        }
                        byte[] contentId = pduPersister.getByteArrayFromPartColumn(c2, 3);
                        if (contentId != null) {
                            part.setContentId(contentId);
                        }
                        byte[] contentLocation = pduPersister.getByteArrayFromPartColumn(c2, 4);
                        if (contentLocation != null) {
                            part.setContentLocation(contentLocation);
                        }
                        byte[] contentType = pduPersister.getByteArrayFromPartColumn(c2, 5);
                        if (contentType != null) {
                            Uri partURI;
                            part.setContentType(contentType);
                            byte[] fileName = pduPersister.getByteArrayFromPartColumn(c2, 6);
                            if (fileName != null) {
                                part.setFilename(fileName);
                            }
                            byte[] name = pduPersister.getByteArrayFromPartColumn(c2, 7);
                            if (name != null) {
                                part.setName(name);
                            }
                            long partId = c2.getLong(0);
                            Uri partURI2;
                            long partId2;
                            if (bArr == 1) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("content://fav-mms/part/");
                                partURI2 = null;
                                partId2 = partId;
                                stringBuilder2.append(partId2);
                                partURI = Uri.parse(stringBuilder2.toString());
                            } else {
                                partURI2 = null;
                                partId2 = partId;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("content://mms/part/");
                                stringBuilder3.append(partId2);
                                partURI = Uri.parse(stringBuilder3.toString());
                            }
                            Uri partURI3 = partURI;
                            part.setDataUri(partURI3);
                            String type = toIsoString(contentType);
                            String str;
                            if (ContentType.isImageType(type) || ContentType.isAudioType(type) || ContentType.isVideoType(type)) {
                                str = type;
                            } else {
                                byte[] buffer;
                                InputStream is;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                InputStream is2 = null;
                                Uri uri;
                                if (ContentType.TEXT_PLAIN.equals(type) || ContentType.APP_SMIL.equals(type)) {
                                    str = type;
                                } else if (ContentType.TEXT_HTML.equals(type)) {
                                    uri = partURI3;
                                    str = type;
                                } else {
                                    try {
                                        InputStream is3 = pduPersister.mContentResolver.openInputStream(partURI3);
                                        try {
                                            buffer = new byte[256];
                                            is = is3;
                                            try {
                                                int len = is.read(buffer);
                                                while (true) {
                                                    uri = partURI3;
                                                    partURI3 = len;
                                                    if (partURI3 < null) {
                                                        break;
                                                    }
                                                    str = type;
                                                    try {
                                                        baos.write(buffer, null, partURI3);
                                                        len = is.read(buffer);
                                                        partURI3 = uri;
                                                        type = str;
                                                    } catch (IOException e2) {
                                                        e = e2;
                                                        is2 = is;
                                                        try {
                                                            Log.e(TAG, "Failed to load part data", e);
                                                            c2.close();
                                                            throw new MmsException(e);
                                                        } catch (Throwable e3) {
                                                            th = e3;
                                                            partURI3 = is2;
                                                            if (partURI3 != null) {
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable e32) {
                                                        partURI3 = is;
                                                        th = e32;
                                                        if (partURI3 != null) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                if (is != null) {
                                                    is.close();
                                                }
                                                part.setData(baos.toByteArray());
                                            } catch (IOException e4) {
                                                e32 = e4;
                                                uri = partURI3;
                                                str = type;
                                                is2 = is;
                                                Log.e(TAG, "Failed to load part data", e32);
                                                c2.close();
                                                throw new MmsException(e32);
                                            } catch (Throwable e322) {
                                                uri = partURI3;
                                                str = type;
                                                partURI3 = is;
                                                th = e322;
                                                if (partURI3 != null) {
                                                }
                                                throw th;
                                            }
                                        } catch (IOException e5) {
                                            e322 = e5;
                                            uri = partURI3;
                                            str = type;
                                            is2 = is3;
                                            Log.e(TAG, "Failed to load part data", e322);
                                            c2.close();
                                            throw new MmsException(e322);
                                        } catch (Throwable e3222) {
                                            uri = partURI3;
                                            str = type;
                                            partURI3 = is3;
                                            th = e3222;
                                            if (partURI3 != null) {
                                            }
                                            throw th;
                                        }
                                    } catch (IOException e6) {
                                        e3222 = e6;
                                        uri = partURI3;
                                        str = type;
                                        Log.e(TAG, "Failed to load part data", e3222);
                                        c2.close();
                                        throw new MmsException(e3222);
                                    } catch (Throwable e32222) {
                                        uri = partURI3;
                                        str = type;
                                        th = e32222;
                                        partURI3 = null;
                                        Uri uri2;
                                        if (partURI3 != null) {
                                            try {
                                                partURI3.close();
                                                uri2 = partURI3;
                                            } catch (IOException e7) {
                                                IOException iOException = e7;
                                                uri2 = partURI3;
                                                Log.e(TAG, "Failed to close stream", e7);
                                            }
                                        } else {
                                            uri2 = partURI3;
                                        }
                                        throw th;
                                    }
                                }
                                String text = c2.getString(8);
                                if (charset != null && charset.intValue() == 3) {
                                    charset = Integer.valueOf(106);
                                }
                                if (!"true".equals("true")) {
                                    buffer = new EncodedStringValue(text != null ? text : "").getTextString();
                                    baos.write(buffer, 0, buffer.length);
                                } else if (charset == null || charset.intValue() == 0) {
                                    buffer = new EncodedStringValue(text != null ? text : "").getTextString();
                                    baos.write(buffer, 0, buffer.length);
                                } else {
                                    try {
                                        EncodedStringValue v = new EncodedStringValue(charset.intValue(), text != null ? text : "");
                                        partURI3 = v.getTextString();
                                        EncodedStringValue encodedStringValue = v;
                                        baos.write(partURI3, null, partURI3.length);
                                    } catch (NullPointerException e8) {
                                        Log.e(TAG, "Failed to EncodedStringValue: ", e8);
                                    } catch (Exception e9) {
                                        Log.e(TAG, "Failed to EncodedStringValue: ", e9);
                                    }
                                }
                                is = null;
                                part.setData(baos.toByteArray());
                            }
                            int partIdx2 = partIdx + 1;
                            parts[partIdx] = part;
                            partIdx = partIdx2;
                            pduPersister = this;
                            j = msgId;
                            bArr = loadType;
                            i = 1;
                        } else {
                            throw new MmsException("Content-Type must be set.");
                        }
                    }
                    if (c2 != null) {
                        c2.close();
                    }
                    return parts;
                }
            } catch (IOException e72) {
                IOException partURI4 = e72;
                Log.e(TAG, "Failed to close stream", e72);
            } catch (Throwable th2) {
                if (c2 != null) {
                    c2.close();
                }
            }
        }
        if (c2 != null) {
            c2.close();
        }
        return null;
    }

    private void loadAddress(long msgId, PduHeaders headers, int loadType) {
        Cursor c;
        Context context;
        ContentResolver contentResolver;
        StringBuilder stringBuilder;
        if (loadType == 1) {
            context = this.mContext;
            contentResolver = this.mContentResolver;
            stringBuilder = new StringBuilder();
            stringBuilder.append("content://fav-mms/");
            stringBuilder.append(msgId);
            stringBuilder.append("/addr");
            c = SqliteWrapper.query(context, contentResolver, Uri.parse(stringBuilder.toString()), new String[]{"address", "charset", "type"}, null, null, null);
        } else {
            context = this.mContext;
            contentResolver = this.mContentResolver;
            stringBuilder = new StringBuilder();
            stringBuilder.append("content://mms/");
            stringBuilder.append(msgId);
            stringBuilder.append("/addr");
            c = SqliteWrapper.query(context, contentResolver, Uri.parse(stringBuilder.toString()), new String[]{"address", "charset", "type"}, null, null, null);
        }
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    String addr = c.getString(null);
                    if (!TextUtils.isEmpty(addr)) {
                        int addrType = c.getInt(2);
                        if (addrType != 137) {
                            if (addrType != 151) {
                                switch (addrType) {
                                    case 129:
                                    case 130:
                                        break;
                                    default:
                                        String str = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Unknown address type: ");
                                        stringBuilder2.append(addrType);
                                        Log.e(str, stringBuilder2.toString());
                                        continue;
                                }
                            }
                            headers.appendEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                        } else {
                            headers.setEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                        }
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:135:0x0293  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x0293  */
    /* JADX WARNING: Missing block: B:16:0x0034, code skipped:
            r5 = PDU_CACHE_INSTANCE;
     */
    /* JADX WARNING: Missing block: B:17:0x0036, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:18:0x0037, code skipped:
            if (null == null) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            PDU_CACHE_INSTANCE.put(r9, new com.google.android.mms.util.PduCacheEntry(null, 0, -1));
     */
    /* JADX WARNING: Missing block: B:21:0x0045, code skipped:
            PDU_CACHE_INSTANCE.setUpdating(r9, false);
            PDU_CACHE_INSTANCE.notifyAll();
     */
    /* JADX WARNING: Missing block: B:22:0x004f, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:23:0x0050, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:33:0x0060, code skipped:
            if (r25.toString() == null) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:35:0x006c, code skipped:
            if (r25.toString().contains("content://fav-mms") == false) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:36:0x006e, code skipped:
            r3 = 1;
     */
    /* JADX WARNING: Missing block: B:37:0x006f, code skipped:
            r7 = r3;
     */
    /* JADX WARNING: Missing block: B:40:0x0076, code skipped:
            r14 = r7;
            r0 = 1;
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            r2 = com.google.android.mms.util.SqliteWrapper.query(r1.mContext, r1.mContentResolver, r9, PDU_PROJECTION, null, null, null);
            r3 = new com.google.android.mms.pdu.PduHeaders();
            r4 = android.content.ContentUris.parseId(r25);
     */
    /* JADX WARNING: Missing block: B:43:0x008d, code skipped:
            if (r2 == null) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:46:0x0093, code skipped:
            if (r2.getCount() != r0) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:48:0x0099, code skipped:
            if (r2.moveToFirst() == false) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:49:0x009b, code skipped:
            r11 = r2.getInt(r0);
            r12 = r2.getLong(2);
            r0 = ENCODED_STRING_COLUMN_INDEX_MAP.entrySet();
            r6 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:51:0x00b4, code skipped:
            if (r6.hasNext() == false) goto L_0x00dc;
     */
    /* JADX WARNING: Missing block: B:52:0x00b6, code skipped:
            r7 = (java.util.Map.Entry) r6.next();
            r17 = r0;
            setEncodedStringValueToHeaders(r2, ((java.lang.Integer) r7.getValue()).intValue(), r3, ((java.lang.Integer) r7.getKey()).intValue());
            r0 = r17;
     */
    /* JADX WARNING: Missing block: B:53:0x00dc, code skipped:
            r17 = r0;
            r0 = TEXT_STRING_COLUMN_INDEX_MAP.entrySet();
            r6 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:55:0x00ec, code skipped:
            if (r6.hasNext() == false) goto L_0x0114;
     */
    /* JADX WARNING: Missing block: B:56:0x00ee, code skipped:
            r7 = (java.util.Map.Entry) r6.next();
            r18 = r0;
            setTextStringToHeaders(r2, ((java.lang.Integer) r7.getValue()).intValue(), r3, ((java.lang.Integer) r7.getKey()).intValue());
            r0 = r18;
     */
    /* JADX WARNING: Missing block: B:57:0x0114, code skipped:
            r18 = r0;
            r0 = OCTET_COLUMN_INDEX_MAP.entrySet();
            r6 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:59:0x0124, code skipped:
            if (r6.hasNext() == false) goto L_0x014c;
     */
    /* JADX WARNING: Missing block: B:60:0x0126, code skipped:
            r7 = (java.util.Map.Entry) r6.next();
            r19 = r0;
            setOctetToHeaders(r2, ((java.lang.Integer) r7.getValue()).intValue(), r3, ((java.lang.Integer) r7.getKey()).intValue());
            r0 = r19;
     */
    /* JADX WARNING: Missing block: B:61:0x014c, code skipped:
            r19 = r0;
            r0 = LONG_COLUMN_INDEX_MAP.entrySet();
            r6 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:63:0x015c, code skipped:
            if (r6.hasNext() == false) goto L_0x0184;
     */
    /* JADX WARNING: Missing block: B:64:0x015e, code skipped:
            r7 = (java.util.Map.Entry) r6.next();
            r20 = r6;
            setLongToHeaders(r2, ((java.lang.Integer) r7.getValue()).intValue(), r3, ((java.lang.Integer) r7.getKey()).intValue());
     */
    /* JADX WARNING: Missing block: B:65:0x0180, code skipped:
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:66:0x0184, code skipped:
            if (r2 == null) goto L_0x0189;
     */
    /* JADX WARNING: Missing block: B:68:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:70:0x018d, code skipped:
            if (r4 == -1) goto L_0x0257;
     */
    /* JADX WARNING: Missing block: B:71:0x018f, code skipped:
            loadAddress(r4, r3, r14);
            r6 = r3.getOctet(140);
            r7 = new com.google.android.mms.pdu.PduBody();
     */
    /* JADX WARNING: Missing block: B:72:0x019f, code skipped:
            if (r6 == 132) goto L_0x01a9;
     */
    /* JADX WARNING: Missing block: B:74:0x01a3, code skipped:
            if (r6 != 128) goto L_0x01a6;
     */
    /* JADX WARNING: Missing block: B:76:0x01a6, code skipped:
            r21 = r0;
     */
    /* JADX WARNING: Missing block: B:77:0x01a9, code skipped:
            r8 = loadParts(r4, r14);
     */
    /* JADX WARNING: Missing block: B:78:0x01ad, code skipped:
            if (r8 == null) goto L_0x01c8;
     */
    /* JADX WARNING: Missing block: B:79:0x01af, code skipped:
            r21 = r0;
            r0 = r8.length;
            r16 = 0;
     */
    /* JADX WARNING: Missing block: B:80:0x01b4, code skipped:
            r1 = r16;
     */
    /* JADX WARNING: Missing block: B:81:0x01b8, code skipped:
            if (r1 >= r0) goto L_0x01ca;
     */
    /* JADX WARNING: Missing block: B:82:0x01ba, code skipped:
            r23 = r0;
            r7.addPart(r8[r1]);
            r16 = r1 + 1;
            r0 = r23;
            r1 = r24;
     */
    /* JADX WARNING: Missing block: B:83:0x01c8, code skipped:
            r21 = r0;
     */
    /* JADX WARNING: Missing block: B:84:0x01ca, code skipped:
            switch(r6) {
                case 128: goto L_0x0216;
                case 129: goto L_0x01fb;
                case 130: goto L_0x01f5;
                case 131: goto L_0x01ef;
                case 132: goto L_0x01e9;
                case 133: goto L_0x01e3;
                case 134: goto L_0x01dd;
                case 135: goto L_0x01d7;
                case 136: goto L_0x01d1;
                case 137: goto L_0x01fb;
                case 138: goto L_0x01fb;
                case 139: goto L_0x01fb;
                case 140: goto L_0x01fb;
                case 141: goto L_0x01fb;
                case 142: goto L_0x01fb;
                case 143: goto L_0x01fb;
                case 144: goto L_0x01fb;
                case 145: goto L_0x01fb;
                case 146: goto L_0x01fb;
                case 147: goto L_0x01fb;
                case 148: goto L_0x01fb;
                case 149: goto L_0x01fb;
                case 150: goto L_0x01fb;
                case 151: goto L_0x01fb;
                default: goto L_0x01cd;
            };
     */
    /* JADX WARNING: Missing block: B:86:0x01d1, code skipped:
            r0 = new com.google.android.mms.pdu.ReadOrigInd(r3);
     */
    /* JADX WARNING: Missing block: B:87:0x01d7, code skipped:
            r0 = new com.google.android.mms.pdu.ReadRecInd(r3);
     */
    /* JADX WARNING: Missing block: B:88:0x01dd, code skipped:
            r0 = new com.google.android.mms.pdu.DeliveryInd(r3);
     */
    /* JADX WARNING: Missing block: B:89:0x01e3, code skipped:
            r0 = new com.google.android.mms.pdu.AcknowledgeInd(r3);
     */
    /* JADX WARNING: Missing block: B:90:0x01e9, code skipped:
            r0 = new com.google.android.mms.pdu.RetrieveConf(r3, r7);
     */
    /* JADX WARNING: Missing block: B:91:0x01ef, code skipped:
            r0 = new com.google.android.mms.pdu.NotifyRespInd(r3);
     */
    /* JADX WARNING: Missing block: B:92:0x01f5, code skipped:
            r0 = new com.google.android.mms.pdu.NotificationInd(r3);
     */
    /* JADX WARNING: Missing block: B:93:0x01fb, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Unsupported PDU type: ");
            r1.append(java.lang.Integer.toHexString(r6));
     */
    /* JADX WARNING: Missing block: B:94:0x0215, code skipped:
            throw new com.google.android.mms.MmsException(r1.toString());
     */
    /* JADX WARNING: Missing block: B:95:0x0216, code skipped:
            r0 = new com.google.android.mms.pdu.SendReq(r3, r7);
     */
    /* JADX WARNING: Missing block: B:96:0x021c, code skipped:
            r1 = r0;
            r2 = PDU_CACHE_INSTANCE;
     */
    /* JADX WARNING: Missing block: B:97:0x021f, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:99:?, code skipped:
            PDU_CACHE_INSTANCE.put(r9, new com.google.android.mms.util.PduCacheEntry(r1, r11, r12));
            PDU_CACHE_INSTANCE.setUpdating(r9, 0);
            PDU_CACHE_INSTANCE.notifyAll();
     */
    /* JADX WARNING: Missing block: B:100:0x0238, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:102:0x023a, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:107:?, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Unrecognized PDU type: ");
            r1.append(java.lang.Integer.toHexString(r6));
     */
    /* JADX WARNING: Missing block: B:108:0x0256, code skipped:
            throw new com.google.android.mms.MmsException(r1.toString());
     */
    /* JADX WARNING: Missing block: B:109:0x0257, code skipped:
            r21 = r0;
     */
    /* JADX WARNING: Missing block: B:110:0x0260, code skipped:
            throw new com.google.android.mms.MmsException("Error! ID of the message: -1.");
     */
    /* JADX WARNING: Missing block: B:113:?, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Bad uri: ");
            r1.append(r9);
     */
    /* JADX WARNING: Missing block: B:114:0x0279, code skipped:
            throw new com.google.android.mms.MmsException(r1.toString());
     */
    /* JADX WARNING: Missing block: B:115:0x027a, code skipped:
            if (r2 != null) goto L_0x027c;
     */
    /* JADX WARNING: Missing block: B:117:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:119:0x0280, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:120:0x0282, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:121:0x0283, code skipped:
            r14 = r7;
     */
    /* JADX WARNING: Missing block: B:122:0x0285, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:132:0x028f, code skipped:
            r14 = r3;
     */
    /* JADX WARNING: Missing block: B:134:0x0292, code skipped:
            monitor-enter(PDU_CACHE_INSTANCE);
     */
    /* JADX WARNING: Missing block: B:135:0x0293, code skipped:
            if (null != null) goto L_0x0296;
     */
    /* JADX WARNING: Missing block: B:137:?, code skipped:
            PDU_CACHE_INSTANCE.put(r9, new com.google.android.mms.util.PduCacheEntry(null, r11, r12));
     */
    /* JADX WARNING: Missing block: B:138:0x02a1, code skipped:
            PDU_CACHE_INSTANCE.setUpdating(r9, false);
            PDU_CACHE_INSTANCE.notifyAll();
     */
    /* JADX WARNING: Missing block: B:140:0x02ad, code skipped:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public GenericPdu load(Uri uri) throws MmsException {
        Throwable th;
        Uri uri2 = uri;
        PduCacheEntry cacheEntry = null;
        int msgBox = 0;
        long threadId = -1;
        int loadType = 0;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                try {
                    if (PDU_CACHE_INSTANCE.isUpdating(uri2)) {
                        PDU_CACHE_INSTANCE.wait();
                        cacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri2);
                        if (cacheEntry != null) {
                            GenericPdu pdu = cacheEntry.getPdu();
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "load: ", e);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
                try {
                    PDU_CACHE_INSTANCE.setUpdating(uri2, true);
                } catch (Throwable th3) {
                    th = th3;
                    cacheEntry = cacheEntry;
                    throw th;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            PduCacheEntry pduCacheEntry = cacheEntry;
        }
    }

    private void persistAddress(long msgId, int type, EncodedStringValue[] array) {
        ContentValues[] allValues = new ContentValues[array.length];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("content://mms/");
        stringBuilder.append(msgId);
        stringBuilder.append("/addr");
        Uri uri = Uri.parse(stringBuilder.toString());
        int idx = 0;
        int length = array.length;
        int i = 0;
        while (i < length) {
            EncodedStringValue addr = array[i];
            ContentValues values = new ContentValues(3);
            values.put("address", toIsoString(addr.getTextString()));
            values.put("charset", Integer.valueOf(addr.getCharacterSet()));
            values.put("type", Integer.valueOf(type));
            int idx2 = idx + 1;
            allValues[idx] = values;
            i++;
            idx = idx2;
        }
        this.mContext.getContentResolver().bulkInsert(uri, allValues);
    }

    private static String getPartContentType(PduPart part) {
        return part.getContentType() == null ? null : toIsoString(part.getContentType());
    }

    public Uri persistPart(PduPart part, long msgId, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("content://mms/");
        stringBuilder.append(msgId);
        stringBuilder.append("/part");
        Uri uri = Uri.parse(stringBuilder.toString());
        ContentValues values = new ContentValues(8);
        int charset = part.getCharset();
        if (charset != 0) {
            values.put("chset", Integer.valueOf(charset));
        } else {
            values.put("chset", Integer.valueOf(106));
        }
        String contentType = getPartContentType(part);
        if (contentType != null) {
            if (ContentType.IMAGE_JPG.equals(contentType)) {
                contentType = ContentType.IMAGE_JPEG;
            }
            if (!ContentType.isSupportedType(contentType)) {
                String keyWord = null;
                if (part.getName() != null) {
                    keyWord = toIsoString(part.getName());
                } else if (part.getContentLocation() != null) {
                    keyWord = toIsoString(part.getContentLocation());
                }
                if (keyWord != null && keyWord.toLowerCase(Locale.US).endsWith(".vcs")) {
                    contentType = ContentType.TEXT_VCALENDAR;
                }
                if (keyWord != null && keyWord.toLowerCase(Locale.US).endsWith(".vcf")) {
                    contentType = ContentType.TEXT_VCARD;
                }
            }
            values.put("ct", contentType);
            if (ContentType.APP_SMIL.equals(contentType)) {
                values.put("seq", Integer.valueOf(-1));
            }
            if (part.getFilename() != null) {
                values.put("fn", toIsoString(part.getFilename()));
            }
            if (part.getName() != null) {
                values.put("name", toIsoString(part.getName()));
            }
            if (part.getContentDisposition() != null) {
                values.put("cd", toIsoString(part.getContentDisposition()));
            }
            if (part.getContentId() != null) {
                values.put("cid", toIsoString(part.getContentId()));
            }
            if (part.getContentLocation() != null) {
                values.put("cl", toIsoString(part.getContentLocation()));
            }
            Uri res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
            if (res != null) {
                persistData(part, res, contentType, preOpenedFiles);
                part.setDataUri(res);
                return res;
            }
            throw new MmsException("Failed to persist part, return null.");
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:144:0x02e7 A:{SYNTHETIC, Splitter:B:144:0x02e7} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x0305 A:{SYNTHETIC, Splitter:B:149:0x0305} */
    /* JADX WARNING: Removed duplicated region for block: B:154:0x0323  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02e7 A:{SYNTHETIC, Splitter:B:144:0x02e7} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x0305 A:{SYNTHETIC, Splitter:B:149:0x0305} */
    /* JADX WARNING: Removed duplicated region for block: B:154:0x0323  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03b2 A:{SYNTHETIC, Splitter:B:179:0x03b2} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03d0 A:{SYNTHETIC, Splitter:B:184:0x03d0} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03b2 A:{SYNTHETIC, Splitter:B:179:0x03b2} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03d0 A:{SYNTHETIC, Splitter:B:184:0x03d0} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03b2 A:{SYNTHETIC, Splitter:B:179:0x03b2} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03d0 A:{SYNTHETIC, Splitter:B:184:0x03d0} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03b2 A:{SYNTHETIC, Splitter:B:179:0x03b2} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03d0 A:{SYNTHETIC, Splitter:B:184:0x03d0} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03b2 A:{SYNTHETIC, Splitter:B:179:0x03b2} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03d0 A:{SYNTHETIC, Splitter:B:184:0x03d0} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void persistData(PduPart part, Uri uri, String contentType, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        OutputStream os;
        Uri dataUri;
        StringBuilder stringBuilder;
        IOException iOException;
        String str;
        StringBuilder stringBuilder2;
        Throwable e;
        Throwable th;
        ContentValues values;
        Context context;
        ContentResolver contentResolver;
        IOException iOException2;
        Uri uri2 = uri;
        String str2 = contentType;
        HashMap<Uri, InputStream> hashMap = preOpenedFiles;
        OutputStream os2 = null;
        InputStream is = null;
        DrmConvertSession drmConvertSession = null;
        Uri dataUri2 = null;
        String path = null;
        try {
            StringBuilder stringBuilder3;
            byte[] data = part.getData();
            if (ContentType.TEXT_PLAIN.equals(str2) || ContentType.APP_SMIL.equals(str2)) {
                os = os2;
                dataUri = null;
            } else if (ContentType.TEXT_HTML.equals(str2)) {
                os = os2;
                dataUri = null;
            } else {
                File f;
                StringBuilder stringBuilder4;
                boolean isDrm = DownloadDrmHelper.isDrmConvertNeeded(contentType);
                if (isDrm) {
                    if (uri2 != null) {
                        try {
                            path = convertUriToPath(this.mContext, uri2);
                            File f2 = new File(path);
                            if (f2.length() > 0) {
                                File file;
                                if (os2 != null) {
                                    try {
                                        os2.close();
                                        dataUri = null;
                                        file = f2;
                                    } catch (IOException e2) {
                                        IOException iOException3 = e2;
                                        String str3 = TAG;
                                        dataUri = null;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("IOException while closing: ");
                                        stringBuilder.append(os2);
                                        Log.e(str3, stringBuilder.toString(), e2);
                                    }
                                } else {
                                    dataUri = null;
                                    file = f2;
                                }
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e22) {
                                        iOException = e22;
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("IOException while closing: ");
                                        stringBuilder2.append(is);
                                        Log.e(str, stringBuilder2.toString(), e22);
                                    }
                                }
                                if (drmConvertSession != null) {
                                    drmConvertSession.close(path);
                                    f = new File(path);
                                    ContentValues values2 = new ContentValues(0);
                                    Context context2 = this.mContext;
                                    ContentResolver contentResolver2 = this.mContentResolver;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("content://mms/resetFilePerm/");
                                    stringBuilder4.append(f.getName());
                                    SqliteWrapper.update(context2, contentResolver2, Uri.parse(stringBuilder4.toString()), values2, null, null);
                                }
                                return;
                            }
                            dataUri = null;
                        } catch (Exception e3) {
                            os = os2;
                            dataUri = null;
                            try {
                                String str4 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Can't get file info for: ");
                                stringBuilder.append(part.getDataUri());
                                Log.e(str4, stringBuilder.toString(), e3);
                            } catch (FileNotFoundException e4) {
                                e = e4;
                                os2 = os;
                                Log.e(TAG, "Failed to open Input/Output stream.", e);
                                throw new MmsException(e);
                            } catch (IOException e5) {
                                e = e5;
                                dataUri2 = dataUri;
                                os2 = os;
                                Log.e(TAG, "Failed to read/write data.", e);
                                throw new MmsException(e);
                            } catch (Throwable e6) {
                                th = e6;
                                os2 = os;
                                if (os2 != null) {
                                    try {
                                        os2.close();
                                    } catch (IOException e222) {
                                        iOException = e222;
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("IOException while closing: ");
                                        stringBuilder2.append(os2);
                                        Log.e(str, stringBuilder2.toString(), e222);
                                    }
                                }
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e2222) {
                                        iOException = e2222;
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("IOException while closing: ");
                                        stringBuilder2.append(is);
                                        Log.e(str, stringBuilder2.toString(), e2222);
                                    }
                                }
                                if (drmConvertSession != null) {
                                    drmConvertSession.close(path);
                                    f = new File(path);
                                    values = new ContentValues(0);
                                    context = this.mContext;
                                    contentResolver = this.mContentResolver;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("content://mms/resetFilePerm/");
                                    stringBuilder.append(f.getName());
                                    SqliteWrapper.update(context, contentResolver, Uri.parse(stringBuilder.toString()), values, null, null);
                                }
                                throw th;
                            }
                        }
                    }
                    dataUri = null;
                    drmConvertSession = DrmConvertSession.open(this.mContext, str2);
                    if (drmConvertSession == null) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Mimetype ");
                        stringBuilder3.append(str2);
                        stringBuilder3.append(" can not be converted.");
                        throw new MmsException(stringBuilder3.toString());
                    }
                } else {
                    dataUri = null;
                }
                os2 = this.mContentResolver.openOutputStream(uri2);
                byte[] buffer;
                int len;
                if (data == null) {
                    try {
                        Uri dataUri3 = part.getDataUri();
                        if (dataUri3 != null) {
                            try {
                                if (!dataUri3.equals(uri2)) {
                                    if (hashMap != null && hashMap.containsKey(dataUri3)) {
                                        is = (InputStream) hashMap.get(dataUri3);
                                    }
                                    if (is == null) {
                                        is = this.mContentResolver.openInputStream(dataUri3);
                                    }
                                    buffer = new byte[RadioAccessFamily.EVDO_B];
                                    while (true) {
                                        int read = is.read(buffer);
                                        len = read;
                                        if (read == -1) {
                                            dataUri = dataUri3;
                                            break;
                                        }
                                        if (isDrm) {
                                            byte[] convertedData = drmConvertSession.convert(buffer, len);
                                            if (convertedData != null) {
                                                os2.write(convertedData, 0, convertedData.length);
                                            } else {
                                                throw new MmsException("Error converting drm data.");
                                            }
                                        }
                                        os2.write(buffer, 0, len);
                                        str2 = contentType;
                                    }
                                    if (os2 != null) {
                                        try {
                                            os2.close();
                                        } catch (IOException e22222) {
                                            iOException2 = e22222;
                                            str2 = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("IOException while closing: ");
                                            stringBuilder.append(os2);
                                            Log.e(str2, stringBuilder.toString(), e22222);
                                        }
                                    }
                                    if (is != null) {
                                        try {
                                            is.close();
                                        } catch (IOException e222222) {
                                            iOException2 = e222222;
                                            str2 = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("IOException while closing: ");
                                            stringBuilder.append(is);
                                            Log.e(str2, stringBuilder.toString(), e222222);
                                        }
                                    }
                                    if (drmConvertSession != null) {
                                        drmConvertSession.close(path);
                                        f = new File(path);
                                        values = new ContentValues(0);
                                        context = this.mContext;
                                        contentResolver = this.mContentResolver;
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("content://mms/resetFilePerm/");
                                        stringBuilder5.append(f.getName());
                                        SqliteWrapper.update(context, contentResolver, Uri.parse(stringBuilder5.toString()), values, null, null);
                                    }
                                }
                            } catch (FileNotFoundException e7) {
                                e6 = e7;
                                dataUri2 = dataUri3;
                                Log.e(TAG, "Failed to open Input/Output stream.", e6);
                                throw new MmsException(e6);
                            } catch (IOException e8) {
                                e6 = e8;
                                dataUri2 = dataUri3;
                                Log.e(TAG, "Failed to read/write data.", e6);
                                throw new MmsException(e6);
                            } catch (Throwable e62) {
                                th = e62;
                                dataUri = dataUri3;
                                if (os2 != null) {
                                }
                                if (is != null) {
                                }
                                if (drmConvertSession != null) {
                                }
                                throw th;
                            }
                        }
                        Log.w(TAG, "Can't find data for this part.");
                        if (os2 != null) {
                            try {
                                os2.close();
                            } catch (IOException e2222222) {
                                iOException2 = e2222222;
                                str2 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("IOException while closing: ");
                                stringBuilder.append(os2);
                                Log.e(str2, stringBuilder.toString(), e2222222);
                            }
                        }
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e22222222) {
                                iOException2 = e22222222;
                                str2 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("IOException while closing: ");
                                stringBuilder.append(is);
                                Log.e(str2, stringBuilder.toString(), e22222222);
                            }
                        }
                        if (drmConvertSession != null) {
                            drmConvertSession.close(path);
                            f = new File(path);
                            ContentValues values3 = new ContentValues(0);
                            Context context3 = this.mContext;
                            ContentResolver contentResolver3 = this.mContentResolver;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("content://mms/resetFilePerm/");
                            stringBuilder4.append(f.getName());
                            SqliteWrapper.update(context3, contentResolver3, Uri.parse(stringBuilder4.toString()), values3, null, null);
                        }
                        return;
                    } catch (FileNotFoundException e9) {
                        e62 = e9;
                        dataUri2 = dataUri;
                    } catch (IOException e10) {
                        e62 = e10;
                        dataUri2 = dataUri;
                        Log.e(TAG, "Failed to read/write data.", e62);
                        throw new MmsException(e62);
                    } catch (Throwable th2) {
                        e62 = th2;
                        th = e62;
                        if (os2 != null) {
                        }
                        if (is != null) {
                        }
                        if (drmConvertSession != null) {
                        }
                        throw th;
                    }
                }
                if (isDrm) {
                    Uri dataUri4 = uri2;
                    try {
                        is = new ByteArrayInputStream(data);
                        buffer = new byte[RadioAccessFamily.EVDO_B];
                        while (true) {
                            len = is.read(buffer);
                            int len2 = len;
                            if (len == -1) {
                                dataUri = dataUri4;
                                break;
                            }
                            byte[] convertedData2 = drmConvertSession.convert(buffer, len2);
                            if (convertedData2 != null) {
                                os2.write(convertedData2, 0, convertedData2.length);
                            } else {
                                throw new MmsException("Error converting drm data.");
                            }
                        }
                    } catch (FileNotFoundException e11) {
                        e62 = e11;
                        Log.e(TAG, "Failed to open Input/Output stream.", e62);
                        throw new MmsException(e62);
                    } catch (IOException e12) {
                        e62 = e12;
                        dataUri2 = dataUri4;
                        Log.e(TAG, "Failed to read/write data.", e62);
                        throw new MmsException(e62);
                    } catch (Throwable th3) {
                        e62 = th3;
                        dataUri = dataUri4;
                        th = e62;
                        if (os2 != null) {
                        }
                        if (is != null) {
                        }
                        if (drmConvertSession != null) {
                        }
                        throw th;
                    }
                }
                os2.write(data);
                if (os2 != null) {
                }
                if (is != null) {
                }
                if (drmConvertSession != null) {
                }
            }
            if ("true".equals("true")) {
                int charset = part.getCharset();
                ContentValues cv = new ContentValues();
                if (charset != 0) {
                    cv.put("text", data != null ? new EncodedStringValue(charset, data).getString() : "");
                } else {
                    cv.put("text", data != null ? new EncodedStringValue(data).getString() : "");
                }
                if (this.mContentResolver.update(uri2, cv, null, null) != 1) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("unable to update ");
                    stringBuilder3.append(uri.toString());
                    throw new MmsException(stringBuilder3.toString());
                }
            } else {
                ContentValues cv2 = new ContentValues();
                cv2.put("text", new EncodedStringValue(data).getString());
                if (this.mContentResolver.update(uri2, cv2, null, null) != 1) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("unable to update ");
                    stringBuilder3.append(uri.toString());
                    throw new MmsException(stringBuilder3.toString());
                }
            }
            os2 = os;
            if (os2 != null) {
            }
            if (is != null) {
            }
            if (drmConvertSession != null) {
            }
        } catch (FileNotFoundException e13) {
            e62 = e13;
            os = os2;
            dataUri = null;
            Log.e(TAG, "Failed to open Input/Output stream.", e62);
            throw new MmsException(e62);
        } catch (IOException e14) {
            e62 = e14;
            os = os2;
            dataUri = null;
            Log.e(TAG, "Failed to read/write data.", e62);
            throw new MmsException(e62);
        } catch (Throwable e622) {
            th = e622;
            dataUri = dataUri2;
            if (os2 != null) {
            }
            if (is != null) {
            }
            if (drmConvertSession != null) {
            }
            throw th;
        }
    }

    public static String convertUriToPath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("") || scheme.equals("file")) {
            return uri.getPath();
        }
        if (scheme.equals("http")) {
            return uri.toString();
        }
        if (scheme.equals("content")) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst()) {
                    throw new IllegalArgumentException("Given Uri could not be found in media store");
                }
                String path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                if (cursor == null) {
                    return path;
                }
                cursor.close();
                return path;
            } catch (SQLiteException e) {
                throw new IllegalArgumentException("Given Uri is not formatted in a way so that it can be found in media store.");
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            throw new IllegalArgumentException("Given Uri scheme is not supported");
        }
    }

    private void updateAddress(long msgId, int type, EncodedStringValue[] array) {
        Context context = this.mContext;
        ContentResolver contentResolver = this.mContentResolver;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("content://mms/");
        stringBuilder.append(msgId);
        stringBuilder.append("/addr");
        Uri parse = Uri.parse(stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("type=");
        stringBuilder2.append(type);
        SqliteWrapper.delete(context, contentResolver, parse, stringBuilder2.toString(), null);
        persistAddress(msgId, type, array);
    }

    public void updateHeaders(Uri uri, SendReq sendReq) {
        int priority;
        byte[] contentType;
        long date;
        Uri uri2 = uri;
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri2)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "updateHeaders: ", e);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri2);
        ContentValues values = new ContentValues(10);
        byte[] contentType2 = sendReq.getContentType();
        if (contentType2 != null) {
            values.put("ct_t", toIsoString(contentType2));
        }
        long date2 = sendReq.getDate();
        if (date2 != -1) {
            values.put("date", Long.valueOf(date2));
        }
        int deliveryReport = sendReq.getDeliveryReport();
        if (deliveryReport != 0) {
            values.put("d_rpt", Integer.valueOf(deliveryReport));
        }
        long expiry = sendReq.getExpiry();
        if (expiry != -1) {
            values.put("exp", Long.valueOf(expiry));
        }
        byte[] msgClass = sendReq.getMessageClass();
        if (msgClass != null) {
            values.put("m_cls", toIsoString(msgClass));
        }
        int priority2 = sendReq.getPriority();
        if (priority2 != 0) {
            values.put("pri", Integer.valueOf(priority2));
        }
        int readReport = sendReq.getReadReport();
        if (readReport != 0) {
            values.put("rr", Integer.valueOf(readReport));
        }
        byte[] transId = sendReq.getTransactionId();
        if (transId != null) {
            values.put("tr_id", toIsoString(transId));
        }
        EncodedStringValue subject = sendReq.getSubject();
        if (subject != null) {
            values.put("sub", toIsoString(subject.getTextString()));
            values.put("sub_cs", Integer.valueOf(subject.getCharacterSet()));
        } else {
            values.put("sub", "");
        }
        long messageSize = sendReq.getMessageSize();
        EncodedStringValue subject2;
        byte[] transId2;
        if (messageSize > 0) {
            subject2 = subject;
            transId2 = transId;
            values.put("m_size", Long.valueOf(messageSize));
        } else {
            subject2 = subject;
            transId2 = transId;
        }
        PduHeaders headers = sendReq.getPduHeaders();
        HashSet<String> recipients = new HashSet();
        long messageSize2 = messageSize;
        int[] iArr = ADDRESS_FIELDS;
        int length = iArr.length;
        int readReport2 = readReport;
        readReport = 0;
        while (readReport < length) {
            int i = length;
            length = iArr[readReport];
            EncodedStringValue[] array = null;
            int[] iArr2 = iArr;
            if (length == 137) {
                if (headers.getEncodedStringValue(length) != null) {
                    priority = priority2;
                    array = new EncodedStringValue[]{headers.getEncodedStringValue(length)};
                } else {
                    priority = priority2;
                }
            } else {
                priority = priority2;
                array = headers.getEncodedStringValues(length);
            }
            EncodedStringValue[] array2 = array;
            if (array2 != null) {
                contentType = contentType2;
                date = date2;
                updateAddress(ContentUris.parseId(uri), length, array2);
                if (length == 151) {
                    priority2 = array2.length;
                    int i2 = 0;
                    while (i2 < priority2) {
                        EncodedStringValue[] array3;
                        int addrType = length;
                        EncodedStringValue v = array2[i2];
                        if (v != null) {
                            array3 = array2;
                            recipients.add(v.getString());
                        } else {
                            array3 = array2;
                        }
                        i2++;
                        length = addrType;
                        array2 = array3;
                    }
                }
            } else {
                contentType = contentType2;
                date = date2;
            }
            readReport++;
            length = i;
            iArr = iArr2;
            priority2 = priority;
            contentType2 = contentType;
            date2 = date;
        }
        priority = priority2;
        contentType = contentType2;
        date = date2;
        if (!recipients.isEmpty()) {
            values.put("thread_id", Long.valueOf(Threads.getOrCreateThreadId(this.mContext, recipients)));
        }
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri2, values, null, null);
    }

    private void updatePart(Uri uri, PduPart part, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        ContentValues values = new ContentValues(7);
        int charset = part.getCharset();
        if (charset != 0) {
            values.put("chset", Integer.valueOf(charset));
        }
        if (part.getContentType() != null) {
            String contentType = toIsoString(part.getContentType());
            values.put("ct", contentType);
            Object value = null;
            if (part.getFilename() != null) {
                value = toIsoString(part.getFilename());
                values.put("fn", (String) value);
            }
            if (part.getName() != null) {
                value = toIsoString(part.getName());
                values.put("name", (String) value);
            }
            if (part.getContentDisposition() != null) {
                value = toIsoString(part.getContentDisposition());
                values.put("cd", (String) value);
            }
            if (part.getContentId() != null) {
                value = toIsoString(part.getContentId());
                values.put("cid", (String) value);
            }
            if (part.getContentLocation() != null) {
                value = toIsoString(part.getContentLocation());
                values.put("cl", (String) value);
            }
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            if (part.getData() != null || uri.equals(part.getDataUri()) == null) {
                persistData(part, uri, contentType, preOpenedFiles);
                return;
            }
            return;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    public void updateParts(Uri uri, PduBody body, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        Uri uri2 = uri;
        PduBody pduBody = body;
        HashMap<Uri, InputStream> hashMap = preOpenedFiles;
        Iterator it;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri2)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "updateParts: ", e);
                    }
                    PduCacheEntry cacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri2);
                    if (cacheEntry != null) {
                        ((MultimediaMessagePdu) cacheEntry.getPdu()).setBody(pduBody);
                    }
                }
                PDU_CACHE_INSTANCE.setUpdating(uri2, true);
            }
            ArrayList<PduPart> toBeCreated = new ArrayList();
            HashMap<Uri, PduPart> toBeUpdated = new HashMap();
            int partsNum = body.getPartsNum();
            StringBuilder filter = new StringBuilder();
            filter.append('(');
            for (int i = 0; i < partsNum; i++) {
                PduPart part = pduBody.getPart(i);
                Uri partUri = part.getDataUri();
                if (!(partUri == null || TextUtils.isEmpty(partUri.getAuthority()))) {
                    if (partUri.getAuthority().startsWith("mms")) {
                        toBeUpdated.put(partUri, part);
                        if (filter.length() > 1) {
                            filter.append(" AND ");
                        }
                        filter.append(HbpcdLookup.ID);
                        filter.append("!=");
                        DatabaseUtils.appendEscapedSQLString(filter, partUri.getLastPathSegment());
                    }
                }
                toBeCreated.add(part);
            }
            filter.append(')');
            long msgId = ContentUris.parseId(uri);
            Context context = this.mContext;
            ContentResolver contentResolver = this.mContentResolver;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Mms.CONTENT_URI);
            stringBuilder.append("/");
            stringBuilder.append(msgId);
            stringBuilder.append("/part");
            SqliteWrapper.delete(context, contentResolver, Uri.parse(stringBuilder.toString()), filter.length() > 2 ? filter.toString() : null, null);
            it = toBeCreated.iterator();
            while (it.hasNext()) {
                persistPart((PduPart) it.next(), msgId, hashMap);
            }
            it = toBeUpdated.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Uri, PduPart> e2 = (Entry) it.next();
                updatePart((Uri) e2.getKey(), (PduPart) e2.getValue(), hashMap);
            }
            PDU_CACHE_INSTANCE.setUpdating(uri2, false);
            PDU_CACHE_INSTANCE.notifyAll();
        } finally {
            it = PDU_CACHE_INSTANCE;
            synchronized (it) {
                PDU_CACHE_INSTANCE.setUpdating(uri2, false);
                PDU_CACHE_INSTANCE.notifyAll();
            }
        }
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        return persist(pdu, uri, createThreadId, groupMmsEnabled, preOpenedFiles, 0);
    }

    /* JADX WARNING: Missing block: B:32:0x0051, code skipped:
            PDU_CACHE_INSTANCE.purge(r9);
            r0 = r40.getPduHeaders();
            r5 = null;
            r8 = new android.content.ContentValues();
            r6 = ENCODED_STRING_COLUMN_NAME_MAP.entrySet();
            r7 = r6.iterator();
     */
    /* JADX WARNING: Missing block: B:34:0x006f, code skipped:
            if (r7.hasNext() == false) goto L_0x00c3;
     */
    /* JADX WARNING: Missing block: B:35:0x0071, code skipped:
            r13 = (java.util.Map.Entry) r7.next();
            r14 = ((java.lang.Integer) r13.getKey()).intValue();
            r3 = r0.getEncodedStringValue(r14);
     */
    /* JADX WARNING: Missing block: B:36:0x0089, code skipped:
            if (r3 == null) goto L_0x00b8;
     */
    /* JADX WARNING: Missing block: B:37:0x008b, code skipped:
            r20 = r5;
            r4 = (java.lang.String) CHARSET_COLUMN_NAME_MAP.get(java.lang.Integer.valueOf(r14));
            r21 = r6;
            r8.put((java.lang.String) r13.getValue(), toIsoString(r3.getTextString()));
            r8.put(r4, java.lang.Integer.valueOf(r3.getCharacterSet()));
     */
    /* JADX WARNING: Missing block: B:38:0x00b8, code skipped:
            r20 = r5;
            r21 = r6;
     */
    /* JADX WARNING: Missing block: B:39:0x00bc, code skipped:
            r5 = r20;
            r6 = r21;
     */
    /* JADX WARNING: Missing block: B:40:0x00c3, code skipped:
            r20 = r5;
            r21 = r6;
            r4 = TEXT_STRING_COLUMN_NAME_MAP.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:42:0x00d5, code skipped:
            if (r4.hasNext() == false) goto L_0x00fb;
     */
    /* JADX WARNING: Missing block: B:43:0x00d7, code skipped:
            r5 = (java.util.Map.Entry) r4.next();
            r6 = r0.getTextString(((java.lang.Integer) r5.getKey()).intValue());
     */
    /* JADX WARNING: Missing block: B:44:0x00eb, code skipped:
            if (r6 == null) goto L_0x00fa;
     */
    /* JADX WARNING: Missing block: B:45:0x00ed, code skipped:
            r8.put((java.lang.String) r5.getValue(), toIsoString(r6));
     */
    /* JADX WARNING: Missing block: B:47:0x00fb, code skipped:
            r4 = OCTET_COLUMN_NAME_MAP.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:49:0x0109, code skipped:
            if (r4.hasNext() == false) goto L_0x012f;
     */
    /* JADX WARNING: Missing block: B:50:0x010b, code skipped:
            r5 = (java.util.Map.Entry) r4.next();
            r6 = r0.getOctet(((java.lang.Integer) r5.getKey()).intValue());
     */
    /* JADX WARNING: Missing block: B:51:0x011f, code skipped:
            if (r6 == 0) goto L_0x012e;
     */
    /* JADX WARNING: Missing block: B:52:0x0121, code skipped:
            r8.put((java.lang.String) r5.getValue(), java.lang.Integer.valueOf(r6));
     */
    /* JADX WARNING: Missing block: B:54:0x012f, code skipped:
            r3 = LONG_COLUMN_NAME_MAP.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:56:0x013d, code skipped:
            if (r3.hasNext() == false) goto L_0x0167;
     */
    /* JADX WARNING: Missing block: B:57:0x013f, code skipped:
            r4 = (java.util.Map.Entry) r3.next();
            r5 = r0.getLongInteger(((java.lang.Integer) r4.getKey()).intValue());
     */
    /* JADX WARNING: Missing block: B:58:0x0157, code skipped:
            if (r5 == -1) goto L_0x0166;
     */
    /* JADX WARNING: Missing block: B:59:0x0159, code skipped:
            r8.put((java.lang.String) r4.getValue(), java.lang.Long.valueOf(r5));
     */
    /* JADX WARNING: Missing block: B:61:0x0167, code skipped:
            r14 = new java.util.HashMap(ADDRESS_FIELDS.length);
            r3 = ADDRESS_FIELDS;
            r4 = r3.length;
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:63:0x0176, code skipped:
            if (r5 >= r4) goto L_0x01db;
     */
    /* JADX WARNING: Missing block: B:64:0x0178, code skipped:
            r7 = r3[r5];
     */
    /* JADX WARNING: Missing block: B:65:0x017c, code skipped:
            if (r7 != 137) goto L_0x01c1;
     */
    /* JADX WARNING: Missing block: B:66:0x017e, code skipped:
            r6 = r0.getEncodedStringValue(r7);
            r18 = null;
     */
    /* JADX WARNING: Missing block: B:67:0x0184, code skipped:
            if (r6 == null) goto L_0x018a;
     */
    /* JADX WARNING: Missing block: B:68:0x0186, code skipped:
            r18 = r6.getString();
     */
    /* JADX WARNING: Missing block: B:69:0x018a, code skipped:
            r22 = r3;
            r3 = r18;
     */
    /* JADX WARNING: Missing block: B:70:0x018e, code skipped:
            if (r3 == null) goto L_0x01a4;
     */
    /* JADX WARNING: Missing block: B:72:0x0194, code skipped:
            if (r3.length() == 0) goto L_0x01a4;
     */
    /* JADX WARNING: Missing block: B:73:0x0196, code skipped:
            r23 = r3;
            r24 = r4;
            r4 = new com.google.android.mms.pdu.EncodedStringValue[]{r6};
            r26 = r11;
     */
    /* JADX WARNING: Missing block: B:74:0x01a4, code skipped:
            r23 = r3;
            r24 = r4;
            r4 = new com.google.android.mms.pdu.EncodedStringValue[1];
            r25 = r6;
            r26 = r11;
            r4[0] = new com.google.android.mms.pdu.EncodedStringValue(r1.mContext.getString(33685936));
     */
    /* JADX WARNING: Missing block: B:75:0x01c1, code skipped:
            r22 = r3;
            r24 = r4;
            r26 = r11;
            r4 = r0.getEncodedStringValues(r7);
     */
    /* JADX WARNING: Missing block: B:76:0x01cb, code skipped:
            r14.put(java.lang.Integer.valueOf(r7), r4);
            r5 = r5 + 1;
            r3 = r22;
            r4 = r24;
            r11 = r26;
     */
    /* JADX WARNING: Missing block: B:77:0x01db, code skipped:
            r26 = r11;
            r11 = new java.util.HashSet();
            r12 = r40.getMessageType();
     */
    /* JADX WARNING: Missing block: B:78:0x01ed, code skipped:
            if (r12 == 130) goto L_0x01f7;
     */
    /* JADX WARNING: Missing block: B:79:0x01ef, code skipped:
            if (r12 == 132) goto L_0x01f7;
     */
    /* JADX WARNING: Missing block: B:80:0x01f1, code skipped:
            if (r12 != 128) goto L_0x01f4;
     */
    /* JADX WARNING: Missing block: B:81:0x01f4, code skipped:
            r6 = r45;
     */
    /* JADX WARNING: Missing block: B:83:0x01f9, code skipped:
            if (r12 == 128) goto L_0x023e;
     */
    /* JADX WARNING: Missing block: B:84:0x01fb, code skipped:
            if (r12 == 130) goto L_0x0203;
     */
    /* JADX WARNING: Missing block: B:85:0x01fd, code skipped:
            if (r12 == 132) goto L_0x0203;
     */
    /* JADX WARNING: Missing block: B:86:0x01ff, code skipped:
            r6 = r45;
     */
    /* JADX WARNING: Missing block: B:88:0x0203, code skipped:
            loadRecipients(137, r11, r14, false);
     */
    /* JADX WARNING: Missing block: B:89:0x0207, code skipped:
            if (r10 == false) goto L_0x01ff;
     */
    /* JADX WARNING: Missing block: B:91:0x020b, code skipped:
            if (r1.mHwCustPduPersister == null) goto L_0x0231;
     */
    /* JADX WARNING: Missing block: B:93:0x0213, code skipped:
            if (r1.mHwCustPduPersister.isShortCodeFeatureEnabled() == false) goto L_0x0231;
     */
    /* JADX WARNING: Missing block: B:95:0x022f, code skipped:
            if (r1.mHwCustPduPersister.hasShortCode((com.google.android.mms.pdu.EncodedStringValue[]) r14.get(java.lang.Integer.valueOf(151)), (com.google.android.mms.pdu.EncodedStringValue[]) r14.get(java.lang.Integer.valueOf(130))) != false) goto L_0x01ff;
     */
    /* JADX WARNING: Missing block: B:96:0x0231, code skipped:
            loadRecipients(151, r11, r14, true);
            filterMyNumber(r10, r11, r14, r45);
            loadRecipients(130, r11, r14, true);
     */
    /* JADX WARNING: Missing block: B:97:0x023e, code skipped:
            r6 = r45;
            loadRecipients(151, r11, r14, false);
     */
    /* JADX WARNING: Missing block: B:98:0x0244, code skipped:
            r16 = 0;
     */
    /* JADX WARNING: Missing block: B:99:0x0246, code skipped:
            if (r42 == false) goto L_0x0254;
     */
    /* JADX WARNING: Missing block: B:101:0x024c, code skipped:
            if (r11.isEmpty() != false) goto L_0x0254;
     */
    /* JADX WARNING: Missing block: B:102:0x024e, code skipped:
            r16 = android.provider.Telephony.Threads.getOrCreateThreadId(r1.mContext, r11);
     */
    /* JADX WARNING: Missing block: B:103:0x0254, code skipped:
            r8.put("thread_id", java.lang.Long.valueOf(r16));
     */
    /* JADX WARNING: Missing block: B:104:0x025f, code skipped:
            r3 = java.lang.System.currentTimeMillis();
            r7 = 0;
            r28 = r0;
     */
    /* JADX WARNING: Missing block: B:105:0x0269, code skipped:
            if ((r2 instanceof com.google.android.mms.pdu.MultimediaMessagePdu) == null) goto L_0x02cf;
     */
    /* JADX WARNING: Missing block: B:106:0x026b, code skipped:
            r0 = ((com.google.android.mms.pdu.MultimediaMessagePdu) r2).getBody();
     */
    /* JADX WARNING: Missing block: B:107:0x0272, code skipped:
            if (r0 == null) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:108:0x0274, code skipped:
            r2 = r0.getPartsNum();
            r29 = true;
     */
    /* JADX WARNING: Missing block: B:109:0x027b, code skipped:
            if (r2 <= true) goto L_0x0280;
     */
    /* JADX WARNING: Missing block: B:110:0x027d, code skipped:
            r29 = false;
     */
    /* JADX WARNING: Missing block: B:111:0x0280, code skipped:
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:112:0x0281, code skipped:
            if (r5 >= r2) goto L_0x02bb;
     */
    /* JADX WARNING: Missing block: B:113:0x0283, code skipped:
            r30 = r2;
            r2 = r0.getPart(r5);
            r31 = r7 + r2.getDataLength();
            persistPart(r2, r3, r44);
            r32 = r0;
            r0 = getPartContentType(r2);
     */
    /* JADX WARNING: Missing block: B:114:0x029c, code skipped:
            if (r0 == null) goto L_0x02b2;
     */
    /* JADX WARNING: Missing block: B:115:0x029e, code skipped:
            r33 = r2;
     */
    /* JADX WARNING: Missing block: B:116:0x02a6, code skipped:
            if (com.google.android.mms.ContentType.APP_SMIL.equals(r0) != false) goto L_0x02b2;
     */
    /* JADX WARNING: Missing block: B:118:0x02ae, code skipped:
            if (com.google.android.mms.ContentType.TEXT_PLAIN.equals(r0) != false) goto L_0x02b2;
     */
    /* JADX WARNING: Missing block: B:119:0x02b0, code skipped:
            r29 = false;
     */
    /* JADX WARNING: Missing block: B:120:0x02b2, code skipped:
            r5 = r5 + 1;
            r2 = r30;
            r7 = r31;
            r0 = r32;
     */
    /* JADX WARNING: Missing block: B:121:0x02bb, code skipped:
            r32 = r0;
            r31 = r7;
            r7 = r44;
            r0 = r31;
     */
    /* JADX WARNING: Missing block: B:122:0x02c4, code skipped:
            r32 = r0;
            r29 = true;
            r7 = r44;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:123:0x02cf, code skipped:
            r29 = true;
            r7 = r44;
            r32 = r20;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:124:0x02d9, code skipped:
            r2 = "text_only";
     */
    /* JADX WARNING: Missing block: B:125:0x02db, code skipped:
            if (r29 == false) goto L_0x02df;
     */
    /* JADX WARNING: Missing block: B:126:0x02dd, code skipped:
            r5 = 1;
     */
    /* JADX WARNING: Missing block: B:127:0x02df, code skipped:
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:128:0x02e0, code skipped:
            r8.put(r2, java.lang.Integer.valueOf(r5));
     */
    /* JADX WARNING: Missing block: B:129:0x02ed, code skipped:
            if (r8.getAsInteger("m_size") != null) goto L_0x02f8;
     */
    /* JADX WARNING: Missing block: B:130:0x02ef, code skipped:
            r8.put("m_size", java.lang.Integer.valueOf(r0));
     */
    /* JADX WARNING: Missing block: B:131:0x02f8, code skipped:
            r8.put("sub_id", java.lang.Integer.valueOf(r45));
     */
    /* JADX WARNING: Missing block: B:132:0x0302, code skipped:
            if (r15 == false) goto L_0x0322;
     */
    /* JADX WARNING: Missing block: B:133:0x0304, code skipped:
            r5 = r9;
            r34 = r0;
            r35 = r3;
            r19 = 0;
            r0 = r8;
            com.google.android.mms.util.SqliteWrapper.update(r1.mContext, r1.mContentResolver, r5, r8, null, null);
            r2 = r5;
            r3 = r26;
     */
    /* JADX WARNING: Missing block: B:134:0x0322, code skipped:
            r34 = r0;
            r35 = r3;
            r19 = 0;
            r2 = com.google.android.mms.util.SqliteWrapper.insert(r1.mContext, r1.mContentResolver, r9, r8);
     */
    /* JADX WARNING: Missing block: B:135:0x0331, code skipped:
            if (r2 == null) goto L_0x03b2;
     */
    /* JADX WARNING: Missing block: B:136:0x0333, code skipped:
            r3 = android.content.ContentUris.parseId(r2);
     */
    /* JADX WARNING: Missing block: B:137:0x0337, code skipped:
            r0 = new android.content.ContentValues(1);
            r0.put("mid", java.lang.Long.valueOf(r3));
            r5 = r1.mContext;
            r6 = r1.mContentResolver;
            r7 = new java.lang.StringBuilder();
            r7.append("content://mms/");
            r37 = r11;
            r7.append(r35);
            r7.append("/part");
            com.google.android.mms.util.SqliteWrapper.update(r5, r6, android.net.Uri.parse(r7.toString()), r0, null, null);
     */
    /* JADX WARNING: Missing block: B:138:0x0376, code skipped:
            if (r15 != false) goto L_0x0390;
     */
    /* JADX WARNING: Missing block: B:139:0x0378, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append(r9);
            r5.append("/");
            r5.append(r3);
            r2 = android.net.Uri.parse(r5.toString());
     */
    /* JADX WARNING: Missing block: B:140:0x0390, code skipped:
            r5 = ADDRESS_FIELDS;
            r6 = r5.length;
            r7 = r19;
     */
    /* JADX WARNING: Missing block: B:141:0x0395, code skipped:
            if (r7 >= r6) goto L_0x03af;
     */
    /* JADX WARNING: Missing block: B:142:0x0397, code skipped:
            r8 = r5[r7];
            r38 = r0;
            r0 = (com.google.android.mms.pdu.EncodedStringValue[]) r14.get(java.lang.Integer.valueOf(r8));
     */
    /* JADX WARNING: Missing block: B:143:0x03a5, code skipped:
            if (r0 == null) goto L_0x03aa;
     */
    /* JADX WARNING: Missing block: B:144:0x03a7, code skipped:
            persistAddress(r3, r8, r0);
     */
    /* JADX WARNING: Missing block: B:145:0x03aa, code skipped:
            r7 = r7 + 1;
            r0 = r38;
     */
    /* JADX WARNING: Missing block: B:146:0x03af, code skipped:
            r38 = r0;
     */
    /* JADX WARNING: Missing block: B:147:0x03b1, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:148:0x03b2, code skipped:
            r37 = r11;
            r10 = r35;
     */
    /* JADX WARNING: Missing block: B:149:0x03bd, code skipped:
            throw new com.google.android.mms.MmsException("persist() failed: return null.");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles, int subscription) throws MmsException {
        Throwable th;
        long j;
        GenericPdu genericPdu = pdu;
        Uri uri2 = uri;
        boolean z = groupMmsEnabled;
        if (uri2 != null) {
            long msgId = -1;
            try {
                msgId = ContentUris.parseId(uri);
            } catch (NumberFormatException e) {
            }
            long msgId2 = msgId;
            boolean existingUri = msgId2 != -1;
            if (existingUri || MESSAGE_BOX_MAP.get(uri2) != null) {
                synchronized (PDU_CACHE_INSTANCE) {
                    try {
                        if (PDU_CACHE_INSTANCE.isUpdating(uri2)) {
                            try {
                                PDU_CACHE_INSTANCE.wait();
                            } catch (InterruptedException e2) {
                                Log.e(TAG, "persist1: ", e2);
                            } catch (Throwable th2) {
                                th = th2;
                                j = msgId2;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        j = msgId2;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            }
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        throw new MmsException("Uri may not be null.");
    }

    private void loadRecipients(int addressType, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, boolean excludeMyNumber) {
        HashSet<String> hashSet = recipients;
        EncodedStringValue[] array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addressType));
        if (array != null) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(this.mContext);
            Set<String> myPhoneNumbers = new HashSet();
            int i = 0;
            if (excludeMyNumber) {
                for (int subid : subscriptionManager.getActiveSubscriptionIdList()) {
                    String myNumber = this.mTelephonyManager.getLine1Number(subid);
                    if (TextUtils.isEmpty(myNumber)) {
                        ContentResolver contentResolver = this.mContentResolver;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("localNumberFromDb_");
                        stringBuilder.append(subid);
                        myNumber = Secure.getString(contentResolver, stringBuilder.toString());
                    }
                    if (myNumber != null) {
                        myPhoneNumbers.add(myNumber);
                    }
                }
            }
            int length = array.length;
            while (i < length) {
                EncodedStringValue v = array[i];
                if (v != null) {
                    String number = v.getString();
                    boolean isAddNumber = true;
                    if (excludeMyNumber) {
                        for (String myNumber2 : myPhoneNumbers) {
                            if (PhoneNumberUtils.compare(number, myNumber2)) {
                                isAddNumber = false;
                                break;
                            }
                        }
                        if (isAddNumber && !hashSet.contains(number)) {
                            hashSet.add(number);
                        }
                    } else if (!hashSet.contains(number)) {
                        hashSet.add(number);
                    }
                }
                i++;
            }
        }
    }

    public Uri move(Uri from, Uri to) throws MmsException {
        long msgId = ContentUris.parseId(from);
        if (msgId != -1) {
            Integer msgBox = (Integer) MESSAGE_BOX_MAP.get(to);
            if (msgBox != null) {
                ContentValues values = new ContentValues(1);
                values.put("msg_box", msgBox);
                SqliteWrapper.update(this.mContext, this.mContentResolver, from, values, null, null);
                return ContentUris.withAppendedId(to, msgId);
            }
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        throw new MmsException("Error! ID of the message: -1.");
    }

    public static String toIsoString(byte[] bytes) {
        try {
            return new String(bytes, CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return "";
        }
    }

    public static byte[] getBytes(String data) {
        try {
            return data.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return new byte[0];
        }
    }

    public void release() {
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse(TEMPORARY_DRM_OBJECT_URI), null, null);
        this.mDrmManagerClient.release();
    }

    public Cursor getPendingMessages(long dueTime) {
        Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        return SqliteWrapper.query(this.mContext, this.mContentResolver, uriBuilder.build(), null, "err_type < ? AND due_time <= ?", new String[]{String.valueOf(10), String.valueOf(dueTime)}, "due_time");
    }

    private void filterMyNumber(boolean groupMmsEnabled, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, int subId) {
        if (groupMmsEnabled && recipients.size() != 1 && recipients.size() <= 2) {
            String myNumber = this.mTelephonyManager.getLine1Number(subId);
            if (TextUtils.isEmpty(myNumber)) {
                ContentResolver contentResolver = this.mContentResolver;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("localNumberFromDb_");
                stringBuilder.append(subId);
                myNumber = Secure.getString(contentResolver, stringBuilder.toString());
            }
            if (TextUtils.isEmpty(myNumber)) {
                EncodedStringValue[] array_to = (EncodedStringValue[]) addressMap.get(Integer.valueOf(151));
                EncodedStringValue[] array_from = (EncodedStringValue[]) addressMap.get(Integer.valueOf(137));
                if (array_to != null && array_from != null) {
                    String number_from = "";
                    int i = 0;
                    for (EncodedStringValue v : array_from) {
                        if (v != null && !TextUtils.isEmpty(v.getString())) {
                            number_from = v.getString();
                            break;
                        }
                    }
                    int length = array_to.length;
                    while (i < length) {
                        EncodedStringValue v2 = array_to[i];
                        if (v2 != null && !number_from.equals(v2.getString()) && recipients.contains(v2.getString())) {
                            recipients.remove(v2.getString());
                            break;
                        }
                        i++;
                    }
                }
            }
        }
    }
}
