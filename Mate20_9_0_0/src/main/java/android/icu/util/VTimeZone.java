package android.icu.util;

import android.icu.impl.Grego;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

public class VTimeZone extends BasicTimeZone {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private static final int DEF_DSTSAVINGS = 3600000;
    private static final long DEF_TZSTARTTIME = 0;
    private static final String EQUALS_SIGN = "=";
    private static final int ERR = 3;
    private static final String ICAL_BEGIN = "BEGIN";
    private static final String ICAL_BEGIN_VTIMEZONE = "BEGIN:VTIMEZONE";
    private static final String ICAL_BYDAY = "BYDAY";
    private static final String ICAL_BYMONTH = "BYMONTH";
    private static final String ICAL_BYMONTHDAY = "BYMONTHDAY";
    private static final String ICAL_DAYLIGHT = "DAYLIGHT";
    private static final String[] ICAL_DOW_NAMES = new String[]{"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
    private static final String ICAL_DTSTART = "DTSTART";
    private static final String ICAL_END = "END";
    private static final String ICAL_END_VTIMEZONE = "END:VTIMEZONE";
    private static final String ICAL_FREQ = "FREQ";
    private static final String ICAL_LASTMOD = "LAST-MODIFIED";
    private static final String ICAL_RDATE = "RDATE";
    private static final String ICAL_RRULE = "RRULE";
    private static final String ICAL_STANDARD = "STANDARD";
    private static final String ICAL_TZID = "TZID";
    private static final String ICAL_TZNAME = "TZNAME";
    private static final String ICAL_TZOFFSETFROM = "TZOFFSETFROM";
    private static final String ICAL_TZOFFSETTO = "TZOFFSETTO";
    private static final String ICAL_TZURL = "TZURL";
    private static final String ICAL_UNTIL = "UNTIL";
    private static final String ICAL_VTIMEZONE = "VTIMEZONE";
    private static final String ICAL_YEARLY = "YEARLY";
    private static final String ICU_TZINFO_PROP = "X-TZINFO";
    private static String ICU_TZVERSION = null;
    private static final int INI = 0;
    private static final long MAX_TIME = Long.MAX_VALUE;
    private static final long MIN_TIME = Long.MIN_VALUE;
    private static final int[] MONTHLENGTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final String NEWLINE = "\r\n";
    private static final String SEMICOLON = ";";
    private static final int TZI = 2;
    private static final int VTZ = 1;
    private static final long serialVersionUID = -6851467294127795902L;
    private volatile transient boolean isFrozen = false;
    private Date lastmod = null;
    private String olsonzid = null;
    private BasicTimeZone tz;
    private String tzurl = null;
    private List<String> vtzlines;

    static {
        try {
            ICU_TZVERSION = TimeZone.getTZDataVersion();
        } catch (MissingResourceException e) {
            ICU_TZVERSION = null;
        }
    }

    public static VTimeZone create(String tzid) {
        BasicTimeZone basicTimeZone = TimeZone.getFrozenICUTimeZone(tzid, true);
        if (basicTimeZone == null) {
            return null;
        }
        VTimeZone vtz = new VTimeZone(tzid);
        vtz.tz = (BasicTimeZone) basicTimeZone.cloneAsThawed();
        vtz.olsonzid = vtz.tz.getID();
        return vtz;
    }

    public static VTimeZone create(Reader reader) {
        VTimeZone vtz = new VTimeZone();
        if (vtz.load(reader)) {
            return vtz;
        }
        return null;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.tz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    public void getOffset(long date, boolean local, int[] offsets) {
        this.tz.getOffset(date, local, offsets);
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        this.tz.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }

    public int getRawOffset() {
        return this.tz.getRawOffset();
    }

    public boolean inDaylightTime(Date date) {
        return this.tz.inDaylightTime(date);
    }

    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tz.setRawOffset(offsetMillis);
    }

    public boolean useDaylightTime() {
        return this.tz.useDaylightTime();
    }

    public boolean observesDaylightTime() {
        return this.tz.observesDaylightTime();
    }

    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (other instanceof VTimeZone) {
            return this.tz.hasSameRules(((VTimeZone) other).tz);
        }
        return this.tz.hasSameRules(other);
    }

    public String getTZURL() {
        return this.tzurl;
    }

    public void setTZURL(String url) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tzurl = url;
    }

    public Date getLastModified() {
        return this.lastmod;
    }

    public void setLastModified(Date date) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.lastmod = date;
    }

    public void write(Writer writer) throws IOException {
        BufferedWriter bw = new BufferedWriter(writer);
        if (this.vtzlines != null) {
            for (String line : this.vtzlines) {
                if (line.startsWith("TZURL:")) {
                    if (this.tzurl != null) {
                        bw.write(ICAL_TZURL);
                        bw.write(COLON);
                        bw.write(this.tzurl);
                        bw.write(NEWLINE);
                    }
                } else if (!line.startsWith("LAST-MODIFIED:")) {
                    bw.write(line);
                    bw.write(NEWLINE);
                } else if (this.lastmod != null) {
                    bw.write(ICAL_LASTMOD);
                    bw.write(COLON);
                    bw.write(getUTCDateTimeString(this.lastmod.getTime()));
                    bw.write(NEWLINE);
                }
            }
            bw.flush();
            return;
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("X-TZINFO:");
            stringBuilder.append(this.olsonzid);
            stringBuilder.append("[");
            stringBuilder.append(ICU_TZVERSION);
            stringBuilder.append("]");
            customProperties[0] = stringBuilder.toString();
        }
        writeZone(writer, this.tz, customProperties);
    }

    public void write(Writer writer, long start) throws IOException {
        TimeZoneRule[] rules = this.tz.getTimeZoneRules(start);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("X-TZINFO:");
            stringBuilder.append(this.olsonzid);
            stringBuilder.append("[");
            stringBuilder.append(ICU_TZVERSION);
            stringBuilder.append("/Partial@");
            stringBuilder.append(start);
            stringBuilder.append("]");
            customProperties[0] = stringBuilder.toString();
        }
        writeZone(writer, rbtz, customProperties);
    }

    public void writeSimple(Writer writer, long time) throws IOException {
        TimeZoneRule[] rules = this.tz.getSimpleTimeZoneRulesNear(time);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("X-TZINFO:");
            stringBuilder.append(this.olsonzid);
            stringBuilder.append("[");
            stringBuilder.append(ICU_TZVERSION);
            stringBuilder.append("/Simple@");
            stringBuilder.append(time);
            stringBuilder.append("]");
            customProperties[0] = stringBuilder.toString();
        }
        writeZone(writer, rbtz, customProperties);
    }

    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        return this.tz.getNextTransition(base, inclusive);
    }

    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        return this.tz.getPreviousTransition(base, inclusive);
    }

    public boolean hasEquivalentTransitions(TimeZone other, long start, long end) {
        if (this == other) {
            return true;
        }
        return this.tz.hasEquivalentTransitions(other, start, end);
    }

    public TimeZoneRule[] getTimeZoneRules() {
        return this.tz.getTimeZoneRules();
    }

    public TimeZoneRule[] getTimeZoneRules(long start) {
        return this.tz.getTimeZoneRules(start);
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private VTimeZone() {
    }

    private VTimeZone(String tzid) {
        super(tzid);
    }

    private boolean load(Reader reader) {
        try {
            this.vtzlines = new LinkedList();
            boolean eol = false;
            boolean start = false;
            boolean success = false;
            StringBuilder line = new StringBuilder();
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    if (start && line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                        this.vtzlines.add(line.toString());
                        success = true;
                    }
                } else if (ch != 13) {
                    if (eol) {
                        if (!(ch == 9 || ch == 32)) {
                            if (start && line.length() > 0) {
                                this.vtzlines.add(line.toString());
                            }
                            line.setLength(0);
                            if (ch != 10) {
                                line.append((char) ch);
                            }
                        }
                        eol = false;
                    } else if (ch == 10) {
                        eol = true;
                        if (start) {
                            if (line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                                this.vtzlines.add(line.toString());
                                success = true;
                                break;
                            }
                        } else if (line.toString().startsWith(ICAL_BEGIN_VTIMEZONE)) {
                            this.vtzlines.add(line.toString());
                            line.setLength(0);
                            start = true;
                            eol = false;
                        }
                    } else {
                        line.append((char) ch);
                    }
                }
            }
            if (success) {
                return parse();
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02be  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x02b9 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x02b9 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02be  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02be  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x02b9 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x020a  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x02b9 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02be  */
    /* JADX WARNING: Missing block: B:142:0x02b2, code skipped:
            r5 = r40;
            r6 = r42;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parse() {
        int initialRawOffset;
        int initialDSTSavings;
        String from;
        TimeZoneRule rule;
        int i;
        int dstSavings;
        if (this.vtzlines == null || this.vtzlines.size() == 0) {
            return false;
        }
        int state;
        String to;
        int rawOffset;
        int state2 = 0;
        String from2 = null;
        String to2 = null;
        String tzname = null;
        String dtstart = null;
        List<String> dates = null;
        List<TimeZoneRule> rules = new ArrayList();
        Iterator valueSep = this.vtzlines.iterator();
        long firstStart = MAX_TIME;
        int initialDSTSavings2 = 0;
        int initialRawOffset2 = 0;
        boolean isRRULE = false;
        boolean dst = false;
        String tzid = null;
        while (valueSep.hasNext()) {
            String line = (String) valueSep.next();
            Iterator it = valueSep;
            int valueSep2 = line.indexOf(COLON);
            if (valueSep2 < 0) {
                valueSep = it;
            } else {
                initialRawOffset = initialRawOffset2;
                initialDSTSavings = initialDSTSavings2;
                String name = line.substring(0, valueSep2);
                String value = line.substring(valueSep2 + 1);
                switch (state2) {
                    case 0:
                        state = state2;
                        from = from2;
                        to = to2;
                        if (name.equals(ICAL_BEGIN) && value.equals(ICAL_VTIMEZONE)) {
                            state2 = 1;
                            break;
                        }
                    case 1:
                        state = state2;
                        from = from2;
                        if (!name.equals(ICAL_TZID)) {
                            if (name.equals(ICAL_TZURL)) {
                                this.tzurl = value;
                                to = to2;
                            } else if (name.equals(ICAL_LASTMOD)) {
                                to = to2;
                                this.lastmod = new Date(parseDateTimeString(value, 0));
                            } else {
                                to = to2;
                                if (name.equals(ICAL_BEGIN)) {
                                    boolean isDST = value.equals(ICAL_DAYLIGHT);
                                    if (value.equals(ICAL_STANDARD) != 0 || isDST) {
                                        if (tzid != null) {
                                            from2 = null;
                                            to2 = null;
                                            tzname = null;
                                            dst = isDST;
                                            isRRULE = false;
                                            state2 = 2;
                                            dates = 0;
                                            break;
                                        }
                                        valueSep2 = 3;
                                    } else {
                                        valueSep2 = 3;
                                    }
                                    state2 = valueSep2;
                                    break;
                                } else if (name.equals(ICAL_END)) {
                                }
                            }
                            state2 = state;
                            break;
                        }
                        tzid = value;
                        state2 = state;
                        break;
                    case 2:
                        if (!name.equals(ICAL_DTSTART)) {
                            if (!name.equals(ICAL_TZNAME)) {
                                if (!name.equals(ICAL_TZOFFSETFROM)) {
                                    if (!name.equals(ICAL_TZOFFSETTO)) {
                                        if (!name.equals(ICAL_RDATE)) {
                                            if (!name.equals(ICAL_RRULE)) {
                                                if (!name.equals(ICAL_END)) {
                                                    state = state2;
                                                    from = from2;
                                                    to = to2;
                                                    state2 = state;
                                                    break;
                                                }
                                                if (dtstart == null || from2 == null) {
                                                    state = state2;
                                                    from = from2;
                                                } else if (to2 == null) {
                                                    state = state2;
                                                    from = from2;
                                                } else {
                                                    if (tzname == null) {
                                                        tzname = getDefaultTZName(tzid, dst);
                                                    }
                                                    int fromOffset;
                                                    int toOffset;
                                                    int dstSavings2;
                                                    long start;
                                                    int initialRawOffset3;
                                                    try {
                                                        fromOffset = offsetStrToMillis(from2);
                                                        try {
                                                            int dstSavings3;
                                                            toOffset = offsetStrToMillis(to2);
                                                            if (dst) {
                                                                rule = null;
                                                                valueSep2 = fromOffset;
                                                                if (toOffset - valueSep2 > 0) {
                                                                    rawOffset = valueSep2;
                                                                    dstSavings3 = toOffset - valueSep2;
                                                                } else {
                                                                    rawOffset = toOffset - 3600000;
                                                                    dstSavings3 = 3600000;
                                                                }
                                                            } else {
                                                                rule = null;
                                                                valueSep2 = fromOffset;
                                                                rawOffset = toOffset;
                                                                dstSavings3 = 0;
                                                            }
                                                            fromOffset = rawOffset;
                                                            dstSavings2 = dstSavings3;
                                                            try {
                                                                TimeZoneRule rule2;
                                                                TimeZoneRule rule3;
                                                                start = parseDateTimeString(dtstart, valueSep2);
                                                                if (isRRULE) {
                                                                    try {
                                                                        rule2 = createRuleByRRULE(tzname, fromOffset, dstSavings2, start, dates, valueSep2);
                                                                    } catch (IllegalArgumentException e) {
                                                                        state = state2;
                                                                        from = from2;
                                                                        i = fromOffset;
                                                                        state2 = rule;
                                                                        fromOffset = valueSep2;
                                                                        initialRawOffset3 = initialRawOffset;
                                                                        valueSep2 = fromOffset;
                                                                        if (state2 == 0) {
                                                                        }
                                                                        from2 = from;
                                                                        if (state2 == 3) {
                                                                        }
                                                                    }
                                                                } else {
                                                                    try {
                                                                        rule2 = createRuleByRDATE(tzname, fromOffset, dstSavings2, start, dates, valueSep2);
                                                                    } catch (IllegalArgumentException e2) {
                                                                        state = state2;
                                                                        from = from2;
                                                                    }
                                                                }
                                                                state2 = rule2;
                                                                if (state2 != 0) {
                                                                    from = from2;
                                                                    try {
                                                                        Date from3 = state2.getFirstStart(valueSep2, null);
                                                                        if (from3.getTime() < firstStart) {
                                                                            rawOffset = from3.getTime();
                                                                            if (dstSavings2 > 0) {
                                                                                initialRawOffset3 = valueSep2;
                                                                                rule3 = state2;
                                                                                firstStart = rawOffset;
                                                                                state2 = 0;
                                                                            } else {
                                                                                rule3 = state2;
                                                                                if (valueSep2 - toOffset == 3600000) {
                                                                                    initialRawOffset3 = valueSep2 - 3600000;
                                                                                    state2 = 3600000;
                                                                                } else {
                                                                                    initialRawOffset3 = valueSep2;
                                                                                    state2 = 0;
                                                                                }
                                                                                firstStart = rawOffset;
                                                                            }
                                                                            initialDSTSavings = state2;
                                                                            i = fromOffset;
                                                                            state2 = rule3;
                                                                            if (state2 == 0) {
                                                                                initialRawOffset = initialRawOffset3;
                                                                                state2 = 3;
                                                                            } else {
                                                                                rules.add(state2);
                                                                                initialRawOffset = initialRawOffset3;
                                                                                state2 = 1;
                                                                            }
                                                                        } else {
                                                                            rule3 = state2;
                                                                        }
                                                                    } catch (IllegalArgumentException e3) {
                                                                        Object obj = state2;
                                                                        i = fromOffset;
                                                                        fromOffset = valueSep2;
                                                                        initialRawOffset3 = initialRawOffset;
                                                                        valueSep2 = fromOffset;
                                                                        if (state2 == 0) {
                                                                        }
                                                                        from2 = from;
                                                                        if (state2 == 3) {
                                                                            this.vtzlines = null;
                                                                            return false;
                                                                        }
                                                                        valueSep = it;
                                                                        initialRawOffset2 = initialRawOffset;
                                                                        initialDSTSavings2 = initialDSTSavings;
                                                                    }
                                                                } else {
                                                                    rule3 = state2;
                                                                    from = from2;
                                                                }
                                                                initialRawOffset3 = initialRawOffset;
                                                                state2 = initialDSTSavings;
                                                                initialDSTSavings = state2;
                                                                i = fromOffset;
                                                                state2 = rule3;
                                                            } catch (IllegalArgumentException e4) {
                                                                state = state2;
                                                                from = from2;
                                                                dstSavings = 0;
                                                                i = fromOffset;
                                                                state2 = rule;
                                                                fromOffset = valueSep2;
                                                                initialRawOffset3 = initialRawOffset;
                                                                valueSep2 = fromOffset;
                                                                if (state2 == 0) {
                                                                }
                                                                from2 = from;
                                                                if (state2 == 3) {
                                                                }
                                                            }
                                                        } catch (IllegalArgumentException e5) {
                                                            state = state2;
                                                            from = from2;
                                                            valueSep2 = fromOffset;
                                                            toOffset = 0;
                                                            dstSavings2 = 0;
                                                            dstSavings = 0;
                                                            state2 = 0;
                                                            initialRawOffset3 = initialRawOffset;
                                                            valueSep2 = fromOffset;
                                                            if (state2 == 0) {
                                                            }
                                                            from2 = from;
                                                            if (state2 == 3) {
                                                            }
                                                        }
                                                    } catch (IllegalArgumentException e6) {
                                                        state = state2;
                                                        from = from2;
                                                        fromOffset = 0;
                                                        toOffset = 0;
                                                        dstSavings2 = 0;
                                                        start = DEF_TZSTARTTIME;
                                                        state2 = 0;
                                                        initialRawOffset3 = initialRawOffset;
                                                        valueSep2 = fromOffset;
                                                        if (state2 == 0) {
                                                        }
                                                        from2 = from;
                                                        if (state2 == 3) {
                                                        }
                                                    }
                                                    if (state2 == 0) {
                                                    }
                                                }
                                                state2 = 3;
                                            } else if (isRRULE || dates == null) {
                                                if (dates == null) {
                                                    dates = new LinkedList();
                                                }
                                                dates.add(value);
                                                isRRULE = true;
                                            } else {
                                                state2 = 3;
                                            }
                                        } else if (isRRULE) {
                                            state2 = 3;
                                        } else {
                                            if (dates == null) {
                                                dates = new LinkedList();
                                            }
                                            int i2 = valueSep2;
                                            StringTokenizer st = new StringTokenizer(value, COMMA);
                                            while (st.hasMoreTokens() != 0) {
                                                dates.add(st.nextToken());
                                            }
                                        }
                                    } else {
                                        to2 = value;
                                    }
                                } else {
                                    from2 = value;
                                }
                            } else {
                                tzname = value;
                            }
                        } else {
                            dtstart = value;
                        }
                        break;
                    default:
                        state = state2;
                        from = from2;
                        to = to2;
                }
            }
        }
        state = state2;
        from = from2;
        to = to2;
        initialRawOffset = initialRawOffset2;
        initialDSTSavings = initialDSTSavings2;
        if (rules.size() == 0) {
            return false;
        }
        String tzname2;
        initialRawOffset2 = initialRawOffset;
        initialDSTSavings2 = initialDSTSavings;
        InitialTimeZoneRule initialRule = new InitialTimeZoneRule(getDefaultTZName(tzid, false), initialRawOffset2, initialDSTSavings2);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(tzid, initialRule);
        int finalRuleCount = 0;
        int finalRuleIdx = -1;
        state2 = 0;
        while (state2 < rules.size()) {
            TimeZoneRule r = (TimeZoneRule) rules.get(state2);
            InitialTimeZoneRule initialRule2 = initialRule;
            if ((r instanceof AnnualTimeZoneRule) != null) {
                tzname2 = tzname;
                if (((AnnualTimeZoneRule) r).getEndYear() == Integer.MAX_VALUE) {
                    finalRuleCount++;
                    finalRuleIdx = state2;
                }
            } else {
                tzname2 = tzname;
            }
            state2++;
            initialRule = initialRule2;
            tzname = tzname2;
        }
        tzname2 = tzname;
        if (finalRuleCount > 2) {
            return false;
        }
        String str;
        boolean z;
        List<String> list;
        if (finalRuleCount != 1) {
            str = dtstart;
            z = dst;
            list = dates;
        } else if (rules.size() == 1) {
            rules.clear();
            int i3 = finalRuleCount;
            str = dtstart;
            z = dst;
            list = dates;
        } else {
            AnnualTimeZoneRule finalRule = (AnnualTimeZoneRule) rules.get(finalRuleIdx);
            int tmpRaw = finalRule.getRawOffset();
            int tmpDST = finalRule.getDSTSavings();
            Date finalStart = finalRule.getFirstStart(initialRawOffset2, initialDSTSavings2);
            finalRuleCount = finalStart;
            rawOffset = 0;
            while (true) {
                str = dtstart;
                z = dst;
                int i4 = rawOffset;
                if (i4 < rules.size()) {
                    if (finalRuleIdx == i4) {
                        list = dates;
                    } else {
                        TimeZoneRule r2 = (TimeZoneRule) rules.get(i4);
                        list = dates;
                        dates = r2.getFinalStart(tmpRaw, tmpDST);
                        if (dates.after(finalRuleCount)) {
                            finalRuleCount = finalRule.getNextStart(dates.getTime(), r2.getRawOffset(), r2.getDSTSavings(), false);
                        }
                    }
                    rawOffset = i4 + 1;
                    dtstart = str;
                    dst = z;
                    dates = list;
                } else {
                    if (finalRuleCount == finalStart) {
                        String timeArrayTimeZoneRule = new TimeArrayTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), new long[]{finalStart.getTime()}, 2);
                    } else {
                        dtstart = new AnnualTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), finalRule.getRule(), finalRule.getStartYear(), Grego.timeToFields(finalRuleCount.getTime(), null)[0]);
                    }
                    rules.set(finalRuleIdx, dtstart);
                }
            }
        }
        for (TimeZoneRule r3 : rules) {
            rbtz.addTransitionRule(r3);
        }
        this.tz = rbtz;
        setID(tzid);
        return true;
    }

    private static String getDefaultTZName(String tzid, boolean isDST) {
        StringBuilder stringBuilder;
        if (isDST) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(tzid);
            stringBuilder.append("(DST)");
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(tzid);
        stringBuilder.append("(STD)");
        return stringBuilder.toString();
    }

    private static TimeZoneRule createRuleByRRULE(String tzname, int rawOffset, int dstSavings, long start, List<String> dates, int fromOffset) {
        List list = dates;
        Object unt = null;
        int i;
        if (list == null || dates.size() == 0) {
            i = fromOffset;
            return null;
        }
        long[] until = new long[1];
        int[] ruleFields = parseRRULE((String) list.get(0), until);
        if (ruleFields == null) {
            return null;
        }
        int firstDay;
        int[] iArr;
        int daysCount;
        int diff;
        DateTimeRule adtr;
        int month = ruleFields[0];
        int dayOfWeek = ruleFields[1];
        int nthDayOfWeek = ruleFields[2];
        int dayOfMonth = ruleFields[3];
        int i2 = -1;
        int i3 = 7;
        int[] days;
        int i4;
        int i5;
        if (dates.size() == 1) {
            if (ruleFields.length > 4) {
                if (ruleFields.length != 10 || month == -1 || dayOfWeek == 0) {
                    return null;
                }
                days = new int[7];
                firstDay = 31;
                i4 = 0;
                while (i4 < 7) {
                    days[i4] = ruleFields[3 + i4];
                    days[i4] = days[i4] > 0 ? days[i4] : (MONTHLENGTH[month] + days[i4]) + 1;
                    firstDay = days[i4] < firstDay ? days[i4] : firstDay;
                    i4++;
                }
                i5 = 1;
                while (i5 < i3) {
                    boolean found = false;
                    i4 = 0;
                    while (i4 < i3) {
                        if (days[i4] == firstDay + i5) {
                            found = true;
                            break;
                        }
                        i4++;
                        i3 = 7;
                    }
                    if (!found) {
                        return null;
                    }
                    i5++;
                    i3 = 7;
                }
                dayOfMonth = firstDay;
            }
            iArr = null;
        } else if (month == -1 || dayOfWeek == 0 || dayOfMonth == 0) {
            i = fromOffset;
            return null;
        } else if (dates.size() > 7) {
            return null;
        } else {
            i5 = month;
            daysCount = ruleFields.length - 3;
            i4 = 31;
            for (firstDay = 0; firstDay < daysCount; firstDay++) {
                i3 = ruleFields[3 + firstDay];
                i3 = i3 > 0 ? i3 : (MONTHLENGTH[month] + i3) + 1;
                i4 = i3 < i4 ? i3 : i4;
            }
            i3 = -1;
            firstDay = i5;
            i5 = 1;
            while (i5 < dates.size()) {
                long[] unt2 = new long[1];
                days = parseRRULE((String) list.get(i5), unt2);
                if (unt2[0] > until[0]) {
                    until = unt2;
                }
                if (days[0] == i2 || days[1] == 0 || days[3] == 0) {
                    return null;
                }
                i2 = days.length - 3;
                if (daysCount + i2 > 7) {
                    return null;
                }
                if (days[1] != dayOfWeek) {
                    return null;
                }
                int i6;
                if (days[0] == month) {
                    i6 = 0;
                } else if (i3 == -1) {
                    diff = days[0] - month;
                    if (diff == -11 || diff == -1) {
                        i6 = 0;
                        i3 = days[0];
                        firstDay = i3;
                        i4 = 31;
                    } else if (diff != 11 && diff != 1) {
                        return null;
                    } else {
                        i6 = 0;
                        i3 = days[0];
                    }
                } else {
                    i6 = 0;
                    if (!(days[i6] == month || days[i6] == i3)) {
                        return null;
                    }
                }
                if (days[i6] == firstDay) {
                    for (diff = 0; diff < i2; diff++) {
                        int dom = days[3 + diff];
                        i6 = dom > 0 ? dom : (MONTHLENGTH[days[0]] + dom) + 1;
                        i4 = i6 < i4 ? i6 : i4;
                    }
                }
                daysCount += i2;
                i5++;
                List<String> list2 = dates;
                unt = null;
                i2 = -1;
            }
            iArr = unt;
            if (daysCount != 7) {
                return iArr;
            }
            month = firstDay;
            dayOfMonth = i4;
        }
        int[] dfields = Grego.timeToFields(start + ((long) fromOffset), iArr);
        daysCount = dfields[0];
        if (month == -1) {
            month = dfields[1];
        }
        if (dayOfWeek == 0 && nthDayOfWeek == 0 && dayOfMonth == 0) {
            diff = dfields[2];
        } else {
            diff = dayOfMonth;
        }
        int timeInDay = dfields[5];
        firstDay = Integer.MAX_VALUE;
        if (until[0] != MIN_TIME) {
            Grego.timeToFields(until[0], dfields);
            firstDay = dfields[0];
        }
        int endYear = firstDay;
        if (dayOfWeek == 0 && nthDayOfWeek == 0 && diff != 0) {
            adtr = new DateTimeRule(month, diff, timeInDay, 0);
        } else if (dayOfWeek != 0 && nthDayOfWeek != 0 && diff == 0) {
            adtr = new DateTimeRule(month, nthDayOfWeek, dayOfWeek, timeInDay, 0);
        } else if (dayOfWeek == 0 || nthDayOfWeek != 0 || diff == 0) {
            return null;
        } else {
            int i7 = timeInDay;
            adtr = new DateTimeRule(month, diff, dayOfWeek, true, timeInDay, 0);
            return new AnnualTimeZoneRule(tzname, rawOffset, dstSavings, adtr, daysCount, endYear);
        }
        return new AnnualTimeZoneRule(tzname, rawOffset, dstSavings, adtr, daysCount, endYear);
    }

    private static int[] parseRRULE(String rrule, long[] until) {
        int sep;
        int month;
        boolean z;
        int[] dayOfMonth = null;
        long untilTime = MIN_TIME;
        boolean yearly = false;
        boolean parseError = false;
        StringTokenizer st = new StringTokenizer(rrule, SEMICOLON);
        int nthDayOfWeek = 0;
        int dayOfWeek = 0;
        int month2 = -1;
        while (st.hasMoreTokens()) {
            String prop = st.nextToken();
            sep = prop.indexOf(EQUALS_SIGN);
            if (sep == -1) {
                month = month2;
                z = parseError;
                parseError = true;
                break;
            }
            String attr = prop.substring(0, sep);
            String value = prop.substring(sep + 1);
            if (attr.equals(ICAL_FREQ)) {
                if (value.equals(ICAL_YEARLY)) {
                    yearly = true;
                } else {
                    parseError = true;
                }
            } else if (attr.equals(ICAL_UNTIL)) {
                try {
                    untilTime = parseDateTimeString(value, 0);
                } catch (IllegalArgumentException iae) {
                    IllegalArgumentException illegalArgumentException = iae;
                    parseError = true;
                }
            } else if (!attr.equals(ICAL_BYMONTH)) {
                int sign;
                if (attr.equals(ICAL_BYDAY)) {
                    int length = value.length();
                    month = month2;
                    if (length >= 2 && length <= 4) {
                        if (length > 2) {
                            int sign2 = 1;
                            if (value.charAt(0) == '+') {
                                sign = 1;
                            } else if (value.charAt(0) == '-') {
                                sign = -1;
                            } else {
                                if (length == 4) {
                                    parseError = true;
                                    break;
                                }
                                sign = Integer.parseInt(value.substring(length - 3, length - 2));
                                if (sign != 0 || sign > 4) {
                                    parseError = true;
                                    break;
                                }
                                sign *= sign2;
                                value = value.substring(length - 2);
                                nthDayOfWeek = sign;
                            }
                            sign2 = sign;
                            try {
                                sign = Integer.parseInt(value.substring(length - 3, length - 2));
                                if (sign != 0) {
                                }
                                parseError = true;
                                break;
                            } catch (NumberFormatException e) {
                                parseError = true;
                            }
                        }
                        sign = 0;
                        while (sign < ICAL_DOW_NAMES.length && value.equals(ICAL_DOW_NAMES[sign]) == 0) {
                            sign++;
                        }
                        if (sign >= ICAL_DOW_NAMES.length) {
                            parseError = true;
                            break;
                        }
                        dayOfWeek = sign + 1;
                    } else {
                        parseError = true;
                        break;
                    }
                }
                month = month2;
                if (attr.equals(ICAL_BYMONTHDAY)) {
                    StringTokenizer days = new StringTokenizer(value, COMMA);
                    dayOfMonth = new int[days.countTokens()];
                    int index = 0;
                    while (true) {
                        sign = index;
                        if (!days.hasMoreTokens()) {
                            break;
                        }
                        index = sign + 1;
                        z = parseError;
                        try {
                            dayOfMonth[sign] = Integer.parseInt(days.nextToken());
                            parseError = z;
                        } catch (NumberFormatException e2) {
                            parseError = true;
                        }
                    }
                }
                z = parseError;
                month2 = month;
                month2 = month;
            } else if (value.length() > 2) {
                parseError = true;
            } else {
                try {
                    month2 = Integer.parseInt(value) - 1;
                    if (month2 < 0 || month2 >= 12) {
                        parseError = true;
                    }
                } catch (NumberFormatException nfe) {
                    NumberFormatException numberFormatException = nfe;
                    parseError = true;
                }
            }
            month = month2;
            break;
        }
        month = month2;
        z = parseError;
        if (parseError || !yearly) {
            return null;
        }
        int[] results;
        until[0] = untilTime;
        if (dayOfMonth == null) {
            results = new int[4];
            results[3] = 0;
        } else {
            results = new int[(dayOfMonth.length + 3)];
            for (sep = 0; sep < dayOfMonth.length; sep++) {
                results[3 + sep] = dayOfMonth[sep];
            }
        }
        results[0] = month;
        results[1] = dayOfWeek;
        results[2] = nthDayOfWeek;
        return results;
    }

    private static TimeZoneRule createRuleByRDATE(String tzname, int rawOffset, int dstSavings, long start, List<String> dates, int fromOffset) {
        long[] times;
        int idx = 0;
        if (dates == null || dates.size() == 0) {
            times = new long[]{start};
        } else {
            times = new long[dates.size()];
            try {
                for (String date : dates) {
                    int idx2 = idx + 1;
                    try {
                        times[idx] = parseDateTimeString(date, fromOffset);
                        idx = idx2;
                    } catch (IllegalArgumentException e) {
                        IllegalArgumentException illegalArgumentException = e;
                        idx = idx2;
                        return null;
                    }
                }
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
        return new TimeArrayTimeZoneRule(tzname, rawOffset, dstSavings, times, 2);
    }

    /* JADX WARNING: Missing block: B:15:0x00ac, code skipped:
            r5 = r38;
            r6 = r39;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeZone(Writer w, BasicTimeZone basictz, String[] customProperties) throws IOException {
        int i;
        int stdToOffset;
        int dstToOffset;
        int dstFromOffset;
        int stdFromOffset;
        String dstName;
        String stdName;
        int dstCount;
        AnnualTimeZoneRule finalDstRule;
        AnnualTimeZoneRule finalStdRule;
        boolean hasTransitions;
        boolean z;
        int dstWeekInMonth;
        int dstMonth;
        int dstDayOfWeek;
        int stdWeekInMonth;
        int[] dtfields;
        int dstCount2;
        Writer writer = w;
        BasicTimeZone basicTimeZone = basictz;
        String[] strArr = customProperties;
        writeHeader(w);
        if (strArr != null && strArr.length > 0) {
            for (i = 0; i < strArr.length; i++) {
                if (strArr[i] != null) {
                    writer.write(strArr[i]);
                    writer.write(NEWLINE);
                }
            }
        }
        AnnualTimeZoneRule finalDstRule2 = null;
        int stdStartYear = 0;
        long stdStartTime = DEF_TZSTARTTIME;
        long stdUntilTime = DEF_TZSTARTTIME;
        int stdCount = 0;
        AnnualTimeZoneRule finalStdRule2 = null;
        int[] dtfields2 = new int[6];
        int stdToOffset2 = 0;
        int stdMonth = 0;
        int stdDayOfWeek = 0;
        int stdWeekInMonth2 = 0;
        int stdMillisInDay = 0;
        int stdFromDSTSavings = 0;
        int dstFromDSTSavings = 0;
        int dstWeekInMonth2 = 0;
        int stdFromOffset2 = 0;
        String dstName2 = null;
        long t = MIN_TIME;
        boolean hasTransitions2 = false;
        int dstFromOffset2 = 0;
        int dstMillisInDay = 0;
        int dstToOffset2 = 0;
        int dstMonth2 = 0;
        int dstStartYear = 0;
        int dstDayOfWeek2 = 0;
        int dstCount3 = 0;
        String stdName2 = null;
        long dstUntilTime = DEF_TZSTARTTIME;
        long dstStartTime = DEF_TZSTARTTIME;
        while (true) {
            boolean hasTransitions3 = hasTransitions2;
            stdToOffset = stdToOffset2;
            TimeZoneTransition tzt = basicTimeZone.getNextTransition(t, false);
            if (tzt == null) {
                dstToOffset = dstToOffset2;
                dstFromOffset = dstFromOffset2;
                stdFromOffset = stdFromOffset2;
                dstName = dstName2;
                stdName = stdName2;
                dstCount = dstCount3;
                finalDstRule = finalDstRule2;
                finalStdRule = finalStdRule2;
                dstToOffset2 = stdMonth;
                hasTransitions = hasTransitions3;
                z = false;
                dstWeekInMonth = dstWeekInMonth2;
                dstMonth = dstMonth2;
                dstFromOffset2 = dstDayOfWeek2;
                dstWeekInMonth2 = stdCount;
                break;
            }
            hasTransitions = true;
            long t2 = tzt.getTime();
            String name = tzt.getTo().getName();
            boolean isDst = tzt.getTo().getDSTSavings() != 0;
            int fromOffset = tzt.getFrom().getRawOffset() + tzt.getFrom().getDSTSavings();
            int fromDSTSavings = tzt.getFrom().getDSTSavings();
            int stdFromOffset3 = stdFromOffset2;
            stdFromOffset2 = tzt.getTo().getDSTSavings() + tzt.getTo().getRawOffset();
            String stdName3 = stdName2;
            Grego.timeToFields(tzt.getTime() + ((long) fromOffset), dtfields2);
            dstCount = Grego.getDayOfWeekInMonth(dtfields2[0], dtfields2[1], dtfields2[2]);
            z = false;
            dstWeekInMonth = dtfields2[0];
            boolean sameRule = false;
            int fromOffset2;
            String name2;
            int dstMillisInDay2;
            int dstWeekInMonth3;
            int dstMonth3;
            int toOffset;
            int year;
            int stdToOffset3;
            if (!isDst) {
                int stdMonth2;
                int toOffset2;
                String name3;
                int i2;
                fromOffset2 = fromOffset;
                name2 = name;
                dstMillisInDay2 = dstMillisInDay;
                dstWeekInMonth3 = dstWeekInMonth2;
                dstMonth3 = dstMonth2;
                dstDayOfWeek = dstDayOfWeek2;
                dstToOffset = dstToOffset2;
                dstFromOffset = dstFromOffset2;
                toOffset = stdFromOffset2;
                year = dstWeekInMonth;
                stdFromOffset = stdFromOffset3;
                stdName = stdName3;
                dstName = dstName2;
                if (finalStdRule2 == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                    finalStdRule2 = (AnnualTimeZoneRule) tzt.getTo();
                }
                if (stdCount > 0) {
                    String stdName4;
                    dstMonth = year;
                    if (dstMonth == stdStartYear + stdCount) {
                        stdName2 = name2;
                        stdName4 = stdName;
                        if (stdName2.equals(stdName4)) {
                            stdFromOffset2 = fromOffset2;
                            dstToOffset2 = stdFromOffset;
                            if (dstToOffset2 == stdFromOffset2) {
                                dstDayOfWeek2 = stdToOffset;
                                dstMonth2 = toOffset;
                                if (dstDayOfWeek2 == dstMonth2) {
                                    dstWeekInMonth2 = stdMonth;
                                    if (dstWeekInMonth2 == dtfields2[1]) {
                                        dstMillisInDay = stdDayOfWeek;
                                        if (dstMillisInDay == dtfields2[3]) {
                                            stdWeekInMonth = stdWeekInMonth2;
                                            if (stdWeekInMonth == dstCount) {
                                                fromOffset = stdMillisInDay;
                                                if (fromOffset == dtfields2[5]) {
                                                    stdUntilTime = t2;
                                                    stdCount++;
                                                    sameRule = true;
                                                }
                                            } else {
                                                fromOffset = stdMillisInDay;
                                            }
                                        } else {
                                            stdWeekInMonth = stdWeekInMonth2;
                                            fromOffset = stdMillisInDay;
                                        }
                                    } else {
                                        dstMillisInDay = stdDayOfWeek;
                                        stdWeekInMonth = stdWeekInMonth2;
                                        fromOffset = stdMillisInDay;
                                    }
                                } else {
                                    dstWeekInMonth2 = stdMonth;
                                    dstMillisInDay = stdDayOfWeek;
                                    stdWeekInMonth = stdWeekInMonth2;
                                    fromOffset = stdMillisInDay;
                                }
                            } else {
                                dstWeekInMonth2 = stdMonth;
                                dstMillisInDay = stdDayOfWeek;
                                stdWeekInMonth = stdWeekInMonth2;
                                fromOffset = stdMillisInDay;
                                dstDayOfWeek2 = stdToOffset;
                                dstMonth2 = toOffset;
                            }
                        } else {
                            dstWeekInMonth2 = stdMonth;
                            dstMillisInDay = stdDayOfWeek;
                            stdWeekInMonth = stdWeekInMonth2;
                            fromOffset = stdMillisInDay;
                            dstDayOfWeek2 = stdToOffset;
                            stdFromOffset2 = fromOffset2;
                            dstMonth2 = toOffset;
                            dstToOffset2 = stdFromOffset;
                        }
                    } else {
                        dstWeekInMonth2 = stdMonth;
                        dstMillisInDay = stdDayOfWeek;
                        stdWeekInMonth = stdWeekInMonth2;
                        fromOffset = stdMillisInDay;
                        dstDayOfWeek2 = stdToOffset;
                        stdFromOffset2 = fromOffset2;
                        stdName2 = name2;
                        dstMonth2 = toOffset;
                        dstToOffset2 = stdFromOffset;
                        stdName4 = stdName;
                    }
                    i = stdCount;
                    if (sameRule) {
                        stdDayOfWeek = i;
                        stdWeekInMonth2 = fromOffset;
                        stdMillisInDay = stdWeekInMonth;
                        stdToOffset = dstMillisInDay;
                        stdMonth2 = dstWeekInMonth2;
                        toOffset2 = dstMonth2;
                        stdToOffset3 = dstDayOfWeek2;
                        fromOffset2 = dstToOffset2;
                        name2 = stdName4;
                        stdMonth = stdFromOffset2;
                        stdFromOffset = dstMonth;
                        name3 = stdName2;
                    } else if (i == 1) {
                        stdDayOfWeek = i;
                        stdWeekInMonth2 = fromOffset;
                        stdMillisInDay = stdWeekInMonth;
                        stdToOffset = dstMillisInDay;
                        stdMonth2 = dstWeekInMonth2;
                        toOffset2 = dstMonth2;
                        stdToOffset3 = dstDayOfWeek2;
                        fromOffset2 = dstToOffset2;
                        writeZonePropsByTime(w, false, stdName4, dstToOffset2, dstDayOfWeek2, stdStartTime, 1);
                        name2 = stdName4;
                        stdMonth = stdFromOffset2;
                        stdFromOffset = dstMonth;
                        name3 = stdName2;
                    } else {
                        stdDayOfWeek = i;
                        stdWeekInMonth2 = fromOffset;
                        stdMillisInDay = stdWeekInMonth;
                        stdToOffset = dstMillisInDay;
                        stdMonth2 = dstWeekInMonth2;
                        toOffset2 = dstMonth2;
                        stdToOffset3 = dstDayOfWeek2;
                        fromOffset2 = dstToOffset2;
                        name2 = stdName4;
                        stdMonth = stdFromOffset2;
                        stdFromOffset = dstMonth;
                        name3 = stdName2;
                        writeZonePropsByDOW(w, false, stdName4, fromOffset2, stdToOffset3, stdMonth2, stdMillisInDay, stdToOffset, stdStartTime, stdUntilTime);
                    }
                    stdCount = stdDayOfWeek;
                } else {
                    stdMonth2 = stdMonth;
                    stdToOffset3 = stdToOffset;
                    stdMonth = fromOffset2;
                    toOffset2 = toOffset;
                    fromOffset2 = stdFromOffset;
                    stdFromOffset = year;
                    stdToOffset = stdDayOfWeek;
                    name3 = name2;
                    name2 = stdName;
                    i2 = stdMillisInDay;
                    stdMillisInDay = stdWeekInMonth2;
                    stdWeekInMonth2 = i2;
                }
                if (sameRule) {
                    stdDayOfWeek = stdToOffset;
                    stdWeekInMonth = stdToOffset3;
                    stdFromOffset2 = fromOffset2;
                    stdName2 = name2;
                    i2 = stdMillisInDay;
                    stdMillisInDay = stdWeekInMonth2;
                    stdWeekInMonth2 = i2;
                } else {
                    fromOffset = stdMonth;
                    stdFromDSTSavings = fromDSTSavings;
                    stdWeekInMonth = toOffset2;
                    stdUntilTime = t2;
                    stdName2 = name3;
                    stdStartYear = stdFromOffset;
                    stdMonth2 = dtfields2[1];
                    stdDayOfWeek = dtfields2[3];
                    stdWeekInMonth2 = dstCount;
                    stdMillisInDay = dtfields2[5];
                    stdStartTime = t2;
                    stdCount = 1;
                    stdFromOffset2 = fromOffset;
                }
                if (finalStdRule2 != null && finalDstRule2 != null) {
                    stdToOffset = stdWeekInMonth;
                    stdFromOffset = stdFromOffset2;
                    stdName = stdName2;
                    dstCount = dstCount3;
                    finalDstRule = finalDstRule2;
                    dstWeekInMonth2 = stdCount;
                    finalStdRule = finalStdRule2;
                    dstMonth2 = stdDayOfWeek;
                    dstDayOfWeek2 = stdWeekInMonth2;
                    dstToOffset2 = stdMonth2;
                    dstWeekInMonth = dstWeekInMonth3;
                    dstMonth = dstMonth3;
                    dstFromOffset2 = dstDayOfWeek;
                    break;
                }
                dtfields = dtfields2;
                stdToOffset2 = stdWeekInMonth;
                dstFromOffset2 = dstFromOffset;
                dstName2 = dstName;
                dstMillisInDay = dstMillisInDay2;
                stdMonth = stdMonth2;
                dstWeekInMonth2 = dstWeekInMonth3;
                dstMonth2 = dstMonth3;
                dstDayOfWeek2 = dstDayOfWeek;
                dstToOffset2 = dstToOffset;
            } else {
                if (finalDstRule2 == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                    finalDstRule2 = (AnnualTimeZoneRule) tzt.getTo();
                }
                if (dstCount3 > 0) {
                    if (dstWeekInMonth == dstStartYear + dstCount3 && name.equals(dstName2) && dstFromOffset2 == fromOffset && dstToOffset2 == stdFromOffset2 && dstMonth2 == dtfields2[1] && dstDayOfWeek2 == dtfields2[3] && dstWeekInMonth2 == dstCount && dstMillisInDay == dtfields2[5]) {
                        dstUntilTime = t2;
                        dstCount3++;
                        sameRule = true;
                    }
                    dstCount2 = dstCount3;
                    if (sameRule) {
                        fromOffset2 = fromOffset;
                        name2 = name;
                        dstMillisInDay2 = dstMillisInDay;
                        dstWeekInMonth3 = dstWeekInMonth2;
                        dstMonth3 = dstMonth2;
                        dstDayOfWeek = dstDayOfWeek2;
                        dstToOffset = dstToOffset2;
                        dstFromOffset = dstFromOffset2;
                        toOffset = stdFromOffset2;
                        year = dstWeekInMonth;
                        stdToOffset3 = dstCount2;
                        stdFromOffset = stdFromOffset3;
                        stdName = stdName3;
                        dstCount2 = 1;
                        dstName = dstName2;
                    } else if (dstCount2 == 1) {
                        stdToOffset3 = dstCount2;
                        dstCount2 = 1;
                        fromOffset2 = fromOffset;
                        name2 = name;
                        dstMillisInDay2 = dstMillisInDay;
                        dstWeekInMonth3 = dstWeekInMonth2;
                        dstMonth3 = dstMonth2;
                        dstDayOfWeek = dstDayOfWeek2;
                        dstToOffset = dstToOffset2;
                        writeZonePropsByTime(w, 1, dstName2, dstFromOffset2, dstToOffset2, dstStartTime, 1);
                        dstFromOffset = dstFromOffset2;
                        toOffset = stdFromOffset2;
                        year = dstWeekInMonth;
                        stdFromOffset = stdFromOffset3;
                        stdName = stdName3;
                        dstName = dstName2;
                    } else {
                        fromOffset2 = fromOffset;
                        name2 = name;
                        dstMillisInDay2 = dstMillisInDay;
                        dstWeekInMonth3 = dstWeekInMonth2;
                        dstMonth3 = dstMonth2;
                        dstDayOfWeek = dstDayOfWeek2;
                        dstToOffset = dstToOffset2;
                        stdToOffset3 = dstCount2;
                        dstCount2 = 1;
                        dstFromOffset = dstFromOffset2;
                        toOffset = stdFromOffset2;
                        stdFromOffset = stdFromOffset3;
                        dstName = dstName2;
                        year = dstWeekInMonth;
                        stdName = stdName3;
                        writeZonePropsByDOW(w, 1, dstName2, dstFromOffset2, dstToOffset, dstMonth2, dstWeekInMonth3, dstDayOfWeek, dstStartTime, dstUntilTime);
                    }
                    dstCount3 = stdToOffset3;
                } else {
                    fromOffset2 = fromOffset;
                    name2 = name;
                    dstMillisInDay2 = dstMillisInDay;
                    dstWeekInMonth3 = dstWeekInMonth2;
                    dstMonth3 = dstMonth2;
                    dstDayOfWeek = dstDayOfWeek2;
                    dstToOffset = dstToOffset2;
                    dstFromOffset = dstFromOffset2;
                    toOffset = stdFromOffset2;
                    year = dstWeekInMonth;
                    stdFromOffset = stdFromOffset3;
                    stdName = stdName3;
                    dstCount2 = 1;
                    dstName = dstName2;
                }
                if (sameRule) {
                    dstFromOffset2 = dstFromOffset;
                    dstName2 = dstName;
                    dstMillisInDay = dstMillisInDay2;
                    dstWeekInMonth2 = dstWeekInMonth3;
                    dstMonth2 = dstMonth3;
                    dstDayOfWeek2 = dstDayOfWeek;
                    dstToOffset2 = dstToOffset;
                } else {
                    dstName2 = name2;
                    dstFromOffset2 = fromOffset2;
                    dstFromDSTSavings = fromDSTSavings;
                    dstToOffset2 = toOffset;
                    i = year;
                    dstMonth2 = dtfields2[dstCount2];
                    dstDayOfWeek2 = dtfields2[3];
                    dstWeekInMonth2 = dstCount;
                    dstUntilTime = t2;
                    dstStartTime = t2;
                    dstStartYear = i;
                    dstMillisInDay = dtfields2[5];
                    dstCount3 = 1;
                }
                if (finalStdRule2 != null && finalDstRule2 != null) {
                    dstMillisInDay2 = dstMillisInDay;
                    dstWeekInMonth = dstWeekInMonth2;
                    dstToOffset = dstToOffset2;
                    dstFromOffset = dstFromOffset2;
                    dstName = dstName2;
                    dstCount = dstCount3;
                    finalDstRule = finalDstRule2;
                    dstWeekInMonth2 = stdCount;
                    finalStdRule = finalStdRule2;
                    dstToOffset2 = stdMonth;
                    dstMonth = dstMonth2;
                    dstFromOffset2 = dstDayOfWeek2;
                    break;
                }
                dtfields = dtfields2;
                stdToOffset2 = stdToOffset;
                stdFromOffset2 = stdFromOffset;
                stdName2 = stdName;
            }
            writer = w;
            basicTimeZone = basictz;
            hasTransitions2 = true;
            t = t2;
            dtfields2 = dtfields;
        }
        int stdCount2;
        int stdDayOfWeek2;
        int stdWeekInMonth3;
        int stdMonth3;
        int i3;
        AnnualTimeZoneRule annualTimeZoneRule;
        if (hasTransitions) {
            AnnualTimeZoneRule finalStdRule3;
            Date nextStart;
            stdCount2 = dstWeekInMonth2;
            stdDayOfWeek2 = dstMonth2;
            stdWeekInMonth3 = dstDayOfWeek2;
            stdMonth3 = dstToOffset2;
            if (dstCount > 0) {
                int dstDayOfWeek3;
                if (finalDstRule != null) {
                    dstDayOfWeek3 = dstFromOffset2;
                    finalStdRule3 = finalStdRule;
                    dstCount2 = dstWeekInMonth;
                    dtfields = dtfields2;
                    dtfields2 = dstMonth;
                    if (dstCount == 1) {
                        writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, dstStartTime);
                    } else {
                        dstMonth = dstDayOfWeek3;
                        if (isEquivalentDateRule(dtfields2, dstCount2, dstMonth, finalDstRule.getRule())) {
                            dstDayOfWeek = dstMonth;
                            writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dtfields2, dstCount2, dstMonth, dstStartTime, Long.MAX_VALUE);
                        } else {
                            writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dtfields2, dstCount2, dstMonth, dstStartTime, dstUntilTime);
                            nextStart = finalDstRule.getNextStart(dstUntilTime, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, false);
                            if (nextStart != null) {
                                writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, nextStart.getTime());
                            }
                        }
                    }
                } else if (dstCount == 1) {
                    writeZonePropsByTime(w, true, dstName, dstFromOffset, dstToOffset, dstStartTime, true);
                    dstDayOfWeek = dstFromOffset2;
                    finalStdRule3 = finalStdRule;
                    dstCount2 = dstWeekInMonth;
                    dtfields = dtfields2;
                    dtfields2 = dstMonth;
                } else {
                    finalStdRule3 = finalStdRule;
                    dstDayOfWeek3 = dstFromOffset2;
                    dtfields2 = dstMonth;
                    writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstFromOffset2, dstStartTime, dstUntilTime);
                }
            } else {
                finalStdRule3 = finalStdRule;
                dstCount2 = dstWeekInMonth;
                dtfields = dtfields2;
                dtfields2 = dstMonth;
            }
            dstMonth = stdCount2;
            if (dstMonth > 0) {
                AnnualTimeZoneRule finalStdRule4 = finalStdRule3;
                AnnualTimeZoneRule finalStdRule5;
                if (finalStdRule4 != null) {
                    finalStdRule5 = finalStdRule4;
                    i3 = dstCount;
                    if (dstMonth == 1) {
                        writeFinalRule(w, false, finalStdRule5, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, stdStartTime);
                    } else {
                        AnnualTimeZoneRule finalStdRule6 = finalStdRule5;
                        dstFromOffset2 = stdDayOfWeek2;
                        stdFromOffset2 = stdWeekInMonth3;
                        stdName2 = stdMonth3;
                        if (isEquivalentDateRule(stdName2, stdFromOffset2, dstFromOffset2, finalStdRule6.getRule())) {
                            stdDayOfWeek = dstFromOffset2;
                            stdWeekInMonth2 = stdFromOffset2;
                            annualTimeZoneRule = finalStdRule6;
                            String str = stdName2;
                            writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdName2, stdFromOffset2, dstFromOffset2, stdStartTime, Long.MAX_VALUE);
                        } else {
                            annualTimeZoneRule = finalStdRule6;
                            writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdName2, stdFromOffset2, dstFromOffset2, stdStartTime, stdUntilTime);
                            nextStart = annualTimeZoneRule.getNextStart(stdUntilTime, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, false);
                            if (nextStart != null) {
                                writeFinalRule(w, false, annualTimeZoneRule, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, nextStart.getTime());
                            }
                        }
                    }
                } else if (dstMonth == 1) {
                    writeZonePropsByTime(w, false, stdName, stdFromOffset, stdToOffset, stdStartTime, true);
                    i3 = dstCount;
                    stdDayOfWeek = stdDayOfWeek2;
                    stdWeekInMonth2 = stdWeekInMonth3;
                    dstCount = dstMonth;
                } else {
                    finalStdRule5 = finalStdRule4;
                    dstCount = dstMonth;
                    writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth3, stdWeekInMonth3, stdDayOfWeek2, stdStartTime, stdUntilTime);
                }
            } else {
                stdDayOfWeek = stdDayOfWeek2;
                stdWeekInMonth2 = stdWeekInMonth3;
                stdMonth = stdMonth3;
                dstCount3 = finalStdRule3;
            }
        } else {
            stdWeekInMonth = basictz.getOffset(DEF_TZSTARTTIME);
            boolean isDst2 = stdWeekInMonth != basictz.getRawOffset() ? true : z;
            int offset = stdWeekInMonth;
            stdCount2 = dstWeekInMonth2;
            stdDayOfWeek2 = dstMonth2;
            stdWeekInMonth3 = dstDayOfWeek2;
            stdMonth3 = dstToOffset2;
            writeZonePropsByTime(w, isDst2, getDefaultTZName(basictz.getID(), isDst2), offset, offset, DEF_TZSTARTTIME - ((long) stdWeekInMonth), 0);
            dstDayOfWeek = dstFromOffset2;
            annualTimeZoneRule = finalStdRule;
            dstCount2 = dstWeekInMonth;
            i3 = dstCount;
            dtfields = dtfields2;
            dstCount = stdCount2;
            stdDayOfWeek = stdDayOfWeek2;
            stdWeekInMonth2 = stdWeekInMonth3;
            stdMonth = stdMonth3;
            dtfields2 = dstMonth;
        }
        writeFooter(w);
    }

    /* JADX WARNING: Missing block: B:39:0x007a, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isEquivalentDateRule(int month, int weekInMonth, int dayOfWeek, DateTimeRule dtrule) {
        if (month != dtrule.getRuleMonth() || dayOfWeek != dtrule.getRuleDayOfWeek() || dtrule.getTimeRuleType() != 0) {
            return false;
        }
        if (dtrule.getDateRuleType() == 1 && dtrule.getRuleWeekInMonth() == weekInMonth) {
            return true;
        }
        int ruleDOM = dtrule.getRuleDayOfMonth();
        if (dtrule.getDateRuleType() == 2) {
            if (ruleDOM % 7 == 1 && (ruleDOM + 6) / 7 == weekInMonth) {
                return true;
            }
            if (month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 6 && weekInMonth == (((MONTHLENGTH[month] - ruleDOM) + 1) / 7) * -1) {
                return true;
            }
        }
        if (dtrule.getDateRuleType() == 3) {
            if (ruleDOM % 7 == 0 && ruleDOM / 7 == weekInMonth) {
                return true;
            }
            if (month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 0 && weekInMonth == -1 * (((MONTHLENGTH[month] - ruleDOM) / 7) + 1)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static void writeZonePropsByTime(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long time, boolean withRDATE) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, time);
        if (withRDATE) {
            writer.write(ICAL_RDATE);
            writer.write(COLON);
            writer.write(getDateTimeString(((long) fromOffset) + time));
            writer.write(NEWLINE);
        }
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, long startTime, long untilTime) throws IOException {
        Writer writer2 = writer;
        beginZoneProps(writer2, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer2, month);
        writer2.write(ICAL_BYMONTHDAY);
        writer2.write(EQUALS_SIGN);
        writer2.write(Integer.toString(dayOfMonth));
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer2, getDateTimeString(untilTime + ((long) fromOffset)));
        } else {
            int i = fromOffset;
        }
        writer2.write(NEWLINE);
        endZoneProps(writer2, isDst);
    }

    private static void writeZonePropsByDOW(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int weekInMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        Writer writer2 = writer;
        beginZoneProps(writer2, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer2, month);
        writer2.write(ICAL_BYDAY);
        writer2.write(EQUALS_SIGN);
        writer2.write(Integer.toString(weekInMonth));
        writer2.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer2, getDateTimeString(untilTime + ((long) fromOffset)));
        } else {
            int i = fromOffset;
        }
        writer2.write(NEWLINE);
        endZoneProps(writer2, isDst);
    }

    private static void writeZonePropsByDOW_GEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        int i = month;
        if (dayOfMonth % 7 == 1) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, i, (dayOfMonth + 6) / 7, dayOfWeek, startTime, untilTime);
        } else if (i == 1 || (MONTHLENGTH[i] - dayOfMonth) % 7 != 6) {
            beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
            int startDay = dayOfMonth;
            int currentMonthDays = 7;
            int i2 = 11;
            int prevMonthDays;
            if (dayOfMonth <= 0) {
                prevMonthDays = 1 - dayOfMonth;
                currentMonthDays = 7 - prevMonthDays;
                if (i - 1 >= 0) {
                    i2 = i - 1;
                }
                writeZonePropsByDOW_GEQ_DOM_sub(writer, i2, -prevMonthDays, dayOfWeek, prevMonthDays, -1, fromOffset);
                startDay = 1;
            } else if (dayOfMonth + 6 > MONTHLENGTH[i]) {
                prevMonthDays = (dayOfMonth + 6) - MONTHLENGTH[i];
                currentMonthDays = 7 - prevMonthDays;
                writeZonePropsByDOW_GEQ_DOM_sub(writer, i + 1 > 11 ? 0 : i + 1, 1, dayOfWeek, prevMonthDays, MAX_TIME, fromOffset);
            }
            writeZonePropsByDOW_GEQ_DOM_sub(writer, i, startDay, dayOfWeek, currentMonthDays, untilTime, fromOffset);
            endZoneProps(writer, isDst);
        } else {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, i, -1 * (((MONTHLENGTH[i] - dayOfMonth) + 1) / 7), dayOfWeek, startTime, untilTime);
        }
    }

    private static void writeZonePropsByDOW_GEQ_DOM_sub(Writer writer, int month, int dayOfMonth, int dayOfWeek, int numDays, long untilTime, int fromOffset) throws IOException {
        int startDayNum = dayOfMonth;
        int i = 1;
        boolean isFeb = month == 1;
        if (dayOfMonth < 0 && !isFeb) {
            startDayNum = (MONTHLENGTH[month] + dayOfMonth) + 1;
        }
        beginRRULE(writer, month);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(startDayNum));
        while (i < numDays) {
            writer.write(COMMA);
            writer.write(Integer.toString(startDayNum + i));
            i++;
        }
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
    }

    private static void writeZonePropsByDOW_LEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        int i = month;
        int i2 = dayOfMonth;
        if (i2 % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, i, i2 / 7, dayOfWeek, startTime, untilTime);
        } else if (i != 1 && (MONTHLENGTH[i] - i2) % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, i, -1 * (((MONTHLENGTH[i] - i2) / 7) + 1), dayOfWeek, startTime, untilTime);
        } else if (i == 1 && i2 == 29) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, 1, -1, dayOfWeek, startTime, untilTime);
        } else {
            writeZonePropsByDOW_GEQ_DOM(writer, isDst, tzname, fromOffset, toOffset, i, i2 - 6, dayOfWeek, startTime, untilTime);
        }
    }

    private static void writeFinalRule(Writer writer, boolean isDst, AnnualTimeZoneRule rule, int fromRawOffset, int fromDSTSavings, long startTime) throws IOException {
        long startTime2;
        int i = fromRawOffset;
        int i2 = fromDSTSavings;
        DateTimeRule dtrule = toWallTimeRule(rule.getRule(), i, i2);
        int timeInDay = dtrule.getRuleMillisInDay();
        if (timeInDay < 0) {
            startTime2 = startTime + ((long) (0 - timeInDay));
        } else if (timeInDay >= Grego.MILLIS_PER_DAY) {
            startTime2 = startTime - ((long) (timeInDay - 86399999));
        } else {
            startTime2 = startTime;
        }
        int toOffset = rule.getRawOffset() + rule.getDSTSavings();
        switch (dtrule.getDateRuleType()) {
            case 0:
                writeZonePropsByDOM(writer, isDst, rule.getName(), i + i2, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), startTime2, MAX_TIME);
                return;
            case 1:
                writeZonePropsByDOW(writer, isDst, rule.getName(), i + i2, toOffset, dtrule.getRuleMonth(), dtrule.getRuleWeekInMonth(), dtrule.getRuleDayOfWeek(), startTime2, MAX_TIME);
                return;
            case 2:
                writeZonePropsByDOW_GEQ_DOM(writer, isDst, rule.getName(), i + i2, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime2, MAX_TIME);
                return;
            case 3:
                writeZonePropsByDOW_LEQ_DOM(writer, isDst, rule.getName(), i + i2, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime2, MAX_TIME);
                return;
            default:
                return;
        }
    }

    private static DateTimeRule toWallTimeRule(DateTimeRule rule, int rawOffset, int dstSavings) {
        if (rule.getTimeRuleType() == 0) {
            return rule;
        }
        DateTimeRule modifiedRule;
        int wallt = rule.getRuleMillisInDay();
        if (rule.getTimeRuleType() == 2) {
            wallt += rawOffset + dstSavings;
        } else if (rule.getTimeRuleType() == 1) {
            wallt += dstSavings;
        }
        int dshift = 0;
        if (wallt < 0) {
            dshift = -1;
            wallt += Grego.MILLIS_PER_DAY;
        } else if (wallt >= Grego.MILLIS_PER_DAY) {
            dshift = 1;
            wallt -= Grego.MILLIS_PER_DAY;
        }
        int month = rule.getRuleMonth();
        int dom = rule.getRuleDayOfMonth();
        int dow = rule.getRuleDayOfWeek();
        int dtype = rule.getDateRuleType();
        if (dshift != 0) {
            int wim;
            if (dtype == 1) {
                wim = rule.getRuleWeekInMonth();
                if (wim > 0) {
                    dtype = 2;
                    dom = ((wim - 1) * 7) + 1;
                } else {
                    dtype = 3;
                    dom = MONTHLENGTH[month] + ((wim + 1) * 7);
                }
            }
            dom += dshift;
            wim = 11;
            if (dom == 0) {
                month--;
                if (month >= 0) {
                    wim = month;
                }
                month = wim;
                dom = MONTHLENGTH[month];
            } else if (dom > MONTHLENGTH[month]) {
                month++;
                month = month > 11 ? 0 : month;
                dom = 1;
            }
            if (dtype != 0) {
                dow += dshift;
                if (dow < 1) {
                    dow = 7;
                } else if (dow > 7) {
                    dow = 1;
                }
            }
        }
        if (dtype == 0) {
            modifiedRule = new DateTimeRule(month, dom, wallt, 0);
        } else {
            DateTimeRule dateTimeRule = new DateTimeRule(month, dom, dow, dtype == 2, wallt, 0);
        }
        return modifiedRule;
    }

    private static void beginZoneProps(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long startTime) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETTO);
        writer.write(COLON);
        writer.write(millisToOffset(toOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETFROM);
        writer.write(COLON);
        writer.write(millisToOffset(fromOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZNAME);
        writer.write(COLON);
        writer.write(tzname);
        writer.write(NEWLINE);
        writer.write(ICAL_DTSTART);
        writer.write(COLON);
        writer.write(getDateTimeString(((long) fromOffset) + startTime));
        writer.write(NEWLINE);
    }

    private static void endZoneProps(Writer writer, boolean isDst) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
    }

    private static void beginRRULE(Writer writer, int month) throws IOException {
        writer.write(ICAL_RRULE);
        writer.write(COLON);
        writer.write(ICAL_FREQ);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_YEARLY);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTH);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(month + 1));
        writer.write(SEMICOLON);
    }

    private static void appendUNTIL(Writer writer, String until) throws IOException {
        if (until != null) {
            writer.write(SEMICOLON);
            writer.write(ICAL_UNTIL);
            writer.write(EQUALS_SIGN);
            writer.write(until);
        }
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
        writer.write(ICAL_TZID);
        writer.write(COLON);
        writer.write(this.tz.getID());
        writer.write(NEWLINE);
        if (this.tzurl != null) {
            writer.write(ICAL_TZURL);
            writer.write(COLON);
            writer.write(this.tzurl);
            writer.write(NEWLINE);
        }
        if (this.lastmod != null) {
            writer.write(ICAL_LASTMOD);
            writer.write(COLON);
            writer.write(getUTCDateTimeString(this.lastmod.getTime()));
            writer.write(NEWLINE);
        }
    }

    private static void writeFooter(Writer writer) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
    }

    private static String getDateTimeString(long time) {
        int[] fields = Grego.timeToFields(time, null);
        StringBuilder sb = new StringBuilder(15);
        sb.append(numToString(fields[0], 4));
        sb.append(numToString(fields[1] + 1, 2));
        sb.append(numToString(fields[2], 2));
        sb.append('T');
        int t = fields[5];
        int hour = t / 3600000;
        t %= 3600000;
        int min = t / 60000;
        int sec = (t % 60000) / 1000;
        sb.append(numToString(hour, 2));
        sb.append(numToString(min, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String getUTCDateTimeString(long time) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getDateTimeString(time));
        stringBuilder.append("Z");
        return stringBuilder.toString();
    }

    private static long parseDateTimeString(String str, int offset) {
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int min = 0;
        int sec = 0;
        boolean isUTC = false;
        boolean isValid = false;
        if (str != null) {
            int length = str.length();
            if ((length == 15 || length == 16) && str.charAt(8) == 'T') {
                if (length == 16) {
                    if (str.charAt(15) == 'Z') {
                        isUTC = true;
                    }
                }
                try {
                    year = Integer.parseInt(str.substring(0, 4));
                    month = Integer.parseInt(str.substring(4, 6)) - 1;
                    day = Integer.parseInt(str.substring(6, 8));
                    hour = Integer.parseInt(str.substring(9, 11));
                    min = Integer.parseInt(str.substring(11, 13));
                    sec = Integer.parseInt(str.substring(13, 15));
                    int maxDayOfMonth = Grego.monthLength(year, month);
                    if (year >= 0 && month >= 0 && month <= 11 && day >= 1 && day <= maxDayOfMonth && hour >= 0 && hour < 24 && min >= 0 && min < 60 && sec >= 0 && sec < 60) {
                        isValid = true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (isValid) {
            long time = (Grego.fieldsToDay(year, month, day) * 86400000) + ((long) (((3600000 * hour) + (60000 * min)) + (sec * 1000)));
            if (isUTC) {
                return time;
            }
            return time - ((long) offset);
        }
        throw new IllegalArgumentException("Invalid date time string format");
    }

    private static int offsetStrToMillis(String str) {
        boolean isValid = false;
        int sign = 0;
        int hour = 0;
        int min = 0;
        int sec = 0;
        if (str != null) {
            int length = str.length();
            if (length == 5 || length == 7) {
                char s = str.charAt(0);
                if (s == '+') {
                    sign = 1;
                } else if (s == '-') {
                    sign = -1;
                }
                try {
                    hour = Integer.parseInt(str.substring(1, 3));
                    min = Integer.parseInt(str.substring(3, 5));
                    if (length == 7) {
                        sec = Integer.parseInt(str.substring(5, 7));
                    }
                    isValid = true;
                } catch (NumberFormatException e) {
                }
            }
        }
        if (isValid) {
            return (((((hour * 60) + min) * 60) + sec) * sign) * 1000;
        }
        throw new IllegalArgumentException("Bad offset string");
    }

    private static String millisToOffset(int millis) {
        StringBuilder sb = new StringBuilder(7);
        if (millis >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
            millis = -millis;
        }
        int t = millis / 1000;
        int sec = t % 60;
        int t2 = (t - sec) / 60;
        t = t2 % 60;
        sb.append(numToString(t2 / 60, 2));
        sb.append(numToString(t, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String numToString(int num, int width) {
        String str = Integer.toString(num);
        int len = str.length();
        if (len >= width) {
            return str.substring(len - width, len);
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = len; i < width; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        VTimeZone vtz = (VTimeZone) super.cloneAsThawed();
        vtz.tz = (BasicTimeZone) this.tz.cloneAsThawed();
        vtz.isFrozen = false;
        return vtz;
    }
}
