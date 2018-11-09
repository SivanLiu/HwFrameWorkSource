package android.icu.impl;

import android.icu.impl.LocaleDisplayNamesImpl.DataTable;
import android.icu.util.ULocale;

public class ICURegionDataTables extends ICUDataTables {
    public /* bridge */ /* synthetic */ DataTable get(ULocale uLocale, boolean z) {
        return super.get(uLocale, z);
    }

    public ICURegionDataTables() {
        super(ICUData.ICU_REGION_BASE_NAME);
    }
}
