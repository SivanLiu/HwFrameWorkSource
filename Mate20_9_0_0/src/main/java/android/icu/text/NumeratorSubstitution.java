package android.icu.text;

import java.text.ParsePosition;

/* compiled from: NFSubstitution */
class NumeratorSubstitution extends NFSubstitution {
    private final double denominator;
    private final boolean withZeros;

    NumeratorSubstitution(int pos, double denominator, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, fixdesc(description));
        this.denominator = denominator;
        this.withZeros = description.endsWith("<<");
    }

    static String fixdesc(String description) {
        if (description.endsWith("<<")) {
            return description.substring(0, description.length() - 1);
        }
        return description;
    }

    public boolean equals(Object that) {
        boolean z = false;
        if (!super.equals(that)) {
            return false;
        }
        NumeratorSubstitution that2 = (NumeratorSubstitution) that;
        if (this.denominator == that2.denominator && this.withZeros == that2.withZeros) {
            z = true;
        }
        return z;
    }

    public void doSubstitution(double number, StringBuilder toInsertInto, int position, int recursionCount) {
        int position2;
        StringBuilder stringBuilder = toInsertInto;
        double numberToFormat = transformNumber(number);
        if (!this.withZeros || this.ruleSet == null) {
            position2 = position;
        } else {
            int len;
            long nf = (long) numberToFormat;
            int len2 = toInsertInto.length();
            while (true) {
                len = len2;
                long j = 10 * nf;
                long nf2 = j;
                if (((double) j) >= this.denominator) {
                    break;
                }
                stringBuilder.insert(position + this.pos, ' ');
                this.ruleSet.format(0, stringBuilder, position + this.pos, recursionCount);
                len2 = len;
                nf = nf2;
            }
            position2 = position + (toInsertInto.length() - len);
        }
        if (numberToFormat == Math.floor(numberToFormat) && this.ruleSet != null) {
            this.ruleSet.format((long) numberToFormat, stringBuilder, position2 + this.pos, recursionCount);
        } else if (this.ruleSet != null) {
            this.ruleSet.format(numberToFormat, stringBuilder, position2 + this.pos, recursionCount);
        } else {
            stringBuilder.insert(this.pos + position2, this.numberFormat.format(numberToFormat));
        }
    }

    public long transformNumber(long number) {
        return Math.round(((double) number) * this.denominator);
    }

    public double transformNumber(double number) {
        return (double) Math.round(this.denominator * number);
    }

    public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse) {
        int zeroCount;
        String text2;
        ParsePosition parsePosition2 = parsePosition;
        int zeroCount2 = 0;
        if (this.withZeros) {
            String workText = text;
            ParsePosition workPos = new ParsePosition(1);
            while (workText.length() > 0 && workPos.getIndex() != 0) {
                workPos.setIndex(0);
                this.ruleSet.parse(workText, workPos, 1.0d).intValue();
                if (workPos.getIndex() == 0) {
                    break;
                }
                zeroCount2++;
                parsePosition2.setIndex(parsePosition.getIndex() + workPos.getIndex());
                workText = workText.substring(workPos.getIndex());
                while (workText.length() > 0 && workText.charAt(0) == ' ') {
                    workText = workText.substring(1);
                    parsePosition2.setIndex(parsePosition.getIndex() + 1);
                }
            }
            String text3 = text.substring(parsePosition.getIndex());
            parsePosition2.setIndex(0);
            zeroCount = zeroCount2;
            text2 = text3;
        } else {
            zeroCount = 0;
            text2 = text;
        }
        Number result = super.doParse(text2, parsePosition2, this.withZeros ? 1.0d : baseValue, upperBound, false);
        if (!this.withZeros) {
            return result;
        }
        long n = result.longValue();
        long d = 1;
        while (d <= n) {
            d *= 10;
        }
        for (zeroCount = 
/*
Method generation error in method: android.icu.text.NumeratorSubstitution.doParse(java.lang.String, java.text.ParsePosition, double, double, boolean):java.lang.Number, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r11_2 'zeroCount' int) = (r11_0 'zeroCount' int), (r11_1 'zeroCount' int) binds: {(r11_0 'zeroCount' int)=B:15:0x0062, (r11_1 'zeroCount' int)=B:16:0x0071} in method: android.icu.text.NumeratorSubstitution.doParse(java.lang.String, java.text.ParsePosition, double, double, boolean):java.lang.Number, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 19 more

*/

    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return newRuleValue / oldRuleValue;
    }

    public double calcUpperBound(double oldUpperBound) {
        return this.denominator;
    }

    char tokenChar() {
        return '<';
    }
}
