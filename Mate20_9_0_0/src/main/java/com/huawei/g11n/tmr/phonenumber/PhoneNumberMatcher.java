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
        CharSequence msgChar;
        Iterable<PhoneNumberMatch> matches;
        int[] result;
        String str = country;
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        int i = 1;
        int i2 = 2;
        int i3 = 0;
        int[] iArr;
        if (this.flag) {
            iArr = null;
        } else {
            Iterable<PhoneNumberMatch> matches2;
            CharSequence msgChar2;
            int[] result2;
            String str2;
            msgChar = msg;
            matches = util.findNumbers(msgChar, str, Leniency.POSSIBLE, Long.MAX_VALUE);
            List<String> list = new ArrayList();
            int y = 0;
            for (PhoneNumberMatch match : matches) {
                matches2 = matches;
                msgChar2 = msgChar;
                list.add(match.rawString());
                matches = matches2;
                msgChar = msgChar2;
                i = 1;
                i3 = 0;
            }
            int tnum = list.isEmpty() ? i3 : list.size();
            if (list.isEmpty()) {
                result2 = new int[i];
                result2[i3] = i3;
                iArr = result2;
            } else {
                result = new int[((2 * tnum) + i)];
                for (PhoneNumberMatch match2 : matches) {
                    result[(2 * y) + i] = match2.start();
                    result[(2 * y) + 2] = match2.end();
                    y++;
                }
                result[i3] = tnum;
                iArr = result;
            }
            int tnum2 = new ArrayList();
            y = ShortNumberInfo.getInstance();
            Pattern shortPattern = Pattern.compile("(?<!(\\d|\\*))\\d{2,8}(?!(\\d|\\*))");
            if (str.equals("CA")) {
                shortPattern = Pattern.compile("(?<!(\\d|\\*))\\d{5,8}(?!(\\d|\\*))");
            }
            Pattern shortPattern2 = shortPattern;
            Matcher shortMatch = shortPattern2.matcher(msg);
            PhoneNumber pn = null;
            while (shortMatch.find()) {
                matches2 = matches;
                msgChar2 = msgChar;
                Pattern shortPattern3 = shortPattern2;
                try {
                    result2 = util.parseAndKeepRawInput(shortMatch.group(), str);
                } catch (NumberParseException e) {
                    NumberParseException numberParseException = e;
                    e.printStackTrace();
                    result2 = pn;
                }
                if (y.isPossibleShortNumberForRegion(result2, str)) {
                    tnum2.add(Integer.valueOf(shortMatch.start()));
                    tnum2.add(Integer.valueOf(shortMatch.end()));
                }
                pn = result2;
                matches = matches2;
                msgChar = msgChar2;
                shortPattern2 = shortPattern3;
                str2 = msg;
            }
            int short_size = tnum2.isEmpty() ? 0 : tnum2.size() / 2;
            matches = new int[(2 * short_size)];
            msgChar = 0;
            while (msgChar < 2 * short_size) {
                matches[msgChar] = ((Integer) tnum2.get(msgChar)).intValue();
                msgChar++;
                str2 = msg;
            }
            if (short_size == 0) {
                return iArr;
            }
            if (iArr[0] != 0) {
                int[] final_result = new int[((2 * (tnum + short_size)) + 1)];
                System.arraycopy(iArr, 0, final_result, 0, iArr.length);
                System.arraycopy(matches, 0, final_result, iArr.length, matches.length);
                final_result[0] = tnum + short_size;
                return final_result;
            }
            if (iArr[0] == 0) {
                shortPattern2 = new int[((2 * short_size) + 1)];
                shortPattern2[0] = short_size;
                System.arraycopy(matches, 0, shortPattern2, 1, matches.length);
                return shortPattern2;
            }
        }
        String src = convertQanChar(msg);
        String filteredString = handleNegativeRule(src, this.phoneNumberRule);
        ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();
        msgChar = filteredString;
        matches = util.findNumbers(msgChar, str, Leniency.POSSIBLE, Long.MAX_VALUE);
        List<MatchedNumberInfo> ret = new ArrayList();
        for (PhoneNumberMatch match3 : matches) {
            PhoneNumberMatch match32;
            if (handleBorderRule(match32, filteredString, this.phoneNumberRule)) {
                PhoneNumberMatch delMatch = handleCodeRule(match32, src, this.phoneNumberRule);
                if (delMatch != null) {
                    String src2;
                    Iterable<PhoneNumberMatch> matchesList;
                    CharSequence filteredStringC;
                    match32 = delMatch;
                    if (util.isValidNumber(match32.number())) {
                        MatchedNumberInfo info = new MatchedNumberInfo();
                        info.setBegin(match32.start());
                        info.setEnd(match32.end());
                        info.setContent(match32.rawString());
                        if ("CN".equals(str)) {
                            src2 = src;
                            src = Pattern.compile("(?<![-\\d])\\d{5,6}[\\/|\\|]\\d{5,6}(?![-\\d])");
                            String str3 = "";
                            matchesList = matches;
                            filteredStringC = msgChar;
                            if (info.getContent().startsWith("(") || info.getContent().startsWith("[")) {
                                matches = info.getContent().substring(1);
                            } else {
                                matches = info.getContent();
                            }
                            if (!src.matcher(matches).matches()) {
                                ret.add(info);
                                src = src2;
                                matches = matchesList;
                                msgChar = filteredStringC;
                                i2 = 2;
                            }
                        } else {
                            src2 = src;
                            matchesList = matches;
                            filteredStringC = msgChar;
                            ret.add(info);
                        }
                    } else {
                        src2 = src;
                        matchesList = matches;
                        filteredStringC = msgChar;
                    }
                    List<MatchedNumberInfo> posList = handlePositiveRule(match32, filteredString, this.phoneNumberRule);
                    if (!(posList == null || posList.isEmpty())) {
                        ret.addAll(posList);
                    }
                    src = src2;
                    matches = matchesList;
                    msgChar = filteredStringC;
                    i2 = 2;
                }
            }
        }
        ret.addAll(this.phoneNumberRule.handleShortPhoneNumbers(filteredString, str));
        List<MatchedNumberInfo> ret2 = deleteRepeatedInfo(ret);
        for (MatchedNumberInfo ret3 : ret2) {
            if (ret3 != null) {
                dealNumbersWithOneBracket(ret3);
            }
        }
        int length = ret2.isEmpty() ? 0 : ret2.size();
        if (length == 0) {
            result = new int[]{0};
        } else {
            result = new int[((length * 2) + 1)];
            result[0] = length;
            for (int i4 = 0; i4 < length; i4++) {
                result[(i4 * 2) + 1] = ((MatchedNumberInfo) ret2.get(i4)).getBegin();
                result[(i4 * 2) + i2] = ((MatchedNumberInfo) ret2.get(i4)).getEnd();
            }
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
                if (msg.charAt(i) == 12305) {
                    hasRight = true;
                }
                if (msg.charAt(i) == 12304 && i == 0) {
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
            if (index == -1) {
                retsb.append(tempstr);
            } else {
                retsb.append(hwchstr.substring(index, index + 1));
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
                if (infoList == null) {
                    continue;
                } else if (!infoList.isEmpty()) {
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
            String str = msg;
            return true;
        }
        int begin = match.start();
        int end = match.end();
        int bStr = begin + -10 < 0 ? 0 : begin - 10;
        String s = msg.substring(bStr, end + 10 > msg.length() ? msg.length() : end + 10);
        for (RegexRule rule : rules) {
            Matcher mat = rule.getPattern().matcher(s);
            int type = rule.getType();
            while (mat.find()) {
                int b = mat.start() + bStr;
                int e = mat.end() + bStr;
                boolean isdel = false;
                switch (type) {
                    case 8:
                        if ((b <= begin && end <= e) || ((b < begin && begin < e && e < end) || (begin < b && b < end && end < e))) {
                            isdel = true;
                            continue;
                        }
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
