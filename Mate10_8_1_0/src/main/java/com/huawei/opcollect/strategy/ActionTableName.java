package com.huawei.opcollect.strategy;

import com.huawei.opcollect.utils.OPCollectConstant;

public enum ActionTableName {
    RAW_DEVICE_INFO(OPCollectConstant.DEVICE_ACTION_NAME),
    RAW_USER_INFO_STATISTIC(OPCollectConstant.USER_INFO_ACTION_NAME),
    RAW_APP_PROBE(OPCollectConstant.APP_PROBE_ACTION_NAME),
    RAW_TRAIN_FLIGHT_TICK_INFO(OPCollectConstant.TRIP_ACTION_NAME),
    RAW_HOTEL_INFO(OPCollectConstant.HOTEL_ACTION_NAME),
    RAW_MEDIA_APP_STASTIC(OPCollectConstant.MEDIA_ACTION_NAME),
    RAW_CELL_RECORD("RawCellRecord"),
    RAW_LOCATION_RECORD(OPCollectConstant.LOCATION_ACTION_NAME),
    RAW_SYSTEM_EVENT("RawSysEvent"),
    RAW_AR_STATUS(OPCollectConstant.AR_ACTION_NAME),
    RAW_WEATHER_INFO(OPCollectConstant.WEATHER_ACTION_NAME),
    RAW_POSITION_STATE("RawPositionState"),
    RAW_FG_APP_EVENT("RawFgAPPEvent"),
    RAW_APP_PROBE_COUNT(OPCollectConstant.APP_PROBE_COUNT_ACTION_NAME);
    
    private String value;

    private ActionTableName(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    void setValue(String value) {
        this.value = value;
    }
}
