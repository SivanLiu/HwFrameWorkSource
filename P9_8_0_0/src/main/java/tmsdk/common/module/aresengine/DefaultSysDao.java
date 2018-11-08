package tmsdk.common.module.aresengine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.n;
import tmsdkobf.im;
import tmsdkobf.iu;
import tmsdkobf.kg;
import tmsdkobf.mg;

public final class DefaultSysDao extends AbsSysDao {
    private static volatile DefaultSysDao zU;
    private static final Uri zV = Uri.parse("content://icc/adn");
    private Context mContext;
    private kg sk = TMServiceFactory.getSysDBService();
    private c zW;
    private iu zX;

    private interface c {
        List<ContactEntity> eR();
    }

    final class a implements c {
        final /* synthetic */ DefaultSysDao Aa;
        private final Uri zY = People.CONTENT_URI;
        private final Uri zZ = Phones.CONTENT_URI;

        a(DefaultSysDao defaultSysDao) {
            this.Aa = defaultSysDao;
        }

        public List<ContactEntity> eR() {
            Object -l_4_R;
            Object -l_1_R = new ArrayList();
            Cursor -l_3_R = this.Aa.sk.query(this.zY, new String[]{"_id", "number", "display_name"}, null, null, "name asc");
            if (this.Aa.d(-l_3_R)) {
                while (!-l_3_R.isAfterLast()) {
                    try {
                        -l_4_R = -l_3_R.getString(1);
                        if (mg.bX(-l_4_R)) {
                            Object -l_5_R = new ContactEntity();
                            -l_5_R.id = -l_3_R.getInt(0);
                            -l_5_R.phonenum = -l_4_R.replaceAll("[ -]+", "");
                            -l_5_R.name = -l_3_R.getString(2);
                            -l_1_R.add(-l_5_R);
                        }
                        -l_3_R.moveToNext();
                    } catch (Object -l_4_R2) {
                        -l_4_R2.printStackTrace();
                    }
                }
            }
            this.Aa.e(-l_3_R);
            return -l_1_R;
        }
    }

    final class b implements c {
        final /* synthetic */ DefaultSysDao Aa;
        private Uri mContactUri = Contacts.CONTENT_URI;

        b(DefaultSysDao defaultSysDao) {
            this.Aa = defaultSysDao;
        }

        public List<ContactEntity> eR() {
            int -l_6_I;
            int -l_7_I;
            Object -l_1_R = new HashMap();
            Object -l_2_R = new HashMap();
            Object -l_3_R = new ArrayList();
            synchronized (this.mContactUri) {
                Cursor -l_5_R = this.Aa.sk.query(this.mContactUri, null, "has_phone_number=1", null, null);
                if (this.Aa.d(-l_5_R)) {
                    -l_6_I = -l_5_R.getColumnIndex("_id");
                    -l_7_I = -l_5_R.getColumnIndex("display_name");
                    while (!-l_5_R.isAfterLast()) {
                        try {
                            -l_1_R.put(Integer.valueOf(-l_5_R.getInt(-l_6_I)), -l_5_R.getString(-l_7_I));
                            -l_5_R.moveToNext();
                        } catch (Object -l_8_R) {
                            -l_8_R.printStackTrace();
                        }
                    }
                }
                this.Aa.e(-l_5_R);
            }
            synchronized (Phone.CONTENT_URI) {
                -l_5_R = this.Aa.sk.query(Phone.CONTENT_URI, null, null, null, null);
                if (this.Aa.d(-l_5_R)) {
                    -l_6_I = -l_5_R.getColumnIndex("data1");
                    -l_7_I = -l_5_R.getColumnIndex("contact_id");
                    while (!-l_5_R.isAfterLast()) {
                        try {
                            -l_2_R.put(-l_5_R.getString(-l_6_I), Integer.valueOf(-l_5_R.getInt(-l_7_I)));
                            -l_5_R.moveToNext();
                        } catch (Object -l_8_R2) {
                            -l_8_R2.printStackTrace();
                        }
                    }
                }
                this.Aa.e(-l_5_R);
            }
            for (Entry -l_5_R2 : -l_2_R.entrySet()) {
                String -l_6_R = (String) -l_5_R2.getKey();
                -l_7_I = ((Integer) -l_5_R2.getValue()).intValue();
                String -l_8_R3 = (String) -l_1_R.get(Integer.valueOf(-l_7_I));
                if (mg.bX(-l_6_R) && -l_6_R != null && -l_6_R.trim().length() > 0) {
                    ContactEntity -l_9_R = new ContactEntity();
                    -l_9_R.id = -l_7_I;
                    -l_9_R.name = -l_8_R3;
                    -l_9_R.phonenum = -l_6_R.replaceAll("[ -]+", "");
                    -l_3_R.add(-l_9_R);
                }
            }
            return -l_3_R;
        }
    }

    private DefaultSysDao(Context context) {
        int -l_2_I = 0;
        this.mContext = context;
        if (n.iX() >= 5) {
            -l_2_I = 1;
        }
        this.zW = -l_2_I == 0 ? new a(this) : new b(this);
        this.zX = iu.bY();
    }

    private ContentValues a(SmsEntity smsEntity, boolean z) {
        Object -l_3_R = new ContentValues();
        -l_3_R.put("address", smsEntity.phonenum);
        -l_3_R.put("body", smsEntity.body);
        -l_3_R.put("date", Long.valueOf(smsEntity.date));
        -l_3_R.put("read", Integer.valueOf(smsEntity.read));
        -l_3_R.put("type", Integer.valueOf(smsEntity.type));
        -l_3_R.put("service_center", smsEntity.serviceCenter == null ? "" : smsEntity.serviceCenter);
        if (!z) {
            Object -l_4_R = im.rE;
            if (-l_4_R != null) {
                Object -l_5_R = -l_4_R.ir();
                if (!(TextUtils.isEmpty(smsEntity.fromCard) || TextUtils.isEmpty(-l_5_R))) {
                    -l_3_R.put(-l_5_R, smsEntity.fromCard);
                }
                if (!TextUtils.isEmpty(smsEntity.fromCard)) {
                    try {
                        Object -l_7_R = -l_4_R.a(this.mContext, Integer.parseInt(smsEntity.fromCard));
                        Object -l_8_R = -l_4_R.is();
                        if (!(-l_8_R == null || -l_7_R == null)) {
                            -l_3_R.put(-l_8_R, -l_7_R);
                        }
                    } catch (Object -l_6_R) {
                        -l_6_R.printStackTrace();
                    }
                }
            }
        }
        return -l_3_R;
    }

    private SmsEntity b(Cursor cursor) {
        int -l_5_I;
        Object -l_4_R;
        Object -l_2_R = new SmsEntity();
        -l_2_R.id = cursor.getInt(cursor.getColumnIndex("_id"));
        -l_2_R.phonenum = cursor.getString(cursor.getColumnIndex("address"));
        if (-l_2_R.phonenum != null && -l_2_R.phonenum.contains(" ")) {
            Object -l_3_R = -l_2_R.phonenum.trim().split("\\s+");
            String str = "";
            -l_5_I = -l_3_R.length;
            if (-l_5_I > 0) {
                -l_4_R = -l_3_R[0];
                for (int -l_6_I = 1; -l_6_I < -l_5_I; -l_6_I++) {
                    -l_4_R = -l_4_R.concat(-l_3_R[-l_6_I]);
                }
                -l_2_R.phonenum = -l_4_R;
            }
        }
        -l_2_R.type = cursor.getInt(cursor.getColumnIndex("type"));
        -l_2_R.body = cursor.getString(cursor.getColumnIndex("body"));
        -l_2_R.date = cursor.getLong(cursor.getColumnIndex("date"));
        int -l_3_I = cursor.getColumnIndex("service_center");
        if (-l_3_I != -1) {
            -l_2_R.serviceCenter = cursor.getString(-l_3_I);
        }
        -l_4_R = im.bM();
        if (!(-l_4_R == null || -l_4_R.ir() == null)) {
            -l_5_I = cursor.getColumnIndex(-l_4_R.ir());
            if (-l_5_I > 0) {
                -l_2_R.fromCard = cursor.getString(-l_5_I);
            }
        }
        return -l_2_R;
    }

    private CallLogEntity c(Cursor cursor) {
        Object -l_2_R = new CallLogEntity();
        -l_2_R.id = cursor.getInt(cursor.getColumnIndex("_id"));
        -l_2_R.phonenum = cursor.getString(cursor.getColumnIndex("number")).replaceAll("[ -]+", "");
        -l_2_R.type = cursor.getInt(cursor.getColumnIndex("type"));
        -l_2_R.duration = cursor.getLong(cursor.getColumnIndex("duration"));
        -l_2_R.date = cursor.getLong(cursor.getColumnIndex("date"));
        Object -l_3_R = im.bM();
        if (!(-l_3_R == null || -l_3_R.it() == null)) {
            int -l_4_I = cursor.getColumnIndex(-l_3_R.it());
            if (-l_4_I > -1) {
                -l_2_R.fromCard = cursor.getString(-l_4_I);
            }
        }
        return -l_2_R;
    }

    private boolean d(Cursor cursor) {
        return cursor != null && cursor.moveToFirst();
    }

    private void e(Cursor cursor) {
        if (cursor != null) {
            try {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static DefaultSysDao getInstance(Context context) {
        if (zU == null) {
            Object -l_1_R = DefaultSysDao.class;
            synchronized (DefaultSysDao.class) {
                if (zU == null) {
                    zU = new DefaultSysDao(context);
                }
            }
        }
        return zU;
    }

    public boolean contains(String str) {
        return this.zX.aM(str);
    }

    public List<CallLogEntity> getAllCallLog() {
        Object -l_1_R = new ArrayList();
        synchronized (Calls.CONTENT_URI) {
            Cursor cursor = null;
            try {
                cursor = this.sk.query(Calls.CONTENT_URI, null, null, null, "date DESC");
                if (d(cursor)) {
                    while (!cursor.isAfterLast()) {
                        if (mg.bX(cursor.getString(cursor.getColumnIndex("number")))) {
                            -l_1_R.add(c(cursor));
                        }
                        cursor.moveToNext();
                    }
                }
                e(cursor);
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
                e(cursor);
            } catch (Throwable th) {
                e(cursor);
            }
        }
        return -l_1_R;
    }

    public List<ContactEntity> getAllContact() {
        try {
            return this.zW.eR();
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public CallLogEntity getLastCallLog() {
        CallLogEntity -l_1_R = null;
        Object obj = null;
        try {
            obj = this.sk.query(Calls.CONTENT_URI, null, null, null, "_id DESC LIMIT 1");
            if (d(obj)) {
                -l_1_R = c(obj);
            }
        } catch (Exception e) {
        }
        e(obj);
        if (-l_1_R != null) {
            -l_1_R.phonenum = -l_1_R.phonenum.length() != 1 ? -l_1_R.phonenum : "null";
        }
        return -l_1_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SmsEntity getLastInBoxSms(int i, int i2) {
        SmsEntity smsEntity = null;
        synchronized (Sms.CONTENT_URI) {
            try {
                Object -l_6_R = this.sk.query(Sms.CONTENT_URI, null, "type=1 AND read=" + i2, null, "_id DESC");
                if (d(-l_6_R)) {
                    smsEntity = b(-l_6_R);
                    long -l_7_J = System.currentTimeMillis() - smsEntity.date;
                    if (i >= 0) {
                        if ((-l_7_J > ((long) (i * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY)) ? 1 : null) == null) {
                        }
                        smsEntity = null;
                    }
                }
                e(-l_6_R);
            } catch (Object -l_5_R) {
                -l_5_R.printStackTrace();
            }
        }
        return smsEntity;
    }

    @Deprecated
    public SmsEntity getLastOutBoxSms(int i) {
        return getLastSentSms(i);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SmsEntity getLastSentSms(int i) {
        SmsEntity smsEntity = null;
        synchronized (Sms.CONTENT_URI) {
            Cursor cursor = null;
            try {
                cursor = this.sk.query(Sms.CONTENT_URI, null, "type=2", null, "_id DESC");
                if (d(cursor)) {
                    smsEntity = b(cursor);
                    long -l_6_J = System.currentTimeMillis() - smsEntity.date;
                    if (i >= 0) {
                        if ((-l_6_J > ((long) (i * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY)) ? 1 : null) == null) {
                        }
                        smsEntity = null;
                    }
                }
                e(cursor);
            } catch (Object -l_5_R) {
                -l_5_R.printStackTrace();
                e(cursor);
            } catch (Throwable th) {
                e(cursor);
            }
        }
        return smsEntity;
    }

    public List<ContactEntity> getSimContact() {
        Object -l_1_R = new String[]{"_id", "name", "traffic"};
        Object -l_2_R = new ArrayList();
        synchronized (zV) {
            Object -l_5_R;
            try {
                Object -l_4_R = this.sk.query(zV, -l_1_R, null, null, null);
                if (-l_4_R != null && d(-l_4_R)) {
                    while (!-l_4_R.isAfterLast()) {
                        -l_5_R = new ContactEntity();
                        -l_5_R.id = -l_4_R.getInt(-l_4_R.getColumnIndex("_id"));
                        -l_5_R.name = -l_4_R.getString(-l_4_R.getColumnIndex("name"));
                        -l_5_R.phonenum = -l_4_R.getString(-l_4_R.getColumnIndex("traffic"));
                        -l_5_R.isSimContact = true;
                        if (-l_5_R.phonenum != null) {
                            -l_2_R.add(-l_5_R);
                        }
                        -l_4_R.moveToNext();
                    }
                }
                e(-l_4_R);
            } catch (Object -l_5_R2) {
                -l_5_R2.printStackTrace();
                return -l_2_R;
            }
        }
        return -l_2_R;
    }

    public synchronized Uri insert(SmsEntity smsEntity) {
        return insert(smsEntity, false);
    }

    public synchronized Uri insert(SmsEntity smsEntity, boolean z) {
        Uri -l_3_R;
        -l_3_R = null;
        if (smsEntity.protocolType != 0) {
            if (smsEntity.protocolType != 2) {
            }
        }
        Object -l_4_R = a(smsEntity, z);
        synchronized (Sms.CONTENT_URI) {
            -l_3_R = this.sk.insert(Sms.CONTENT_URI, -l_4_R);
            if (-l_3_R == null) {
                -l_3_R = this.sk.insert(Uri.parse("content://sms/inbox"), -l_4_R);
            }
        }
        return -l_3_R;
    }

    public boolean remove(CallLogEntity callLogEntity) {
        boolean z = false;
        synchronized (Calls.CONTENT_URI) {
            if (this.sk.delete(Calls.CONTENT_URI, "_id=" + callLogEntity.id, null) > 0) {
                z = true;
            }
        }
        return z;
    }

    public boolean remove(SmsEntity smsEntity) {
        boolean z = false;
        Object -l_2_R = Sms.CONTENT_URI;
        if (smsEntity.protocolType == 1) {
            -l_2_R = Mms.CONTENT_URI;
        }
        Object -l_3_R = -l_2_R;
        synchronized (-l_2_R) {
            if (this.sk.delete(-l_2_R, "_id=" + smsEntity.id, null) > 0) {
                z = true;
            }
            return z;
        }
    }

    public boolean supportThisPhone() {
        return false;
    }
}
