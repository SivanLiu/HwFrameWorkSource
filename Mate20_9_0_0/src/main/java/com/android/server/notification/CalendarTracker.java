package com.android.server.notification;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.service.notification.ZenModeConfig.EventInfo;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

public class CalendarTracker {
    private static final String[] ATTENDEE_PROJECTION = new String[]{"event_id", "attendeeEmail", "attendeeStatus"};
    private static final String ATTENDEE_SELECTION = "event_id = ? AND attendeeEmail = ?";
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    private static final boolean DEBUG_ATTENDEES = false;
    private static final int EVENT_CHECK_LOOKAHEAD = 86400000;
    private static final String INSTANCE_ORDER_BY = "begin ASC";
    private static final String[] INSTANCE_PROJECTION = new String[]{"begin", "end", "title", "visible", "event_id", "calendar_displayName", "ownerAccount", "calendar_id", "availability"};
    private static final String TAG = "ConditionProviders.CT";
    private Callback mCallback;
    private final ContentObserver mObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange, Uri u) {
            if (CalendarTracker.DEBUG) {
                String str = CalendarTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onChange selfChange=");
                stringBuilder.append(selfChange);
                stringBuilder.append(" uri=");
                stringBuilder.append(u);
                stringBuilder.append(" u=");
                stringBuilder.append(CalendarTracker.this.mUserContext.getUserId());
                Log.d(str, stringBuilder.toString());
            }
            if (CalendarTracker.this.mCallback != null) {
                CalendarTracker.this.mCallback.onChanged();
            }
        }

        public void onChange(boolean selfChange) {
            if (CalendarTracker.DEBUG) {
                String str = CalendarTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onChange selfChange=");
                stringBuilder.append(selfChange);
                Log.d(str, stringBuilder.toString());
            }
        }
    };
    private boolean mRegistered;
    private final Context mSystemContext;
    private final Context mUserContext;

    public interface Callback {
        void onChanged();
    }

    public static class CheckEventResult {
        public boolean inEvent;
        public long recheckAt;
    }

    public CalendarTracker(Context systemContext, Context userContext) {
        this.mSystemContext = systemContext;
        this.mUserContext = userContext;
    }

    public void setCallback(Callback callback) {
        if (this.mCallback != callback) {
            this.mCallback = callback;
            setRegistered(this.mCallback != null);
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mCallback=");
        pw.println(this.mCallback);
        pw.print(prefix);
        pw.print("mRegistered=");
        pw.println(this.mRegistered);
        pw.print(prefix);
        pw.print("u=");
        pw.println(this.mUserContext.getUserId());
    }

    private ArraySet<Long> getPrimaryCalendars() {
        long start = System.currentTimeMillis();
        ArraySet<Long> rt = new ArraySet();
        String primary = "\"primary\"";
        String selection = "\"primary\" = 1";
        Cursor cursor = null;
        try {
            cursor = this.mUserContext.getContentResolver().query(Calendars.CONTENT_URI, new String[]{"_id", "(account_name=ownerAccount) AS \"primary\""}, "\"primary\" = 1", null, null);
            while (cursor != null && cursor.moveToNext()) {
                rt.add(Long.valueOf(cursor.getLong(0)));
            }
            if (cursor != null) {
                cursor.close();
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPrimaryCalendars took ");
                stringBuilder.append(System.currentTimeMillis() - start);
                Log.d(str, stringBuilder.toString());
            }
            return rt;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:132:0x0243  */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x024a  */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x0243  */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x024a  */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x024a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public CheckEventResult checkEvent(EventInfo filter, long time) {
        Cursor cursor;
        CheckEventResult result;
        Exception e;
        CheckEventResult result2;
        Cursor cursor2;
        Throwable th;
        EventInfo eventInfo = filter;
        long j = time;
        Builder uriBuilder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, j);
        ContentUris.appendId(uriBuilder, j + 86400000);
        Uri uri = uriBuilder.build();
        Cursor cursor3 = this.mUserContext.getContentResolver().query(uri, INSTANCE_PROJECTION, null, null, INSTANCE_ORDER_BY);
        CheckEventResult result3 = new CheckEventResult();
        result3.recheckAt = 86400000 + j;
        Uri uri2;
        Builder uriBuilder2;
        try {
            ArraySet<Long> primaryCalendars = getPrimaryCalendars();
            while (cursor3 != null) {
                try {
                    if (!cursor3.moveToNext()) {
                        break;
                    }
                    long begin = cursor3.getLong(0);
                    long end = cursor3.getLong(1);
                    String title = cursor3.getString(2);
                    boolean calendarVisible = cursor3.getInt(3) == 1;
                    int eventId = cursor3.getInt(4);
                    String name = cursor3.getString(5);
                    String owner = cursor3.getString(6);
                    int availability = cursor3.getInt(8);
                    uri2 = uri;
                    long calendarId = cursor3.getLong(7);
                    try {
                        ArraySet<Long> primaryCalendars2;
                        boolean z;
                        int eventId2;
                        String name2;
                        int availability2;
                        String owner2;
                        boolean calendarPrimary = primaryCalendars.contains(Long.valueOf(calendarId));
                        if (DEBUG) {
                            primaryCalendars2 = primaryCalendars;
                            try {
                                String str;
                                Object[] objArr;
                                String str2 = TAG;
                                uriBuilder2 = uriBuilder;
                                try {
                                    str = "%s %s-%s v=%s a=%s eid=%s n=%s o=%s cid=%s p=%s";
                                    cursor = cursor3;
                                    try {
                                        objArr = new Object[10];
                                        z = false;
                                        objArr[0] = title;
                                        result = result3;
                                    } catch (Exception e2) {
                                        e = e2;
                                        result2 = result3;
                                        cursor2 = cursor;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        result2 = result3;
                                        cursor2 = cursor;
                                    }
                                } catch (Exception e3) {
                                    e = e3;
                                    cursor2 = cursor3;
                                    result2 = result3;
                                } catch (Throwable th3) {
                                    th = th3;
                                    cursor2 = cursor3;
                                    result2 = result3;
                                }
                                try {
                                    objArr[1] = new Date(begin);
                                    objArr[2] = new Date(end);
                                    objArr[3] = Boolean.valueOf(calendarVisible);
                                    int availability3 = availability;
                                    objArr[4] = availabilityToString(availability3);
                                    int eventId3 = eventId;
                                    objArr[5] = Integer.valueOf(eventId3);
                                    eventId2 = eventId3;
                                    name2 = name;
                                    objArr[6] = name2;
                                    availability2 = availability3;
                                    owner2 = owner;
                                    objArr[7] = owner2;
                                    objArr[8] = Long.valueOf(calendarId);
                                    objArr[9] = Boolean.valueOf(calendarPrimary);
                                    Log.d(str2, String.format(str, objArr));
                                } catch (Exception e4) {
                                    e = e4;
                                    cursor2 = cursor;
                                    result2 = result;
                                } catch (Throwable th4) {
                                    th = th4;
                                    cursor2 = cursor;
                                    result2 = result;
                                }
                            } catch (Exception e5) {
                                e = e5;
                                uriBuilder2 = uriBuilder;
                                cursor2 = cursor3;
                                result2 = result3;
                            } catch (Throwable th5) {
                                th = th5;
                                uriBuilder2 = uriBuilder;
                                cursor2 = cursor3;
                                result2 = result3;
                            }
                        } else {
                            primaryCalendars2 = primaryCalendars;
                            uriBuilder2 = uriBuilder;
                            cursor = cursor3;
                            result = result3;
                            eventId2 = eventId;
                            name2 = name;
                            owner2 = owner;
                            availability2 = availability;
                            z = false;
                        }
                        boolean meetsTime = (j < begin || j >= end) ? z : true;
                        boolean meetsCalendar = (calendarVisible && calendarPrimary && (eventInfo.calendar == null || Objects.equals(eventInfo.calendar, owner2) || Objects.equals(eventInfo.calendar, name2))) ? true : z;
                        if (availability2 != 1) {
                            z = true;
                        }
                        boolean meetsAvailability = z;
                        if (meetsCalendar && meetsAvailability) {
                            try {
                                if (DEBUG) {
                                    Log.d(TAG, "  MEETS CALENDAR & AVAILABILITY");
                                } else {
                                    boolean z2 = meetsCalendar;
                                }
                                int eventId4 = eventId2;
                                if (meetsAttendee(eventInfo, eventId4, owner2)) {
                                    if (DEBUG) {
                                        Log.d(TAG, "    MEETS ATTENDEE");
                                    }
                                    if (meetsTime) {
                                        if (DEBUG) {
                                            Log.d(TAG, "      MEETS TIME");
                                        }
                                        result2 = result;
                                        try {
                                            result2.inEvent = true;
                                        } catch (Exception e6) {
                                            e = e6;
                                            cursor2 = cursor;
                                            try {
                                                Slog.w(TAG, "error reading calendar", e);
                                                if (cursor2 != null) {
                                                }
                                                return result2;
                                            } catch (Throwable th6) {
                                                th = th6;
                                                if (cursor2 != null) {
                                                }
                                                throw th;
                                            }
                                        } catch (Throwable th7) {
                                            th = th7;
                                            cursor2 = cursor;
                                            if (cursor2 != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    result2 = result;
                                    if (begin > j) {
                                        if (begin < result2.recheckAt) {
                                            result2.recheckAt = begin;
                                            result3 = result2;
                                            uri = uri2;
                                            primaryCalendars = primaryCalendars2;
                                            uriBuilder = uriBuilder2;
                                            cursor3 = cursor;
                                            eventInfo = filter;
                                        }
                                    } else {
                                        int i = eventId4;
                                        boolean z3 = calendarVisible;
                                    }
                                    if (end > j && end < result2.recheckAt) {
                                        result2.recheckAt = end;
                                    }
                                    result3 = result2;
                                    uri = uri2;
                                    primaryCalendars = primaryCalendars2;
                                    uriBuilder = uriBuilder2;
                                    cursor3 = cursor;
                                    eventInfo = filter;
                                }
                            } catch (Exception e7) {
                                e = e7;
                                result2 = result;
                                cursor2 = cursor;
                            } catch (Throwable th8) {
                                th = th8;
                                result2 = result;
                                cursor2 = cursor;
                            }
                        }
                        result2 = result;
                        result3 = result2;
                        uri = uri2;
                        primaryCalendars = primaryCalendars2;
                        uriBuilder = uriBuilder2;
                        cursor3 = cursor;
                        eventInfo = filter;
                    } catch (Exception e8) {
                        e = e8;
                        uriBuilder2 = uriBuilder;
                        result2 = result3;
                        cursor2 = cursor3;
                    } catch (Throwable th9) {
                        th = th9;
                        uriBuilder2 = uriBuilder;
                        result2 = result3;
                        cursor2 = cursor3;
                    }
                } catch (Exception e9) {
                    e = e9;
                    uriBuilder2 = uriBuilder;
                    uri2 = uri;
                    result2 = result3;
                    cursor2 = cursor3;
                    Slog.w(TAG, "error reading calendar", e);
                    if (cursor2 != null) {
                    }
                    return result2;
                } catch (Throwable th10) {
                    th = th10;
                    uriBuilder2 = uriBuilder;
                    uri2 = uri;
                    result2 = result3;
                    cursor2 = cursor3;
                    if (cursor2 != null) {
                    }
                    throw th;
                }
            }
            uri2 = uri;
            cursor = cursor3;
            result2 = result3;
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e10) {
            e = e10;
            uriBuilder2 = uriBuilder;
            uri2 = uri;
            cursor2 = cursor3;
            result2 = result3;
            Slog.w(TAG, "error reading calendar", e);
            if (cursor2 != null) {
                cursor2.close();
            }
            return result2;
        } catch (Throwable th11) {
            th = th11;
            uriBuilder2 = uriBuilder;
            uri2 = uri;
            cursor2 = cursor3;
            result2 = result3;
            if (cursor2 != null) {
                cursor2.close();
            }
            throw th;
        }
        return result2;
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x013b  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0142  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean meetsAttendee(EventInfo filter, int eventId, String email) {
        Throwable th;
        int i;
        String str;
        StringBuilder stringBuilder;
        String str2 = email;
        long start = System.currentTimeMillis();
        String selection = ATTENDEE_SELECTION;
        int i2 = 2;
        selectionArgs = new String[2];
        boolean z = false;
        selectionArgs[0] = Integer.toString(eventId);
        int i3 = 1;
        selectionArgs[1] = str2;
        Cursor cursor = this.mUserContext.getContentResolver().query(Attendees.CONTENT_URI, ATTENDEE_PROJECTION, selection, selectionArgs, null);
        EventInfo eventInfo;
        String[] strArr;
        String str3;
        if (cursor != null) {
            try {
                if (cursor.getCount() == 0) {
                    eventInfo = filter;
                    strArr = selectionArgs;
                    str3 = selection;
                    selectionArgs = eventId;
                } else {
                    boolean rt = false;
                    while (cursor != null && cursor.moveToNext()) {
                        long rowEventId = cursor.getLong(z);
                        String rowEmail = cursor.getString(i3);
                        int status = cursor.getInt(i2);
                        try {
                            boolean z2;
                            boolean eventMeets;
                            boolean meetsReply = meetsReply(filter.reply, status);
                            if (DEBUG) {
                                String str4 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                strArr = selectionArgs;
                                try {
                                    stringBuilder2.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                    String str5 = "status=%s, meetsReply=%s";
                                    str3 = selection;
                                    try {
                                        r13 = new Object[2];
                                        z2 = false;
                                        r13[0] = attendeeStatusToString(status);
                                        r13[1] = Boolean.valueOf(meetsReply);
                                        stringBuilder2.append(String.format(str5, r13));
                                        Log.d(str4, stringBuilder2.toString());
                                    } catch (Throwable th2) {
                                        th = th2;
                                        i = eventId;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    str3 = selection;
                                    i = eventId;
                                }
                            } else {
                                strArr = selectionArgs;
                                str3 = selection;
                                z2 = z;
                                selection = i2;
                            }
                            if (rowEventId == ((long) eventId)) {
                                try {
                                    if (Objects.equals(rowEmail, str2) && meetsReply) {
                                        eventMeets = true;
                                        rt |= eventMeets;
                                        z = z2;
                                        selectionArgs = strArr;
                                        selection = str3;
                                        i2 = 2;
                                        i3 = 1;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            }
                            eventMeets = z2;
                            rt |= eventMeets;
                            z = z2;
                            selectionArgs = strArr;
                            selection = str3;
                            i2 = 2;
                            i3 = 1;
                        } catch (Throwable th5) {
                            th = th5;
                            strArr = selectionArgs;
                            str3 = selection;
                            selectionArgs = eventId;
                            if (cursor != null) {
                            }
                            if (DEBUG) {
                            }
                            throw th;
                        }
                    }
                    eventInfo = filter;
                    strArr = selectionArgs;
                    str3 = selection;
                    selectionArgs = eventId;
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (DEBUG) {
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("meetsAttendee took ");
                        stringBuilder3.append(System.currentTimeMillis() - start);
                        Log.d(str, stringBuilder3.toString());
                    }
                    return rt;
                }
            } catch (Throwable th6) {
                th = th6;
                eventInfo = filter;
                strArr = selectionArgs;
                str3 = selection;
                selectionArgs = eventId;
                if (cursor != null) {
                    cursor.close();
                }
                if (DEBUG) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("meetsAttendee took ");
                    stringBuilder.append(System.currentTimeMillis() - start);
                    Log.d(TAG, stringBuilder.toString());
                }
                throw th;
            }
        }
        eventInfo = filter;
        strArr = selectionArgs;
        str3 = selection;
        selectionArgs = eventId;
        if (DEBUG) {
            Log.d(TAG, "No attendees found");
        }
        if (cursor != null) {
            cursor.close();
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("meetsAttendee took ");
            stringBuilder.append(System.currentTimeMillis() - start);
            Log.d(str, stringBuilder.toString());
        }
        return true;
    }

    private void setRegistered(boolean registered) {
        if (this.mRegistered != registered) {
            ContentResolver cr = this.mSystemContext.getContentResolver();
            if (this.mRegistered) {
                if (DEBUG) {
                    Log.d(TAG, "unregister content observer u=0");
                }
                cr.unregisterContentObserver(this.mObserver);
            }
            this.mRegistered = registered;
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mRegistered = ");
                stringBuilder.append(registered);
                stringBuilder.append(" u=");
                stringBuilder.append(0);
                Log.d(str, stringBuilder.toString());
            }
            if (this.mRegistered) {
                if (DEBUG) {
                    Log.d(TAG, "register content observer u=0");
                }
                cr.registerContentObserver(Instances.CONTENT_URI, true, this.mObserver, 0);
                cr.registerContentObserver(Events.CONTENT_URI, true, this.mObserver, 0);
                cr.registerContentObserver(Calendars.CONTENT_URI, true, this.mObserver, 0);
                cr.registerContentObserver(Attendees.CONTENT_URI, true, this.mObserver, 0);
            }
        }
    }

    private static String attendeeStatusToString(int status) {
        switch (status) {
            case 0:
                return "ATTENDEE_STATUS_NONE";
            case 1:
                return "ATTENDEE_STATUS_ACCEPTED";
            case 2:
                return "ATTENDEE_STATUS_DECLINED";
            case 3:
                return "ATTENDEE_STATUS_INVITED";
            case 4:
                return "ATTENDEE_STATUS_TENTATIVE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ATTENDEE_STATUS_UNKNOWN_");
                stringBuilder.append(status);
                return stringBuilder.toString();
        }
    }

    private static String availabilityToString(int availability) {
        switch (availability) {
            case 0:
                return "AVAILABILITY_BUSY";
            case 1:
                return "AVAILABILITY_FREE";
            case 2:
                return "AVAILABILITY_TENTATIVE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AVAILABILITY_UNKNOWN_");
                stringBuilder.append(availability);
                return stringBuilder.toString();
        }
    }

    private static boolean meetsReply(int reply, int attendeeStatus) {
        boolean z = false;
        switch (reply) {
            case 0:
                if (attendeeStatus != 2) {
                    z = true;
                }
                return z;
            case 1:
                if (attendeeStatus == 1 || attendeeStatus == 4) {
                    z = true;
                }
                return z;
            case 2:
                if (attendeeStatus == 1) {
                    z = true;
                }
                return z;
            default:
                return false;
        }
    }
}
