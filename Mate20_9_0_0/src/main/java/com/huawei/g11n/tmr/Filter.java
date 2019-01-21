package com.huawei.g11n.tmr;

import com.huawei.g11n.tmr.datetime.detect.RuleLevel;
import com.huawei.g11n.tmr.datetime.utils.LocaleParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Filter {
    private String locale;
    private RuleLevel rlevel;

    public Filter(String locale) {
        this.locale = locale;
        this.rlevel = new RuleLevel(locale);
    }

    public List<Match> filterOverlay(List<Match> ms) {
        if (ms == null) {
            return null;
        }
        Match cp;
        List<Match> r2 = new ArrayList();
        for (int i = 0; i < ms.size(); i++) {
            Match c = (Match) ms.get(i);
            int valid = 1;
            Iterator<Match> it = r2.iterator();
            while (it.hasNext()) {
                cp = (Match) it.next();
                if (cp.getBegin() == c.getBegin() && cp.getEnd() == c.getEnd()) {
                    if (this.rlevel.compare(Integer.parseInt(cp.getRegex()), Integer.parseInt(c.getRegex())) > -1) {
                        valid = -1;
                    } else {
                        it.remove();
                    }
                } else if (cp.getBegin() >= c.getBegin() || c.getBegin() >= cp.getEnd() || cp.getEnd() >= c.getEnd()) {
                    if (c.getBegin() < cp.getBegin() && c.getEnd() > cp.getBegin() && c.getEnd() < cp.getEnd()) {
                        if (this.rlevel.compare(Integer.parseInt(cp.getRegex()), Integer.parseInt(c.getRegex())) > -1) {
                            valid = -1;
                        } else {
                            it.remove();
                        }
                    }
                } else if (this.rlevel.compare(Integer.parseInt(cp.getRegex()), Integer.parseInt(c.getRegex())) > -1) {
                    valid = -1;
                } else {
                    it.remove();
                }
            }
            if (valid == 1) {
                r2.add(c);
            }
        }
        List<Match> ms2 = r2;
        ArrayList r = new ArrayList();
        for (int i2 = 0; i2 < ms2.size(); i2++) {
            cp = (Match) ms2.get(i2);
            int valid2 = 1;
            Iterator<Match> it2 = r.iterator();
            while (it2.hasNext() != 0) {
                Match i3 = (Match) it2.next();
                if (i3.getBegin() > cp.getBegin() && i3.getEnd() < cp.getEnd()) {
                    it2.remove();
                } else if (i3.getBegin() > cp.getBegin() && i3.getEnd() == cp.getEnd()) {
                    it2.remove();
                } else if (i3.getBegin() == cp.getBegin() && i3.getEnd() < cp.getEnd()) {
                    it2.remove();
                } else if (i3.getBegin() <= cp.getBegin() && i3.getEnd() >= cp.getEnd()) {
                    valid2 = -1;
                } else if (i3.getBegin() >= cp.getBegin() || cp.getBegin() >= i3.getEnd() || i3.getEnd() >= cp.getEnd()) {
                    if (cp.getBegin() < i3.getBegin() && cp.getEnd() > i3.getBegin() && cp.getEnd() < i3.getEnd()) {
                        if (this.rlevel.compare(Integer.parseInt(i3.getRegex()), Integer.parseInt(cp.getRegex())) > -1) {
                            valid2 = -1;
                        } else {
                            it2.remove();
                        }
                    }
                } else if (this.rlevel.compare(Integer.parseInt(i3.getRegex()), Integer.parseInt(cp.getRegex())) > -1) {
                    valid2 = -1;
                } else {
                    it2.remove();
                }
            }
            if (valid2 == 1) {
                r.add(cp);
            }
        }
        order(r);
        return r;
    }

    public void order(List<Match> ms) {
        Collections.sort(ms);
    }

    private int nestDealDate(String content, Match curren, List<Match> list, int pptype) {
        String str = content;
        List<Match> list2 = list;
        int result = 0;
        boolean z = false;
        Match next = (Match) list2.get(0);
        int type = getType(next.getRegex());
        if (type != 1 && type != 5 && type != 6) {
            return 0;
        }
        if (type == curren.getType()) {
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
        List<Match> list = ms;
        List<Match> result = new ArrayList();
        int i = 0;
        while (i < ms.size()) {
            int i2;
            Match m = (Match) list.get(i);
            int type = getType(m.getRegex());
            m.setType(type);
            if (type == 1 || type == 5 || type == 6 || type == 7) {
                int hasNum = (ms.size() - 1) - i;
                List<Match> sub = null;
                if (hasNum > 1) {
                    sub = list.subList(i + 1, i + 3);
                } else if (hasNum == 1) {
                    sub = list.subList(i + 1, i + 2);
                }
                if (hasNum == 0 || sub == null) {
                    i2 = 1;
                    m.setType(1);
                    result.add(m);
                    i += i2;
                    str = content;
                } else {
                    int status = nestDealDate(str, m, sub, -1);
                    m.setType(1);
                    if (status == 0) {
                        result.add(m);
                    } else {
                        if (status == 1) {
                            i++;
                        } else if (status == 2) {
                            i += 2;
                        }
                        Match e = (Match) list.get(i);
                        int add = 0;
                        int be = str.indexOf(40, m.getEnd());
                        if (be != -1 && be < e.getBegin()) {
                            int end = str.indexOf(41, e.getEnd());
                            if (end != -1 && str.substring(e.getEnd(), end + 1).trim().equals(")")) {
                                add = (end - e.getEnd()) + 1;
                            }
                        }
                        m.setEnd(e.getEnd() + add);
                        result.add(m);
                        i2 = 1;
                        i += i2;
                        str = content;
                    }
                }
            } else {
                result.add(m);
            }
            i2 = 1;
            i += i2;
            str = content;
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
                int ctype = curren.getType();
                int ntype = m.getType();
                if ((ctype != ntype || (ctype != 1 && ctype != 2 && ctype != 0)) && (ctype != 0 || ntype != 2)) {
                    curren = m;
                } else if (rel.matcher(content.substring(curren.getEnd(), m.getBegin())).matches()) {
                    curren.setEnd(m.getEnd());
                    curren.setType(3);
                    if (ctype == 2 && ntype == 2) {
                        curren.setIsTimePeriod(true);
                    }
                    it.remove();
                } else {
                    curren = m;
                }
            }
        }
        return ms;
    }

    private List<Match> filterDateTime(String content, List<Match> ms, String dts) {
        String str = content;
        StringBuilder stringBuilder = new StringBuilder("\\s*(at|às|،‏|u|kl\\.|को|的|o|à|a\\s+les|ve|la|pada|kl|στις|alle|jam|मा|এ|ຂອງວັນທີ");
        stringBuilder.append(dts);
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
                int ctype = curren.getType();
                int type = m.getType();
                if ((ctype == 1 && type == i) || ((ctype == 1 && type == 3 && m.isTimePeriod()) || ((ctype == i && type == 1) || (ctype == 3 && curren.isTimePeriod() && type == 1)))) {
                    boolean flag;
                    String ss = str.substring(curren.getEnd(), m.getBegin());
                    if (ss.trim().equals("")) {
                        flag = true;
                    } else {
                        flag = dt.matcher(ss).matches();
                    }
                    if (flag) {
                        curren.setEnd(m.getEnd());
                        if ((ctype == 1 && type == i) || (ctype == i && type == 1)) {
                            curren.setType(0);
                        } else {
                            curren.setType(3);
                        }
                        it.remove();
                        dt2 = dt;
                        dt = i;
                    } else {
                        String bStr2;
                        boolean change = true;
                        if (ctype == i && type == 1) {
                            int add = 0;
                            bStr2 = str.substring(curren.getEnd());
                            Matcher bcm = Pattern.compile("\\s*\\((.*?)\\s*\\)").matcher(bStr2);
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
                                String str2 = bStr2;
                                dt = null;
                            }
                            if (dt != null) {
                                curren.setEnd(curren.getEnd() + add);
                                curren.setType(0);
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
                                bStr2 = str.substring(curren.getEnd(), m.getBegin());
                                if (bStr.trim().endsWith("(") && bStr2.trim().equals(")")) {
                                    curren.setBegin(bStr.lastIndexOf(40));
                                    curren.setEnd(m.getEnd());
                                    curren.setType(0);
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
                bStr = dts;
            }
        }
        return ms;
    }

    private List<Match> filterDateTimePunc(String content, List<Match> ms) {
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
                        if ((ctype == 1 && type == 2) || (ctype == 2 && type == 1)) {
                            curren.setType(0);
                        } else {
                            curren.setType(3);
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
        return ms;
    }

    private List<Match> filterDatePeriod(String content, List<Match> ms, String ps, String dts) {
        return filterDateTimePunc(content, filterPeriod(content, filterDateTime(content, filterDate(content, ms), dts), ps));
    }

    public static int getType(String name) {
        int n = Integer.parseInt(name);
        if (n <= 19999 || n >= 30000) {
            if (n > 29999 && n < 40000) {
                return 2;
            }
            if (n <= 9999 || n >= 20000) {
                return 3;
            }
            return 0;
        } else if (n == 20009 || n == 20011 || n == 21026) {
            return 6;
        } else {
            if (n == 20010) {
                return 5;
            }
            return 1;
        }
    }

    private List<Match> filterByRules(String content, List<Match> ms, RuleInit obj) {
        List<Match> clears = obj.clear(content);
        if (clears == null || clears.isEmpty()) {
            return ms;
        }
        Iterator<Match> it = ms.iterator();
        while (it.hasNext()) {
            Match m = (Match) it.next();
            for (Match t : clears) {
                if (m.getBegin() >= t.getBegin() && m.getEnd() <= t.getEnd()) {
                    it.remove();
                    break;
                }
            }
        }
        return ms;
    }

    private List<Match> filterByPast(String content, List<Match> ms, RuleInit obj) {
        List<Match> past = obj.pastFind(content);
        if (past == null || past.isEmpty()) {
            return ms;
        }
        for (Match p : past) {
            Integer name = Integer.valueOf(p.getRegex());
            Iterator<Match> it = ms.iterator();
            while (it.hasNext()) {
                Match m = (Match) it.next();
                if (name.intValue() < 200) {
                    if (p.getEnd() == m.getBegin()) {
                        it.remove();
                        break;
                    }
                } else if (p.getBegin() == m.getEnd()) {
                    it.remove();
                    break;
                }
            }
        }
        return ms;
    }

    public List<Match> filter(String content, List<Match> ms, RuleInit obj) {
        return filterByPast(content, filterByRules(content, filterDatePeriod(content, filterOverlay(ms), obj.getPeriodString(), obj.getDTBridgeString()), obj), obj);
    }
}
