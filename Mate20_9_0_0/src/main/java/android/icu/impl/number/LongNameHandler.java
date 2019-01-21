package android.icu.impl.number;

import android.icu.impl.CurrencyData;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.number.NumberFormatter.UnitWidth;
import android.icu.text.NumberFormat.Field;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.ICUException;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class LongNameHandler implements MicroPropsGenerator {
    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final MicroPropsGenerator parent;
    private final PluralRules rules;

    private static final class PluralTableSink extends Sink {
        Map<StandardPlural, String> output;

        public PluralTableSink(Map<StandardPlural, String> output) {
            this.output = output;
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table pluralsTable = value.getTable();
            for (int i = 0; pluralsTable.getKeyAndValue(i, key, value); i++) {
                if (!(key.contentEquals("dnam") || key.contentEquals("per"))) {
                    StandardPlural plural = StandardPlural.fromString(key);
                    if (!this.output.containsKey(plural)) {
                        this.output.put(plural, value.getString());
                    }
                }
            }
        }
    }

    private static void getMeasureData(ULocale locale, MeasureUnit unit, UnitWidth width, Map<StandardPlural, String> output) {
        PluralTableSink sink = new PluralTableSink(output);
        ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);
        StringBuilder key = new StringBuilder();
        key.append("units");
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append("/");
        key.append(unit.getType());
        key.append("/");
        key.append(unit.getSubtype());
        resource.getAllItemsWithFallback(key.toString(), sink);
    }

    private static void getCurrencyLongNameData(ULocale locale, Currency currency, Map<StandardPlural, String> output) {
        for (Entry<String, String> e : CurrencyData.provider.getInstance(locale, true).getUnitPatterns().entrySet()) {
            String pluralKeyword = (String) e.getKey();
            output.put(StandardPlural.fromString((CharSequence) e.getKey()), ((String) e.getValue()).replace("{1}", currency.getName(locale, 2, pluralKeyword, null)));
        }
    }

    private LongNameHandler(Map<StandardPlural, SimpleModifier> modifiers, PluralRules rules, MicroPropsGenerator parent) {
        this.modifiers = modifiers;
        this.rules = rules;
        this.parent = parent;
    }

    public static LongNameHandler forCurrencyLongNames(ULocale locale, Currency currency, PluralRules rules, MicroPropsGenerator parent) {
        Map<StandardPlural, String> simpleFormats = new EnumMap(StandardPlural.class);
        getCurrencyLongNameData(locale, currency, simpleFormats);
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap(StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    public static LongNameHandler forMeasureUnit(ULocale locale, MeasureUnit unit, UnitWidth width, PluralRules rules, MicroPropsGenerator parent) {
        Map<StandardPlural, String> simpleFormats = new EnumMap(StandardPlural.class);
        getMeasureData(locale, unit, width, simpleFormats);
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap(StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    private static void simpleFormatsToModifiers(Map<StandardPlural, String> simpleFormats, Field field, Map<StandardPlural, SimpleModifier> output) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = (String) simpleFormats.get(plural);
            if (simpleFormat == null) {
                simpleFormat = (String) simpleFormats.get(StandardPlural.OTHER);
            }
            if (simpleFormat != null) {
                output.put(plural, new SimpleModifier(SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1), null, false));
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find data in 'other' plural variant with field ");
                stringBuilder.append(field);
                throw new ICUException(stringBuilder.toString());
            }
        }
    }

    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = this.parent.processQuantity(quantity);
        DecimalQuantity copy = quantity.createCopy();
        micros.rounding.apply(copy);
        micros.modOuter = (Modifier) this.modifiers.get(copy.getStandardPlural(this.rules));
        return micros;
    }
}
