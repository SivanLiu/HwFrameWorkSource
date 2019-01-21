package android.icu.impl;

import android.icu.text.StringTransform;
import android.icu.text.SymbolTable;
import android.icu.text.UnicodeSet;
import android.icu.util.Freezable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class UnicodeRegex implements Cloneable, Freezable<UnicodeRegex>, StringTransform {
    private static final UnicodeRegex STANDARD = new UnicodeRegex();
    private Comparator<Object> LongestFirst = new Comparator<Object>() {
        public int compare(Object obj0, Object obj1) {
            String arg0 = obj0.toString();
            String arg1 = obj1.toString();
            int len0 = arg0.length();
            int len1 = arg1.length();
            if (len0 != len1) {
                return len1 - len0;
            }
            return arg0.compareTo(arg1);
        }
    };
    private String bnfCommentString = "#";
    private String bnfLineSeparator = "\n";
    private String bnfVariableInfix = "=";
    private SymbolTable symbolTable;

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public UnicodeRegex setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        return this;
    }

    /* JADX WARNING: Missing block: B:5:0x0025, code skipped:
            r0 = r11;
     */
    /* JADX WARNING: Missing block: B:15:0x0037, code skipped:
            r0 = r11;
     */
    /* JADX WARNING: Missing block: B:27:0x0066, code skipped:
            r13.append(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String transform(String regex) {
        String str = regex;
        StringBuilder result = new StringBuilder();
        UnicodeSet temp = new UnicodeSet();
        int i = 0;
        ParsePosition pos = new ParsePosition(0);
        int state = 0;
        while (true) {
            int i2 = i;
            if (i2 < regex.length()) {
                char ch = str.charAt(i2);
                switch (state) {
                    case 0:
                        if (ch != PatternTokenizer.BACK_SLASH) {
                            if (ch == '[' && UnicodeSet.resemblesPattern(str, i2)) {
                                char c = ch;
                                i2 = processSet(str, i2, result, temp, pos);
                                break;
                            }
                        } else if (!UnicodeSet.resemblesPattern(str, i2)) {
                            state = 1;
                            break;
                        } else {
                            i2 = processSet(str, i2, result, temp, pos);
                            break;
                        }
                    case 1:
                        if (ch != 'Q') {
                            state = 0;
                            break;
                        }
                        state = 1;
                        break;
                    case 2:
                        if (ch == PatternTokenizer.BACK_SLASH) {
                            state = 3;
                            break;
                        }
                        break;
                    case 3:
                        if (ch == 'E') {
                        }
                        state = 2;
                        break;
                }
            }
            return result.toString();
            i = i2 + 1;
        }
    }

    public static String fix(String regex) {
        return STANDARD.transform(regex);
    }

    public static Pattern compile(String regex) {
        return Pattern.compile(STANDARD.transform(regex));
    }

    public static Pattern compile(String regex, int options) {
        return Pattern.compile(STANDARD.transform(regex), options);
    }

    public String compileBnf(String bnfLines) {
        return compileBnf(Arrays.asList(bnfLines.split("\\r\\n?|\\n")));
    }

    public String compileBnf(List<String> lines) {
        Map<String, String> variables = getVariables(lines);
        Set<String> unused = new LinkedHashSet(variables.keySet());
        for (int i = 0; i < 2; i++) {
            for (Entry<String, String> entry : variables.entrySet()) {
                String variable = (String) entry.getKey();
                String definition = (String) entry.getValue();
                for (Entry<String, String> entry2 : variables.entrySet()) {
                    String variable2 = (String) entry2.getKey();
                    String definition2 = (String) entry2.getValue();
                    if (!variable.equals(variable2)) {
                        String altered2 = definition2.replace(variable, definition);
                        if (!altered2.equals(definition2)) {
                            unused.remove(variable);
                            variables.put(variable2, altered2);
                        }
                    }
                }
            }
        }
        if (unused.size() == 1) {
            return (String) variables.get(unused.iterator().next());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a single root: ");
        stringBuilder.append(unused);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String getBnfCommentString() {
        return this.bnfCommentString;
    }

    public void setBnfCommentString(String bnfCommentString) {
        this.bnfCommentString = bnfCommentString;
    }

    public String getBnfVariableInfix() {
        return this.bnfVariableInfix;
    }

    public void setBnfVariableInfix(String bnfVariableInfix) {
        this.bnfVariableInfix = bnfVariableInfix;
    }

    public String getBnfLineSeparator() {
        return this.bnfLineSeparator;
    }

    public void setBnfLineSeparator(String bnfLineSeparator) {
        this.bnfLineSeparator = bnfLineSeparator;
    }

    public static List<String> appendLines(List<String> result, String file, String encoding) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            List<String> appendLines = appendLines((List) result, is, encoding);
            return appendLines;
        } finally {
            is.close();
        }
    }

    public static List<String> appendLines(List<String> result, InputStream inputStream, String encoding) throws UnsupportedEncodingException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, encoding == null ? "UTF-8" : encoding));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                return result;
            }
            result.add(line);
        }
    }

    public UnicodeRegex cloneAsThawed() {
        try {
            return (UnicodeRegex) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException();
        }
    }

    public UnicodeRegex freeze() {
        return this;
    }

    public boolean isFrozen() {
        return true;
    }

    private int processSet(String regex, int i, StringBuilder result, UnicodeSet temp, ParsePosition pos) {
        try {
            pos.setIndex(i);
            UnicodeSet x = temp.clear().applyPattern(regex, pos, this.symbolTable, 0);
            x.complement().complement();
            result.append(x.toPattern(false));
            return pos.getIndex() - 1;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error in ");
            stringBuilder.append(regex);
            throw ((IllegalArgumentException) new IllegalArgumentException(stringBuilder.toString()).initCause(e));
        }
    }

    private Map<String, String> getVariables(List<String> lines) {
        Map<String, String> variables = new TreeMap(this.LongestFirst);
        String variable = null;
        StringBuffer definition = new StringBuffer();
        int count = 0;
        for (String line : lines) {
            count++;
            String line2;
            if (line2.length() != 0) {
                if (line2.charAt(0) == 65279) {
                    line2 = line2.substring(1);
                }
                if (this.bnfCommentString != null) {
                    int hashPos = line2.indexOf(this.bnfCommentString);
                    if (hashPos >= 0) {
                        line2 = line2.substring(0, hashPos);
                    }
                }
                String trimline = line2.trim();
                if (trimline.length() != 0) {
                    String linePart = line2;
                    if (linePart.trim().length() != 0) {
                        boolean terminated = trimline.endsWith(";");
                        if (terminated) {
                            linePart = linePart.substring(0, linePart.lastIndexOf(59));
                        }
                        int equalsPos = linePart.indexOf(this.bnfVariableInfix);
                        StringBuilder stringBuilder;
                        if (equalsPos >= 0) {
                            if (variable == null) {
                                variable = linePart.substring(0, equalsPos).trim();
                                if (variables.containsKey(variable)) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Duplicate variable definition in ");
                                    stringBuilder.append(line2);
                                    throw new IllegalArgumentException(stringBuilder.toString());
                                }
                                definition.append(linePart.substring(equalsPos + 1).trim());
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Missing ';' before ");
                                stringBuilder.append(count);
                                stringBuilder.append(") ");
                                stringBuilder.append(line2);
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        } else if (variable != null) {
                            definition.append(this.bnfLineSeparator);
                            definition.append(linePart);
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Missing '=' at ");
                            stringBuilder.append(count);
                            stringBuilder.append(") ");
                            stringBuilder.append(line2);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        if (terminated) {
                            variables.put(variable, definition.toString());
                            variable = null;
                            definition.setLength(0);
                        }
                    }
                }
            }
        }
        if (variable == null) {
            return variables;
        }
        throw new IllegalArgumentException("Missing ';' at end");
    }
}
