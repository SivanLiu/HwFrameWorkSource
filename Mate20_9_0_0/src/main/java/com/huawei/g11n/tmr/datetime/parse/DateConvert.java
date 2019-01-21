package com.huawei.g11n.tmr.datetime.parse;

import com.huawei.g11n.tmr.Filter;
import com.huawei.g11n.tmr.Match;
import com.huawei.g11n.tmr.datetime.utils.DatePeriod;
import com.huawei.g11n.tmr.datetime.utils.LocaleParam;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateConvert {
    private String locale;

    public DateConvert(String locale) {
        this.locale = locale;
    }

    public DatePeriod filterByParse(String content, List<Match> ms, String ps, String dts) {
        if (ms == null || ms.isEmpty()) {
            return null;
        }
        DatePeriod result;
        if (ms.size() == 1) {
            result = ((Match) ms.get(0)).getDp();
        } else {
            result = convertMutilMatch(content, ms, ps, dts).getDp();
        }
        return result;
    }

    private int nestDealDate(String content, Match curren, List<Match> list, int pptype) {
        String str = content;
        List<Match> list2 = list;
        int result = 0;
        boolean z = false;
        Match next = (Match) list2.get(0);
        int type = Filter.getType(next.getRegex());
        if (type != 1 && type != 5 && type != 6) {
            return 0;
        }
        if (type == Filter.getType(curren.getRegex())) {
            int i = pptype;
        } else if (type != pptype) {
            boolean isThree = false;
            String ss = str.substring(curren.getEnd(), next.getBegin());
            if (LocaleParam.isRelDates(ss, this.locale) || ss.trim().equals("(")) {
                if (list.size() > 1 && nestDealDate(str, next, list2.subList(1, list.size()), curren.getType()) == 1) {
                    isThree = true;
                }
                boolean flag15 = false;
                if (ss.trim().equals("(")) {
                    Matcher bcm = Pattern.compile("\\s*\\((.*?)\\s*\\)").matcher(str.substring(curren.getEnd()));
                    String d = null;
                    if (bcm.lookingAt()) {
                        d = bcm.group(1);
                    }
                    int end = isThree ? ((Match) list2.get(1)).getEnd() : next.getEnd();
                    if (d != null) {
                        z = d.trim().equals(str.substring(next.getBegin(), end).trim());
                    }
                    flag15 = z;
                }
                if (LocaleParam.isRelDates(ss, this.locale) || flag15) {
                    result = isThree ? 2 : 1;
                }
            }
            return result;
        }
        return 0;
    }

    private List<Match> filterDate(String content, List<Match> ms) {
        String str = content;
        List list = ms;
        List<Match> result = new ArrayList();
        int i = 0;
        while (i < ms.size()) {
            Match m = (Match) list.get(i);
            int type = Filter.getType(m.getRegex());
            if (type == 1 || type == 5 || type == 6 || type == 7) {
                int hasNum = (ms.size() - 1) - i;
                List<Match> sub = null;
                if (hasNum > 1) {
                    sub = list.subList(i + 1, i + 3);
                } else if (hasNum == 1) {
                    sub = list.subList(i + 1, i + 2);
                }
                if (hasNum == 0 || sub == null) {
                    result.add(m);
                } else {
                    int status = nestDealDate(str, m, sub, -1);
                    m.setType(1);
                    Match change = null;
                    if (status == 0) {
                        result.add(m);
                    } else {
                        int ct;
                        int nt;
                        if (status == 1) {
                            i++;
                            if (Filter.getType(m.getRegex()) > Filter.getType(((Match) list.get(i)).getRegex())) {
                                change = (Match) list.get(i);
                            }
                        } else if (status == 2) {
                            i += 2;
                            ct = Filter.getType(m.getRegex());
                            nt = Filter.getType(((Match) list.get(i - 1)).getRegex());
                            int nt2 = Filter.getType(((Match) list.get(i)).getRegex());
                            if (nt < nt2 && nt < ct) {
                                change = (Match) list.get(i - 1);
                            } else if (nt2 < nt && nt2 < ct) {
                                change = (Match) list.get(i);
                            }
                        }
                        Match e = (Match) list.get(i);
                        ct = 0;
                        nt = str.indexOf(40, m.getEnd());
                        if (nt != -1 && nt < e.getBegin()) {
                            int end = str.indexOf(41, e.getEnd());
                            if (end != -1 && str.substring(e.getEnd(), end + 1).trim().equals(")")) {
                                ct = (end - e.getEnd()) + 1;
                            }
                        }
                        m.setEnd(e.getEnd() + ct);
                        if (change != null) {
                            m.getDp().setBegin(change.getDp().getBegin());
                        }
                        result.add(m);
                    }
                }
            } else {
                result.add(m);
            }
            i++;
            str = content;
            List<Match> list2 = ms;
        }
        return result;
    }

    private List<Match> filterPeriod(String content, List<Match> ms, String ps) {
        StringBuilder stringBuilder = new StringBuilder("\\.?\\s*(-{1,2}|~|起?至|到|au|–|—|～|تا|देखि|да|па|থেকে|ຫາ");
        stringBuilder.append(ps);
        stringBuilder.append(")\\s*");
        Pattern rel = Pattern.compile(stringBuilder.toString(), 2);
        Iterator<Match> it = ms.iterator();
        Match curren = null;
        while (it.hasNext()) {
            Match m = (Match) it.next();
            if (curren == null) {
                curren = m;
            } else {
                int ctype = curren.getDp().getType();
                int ntype = m.getDp().getType();
                if ((ctype != ntype || (ctype != 1 && ctype != 2 && ctype != 0)) && (ctype != 0 || ntype != 2)) {
                    curren = m;
                } else if (rel.matcher(content.substring(curren.getEnd(), m.getBegin())).matches()) {
                    curren.setEnd(m.getEnd());
                    curren.setType(3);
                    if (ctype == 2 && ntype == 2) {
                        curren.setIsTimePeriod(true);
                    }
                    curren.getDp().setEnd(m.getDp().getBegin());
                    it.remove();
                }
            }
        }
        return ms;
    }

    private List<Match> filterDateTime(String content, List<Match> ms, String DTBridge) {
        String str = content;
        StringBuilder stringBuilder = new StringBuilder("\\s*(at|às|،‏|u|kl\\.|को|的|o|à|a\\s+les|ve|la|pada|kl|στις|alle|jam|ຂອງວັນທີ");
        stringBuilder.append(DTBridge);
        stringBuilder.append(")\\s*");
        int i = 2;
        Pattern dt = Pattern.compile(stringBuilder.toString(), 2);
        Iterator<Match> it = ms.iterator();
        Match curren = null;
        while (it.hasNext()) {
            Match m = (Match) it.next();
            if (curren == null) {
                curren = m;
            } else {
                Pattern dt2;
                String bStr;
                int ctype = curren.getDp().getType();
                int type = m.getDp().getType();
                curren.setType(ctype);
                m.setType(type);
                if ((ctype == 1 && type == i) || ((ctype == 1 && type == 5) || ((ctype == i && type == 1) || (ctype == 5 && type == 1)))) {
                    boolean flag;
                    String ss = str.substring(curren.getEnd(), m.getBegin());
                    if (ss.trim().equals("")) {
                        flag = true;
                    } else {
                        flag = dt.matcher(ss).matches();
                    }
                    if (flag) {
                        curren.setEnd(m.getEnd());
                        if (ctype == 1 && type == i) {
                            curren.getDp().getBegin().setTime(m.getDp().getBegin().getTime());
                        } else if (ctype == i && type == 1) {
                            curren.getDp().getBegin().setDay(m.getDp().getBegin().getDate());
                        } else if (ctype == 1 && type == 5) {
                            curren.getDp().getBegin().setTime(m.getDp().getBegin().getTime());
                            curren.getDp().setEnd(m.getDp().getEnd());
                        } else if (ctype == 5 && type == 1) {
                            curren.getDp().getBegin().setDay(m.getDp().getBegin().getDate());
                            curren.getDp().getEnd().setDay(m.getDp().getBegin().getDate());
                        }
                        it.remove();
                        dt2 = dt;
                        dt = i;
                    } else {
                        boolean change = true;
                        if (ctype == i && type == 1) {
                            int add = 0;
                            Matcher bcm = Pattern.compile("\\s*\\((.*?)\\s*\\)").matcher(str.substring(curren.getEnd()));
                            String d = null;
                            if (bcm.lookingAt()) {
                                d = bcm.group(1);
                                add = bcm.group().length();
                            }
                            String d2 = d;
                            if (d2 != null) {
                                dt2 = dt;
                                dt = d2.trim().equals(str.substring(m.getBegin(), m.getEnd()).trim());
                            } else {
                                dt2 = dt;
                                Matcher matcher = bcm;
                                dt = null;
                            }
                            if (dt != null) {
                                curren.setEnd(curren.getEnd() + add);
                                curren.getDp().getBegin().setDay(m.getDp().getBegin().getDate());
                                it.remove();
                                change = false;
                            }
                        } else {
                            dt2 = dt;
                        }
                        if (ctype == 1) {
                            dt = 2;
                            if (type == 2) {
                                bStr = str.substring(0, curren.getBegin());
                                String eStr = str.substring(curren.getEnd(), m.getBegin());
                                if (bStr.trim().endsWith("(") && eStr.trim().startsWith(")")) {
                                    curren.setBegin(bStr.lastIndexOf(40));
                                    curren.setEnd(m.getEnd());
                                    curren.getDp().getBegin().setTime(m.getDp().getBegin().getTime());
                                    it.remove();
                                    change = false;
                                }
                            }
                        } else {
                            dt = 2;
                        }
                        if (change) {
                            curren = m;
                        }
                    }
                } else {
                    dt2 = dt;
                    dt = i;
                    curren = m;
                }
                i = dt;
                dt = dt2;
                bStr = DTBridge;
            }
        }
        return ms;
    }

    private Match filterDateTimePunc(String content, List<Match> ms) {
        Iterator<Match> it = ms.iterator();
        Match curren = null;
        while (it.hasNext()) {
            Match m = (Match) it.next();
            if (curren == null) {
                curren = m;
            } else {
                int ctype = curren.getType();
                int type = m.getType();
                boolean flag = false;
                if ((ctype == 1 && type == 2) || ((ctype == 1 && type == 3 && m.isTimePeriod()) || ((ctype == 2 && type == 1) || (ctype == 3 && curren.isTimePeriod() && type == 1)))) {
                    String ss = content.substring(curren.getEnd(), m.getBegin());
                    if (ss.trim().equals("，") || ss.trim().equals(",")) {
                        flag = true;
                    }
                    if (flag) {
                        curren.setEnd(m.getEnd());
                        if (ctype == 1 && type == 2) {
                            curren.setType(0);
                            curren.getDp().getBegin().setTime(m.getDp().getBegin().getTime());
                        } else if (ctype == 2 && type == 1) {
                            curren.setType(0);
                            curren.getDp().getBegin().setDay(m.getDp().getBegin().getDate());
                        } else {
                            curren.setType(3);
                            curren.getDp().getBegin().setTime(m.getDp().getBegin().getTime());
                            curren.getDp().setEnd(m.getDp().getEnd());
                        }
                        it.remove();
                    } else {
                        curren = m;
                    }
                } else {
                    curren = m;
                }
            }
        }
        return curren;
    }

    private Match convertMutilMatch(String content, List<Match> ms, String ps, String dts) {
        return filterDateTimePunc(content, filterPeriod(content, filterDateTime(content, filterDate(content, ms), dts), ps));
    }

    /* JADX WARNING: Removed duplicated region for block: B:274:0x05f0  */
    /* JADX WARNING: Removed duplicated region for block: B:273:0x05ee  */
    /* JADX WARNING: Removed duplicated region for block: B:288:0x0639  */
    /* JADX WARNING: Removed duplicated region for block: B:287:0x0634  */
    /* JADX WARNING: Removed duplicated region for block: B:292:0x0653  */
    /* JADX WARNING: Removed duplicated region for block: B:291:0x064e  */
    /* JADX WARNING: Removed duplicated region for block: B:296:0x0669  */
    /* JADX WARNING: Removed duplicated region for block: B:295:0x0664  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x0690  */
    /* JADX WARNING: Removed duplicated region for block: B:299:0x0676  */
    /* JADX WARNING: Removed duplicated region for block: B:303:0x06a4  */
    /* JADX WARNING: Removed duplicated region for block: B:312:0x06ec  */
    /* JADX WARNING: Removed duplicated region for block: B:311:0x06e7  */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x06fc  */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x06f7  */
    /* JADX WARNING: Removed duplicated region for block: B:320:0x070c  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x0707  */
    /* JADX WARNING: Removed duplicated region for block: B:329:0x0746  */
    /* JADX WARNING: Removed duplicated region for block: B:328:0x0741  */
    /* JADX WARNING: Removed duplicated region for block: B:333:0x0760  */
    /* JADX WARNING: Removed duplicated region for block: B:332:0x075b  */
    /* JADX WARNING: Removed duplicated region for block: B:337:0x0776  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0771  */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x079d  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x0783  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x07b1  */
    /* JADX WARNING: Removed duplicated region for block: B:353:0x07e6  */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x07e1  */
    /* JADX WARNING: Removed duplicated region for block: B:357:0x07f6  */
    /* JADX WARNING: Removed duplicated region for block: B:356:0x07f1  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x0806  */
    /* JADX WARNING: Removed duplicated region for block: B:360:0x0801  */
    /* JADX WARNING: Removed duplicated region for block: B:368:0x0826  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0815  */
    /* JADX WARNING: Missing block: B:190:0x052c, code skipped:
            if (r11 == 1) goto L_0x0530;
     */
    /* JADX WARNING: Missing block: B:242:0x0585, code skipped:
            if (r10 != 2) goto L_0x0588;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<Date> convert(DatePeriod dp, long rTime) {
        long j = rTime;
        List<Date> result = new ArrayList();
        if (dp == null) {
            return null;
        }
        List<Date> result2;
        int status = dp.getType();
        int year;
        int hour;
        int min;
        int sec;
        String tz;
        int year2;
        int day;
        int sec2;
        String tm;
        if (status == 0) {
            StringBuffer t;
            year = dp.getBegin().getDate().getYear();
            int month = dp.getBegin().getDate().getMonth();
            int day2 = dp.getBegin().getDate().getDay();
            hour = dp.getBegin().getTime().getClock();
            min = dp.getBegin().getTime().getMinute();
            sec = dp.getBegin().getTime().getSecond();
            String tm2 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getMark() : "";
            tz = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getTimezone() : "";
            StringBuffer t2 = new StringBuffer();
            StringBuffer s = new StringBuffer();
            if (hour > 12 && !tm2.trim().equals("")) {
                tm2 = "";
            }
            StringBuffer s2 = s;
            s2.append(hour != -1 ? Integer.valueOf(hour) : "00");
            s2.append(":");
            s2.append(min != -1 ? Integer.valueOf(min) : "00");
            s2.append(":");
            s2.append(sec != -1 ? Integer.valueOf(sec) : "00");
            if (tm2.equals("")) {
                t = t2;
                t.append("HH");
                t.append(":mm:ss");
            } else {
                t = t2;
                t.append("hh");
                t.append(":mm:ss");
                t.append(" a");
                s2.append(" ");
                s2.append(tm2);
            }
            if (!tz.equals("")) {
                t.append(" Z");
                s2.append(" ");
                s2.append(tz);
            }
            SimpleDateFormat for1 = new SimpleDateFormat(t.toString(), Locale.ENGLISH);
            Date d1 = null;
            try {
                d1 = for1.parse(s2.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar c = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();
            c2.setTime(new Date(j));
            if (d1 != null) {
                c.setTime(d1);
            }
            if (year != -1) {
                c.set(1, year);
                Date date = d1;
            } else {
                c.set(1, c2.get(1));
            }
            if (month != -1) {
                c.set(2, month);
            } else {
                c.set(2, c2.get(2));
            }
            if (day2 != -1) {
                c.set(5, day2);
            } else {
                c.set(5, c2.get(5));
            }
            result.add(c.getTime());
        } else if (status == 1) {
            year2 = dp.getBegin().getDate().getYear();
            sec = dp.getBegin().getDate().getMonth();
            day = dp.getBegin().getDate().getDay();
            Calendar c3 = Calendar.getInstance();
            Calendar c22 = Calendar.getInstance();
            c22.setTime(new Date(j));
            if (year2 != -1) {
                c3.set(1, year2);
            } else {
                c3.set(1, c22.get(1));
            }
            if (sec != -1) {
                c3.set(2, sec);
            } else {
                c3.set(2, c22.get(2));
            }
            if (day != -1) {
                c3.set(5, day);
            } else {
                c3.set(5, c22.get(5));
            }
            c3.set(11, 0);
            c3.set(12, 0);
            c3.set(13, 0);
            result.add(c3.getTime());
        } else if (status == 2) {
            sec = dp.getBegin().getTime().getClock();
            day = dp.getBegin().getTime().getMinute();
            sec2 = dp.getBegin().getTime().getSecond();
            tm = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getMark() : "";
            String tz2 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getTimezone() : "";
            StringBuffer t3 = new StringBuffer();
            StringBuffer s3 = new StringBuffer();
            t3.append("yyyy-MM-dd ");
            Calendar c4 = Calendar.getInstance();
            c4.setTime(new Date(j));
            s3.append(c4.get(1));
            s3.append("-");
            s3.append(c4.get(2) + 1);
            s3.append("-");
            s3.append(c4.get(5));
            s3.append(" ");
            if (sec > 12 && !tm.trim().equals("")) {
                tm = "";
            }
            String tm3 = tm;
            s3.append(sec != -1 ? Integer.valueOf(sec) : "00");
            s3.append(":");
            s3.append(day != -1 ? Integer.valueOf(day) : "00");
            s3.append(":");
            s3.append(sec2 != -1 ? Integer.valueOf(sec2) : "00");
            if (tm3.equals("")) {
                t3.append("HH");
                t3.append(":mm:ss");
            } else {
                t3.append("hh");
                t3.append(":mm:ss");
                t3.append(" a");
                s3.append(" ");
                s3.append(tm3);
            }
            if (!tz2.equals("")) {
                t3.append(" Z");
                s3.append(" ");
                s3.append(tz2);
            }
            Date d12 = null;
            try {
                d12 = new SimpleDateFormat(t3.toString(), Locale.ENGLISH).parse(s3.toString());
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
            result.add(d12);
        } else {
            if (status > 2) {
                int hour2;
                Calendar c5;
                StringBuffer t4;
                StringBuffer s4;
                List<Date> tz22;
                int hour3;
                StringBuffer result3;
                StringBuffer t5;
                SimpleDateFormat for12;
                StringBuffer s5;
                Date d;
                Calendar tc;
                StringBuffer t22;
                StringBuffer s22;
                StringBuffer s23;
                StringBuffer t23;
                SimpleDateFormat for2;
                year2 = dp.getBegin().getDate() != null ? dp.getBegin().getDate().getYear() : -1;
                sec = dp.getBegin().getDate() != null ? dp.getBegin().getDate().getMonth() : -1;
                day = dp.getBegin().getDate() != null ? dp.getBegin().getDate().getDay() : -1;
                sec2 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getClock() : -1;
                int min2 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getMinute() : -1;
                year = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getSecond() : -1;
                String tm4 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getMark() : "";
                String tz3 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().getTimezone() : "";
                hour = dp.getEnd().getDate() != null ? dp.getEnd().getDate().getYear() : -1;
                min = dp.getEnd().getDate() != null ? dp.getEnd().getDate().getMonth() : -1;
                int day22 = dp.getEnd().getDate() != null ? dp.getEnd().getDate().getDay() : -1;
                List<Date> result4 = result;
                result = dp.getEnd().getTime() != null ? dp.getEnd().getTime().getClock() : -1;
                int status2 = status;
                int min22 = dp.getEnd().getTime() != null ? dp.getEnd().getTime().getMinute() : -1;
                int sec22 = dp.getEnd().getTime() != null ? dp.getEnd().getTime().getSecond() : -1;
                String tzone = dp.getEnd().getTime() != null ? dp.getEnd().getTime().getMark() : "";
                int hour22 = result;
                result = dp.getEnd().getTime() != null ? dp.getEnd().getTime().getTimezone() : "";
                boolean isBefore1 = dp.getBegin().getTime() != null ? dp.getBegin().getTime().isMarkBefore() : true;
                boolean isBefore2 = dp.getEnd().getTime() != null ? dp.getEnd().getTime().isMarkBefore() : true;
                int sec3 = year;
                year = dp.getBegin().getSatuts();
                int min3 = min2;
                min2 = dp.getEnd().getSatuts();
                int i = year != 0 ? 1 : 1;
                if (min2 == 0 || min2 == i) {
                    if (year2 != -1 && hour == -1) {
                        hour = year2;
                    }
                    if (year2 == -1 && hour != -1) {
                        year2 = hour;
                    }
                    if (sec != -1 && min == -1) {
                        min = sec;
                    }
                    if (sec == -1 && min != -1) {
                        sec = min;
                    }
                }
                if ((year == 0 || year == 1) && min2 == 2) {
                    if (year2 != -1 && year2 == -1) {
                        hour = year2;
                    }
                    if (sec != -1 && month2 == -1) {
                        min = sec;
                    }
                    if (day != -1 && day22 == -1) {
                        day22 = day;
                    }
                }
                if ((min2 == 0 || min2 == 1) && year == 2) {
                    if (hour != -1 && year2 == -1) {
                        year2 = hour;
                    }
                    if (min != -1 && sec == -1) {
                        sec = min;
                    }
                    if (day22 != -1 && day == -1) {
                        day = day22;
                    }
                }
                i = year2;
                tm = "";
                String tzone2;
                boolean isBefore;
                if (year != 0) {
                    hour2 = sec2;
                    if (year != 2) {
                        if (min2 != 0) {
                        }
                    }
                    tzone2 = tm;
                    isBefore = true;
                    boolean isBefore3;
                    if (!tm4.trim().equals("") && !tzone.trim().equals("")) {
                        isBefore3 = isBefore2;
                        if (!(i == hour && sec == min && day == day22)) {
                            isBefore3 = true;
                        }
                        if (!isBefore3) {
                            tm4 = tzone;
                        }
                    } else if (tzone.trim().equals("") || tm4.trim().equals("")) {
                    } else {
                        isBefore3 = isBefore1;
                        if (i == hour && sec == min && day == day22) {
                            isBefore3 = true;
                        } else {
                            isBefore3 = false;
                        }
                        if (isBefore3) {
                            tzone = tm4;
                        }
                    }
                    if (result.equals("")) {
                        Object tzone3 = result;
                    } else if (!result.equals("") || tz3.equals("")) {
                        tz = tzone;
                        tzone = tzone2;
                        c5 = Calendar.getInstance();
                        t4 = new StringBuffer();
                        s4 = new StringBuffer();
                        tz22 = result;
                        hour3 = hour2;
                        if (hour3 > 12 && !tm.trim().equals("")) {
                            tm4 = "";
                        }
                        result3 = s4;
                        result3.append(hour3 == -1 ? Integer.valueOf(hour3) : "08");
                        result3.append(":");
                        hour3 = min3;
                        result3.append(hour3 == -1 ? Integer.valueOf(hour3) : "00");
                        result3.append(":");
                        min2 = sec3;
                        result3.append(min2 == -1 ? Integer.valueOf(min2) : "00");
                        if (tm4.equals("")) {
                            t5 = t4;
                            t5.append("hh");
                            t5.append(":mm:ss");
                            t5.append(" a");
                            result3.append(" ");
                            result3.append(tm4);
                        } else {
                            t5 = t4;
                            t5.append("HH");
                            t5.append(":mm:ss");
                        }
                        if (!tzone.equals("")) {
                            t5.append(" Z");
                            result3.append(" ");
                            result3.append(tzone);
                        }
                        for12 = new SimpleDateFormat(t5.toString(), Locale.ENGLISH);
                        c5.setTime(for12.parse(result3.toString()));
                        s5 = result3;
                        d = new Date(rTime);
                        tc = Calendar.getInstance();
                        tc.setTime(d);
                        if (i == -1) {
                            c5.set(1, i);
                        } else {
                            c5.set(1, tc.get(1));
                        }
                        if (sec == -1) {
                            c5.set(2, sec);
                        } else {
                            c5.set(2, tc.get(2));
                        }
                        if (day == -1) {
                            c5.set(5, day);
                        } else {
                            c5.set(5, tc.get(5));
                        }
                        result = Calendar.getInstance();
                        t22 = new StringBuffer();
                        s22 = new StringBuffer();
                        i = hour22;
                        if (i > 12 && !tm2.trim().equals("")) {
                            tz = "";
                        }
                        s23 = s22;
                        s23.append(i == -1 ? Integer.valueOf(i) : "08");
                        s23.append(":");
                        i = min22;
                        s23.append(i == -1 ? Integer.valueOf(i) : "00");
                        s23.append(":");
                        day = sec22;
                        s23.append(day == -1 ? Integer.valueOf(day) : "00");
                        if (tz.equals("")) {
                            t23 = t22;
                            t23.append("hh");
                            t23.append(":mm:ss");
                            t23.append(" a");
                            s23.append(" ");
                            s23.append(tz);
                        } else {
                            t23 = t22;
                            t23.append("HH");
                            t23.append(":mm:ss");
                        }
                        if (!tzone.equals("")) {
                            t23.append(" Z");
                            s23.append(" ");
                            s23.append(tzone);
                        }
                        for2 = new SimpleDateFormat(t23.toString(), Locale.ENGLISH);
                        result.setTime(for2.parse(s23.toString()));
                        if (hour == -1) {
                            result.set(1, hour);
                        } else {
                            result.set(1, tc.get(1));
                        }
                        if (min == -1) {
                            result.set(2, min);
                        } else {
                            result.set(2, tc.get(2));
                        }
                        if (day22 == -1) {
                            result.set(5, day22);
                        } else {
                            result.set(5, tc.get(5));
                        }
                        SimpleDateFormat simpleDateFormat;
                        if (c5.compareTo(result) != 1) {
                            if (status2 == 5) {
                                simpleDateFormat = for2;
                                c5.add(5, -1);
                            }
                        } else {
                            simpleDateFormat = for2;
                            status = status2;
                        }
                        result2 = result4;
                        result2.add(c5.getTime());
                        result2.add(result.getTime());
                    } else {
                        tm = tz3;
                    }
                } else {
                    hour2 = sec2;
                    tzone2 = tm;
                    isBefore = true;
                    if (!tm4.trim().equals("")) {
                    }
                    if (tzone.trim().equals("")) {
                    }
                    if (result.equals("")) {
                    }
                }
                tz = tzone;
                tzone = tm;
                c5 = Calendar.getInstance();
                t4 = new StringBuffer();
                s4 = new StringBuffer();
                tz22 = result;
                hour3 = hour2;
                tm4 = "";
                if (hour3 == -1) {
                }
                result3 = s4;
                result3.append(hour3 == -1 ? Integer.valueOf(hour3) : "08");
                result3.append(":");
                hour3 = min3;
                if (hour3 == -1) {
                }
                result3.append(hour3 == -1 ? Integer.valueOf(hour3) : "00");
                result3.append(":");
                min2 = sec3;
                if (min2 == -1) {
                }
                result3.append(min2 == -1 ? Integer.valueOf(min2) : "00");
                if (tm4.equals("")) {
                }
                if (tzone.equals("")) {
                }
                for12 = new SimpleDateFormat(t5.toString(), Locale.ENGLISH);
                try {
                    c5.setTime(for12.parse(result3.toString()));
                } catch (ParseException e22) {
                    e22.printStackTrace();
                }
                s5 = result3;
                d = new Date(rTime);
                tc = Calendar.getInstance();
                tc.setTime(d);
                if (i == -1) {
                }
                if (sec == -1) {
                }
                if (day == -1) {
                }
                result = Calendar.getInstance();
                t22 = new StringBuffer();
                s22 = new StringBuffer();
                i = hour22;
                tz = "";
                if (i == -1) {
                }
                s23 = s22;
                s23.append(i == -1 ? Integer.valueOf(i) : "08");
                s23.append(":");
                i = min22;
                if (i == -1) {
                }
                s23.append(i == -1 ? Integer.valueOf(i) : "00");
                s23.append(":");
                day = sec22;
                if (day == -1) {
                }
                s23.append(day == -1 ? Integer.valueOf(day) : "00");
                if (tz.equals("")) {
                }
                if (tzone.equals("")) {
                }
                for2 = new SimpleDateFormat(t23.toString(), Locale.ENGLISH);
                try {
                    result.setTime(for2.parse(s23.toString()));
                } catch (ParseException e222) {
                    e222.printStackTrace();
                }
                if (hour == -1) {
                }
                if (min == -1) {
                }
                if (day22 == -1) {
                }
                if (c5.compareTo(result) != 1) {
                }
                result2 = result4;
                result2.add(c5.getTime());
                result2.add(result.getTime());
            } else {
                result2 = result;
            }
            return result2;
        }
        result2 = result;
        return result2;
    }
}
