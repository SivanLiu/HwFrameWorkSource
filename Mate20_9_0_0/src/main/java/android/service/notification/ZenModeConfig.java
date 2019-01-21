package android.service.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.NotificationManager.Policy;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.Contacts;
import android.provider.Settings.Global;
import android.provider.SettingsStringUtil;
import android.provider.Telephony.BaseMmsColumns;
import android.telecom.Logging.Session;
import android.telephony.SubscriptionPlan;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeConfig implements Parcelable {
    private static final String ALLOW_ATT_ALARMS = "alarms";
    private static final String ALLOW_ATT_CALLS = "calls";
    private static final String ALLOW_ATT_CALLS_FROM = "callsFrom";
    private static final String ALLOW_ATT_EVENTS = "events";
    private static final String ALLOW_ATT_FROM = "from";
    private static final String ALLOW_ATT_MEDIA = "media";
    private static final String ALLOW_ATT_MESSAGES = "messages";
    private static final String ALLOW_ATT_MESSAGES_FROM = "messagesFrom";
    private static final String ALLOW_ATT_REMINDERS = "reminders";
    private static final String ALLOW_ATT_REPEAT_CALLERS = "repeatCallers";
    private static final String ALLOW_ATT_SCREEN_OFF = "visualScreenOff";
    private static final String ALLOW_ATT_SCREEN_ON = "visualScreenOn";
    private static final String ALLOW_ATT_SYSTEM = "system";
    private static final String ALLOW_TAG = "allow";
    public static final int[] ALL_DAYS = new int[]{1, 2, 3, 4, 5, 6, 7};
    private static final String AUTOMATIC_TAG = "automatic";
    private static final String CONDITION_ATT_FLAGS = "flags";
    private static final String CONDITION_ATT_ICON = "icon";
    private static final String CONDITION_ATT_ID = "id";
    private static final String CONDITION_ATT_LINE1 = "line1";
    private static final String CONDITION_ATT_LINE2 = "line2";
    private static final String CONDITION_ATT_STATE = "state";
    private static final String CONDITION_ATT_SUMMARY = "summary";
    public static final String COUNTDOWN_PATH = "countdown";
    public static final Creator<ZenModeConfig> CREATOR = new Creator<ZenModeConfig>() {
        public ZenModeConfig createFromParcel(Parcel source) {
            return new ZenModeConfig(source);
        }

        public ZenModeConfig[] newArray(int size) {
            return new ZenModeConfig[size];
        }
    };
    private static final int DAY_MINUTES = 1440;
    private static final boolean DEFAULT_ALLOW_ALARMS = true;
    private static final boolean DEFAULT_ALLOW_CALLS = true;
    private static final boolean DEFAULT_ALLOW_EVENTS = false;
    private static final boolean DEFAULT_ALLOW_MEDIA = true;
    private static final boolean DEFAULT_ALLOW_MESSAGES = false;
    private static final boolean DEFAULT_ALLOW_REMINDERS = false;
    private static final boolean DEFAULT_ALLOW_REPEAT_CALLERS = true;
    private static final boolean DEFAULT_ALLOW_SYSTEM = false;
    private static final int DEFAULT_CALLS_SOURCE = 2;
    private static final boolean DEFAULT_CHANNELS_BYPASSING_DND = false;
    public static final List<String> DEFAULT_RULE_IDS = Arrays.asList(new String[]{EVERY_NIGHT_DEFAULT_RULE_ID, EVENTS_DEFAULT_RULE_ID});
    private static final int DEFAULT_SOURCE = 1;
    private static final int DEFAULT_SUPPRESSED_VISUAL_EFFECTS = 0;
    private static final String DISALLOW_ATT_VISUAL_EFFECTS = "visualEffects";
    private static final String DISALLOW_TAG = "disallow";
    public static final String EVENTS_DEFAULT_RULE_ID = "EVENTS_DEFAULT_RULE";
    public static final String EVENT_PATH = "event";
    public static final String EVERY_NIGHT_DEFAULT_RULE_ID = "EVERY_NIGHT_DEFAULT_RULE";
    public static final String IS_ALARM_PATH = "alarm";
    private static final String MANUAL_TAG = "manual";
    public static final int MAX_SOURCE = 2;
    private static final int MINUTES_MS = 60000;
    public static final int[] MINUTE_BUCKETS = generateMinuteBuckets();
    private static final String RULE_ATT_COMPONENT = "component";
    private static final String RULE_ATT_CONDITION_ID = "conditionId";
    private static final String RULE_ATT_CREATION_TIME = "creationTime";
    private static final String RULE_ATT_ENABLED = "enabled";
    private static final String RULE_ATT_ENABLER = "enabler";
    private static final String RULE_ATT_ID = "ruleId";
    private static final String RULE_ATT_NAME = "name";
    private static final String RULE_ATT_SNOOZING = "snoozing";
    private static final String RULE_ATT_ZEN = "zen";
    public static final String SCHEDULE_PATH = "schedule";
    private static final int SECONDS_MS = 1000;
    public static final int SOURCE_ANYONE = 0;
    public static final int SOURCE_CONTACT = 1;
    public static final int SOURCE_STAR = 2;
    private static final String STATE_ATT_CHANNELS_BYPASSING_DND = "areChannelsBypassingDnd";
    private static final String STATE_TAG = "state";
    public static final String SYSTEM_AUTHORITY = "android";
    private static String TAG = "ZenModeConfig";
    public static final int[] WEEKEND_DAYS = new int[]{1, 7};
    public static final int[] WEEKNIGHT_DAYS = new int[]{1, 2, 3, 4, 5};
    public static final int XML_VERSION = 8;
    public static final int XML_VERSION_P = 8;
    private static final String ZEN_ATT_USER = "user";
    private static final String ZEN_ATT_VERSION = "version";
    public static final String ZEN_TAG = "zen";
    private static final int ZERO_VALUE_MS = 10000;
    public boolean allowAlarms = true;
    public boolean allowCalls = true;
    public int allowCallsFrom = 2;
    public boolean allowEvents = false;
    public boolean allowMedia = true;
    public boolean allowMessages = false;
    public int allowMessagesFrom = 1;
    public boolean allowReminders = false;
    public boolean allowRepeatCallers = true;
    public boolean allowSystem = false;
    public boolean areChannelsBypassingDnd = false;
    public ArrayMap<String, ZenRule> automaticRules = new ArrayMap();
    public ZenRule manualRule;
    public int suppressedVisualEffects = 0;
    public int user = 0;
    public int version;

    public static class Diff {
        private final ArrayList<String> lines = new ArrayList();

        public String toString() {
            StringBuilder sb = new StringBuilder("Diff[");
            int N = this.lines.size();
            for (int i = 0; i < N; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append((String) this.lines.get(i));
            }
            sb.append(']');
            return sb.toString();
        }

        private Diff addLine(String item, String action) {
            ArrayList arrayList = this.lines;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(item);
            stringBuilder.append(SettingsStringUtil.DELIMITER);
            stringBuilder.append(action);
            arrayList.add(stringBuilder.toString());
            return this;
        }

        public Diff addLine(String item, String subitem, Object from, Object to) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(item);
            stringBuilder.append(".");
            stringBuilder.append(subitem);
            return addLine(stringBuilder.toString(), from, to);
        }

        public Diff addLine(String item, Object from, Object to) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(from);
            stringBuilder.append(Session.SUBSESSION_SEPARATION_CHAR);
            stringBuilder.append(to);
            return addLine(item, stringBuilder.toString());
        }
    }

    public static class EventInfo {
        public static final int REPLY_ANY_EXCEPT_NO = 0;
        public static final int REPLY_YES = 2;
        public static final int REPLY_YES_OR_MAYBE = 1;
        public String calendar;
        public int reply;
        public int userId = -10000;

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof EventInfo)) {
                return false;
            }
            EventInfo other = (EventInfo) o;
            if (this.userId == other.userId && Objects.equals(this.calendar, other.calendar) && this.reply == other.reply) {
                z = true;
            }
            return z;
        }

        public EventInfo copy() {
            EventInfo rt = new EventInfo();
            rt.userId = this.userId;
            rt.calendar = this.calendar;
            rt.reply = this.reply;
            return rt;
        }

        public static int resolveUserId(int userId) {
            return userId == -10000 ? ActivityManager.getCurrentUser() : userId;
        }
    }

    public static class ScheduleInfo {
        public int[] days;
        public int endHour;
        public int endMinute;
        public boolean exitAtAlarm;
        public long nextAlarm;
        public int startHour;
        public int startMinute;

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof ScheduleInfo)) {
                return false;
            }
            ScheduleInfo other = (ScheduleInfo) o;
            if (ZenModeConfig.toDayList(this.days).equals(ZenModeConfig.toDayList(other.days)) && this.startHour == other.startHour && this.startMinute == other.startMinute && this.endHour == other.endHour && this.endMinute == other.endMinute && this.exitAtAlarm == other.exitAtAlarm) {
                z = true;
            }
            return z;
        }

        public ScheduleInfo copy() {
            ScheduleInfo rt = new ScheduleInfo();
            if (this.days != null) {
                rt.days = new int[this.days.length];
                System.arraycopy(this.days, 0, rt.days, 0, this.days.length);
            }
            rt.startHour = this.startHour;
            rt.startMinute = this.startMinute;
            rt.endHour = this.endHour;
            rt.endMinute = this.endMinute;
            rt.exitAtAlarm = this.exitAtAlarm;
            rt.nextAlarm = this.nextAlarm;
            return rt;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ScheduleInfo{days=");
            stringBuilder.append(Arrays.toString(this.days));
            stringBuilder.append(", startHour=");
            stringBuilder.append(this.startHour);
            stringBuilder.append(", startMinute=");
            stringBuilder.append(this.startMinute);
            stringBuilder.append(", endHour=");
            stringBuilder.append(this.endHour);
            stringBuilder.append(", endMinute=");
            stringBuilder.append(this.endMinute);
            stringBuilder.append(", exitAtAlarm=");
            stringBuilder.append(this.exitAtAlarm);
            stringBuilder.append(", nextAlarm=");
            stringBuilder.append(ts(this.nextAlarm));
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        protected static String ts(long time) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(new Date(time));
            stringBuilder.append(" (");
            stringBuilder.append(time);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    public static class ZenRule implements Parcelable {
        public static final Creator<ZenRule> CREATOR = new Creator<ZenRule>() {
            public ZenRule createFromParcel(Parcel source) {
                return new ZenRule(source);
            }

            public ZenRule[] newArray(int size) {
                return new ZenRule[size];
            }
        };
        public ComponentName component;
        public Condition condition;
        public Uri conditionId;
        public long creationTime;
        public boolean enabled;
        public String enabler;
        public String id;
        public String name;
        public boolean snoozing;
        public int zenMode;

        public ZenRule(Parcel source) {
            boolean z = false;
            this.enabled = source.readInt() == 1;
            if (source.readInt() == 1) {
                z = true;
            }
            this.snoozing = z;
            if (source.readInt() == 1) {
                this.name = source.readString();
            }
            this.zenMode = source.readInt();
            this.conditionId = (Uri) source.readParcelable(null);
            this.condition = (Condition) source.readParcelable(null);
            this.component = (ComponentName) source.readParcelable(null);
            if (source.readInt() == 1) {
                this.id = source.readString();
            }
            this.creationTime = source.readLong();
            if (source.readInt() == 1) {
                this.enabler = source.readString();
            }
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.enabled);
            dest.writeInt(this.snoozing);
            if (this.name != null) {
                dest.writeInt(1);
                dest.writeString(this.name);
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.zenMode);
            dest.writeParcelable(this.conditionId, 0);
            dest.writeParcelable(this.condition, 0);
            dest.writeParcelable(this.component, 0);
            if (this.id != null) {
                dest.writeInt(1);
                dest.writeString(this.id);
            } else {
                dest.writeInt(0);
            }
            dest.writeLong(this.creationTime);
            if (this.enabler != null) {
                dest.writeInt(1);
                dest.writeString(this.enabler);
                return;
            }
            dest.writeInt(0);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(ZenRule.class.getSimpleName());
            stringBuilder.append('[');
            stringBuilder.append("enabled=");
            stringBuilder.append(this.enabled);
            stringBuilder.append(",snoozing=");
            stringBuilder.append(this.snoozing);
            stringBuilder.append(",name=");
            stringBuilder.append(this.name);
            stringBuilder.append(",zenMode=");
            stringBuilder.append(Global.zenModeToString(this.zenMode));
            stringBuilder.append(",conditionId=");
            stringBuilder.append(this.conditionId);
            stringBuilder.append(",condition=");
            stringBuilder.append(this.condition);
            stringBuilder.append(",component=");
            stringBuilder.append(this.component);
            stringBuilder.append(",id=");
            stringBuilder.append(this.id);
            stringBuilder.append(",creationTime=");
            stringBuilder.append(this.creationTime);
            stringBuilder.append(",enabler=");
            stringBuilder.append(this.enabler);
            stringBuilder.append(']');
            return stringBuilder.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1138166333441L, this.id);
            proto.write(1138166333442L, this.name);
            proto.write(1112396529667L, this.creationTime);
            proto.write(1133871366148L, this.enabled);
            proto.write(1138166333445L, this.enabler);
            proto.write(1133871366150L, this.snoozing);
            proto.write(1159641169927L, this.zenMode);
            if (this.conditionId != null) {
                proto.write(1138166333448L, this.conditionId.toString());
            }
            if (this.condition != null) {
                this.condition.writeToProto(proto, 1146756268041L);
            }
            if (this.component != null) {
                this.component.writeToProto(proto, 1146756268042L);
            }
            proto.end(token);
        }

        private static void appendDiff(Diff d, String item, ZenRule from, ZenRule to) {
            if (d != null) {
                if (from == null) {
                    if (to != null) {
                        d.addLine(item, "insert");
                    }
                    return;
                }
                from.appendDiff(d, item, to);
            }
        }

        private void appendDiff(Diff d, String item, ZenRule to) {
            if (to == null) {
                d.addLine(item, "delete");
                return;
            }
            if (this.enabled != to.enabled) {
                d.addLine(item, ZenModeConfig.RULE_ATT_ENABLED, Boolean.valueOf(this.enabled), Boolean.valueOf(to.enabled));
            }
            if (this.snoozing != to.snoozing) {
                d.addLine(item, ZenModeConfig.RULE_ATT_SNOOZING, Boolean.valueOf(this.snoozing), Boolean.valueOf(to.snoozing));
            }
            if (!Objects.equals(this.name, to.name)) {
                d.addLine(item, "name", this.name, to.name);
            }
            if (this.zenMode != to.zenMode) {
                d.addLine(item, "zenMode", Integer.valueOf(this.zenMode), Integer.valueOf(to.zenMode));
            }
            if (!Objects.equals(this.conditionId, to.conditionId)) {
                d.addLine(item, ZenModeConfig.RULE_ATT_CONDITION_ID, this.conditionId, to.conditionId);
            }
            if (!Objects.equals(this.condition, to.condition)) {
                d.addLine(item, Condition.SCHEME, this.condition, to.condition);
            }
            if (!Objects.equals(this.component, to.component)) {
                d.addLine(item, "component", this.component, to.component);
            }
            if (!Objects.equals(this.id, to.id)) {
                d.addLine(item, ZenModeConfig.CONDITION_ATT_ID, this.id, to.id);
            }
            if (this.creationTime != to.creationTime) {
                d.addLine(item, "creationTime", Long.valueOf(this.creationTime), Long.valueOf(to.creationTime));
            }
            if (this.enabler != to.enabler) {
                d.addLine(item, ZenModeConfig.RULE_ATT_ENABLER, this.enabler, to.enabler);
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof ZenRule)) {
                return false;
            }
            boolean z = true;
            if (o == this) {
                return true;
            }
            ZenRule other = (ZenRule) o;
            if (!(other.enabled == this.enabled && other.snoozing == this.snoozing && Objects.equals(other.name, this.name) && other.zenMode == this.zenMode && Objects.equals(other.conditionId, this.conditionId) && Objects.equals(other.condition, this.condition) && Objects.equals(other.component, this.component) && Objects.equals(other.id, this.id) && other.creationTime == this.creationTime && Objects.equals(other.enabler, this.enabler))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Boolean.valueOf(this.enabled), Boolean.valueOf(this.snoozing), this.name, Integer.valueOf(this.zenMode), this.conditionId, this.condition, this.component, this.id, Long.valueOf(this.creationTime), this.enabler});
        }

        public boolean isAutomaticActive() {
            return this.enabled && !this.snoozing && this.component != null && isTrueOrUnknown();
        }

        public boolean isTrueOrUnknown() {
            return this.condition != null && (this.condition.state == 1 || this.condition.state == 2);
        }
    }

    public ZenModeConfig(Parcel source) {
        boolean z = true;
        this.allowCalls = source.readInt() == 1;
        this.allowRepeatCallers = source.readInt() == 1;
        this.allowMessages = source.readInt() == 1;
        this.allowReminders = source.readInt() == 1;
        this.allowEvents = source.readInt() == 1;
        this.allowCallsFrom = source.readInt();
        this.allowMessagesFrom = source.readInt();
        this.user = source.readInt();
        this.manualRule = (ZenRule) source.readParcelable(null);
        int len = source.readInt();
        if (len > 0) {
            String[] ids = new String[len];
            ZenRule[] rules = new ZenRule[len];
            source.readStringArray(ids);
            source.readTypedArray(rules, ZenRule.CREATOR);
            for (int i = 0; i < len; i++) {
                this.automaticRules.put(ids[i], rules[i]);
            }
        }
        this.allowAlarms = source.readInt() == 1;
        this.allowMedia = source.readInt() == 1;
        this.allowSystem = source.readInt() == 1;
        this.suppressedVisualEffects = source.readInt();
        if (source.readInt() != 1) {
            z = false;
        }
        this.areChannelsBypassingDnd = z;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.allowCalls);
        dest.writeInt(this.allowRepeatCallers);
        dest.writeInt(this.allowMessages);
        dest.writeInt(this.allowReminders);
        dest.writeInt(this.allowEvents);
        dest.writeInt(this.allowCallsFrom);
        dest.writeInt(this.allowMessagesFrom);
        dest.writeInt(this.user);
        dest.writeParcelable(this.manualRule, 0);
        if (this.automaticRules.isEmpty()) {
            dest.writeInt(0);
        } else {
            int len = this.automaticRules.size();
            String[] ids = new String[len];
            ZenRule[] rules = new ZenRule[len];
            for (int i = 0; i < len; i++) {
                ids[i] = (String) this.automaticRules.keyAt(i);
                rules[i] = (ZenRule) this.automaticRules.valueAt(i);
            }
            dest.writeInt(len);
            dest.writeStringArray(ids);
            dest.writeTypedArray(rules, 0);
        }
        dest.writeInt(this.allowAlarms);
        dest.writeInt(this.allowMedia);
        dest.writeInt(this.allowSystem);
        dest.writeInt(this.suppressedVisualEffects);
        dest.writeInt(this.areChannelsBypassingDnd);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(ZenModeConfig.class.getSimpleName());
        stringBuilder.append('[');
        stringBuilder.append("user=");
        stringBuilder.append(this.user);
        stringBuilder.append(",allowAlarms=");
        stringBuilder.append(this.allowAlarms);
        stringBuilder.append(",allowMedia=");
        stringBuilder.append(this.allowMedia);
        stringBuilder.append(",allowSystem=");
        stringBuilder.append(this.allowSystem);
        stringBuilder.append(",allowReminders=");
        stringBuilder.append(this.allowReminders);
        stringBuilder.append(",allowEvents=");
        stringBuilder.append(this.allowEvents);
        stringBuilder.append(",allowCalls=");
        stringBuilder.append(this.allowCalls);
        stringBuilder.append(",allowRepeatCallers=");
        stringBuilder.append(this.allowRepeatCallers);
        stringBuilder.append(",allowMessages=");
        stringBuilder.append(this.allowMessages);
        stringBuilder.append(",allowCallsFrom=");
        stringBuilder.append(sourceToString(this.allowCallsFrom));
        stringBuilder.append(",allowMessagesFrom=");
        stringBuilder.append(sourceToString(this.allowMessagesFrom));
        stringBuilder.append(",suppressedVisualEffects=");
        stringBuilder.append(this.suppressedVisualEffects);
        stringBuilder.append(",areChannelsBypassingDnd=");
        stringBuilder.append(this.areChannelsBypassingDnd);
        stringBuilder.append(",automaticRules=");
        stringBuilder.append(this.automaticRules);
        stringBuilder.append(",manualRule=");
        stringBuilder.append(this.manualRule);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private Diff diff(ZenModeConfig to) {
        Diff d = new Diff();
        if (to == null) {
            return d.addLine("config", "delete");
        }
        if (this.user != to.user) {
            d.addLine("user", Integer.valueOf(this.user), Integer.valueOf(to.user));
        }
        if (this.allowAlarms != to.allowAlarms) {
            d.addLine("allowAlarms", Boolean.valueOf(this.allowAlarms), Boolean.valueOf(to.allowAlarms));
        }
        if (this.allowMedia != to.allowMedia) {
            d.addLine("allowMedia", Boolean.valueOf(this.allowMedia), Boolean.valueOf(to.allowMedia));
        }
        if (this.allowSystem != to.allowSystem) {
            d.addLine("allowSystem", Boolean.valueOf(this.allowSystem), Boolean.valueOf(to.allowSystem));
        }
        if (this.allowCalls != to.allowCalls) {
            d.addLine("allowCalls", Boolean.valueOf(this.allowCalls), Boolean.valueOf(to.allowCalls));
        }
        if (this.allowReminders != to.allowReminders) {
            d.addLine("allowReminders", Boolean.valueOf(this.allowReminders), Boolean.valueOf(to.allowReminders));
        }
        if (this.allowEvents != to.allowEvents) {
            d.addLine("allowEvents", Boolean.valueOf(this.allowEvents), Boolean.valueOf(to.allowEvents));
        }
        if (this.allowRepeatCallers != to.allowRepeatCallers) {
            d.addLine("allowRepeatCallers", Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(to.allowRepeatCallers));
        }
        if (this.allowMessages != to.allowMessages) {
            d.addLine("allowMessages", Boolean.valueOf(this.allowMessages), Boolean.valueOf(to.allowMessages));
        }
        if (this.allowCallsFrom != to.allowCallsFrom) {
            d.addLine("allowCallsFrom", Integer.valueOf(this.allowCallsFrom), Integer.valueOf(to.allowCallsFrom));
        }
        if (this.allowMessagesFrom != to.allowMessagesFrom) {
            d.addLine("allowMessagesFrom", Integer.valueOf(this.allowMessagesFrom), Integer.valueOf(to.allowMessagesFrom));
        }
        if (this.suppressedVisualEffects != to.suppressedVisualEffects) {
            d.addLine("suppressedVisualEffects", Integer.valueOf(this.suppressedVisualEffects), Integer.valueOf(to.suppressedVisualEffects));
        }
        ArraySet<String> allRules = new ArraySet();
        addKeys(allRules, this.automaticRules);
        addKeys(allRules, to.automaticRules);
        int N = allRules.size();
        for (int i = 0; i < N; i++) {
            String rule = (String) allRules.valueAt(i);
            ZenRule toRule = null;
            ZenRule fromRule = this.automaticRules != null ? (ZenRule) this.automaticRules.get(rule) : null;
            if (to.automaticRules != null) {
                toRule = (ZenRule) to.automaticRules.get(rule);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("automaticRule[");
            stringBuilder.append(rule);
            stringBuilder.append("]");
            ZenRule.appendDiff(d, stringBuilder.toString(), fromRule, toRule);
        }
        ZenRule.appendDiff(d, "manualRule", this.manualRule, to.manualRule);
        if (this.areChannelsBypassingDnd != to.areChannelsBypassingDnd) {
            d.addLine(STATE_ATT_CHANNELS_BYPASSING_DND, Boolean.valueOf(this.areChannelsBypassingDnd), Boolean.valueOf(to.areChannelsBypassingDnd));
        }
        return d;
    }

    public static Diff diff(ZenModeConfig from, ZenModeConfig to) {
        if (from != null) {
            return from.diff(to);
        }
        Diff d = new Diff();
        if (to != null) {
            d.addLine("config", "insert");
        }
        return d;
    }

    private static <T> void addKeys(ArraySet<T> set, ArrayMap<T, ?> map) {
        if (map != null) {
            for (int i = 0; i < map.size(); i++) {
                set.add(map.keyAt(i));
            }
        }
    }

    public boolean isValid() {
        if (!isValidManualRule(this.manualRule)) {
            return false;
        }
        int N = this.automaticRules.size();
        for (int i = 0; i < N; i++) {
            if (!isValidAutomaticRule((ZenRule) this.automaticRules.valueAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidManualRule(ZenRule rule) {
        return rule == null || (Global.isValidZenMode(rule.zenMode) && sameCondition(rule));
    }

    private static boolean isValidAutomaticRule(ZenRule rule) {
        return (rule == null || TextUtils.isEmpty(rule.name) || !Global.isValidZenMode(rule.zenMode) || rule.conditionId == null || !sameCondition(rule)) ? false : true;
    }

    private static boolean sameCondition(ZenRule rule) {
        boolean z = false;
        if (rule == null) {
            return false;
        }
        if (rule.conditionId == null) {
            if (rule.condition == null) {
                z = true;
            }
            return z;
        }
        if (rule.condition == null || rule.conditionId.equals(rule.condition.id)) {
            z = true;
        }
        return z;
    }

    private static int[] generateMinuteBuckets() {
        int[] buckets = new int[15];
        buckets[0] = 15;
        int i = 1;
        buckets[1] = 30;
        buckets[2] = 45;
        while (i <= 12) {
            buckets[2 + i] = 60 * i;
            i++;
        }
        return buckets;
    }

    public static String sourceToString(int source) {
        switch (source) {
            case 0:
                return "anyone";
            case 1:
                return Contacts.AUTHORITY;
            case 2:
                return "stars";
            default:
                return "UNKNOWN";
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof ZenModeConfig)) {
            return false;
        }
        boolean z = true;
        if (o == this) {
            return true;
        }
        ZenModeConfig other = (ZenModeConfig) o;
        if (!(other.allowAlarms == this.allowAlarms && other.allowMedia == this.allowMedia && other.allowSystem == this.allowSystem && other.allowCalls == this.allowCalls && other.allowRepeatCallers == this.allowRepeatCallers && other.allowMessages == this.allowMessages && other.allowCallsFrom == this.allowCallsFrom && other.allowMessagesFrom == this.allowMessagesFrom && other.allowReminders == this.allowReminders && other.allowEvents == this.allowEvents && other.user == this.user && Objects.equals(other.automaticRules, this.automaticRules) && Objects.equals(other.manualRule, this.manualRule) && other.suppressedVisualEffects == this.suppressedVisualEffects && other.areChannelsBypassingDnd == this.areChannelsBypassingDnd)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Boolean.valueOf(this.allowAlarms), Boolean.valueOf(this.allowMedia), Boolean.valueOf(this.allowSystem), Boolean.valueOf(this.allowCalls), Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(this.allowMessages), Integer.valueOf(this.allowCallsFrom), Integer.valueOf(this.allowMessagesFrom), Boolean.valueOf(this.allowReminders), Boolean.valueOf(this.allowEvents), Integer.valueOf(this.user), this.automaticRules, this.manualRule, Integer.valueOf(this.suppressedVisualEffects), Boolean.valueOf(this.areChannelsBypassingDnd)});
    }

    private static String toDayList(int[] days) {
        if (days == null || days.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(days[i]);
        }
        return sb.toString();
    }

    private static int[] tryParseDayList(String dayList, String sep) {
        if (dayList == null) {
            return null;
        }
        String[] tokens = dayList.split(sep);
        if (tokens.length == 0) {
            return null;
        }
        int[] rt = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            int day = tryParseInt(tokens[i], -1);
            if (day == -1) {
                return null;
            }
            rt[i] = day;
        }
        return rt;
    }

    private static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static long tryParseLong(String value, long defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    public static ZenModeConfig readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type = parser.getEventType();
        if (type != 2) {
            return null;
        }
        String tag = parser.getName();
        if (!"zen".equals(tag)) {
            return null;
        }
        ZenModeConfig rt = new ZenModeConfig();
        rt.version = safeInt(parser, "version", 8);
        rt.user = safeInt(parser, "user", rt.user);
        boolean readSuppressedEffects = false;
        while (true) {
            int next = parser.next();
            int type2 = next;
            if (next != 1) {
                String tag2 = parser.getName();
                if (type2 == 3 && "zen".equals(tag2)) {
                    return rt;
                }
                if (type2 == 2) {
                    if (ALLOW_TAG.equals(tag2)) {
                        rt.allowCalls = safeBoolean(parser, ALLOW_ATT_CALLS, true);
                        rt.allowRepeatCallers = safeBoolean(parser, ALLOW_ATT_REPEAT_CALLERS, true);
                        rt.allowMessages = safeBoolean(parser, ALLOW_ATT_MESSAGES, false);
                        rt.allowReminders = safeBoolean(parser, ALLOW_ATT_REMINDERS, false);
                        rt.allowEvents = safeBoolean(parser, ALLOW_ATT_EVENTS, false);
                        next = safeInt(parser, ALLOW_ATT_FROM, -1);
                        int callsFrom = safeInt(parser, ALLOW_ATT_CALLS_FROM, -1);
                        int messagesFrom = safeInt(parser, ALLOW_ATT_MESSAGES_FROM, -1);
                        if (isValidSource(callsFrom) && isValidSource(messagesFrom)) {
                            rt.allowCallsFrom = callsFrom;
                            rt.allowMessagesFrom = messagesFrom;
                        } else if (isValidSource(next)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Migrating existing shared 'from': ");
                            stringBuilder.append(sourceToString(next));
                            Slog.i(str, stringBuilder.toString());
                            rt.allowCallsFrom = next;
                            rt.allowMessagesFrom = next;
                        } else {
                            rt.allowCallsFrom = 2;
                            rt.allowMessagesFrom = 1;
                        }
                        rt.allowAlarms = safeBoolean(parser, ALLOW_ATT_ALARMS, true);
                        rt.allowMedia = safeBoolean(parser, "media", true);
                        rt.allowSystem = safeBoolean(parser, "system", false);
                        Boolean allowWhenScreenOff = unsafeBoolean(parser, ALLOW_ATT_SCREEN_OFF);
                        if (allowWhenScreenOff != null) {
                            readSuppressedEffects = true;
                            if (allowWhenScreenOff.booleanValue()) {
                                rt.suppressedVisualEffects |= 12;
                            }
                        }
                        Boolean allowWhenScreenOn = unsafeBoolean(parser, ALLOW_ATT_SCREEN_ON);
                        if (allowWhenScreenOn != null) {
                            readSuppressedEffects = true;
                            if (allowWhenScreenOn.booleanValue()) {
                                rt.suppressedVisualEffects |= 16;
                            }
                        }
                        if (readSuppressedEffects) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Migrated visual effects to ");
                            stringBuilder2.append(rt.suppressedVisualEffects);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                    } else if (DISALLOW_TAG.equals(tag2) && !readSuppressedEffects) {
                        rt.suppressedVisualEffects = safeInt(parser, DISALLOW_ATT_VISUAL_EFFECTS, 0);
                    } else if (MANUAL_TAG.equals(tag2)) {
                        rt.manualRule = readRuleXml(parser);
                    } else if (AUTOMATIC_TAG.equals(tag2)) {
                        String id = parser.getAttributeValue(null, RULE_ATT_ID);
                        ZenRule automaticRule = readRuleXml(parser);
                        if (!(id == null || automaticRule == null)) {
                            automaticRule.id = id;
                            rt.automaticRules.put(id, automaticRule);
                        }
                    } else if ("state".equals(tag2)) {
                        rt.areChannelsBypassingDnd = safeBoolean(parser, STATE_ATT_CHANNELS_BYPASSING_DND, false);
                    }
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    public void writeXml(XmlSerializer out, Integer version) throws IOException {
        out.startTag(null, "zen");
        out.attribute(null, "version", Integer.toString(version == null ? 8 : version.intValue()));
        out.attribute(null, "user", Integer.toString(this.user));
        out.startTag(null, ALLOW_TAG);
        out.attribute(null, ALLOW_ATT_CALLS, Boolean.toString(this.allowCalls));
        out.attribute(null, ALLOW_ATT_REPEAT_CALLERS, Boolean.toString(this.allowRepeatCallers));
        out.attribute(null, ALLOW_ATT_MESSAGES, Boolean.toString(this.allowMessages));
        out.attribute(null, ALLOW_ATT_REMINDERS, Boolean.toString(this.allowReminders));
        out.attribute(null, ALLOW_ATT_EVENTS, Boolean.toString(this.allowEvents));
        out.attribute(null, ALLOW_ATT_CALLS_FROM, Integer.toString(this.allowCallsFrom));
        out.attribute(null, ALLOW_ATT_MESSAGES_FROM, Integer.toString(this.allowMessagesFrom));
        out.attribute(null, ALLOW_ATT_ALARMS, Boolean.toString(this.allowAlarms));
        out.attribute(null, "media", Boolean.toString(this.allowMedia));
        out.attribute(null, "system", Boolean.toString(this.allowSystem));
        out.endTag(null, ALLOW_TAG);
        out.startTag(null, DISALLOW_TAG);
        out.attribute(null, DISALLOW_ATT_VISUAL_EFFECTS, Integer.toString(this.suppressedVisualEffects));
        out.endTag(null, DISALLOW_TAG);
        if (this.manualRule != null) {
            out.startTag(null, MANUAL_TAG);
            writeRuleXml(this.manualRule, out);
            out.endTag(null, MANUAL_TAG);
        }
        int N = this.automaticRules.size();
        for (int i = 0; i < N; i++) {
            String id = (String) this.automaticRules.keyAt(i);
            ZenRule automaticRule = (ZenRule) this.automaticRules.valueAt(i);
            out.startTag(null, AUTOMATIC_TAG);
            out.attribute(null, RULE_ATT_ID, id);
            writeRuleXml(automaticRule, out);
            out.endTag(null, AUTOMATIC_TAG);
        }
        out.startTag(null, "state");
        out.attribute(null, STATE_ATT_CHANNELS_BYPASSING_DND, Boolean.toString(this.areChannelsBypassingDnd));
        out.endTag(null, "state");
        out.endTag(null, "zen");
    }

    public static ZenRule readRuleXml(XmlPullParser parser) {
        ZenRule rt = new ZenRule();
        rt.enabled = safeBoolean(parser, RULE_ATT_ENABLED, true);
        rt.snoozing = safeBoolean(parser, RULE_ATT_SNOOZING, false);
        rt.name = parser.getAttributeValue(null, "name");
        String zen = parser.getAttributeValue(null, "zen");
        rt.zenMode = tryParseZenMode(zen, -1);
        StringBuilder stringBuilder;
        if (rt.zenMode == -1) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad zen mode in rule xml:");
            stringBuilder.append(zen);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
        rt.conditionId = safeUri(parser, RULE_ATT_CONDITION_ID);
        rt.component = safeComponentName(parser, "component");
        rt.creationTime = safeLong(parser, "creationTime", 0);
        rt.enabler = parser.getAttributeValue(null, RULE_ATT_ENABLER);
        rt.condition = readConditionXml(parser);
        if (rt.zenMode != 1 && Condition.isValidId(rt.conditionId, SYSTEM_AUTHORITY)) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Updating zenMode of automatic rule ");
            stringBuilder.append(rt.name);
            Slog.i(str2, stringBuilder.toString());
            rt.zenMode = 1;
        }
        return rt;
    }

    public static void writeRuleXml(ZenRule rule, XmlSerializer out) throws IOException {
        out.attribute(null, RULE_ATT_ENABLED, Boolean.toString(rule.enabled));
        out.attribute(null, RULE_ATT_SNOOZING, Boolean.toString(rule.snoozing));
        if (rule.name != null) {
            out.attribute(null, "name", rule.name);
        }
        out.attribute(null, "zen", Integer.toString(rule.zenMode));
        if (rule.component != null) {
            out.attribute(null, "component", rule.component.flattenToString());
        }
        if (rule.conditionId != null) {
            out.attribute(null, RULE_ATT_CONDITION_ID, rule.conditionId.toString());
        }
        out.attribute(null, "creationTime", Long.toString(rule.creationTime));
        if (rule.enabler != null) {
            out.attribute(null, RULE_ATT_ENABLER, rule.enabler);
        }
        if (rule.condition != null) {
            writeConditionXml(rule.condition, out);
        }
    }

    public static Condition readConditionXml(XmlPullParser parser) {
        XmlPullParser xmlPullParser = parser;
        Uri id = safeUri(xmlPullParser, CONDITION_ATT_ID);
        if (id == null) {
            return null;
        }
        try {
            return new Condition(id, xmlPullParser.getAttributeValue(null, "summary"), xmlPullParser.getAttributeValue(null, CONDITION_ATT_LINE1), xmlPullParser.getAttributeValue(null, CONDITION_ATT_LINE2), safeInt(xmlPullParser, "icon", -1), safeInt(xmlPullParser, "state", -1), safeInt(xmlPullParser, "flags", -1));
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Unable to read condition xml", e);
            return null;
        }
    }

    public static void writeConditionXml(Condition c, XmlSerializer out) throws IOException {
        out.attribute(null, CONDITION_ATT_ID, c.id.toString());
        out.attribute(null, "summary", c.summary);
        out.attribute(null, CONDITION_ATT_LINE1, c.line1);
        out.attribute(null, CONDITION_ATT_LINE2, c.line2);
        out.attribute(null, "icon", Integer.toString(c.icon));
        out.attribute(null, "state", Integer.toString(c.state));
        out.attribute(null, "flags", Integer.toString(c.flags));
    }

    public static boolean isValidHour(int val) {
        return val >= 0 && val < 24;
    }

    public static boolean isValidMinute(int val) {
        return val >= 0 && val < 60;
    }

    private static boolean isValidSource(int source) {
        return source >= 0 && source <= 2;
    }

    private static Boolean unsafeBoolean(XmlPullParser parser, String att) {
        String val = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return Boolean.valueOf(Boolean.parseBoolean(val));
    }

    private static boolean safeBoolean(XmlPullParser parser, String att, boolean defValue) {
        return safeBoolean(parser.getAttributeValue(null, att), defValue);
    }

    private static boolean safeBoolean(String val, boolean defValue) {
        if (TextUtils.isEmpty(val)) {
            return defValue;
        }
        return Boolean.parseBoolean(val);
    }

    private static int safeInt(XmlPullParser parser, String att, int defValue) {
        return tryParseInt(parser.getAttributeValue(null, att), defValue);
    }

    private static ComponentName safeComponentName(XmlPullParser parser, String att) {
        String val = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return ComponentName.unflattenFromString(val);
    }

    private static Uri safeUri(XmlPullParser parser, String att) {
        String val = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return Uri.parse(val);
    }

    private static long safeLong(XmlPullParser parser, String att, long defValue) {
        return tryParseLong(parser.getAttributeValue(null, att), defValue);
    }

    public int describeContents() {
        return 0;
    }

    public ZenModeConfig copy() {
        Parcel parcel = Parcel.obtain();
        try {
            writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            ZenModeConfig zenModeConfig = new ZenModeConfig(parcel);
            return zenModeConfig;
        } finally {
            parcel.recycle();
        }
    }

    public Policy toNotificationPolicy() {
        int priorityCategories = 0;
        if (this.allowCalls) {
            priorityCategories = 0 | 8;
        }
        if (this.allowMessages) {
            priorityCategories |= 4;
        }
        if (this.allowEvents) {
            priorityCategories |= 2;
        }
        if (this.allowReminders) {
            priorityCategories |= 1;
        }
        if (this.allowRepeatCallers) {
            priorityCategories |= 16;
        }
        if (this.allowAlarms) {
            priorityCategories |= 32;
        }
        if (this.allowMedia) {
            priorityCategories |= 64;
        }
        if (this.allowSystem) {
            priorityCategories |= 128;
        }
        return new Policy(priorityCategories, sourceToPrioritySenders(this.allowCallsFrom, 1), sourceToPrioritySenders(this.allowMessagesFrom, 1), this.suppressedVisualEffects, this.areChannelsBypassingDnd ? 1 : 0);
    }

    public static ScheduleCalendar toScheduleCalendar(Uri conditionId) {
        ScheduleInfo schedule = tryParseScheduleConditionId(conditionId);
        if (schedule == null || schedule.days == null || schedule.days.length == 0) {
            return null;
        }
        ScheduleCalendar sc = new ScheduleCalendar();
        sc.setSchedule(schedule);
        sc.setTimeZone(TimeZone.getDefault());
        return sc;
    }

    private static int sourceToPrioritySenders(int source, int def) {
        switch (source) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return def;
        }
    }

    private static int prioritySendersToSource(int prioritySenders, int def) {
        switch (prioritySenders) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return def;
        }
    }

    public void applyNotificationPolicy(Policy policy) {
        if (policy != null) {
            boolean z = false;
            this.allowAlarms = (policy.priorityCategories & 32) != 0;
            this.allowMedia = (policy.priorityCategories & 64) != 0;
            this.allowSystem = (policy.priorityCategories & 128) != 0;
            this.allowEvents = (policy.priorityCategories & 2) != 0;
            this.allowReminders = (policy.priorityCategories & 1) != 0;
            this.allowCalls = (policy.priorityCategories & 8) != 0;
            this.allowMessages = (policy.priorityCategories & 4) != 0;
            this.allowRepeatCallers = (policy.priorityCategories & 16) != 0;
            this.allowCallsFrom = prioritySendersToSource(policy.priorityCallSenders, this.allowCallsFrom);
            this.allowMessagesFrom = prioritySendersToSource(policy.priorityMessageSenders, this.allowMessagesFrom);
            if (policy.suppressedVisualEffects != -1) {
                this.suppressedVisualEffects = policy.suppressedVisualEffects;
            }
            if (policy.state != -1) {
                if ((policy.state & 1) != 0) {
                    z = true;
                }
                this.areChannelsBypassingDnd = z;
            }
        }
    }

    public static Condition toTimeCondition(Context context, int minutesFromNow, int userHandle) {
        return toTimeCondition(context, minutesFromNow, userHandle, false);
    }

    public static Condition toTimeCondition(Context context, int minutesFromNow, int userHandle, boolean shortVersion) {
        return toTimeCondition(context, System.currentTimeMillis() + (minutesFromNow == 0 ? 10000 : (long) (MINUTES_MS * minutesFromNow)), minutesFromNow, userHandle, shortVersion);
    }

    public static Condition toTimeCondition(Context context, long time, int minutes, int userHandle, boolean shortVersion) {
        String quantityString;
        String line1;
        String line2;
        long j = time;
        int i = minutes;
        CharSequence formattedTime = getFormattedTime(context, j, isToday(time), userHandle);
        Resources res = context.getResources();
        int num;
        int summaryResId;
        int line1ResId;
        if (i < 60) {
            num = i;
            if (shortVersion) {
                summaryResId = 18153510;
            } else {
                summaryResId = 18153509;
            }
            quantityString = res.getQuantityString(summaryResId, num, new Object[]{Integer.valueOf(num), formattedTime});
            if (shortVersion) {
                line1ResId = 18153508;
            } else {
                line1ResId = 18153507;
            }
            line1 = res.getQuantityString(line1ResId, num, new Object[]{Integer.valueOf(num), formattedTime});
            line2 = res.getString(17041443, new Object[]{formattedTime});
        } else if (i < DAY_MINUTES) {
            num = Math.round(((float) i) / 60.0f);
            if (shortVersion) {
                summaryResId = 18153506;
            } else {
                summaryResId = 18153505;
            }
            quantityString = res.getQuantityString(summaryResId, num, new Object[]{Integer.valueOf(num), formattedTime});
            if (shortVersion) {
                line1ResId = 18153504;
            } else {
                line1ResId = 18153503;
            }
            line1 = res.getQuantityString(line1ResId, num, new Object[]{Integer.valueOf(num), formattedTime});
            line2 = res.getString(17041443, new Object[]{formattedTime});
        } else {
            quantityString = res.getString(17041443, new Object[]{formattedTime});
            line2 = quantityString;
            line1 = quantityString;
        }
        return new Condition(toCountdownConditionId(j, false), quantityString, line1, line2, 0, 1, 1);
    }

    public static Condition toNextAlarmCondition(Context context, long alarm, int userHandle) {
        long j = alarm;
        CharSequence formattedTime = getFormattedTime(context, j, isToday(alarm), userHandle);
        String line1 = context.getResources().getString(17041443, new Object[]{formattedTime});
        return new Condition(toCountdownConditionId(j, true), "", line1, "", 0, 1, 1);
    }

    public static CharSequence getFormattedTime(Context context, long time, boolean isSameDay, int userHandle) {
        String skeleton = new StringBuilder();
        skeleton.append(!isSameDay ? "EEE " : "");
        skeleton.append(DateFormat.is24HourFormat(context, userHandle) ? "Hm" : "hma");
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton.toString()), time);
    }

    public static boolean isToday(long time) {
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar endTime = new GregorianCalendar();
        endTime.setTimeInMillis(time);
        if (now.get(1) == endTime.get(1) && now.get(2) == endTime.get(2) && now.get(5) == endTime.get(5)) {
            return true;
        }
        return false;
    }

    public static Uri toCountdownConditionId(long time, boolean alarm) {
        return new Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath(COUNTDOWN_PATH).appendPath(Long.toString(time)).appendPath(IS_ALARM_PATH).appendPath(Boolean.toString(alarm)).build();
    }

    public static long tryParseCountdownConditionId(Uri conditionId) {
        if (!Condition.isValidId(conditionId, SYSTEM_AUTHORITY) || conditionId.getPathSegments().size() < 2 || !COUNTDOWN_PATH.equals(conditionId.getPathSegments().get(0))) {
            return 0;
        }
        try {
            return Long.parseLong((String) conditionId.getPathSegments().get(1));
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing countdown condition: ");
            stringBuilder.append(conditionId);
            Slog.w(str, stringBuilder.toString(), e);
            return 0;
        }
    }

    public static boolean isValidCountdownConditionId(Uri conditionId) {
        return tryParseCountdownConditionId(conditionId) != 0;
    }

    public static boolean isValidCountdownToAlarmConditionId(Uri conditionId) {
        if (tryParseCountdownConditionId(conditionId) == 0 || conditionId.getPathSegments().size() < 4 || !IS_ALARM_PATH.equals(conditionId.getPathSegments().get(2))) {
            return false;
        }
        try {
            return Boolean.parseBoolean((String) conditionId.getPathSegments().get(3));
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing countdown alarm condition: ");
            stringBuilder.append(conditionId);
            Slog.w(str, stringBuilder.toString(), e);
            return false;
        }
    }

    public static Uri toScheduleConditionId(ScheduleInfo schedule) {
        Builder appendQueryParameter = new Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath(SCHEDULE_PATH).appendQueryParameter("days", toDayList(schedule.days));
        String str = BaseMmsColumns.START;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(schedule.startHour);
        stringBuilder.append(".");
        stringBuilder.append(schedule.startMinute);
        stringBuilder = new StringBuilder();
        stringBuilder.append(schedule.endHour);
        stringBuilder.append(".");
        stringBuilder.append(schedule.endMinute);
        return appendQueryParameter.appendQueryParameter(str, stringBuilder.toString()).appendQueryParameter("end", stringBuilder.toString()).appendQueryParameter("exitAtAlarm", String.valueOf(schedule.exitAtAlarm)).build();
    }

    public static boolean isValidScheduleConditionId(Uri conditionId) {
        try {
            ScheduleInfo info = tryParseScheduleConditionId(conditionId);
            if (info == null || info.days == null || info.days.length == 0) {
                return false;
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            return false;
        }
    }

    public static ScheduleInfo tryParseScheduleConditionId(Uri conditionId) {
        boolean isSchedule = conditionId != null && conditionId.getScheme().equals(Condition.SCHEME) && conditionId.getAuthority().equals(SYSTEM_AUTHORITY) && conditionId.getPathSegments().size() == 1 && ((String) conditionId.getPathSegments().get(0)).equals(SCHEDULE_PATH);
        if (!isSchedule) {
            return null;
        }
        int[] start = tryParseHourAndMinute(conditionId.getQueryParameter(BaseMmsColumns.START));
        int[] end = tryParseHourAndMinute(conditionId.getQueryParameter("end"));
        if (start == null || end == null) {
            return null;
        }
        ScheduleInfo rt = new ScheduleInfo();
        rt.days = tryParseDayList(conditionId.getQueryParameter("days"), "\\.");
        rt.startHour = start[0];
        rt.startMinute = start[1];
        rt.endHour = end[0];
        rt.endMinute = end[1];
        rt.exitAtAlarm = safeBoolean(conditionId.getQueryParameter("exitAtAlarm"), false);
        return rt;
    }

    public static ComponentName getScheduleConditionProvider() {
        return new ComponentName(SYSTEM_AUTHORITY, "ScheduleConditionProvider");
    }

    public static Uri toEventConditionId(EventInfo event) {
        return new Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath("event").appendQueryParameter("userId", Long.toString((long) event.userId)).appendQueryParameter("calendar", event.calendar != null ? event.calendar : "").appendQueryParameter("reply", Integer.toString(event.reply)).build();
    }

    public static boolean isValidEventConditionId(Uri conditionId) {
        return tryParseEventConditionId(conditionId) != null;
    }

    public static EventInfo tryParseEventConditionId(Uri conditionId) {
        boolean isEvent = true;
        if (!(conditionId != null && conditionId.getScheme().equals(Condition.SCHEME) && conditionId.getAuthority().equals(SYSTEM_AUTHORITY) && conditionId.getPathSegments().size() == 1 && ((String) conditionId.getPathSegments().get(0)).equals("event"))) {
            isEvent = false;
        }
        if (!isEvent) {
            return null;
        }
        EventInfo rt = new EventInfo();
        rt.userId = tryParseInt(conditionId.getQueryParameter("userId"), -10000);
        rt.calendar = conditionId.getQueryParameter("calendar");
        if (TextUtils.isEmpty(rt.calendar) || tryParseLong(rt.calendar, -1) != -1) {
            rt.calendar = null;
        }
        rt.reply = tryParseInt(conditionId.getQueryParameter("reply"), 0);
        return rt;
    }

    public static ComponentName getEventConditionProvider() {
        return new ComponentName(SYSTEM_AUTHORITY, "EventConditionProvider");
    }

    private static int[] tryParseHourAndMinute(String value) {
        int[] iArr = null;
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        int i = value.indexOf(46);
        if (i < 1 || i >= value.length() - 1) {
            return null;
        }
        int hour = tryParseInt(value.substring(0, i), -1);
        int minute = tryParseInt(value.substring(i + 1), -1);
        if (isValidHour(hour) && isValidMinute(minute)) {
            iArr = new int[]{hour, minute};
        }
        return iArr;
    }

    private static int tryParseZenMode(String value, int defValue) {
        int rt = tryParseInt(value, defValue);
        return Global.isValidZenMode(rt) ? rt : defValue;
    }

    public static String newRuleId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getOwnerCaption(Context context, String owner) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(owner, null);
            if (info != null) {
                CharSequence seq = info.loadLabel(pm);
                if (seq != null) {
                    String str = seq.toString().trim();
                    if (str.length() > 0) {
                        return str;
                    }
                }
            }
        } catch (Throwable e) {
            Slog.w(TAG, "Error loading owner caption", e);
        }
        return "";
    }

    public static String getConditionSummary(Context context, ZenModeConfig config, int userHandle, boolean shortVersion) {
        return getConditionLine(context, config, userHandle, false, shortVersion);
    }

    private static String getConditionLine(Context context, ZenModeConfig config, int userHandle, boolean useLine1, boolean shortVersion) {
        Context context2 = context;
        ZenModeConfig zenModeConfig = config;
        if (zenModeConfig == null) {
            return "";
        }
        String summary = "";
        if (zenModeConfig.manualRule != null) {
            Uri id = zenModeConfig.manualRule.conditionId;
            if (zenModeConfig.manualRule.enabler != null) {
                summary = getOwnerCaption(context2, zenModeConfig.manualRule.enabler);
            } else if (id == null) {
                summary = context2.getString(17041440);
            } else {
                long time = tryParseCountdownConditionId(id);
                Condition c = zenModeConfig.manualRule.condition;
                if (time > 0) {
                    long span = time - System.currentTimeMillis();
                    c = toTimeCondition(context2, time, Math.round(((float) span) / 60000.0f), userHandle, shortVersion);
                }
                String rt = c == null ? "" : useLine1 ? c.line1 : c.summary;
                summary = TextUtils.isEmpty(rt) ? "" : rt;
            }
        }
        for (ZenRule automaticRule : zenModeConfig.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                if (summary.isEmpty()) {
                    summary = automaticRule.name;
                } else {
                    summary = context.getResources().getString(17041442, new Object[]{summary, automaticRule.name});
                }
            }
        }
        return summary;
    }

    public static boolean areAllPriorityOnlyNotificationZenSoundsMuted(Policy policy) {
        boolean allowReminders = (policy.priorityCategories & 1) != 0;
        boolean allowCalls = (policy.priorityCategories & 8) != 0;
        boolean allowMessages = (policy.priorityCategories & 4) != 0;
        boolean allowEvents = (policy.priorityCategories & 2) != 0;
        boolean allowRepeatCallers = (policy.priorityCategories & 16) != 0;
        boolean areChannelsBypassingDnd = (policy.state & 1) != 0;
        if (allowReminders || allowCalls || allowMessages || allowEvents || allowRepeatCallers || areChannelsBypassingDnd) {
            return false;
        }
        return true;
    }

    public static boolean isZenOverridingRinger(int zen, ZenModeConfig zenConfig) {
        if (zen == 2 || zen == 3) {
            return true;
        }
        if (zen == 1 && areAllPriorityOnlyNotificationZenSoundsMuted(zenConfig)) {
            return true;
        }
        return false;
    }

    public static boolean areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeConfig config) {
        return (config.allowReminders || config.allowCalls || config.allowMessages || config.allowEvents || config.allowRepeatCallers || config.areChannelsBypassingDnd) ? false : true;
    }

    public static boolean areAllZenBehaviorSoundsMuted(ZenModeConfig config) {
        return (config.allowAlarms || config.allowMedia || config.allowSystem || !areAllPriorityOnlyNotificationZenSoundsMuted(config)) ? false : true;
    }

    public static String getDescription(Context context, boolean zenOn, ZenModeConfig config, boolean describeForeverCondition) {
        String str = null;
        if (!zenOn || config == null) {
            return null;
        }
        String secondaryText = "";
        long latestEndTime = -1;
        if (config.manualRule != null) {
            Uri id = config.manualRule.conditionId;
            if (config.manualRule.enabler != null) {
                String appName = getOwnerCaption(context, config.manualRule.enabler);
                if (!appName.isEmpty()) {
                    secondaryText = appName;
                }
            } else if (id != null) {
                latestEndTime = tryParseCountdownConditionId(id);
                if (latestEndTime > 0) {
                    secondaryText = context.getString(17041443, new Object[]{getFormattedTime(context, latestEndTime, isToday(latestEndTime), context.getUserId())});
                }
            } else if (describeForeverCondition) {
                return context.getString(17041440);
            } else {
                return null;
            }
        }
        for (ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                if (!isValidEventConditionId(automaticRule.conditionId) && !isValidScheduleConditionId(automaticRule.conditionId)) {
                    return automaticRule.name;
                }
                long endTime = parseAutomaticRuleEndTime(context, automaticRule.conditionId);
                if (endTime > latestEndTime) {
                    latestEndTime = endTime;
                    secondaryText = automaticRule.name;
                }
            }
        }
        if (!secondaryText.equals("")) {
            str = secondaryText;
        }
        return str;
    }

    private static long parseAutomaticRuleEndTime(Context context, Uri id) {
        if (isValidEventConditionId(id)) {
            return SubscriptionPlan.BYTES_UNLIMITED;
        }
        if (!isValidScheduleConditionId(id)) {
            return -1;
        }
        ScheduleCalendar schedule = toScheduleCalendar(id);
        long endTimeMs = schedule.getNextChangeTime(System.currentTimeMillis());
        if (schedule.exitAtAlarm()) {
            long nextAlarm = getNextAlarm(context);
            schedule.maybeSetNextAlarm(System.currentTimeMillis(), nextAlarm);
            if (schedule.shouldExitForAlarm(endTimeMs)) {
                return nextAlarm;
            }
        }
        return endTimeMs;
    }

    private static long getNextAlarm(Context context) {
        AlarmClockInfo info = ((AlarmManager) context.getSystemService(IS_ALARM_PATH)).getNextAlarmClock(context.getUserId());
        return info != null ? info.getTriggerTime() : 0;
    }
}
