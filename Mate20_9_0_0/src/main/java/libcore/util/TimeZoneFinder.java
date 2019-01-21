package libcore.util;

import android.icu.util.TimeZone;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import libcore.util.CountryTimeZones.OffsetResult;
import libcore.util.CountryTimeZones.TimeZoneMapping;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class TimeZoneFinder {
    private static final String COUNTRY_CODE_ATTRIBUTE = "code";
    private static final String COUNTRY_ELEMENT = "country";
    private static final String COUNTRY_ZONES_ELEMENT = "countryzones";
    private static final String DEFAULT_TIME_ZONE_ID_ATTRIBUTE = "default";
    private static final String EVER_USES_UTC_ATTRIBUTE = "everutc";
    private static final String FALSE_ATTRIBUTE_VALUE = "n";
    private static final String IANA_VERSION_ATTRIBUTE = "ianaversion";
    private static final String TIMEZONES_ELEMENT = "timezones";
    private static final String TRUE_ATTRIBUTE_VALUE = "y";
    private static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private static final String ZONE_ID_ELEMENT = "id";
    private static final String ZONE_NOT_USED_AFTER_ATTRIBUTE = "notafter";
    private static final String ZONE_SHOW_IN_PICKER_ATTRIBUTE = "picker";
    private static TimeZoneFinder instance;
    private CountryTimeZones lastCountryTimeZones;
    private final ReaderSupplier xmlSource;

    private interface ReaderSupplier {
        Reader get() throws IOException;

        static ReaderSupplier forFile(String fileName, Charset charSet) throws IOException {
            Path file = Paths.get(fileName, new String[0]);
            StringBuilder stringBuilder;
            if (!Files.exists(file, new LinkOption[0])) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(fileName);
                stringBuilder.append(" does not exist");
                throw new FileNotFoundException(stringBuilder.toString());
            } else if (Files.isRegularFile(file, new LinkOption[0]) || !Files.isReadable(file)) {
                return new -$$Lambda$TimeZoneFinder$ReaderSupplier$IAVNuAYizGfcsPtGXEBkDPhlBF0(file, charSet);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(fileName);
                stringBuilder.append(" must be a regular readable file.");
                throw new IOException(stringBuilder.toString());
            }
        }

        static ReaderSupplier forString(String xml) {
            return new -$$Lambda$TimeZoneFinder$ReaderSupplier$XQfCWjApX_mZlUFF8542zlelCgU(xml);
        }
    }

    private interface TimeZonesProcessor {
        public static final boolean CONTINUE = true;
        public static final boolean HALT = false;

        boolean processHeader(String ianaVersion) throws XmlPullParserException {
            return true;
        }

        boolean processCountryZones(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> list, String debugInfo) throws XmlPullParserException {
            return true;
        }
    }

    private static class CountryZonesLookupExtractor implements TimeZonesProcessor {
        private List<CountryTimeZones> countryTimeZonesList;

        private CountryZonesLookupExtractor() {
            this.countryTimeZonesList = new ArrayList(250);
        }

        public boolean processCountryZones(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> timeZoneMappings, String debugInfo) throws XmlPullParserException {
            this.countryTimeZonesList.add(CountryTimeZones.createValidated(countryIso, defaultTimeZoneId, everUsesUtc, timeZoneMappings, debugInfo));
            return true;
        }

        CountryZonesFinder getCountryZonesLookup() {
            return new CountryZonesFinder(this.countryTimeZonesList);
        }
    }

    private static class IanaVersionExtractor implements TimeZonesProcessor {
        private String ianaVersion;

        private IanaVersionExtractor() {
        }

        public boolean processHeader(String ianaVersion) throws XmlPullParserException {
            this.ianaVersion = ianaVersion;
            return false;
        }

        public String getIanaVersion() {
            return this.ianaVersion;
        }
    }

    private static class SelectiveCountryTimeZonesExtractor implements TimeZonesProcessor {
        private final String countryCodeToMatch;
        private CountryTimeZones validatedCountryTimeZones;

        private SelectiveCountryTimeZonesExtractor(String countryCodeToMatch) {
            this.countryCodeToMatch = TimeZoneFinder.normalizeCountryIso(countryCodeToMatch);
        }

        public boolean processCountryZones(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> timeZoneMappings, String debugInfo) {
            countryIso = TimeZoneFinder.normalizeCountryIso(countryIso);
            if (!this.countryCodeToMatch.equals(countryIso)) {
                return true;
            }
            this.validatedCountryTimeZones = CountryTimeZones.createValidated(countryIso, defaultTimeZoneId, everUsesUtc, timeZoneMappings, debugInfo);
            return false;
        }

        CountryTimeZones getValidatedCountryTimeZones() {
            return this.validatedCountryTimeZones;
        }
    }

    private static class TimeZonesValidator implements TimeZonesProcessor {
        private final Set<String> knownCountryCodes;

        private TimeZonesValidator() {
            this.knownCountryCodes = new HashSet();
        }

        public boolean processCountryZones(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> timeZoneMappings, String debugInfo) throws XmlPullParserException {
            StringBuilder stringBuilder;
            if (!TimeZoneFinder.normalizeCountryIso(countryIso).equals(countryIso)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Country code: ");
                stringBuilder.append(countryIso);
                stringBuilder.append(" is not normalized at ");
                stringBuilder.append(debugInfo);
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (this.knownCountryCodes.contains(countryIso)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Second entry for country code: ");
                stringBuilder.append(countryIso);
                stringBuilder.append(" at ");
                stringBuilder.append(debugInfo);
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (timeZoneMappings.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No time zone IDs for country code: ");
                stringBuilder.append(countryIso);
                stringBuilder.append(" at ");
                stringBuilder.append(debugInfo);
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (TimeZoneMapping.containsTimeZoneId(timeZoneMappings, defaultTimeZoneId)) {
                this.knownCountryCodes.add(countryIso);
                return true;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("defaultTimeZoneId for country code: ");
                stringBuilder.append(countryIso);
                stringBuilder.append(" is not one of the zones ");
                stringBuilder.append(timeZoneMappings);
                stringBuilder.append(" at ");
                stringBuilder.append(debugInfo);
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    private TimeZoneFinder(ReaderSupplier xmlSource) {
        this.xmlSource = xmlSource;
    }

    public static TimeZoneFinder getInstance() {
        synchronized (TimeZoneFinder.class) {
            if (instance == null) {
                String[] tzLookupFilePaths = TimeZoneDataFiles.getTimeZoneFilePaths(TZLOOKUP_FILE_NAME);
                instance = createInstanceWithFallback(tzLookupFilePaths[0], tzLookupFilePaths[1]);
            }
        }
        return instance;
    }

    public static TimeZoneFinder createInstanceWithFallback(String... tzLookupFilePaths) {
        IOException lastException = null;
        int length = tzLookupFilePaths.length;
        int i = 0;
        while (i < length) {
            try {
                return createInstance(tzLookupFilePaths[i]);
            } catch (IOException e) {
                if (lastException != null) {
                    e.addSuppressed(lastException);
                }
                lastException = e;
                i++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No valid file found in set: ");
        stringBuilder.append(Arrays.toString(tzLookupFilePaths));
        stringBuilder.append(" Printing exceptions and falling back to empty map.");
        System.logE(stringBuilder.toString(), lastException);
        return createInstanceForTests("<timezones><countryzones /></timezones>");
    }

    public static TimeZoneFinder createInstance(String path) throws IOException {
        return new TimeZoneFinder(ReaderSupplier.forFile(path, StandardCharsets.UTF_8));
    }

    public static TimeZoneFinder createInstanceForTests(String xml) {
        return new TimeZoneFinder(ReaderSupplier.forString(xml));
    }

    public void validate() throws IOException {
        try {
            processXml(new TimeZonesValidator());
        } catch (XmlPullParserException e) {
            throw new IOException("Parsing error", e);
        }
    }

    public String getIanaVersion() {
        IanaVersionExtractor ianaVersionExtractor = new IanaVersionExtractor();
        try {
            processXml(ianaVersionExtractor);
            return ianaVersionExtractor.getIanaVersion();
        } catch (IOException | XmlPullParserException e) {
            return null;
        }
    }

    public CountryZonesFinder getCountryZonesFinder() {
        CountryZonesLookupExtractor extractor = new CountryZonesLookupExtractor();
        try {
            processXml(extractor);
            return extractor.getCountryZonesLookup();
        } catch (IOException | XmlPullParserException e) {
            System.logW("Error reading country zones ", e);
            return null;
        }
    }

    public TimeZone lookupTimeZoneByCountryAndOffset(String countryIso, int offsetMillis, boolean isDst, long whenMillis, TimeZone bias) {
        CountryTimeZones countryTimeZones = lookupCountryTimeZones(countryIso);
        TimeZone timeZone = null;
        if (countryTimeZones == null) {
            return null;
        }
        OffsetResult offsetResult = countryTimeZones.lookupByOffsetWithBias(offsetMillis, isDst, whenMillis, bias);
        if (offsetResult != null) {
            timeZone = offsetResult.mTimeZone;
        }
        return timeZone;
    }

    public String lookupDefaultTimeZoneIdByCountry(String countryIso) {
        CountryTimeZones countryTimeZones = lookupCountryTimeZones(countryIso);
        return countryTimeZones == null ? null : countryTimeZones.getDefaultTimeZoneId();
    }

    public List<TimeZone> lookupTimeZonesByCountry(String countryIso) {
        CountryTimeZones countryTimeZones = lookupCountryTimeZones(countryIso);
        return countryTimeZones == null ? null : countryTimeZones.getIcuTimeZones();
    }

    public List<String> lookupTimeZoneIdsByCountry(String countryIso) {
        CountryTimeZones countryTimeZones = lookupCountryTimeZones(countryIso);
        return countryTimeZones == null ? null : extractTimeZoneIds(countryTimeZones.getTimeZoneMappings());
    }

    /* JADX WARNING: Missing block: B:10:0x0012, code skipped:
            r0 = new libcore.util.TimeZoneFinder.SelectiveCountryTimeZonesExtractor(r5, null);
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            processXml(r0);
            r2 = r0.getValidatedCountryTimeZones();
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            if (r2 != null) goto L_0x0022;
     */
    /* JADX WARNING: Missing block: B:14:0x0021, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:15:0x0022, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            r4.lastCountryTimeZones = r2;
     */
    /* JADX WARNING: Missing block: B:18:0x0025, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:19:0x0026, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:24:0x002a, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:25:0x002b, code skipped:
            java.lang.System.logW("Error reading country zones ", r2);
     */
    /* JADX WARNING: Missing block: B:26:0x0030, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public CountryTimeZones lookupCountryTimeZones(String countryIso) {
        synchronized (this) {
            if (this.lastCountryTimeZones == null || !this.lastCountryTimeZones.isForCountryCode(countryIso)) {
            } else {
                CountryTimeZones countryTimeZones = this.lastCountryTimeZones;
                return countryTimeZones;
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x005a, code skipped:
            if (r0 != null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:23:0x005c, code skipped:
            if (r1 != null) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:26:0x0062, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:0x0063, code skipped:
            r1.addSuppressed(r3);
     */
    /* JADX WARNING: Missing block: B:28:0x0067, code skipped:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void processXml(TimeZonesProcessor processor) throws XmlPullParserException, IOException {
        Reader reader = this.xmlSource.get();
        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        xmlPullParserFactory.setNamespaceAware(false);
        XmlPullParser parser = xmlPullParserFactory.newPullParser();
        parser.setInput(reader);
        findRequiredStartTag(parser, TIMEZONES_ELEMENT);
        if (processor.processHeader(parser.getAttributeValue(null, IANA_VERSION_ATTRIBUTE))) {
            findRequiredStartTag(parser, COUNTRY_ZONES_ELEMENT);
            if (processCountryZones(parser, processor)) {
                checkOnEndTag(parser, COUNTRY_ZONES_ELEMENT);
                parser.next();
                consumeUntilEndTag(parser, TIMEZONES_ELEMENT);
                checkOnEndTag(parser, TIMEZONES_ELEMENT);
                if (reader != null) {
                    reader.close();
                }
                return;
            }
            if (reader != null) {
                reader.close();
            }
            return;
        }
        if (reader != null) {
            reader.close();
        }
    }

    private static boolean processCountryZones(XmlPullParser parser, TimeZonesProcessor processor) throws IOException, XmlPullParserException {
        while (findOptionalStartTag(parser, COUNTRY_ELEMENT)) {
            if (processor == null) {
                consumeUntilEndTag(parser, COUNTRY_ELEMENT);
            } else {
                String code = parser.getAttributeValue(null, COUNTRY_CODE_ATTRIBUTE);
                StringBuilder stringBuilder;
                if (code == null || code.isEmpty()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find country code: ");
                    stringBuilder.append(parser.getPositionDescription());
                    throw new XmlPullParserException(stringBuilder.toString());
                }
                String defaultTimeZoneId = parser.getAttributeValue(null, DEFAULT_TIME_ZONE_ID_ATTRIBUTE);
                if (defaultTimeZoneId == null || defaultTimeZoneId.isEmpty()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find default time zone ID: ");
                    stringBuilder.append(parser.getPositionDescription());
                    throw new XmlPullParserException(stringBuilder.toString());
                }
                Boolean everUsesUtc = parseBooleanAttribute(parser, EVER_USES_UTC_ATTRIBUTE, null);
                if (everUsesUtc != null) {
                    String debugInfo = parser.getPositionDescription();
                    if (!processor.processCountryZones(code, defaultTimeZoneId, everUsesUtc.booleanValue(), parseTimeZoneMappings(parser), debugInfo)) {
                        return false;
                    }
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to find UTC hint attribute (everutc): ");
                    stringBuilder2.append(parser.getPositionDescription());
                    throw new XmlPullParserException(stringBuilder2.toString());
                }
            }
            checkOnEndTag(parser, COUNTRY_ELEMENT);
        }
        return true;
    }

    private static List<TimeZoneMapping> parseTimeZoneMappings(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<TimeZoneMapping> timeZoneMappings = new ArrayList();
        while (findOptionalStartTag(parser, ZONE_ID_ELEMENT)) {
            boolean showInPicker = parseBooleanAttribute(parser, ZONE_SHOW_IN_PICKER_ATTRIBUTE, Boolean.valueOf(true)).booleanValue();
            Long notUsedAfter = parseLongAttribute(parser, ZONE_NOT_USED_AFTER_ATTRIBUTE, null);
            String zoneIdString = consumeText(parser);
            checkOnEndTag(parser, ZONE_ID_ELEMENT);
            if (zoneIdString == null || zoneIdString.length() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Missing text for id): ");
                stringBuilder.append(parser.getPositionDescription());
                throw new XmlPullParserException(stringBuilder.toString());
            }
            timeZoneMappings.add(new TimeZoneMapping(zoneIdString, showInPicker, notUsedAfter));
        }
        return Collections.unmodifiableList(timeZoneMappings);
    }

    private static Long parseLongAttribute(XmlPullParser parser, String attributeName, Long defaultValue) throws XmlPullParserException {
        String attributeValueString = parser.getAttributeValue(null, attributeName);
        if (attributeValueString == null) {
            return defaultValue;
        }
        try {
            return Long.valueOf(Long.parseLong(attributeValueString));
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attribute \"");
            stringBuilder.append(attributeName);
            stringBuilder.append("\" is not a long value: ");
            stringBuilder.append(parser.getPositionDescription());
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    private static Boolean parseBooleanAttribute(XmlPullParser parser, String attributeName, Boolean defaultValue) throws XmlPullParserException {
        String attributeValueString = parser.getAttributeValue(null, attributeName);
        if (attributeValueString == null) {
            return defaultValue;
        }
        boolean isTrue = "y".equals(attributeValueString);
        if (isTrue || FALSE_ATTRIBUTE_VALUE.equals(attributeValueString)) {
            return Boolean.valueOf(isTrue);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute \"");
        stringBuilder.append(attributeName);
        stringBuilder.append("\" is not \"y\" or \"n\": ");
        stringBuilder.append(parser.getPositionDescription());
        throw new XmlPullParserException(stringBuilder.toString());
    }

    private static void findRequiredStartTag(XmlPullParser parser, String elementName) throws IOException, XmlPullParserException {
        findStartTag(parser, elementName, true);
    }

    private static boolean findOptionalStartTag(XmlPullParser parser, String elementName) throws IOException, XmlPullParserException {
        return findStartTag(parser, elementName, false);
    }

    private static boolean findStartTag(XmlPullParser parser, String elementName, boolean elementRequired) throws IOException, XmlPullParserException {
        while (true) {
            int next = parser.next();
            int type = next;
            StringBuilder stringBuilder;
            if (next != 1) {
                switch (type) {
                    case 2:
                        String currentElementName = parser.getName();
                        if (!elementName.equals(currentElementName)) {
                            parser.next();
                            consumeUntilEndTag(parser, currentElementName);
                            break;
                        }
                        return true;
                    case 3:
                        if (!elementRequired) {
                            return false;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No child element found with name ");
                        stringBuilder.append(elementName);
                        throw new XmlPullParserException(stringBuilder.toString());
                    default:
                        break;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected end of document while looking for ");
            stringBuilder.append(elementName);
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    private static void consumeUntilEndTag(XmlPullParser parser, String elementName) throws IOException, XmlPullParserException {
        if (parser.getEventType() != 3 || !elementName.equals(parser.getName())) {
            int requiredDepth = parser.getDepth();
            if (parser.getEventType() == 2) {
                requiredDepth--;
            }
            while (parser.getEventType() != 1) {
                int type = parser.next();
                int currentDepth = parser.getDepth();
                StringBuilder stringBuilder;
                if (currentDepth < requiredDepth) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected depth while looking for end tag: ");
                    stringBuilder.append(parser.getPositionDescription());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (currentDepth == requiredDepth && type == 3) {
                    if (!elementName.equals(parser.getName())) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unexpected eng tag: ");
                        stringBuilder.append(parser.getPositionDescription());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                    return;
                }
            }
            throw new XmlPullParserException("Unexpected end of document");
        }
    }

    private static String consumeText(XmlPullParser parser) throws IOException, XmlPullParserException {
        int type = parser.next();
        if (type == 4) {
            String text = parser.getText();
            type = parser.next();
            if (type == 3) {
                return text;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected nested tag or end of document when expecting text: type=");
            stringBuilder.append(type);
            stringBuilder.append(" at ");
            stringBuilder.append(parser.getPositionDescription());
            throw new XmlPullParserException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Text not found. Found type=");
        stringBuilder2.append(type);
        stringBuilder2.append(" at ");
        stringBuilder2.append(parser.getPositionDescription());
        throw new XmlPullParserException(stringBuilder2.toString());
    }

    private static void checkOnEndTag(XmlPullParser parser, String elementName) throws XmlPullParserException {
        if (parser.getEventType() != 3 || !parser.getName().equals(elementName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected tag encountered: ");
            stringBuilder.append(parser.getPositionDescription());
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    private static List<String> extractTimeZoneIds(List<TimeZoneMapping> timeZoneMappings) {
        List<String> zoneIds = new ArrayList(timeZoneMappings.size());
        for (TimeZoneMapping timeZoneMapping : timeZoneMappings) {
            zoneIds.add(timeZoneMapping.timeZoneId);
        }
        return Collections.unmodifiableList(zoneIds);
    }

    static String normalizeCountryIso(String countryIso) {
        return countryIso.toLowerCase(Locale.US);
    }
}
