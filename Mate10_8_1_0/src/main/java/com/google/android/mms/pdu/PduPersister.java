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
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

public class PduPersister {
    static final /* synthetic */ boolean -assertionsDisabled = (PduPersister.class.desiredAssertionStatus() ^ 1);
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
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(186), Integer.valueOf(11));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(134), Integer.valueOf(12));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(140), Integer.valueOf(13));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(141), Integer.valueOf(14));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(143), Integer.valueOf(15));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(144), Integer.valueOf(16));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(155), Integer.valueOf(17));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(145), Integer.valueOf(18));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(153), Integer.valueOf(19));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(149), Integer.valueOf(20));
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(186), "ct_cls");
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
        Context application = actThread != null ? actThread.getApplication() : null;
        if (application == null) {
            application = context.getApplicationContext();
        }
        if (sPersister == null) {
            sPersister = new PduPersister(application);
        } else if (!application.equals(sPersister.mContext)) {
            Log.w(TAG, "PduPersister create a new one. may cause memory leak");
            sPersister.release();
            sPersister = new PduPersister(application);
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

    private PduPart[] loadParts(long msgId, int loadType) throws MmsException {
        Cursor c;
        if (loadType == 1) {
            c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://fav-mms/" + msgId + "/part"), PART_PROJECTION, null, null, null);
        } else {
            c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/part"), PART_PROJECTION, null, null, null);
        }
        if (c != null) {
            if (c.getCount() != 0) {
                PduPart[] parts = new PduPart[c.getCount()];
                int partIdx = 0;
                while (c.moveToNext()) {
                    PduPart part = new PduPart();
                    Integer charset = getIntegerFromPartColumn(c, 1);
                    if (charset != null) {
                        part.setCharset(charset.intValue());
                    }
                    byte[] contentDisposition = getByteArrayFromPartColumn(c, 2);
                    if (contentDisposition != null) {
                        part.setContentDisposition(contentDisposition);
                    }
                    byte[] contentId = getByteArrayFromPartColumn(c, 3);
                    if (contentId != null) {
                        part.setContentId(contentId);
                    }
                    byte[] contentLocation = getByteArrayFromPartColumn(c, 4);
                    if (contentLocation != null) {
                        part.setContentLocation(contentLocation);
                    }
                    byte[] contentType = getByteArrayFromPartColumn(c, 5);
                    if (contentType != null) {
                        Uri partURI;
                        part.setContentType(contentType);
                        byte[] fileName = getByteArrayFromPartColumn(c, 6);
                        if (fileName != null) {
                            part.setFilename(fileName);
                        }
                        byte[] name = getByteArrayFromPartColumn(c, 7);
                        if (name != null) {
                            part.setName(name);
                        }
                        long partId = c.getLong(0);
                        if (loadType == 1) {
                            partURI = Uri.parse("content://fav-mms/part/" + partId);
                        } else {
                            partURI = Uri.parse("content://mms/part/" + partId);
                        }
                        part.setDataUri(partURI);
                        String type = toIsoString(contentType);
                        if (!(ContentType.isImageType(type) || (ContentType.isAudioType(type) ^ 1) == 0 || (ContentType.isVideoType(type) ^ 1) == 0)) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            InputStream inputStream = null;
                            if (ContentType.TEXT_PLAIN.equals(type) || ContentType.APP_SMIL.equals(type) || ContentType.TEXT_HTML.equals(type)) {
                                String text = c.getString(8);
                                if (charset != null && charset.intValue() == 3) {
                                    charset = Integer.valueOf(106);
                                }
                                byte[] blob;
                                if (!"true".equals("true")) {
                                    if (text == null) {
                                        text = "";
                                    }
                                    blob = new EncodedStringValue(text).getTextString();
                                    baos.write(blob, 0, blob.length);
                                } else if (charset == null || charset.intValue() == 0) {
                                    if (text == null) {
                                        text = "";
                                    }
                                    blob = new EncodedStringValue(text).getTextString();
                                    baos.write(blob, 0, blob.length);
                                } else {
                                    try {
                                        int intValue = charset.intValue();
                                        if (text == null) {
                                            text = "";
                                        }
                                        blob = new EncodedStringValue(intValue, text).getTextString();
                                        baos.write(blob, 0, blob.length);
                                    } catch (Throwable e) {
                                        Log.e(TAG, "Failed to EncodedStringValue: ", e);
                                    } catch (Throwable e2) {
                                        Log.e(TAG, "Failed to EncodedStringValue: ", e2);
                                    } catch (Throwable th) {
                                        if (c != null) {
                                            c.close();
                                        }
                                    }
                                }
                            } else {
                                try {
                                    inputStream = this.mContentResolver.openInputStream(partURI);
                                    byte[] buffer = new byte[256];
                                    for (int len = inputStream.read(buffer); len >= 0; len = inputStream.read(buffer)) {
                                        baos.write(buffer, 0, len);
                                    }
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (Throwable e3) {
                                            Log.e(TAG, "Failed to close stream", e3);
                                        }
                                    }
                                } catch (Throwable e32) {
                                    Log.e(TAG, "Failed to load part data", e32);
                                    c.close();
                                    throw new MmsException(e32);
                                } catch (Throwable th2) {
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (Throwable e322) {
                                            Log.e(TAG, "Failed to close stream", e322);
                                        }
                                    }
                                }
                            }
                            if (baos != null) {
                                part.setData(baos.toByteArray());
                            } else {
                                continue;
                            }
                        }
                        int partIdx2 = partIdx + 1;
                        parts[partIdx] = part;
                        partIdx = partIdx2;
                    } else {
                        throw new MmsException("Content-Type must be set.");
                    }
                }
                if (c != null) {
                    c.close();
                }
                return parts;
            }
        }
        if (c != null) {
            c.close();
        }
        return null;
    }

    private void loadAddress(long msgId, PduHeaders headers, int loadType) {
        Cursor c;
        if (loadType == 1) {
            c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://fav-mms/" + msgId + "/addr"), new String[]{"address", "charset", "type"}, null, null, null);
        } else {
            c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), new String[]{"address", "charset", "type"}, null, null, null);
        }
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    String addr = c.getString(0);
                    if (!TextUtils.isEmpty(addr)) {
                        int addrType = c.getInt(2);
                        switch (addrType) {
                            case 129:
                            case 130:
                            case 151:
                                headers.appendEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                                break;
                            case 137:
                                headers.setEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                                break;
                            default:
                                Log.e(TAG, "Unknown address type: " + addrType);
                                break;
                        }
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public GenericPdu load(Uri uri) throws MmsException {
        Throwable th;
        PduCacheEntry cacheEntry = null;
        int loadType = 0;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                try {
                    if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                        PDU_CACHE_INSTANCE.wait();
                        cacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri);
                        if (cacheEntry != null) {
                            GenericPdu pdu = cacheEntry.getPdu();
                            synchronized (PDU_CACHE_INSTANCE) {
                                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                PDU_CACHE_INSTANCE.notifyAll();
                            }
                            return pdu;
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "load: ", e);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
                PduCacheEntry cacheEntry2 = cacheEntry;
                try {
                    PDU_CACHE_INSTANCE.setUpdating(uri, true);
                    try {
                    } catch (Throwable th3) {
                        th = th3;
                        cacheEntry = cacheEntry2;
                        synchronized (PDU_CACHE_INSTANCE) {
                            PDU_CACHE_INSTANCE.setUpdating(uri, false);
                            PDU_CACHE_INSTANCE.notifyAll();
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    cacheEntry = cacheEntry2;
                    throw th;
                }
            }
        } catch (Throwable th5) {
            th = th5;
            synchronized (PDU_CACHE_INSTANCE) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
            }
            throw th;
        }
    }

    private void persistAddress(long msgId, int type, EncodedStringValue[] array) {
        ContentValues[] allValues = new ContentValues[array.length];
        Uri uri = Uri.parse("content://mms/" + msgId + "/addr");
        int i = 0;
        int length = array.length;
        int idx = 0;
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
        Uri uri = Uri.parse("content://mms/" + msgId + "/part");
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
            if (res == null) {
                throw new MmsException("Failed to persist part, return null.");
            }
            persistData(part, res, contentType, preOpenedFiles);
            part.setDataUri(res);
            return res;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    private void persistData(PduPart part, Uri uri, String contentType, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        Throwable e;
        Throwable e2;
        Throwable th;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        DrmConvertSession drmConvertSession = null;
        String str = null;
        try {
            byte[] data = part.getData();
            if (!ContentType.TEXT_PLAIN.equals(contentType) && !ContentType.APP_SMIL.equals(contentType) && !ContentType.TEXT_HTML.equals(contentType)) {
                boolean isDrm = DownloadDrmHelper.isDrmConvertNeeded(contentType);
                if (isDrm) {
                    if (uri != null) {
                        try {
                            str = convertUriToPath(this.mContext, uri);
                            if (new File(str).length() > 0) {
                                return;
                            }
                        } catch (Throwable e3) {
                            Log.e(TAG, "Can't get file info for: " + part.getDataUri(), e3);
                        }
                    }
                    drmConvertSession = DrmConvertSession.open(this.mContext, contentType);
                    if (drmConvertSession == null) {
                        throw new MmsException("Mimetype " + contentType + " can not be converted.");
                    }
                }
                outputStream = this.mContentResolver.openOutputStream(uri);
                Uri dataUri;
                byte[] buffer;
                int len;
                byte[] convertedData;
                if (data == null) {
                    dataUri = part.getDataUri();
                    if (dataUri != null && !dataUri.equals(uri)) {
                        if (preOpenedFiles != null) {
                            if (preOpenedFiles.containsKey(dataUri)) {
                                inputStream = (InputStream) preOpenedFiles.get(dataUri);
                            }
                        }
                        if (inputStream == null) {
                            inputStream = this.mContentResolver.openInputStream(dataUri);
                        }
                        buffer = new byte[RadioAccessFamily.EVDO_B];
                        while (true) {
                            len = inputStream.read(buffer);
                            if (len == -1) {
                                break;
                            } else if (isDrm) {
                                convertedData = drmConvertSession.convert(buffer, len);
                                if (convertedData != null) {
                                    outputStream.write(convertedData, 0, convertedData.length);
                                } else {
                                    throw new MmsException("Error converting drm data.");
                                }
                            } else {
                                outputStream.write(buffer, 0, len);
                            }
                        }
                    } else {
                        Log.w(TAG, "Can't find data for this part.");
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (Throwable e4) {
                                Log.e(TAG, "IOException while closing: " + outputStream, e4);
                            }
                        }
                        if (drmConvertSession != null) {
                            drmConvertSession.close(str);
                            SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + new File(str).getName()), new ContentValues(0), null, null);
                        }
                        return;
                    }
                } else if (isDrm) {
                    dataUri = uri;
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    try {
                        buffer = new byte[RadioAccessFamily.EVDO_B];
                        while (true) {
                            len = byteArrayInputStream.read(buffer);
                            if (len == -1) {
                                break;
                            }
                            convertedData = drmConvertSession.convert(buffer, len);
                            if (convertedData != null) {
                                outputStream.write(convertedData, 0, convertedData.length);
                            } else {
                                throw new MmsException("Error converting drm data.");
                            }
                        }
                        inputStream = byteArrayInputStream;
                    } catch (FileNotFoundException e5) {
                        e2 = e5;
                        inputStream = byteArrayInputStream;
                        try {
                            Log.e(TAG, "Failed to open Input/Output stream.", e2);
                            throw new MmsException(e2);
                        } catch (Throwable th2) {
                            th = th2;
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (Throwable e42) {
                                    Log.e(TAG, "IOException while closing: " + outputStream, e42);
                                }
                            }
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Throwable e422) {
                                    Log.e(TAG, "IOException while closing: " + inputStream, e422);
                                }
                            }
                            if (drmConvertSession != null) {
                                drmConvertSession.close(str);
                                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + new File(str).getName()), new ContentValues(0), null, null);
                            }
                            throw th;
                        }
                    } catch (IOException e6) {
                        e422 = e6;
                        inputStream = byteArrayInputStream;
                        Log.e(TAG, "Failed to read/write data.", e422);
                        throw new MmsException(e422);
                    } catch (Throwable th22) {
                        th = th22;
                        inputStream = byteArrayInputStream;
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (drmConvertSession != null) {
                            drmConvertSession.close(str);
                            SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + new File(str).getName()), new ContentValues(0), null, null);
                        }
                        throw th;
                    }
                } else {
                    outputStream.write(data);
                }
            } else if ("true".equals("true")) {
                int charset = part.getCharset();
                cv = new ContentValues();
                if (charset != 0) {
                    cv.put("text", data != null ? new EncodedStringValue(charset, data).getString() : "");
                } else {
                    String string;
                    String str2 = "text";
                    if (data != null) {
                        string = new EncodedStringValue(data).getString();
                    } else {
                        string = "";
                    }
                    cv.put(str2, string);
                }
                if (this.mContentResolver.update(uri, cv, null, null) != 1) {
                    throw new MmsException("unable to update " + uri.toString());
                }
            } else {
                cv = new ContentValues();
                cv.put("text", new EncodedStringValue(data).getString());
                if (this.mContentResolver.update(uri, cv, null, null) != 1) {
                    throw new MmsException("unable to update " + uri.toString());
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable e4222) {
                    Log.e(TAG, "IOException while closing: " + outputStream, e4222);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable e42222) {
                    Log.e(TAG, "IOException while closing: " + inputStream, e42222);
                }
            }
            if (drmConvertSession != null) {
                drmConvertSession.close(str);
                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + new File(str).getName()), new ContentValues(0), null, null);
            }
        } catch (FileNotFoundException e7) {
            e2 = e7;
            Log.e(TAG, "Failed to open Input/Output stream.", e2);
            throw new MmsException(e2);
        } catch (IOException e8) {
            e42222 = e8;
            Log.e(TAG, "Failed to read/write data.", e42222);
            throw new MmsException(e42222);
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
                if (!(cursor == null || cursor.getCount() == 0)) {
                    if ((cursor.moveToFirst() ^ 1) == 0) {
                        String path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                        if (cursor == null) {
                            return path;
                        }
                        cursor.close();
                        return path;
                    }
                }
                throw new IllegalArgumentException("Given Uri could not be found in media store");
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
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), "type=" + type, null);
        persistAddress(msgId, type, array);
    }

    public void updateHeaders(Uri uri, SendReq sendReq) {
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (Throwable e) {
                    Log.e(TAG, "updateHeaders: ", e);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri);
        ContentValues values = new ContentValues(10);
        byte[] contentType = sendReq.getContentType();
        if (contentType != null) {
            values.put("ct_t", toIsoString(contentType));
        }
        long date = sendReq.getDate();
        if (date != -1) {
            values.put("date", Long.valueOf(date));
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
        int priority = sendReq.getPriority();
        if (priority != 0) {
            values.put("pri", Integer.valueOf(priority));
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
        if (messageSize > 0) {
            values.put("m_size", Long.valueOf(messageSize));
        }
        PduHeaders headers = sendReq.getPduHeaders();
        HashSet<String> recipients = new HashSet();
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == 137) {
                if (headers.getEncodedStringValue(addrType) != null) {
                    array = new EncodedStringValue[]{headers.getEncodedStringValue(addrType)};
                }
            } else {
                array = headers.getEncodedStringValues(addrType);
            }
            if (array != null) {
                updateAddress(ContentUris.parseId(uri), addrType, array);
                if (addrType == 151) {
                    for (EncodedStringValue v : array) {
                        if (v != null) {
                            recipients.add(v.getString());
                        }
                    }
                }
            }
        }
        if (!recipients.isEmpty()) {
            values.put("thread_id", Long.valueOf(Threads.getOrCreateThreadId(this.mContext, recipients)));
        }
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
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
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            if (part.getData() != null || (uri.equals(part.getDataUri()) ^ 1) != 0) {
                persistData(part, uri, contentType, preOpenedFiles);
                return;
            }
            return;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    public void updateParts(Uri uri, PduBody body, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        PduPart pduPart;
        try {
            PduPart part;
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "updateParts: ", e);
                    }
                    PduCacheEntry cacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri);
                    if (cacheEntry != null) {
                        ((MultimediaMessagePdu) cacheEntry.getPdu()).setBody(body);
                    }
                }
                PDU_CACHE_INSTANCE.setUpdating(uri, true);
            }
            ArrayList<PduPart> toBeCreated = new ArrayList();
            HashMap<Uri, PduPart> toBeUpdated = new HashMap();
            int partsNum = body.getPartsNum();
            StringBuilder filter = new StringBuilder().append('(');
            for (int i = 0; i < partsNum; i++) {
                part = body.getPart(i);
                Uri partUri = part.getDataUri();
                if (partUri == null || TextUtils.isEmpty(partUri.getAuthority()) || (partUri.getAuthority().startsWith("mms") ^ 1) != 0) {
                    toBeCreated.add(part);
                } else {
                    toBeUpdated.put(partUri, part);
                    if (filter.length() > 1) {
                        filter.append(" AND ");
                    }
                    filter.append(HbpcdLookup.ID);
                    filter.append("!=");
                    DatabaseUtils.appendEscapedSQLString(filter, partUri.getLastPathSegment());
                }
            }
            filter.append(')');
            long msgId = ContentUris.parseId(uri);
            pduPart = this.mContext;
            SqliteWrapper.delete(pduPart, this.mContentResolver, Uri.parse(Mms.CONTENT_URI + "/" + msgId + "/part"), filter.length() > 2 ? filter.toString() : null, null);
            for (PduPart part2 : toBeCreated) {
                persistPart(part2, msgId, preOpenedFiles);
            }
            for (Entry<Uri, PduPart> e2 : toBeUpdated.entrySet()) {
                pduPart = (PduPart) e2.getValue();
                updatePart((Uri) e2.getKey(), pduPart, preOpenedFiles);
            }
            PDU_CACHE_INSTANCE.setUpdating(uri, false);
            PDU_CACHE_INSTANCE.notifyAll();
        } finally {
            pduPart = PDU_CACHE_INSTANCE;
            synchronized (pduPart) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
            }
        }
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        return persist(pdu, uri, createThreadId, groupMmsEnabled, preOpenedFiles, 0);
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles, int subscription) throws MmsException {
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        long msgId = -1;
        try {
            msgId = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
        }
        boolean existingUri = msgId != -1;
        if (existingUri || MESSAGE_BOX_MAP.get(uri) != null) {
            int i;
            EncodedStringValue[] array;
            long dummyId;
            boolean textOnly;
            int messageSize;
            PduBody body;
            int partsNum;
            int i2;
            PduPart part;
            String contentType;
            Uri res;
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (Throwable e2) {
                        Log.e(TAG, "persist1: ", e2);
                    }
                }
            }
            PDU_CACHE_INSTANCE.purge(uri);
            PduHeaders header = pdu.getPduHeaders();
            ContentValues values = new ContentValues();
            for (Entry<Integer, String> e3 : ENCODED_STRING_COLUMN_NAME_MAP.entrySet()) {
                int field = ((Integer) e3.getKey()).intValue();
                EncodedStringValue encodedString = header.getEncodedStringValue(field);
                if (encodedString != null) {
                    String charsetColumn = (String) CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(field));
                    values.put((String) e3.getValue(), toIsoString(encodedString.getTextString()));
                    values.put(charsetColumn, Integer.valueOf(encodedString.getCharacterSet()));
                }
            }
            for (Entry<Integer, String> e32 : TEXT_STRING_COLUMN_NAME_MAP.entrySet()) {
                byte[] text = header.getTextString(((Integer) e32.getKey()).intValue());
                if (text != null) {
                    values.put((String) e32.getValue(), toIsoString(text));
                }
            }
            for (Entry<Integer, String> e322 : OCTET_COLUMN_NAME_MAP.entrySet()) {
                int b = header.getOctet(((Integer) e322.getKey()).intValue());
                if (b != 0) {
                    values.put((String) e322.getValue(), Integer.valueOf(b));
                }
            }
            for (Entry<Integer, String> e3222 : LONG_COLUMN_NAME_MAP.entrySet()) {
                long l = header.getLongInteger(((Integer) e3222.getKey()).intValue());
                if (l != -1) {
                    values.put((String) e3222.getValue(), Long.valueOf(l));
                }
            }
            HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap(ADDRESS_FIELDS.length);
            for (int addrType : ADDRESS_FIELDS) {
                if (addrType == 137) {
                    EncodedStringValue v = header.getEncodedStringValue(addrType);
                    String str = null;
                    if (v != null) {
                        str = v.getString();
                    }
                    array = (str == null || str.length() == 0) ? new EncodedStringValue[]{new EncodedStringValue(this.mContext.getString(33685936))} : new EncodedStringValue[]{v};
                } else {
                    array = header.getEncodedStringValues(addrType);
                }
                addressMap.put(Integer.valueOf(addrType), array);
            }
            HashSet<String> recipients = new HashSet();
            int msgType = pdu.getMessageType();
            if (!(msgType == 130 || msgType == 132)) {
                if (msgType == 128) {
                }
                dummyId = System.currentTimeMillis();
                textOnly = true;
                messageSize = 0;
                if (pdu instanceof MultimediaMessagePdu) {
                    body = ((MultimediaMessagePdu) pdu).getBody();
                    if (body != null) {
                        partsNum = body.getPartsNum();
                        if (partsNum > 2) {
                            textOnly = false;
                        }
                        for (i2 = 0; i2 < partsNum; i2++) {
                            part = body.getPart(i2);
                            messageSize += part.getDataLength();
                            persistPart(part, dummyId, preOpenedFiles);
                            contentType = getPartContentType(part);
                            if (!(contentType == null || (ContentType.APP_SMIL.equals(contentType) ^ 1) == 0 || (ContentType.TEXT_PLAIN.equals(contentType) ^ 1) == 0)) {
                                textOnly = false;
                            }
                        }
                    }
                }
                values.put("text_only", Integer.valueOf(textOnly ? 1 : 0));
                if (values.getAsInteger("m_size") == null) {
                    values.put("m_size", Integer.valueOf(messageSize));
                }
                values.put("sub_id", Integer.valueOf(subscription));
                if (existingUri) {
                    res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
                    if (res != null) {
                        throw new MmsException("persist() failed: return null.");
                    }
                    msgId = ContentUris.parseId(res);
                } else {
                    res = uri;
                    SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
                }
                values = new ContentValues(1);
                values.put("mid", Long.valueOf(msgId));
                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + dummyId + "/part"), values, null, null);
                if (!existingUri) {
                    res = Uri.parse(uri + "/" + msgId);
                }
                for (int addrType2 : ADDRESS_FIELDS) {
                    array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addrType2));
                    if (array != null) {
                        persistAddress(msgId, addrType2, array);
                    }
                }
                return res;
            }
            switch (msgType) {
                case 128:
                    loadRecipients(151, recipients, addressMap, false);
                    break;
                case 130:
                case 132:
                    loadRecipients(137, recipients, addressMap, false);
                    if (groupMmsEnabled && !(this.mHwCustPduPersister != null && (this.mHwCustPduPersister.isShortCodeFeatureEnabled() ^ 1) == 0 && (this.mHwCustPduPersister.hasShortCode((EncodedStringValue[]) addressMap.get(Integer.valueOf(151)), (EncodedStringValue[]) addressMap.get(Integer.valueOf(130))) ^ 1) == 0)) {
                        loadRecipients(151, recipients, addressMap, true);
                        filterMyNumber(groupMmsEnabled, recipients, addressMap, subscription);
                        loadRecipients(130, recipients, addressMap, true);
                        break;
                    }
            }
            long threadId = 0;
            if (createThreadId && (recipients.isEmpty() ^ 1) != 0) {
                threadId = Threads.getOrCreateThreadId(this.mContext, recipients);
            }
            values.put("thread_id", Long.valueOf(threadId));
            dummyId = System.currentTimeMillis();
            textOnly = true;
            messageSize = 0;
            if (pdu instanceof MultimediaMessagePdu) {
                body = ((MultimediaMessagePdu) pdu).getBody();
                if (body != null) {
                    partsNum = body.getPartsNum();
                    if (partsNum > 2) {
                        textOnly = false;
                    }
                    for (i2 = 0; i2 < partsNum; i2++) {
                        part = body.getPart(i2);
                        messageSize += part.getDataLength();
                        persistPart(part, dummyId, preOpenedFiles);
                        contentType = getPartContentType(part);
                        textOnly = false;
                    }
                }
            }
            if (textOnly) {
            }
            values.put("text_only", Integer.valueOf(textOnly ? 1 : 0));
            if (values.getAsInteger("m_size") == null) {
                values.put("m_size", Integer.valueOf(messageSize));
            }
            values.put("sub_id", Integer.valueOf(subscription));
            if (existingUri) {
                res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
                if (res != null) {
                    msgId = ContentUris.parseId(res);
                } else {
                    throw new MmsException("persist() failed: return null.");
                }
            }
            res = uri;
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            values = new ContentValues(1);
            values.put("mid", Long.valueOf(msgId));
            SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + dummyId + "/part"), values, null, null);
            if (existingUri) {
                res = Uri.parse(uri + "/" + msgId);
            }
            for (i = 0; i < r8; i++) {
                array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addrType2));
                if (array != null) {
                    persistAddress(msgId, addrType2, array);
                }
            }
            return res;
        }
        throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
    }

    private void loadRecipients(int addressType, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, boolean excludeMyNumber) {
        EncodedStringValue[] array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addressType));
        if (array != null) {
            String myNumber;
            SubscriptionManager subscriptionManager = SubscriptionManager.from(this.mContext);
            Set<String> myPhoneNumbers = new HashSet();
            if (excludeMyNumber) {
                for (int subid : subscriptionManager.getActiveSubscriptionIdList()) {
                    myNumber = this.mTelephonyManager.getLine1Number(subid);
                    if (TextUtils.isEmpty(myNumber)) {
                        myNumber = Secure.getString(this.mContentResolver, "localNumberFromDb_" + subid);
                    }
                    if (myNumber != null) {
                        myPhoneNumbers.add(myNumber);
                    }
                }
            }
            for (EncodedStringValue v : array) {
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
                        if (isAddNumber && (recipients.contains(number) ^ 1) != 0) {
                            recipients.add(number);
                        }
                    } else if (!recipients.contains(number)) {
                        recipients.add(number);
                    }
                }
            }
        }
    }

    public Uri move(Uri from, Uri to) throws MmsException {
        long msgId = ContentUris.parseId(from);
        if (msgId == -1) {
            throw new MmsException("Error! ID of the message: -1.");
        }
        Integer msgBox = (Integer) MESSAGE_BOX_MAP.get(to);
        if (msgBox == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        ContentValues values = new ContentValues(1);
        values.put("msg_box", msgBox);
        SqliteWrapper.update(this.mContext, this.mContentResolver, from, values, null, null);
        return ContentUris.withAppendedId(to, msgId);
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
        String[] selectionArgs = new String[]{String.valueOf(10), String.valueOf(dueTime)};
        return SqliteWrapper.query(this.mContext, this.mContentResolver, uriBuilder.build(), null, "err_type < ? AND due_time <= ?", selectionArgs, "due_time");
    }

    private void filterMyNumber(boolean groupMmsEnabled, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, int subId) {
        int i = 0;
        if (groupMmsEnabled && recipients.size() != 1 && recipients.size() <= 2) {
            String myNumber = this.mTelephonyManager.getLine1Number(subId);
            if (TextUtils.isEmpty(myNumber)) {
                myNumber = Secure.getString(this.mContentResolver, "localNumberFromDb_" + subId);
            }
            if (TextUtils.isEmpty(myNumber)) {
                EncodedStringValue[] array_to = (EncodedStringValue[]) addressMap.get(Integer.valueOf(151));
                EncodedStringValue[] array_from = (EncodedStringValue[]) addressMap.get(Integer.valueOf(137));
                if (array_to != null && array_from != null) {
                    EncodedStringValue v;
                    String number_from = "";
                    for (EncodedStringValue v2 : array_from) {
                        if (v2 != null && (TextUtils.isEmpty(v2.getString()) ^ 1) != 0) {
                            number_from = v2.getString();
                            break;
                        }
                    }
                    int length = array_to.length;
                    while (i < length) {
                        v2 = array_to[i];
                        if (v2 != null && (number_from.equals(v2.getString()) ^ 1) != 0 && recipients.contains(v2.getString())) {
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
