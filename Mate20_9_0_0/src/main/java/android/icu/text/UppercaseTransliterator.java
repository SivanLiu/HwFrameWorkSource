package android.icu.text;

import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator.Factory;
import android.icu.text.Transliterator.Position;
import android.icu.util.ULocale;

class UppercaseTransliterator extends Transliterator {
    static final String _ID = "Any-Upper";
    private int caseLocale;
    private final UCaseProps csp;
    private ReplaceableContextIterator iter;
    private final ULocale locale;
    private StringBuilder result;
    SourceTargetUtility sourceTargetUtility = null;

    static void register() {
        Transliterator.registerFactory(_ID, new Factory() {
            public Transliterator getInstance(String ID) {
                return new UppercaseTransliterator(ULocale.US);
            }
        });
    }

    public UppercaseTransliterator(ULocale loc) {
        super(_ID, null);
        this.locale = loc;
        this.csp = UCaseProps.INSTANCE;
        this.iter = new ReplaceableContextIterator();
        this.result = new StringBuilder();
        this.caseLocale = UCaseProps.getCaseLocale(this.locale);
    }

    protected synchronized void handleTransliterate(Replaceable text, Position offsets, boolean isIncremental) {
        if (this.csp != null) {
            if (offsets.start < offsets.limit) {
                this.iter.setText(text);
                this.result.setLength(0);
                this.iter.setIndex(offsets.start);
                this.iter.setLimit(offsets.limit);
                this.iter.setContextLimits(offsets.contextStart, offsets.contextLimit);
                while (true) {
                    int nextCaseMapCP = this.iter.nextCaseMapCP();
                    int c = nextCaseMapCP;
                    if (nextCaseMapCP >= 0) {
                        nextCaseMapCP = this.csp.toFullUpper(c, this.iter, this.result, this.caseLocale);
                        if (this.iter.didReachLimit() && isIncremental) {
                            offsets.start = this.iter.getCaseMapCPStart();
                            return;
                        } else if (nextCaseMapCP >= 0) {
                            if (nextCaseMapCP <= 31) {
                                c = this.iter.replace(this.result.toString());
                                this.result.setLength(0);
                            } else {
                                c = this.iter.replace(UTF16.valueOf(nextCaseMapCP));
                            }
                            if (c != 0) {
                                offsets.limit += c;
                                offsets.contextLimit += c;
                            }
                        }
                    } else {
                        offsets.start = offsets.limit;
                        return;
                    }
                }
            }
        }
    }

    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        synchronized (this) {
            if (this.sourceTargetUtility == null) {
                this.sourceTargetUtility = new SourceTargetUtility(new Transform<String, String>() {
                    public String transform(String source) {
                        return UCharacter.toUpperCase(UppercaseTransliterator.this.locale, source);
                    }
                });
            }
        }
        this.sourceTargetUtility.addSourceTargetSet(this, inputFilter, sourceSet, targetSet);
    }
}
