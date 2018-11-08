package tmsdk.common.utils;

import android.content.Context;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.TccCryptor;
import tmsdkobf.dz;
import tmsdkobf.ea;
import tmsdkobf.fn;
import tmsdkobf.ls;

public class d extends c {
    public ArrayList<dz> LD = null;
    Context mContext;

    public d(Context context, String str) {
        super(context, str);
        this.mContext = context;
    }

    private ea v(ArrayList<dz> arrayList) {
        Object -l_2_R = new ea();
        -l_2_R.iC = arrayList;
        return -l_2_R;
    }

    public boolean a(String str, String str2, ls lsVar, ArrayList<dz> arrayList) {
        Object -l_7_R;
        Object -l_11_R;
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream -l_6_R = new FileOutputStream(new File(str));
            if (lsVar != null) {
                -l_6_R.write(lsVar.eD());
            }
            try {
                -l_7_R = v(arrayList);
                Object -l_8_R = new fn();
                -l_8_R.B("UTF-8");
                -l_8_R.put(str2, -l_7_R);
                -l_6_R.write(TccCryptor.encrypt(-l_8_R.l(), null));
                -l_6_R.flush();
                -l_6_R.close();
                if (-l_6_R != null) {
                    try {
                        -l_6_R.close();
                    } catch (Object -l_10_R) {
                        -l_10_R.printStackTrace();
                    }
                }
                return true;
            } catch (FileNotFoundException e) {
                -l_7_R = e;
                fileOutputStream = -l_6_R;
                try {
                    -l_7_R.printStackTrace();
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Object -l_7_R2) {
                            -l_7_R2.printStackTrace();
                        }
                    }
                    return false;
                } catch (Throwable th) {
                    -l_11_R = th;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Object -l_12_R) {
                            -l_12_R.printStackTrace();
                        }
                    }
                    throw -l_11_R;
                }
            } catch (IOException e2) {
                -l_7_R2 = e2;
                fileOutputStream = -l_6_R;
                -l_7_R2.printStackTrace();
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Object -l_7_R22) {
                        -l_7_R22.printStackTrace();
                    }
                }
                return false;
            } catch (Throwable th2) {
                -l_11_R = th2;
                fileOutputStream = -l_6_R;
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw -l_11_R;
            }
        } catch (FileNotFoundException e3) {
            -l_7_R22 = e3;
            -l_7_R22.printStackTrace();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return false;
        } catch (IOException e4) {
            -l_7_R22 = e4;
            -l_7_R22.printStackTrace();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return false;
        }
    }

    public boolean h(String str, boolean z) {
        ea -l_3_R = null;
        try {
            -l_3_R = (ea) a(TMSDKContext.getApplicaionContext(), str, "UTF-8", z).a(this.LA, new ea());
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
        }
        if (-l_3_R == null || -l_3_R.iC == null) {
            return false;
        }
        this.LD = -l_3_R.iC;
        return true;
    }
}
