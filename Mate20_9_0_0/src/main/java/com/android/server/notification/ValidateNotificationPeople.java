package com.android.server.notification;

import android.app.Person;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ValidateNotificationPeople implements NotificationSignalExtractor {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final boolean ENABLE_PEOPLE_VALIDATOR = true;
    private static final String[] LOOKUP_PROJECTION = new String[]{"_id", "starred"};
    private static final int MAX_PEOPLE = 10;
    static final float NONE = 0.0f;
    private static final int PEOPLE_CACHE_SIZE = 200;
    private static final String SETTING_ENABLE_PEOPLE_VALIDATOR = "validate_notification_people_enabled";
    static final float STARRED_CONTACT = 1.0f;
    private static final String TAG = "ValidateNoPeople";
    static final float VALID_CONTACT = 0.5f;
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private Context mBaseContext;
    protected boolean mEnabled;
    private int mEvictionCount;
    private Handler mHandler;
    private ContentObserver mObserver;
    private LruCache<String, LookupResult> mPeopleCache;
    private NotificationUsageStats mUsageStats;
    private Map<Integer, Context> mUserToContextMap;

    private static class LookupResult {
        private static final long CONTACT_REFRESH_MILLIS = 3600000;
        private float mAffinity = ValidateNotificationPeople.NONE;
        private final long mExpireMillis = (System.currentTimeMillis() + 3600000);

        public void mergeContact(Cursor cursor) {
            this.mAffinity = Math.max(this.mAffinity, 0.5f);
            int idIdx = cursor.getColumnIndex("_id");
            if (idIdx >= 0) {
                int id = cursor.getInt(idIdx);
                if (ValidateNotificationPeople.DEBUG) {
                    String str = ValidateNotificationPeople.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("contact _ID is: ");
                    stringBuilder.append(id);
                    Slog.d(str, stringBuilder.toString());
                }
            } else {
                Slog.i(ValidateNotificationPeople.TAG, "invalid cursor: no _ID");
            }
            int starIdx = cursor.getColumnIndex("starred");
            if (starIdx >= 0) {
                boolean isStarred = cursor.getInt(starIdx) != 0;
                if (isStarred) {
                    this.mAffinity = Math.max(this.mAffinity, 1.0f);
                }
                if (ValidateNotificationPeople.DEBUG) {
                    String str2 = ValidateNotificationPeople.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("contact STARRED is: ");
                    stringBuilder2.append(isStarred);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no STARRED");
            }
        }

        private boolean isExpired() {
            return this.mExpireMillis < System.currentTimeMillis();
        }

        private boolean isInvalid() {
            return this.mAffinity == ValidateNotificationPeople.NONE || isExpired();
        }

        public float getAffinity() {
            if (isInvalid()) {
                return ValidateNotificationPeople.NONE;
            }
            return this.mAffinity;
        }
    }

    private class PeopleRankingReconsideration extends RankingReconsideration {
        private static final long LOOKUP_TIME = 1000;
        private float mContactAffinity;
        private final Context mContext;
        private final LinkedList<String> mPendingLookups;
        private NotificationRecord mRecord;

        /* synthetic */ PeopleRankingReconsideration(ValidateNotificationPeople x0, Context x1, String x2, LinkedList x3, AnonymousClass1 x4) {
            this(x1, x2, x3);
        }

        private PeopleRankingReconsideration(Context context, String key, LinkedList<String> pendingLookups) {
            super(key, 1000);
            this.mContactAffinity = ValidateNotificationPeople.NONE;
            this.mContext = context;
            this.mPendingLookups = pendingLookups;
        }

        public void work() {
            if (ValidateNotificationPeople.VERBOSE) {
                String str = ValidateNotificationPeople.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Executing: validation for: ");
                stringBuilder.append(this.mKey);
                Slog.i(str, stringBuilder.toString());
            }
            long timeStartMs = System.currentTimeMillis();
            Iterator it = this.mPendingLookups.iterator();
            while (it.hasNext()) {
                String str2;
                StringBuilder stringBuilder2;
                LookupResult lookupResult;
                String handle = (String) it.next();
                Uri uri = Uri.parse(handle);
                if ("tel".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        str2 = ValidateNotificationPeople.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checking telephone URI: ");
                        stringBuilder2.append(handle);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    lookupResult = ValidateNotificationPeople.this.resolvePhoneContact(this.mContext, uri.getSchemeSpecificPart());
                } else if ("mailto".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        str2 = ValidateNotificationPeople.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checking mailto URI: ");
                        stringBuilder2.append(handle);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    lookupResult = ValidateNotificationPeople.this.resolveEmailContact(this.mContext, uri.getSchemeSpecificPart());
                } else if (handle.startsWith(Contacts.CONTENT_LOOKUP_URI.toString())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        str2 = ValidateNotificationPeople.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checking lookup URI: ");
                        stringBuilder2.append(handle);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    lookupResult = ValidateNotificationPeople.this.searchContacts(this.mContext, uri);
                } else {
                    lookupResult = new LookupResult();
                    if (!Settings.ATTR_NAME.equals(uri.getScheme())) {
                        str2 = ValidateNotificationPeople.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("unsupported URI ");
                        stringBuilder2.append(handle);
                        Slog.w(str2, stringBuilder2.toString());
                    }
                }
                if (lookupResult != null) {
                    synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                        ValidateNotificationPeople.this.mPeopleCache.put(ValidateNotificationPeople.this.getCacheKey(this.mContext.getUserId(), handle), lookupResult);
                    }
                    if (ValidateNotificationPeople.DEBUG) {
                        str2 = ValidateNotificationPeople.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("lookup contactAffinity is ");
                        stringBuilder2.append(lookupResult.getAffinity());
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    this.mContactAffinity = Math.max(this.mContactAffinity, lookupResult.getAffinity());
                } else if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "lookupResult is null");
                }
            }
            if (ValidateNotificationPeople.DEBUG) {
                String str3 = ValidateNotificationPeople.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Validation finished in ");
                stringBuilder3.append(System.currentTimeMillis() - timeStartMs);
                stringBuilder3.append("ms");
                Slog.d(str3, stringBuilder3.toString());
            }
            if (this.mRecord != null) {
                NotificationUsageStats access$1000 = ValidateNotificationPeople.this.mUsageStats;
                NotificationRecord notificationRecord = this.mRecord;
                boolean z = true;
                boolean z2 = this.mContactAffinity > ValidateNotificationPeople.NONE;
                if (this.mContactAffinity != 1.0f) {
                    z = false;
                }
                access$1000.registerPeopleAffinity(notificationRecord, z2, z, false);
            }
        }

        public void applyChangesLocked(NotificationRecord operand) {
            operand.setContactAffinity(Math.max(this.mContactAffinity, operand.getContactAffinity()));
            if (ValidateNotificationPeople.VERBOSE) {
                String str = ValidateNotificationPeople.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("final affinity: ");
                stringBuilder.append(operand.getContactAffinity());
                Slog.i(str, stringBuilder.toString());
            }
        }

        public float getContactAffinity() {
            return this.mContactAffinity;
        }

        public void setRecord(NotificationRecord record) {
            this.mRecord = record;
        }
    }

    public void initialize(Context context, NotificationUsageStats usageStats) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Initializing  ");
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append(".");
            Slog.d(str, stringBuilder.toString());
        }
        this.mUserToContextMap = new ArrayMap();
        this.mBaseContext = context;
        this.mUsageStats = usageStats;
        this.mPeopleCache = new LruCache(200);
        this.mEnabled = 1 == Global.getInt(this.mBaseContext.getContentResolver(), SETTING_ENABLE_PEOPLE_VALIDATOR, 1);
        if (this.mEnabled) {
            this.mHandler = new Handler();
            this.mObserver = new ContentObserver(this.mHandler) {
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    super.onChange(selfChange, uri, userId);
                    if ((ValidateNotificationPeople.DEBUG || ValidateNotificationPeople.this.mEvictionCount % 100 == 0) && ValidateNotificationPeople.VERBOSE) {
                        String str = ValidateNotificationPeople.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mEvictionCount: ");
                        stringBuilder.append(ValidateNotificationPeople.this.mEvictionCount);
                        Slog.i(str, stringBuilder.toString());
                    }
                    ValidateNotificationPeople.this.mPeopleCache.evictAll();
                    ValidateNotificationPeople.this.mEvictionCount = ValidateNotificationPeople.this.mEvictionCount + 1;
                }
            };
            this.mBaseContext.getContentResolver().registerContentObserver(Contacts.CONTENT_URI, true, this.mObserver, -1);
        }
    }

    public RankingReconsideration process(NotificationRecord record) {
        if (!this.mEnabled) {
            if (VERBOSE) {
                Slog.i(TAG, "disabled");
            }
            return null;
        } else if (record == null || record.getNotification() == null) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping empty notification");
            }
            return null;
        } else if (record.getUserId() == -1) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping global notification");
            }
            return null;
        } else {
            Context context = getContextAsUser(record.getUser());
            if (context != null) {
                return validatePeople(context, record);
            }
            if (VERBOSE) {
                Slog.i(TAG, "skipping notification that lacks a context");
            }
            return null;
        }
    }

    public void setConfig(RankingConfig config) {
    }

    public void setZenHelper(ZenModeHelper helper) {
    }

    public float getContactAffinity(UserHandle userHandle, Bundle extras, int timeoutMs, float timeoutAffinity) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checking affinity for ");
            stringBuilder.append(userHandle);
            Slog.d(str, stringBuilder.toString());
        }
        if (extras == null) {
            return NONE;
        }
        String key = Long.toString(System.nanoTime());
        float[] affinityOut = new float[1];
        Context context = getContextAsUser(userHandle);
        if (context == null) {
            return NONE;
        }
        final PeopleRankingReconsideration prr = validatePeople(context, key, extras, null, affinityOut);
        float affinity = affinityOut[0];
        if (prr != null) {
            final Semaphore s = new Semaphore(0);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    prr.work();
                    s.release();
                }
            });
            try {
                if (s.tryAcquire((long) timeoutMs, TimeUnit.MILLISECONDS)) {
                    affinity = Math.max(prr.getContactAffinity(), affinity);
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Timeout while waiting for affinity: ");
                    stringBuilder2.append(key);
                    stringBuilder2.append(". Returning timeoutAffinity=");
                    stringBuilder2.append(timeoutAffinity);
                    Slog.w(str2, stringBuilder2.toString());
                    return timeoutAffinity;
                }
            } catch (InterruptedException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("InterruptedException while waiting for affinity: ");
                stringBuilder3.append(key);
                stringBuilder3.append(". Returning affinity=");
                stringBuilder3.append(affinity);
                Slog.w(str3, stringBuilder3.toString(), e);
                return affinity;
            }
        }
        return affinity;
    }

    private Context getContextAsUser(UserHandle userHandle) {
        Context context = (Context) this.mUserToContextMap.get(Integer.valueOf(userHandle.getIdentifier()));
        if (context != null) {
            return context;
        }
        try {
            context = this.mBaseContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
            this.mUserToContextMap.put(Integer.valueOf(userHandle.getIdentifier()), context);
            return context;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "failed to create package context for lookups", e);
            return context;
        }
    }

    private RankingReconsideration validatePeople(Context context, NotificationRecord record) {
        float[] affinityOut = new float[1];
        PeopleRankingReconsideration rr = validatePeople(context, record.getKey(), record.getNotification().extras, record.getPeopleOverride(), affinityOut);
        boolean z = false;
        float affinity = affinityOut[0];
        record.setContactAffinity(affinity);
        if (rr == null) {
            NotificationUsageStats notificationUsageStats = this.mUsageStats;
            boolean z2 = affinity > NONE;
            if (affinity == 1.0f) {
                z = true;
            }
            notificationUsageStats.registerPeopleAffinity(record, z2, z, true);
        } else {
            rr.setRecord(record);
        }
        return rr;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0093  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private PeopleRankingReconsideration validatePeople(Context context, String key, Bundle extras, List<String> peopleOverride, float[] affinityOut) {
        String str = key;
        if (extras == null) {
            return null;
        }
        ArraySet<String> people = new ArraySet(peopleOverride);
        String[] notificationPeople = getExtraPeople(extras);
        if (notificationPeople != null) {
            people.addAll(Arrays.asList(notificationPeople));
        }
        if (VERBOSE) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Validating: ");
            stringBuilder.append(str);
            stringBuilder.append(" for ");
            stringBuilder.append(context.getUserId());
            Slog.i(str2, stringBuilder.toString());
        }
        LinkedList<String> pendingLookups = new LinkedList();
        int personIdx = 0;
        float affinity = NONE;
        for (String handle : people) {
            if (!TextUtils.isEmpty(handle)) {
                synchronized (this.mPeopleCache) {
                    LookupResult personIdx2 = (LookupResult) this.mPeopleCache.get(getCacheKey(context.getUserId(), handle));
                    if (personIdx2 != null) {
                        if (!personIdx2.isExpired()) {
                            if (DEBUG) {
                                Slog.d(TAG, "using cached lookupResult");
                            }
                            if (personIdx2 != null) {
                                affinity = Math.max(affinity, personIdx2.getAffinity());
                            }
                        }
                    }
                    pendingLookups.add(handle);
                    if (personIdx2 != null) {
                    }
                }
                personIdx++;
                if (personIdx == 10) {
                    break;
                }
            }
        }
        float affinity2 = affinity;
        affinityOut[0] = affinity2;
        String str3;
        StringBuilder stringBuilder2;
        if (pendingLookups.isEmpty()) {
            if (VERBOSE) {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("final affinity: ");
                stringBuilder2.append(affinity2);
                Slog.i(str3, stringBuilder2.toString());
            }
            return null;
        }
        if (DEBUG) {
            str3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Pending: future work scheduled for: ");
            stringBuilder2.append(str);
            Slog.d(str3, stringBuilder2.toString());
        }
        return new PeopleRankingReconsideration(this, context, str, pendingLookups, null);
    }

    private String getCacheKey(int userId, String handle) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Integer.toString(userId));
        stringBuilder.append(":");
        stringBuilder.append(handle);
        return stringBuilder.toString();
    }

    public static String[] getExtraPeople(Bundle extras) {
        return combineLists(getExtraPeopleForKey(extras, "android.people"), getExtraPeopleForKey(extras, "android.people.list"));
    }

    private static String[] combineLists(String[] first, String[] second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        ArraySet<String> people = new ArraySet(first.length + second.length);
        int i = 0;
        for (String person : first) {
            people.add(person);
        }
        int length = second.length;
        while (i < length) {
            people.add(second[i]);
            i++;
        }
        return (String[]) people.toArray();
    }

    private static String[] getExtraPeopleForKey(Bundle extras, String key) {
        ArrayList people = extras.get(key);
        if (people instanceof String[]) {
            return (String[]) people;
        }
        int i = 0;
        if (people instanceof ArrayList) {
            ArrayList<String> arrayList = people;
            if (arrayList.isEmpty()) {
                return null;
            }
            int N;
            String[] array;
            if (arrayList.get(0) instanceof String) {
                ArrayList<String> stringArray = arrayList;
                return (String[]) stringArray.toArray(new String[stringArray.size()]);
            } else if (arrayList.get(0) instanceof CharSequence) {
                ArrayList<CharSequence> charSeqList = arrayList;
                N = charSeqList.size();
                array = new String[N];
                while (i < N) {
                    array[i] = ((CharSequence) charSeqList.get(i)).toString();
                    i++;
                }
                return array;
            } else if (!(arrayList.get(0) instanceof Person)) {
                return null;
            } else {
                ArrayList<Person> list = arrayList;
                N = list.size();
                array = new String[N];
                while (i < N) {
                    array[i] = ((Person) list.get(i)).resolveToLegacyUri();
                    i++;
                }
                return array;
            }
        } else if (people instanceof String) {
            return new String[]{(String) people};
        } else if (people instanceof char[]) {
            return new String[]{new String((char[]) people)};
        } else if (people instanceof CharSequence) {
            return new String[]{((CharSequence) people).toString()};
        } else if (!(people instanceof CharSequence[])) {
            return null;
        } else {
            CharSequence[] charSeqArray = (CharSequence[]) people;
            int N2 = charSeqArray.length;
            String[] array2 = new String[N2];
            while (i < N2) {
                array2[i] = charSeqArray[i].toString();
                i++;
            }
            return array2;
        }
    }

    private LookupResult resolvePhoneContact(Context context, String number) {
        return searchContacts(context, Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)));
    }

    private LookupResult resolveEmailContact(Context context, String email) {
        return searchContacts(context, Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(email)));
    }

    /* JADX WARNING: Missing block: B:13:0x002f, code skipped:
            if (r1 != null) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:14:0x0031, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:19:0x003f, code skipped:
            if (r1 == null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:20:0x0042, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private LookupResult searchContacts(Context context, Uri lookupUri) {
        LookupResult lookupResult = new LookupResult();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION, null, null, null);
            if (c == null) {
                Slog.w(TAG, "Null cursor from contacts query.");
                if (c != null) {
                    c.close();
                }
                return lookupResult;
            }
            while (c.moveToNext()) {
                lookupResult.mergeContact(c);
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }
}
