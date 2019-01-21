package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.prefixmapper.PrefixTimeZonesMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneNumberToTimeZonesMapper {
    private static final String MAPPING_DATA_DIRECTORY = "/com/android/i18n/phonenumbers/timezones/data/";
    private static final String MAPPING_DATA_FILE_NAME = "map_data";
    private static final String UNKNOWN_TIMEZONE = "Etc/Unknown";
    static final List<String> UNKNOWN_TIME_ZONE_LIST = new ArrayList(1);
    private static final Logger logger = Logger.getLogger(PhoneNumberToTimeZonesMapper.class.getName());
    private PrefixTimeZonesMap prefixTimeZonesMap;

    private static class LazyHolder {
        private static final PhoneNumberToTimeZonesMapper INSTANCE = new PhoneNumberToTimeZonesMapper(PhoneNumberToTimeZonesMapper.loadPrefixTimeZonesMapFromFile("/com/android/i18n/phonenumbers/timezones/data/map_data"));

        private LazyHolder() {
        }
    }

    static {
        UNKNOWN_TIME_ZONE_LIST.add(UNKNOWN_TIMEZONE);
    }

    PhoneNumberToTimeZonesMapper(String prefixTimeZonesMapDataDirectory) {
        this.prefixTimeZonesMap = null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefixTimeZonesMapDataDirectory);
        stringBuilder.append(MAPPING_DATA_FILE_NAME);
        this.prefixTimeZonesMap = loadPrefixTimeZonesMapFromFile(stringBuilder.toString());
    }

    private PhoneNumberToTimeZonesMapper(PrefixTimeZonesMap prefixTimeZonesMap) {
        this.prefixTimeZonesMap = null;
        this.prefixTimeZonesMap = prefixTimeZonesMap;
    }

    private static PrefixTimeZonesMap loadPrefixTimeZonesMapFromFile(String path) {
        InputStream source = PhoneNumberToTimeZonesMapper.class.getResourceAsStream(path);
        ObjectInputStream in = null;
        PrefixTimeZonesMap map = new PrefixTimeZonesMap();
        try {
            in = new ObjectInputStream(source);
            map.readExternal(in);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.toString());
        } catch (Throwable th) {
            close(in);
        }
        close(in);
        return map;
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.toString());
            }
        }
    }

    public static synchronized PhoneNumberToTimeZonesMapper getInstance() {
        PhoneNumberToTimeZonesMapper access$200;
        synchronized (PhoneNumberToTimeZonesMapper.class) {
            access$200 = LazyHolder.INSTANCE;
        }
        return access$200;
    }

    public List<String> getTimeZonesForGeographicalNumber(PhoneNumber number) {
        return getTimeZonesForGeocodableNumber(number);
    }

    public List<String> getTimeZonesForNumber(PhoneNumber number) {
        PhoneNumberType numberType = PhoneNumberUtil.getInstance().getNumberType(number);
        if (numberType == PhoneNumberType.UNKNOWN) {
            return UNKNOWN_TIME_ZONE_LIST;
        }
        if (PhoneNumberUtil.getInstance().isNumberGeographical(numberType, number.getCountryCode())) {
            return getTimeZonesForGeographicalNumber(number);
        }
        return getCountryLevelTimeZonesforNumber(number);
    }

    public static String getUnknownTimeZone() {
        return UNKNOWN_TIMEZONE;
    }

    private List<String> getTimeZonesForGeocodableNumber(PhoneNumber number) {
        List<String> timezones = this.prefixTimeZonesMap.lookupTimeZonesForNumber(number);
        return Collections.unmodifiableList(timezones.isEmpty() ? UNKNOWN_TIME_ZONE_LIST : timezones);
    }

    private List<String> getCountryLevelTimeZonesforNumber(PhoneNumber number) {
        List<String> timezones = this.prefixTimeZonesMap.lookupCountryLevelTimeZonesForNumber(number);
        return Collections.unmodifiableList(timezones.isEmpty() ? UNKNOWN_TIME_ZONE_LIST : timezones);
    }
}
