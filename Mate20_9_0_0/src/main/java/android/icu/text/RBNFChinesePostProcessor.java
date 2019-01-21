package android.icu.text;

final class RBNFChinesePostProcessor implements RBNFPostProcessor {
    private static final String[] rulesetNames = new String[]{"%traditional", "%simplified", "%accounting", "%time"};
    private int format;
    private boolean longForm;

    RBNFChinesePostProcessor() {
    }

    public void init(RuleBasedNumberFormat formatter, String rules) {
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0030  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void process(StringBuilder buf, NFRuleSet ruleSet) {
        StringBuilder stringBuilder = buf;
        String name = ruleSet.getName();
        int i = 0;
        int i2 = 0;
        while (i2 < rulesetNames.length) {
            if (rulesetNames[i2].equals(name)) {
                this.format = i2;
                boolean z = i2 == 1 || i2 == 3;
                this.longForm = z;
                if (this.longForm) {
                    int n;
                    int i3;
                    String DIAN = "點";
                    String[][] markers = new String[][]{new String[]{"萬", "億", "兆", "〇"}, new String[]{"万", "亿", "兆", "〇"}, new String[]{"萬", "億", "兆", "零"}};
                    String[] m = markers[this.format];
                    while (i < m.length - 1) {
                        n = stringBuilder.indexOf(m[i]);
                        if (n != -1) {
                            stringBuilder.insert(m[i].length() + n, '|');
                        }
                        i++;
                    }
                    i = stringBuilder.indexOf("點");
                    if (i == -1) {
                        i = buf.length();
                    }
                    int s = 0;
                    n = -1;
                    String ling = markers[this.format][3];
                    while (i >= 0) {
                        int m2 = stringBuilder.lastIndexOf("|", i);
                        int nn = stringBuilder.lastIndexOf(ling, i);
                        int ns = 0;
                        if (nn > m2) {
                            i3 = (nn <= 0 || stringBuilder.charAt(nn - 1) == '*') ? 1 : 2;
                            ns = i3;
                        }
                        i = m2 - 1;
                        switch ((s * 3) + ns) {
                            case 0:
                                i3 = ns;
                                s = -1;
                                break;
                            case 1:
                                i3 = ns;
                                s = nn;
                                break;
                            case 2:
                                i3 = ns;
                                s = -1;
                                break;
                            case 3:
                                i3 = ns;
                                s = -1;
                                break;
                            case 4:
                                stringBuilder.delete(nn - 1, ling.length() + nn);
                                i3 = 0;
                                s = -1;
                                break;
                            case 5:
                                stringBuilder.delete(n - 1, ling.length() + n);
                                i3 = ns;
                                s = -1;
                                break;
                            case 6:
                                i3 = ns;
                                s = -1;
                                break;
                            case 7:
                                stringBuilder.delete(nn - 1, ling.length() + nn);
                                i3 = 0;
                                s = -1;
                                break;
                            case 8:
                                i3 = ns;
                                s = -1;
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        n = s;
                        s = i3;
                    }
                    i3 = buf.length();
                    while (true) {
                        i3--;
                        if (i3 >= 0) {
                            char c = stringBuilder.charAt(i3);
                            if (c == '*' || c == '|') {
                                stringBuilder.delete(i3, i3 + 1);
                            }
                        } else {
                            return;
                        }
                    }
                }
                for (i = stringBuilder.indexOf("*"); i != -1; i = stringBuilder.indexOf("*", i)) {
                    stringBuilder.delete(i, i + 1);
                }
                return;
            }
            i2++;
        }
        if (this.longForm) {
        }
    }
}
