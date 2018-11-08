package tmsdkobf;

import android.os.Handler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import tmsdk.common.utils.f;
import tmsdk.fg.module.spacemanager.SpaceManager;
import tmsdkobf.ry.a;

public class sc {
    private AtomicBoolean JR = new AtomicBoolean();

    private List<ry> P(List<sa> list) {
        List -l_2_R = new ArrayList();
        for (int -l_3_I = list.size() - 1; -l_3_I >= 0; -l_3_I--) {
            if (this.JR.get()) {
                return null;
            }
            sa -l_4_R = (sa) list.get(-l_3_I);
            if (!-l_4_R.mIsScreenShot) {
                a(-l_2_R, new a(-l_4_R));
            }
        }
        return -l_2_R;
    }

    private long[] Q(List<ry> list) {
        int -l_2_I = 0;
        long -l_3_J = 0;
        for (ry -l_6_R : list) {
            -l_2_I += -l_6_R.mItemList.size();
            Object -l_7_R = -l_6_R.mItemList.iterator();
            while (-l_7_R.hasNext()) {
                -l_3_J += ((a) -l_7_R.next()).mSize;
            }
        }
        return new long[]{(long) -l_2_I, -l_3_J};
    }

    private List<ry> a(ry ryVar) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = x(ryVar.mItemList);
        if (this.JR.get() || -l_3_R == null) {
            return null;
        }
        for (int -l_4_I = 0; -l_4_I < -l_3_R.size(); -l_4_I++) {
            if (this.JR.get()) {
                return null;
            }
            ArrayList -l_5_R = (ArrayList) -l_3_R.get(-l_4_I);
            if (!(-l_5_R == null || -l_5_R.isEmpty())) {
                Object -l_6_R = new ry(ryVar);
                Object -l_7_R = -l_5_R.iterator();
                while (-l_7_R.hasNext()) {
                    -l_6_R.mItemList.add((a) ryVar.mItemList.get(((Integer) -l_7_R.next()).intValue()));
                }
                b(-l_6_R);
                -l_2_R.add(-l_6_R);
            }
        }
        return -l_2_R;
    }

    private void a(List<ry> list, a aVar) {
        for (ry -l_4_R : list) {
            Object obj;
            if (-l_4_R.mTime - aVar.mTime > 30000) {
                obj = 1;
                continue;
            } else {
                obj = null;
                continue;
            }
            if (obj == null) {
                -l_4_R.mItemList.add(aVar);
                return;
            }
        }
        Object -l_3_R = new ry(aVar.mTime);
        -l_3_R.mTime = aVar.mTime;
        -l_3_R.mItemList.add(aVar);
        list.add(-l_3_R);
    }

    private ArrayList<Integer> b(int i, ArrayList<ArrayList<Integer>> arrayList) {
        Object -l_3_R = arrayList.iterator();
        while (-l_3_R.hasNext()) {
            ArrayList -l_4_R = (ArrayList) -l_3_R.next();
            if (-l_4_R.contains(Integer.valueOf(i))) {
                return -l_4_R;
            }
        }
        -l_3_R = new ArrayList();
        arrayList.add(-l_3_R);
        return -l_3_R;
    }

    private void b(ry ryVar) {
        Object -l_2_R = ryVar.mItemList;
        for (int -l_3_I = 0; -l_3_I < -l_2_R.size(); -l_3_I++) {
            if (-l_3_I == 0) {
                ((a) -l_2_R.get(-l_3_I)).mSelected = false;
            } else {
                ((a) -l_2_R.get(-l_3_I)).mSelected = true;
            }
        }
    }

    private ArrayList<ArrayList<Integer>> x(ArrayList<a> arrayList) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = new HashSet();
        y(arrayList);
        int -l_4_I = 0;
        while (-l_4_I < arrayList.size()) {
            if (this.JR.get()) {
                return null;
            }
            Object -l_5_R = b(-l_4_I, -l_2_R);
            Object -l_6_R = ((a) arrayList.get(-l_4_I)).Qd;
            int -l_7_I = -l_4_I + 1;
            while (-l_7_I < arrayList.size()) {
                Object -l_8_R = ((a) arrayList.get(-l_7_I)).Qd;
                if (!(-l_6_R == null || -l_8_R == null || rx.a(-l_6_R, -l_8_R) < 75 || -l_3_R.contains(Integer.valueOf(-l_7_I)))) {
                    -l_3_R.add(Integer.valueOf(-l_7_I));
                    -l_5_R.add(Integer.valueOf(-l_7_I));
                }
                -l_7_I++;
            }
            if (!(-l_5_R.isEmpty() || -l_3_R.contains(Integer.valueOf(-l_4_I)))) {
                -l_3_R.add(Integer.valueOf(-l_4_I));
                -l_5_R.add(Integer.valueOf(-l_4_I));
            }
            -l_4_I++;
        }
        for (-l_4_I = 0; -l_4_I < arrayList.size(); -l_4_I++) {
            a -l_5_R2 = (a) arrayList.get(-l_4_I);
            if (-l_5_R2.Qd != null) {
                -l_5_R2.Qd.finish();
                -l_5_R2.Qd = null;
            }
        }
        return -l_2_R;
    }

    private void y(ArrayList<a> arrayList) {
        for (int -l_2_I = 0; -l_2_I < arrayList.size() && !this.JR.get(); -l_2_I++) {
            a -l_3_R = (a) arrayList.get(-l_2_I);
            -l_3_R.Qd = rx.a(-l_3_R);
        }
    }

    public List<ry> a(ArrayList<sa> arrayList, Handler handler) {
        this.JR.set(false);
        if (handler == null || arrayList == null) {
            f.e("SimilarPhotoProcesser", "startScan parameter is null");
            return null;
        }
        Object -l_3_R = new ArrayList();
        Object -l_4_R = P(arrayList);
        if (this.JR.get()) {
            handler.sendMessage(handler.obtainMessage(4356));
            return null;
        } else if (-l_4_R != null) {
            int -l_6_I = (int) Q(-l_4_R)[0];
            int -l_7_I = 0;
            for (int -l_8_I = 0; -l_8_I < -l_4_R.size(); -l_8_I++) {
                if (this.JR.get()) {
                    handler.sendMessage(handler.obtainMessage(4356));
                    return null;
                }
                ry -l_9_R = (ry) -l_4_R.get(-l_8_I);
                Object -l_10_R = a(-l_9_R);
                if (this.JR.get()) {
                    handler.sendMessage(handler.obtainMessage(4356));
                    return null;
                }
                Object -l_11_R;
                -l_7_I += -l_9_R.mItemList.size();
                -l_3_R.addAll(-l_10_R);
                if (-l_10_R.size() > 0) {
                    -l_11_R = handler.obtainMessage(4354);
                    -l_11_R.obj = ri.I(-l_10_R);
                    handler.sendMessage(-l_11_R);
                }
                -l_11_R = handler.obtainMessage(4355);
                -l_11_R.arg1 = (-l_7_I * 100) / -l_6_I;
                handler.sendMessage(-l_11_R);
            }
            Object -l_8_R = handler.obtainMessage(4357);
            -l_8_R.arg1 = 0;
            -l_8_R.obj = ri.I(-l_3_R);
            handler.sendMessage(-l_8_R);
            return -l_3_R;
        } else {
            f.e("SimilarPhotoProcesser", "startScan sort get null result");
            Object -l_5_R = handler.obtainMessage(4357);
            -l_5_R.arg1 = SpaceManager.ERROR_CODE_UNKNOW;
            -l_5_R.obj = null;
            handler.sendMessage(-l_5_R);
            return null;
        }
    }

    public void cancel() {
        this.JR.set(true);
    }
}
