package sun.nio.fs;

import java.sql.Types;
import java.util.regex.PatternSyntaxException;

public class Globs {
    private static char EOL = 0;
    private static final String globMetaChars = "\\*?[{";
    private static final String regexMetaChars = ".^$+{[]|()";

    private Globs() {
    }

    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf((int) c) != -1;
    }

    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf((int) c) != -1;
    }

    private static char next(String glob, int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }

    /* JADX WARNING: Removed duplicated region for block: B:109:0x011c A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0115  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String toRegexPattern(String globPattern, boolean isDos) {
        StringBuilder regex = new StringBuilder("^");
        boolean inGroup = false;
        int i = 0;
        while (i < globPattern.length()) {
            int i2 = i + 1;
            char c = globPattern.charAt(i);
            if (c != '*') {
                if (c != ',') {
                    if (c != '/') {
                        if (c != '?') {
                            if (c != '{') {
                                if (c != '}') {
                                    switch (c) {
                                        case Types.DATE /*91*/:
                                            if (isDos) {
                                                regex.append("[[^\\\\]&&[");
                                            } else {
                                                regex.append("[[^/]&&[");
                                            }
                                            if (next(globPattern, i2) == '^') {
                                                regex.append("\\^");
                                                i2++;
                                            } else {
                                                if (next(globPattern, i2) == '!') {
                                                    regex.append('^');
                                                    i2++;
                                                }
                                                if (next(globPattern, i2) == '-') {
                                                    regex.append('-');
                                                    i2++;
                                                }
                                            }
                                            boolean hasRangeStart = false;
                                            char c2 = c;
                                            c = 0;
                                            while (i2 < globPattern.length()) {
                                                int i3 = i2 + 1;
                                                c2 = globPattern.charAt(i2);
                                                if (c2 == ']') {
                                                    i2 = i3;
                                                } else if (c2 == '/' || (isDos && c2 == '\\')) {
                                                    throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern, i3 - 1);
                                                } else {
                                                    if (c2 == '\\' || c2 == '[' || (c2 == '&' && next(globPattern, i3) == '&')) {
                                                        regex.append('\\');
                                                    }
                                                    regex.append(c2);
                                                    if (c2 != '-') {
                                                        hasRangeStart = true;
                                                        c = c2;
                                                        i2 = i3;
                                                    } else if (hasRangeStart) {
                                                        i2 = i3 + 1;
                                                        char next = next(globPattern, i3);
                                                        c2 = next;
                                                        if (!(next == EOL || c2 == ']')) {
                                                            if (c2 >= c) {
                                                                regex.append(c2);
                                                                hasRangeStart = false;
                                                            } else {
                                                                throw new PatternSyntaxException("Invalid range", globPattern, i2 - 3);
                                                            }
                                                        }
                                                    } else {
                                                        throw new PatternSyntaxException("Invalid range", globPattern, i3 - 1);
                                                    }
                                                }
                                                if (c2 != ']') {
                                                    regex.append("]]");
                                                    break;
                                                }
                                                throw new PatternSyntaxException("Missing ']", globPattern, i2 - 1);
                                            }
                                            if (c2 != ']') {
                                            }
                                            break;
                                        case Types.TIME /*92*/:
                                            if (i2 != globPattern.length()) {
                                                int i4 = i2 + 1;
                                                char i5 = globPattern.charAt(i2);
                                                if (isGlobMeta(i5) || isRegexMeta(i5)) {
                                                    regex.append('\\');
                                                }
                                                regex.append(i5);
                                                i = i4;
                                                continue;
                                            } else {
                                                throw new PatternSyntaxException("No character to escape", globPattern, i2 - 1);
                                            }
                                        default:
                                            if (isRegexMeta(c)) {
                                                regex.append('\\');
                                            }
                                            regex.append(c);
                                            break;
                                    }
                                } else if (inGroup) {
                                    regex.append("))");
                                    inGroup = false;
                                } else {
                                    regex.append('}');
                                }
                            } else if (inGroup) {
                                throw new PatternSyntaxException("Cannot nest groups", globPattern, i2 - 1);
                            } else {
                                regex.append("(?:(?:");
                                inGroup = true;
                            }
                        } else if (isDos) {
                            regex.append("[^\\\\]");
                        } else {
                            regex.append("[^/]");
                        }
                    } else if (isDos) {
                        regex.append("\\\\");
                    } else {
                        regex.append(c);
                    }
                } else if (inGroup) {
                    regex.append(")|(?:");
                } else {
                    regex.append(',');
                }
            } else if (next(globPattern, i2) == '*') {
                regex.append(".*");
                i2++;
            } else if (isDos) {
                regex.append("[^\\\\]*");
            } else {
                regex.append("[^/]*");
            }
            i = i2;
        }
        if (inGroup) {
            throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
        }
        regex.append('$');
        return regex.toString();
    }

    static String toUnixRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, false);
    }

    static String toWindowsRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, true);
    }
}
