package com.android.server.emcom.xengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XEngineConfigInfo {
    Map<String, String> autoGrabParams = new HashMap();
    int grade;
    public HicomFeaturesInfo hicomFeaturesInfo;
    boolean isForeground;
    int mUid;
    String mVersion;
    int mainCardPsStatus;
    String packageName;
    List<TimePairInfo> timeInfos = new ArrayList();
    List<BoostViewInfo> viewInfos = new ArrayList();

    public static class BoostViewInfo {
        String container;
        int grade;
        String keyword;
        int mainCardPsStatus;
        int maxCount;
        int maxDepth;
        String rootView;
        String version;

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("{version=").append(this.version).append(", ").append("rootView=").append(this.rootView).append(", ").append("container=").append(this.container).append(", ").append("keyword=").append(this.keyword).append(", ").append("maxDepth=").append(this.maxDepth).append(", ").append("maxCount=").append(this.maxCount).append(", ").append("grade=").append(this.grade).append(", ").append("mainCardPsStatus=").append(this.mainCardPsStatus).append("}");
            return buffer.toString();
        }
    }

    public static class HicomFeaturesInfo {
        public int maxGrade;
        public int minGrade;
        public int multiPath;
        public int multiPathType;
        public int objectiveDelay;
        Proxy proxy;
        public int wifiMode;

        public static class Proxy {
            HttpAcc httpAcc;

            public static class HttpAcc {
                boolean enableMpip;
                int flowNum;
                int mode;
                int port;
                boolean randomMpip;
                float threload;

                public int getMode() {
                    return this.mode;
                }

                public void setMode(int mode) {
                    this.mode = mode;
                }

                public int getPort() {
                    return this.port;
                }

                public void setPort(int port) {
                    this.port = port;
                }

                public int getFlowNum() {
                    return this.flowNum;
                }

                public void setFlowNum(int flowNum) {
                    this.flowNum = flowNum;
                }

                public boolean getEnableMpip() {
                    return this.enableMpip;
                }

                public void setEnableMpip(boolean enableMpip) {
                    this.enableMpip = enableMpip;
                }

                public float getThreload() {
                    return this.threload;
                }

                public void setThreload(float threload) {
                    this.threload = threload;
                }

                public boolean getRandomMpip() {
                    return this.randomMpip;
                }

                public void setRandomMpip(boolean randomMpip) {
                    this.randomMpip = randomMpip;
                }

                public String toString() {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("{mode=").append(this.mode).append(", ").append("port=").append(this.port).append(", ").append("flowNum=").append(this.flowNum).append(", ").append("enableMpip=").append(this.enableMpip).append(", ").append("randomMpip=").append(this.randomMpip).append(", ").append("threload=").append(this.threload).append("}");
                    return buffer.toString();
                }
            }

            public HttpAcc getHttpAcc() {
                return this.httpAcc;
            }

            public void setHttpAcc(HttpAcc httpAcc) {
                this.httpAcc = httpAcc;
            }
        }

        public Proxy getProxy() {
            return this.proxy;
        }

        public void setProxy(Proxy proxy) {
            this.proxy = proxy;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("multiPath=").append(this.multiPath).append(", ").append("multiPathType=").append(this.multiPathType).append(", ").append("wifiMode=").append(this.wifiMode).append(", ").append("objectiveDelay=").append(this.objectiveDelay).append("maxGrade=").append(this.maxGrade).append(", ").append("minGrade=").append(this.minGrade).append("}");
            return buffer.toString();
        }
    }

    public static class HicomMpipInfo {
        public int mUid;
        public int multiPathType;

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("mUid=").append(this.mUid).append(", ").append("multiPathType=").append(this.multiPathType).append("}");
            return buffer.toString();
        }
    }

    public static class TimePairInfo {
        String endTime;
        String startTime;

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("{startTime=").append(this.startTime).append(", ").append("endTime=").append(this.endTime).append("}");
            return buffer.toString();
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

    public HicomFeaturesInfo getHicomFeaturesInfo() {
        return this.hicomFeaturesInfo;
    }

    public int getUid() {
        return this.mUid;
    }

    public void setUid(int uid) {
        this.mUid = uid;
    }

    public String getmVersion() {
        return this.mVersion;
    }

    public void setmVersion(String mVersion) {
        this.mVersion = mVersion;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("packageName=").append(this.packageName).append(", ").append("grade=").append(this.grade).append(", ").append("isForeground=").append(this.isForeground).append(", ").append("mainCardPsStatus=").append(this.mainCardPsStatus).append(", ").append("autoGrabParams=").append(this.autoGrabParams).append(", ").append("viewInfos=").append(this.viewInfos).append(", ").append("hicomFeaturesInfos=").append(this.hicomFeaturesInfo).append(", ").append("timeInfos=").append(this.timeInfos);
        return buffer.toString();
    }
}
