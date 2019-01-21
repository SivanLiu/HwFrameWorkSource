package android.icu.text;

import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator.Factory;
import android.icu.text.Transliterator.Position;
import android.icu.util.ULocale;

class TitlecaseTransliterator extends Transliterator {
    static final String _ID = "Any-Title";
    private int caseLocale;
    private final UCaseProps csp;
    private ReplaceableContextIterator iter;
    private final ULocale locale;
    private StringBuilder result;
    SourceTargetUtility sourceTargetUtility = null;

    static void register() {
        Transliterator.registerFactory(_ID, new Factory() {
            public Transliterator getInstance(String ID) {
                return new TitlecaseTransliterator(ULocale.US);
            }
        });
        Transliterator.registerSpecialInverse("Title", "Lower", false);
    }

    public TitlecaseTransliterator(ULocale loc) {
        super(_ID, null);
        this.locale = loc;
        setMaximumContextLength(2);
        this.csp = UCaseProps.INSTANCE;
        this.iter = new ReplaceableContextIterator();
        this.result = new StringBuilder();
        this.caseLocale = UCaseProps.getCaseLocale(this.locale);
    }

    protected synchronized void handleTransliterate(Replaceable text, Position offsets, boolean isIncremental) {
        if (offsets.start < offsets.limit) {
            int c;
            boolean doTitle = true;
            int start = offsets.start - 1;
            while (start >= offsets.contextStart) {
                c = text.char32At(start);
                int type = this.csp.getTypeOrIgnorable(c);
                if (type > 0) {
                    doTitle = false;
                    break;
                } else if (type == 0) {
                    break;
                } else {
                    start -= UTF16.getCharCount(c);
                }
            }
            this.iter.setText(text);
            this.iter.setIndex(offsets.start);
            this.iter.setLimit(offsets.limit);
            this.iter.setContextLimits(offsets.contextStart, offsets.contextLimit);
            this.result.setLength(0);
            while (true) {
                c = this.iter.nextCaseMapCP();
                int c2 = c;
                if (c >= 0) {
                    c = this.csp.getTypeOrIgnorable(c2);
                    if (c >= 0) {
                        if (doTitle) {
                            c2 = this.csp.toFullTitle(c2, this.iter, this.result, this.caseLocale);
                        } else {
                            c2 = this.csp.toFullLower(c2, this.iter, this.result, this.caseLocale);
                        }
                        doTitle = c == 0;
                        if (this.iter.didReachLimit() && isIncremental) {
                            offsets.start = this.iter.getCaseMapCPStart();
                            return;
                        } else if (c2 >= 0) {
                            int delta;
                            if (c2 <= 31) {
                                delta = this.iter.replace(this.result.toString());
                                this.result.setLength(0);
                            } else {
                                delta = this.iter.replace(UTF16.valueOf(c2));
                            }
                            if (delta != 0) {
                                offsets.limit += delta;
                                offsets.contextLimit += delta;
                            }
                        }
                    }
                } else {
                    offsets.start = offsets.limit;
                    return;
                }
            }
        }
    }

    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        synchronized (this) {
            if (this.sourceTargetUtility == null) {
                this.sourceTargetUtility = new SourceTargetUtility(new Transform<String, String>() {
                    public String transform(String source) {
                        return UCharacter.toTitleCase(TitlecaseTransliterator.this.locale, source, null);
                    }
                });
            }
        }
        this.sourceTargetUtility.addSourceTargetSet(this, inputFilter, sourceSet, targetSet);
    }
}
