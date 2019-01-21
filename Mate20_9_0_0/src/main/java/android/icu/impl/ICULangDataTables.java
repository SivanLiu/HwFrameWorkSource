package android.icu.impl;

import android.icu.impl.LocaleDisplayNamesImpl.DataTable;
import android.icu.util.ULocale;

public class ICULangDataTables extends ICUDataTables {
    public /* bridge */ /* synthetic */ DataTable get(ULocale uLocale, boolean z) {
        return super.get(uLocale, z);
    }

    public ICULangDataTables() {
        super(ICUData.ICU_LANG_BASE_NAME);
    }
}
