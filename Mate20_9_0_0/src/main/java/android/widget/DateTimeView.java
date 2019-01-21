package android.widget;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.icu.util.Calendar;
import android.os.Handler;
import android.telephony.SubscriptionPlan;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import libcore.icu.DateUtilsBridge;

@RemoteView
public class DateTimeView extends TextView {
    private static final int SHOW_MONTH_DAY_YEAR = 1;
    private static final int SHOW_TIME = 0;
    private static final ThreadLocal<ReceiverInfo> sReceiverInfo = new ThreadLocal();
    int mLastDisplay;
    DateFormat mLastFormat;
    private String mNowText;
    private boolean mShowRelativeTime;
    Date mTime;
    long mTimeMillis;
    private long mUpdateTimeMillis;

    private static class ReceiverInfo {
        private final ArrayList<DateTimeView> mAttachedViews;
        private Handler mHandler;
        private final ContentObserver mObserver;
        private final BroadcastReceiver mReceiver;

        private ReceiverInfo() {
            this.mAttachedViews = new ArrayList();
            this.mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (!"android.intent.action.TIME_TICK".equals(intent.getAction()) || System.currentTimeMillis() >= ReceiverInfo.this.getSoonestUpdateTime()) {
                        ReceiverInfo.this.updateAll();
                    }
                }
            };
            this.mObserver = new ContentObserver(new Handler()) {
                public void onChange(boolean selfChange) {
                    ReceiverInfo.this.updateAll();
                }
            };
            this.mHandler = new Handler();
        }

        public void addView(DateTimeView v) {
            synchronized (this.mAttachedViews) {
                boolean register = this.mAttachedViews.isEmpty();
                this.mAttachedViews.add(v);
                if (register) {
                    register(getApplicationContextIfAvailable(v.getContext()));
                }
            }
        }

        public void removeView(DateTimeView v) {
            synchronized (this.mAttachedViews) {
                if (this.mAttachedViews.remove(v) && this.mAttachedViews.isEmpty()) {
                    unregister(getApplicationContextIfAvailable(v.getContext()));
                }
            }
        }

        void updateAll() {
            synchronized (this.mAttachedViews) {
                int count = this.mAttachedViews.size();
                for (int i = 0; i < count; i++) {
                    DateTimeView view = (DateTimeView) this.mAttachedViews.get(i);
                    view.post(new -$$Lambda$DateTimeView$ReceiverInfo$AVLnX7U5lTcE9jLnlKKNAT1GUeI(view));
                }
            }
        }

        long getSoonestUpdateTime() {
            long result = SubscriptionPlan.BYTES_UNLIMITED;
            synchronized (this.mAttachedViews) {
                int count = this.mAttachedViews.size();
                for (int i = 0; i < count; i++) {
                    long time = ((DateTimeView) this.mAttachedViews.get(i)).mUpdateTimeMillis;
                    if (time < result) {
                        result = time;
                    }
                }
            }
            return result;
        }

        static final Context getApplicationContextIfAvailable(Context context) {
            Context ac = context.getApplicationContext();
            return ac != null ? ac : ActivityThread.currentApplication().getApplicationContext();
        }

        void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            context.registerReceiver(this.mReceiver, filter, null, this.mHandler);
        }

        void unregister(Context context) {
            context.unregisterReceiver(this.mReceiver);
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
            synchronized (this.mAttachedViews) {
                if (!this.mAttachedViews.isEmpty()) {
                    unregister(((DateTimeView) this.mAttachedViews.get(0)).getContext());
                    register(((DateTimeView) this.mAttachedViews.get(0)).getContext());
                }
            }
        }
    }

    public DateTimeView(Context context) {
        this(context, null);
    }

    public DateTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastDisplay = -1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DateTimeView, 0, 0);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            if (a.getIndex(i) == 0) {
                setShowRelativeTime(a.getBoolean(i, false));
            }
        }
        a.recycle();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ReceiverInfo ri = (ReceiverInfo) sReceiverInfo.get();
        if (ri == null) {
            ri = new ReceiverInfo();
            sReceiverInfo.set(ri);
        }
        ri.addView(this);
        if (this.mShowRelativeTime) {
            update();
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ReceiverInfo ri = (ReceiverInfo) sReceiverInfo.get();
        if (ri != null) {
            ri.removeView(this);
        }
    }

    @RemotableViewMethod
    public void setTime(long time) {
        Time t = new Time();
        t.set(time);
        this.mTimeMillis = t.toMillis(false);
        this.mTime = new Date(t.year - 1900, t.month, t.monthDay, t.hour, t.minute, 0);
        update();
    }

    @RemotableViewMethod
    public void setShowRelativeTime(boolean showRelativeTime) {
        this.mShowRelativeTime = showRelativeTime;
        updateNowText();
        update();
    }

    @RemotableViewMethod
    public void setVisibility(int visibility) {
        boolean gotVisible = visibility != 8 && getVisibility() == 8;
        super.setVisibility(visibility);
        if (gotVisible) {
            update();
        }
    }

    void update() {
        if (this.mTime != null && getVisibility() != 8) {
            long now = System.currentTimeMillis();
            long duration = Math.abs(now - this.mTimeMillis);
            boolean withinHour = duration < DateUtils.HOUR_IN_MILLIS;
            if (this.mShowRelativeTime && withinHour) {
                updateRelativeTime();
                return;
            }
            int display;
            DateFormat format;
            Date time = this.mTime;
            Time t = new Time();
            t.set(this.mTimeMillis);
            t.second = 0;
            t.hour -= 12;
            long twelveHoursBefore = t.toMillis(false);
            t.hour += 12;
            long twelveHoursAfter = t.toMillis(false);
            t.hour = 0;
            t.minute = 0;
            long midnightBefore = t.toMillis(false);
            t.monthDay++;
            long midnightAfter = t.toMillis(false);
            t.set(System.currentTimeMillis());
            t.second = 0;
            long nowMillis = t.normalize(false);
            if ((nowMillis < midnightBefore || nowMillis >= midnightAfter) && (nowMillis < twelveHoursBefore || nowMillis >= twelveHoursAfter)) {
                display = 1;
            } else {
                display = 0;
            }
            if (display != this.mLastDisplay || this.mLastFormat == null) {
                switch (display) {
                    case 0:
                        format = getTimeFormat();
                        break;
                    case 1:
                        format = DateFormat.getDateInstance(3);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown display value: ");
                        stringBuilder.append(display);
                        throw new RuntimeException(stringBuilder.toString());
                }
                this.mLastFormat = format;
            } else {
                format = this.mLastFormat;
            }
            boolean z = withinHour;
            String text = format.format(this.mTime);
            setText((CharSequence) text);
            Time time_now = new Time();
            time_now.set(now);
            long j = now;
            time_now.second = 0;
            time_now.minute = 0;
            time_now.hour = 0;
            long yesterdayLimit = time_now.toMillis(false);
            time_now.monthDay--;
            now = time_now.toMillis(false);
            if (this.mTimeMillis < yesterdayLimit && now < this.mTimeMillis) {
                setText(33685891);
            }
            if (display == 0) {
                this.mUpdateTimeMillis = twelveHoursAfter > midnightAfter ? twelveHoursAfter : midnightAfter;
            } else if (this.mTimeMillis < nowMillis) {
                this.mUpdateTimeMillis = 0;
            } else {
                this.mUpdateTimeMillis = twelveHoursBefore < midnightBefore ? twelveHoursBefore : midnightBefore;
            }
        }
    }

    private void updateRelativeTime() {
        long now = System.currentTimeMillis();
        long duration = Math.abs(now - this.mTimeMillis);
        boolean past = now >= this.mTimeMillis;
        if (duration < DateUtils.MINUTE_IN_MILLIS) {
            setText((CharSequence) this.mNowText);
            this.mUpdateTimeMillis = (this.mTimeMillis + DateUtils.MINUTE_IN_MILLIS) + 1;
            return;
        }
        int count;
        String result;
        int i = (duration > DateUtils.HOUR_IN_MILLIS ? 1 : (duration == DateUtils.HOUR_IN_MILLIS ? 0 : -1));
        long millisIncrease = DateUtils.YEAR_IN_MILLIS;
        Resources resources;
        if (i < 0) {
            count = (int) (duration / 60000);
            resources = getContext().getResources();
            if (past) {
                i = 18153486;
            } else {
                i = 18153487;
            }
            result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
            millisIncrease = DateUtils.MINUTE_IN_MILLIS;
        } else {
            long millisIncrease2 = DateUtils.DAY_IN_MILLIS;
            if (duration < DateUtils.DAY_IN_MILLIS) {
                count = (int) (duration / DateUtils.HOUR_IN_MILLIS);
                resources = getContext().getResources();
                if (past) {
                    i = 18153481;
                } else {
                    i = 18153482;
                }
                result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
                millisIncrease = DateUtils.HOUR_IN_MILLIS;
            } else if (duration < DateUtils.YEAR_IN_MILLIS) {
                int i2;
                TimeZone timeZone = TimeZone.getDefault();
                int count2 = Math.max(Math.abs(dayDistance(timeZone, this.mTimeMillis, now)), 1);
                Resources resources2 = getContext().getResources();
                if (past) {
                    i2 = 18153476;
                } else {
                    i2 = 18153477;
                }
                result = String.format(resources2.getQuantityString(i2, count2), new Object[]{Integer.valueOf(count2)});
                if (past || count2 != 1) {
                    this.mUpdateTimeMillis = computeNextMidnight(timeZone);
                    millisIncrease2 = -1;
                }
                millisIncrease = millisIncrease2;
                count = count2;
            } else {
                count = (int) (duration / 1384828928);
                resources = getContext().getResources();
                if (past) {
                    i = 18153491;
                } else {
                    i = 18153492;
                }
                result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
            }
        }
        long millisIncrease3 = millisIncrease;
        if (millisIncrease3 != -1) {
            if (past) {
                this.mUpdateTimeMillis = (this.mTimeMillis + (((long) (count + 1)) * millisIncrease3)) + 1;
            } else {
                this.mUpdateTimeMillis = (this.mTimeMillis - (((long) count) * millisIncrease3)) + 1;
            }
        }
        setText((CharSequence) result);
    }

    private long computeNextMidnight(TimeZone timeZone) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(DateUtilsBridge.icuTimeZone(timeZone));
        c.add(5, 1);
        c.set(11, 0);
        c.set(12, 0);
        c.set(13, 0);
        c.set(14, 0);
        return c.getTimeInMillis();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateNowText();
        update();
    }

    private void updateNowText() {
        if (this.mShowRelativeTime) {
            this.mNowText = getContext().getResources().getString(17040610);
        }
    }

    private static int dayDistance(TimeZone timeZone, long startTime, long endTime) {
        return Time.getJulianDay(endTime, (long) (timeZone.getOffset(endTime) / 1000)) - Time.getJulianDay(startTime, (long) (timeZone.getOffset(startTime) / 1000));
    }

    private DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(getContext());
    }

    void clearFormatAndUpdate() {
        this.mLastFormat = null;
        update();
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        if (this.mShowRelativeTime) {
            String result;
            long now = System.currentTimeMillis();
            long duration = Math.abs(now - this.mTimeMillis);
            boolean past = now >= this.mTimeMillis;
            int count;
            Resources resources;
            int i;
            if (duration < DateUtils.MINUTE_IN_MILLIS) {
                result = this.mNowText;
            } else if (duration < DateUtils.HOUR_IN_MILLIS) {
                count = (int) (duration / 60000);
                resources = getContext().getResources();
                if (past) {
                    i = 18153484;
                } else {
                    i = 18153485;
                }
                result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
            } else if (duration < DateUtils.DAY_IN_MILLIS) {
                count = (int) (duration / DateUtils.HOUR_IN_MILLIS);
                resources = getContext().getResources();
                if (past) {
                    i = 18153479;
                } else {
                    i = 18153480;
                }
                result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
            } else if (duration < DateUtils.YEAR_IN_MILLIS) {
                int i2;
                int count2 = Math.max(Math.abs(dayDistance(TimeZone.getDefault(), this.mTimeMillis, now)), 1);
                Resources resources2 = getContext().getResources();
                if (past) {
                    i2 = 18153474;
                } else {
                    i2 = 18153475;
                }
                result = String.format(resources2.getQuantityString(i2, count2), new Object[]{Integer.valueOf(count2)});
            } else {
                count = (int) (duration / 1384828928);
                resources = getContext().getResources();
                if (past) {
                    i = 18153489;
                } else {
                    i = 18153490;
                }
                result = String.format(resources.getQuantityString(i, count), new Object[]{Integer.valueOf(count)});
            }
            info.setText(result);
        }
    }

    public static void setReceiverHandler(Handler handler) {
        ReceiverInfo ri = (ReceiverInfo) sReceiverInfo.get();
        if (ri == null) {
            ri = new ReceiverInfo();
            sReceiverInfo.set(ri);
        }
        ri.setHandler(handler);
    }
}
