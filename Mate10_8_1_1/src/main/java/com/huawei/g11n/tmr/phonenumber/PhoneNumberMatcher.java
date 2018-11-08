package com.huawei.g11n.tmr.phonenumber;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberMatch;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.ShortNumberInfo;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule.RegexRule;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_DE_DE;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_EN_GB;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_ES_ES;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_FR_FR;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_IT_IT;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_PT_PT;
import com.huawei.g11n.tmr.phonenumber.data.PhoneNumberRule_ZH_CN;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberMatcher extends AbstractPhoneNumberMatcher {
    private static final char REPLACE_CHAR = 'A';
    private boolean flag = true;
    private PhoneNumberRule phoneNumberRule;

    public PhoneNumberMatcher(String country) {
        super(country);
        if ("CN".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_ZH_CN(country);
        } else if ("GB".equals(country) || "UK".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_EN_GB(country);
        } else if ("DE".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_DE_DE(country);
        } else if ("IT".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_IT_IT(country);
        } else if ("FR".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_FR_FR(country);
        } else if ("ES".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_ES_ES(country);
        } else if ("PT".equals(country)) {
            this.phoneNumberRule = new PhoneNumberRule_PT_PT(country);
        } else {
            this.flag = false;
            this.phoneNumberRule = null;
        }
    }

    public List<MatchedNumberInfo> deleteRepeatedInfo(List<MatchedNumberInfo> list) {
        List<MatchedNumberInfo> result = new ArrayList();
        for (MatchedNumberInfo info : list) {
            boolean isAdd = true;
            for (MatchedNumberInfo in : result) {
                if (info.getBegin() == in.getBegin() && info.getEnd() == in.getEnd()) {
                    isAdd = false;
                }
            }
            if (isAdd) {
                result.add(info);
            }
        }
        return result;
    }

    public int[] getMatchedPhoneNumber(String msg, String country) {
        PhoneNumberMatch match;
        int[] result;
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        if (!this.flag) {
            Object result2;
            Iterable<PhoneNumberMatch> matches = util.findNumbers(msg, country, Leniency.POSSIBLE, Long.MAX_VALUE);
            List<String> list = new ArrayList();
            int y = 0;
            for (PhoneNumberMatch match2 : matches) {
                list.add(match2.rawString());
            }
            int tnum = !list.isEmpty() ? list.size() : 0;
            if (list.isEmpty()) {
                result2 = new int[]{0};
            } else {
                result2 = new int[((tnum * 2) + 1)];
                for (PhoneNumberMatch match22 : matches) {
                    result2[(y * 2) + 1] = match22.start();
                    result2[(y * 2) + 2] = match22.end();
                    y++;
                }
                result2[0] = tnum;
            }
            PhoneNumber pn = null;
            List<Integer> shortList = new ArrayList();
            ShortNumberInfo info = ShortNumberInfo.getInstance();
            Pattern shortPattern = Pattern.compile("(?<!(\\d|\\*))\\d{2,8}(?!(\\d|\\*))");
            if (country.equals("CA")) {
                shortPattern = Pattern.compile("(?<!(\\d|\\*))\\d{5,8}(?!(\\d|\\*))");
            }
            Matcher shortMatch = shortPattern.matcher(msg);
            while (shortMatch.find()) {
                try {
                    pn = util.parseAndKeepRawInput(shortMatch.group(), country);
                } catch (NumberParseException e) {
                    e.printStackTrace();
                }
                if (info.isPossibleShortNumberForRegion(pn, country)) {
                    shortList.add(Integer.valueOf(shortMatch.start()));
                    shortList.add(Integer.valueOf(shortMatch.end()));
                }
            }
            int short_size = !shortList.isEmpty() ? shortList.size() / 2 : 0;
            Object result_short = new int[(short_size * 2)];
            for (int z = 0; z < short_size * 2; z++) {
                result_short[z] = ((Integer) shortList.get(z)).intValue();
            }
            if (short_size == 0) {
                return result2;
            }
            int[] final_result;
            if (result2[0] != 0) {
                final_result = new int[(((tnum + short_size) * 2) + 1)];
                System.arraycopy(result2, 0, final_result, 0, result2.length);
                System.arraycopy(result_short, 0, final_result, result2.length, result_short.length);
                final_result[0] = tnum + short_size;
                return final_result;
            } else if (result2[0] == 0) {
                final_result = new int[((short_size * 2) + 1)];
                final_result[0] = short_size;
                System.arraycopy(result_short, 0, final_result, 1, result_short.length);
                return final_result;
            }
        }
        String src = convertQanChar(msg);
        String filteredString = handleNegativeRule(src, this.phoneNumberRule);
        ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();
        Iterable<PhoneNumberMatch> matchesList = util.findNumbers(filteredString, country, Leniency.POSSIBLE, Long.MAX_VALUE);
        List<MatchedNumberInfo> ret = new ArrayList();
        for (PhoneNumberMatch match222 : matchesList) {
            if (handleBorderRule(match222, filteredString, this.phoneNumberRule)) {
                PhoneNumberMatch delMatch = handleCodeRule(match222, src, this.phoneNumberRule);
                if (delMatch != null) {
                    match222 = delMatch;
                    if (util.isValidNumber(delMatch.number())) {
                        MatchedNumberInfo info2 = new MatchedNumberInfo();
                        info2.setBegin(delMatch.start());
                        info2.setEnd(delMatch.end());
                        info2.setContent(delMatch.rawString());
                        if ("CN".equals(country)) {
                            Pattern p = Pattern.compile("(?<![-\\d])\\d{5,6}[\\/|\\|]\\d{5,6}(?![-\\d])");
                            String str = "";
                            if (info2.getContent().startsWith("(") || info2.getContent().startsWith("[")) {
                                str = info2.getContent().substring(1);
                            } else {
                                str = info2.getContent();
                            }
                            if (!p.matcher(str).matches()) {
                                ret.add(info2);
                            }
                        } else {
                            ret.add(info2);
                        }
                    }
                    List<MatchedNumberInfo> posList = handlePositiveRule(delMatch, filteredString, this.phoneNumberRule);
                    if (!(posList == null || posList.isEmpty())) {
                        ret.addAll(posList);
                    }
                }
            }
        }
        ret.addAll(this.phoneNumberRule.handleShortPhoneNumbers(filteredString, country));
        ret = deleteRepeatedInfo(ret);
        for (MatchedNumberInfo mi : ret) {
            if (mi != null) {
                dealNumbersWithOneBracket(mi);
            }
        }
        int length = !ret.isEmpty() ? ret.size() : 0;
        if (length != 0) {
            result = new int[((length * 2) + 1)];
            result[0] = length;
            for (int i = 0; i < length; i++) {
                result[(i * 2) + 1] = ((MatchedNumberInfo) ret.get(i)).getBegin();
                result[(i * 2) + 2] = ((MatchedNumberInfo) ret.get(i)).getEnd();
            }
        } else {
            result = new int[]{0};
        }
        return result;
    }

    private PhoneNumberMatch handleCodeRule(PhoneNumberMatch match, String msg, PhoneNumberRule phoneNumberRule2) {
        List<RegexRule> rules = this.phoneNumberRule.getCodesRules();
        boolean isValid = true;
        if (rules != null) {
            for (RegexRule r : rules) {
                match = r.isValid(match, msg);
                if (match == null) {
                    isValid = false;
                    break;
                }
            }
        }
        isValid = true;
        if (isValid) {
            return match;
        }
        return null;
    }

    private static boolean isNumbersWithOneBracket(String msg) {
        boolean hasRight = false;
        boolean hasLeft = false;
        if (msg != null) {
            int i = 0;
            while (i < msg.length()) {
                if (msg.charAt(i) == ')') {
                    hasRight = true;
                }
                if (msg.charAt(i) == '(' && i == 0) {
                    hasLeft = true;
                }
                if (msg.charAt(i) == ']') {
                    hasRight = true;
                }
                if (msg.charAt(i) == '[' && i == 0) {
                    hasLeft = true;
                }
                if (msg.charAt(i) == '】') {
                    hasRight = true;
                }
                if (msg.charAt(i) == '【' && i == 0) {
                    hasLeft = true;
                }
                i++;
            }
            if (hasLeft && !hasRight) {
                return true;
            }
        }
        return false;
    }

    private static MatchedNumberInfo dealNumbersWithOneBracket(MatchedNumberInfo info) {
        if (!isNumbersWithOneBracket(info.getContent())) {
            return info;
        }
        info.setBegin(info.getBegin() + 1);
        info.setContent(info.getContent().substring(1));
        return info;
    }

    private static String dealStringWithOneBracket(String msg) {
        if (isNumbersWithOneBracket(msg)) {
            return msg.substring(1);
        }
        return msg;
    }

    private static String convertQanChar(String instr) {
        StringBuffer retsb = new StringBuffer("");
        String fwchstr = "：／．＼∕，.！（）？﹡；：﹣—－【】－＋＝｛｝１２３４５６７８９０ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ";
        String hwchstr = ":/.\\/,.!()?*;:---[]-+={}1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < instr.length(); i++) {
            String tempstr = instr.substring(i, i + 1);
            int index = fwchstr.indexOf(tempstr);
            if (index != -1) {
                retsb.append(hwchstr.substring(index, index + 1));
            } else {
                retsb.append(tempstr);
            }
        }
        return retsb.toString();
    }

    private static String handleNegativeRule(String src, PhoneNumberRule phoneNumberRule) {
        String ret = src;
        for (RegexRule rule : phoneNumberRule.getNegativeRules()) {
            Matcher m = rule.getPattern().matcher(ret);
            while (m.find()) {
                ret = replaceSpecifiedPos(ret.toCharArray(), m.start(), m.end());
            }
        }
        return ret;
    }

    private static List<MatchedNumberInfo> handlePositiveRule(PhoneNumberMatch match, String msg, PhoneNumberRule phoneNumberRule) {
        List<RegexRule> rules = phoneNumberRule.getPositiveRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        List<MatchedNumberInfo> ret = new ArrayList();
        String str = dealStringWithOneBracket(match.rawString());
        for (RegexRule rule : rules) {
            if (rule.getPattern().matcher(str).find()) {
                ret.addAll(rule.handle(match, msg));
                return ret;
            } else if (rule.getPattern().matcher(msg).find()) {
                List<MatchedNumberInfo> infoList = rule.handle(match, msg);
                if (!(infoList == null || infoList.isEmpty())) {
                    ret.addAll(infoList);
                    return ret;
                }
            }
        }
        return null;
    }

    private static boolean handleBorderRule(PhoneNumberMatch match, String msg, PhoneNumberRule phoneNumberRule) {
        List<RegexRule> rules = phoneNumberRule.getBorderRules();
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        int begin = match.start();
        int end = match.end();
        int bStr = begin + -10 >= 0 ? begin - 10 : 0;
        String s = msg.substring(bStr, end + 10 <= msg.length() ? end + 10 : msg.length());
        for (RegexRule rule : rules) {
            Matcher mat = rule.getPattern().matcher(s);
            int type = rule.getType();
            while (mat.find()) {
                int b = mat.start() + bStr;
                int e = mat.end() + bStr;
                boolean isdel = false;
                switch (type) {
                    case 8:
                        if (b > begin || end > e) {
                            if (b >= begin || begin >= e || e >= end) {
                                if (begin >= b) {
                                    continue;
                                } else if (b >= end) {
                                    continue;
                                } else if (end >= e) {
                                    continue;
                                }
                            }
                        }
                        isdel = true;
                        continue;
                    case 9:
                        if (b <= begin && end <= e) {
                            isdel = true;
                            continue;
                        }
                    default:
                        break;
                }
                if (isdel) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String replaceSpecifiedPos(char[] chs, int s, int e) {
        if (s > e) {
            return new String(chs);
        }
        int i = 0;
        while (i < chs.length) {
            if (i >= s && i < e) {
                chs[i] = REPLACE_CHAR;
            }
            i++;
        }
        return new String(chs);
    }
}
