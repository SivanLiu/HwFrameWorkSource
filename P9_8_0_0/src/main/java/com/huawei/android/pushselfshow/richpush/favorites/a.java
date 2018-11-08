package com.huawei.android.pushselfshow.richpush.favorites;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.a.d;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class a extends BaseAdapter {
    private Context a;
    private boolean b = true;
    private boolean c = false;
    private List d = new ArrayList();

    private static class a {
        ImageView a;
        TextView b;
        TextView c;
        CheckBox d;

        private a() {
        }
    }

    public a(Context context) {
        this.a = context;
    }

    public e a(int i) {
        return (e) this.d.get(i);
    }

    public List a() {
        return this.d;
    }

    public void a(int i, e eVar) {
        try {
            if (this.d.size() >= i) {
                this.d.set(i, eVar);
            }
        } catch (Object -l_3_R) {
            c.d("PushSelfShowLog", -l_3_R.toString());
        }
    }

    public void a(boolean z) {
        this.b = z;
        notifyDataSetChanged();
    }

    public void a(boolean z, Set set) {
        this.c = z;
        int -l_3_I = 0;
        for (e -l_5_R : this.d) {
            if (set != null && set.contains(Integer.valueOf(-l_3_I))) {
                -l_5_R.a(!z);
            } else {
                -l_5_R.a(z);
            }
            int -l_3_I2 = -l_3_I + 1;
            a(-l_3_I, -l_5_R);
            -l_3_I = -l_3_I2;
        }
        notifyDataSetChanged();
    }

    public void b() {
        this.d = d.a(this.a, null);
    }

    public int getCount() {
        return this.d.size();
    }

    public /* synthetic */ Object getItem(int i) {
        return a(i);
    }

    public long getItemId(int i) {
        return (long) i;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        Object -l_4_R;
        Object -l_4_R2;
        if (view != null) {
            try {
                -l_4_R = (a) view.getTag();
            } catch (Exception e) {
                -l_4_R2 = e;
                c.b("PushSelfShowLog", -l_4_R2.toString());
                -l_4_R = -l_4_R2;
                return view;
            }
        }
        -l_4_R2 = new a();
        try {
            view = ((LayoutInflater) this.a.getSystemService("layout_inflater")).inflate(com.huawei.android.pushselfshow.utils.d.c(this.a, "hwpush_collection_item"), null);
            -l_4_R2.a = (ImageView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.a, "hwpush_favicon"));
            -l_4_R2.b = (TextView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.a, "hwpush_selfshowmsg_title"));
            -l_4_R2.c = (TextView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.a, "hwpush_selfshowmsg_content"));
            -l_4_R2.d = (CheckBox) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.a, "hwpush_delCheck"));
            view.setTag(-l_4_R2);
            -l_4_R = -l_4_R2;
        } catch (Exception e2) {
            -l_4_R = -l_4_R2;
            Exception -l_4_R3 = e2;
            c.b("PushSelfShowLog", -l_4_R2.toString());
            -l_4_R = -l_4_R2;
            return view;
        }
        Object -l_5_R = ((e) this.d.get(i)).d();
        if (-l_5_R == null) {
            -l_5_R = BitmapFactory.decodeResource(this.a.getResources(), com.huawei.android.pushselfshow.utils.d.g(this.a, "hwpush_main_icon"));
        }
        -l_4_R.a.setBackgroundDrawable(new BitmapDrawable(this.a.getResources(), -l_5_R));
        Object -l_7_R = ((e) this.d.get(i)).b().p();
        if (-l_7_R != null && -l_7_R.length() > 0) {
            -l_4_R.b.setText(-l_7_R);
        }
        Object -l_8_R = ((e) this.d.get(i)).b().n();
        if (-l_8_R != null) {
            if (-l_8_R.length() > 0) {
                -l_4_R.c.setText(-l_8_R);
            }
        }
        if (this.b) {
            -l_4_R.d.setVisibility(4);
        } else {
            -l_4_R.d.setVisibility(0);
            if (this.c || ((e) this.d.get(i)).a()) {
                -l_4_R.d.setChecked(true);
            } else {
                -l_4_R.d.setChecked(false);
            }
        }
        return view;
    }
}
