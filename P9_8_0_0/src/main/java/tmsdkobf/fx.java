package tmsdkobf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.net.URISyntaxException;
import java.util.HashSet;

public final class fx {
    private String[] mArgs;
    private int nA;
    private String nB;
    private String ny = null;
    private Intent nz = null;

    public fx(String str) {
        this.mArgs = str.split(" ");
        if (this.mArgs != null && this.mArgs.length >= 1) {
            this.nA = 1;
            this.nB = null;
            return;
        }
        throw new IllegalArgumentException("Illegal argument: " + str);
    }

    private String K() {
        if (this.nB != null) {
            throw new IllegalArgumentException("No argument expected after \"" + this.mArgs[this.nA - 1] + "\"");
        } else if (this.nA >= this.mArgs.length) {
            return null;
        } else {
            Object -l_1_R = this.mArgs[this.nA];
            if (!-l_1_R.startsWith("-")) {
                return null;
            }
            this.nA++;
            if (-l_1_R.equals("--")) {
                return null;
            }
            if (-l_1_R.length() <= 1 || -l_1_R.charAt(1) == '-') {
                this.nB = null;
                return -l_1_R;
            } else if (-l_1_R.length() <= 2) {
                this.nB = null;
                return -l_1_R;
            } else {
                this.nB = -l_1_R.substring(2);
                return -l_1_R.substring(0, 2);
            }
        }
    }

    private String L() {
        if (this.nB != null) {
            Object -l_1_R = this.nB;
            this.nB = null;
            return -l_1_R;
        } else if (this.nA >= this.mArgs.length) {
            return null;
        } else {
            String[] strArr = this.mArgs;
            int i = this.nA;
            this.nA = i + 1;
            return strArr[i];
        }
    }

    private String M() {
        Object -l_1_R = L();
        if (-l_1_R != null) {
            return -l_1_R;
        }
        throw new IllegalArgumentException("Argument expected after \"" + this.mArgs[this.nA - 1] + "\"");
    }

    private void N() {
        if (this.mArgs != null && this.mArgs.length > 0) {
            try {
                this.ny = M();
                this.nz = O();
            } catch (Exception e) {
                this.ny = null;
                this.nz = null;
            }
        }
        this.mArgs = null;
    }

    private Intent O() throws URISyntaxException {
        Object -l_6_R;
        Object -l_1_R = new Intent();
        int -l_2_I = 0;
        Uri -l_3_R = null;
        String str = null;
        while (true) {
            Object -l_5_R = K();
            if (-l_5_R == null) {
                break;
            }
            Object -l_8_R;
            Object -l_9_R;
            Object -l_7_R;
            if (-l_5_R.equals("-a")) {
                -l_1_R.setAction(M());
            } else if (-l_5_R.equals("-d")) {
                -l_3_R = Uri.parse(M());
                -l_2_I = 1;
            } else if (-l_5_R.equals("-t")) {
                str = M();
                -l_2_I = 1;
            } else if (-l_5_R.equals("-c")) {
                -l_1_R.addCategory(M());
            } else if (-l_5_R.equals("-e") || -l_5_R.equals("--es")) {
                -l_1_R.putExtra(M(), M());
            } else if (-l_5_R.equals("--esn")) {
                -l_1_R.putExtra(M(), (String) null);
            } else if (-l_5_R.equals("--ei")) {
                -l_1_R.putExtra(M(), Integer.valueOf(M()));
            } else if (-l_5_R.equals("--eu")) {
                -l_1_R.putExtra(M(), Uri.parse(M()));
            } else if (-l_5_R.equals("--eia")) {
                -l_6_R = M();
                -l_8_R = M().split(",");
                -l_9_R = new int[-l_8_R.length];
                for (-l_10_I = 0; -l_10_I < -l_8_R.length; -l_10_I++) {
                    -l_9_R[-l_10_I] = Integer.valueOf(-l_8_R[-l_10_I]).intValue();
                }
                -l_1_R.putExtra(-l_6_R, -l_9_R);
            } else if (-l_5_R.equals("--el")) {
                -l_1_R.putExtra(M(), Long.valueOf(M()));
            } else if (-l_5_R.equals("--ela")) {
                -l_6_R = M();
                -l_8_R = M().split(",");
                -l_9_R = new long[-l_8_R.length];
                for (-l_10_I = 0; -l_10_I < -l_8_R.length; -l_10_I++) {
                    -l_9_R[-l_10_I] = Long.valueOf(-l_8_R[-l_10_I]).longValue();
                }
                -l_1_R.putExtra(-l_6_R, -l_9_R);
            } else if (-l_5_R.equals("--ez")) {
                -l_1_R.putExtra(M(), Boolean.valueOf(M()));
            } else if (-l_5_R.equals("-n")) {
                -l_6_R = M();
                -l_7_R = ComponentName.unflattenFromString(-l_6_R);
                if (-l_7_R != null) {
                    -l_1_R.setComponent(-l_7_R);
                } else {
                    throw new IllegalArgumentException("Bad component name: " + -l_6_R);
                }
            } else if (-l_5_R.equals("-f")) {
                -l_1_R.setFlags(Integer.decode(M()).intValue());
            } else if (-l_5_R.equals("-p")) {
                -l_1_R.setPackage(M());
            } else if (!-l_5_R.equals("--exclude-stopped-packages")) {
                return null;
            } else {
                -l_1_R.addFlags(16);
            }
            -l_2_I = 1;
        }
        -l_1_R.setDataAndType(-l_3_R, str);
        -l_6_R = L();
        if (-l_6_R != null) {
            if (-l_6_R.indexOf(58) >= 0) {
                -l_7_R = Intent.parseUri(-l_6_R, 1);
                -l_1_R.addCategory("android.intent.category.BROWSABLE");
                -l_1_R.setComponent(null);
                -l_1_R.setSelector(null);
            } else if (-l_6_R.indexOf(47) < 0) {
                -l_7_R = new Intent("android.intent.action.MAIN");
                -l_7_R.addCategory("android.intent.category.LAUNCHER");
                -l_7_R.setPackage(-l_6_R);
            } else {
                -l_7_R = new Intent("android.intent.action.MAIN");
                -l_7_R.addCategory("android.intent.category.LAUNCHER");
                -l_7_R.setComponent(ComponentName.unflattenFromString(-l_6_R));
            }
            -l_8_R = -l_1_R.getExtras();
            -l_1_R.replaceExtras((Bundle) null);
            -l_9_R = -l_7_R.getExtras();
            -l_7_R.replaceExtras((Bundle) null);
            if (!(-l_1_R.getAction() == null || -l_7_R.getCategories() == null)) {
                Object -l_11_R = new HashSet(-l_7_R.getCategories()).iterator();
                while (-l_11_R.hasNext()) {
                    -l_7_R.removeCategory((String) -l_11_R.next());
                }
            }
            -l_1_R.fillIn(-l_7_R, 8);
            if (-l_8_R != null) {
                if (-l_9_R != null) {
                    -l_9_R.putAll(-l_8_R);
                }
                -l_1_R.replaceExtras(-l_8_R);
                -l_2_I = 1;
            }
            -l_8_R = -l_9_R;
            -l_1_R.replaceExtras(-l_8_R);
            -l_2_I = 1;
        }
        if (-l_2_I != 0) {
            return -l_1_R;
        }
        throw new IllegalArgumentException("No intent supplied");
    }

    public String J() {
        N();
        return this.ny;
    }

    public boolean d(Context context) {
        try {
            Object -l_2_R = J();
            if (-l_2_R == null) {
                return false;
            }
            if (-l_2_R.equals("start")) {
                getIntent().addFlags(268435456);
                context.startActivity(getIntent());
                return true;
            } else if (-l_2_R.equals("startservice")) {
                context.startService(getIntent());
                return true;
            } else {
                if (-l_2_R.equals("broadcast")) {
                    context.sendBroadcast(getIntent());
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
        }
    }

    public Intent getIntent() {
        N();
        return this.nz;
    }
}
