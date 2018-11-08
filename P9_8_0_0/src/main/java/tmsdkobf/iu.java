package tmsdkobf;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import java.util.HashMap;
import java.util.Map.Entry;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.f;

public final class iu {
    private static iu sd;
    private Handler mHandler = new Handler(TMSDKContext.getApplicaionContext().getMainLooper());
    private is se = new is();
    private b sf;
    private a sg = new a(this, this.mHandler);
    private in sh;
    private volatile boolean si;
    private boolean sj = false;
    private kg sk;

    final class a extends ContentObserver {
        final /* synthetic */ iu sl;

        public a(iu iuVar, Handler handler) {
            this.sl = iuVar;
            super(handler);
        }

        public void bZ() {
            try {
                TMSDKContext.getApplicaionContext().getContentResolver().registerContentObserver(Contacts.CONTENT_URI, true, this);
            } catch (Object -l_2_R) {
                -l_2_R.printStackTrace();
            }
        }

        public void onChange(boolean z) {
            if (this.sl.si) {
                this.sl.sf.i(true);
                this.sl.sh.a(this.sl.sf);
                synchronized (this.sl) {
                    this.sl.sf = null;
                    this.sl.sf = new b(this.sl);
                }
                this.sl.sh.addTask(this.sl.sf, null);
            }
        }
    }

    final class b implements Runnable {
        final /* synthetic */ iu sl;
        private volatile boolean sm;
        private boolean sn;

        b(iu iuVar) {
            this(iuVar, false);
        }

        b(iu iuVar, boolean z) {
            this.sl = iuVar;
            this.sn = z;
        }

        private void ca() {
            Object -l_5_R;
            f.d("ContactsLookupCache", "reCache() started");
            Object -l_1_R = new HashMap();
            HashMap -l_2_R = new HashMap();
            Cursor cursor = null;
            try {
                cursor = this.sl.sk.query(Phone.CONTENT_URI, new String[]{"data1", "contact_id"}, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        -l_5_R = cursor.getString(0);
                        if (!(-l_5_R == null || iu.aN(-l_5_R) || ((Long) -l_1_R.put(-l_5_R, Long.valueOf(cursor.getLong(1)))) == null)) {
                            f.g("ContactsLookupCache", "Duplicated number " + -l_5_R);
                        }
                    }
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Object -l_4_R) {
                            -l_4_R.printStackTrace();
                        }
                    }
                    if (this.sm) {
                        -l_1_R.clear();
                        return;
                    }
                    Cursor cursor2 = null;
                    try {
                        cursor2 = this.sl.sk.query(Contacts.CONTENT_URI, new String[]{"_id", "display_name"}, "has_phone_number=1", null, null);
                        if (cursor2 != null) {
                            while (cursor2.moveToNext()) {
                                long -l_6_J = cursor2.getLong(0);
                                HashMap hashMap = -l_2_R;
                                hashMap.put(Long.valueOf(-l_6_J), cursor2.getString(1));
                            }
                            if (cursor2 != null) {
                                try {
                                    cursor2.close();
                                } catch (Object -l_5_R2) {
                                    -l_5_R2.printStackTrace();
                                }
                            }
                            if (this.sm) {
                                -l_1_R.clear();
                                -l_2_R.clear();
                                return;
                            }
                            is -l_5_R3 = new is();
                            for (Entry -l_7_R : -l_1_R.entrySet()) {
                                String -l_9_R = (String) -l_2_R.get((Long) -l_7_R.getValue());
                                if (-l_9_R == null) {
                                    -l_9_R = "";
                                }
                                -l_5_R3.i((String) -l_7_R.getKey(), -l_9_R);
                            }
                            -l_1_R.clear();
                            -l_2_R.clear();
                            if (this.sm) {
                                -l_5_R3.clear();
                                return;
                            }
                            synchronized (this.sl) {
                                this.sl.se.clear();
                                this.sl.se = -l_5_R3;
                            }
                            f.d("ContactsLookupCache", "reCache() finished");
                            return;
                        }
                        f.e("ContactsLookupCache", "null nameCursor");
                        if (cursor2 != null) {
                            try {
                                cursor2.close();
                            } catch (Object -l_6_R) {
                                -l_6_R.printStackTrace();
                            }
                        }
                    } catch (Object -l_5_R22) {
                        f.b("ContactsLookupCache", "reCache", -l_5_R22);
                        if (cursor2 != null) {
                            try {
                                cursor2.close();
                            } catch (Object -l_5_R222) {
                                -l_5_R222.printStackTrace();
                            }
                        }
                    } catch (Throwable th) {
                        if (cursor2 != null) {
                            try {
                                cursor2.close();
                            } catch (Object -l_12_R) {
                                -l_12_R.printStackTrace();
                            }
                        }
                    }
                } else {
                    f.e("ContactsLookupCache", "null numberCursor");
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Object -l_5_R2222) {
                            -l_5_R2222.printStackTrace();
                        }
                    }
                }
            } catch (Object -l_4_R2) {
                f.b("ContactsLookupCache", "reCache", -l_4_R2);
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Object -l_4_R22) {
                        -l_4_R22.printStackTrace();
                    }
                }
            } catch (Throwable th2) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Object -l_10_R) {
                        -l_10_R.printStackTrace();
                    }
                }
            }
        }

        void i(boolean z) {
            this.sm = z;
        }

        public void run() {
            i(false);
            try {
                Thread.sleep(!this.sn ? 5000 : 20000);
            } catch (InterruptedException e) {
                if (this.sm) {
                    return;
                }
            }
            ca();
            this.sl.si = true;
        }
    }

    private iu() {
        this.sg.bZ();
        this.sh = im.bJ();
        this.sk = TMServiceFactory.getSysDBService();
    }

    private static boolean aN(String str) {
        boolean z = false;
        if (str == null) {
            return false;
        }
        int -l_1_I = str.indexOf(64);
        if (-l_1_I == -1 || -l_1_I > str.length() - 3) {
            return false;
        }
        if (str.indexOf(46, -l_1_I + 2) >= 0) {
            z = true;
        }
        return z;
    }

    public static iu bY() {
        if (sd == null) {
            Object -l_0_R = iu.class;
            synchronized (iu.class) {
                if (sd == null) {
                    sd = new iu();
                }
            }
        }
        return sd;
    }

    public synchronized String aL(String str) {
        if (str == null) {
            return null;
        }
        if (this.si) {
            return this.se.getName(str);
        }
        Object -l_2_R = new String[]{"display_name", "number"};
        Cursor cursor = null;
        String str2 = null;
        try {
            Object -l_4_R = this.sk.query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)), -l_2_R, null, null, null);
            if (-l_4_R != null) {
                if (-l_4_R.moveToNext()) {
                    str2 = -l_4_R.getString(0);
                }
            }
            if (-l_4_R != null) {
                try {
                    -l_4_R.close();
                } catch (Object -l_6_R) {
                    f.b("ContactsLookupCache", "closing Cursor", -l_6_R);
                }
            }
        } catch (Object -l_6_R2) {
            f.b("ContactsLookupCache", "lookupName", -l_6_R2);
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_6_R22) {
                    f.b("ContactsLookupCache", "closing Cursor", -l_6_R22);
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_8_R) {
                    f.b("ContactsLookupCache", "closing Cursor", -l_8_R);
                }
            }
        }
        Object -l_6_R222 = iu.class;
        synchronized (iu.class) {
            if (!this.sj) {
                this.sf = new b(this, true);
                this.sh.a(1, this.sf, null);
                this.sj = true;
            }
            return str2;
        }
    }

    public boolean aM(String str) {
        return aL(str) != null;
    }
}
