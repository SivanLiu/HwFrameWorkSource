package com.huawei.g11n.tmr.address;

import com.huawei.g11n.tmr.address.jni.DicSearch;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SerEn {
    private static final int TYPE_BUILDING = 1;
    private static final int TYPE_BUILDING2 = 2;
    private static final int TYPE_CITY = 0;
    private String location = this.reguEx.location;
    ArrayList<Integer> match_index_2 = new ArrayList();
    private String not = "(?i)(?:my|your|his|her|its|their|our|this|that|the|a|an|what|which|whose)";
    private Pattern p1346 = this.reguEx.p1346;
    private Pattern p28 = this.reguEx.p28;
    private Pattern p2s = this.reguEx.p2s;
    private Pattern p52 = this.reguEx.p52;
    private Pattern p52_sub = this.reguEx.p52_sub;
    private Pattern p52s = this.reguEx.p52s;
    private Pattern pCode_a = Pattern.compile("(?<!\\d)(?:\\d{5}(?:\\s*-\\s*\\d{4})?)(?!\\d)");
    Pattern pComma;
    Pattern pCut;
    private Pattern pDir = Pattern.compile("\\s*(south|north|west|east)\\s*");
    Pattern pLocation;
    Pattern pNo;
    Pattern pNot_1;
    Pattern pNot_2;
    private Pattern pNum;
    Pattern pPre_city;
    Pattern pPre_uni;
    private Pattern pRoad = Pattern.compile("(?i)(?:\\s*(?:(in|on|at)\\s+)?(?:the\\s+)?(boulevard|avenue|street|freeway|road|circle|lane|drive|court|ally|parkway|Ave|AV|Blvd|Cir|Ct|Dr|Ln|Pkwy|Rd|Sq|St|Way|Fwy|Crescent|Highway))");
    Pattern pSingle;
    private Pattern p_box = Pattern.compile(this.reguEx.post_box);
    private Pattern p_resultclean = Pattern.compile("(?:(?:[^0-9a-zA-Z]*)(?i)(?:(?:in|at|on|from|to|of|and)\\s+)?(?:(?:the)\\s+)?)(?:([\\s\\S]*)?,|([\\s\\S]*))");
    private ReguEx reguEx = new ReguEx();
    private String road_suf = "(?:boulevard|avenue|street|freeway|road|circle|way|lane|drive|court|ally|parkway|Crescent|Highway|(?:Ave|AV|Blvd|Cir|Ct|Dr|Ln|Pkwy|Rd|Sq|St|Fwy)(?:\\.|\\b))";

    SerEn() {
        StringBuilder stringBuilder = new StringBuilder("([\\s\\S]*?)(?<![a-zA-Z])");
        stringBuilder.append(this.location);
        stringBuilder.append("(?![a-zA-Z])");
        this.pNot_1 = Pattern.compile(stringBuilder.toString());
        stringBuilder = new StringBuilder("[\\s\\S]*(?<![a-zA-Z])");
        stringBuilder.append(this.not);
        stringBuilder.append("\\s+");
        this.pNot_2 = Pattern.compile(stringBuilder.toString());
        this.pNum = Pattern.compile("(?:(?:\\s*[:,\\.\"-]\\s*|\\s*)\\d+(?:\\s*[,\\.\":-]\\s*|\\s+))+");
        stringBuilder = new StringBuilder("(?:([\\s\\S]*?)(?<![a-zA-Z])((?:");
        stringBuilder.append(this.location);
        stringBuilder.append(")((?:\\s+|\\s*&\\s*)(?:");
        stringBuilder.append(this.location);
        stringBuilder.append("))?");
        stringBuilder.append(")(?![a-zA-Z]))");
        this.pLocation = Pattern.compile(stringBuilder.toString());
        this.pComma = Pattern.compile("(?:(?:[\\s\\S]*)(?:,|\\.)([\\s\\S]*))");
        this.pPre_city = Pattern.compile("(?<![a-zA-Z])(?:\\s*[,.]*\\s*)*(?:(?i)in)(?![a-zA-Z])");
        this.pPre_uni = Pattern.compile("(?:\\b(?i)(in|at|from|near|to|of|for)\\b([\\s\\S]*))");
        this.pNo = Pattern.compile("(?:[\\s\\S]*(?<![a-zA-Z])(?i)(the|in|on|at|from|to|of|for)(?:(?:(?:\\s*[,.-:'\"()]\\s*)+)|\\s+))");
        this.pCut = Pattern.compile("(\\s*[,.]?\\s*(?:(?i)(?:in|on|at|from|of)\\s+)?(?:(?i)(uptown|downtown)\\s+)?)?[\\s\\S]*");
        this.pSingle = Pattern.compile("(?:\\.)?\\s*,\\s*[A-Z][a-z]+(?:\\s*(?:[,.)\"'])\\s*)*");
    }

    /* JADX WARNING: Removed duplicated region for block: B:342:0x0d77  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x0ced  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x0ced  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x0d77  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x0972  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x08e5  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x08e5  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x0972  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x0972  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x08e5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ArrayList<Match> search(String source) {
        int end;
        String city;
        Pattern pattern;
        Matcher m28;
        Pattern pattern2;
        Pattern pattern3;
        Pattern pNot_road;
        String str;
        Matcher m282;
        String out;
        Matcher m_resultclean;
        StringBuilder stringBuilder;
        int start;
        int end2;
        String city2;
        Pattern pSingle;
        Matcher m_resultclean2;
        int start2;
        int start3;
        int outLen;
        String cut;
        Matcher m52;
        Pattern pSingle2;
        Pattern pPre_road;
        Pattern pNot_road2;
        int start4;
        Matcher matcher;
        String[] strArr;
        Exception e;
        int length_bui;
        String[] buildings;
        int head;
        PrintStream printStream;
        String pSingle3 = source;
        ArrayList<Integer> nn = new ArrayList();
        Pattern p_big = Pattern.compile("[A-Z0-9]");
        Pattern pCut = Pattern.compile("(\\s*[,.]?\\s*(?:(?i)(?:in|on|at|from|of)\\s+)?(?:(?i)(?:uptown|downtown)\\s+)?)?[\\s\\S]*");
        Pattern pSingle4 = Pattern.compile("(?:\\.)?\\s*,\\s*[A-Z][a-z]+(?:\\s*(?:[,.)\"'])\\s*)*");
        StringBuilder stringBuilder2 = new StringBuilder("(?i)(?<![a-z])(?:(?:in|on|at|to)\\s+(?:the\\s+)?|the\\s+)((?:[\\s\\S]+?)(?:(?<![a-z])((?:in|on|at|to)\\s+(?:the\\s+)?|the\\s+))?");
        stringBuilder2.append(this.road_suf);
        stringBuilder2.append("(?![a-zA-Z])[\\s\\S]*)");
        Pattern pPre_road2 = Pattern.compile(stringBuilder2.toString());
        StringBuilder stringBuilder3 = new StringBuilder("(?i)((?<![a-zA-Z])(?:a|what|which|whose|i|you|this|that|my|his|her|out|their|its)\\s+)([\\s\\S]+)?");
        stringBuilder3.append(this.road_suf);
        stringBuilder3.append("(?![a-zA-Z])");
        Pattern pNot_road3 = Pattern.compile(stringBuilder3.toString());
        StringBuilder stringBuilder4 = new StringBuilder("(?:[^0-9a-zA-Z]*|\\s*(?:(?i)the|this|a|that)\\s*)(?:");
        stringBuilder4.append(this.location);
        stringBuilder4.append(")[^0-9a-zA-Z]*");
        Pattern pBuilding = Pattern.compile(stringBuilder4.toString());
        int end3 = 0;
        String out2 = "";
        int num = 0;
        nn.add(Integer.valueOf(0));
        Matcher m522 = this.p52.matcher(pSingle3);
        String cut2 = "";
        Matcher m283 = this.p28.matcher(pSingle3);
        int outLen2 = 0;
        Matcher m1346 = this.p1346.matcher(pSingle3);
        int start5 = 0;
        Matcher m_52sub = this.p52_sub.matcher(pSingle3);
        boolean noBox = true;
        this.match_index_2.clear();
        String city3 = "";
        while (true) {
            end = end3;
            if (!m283.find()) {
                break;
            }
            city = city3;
            pattern = p_big;
            m28 = m283;
            pattern2 = pCut;
            pattern3 = pSingle4;
            p_big = pPre_road2;
            pNot_road = pNot_road3;
            str = out2;
            m282 = m28;
            if (m282.group(1) != null) {
                if (m282.group(4) != null) {
                    city3 = this.p_resultclean.matcher(m282.group());
                    if (city3.matches()) {
                        if (city3.group(1) != null) {
                            out = city3.group(1);
                            out2 = out;
                            outLen2 = out.length() + 1;
                        } else {
                            out = city3.group(2);
                            outLen2 = out.length();
                            out2 = out;
                        }
                        pCut = m282.start() + (m282.group().length() - outLen2);
                        end3 = pCut + out2.length();
                        nn.add(Integer.valueOf(pCut));
                        nn.add(Integer.valueOf(end3));
                        if (m282.group(2) != null) {
                            noBox = false;
                        }
                    }
                    pPre_road2 = p_big;
                    pNot_road3 = pNot_road;
                    end3 = end;
                } else {
                    if (m282.group(2) != null) {
                        m_resultclean = this.p_resultclean.matcher(m282.group());
                    } else {
                        city3 = searCity(m282.group(3), 1);
                        if (city3 == null) {
                            city3 = "";
                        }
                        pCut = this.p_resultclean;
                        stringBuilder = new StringBuilder(String.valueOf(city3));
                        stringBuilder.append(m282.group(5));
                        stringBuilder.append(m282.group(6));
                        city = city3;
                        m_resultclean = pCut.matcher(stringBuilder.toString());
                    }
                    if (m_resultclean.matches()) {
                        if (m_resultclean.group(1) != null) {
                            out = m_resultclean.group(1);
                            out2 = out;
                            outLen2 = out.length() + 1;
                        } else {
                            out = m_resultclean.group(2);
                            outLen2 = out.length();
                            out2 = out;
                        }
                        start = m282.start(5);
                        stringBuilder2 = new StringBuilder(String.valueOf(m282.group(5)));
                        stringBuilder2.append(m282.group(6));
                        pCut = (stringBuilder2.toString().length() - outLen2) + start;
                        end3 = pCut + out2.length();
                        nn.add(Integer.valueOf(pCut));
                        nn.add(Integer.valueOf(end3));
                        if (m282.group(2) != null) {
                            noBox = false;
                        }
                    }
                    pPre_road2 = p_big;
                    pNot_road3 = pNot_road;
                    end3 = end;
                }
                pPre_road2 = p_big;
                pNot_road3 = pNot_road;
                start5 = pCut;
                city3 = city;
                p_big = pattern;
                pCut = pattern2;
                pSingle4 = pattern3;
                m283 = m282;
                pSingle3 = source;
            } else if (this.pCode_a.matcher(m282.group()).find()) {
                if (m282.group(6).indexOf(45) != -1) {
                    pSingle4 = m282.start(6);
                    end3 = pSingle4 + m282.group(6).length();
                    nn.add(Integer.valueOf(pSingle4));
                    nn.add(Integer.valueOf(end3));
                } else {
                    if (m282.group(5) != null && m282.group(5).length() > 0) {
                        pSingle4 = m282.start(6);
                        end3 = pSingle4 + m282.group(6).length();
                        nn.add(Integer.valueOf(pSingle4));
                        nn.add(Integer.valueOf(end3));
                    }
                    pPre_road2 = p_big;
                    pNot_road3 = pNot_road;
                    end3 = end;
                }
                pPre_road2 = p_big;
                pNot_road3 = pNot_road;
                start5 = pSingle4;
            } else {
                pCut = m282.start();
                end3 = pCut + m282.group().length();
                nn.add(Integer.valueOf(pCut));
                nn.add(Integer.valueOf(end3));
                pPre_road2 = p_big;
                pNot_road3 = pNot_road;
                start5 = pCut;
            }
            city3 = city;
            out2 = str;
            p_big = pattern;
            pCut = pattern2;
            pSingle4 = pattern3;
            m283 = m282;
            pSingle3 = source;
        }
        if (noBox) {
            Matcher m_box = this.p_box.matcher(pSingle3);
            while (m_box.find()) {
                city = city3;
                city3 = m_box.start();
                str = out2;
                end2 = m_box.group().length() + city3;
                Matcher m_box2 = m_box;
                nn.add(Integer.valueOf(city3));
                nn.add(Integer.valueOf(end2));
                start5 = city3;
                end = end2;
                city3 = city;
                out2 = str;
                m_box = m_box2;
            }
            end3 = end;
        } else {
            str = out2;
            end3 = end;
        }
        while (true) {
            city2 = city3;
            if (!m522.find()) {
                break;
            }
            pattern = p_big;
            m28 = m283;
            pSingle = pSingle4;
            p_big = pPre_road2;
            pNot_road = pNot_road3;
            out2 = "";
            m_resultclean = this.pRoad.matcher(m522.group());
            if (m_resultclean.matches()) {
                pSingle4 = pSingle;
                pPre_road2 = p_big;
                pNot_road3 = pNot_road;
                city3 = city2;
                m283 = m28;
                p_big = pattern;
                pSingle3 = source;
            } else {
                String out3;
                String str2;
                Matcher mNot_road;
                if (m522.group(5) == null) {
                    m_resultclean2 = this.p_resultclean.matcher(m522.group(1));
                    if (m_resultclean2.matches()) {
                        if (m_resultclean2.group(1) != null) {
                            out3 = m_resultclean2.group(1);
                            end2 = out3.length() + 1;
                        } else {
                            out2 = m_resultclean2.group(2);
                            str2 = out2;
                            end2 = out2.length();
                            out3 = str2;
                        }
                        start2 = m522.start(1) + (m522.group(1).length() - end2);
                        end3 = start2 + out3.length();
                        pattern3 = pSingle;
                        pattern2 = pCut;
                        outLen2 = end2;
                        start5 = start2;
                        city3 = city2;
                    } else {
                        pattern3 = pSingle;
                        pattern2 = pCut;
                        city3 = city2;
                        if (out2.length() > null) {
                            pSingle = p_big.matcher(out2);
                            if (pSingle.find()) {
                                if (pSingle.group(2) == null) {
                                    start5 += out2.length() - pSingle.group(1).length();
                                    out2 = pSingle.group(1);
                                } else {
                                    out2 = "";
                                }
                            }
                            mNot_road = pNot_road.matcher(out2);
                            if (mNot_road.find()) {
                                if (mNot_road.group(2) == null || mNot_road.group(2).length() <= 0) {
                                    start3 = "";
                                } else {
                                    start3 = out2.substring(mNot_road.group(1).length(), out2.length());
                                    start5 += mNot_road.group(1).length();
                                }
                                out2 = start3;
                            }
                            start3 = start5;
                            if (out2.length() > 0) {
                                nn.add(Integer.valueOf(start3));
                                nn.add(Integer.valueOf(end3));
                            }
                            pNot_road3 = pNot_road;
                            start5 = start3;
                            m283 = m28;
                            pCut = pattern2;
                            pSingle4 = pattern3;
                            pSingle3 = source;
                            pPre_road2 = p_big;
                            p_big = pattern;
                        } else {
                            pPre_road2 = p_big;
                            pNot_road3 = pNot_road;
                            m283 = m28;
                            p_big = pattern;
                            pCut = pattern2;
                            pSingle4 = pattern3;
                            pSingle3 = source;
                        }
                    }
                } else {
                    if (m522.group(6) != null) {
                        m_resultclean2 = this.p_resultclean.matcher(m522.group());
                        if (m_resultclean2.matches()) {
                            if (m_resultclean2.group(1) != null) {
                                city3 = m_resultclean2.group(1);
                                outLen = city3.length() + 1;
                            } else {
                                city3 = m_resultclean2.group(2);
                                outLen = city3.length();
                            }
                            start3 = m522.start() + (m522.group().length() - outLen);
                            pattern3 = pSingle;
                            pattern2 = pCut;
                            start5 = start3;
                            outLen2 = outLen;
                            end3 = city3.length() + start3;
                            out2 = city3;
                        } else {
                            pattern3 = pSingle;
                            pattern2 = pCut;
                        }
                        city3 = city2;
                    } else {
                        m_resultclean = pCut.matcher(m522.group(5));
                        if (!m_resultclean.matches()) {
                            cut = "";
                        } else if (m_resultclean.group(1) != null) {
                            cut = m_resultclean.group(1);
                        } else {
                            cut = "";
                        }
                        pattern2 = pCut;
                        city3 = searCity(m522.group(5).substring(cut.length(), m522.group(5).length()), 2);
                        String out4;
                        if (city3 == null) {
                            pCut = Pattern.compile("(?<![a-zA-Z])(?:\\s*[,.]*\\s*)*(?:(?i)in)(?![a-zA-Z])");
                            if (pCut.matcher(m522.group(3)).lookingAt()) {
                                pCut = this.p_resultclean.matcher(m522.group());
                                if (pCut.matches()) {
                                    if (pCut.group(1) != null) {
                                        out2 = pCut.group(1);
                                        start2 = out2.length() + 1;
                                    } else {
                                        out2 = pCut.group(2);
                                        start2 = out2.length();
                                    }
                                    outLen = m522.start() + (m522.group().length() - start2);
                                    pattern3 = pSingle;
                                    end3 = out2.length() + outLen;
                                    cut2 = cut;
                                    start5 = outLen;
                                    outLen2 = start2;
                                    m_resultclean2 = pCut;
                                } else {
                                    pattern3 = pSingle;
                                    cut2 = cut;
                                    pSingle4 = pCut;
                                }
                            } else {
                                Pattern pattern4 = pCut;
                                mNot_road = pSingle.matcher(m522.group(5));
                                if (mNot_road.matches()) {
                                    pattern3 = pSingle;
                                    pSingle = this.p_resultclean.matcher(m522.group());
                                    if (pSingle.matches()) {
                                        if (pSingle.group(1) != null) {
                                            out2 = pSingle.group(1);
                                            start2 = out2.length() + 1;
                                        } else {
                                            out2 = pSingle.group(2);
                                            start2 = out2.length();
                                        }
                                        outLen = m522.start() + (m522.group().length() - start2);
                                        end3 = out2.length() + outLen;
                                        cut2 = cut;
                                        start5 = outLen;
                                        outLen2 = start2;
                                        m_resultclean2 = pSingle;
                                    } else {
                                        cut2 = cut;
                                        pSingle4 = pSingle;
                                    }
                                } else {
                                    pattern3 = pSingle;
                                    pSingle = this.p_resultclean.matcher(m522.group(1));
                                    if (pSingle.matches()) {
                                        if (pSingle.group(1) != null) {
                                            out4 = pSingle.group(1);
                                            end2 = out4.length() + 1;
                                        } else {
                                            out2 = pSingle.group(2);
                                            str2 = out2;
                                            end2 = out2.length();
                                            out4 = str2;
                                        }
                                        start2 = m522.start(1) + (m522.group(1).length() - end2);
                                        end3 = start2 + out4.length();
                                        cut2 = cut;
                                        outLen2 = end2;
                                        start5 = start2;
                                        pSingle4 = pSingle;
                                        out2 = out4;
                                    } else {
                                        cut2 = cut;
                                        pSingle4 = pSingle;
                                    }
                                }
                            }
                        } else {
                            StringBuilder stringBuilder5;
                            pattern3 = pSingle;
                            pSingle = new StringBuilder(String.valueOf(cut));
                            pSingle.append(city3);
                            city3 = pSingle.toString();
                            if (m522.group(7) == null) {
                                if (m522.group(4) != null) {
                                    stringBuilder5 = new StringBuilder(String.valueOf(m522.group(4)));
                                    stringBuilder5.append(city3);
                                    city3 = stringBuilder5.toString();
                                }
                            } else if (m522.group(4) != null) {
                                stringBuilder5 = new StringBuilder(String.valueOf(m522.group(4)));
                                stringBuilder5.append(m522.group(5));
                                stringBuilder5.append(m522.group(7));
                                city3 = stringBuilder5.toString();
                            } else {
                                stringBuilder5 = new StringBuilder(String.valueOf(m522.group(5)));
                                stringBuilder5.append(m522.group(7));
                                city3 = stringBuilder5.toString();
                            }
                            pSingle = this.p_resultclean;
                            stringBuilder5 = new StringBuilder(String.valueOf(m522.group(1)));
                            stringBuilder5.append(m522.group(3));
                            stringBuilder5.append(city3);
                            m_resultclean2 = pSingle.matcher(stringBuilder5.toString());
                            if (m_resultclean2.matches() != null) {
                                int outLen3;
                                if (m_resultclean2.group(1) != null) {
                                    out4 = m_resultclean2.group(1);
                                    out3 = out4;
                                    outLen3 = out4.length() + 1;
                                } else {
                                    out3 = m_resultclean2.group(2);
                                    outLen3 = out3.length();
                                }
                                out2 = m522.start(1);
                                Matcher m_resultclean3 = m_resultclean2;
                                String cut3 = cut;
                                stringBuilder = new StringBuilder(String.valueOf(m522.group(1)));
                                stringBuilder.append(m522.group(3));
                                stringBuilder.append(city3);
                                out2 += stringBuilder.toString().length() - outLen3;
                                end3 = out3.length() + out2;
                                outLen2 = outLen3;
                                start5 = out2;
                                m_resultclean2 = m_resultclean3;
                                cut2 = cut3;
                            } else {
                                cut2 = cut;
                            }
                        }
                    }
                    if (out2.length() > null) {
                    }
                }
                out2 = out3;
                if (out2.length() > null) {
                }
            }
        }
        while (m_52sub.find()) {
            m52 = m522;
            pattern = p_big;
            m28 = m283;
            pSingle2 = pSingle4;
            pPre_road = pPre_road2;
            pNot_road2 = pNot_road3;
            out2 = "";
            m282 = this.pRoad.matcher(m_52sub.group());
            if (m282.matches() == null) {
                String out5;
                Matcher matcher2;
                if (m_52sub.group(5) == null) {
                    m522 = this.p_resultclean.matcher(m_52sub.group(1));
                    if (m522.matches()) {
                        int outLen4;
                        if (m522.group(1) != null) {
                            out5 = m522.group(1);
                            out = out5;
                            outLen4 = out5.length() + 1;
                        } else {
                            out = m522.group(2);
                            outLen4 = out.length();
                        }
                        start3 = m_52sub.start(1) + (m_52sub.group(1).length() - outLen4);
                        matcher2 = m282;
                        end3 = out.length() + start3;
                        outLen2 = outLen4;
                        out2 = out;
                        start5 = start3;
                        pSingle = pSingle2;
                        if (out2.length() <= 0) {
                            p_big = pPre_road;
                            m_resultclean = p_big.matcher(out2);
                            if (m_resultclean.find()) {
                                if (m_resultclean.group(2) == null) {
                                    start5 += out2.length() - m_resultclean.group(1).length();
                                    out2 = m_resultclean.group(1);
                                } else {
                                    out2 = "";
                                }
                            }
                            pNot_road = pNot_road2;
                            m_resultclean2 = pNot_road.matcher(out2);
                            if (m_resultclean2.find()) {
                                if (m_resultclean2.group(2) == null || m_resultclean2.group(2).length() <= 0) {
                                    start3 = "";
                                } else {
                                    start3 = out2.substring(m_resultclean2.group(1).length(), out2.length());
                                    start5 += m_resultclean2.group(1).length();
                                }
                                out2 = start3;
                            }
                            start3 = start5;
                            if (out2.length() > 0) {
                                nn.add(Integer.valueOf(start3));
                                nn.add(Integer.valueOf(end3));
                            }
                            pSingle4 = pSingle;
                            pNot_road3 = pNot_road;
                            start5 = start3;
                            m283 = m28;
                            m522 = m52;
                            pSingle3 = source;
                            pPre_road2 = p_big;
                            p_big = pattern;
                        } else {
                            pSingle4 = pSingle;
                            m283 = m28;
                            m522 = m52;
                            pNot_road3 = pNot_road2;
                            pPre_road2 = pPre_road;
                            p_big = pattern;
                            pSingle3 = source;
                        }
                    }
                } else if (m_52sub.group(6) != null) {
                    m522 = this.p_resultclean.matcher(m_52sub.group());
                    if (m522.matches()) {
                        if (m522.group(1) != null) {
                            out5 = m522.group(1);
                            start = out5.length() + 1;
                        } else {
                            out5 = m522.group(2);
                            start = out5.length();
                        }
                        start4 = m_52sub.start() + (m_52sub.group().length() - start);
                        matcher2 = m282;
                        start5 = start4;
                        out2 = out5;
                        outLen2 = start;
                        end3 = out5.length() + start4;
                        pSingle = pSingle2;
                        if (out2.length() <= 0) {
                        }
                    }
                } else {
                    m522 = pCut.matcher(m_52sub.group(5));
                    if (!m522.matches()) {
                        out5 = "";
                    } else if (m522.group(1) != null) {
                        out5 = m522.group(1);
                    } else {
                        out5 = "";
                    }
                    String city4 = searCity(m_52sub.group(5).substring(out5.length(), m_52sub.group(5).length()), 2);
                    Matcher matcher3;
                    if (city4 != null) {
                        StringBuilder stringBuilder6;
                        matcher3 = m522;
                        pSingle = pSingle2;
                        city3 = new StringBuilder(String.valueOf(out5));
                        city3.append(city4);
                        city3 = city3.toString();
                        if (m_52sub.group(7) == null) {
                            if (m_52sub.group(4) != null) {
                                stringBuilder6 = new StringBuilder(String.valueOf(m_52sub.group(4)));
                                stringBuilder6.append(city3);
                                city3 = stringBuilder6.toString();
                            }
                        } else if (m_52sub.group(4) != null) {
                            stringBuilder6 = new StringBuilder(String.valueOf(m_52sub.group(4)));
                            stringBuilder6.append(m_52sub.group(5));
                            stringBuilder6.append(m_52sub.group(7));
                            city3 = stringBuilder6.toString();
                        } else {
                            stringBuilder6 = new StringBuilder(String.valueOf(m_52sub.group(5)));
                            stringBuilder6.append(m_52sub.group(7));
                            city3 = stringBuilder6.toString();
                        }
                        m522 = this.p_resultclean;
                        stringBuilder6 = new StringBuilder(String.valueOf(m_52sub.group(1)));
                        stringBuilder6.append(m_52sub.group(3));
                        stringBuilder6.append(city3);
                        m522 = m522.matcher(stringBuilder6.toString());
                        Matcher m_resultclean4;
                        if (m522.matches()) {
                            if (m522.group(1) != null) {
                                out = m522.group(1);
                                cut = out;
                                start = out.length() + 1;
                            } else {
                                cut = m522.group(2);
                                start = cut.length();
                            }
                            outLen = m_52sub.start(1);
                            m_resultclean4 = m522;
                            StringBuilder stringBuilder7 = new StringBuilder(String.valueOf(m_52sub.group(1)));
                            stringBuilder7.append(m_52sub.group(3));
                            stringBuilder7.append(city3);
                            outLen += stringBuilder7.toString().length() - start;
                            city2 = city3;
                            end3 = cut.length() + outLen;
                            cut2 = out5;
                            outLen2 = start;
                            out2 = cut;
                            start5 = outLen;
                            m522 = m_resultclean4;
                        } else {
                            m_resultclean4 = m522;
                            city2 = city3;
                            cut2 = out5;
                        }
                    } else if (Pattern.compile("(?<![a-zA-Z])(?:\\s*[,.]*\\s*)*(?:(?i)in)(?![a-zA-Z])").matcher(m_52sub.group(3)).lookingAt()) {
                        m_resultclean = this.p_resultclean.matcher(m_52sub.group());
                        if (m_resultclean.matches()) {
                            if (m_resultclean.group(1) != null) {
                                out2 = m_resultclean.group(1);
                                start2 = out2.length() + 1;
                            } else {
                                out2 = m_resultclean.group(2);
                                start2 = out2.length();
                            }
                            outLen = m_52sub.start() + (m_52sub.group().length() - start2);
                            end3 = out2.length() + outLen;
                            matcher2 = m282;
                            city2 = city4;
                            cut2 = out5;
                            start5 = outLen;
                            outLen2 = start2;
                            pSingle = pSingle2;
                            m522 = m_resultclean;
                        } else {
                            matcher2 = m282;
                            city2 = city4;
                            cut2 = out5;
                            pSingle = pSingle2;
                            m522 = m_resultclean;
                        }
                    } else {
                        matcher2 = m282;
                        pSingle = pSingle2;
                        m_resultclean = pSingle.matcher(m_52sub.group(5));
                        if (m_resultclean.matches()) {
                            m_resultclean = this.p_resultclean.matcher(m_52sub.group());
                            if (m_resultclean.matches()) {
                                if (m_resultclean.group(1) != null) {
                                    out2 = m_resultclean.group(1);
                                    start2 = out2.length() + 1;
                                } else {
                                    out2 = m_resultclean.group(2);
                                    start2 = out2.length();
                                }
                                outLen = m_52sub.start() + (m_52sub.group().length() - start2);
                                end3 = out2.length() + outLen;
                                city2 = city4;
                                cut2 = out5;
                                start5 = outLen;
                                outLen2 = start2;
                                m522 = m_resultclean;
                            } else {
                                city2 = city4;
                                cut2 = out5;
                                m522 = m_resultclean;
                            }
                        } else {
                            Matcher matcher4 = m_resultclean;
                            matcher3 = m522;
                            m522 = this.p_resultclean.matcher(m_52sub.group(1));
                            if (m522.matches()) {
                                if (m522.group(1) != null) {
                                    city3 = m522.group(1);
                                    end2 = city3.length() + 1;
                                } else {
                                    city3 = m522.group(2);
                                    end2 = city3.length();
                                }
                                start2 = m_52sub.start(1) + (m_52sub.group(1).length() - end2);
                                end3 = start2 + city3.length();
                                city2 = city4;
                                cut2 = out5;
                                outLen2 = end2;
                                start5 = start2;
                                out2 = city3;
                            } else {
                                Matcher m_resultclean5 = m522;
                                city2 = city4;
                                cut2 = out5;
                            }
                        }
                    }
                    if (out2.length() <= 0) {
                    }
                }
                pSingle = pSingle2;
                if (out2.length() <= 0) {
                }
            } else {
                m283 = m28;
                m522 = m52;
                pNot_road3 = pNot_road2;
                pPre_road2 = pPre_road;
                pSingle4 = pSingle2;
                p_big = pattern;
                pSingle3 = source;
            }
        }
        while (m1346.find()) {
            m52 = m522;
            m28 = m283;
            pSingle2 = pSingle4;
            pPre_road = pPre_road2;
            pNot_road2 = pNot_road3;
            int end4 = end3;
            String out6 = out2;
            m522 = p_big.matcher(m1346.group());
            if (m522.find()) {
                start = m1346.start();
                String[] buildings2 = new String[8];
                this.match_index_2.clear();
                String[] buildings3 = searBuilding(m1346.group(), start);
                if (buildings3 != null) {
                    Iterator<Integer> it = this.match_index_2.iterator();
                    end2 = 0;
                    int length_bui2;
                    for (outLen = buildings3.length; end2 < outLen; outLen = length_bui2) {
                        if (buildings3[end2] == null) {
                            matcher = m522;
                            pattern = p_big;
                            strArr = buildings3;
                            break;
                        }
                        m283 = this.p_resultclean.matcher(buildings3[end2]);
                        if (m283.matches()) {
                            matcher = m522;
                            if (m283.group(1) != null) {
                                city3 = m283.group(1);
                                start2 = city3.length() + 1;
                                m522 = buildings3[end2].length() - start2;
                            } else {
                                city3 = m283.group(2);
                                start2 = city3.length();
                                m522 = buildings3[end2].length() - start2;
                            }
                            pattern = p_big;
                            Matcher mNum = this.pNum.matcher(city3);
                            if (mNum.lookingAt()) {
                                strArr = buildings3;
                                m522 += mNum.group().length();
                                m283 = city3.substring(mNum.group().length(), city3.length());
                            } else {
                                strArr = buildings3;
                                m283 = city3;
                            }
                            if (it.hasNext()) {
                                start3 = ((Integer) it.next()).intValue();
                                m522 += start3;
                                p_big = m522 + m283.length();
                                try {
                                    city3 = pSingle3.substring(m522, p_big);
                                    try {
                                        Matcher mDir = this.pDir.matcher(city3);
                                        if (mDir.lookingAt()) {
                                            m283 = city3;
                                            String str3 = city3;
                                            Matcher matcher5 = mDir;
                                            length_bui2 = outLen;
                                        } else {
                                            pPre_road2 = Pattern.compile("((?:(?:[a-z][A-Za-z0-9]*)(?:\\s+|\\s*[,.]\\s*))+)([\\s\\S]+)");
                                            m_resultclean = pPre_road2.matcher(city3);
                                            if (m_resultclean.matches()) {
                                                length_bui2 = outLen;
                                                try {
                                                    m522 += m_resultclean.group(1).length();
                                                    m283 = m283.substring(m_resultclean.group(1).length(), m283.length());
                                                } catch (Exception e2) {
                                                    e = e2;
                                                    buildings3 = System.out;
                                                    stringBuilder3 = new StringBuilder(String.valueOf(m522));
                                                    stringBuilder3.append("**");
                                                    stringBuilder3.append(p_big);
                                                    buildings3.println(stringBuilder3.toString());
                                                    start5 = m522;
                                                    end4 = p_big;
                                                    out6 = m283;
                                                    end2++;
                                                    m522 = matcher;
                                                    p_big = pattern;
                                                    buildings3 = strArr;
                                                }
                                            } else {
                                                length_bui2 = outLen;
                                            }
                                        }
                                        if (!pBuilding.matcher(m283).matches()) {
                                            nn.add(Integer.valueOf(m522));
                                            nn.add(Integer.valueOf(p_big));
                                        }
                                    } catch (Exception e3) {
                                        e = e3;
                                        length_bui2 = outLen;
                                        buildings3 = System.out;
                                        stringBuilder3 = new StringBuilder(String.valueOf(m522));
                                        stringBuilder3.append("**");
                                        stringBuilder3.append(p_big);
                                        buildings3.println(stringBuilder3.toString());
                                        start5 = m522;
                                        end4 = p_big;
                                        out6 = m283;
                                        end2++;
                                        m522 = matcher;
                                        p_big = pattern;
                                        buildings3 = strArr;
                                    }
                                } catch (Exception e4) {
                                    e = e4;
                                    int i = start3;
                                    length_bui2 = outLen;
                                    buildings3 = System.out;
                                    stringBuilder3 = new StringBuilder(String.valueOf(m522));
                                    stringBuilder3.append("**");
                                    stringBuilder3.append(p_big);
                                    buildings3.println(stringBuilder3.toString());
                                    start5 = m522;
                                    end4 = p_big;
                                    out6 = m283;
                                    end2++;
                                    m522 = matcher;
                                    p_big = pattern;
                                    buildings3 = strArr;
                                }
                                start5 = m522;
                                end4 = p_big;
                                out6 = m283;
                            } else {
                                length_bui2 = outLen;
                                Matcher matcher6 = m522;
                                out6 = m283;
                                outLen2 = start2;
                            }
                        } else {
                            matcher = m522;
                            pattern = p_big;
                            Matcher matcher7 = m283;
                            strArr = buildings3;
                            length_bui2 = outLen;
                        }
                        end2++;
                        m522 = matcher;
                        p_big = pattern;
                        buildings3 = strArr;
                    }
                    matcher = m522;
                    pattern = p_big;
                    strArr = buildings3;
                    end3 = end4;
                    out2 = out6;
                } else {
                    matcher = m522;
                    pattern = p_big;
                    strArr = buildings3;
                    end3 = end4;
                    out2 = out6;
                }
                this.match_index_2.clear();
                m522 = searSpot(m1346.group(), start);
                if (m522 != null) {
                    start4 = m522.length;
                    Iterator<Integer> it2 = this.match_index_2.iterator();
                    start3 = 0;
                    while (start3 < start4 && m522[start3] != null) {
                        Matcher m_resultclean6 = this.p_resultclean.matcher(m522[start3]);
                        if (m_resultclean6.matches()) {
                            length_bui = start4;
                            if (m_resultclean6.group(1) != null) {
                                city3 = m_resultclean6.group(1);
                                end2 = city3.length() + 1;
                                outLen2 = end2;
                                end2 = m522[start3].length() - end2;
                            } else {
                                city3 = m_resultclean6.group(2);
                                start4 = city3.length();
                                end2 = m522[start3].length() - start4;
                                outLen2 = start4;
                            }
                            start4 = this.pNum.matcher(city3);
                            if (start4.lookingAt()) {
                                buildings = m522;
                                head = start;
                                start5 = end2 + start4.group().length();
                                out2 = city3.substring(start4.group().length(), city3.length());
                            } else {
                                buildings = m522;
                                head = start;
                                start5 = end2;
                                out2 = city3;
                            }
                            if (it2.hasNext()) {
                                m522 = ((Integer) it2.next()).intValue();
                                start = m522 + start5;
                                end3 = start + out2.length();
                                Matcher matcher8;
                                try {
                                    city3 = pSingle3.substring(start, end3);
                                    m282 = this.pDir.matcher(city3);
                                    if (m282.lookingAt()) {
                                        out2 = city3;
                                        String str4 = city3;
                                        Matcher matcher9 = m282;
                                        matcher8 = m522;
                                    } else {
                                        pSingle = Pattern.compile("((?:(?:[a-z][A-Za-z0-9]*)(?:\\s+|\\s*[,.]\\s*))+)([\\s\\S]+)");
                                        m_resultclean = pSingle.matcher(city3);
                                        int first;
                                        if (m_resultclean.matches()) {
                                            first = m522;
                                            try {
                                                start += m_resultclean.group(1).length();
                                                out2 = out2.substring(m_resultclean.group(1).length(), out2.length());
                                            } catch (Exception e5) {
                                                e = e5;
                                                printStream = System.out;
                                                m522 = new StringBuilder(String.valueOf(start));
                                                m522.append("**");
                                                m522.append(end3);
                                                printStream.println(m522.toString());
                                                start5 = start;
                                                start3++;
                                                start4 = length_bui;
                                                m522 = buildings;
                                                start = head;
                                                pSingle3 = source;
                                            }
                                        } else {
                                            first = m522;
                                        }
                                    }
                                    if (!pBuilding.matcher(out2).matches()) {
                                        nn.add(Integer.valueOf(start));
                                        nn.add(Integer.valueOf(end3));
                                    }
                                } catch (Exception e6) {
                                    e = e6;
                                    matcher8 = m522;
                                    printStream = System.out;
                                    m522 = new StringBuilder(String.valueOf(start));
                                    m522.append("**");
                                    m522.append(end3);
                                    printStream.println(m522.toString());
                                    start5 = start;
                                    start3++;
                                    start4 = length_bui;
                                    m522 = buildings;
                                    start = head;
                                    pSingle3 = source;
                                }
                                start5 = start;
                            }
                        } else {
                            buildings = m522;
                            length_bui = start4;
                            head = start;
                        }
                        start3++;
                        start4 = length_bui;
                        m522 = buildings;
                        start = head;
                        pSingle3 = source;
                    }
                    m283 = m28;
                    m522 = m52;
                    pNot_road3 = pNot_road2;
                    pPre_road2 = pPre_road;
                    pSingle4 = pSingle2;
                    p_big = pattern;
                    pSingle3 = source;
                } else {
                    m283 = m28;
                    m522 = m52;
                    pNot_road3 = pNot_road2;
                    pPre_road2 = pPre_road;
                    pSingle4 = pSingle2;
                    p_big = pattern;
                }
            } else {
                end3 = end4;
                out2 = out6;
                m283 = m28;
                m522 = m52;
                pNot_road3 = pNot_road2;
                pPre_road2 = pPre_road;
                pSingle4 = pSingle2;
            }
        }
        int num2 = nn.size();
        end3 = new int[num2];
        out2 = 0;
        while (out2 < num2) {
            m52 = m522;
            m28 = m283;
            pSingle2 = pSingle4;
            pPre_road = pPre_road2;
            pNot_road2 = pNot_road3;
            end3[out2] = ((Integer) nn.get(out2)).intValue();
            out2++;
            m283 = m28;
            m522 = m52;
        }
        if (num2 > 4) {
            end2 = new int[num2];
            num = 0;
            m283 = 1;
            while (true) {
                m52 = m522;
                if (m283 >= (num2 - 1) / 2) {
                    break;
                }
                pSingle2 = pSingle4;
                pPre_road = pPre_road2;
                pNot_road2 = pNot_road3;
                for (m522 = m283 + 1; m522 < (num2 + 1) / 2; m522++) {
                    if (end3[(m283 * 2) - 1] > end3[(m522 * 2) - 1]) {
                        start = (m283 * 2) - 1;
                        end3[start] = end3[start] + end3[(m522 * 2) - 1];
                        end3[(m522 * 2) - 1] = end3[(m283 * 2) - 1] - end3[(m522 * 2) - 1];
                        end3[(m283 * 2) - 1] = end3[(m283 * 2) - 1] - end3[(m522 * 2) - 1];
                        start = m283 * 2;
                        end3[start] = end3[start] + end3[m522 * 2];
                        end3[m522 * 2] = end3[m283 * 2] - end3[m522 * 2];
                        end3[m283 * 2] = end3[m283 * 2] - end3[m522 * 2];
                    }
                }
                m283++;
                m522 = m52;
                pNot_road3 = pNot_road2;
                pPre_road2 = pPre_road;
                pSingle4 = pSingle2;
            }
            m522 = true;
            while (m522 < (num2 + 1) / 2) {
                num++;
                end2[(num * 2) - 1] = end3[(m522 * 2) - 1];
                end2[num * 2] = end3[m522 * 2];
                m283 = m522 + 1;
                while (true) {
                    pNot_road2 = pNot_road3;
                    if (m283 < (num2 + 1) / 2) {
                        pPre_road = pPre_road2;
                        if (end3[m522 * 2] < end3[(m283 * 2) - 1]) {
                            pSingle2 = pSingle4;
                            m522 = m283 - 1;
                            break;
                        }
                        pSingle2 = pSingle4;
                        end3[m522 * 2] = max(end3[m522 * 2], end3[m283 * 2]);
                        end2[num * 2] = end3[m522 * 2];
                        if (m283 == ((num2 + 1) / 2) - 1) {
                            m522 = m283;
                        }
                        m283++;
                        pNot_road3 = pNot_road2;
                        pPre_road2 = pPre_road;
                        pSingle4 = pSingle2;
                    } else {
                        pSingle2 = pSingle4;
                        pPre_road = pPre_road2;
                        break;
                    }
                }
                m522++;
                pNot_road3 = pNot_road2;
                pPre_road2 = pPre_road;
                pSingle4 = pSingle2;
            }
            end3[0] = num;
            end2[0] = num;
            return createAddressResultData(end2, pSingle3);
        }
        m28 = m283;
        pSingle2 = pSingle4;
        pPre_road = pPre_road2;
        pNot_road2 = pNot_road3;
        end3[0] = (num2 - 1) / 2;
        return createAddressResultData(end3, pSingle3);
    }

    private ArrayList<Match> createAddressResultData(int[] addrArray, String source) {
        if (addrArray.length == 0) {
            return null;
        }
        ArrayList<Match> matchedList = new ArrayList();
        int count = addrArray[0];
        for (int i = 1; i < (count * 2) + 1; i += 2) {
            Match mu = new Match();
            mu.setMatchedAddr(source.substring(addrArray[i], addrArray[i + 1]));
            mu.setStartPos(Integer.valueOf(addrArray[i]));
            mu.setEndPos(Integer.valueOf(addrArray[i + 1]));
            matchedList.add(mu);
        }
        return sortAndMergePosList(matchedList, source);
    }

    private String[] searSpot(String string, int head) {
        int length = string.length();
        int head_0 = head;
        int i = 8;
        String[] results = new String[8];
        int count = string;
        Pattern pCut = Pattern.compile("(\\s*[,.]?\\s*(?:(?i)(?:in|on|at|from|of)\\s+)?(?:(?i)(uptown|downtown)\\s+)?)?[\\s\\S]*");
        Pattern pSingle = Pattern.compile("(?:\\.)?\\s*,\\s*[A-Z][a-z]+(?:\\s*(?:[,.)\"'])\\s*)*");
        Pattern pPre_city = Pattern.compile("(?:\\s*(?:,|\\.){0,2}\\s*\\b(?i)(?:in)\\b(.*))");
        int full = count.length();
        int length_bracket = 0;
        int count2 = 0;
        int index = 0;
        String cut = "";
        String building = "";
        String s_right = "";
        String city = "";
        while (index < length) {
            int length2;
            String str;
            int position;
            count = count.substring(index, length);
            int head2 = head_0 + (full - count.length());
            length -= index;
            index = 0;
            int head_02 = head_0;
            int position2 = DicSearch.dicsearch(2, count.toLowerCase(Locale.getDefault()));
            if (position2 == 0) {
                while (index < length && ((count.charAt(index) >= 'a' && count.charAt(index) <= 'z') || ((count.charAt(index) >= 'A' && count.charAt(index) <= 'Z') || (count.charAt(index) >= '0' && count.charAt(index) <= '9')))) {
                    index++;
                }
                length2 = length;
                str = count;
                position = position2;
                count = count2;
                count2 = 1;
            } else {
                StringBuilder stringBuilder;
                String cut2;
                int length_bracket2;
                building = count.substring(0, position2);
                String s_right2 = count.substring(position2, count.length());
                int length_bracket3 = searchBracket(s_right2);
                if (length_bracket3 > 0) {
                    length2 = length;
                    stringBuilder = new StringBuilder(String.valueOf(building));
                    stringBuilder.append(s_right2.substring(0, length_bracket3));
                    building = stringBuilder.toString();
                    s_right2 = s_right2.substring(length_bracket3, s_right2.length());
                } else {
                    length2 = length;
                }
                String cut3 = "";
                Matcher m52s = this.p52s.matcher(s_right2);
                String city2 = "";
                StringBuilder stringBuilder2;
                String s_right3;
                StringBuilder stringBuilder3;
                if (!m52s.lookingAt()) {
                    cut2 = cut3;
                    length_bracket2 = length_bracket3;
                    str = count;
                    position = position2;
                    Matcher m2s = this.p2s.matcher(s_right2);
                    if (m2s.lookingAt()) {
                        if (m2s.group(3) == null) {
                            i = count2 + 1;
                            results[count2] = building;
                            this.match_index_2.add(Integer.valueOf(head2));
                        } else if (m2s.group(4) != null) {
                            i = count2 + 1;
                            stringBuilder2 = new StringBuilder(String.valueOf(building));
                            stringBuilder2.append(m2s.group());
                            results[count2] = stringBuilder2.toString();
                            this.match_index_2.add(Integer.valueOf(head2));
                        } else {
                            Matcher mCut = pCut.matcher(m2s.group(3));
                            if (!mCut.matches()) {
                                s_right = "";
                            } else if (mCut.group(1) != null) {
                                s_right = mCut.group(1);
                            } else {
                                s_right = "";
                            }
                            s_right3 = s_right2;
                            s_right2 = searCity(m2s.group(3).substring(s_right.length(), m2s.group(3).length()), 2);
                            if (s_right2 != null) {
                                StringBuilder stringBuilder4;
                                stringBuilder = new StringBuilder(String.valueOf(s_right));
                                stringBuilder.append(s_right2);
                                s_right2 = stringBuilder.toString();
                                if (m2s.group(6) == null) {
                                    if (m2s.group(2) != null) {
                                        stringBuilder4 = new StringBuilder(String.valueOf(m2s.group(2)));
                                        stringBuilder4.append(s_right2);
                                        s_right2 = stringBuilder4.toString();
                                    }
                                } else if (m2s.group(2) == null) {
                                    stringBuilder = new StringBuilder(String.valueOf(m2s.group(3)));
                                    stringBuilder.append(m2s.group(5));
                                    stringBuilder.append(m2s.group(6));
                                    s_right2 = stringBuilder.toString();
                                } else {
                                    stringBuilder = new StringBuilder(String.valueOf(m2s.group(2)));
                                    stringBuilder.append(m2s.group(3));
                                    stringBuilder.append(m2s.group(5));
                                    stringBuilder.append(m2s.group(6));
                                    s_right2 = stringBuilder.toString();
                                }
                                stringBuilder = new StringBuilder(String.valueOf(m2s.group(1)));
                                stringBuilder.append(s_right2);
                                s_right2 = stringBuilder.toString();
                                i = count2 + 1;
                                stringBuilder4 = new StringBuilder(String.valueOf(building));
                                stringBuilder4.append(s_right2);
                                results[count2] = stringBuilder4.toString();
                                this.match_index_2.add(Integer.valueOf(head2));
                                city2 = s_right2;
                                count = i;
                                cut2 = s_right;
                            } else {
                                String city3;
                                if (pPre_city.matcher(m2s.group(1)).matches()) {
                                    count = count2 + 1;
                                    city3 = s_right2;
                                    stringBuilder3 = new StringBuilder(String.valueOf(building));
                                    stringBuilder3.append(m2s.group());
                                    results[count2] = stringBuilder3.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                } else {
                                    city3 = s_right2;
                                    s_right2 = pSingle.matcher(m2s.group(3));
                                    String mSingle;
                                    if (s_right2.matches()) {
                                        count = count2 + 1;
                                        mSingle = s_right2;
                                        stringBuilder3 = new StringBuilder(String.valueOf(building));
                                        stringBuilder3.append(m2s.group());
                                        results[count2] = stringBuilder3.toString();
                                        this.match_index_2.add(Integer.valueOf(head2));
                                    } else {
                                        mSingle = s_right2;
                                        s_right2 = count2 + 1;
                                        results[count2] = building;
                                        this.match_index_2.add(Integer.valueOf(head2));
                                        count = s_right2;
                                    }
                                }
                                cut2 = s_right;
                                city2 = city3;
                            }
                        }
                        s_right3 = s_right2;
                        count = i;
                    } else {
                        s_right3 = s_right2;
                        head_0 = count2 + 1;
                        results[count2] = building;
                        this.match_index_2.add(Integer.valueOf(head2));
                        count = head_0;
                    }
                } else if (m52s.group(6) == null) {
                    int count3 = count2 + 1;
                    cut2 = cut3;
                    StringBuilder stringBuilder5 = new StringBuilder(String.valueOf(building));
                    stringBuilder5.append(m52s.group());
                    results[count2] = stringBuilder5.toString();
                    this.match_index_2.add(Integer.valueOf(head2));
                    s_right3 = s_right2;
                    length_bracket2 = length_bracket3;
                    str = count;
                    position = position2;
                    count = count3;
                } else {
                    cut2 = cut3;
                    if (m52s.group(7) != null) {
                        int count4 = count2 + 1;
                        cut3 = new StringBuilder(String.valueOf(building));
                        cut3.append(m52s.group());
                        results[count2] = cut3.toString();
                        this.match_index_2.add(Integer.valueOf(head2));
                        s_right3 = s_right2;
                        length_bracket2 = length_bracket3;
                        str = count;
                        position = position2;
                        count = count4;
                    } else {
                        String cut4;
                        cut3 = pCut.matcher(m52s.group(6));
                        if (!cut3.matches()) {
                            cut4 = "";
                        } else if (cut3.group(1) != null) {
                            cut4 = cut3.group(1);
                        } else {
                            cut4 = "";
                        }
                        String mCut2 = cut3;
                        length_bracket2 = length_bracket3;
                        str = count;
                        position = position2;
                        cut3 = searCity(m52s.group(6).substring(cut4.length(), m52s.group(6).length()), 2);
                        if (cut3 == null) {
                            Matcher mPre_city = pPre_city.matcher(m52s.group(4));
                            if (mPre_city.matches() != 0) {
                                count = count2 + 1;
                                stringBuilder3 = new StringBuilder(String.valueOf(building));
                                stringBuilder3.append(m52s.group());
                                results[count2] = stringBuilder3.toString();
                                this.match_index_2.add(Integer.valueOf(head2));
                            } else {
                                Matcher matcher = mPre_city;
                                mPre_city = pSingle.matcher(m52s.group(3));
                                if (mPre_city.matches() != 0) {
                                    count = count2 + 1;
                                    stringBuilder3 = new StringBuilder(String.valueOf(building));
                                    stringBuilder3.append(m52s.group());
                                    results[count2] = stringBuilder3.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                } else {
                                    length_bracket3 = count2 + 1;
                                    count = new StringBuilder(String.valueOf(building));
                                    int count5 = length_bracket3;
                                    count.append(m52s.group(1));
                                    count.append(m52s.group(2));
                                    results[count2] = count.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                    cut2 = cut4;
                                    s_right3 = s_right2;
                                    city2 = cut3;
                                    count = count5;
                                }
                            }
                            cut2 = cut4;
                            s_right3 = s_right2;
                            city2 = cut3;
                        } else {
                            String cut5;
                            stringBuilder2 = new StringBuilder(String.valueOf(cut4));
                            stringBuilder2.append(cut3);
                            cut3 = stringBuilder2.toString();
                            if (m52s.group(8) == 0) {
                                if (m52s.group(5) != 0) {
                                    count = new StringBuilder(String.valueOf(m52s.group(5)));
                                    count.append(cut3);
                                    cut3 = count.toString();
                                }
                            } else if (m52s.group(5) == 0) {
                                stringBuilder2 = new StringBuilder(String.valueOf(m52s.group(6)));
                                stringBuilder2.append(m52s.group(8));
                                cut3 = stringBuilder2.toString();
                            } else {
                                stringBuilder2 = new StringBuilder(String.valueOf(m52s.group(5)));
                                stringBuilder2.append(m52s.group(6));
                                stringBuilder2.append(m52s.group(8));
                                cut3 = stringBuilder2.toString();
                                length_bracket3 = count2 + 1;
                                stringBuilder3 = new StringBuilder(String.valueOf(building));
                                cut5 = cut4;
                                stringBuilder3.append(m52s.group(1));
                                stringBuilder3.append(m52s.group(2));
                                stringBuilder3.append(m52s.group(4));
                                stringBuilder3.append(cut3);
                                results[count2] = stringBuilder3.toString();
                                this.match_index_2.add(Integer.valueOf(head2));
                                s_right3 = s_right2;
                                city2 = cut3;
                                count = length_bracket3;
                                cut2 = cut5;
                            }
                            length_bracket3 = count2 + 1;
                            stringBuilder3 = new StringBuilder(String.valueOf(building));
                            cut5 = cut4;
                            stringBuilder3.append(m52s.group(1));
                            stringBuilder3.append(m52s.group(2));
                            stringBuilder3.append(m52s.group(4));
                            stringBuilder3.append(cut3);
                            results[count2] = stringBuilder3.toString();
                            this.match_index_2.add(Integer.valueOf(head2));
                            s_right3 = s_right2;
                            city2 = cut3;
                            count = length_bracket3;
                            cut2 = cut5;
                        }
                    }
                }
                count2 = 1;
                index = (results[count - 1].length() + 0) - 1;
                cut = cut2;
                length_bracket = length_bracket2;
            }
            index += count2;
            count2 = count;
            head_0 = head_02;
            length = length2;
            count = str;
            position2 = position;
            i = 8;
        }
        if (count2 >= i) {
            return results;
        }
        index = new String[count2];
        for (i = 0; i < count2; i++) {
            index[i] = results[i];
        }
        return index;
    }

    private int max(int i, int j) {
        if (i > j) {
            return i;
        }
        return j;
    }

    public String[] searBuilding(String string, int head) {
        String sub_left = "";
        boolean flag = true;
        if (stanWri(string)) {
            flag = false;
        }
        return searBuilding_suf(string, sub_left, 0, flag, head);
    }

    /* JADX WARNING: Removed duplicated region for block: B:131:0x05b8  */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x049f  */
    /* JADX WARNING: Removed duplicated region for block: B:177:0x07ed  */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x07b8  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x08dd  */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x08b2  */
    /* JADX WARNING: Removed duplicated region for block: B:219:0x0a54  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:225:0x0a8b  */
    /* JADX WARNING: Removed duplicated region for block: B:231:0x0a9d  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x0abd A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:237:0x0ab0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String[] searBuilding_suf(String str, String sub_left, int left_state, boolean flag, int head) {
        String str2;
        int head2;
        String[] results_2;
        String[] results_3;
        String str3 = str;
        int i = left_state;
        String cut = "";
        String[] results = new String[8];
        String[] results_22 = new String[0];
        String[] results_32 = new String[0];
        int count = 0;
        String sub1 = "";
        String sub2 = "";
        String sub_right = "";
        String building = "";
        String city = "";
        Matcher mLocation = this.pNot_1.matcher(str3);
        if (mLocation.lookingAt()) {
            mLocation = this.pNot_2.matcher(mLocation.group(1));
            if (mLocation.lookingAt()) {
                int count2;
                int head3 = mLocation.group().length();
                str2 = str3.substring(head3, str.length());
                head2 = head + head3;
                mLocation = this.pLocation.matcher(str2);
                String[] strArr;
                String[] results_33;
                String str4;
                StringBuilder stringBuilder;
                String str5;
                String str6;
                int left_state2;
                if (mLocation.find()) {
                    strArr = results_22;
                    results_33 = results_32;
                    str4 = sub_right;
                    int head4 = head2;
                    if (i == 1) {
                        stringBuilder = new StringBuilder(String.valueOf(sub_left));
                        stringBuilder.append(str2);
                        str2 = stringBuilder.toString();
                    }
                    str5 = sub1;
                    str6 = sub2;
                    int i2 = i;
                    results_2 = searBuilding_dic(str2, head4 - sub_left.length());
                    results_3 = results_33;
                    left_state2 = i2;
                } else {
                    sub1 = mLocation.group(1);
                    Matcher mNo = this.pNo.matcher(sub1);
                    String str7;
                    String sub_right2;
                    int left_state3;
                    if (sub1.length() <= 0 || !noBlank(sub1)) {
                        str7 = cut;
                        strArr = results_22;
                        results_33 = results_32;
                        str4 = sub_right;
                        cut = sub1;
                        results_32 = head2;
                        sub_right = mLocation.group();
                        head2 = str2.substring(sub_right.length(), str2.length());
                        if (noBlank(head2)) {
                            sub_right2 = head2;
                            results_2 = searBuilding_suf(head2, sub_right, 1, flag, results_32 + (str2.length() - head2.length()));
                            str5 = cut;
                            str6 = sub2;
                            results_3 = results_33;
                            str4 = sub_right2;
                            results_33 = 1;
                            left_state3 = sub_right;
                        } else {
                            str5 = cut;
                            str6 = sub2;
                            results_2 = strArr;
                            results_3 = results_33;
                            Object obj = head2;
                            results_33 = 1;
                            left_state3 = sub_right;
                        }
                    } else {
                        Matcher mComma;
                        Matcher mComma2;
                        if (!mNo.matches()) {
                            strArr = results_22;
                            results_33 = results_32;
                            cut = sub1;
                            results_22 = mLocation;
                            results_32 = head2;
                        } else if (mLocation.group(3) != null) {
                            str7 = cut;
                            strArr = results_22;
                            results_33 = results_32;
                            cut = sub1;
                            results_22 = mLocation;
                            results_32 = head2;
                        } else {
                            mNo = mLocation.group();
                            sub_right = str2.substring(mNo.length(), str2.length());
                            if (noBlank(sub_right)) {
                                Matcher sub_left2 = mNo;
                                cut = sub1;
                                results_22 = mLocation;
                                results_33 = results_32;
                                results_32 = head2;
                                results_2 = searBuilding_suf(sub_right, mNo, 1, flag, head2 + (str2.length() - sub_right.length()));
                                str5 = cut;
                                str6 = sub2;
                                str4 = sub_right;
                                results_3 = results_33;
                                results_33 = 1;
                                left_state3 = sub_left2;
                            } else {
                                str7 = cut;
                                strArr = results_22;
                                results_33 = results_32;
                                results_22 = mLocation;
                                results_32 = head2;
                                str4 = sub_right;
                                results_2 = strArr;
                                results_3 = results_33;
                                left_state2 = 1;
                                left_state3 = mNo;
                            }
                        }
                        Matcher mComma3 = this.pComma.matcher(cut);
                        String[] temp;
                        int length;
                        if (mComma3.find()) {
                            sub2 = mComma3.group(1);
                            if (sub2 != null && noBlank(sub2) && divStr(sub2).length <= 4) {
                                mNo = new StringBuilder(String.valueOf(sub2));
                                mNo.append(results_22.group(2));
                                building = mNo.toString();
                                this.match_index_2.add(Integer.valueOf(results_32 + mComma3.start(1)));
                            }
                            if (building.length() == 0 && flag) {
                                boolean sub1_undone;
                                String sub22;
                                str3 = cut;
                                mNo = true;
                                while (mNo != null) {
                                    sub1_undone = mNo;
                                    mComma = mComma3;
                                    sub22 = sub2;
                                    mNo = this.pPre_uni.matcher(str3);
                                    String sub1_temp;
                                    if (mNo.find()) {
                                        sub2 = mNo.group(2);
                                        if (sub2 == null || !noBlank(sub2)) {
                                            sub1_temp = str3;
                                            str3 = null;
                                        } else if (divStr(sub2).length <= 4) {
                                            StringBuilder stringBuilder2 = new StringBuilder(String.valueOf(sub2));
                                            sub1_temp = str3;
                                            stringBuilder2.append(results_22.group(2));
                                            building = stringBuilder2.toString();
                                            this.match_index_2.add(Integer.valueOf(results_32 + (cut.length() - sub2.length())));
                                            str3 = null;
                                        } else {
                                            str3 = sub2;
                                            mNo = sub1_undone;
                                            mComma3 = mComma;
                                        }
                                        mNo = str3;
                                        mComma3 = mComma;
                                        str3 = sub1_temp;
                                    } else {
                                        sub1_temp = str3;
                                        mNo = null;
                                        mComma3 = mComma;
                                        sub2 = sub22;
                                    }
                                }
                                if (building.length() == 0) {
                                    temp = divStr(cut);
                                    length = temp.length;
                                    sub1_undone = mNo;
                                    if (length > 4) {
                                        mComma = mComma3;
                                        mNo = new StringBuilder(String.valueOf(temp[length - 4]));
                                        mNo.append(temp[length - 3]);
                                        mNo.append(temp[length - 2]);
                                        mNo.append(temp[length - 1]);
                                        mNo.append(results_22.group(2));
                                        building = mNo.toString();
                                        sub22 = sub2;
                                        this.match_index_2.add(Integer.valueOf(results_32 + (cut.length() - (building.length() - results_22.group(2).length()))));
                                    } else {
                                        String[] strArr2 = temp;
                                        mComma = mComma3;
                                        sub22 = sub2;
                                        if (length > 0) {
                                            mNo = new StringBuilder(String.valueOf(cut));
                                            mNo.append(results_22.group(2));
                                            building = mNo.toString();
                                        }
                                        this.match_index_2.add(Integer.valueOf(results_32));
                                    }
                                    sub2 = sub22;
                                } else {
                                    mComma = mComma3;
                                    sub22 = sub2;
                                    str4 = sub_right;
                                }
                            } else {
                                mComma = mComma3;
                                str4 = sub_right;
                            }
                        } else {
                            boolean sub1_undone2;
                            String sub23;
                            mComma = mComma3;
                            str3 = cut;
                            mNo = true;
                            while (mNo != null) {
                                sub1_undone2 = mNo;
                                str5 = cut;
                                sub23 = sub2;
                                str4 = sub_right;
                                mComma2 = mComma;
                                mLocation = this.pPre_uni.matcher(str3);
                                if (mLocation.find()) {
                                    sub2 = mLocation.group(2);
                                    if (sub2 == null || !noBlank(sub2)) {
                                        cut = str5;
                                        mNo = null;
                                    } else if (divStr(sub2).length <= 4) {
                                        cut = new StringBuilder(String.valueOf(sub2));
                                        cut.append(results_22.group(2));
                                        building = cut.toString();
                                        cut = str5;
                                        this.match_index_2.add(Integer.valueOf(results_32 + (cut.length() - sub2.length())));
                                        mNo = null;
                                    } else {
                                        cut = str5;
                                        str3 = sub2;
                                        mComma = mComma2;
                                        mNo = sub1_undone2;
                                    }
                                    mComma = mComma2;
                                } else {
                                    cut = str5;
                                    mNo = null;
                                    mComma = mComma2;
                                    sub2 = sub23;
                                }
                                sub_right = str4;
                            }
                            if (building.length() == 0) {
                                temp = divStr(cut);
                                length = temp.length;
                                if (length > 4) {
                                    sub1_undone2 = mNo;
                                    StringBuilder stringBuilder3 = new StringBuilder(String.valueOf(temp[length - 4]));
                                    stringBuilder3.append(temp[length - 3]);
                                    stringBuilder3.append(temp[length - 2]);
                                    stringBuilder3.append(temp[length - 1]);
                                    stringBuilder3.append(results_22.group(2));
                                    building = stringBuilder3.toString();
                                    sub23 = sub2;
                                    str4 = sub_right;
                                    this.match_index_2.add(Integer.valueOf(results_32 + (cut.length() - (building.length() - results_22.group(2).length()))));
                                } else {
                                    Matcher matcher = mNo;
                                    String[] strArr3 = temp;
                                    sub23 = sub2;
                                    str4 = sub_right;
                                    if (length > 0) {
                                        mNo = new StringBuilder(String.valueOf(cut));
                                        mNo.append(results_22.group(2));
                                        building = mNo.toString();
                                    }
                                    this.match_index_2.add(Integer.valueOf(results_32));
                                }
                                sub2 = sub23;
                            } else {
                                str4 = sub_right;
                            }
                        }
                        if (building.length() == 0 && results_22.group(3) != null) {
                            mNo = results_22.group(2);
                            this.match_index_2.add(Integer.valueOf(results_32 + results_22.group(1).length()));
                            building = mNo;
                        }
                        String sub_left3;
                        String sub3;
                        String city2;
                        if (building.length() > 0) {
                            String[] results_34;
                            sub2 = results_22.group();
                            if (sub2.length() > building.length()) {
                                mNo = sub2.length() - building.length();
                            } else {
                                mNo = null;
                            }
                            int position = mNo;
                            str3 = sub2.substring(null, position);
                            if (i == 1) {
                                mNo = new StringBuilder(String.valueOf(sub_left));
                                mNo.append(str3);
                                str3 = mNo.toString();
                            }
                            if (noBlank(str3) != null) {
                                results_33 = 2;
                                results_34 = searBuilding_dic(str3, results_32 - sub_left.length());
                                sub_left3 = "";
                            } else {
                                sub_left3 = sub_left;
                                results_34 = results_33;
                                results_33 = i;
                            }
                            sub3 = str2.substring(sub2.length(), str2.length());
                            int i3;
                            if (noBlank(sub3)) {
                                int count3;
                                city = "";
                                str7 = "";
                                mLocation = this.p52s.matcher(sub3);
                                StringBuilder stringBuilder4;
                                String sub32;
                                if (mLocation.lookingAt()) {
                                    if (mLocation.group(6) == null) {
                                        count3 = 0 + 1;
                                        stringBuilder = new StringBuilder(String.valueOf(building));
                                        stringBuilder.append(mLocation.group());
                                        results[0] = stringBuilder.toString();
                                        count = sub3.substring(mLocation.group().length(), sub3.length());
                                        if (noBlank(count)) {
                                            position = mLocation;
                                            results_3 = searBuilding_suf(count, sub_left3, results_33, flag, (results_32 + sub2.length()) + mLocation.group().length());
                                        }
                                        str5 = cut;
                                        str6 = sub2;
                                    } else {
                                        i3 = position;
                                        mComma2 = mComma;
                                        position = mLocation;
                                        if (position.group(7) != null) {
                                            count3 = 0 + 1;
                                            stringBuilder = new StringBuilder(String.valueOf(building));
                                            stringBuilder.append(position.group());
                                            results[0] = stringBuilder.toString();
                                            count = sub3.substring(position.group().length(), sub3.length());
                                            if (noBlank(count)) {
                                                results_3 = searBuilding_suf(count, sub_left3, results_33, flag, (results_32 + sub2.length()) + position.group().length());
                                            }
                                            str5 = cut;
                                            str6 = sub2;
                                        } else {
                                            String cut2;
                                            mLocation = this.pCut.matcher(position.group(6));
                                            if (!mLocation.matches()) {
                                                str3 = "";
                                            } else if (mLocation.group(1) != null) {
                                                sub1 = mLocation.group(1);
                                                cut2 = sub1;
                                                sub1 = searCity(position.group(6).substring(sub1.length(), position.group(6).length()), 2);
                                                if (sub1 != null) {
                                                    int count4;
                                                    mNo = this.pPre_city.matcher(position.group(4));
                                                    Matcher mPre_city;
                                                    if (mNo.lookingAt()) {
                                                        count4 = 0 + 1;
                                                        stringBuilder4 = new StringBuilder(String.valueOf(building));
                                                        stringBuilder4.append(position.group());
                                                        results[0] = stringBuilder4.toString();
                                                        sub32 = sub3.substring(position.group().length(), sub3.length());
                                                        mPre_city = mNo;
                                                    } else {
                                                        Matcher mSingle = this.pSingle.matcher(position.group(3));
                                                        if (mSingle.matches()) {
                                                            int count5 = 0 + 1;
                                                            mSingle = new StringBuilder(String.valueOf(building));
                                                            mSingle.append(position.group());
                                                            results[0] = mSingle.toString();
                                                            sub32 = sub3.substring(position.group().length(), sub3.length());
                                                            mPre_city = mNo;
                                                            count4 = count5;
                                                        } else {
                                                            int count6 = 0 + 1;
                                                            stringBuilder4 = new StringBuilder(String.valueOf(building));
                                                            mPre_city = mNo;
                                                            stringBuilder4.append(position.group(1));
                                                            stringBuilder4.append(position.group(2));
                                                            results[0] = stringBuilder4.toString();
                                                            sub32 = sub3.substring(position.group(1).length() + position.group(2).length(), sub3.length());
                                                            count4 = count6;
                                                        }
                                                    }
                                                    if (noBlank(sub32)) {
                                                        str5 = cut;
                                                        str6 = sub2;
                                                        sub2 = cut2;
                                                        cut = sub1;
                                                        results_2 = searBuilding_suf(sub32, sub_left3, results_33, flag, results_32 + (str2.length() - sub32.length()));
                                                        city = cut;
                                                        str7 = sub2;
                                                        results_3 = results_34;
                                                        count = count4;
                                                    } else {
                                                        str5 = cut;
                                                        str6 = sub2;
                                                        city = sub1;
                                                        str7 = cut2;
                                                        results_2 = strArr;
                                                        results_3 = results_34;
                                                        count = count4;
                                                    }
                                                } else {
                                                    Matcher matcher2 = mLocation;
                                                    str5 = cut;
                                                    str6 = sub2;
                                                    sub2 = cut2;
                                                    cut = sub1;
                                                    str3 = new StringBuilder(String.valueOf(sub2));
                                                    str3.append(cut);
                                                    str3 = str3.toString();
                                                    if (position.group(8) == null) {
                                                        if (position.group(5) != null) {
                                                            mNo = new StringBuilder(String.valueOf(position.group(5)));
                                                            mNo.append(str3);
                                                            str3 = mNo.toString();
                                                        }
                                                    } else if (position.group(5) == null) {
                                                        stringBuilder4 = new StringBuilder(String.valueOf(position.group(6)));
                                                        stringBuilder4.append(position.group(8));
                                                        str3 = stringBuilder4.toString();
                                                    } else {
                                                        stringBuilder4 = new StringBuilder(String.valueOf(position.group(5)));
                                                        stringBuilder4.append(position.group(6));
                                                        stringBuilder4.append(position.group(8));
                                                        str3 = stringBuilder4.toString();
                                                    }
                                                    cut = str3;
                                                    city = null + 1;
                                                    stringBuilder = new StringBuilder(String.valueOf(building));
                                                    stringBuilder.append(position.group(1));
                                                    stringBuilder.append(position.group(2));
                                                    stringBuilder.append(position.group(4));
                                                    stringBuilder.append(cut);
                                                    results[0] = stringBuilder.toString();
                                                    sub32 = sub3.substring(((position.group(1).length() + position.group(2).length()) + position.group(4).length()) + cut.length(), sub3.length());
                                                    if (noBlank(sub32)) {
                                                        results_2 = searBuilding_suf(sub32, sub_left3, results_33, flag, results_32 + (str2.length() - sub32.length()));
                                                        str7 = sub2;
                                                        count = city;
                                                    } else {
                                                        str7 = sub2;
                                                        count = city;
                                                        results_2 = strArr;
                                                    }
                                                    results_3 = results_34;
                                                    city = cut;
                                                }
                                            } else {
                                                str3 = "";
                                            }
                                            sub1 = str3;
                                            cut2 = sub1;
                                            sub1 = searCity(position.group(6).substring(sub1.length(), position.group(6).length()), 2);
                                            if (sub1 != null) {
                                            }
                                        }
                                    }
                                    results_2 = results_3;
                                    str5 = cut;
                                    count = count3;
                                    results_3 = results_34;
                                } else {
                                    str5 = cut;
                                    str6 = sub2;
                                    i3 = position;
                                    mComma2 = mComma;
                                    position = mLocation;
                                    cut = this.p2s.matcher(sub3);
                                    if (!cut.lookingAt()) {
                                        sub2 = sub3;
                                        count3 = 0 + 1;
                                        results[0] = building;
                                        results_3 = searBuilding_suf(sub2, sub_left3, results_33, flag, results_32 + (str2.length() - sub2.length()));
                                    } else if (cut.group(3) == null) {
                                        int count7 = 0 + 1;
                                        results[0] = building;
                                        if (noBlank(sub3)) {
                                            int count8 = count7;
                                            count7 = sub3;
                                            results_2 = searBuilding_suf(sub3, sub_left3, results_33, flag, results_32 + (str2.length() - sub3.length()));
                                            results_3 = results_34;
                                            count = count8;
                                        } else {
                                            results_2 = strArr;
                                            results_3 = results_34;
                                            count = count7;
                                        }
                                    } else {
                                        sub2 = sub3;
                                        if (cut.group(4) != null) {
                                            count3 = 0 + 1;
                                            stringBuilder = new StringBuilder(String.valueOf(building));
                                            stringBuilder.append(cut.group());
                                            results[0] = stringBuilder.toString();
                                            count = sub2.substring(cut.group().length(), sub2.length());
                                            if (noBlank(count)) {
                                                results_3 = searBuilding_suf(count, sub_left3, results_33, flag, results_32 + (str2.length() - count.length()));
                                            }
                                        } else {
                                            String cut3;
                                            int count9;
                                            sub3 = this.pCut.matcher(cut.group(3));
                                            if (!sub3.matches()) {
                                                str3 = "";
                                            } else if (sub3.group(1) != null) {
                                                cut3 = sub3.group(1);
                                                str3 = searCity(cut.group(3).substring(cut3.length(), cut.group(3).length()), 2);
                                                if (str3 == null) {
                                                    stringBuilder4 = new StringBuilder(String.valueOf(cut3));
                                                    stringBuilder4.append(str3);
                                                    str3 = stringBuilder4.toString();
                                                    if (cut.group(6) == null) {
                                                        if (cut.group(2) != null) {
                                                            mNo = new StringBuilder(String.valueOf(cut.group(2)));
                                                            mNo.append(str3);
                                                            str3 = mNo.toString();
                                                        }
                                                    } else if (cut.group(2) == null) {
                                                        stringBuilder4 = new StringBuilder(String.valueOf(cut.group(3)));
                                                        stringBuilder4.append(cut.group(5));
                                                        stringBuilder4.append(cut.group(6));
                                                        str3 = stringBuilder4.toString();
                                                    } else {
                                                        stringBuilder4 = new StringBuilder(String.valueOf(cut.group(2)));
                                                        stringBuilder4.append(cut.group(3));
                                                        stringBuilder4.append(cut.group(5));
                                                        stringBuilder4.append(cut.group(6));
                                                        str3 = stringBuilder4.toString();
                                                    }
                                                    stringBuilder4 = new StringBuilder(String.valueOf(cut.group(1)));
                                                    stringBuilder4.append(str3);
                                                    str3 = stringBuilder4.toString();
                                                } else if (this.pPre_city.matcher(cut.group(1)).lookingAt() != null) {
                                                    str3 = cut.group();
                                                } else if (this.pSingle.matcher(cut.group(3)).matches()) {
                                                    str3 = cut.group();
                                                } else {
                                                    str3 = "";
                                                }
                                                sub1 = str3;
                                                count9 = 0 + 1;
                                                stringBuilder = new StringBuilder(String.valueOf(building));
                                                stringBuilder.append(sub1);
                                                results[0] = stringBuilder.toString();
                                                sub32 = sub2.substring(sub1.length(), sub2.length());
                                                if (noBlank(sub32)) {
                                                    str7 = cut3;
                                                    count = count9;
                                                    results_2 = strArr;
                                                    results_3 = results_34;
                                                } else {
                                                    city2 = sub1;
                                                    str7 = cut3;
                                                    sub_right2 = sub3;
                                                    results_2 = searBuilding_suf(sub32, sub_left3, results_33, flag, results_32 + (str2.length() - sub32.length()));
                                                    count = count9;
                                                    count9 = city2;
                                                    results_3 = results_34;
                                                }
                                            } else {
                                                str3 = "";
                                            }
                                            cut3 = str3;
                                            str3 = searCity(cut.group(3).substring(cut3.length(), cut.group(3).length()), 2);
                                            if (str3 == null) {
                                            }
                                            sub1 = str3;
                                            count9 = 0 + 1;
                                            stringBuilder = new StringBuilder(String.valueOf(building));
                                            stringBuilder.append(sub1);
                                            results[0] = stringBuilder.toString();
                                            sub32 = sub2.substring(sub1.length(), sub2.length());
                                            if (noBlank(sub32)) {
                                            }
                                        }
                                    }
                                    results_2 = results_3;
                                    count = count3;
                                    results_3 = results_34;
                                }
                                count = count3;
                            } else {
                                str5 = cut;
                                str6 = sub2;
                                i3 = position;
                                mComma2 = mComma;
                                sub2 = sub3;
                                results_3 = null + 1;
                                results[0] = building;
                                count = results_3;
                            }
                            results_2 = strArr;
                            results_3 = results_34;
                        } else {
                            str5 = cut;
                            mComma2 = mComma;
                            sub_right = results_22.group();
                            sub3 = str2.substring(sub_right.length(), str2.length());
                            if (noBlank(sub3)) {
                                city2 = sub3;
                                results_2 = searBuilding_suf(sub3, sub_right, 1, flag, results_32 + (str2.length() - sub3.length()));
                                str6 = sub2;
                                sub_left3 = sub_right;
                                str4 = city2;
                                results_3 = results_33;
                                results_33 = 1;
                            } else {
                                str6 = sub2;
                                sub_left3 = sub_right;
                                str4 = sub3;
                                results_2 = strArr;
                                results_3 = results_33;
                                results_33 = 1;
                            }
                        }
                    }
                }
                if (results_3.length > 0) {
                    head3 = 0;
                    while (head3 < results_3.length) {
                        count2 = count + 1;
                        results[count] = results_3[head3];
                        head3++;
                        count = count2;
                    }
                }
                if (results_2.length > 0) {
                    head3 = 0;
                    while (head3 < results_2.length) {
                        count2 = count + 1;
                        results[count] = results_2[head3];
                        head3++;
                        count = count2;
                    }
                }
                if (count < 8) {
                    return results;
                }
                String[] re = new String[count];
                for (count2 = 0; count2 < count; count2++) {
                    re[count2] = results[count2];
                }
                return re;
            }
        }
        head2 = head;
        str2 = str3;
        mLocation = this.pLocation.matcher(str2);
        if (mLocation.find()) {
        }
        if (results_3.length > 0) {
        }
        if (results_2.length > 0) {
        }
        if (count < 8) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:117:0x05b8  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x058b  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x058b  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x05b8  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x05b8  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x058b  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x058b  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x05b8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String[] searBuilding_dic(String string, int head) {
        int length = string.length();
        int head_0 = head;
        String[] results = new String[8];
        String str = string;
        Pattern pPre_building = Pattern.compile("[\\s\\S]*(?<![a-zA-Z])((?i)(in|at|from|near|to|reach))\\b(\\s+(?i)the\\b)?(?:(?:(?:\\s*[,.-:'\"()]\\s*)+)|\\s+)?");
        Pattern pCut = Pattern.compile("(\\s*[,.]?\\s*(?:(?i)(?:in|on|at|from|of)\\s+)?(?:(?i)(uptown|downtown)\\s+)?)?[\\s\\S]*");
        Pattern pSingle = Pattern.compile("(?:\\.)?\\s*,\\s*[A-Z][a-z]+(?:\\s*(?:[,.)\"'])\\s*)*");
        Pattern pPre_city = Pattern.compile("(?:\\s*(?:,|\\.){0,2}\\s*\\b(?i)(?:in)\\b(.*))");
        int full = str.length();
        int length_bracket = 0;
        boolean flag = true;
        int count = 0;
        String building = "";
        int index = 0;
        String city = "";
        String cut = "";
        int head2 = head;
        String s_right = "";
        String s_left = "";
        String string2 = string;
        while (index < length) {
            int length2;
            int count2;
            String str2;
            int position;
            Pattern pCut2;
            str = str.substring(index, length);
            head2 = (full - str.length()) + head_0;
            length -= index;
            index = 0;
            int head_02 = head_0;
            String s_left2 = string2.substring(null, string2.length() - length);
            String string3 = string2;
            int position2 = DicSearch.dicsearch(1, str.toLowerCase(Locale.getDefault()));
            if (position2 == 0) {
                while (index < length && ((str.charAt(index) >= 'a' && str.charAt(index) <= 'z') || ((str.charAt(index) >= 'A' && str.charAt(index) <= 'Z') || (str.charAt(index) >= '0' && str.charAt(index) <= '9')))) {
                    index++;
                }
                length2 = length;
                count2 = count;
                str2 = str;
                position = position2;
                pCut2 = pCut;
                string2 = string3;
            } else {
                StringBuilder stringBuilder;
                Pattern pCut3;
                s_left = str.substring(0, position2);
                string2 = str.substring(position2, str.length());
                length2 = length;
                length = searchBracket(string2);
                if (length > 0) {
                    position = position2;
                    stringBuilder = new StringBuilder(String.valueOf(s_left));
                    stringBuilder.append(string2.substring(0, length));
                    s_left = stringBuilder.toString();
                    string2 = string2.substring(length, string2.length());
                } else {
                    position = position2;
                }
                String cut2 = "";
                int length_bracket2 = length;
                Matcher m52s = this.p52s.matcher(string2);
                String city2 = "";
                String cut3;
                int index2;
                Matcher matcher;
                String s_right2;
                String str3;
                int count3;
                Matcher mPre_city;
                StringBuilder stringBuilder2;
                int count4;
                StringBuilder stringBuilder3;
                if (m52s.lookingAt()) {
                    if (m52s.group(6) == null) {
                        int count5 = count + 1;
                        cut3 = cut2;
                        stringBuilder = new StringBuilder(String.valueOf(s_left));
                        stringBuilder.append(m52s.group());
                        results[count] = stringBuilder.toString();
                        this.match_index_2.add(Integer.valueOf(head2));
                        index2 = 0;
                        matcher = m52s;
                        s_right2 = string2;
                        str3 = str;
                        pCut2 = pCut;
                        count3 = count5;
                    } else {
                        cut3 = cut2;
                        StringBuilder stringBuilder4;
                        if (m52s.group(7) != null) {
                            int count6 = count + 1;
                            stringBuilder4 = new StringBuilder(String.valueOf(s_left));
                            stringBuilder4.append(m52s.group());
                            results[count] = stringBuilder4.toString();
                            this.match_index_2.add(Integer.valueOf(head2));
                            index2 = 0;
                            s_right2 = string2;
                            str3 = str;
                            pCut2 = pCut;
                            count3 = count6;
                        } else {
                            Matcher mCut = pCut.matcher(m52s.group(6));
                            if (!mCut.matches()) {
                                city = "";
                            } else if (mCut.group(1) != null) {
                                city = mCut.group(1);
                            } else {
                                city = "";
                            }
                            str3 = str;
                            index2 = 0;
                            pCut3 = pCut;
                            index = searCity(m52s.group(6).substring(city.length(), m52s.group(6).length()), 2);
                            if (index == 0) {
                                mPre_city = pPre_city.matcher(m52s.group(4));
                                if (mPre_city.matches()) {
                                    position2 = count + 1;
                                    stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                    stringBuilder2.append(m52s.group());
                                    results[count] = stringBuilder2.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                } else {
                                    Matcher matcher2 = mPre_city;
                                    mPre_city = pSingle.matcher(m52s.group(3));
                                    if (mPre_city.matches()) {
                                        position2 = count + 1;
                                        stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                        stringBuilder2.append(m52s.group());
                                        results[count] = stringBuilder2.toString();
                                        this.match_index_2.add(Integer.valueOf(head2));
                                    } else {
                                        count4 = count + 1;
                                        stringBuilder4 = new StringBuilder(String.valueOf(s_left));
                                        int count7 = count4;
                                        stringBuilder4.append(m52s.group(1));
                                        stringBuilder4.append(m52s.group(2));
                                        results[count] = stringBuilder4.toString();
                                        this.match_index_2.add(Integer.valueOf(head2));
                                        city2 = index;
                                        matcher = m52s;
                                        s_right2 = string2;
                                        cut3 = city;
                                        pCut2 = pCut3;
                                        count3 = count7;
                                    }
                                }
                                city2 = index;
                                s_right2 = string2;
                                cut3 = city;
                                count3 = position2;
                            } else {
                                String cut4;
                                stringBuilder3 = new StringBuilder(String.valueOf(city));
                                stringBuilder3.append(index);
                                index = stringBuilder3.toString();
                                if (m52s.group(8) == null) {
                                    if (m52s.group(5) != null) {
                                        stringBuilder4 = new StringBuilder(String.valueOf(m52s.group(5)));
                                        stringBuilder4.append(index);
                                        index = stringBuilder4.toString();
                                    }
                                } else if (m52s.group(5) == null) {
                                    stringBuilder3 = new StringBuilder(String.valueOf(m52s.group(6)));
                                    stringBuilder3.append(m52s.group(8));
                                    index = stringBuilder3.toString();
                                } else {
                                    stringBuilder3 = new StringBuilder(String.valueOf(m52s.group(5)));
                                    stringBuilder3.append(m52s.group(6));
                                    stringBuilder3.append(m52s.group(8));
                                    index = stringBuilder3.toString();
                                    str = count + 1;
                                    stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                    cut4 = city;
                                    stringBuilder2.append(m52s.group(1));
                                    stringBuilder2.append(m52s.group(2));
                                    stringBuilder2.append(m52s.group(4));
                                    stringBuilder2.append(index);
                                    results[count] = stringBuilder2.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                    city2 = index;
                                    matcher = m52s;
                                    s_right2 = string2;
                                    count3 = str;
                                    pCut2 = pCut3;
                                    cut3 = cut4;
                                }
                                str = count + 1;
                                stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                cut4 = city;
                                stringBuilder2.append(m52s.group(1));
                                stringBuilder2.append(m52s.group(2));
                                stringBuilder2.append(m52s.group(4));
                                stringBuilder2.append(index);
                                results[count] = stringBuilder2.toString();
                                this.match_index_2.add(Integer.valueOf(head2));
                                city2 = index;
                                matcher = m52s;
                                s_right2 = string2;
                                count3 = str;
                                pCut2 = pCut3;
                                cut3 = cut4;
                            }
                        }
                    }
                    if (flag) {
                        index = (index2 + results[count3 - 1].length()) - 1;
                        str2 = str3;
                        building = s_left;
                        length_bracket = length_bracket2;
                        city = city2;
                        cut = cut3;
                        s_right = s_right2;
                        count2 = count3;
                        string2 = str2.substring(results[count3 - 1].length(), str2.length());
                    } else {
                        str2 = str3;
                        length = 1;
                        index = (index2 + s_left.length()) - 1;
                        building = s_left;
                        flag = true;
                        length_bracket = length_bracket2;
                        cut = cut3;
                        count2 = count3;
                        string2 = str2.substring(s_left.length(), str2.length());
                        city = city2;
                        index += length;
                        str = str2;
                        count = count2;
                        length = length2;
                        position2 = position;
                        pCut = pCut2;
                        s_left = s_left2;
                        head_0 = head_02;
                    }
                } else {
                    index2 = 0;
                    str3 = str;
                    cut3 = cut2;
                    pCut3 = pCut;
                    Matcher m2s = this.p2s.matcher(string2);
                    if (!m2s.lookingAt()) {
                        s_right2 = string2;
                        pCut2 = pCut3;
                        if (pPre_building.matcher(s_left2).matches()) {
                            count3 = count + 1;
                            results[count] = s_left;
                            this.match_index_2.add(Integer.valueOf(head2));
                            if (flag) {
                            }
                        } else {
                            flag = false;
                        }
                    } else if (m2s.group(3) == null) {
                        if (pPre_building.matcher(s_left2).matches()) {
                            count4 = count + 1;
                            results[count] = s_left;
                            this.match_index_2.add(Integer.valueOf(head2));
                            matcher = m52s;
                            s_right2 = string2;
                            count3 = count4;
                        } else {
                            flag = false;
                            matcher = m52s;
                            s_right2 = string2;
                            pCut2 = pCut3;
                        }
                    } else if (m2s.group(4) != null) {
                        int count8 = count + 1;
                        stringBuilder3 = new StringBuilder(String.valueOf(s_left));
                        stringBuilder3.append(m2s.group());
                        results[count] = stringBuilder3.toString();
                        this.match_index_2.add(Integer.valueOf(head2));
                        matcher = m52s;
                        s_right2 = string2;
                        count3 = count8;
                    } else {
                        Pattern pCut4 = pCut3;
                        mPre_city = pCut4.matcher(m2s.group(3));
                        if (!mPre_city.matches()) {
                            cut2 = "";
                        } else if (mPre_city.group(1) != null) {
                            cut2 = mPre_city.group(1);
                        } else {
                            cut2 = "";
                        }
                        matcher = m52s;
                        s_right2 = string2;
                        pCut2 = pCut4;
                        city = searCity(m2s.group(3).substring(cut2.length(), m2s.group(3).length()), 2);
                        if (city != null) {
                            StringBuilder stringBuilder5;
                            m52s = new StringBuilder(String.valueOf(cut2));
                            m52s.append(city);
                            m52s = m52s.toString();
                            if (m2s.group(6) == null) {
                                if (m2s.group(2) != null) {
                                    stringBuilder = new StringBuilder(String.valueOf(m2s.group(2)));
                                    stringBuilder.append(m52s);
                                    m52s = stringBuilder.toString();
                                }
                            } else if (m2s.group(2) == null) {
                                stringBuilder5 = new StringBuilder(String.valueOf(m2s.group(3)));
                                stringBuilder5.append(m2s.group(5));
                                stringBuilder5.append(m2s.group(6));
                                m52s = stringBuilder5.toString();
                            } else {
                                stringBuilder5 = new StringBuilder(String.valueOf(m2s.group(2)));
                                stringBuilder5.append(m2s.group(3));
                                stringBuilder5.append(m2s.group(5));
                                stringBuilder5.append(m2s.group(6));
                                m52s = stringBuilder5.toString();
                            }
                            stringBuilder5 = new StringBuilder(String.valueOf(m2s.group(1)));
                            stringBuilder5.append(m52s);
                            city = stringBuilder5.toString();
                            m52s = count + 1;
                            stringBuilder5 = new StringBuilder(String.valueOf(s_left));
                            stringBuilder5.append(city);
                            results[count] = stringBuilder5.toString();
                            this.match_index_2.add(Integer.valueOf(head2));
                            count3 = m52s;
                        } else {
                            m52s = pPre_city.matcher(m2s.group(1));
                            Matcher mPre_city2;
                            if (m52s.matches()) {
                                count3 = count + 1;
                                mPre_city2 = m52s;
                                stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                stringBuilder2.append(m2s.group());
                                results[count] = stringBuilder2.toString();
                                this.match_index_2.add(Integer.valueOf(head2));
                            } else {
                                mPre_city2 = m52s;
                                m52s = pSingle.matcher(m2s.group(3));
                                Matcher mSingle;
                                if (m52s.matches()) {
                                    count3 = count + 1;
                                    mSingle = m52s;
                                    stringBuilder2 = new StringBuilder(String.valueOf(s_left));
                                    stringBuilder2.append(m2s.group());
                                    results[count] = stringBuilder2.toString();
                                    this.match_index_2.add(Integer.valueOf(head2));
                                } else {
                                    mSingle = m52s;
                                    if (pPre_building.matcher(s_left2).matches()) {
                                        count3 = count + 1;
                                        results[count] = s_left;
                                        this.match_index_2.add(Integer.valueOf(head2));
                                    } else {
                                        flag = false;
                                        count3 = count;
                                    }
                                }
                            }
                        }
                        city2 = city;
                        cut3 = cut2;
                        if (flag) {
                        }
                    }
                    count3 = count;
                    if (flag) {
                    }
                }
                pCut2 = pCut3;
                if (flag) {
                }
            }
            length = 1;
            index += length;
            str = str2;
            count = count2;
            length = length2;
            position2 = position;
            pCut = pCut2;
            s_left = s_left2;
            head_0 = head_02;
        }
        if (count < 8) {
            index = new String[count];
            for (head2 = 0; head2 < count; head2++) {
                index[head2] = results[head2];
            }
            return index;
        }
        return results;
    }

    private boolean noBlank(String str) {
        int n = str.length();
        str = str.toLowerCase(Locale.getDefault());
        boolean flag = true;
        int index = 0;
        while (flag && index < n) {
            if ((str.charAt(index) <= 'z' && str.charAt(index) >= 'a') || (str.charAt(index) <= '9' && str.charAt(index) >= '0')) {
                flag = false;
            }
            index++;
        }
        return !flag;
    }

    private String[] divStr(String str) {
        int index;
        String[] strs = new String[150];
        int length = str.length();
        int pr = 0;
        strs[0] = "";
        for (index = 0; index < length; index++) {
            char letter = str.charAt(index);
            StringBuilder stringBuilder;
            if ((letter <= 'z' && letter >= 'a') || ((letter <= 'Z' && letter >= 'A') || (letter <= '9' && letter >= '0'))) {
                stringBuilder = new StringBuilder(String.valueOf(strs[pr]));
                stringBuilder.append(letter);
                strs[pr] = stringBuilder.toString();
            } else if (strs[pr].length() > 0) {
                stringBuilder = new StringBuilder(String.valueOf(strs[pr]));
                stringBuilder.append(letter);
                strs[pr] = stringBuilder.toString();
                pr++;
                strs[pr] = "";
            } else if (pr > 0) {
                int i = pr - 1;
                StringBuilder stringBuilder2 = new StringBuilder(String.valueOf(strs[i]));
                stringBuilder2.append(letter);
                strs[i] = stringBuilder2.toString();
            }
        }
        if (strs[pr].length() > 0) {
            pr++;
        }
        if (pr >= 150) {
            return strs;
        }
        String[] re = new String[pr];
        for (index = 0; index < pr; index++) {
            re[index] = strs[index];
        }
        return re;
    }

    private boolean stanWri(String str) {
        String[] strs = divStr(str);
        int length = strs.length;
        boolean flag = true;
        int index = 0;
        while (flag && index < length) {
            int length_2 = strs[index].length();
            int index_2 = 1;
            while (flag && index_2 < length_2) {
                char letter = strs[index].charAt(index_2);
                if (letter <= 'Z' && letter >= 'A') {
                    flag = false;
                }
                index_2++;
            }
            if (length > 3) {
                if (index == 0) {
                    index = (length / 2) - 1;
                } else if (index == (length / 2) - 1) {
                    index = length - 2;
                }
            }
            index++;
        }
        return flag;
    }

    public String searCity(String string, int mode) {
        int length = string.length();
        String str = string;
        Matcher mCity = Pattern.compile("([\\s\\S]*(?i)(town|city|county)\\b)(?:.*)").matcher(str);
        if (mode == 1) {
            if (mCity.find() && noBlank(mCity.group(1).substring(0, mCity.group(2).length()))) {
                return str;
            }
            int index = 0;
            while (index < length) {
                str = str.substring(index, length);
                length -= index;
                index = 0;
                if (DicSearch.dicsearch(0, str.toLowerCase(Locale.getDefault())) != 0) {
                    return str;
                }
                while (index < length && ((str.charAt(index) >= 'a' && str.charAt(index) <= 'z') || ((str.charAt(index) >= 'A' && str.charAt(index) <= 'Z') || (str.charAt(index) >= '0' && str.charAt(index) <= '9')))) {
                    index++;
                }
                index++;
            }
        } else if (mCity.find() && noBlank(mCity.group(1).substring(0, mCity.group(2).length()))) {
            return mCity.group(1);
        } else {
            int position = DicSearch.dicsearch(0, str.toLowerCase(Locale.getDefault()));
            if (position > 0) {
                Matcher mCity2 = Pattern.compile("(\\s+(?i)(town|city|county))\\b.*").matcher(str.substring(position, length));
                if (!mCity2.matches()) {
                    return str.substring(0, position);
                }
                StringBuilder stringBuilder = new StringBuilder(String.valueOf(str.substring(0, position)));
                stringBuilder.append(mCity2.group(1));
                return stringBuilder.toString();
            }
        }
        return null;
    }

    public int searchBracket(String str) {
        Matcher mBracket = Pattern.compile("(\\s*.?\\s*)\\)").matcher(str);
        if (mBracket.lookingAt()) {
            return mBracket.group().length();
        }
        return 0;
    }

    public String noShut(String str) {
        Matcher mShut = Pattern.compile("\\s*#").matcher(str);
        if (mShut.lookingAt()) {
            return str.substring(mShut.group().length(), str.length());
        }
        return str;
    }

    private ArrayList<Match> sortAndMergePosList(ArrayList<Match> posList, String sourceTxt) {
        if (posList.isEmpty()) {
            return null;
        }
        Collections.sort(posList, new Comparator<Match>() {
            public int compare(Match p1, Match p2) {
                if (p1.getStartPos().compareTo(p2.getStartPos()) == 0) {
                    return p1.getEndPos().compareTo(p2.getEndPos());
                }
                return p1.getStartPos().compareTo(p2.getStartPos());
            }
        });
        int i = posList.size() - 1;
        while (i > 0) {
            if (((Match) posList.get(i - 1)).getStartPos().intValue() <= ((Match) posList.get(i)).getStartPos().intValue() && ((Match) posList.get(i)).getStartPos().intValue() <= ((Match) posList.get(i - 1)).getEndPos().intValue()) {
                if (((Match) posList.get(i - 1)).getEndPos().intValue() < ((Match) posList.get(i)).getEndPos().intValue()) {
                    ((Match) posList.get(i - 1)).setEndPos(((Match) posList.get(i)).getEndPos());
                    ((Match) posList.get(i - 1)).setMatchedAddr(sourceTxt.substring(((Match) posList.get(i - 1)).getStartPos().intValue(), ((Match) posList.get(i - 1)).getEndPos().intValue()));
                    posList.remove(i);
                } else if (((Match) posList.get(i - 1)).getEndPos().intValue() >= ((Match) posList.get(i)).getEndPos().intValue()) {
                    posList.remove(i);
                }
            }
            i--;
        }
        return posList;
    }
}
