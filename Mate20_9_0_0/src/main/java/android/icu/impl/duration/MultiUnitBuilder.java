package android.icu.impl.duration;

/* compiled from: BasicPeriodBuilderFactory */
class MultiUnitBuilder extends PeriodBuilderImpl {
    private int nPeriods;

    MultiUnitBuilder(int nPeriods, Settings settings) {
        super(settings);
        this.nPeriods = nPeriods;
    }

    public static MultiUnitBuilder get(int nPeriods, Settings settings) {
        if (nPeriods <= 0 || settings == null) {
            return null;
        }
        return new MultiUnitBuilder(nPeriods, settings);
    }

    protected PeriodBuilder withSettings(Settings settingsToUse) {
        return get(this.nPeriods, settingsToUse);
    }

    protected Period handleCreate(long duration, long referenceDate, boolean inPast) {
        boolean z;
        Period period = null;
        int n = 0;
        short uset = this.settings.effectiveSet();
        long duration2 = duration;
        for (int i = 0; i < TimeUnit.units.length; i++) {
            if (((1 << i) & uset) != 0) {
                TimeUnit unit = TimeUnit.units[i];
                if (n == this.nPeriods) {
                    break;
                }
                long unitDuration = approximateDurationOf(unit);
                if (duration2 >= unitDuration || n > 0) {
                    n++;
                    double count = ((double) duration2) / ((double) unitDuration);
                    if (n < this.nPeriods) {
                        count = Math.floor(count);
                        duration2 -= (long) (((double) unitDuration) * count);
                    }
                    if (period == null) {
                        period = Period.at((float) count, unit).inPast(inPast);
                    } else {
                        z = inPast;
                        period = period.and((float) count, unit);
                    }
                }
            }
            z = inPast;
        }
        z = inPast;
        return period;
    }
}
