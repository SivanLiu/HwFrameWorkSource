package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUData;
import android.icu.impl.ICULocaleService;
import android.icu.impl.ICULocaleService.ICUResourceBundleFactory;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundle.OpenType;
import android.icu.impl.ICUService;
import android.icu.impl.ICUService.Factory;
import android.icu.impl.locale.BaseLocale;
import android.icu.util.ULocale;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.MissingResourceException;

final class BreakIteratorFactory extends BreakIteratorServiceShim {
    private static final String[] KIND_NAMES = new String[]{"grapheme", "word", "line", "sentence", "title"};
    static final ICULocaleService service = new BFService();

    private static class BFService extends ICULocaleService {
        BFService() {
            super("BreakIterator");
            registerFactory(new ICUResourceBundleFactory() {
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return BreakIteratorFactory.createBreakInstance(loc, kind);
                }
            });
            markDefault();
        }

        public String validateFallbackLocale() {
            return "";
        }
    }

    BreakIteratorFactory() {
    }

    public Object registerInstance(BreakIterator iter, ULocale locale, int kind) {
        iter.setText(new StringCharacterIterator(""));
        return service.registerObject((Object) iter, locale, kind);
    }

    public boolean unregister(Object key) {
        if (service.isDefault()) {
            return false;
        }
        return service.unregisterFactory((Factory) key);
    }

    public Locale[] getAvailableLocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    public ULocale[] getAvailableULocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return service.getAvailableULocales();
    }

    public BreakIterator createBreakIterator(ULocale locale, int kind) {
        if (service.isDefault()) {
            return createBreakInstance(locale, kind);
        }
        ULocale[] actualLoc = new ULocale[1];
        BreakIterator iter = (BreakIterator) service.get(locale, kind, actualLoc);
        iter.setLocale(actualLoc[0], actualLoc[0]);
        return iter;
    }

    private static BreakIterator createBreakInstance(ULocale locale, int kind) {
        String lbKeyValue;
        StringBuilder stringBuilder;
        RuleBasedBreakIterator iter = null;
        ICUResourceBundle rb = ICUResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, locale, OpenType.LOCALE_ROOT);
        String typeKeyExt = null;
        if (kind == 2) {
            lbKeyValue = locale.getKeywordValue("lb");
            if (lbKeyValue != null && (lbKeyValue.equals("strict") || lbKeyValue.equals("normal") || lbKeyValue.equals("loose"))) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(BaseLocale.SEP);
                stringBuilder.append(lbKeyValue);
                typeKeyExt = stringBuilder.toString();
            }
        }
        if (typeKeyExt == null) {
            try {
                lbKeyValue = KIND_NAMES[kind];
            } catch (Exception e) {
                throw new MissingResourceException(e.toString(), "", "");
            }
        }
        lbKeyValue = new StringBuilder();
        lbKeyValue.append(KIND_NAMES[kind]);
        lbKeyValue.append(typeKeyExt);
        lbKeyValue = lbKeyValue.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("boundaries/");
        stringBuilder.append(lbKeyValue);
        String brkfname = rb.getStringWithFallback(stringBuilder.toString());
        String rulesFileName = new StringBuilder();
        rulesFileName.append("brkitr/");
        rulesFileName.append(brkfname);
        try {
            iter = RuleBasedBreakIterator.getInstanceFromCompiledRules(ICUBinary.getData(rulesFileName.toString()));
        } catch (IOException e2) {
            Assert.fail(e2);
        }
        Exception e22 = ULocale.forLocale(rb.getLocale());
        iter.setLocale(e22, e22);
        iter.setBreakType(kind);
        if (kind == 3) {
            brkfname = locale.getKeywordValue("ss");
            if (brkfname != null && brkfname.equals("standard")) {
                return FilteredBreakIteratorBuilder.getInstance(new ULocale(locale.getBaseName())).wrapIteratorWithFilter(iter);
            }
        }
        return iter;
    }
}
