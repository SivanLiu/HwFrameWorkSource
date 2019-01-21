package android.icu.impl;

import android.icu.text.CurrencyMetaInfo;
import android.icu.text.CurrencyMetaInfo.CurrencyDigits;
import android.icu.text.CurrencyMetaInfo.CurrencyFilter;
import android.icu.text.CurrencyMetaInfo.CurrencyInfo;
import android.icu.util.Currency.CurrencyUsage;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ICUCurrencyMetaInfo extends CurrencyMetaInfo {
    private static final int Currency = 2;
    private static final int Date = 4;
    private static final int Everything = Integer.MAX_VALUE;
    private static final long MASK = 4294967295L;
    private static final int Region = 1;
    private static final int Tender = 8;
    private ICUResourceBundle digitInfo;
    private ICUResourceBundle regionInfo;

    private interface Collector<T> {
        void collect(String str, String str2, long j, long j2, int i, boolean z);

        int collects();

        List<T> getList();
    }

    private static class UniqueList<T> {
        private List<T> list = new ArrayList();
        private Set<T> seen = new HashSet();

        private UniqueList() {
        }

        private static <T> UniqueList<T> create() {
            return new UniqueList();
        }

        void add(T value) {
            if (!this.seen.contains(value)) {
                this.list.add(value);
                this.seen.add(value);
            }
        }

        List<T> list() {
            return Collections.unmodifiableList(this.list);
        }
    }

    private static class CurrencyCollector implements Collector<String> {
        private final UniqueList<String> result;

        private CurrencyCollector() {
            this.result = UniqueList.create();
        }

        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(currency);
        }

        public int collects() {
            return 2;
        }

        public List<String> getList() {
            return this.result.list();
        }
    }

    private static class InfoCollector implements Collector<CurrencyInfo> {
        private List<CurrencyInfo> result;

        private InfoCollector() {
            this.result = new ArrayList();
        }

        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(new CurrencyInfo(region, currency, from, to, priority, tender));
        }

        public List<CurrencyInfo> getList() {
            return Collections.unmodifiableList(this.result);
        }

        public int collects() {
            return Integer.MAX_VALUE;
        }
    }

    private static class RegionCollector implements Collector<String> {
        private final UniqueList<String> result;

        private RegionCollector() {
            this.result = UniqueList.create();
        }

        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(region);
        }

        public int collects() {
            return 1;
        }

        public List<String> getList() {
            return this.result.list();
        }
    }

    public ICUCurrencyMetaInfo() {
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        this.regionInfo = bundle.findTopLevel("CurrencyMap");
        this.digitInfo = bundle.findTopLevel("CurrencyMeta");
    }

    public List<CurrencyInfo> currencyInfo(CurrencyFilter filter) {
        return collect(new InfoCollector(), filter);
    }

    public List<String> currencies(CurrencyFilter filter) {
        return collect(new CurrencyCollector(), filter);
    }

    public List<String> regions(CurrencyFilter filter) {
        return collect(new RegionCollector(), filter);
    }

    public CurrencyDigits currencyDigits(String isoCode) {
        return currencyDigits(isoCode, CurrencyUsage.STANDARD);
    }

    public CurrencyDigits currencyDigits(String isoCode, CurrencyUsage currencyPurpose) {
        ICUResourceBundle b = this.digitInfo.findWithFallback(isoCode);
        if (b == null) {
            b = this.digitInfo.findWithFallback("DEFAULT");
        }
        int[] data = b.getIntVector();
        if (currencyPurpose == CurrencyUsage.CASH) {
            return new CurrencyDigits(data[2], data[3]);
        }
        if (currencyPurpose == CurrencyUsage.STANDARD) {
            return new CurrencyDigits(data[0], data[1]);
        }
        return new CurrencyDigits(data[0], data[1]);
    }

    private <T> List<T> collect(Collector<T> collector, CurrencyFilter filter) {
        if (filter == null) {
            filter = CurrencyFilter.all();
        }
        int needed = collector.collects();
        if (filter.region != null) {
            needed |= 1;
        }
        if (filter.currency != null) {
            needed |= 2;
        }
        if (!(filter.from == Long.MIN_VALUE && filter.to == Long.MAX_VALUE)) {
            needed |= 4;
        }
        if (filter.tenderOnly) {
            needed |= 8;
        }
        if (needed != 0) {
            if (filter.region != null) {
                ICUResourceBundle b = this.regionInfo.findWithFallback(filter.region);
                if (b != null) {
                    collectRegion(collector, filter, needed, b);
                }
            } else {
                for (int i = 0; i < this.regionInfo.getSize(); i++) {
                    collectRegion(collector, filter, needed, this.regionInfo.at(i));
                }
            }
        }
        return collector.getList();
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00a8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private <T> void collectRegion(Collector<T> collector, CurrencyFilter filter, int needed, ICUResourceBundle b) {
        CurrencyFilter currencyFilter = filter;
        int i = needed;
        String region = b.getKey();
        boolean z = true;
        if (i == 1) {
            collector.collect(b.getKey(), null, 0, 0, -1, false);
            return;
        }
        boolean z2 = false;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < b.getSize()) {
                boolean z3;
                int i4;
                ICUResourceBundle r = b.at(i3);
                if (r.getSize() == 0) {
                    z3 = z;
                    i4 = i3;
                } else {
                    boolean z4;
                    String currency = null;
                    if ((i & 2) != 0) {
                        currency = r.at("id").getString();
                        if (!(currencyFilter.currency == null || currencyFilter.currency.equals(currency))) {
                            i4 = i3;
                            z3 = true;
                        }
                    }
                    String currency2 = currency;
                    long from;
                    long to;
                    boolean tender;
                    if ((i & 4) != 0) {
                        from = Long.MIN_VALUE;
                        long from2 = getDate(r.at("from"), Long.MIN_VALUE, z2);
                        i4 = i3;
                        z4 = true;
                        long to2 = getDate(r.at("to"), Long.MAX_VALUE, true);
                        if (currencyFilter.from <= to2 && currencyFilter.to >= from2) {
                            from = from2;
                            to = to2;
                            if ((i & 8) == 0) {
                                ICUResourceBundle tenderBundle = r.at("tender");
                                boolean tender2 = (tenderBundle == null || "true".equals(tenderBundle.getString())) ? z4 : false;
                                if (!currencyFilter.tenderOnly || tender2) {
                                    tender = tender2;
                                }
                            } else {
                                tender = true;
                            }
                            z3 = z4;
                            collector.collect(region, currency2, from, to, i4, tender);
                        }
                    } else {
                        from = Long.MIN_VALUE;
                        i4 = i3;
                        z4 = true;
                        to = Long.MAX_VALUE;
                        if ((i & 8) == 0) {
                        }
                        z3 = z4;
                        collector.collect(region, currency2, from, to, i4, tender);
                    }
                    z3 = z4;
                }
                i2 = i4 + 1;
                z = z3;
                z2 = false;
            } else {
                return;
            }
        }
    }

    private long getDate(ICUResourceBundle b, long defaultValue, boolean endOfDay) {
        if (b == null) {
            return defaultValue;
        }
        int[] values = b.getIntVector();
        return (((long) values[0]) << 32) | (((long) values[1]) & MASK);
    }
}
