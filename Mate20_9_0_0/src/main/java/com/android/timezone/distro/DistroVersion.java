package com.android.timezone.distro;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistroVersion {
    public static final int CURRENT_FORMAT_MAJOR_VERSION = 2;
    public static final int CURRENT_FORMAT_MINOR_VERSION = 1;
    public static final int DISTRO_VERSION_FILE_LENGTH = ((((FORMAT_VERSION_STRING_LENGTH + 1) + 5) + 1) + 3);
    private static final Pattern DISTRO_VERSION_PATTERN;
    private static final Pattern FORMAT_VERSION_PATTERN = Pattern.compile("(\\d{3})\\.(\\d{3})");
    private static final int FORMAT_VERSION_STRING_LENGTH = FULL_CURRENT_FORMAT_VERSION_STRING.length();
    private static final String FULL_CURRENT_FORMAT_VERSION_STRING = toFormatVersionString(2, 1);
    private static final int REVISION_LENGTH = 3;
    private static final Pattern REVISION_PATTERN = Pattern.compile("(\\d{3})");
    private static final int RULES_VERSION_LENGTH = 5;
    private static final Pattern RULES_VERSION_PATTERN = Pattern.compile("(\\d{4}\\w)");
    public final int formatMajorVersion;
    public final int formatMinorVersion;
    public final int revision;
    public final String rulesVersion;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FORMAT_VERSION_PATTERN.pattern());
        stringBuilder.append("\\|");
        stringBuilder.append(RULES_VERSION_PATTERN.pattern());
        stringBuilder.append("\\|");
        stringBuilder.append(REVISION_PATTERN.pattern());
        stringBuilder.append(".*");
        DISTRO_VERSION_PATTERN = Pattern.compile(stringBuilder.toString());
    }

    public DistroVersion(int formatMajorVersion, int formatMinorVersion, String rulesVersion, int revision) throws DistroException {
        this.formatMajorVersion = validate3DigitVersion(formatMajorVersion);
        this.formatMinorVersion = validate3DigitVersion(formatMinorVersion);
        if (RULES_VERSION_PATTERN.matcher(rulesVersion).matches()) {
            this.rulesVersion = rulesVersion;
            this.revision = validate3DigitVersion(revision);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid rulesVersion: ");
        stringBuilder.append(rulesVersion);
        throw new DistroException(stringBuilder.toString());
    }

    public static DistroVersion fromBytes(byte[] bytes) throws DistroException {
        String distroVersion = new String(bytes, StandardCharsets.US_ASCII);
        StringBuilder stringBuilder;
        try {
            Matcher matcher = DISTRO_VERSION_PATTERN.matcher(distroVersion);
            if (matcher.matches()) {
                String formatMajorVersion = matcher.group(1);
                String formatMinorVersion = matcher.group(2);
                return new DistroVersion(from3DigitVersionString(formatMajorVersion), from3DigitVersionString(formatMinorVersion), matcher.group(3), from3DigitVersionString(matcher.group(4)));
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid distro version string: \"");
            stringBuilder.append(distroVersion);
            stringBuilder.append("\"");
            throw new DistroException(stringBuilder.toString());
        } catch (IndexOutOfBoundsException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Distro version string too short: \"");
            stringBuilder.append(distroVersion);
            stringBuilder.append("\"");
            throw new DistroException(stringBuilder.toString());
        }
    }

    public byte[] toBytes() {
        return toBytes(this.formatMajorVersion, this.formatMinorVersion, this.rulesVersion, this.revision);
    }

    public static byte[] toBytes(int majorFormatVersion, int minorFormatVerison, String rulesVersion, int revision) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toFormatVersionString(majorFormatVersion, minorFormatVerison));
        stringBuilder.append("|");
        stringBuilder.append(rulesVersion);
        stringBuilder.append("|");
        stringBuilder.append(to3DigitVersionString(revision));
        return stringBuilder.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public static boolean isCompatibleWithThisDevice(DistroVersion distroVersion) {
        return 2 == distroVersion.formatMajorVersion && 1 <= distroVersion.formatMinorVersion;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DistroVersion that = (DistroVersion) o;
        if (this.formatMajorVersion == that.formatMajorVersion && this.formatMinorVersion == that.formatMinorVersion && this.revision == that.revision) {
            return this.rulesVersion.equals(that.rulesVersion);
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * this.formatMajorVersion) + this.formatMinorVersion)) + this.rulesVersion.hashCode())) + this.revision;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DistroVersion{formatMajorVersion=");
        stringBuilder.append(this.formatMajorVersion);
        stringBuilder.append(", formatMinorVersion=");
        stringBuilder.append(this.formatMinorVersion);
        stringBuilder.append(", rulesVersion='");
        stringBuilder.append(this.rulesVersion);
        stringBuilder.append('\'');
        stringBuilder.append(", revision=");
        stringBuilder.append(this.revision);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String to3DigitVersionString(int version) {
        try {
            return String.format(Locale.ROOT, "%03d", new Object[]{Integer.valueOf(validate3DigitVersion(version))});
        } catch (DistroException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int from3DigitVersionString(String versionString) throws DistroException {
        String parseErrorMessage = "versionString must be a zero padded, 3 digit, positive decimal integer";
        if (versionString.length() == 3) {
            try {
                return validate3DigitVersion(Integer.parseInt(versionString));
            } catch (NumberFormatException e) {
                throw new DistroException("versionString must be a zero padded, 3 digit, positive decimal integer", e);
            }
        }
        throw new DistroException("versionString must be a zero padded, 3 digit, positive decimal integer");
    }

    private static int validate3DigitVersion(int value) throws DistroException {
        if (value >= 0 && value <= 999) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected 0 <= value <= 999, was ");
        stringBuilder.append(value);
        throw new DistroException(stringBuilder.toString());
    }

    private static String toFormatVersionString(int majorFormatVersion, int minorFormatVersion) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(to3DigitVersionString(majorFormatVersion));
        stringBuilder.append(".");
        stringBuilder.append(to3DigitVersionString(minorFormatVersion));
        return stringBuilder.toString();
    }
}
