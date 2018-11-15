package com.android.server.mtm.iaware.appmng.rule;

import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.StringIntegerMap;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.XmlValue;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public abstract class Config {
    ArrayMap<String, String> mProperties;
    RuleNode mRules;

    public Config(ArrayMap<String, String> prop, RuleNode rules) {
        this.mProperties = prop;
        this.mRules = rules;
    }

    private void printRuleNode(PrintWriter pw, RuleNode node, int depth) {
        if (node != null && pw != null) {
            StringBuilder stringBuilder;
            XmlValue value = node.getValue();
            if (value != null) {
                StringBuilder stringBuilder2;
                if (value.isString()) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("|");
                    stringBuilder2.append(getDepthString(depth));
                    stringBuilder2.append(node.getCurrentType());
                    stringBuilder2.append(":");
                    stringBuilder2.append(value.getStringValue());
                    pw.println(stringBuilder2.toString());
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("|");
                    stringBuilder2.append(getDepthString(depth));
                    stringBuilder2.append(node.getCurrentType());
                    stringBuilder2.append(":");
                    stringBuilder2.append(value.getIntValue());
                    pw.println(stringBuilder2.toString());
                }
                String index = value.getIndex();
                if (index != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("|");
                    stringBuilder3.append(getDepthString(depth));
                    stringBuilder3.append("index:");
                    stringBuilder3.append(index);
                    pw.println(stringBuilder3.toString());
                }
                int hwStop = value.getHwStop();
                if (hwStop != -1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("|");
                    stringBuilder.append(getDepthString(depth));
                    stringBuilder.append("hwStop:");
                    stringBuilder.append(hwStop);
                    pw.println(stringBuilder.toString());
                }
            }
            if (node.hasChild()) {
                StringIntegerMap childs = node.getChilds();
                if (childs.isStringMap()) {
                    for (Entry<String, RuleNode> entry : childs.getStringMap().entrySet()) {
                        printRuleNode(pw, (RuleNode) entry.getValue(), depth + 1);
                    }
                } else {
                    ArrayList<Integer> sortedList = childs.getSortedList();
                    if (sortedList != null) {
                        stringBuilder = new StringBuilder();
                        int listSize = sortedList.size();
                        for (int i = 0; i < listSize; i++) {
                            stringBuilder.append((Integer) sortedList.get(i));
                            stringBuilder.append(" ");
                        }
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("|");
                        stringBuilder4.append(getDepthString(depth));
                        stringBuilder4.append("sorted child:");
                        stringBuilder4.append(stringBuilder.toString());
                        pw.println(stringBuilder4.toString());
                    }
                    LinkedHashMap<Integer, RuleNode> integerMap = childs.getIntegerMap();
                    if (integerMap != null) {
                        for (Integer key : integerMap.keySet()) {
                            if (key == null) {
                                pw.println("bad config key == null");
                                return;
                            }
                            printRuleNode(pw, (RuleNode) integerMap.get(key), depth + 1);
                        }
                    }
                }
            }
        }
    }

    private String getDepthString(int depth) {
        StringBuilder sb = new StringBuilder(depth);
        for (int i = 0; i < depth; i++) {
            sb.append("--");
        }
        return sb.toString();
    }

    public void dump(PrintWriter pw) {
        printRuleNode(pw, this.mRules, 0);
    }
}
