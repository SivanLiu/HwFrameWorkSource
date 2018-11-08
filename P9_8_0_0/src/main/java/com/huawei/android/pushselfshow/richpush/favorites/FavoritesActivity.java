package com.huawei.android.pushselfshow.richpush.favorites;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.huawei.android.pushselfshow.richpush.RichPushHtmlActivity;
import com.huawei.android.pushselfshow.richpush.html.HtmlViewer;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.HashSet;
import java.util.Set;

public class FavoritesActivity implements com.huawei.android.pushselfshow.utils.c.a {
    com.huawei.android.pushselfshow.utils.c a = new com.huawei.android.pushselfshow.utils.c(this);
    private Activity b;
    private ImageView c;
    private TextView d;
    private TextView e;
    private ListView f;
    private LinearLayout g;
    private a h;
    private MenuItem i;
    private MenuItem j;
    private boolean k = false;
    private byte[] l = null;
    private byte[] m = null;
    private AlertDialog n = null;

    private class a implements OnClickListener {
        final /* synthetic */ FavoritesActivity a;
        private Context b;

        private a(FavoritesActivity favoritesActivity, Context context) {
            this.a = favoritesActivity;
            this.b = context;
        }

        public void onClick(View view) {
            if (this.a.k) {
                this.a.e();
                return;
            }
            Object -l_2_R = this.a.b.getActionBar();
            if (-l_2_R != null) {
                -l_2_R.setDisplayShowHomeEnabled(true);
                -l_2_R.setDisplayHomeAsUpEnabled(true);
                -l_2_R.setDisplayShowTitleEnabled(true);
                -l_2_R.setDisplayShowCustomEnabled(false);
                -l_2_R.setTitle(this.b.getString(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_msg_collect")));
            }
            this.a.c.setVisibility(4);
            this.a.e.setVisibility(8);
            this.a.e.setText("");
            this.a.d.setText(this.b.getString(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_msg_collect")));
            this.a.a(false);
            this.a.h.a(true);
            this.a.f.setOnItemClickListener(new d());
            this.a.f.setLongClickable(true);
        }
    }

    private class b implements OnItemClickListener {
        final /* synthetic */ FavoritesActivity a;
        private Context b;

        private b(FavoritesActivity favoritesActivity, Context context) {
            this.a = favoritesActivity;
            this.b = context;
        }

        public void onItemClick(AdapterView adapterView, View view, int i, long j) {
            CheckBox -l_6_R = (CheckBox) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_delCheck"));
            e -l_7_R = this.a.h.a(i);
            if (-l_6_R.isChecked()) {
                -l_6_R.setChecked(false);
                -l_7_R.a(false);
            } else {
                -l_6_R.setChecked(true);
                -l_7_R.a(true);
            }
            this.a.h.a(i, -l_7_R);
            int -l_8_I = 0;
            Object<e> -l_9_R = this.a.h.a();
            for (e -l_11_R : -l_9_R) {
                if (-l_11_R.a()) {
                    -l_8_I++;
                }
            }
            if (-l_8_I <= 0) {
                this.a.e.setVisibility(8);
                this.a.e.setText("");
                this.a.i.setEnabled(false);
                this.a.a(this.b, false);
                return;
            }
            this.a.e.setVisibility(0);
            this.a.e.setText(String.valueOf(-l_8_I));
            this.a.i.setEnabled(true);
            if (-l_8_I != -l_9_R.size()) {
                this.a.a(this.b, false);
            } else {
                this.a.a(this.b, true);
            }
        }
    }

    private class c implements OnItemLongClickListener {
        final /* synthetic */ FavoritesActivity a;

        private c(FavoritesActivity favoritesActivity) {
            this.a = favoritesActivity;
        }

        public boolean onItemLongClick(AdapterView adapterView, View view, int i, long j) {
            this.a.d();
            this.a.i.setEnabled(true);
            Set -l_6_R = new HashSet();
            -l_6_R.add(Integer.valueOf(i));
            this.a.h.a(false, -l_6_R);
            this.a.e.setVisibility(0);
            this.a.e.setText("1");
            return true;
        }
    }

    private class d implements OnItemClickListener {
        final /* synthetic */ FavoritesActivity a;

        private d(FavoritesActivity favoritesActivity) {
            this.a = favoritesActivity;
        }

        public void onItemClick(AdapterView adapterView, View view, int i, long j) {
            Object -l_6_R = this.a.h.a(i);
            Object -l_7_R = new Intent(this.a.b, RichPushHtmlActivity.class);
            -l_7_R.putExtra("type", -l_6_R.b().y());
            -l_7_R.putExtra("selfshow_info", -l_6_R.b().c());
            -l_7_R.putExtra("selfshow_token", -l_6_R.b().d());
            -l_7_R.putExtra("selfshow_from_list", true);
            -l_7_R.setFlags(268468240);
            -l_7_R.setPackage(this.a.b.getPackageName());
            this.a.b.finish();
            this.a.b.startActivity(-l_7_R);
        }
    }

    private View a() {
        Object -l_1_R = this.b.getLayoutInflater().inflate(com.huawei.android.pushselfshow.utils.d.c(this.b, "hwpush_collection_listview"), null);
        this.f = (ListView) -l_1_R.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_collection_list"));
        this.h = new a(this.b);
        this.f.setAdapter(this.h);
        this.f.setLongClickable(true);
        this.f.setOnItemLongClickListener(new c());
        this.f.setOnItemClickListener(new d());
        return -l_1_R;
    }

    private void a(Context context, boolean z) {
        if (z) {
            this.j.setTitle(com.huawei.android.pushselfshow.utils.d.a(context, "hwpush_unselectall"));
            Object -l_3_R = context.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(context, "hwpush_ic_toolbar_multiple1"));
            try {
                int -l_4_I = context.getResources().getIdentifier("colorful_emui", "color", "androidhwext");
                if (-l_4_I != 0) {
                    int -l_5_I = context.getResources().getColor(-l_4_I);
                    if (-l_5_I != 0) {
                        -l_3_R.setTint(-l_5_I);
                    }
                }
            } catch (Object -l_4_R) {
                com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_4_R.toString());
            } catch (Object -l_4_R2) {
                com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_4_R2.toString());
            }
            this.j.setIcon(-l_3_R);
            return;
        }
        this.j.setIcon(context.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(context, "hwpush_ic_toolbar_multiple")));
        this.j.setTitle(com.huawei.android.pushselfshow.utils.d.a(context, "hwpush_selectall"));
    }

    private void a(View view) {
        this.c = (ImageView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_bt_delete"));
        this.d = (TextView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_txt_delitem"));
        this.e = (TextView) view.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_txt_delnum"));
        com.huawei.android.pushselfshow.utils.a.a(this.b, this.d);
        com.huawei.android.pushselfshow.utils.a.a(this.b, this.e);
        if (com.huawei.android.pushselfshow.utils.a.d()) {
            int -l_2_I = com.huawei.android.pushselfshow.utils.a.k(this.b);
            if (-1 != -l_2_I) {
                int -l_3_I;
                if (-l_2_I != 0) {
                    -l_3_I = this.b.getResources().getColor(com.huawei.android.pushselfshow.utils.d.f(this.b, "hwpush_white"));
                    this.c.setImageDrawable(this.b.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(this.b, "hwpush_ic_cancel")));
                    this.e.setBackground(this.b.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(this.b, "hwpush_pic_ab_number")));
                    this.e.setTextColor(-l_3_I);
                } else {
                    -l_3_I = this.b.getResources().getColor(com.huawei.android.pushselfshow.utils.d.f(this.b, "hwpush_black"));
                    this.c.setImageDrawable(this.b.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(this.b, "hwpush_ic_cancel_light")));
                    this.e.setBackground(this.b.getResources().getDrawable(com.huawei.android.pushselfshow.utils.d.g(this.b, "hwpush_pic_ab_number_light")));
                }
                this.d.setTextColor(-l_3_I);
            }
        }
        this.c.setOnClickListener(new a(this.b));
    }

    private void a(boolean z) {
        this.i.setVisible(z);
        this.j.setVisible(z);
    }

    private void b() {
        if (this.h != null && this.f != null && this.g != null) {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "count:" + this.h.getCount());
            if (this.h.getCount() != 0) {
                this.f.setVisibility(0);
                this.g.setVisibility(8);
            } else {
                this.f.setVisibility(8);
                this.g.setVisibility(0);
            }
        }
    }

    private int c() {
        if (this.h == null) {
            return 0;
        }
        int -l_1_I = 0;
        for (e -l_4_R : this.h.a()) {
            if (-l_4_R != null && -l_4_R.a()) {
                -l_1_I++;
            }
        }
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "selectItemsNum:" + -l_1_I);
        return -l_1_I;
    }

    private void d() {
        Object -l_1_R = this.b.getActionBar();
        if (-l_1_R != null) {
            -l_1_R.setDisplayHomeAsUpEnabled(false);
            -l_1_R.setDisplayShowTitleEnabled(false);
            -l_1_R.setDisplayShowCustomEnabled(true);
            -l_1_R.setDisplayShowHomeEnabled(false);
            View -l_2_R = this.b.getLayoutInflater().inflate(com.huawei.android.pushselfshow.utils.d.c(this.b, "hwpush_custom_titlebar"), null);
            a(-l_2_R);
            -l_1_R.setCustomView(-l_2_R);
        }
        a(true);
        this.c.setVisibility(0);
        this.d.setText(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_deltitle"));
        this.f.setOnItemClickListener(new b(this.b));
        this.h.a(false);
        this.f.setLongClickable(false);
        if (1 != this.h.a().size()) {
            a(this.b, false);
        } else {
            a(this.b, true);
        }
    }

    private void e() {
        Object -l_1_R = new Intent(this.b, RichPushHtmlActivity.class);
        -l_1_R.putExtra("type", "html");
        -l_1_R.putExtra("selfshow_info", this.l);
        -l_1_R.putExtra("selfshow_token", this.m);
        -l_1_R.setFlags(268468240);
        -l_1_R.setPackage(this.b.getPackageName());
        this.b.finish();
        this.b.startActivity(-l_1_R);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message message) {
        try {
            switch (message.what) {
                case CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY /*1000*/:
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "mHandler MSG_LOAD_DONE");
                    this.f.setAdapter(this.h);
                    b();
                    if (this.k) {
                        d();
                        break;
                    }
                    break;
                case 1001:
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "mHandler MSG_DELETE_DONE");
                    if (!this.k) {
                        this.f.setAdapter(this.h);
                        this.c.performClick();
                        b();
                        break;
                    }
                    e();
                    return;
            }
        } catch (Object -l_2_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "handleMessage error:" + message.what + "," + -l_2_R.toString(), -l_2_R);
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onActivityResult");
    }

    public void onCreate(Intent intent) {
        Object -l_2_R;
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onCreate");
        try {
            Object -l_4_R;
            -l_2_R = this.b.getActionBar();
            if (-l_2_R != null) {
                -l_2_R.setDisplayHomeAsUpEnabled(true);
                -l_2_R.setDisplayShowTitleEnabled(true);
                -l_2_R.setTitle(this.b.getString(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_msg_favorites")));
            }
            Object -l_3_R = intent.getStringExtra("selfshow_info");
            if (!TextUtils.isEmpty(-l_3_R)) {
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "before decrypt");
                -l_4_R = com.huawei.android.pushagent.a.a.a.d.b(null, -l_3_R);
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "after decrypt");
                if (TextUtils.isEmpty(-l_4_R)) {
                    com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "get msgdata failed");
                    this.b.finish();
                    return;
                }
                this.k = intent.getBooleanExtra("selfshowMsgOutOfBound", false);
                this.m = intent.getByteArrayExtra("selfshow_token");
                this.l = -l_4_R.getBytes("UTF-8");
            }
            -l_4_R = new RelativeLayout(this.b);
            Object -l_5_R = a();
            this.g = (LinearLayout) -l_5_R.findViewById(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_no_collection_view"));
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "mNoCollectionLayout:" + this.g);
            -l_4_R.addView(-l_5_R);
            new Thread(new b(this)).start();
            this.b.setContentView(-l_4_R);
            if (this.k && this.i != null) {
                this.i.setEnabled(false);
            }
        } catch (Object -l_2_R2) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "call" + HtmlViewer.class.getName() + " onCreate(Intent intent) err: " + -l_2_R2.toString(), -l_2_R2);
        } catch (Object -l_2_R22) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_2_R22.toString());
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.b.getMenuInflater().inflate(com.huawei.android.pushselfshow.utils.d.d(this.b, "hwpush_collection_menu"), menu);
        return true;
    }

    public void onDestroy() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onDestroy");
        if (this.n != null && this.n.isShowing()) {
            this.n.dismiss();
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onKeyDown");
        if (i == 4 && keyEvent.getAction() == 0) {
            int -l_3_I = 0;
            if (this.c != null) {
                -l_3_I = this.c.getVisibility() != 0 ? 0 : 1;
            }
            if (this.k) {
                e();
            } else if (-l_3_I == 0) {
                this.b.finish();
            } else {
                this.c.performClick();
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "FavoritesActivity onOptionsItemSelected:" + menuItem);
        if (menuItem != null) {
            int -l_2_I = menuItem.getItemId();
            if (-l_2_I == 16908332) {
                onKeyDown(4, new KeyEvent(0, 4));
            } else if (-l_2_I == com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_menu_delete")) {
                Object -l_4_R = "";
                try {
                    -l_4_R = this.b.getResources().getQuantityString(com.huawei.android.pushselfshow.utils.d.b(this.b, "hwpush_delete_tip"), c());
                } catch (Object -l_5_R) {
                    -l_4_R = "";
                    com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_5_R.toString(), -l_5_R);
                }
                this.n = new Builder(this.b, com.huawei.android.pushselfshow.utils.a.i(this.b)).setTitle(-l_4_R).setPositiveButton(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_delete"), new c(this)).setNegativeButton(com.huawei.android.pushselfshow.utils.d.a(this.b, "hwpush_cancel"), null).create();
                this.n.show();
                this.n.getButton(-1).setTextColor(Color.parseColor("#ffd43e25"));
            } else if (-l_2_I == com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_menu_selectall")) {
                boolean -l_3_I = false;
                for (e -l_6_R : this.h.a()) {
                    if (!-l_6_R.a()) {
                        -l_3_I = true;
                        break;
                    }
                }
                this.h.a(-l_3_I, null);
                if (-l_3_I) {
                    this.e.setVisibility(0);
                    this.e.setText(String.valueOf(this.h.getCount()));
                    this.i.setEnabled(true);
                    a(this.b, true);
                } else {
                    this.e.setVisibility(8);
                    this.e.setText("");
                    this.i.setEnabled(false);
                    a(this.b, false);
                }
            }
            return true;
        }
        com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "onOptionsItemSelected, item is null");
        return false;
    }

    public void onPause() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onPause");
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "FavoritesActivity onPrepareOptionsMenu:" + menu);
        this.i = menu.findItem(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_menu_delete"));
        this.j = menu.findItem(com.huawei.android.pushselfshow.utils.d.e(this.b, "hwpush_menu_selectall"));
        a(false);
        return true;
    }

    public void onRestart() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onRestart");
    }

    public void onResume() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onResume");
    }

    public void onStart() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onStart");
    }

    public void onStop() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "FavoritesActivity onStop");
    }

    public void setActivity(Activity activity) {
        this.b = activity;
    }
}
