package android.icu.impl.duration;

import android.icu.impl.duration.impl.PeriodFormatterData;

class BasicPeriodFormatter implements PeriodFormatter {
    private Customizations customs;
    private PeriodFormatterData data;
    private BasicPeriodFormatterFactory factory;
    private String localeName;

    BasicPeriodFormatter(BasicPeriodFormatterFactory factory, String localeName, PeriodFormatterData data, Customizations customs) {
        this.factory = factory;
        this.localeName = localeName;
        this.data = data;
        this.customs = customs;
    }

    public String format(Period period) {
        if (period.isSet()) {
            return format(period.timeLimit, period.inFuture, period.counts);
        }
        throw new IllegalArgumentException("period is not set");
    }

    public PeriodFormatter withLocale(String locName) {
        if (this.localeName.equals(locName)) {
            return this;
        }
        return new BasicPeriodFormatter(this.factory, locName, this.factory.getData(locName), this.customs);
    }

    /* JADX WARNING: Removed duplicated region for block: B:68:0x00df  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00dc  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00ec  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00ea  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x00f6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String format(int tl, boolean inFuture, int[] counts) {
        int i;
        int mask;
        StringBuffer sb;
        int tl2;
        int td;
        int td2;
        boolean useDigitPrefix;
        boolean multiple;
        boolean skipped;
        boolean countSep;
        int j;
        boolean useDigitPrefix2;
        boolean i2;
        int mask2;
        boolean isZero;
        int[] iArr = counts;
        int mask3 = 0;
        int i3 = 0;
        while (true) {
            i = 1;
            if (i3 >= iArr.length) {
                break;
            }
            if (iArr[i3] > 0) {
                mask3 |= 1 << i3;
            }
            i3++;
        }
        if (!this.data.allowZero()) {
            mask = mask3;
            mask3 = 0;
            i3 = 1;
            while (mask3 < iArr.length) {
                if ((mask & i3) != 0 && iArr[mask3] == 1) {
                    mask &= ~i3;
                }
                mask3++;
                i3 <<= 1;
            }
            if (mask == 0) {
                return null;
            }
            mask3 = mask;
        }
        boolean forceD3Seconds = false;
        if (!(this.data.useMilliseconds() == 0 || ((1 << TimeUnit.MILLISECOND.ordinal) & mask3) == 0)) {
            mask = TimeUnit.SECOND.ordinal;
            int mx = TimeUnit.MILLISECOND.ordinal;
            int sf = 1 << mask;
            int mf = 1 << mx;
            switch (this.data.useMilliseconds()) {
                case 1:
                    if ((mask3 & sf) == 0) {
                        mask3 |= sf;
                        iArr[mask] = 1;
                    }
                    iArr[mask] = iArr[mask] + ((iArr[mx] - 1) / 1000);
                    mask3 &= ~mf;
                    forceD3Seconds = true;
                    break;
                case 2:
                    if ((mask3 & sf) != 0) {
                        iArr[mask] = iArr[mask] + ((iArr[mx] - 1) / 1000);
                        mask3 &= ~mf;
                        forceD3Seconds = true;
                        break;
                    }
                    break;
            }
        }
        boolean first = false;
        boolean last = iArr.length - 1;
        while (first < iArr.length && ((1 << first) & mask3) == 0) {
            first++;
        }
        while (last > first && ((1 << last) & mask3) == 0) {
            last--;
        }
        boolean isZero2 = true;
        boolean i4 = first;
        while (i4 <= last) {
            if (((1 << i4) & mask3) == 0 || iArr[i4] <= 1) {
                i4++;
            } else {
                isZero2 = false;
                sb = new StringBuffer();
                tl2 = (this.customs.displayLimit || isZero2) ? 0 : tl;
                td = (this.customs.displayDirection || isZero2) ? 0 : inFuture ? 2 : 1;
                td2 = td;
                useDigitPrefix = this.data.appendPrefix(tl2, td2, sb);
                multiple = first == last;
                skipped = false;
                countSep = this.customs.separatorVariant == (byte) 0;
                j = first;
                useDigitPrefix2 = useDigitPrefix;
                useDigitPrefix = true;
                i2 = j;
                while (i2 <= last) {
                    boolean wasSkipped;
                    TimeUnit unit;
                    int count;
                    boolean j2;
                    if (skipped) {
                        this.data.appendSkippedUnit(sb);
                        skipped = false;
                        wasSkipped = true;
                    } else {
                        wasSkipped = useDigitPrefix;
                    }
                    boolean skipped2 = skipped;
                    while (true) {
                        useDigitPrefix = j + 1;
                        if (useDigitPrefix >= last || (mask3 & (i << useDigitPrefix)) != 0) {
                            unit = TimeUnit.units[i2];
                            count = iArr[i2] - 1;
                            i = this.customs.countVariant;
                        } else {
                            skipped2 = true;
                            j2 = useDigitPrefix;
                        }
                    }
                    unit = TimeUnit.units[i2];
                    count = iArr[i2] - 1;
                    i = this.customs.countVariant;
                    if (i2 != last) {
                        i = 0;
                    } else if (forceD3Seconds) {
                        i = 5;
                    }
                    boolean forceD3Seconds2 = forceD3Seconds;
                    forceD3Seconds = useDigitPrefix;
                    mask2 = mask3;
                    int td3 = td2;
                    isZero = isZero2;
                    isZero2 = i2;
                    int tl3 = tl2;
                    skipped = skipped2 | this.data.appendUnit(unit, count, i, this.customs.unitVariant, countSep, useDigitPrefix2, multiple, i2 == last, wasSkipped, sb);
                    useDigitPrefix = false;
                    if (this.customs.separatorVariant == (byte) 0 || forceD3Seconds > last) {
                        useDigitPrefix2 = false;
                    } else {
                        useDigitPrefix2 = this.data.appendUnitSeparator(unit, this.customs.separatorVariant == (byte) 2, isZero2 == first, forceD3Seconds == last, sb);
                    }
                    i2 = forceD3Seconds;
                    j2 = forceD3Seconds;
                    forceD3Seconds = forceD3Seconds2;
                    mask3 = mask2;
                    isZero2 = isZero;
                    td2 = td3;
                    tl2 = tl3;
                    iArr = counts;
                    i = 1;
                }
                mask2 = mask3;
                isZero = isZero2;
                this.data.appendSuffix(tl2, td2, sb);
                return sb.toString();
            }
        }
        sb = new StringBuffer();
        if (this.customs.displayLimit) {
        }
        if (this.customs.displayDirection) {
        }
        td2 = td;
        useDigitPrefix = this.data.appendPrefix(tl2, td2, sb);
        if (first == last) {
        }
        skipped = false;
        if (this.customs.separatorVariant == (byte) 0) {
        }
        j = first;
        useDigitPrefix2 = useDigitPrefix;
        useDigitPrefix = true;
        i2 = j;
        while (i2 <= last) {
        }
        mask2 = mask3;
        isZero = isZero2;
        this.data.appendSuffix(tl2, td2, sb);
        return sb.toString();
    }
}
