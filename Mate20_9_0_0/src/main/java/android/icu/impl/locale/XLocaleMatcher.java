package android.icu.impl.locale;

import android.icu.impl.locale.XCldrStub.ImmutableMultimap;
import android.icu.impl.locale.XCldrStub.ImmutableSet;
import android.icu.impl.locale.XCldrStub.LinkedHashMultimap;
import android.icu.impl.locale.XCldrStub.Multimap;
import android.icu.impl.locale.XLikelySubtags.LSR;
import android.icu.impl.locale.XLocaleDistance.DistanceOption;
import android.icu.util.LocalePriorityList;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class XLocaleMatcher {
    private static final LSR UND = new LSR("und", "", "");
    private static final ULocale UND_LOCALE = new ULocale("und");
    private final ULocale defaultLanguage;
    private final int demotionPerAdditionalDesiredLocale;
    private final DistanceOption distanceOption;
    private final Set<ULocale> exactSupportedLocales;
    private final XLocaleDistance localeDistance;
    private final Map<LSR, Set<ULocale>> supportedLanguages;
    private final int thresholdDistance;

    public static class Builder {
        private ULocale defaultLanguage;
        private int demotionPerAdditionalDesiredLocale = -1;
        private DistanceOption distanceOption;
        private XLocaleDistance localeDistance;
        private Set<ULocale> supportedLanguagesList;
        private int thresholdDistance = -1;

        public Builder setSupportedLocales(String languagePriorityList) {
            this.supportedLanguagesList = XLocaleMatcher.asSet(LocalePriorityList.add(languagePriorityList).build());
            return this;
        }

        public Builder setSupportedLocales(LocalePriorityList languagePriorityList) {
            this.supportedLanguagesList = XLocaleMatcher.asSet(languagePriorityList);
            return this;
        }

        public Builder setSupportedLocales(Set<ULocale> languagePriorityList) {
            this.supportedLanguagesList = languagePriorityList;
            return this;
        }

        public Builder setThresholdDistance(int thresholdDistance) {
            this.thresholdDistance = thresholdDistance;
            return this;
        }

        public Builder setDemotionPerAdditionalDesiredLocale(int demotionPerAdditionalDesiredLocale) {
            this.demotionPerAdditionalDesiredLocale = demotionPerAdditionalDesiredLocale;
            return this;
        }

        public Builder setLocaleDistance(XLocaleDistance localeDistance) {
            this.localeDistance = localeDistance;
            return this;
        }

        public Builder setDefaultLanguage(ULocale defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
            return this;
        }

        public Builder setDistanceOption(DistanceOption distanceOption) {
            this.distanceOption = distanceOption;
            return this;
        }

        public XLocaleMatcher build() {
            return new XLocaleMatcher(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public XLocaleMatcher(String supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }

    public XLocaleMatcher(LocalePriorityList supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }

    public XLocaleMatcher(Set<ULocale> supportedLocales) {
        this(builder().setSupportedLocales((Set) supportedLocales));
    }

    private XLocaleMatcher(Builder builder) {
        XLocaleDistance xLocaleDistance;
        int defaultScriptDistance;
        ULocale access$500;
        int defaultRegionDistance;
        if (builder.localeDistance == null) {
            xLocaleDistance = XLocaleDistance.getDefault();
        } else {
            xLocaleDistance = builder.localeDistance;
        }
        this.localeDistance = xLocaleDistance;
        if (builder.thresholdDistance < 0) {
            defaultScriptDistance = this.localeDistance.getDefaultScriptDistance();
        } else {
            defaultScriptDistance = builder.thresholdDistance;
        }
        this.thresholdDistance = defaultScriptDistance;
        Multimap<LSR, ULocale> temp2 = extractLsrMap(builder.supportedLanguagesList, extractLsrSet(this.localeDistance.getParadigms()));
        this.supportedLanguages = temp2.asMap();
        this.exactSupportedLocales = ImmutableSet.copyOf(temp2.values());
        if (builder.defaultLanguage != null) {
            access$500 = builder.defaultLanguage;
        } else if (this.supportedLanguages.isEmpty()) {
            access$500 = null;
        } else {
            access$500 = (ULocale) ((Set) ((Entry) this.supportedLanguages.entrySet().iterator().next()).getValue()).iterator().next();
        }
        this.defaultLanguage = access$500;
        if (builder.demotionPerAdditionalDesiredLocale < 0) {
            defaultRegionDistance = this.localeDistance.getDefaultRegionDistance() + 1;
        } else {
            defaultRegionDistance = builder.demotionPerAdditionalDesiredLocale;
        }
        this.demotionPerAdditionalDesiredLocale = defaultRegionDistance;
        this.distanceOption = builder.distanceOption;
    }

    private Set<LSR> extractLsrSet(Set<ULocale> languagePriorityList) {
        Set<LSR> result = new LinkedHashSet();
        for (ULocale item : languagePriorityList) {
            result.add(item.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(item));
        }
        return result;
    }

    private Multimap<LSR, ULocale> extractLsrMap(Set<ULocale> languagePriorityList, Set<LSR> priorities) {
        Multimap<LSR, ULocale> builder = LinkedHashMultimap.create();
        for (ULocale item : languagePriorityList) {
            builder.put(item.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(item), item);
        }
        if (builder.size() > 1 && priorities != null) {
            Multimap<LSR, ULocale> builder2 = LinkedHashMultimap.create();
            boolean first = true;
            for (Entry<LSR, Set<ULocale>> entry : builder.asMap().entrySet()) {
                Object key = (LSR) entry.getKey();
                if (first || priorities.contains(key)) {
                    builder2.putAll(key, (Collection) entry.getValue());
                    first = false;
                }
            }
            builder2.putAll(builder);
            if (builder2.equals(builder)) {
                builder = builder2;
            } else {
                throw new IllegalArgumentException();
            }
        }
        return ImmutableMultimap.copyOf(builder);
    }

    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatch(ulocale, null);
    }

    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build(), null);
    }

    public ULocale getBestMatch(ULocale... locales) {
        return getBestMatch(new LinkedHashSet(Arrays.asList(locales)), null);
    }

    public ULocale getBestMatch(Set<ULocale> desiredLanguages) {
        return getBestMatch((Set) desiredLanguages, null);
    }

    public ULocale getBestMatch(LocalePriorityList desiredLanguages) {
        return getBestMatch(desiredLanguages, null);
    }

    public ULocale getBestMatch(LocalePriorityList desiredLanguages, Output<ULocale> outputBestDesired) {
        return getBestMatch(asSet(desiredLanguages), (Output) outputBestDesired);
    }

    private static Set<ULocale> asSet(LocalePriorityList languageList) {
        Set<ULocale> temp = new LinkedHashSet();
        Iterator it = languageList.iterator();
        while (it.hasNext()) {
            temp.add((ULocale) it.next());
        }
        return temp;
    }

    public ULocale getBestMatch(Set<ULocale> desiredLanguages, Output<ULocale> outputBestDesired) {
        Output output = outputBestDesired;
        if (desiredLanguages.size() == 1) {
            return getBestMatch((ULocale) desiredLanguages.iterator().next(), output);
        }
        Multimap<LSR, ULocale> desiredLSRs = extractLsrMap(desiredLanguages, null);
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        int delta = 0;
        loop0:
        for (Entry<LSR, ULocale> desiredLsrAndLocale : desiredLSRs.entries()) {
            Set<ULocale> set;
            ULocale desiredLocale = (ULocale) desiredLsrAndLocale.getValue();
            LSR desiredLSR = (LSR) desiredLsrAndLocale.getKey();
            if (delta < bestDistance) {
                if (this.exactSupportedLocales.contains(desiredLocale)) {
                    if (output != null) {
                        output.value = desiredLocale;
                    }
                    return desiredLocale;
                }
                Collection<ULocale> found = (Collection) this.supportedLanguages.get(desiredLSR);
                if (found != null) {
                    if (output != null) {
                        output.value = desiredLocale;
                    }
                    return (ULocale) found.iterator().next();
                }
            }
            for (Entry<LSR, Set<ULocale>> supportedLsrAndLocale : this.supportedLanguages.entrySet()) {
                Multimap<LSR, ULocale> desiredLSRs2 = desiredLSRs;
                int distance = this.localeDistance.distanceRaw(desiredLSR, (LSR) supportedLsrAndLocale.getKey(), this.thresholdDistance, this.distanceOption) + delta;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDesiredLocale = desiredLocale;
                    bestSupportedLocales = (Collection) supportedLsrAndLocale.getValue();
                    if (distance == 0) {
                        break loop0;
                    }
                }
                desiredLSRs = desiredLSRs2;
                set = desiredLanguages;
            }
            delta += this.demotionPerAdditionalDesiredLocale;
            set = desiredLanguages;
        }
        if (bestDistance >= this.thresholdDistance) {
            if (output != null) {
                output.value = null;
            }
            return this.defaultLanguage;
        }
        if (output != null) {
            output.value = bestDesiredLocale;
        }
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        return (ULocale) bestSupportedLocales.iterator().next();
    }

    public ULocale getBestMatch(ULocale desiredLocale, Output<ULocale> outputBestDesired) {
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        LSR desiredLSR = desiredLocale.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(desiredLocale);
        if (this.exactSupportedLocales.contains(desiredLocale)) {
            if (outputBestDesired != null) {
                outputBestDesired.value = desiredLocale;
            }
            return desiredLocale;
        }
        if (this.distanceOption == DistanceOption.NORMAL) {
            Collection<ULocale> found = (Collection) this.supportedLanguages.get(desiredLSR);
            if (found != null) {
                if (outputBestDesired != null) {
                    outputBestDesired.value = desiredLocale;
                }
                return (ULocale) found.iterator().next();
            }
        }
        for (Entry<LSR, Set<ULocale>> supportedLsrAndLocale : this.supportedLanguages.entrySet()) {
            int distance = this.localeDistance.distanceRaw(desiredLSR, (LSR) supportedLsrAndLocale.getKey(), this.thresholdDistance, this.distanceOption);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDesiredLocale = desiredLocale;
                bestSupportedLocales = (Collection) supportedLsrAndLocale.getValue();
                if (distance == 0) {
                    break;
                }
            }
        }
        if (bestDistance >= this.thresholdDistance) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            return this.defaultLanguage;
        }
        if (outputBestDesired != null) {
            outputBestDesired.value = bestDesiredLocale;
        }
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        return (ULocale) bestSupportedLocales.iterator().next();
    }

    public static ULocale combine(ULocale bestSupported, ULocale bestDesired) {
        if (bestSupported.equals(bestDesired) || bestDesired == null) {
            return bestSupported;
        }
        android.icu.util.ULocale.Builder b = new android.icu.util.ULocale.Builder().setLocale(bestSupported);
        String region = bestDesired.getCountry();
        if (!region.isEmpty()) {
            b.setRegion(region);
        }
        String variants = bestDesired.getVariant();
        if (!variants.isEmpty()) {
            b.setVariant(variants);
        }
        for (Character extensionKey : bestDesired.getExtensionKeys()) {
            char extensionKey2 = extensionKey.charValue();
            b.setExtension(extensionKey2, bestDesired.getExtension(extensionKey2));
        }
        return b.build();
    }

    public int distance(ULocale desired, ULocale supported) {
        return this.localeDistance.distanceRaw(LSR.fromMaximalized(desired), LSR.fromMaximalized(supported), this.thresholdDistance, this.distanceOption);
    }

    public int distance(String desiredLanguage, String supportedLanguage) {
        return this.localeDistance.distanceRaw(LSR.fromMaximalized(new ULocale(desiredLanguage)), LSR.fromMaximalized(new ULocale(supportedLanguage)), this.thresholdDistance, this.distanceOption);
    }

    public String toString() {
        return this.exactSupportedLocales.toString();
    }

    public double match(ULocale desired, ULocale supported) {
        return ((double) (100 - distance(desired, supported))) / 100.0d;
    }

    @Deprecated
    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        return match(desired, supported);
    }

    public ULocale canonicalize(ULocale ulocale) {
        return null;
    }

    public int getThresholdDistance() {
        return this.thresholdDistance;
    }
}
