package android.icu.util;

import android.icu.impl.ICUData;
import android.icu.text.DateFormat;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;

public final class VersionInfo implements Comparable<VersionInfo> {
    @Deprecated
    public static final VersionInfo ICU_DATA_VERSION = getInstance(60, 2, 0, 0);
    @Deprecated
    public static final String ICU_DATA_VERSION_PATH = "60b";
    public static final VersionInfo ICU_VERSION = getInstance(60, 2, 0, 0);
    private static final String INVALID_VERSION_NUMBER_ = "Invalid version number: Version number may be negative or greater than 255";
    private static final int LAST_BYTE_MASK_ = 255;
    private static final ConcurrentHashMap<Integer, VersionInfo> MAP_ = new ConcurrentHashMap();
    private static volatile String TZDATA_VERSION = null;
    public static final VersionInfo UCOL_BUILDER_VERSION = getInstance(9);
    public static final VersionInfo UCOL_RUNTIME_VERSION = getInstance(9);
    @Deprecated
    public static final VersionInfo UCOL_TAILORINGS_VERSION = getInstance(1);
    public static final VersionInfo UNICODE_10_0 = getInstance(10, 0, 0, 0);
    public static final VersionInfo UNICODE_1_0 = getInstance(1, 0, 0, 0);
    public static final VersionInfo UNICODE_1_0_1 = getInstance(1, 0, 1, 0);
    public static final VersionInfo UNICODE_1_1_0 = getInstance(1, 1, 0, 0);
    public static final VersionInfo UNICODE_1_1_5 = getInstance(1, 1, 5, 0);
    public static final VersionInfo UNICODE_2_0 = getInstance(2, 0, 0, 0);
    public static final VersionInfo UNICODE_2_1_2 = getInstance(2, 1, 2, 0);
    public static final VersionInfo UNICODE_2_1_5 = getInstance(2, 1, 5, 0);
    public static final VersionInfo UNICODE_2_1_8 = getInstance(2, 1, 8, 0);
    public static final VersionInfo UNICODE_2_1_9 = getInstance(2, 1, 9, 0);
    public static final VersionInfo UNICODE_3_0 = getInstance(3, 0, 0, 0);
    public static final VersionInfo UNICODE_3_0_1 = getInstance(3, 0, 1, 0);
    public static final VersionInfo UNICODE_3_1_0 = getInstance(3, 1, 0, 0);
    public static final VersionInfo UNICODE_3_1_1 = getInstance(3, 1, 1, 0);
    public static final VersionInfo UNICODE_3_2 = getInstance(3, 2, 0, 0);
    public static final VersionInfo UNICODE_4_0 = getInstance(4, 0, 0, 0);
    public static final VersionInfo UNICODE_4_0_1 = getInstance(4, 0, 1, 0);
    public static final VersionInfo UNICODE_4_1 = getInstance(4, 1, 0, 0);
    public static final VersionInfo UNICODE_5_0 = getInstance(5, 0, 0, 0);
    public static final VersionInfo UNICODE_5_1 = getInstance(5, 1, 0, 0);
    public static final VersionInfo UNICODE_5_2 = getInstance(5, 2, 0, 0);
    public static final VersionInfo UNICODE_6_0 = getInstance(6, 0, 0, 0);
    public static final VersionInfo UNICODE_6_1 = getInstance(6, 1, 0, 0);
    public static final VersionInfo UNICODE_6_2 = getInstance(6, 2, 0, 0);
    public static final VersionInfo UNICODE_6_3 = getInstance(6, 3, 0, 0);
    public static final VersionInfo UNICODE_7_0 = getInstance(7, 0, 0, 0);
    public static final VersionInfo UNICODE_8_0 = getInstance(8, 0, 0, 0);
    public static final VersionInfo UNICODE_9_0 = getInstance(9, 0, 0, 0);
    private static final VersionInfo UNICODE_VERSION = UNICODE_10_0;
    private static volatile VersionInfo javaVersion;
    private int m_version_;

    public static VersionInfo getInstance(String version) {
        int length = version.length();
        int[] array = new int[]{0, 0, 0, 0};
        int count = 0;
        int index = 0;
        while (count < 4 && index < length) {
            char c = version.charAt(index);
            if (c == '.') {
                count++;
            } else {
                c = (char) (c - 48);
                if (c < 0 || c > 9) {
                    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
                }
                array[count] = array[count] * 10;
                array[count] = array[count] + c;
            }
            index++;
        }
        if (index == length) {
            int i = 0;
            while (i < 4) {
                if (array[i] < 0 || array[i] > 255) {
                    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
                }
                i++;
            }
            return getInstance(array[0], array[1], array[2], array[3]);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid version number: String '");
        stringBuilder.append(version);
        stringBuilder.append("' exceeds version format");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static VersionInfo getInstance(int major, int minor, int milli, int micro) {
        if (major < 0 || major > 255 || minor < 0 || minor > 255 || milli < 0 || milli > 255 || micro < 0 || micro > 255) {
            throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
        }
        int version = getInt(major, minor, milli, micro);
        Integer key = Integer.valueOf(version);
        VersionInfo result = (VersionInfo) MAP_.get(key);
        if (result != null) {
            return result;
        }
        result = new VersionInfo(version);
        VersionInfo tmpvi = (VersionInfo) MAP_.putIfAbsent(key, result);
        if (tmpvi != null) {
            return tmpvi;
        }
        return result;
    }

    public static VersionInfo getInstance(int major, int minor, int milli) {
        return getInstance(major, minor, milli, 0);
    }

    public static VersionInfo getInstance(int major, int minor) {
        return getInstance(major, minor, 0, 0);
    }

    public static VersionInfo getInstance(int major) {
        return getInstance(major, 0, 0, 0);
    }

    @Deprecated
    public static VersionInfo javaVersion() {
        if (javaVersion == null) {
            synchronized (VersionInfo.class) {
                if (javaVersion == null) {
                    char[] chars = System.getProperty("java.version").toCharArray();
                    int count = 0;
                    int w = 0;
                    char c = 0;
                    boolean numeric = false;
                    while (c < chars.length) {
                        char r = c + 1;
                        c = chars[c];
                        if (c >= '0') {
                            if (c <= '9') {
                                numeric = true;
                                int w2 = w + 1;
                                chars[w] = c;
                                w = w2;
                                c = r;
                            }
                        }
                        if (!numeric) {
                            continue;
                        } else if (count == 3) {
                            c = r;
                            break;
                        } else {
                            numeric = false;
                            int w3 = w + 1;
                            chars[w] = '.';
                            count++;
                            w = w3;
                        }
                        c = r;
                    }
                    while (w > 0 && chars[w - 1] == '.') {
                        w--;
                    }
                    javaVersion = getInstance(new String(chars, 0, w));
                }
            }
        }
        return javaVersion;
    }

    public String toString() {
        StringBuilder result = new StringBuilder(7);
        result.append(getMajor());
        result.append('.');
        result.append(getMinor());
        result.append('.');
        result.append(getMilli());
        result.append('.');
        result.append(getMicro());
        return result.toString();
    }

    public int getMajor() {
        return (this.m_version_ >> 24) & 255;
    }

    public int getMinor() {
        return (this.m_version_ >> 16) & 255;
    }

    public int getMilli() {
        return (this.m_version_ >> 8) & 255;
    }

    public int getMicro() {
        return this.m_version_ & 255;
    }

    public boolean equals(Object other) {
        return other == this;
    }

    public int hashCode() {
        return this.m_version_;
    }

    public int compareTo(VersionInfo other) {
        return this.m_version_ - other.m_version_;
    }

    private VersionInfo(int compactversion) {
        this.m_version_ = compactversion;
    }

    private static int getInt(int major, int minor, int milli, int micro) {
        return (((major << 24) | (minor << 16)) | (milli << 8)) | micro;
    }

    public static void main(String[] args) {
        StringBuilder stringBuilder;
        String icuApiVer;
        if (ICU_VERSION.getMajor() <= 4) {
            if (ICU_VERSION.getMinor() % 2 != 0) {
                int major = ICU_VERSION.getMajor();
                int minor = ICU_VERSION.getMinor() + 1;
                if (minor >= 10) {
                    minor -= 10;
                    major++;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(major);
                stringBuilder.append(".");
                stringBuilder.append(minor);
                stringBuilder.append(DateFormat.NUM_MONTH);
                stringBuilder.append(ICU_VERSION.getMilli());
                icuApiVer = stringBuilder.toString();
            } else {
                icuApiVer = ICU_VERSION.getVersionString(2, 2);
            }
        } else if (ICU_VERSION.getMinor() == 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("");
            stringBuilder2.append(ICU_VERSION.getMajor());
            stringBuilder2.append(DateFormat.NUM_MONTH);
            stringBuilder2.append(ICU_VERSION.getMilli());
            icuApiVer = stringBuilder2.toString();
        } else {
            icuApiVer = ICU_VERSION.getVersionString(2, 2);
        }
        PrintStream printStream = System.out;
        stringBuilder = new StringBuilder();
        stringBuilder.append("International Components for Unicode for Java ");
        stringBuilder.append(icuApiVer);
        printStream.println(stringBuilder.toString());
        System.out.println("");
        printStream = System.out;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Implementation Version: ");
        stringBuilder.append(ICU_VERSION.getVersionString(2, 4));
        printStream.println(stringBuilder.toString());
        printStream = System.out;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unicode Data Version:   ");
        stringBuilder.append(UNICODE_VERSION.getVersionString(2, 4));
        printStream.println(stringBuilder.toString());
        printStream = System.out;
        stringBuilder = new StringBuilder();
        stringBuilder.append("CLDR Data Version:      ");
        stringBuilder.append(LocaleData.getCLDRVersion().getVersionString(2, 4));
        printStream.println(stringBuilder.toString());
        PrintStream printStream2 = System.out;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Time Zone Data Version: ");
        stringBuilder3.append(getTZDataVersion());
        printStream2.println(stringBuilder3.toString());
    }

    @Deprecated
    public String getVersionString(int minDigits, int maxDigits) {
        int i = 1;
        if (minDigits < 1 || maxDigits < 1 || minDigits > 4 || maxDigits > 4 || minDigits > maxDigits) {
            throw new IllegalArgumentException("Invalid min/maxDigits range");
        }
        int[] digits = new int[]{getMajor(), getMinor(), getMilli(), getMicro()};
        int numDigits = maxDigits;
        while (numDigits > minDigits && digits[numDigits - 1] == 0) {
            numDigits--;
        }
        StringBuilder verStr = new StringBuilder(7);
        verStr.append(digits[0]);
        while (i < numDigits) {
            verStr.append(".");
            verStr.append(digits[i]);
            i++;
        }
        return verStr.toString();
    }

    static String getTZDataVersion() {
        if (TZDATA_VERSION == null) {
            synchronized (VersionInfo.class) {
                if (TZDATA_VERSION == null) {
                    TZDATA_VERSION = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "zoneinfo64").getString("TZVersion");
                }
            }
        }
        return TZDATA_VERSION;
    }
}
