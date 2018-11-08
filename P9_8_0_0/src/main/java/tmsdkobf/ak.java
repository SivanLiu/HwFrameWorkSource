package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ak extends JceStruct {
    static ArrayList<Map<Integer, String>> bs = new ArrayList();
    public ArrayList<Map<Integer, String>> br = null;

    static {
        Object -l_0_R = new HashMap();
        -l_0_R.put(Integer.valueOf(0), "");
        bs.add(-l_0_R);
    }

    public JceStruct newInit() {
        return new ak();
    }

    public void readFrom(JceInputStream jceInputStream) {
        this.br = (ArrayList) jceInputStream.read(bs, 0, true);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.br, 0);
    }
}
