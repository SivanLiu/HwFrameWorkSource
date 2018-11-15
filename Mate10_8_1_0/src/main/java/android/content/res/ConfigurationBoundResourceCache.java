package android.content.res;

import android.content.res.Resources.Theme;

public class ConfigurationBoundResourceCache<T> extends ThemedResourceCache<ConstantState<T>> {
    public /* bridge */ /* synthetic */ Object get(long j, Theme theme) {
        return super.get(j, theme);
    }

    public /* bridge */ /* synthetic */ void onConfigurationChange(int i) {
        super.onConfigurationChange(i);
    }

    public /* bridge */ /* synthetic */ void put(long j, Theme theme, Object obj) {
        super.put(j, theme, obj);
    }

    public /* bridge */ /* synthetic */ void put(long j, Theme theme, Object obj, boolean z) {
        super.put(j, theme, obj, z);
    }

    public T getInstance(long key, Resources resources, Theme theme) {
        ConstantState<T> entry = (ConstantState) get(key, theme);
        if (entry != null) {
            return entry.newInstance(resources, theme);
        }
        return null;
    }

    public boolean shouldInvalidateEntry(ConstantState<T> entry, int configChanges) {
        return Configuration.needNewResources(configChanges, entry.getChangingConfigurations());
    }
}
