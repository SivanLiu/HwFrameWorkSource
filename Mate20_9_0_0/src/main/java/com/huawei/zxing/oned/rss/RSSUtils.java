package com.huawei.zxing.oned.rss;

public final class RSSUtils {
    private RSSUtils() {
    }

    public static int getRSSvalue(int[] widths, int maxWidth, boolean noNarrow) {
        int width;
        int elements = widths.length;
        int bar = 0;
        int n = 0;
        for (int width2 : widths) {
            n += width2;
        }
        int n2 = 0;
        int narrowMask = 0;
        while (bar < elements - 1) {
            width2 = 1;
            narrowMask |= 1 << bar;
            while (width2 < widths[bar]) {
                int subVal = combins((n - width2) - 1, (elements - bar) - 2);
                if (noNarrow && narrowMask == 0 && (n - width2) - ((elements - bar) - 1) >= (elements - bar) - 1) {
                    subVal -= combins((n - width2) - (elements - bar), (elements - bar) - 2);
                }
                if ((elements - bar) - 1 > 1) {
                    int lessVal = 0;
                    for (int mxwElement = (n - width2) - ((elements - bar) - 2); mxwElement > maxWidth; mxwElement--) {
                        lessVal += combins(((n - width2) - mxwElement) - 1, (elements - bar) - 3);
                    }
                    subVal -= ((elements - 1) - bar) * lessVal;
                } else if (n - width2 > maxWidth) {
                    subVal--;
                }
                n2 += subVal;
                width2++;
                narrowMask &= ~(1 << bar);
            }
            n -= width2;
            bar++;
        }
        return n2;
    }

    private static int combins(int n, int r) {
        int minDenom;
        int maxDenom;
        if (n - r > r) {
            minDenom = r;
            maxDenom = n - r;
        } else {
            minDenom = n - r;
            maxDenom = r;
        }
        int j = 1;
        int val = 1;
        for (int i = n; i > maxDenom; i--) {
            val *= i;
            if (j <= minDenom) {
                val /= j;
                j++;
            }
        }
        while (j <= minDenom) {
            val /= j;
            j++;
        }
        return val;
    }
}
