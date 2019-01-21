package com.huawei.g11n.tmr;

import com.huawei.g11n.tmr.datetime.detect.LocaleRules;
import com.huawei.g11n.tmr.datetime.detect.Rules;
import com.huawei.g11n.tmr.datetime.detect.UniverseRule;
import com.huawei.g11n.tmr.datetime.parse.DateConvert;
import com.huawei.g11n.tmr.datetime.parse.DateParse;
import com.huawei.g11n.tmr.datetime.parse.ParseRules;
import com.huawei.g11n.tmr.datetime.utils.DatePeriod;
import com.huawei.g11n.tmr.datetime.utils.LocaleParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class RuleInit {
    private String DTBridgeString;
    private RulesEngine clear;
    private List<RulesEngine> detects;
    private String locale;
    private String locale_bk;
    private HashMap<Integer, String> parses;
    private RulesEngine past;
    private String periodString;
    private DateParse rp;
    private HashMap<Integer, RulesEngine> subDetectsMap;

    public String getDTBridgeString() {
        return this.DTBridgeString;
    }

    public void setDTBridgeString(String dTBridgeString) {
        this.DTBridgeString = dTBridgeString;
    }

    public String getPeriodString() {
        return this.periodString;
    }

    public void setPeriodString(String periodString) {
        this.periodString = periodString;
    }

    public RuleInit(String locale, String localeBk) {
        this.locale = locale;
        this.locale_bk = localeBk;
        init();
        this.rp = new DateParse(locale, localeBk, this);
    }

    private void init() {
        LocaleParam param;
        LocaleRules localeRules;
        HashMap<Integer, String> hashMap;
        HashMap<Integer, String> hashMap2;
        HashMap<Integer, String> rules = new HashMap();
        UniverseRule ur = new UniverseRule();
        rules.putAll(ur.getRules());
        Rules rl = new Rules();
        HashMap<Integer, String> localeRules2 = new HashMap();
        HashMap<Integer, String> bkLocaleRules = new HashMap();
        LocaleRules lr = new LocaleRules(rl);
        localeRules2.putAll(lr.getLocaleRules(this.locale));
        bkLocaleRules.putAll(lr.getLocaleRules(this.locale_bk));
        LocaleParam param2 = new LocaleParam(this.locale);
        LocaleParam param_bk = new LocaleParam(this.locale_bk);
        setPeriodString("");
        if (this.locale.equals("jv") || this.locale.equals("fil")) {
            setPeriodString(param2.get("param_period").replace("\\b", ""));
        }
        setDTBridgeString("");
        if (this.locale.equals("be") || this.locale.equals("fil") || this.locale.equals("kk")) {
            setDTBridgeString(param2.get("param_DateTimeBridge").replace("\\b", ""));
        }
        RulesEngine uniDetects = new RulesEngine(this.locale, rules, rl.getSubRules(), param2, param_bk);
        this.detects = new ArrayList();
        this.detects.add(uniDetects);
        this.subDetectsMap = new HashMap();
        for (Integer name : ur.getSubRulesMaps().keySet()) {
            param = param2;
            localeRules = lr;
            hashMap = bkLocaleRules;
            hashMap2 = localeRules2;
            this.subDetectsMap.put(name, new RulesEngine(this.locale, (HashMap) ur.getSubRulesMaps().get(name), rl.getSubRules(), param, param_bk));
            param2 = param;
        }
        if (localeRules2.isEmpty()) {
            param = param2;
            localeRules = lr;
            hashMap = bkLocaleRules;
            hashMap2 = localeRules2;
        } else {
            param = param2;
            hashMap = bkLocaleRules;
            this.detects.add(new RulesEngine(this.locale, localeRules2, rl.getSubRules(), param, null));
        }
        if (!hashMap.isEmpty()) {
            this.detects.add(new RulesEngine(this.locale_bk, hashMap, rl.getSubRules(), param_bk, null));
        }
        ParseRules pr = new ParseRules();
        LocaleParam localeParam = param;
        LocaleParam localeParam2 = param_bk;
        this.parses = new RulesEngine(this.locale, pr.getRules(), rl.getSubRules(), localeParam, localeParam2, false).getRegexs();
        this.clear = new RulesEngine(this.locale, rl.getFilterRegex(), rl.getSubRules(), localeParam, localeParam2);
        this.past = new RulesEngine(this.locale, rl.getPastRegex(), null, localeParam, localeParam2);
    }

    public List<Match> detect(String msg) {
        Match c;
        List<Match> r1 = new ArrayList();
        for (RulesEngine detect : this.detects) {
            r1.addAll(detect.match(msg));
        }
        Iterator<Match> r1it = r1.iterator();
        ArrayList subResult = new ArrayList();
        while (r1it.hasNext()) {
            c = (Match) r1it.next();
            if (this.subDetectsMap.containsKey(Integer.valueOf(c.regex))) {
                List<Match> rsub = ((RulesEngine) this.subDetectsMap.get(Integer.valueOf(c.regex))).match(msg.substring(c.begin, c.end));
                for (Match ma : rsub) {
                    ma.setBegin(ma.getBegin() + c.getBegin());
                    ma.setEnd(ma.getEnd() + c.getBegin());
                }
                subResult.addAll(rsub);
                r1it.remove();
            }
        }
        r1.addAll(subResult);
        return new Filter(this.locale).filter(msg, r1, this);
    }

    public DatePeriod parse(String msg, long defaultTime) {
        Match c;
        List<Match> rsub;
        List<Match> r1 = new ArrayList();
        for (RulesEngine detect : this.detects) {
            r1.addAll(detect.match(msg));
        }
        Iterator<Match> r1it = r1.iterator();
        ArrayList subResult = new ArrayList();
        while (r1it.hasNext()) {
            c = (Match) r1it.next();
            if (this.subDetectsMap.containsKey(Integer.valueOf(c.regex))) {
                rsub = ((RulesEngine) this.subDetectsMap.get(Integer.valueOf(c.regex))).match(msg.substring(c.begin, c.end));
                for (Match ma : rsub) {
                    ma.setBegin(ma.getBegin() + c.getBegin());
                    ma.setEnd(ma.getEnd() + c.getBegin());
                }
                subResult.addAll(rsub);
                r1it.remove();
            }
        }
        r1.addAll(subResult);
        rsub = new Filter(this.locale).filterOverlay(r1);
        for (Match r12 : rsub) {
            DatePeriod tdp = this.rp.parse(msg.substring(r12.begin, r12.end), r12.getRegex(), defaultTime);
            if (tdp != null) {
                r12.setDp(tdp);
            }
        }
        return new DateConvert(this.locale).filterByParse(msg, rsub, getPeriodString(), getDTBridgeString());
    }

    public Pattern getParseByKey(Integer key) {
        return Pattern.compile((String) this.parses.get(key), 2);
    }

    public Pattern getDetectByKey(Integer key) {
        Pattern result = null;
        for (RulesEngine detect : this.detects) {
            result = detect.getPatterns(key);
            if (result != null) {
                break;
            }
        }
        if (result == null) {
            for (Integer name : this.subDetectsMap.keySet()) {
                result = ((RulesEngine) this.subDetectsMap.get(name)).getPatterns(key);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    public List<Match> clear(String msg) {
        return this.clear.match(msg);
    }

    public List<Match> pastFind(String msg) {
        return this.past.match(msg);
    }

    public String getLocale() {
        return this.locale;
    }

    public String getLocale_bk() {
        return this.locale_bk;
    }
}
