package com.huawei.android.pushselfshow.richpush.html;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import com.huawei.android.pushselfshow.richpush.RichPushActivity;
import com.huawei.android.pushselfshow.richpush.html.api.ExposedJsApi;
import com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider;
import com.huawei.android.pushselfshow.richpush.tools.Console;
import com.huawei.android.pushselfshow.utils.a.e;
import com.huawei.android.pushselfshow.utils.b.b;
import com.huawei.android.pushselfshow.utils.c;
import com.huawei.android.pushselfshow.utils.c.a;
import com.huawei.android.pushselfshow.utils.d;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.io.File;

public class HtmlViewer implements a {
    public static final String TAG = "PushSelfShowLog";
    PageProgressView a;
    c b = new c(this);
    c c = new c(this);
    private Activity d;
    public b dtl = null;
    private WebView e;
    private com.huawei.android.pushselfshow.richpush.tools.a f;
    private com.huawei.android.pushselfshow.c.a g = null;
    private String h;
    private ExposedJsApi i;
    private MenuItem j;
    private MenuItem k;
    private MenuItem l;
    private boolean m;
    private boolean n = false;
    private boolean o = false;
    private AlertDialog p = null;
    private AlertDialog q = null;
    private boolean r = false;

    private View a(Context context) {
        Object -l_2_R = null;
        Object -l_6_R = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(d.c(context, "hwpush_collect_tip_image"), null);
        try {
            Object -l_3_R = Class.forName("huawei.android.widget.DialogContentHelper");
            Object -l_4_R = -l_3_R.getDeclaredConstructor(new Class[]{Boolean.TYPE, Boolean.TYPE, Context.class}).newInstance(new Object[]{Boolean.valueOf(true), Boolean.valueOf(true), context});
            Object -l_8_R = -l_3_R.getDeclaredMethod("beginLayout", new Class[0]);
            Object -l_9_R = -l_3_R.getDeclaredMethod("insertView", new Class[]{View.class, OnClickListener.class});
            Object -l_10_R = -l_3_R.getDeclaredMethod("insertBodyText", new Class[]{CharSequence.class});
            Object -l_11_R = -l_3_R.getDeclaredMethod("endLayout", new Class[0]);
            -l_8_R.invoke(-l_4_R, new Object[0]);
            -l_9_R.invoke(-l_4_R, new Object[]{-l_6_R, null});
            Object -l_12_R = this.d.getString(d.a(context, "hwpush_collect_tip"));
            -l_10_R.invoke(-l_4_R, new Object[]{-l_12_R});
            return (View) -l_11_R.invoke(-l_4_R, new Object[0]);
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "DialogContentHelper ClassNotFoundException");
            return -l_2_R;
        } catch (Object -l_7_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R.toString(), -l_7_R);
            return -l_2_R;
        } catch (Object -l_7_R2) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R2.toString(), -l_7_R2);
            return -l_2_R;
        } catch (Object -l_7_R22) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R22.toString(), -l_7_R22);
            return -l_2_R;
        } catch (Object -l_7_R222) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R222.toString(), -l_7_R222);
            return -l_2_R;
        } catch (Object -l_7_R2222) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R2222.toString(), -l_7_R2222);
            return -l_2_R;
        } catch (Object -l_7_R22222) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_7_R22222.toString(), -l_7_R22222);
            return -l_2_R;
        }
    }

    private void a() {
        this.e.getSettings().setJavaScriptEnabled(true);
        if (VERSION.SDK_INT >= 11 && VERSION.SDK_INT <= 16) {
            this.e.removeJavascriptInterface("searchBoxJavaBridge_");
            this.e.removeJavascriptInterface("accessibility");
            this.e.removeJavascriptInterface("accessibilityTraversal");
        }
        if (VERSION.SDK_INT <= 18) {
            this.e.getSettings().setSavePassword(false);
        }
        this.e.getSettings().setPluginState(PluginState.ON);
        this.e.getSettings().setLoadsImagesAutomatically(true);
        this.e.getSettings().setDomStorageEnabled(true);
        this.e.getSettings().setSupportZoom(true);
        this.e.setScrollBarStyle(0);
        this.e.setHorizontalScrollBarEnabled(false);
        this.e.setVerticalScrollBarEnabled(false);
        this.e.getSettings().setSupportMultipleWindows(true);
        this.e.setDownloadListener(new a(this));
        this.e.setOnTouchListener(new c(this));
        this.e.setWebChromeClient(new d(this));
        this.e.setWebViewClient(new e(this));
    }

    private void a(Activity activity) {
        if (activity != null) {
            this.l.setEnabled(false);
            this.r = true;
            if (VERSION.SDK_INT >= 23 && !com.huawei.android.pushselfshow.utils.a.e(activity) && com.huawei.android.pushselfshow.utils.a.g(activity) && activity.checkSelfPermission("com.huawei.pushagent.permission.RICHMEDIA_PROVIDER") != 0) {
                a(new String[]{"com.huawei.pushagent.permission.RICHMEDIA_PROVIDER"}, 10003);
                return;
            }
            new Thread(new h(this, activity)).start();
        }
    }

    private void a(String str) {
        Object -l_2_R = this.d.getActionBar();
        if (-l_2_R != null) {
            -l_2_R.setTitle(str);
        }
    }

    private void a(String[] strArr, int i) {
        try {
            Intent -l_3_R = new Intent("huawei.intent.action.REQUEST_PERMISSIONS");
            -l_3_R.setPackage("com.huawei.systemmanager");
            -l_3_R.putExtra("KEY_HW_PERMISSION_ARRAY", strArr);
            -l_3_R.putExtra("KEY_HW_PERMISSION_PKG", this.d.getPackageName());
            if (com.huawei.android.pushselfshow.utils.a.a(this.d, "com.huawei.systemmanager", -l_3_R).booleanValue()) {
                try {
                    com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "checkAndRequestPermission: systemmanager permission activity is exist");
                    this.d.startActivityForResult(-l_3_R, i);
                    return;
                } catch (Object -l_4_R) {
                    com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "checkAndRequestPermission: Exception", -l_4_R);
                    this.d.requestPermissions(strArr, i);
                    return;
                }
            }
            com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "checkAndRequestPermission: systemmanager permission activity is not exist");
            this.d.requestPermissions(strArr, i);
        } catch (Object -l_3_R2) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
        }
    }

    private int b(Activity activity) {
        int -l_2_I = 0;
        if (activity == null) {
            return 0;
        }
        Cursor cursor = null;
        try {
            Object -l_4_R = e.a().a((Context) activity, RichMediaProvider.a.f, "SELECT pushmsg._id,pushmsg.msg,pushmsg.token,pushmsg.url,notify.bmp FROM pushmsg LEFT OUTER JOIN notify ON pushmsg.url = notify.url order by pushmsg._id desc limit 1000;", null);
            if (-l_4_R != null) {
                -l_2_I = -l_4_R.getCount();
            }
            if (-l_4_R != null) {
                -l_4_R.close();
            }
        } catch (Object -l_5_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_5_R.toString(), -l_5_R);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "currentExistCount:" + -l_2_I);
        return -l_2_I;
    }

    private void c(Activity activity) {
        this.q = new Builder(activity, com.huawei.android.pushselfshow.utils.a.i(activity)).setTitle(d.a(activity, "hwpush_dialog_limit_title")).setMessage(d.a(activity, "hwpush_dialog_limit_message")).setNegativeButton(17039360, null).setPositiveButton(d.a(activity, "hwpush_dialog_limit_ok"), new j(this)).setOnDismissListener(new i(this, activity)).create();
        this.q.show();
    }

    private void d(Activity activity) {
        if (activity != null) {
            Object -l_2_R = new Intent(activity, RichPushActivity.class);
            if (this.g != null) {
                try {
                    -l_2_R.putExtra("selfshow_token", this.g.d());
                    -l_2_R.putExtra("selfshow_info", com.huawei.android.pushagent.a.a.a.d.a(null, new String(this.g.c(), "UTF-8")));
                } catch (Object -l_3_R) {
                    com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_3_R.toString());
                }
            }
            -l_2_R.setFlags(268468240);
            -l_2_R.putExtra("selfshowMsgOutOfBound", true);
            -l_2_R.setPackage(activity.getPackageName());
            activity.finish();
            activity.startActivity(-l_2_R);
        }
    }

    public void downLoadFailed() {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "downLoadFailed:");
        this.c = null;
        showErrorHtmlURI(this.d.getString(d.a(this.d, "hwpush_download_failed")));
    }

    public void downLoadSuccess(String str) {
        try {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "downLoadSuccess:" + str + "，and start loadLocalZip");
            loadLocalZip(str);
        } catch (Object -l_2_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "downLoadSuccess failed", -l_2_R);
        }
    }

    public void enableJavaJS(String str) {
        try {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "enable JavaJs support and indexFileUrl is " + str);
            String -l_2_R = null;
            if (str != null) {
                -l_2_R = str.substring(0, str.lastIndexOf("/"));
            }
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "m_activity is " + this.d);
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "webView is " + this.e);
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "localPath is " + -l_2_R);
            if (this.g.B() == 0) {
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "pushmsg.needUserId false");
                this.i = new ExposedJsApi(this.d, this.e, -l_2_R, false, "");
            } else {
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "pushmsg.needUserId true");
                this.i = new ExposedJsApi(this.d, this.e, -l_2_R, true, this.g.K());
            }
            this.e.addJavascriptInterface(new Console(), "console");
            this.e.addJavascriptInterface(this.i, "_nativeApi");
        } catch (Object -l_2_R2) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "enable JavaJs support failed ", -l_2_R2);
        }
    }

    public void handleMessage(Message message) {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "handleMessage " + message.what + "," + message.toString());
        switch (message.what) {
            case 1:
                downLoadSuccess((String) message.obj);
                return;
            case 2:
                downLoadFailed();
                return;
            case CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY /*1000*/:
                c(this.d);
                return;
            default:
                return;
        }
    }

    public void loadLocalZip(String str) {
        if (str != null && str.length() > 0) {
            this.h = com.huawei.android.pushselfshow.richpush.tools.d.a(this.d, str);
            if (this.h != null && this.h.length() > 0) {
                Object -l_3_R = Uri.fromFile(new File(this.h));
                enableJavaJS(this.h);
                this.g.d(-l_3_R.toString());
                this.g.f("text/html_local");
                this.f.a(this.g);
                this.e.loadUrl(-l_3_R.toString());
                return;
            }
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "check index.html file failed");
            this.c = null;
        }
        showErrorHtmlURI(this.d.getString(d.a(this.d, "hwpush_invalid_content")));
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        try {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "run HtmlViewer onActivityResult");
            if (this.i != null) {
                this.i.onActivityResult(i, i2, intent);
            }
            if (10003 != i) {
                if (10004 != i) {
                    if (10005 == i && this.g != null) {
                        loadLocalZip(this.g.x());
                    }
                } else if (this.g != null && this.c != null) {
                    this.dtl = new b(this.c, this.d, this.g.x(), com.huawei.android.pushselfshow.richpush.tools.b.a("application/zip"));
                    this.dtl.b();
                }
            } else if (i2 == 0) {
                com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "onActivityResult: RESULT_CANCELED");
                this.l.setEnabled(true);
                this.r = false;
            } else if (-1 == i2) {
                com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "onActivityResult: RESULT_OK");
                if (this.d.checkSelfPermission("com.huawei.pushagent.permission.RICHMEDIA_PROVIDER") != 0) {
                    this.l.setEnabled(true);
                    this.r = false;
                    return;
                }
                com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "onActivityResult: Permission is granted");
                new Thread(new f(this)).start();
            }
        } catch (Object -l_4_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", -l_4_R.toString(), -l_4_R);
        }
    }

    public void onCreate(Intent intent) {
        Object -l_2_R;
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "HtmlViewer onCreate");
        if (intent != null) {
            try {
                this.m = intent.getBooleanExtra("selfshow_from_list", false);
                this.r = intent.getBooleanExtra("collect_img_disable", false);
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "mCollectImgDisable:" + this.r);
                this.f = new com.huawei.android.pushselfshow.richpush.tools.a(this.d);
                -l_2_R = this.d.getActionBar();
                if (-l_2_R != null) {
                    -l_2_R.setDisplayHomeAsUpEnabled(true);
                }
                this.d.setRequestedOrientation(5);
                Object -l_3_R = new RelativeLayout(this.d);
                int -l_4_I = d.c(this.d, "hwpush_msg_show");
                if (com.huawei.android.pushagent.a.a.a.a() < 11) {
                    -l_4_I = d.c(this.d, "hwpush_msg_show_before_emui5_0");
                }
                RelativeLayout -l_5_R = (RelativeLayout) this.d.getLayoutInflater().inflate(-l_4_I, null);
                -l_3_R.addView(-l_5_R);
                this.d.setContentView(-l_3_R);
                this.a = (PageProgressView) -l_5_R.findViewById(d.e(this.d, "hwpush_progressbar"));
                this.e = (WebView) -l_5_R.findViewById(d.e(this.d, "hwpush_msg_show_view"));
                a();
                if (intent.hasExtra("selfshow_info")) {
                    this.g = new com.huawei.android.pushselfshow.c.a(intent.getByteArrayExtra("selfshow_info"), intent.getByteArrayExtra("selfshow_token"));
                    if (this.g.b()) {
                        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "pushmsg.rpct:" + this.g.z());
                        this.f.a(this.g);
                    } else {
                        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "parseMessage failed");
                        return;
                    }
                }
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "pushmsg is null");
                showErrorHtmlURI(this.d.getString(d.a(this.d, "hwpush_invalid_content")));
                if (this.g == null) {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "pushmsg is null :");
                    this.g = new com.huawei.android.pushselfshow.c.a();
                } else {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "fileurl :" + this.g.x() + ", the pushmsg is " + this.g.toString());
                }
                com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "pushmsg.rpct:" + this.g.z());
                if (!"application/zip".equals(this.g.z())) {
                    if ("application/zip_local".equals(this.g.z())) {
                        loadLocalZip(this.g.x());
                    } else {
                        if (!"text/html".equals(this.g.z())) {
                            if (!"text/html_local".equals(this.g.z())) {
                                showErrorHtmlURI(this.d.getString(d.a(this.d, "hwpush_invalid_content")));
                            }
                        }
                        Object -l_6_R = null;
                        if ("text/html_local".equals(this.g.z())) {
                            -l_6_R = this.g.x();
                        }
                        enableJavaJS(-l_6_R);
                        this.e.loadUrl(this.g.x());
                    }
                } else if (-1 != com.huawei.android.pushagent.a.a.a.a(this.d)) {
                    this.dtl = new b(this.c, this.d, this.g.x(), com.huawei.android.pushselfshow.richpush.tools.b.a("application/zip"));
                    this.dtl.b();
                } else {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "no network. can not load message");
                }
            } catch (Object -l_2_R2) {
                com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "call" + HtmlViewer.class.getName() + " onCreate(Intent intent) err: " + -l_2_R2.toString(), -l_2_R2);
            }
            return;
        }
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "onCreate, intent is null");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "HtmlViewer onCreateOptionsMenu:" + menu);
        this.d.getMenuInflater().inflate(d.d(this.d, "hwpush_msg_show_menu"), menu);
        return true;
    }

    public void onDestroy() {
        try {
            if (this.p != null && this.p.isShowing()) {
                this.p.dismiss();
            }
            if (this.q != null) {
                if (this.q.isShowing()) {
                    this.q.dismiss();
                }
            }
            if (this.i != null) {
                this.i.onDestroy();
            }
            if (!(this.h == null || this.o)) {
                Object -l_1_R = this.h.substring(0, this.h.lastIndexOf("/"));
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "try to remove dir " + -l_1_R);
                com.huawei.android.pushselfshow.utils.a.a(new File(-l_1_R));
            }
            if (this.dtl != null && this.dtl.e) {
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "cancel ProgressDialog loading dialog when richpush file is downloading");
                this.dtl.a();
                this.c = null;
            }
            this.e.stopLoading();
            this.e = null;
        } catch (IndexOutOfBoundsException e) {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "remove unsuccess ,maybe removed before");
        } catch (Exception e2) {
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "remove unsuccess ,maybe removed before");
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4 && keyEvent.getAction() == 0) {
            if (this.m) {
                Object -l_3_R = new Intent(this.d, RichPushActivity.class);
                -l_3_R.putExtra("type", "favorite");
                -l_3_R.setPackage(this.d.getPackageName());
                this.d.finish();
                this.d.startActivity(-l_3_R);
            } else {
                this.d.finish();
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "HtmlViewer onOptionsItemSelected:" + menuItem);
        if (menuItem != null) {
            int -l_2_I = menuItem.getItemId();
            if (-l_2_I == 16908332) {
                onKeyDown(4, new KeyEvent(0, 4));
            } else if (-l_2_I != d.e(this.d, "hwpush_menu_back")) {
                if (-l_2_I != d.e(this.d, "hwpush_menu_forward")) {
                    if (-l_2_I == d.e(this.d, "hwpush_menu_refresh")) {
                        setProgress(0);
                        this.e.reload();
                    } else if (-l_2_I == d.e(this.d, "hwpush_menu_collect")) {
                        Object -l_3_R = new com.huawei.android.pushagent.a.a.e(this.d, "push_client_self_info");
                        Object -l_4_R = "isFirstCollect";
                        int -l_5_I = 1;
                        if (-l_3_R.c("isFirstCollect")) {
                            -l_5_I = 0;
                        }
                        if (-l_5_I == 0) {
                            a(this.d);
                        } else {
                            this.p = new Builder(this.d).setPositiveButton(d.a(this.d, "hwpush_collect_tip_known"), new b(this, -l_3_R)).create();
                            if (com.huawei.android.pushagent.a.a.a.a() <= 9) {
                                this.p.setTitle(d.a(this.d, "hwpush_msg_collect"));
                                this.p.setMessage(this.d.getString(d.a(this.d, "hwpush_collect_tip")));
                            } else {
                                Object -l_6_R = a(this.d);
                                if (-l_6_R == null) {
                                    this.p.setTitle(d.a(this.d, "hwpush_msg_collect"));
                                    this.p.setMessage(this.d.getString(d.a(this.d, "hwpush_collect_tip")));
                                } else {
                                    this.p.setView(-l_6_R);
                                }
                            }
                            this.p.show();
                        }
                    }
                } else if (this.e != null && this.e.canGoForward()) {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", " can Go Forward " + this.e.canGoForward());
                    this.e.goForward();
                }
            } else if (this.e != null && this.e.canGoBack()) {
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "can go back " + this.e.canGoBack());
                this.e.goBack();
            }
            return true;
        }
        com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "onOptionsItemSelected, item is null");
        return false;
    }

    public void onPause() {
        if (this.i != null) {
            this.i.onPause();
        }
        try {
            this.e.getClass().getMethod("onPause", new Class[0]).invoke(this.e, (Object[]) null);
        } catch (Object -l_1_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "htmlviewer onpause error", -l_1_R);
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "HtmlViewer onPrepareOptionsMenu:" + menu);
        this.j = menu.findItem(d.e(this.d, "hwpush_menu_back"));
        this.k = menu.findItem(d.e(this.d, "hwpush_menu_forward"));
        this.l = menu.findItem(d.e(this.d, "hwpush_menu_collect"));
        if (this.e != null) {
            if (this.e.canGoBack()) {
                this.j.setEnabled(true);
            } else {
                this.j.setEnabled(false);
            }
            if (this.e.canGoForward()) {
                this.k.setEnabled(true);
            } else {
                this.k.setEnabled(false);
            }
        }
        if (this.m || this.r) {
            this.l.setEnabled(false);
            this.r = true;
        }
        return true;
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "enter HtmlViewer onRequestPermissionsResult");
        if (10003 != i) {
            if (10004 != i) {
                if (10005 == i && this.g != null) {
                    loadLocalZip(this.g.x());
                }
            } else if (this.g != null && this.c != null) {
                this.dtl = new b(this.c, this.d, this.g.x(), com.huawei.android.pushselfshow.richpush.tools.b.a("application/zip"));
                this.dtl.b();
            }
        } else if (iArr != null && iArr.length > 0 && iArr[0] == 0) {
            new Thread(new g(this)).start();
        } else {
            this.l.setEnabled(true);
            this.r = false;
        }
    }

    public void onResume() {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "HtmlViewer onResume");
        if (this.i != null) {
            this.i.onResume();
        }
        try {
            this.e.getClass().getMethod("onResume", new Class[0]).invoke(this.e, (Object[]) null);
        } catch (Object -l_1_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "htmlviewer onResume error", -l_1_R);
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("collect_img_disable", this.r);
    }

    public void onStop() {
        if (this.i != null) {
            this.i.onPause();
        }
    }

    public String prepareJS(String str) {
        Object obj = 1;
        Object -l_2_R;
        try {
            -l_2_R = b.b(this.d) + File.separator + this.d.getPackageName().replace(".", "");
            if (!new File(-l_2_R).exists()) {
                if (new File(-l_2_R).mkdirs()) {
                    com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "mkdir true");
                }
            }
            com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS fileHeader is " + -l_2_R);
            String -l_3_R = null;
            String -l_4_R = -l_2_R + File.separator + "newpush.js";
            Object -l_5_R = new File(-l_4_R);
            if (-l_5_R.exists()) {
                if (System.currentTimeMillis() - -l_5_R.lastModified() > 1300000000) {
                    obj = null;
                }
                if (obj == null) {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "new push.js may be out of date ,or try to update");
                    if (com.huawei.android.pushagent.a.a.a.a(this.d) != -1 && new b().b(this.d, "http://open.hicloud.com/android/push1.0.js", -l_4_R) && new File(-l_4_R).exists()) {
                        -l_3_R = -l_4_R;
                        com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS dlUtil.downLoadSgThread  pushUrl is " + -l_4_R);
                    }
                } else {
                    -l_3_R = -l_4_R;
                    com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS  not arrival update  pushUrl is " + -l_4_R);
                }
            } else if (com.huawei.android.pushagent.a.a.a.a(this.d) != -1 && new b().b(this.d, "http://open.hicloud.com/android/push1.0.js", -l_4_R) && new File(-l_4_R).exists()) {
                -l_3_R = -l_4_R;
                com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS new js isnot exist, so  downloaded  pushUrl is " + -l_4_R);
            }
            if (-l_3_R == null || -l_3_R.length() == 0) {
                com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "  pushUrl is " + -l_3_R);
                String -l_6_R = -l_2_R + File.separator + "push1.0.js";
                com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "  pushjsPath is " + -l_6_R);
                if (new File(-l_6_R).exists()) {
                    if (new File(-l_6_R).delete()) {
                        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "delete pushjsPath success");
                    }
                    com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS new js  is not prepared so use local  pushUrl is " + -l_3_R);
                } else {
                    com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", " new File(pushjsPath) not exists() ");
                }
                com.huawei.android.pushselfshow.utils.a.a(this.d, "pushresources" + File.separator + "push1.0.js", -l_6_R);
                -l_3_R = -l_6_R;
            }
            if (-l_3_R.length() > 0) {
                com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "  pushUrl is " + -l_3_R);
                Object -l_6_R2 = -l_3_R.substring(-l_3_R.lastIndexOf("/"));
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", " pushUrlName is %s,destPath is %s ", -l_6_R2, str.substring("file://".length(), str.lastIndexOf("/")) + -l_6_R2);
                com.huawei.android.pushselfshow.utils.a.a(new File(-l_3_R), new File(-l_7_R));
                return "." + -l_6_R2;
            }
        } catch (Object -l_2_R2) {
            com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS", -l_2_R2);
        } catch (Object -l_2_R22) {
            com.huawei.android.pushagent.a.a.c.e("PushSelfShowLog", "prepareJS", -l_2_R22);
        }
        return "http://open.hicloud.com/android/push1.0.js";
    }

    public void setActivity(Activity activity) {
        this.d = activity;
    }

    public void setProgress(int i) {
        if (i < 100) {
            if (!this.n) {
                this.a.setVisibility(0);
                this.n = true;
            }
            this.a.a((i * 10000) / 100);
            return;
        }
        this.a.a(10000);
        this.a.setVisibility(4);
        this.n = false;
    }

    public void showErrorHtmlURI(String str) {
        try {
            Object -l_3_R = new com.huawei.android.pushselfshow.richpush.tools.c(this.d, str).a();
            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "showErrorHtmlURI,filePath is " + -l_3_R);
            if (-l_3_R != null) {
                if (-l_3_R.length() > 0) {
                    Object -l_5_R = Uri.fromFile(new File(-l_3_R));
                    enableJavaJS(null);
                    this.e.loadUrl(-l_5_R.toString());
                }
            }
        } catch (Object -l_2_R) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "showErrorHtmlURI failed", -l_2_R);
        }
        if (this.d.getString(d.a(this.d, "hwpush_download_failed")).equals(str)) {
            com.huawei.android.pushselfshow.utils.a.a(this.d, "12", this.g, -1);
            return;
        }
        com.huawei.android.pushselfshow.utils.a.a(this.d, "6", this.g, -1);
    }
}
