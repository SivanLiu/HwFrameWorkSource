package android.icu.math;

import android.icu.lang.UCharacter;
import android.icu.text.PluralRules;
import java.io.Serializable;
import java.math.BigInteger;

public class BigDecimal extends Number implements Serializable, Comparable<BigDecimal> {
    private static final int MaxArg = 999999999;
    private static final int MaxExp = 999999999;
    private static final int MinArg = -999999999;
    private static final int MinExp = -999999999;
    public static final BigDecimal ONE = new BigDecimal(1);
    public static final int ROUND_CEILING = 2;
    public static final int ROUND_DOWN = 1;
    public static final int ROUND_FLOOR = 3;
    public static final int ROUND_HALF_DOWN = 5;
    public static final int ROUND_HALF_EVEN = 6;
    public static final int ROUND_HALF_UP = 4;
    public static final int ROUND_UNNECESSARY = 7;
    public static final int ROUND_UP = 0;
    public static final BigDecimal TEN = new BigDecimal(10);
    public static final BigDecimal ZERO = new BigDecimal(0);
    private static byte[] bytecar = new byte[190];
    private static byte[] bytedig = diginit();
    private static final byte isneg = (byte) -1;
    private static final byte ispos = (byte) 1;
    private static final byte iszero = (byte) 0;
    private static final MathContext plainMC = new MathContext(0, 0);
    private static final long serialVersionUID = 8245355804974198832L;
    private int exp;
    private byte form;
    private byte ind;
    private byte[] mant;

    public BigDecimal(java.math.BigDecimal bd) {
        this(bd.toString());
    }

    public BigDecimal(BigInteger bi) {
        this(bi.toString(10));
    }

    public BigDecimal(BigInteger bi, int scale) {
        this(bi.toString(10));
        if (scale >= 0) {
            this.exp = -scale;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Negative scale: ");
        stringBuilder.append(scale);
        throw new NumberFormatException(stringBuilder.toString());
    }

    public BigDecimal(char[] inchars) {
        this(inchars, 0, inchars.length);
    }

    /* JADX WARNING: Removed duplicated region for block: B:78:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0111  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0124  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0199  */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x015c  */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x01be  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public BigDecimal(char[] inchars, int offset, int length) {
        int length2;
        int offset2;
        int $2;
        int dotoff;
        this.form = (byte) 0;
        char si = 0;
        if (length <= 0) {
            bad(inchars);
        }
        this.ind = (byte) 1;
        if (inchars[offset] == '-') {
            length2 = length - 1;
            if (length2 == 0) {
                bad(inchars);
            }
            this.ind = (byte) -1;
            offset2 = offset + 1;
        } else if (inchars[offset] == '+') {
            length2 = length - 1;
            if (length2 == 0) {
                bad(inchars);
            }
            offset2 = offset + 1;
        } else {
            offset2 = offset;
            length2 = length;
        }
        boolean exotic = false;
        boolean hadexp = false;
        int d = 0;
        int dotoff2 = -1;
        int last = -1;
        int $1 = length2;
        int i = offset2;
        while ($1 > 0) {
            si = inchars[i];
            if (si >= '0' && si <= '9') {
                d++;
                last = i;
            } else if (si == '.') {
                if (dotoff2 >= 0) {
                    bad(inchars);
                }
                dotoff2 = i - offset2;
            } else if (si == 'e' || si == 'E') {
                int k;
                char sj;
                int dvalue;
                int dvalue2;
                if (i - offset2 > length2 - 2) {
                    bad(inchars);
                }
                boolean eneg = false;
                if (inchars[i + 1] == '-') {
                    eneg = true;
                    k = i + 2;
                } else if (inchars[i + 1] == '+') {
                    k = i + 2;
                } else {
                    k = i + 1;
                }
                int k2 = k;
                boolean eneg2 = eneg;
                int elen = length2 - (k2 - offset2);
                if (((elen == 0 ? 1 : 0) | (elen > 9 ? 1 : 0)) != 0) {
                    bad(inchars);
                }
                $2 = elen;
                int j = k2;
                while ($2 > 0) {
                    sj = inchars[j];
                    if (sj < '0') {
                        bad(inchars);
                    }
                    if (sj > '9') {
                        if (!UCharacter.isDigit(sj)) {
                            bad(inchars);
                        }
                        dvalue = UCharacter.digit(sj, 10);
                        if (dvalue < 0) {
                            bad(inchars);
                        }
                    } else {
                        dvalue = sj - 48;
                    }
                    this.exp = (this.exp * 10) + dvalue;
                    $2--;
                    j++;
                }
                if (eneg2) {
                    this.exp = -this.exp;
                }
                hadexp = true;
                if (d == 0) {
                    bad(inchars);
                }
                if (dotoff2 >= 0) {
                    this.exp = (this.exp + dotoff2) - d;
                }
                $2 = last - 1;
                i = offset2;
                length2 = d;
                dotoff = dotoff2;
                while (i <= $2) {
                    si = inchars[i];
                    int $3 = $2;
                    if (si != '0') {
                        if (si != '.') {
                            if (si <= '9' || UCharacter.digit(si, 10) != 0) {
                                break;
                            }
                            offset2++;
                            dotoff--;
                            length2--;
                        } else {
                            offset2++;
                            dotoff--;
                        }
                    } else {
                        offset2++;
                        dotoff--;
                        length2--;
                    }
                    i++;
                    $2 = $3;
                }
                this.mant = new byte[length2];
                $2 = offset2;
                char c;
                if (exotic) {
                    c = si;
                    offset2 = length2;
                    i = 0;
                    while (offset2 > 0) {
                        if (i == dotoff) {
                            $2++;
                        }
                        this.mant[i] = (byte) (inchars[$2] - 48);
                        $2++;
                        offset2--;
                        i++;
                    }
                } else {
                    j = length2;
                    i = 0;
                    while (j > 0) {
                        if (i == dotoff) {
                            $2++;
                        }
                        sj = inchars[$2];
                        int offset3 = offset2;
                        if (sj <= '9') {
                            c = si;
                            this.mant[i] = (byte) (sj - 48);
                        } else {
                            c = si;
                            dvalue2 = UCharacter.digit(sj, 10);
                            if (dvalue2 < 0) {
                                bad(inchars);
                            }
                            this.mant[i] = (byte) dvalue2;
                            dvalue = dvalue2;
                        }
                        $2++;
                        j--;
                        i++;
                        offset2 = offset3;
                        si = c;
                    }
                    c = si;
                }
                if (this.mant[0] != (byte) 0) {
                    this.ind = (byte) 0;
                    if (this.exp > 0) {
                        this.exp = 0;
                    }
                    if (hadexp) {
                        this.mant = ZERO.mant;
                        this.exp = 0;
                        return;
                    }
                    return;
                } else if (hadexp) {
                    offset2 = 1;
                    this.form = (byte) 1;
                    int mag = (this.exp + this.mant.length) - 1;
                    dvalue2 = mag < -999999999 ? 1 : 0;
                    if (mag <= 999999999) {
                        offset2 = 0;
                    }
                    if ((offset2 | dvalue2) != 0) {
                        bad(inchars);
                        return;
                    }
                    return;
                } else {
                    return;
                }
            } else {
                if (!UCharacter.isDigit(si)) {
                    bad(inchars);
                }
                d++;
                exotic = true;
                last = i;
            }
            $1--;
            i++;
        }
        if (d == 0) {
        }
        if (dotoff2 >= 0) {
        }
        $2 = last - 1;
        i = offset2;
        length2 = d;
        dotoff = dotoff2;
        while (i <= $2) {
        }
        this.mant = new byte[length2];
        $2 = offset2;
        if (exotic) {
        }
        if (this.mant[0] != (byte) 0) {
        }
    }

    public BigDecimal(double num) {
        this(new java.math.BigDecimal(num).toString());
    }

    public BigDecimal(int num) {
        this.form = (byte) 0;
        if (num > 9 || num < -9) {
            if (num > 0) {
                this.ind = (byte) 1;
                num = -num;
            } else {
                this.ind = (byte) -1;
            }
            int mun = num;
            int i = 9;
            while (true) {
                mun /= 10;
                if (mun == 0) {
                    break;
                }
                i--;
            }
            this.mant = new byte[(10 - i)];
            int i2 = (10 - i) - 1;
            while (true) {
                this.mant[i2] = (byte) (-((byte) (num % 10)));
                num /= 10;
                if (num != 0) {
                    i2--;
                } else {
                    return;
                }
            }
        }
        if (num == 0) {
            this.mant = ZERO.mant;
            this.ind = (byte) 0;
        } else if (num == 1) {
            this.mant = ONE.mant;
            this.ind = (byte) 1;
        } else if (num == -1) {
            this.mant = ONE.mant;
            this.ind = (byte) -1;
        } else {
            this.mant = new byte[1];
            if (num > 0) {
                this.mant[0] = (byte) num;
                this.ind = (byte) 1;
            } else {
                this.mant[0] = (byte) (-num);
                this.ind = (byte) -1;
            }
        }
    }

    public BigDecimal(long num) {
        this.form = (byte) 0;
        if (num > 0) {
            this.ind = (byte) 1;
            num = -num;
        } else if (num == 0) {
            this.ind = (byte) 0;
        } else {
            this.ind = (byte) -1;
        }
        long mun = num;
        int i = 18;
        while (true) {
            mun /= 10;
            if (mun == 0) {
                break;
            }
            i--;
        }
        this.mant = new byte[(19 - i)];
        int i2 = (19 - i) - 1;
        while (true) {
            this.mant[i2] = (byte) (-((byte) ((int) (num % 10))));
            num /= 10;
            if (num != 0) {
                i2--;
            } else {
                return;
            }
        }
    }

    public BigDecimal(String string) {
        this(string.toCharArray(), 0, string.length());
    }

    private BigDecimal() {
        this.form = (byte) 0;
    }

    public BigDecimal abs() {
        return abs(plainMC);
    }

    public BigDecimal abs(MathContext set) {
        if (this.ind == (byte) -1) {
            return negate(set);
        }
        return plus(set);
    }

    public BigDecimal add(BigDecimal rhs) {
        return add(rhs, plainMC);
    }

    public BigDecimal add(BigDecimal rhs, MathContext set) {
        BigDecimal bigDecimal;
        BigDecimal rhs2 = rhs;
        MathContext mathContext = set;
        if (mathContext.lostDigits) {
            bigDecimal = this;
            bigDecimal.checkdigits(rhs2, mathContext.digits);
        } else {
            bigDecimal = this;
        }
        BigDecimal lhs = bigDecimal;
        if (lhs.ind == (byte) 0 && mathContext.form != 0) {
            return rhs.plus(set);
        }
        if (rhs2.ind == (byte) 0 && mathContext.form != 0) {
            return lhs.plus(mathContext);
        }
        int newlen;
        int tlen;
        int mult;
        int reqdig = mathContext.digits;
        if (reqdig > 0) {
            if (lhs.mant.length > reqdig) {
                lhs = clone(lhs).round(mathContext);
            }
            if (rhs2.mant.length > reqdig) {
                rhs2 = clone(rhs).round(mathContext);
            }
        }
        BigDecimal res = new BigDecimal();
        int newlen2 = 0;
        byte[] usel = lhs.mant;
        int tlen2 = 0;
        int usellen = lhs.mant.length;
        int mult2 = 0;
        byte[] user = rhs2.mant;
        byte[] t = null;
        int userlen = rhs2.mant.length;
        int ia = 0;
        int ib = 0;
        int ea = 0;
        if (lhs.exp == rhs2.exp) {
            res.exp = lhs.exp;
            newlen = newlen2;
        } else if (lhs.exp > rhs2.exp) {
            newlen = (lhs.exp + usellen) - rhs2.exp;
            if (newlen < (userlen + reqdig) + 1 || reqdig <= 0) {
                res.exp = rhs2.exp;
                if (newlen > reqdig + 1 && reqdig > 0) {
                    tlen = (newlen - reqdig) - 1;
                    userlen -= tlen;
                    res.exp += tlen;
                    newlen = reqdig + 1;
                    tlen2 = tlen;
                }
                if (newlen > usellen) {
                    usellen = newlen;
                }
            } else {
                res.mant = usel;
                res.exp = lhs.exp;
                res.ind = lhs.ind;
                if (usellen < reqdig) {
                    res.mant = extend(lhs.mant, reqdig);
                    res.exp -= reqdig - usellen;
                }
                return res.finish(mathContext, false);
            }
        } else {
            newlen = (rhs2.exp + userlen) - lhs.exp;
            if (newlen < (usellen + reqdig) + 1 || reqdig <= 0) {
                res.exp = lhs.exp;
                if (newlen > reqdig + 1 && reqdig > 0) {
                    tlen = (newlen - reqdig) - 1;
                    usellen -= tlen;
                    res.exp += tlen;
                    newlen = reqdig + 1;
                    tlen2 = tlen;
                }
                if (newlen > userlen) {
                    userlen = newlen;
                }
            } else {
                res.mant = user;
                res.exp = rhs2.exp;
                res.ind = rhs2.ind;
                if (userlen < reqdig) {
                    res.mant = extend(rhs2.mant, reqdig);
                    res.exp -= reqdig - userlen;
                }
                return res.finish(mathContext, false);
            }
        }
        if (lhs.ind == (byte) 0) {
            res.ind = (byte) 1;
        } else {
            res.ind = lhs.ind;
        }
        if ((lhs.ind == (byte) -1 ? 1 : null) == (rhs2.ind == -1 ? 1 : null)) {
            newlen = 1;
        } else {
            newlen = -1;
            if (rhs2.ind != (byte) 0) {
                BigDecimal bigDecimal2;
                if (((usellen < userlen ? 1 : 0) | (lhs.ind == (byte) 0 ? 1 : 0)) != 0) {
                    byte[] t2 = usel;
                    usel = user;
                    user = t2;
                    tlen2 = usellen;
                    usellen = userlen;
                    userlen = tlen2;
                    res.ind = (byte) (-res.ind);
                    bigDecimal2 = rhs2;
                    mult = -1;
                    t = t2;
                } else if (usellen <= userlen) {
                    bigDecimal2 = rhs2;
                    int ea2 = usel.length - 1;
                    mult = -1;
                    int eb = user.length - 1;
                    newlen = 0;
                    tlen = 0;
                    while (true) {
                        byte ca;
                        if (newlen <= ea2) {
                            ca = usel[newlen];
                        } else if (tlen <= eb) {
                            ca = (byte) 0;
                        } else if (mathContext.form != 0) {
                            return ZERO;
                        } else {
                            ea = ea2;
                            ia = newlen;
                            ib = tlen;
                        }
                        byte ca2 = ca;
                        if (tlen <= eb) {
                            ca = user[tlen];
                        } else {
                            ca = (byte) 0;
                        }
                        byte cb = ca;
                        if (ca2 == cb) {
                            newlen++;
                            tlen++;
                            ea2 = ea2;
                        } else if (ca2 < cb) {
                            byte[] t3 = usel;
                            usel = user;
                            user = t3;
                            tlen2 = usellen;
                            usellen = userlen;
                            userlen = tlen2;
                            int ea3 = ea2;
                            res.ind = (byte) (-res.ind);
                            ia = newlen;
                            ib = tlen;
                            t = t3;
                            ea = ea3;
                        } else {
                            ia = newlen;
                            ib = tlen;
                            ea = ea2;
                        }
                    }
                }
                res.mant = byteaddsub(usel, usellen, user, userlen, mult, false);
                return res.finish(mathContext, false);
            }
        }
        mult = newlen;
        res.mant = byteaddsub(usel, usellen, user, userlen, mult, false);
        return res.finish(mathContext, false);
    }

    public int compareTo(BigDecimal rhs) {
        return compareTo(rhs, plainMC);
    }

    public int compareTo(BigDecimal rhs, MathContext set) {
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        int i = 1;
        if (((this.ind == rhs.ind ? 1 : 0) & (this.exp == rhs.exp ? 1 : 0)) != 0) {
            int thislength = this.mant.length;
            if (thislength < rhs.mant.length) {
                return (byte) (-this.ind);
            }
            if (thislength > rhs.mant.length) {
                return this.ind;
            }
            int i2 = thislength <= set.digits ? 1 : 0;
            if (set.digits != 0) {
                i = 0;
            }
            if ((i2 | i) != 0) {
                i2 = thislength;
                int i3 = 0;
                while (i2 > 0) {
                    if (this.mant[i3] < rhs.mant[i3]) {
                        return (byte) (-this.ind);
                    }
                    if (this.mant[i3] > rhs.mant[i3]) {
                        return this.ind;
                    }
                    i2--;
                    i3++;
                }
                return 0;
            }
        } else if (this.ind < rhs.ind) {
            return -1;
        } else {
            if (this.ind > rhs.ind) {
                return 1;
            }
        }
        BigDecimal newrhs = clone(rhs);
        newrhs.ind = (byte) (-newrhs.ind);
        return add(newrhs, set).ind;
    }

    public BigDecimal divide(BigDecimal rhs) {
        return dodivide('D', rhs, plainMC, -1);
    }

    public BigDecimal divide(BigDecimal rhs, int round) {
        return dodivide('D', rhs, new MathContext(0, 0, false, round), -1);
    }

    public BigDecimal divide(BigDecimal rhs, int scale, int round) {
        if (scale >= 0) {
            return dodivide('D', rhs, new MathContext(0, 0, false, round), scale);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Negative scale: ");
        stringBuilder.append(scale);
        throw new ArithmeticException(stringBuilder.toString());
    }

    public BigDecimal divide(BigDecimal rhs, MathContext set) {
        return dodivide('D', rhs, set, -1);
    }

    public BigDecimal divideInteger(BigDecimal rhs) {
        return dodivide('I', rhs, plainMC, 0);
    }

    public BigDecimal divideInteger(BigDecimal rhs, MathContext set) {
        return dodivide('I', rhs, set, 0);
    }

    public BigDecimal max(BigDecimal rhs) {
        return max(rhs, plainMC);
    }

    public BigDecimal max(BigDecimal rhs, MathContext set) {
        if (compareTo(rhs, set) >= 0) {
            return plus(set);
        }
        return rhs.plus(set);
    }

    public BigDecimal min(BigDecimal rhs) {
        return min(rhs, plainMC);
    }

    public BigDecimal min(BigDecimal rhs, MathContext set) {
        if (compareTo(rhs, set) <= 0) {
            return plus(set);
        }
        return rhs.plus(set);
    }

    public BigDecimal multiply(BigDecimal rhs) {
        return multiply(rhs, plainMC);
    }

    public BigDecimal multiply(BigDecimal rhs, MathContext set) {
        BigDecimal bigDecimal;
        byte[] multer;
        byte[] multand;
        int acclen;
        byte[] multand2;
        BigDecimal res;
        int acclen2;
        boolean acclen3;
        BigDecimal rhs2 = rhs;
        MathContext mathContext = set;
        if (mathContext.lostDigits) {
            bigDecimal = this;
            bigDecimal.checkdigits(rhs2, mathContext.digits);
        } else {
            bigDecimal = this;
        }
        BigDecimal lhs = bigDecimal;
        int padding = 0;
        int reqdig = mathContext.digits;
        if (reqdig > 0) {
            if (lhs.mant.length > reqdig) {
                lhs = clone(lhs).round(mathContext);
            }
            if (rhs2.mant.length > reqdig) {
                rhs2 = clone(rhs).round(mathContext);
            }
        } else {
            if (lhs.exp > 0) {
                padding = 0 + lhs.exp;
            }
            if (rhs2.exp > 0) {
                padding += rhs2.exp;
            }
        }
        if (lhs.mant.length < rhs2.mant.length) {
            multer = lhs.mant;
            multand = rhs2.mant;
        } else {
            multer = rhs2.mant;
            multand = lhs.mant;
        }
        int multandlen = (multer.length + multand.length) - 1;
        boolean z = false;
        if (multer[0] * multand[0] > 9) {
            acclen = multandlen + 1;
        } else {
            acclen = multandlen;
        }
        BigDecimal res2 = new BigDecimal();
        byte[] acc = new byte[acclen];
        int multandlen2 = multandlen;
        byte mult = (byte) 0;
        int n = 0;
        int $7 = multer.length;
        byte[] acc2 = acc;
        while ($7 > 0) {
            byte[] multer2;
            byte mult2 = multer[n];
            if (mult2 != (byte) 0) {
                int length = acc2.length;
                byte[] bArr = acc2;
                multer2 = multer;
                multer = acc2;
                acc2 = multand;
                multand2 = multand;
                res = res2;
                acclen2 = acclen;
                acclen3 = z;
                acc2 = byteaddsub(bArr, length, acc2, multandlen2, mult2, true);
            } else {
                multer2 = multer;
                multand2 = multand;
                acclen2 = acclen;
                multer = acc2;
                res = res2;
                acclen3 = z;
            }
            multandlen2--;
            $7--;
            n++;
            res2 = res;
            z = acclen3;
            mult = mult2;
            multer = multer2;
            multand = multand2;
            acclen = acclen2;
        }
        multand2 = multand;
        acclen2 = acclen;
        multer = acc2;
        res = res2;
        acclen3 = z;
        res.ind = (byte) (lhs.ind * rhs2.ind);
        res.exp = (lhs.exp + rhs2.exp) - padding;
        if (padding == 0) {
            res.mant = multer;
        } else {
            res.mant = extend(multer, multer.length + padding);
        }
        return res.finish(mathContext, acclen3);
    }

    public BigDecimal negate() {
        return negate(plainMC);
    }

    public BigDecimal negate(MathContext set) {
        if (set.lostDigits) {
            checkdigits((BigDecimal) null, set.digits);
        }
        BigDecimal res = clone(this);
        res.ind = (byte) (-res.ind);
        return res.finish(set, false);
    }

    public BigDecimal plus() {
        return plus(plainMC);
    }

    public BigDecimal plus(MathContext set) {
        if (set.lostDigits) {
            checkdigits((BigDecimal) null, set.digits);
        }
        if (set.form == 0 && this.form == (byte) 0 && (this.mant.length <= set.digits || set.digits == 0)) {
            return this;
        }
        return clone(this).finish(set, false);
    }

    public BigDecimal pow(BigDecimal rhs) {
        return pow(rhs, plainMC);
    }

    public BigDecimal pow(BigDecimal rhs, MathContext set) {
        int workdigits;
        int L = 0;
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        int n = rhs.intcheck(-999999999, 999999999);
        BigDecimal lhs = this;
        int reqdig = set.digits;
        StringBuilder stringBuilder;
        if (reqdig == 0) {
            if (rhs.ind != (byte) -1) {
                workdigits = 0;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Negative power: ");
                stringBuilder.append(rhs.toString());
                throw new ArithmeticException(stringBuilder.toString());
            }
        } else if (rhs.mant.length + rhs.exp <= reqdig) {
            if (lhs.mant.length > reqdig) {
                lhs = clone(lhs).round(set);
            }
            L = rhs.mant.length + rhs.exp;
            workdigits = (reqdig + L) + 1;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Too many digits: ");
            stringBuilder.append(rhs.toString());
            throw new ArithmeticException(stringBuilder.toString());
        }
        BigDecimal lhs2 = lhs;
        MathContext workset = new MathContext(workdigits, set.form, false, set.roundingMode);
        workdigits = ONE;
        if (n == 0) {
            return workdigits;
        }
        BigDecimal workdigits2;
        if (n < 0) {
            n = -n;
        }
        boolean seenbit = false;
        int i = 1;
        while (true) {
            n += n;
            if (n < 0) {
                seenbit = true;
                workdigits2 = workdigits2.multiply(lhs2, workset);
            }
            if (i == 31) {
                break;
            }
            if (seenbit) {
                workdigits = workdigits.multiply(workdigits, workset);
            }
            i++;
        }
        if (rhs.ind < (byte) 0) {
            workdigits2 = ONE.divide((BigDecimal) workdigits, workset);
        }
        return workdigits2.finish(set, true);
    }

    public BigDecimal remainder(BigDecimal rhs) {
        return dodivide('R', rhs, plainMC, -1);
    }

    public BigDecimal remainder(BigDecimal rhs, MathContext set) {
        return dodivide('R', rhs, set, -1);
    }

    public BigDecimal subtract(BigDecimal rhs) {
        return subtract(rhs, plainMC);
    }

    public BigDecimal subtract(BigDecimal rhs, MathContext set) {
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        BigDecimal newrhs = clone(rhs);
        newrhs.ind = (byte) (-newrhs.ind);
        return add(newrhs, set);
    }

    public byte byteValueExact() {
        int num = intValueExact();
        int i = 0;
        int i2 = num > 127 ? 1 : 0;
        if (num < -128) {
            i = 1;
        }
        if ((i | i2) == 0) {
            return (byte) num;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Conversion overflow: ");
        stringBuilder.append(toString());
        throw new ArithmeticException(stringBuilder.toString());
    }

    public double doubleValue() {
        return Double.valueOf(toString()).doubleValue();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BigDecimal)) {
            return false;
        }
        BigDecimal rhs = (BigDecimal) obj;
        if (this.ind != rhs.ind) {
            return false;
        }
        int $8;
        int i;
        if ((((this.mant.length == rhs.mant.length ? 1 : 0) & (this.exp == rhs.exp ? 1 : 0)) & (this.form == rhs.form ? 1 : 0)) != 0) {
            $8 = this.mant.length;
            i = 0;
            while ($8 > 0) {
                if (this.mant[i] != rhs.mant[i]) {
                    return false;
                }
                $8--;
                i++;
            }
        } else {
            char[] lca = layout();
            char[] rca = rhs.layout();
            if (lca.length != rca.length) {
                return false;
            }
            $8 = lca.length;
            i = 0;
            while ($8 > 0) {
                if (lca[i] != rca[i]) {
                    return false;
                }
                $8--;
                i++;
            }
        }
        return true;
    }

    public float floatValue() {
        return Float.valueOf(toString()).floatValue();
    }

    public String format(int before, int after) {
        return format(before, after, -1, -1, 1, 4);
    }

    /* JADX WARNING: Removed duplicated region for block: B:95:0x018e  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00e2 A:{LOOP_START, PHI: r7 r8 , LOOP:0: B:67:0x00e2->B:92:0x0174} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x019a  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x01dc  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00c0  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00bc  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00e2 A:{LOOP_START, PHI: r7 r8 , LOOP:0: B:67:0x00e2->B:92:0x0174} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x018e  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x019a  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x01dc  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00bc  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00c0  */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x018e  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00e2 A:{LOOP_START, PHI: r7 r8 , LOOP:0: B:67:0x00e2->B:92:0x0174} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x019a  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x01dc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String format(int before, int after, int explaces, int exdigits, int exformint, int exround) {
        byte[] newmant;
        BigDecimal num;
        int mag;
        char[] a;
        int i = before;
        int i2 = after;
        int i3 = explaces;
        int i4 = exdigits;
        int i5 = exformint;
        int i6 = exround;
        int mag2 = 0;
        int thisafter = 0;
        int lead;
        if (((i < -1 ? 1 : 0) | (i == 0 ? 1 : 0)) != 0) {
            lead = 0;
            badarg("format", 1, String.valueOf(before));
        } else {
            lead = 0;
        }
        if (i2 < -1) {
            badarg("format", 2, String.valueOf(after));
        }
        if (((i3 < -1 ? 1 : 0) | (i3 == 0 ? 1 : 0)) != 0) {
            badarg("format", 3, String.valueOf(explaces));
        }
        if (i4 < -1) {
            badarg("format", 4, String.valueOf(explaces));
        }
        if (!(i5 == 1 || i5 == 2)) {
            if (i5 == -1) {
                i5 = 1;
            } else {
                badarg("format", 5, String.valueOf(exformint));
            }
        }
        int exformint2 = i5;
        if (i6 == 4) {
            newmant = null;
            i5 = i6;
        } else if (i6 == -1) {
            i5 = 4;
            newmant = null;
        } else {
            try {
                newmant = null;
                try {
                    MathContext mathContext = new MathContext(9, 1, null, i6);
                    i5 = i6;
                } catch (IllegalArgumentException e) {
                    badarg("format", 6, String.valueOf(exround));
                    i5 = i6;
                    num = clone(this);
                    if (i4 == -1) {
                    }
                    mag = mag2;
                    if (i2 < 0) {
                    }
                    a = num.layout();
                    if (i > 0) {
                    }
                    if (i3 > 0) {
                    }
                    exformint2 = 0;
                    return new String(a);
                }
            } catch (IllegalArgumentException e2) {
                newmant = null;
                badarg("format", 6, String.valueOf(exround));
                i5 = i6;
                num = clone(this);
                if (i4 == -1) {
                }
                mag = mag2;
                if (i2 < 0) {
                }
                a = num.layout();
                if (i > 0) {
                }
                if (i3 > 0) {
                }
                exformint2 = 0;
                return new String(a);
            }
        }
        num = clone(this);
        if (i4 == -1) {
            num.form = (byte) 0;
        } else if (num.ind == (byte) 0) {
            num.form = (byte) 0;
        } else {
            int thisafter2;
            int p;
            char[] newa;
            int i7;
            mag = num.exp + num.mant.length;
            if (mag > i4) {
                num.form = (byte) exformint2;
            } else if (mag < -5) {
                num.form = (byte) exformint2;
            } else {
                num.form = (byte) 0;
            }
            int thisafter3;
            int i8;
            if (i2 < 0) {
                while (true) {
                    if (num.form == (byte) 0) {
                        thisafter3 = -num.exp;
                    } else if (num.form == (byte) 1) {
                        thisafter3 = num.mant.length - 1;
                    } else {
                        thisafter3 = ((num.exp + num.mant.length) - 1) % 3;
                        if (thisafter3 < 0) {
                            thisafter3 = 3 + thisafter3;
                        }
                        thisafter3++;
                        if (thisafter3 >= num.mant.length) {
                            thisafter2 = 0;
                        } else {
                            thisafter2 = num.mant.length - thisafter3;
                        }
                        thisafter3 = thisafter2;
                    }
                    int i9;
                    if (thisafter3 == i2) {
                        i9 = exformint2;
                        i8 = mag;
                        break;
                    } else if (thisafter3 < i2) {
                        byte[] newmant2 = extend(num.mant, (num.mant.length + i2) - thisafter3);
                        num.mant = newmant2;
                        num.exp -= i2 - thisafter3;
                        if (num.exp >= -999999999) {
                            i8 = mag;
                            newmant = newmant2;
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Exponent Overflow: ");
                            stringBuilder.append(num.exp);
                            throw new ArithmeticException(stringBuilder.toString());
                        }
                    } else {
                        i9 = exformint2;
                        i8 = mag;
                        int chop = thisafter3 - i2;
                        if (chop > num.mant.length) {
                            num.mant = ZERO.mant;
                            num.ind = (byte) 0;
                            num.exp = 0;
                        } else {
                            int need = num.mant.length - chop;
                            int oldexp = num.exp;
                            num.round(need, i5);
                            if (num.exp - oldexp == chop) {
                                break;
                            }
                        }
                        exformint2 = i9;
                        mag = i8;
                        i4 = exdigits;
                    }
                }
            } else {
                i8 = mag;
                thisafter3 = thisafter;
            }
            a = num.layout();
            if (i > 0) {
                thisafter2 = a.length;
                p = 0;
                while (thisafter2 > 0 && a[p] != '.' && a[p] != 'E') {
                    thisafter2--;
                    p++;
                }
                if (p > i) {
                    badarg("format", 1, String.valueOf(before));
                }
                if (p < i) {
                    newa = new char[((a.length + i) - p)];
                    exformint2 = i - p;
                    mag = 0;
                    while (exformint2 > 0) {
                        newa[mag] = ' ';
                        exformint2--;
                        mag++;
                    }
                    System.arraycopy(a, 0, newa, mag, a.length);
                    a = newa;
                    i7 = mag;
                }
            }
            if (i3 > 0) {
                exformint2 = a.length - 1;
                p = a.length - 1;
                while (exformint2 > 0 && a[p] != 'E') {
                    exformint2--;
                    p--;
                }
                if (p == 0) {
                    newa = new char[((a.length + i3) + 2)];
                    System.arraycopy(a, 0, newa, 0, a.length);
                    exformint2 = i3 + 2;
                    i7 = a.length;
                    while (exformint2 > 0) {
                        newa[i7] = ' ';
                        exformint2--;
                        i7++;
                    }
                    a = newa;
                } else {
                    exformint2 = (a.length - p) - 2;
                    if (exformint2 > i3) {
                        badarg("format", 3, String.valueOf(explaces));
                    }
                    if (exformint2 < i3) {
                        newa = new char[((a.length + i3) - exformint2)];
                        System.arraycopy(a, 0, newa, 0, p + 2);
                        i = i3 - exformint2;
                        mag = p + 2;
                        while (i > 0) {
                            newa[mag] = '0';
                            i--;
                            mag++;
                        }
                        System.arraycopy(a, p + 2, newa, mag, exformint2);
                        a = newa;
                        i7 = mag;
                    }
                    return new String(a);
                }
            }
            exformint2 = 0;
            return new String(a);
        }
        mag = mag2;
        if (i2 < 0) {
        }
        a = num.layout();
        if (i > 0) {
        }
        if (i3 > 0) {
        }
        exformint2 = 0;
        return new String(a);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public int intValue() {
        return toBigInteger().intValue();
    }

    public int intValueExact() {
        if (this.ind == (byte) 0) {
            return 0;
        }
        StringBuilder stringBuilder;
        int useexp;
        int lodigit = this.mant.length - 1;
        if (this.exp < 0) {
            lodigit += this.exp;
            if (!allzero(this.mant, lodigit + 1)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Decimal part non-zero: ");
                stringBuilder.append(toString());
                throw new ArithmeticException(stringBuilder.toString());
            } else if (lodigit < 0) {
                return 0;
            } else {
                useexp = 0;
            }
        } else if (this.exp + lodigit <= 9) {
            useexp = this.exp;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Conversion overflow: ");
            stringBuilder.append(toString());
            throw new ArithmeticException(stringBuilder.toString());
        }
        int result = 0;
        int $16 = lodigit + useexp;
        for (int i = 0; i <= $16; i++) {
            result *= 10;
            if (i <= lodigit) {
                result += this.mant[i];
            }
        }
        if (lodigit + useexp != 9 || result / 1000000000 == this.mant[0]) {
            if (this.ind == (byte) 1) {
                return result;
            }
            return -result;
        } else if (result == Integer.MIN_VALUE && this.ind == (byte) -1 && this.mant[0] == (byte) 2) {
            return result;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Conversion overflow: ");
            stringBuilder.append(toString());
            throw new ArithmeticException(stringBuilder.toString());
        }
    }

    public long longValue() {
        return toBigInteger().longValue();
    }

    public long longValueExact() {
        if (this.ind == (byte) 0) {
            return 0;
        }
        int useexp;
        int lodigit = this.mant.length - 1;
        StringBuilder stringBuilder;
        if (this.exp < 0) {
            int cstart;
            lodigit += this.exp;
            if (lodigit < 0) {
                cstart = 0;
            } else {
                cstart = lodigit + 1;
            }
            if (!allzero(this.mant, cstart)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Decimal part non-zero: ");
                stringBuilder.append(toString());
                throw new ArithmeticException(stringBuilder.toString());
            } else if (lodigit < 0) {
                return 0;
            } else {
                useexp = 0;
            }
        } else if (this.exp + this.mant.length <= 18) {
            useexp = this.exp;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Conversion overflow: ");
            stringBuilder.append(toString());
            throw new ArithmeticException(stringBuilder.toString());
        }
        long result = 0;
        int $17 = lodigit + useexp;
        for (int i = 0; i <= $17; i++) {
            result *= 10;
            if (i <= lodigit) {
                result += (long) this.mant[i];
            }
        }
        if (lodigit + useexp != 18 || result / 1000000000000000000L == ((long) this.mant[0])) {
            if (this.ind == (byte) 1) {
                return result;
            }
            return -result;
        } else if (result == Long.MIN_VALUE && this.ind == (byte) -1 && this.mant[0] == (byte) 9) {
            return result;
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Conversion overflow: ");
            stringBuilder2.append(toString());
            throw new ArithmeticException(stringBuilder2.toString());
        }
    }

    public BigDecimal movePointLeft(int n) {
        BigDecimal res = clone(this);
        res.exp -= n;
        return res.finish(plainMC, false);
    }

    public BigDecimal movePointRight(int n) {
        BigDecimal res = clone(this);
        res.exp += n;
        return res.finish(plainMC, false);
    }

    public int scale() {
        if (this.exp >= 0) {
            return 0;
        }
        return -this.exp;
    }

    public BigDecimal setScale(int scale) {
        return setScale(scale, 7);
    }

    public BigDecimal setScale(int scale, int round) {
        int ourscale = scale();
        if (ourscale == scale && this.form == (byte) 0) {
            return this;
        }
        BigDecimal res = clone(this);
        if (ourscale <= scale) {
            int padding;
            if (ourscale == 0) {
                padding = res.exp + scale;
            } else {
                padding = scale - ourscale;
            }
            res.mant = extend(res.mant, res.mant.length + padding);
            res.exp = -scale;
        } else if (scale >= 0) {
            res = res.round(res.mant.length - (ourscale - scale), round);
            if (res.exp != (-scale)) {
                res.mant = extend(res.mant, res.mant.length + 1);
                res.exp--;
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Negative scale: ");
            stringBuilder.append(scale);
            throw new ArithmeticException(stringBuilder.toString());
        }
        res.form = (byte) 0;
        return res;
    }

    public short shortValueExact() {
        int num = intValueExact();
        int i = 0;
        int i2 = num > 32767 ? 1 : 0;
        if (num < -32768) {
            i = 1;
        }
        if ((i | i2) == 0) {
            return (short) num;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Conversion overflow: ");
        stringBuilder.append(toString());
        throw new ArithmeticException(stringBuilder.toString());
    }

    public int signum() {
        return this.ind;
    }

    public java.math.BigDecimal toBigDecimal() {
        return new java.math.BigDecimal(unscaledValue(), scale());
    }

    public BigInteger toBigInteger() {
        BigDecimal res;
        int i = 1;
        int i2 = this.exp >= 0 ? 1 : 0;
        if (this.form != (byte) 0) {
            i = 0;
        }
        if ((i2 & i) != 0) {
            res = this;
        } else if (this.exp >= 0) {
            res = clone(this);
            res.form = (byte) 0;
        } else if ((-this.exp) >= this.mant.length) {
            res = ZERO;
        } else {
            res = clone(this);
            int newlen = res.mant.length + res.exp;
            byte[] newmant = new byte[newlen];
            System.arraycopy(res.mant, 0, newmant, 0, newlen);
            res.mant = newmant;
            res.form = (byte) 0;
            res.exp = 0;
        }
        return new BigInteger(new String(res.layout()));
    }

    public BigInteger toBigIntegerExact() {
        if (this.exp >= 0 || allzero(this.mant, this.mant.length + this.exp)) {
            return toBigInteger();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Decimal part non-zero: ");
        stringBuilder.append(toString());
        throw new ArithmeticException(stringBuilder.toString());
    }

    public char[] toCharArray() {
        return layout();
    }

    public String toString() {
        return new String(layout());
    }

    public BigInteger unscaledValue() {
        BigDecimal res;
        if (this.exp >= 0) {
            res = this;
        } else {
            res = clone(this);
            res.exp = 0;
        }
        return res.toBigInteger();
    }

    public static BigDecimal valueOf(double dub) {
        return new BigDecimal(new Double(dub).toString());
    }

    public static BigDecimal valueOf(long lint) {
        return valueOf(lint, 0);
    }

    public static BigDecimal valueOf(long lint, int scale) {
        BigDecimal res;
        if (lint == 0) {
            res = ZERO;
        } else if (lint == 1) {
            res = ONE;
        } else if (lint == 10) {
            res = TEN;
        } else {
            res = new BigDecimal(lint);
        }
        if (scale == 0) {
            return res;
        }
        if (scale >= 0) {
            res = clone(res);
            res.exp = -scale;
            return res;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Negative scale: ");
        stringBuilder.append(scale);
        throw new NumberFormatException(stringBuilder.toString());
    }

    private char[] layout() {
        char[] cmant = new char[this.mant.length];
        int $18 = this.mant.length;
        int i = 0;
        while ($18 > 0) {
            cmant[i] = (char) (this.mant[i] + 48);
            $18--;
            i++;
        }
        char[] rec;
        if (this.form != (byte) 0) {
            int sig;
            StringBuilder sb = new StringBuilder(cmant.length + 15);
            if (this.ind == (byte) -1) {
                sb.append('-');
            }
            $18 = (this.exp + cmant.length) - 1;
            if (this.form == (byte) 1) {
                sb.append(cmant[0]);
                if (cmant.length > 1) {
                    sb.append('.');
                    sb.append(cmant, 1, cmant.length - 1);
                }
            } else {
                sig = $18 % 3;
                if (sig < 0) {
                    sig += 3;
                }
                $18 -= sig;
                int sig2 = sig + 1;
                if (sig2 >= cmant.length) {
                    sb.append(cmant, 0, cmant.length);
                    for (sig = sig2 - cmant.length; sig > 0; sig--) {
                        sb.append('0');
                    }
                } else {
                    sb.append(cmant, 0, sig2);
                    sb.append('.');
                    sb.append(cmant, sig2, cmant.length - sig2);
                }
            }
            if ($18 != 0) {
                char csign;
                if ($18 < 0) {
                    csign = '-';
                    $18 = -$18;
                } else {
                    csign = '+';
                }
                char csign2 = csign;
                sb.append('E');
                sb.append(csign2);
                sb.append($18);
            }
            sig = new char[sb.length()];
            rec = sb.length();
            if (rec != null) {
                sb.getChars(0, rec, sig, 0);
            }
            return sig;
        } else if (this.exp != 0) {
            $18 = this.ind == (byte) -1 ? 1 : 0;
            int mag = this.exp + cmant.length;
            if (mag < 1) {
                rec = new char[(($18 + 2) - this.exp)];
                if ($18 != 0) {
                    rec[0] = '-';
                }
                rec[$18] = '0';
                rec[$18 + 1] = '.';
                int $20 = -mag;
                i = $18 + 2;
                while ($20 > 0) {
                    rec[i] = '0';
                    $20--;
                    i++;
                }
                System.arraycopy(cmant, 0, rec, ($18 + 2) - mag, cmant.length);
                return rec;
            } else if (mag > cmant.length) {
                rec = new char[($18 + mag)];
                if ($18 != 0) {
                    rec[0] = '-';
                }
                System.arraycopy(cmant, 0, rec, $18, cmant.length);
                int $21 = mag - cmant.length;
                int i2 = cmant.length + $18;
                while ($21 > 0) {
                    rec[i2] = '0';
                    $21--;
                    i2++;
                }
                return rec;
            } else {
                rec = new char[(($18 + 1) + cmant.length)];
                if ($18 != 0) {
                    rec[0] = '-';
                }
                System.arraycopy(cmant, 0, rec, $18, mag);
                rec[$18 + mag] = '.';
                System.arraycopy(cmant, mag, rec, ($18 + mag) + 1, cmant.length - mag);
                return rec;
            }
        } else if (this.ind >= (byte) 0) {
            return cmant;
        } else {
            rec = new char[(cmant.length + 1)];
            rec[0] = '-';
            System.arraycopy(cmant, 0, rec, 1, cmant.length);
            return rec;
        }
    }

    private int intcheck(int min, int max) {
        int i = intValueExact();
        int i2 = 0;
        int i3 = i < min ? 1 : 0;
        if (i > max) {
            i2 = 1;
        }
        if ((i2 | i3) == 0) {
            return i;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Conversion overflow: ");
        stringBuilder.append(i);
        throw new ArithmeticException(stringBuilder.toString());
    }

    private BigDecimal dodivide(char code, BigDecimal rhs, MathContext set, int scale) {
        int i;
        int i2;
        char c = code;
        byte reqdig = rhs;
        MathContext mathContext = set;
        byte scale2 = scale;
        int d = 0;
        byte[] newvar1 = null;
        byte lasthave = (byte) 0;
        int actdig = 0;
        byte[] newmant = null;
        int thisdigit = 0;
        if (mathContext.lostDigits != 0) {
            i = 0;
            i2 = this;
            i2.checkdigits(reqdig, mathContext.digits);
        } else {
            i = 0;
            i2 = this;
        }
        BigDecimal lhs = i2;
        byte v2;
        int ba;
        int mult;
        int start;
        int padding;
        int d2;
        byte[] newvar12;
        byte lasthave2;
        int actdig2;
        byte[] newmant2;
        if (reqdig.ind != (byte) 0) {
            v2 = (byte) 0;
            if (lhs.ind != (byte) 0) {
                byte v22;
                int reqdig2;
                int scale3;
                int reqdig3;
                BigDecimal lhs2 = mathContext.digits;
                if (lhs2 > null) {
                    if (lhs.mant.length > lhs2) {
                        lhs = clone(lhs).round(mathContext);
                    }
                    if (reqdig.mant.length > lhs2) {
                        reqdig = clone(rhs).round(mathContext);
                    }
                    v22 = reqdig;
                    reqdig2 = lhs2;
                } else {
                    if (scale2 == (byte) -1) {
                        scale2 = lhs.scale();
                    }
                    i2 = lhs.mant.length;
                    if (scale2 != (-lhs.exp)) {
                        scale3 = scale2;
                        i2 = (i2 + scale2) + lhs.exp;
                    } else {
                        scale3 = scale2;
                    }
                    reqdig3 = (i2 - (reqdig.mant.length - (byte) 1)) - reqdig.exp;
                    if (reqdig3 < lhs.mant.length) {
                        reqdig3 = lhs.mant.length;
                    }
                    lhs2 = reqdig3;
                    if (lhs2 < reqdig.mant.length) {
                        lhs2 = reqdig.mant.length;
                    }
                    v22 = reqdig;
                    reqdig2 = lhs2;
                    reqdig3 = scale3;
                }
                lhs2 = lhs;
                ba = 0;
                int newexp = ((lhs2.exp - v22.exp) + lhs2.mant.length) - v22.mant.length;
                if (newexp >= 0 || c == 'D') {
                    BigDecimal lhs3;
                    mult = 0;
                    lhs = new BigDecimal();
                    start = 0;
                    lhs.ind = (byte) (lhs2.ind * v22.ind);
                    lhs.exp = newexp;
                    lhs.mant = new byte[(reqdig2 + 1)];
                    int newlen = (reqdig2 + reqdig2) + 1;
                    byte[] var1 = extend(lhs2.mant, newlen);
                    int var1len = newlen;
                    byte[] var2 = v22.mant;
                    scale3 = newlen;
                    int b2b = (var2[0] * 10) + 1;
                    byte[] var12 = var1;
                    if (var2.length > 1) {
                        b2b += var2[1];
                    }
                    newlen = b2b;
                    padding = 0;
                    int padding2 = scale3;
                    int have = 0;
                    loop0:
                    while (true) {
                        byte[] var22;
                        d2 = d;
                        newvar12 = newvar1;
                        lasthave2 = lasthave;
                        lasthave = (byte) 0;
                        d = var1len;
                        newvar1 = var12;
                        while (d >= padding2) {
                            if (d == padding2) {
                                thisdigit = d;
                                actdig2 = actdig;
                                actdig = 0;
                                while (thisdigit > 0) {
                                    byte v23;
                                    newmant2 = newmant;
                                    if (actdig < var2.length) {
                                        v23 = var2[actdig];
                                    } else {
                                        v23 = (byte) 0;
                                    }
                                    var22 = var2;
                                    if (newvar1[actdig] < v23) {
                                        i = actdig;
                                        v2 = v23;
                                        break;
                                    } else if (newvar1[actdig] > v23) {
                                        lhs3 = lhs2;
                                        i = actdig;
                                        v2 = v23;
                                        ba = newvar1[0];
                                    } else {
                                        thisdigit--;
                                        actdig++;
                                        v2 = v23;
                                        newmant = newmant2;
                                        var2 = var22;
                                    }
                                }
                                newmant2 = newmant;
                                lhs.mant[have] = (byte) (lasthave + 1);
                                have++;
                                newvar1[0] = (byte) 0;
                                i = actdig;
                                break loop0;
                            }
                            lhs3 = lhs2;
                            var22 = var2;
                            actdig2 = actdig;
                            newmant2 = newmant;
                            actdig = newvar1[null] * 10;
                            if (d > 1) {
                                actdig += newvar1[1];
                            }
                            ba = actdig;
                            i2 = (ba * 10) / newlen;
                            if (i2 == 0) {
                                i2 = 1;
                            }
                            lasthave += i2;
                            newvar1 = byteaddsub(newvar1, d, var22, padding2, -i2, true);
                            if (newvar1[0] != (byte) 0) {
                                mult = i2;
                            } else {
                                actdig = d - 2;
                                newmant = null;
                                while (newmant <= actdig && newvar1[newmant] == 0) {
                                    d--;
                                    newmant++;
                                }
                                if (newmant != null) {
                                    System.arraycopy(newvar1, newmant, newvar1, 0, d);
                                }
                                mult = i2;
                                byte[] bArr = newmant;
                            }
                            actdig = actdig2;
                            newmant = newmant2;
                            var2 = var22;
                            lhs2 = lhs3;
                            c = code;
                        }
                        var22 = var2;
                        actdig2 = actdig;
                        newmant2 = newmant;
                        if (((have != 0 ? 1 : 0) | (lasthave != (byte) 0 ? 1 : 0)) != 0) {
                            lhs.mant[have] = (byte) lasthave;
                            have++;
                            if (have != reqdig2 + 1) {
                                if (newvar1[0] == (byte) 0) {
                                    break;
                                }
                            }
                            break;
                        }
                        if ((reqdig3 >= 0 && (-lhs.exp) > reqdig3) || (c != 'D' && lhs.exp <= 0)) {
                            break;
                        }
                        lhs.exp--;
                        padding2--;
                        var1len = d;
                        var12 = newvar1;
                        byte b = lasthave;
                        d = d2;
                        newvar1 = newvar12;
                        lasthave = lasthave2;
                        actdig = actdig2;
                        newmant = newmant2;
                        var2 = var22;
                    }
                    if (have == 0) {
                        have = 1;
                    }
                    if (((c == 'I' ? 1 : 0) | (c == 'R' ? 1 : 0)) == 0) {
                        if (newvar1[null] != (byte) 0) {
                            byte lasthave3 = lhs.mant[have - 1];
                            if (lasthave3 % 5 == 0) {
                                lhs.mant[have - 1] = (byte) (lasthave3 + 1);
                            }
                            lasthave2 = lasthave3;
                        }
                    } else if (lhs.exp + have > reqdig2) {
                        throw new ArithmeticException("Integer overflow");
                    } else if (c != 'R') {
                    } else if (lhs.mant[0] == (byte) 0) {
                        return clone(lhs2).finish(mathContext, false);
                    } else {
                        if (newvar1[0] == (byte) 0) {
                            return ZERO;
                        }
                        int padding3;
                        boolean z;
                        lhs.ind = lhs2.ind;
                        newexp = ((reqdig2 + reqdig2) + 1) - lhs2.mant.length;
                        lhs.exp = (lhs.exp - newexp) + lhs2.exp;
                        actdig = d;
                        int i3 = actdig - 1;
                        while (i3 >= 1) {
                            padding3 = newexp;
                            lhs3 = lhs2;
                            if (((lhs.exp < lhs2.exp ? 1 : 0) & (lhs.exp < v22.exp ? 1 : 0)) == 0 || newvar1[i3] != (byte) 0) {
                                break;
                            }
                            actdig--;
                            lhs.exp++;
                            i3--;
                            newexp = padding3;
                            lhs2 = lhs3;
                            c = code;
                        }
                        padding3 = newexp;
                        byte[] newvar13;
                        if (actdig < newvar1.length) {
                            newvar13 = new byte[actdig];
                            z = false;
                            System.arraycopy(newvar1, 0, newvar13, 0, actdig);
                            newvar1 = newvar13;
                        } else {
                            z = false;
                            newvar13 = newvar12;
                        }
                        lhs.mant = newvar1;
                        return lhs.finish(mathContext, z);
                    }
                    if (reqdig3 >= 0) {
                        if (have != lhs.mant.length) {
                            lhs.exp -= lhs.mant.length - have;
                        }
                        lhs.round(lhs.mant.length - ((-lhs.exp) - reqdig3), mathContext.roundingMode);
                        if (lhs.exp != (-reqdig3)) {
                            actdig = 1;
                            lhs.mant = extend(lhs.mant, lhs.mant.length + 1);
                            lhs.exp--;
                        } else {
                            actdig = 1;
                        }
                        return lhs.finish(mathContext, actdig);
                    }
                    if (have == lhs.mant.length) {
                        lhs.round(mathContext);
                        have = reqdig2;
                    } else if (lhs.mant[0] == (byte) 0) {
                        return ZERO;
                    } else {
                        newmant = new byte[have];
                        System.arraycopy(lhs.mant, 0, newmant, 0, have);
                        lhs.mant = newmant;
                        newmant2 = newmant;
                    }
                    return lhs.finish(mathContext, true);
                } else if (c == 'I') {
                    return ZERO;
                } else {
                    mult = 0;
                    return clone(lhs2).finish(mathContext, 0);
                }
            } else if (mathContext.form != 0) {
                return ZERO;
            } else {
                if (scale2 == (byte) -1) {
                    return lhs;
                }
                return lhs.setScale(scale2);
            }
        }
        v2 = (byte) 0;
        ba = 0;
        mult = 0;
        start = 0;
        padding = 0;
        d2 = 0;
        newvar12 = null;
        lasthave2 = (byte) 0;
        actdig2 = 0;
        newmant2 = null;
        throw new ArithmeticException("Divide by 0");
    }

    private void bad(char[] s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a number: ");
        stringBuilder.append(String.valueOf(s));
        throw new NumberFormatException(stringBuilder.toString());
    }

    private void badarg(String name, int pos, String value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad argument ");
        stringBuilder.append(pos);
        stringBuilder.append(" to ");
        stringBuilder.append(name);
        stringBuilder.append(PluralRules.KEYWORD_RULE_SEPARATOR);
        stringBuilder.append(value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static final byte[] extend(byte[] inarr, int newlen) {
        if (inarr.length == newlen) {
            return inarr;
        }
        byte[] newarr = new byte[newlen];
        System.arraycopy(inarr, 0, newarr, 0, inarr.length);
        return newarr;
    }

    private static final byte[] byteaddsub(byte[] a, int avlen, byte[] b, int bvlen, int m, boolean reuse) {
        byte[] bArr = a;
        byte[] bArr2 = b;
        int i = m;
        int dp90 = 0;
        int alength = bArr.length;
        int blength = bArr2.length;
        int ap = avlen - 1;
        int bp = bvlen - 1;
        int maxarr = bp;
        if (maxarr < ap) {
            maxarr = ap;
        }
        byte[] reb = null;
        if (reuse && maxarr + 1 == alength) {
            reb = bArr;
        }
        if (reb == null) {
            reb = new byte[(maxarr + 1)];
        }
        boolean quickm = false;
        int op = 0;
        if (i == 1) {
            quickm = true;
        } else if (i == -1) {
            quickm = true;
        }
        int digit = 0;
        op = maxarr;
        while (true) {
            int dp902 = dp90;
            if (op < 0) {
                break;
            }
            if (ap >= 0) {
                if (ap < alength) {
                    digit += bArr[ap];
                }
                ap--;
            }
            if (bp >= 0) {
                if (bp < blength) {
                    if (!quickm) {
                        digit += bArr2[bp] * i;
                    } else if (i > 0) {
                        digit += bArr2[bp];
                    } else {
                        digit -= bArr2[bp];
                    }
                }
                bp--;
            }
            if (digit >= 10 || digit < 0) {
                dp90 = digit + 90;
                reb[op] = bytedig[dp90];
                digit = bytecar[dp90];
            } else {
                reb[op] = (byte) digit;
                digit = 0;
                dp90 = dp902;
            }
            op--;
        }
        if (digit == 0) {
            return reb;
        }
        byte[] newarr = null;
        if (reuse && maxarr + 2 == bArr.length) {
            newarr = bArr;
        }
        if (newarr == null) {
            bArr2 = new byte[(maxarr + 2)];
        } else {
            bArr2 = newarr;
        }
        bArr2[0] = (byte) digit;
        if (maxarr < 10) {
            int $24 = maxarr + 1;
            int i2 = 0;
            while ($24 > 0) {
                bArr2[i2 + 1] = reb[i2];
                $24--;
                i2++;
            }
        } else {
            System.arraycopy(reb, 0, bArr2, 1, maxarr + 1);
        }
        return bArr2;
    }

    private static final byte[] diginit() {
        byte[] work = new byte[190];
        for (int op = 0; op <= 189; op++) {
            int digit = op - 90;
            if (digit >= 0) {
                work[op] = (byte) (digit % 10);
                bytecar[op] = (byte) (digit / 10);
            } else {
                digit += 100;
                work[op] = (byte) (digit % 10);
                bytecar[op] = (byte) ((digit / 10) - 10);
            }
        }
        return work;
    }

    private static final BigDecimal clone(BigDecimal dec) {
        BigDecimal copy = new BigDecimal();
        copy.ind = dec.ind;
        copy.exp = dec.exp;
        copy.form = dec.form;
        copy.mant = dec.mant;
        return copy;
    }

    private void checkdigits(BigDecimal rhs, int dig) {
        if (dig != 0) {
            StringBuilder stringBuilder;
            if (this.mant.length > dig && !allzero(this.mant, dig)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Too many digits: ");
                stringBuilder.append(toString());
                throw new ArithmeticException(stringBuilder.toString());
            } else if (rhs != null && rhs.mant.length > dig && !allzero(rhs.mant, dig)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Too many digits: ");
                stringBuilder.append(rhs.toString());
                throw new ArithmeticException(stringBuilder.toString());
            }
        }
    }

    private BigDecimal round(MathContext set) {
        return round(set.digits, set.roundingMode);
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x00fc  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00fb A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private BigDecimal round(int len, int mode) {
        int i = len;
        int i2 = mode;
        int adjust = this.mant.length - i;
        if (adjust <= 0) {
            return this;
        }
        boolean reuse;
        byte first;
        this.exp += adjust;
        int sign = this.ind;
        byte[] oldmant = this.mant;
        if (i > 0) {
            this.mant = new byte[i];
            System.arraycopy(oldmant, 0, this.mant, 0, i);
            reuse = true;
            first = oldmant[i];
        } else {
            this.mant = ZERO.mant;
            this.ind = (byte) 0;
            reuse = false;
            if (i == 0) {
                first = oldmant[0];
            } else {
                first = (byte) 0;
            }
        }
        int increment = 0;
        if (i2 == 4) {
            if (first >= (byte) 5) {
                increment = sign;
            }
        } else if (i2 == 7) {
            if (!allzero(oldmant, i)) {
                throw new ArithmeticException("Rounding necessary");
            }
        } else if (i2 == 5) {
            if (first > (byte) 5) {
                increment = sign;
            } else if (first == (byte) 5 && !allzero(oldmant, i + 1)) {
                increment = sign;
            }
        } else if (i2 == 6) {
            if (first > (byte) 5) {
                increment = sign;
            } else if (first == (byte) 5) {
                if (!allzero(oldmant, i + 1)) {
                    increment = sign;
                } else if (this.mant[this.mant.length - 1] % 2 != 0) {
                    increment = sign;
                }
            }
        } else if (i2 != 1) {
            if (i2 == 0) {
                if (!allzero(oldmant, i)) {
                    increment = sign;
                }
            } else if (i2 == 2) {
                if (sign > 0 && !allzero(oldmant, i)) {
                    increment = sign;
                }
            } else if (i2 != 3) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad round value: ");
                stringBuilder.append(i2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (sign < 0 && !allzero(oldmant, i)) {
                increment = sign;
            }
        }
        if (increment != 0) {
            if (this.ind == (byte) 0) {
                this.mant = ONE.mant;
                this.ind = (byte) increment;
            } else {
                if (this.ind == (byte) -1) {
                    increment = -increment;
                }
                int i3 = 1;
                byte[] newmant = byteaddsub(this.mant, this.mant.length, ONE.mant, 1, increment, reuse);
                if (newmant.length > this.mant.length) {
                    this.exp++;
                    System.arraycopy(newmant, 0, this.mant, 0, this.mant.length);
                } else {
                    this.mant = newmant;
                }
                if (this.exp > 999999999) {
                    return this;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exponent Overflow: ");
                stringBuilder2.append(this.exp);
                throw new ArithmeticException(stringBuilder2.toString());
            }
        }
        if (this.exp > 999999999) {
        }
    }

    private static final boolean allzero(byte[] array, int start) {
        if (start < 0) {
            start = 0;
        }
        int $25 = array.length - 1;
        for (int i = start; i <= $25; i++) {
            if (array[i] != (byte) 0) {
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:52:0x00a7, code skipped:
            if (r9 <= 999999999) goto L_0x00c1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private BigDecimal finish(MathContext set, boolean strip) {
        int i;
        byte[] newmant;
        if (set.digits != 0 && this.mant.length > set.digits) {
            round(set);
        }
        int i2 = 1;
        if (strip && set.form != 0) {
            int d = this.mant.length;
            i = d - 1;
            while (i >= 1 && this.mant[i] == (byte) 0) {
                d--;
                this.exp++;
                i--;
            }
            if (d < this.mant.length) {
                newmant = new byte[d];
                System.arraycopy(this.mant, 0, newmant, 0, d);
                this.mant = newmant;
            }
        }
        this.form = (byte) 0;
        int $26 = this.mant.length;
        i = 0;
        while ($26 > 0) {
            if (this.mant[i] != (byte) 0) {
                if (i > 0) {
                    newmant = new byte[(this.mant.length - i)];
                    System.arraycopy(this.mant, i, newmant, 0, this.mant.length - i);
                    this.mant = newmant;
                }
                int mag = this.exp + this.mant.length;
                if (mag > 0) {
                    if (mag > set.digits && set.digits != 0) {
                        this.form = (byte) set.form;
                    }
                    if (mag - 1 <= 999999999) {
                        return this;
                    }
                } else if (mag < -5) {
                    this.form = (byte) set.form;
                }
                mag--;
                int i3 = mag < -999999999 ? 1 : 0;
                if (mag <= 999999999) {
                    i2 = 0;
                }
                if ((i2 | i3) != 0) {
                    if (this.form == (byte) 2) {
                        int sig = mag % 3;
                        if (sig < 0) {
                            sig = 3 + sig;
                        }
                        mag -= sig;
                        if (mag >= -999999999) {
                        }
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exponent Overflow: ");
                    stringBuilder.append(mag);
                    throw new ArithmeticException(stringBuilder.toString());
                }
                return this;
            }
            $26--;
            i++;
        }
        this.ind = (byte) 0;
        if (set.form != 0) {
            this.exp = 0;
        } else if (this.exp > 0) {
            this.exp = 0;
        } else if (this.exp < -999999999) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exponent Overflow: ");
            stringBuilder2.append(this.exp);
            throw new ArithmeticException(stringBuilder2.toString());
        }
        this.mant = ZERO.mant;
        return this;
    }
}
