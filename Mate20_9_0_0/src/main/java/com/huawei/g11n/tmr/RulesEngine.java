package com.huawei.g11n.tmr;

import com.huawei.g11n.tmr.datetime.utils.LocaleParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RulesEngine {
    private String locale;
    private HashMap<Integer, Pattern> patterns;
    private HashMap<Integer, String> regexs;

    public RulesEngine(String locale, HashMap<Integer, String> rules, HashMap<String, String> subRules, LocaleParam param, LocaleParam param_en, boolean isPat) {
        this.locale = locale;
        init(locale, rules, subRules, param, param_en, isPat);
    }

    public RulesEngine(String locale, HashMap<Integer, String> rules, HashMap<String, String> subRules, LocaleParam param, LocaleParam param_en) {
        this.locale = locale;
        init(locale, rules, subRules, param, param_en, true);
    }

    public Pattern getPatterns(Integer key) {
        if (this.patterns != null && this.patterns.containsKey(key)) {
            return (Pattern) this.patterns.get(key);
        }
        return null;
    }

    private void init(String locale, HashMap<Integer, String> rules, HashMap<String, String> subRules, LocaleParam param, LocaleParam param_bk, boolean isPat) {
        RulesEngine rulesEngine = this;
        HashMap hashMap = subRules;
        LocaleParam localeParam = param;
        LocaleParam localeParam2 = param_bk;
        rulesEngine.patterns = new HashMap();
        rulesEngine.regexs = new HashMap();
        Pattern pattern = Pattern.compile("\\[(param_\\w+)\\]");
        Pattern pattern2 = Pattern.compile("\\[regex_(\\w+)\\]");
        Pattern pattern3 = Pattern.compile("\\[paramopt_(\\w+)\\]");
        for (Entry<Integer, String> entry : rules.entrySet()) {
            Matcher match2;
            Matcher match3;
            String pmo;
            HashMap<String, String> hashMap2;
            boolean valid = true;
            Integer name = (Integer) entry.getKey();
            String rule = (String) entry.getValue();
            int i = 1;
            if (!(hashMap == null || subRules.isEmpty())) {
                match2 = pattern2.matcher(rule);
                while (match2.find()) {
                    rule = rule.replace(match2.group(), (CharSequence) hashMap.get(match2.group(i)));
                    i = 1;
                }
            }
            if (!(localeParam == null && localeParam2 == null)) {
                match3 = pattern3.matcher(rule);
                while (match3.find()) {
                    StringBuilder stringBuilder;
                    String value = new StringBuilder("param_");
                    value.append(match3.group(1));
                    value = value.toString();
                    int findex = match3.start();
                    int rindex = findex;
                    int fend = match3.end();
                    pmo = (localeParam == null || localeParam.get(value) == null) ? "" : localeParam.get(value);
                    String str = (localeParam2 == null || localeParam2.get(value) == null) ? "" : localeParam2.get(value);
                    String pmo2 = str;
                    if (pmo.isEmpty()) {
                        value = pmo2;
                        if (!value.isEmpty()) {
                            pmo = value;
                            if (pmo.isEmpty() || !pmo2.isEmpty()) {
                            } else {
                                if (rule.substring(findex - 1, findex).equals("|") != null) {
                                    value = findex - 1;
                                } else {
                                    value = findex;
                                }
                                rule = rule.replace(rule.substring(value, fend), "");
                            }
                            stringBuilder = new StringBuilder("paramopt_");
                            stringBuilder.append(match3.group(1));
                            rule = rule.replace("[".concat(stringBuilder.toString()).concat("]"), pmo);
                            rulesEngine = this;
                            hashMap2 = subRules;
                            localeParam = param;
                            localeParam2 = param_bk;
                        }
                    } else {
                        value = pmo2;
                    }
                    if (!(pmo.trim().isEmpty() || value.trim().isEmpty())) {
                        pmo = pmo.concat("|").concat(value);
                    }
                    if (pmo.isEmpty()) {
                    }
                    stringBuilder = new StringBuilder("paramopt_");
                    stringBuilder.append(match3.group(1));
                    rule = rule.replace("[".concat(stringBuilder.toString()).concat("]"), pmo);
                    rulesEngine = this;
                    hashMap2 = subRules;
                    localeParam = param;
                    localeParam2 = param_bk;
                }
            }
            if (localeParam != null || localeParam2 != null) {
                match2 = pattern.matcher(rule);
                while (match2.find()) {
                    String value2 = match2.group(1);
                    match3 = (localeParam == null || localeParam.get(value2) == null) ? "" : localeParam.get(value2);
                    pmo = (localeParam2 == null || localeParam2.get(value2) == null) ? "" : localeParam2.get(value2);
                    if (match3.isEmpty() && !pmo.isEmpty()) {
                        match3 = pmo;
                    } else if (!(match3.trim().isEmpty() || pmo.trim().isEmpty() || match3.endsWith("]") || match3.endsWith("]\\b") || !rulesEngine.isConactBkParam(name))) {
                        match3 = match3.concat("|").concat(pmo);
                    }
                    if (match3.trim().isEmpty()) {
                        valid = false;
                        break;
                    }
                    rule = rule.replace("[".concat(value2).concat("]"), match3);
                    rulesEngine = this;
                    hashMap2 = subRules;
                }
            }
            if (!(rule == null || rule.trim().equals("") || !valid)) {
                if (isPat) {
                    rulesEngine.patterns.put(name, Pattern.compile(rule, 2));
                } else {
                    rulesEngine.regexs.put(name, rule);
                }
            }
            hashMap2 = subRules;
        }
    }

    private boolean isConactBkParam(Integer rNum) {
        if ((rNum.intValue() != 20009 && rNum.intValue() != 20010 && rNum.intValue() != 20011) || this.locale.equals("zh_hans") || this.locale.equals("en")) {
            return true;
        }
        return false;
    }

    public HashMap<Integer, String> getRegexs() {
        return this.regexs;
    }

    public List<Match> match(String msg) {
        List<Match> matchs = new ArrayList();
        List<Integer> keys = new ArrayList();
        keys.addAll(this.patterns.keySet());
        Collections.sort(keys);
        for (Integer name : keys) {
            Matcher match = ((Pattern) this.patterns.get(name)).matcher(msg);
            while (match.find()) {
                matchs.add(new Match(match.start(), match.end(), String.valueOf(name)));
            }
        }
        return matchs;
    }

    public Pattern getPattenById(Integer id) {
        if (this.patterns == null || !this.patterns.containsKey(id)) {
            return null;
        }
        return (Pattern) this.patterns.get(id);
    }

    public String getLocale() {
        return this.locale;
    }
}
