package android.icu.impl.duration.impl;

import android.icu.impl.ICUData;
import android.icu.impl.locale.BaseLocale;
import android.icu.util.ICUUncheckedIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

public class ResourceBasedPeriodFormatterDataService extends PeriodFormatterDataService {
    private static final String PATH = "data/";
    private static final ResourceBasedPeriodFormatterDataService singleton = new ResourceBasedPeriodFormatterDataService();
    private Collection<String> availableLocales;
    private Map<String, PeriodFormatterData> cache = new HashMap();
    private PeriodFormatterData lastData = null;
    private String lastLocale = null;

    public static ResourceBasedPeriodFormatterDataService getInstance() {
        return singleton;
    }

    private ResourceBasedPeriodFormatterDataService() {
        List<String> localeNames = new ArrayList();
        InputStream is = ICUData.getRequiredStream(getClass(), "data/index.txt");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while (true) {
                String readLine = br.readLine();
                String string = readLine;
                if (readLine == null) {
                    break;
                }
                string = string.trim();
                if (!string.startsWith("#")) {
                    if (string.length() != 0) {
                        localeNames.add(string);
                    }
                }
            }
            br.close();
            try {
                is.close();
            } catch (IOException e) {
            }
            this.availableLocales = Collections.unmodifiableList(localeNames);
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IO Error reading data/index.txt: ");
            stringBuilder.append(e2.toString());
            throw new IllegalStateException(stringBuilder.toString());
        } catch (Throwable th) {
            try {
                is.close();
            } catch (IOException e3) {
            }
        }
    }

    public PeriodFormatterData get(String localeName) {
        StringBuilder stringBuilder;
        int x = localeName.indexOf(64);
        if (x != -1) {
            localeName = localeName.substring(0, x);
        }
        synchronized (this) {
            if (this.lastLocale == null || !this.lastLocale.equals(localeName)) {
                PeriodFormatterData ld = (PeriodFormatterData) this.cache.get(localeName);
                if (ld == null) {
                    String ln = localeName;
                    while (!this.availableLocales.contains(ln)) {
                        int ix = ln.lastIndexOf(BaseLocale.SEP);
                        if (ix <= -1) {
                            if ("test".equals(ln)) {
                                ln = null;
                                break;
                            }
                            ln = "test";
                        } else {
                            ln = ln.substring(0, ix);
                        }
                    }
                    if (ln != null) {
                        String name = new StringBuilder();
                        name.append("data/pfd_");
                        name.append(ln);
                        name.append(".xml");
                        name = name.toString();
                        try {
                            InputStreamReader reader = new InputStreamReader(ICUData.getRequiredStream(getClass(), name), "UTF-8");
                            DataRecord dr = DataRecord.read(ln, new XMLRecordReader(reader));
                            reader.close();
                            if (dr != null) {
                                ld = new PeriodFormatterData(localeName, dr);
                            }
                            this.cache.put(localeName, ld);
                        } catch (UnsupportedEncodingException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unhandled encoding for resource ");
                            stringBuilder.append(name);
                            throw new MissingResourceException(stringBuilder.toString(), name, "");
                        } catch (IOException e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to close() resource ");
                            stringBuilder.append(name);
                            throw new ICUUncheckedIOException(stringBuilder.toString(), e2);
                        }
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Duration data not found for  ");
                    stringBuilder2.append(localeName);
                    throw new MissingResourceException(stringBuilder2.toString(), PATH, localeName);
                }
                this.lastData = ld;
                this.lastLocale = localeName;
                return ld;
            }
            PeriodFormatterData periodFormatterData = this.lastData;
            return periodFormatterData;
        }
    }

    public Collection<String> getAvailableLocales() {
        return this.availableLocales;
    }
}
