package com.huawei.android.pushselfshow.richpush.html.a;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.richpush.html.api.NativeToJsMessageQueue;
import com.huawei.android.pushselfshow.richpush.html.api.b;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class e implements OnCompletionListener, OnErrorListener, OnPreparedListener, g {
    public String a = null;
    Handler b = new Handler();
    Runnable c = null;
    boolean d = true;
    private a e = a.MEDIA_NONE;
    private String f = null;
    private int g = CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY;
    private MediaPlayer h = null;
    private int i = 0;
    private NativeToJsMessageQueue j;

    public enum a {
        MEDIA_NONE,
        MEDIA_STARTING,
        MEDIA_RUNNING,
        MEDIA_PAUSED,
        MEDIA_STOPPED
    }

    public e(Context context) {
        c.e("PushSelfShowLog", "init AudioPlayer");
    }

    private void a(a aVar) {
        this.e = aVar;
    }

    private boolean j() {
        Object -l_4_R;
        int -l_1_I = this.e.ordinal();
        if (-l_1_I != a.MEDIA_NONE.ordinal()) {
            return -l_1_I != a.MEDIA_STARTING.ordinal();
        } else {
            if (this.h == null) {
                this.h = new MediaPlayer();
                this.h.setOnErrorListener(this);
                this.h.setOnPreparedListener(this);
                this.h.setOnCompletionListener(this);
            }
            FileInputStream -l_2_R = null;
            try {
                if (b.a(this.f)) {
                    this.h.setDataSource(this.f);
                    this.h.setAudioStreamType(3);
                    a(a.MEDIA_STARTING);
                    this.h.prepareAsync();
                } else {
                    Object -l_3_R = new File(this.f);
                    if (-l_3_R.exists()) {
                        FileInputStream -l_2_R2 = new FileInputStream(-l_3_R);
                        try {
                            this.h.setDataSource(-l_2_R2.getFD());
                            a(a.MEDIA_STARTING);
                            this.h.prepare();
                            -l_2_R = -l_2_R2;
                        } catch (RuntimeException e) {
                            -l_2_R = -l_2_R2;
                            try {
                                c.e("PushSelfShowLog", "prepareAsync/prepare error");
                                a(a.MEDIA_NONE);
                                if (-l_2_R != null) {
                                    try {
                                        -l_2_R.close();
                                    } catch (Exception e2) {
                                        c.e("PushSelfShowLog", "close fileInputStream error");
                                    }
                                }
                                return false;
                            } catch (Throwable th) {
                                -l_4_R = th;
                                if (-l_2_R != null) {
                                    try {
                                        -l_2_R.close();
                                    } catch (Exception e3) {
                                        c.e("PushSelfShowLog", "close fileInputStream error");
                                    }
                                }
                                throw -l_4_R;
                            }
                        } catch (FileNotFoundException e4) {
                            -l_2_R = -l_2_R2;
                            c.e("PushSelfShowLog", "prepareAsync/prepare error");
                            a(a.MEDIA_NONE);
                            if (-l_2_R != null) {
                                try {
                                    -l_2_R.close();
                                } catch (Exception e5) {
                                    c.e("PushSelfShowLog", "close fileInputStream error");
                                }
                            }
                            return false;
                        } catch (IOException e6) {
                            -l_2_R = -l_2_R2;
                            c.e("PushSelfShowLog", "prepareAsync/prepare error");
                            a(a.MEDIA_NONE);
                            if (-l_2_R != null) {
                                try {
                                    -l_2_R.close();
                                } catch (Exception e7) {
                                    c.e("PushSelfShowLog", "close fileInputStream error");
                                }
                            }
                            return false;
                        } catch (Throwable th2) {
                            -l_4_R = th2;
                            -l_2_R = -l_2_R2;
                            if (-l_2_R != null) {
                                -l_2_R.close();
                            }
                            throw -l_4_R;
                        }
                    }
                }
                if (-l_2_R != null) {
                    try {
                        -l_2_R.close();
                    } catch (Exception e8) {
                        c.e("PushSelfShowLog", "close fileInputStream error");
                    }
                }
            } catch (RuntimeException e9) {
                c.e("PushSelfShowLog", "prepareAsync/prepare error");
                a(a.MEDIA_NONE);
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
                return false;
            } catch (FileNotFoundException e10) {
                c.e("PushSelfShowLog", "prepareAsync/prepare error");
                a(a.MEDIA_NONE);
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
                return false;
            } catch (IOException e11) {
                c.e("PushSelfShowLog", "prepareAsync/prepare error");
                a(a.MEDIA_NONE);
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
                return false;
            }
            return false;
        }
    }

    private float k() {
        try {
            return ((float) this.h.getDuration()) / 1000.0f;
        } catch (Exception e) {
            c.e("PushSelfShowLog", "getDurationInSeconds error ");
            return -1.0f;
        }
    }

    public String a(String str, JSONObject jSONObject) {
        return null;
    }

    public void a() {
        if (j() && this.h != null) {
            h();
        }
    }

    public void a(int i) {
        try {
            if (j()) {
                this.h.seekTo(i);
                c.a("PushSelfShowLog", "Send a onStatus update for the new seek");
                return;
            }
            this.i = i;
        } catch (IllegalStateException e) {
            c.a("PushSelfShowLog", "seekToPlaying failed");
        } catch (Exception e2) {
            c.a("PushSelfShowLog", "seekToPlaying failed");
        }
    }

    public void a(int i, int i2, Intent intent) {
    }

    public void a(NativeToJsMessageQueue nativeToJsMessageQueue, String str, String str2, JSONObject jSONObject) {
        if (nativeToJsMessageQueue != null) {
            this.j = nativeToJsMessageQueue;
            if ("preparePlaying".equals(str)) {
                d();
                if (str2 == null) {
                    c.a("PushSelfShowLog", "Audio exec callback is null ");
                } else {
                    this.a = str2;
                    a(jSONObject);
                }
            } else if ("startPlaying".equals(str)) {
                a();
            } else if ("seekToPlaying".equals(str)) {
                if (jSONObject != null) {
                    try {
                        if (jSONObject.has("milliseconds")) {
                            a(jSONObject.getInt("milliseconds"));
                        }
                    } catch (JSONException e) {
                        c.a("PushSelfShowLog", "seekto error");
                    }
                }
            } else if ("pausePlaying".equals(str)) {
                e();
            } else if ("stopPlaying".equals(str)) {
                f();
            } else if ("getPlayingStatus".equals(str)) {
                if (jSONObject != null) {
                    try {
                        if (jSONObject.has("frequently")) {
                            int -l_5_I = jSONObject.getInt("frequently");
                            if (-l_5_I > this.g) {
                                this.g = -l_5_I;
                            }
                        }
                    } catch (JSONException e2) {
                        c.a("PushSelfShowLog", "seekto error");
                    }
                }
                c.e("PushSelfShowLog", "this.frequently is " + this.g);
                g();
            } else {
                nativeToJsMessageQueue.a(str2, com.huawei.android.pushselfshow.richpush.html.api.d.a.METHOD_NOT_FOUND_EXCEPTION, "error", null);
            }
            return;
        }
        c.a("PushSelfShowLog", "jsMessageQueue is null while run into Audio Player exec");
    }

    public void a(JSONObject jSONObject) {
        c.e("PushSelfShowLog", " run into Audio player createAudio");
        if (jSONObject != null && jSONObject.has(CheckVersionField.CHECK_VERSION_SERVER_URL)) {
            try {
                String -l_2_R = jSONObject.getString(CheckVersionField.CHECK_VERSION_SERVER_URL);
                Object -l_4_R = b.a(this.j.a(), -l_2_R);
                if (-l_4_R != null) {
                    if (-l_4_R.length() > 0) {
                        this.f = -l_4_R;
                        this.j.a(this.a, com.huawei.android.pushselfshow.richpush.html.api.d.a.OK, "success", null);
                        if (jSONObject.has("pauseOnActivityPause")) {
                            this.d = jSONObject.getBoolean("pauseOnActivityPause");
                        }
                    }
                }
                c.e("PushSelfShowLog", -l_2_R + "File not exist");
                this.j.a(this.a, com.huawei.android.pushselfshow.richpush.html.api.d.a.AUDIO_ONLY_SUPPORT_HTTP, "error", null);
                if (jSONObject.has("pauseOnActivityPause")) {
                    this.d = jSONObject.getBoolean("pauseOnActivityPause");
                }
            } catch (Object -l_2_R2) {
                c.e("PushSelfShowLog", "startPlaying failed ", -l_2_R2);
                this.j.a(this.a, com.huawei.android.pushselfshow.richpush.html.api.d.a.JSON_EXCEPTION, "error", null);
            }
        } else {
            this.j.a(this.a, com.huawei.android.pushselfshow.richpush.html.api.d.a.JSON_EXCEPTION, "error", null);
        }
        c.e("PushSelfShowLog", " this.audioFile = " + this.f);
    }

    public void b() {
        c.e("PushSelfShowLog", "Audio onResume");
    }

    public void c() {
        c.b("PushSelfShowLog", "Audio onPause and pauseOnActivityPause is %s  this.player is %s", Boolean.valueOf(this.d), this.h);
        d();
    }

    public void d() {
        c.e("PushSelfShowLog", "Audio reset/Destory");
        try {
            this.d = true;
            if (this.h != null) {
                if (this.e != a.MEDIA_RUNNING) {
                    if (this.e != a.MEDIA_PAUSED) {
                        this.h.release();
                        this.h = null;
                    }
                }
                this.h.stop();
                this.h.release();
                this.h = null;
            }
            this.f = null;
            a(a.MEDIA_NONE);
            this.g = CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY;
            this.i = 0;
            if (this.c != null) {
                this.b.removeCallbacks(this.c);
            }
            this.c = null;
        } catch (IllegalStateException e) {
            c.a("PushSelfShowLog", "reset music error");
        } catch (Exception e2) {
            c.a("PushSelfShowLog", "reset music error");
        }
    }

    public void e() {
        if (this.e == a.MEDIA_RUNNING && this.h != null) {
            this.h.pause();
            a(a.MEDIA_PAUSED);
            return;
        }
        c.a("PushSelfShowLog", "AudioPlayer Error: pausePlaying() called during invalid state: " + this.e.ordinal());
    }

    public void f() {
        if (this.e == a.MEDIA_RUNNING || this.e == a.MEDIA_PAUSED) {
            this.h.pause();
            this.h.seekTo(0);
            c.a("PushSelfShowLog", "stopPlaying is calling stopped");
            a(a.MEDIA_STOPPED);
            return;
        }
        c.a("PushSelfShowLog", "AudioPlayer Error: stopPlaying() called during invalid state: " + this.e.ordinal());
    }

    public void g() {
        c.e("PushSelfShowLog", "getPlayingStatusRb is " + this.c);
        if (this.c != null) {
            try {
                this.b.removeCallbacks(this.c);
            } catch (Exception e) {
                c.e("PushSelfShowLog", "getPlayingStatus error,handler.removeCallbacks");
            }
        } else {
            this.c = new f(this);
        }
        this.b.postDelayed(this.c, (long) this.g);
        c.e("PushSelfShowLog", "handler.postDelayed " + this.g);
    }

    public void h() {
        try {
            this.h.start();
            a(a.MEDIA_RUNNING);
            this.i = 0;
        } catch (Object -l_1_R) {
            c.e("PushSelfShowLog", "play() error ", -l_1_R);
        }
    }

    public long i() {
        return (this.e == a.MEDIA_RUNNING || this.e == a.MEDIA_PAUSED) ? (long) (this.h.getCurrentPosition() / CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY) : -1;
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
        c.a("PushSelfShowLog", "on completion is calling stopped");
        a(a.MEDIA_STOPPED);
    }

    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        c.a("PushSelfShowLog", "AudioPlayer.onError(" + i + ", " + i2 + ")");
        Object -l_4_R = new JSONObject();
        try {
            -l_4_R.put(CheckVersionField.CHECK_VERSION_SERVER_URL, this.f);
            this.j.a(this.a, com.huawei.android.pushselfshow.richpush.html.api.d.a.AUDIO_PLAY_ERROR, "error", -l_4_R);
        } catch (JSONException e) {
            c.e("PushSelfShowLog", "onError error");
        }
        d();
        return false;
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        a(this.i);
        h();
    }
}
