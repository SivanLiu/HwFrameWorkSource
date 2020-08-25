package com.huawei.opcollect.weather;

public class HwWeatherData {
    private int airPm10;
    private int airPm25;
    private int airPnum;
    private String airQuality;
    private String airStatusDesc;
    private int currHighTemp;
    private int currLowTemp;
    private int currentTemperature = 0;
    private int dayIndex;
    private int nightCurrHighTemp;
    private int nightCurrLowTemp;
    private int nightCurrentTemperature;
    private int nightWeatherIcon;
    private String nightWeatherNativeDes;
    private long observationTime;
    private long sunriseTime;
    private long sunsetTime;
    private int weatherIcon = 100;
    private String weatherNativeDes;
    private String windDirection;
    private int windSpeed;

    public int getDayIndex() {
        return this.dayIndex;
    }

    public void setDayIndex(int dayIndex2) {
        this.dayIndex = dayIndex2;
    }

    public long getSunsetTime() {
        return this.sunsetTime;
    }

    public void setSunsetTime(long sunsetTime2) {
        this.sunsetTime = sunsetTime2;
    }

    public long getSunriseTime() {
        return this.sunriseTime;
    }

    public void setSunriseTime(long sunriseTime2) {
        this.sunriseTime = sunriseTime2;
    }

    public long getObservationTime() {
        return this.observationTime;
    }

    public void setObservationTime(long observationTime2) {
        this.observationTime = observationTime2;
    }

    public int getWeatherIcon() {
        return this.weatherIcon;
    }

    public void setWeatherIcon(int weatherIcon2) {
        this.weatherIcon = weatherIcon2;
    }

    public String getWeatherNativeDes() {
        return this.weatherNativeDes;
    }

    public void setWeatherNativeDes(String weatherNativeDes2) {
        this.weatherNativeDes = weatherNativeDes2;
    }

    public int getCurrentTemperature() {
        return this.currentTemperature;
    }

    public void setCurrentTemperature(int currentTemperature2) {
        this.currentTemperature = currentTemperature2;
    }

    public int getCurrHighTemp() {
        return this.currHighTemp;
    }

    public void setCurrHighTemp(int currHighTemp2) {
        this.currHighTemp = currHighTemp2;
    }

    public int getCurrLowTemp() {
        return this.currLowTemp;
    }

    public void setCurrLowTemp(int currLowTemp2) {
        this.currLowTemp = currLowTemp2;
    }

    public int getNightWeatherIcon() {
        return this.nightWeatherIcon;
    }

    public void setNightWeatherIcon(int nightWeatherIcon2) {
        this.nightWeatherIcon = nightWeatherIcon2;
    }

    public String getNightWeatherNativeDes() {
        return this.nightWeatherNativeDes;
    }

    public void setNightWeatherNativeDes(String nightWeatherNativeDes2) {
        this.nightWeatherNativeDes = nightWeatherNativeDes2;
    }

    public int getNightCurrentTemperature() {
        return this.nightCurrentTemperature;
    }

    public void setNightCurrentTemperature(int nightCurrentTemperature2) {
        this.nightCurrentTemperature = nightCurrentTemperature2;
    }

    public int getNightCurrHighTemp() {
        return this.nightCurrHighTemp;
    }

    public void setNightCurrHighTemp(int nightCurrHighTemp2) {
        this.nightCurrHighTemp = nightCurrHighTemp2;
    }

    public int getNightCurrLowTemp() {
        return this.nightCurrLowTemp;
    }

    public void setNightCurrLowTemp(int nightCurrLowTemp2) {
        this.nightCurrLowTemp = nightCurrLowTemp2;
    }

    public String getAirQuality() {
        return this.airQuality;
    }

    public void setAirQuality(String airQuality2) {
        this.airQuality = airQuality2;
    }

    public String getAirStatusDesc() {
        return this.airStatusDesc;
    }

    public void setAirStatusDesc(String airStatusDesc2) {
        this.airStatusDesc = airStatusDesc2;
    }

    public int getAirPnum() {
        return this.airPnum;
    }

    public void setAirPnum(int airPnum2) {
        this.airPnum = airPnum2;
    }

    public int getAirPm25() {
        return this.airPm25;
    }

    public void setAirPm25(int airPm252) {
        this.airPm25 = airPm252;
    }

    public int getAirPm10() {
        return this.airPm10;
    }

    public void setAirPm10(int airPm102) {
        this.airPm10 = airPm102;
    }

    public String getWindDirection() {
        return this.windDirection;
    }

    public void setWindDirection(String windDirection2) {
        this.windDirection = windDirection2;
    }

    public int getWindSpeed() {
        return this.windSpeed;
    }

    public void setWindSpeed(int windSpeed2) {
        this.windSpeed = windSpeed2;
    }

    public String toString() {
        return "HwWeatherData{dayIndex=" + this.dayIndex + ", sunriseTime=" + this.sunriseTime + ", sunsetTime=" + this.sunsetTime + ", observationTime=" + this.observationTime + ", weatherIcon=" + this.weatherIcon + ", weatherNativeDes='" + this.weatherNativeDes + '\'' + ", currentTemperature=" + this.currentTemperature + ", currHighTemp=" + this.currHighTemp + ", currLowTemp=" + this.currLowTemp + ", nightWeatherIcon=" + this.nightWeatherIcon + ", nightWeatherNativeDes='" + this.nightWeatherNativeDes + '\'' + ", nightCurrentTemperature=" + this.nightCurrentTemperature + ", nightCurrHighTemp=" + this.nightCurrHighTemp + ", nightCurrLowTemp=" + this.nightCurrLowTemp + ", airQuality='" + this.airQuality + '\'' + ", airStatusDesc='" + this.airStatusDesc + '\'' + ", airPnum=" + this.airPnum + ", airPm25=" + this.airPm25 + ", airPm10=" + this.airPm10 + ", windDirection='" + this.windDirection + '\'' + ", windSpeed=" + this.windSpeed + '}';
    }
}
