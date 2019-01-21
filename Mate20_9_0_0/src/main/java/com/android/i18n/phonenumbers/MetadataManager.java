package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MetadataManager {
    private static final String ALTERNATE_FORMATS_FILE_PREFIX = "/com/android/i18n/phonenumbers/data/PhoneNumberAlternateFormatsProto";
    static final MetadataLoader DEFAULT_METADATA_LOADER = new MetadataLoader() {
        public InputStream loadMetadata(String metadataFileName) {
            return MetadataManager.class.getResourceAsStream(metadataFileName);
        }
    };
    static final String MULTI_FILE_PHONE_NUMBER_METADATA_FILE_PREFIX = "/com/android/i18n/phonenumbers/data/PhoneNumberMetadataProto";
    private static final String SHORT_NUMBER_METADATA_FILE_PREFIX = "/com/android/i18n/phonenumbers/data/ShortNumberMetadataProto";
    static final String SINGLE_FILE_PHONE_NUMBER_METADATA_FILE_NAME = "/com/android/i18n/phonenumbers/data/SingleFilePhoneNumberMetadataProto";
    private static final Set<Integer> alternateFormatsCountryCodes = AlternateFormatsCountryCodeSet.getCountryCodeSet();
    private static final ConcurrentHashMap<Integer, PhoneMetadata> alternateFormatsMap = new ConcurrentHashMap();
    private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());
    private static final ConcurrentHashMap<String, PhoneMetadata> shortNumberMetadataMap = new ConcurrentHashMap();
    private static final Set<String> shortNumberMetadataRegionCodes = ShortNumbersRegionCodeSet.getRegionCodeSet();

    static class SingleFileMetadataMaps {
        private final Map<Integer, PhoneMetadata> countryCallingCodeToMetadata;
        private final Map<String, PhoneMetadata> regionCodeToMetadata;

        static SingleFileMetadataMaps load(String fileName, MetadataLoader metadataLoader) {
            List<PhoneMetadata> metadataList = MetadataManager.getMetadataFromSingleFileName(fileName, metadataLoader);
            Map<String, PhoneMetadata> regionCodeToMetadata = new HashMap();
            Map<Integer, PhoneMetadata> countryCallingCodeToMetadata = new HashMap();
            for (PhoneMetadata metadata : metadataList) {
                String regionCode = metadata.getId();
                if (PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode)) {
                    countryCallingCodeToMetadata.put(Integer.valueOf(metadata.getCountryCode()), metadata);
                } else {
                    regionCodeToMetadata.put(regionCode, metadata);
                }
            }
            return new SingleFileMetadataMaps(regionCodeToMetadata, countryCallingCodeToMetadata);
        }

        private SingleFileMetadataMaps(Map<String, PhoneMetadata> regionCodeToMetadata, Map<Integer, PhoneMetadata> countryCallingCodeToMetadata) {
            this.regionCodeToMetadata = Collections.unmodifiableMap(regionCodeToMetadata);
            this.countryCallingCodeToMetadata = Collections.unmodifiableMap(countryCallingCodeToMetadata);
        }

        PhoneMetadata get(String regionCode) {
            return (PhoneMetadata) this.regionCodeToMetadata.get(regionCode);
        }

        PhoneMetadata get(int countryCallingCode) {
            return (PhoneMetadata) this.countryCallingCodeToMetadata.get(Integer.valueOf(countryCallingCode));
        }
    }

    private MetadataManager() {
    }

    static PhoneMetadata getAlternateFormatsForCountry(int countryCallingCode) {
        if (alternateFormatsCountryCodes.contains(Integer.valueOf(countryCallingCode))) {
            return getMetadataFromMultiFilePrefix(Integer.valueOf(countryCallingCode), alternateFormatsMap, ALTERNATE_FORMATS_FILE_PREFIX, DEFAULT_METADATA_LOADER);
        }
        return null;
    }

    static PhoneMetadata getShortNumberMetadataForRegion(String regionCode) {
        if (shortNumberMetadataRegionCodes.contains(regionCode)) {
            return getMetadataFromMultiFilePrefix(regionCode, shortNumberMetadataMap, SHORT_NUMBER_METADATA_FILE_PREFIX, DEFAULT_METADATA_LOADER);
        }
        return null;
    }

    static Set<String> getSupportedShortNumberRegions() {
        return Collections.unmodifiableSet(shortNumberMetadataRegionCodes);
    }

    static <T> PhoneMetadata getMetadataFromMultiFilePrefix(T key, ConcurrentHashMap<T, PhoneMetadata> map, String filePrefix, MetadataLoader metadataLoader) {
        PhoneMetadata metadata = (PhoneMetadata) map.get(key);
        if (metadata != null) {
            return metadata;
        }
        String fileName = new StringBuilder();
        fileName.append(filePrefix);
        fileName.append("_");
        fileName.append(key);
        fileName = fileName.toString();
        List<PhoneMetadata> metadataList = getMetadataFromSingleFileName(fileName, metadataLoader);
        if (metadataList.size() > 1) {
            Logger logger = logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("more than one metadata in file ");
            stringBuilder.append(fileName);
            logger.log(level, stringBuilder.toString());
        }
        metadata = (PhoneMetadata) metadataList.get(0);
        PhoneMetadata oldValue = (PhoneMetadata) map.putIfAbsent(key, metadata);
        return oldValue != null ? oldValue : metadata;
    }

    static SingleFileMetadataMaps getSingleFileMetadataMaps(AtomicReference<SingleFileMetadataMaps> ref, String fileName, MetadataLoader metadataLoader) {
        SingleFileMetadataMaps maps = (SingleFileMetadataMaps) ref.get();
        if (maps != null) {
            return maps;
        }
        ref.compareAndSet(null, SingleFileMetadataMaps.load(fileName, metadataLoader));
        return (SingleFileMetadataMaps) ref.get();
    }

    private static List<PhoneMetadata> getMetadataFromSingleFileName(String fileName, MetadataLoader metadataLoader) {
        InputStream source = metadataLoader.loadMetadata(fileName);
        if (source != null) {
            List<PhoneMetadata> metadataList = loadMetadataAndCloseInput(source).getMetadataList();
            if (metadataList.size() != 0) {
                return metadataList;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("empty metadata: ");
            stringBuilder.append(fileName);
            throw new IllegalStateException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("missing metadata: ");
        stringBuilder2.append(fileName);
        throw new IllegalStateException(stringBuilder2.toString());
    }

    private static PhoneMetadataCollection loadMetadataAndCloseInput(InputStream source) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(source);
            PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
            metadataCollection.readExternal(ois);
            try {
                ois.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "error closing input stream (ignored)", e);
            }
            return metadataCollection;
        } catch (IOException e2) {
            throw new RuntimeException("cannot load/parse metadata", e2);
        } catch (IOException e3) {
            throw new RuntimeException("cannot load/parse metadata", e3);
        } catch (Throwable th) {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e32) {
                    logger.log(Level.WARNING, "error closing input stream (ignored)", e32);
                }
            } else {
                source.close();
            }
        }
    }
}
