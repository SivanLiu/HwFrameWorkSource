package com.android.server.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hsm.HwSystemManager;
import android.media.AudioFormat;
import android.media.AudioFormat.Builder;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.AudioSystem.AudioRecordingCallback;
import android.media.IRecordingConfigDispatcher;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import android.util.Xml;
import com.android.server.audio.AudioEventLogger.Event;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class RecordingActivityMonitor implements AudioRecordingCallback {
    public static final int AUDIO_TYPE = 1;
    private static final String CONFIG_FILE_WHITE_BLACK_APP = "/system/emui/base/xml/hw_Recordeffect_app_config.xml";
    private static final String NODE_ATTR_PACKAGE = "package";
    private static final String NODE_SCENE1 = "voice_message_5px";
    private static final String NODE_SCENE10 = "default";
    private static final String NODE_SCENE2 = "video_message_5px";
    private static final String NODE_SCENE3 = "karaoke_5px";
    private static final String NODE_SCENE4 = "live_telecast_5px";
    private static final String NODE_SCENE5 = "default_5px";
    private static final String NODE_SCENE6 = "voice_message";
    private static final String NODE_SCENE7 = "video_message";
    private static final String NODE_SCENE8 = "karaoke";
    private static final String NODE_SCENE9 = "live_telecast";
    private static final String NODE_WHITEAPP = "whiteapp";
    public static final String TAG = "AudioService.RecordingActivityMonitor";
    private static final AudioEventLogger sEventLogger = new AudioEventLogger(50, "recording activity as reported through AudioSystem.AudioRecordingCallback");
    private ArrayList<RecMonitorClient> mClients = new ArrayList();
    private boolean mHasPublicClients = false;
    private ArrayList<String> mKaraokeWhiteList1 = null;
    private ArrayList<String> mKaraokeWhiteList10 = null;
    private ArrayList<String> mKaraokeWhiteList2 = null;
    private ArrayList<String> mKaraokeWhiteList3 = null;
    private ArrayList<String> mKaraokeWhiteList4 = null;
    private ArrayList<String> mKaraokeWhiteList5 = null;
    private ArrayList<String> mKaraokeWhiteList6 = null;
    private ArrayList<String> mKaraokeWhiteList7 = null;
    private ArrayList<String> mKaraokeWhiteList8 = null;
    private ArrayList<String> mKaraokeWhiteList9 = null;
    private final PackageManager mPackMan;
    private HashMap<Integer, AudioRecordingConfiguration> mRecordConfigs = new HashMap();
    private Integer whatsapp_session = Integer.valueOf(0);

    private static final class RecMonitorClient implements DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        final IRecordingConfigDispatcher mDispatcherCb;
        final boolean mIsPrivileged;

        RecMonitorClient(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
            this.mDispatcherCb = rcdb;
            this.mIsPrivileged = isPrivileged;
        }

        public void binderDied() {
            Log.w(RecordingActivityMonitor.TAG, "client died");
            sMonitor.unregisterRecordingCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(RecordingActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    private static final class RecordingEvent extends Event {
        private final int mClientUid;
        private final String mPackName;
        private final int mRecEvent;
        private final int mSession;
        private final int mSource;

        RecordingEvent(int event, int uid, int session, int source, String packName) {
            this.mRecEvent = event;
            this.mClientUid = uid;
            this.mSession = session;
            this.mSource = source;
            this.mPackName = packName;
        }

        public String eventToString() {
            String str;
            StringBuilder stringBuilder = new StringBuilder("rec ");
            stringBuilder.append(this.mRecEvent == 1 ? "start" : "stop ");
            stringBuilder.append(" uid:");
            stringBuilder.append(this.mClientUid);
            stringBuilder.append(" session:");
            stringBuilder.append(this.mSession);
            stringBuilder.append(" src:");
            stringBuilder.append(MediaRecorder.toLogFriendlyAudioSource(this.mSource));
            if (this.mPackName == null) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" pack:");
                stringBuilder2.append(this.mPackName);
                str = stringBuilder2.toString();
            }
            stringBuilder.append(str);
            return stringBuilder.toString();
        }
    }

    RecordingActivityMonitor(Context ctxt) {
        RecMonitorClient.sMonitor = this;
        this.mPackMan = ctxt.getPackageManager();
    }

    private void getAppInWhiteBlackList(List<String> whiteAppList1, List<String> whiteAppList2, List<String> whiteAppList3, List<String> whiteAppList4, List<String> whiteAppList5, List<String> whiteAppList6, List<String> whiteAppList7, List<String> whiteAppList8, List<String> whiteAppList9, List<String> whiteAppList10) {
        InputStream in = null;
        XmlPullParser xmlParser = null;
        try {
            File configFile = new File(CONFIG_FILE_WHITE_BLACK_APP);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwCfgFilePolicy get Record White List CfgFile not null, path = ");
            stringBuilder.append(configFile.getPath());
            Log.v(str, stringBuilder.toString());
            in = new FileInputStream(configFile.getPath());
            xmlParser = Xml.newPullParser();
            xmlParser.setInput(in, null);
            if (xmlParser != null) {
                parseXmlForWhiteBlackList(xmlParser, whiteAppList1, whiteAppList2, whiteAppList3, whiteAppList4, whiteAppList5, whiteAppList6, whiteAppList7, whiteAppList8, whiteAppList9, whiteAppList10);
            }
            try {
                in.close();
            } catch (IOException e) {
                IOException iOException = e;
                Log.e(TAG, "RecordWhiteList IO Close Fail");
            }
        } catch (FileNotFoundException e2) {
            Log.e(TAG, "RecordWhiteList FileNotFoundException");
            if (in != null) {
                in.close();
            }
        } catch (XmlPullParserException e3) {
            Log.e(TAG, "RecordWhiteList XmlPullParserException");
            if (in != null) {
                in.close();
            }
        } catch (Exception e4) {
            Log.e(TAG, "RecordWhiteList getAppInWhiteBlackList Exception ", e4);
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            XmlPullParser xmlPullParser = xmlParser;
            InputStream in2 = in;
            Throwable in3 = th;
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e5) {
                    IOException iOException2 = e5;
                    Log.e(TAG, "RecordWhiteList IO Close Fail");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:125:0x0296, code:
            r16 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseXmlForWhiteBlackList(XmlPullParser parser, List<String> whiteAppList1, List<String> whiteAppList2, List<String> whiteAppList3, List<String> whiteAppList4, List<String> whiteAppList5, List<String> whiteAppList6, List<String> whiteAppList7, List<String> whiteAppList8, List<String> whiteAppList9, List<String> whiteAppList10) {
        int scene_id = 0;
        while (true) {
            XmlPullParser xmlPullParser;
            List<String> list;
            List<String> list2;
            List<String> list3;
            List<String> list4;
            List<String> list5;
            List<String> list6;
            List<String> list7;
            List<String> list8;
            List<String> list9;
            List<String> list10;
            try {
                int next = parser.next();
                int eventType = next;
                if (next == 1) {
                    xmlPullParser = parser;
                    list = whiteAppList1;
                    list2 = whiteAppList2;
                    list3 = whiteAppList3;
                    list4 = whiteAppList4;
                    list5 = whiteAppList5;
                    list6 = whiteAppList6;
                    list7 = whiteAppList7;
                    list8 = whiteAppList8;
                    list9 = whiteAppList9;
                    list10 = whiteAppList10;
                    return;
                } else if (eventType == 2) {
                    int scene_id2;
                    String nodeName = parser.getName();
                    if (nodeName.equals(NODE_SCENE1)) {
                        scene_id = 1;
                    } else if (nodeName.equals(NODE_SCENE2)) {
                        scene_id = 2;
                    } else if (nodeName.equals(NODE_SCENE3)) {
                        scene_id = 3;
                    } else if (nodeName.equals(NODE_SCENE4)) {
                        scene_id = 4;
                    } else if (nodeName.equals(NODE_SCENE5)) {
                        scene_id = 5;
                    } else if (nodeName.equals(NODE_SCENE6)) {
                        scene_id = 6;
                    } else if (nodeName.equals(NODE_SCENE7)) {
                        scene_id = 7;
                    } else if (nodeName.equals(NODE_SCENE8)) {
                        scene_id = 8;
                    } else if (nodeName.equals(NODE_SCENE9)) {
                        scene_id = 9;
                    } else if (nodeName.equals("default")) {
                        scene_id = 10;
                    }
                    if (nodeName.equals(NODE_WHITEAPP)) {
                        try {
                            String packageName = parser.getAttributeValue(null, "package");
                            try {
                                if (isValidCharSequence(packageName)) {
                                    switch (scene_id) {
                                        case 1:
                                            list2 = whiteAppList2;
                                            list3 = whiteAppList3;
                                            list4 = whiteAppList4;
                                            list5 = whiteAppList5;
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            whiteAppList1.add(packageName);
                                            break;
                                        case 2:
                                            list3 = whiteAppList3;
                                            list4 = whiteAppList4;
                                            list5 = whiteAppList5;
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList2.add(packageName);
                                                list = whiteAppList1;
                                                break;
                                            } catch (XmlPullParserException e) {
                                                list = whiteAppList1;
                                                break;
                                            } catch (IOException e2) {
                                                list = whiteAppList1;
                                                break;
                                            }
                                        case 3:
                                            list4 = whiteAppList4;
                                            list5 = whiteAppList5;
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList3.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                break;
                                            } catch (XmlPullParserException e3) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                break;
                                            } catch (IOException e4) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                break;
                                            }
                                        case 4:
                                            list5 = whiteAppList5;
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList4.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                break;
                                            } catch (XmlPullParserException e5) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                break;
                                            } catch (IOException e6) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                break;
                                            }
                                        case 5:
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList5.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                break;
                                            } catch (XmlPullParserException e7) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                break;
                                            } catch (IOException e8) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                break;
                                            }
                                        case 6:
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList6.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                break;
                                            } catch (XmlPullParserException e9) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                break;
                                            } catch (IOException e10) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                break;
                                            }
                                        case 7:
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList7.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                break;
                                            } catch (XmlPullParserException e11) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                break;
                                            } catch (IOException e12) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                break;
                                            }
                                        case 8:
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList8.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                break;
                                            } catch (XmlPullParserException e13) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                break;
                                            } catch (IOException e14) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                break;
                                            }
                                        case 9:
                                            list10 = whiteAppList10;
                                            try {
                                                whiteAppList9.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                break;
                                            } catch (XmlPullParserException e15) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                break;
                                            } catch (IOException e16) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                break;
                                            }
                                        case 10:
                                            try {
                                                whiteAppList10.add(packageName);
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                list9 = whiteAppList9;
                                                break;
                                            } catch (XmlPullParserException e17) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                list9 = whiteAppList9;
                                                break;
                                            } catch (IOException e18) {
                                                list = whiteAppList1;
                                                list2 = whiteAppList2;
                                                list3 = whiteAppList3;
                                                list4 = whiteAppList4;
                                                list5 = whiteAppList5;
                                                list6 = whiteAppList6;
                                                list7 = whiteAppList7;
                                                list8 = whiteAppList8;
                                                list9 = whiteAppList9;
                                                break;
                                            }
                                        default:
                                            list = whiteAppList1;
                                            list2 = whiteAppList2;
                                            list3 = whiteAppList3;
                                            list4 = whiteAppList4;
                                            list5 = whiteAppList5;
                                            list6 = whiteAppList6;
                                            list7 = whiteAppList7;
                                            list8 = whiteAppList8;
                                            list9 = whiteAppList9;
                                            list10 = whiteAppList10;
                                            scene_id2 = scene_id;
                                            try {
                                                Log.e(TAG, "RecordWhiteList parseXmlForWhiteBlackList err");
                                                break;
                                            } catch (XmlPullParserException e19) {
                                                break;
                                            } catch (IOException e20) {
                                                break;
                                            }
                                    }
                                }
                            } catch (XmlPullParserException e21) {
                            } catch (IOException e22) {
                            }
                        } catch (XmlPullParserException e23) {
                        } catch (IOException e24) {
                        }
                    } else {
                        xmlPullParser = parser;
                    }
                    list = whiteAppList1;
                    list2 = whiteAppList2;
                    list3 = whiteAppList3;
                    list4 = whiteAppList4;
                    list5 = whiteAppList5;
                    list6 = whiteAppList6;
                    list7 = whiteAppList7;
                    list8 = whiteAppList8;
                    list9 = whiteAppList9;
                    list10 = whiteAppList10;
                    scene_id2 = scene_id;
                    scene_id = scene_id2;
                } else {
                    xmlPullParser = parser;
                    list = whiteAppList1;
                    list2 = whiteAppList2;
                    list3 = whiteAppList3;
                    list4 = whiteAppList4;
                    list5 = whiteAppList5;
                    list6 = whiteAppList6;
                    list7 = whiteAppList7;
                    list8 = whiteAppList8;
                    list9 = whiteAppList9;
                    list10 = whiteAppList10;
                }
            } catch (XmlPullParserException e25) {
                xmlPullParser = parser;
                list = whiteAppList1;
                list2 = whiteAppList2;
                list3 = whiteAppList3;
                list4 = whiteAppList4;
                list5 = whiteAppList5;
                list6 = whiteAppList6;
                list7 = whiteAppList7;
                list8 = whiteAppList8;
                list9 = whiteAppList9;
                list10 = whiteAppList10;
                Log.e(TAG, "RecordWhiteList XmlPullParserException");
                return;
            } catch (IOException e26) {
                xmlPullParser = parser;
                list = whiteAppList1;
                list2 = whiteAppList2;
                list3 = whiteAppList3;
                list4 = whiteAppList4;
                list5 = whiteAppList5;
                list6 = whiteAppList6;
                list7 = whiteAppList7;
                list8 = whiteAppList8;
                list9 = whiteAppList9;
                list10 = whiteAppList10;
                Log.e(TAG, "RecordWhiteList IOException");
                return;
            }
        }
    }

    public boolean isValidCharSequence(CharSequence charSeq) {
        if (charSeq == null || charSeq.length() == 0) {
            return false;
        }
        return true;
    }

    private boolean getPackageNameInWhiteBlackList(String packageName) {
        if (this.mKaraokeWhiteList1 == null) {
            this.mKaraokeWhiteList1 = new ArrayList();
            this.mKaraokeWhiteList2 = new ArrayList();
            this.mKaraokeWhiteList3 = new ArrayList();
            this.mKaraokeWhiteList4 = new ArrayList();
            this.mKaraokeWhiteList5 = new ArrayList();
            this.mKaraokeWhiteList6 = new ArrayList();
            this.mKaraokeWhiteList7 = new ArrayList();
            this.mKaraokeWhiteList8 = new ArrayList();
            this.mKaraokeWhiteList9 = new ArrayList();
            this.mKaraokeWhiteList10 = new ArrayList();
            getAppInWhiteBlackList(this.mKaraokeWhiteList1, this.mKaraokeWhiteList2, this.mKaraokeWhiteList3, this.mKaraokeWhiteList4, this.mKaraokeWhiteList5, this.mKaraokeWhiteList6, this.mKaraokeWhiteList7, this.mKaraokeWhiteList8, this.mKaraokeWhiteList9, this.mKaraokeWhiteList10);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list1 =");
            stringBuilder.append(this.mKaraokeWhiteList1.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list2 =");
            stringBuilder.append(this.mKaraokeWhiteList2.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list3 =");
            stringBuilder.append(this.mKaraokeWhiteList3.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list4 =");
            stringBuilder.append(this.mKaraokeWhiteList4.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list5 =");
            stringBuilder.append(this.mKaraokeWhiteList5.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list6 =");
            stringBuilder.append(this.mKaraokeWhiteList6.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list7 =");
            stringBuilder.append(this.mKaraokeWhiteList7.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list8 =");
            stringBuilder.append(this.mKaraokeWhiteList8.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list9 =");
            stringBuilder.append(this.mKaraokeWhiteList9.toString());
            Log.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Record white list10 =");
            stringBuilder.append(this.mKaraokeWhiteList10.toString());
            Log.i(str, stringBuilder.toString());
        }
        if (AudioSystem.getParameters("record_algo_version").equals("record5_0")) {
            if (this.mKaraokeWhiteList1.contains(packageName)) {
                Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=voice_message", new Object[0]));
                AudioSystem.setParameters("RECORD_SCENE=voice_message");
                return true;
            } else if (this.mKaraokeWhiteList2.contains(packageName)) {
                Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=video_message", new Object[0]));
                AudioSystem.setParameters("RECORD_SCENE=video_message");
                return true;
            } else if (this.mKaraokeWhiteList3.contains(packageName)) {
                Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=karaoke", new Object[0]));
                AudioSystem.setParameters("RECORD_SCENE=karaoke");
                return true;
            } else if (this.mKaraokeWhiteList4.contains(packageName)) {
                Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=live_telecast", new Object[0]));
                AudioSystem.setParameters("RECORD_SCENE=live_telecast");
                return true;
            } else if (this.mKaraokeWhiteList5.contains(packageName)) {
                Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=default", new Object[0]));
                AudioSystem.setParameters("RECORD_SCENE=default");
                return true;
            } else {
                Log.i(TAG, String.format("HuaweiProcess, app %s not in Record White Lise", new Object[]{packageName}));
                return false;
            }
        } else if (this.mKaraokeWhiteList6.contains(packageName)) {
            Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=voice_message", new Object[0]));
            AudioSystem.setParameters("RECORD_SCENE=voice_message");
            return true;
        } else if (this.mKaraokeWhiteList7.contains(packageName)) {
            Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=video_message", new Object[0]));
            AudioSystem.setParameters("RECORD_SCENE=video_message");
            return true;
        } else if (this.mKaraokeWhiteList8.contains(packageName)) {
            Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=karaoke", new Object[0]));
            AudioSystem.setParameters("RECORD_SCENE=karaoke");
            return true;
        } else if (this.mKaraokeWhiteList9.contains(packageName)) {
            Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=live_telecast", new Object[0]));
            AudioSystem.setParameters("RECORD_SCENE=live_telecast");
            return true;
        } else if (this.mKaraokeWhiteList10.contains(packageName)) {
            Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=default", new Object[0]));
            AudioSystem.setParameters("RECORD_SCENE=default");
            return true;
        } else {
            Log.i(TAG, String.format("HuaweiProcess, app %s not in Record White Lise", new Object[]{packageName}));
            return false;
        }
    }

    private String getPackageNameByUid(int uid) {
        if (this.mPackMan == null) {
            Log.v(TAG, "mPackMan is null.");
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String packageName;
        String[] packages = this.mPackMan.getPackagesForUid(uid);
        if (packages == null || packages.length <= 0) {
            packageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else {
            packageName = packages[null];
        }
        return packageName;
    }

    private void notifyIwareRecordingInfo(int event, int uid) {
        if (event == 0 || event == 1) {
            HwSystemManager.notifyBackgroundMgr(getPackageNameByUid(uid), Binder.getCallingPid(), uid, 1, event);
        }
    }

    public void onRecordingConfigurationChanged(int event, int uid, int session, int source, int[] recordingInfo, String packName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("event: ");
        stringBuilder.append(event);
        stringBuilder.append(" uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" session: ");
        stringBuilder.append(session);
        stringBuilder.append(" source: ");
        stringBuilder.append(source);
        Log.v(str, stringBuilder.toString());
        notifyIwareRecordingInfo(event, uid);
        if (!MediaRecorder.isSystemOnlyAudioSource(source)) {
            List<AudioRecordingConfiguration> configsSystem = updateSnapshot(event, uid, session, source, recordingInfo);
            if (configsSystem != null) {
                synchronized (this.mClients) {
                    List<AudioRecordingConfiguration> configsPublic;
                    if (this.mHasPublicClients) {
                        configsPublic = anonymizeForPublicConsumption(configsSystem);
                    } else {
                        configsPublic = new ArrayList();
                    }
                    Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
                    while (clientIterator.hasNext()) {
                        RecMonitorClient rmc = (RecMonitorClient) clientIterator.next();
                        try {
                            if (rmc.mIsPrivileged) {
                                rmc.mDispatcherCb.dispatchRecordingConfigChange(configsSystem);
                            } else {
                                rmc.mDispatcherCb.dispatchRecordingConfigChange(configsPublic);
                            }
                        } catch (RemoteException e) {
                            Log.w(TAG, "Could not call dispatchRecordingConfigChange() on client", e);
                        }
                    }
                }
            }
        }
    }

    protected void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nRecordActivityMonitor dump time: ");
        stringBuilder.append(DateFormat.getTimeInstance().format(new Date()));
        pw.println(stringBuilder.toString());
        synchronized (this.mRecordConfigs) {
            for (AudioRecordingConfiguration conf : this.mRecordConfigs.values()) {
                conf.dump(pw);
            }
        }
        pw.println("\n");
        sEventLogger.dump(pw);
    }

    private ArrayList<AudioRecordingConfiguration> anonymizeForPublicConsumption(List<AudioRecordingConfiguration> sysConfigs) {
        ArrayList<AudioRecordingConfiguration> publicConfigs = new ArrayList();
        for (AudioRecordingConfiguration config : sysConfigs) {
            publicConfigs.add(AudioRecordingConfiguration.anonymizedCopy(config));
        }
        return publicConfigs;
    }

    void initMonitor() {
        AudioSystem.setRecordingCallback(this);
    }

    void registerRecordingCallback(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
        if (rcdb != null) {
            synchronized (this.mClients) {
                RecMonitorClient rmc = new RecMonitorClient(rcdb, isPrivileged);
                if (rmc.init()) {
                    if (!isPrivileged) {
                        this.mHasPublicClients = true;
                    }
                    this.mClients.add(rmc);
                }
            }
        }
    }

    void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        if (rcdb != null) {
            synchronized (this.mClients) {
                Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
                boolean hasPublicClients = false;
                while (clientIterator.hasNext()) {
                    RecMonitorClient rmc = (RecMonitorClient) clientIterator.next();
                    if (rcdb.equals(rmc.mDispatcherCb)) {
                        rmc.release();
                        clientIterator.remove();
                    } else if (!rmc.mIsPrivileged) {
                        hasPublicClients = true;
                    }
                }
                this.mHasPublicClients = hasPublicClients;
            }
        }
    }

    List<AudioRecordingConfiguration> getActiveRecordingConfigurations(boolean isPrivileged) {
        synchronized (this.mRecordConfigs) {
            if (isPrivileged) {
                List arrayList = new ArrayList(this.mRecordConfigs.values());
                return arrayList;
            }
            List<AudioRecordingConfiguration> configsPublic = anonymizeForPublicConsumption(new ArrayList(this.mRecordConfigs.values()));
            return configsPublic;
        }
    }

    /* JADX WARNING: Missing block: B:51:0x0169, code:
            if (r9 == false) goto L_0x0177;
     */
    /* JADX WARNING: Missing block: B:52:0x016b, code:
            r2 = new java.util.ArrayList(r1.mRecordConfigs.values());
     */
    /* JADX WARNING: Missing block: B:53:0x0177, code:
            r2 = null;
     */
    /* JADX WARNING: Missing block: B:54:0x0178, code:
            monitor-exit(r18);
     */
    /* JADX WARNING: Missing block: B:55:0x0179, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private List<AudioRecordingConfiguration> updateSnapshot(int event, int uid, int session, int source, int[] recordingInfo) {
        Throwable th;
        int i = uid;
        int i2 = session;
        HashMap hashMap = this.mRecordConfigs;
        synchronized (hashMap) {
            boolean configChanged = true;
            boolean configChanged2 = false;
            HashMap hashMap2;
            int i3;
            int i4;
            switch (event) {
                case 0:
                    hashMap2 = hashMap;
                    i3 = i2;
                    if (this.mRecordConfigs.remove(new Integer(i3)) == null) {
                        configChanged = false;
                    }
                    boolean configChanged3 = configChanged;
                    if (configChanged3) {
                        Event recordingEvent = recordingEvent;
                        i4 = i3;
                        sEventLogger.log(new RecordingEvent(event, i, i3, source, null));
                    } else {
                        i4 = i3;
                    }
                    if (this.whatsapp_session.equals(new Integer(i4))) {
                        this.whatsapp_session = Integer.valueOf(0);
                        AudioSystem.setParameters("RECORD_SCENE=off");
                        Log.i(TAG, String.format("HuaweiProcess, set RECORD_SCENE=off", new Object[0]));
                    }
                    configChanged2 = configChanged3;
                    break;
                case 1:
                    try {
                        String packageName;
                        AudioFormat clientFormat = new Builder().setEncoding(recordingInfo[0]).setChannelMask(recordingInfo[1]).setSampleRate(recordingInfo[2]).build();
                        AudioFormat deviceFormat = new Builder().setEncoding(recordingInfo[3]).setChannelMask(recordingInfo[4]).setSampleRate(recordingInfo[5]).build();
                        int patchHandle = recordingInfo[6];
                        Integer sessionKey = new Integer(i2);
                        String[] packages = this.mPackMan.getPackagesForUid(i);
                        if (packages == null || packages.length <= 0) {
                            packageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        } else {
                            packageName = packages[0];
                        }
                        String packageName2 = packageName;
                        packageName = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("uid = ");
                        stringBuilder.append(i);
                        stringBuilder.append(" packageName = ");
                        stringBuilder.append(packageName2);
                        Log.v(packageName, stringBuilder.toString());
                        AudioRecordingConfiguration updatedConfig = new AudioRecordingConfiguration(i, i2, source, clientFormat, deviceFormat, patchHandle, packageName2);
                        if (!this.mRecordConfigs.containsKey(sessionKey)) {
                            this.mRecordConfigs.put(sessionKey, updatedConfig);
                        } else if (updatedConfig.equals(this.mRecordConfigs.get(sessionKey))) {
                            configChanged = false;
                        } else {
                            this.mRecordConfigs.remove(sessionKey);
                            this.mRecordConfigs.put(sessionKey, updatedConfig);
                            configChanged = true;
                        }
                        boolean configChanged4 = configChanged;
                        if (configChanged4) {
                            Event recordingEvent2 = recordingEvent2;
                            String packageName3 = packageName2;
                            hashMap2 = hashMap;
                            i3 = i2;
                            sEventLogger.log(new RecordingEvent(event, i, i2, source, packageName3));
                            if (getPackageNameInWhiteBlackList(packageName3)) {
                                this.whatsapp_session = sessionKey;
                            }
                        } else {
                            hashMap2 = hashMap;
                            i3 = i2;
                        }
                        configChanged2 = configChanged4;
                        i4 = i3;
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                        i4 = i3;
                        throw th;
                    }
                    break;
                default:
                    hashMap2 = hashMap;
                    i4 = i2;
                    try {
                        Log.e(TAG, String.format("Unknown event %d for session %d, source %d", new Object[]{Integer.valueOf(event), Integer.valueOf(session), Integer.valueOf(source)}));
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                        break;
                    }
            }
        }
    }
}
