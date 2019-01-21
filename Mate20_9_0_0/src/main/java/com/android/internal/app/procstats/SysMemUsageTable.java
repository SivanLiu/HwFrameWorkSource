package com.android.internal.app.procstats;

import android.util.DebugUtils;
import com.android.internal.app.procstats.SparseMappingTable.Table;
import java.io.PrintWriter;

public class SysMemUsageTable extends Table {
    public SysMemUsageTable(SparseMappingTable tableData) {
        super(tableData);
    }

    public void mergeStats(SysMemUsageTable that) {
        int N = that.getKeyCount();
        for (int i = 0; i < N; i++) {
            int key = that.getKeyAt(i);
            mergeStats(SparseMappingTable.getIdFromKey(key), that.getArrayForKey(key), SparseMappingTable.getIndexFromKey(key));
        }
    }

    public void mergeStats(int state, long[] addData, int addOff) {
        int key = getOrAddKey((byte) state, 16);
        mergeSysMemUsage(getArrayForKey(key), SparseMappingTable.getIndexFromKey(key), addData, addOff);
    }

    public long[] getTotalMemUsage() {
        long[] total = new long[16];
        int N = getKeyCount();
        for (int i = 0; i < N; i++) {
            int key = getKeyAt(i);
            mergeSysMemUsage(total, 0, getArrayForKey(key), SparseMappingTable.getIndexFromKey(key));
        }
        return total;
    }

    public static void mergeSysMemUsage(long[] dstData, int dstOff, long[] addData, int addOff) {
        long dstCount = dstData[dstOff + 0];
        long addCount = addData[addOff + 0];
        int i = 16;
        int i2 = 1;
        int i3;
        if (dstCount == 0) {
            dstData[dstOff + 0] = addCount;
            while (true) {
                i3 = i2;
                if (i3 < 16) {
                    dstData[dstOff + i3] = addData[addOff + i3];
                    i2 = i3 + 1;
                } else {
                    return;
                }
            }
        } else if (addCount > 0) {
            dstData[dstOff + 0] = dstCount + addCount;
            i3 = 1;
            while (i3 < i) {
                if (dstData[dstOff + i3] > addData[addOff + i3]) {
                    dstData[dstOff + i3] = addData[addOff + i3];
                }
                dstData[(dstOff + i3) + i2] = (long) (((((double) dstData[(dstOff + i3) + i2]) * ((double) dstCount)) + (((double) addData[(addOff + i3) + 1]) * ((double) addCount))) / ((double) (dstCount + addCount)));
                if (dstData[(dstOff + i3) + 2] < addData[(addOff + i3) + 2]) {
                    dstData[(dstOff + i3) + 2] = addData[(addOff + i3) + 2];
                }
                i3 += 3;
                i = 16;
                i2 = 1;
            }
        }
    }

    public void dump(PrintWriter pw, String prefix, int[] screenStates, int[] memStates) {
        PrintWriter printWriter = pw;
        int[] iArr = screenStates;
        int[] iArr2 = memStates;
        int printedScreen = -1;
        int is = 0;
        while (true) {
            int is2 = is;
            if (is2 < iArr.length) {
                int printedScreen2 = printedScreen;
                printedScreen = -1;
                is = 0;
                while (true) {
                    int im = is;
                    if (im >= iArr2.length) {
                        break;
                    }
                    int iscreen = iArr[is2];
                    int imem = iArr2[im];
                    int bucket = (iscreen + imem) * 14;
                    long count = getValueForId((byte) bucket, 0);
                    if (count > 0) {
                        int printedScreen3;
                        pw.print(prefix);
                        if (iArr.length > 1) {
                            DumpUtils.printScreenLabel(printWriter, printedScreen2 != iscreen ? iscreen : -1);
                            printedScreen3 = iscreen;
                        } else {
                            printedScreen3 = printedScreen2;
                        }
                        if (iArr2.length > 1) {
                            DumpUtils.printMemLabel(printWriter, printedScreen != imem ? imem : -1, 0);
                            printedScreen = imem;
                        }
                        int printedMem = printedScreen;
                        printWriter.print(": ");
                        printWriter.print(count);
                        printWriter.println(" samples:");
                        PrintWriter printWriter2 = printWriter;
                        String str = prefix;
                        int i = bucket;
                        dumpCategory(printWriter2, str, "  Cached", i, 1);
                        dumpCategory(printWriter2, str, "  Free", i, 4);
                        dumpCategory(printWriter2, str, "  ZRam", i, 7);
                        dumpCategory(printWriter2, str, "  Kernel", i, 10);
                        dumpCategory(printWriter2, str, "  Native", i, 13);
                        printedScreen2 = printedScreen3;
                        printedScreen = printedMem;
                    }
                    is = im + 1;
                }
                is = is2 + 1;
                printedScreen = printedScreen2;
            } else {
                return;
            }
        }
    }

    private void dumpCategory(PrintWriter pw, String prefix, String label, int bucket, int index) {
        pw.print(prefix);
        pw.print(label);
        pw.print(": ");
        DebugUtils.printSizeValue(pw, getValueForId((byte) bucket, index) * 1024);
        pw.print(" min, ");
        DebugUtils.printSizeValue(pw, getValueForId((byte) bucket, index + 1) * 1024);
        pw.print(" avg, ");
        DebugUtils.printSizeValue(pw, getValueForId((byte) bucket, index + 2) * 1024);
        pw.println(" max");
    }
}
