package com.android.server.rms.iaware.memory.data.content;

import android.rms.iaware.AwareLog;
import android.rms.iaware.DataNormalizer;
import android.util.ArrayMap;
import java.util.Arrays;

public class AttrSegments {
    private static final String TAG = "AwareMem_AttrSegm";
    private Integer mEvent;
    private ArrayMap<String, ArrayMap<String, String>> mSegments;

    public static final class Builder {
        private String[] collectList = null;
        private Integer event = null;

        public void addCollectData(String collectData) {
            try {
                this.collectList = DataNormalizer.getCollectArray(collectData);
                this.event = AttrSegments.parseEvent(this.collectList);
            } catch (Exception e) {
                String str = AttrSegments.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" Fail to parse collectData=");
                stringBuilder.append(collectData);
                AwareLog.e(str, stringBuilder.toString());
                this.collectList = null;
                this.event = null;
            }
        }

        public AttrSegments build() {
            AttrSegments segments = new AttrSegments();
            if (this.collectList == null || this.collectList.length < 1 || this.event == null) {
                AwareLog.e(AttrSegments.TAG, " Invalid collectData, or event");
                return segments;
            }
            int length = this.collectList.length;
            for (int i = 1; i < length; i++) {
                String[] collects = DataNormalizer.parseCollect(this.collectList[i]);
                if (collects != null) {
                    String[] conditionArray = DataNormalizer.getConditionArray(collects[1]);
                    if (conditionArray != null) {
                        ArrayMap<String, ArrayMap<String, String>> segmentMap = segments.getSegmentMap();
                        ArrayMap<String, String> conditionMap = (ArrayMap) segmentMap.get(collects[0]);
                        if (conditionMap == null) {
                            conditionMap = new ArrayMap();
                            segmentMap.put(collects[0], conditionMap);
                        }
                        for (String[] conditions : conditionArray) {
                            String[] conditions2 = DataNormalizer.parseCondition(conditions2);
                            if (conditions2 != null) {
                                conditionMap.put(conditions2[0], conditions2[1]);
                            }
                        }
                    }
                }
            }
            segments.mEvent = this.event;
            return segments;
        }
    }

    private AttrSegments() {
        this.mEvent = null;
        this.mSegments = new ArrayMap();
    }

    public boolean isValid() {
        return this.mEvent != null;
    }

    public Integer getEvent() {
        return this.mEvent;
    }

    public ArrayMap<String, String> getSegment(String key) {
        return (ArrayMap) this.mSegments.get(key);
    }

    private ArrayMap<String, ArrayMap<String, String>> getSegmentMap() {
        return this.mSegments;
    }

    private static Integer parseEvent(String[] collectList) {
        Integer eventId = null;
        if (collectList == null || collectList.length < 1) {
            AwareLog.e(TAG, " parseEvent collectList error");
            return null;
        }
        String[] collects = DataNormalizer.parseCollect(collectList[0]);
        if (collects == null || collects.length < 2) {
            AwareLog.e(TAG, " parseEvent collects error");
            return null;
        }
        String[] conditionArray = DataNormalizer.getConditionArray(collects[1]);
        if (conditionArray == null || conditionArray.length < 1) {
            AwareLog.e(TAG, " parseEvent conditionArray error");
            return null;
        }
        String[] eventArray = DataNormalizer.parseCondition(conditionArray[0]);
        if (eventArray == null || eventArray.length < 2) {
            AwareLog.e(TAG, " parseEvent eventArray error");
            return null;
        }
        try {
            eventId = Integer.valueOf(Integer.parseInt(eventArray[1]));
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" Parse event NumberFormatException. data=");
            stringBuilder.append(Arrays.toString(collectList));
            AwareLog.e(str, stringBuilder.toString());
        }
        return eventId;
    }
}
