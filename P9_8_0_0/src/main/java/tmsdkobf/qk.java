package tmsdkobf;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class qk {
    private static Map<String, Pattern> Nx = null;

    public static Pattern cT(String str) {
        if (Nx == null) {
            Nx = new HashMap();
        }
        Pattern -l_1_R = (Pattern) Nx.get(str);
        if (-l_1_R != null) {
            return -l_1_R;
        }
        Object -l_1_R2 = Pattern.compile(str);
        Nx.put(str, -l_1_R2);
        return -l_1_R2;
    }

    public static int jq() {
        if (Nx == null) {
            return 0;
        }
        int -l_0_I = Nx.size();
        Nx = null;
        return -l_0_I;
    }
}
