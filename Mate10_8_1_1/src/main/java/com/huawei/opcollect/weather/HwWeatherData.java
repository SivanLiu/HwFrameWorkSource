package com.huawei.opcollect.weather;

public class HwWeatherData {
    private int air_pm10;
    private int air_pm25;
    private int air_pnum;
    private String air_quality;
    private String air_status_desc;
    private int curr_hightemp;
    private int curr_lowtemp;
    private int current_temperature = 0;
    private int day_index;
    private int night_curr_hightemp;
    private int night_curr_lowtemp;
    private int night_current_temperature;
    private int night_weather_icon;
    private String night_weather_native_des;
    private long observation_time;
    private long sunrise_time;
    private long sunset_time;
    private int weather_icon = 100;
    private String weather_native_des;
    private String wind_directon;
    private int wind_speed;

    public String getWind_directon() {
        return this.wind_directon;
    }

    public void setWind_directon(String wind_directon) {
        this.wind_directon = wind_directon;
    }

    public int getWind_speed() {
        return this.wind_speed;
    }

    public void setWind_speed(int wind_speed) {
        this.wind_speed = wind_speed;
    }

    public int getWeather_icon() {
        return this.weather_icon;
    }

    public void setWeather_icon(int weather_icon) {
        this.weather_icon = weather_icon;
    }

    public int getDay_index() {
        return this.day_index;
    }

    public void setDay_index(int day_index) {
        this.day_index = day_index;
    }

    public long getSunrise_time() {
        return this.sunrise_time;
    }

    public void setSunrise_time(long sunrise_time) {
        this.sunrise_time = sunrise_time;
    }

    public long getSunset_time() {
        return this.sunset_time;
    }

    public void setSunset_time(long sunset_time) {
        this.sunset_time = sunset_time;
    }

    public long getObservation_time() {
        return this.observation_time;
    }

    public void setObservation_time(long observation_time) {
        this.observation_time = observation_time;
    }

    public String getWeather_native_des() {
        return this.weather_native_des;
    }

    public void setWeather_native_des(String weather_native_des) {
        this.weather_native_des = weather_native_des;
    }

    public int getCurrent_temperature() {
        return this.current_temperature;
    }

    public void setCurrent_temperature(int current_temperature) {
        this.current_temperature = current_temperature;
    }

    public int getCurr_hightemp() {
        return this.curr_hightemp;
    }

    public void setCurr_hightemp(int curr_hightemp) {
        this.curr_hightemp = curr_hightemp;
    }

    public int getCurr_lowtemp() {
        return this.curr_lowtemp;
    }

    public void setCurr_lowtemp(int curr_lowtemp) {
        this.curr_lowtemp = curr_lowtemp;
    }

    public int getNight_weather_icon() {
        return this.night_weather_icon;
    }

    public void setNight_weather_icon(int night_weather_icon) {
        this.night_weather_icon = night_weather_icon;
    }

    public String getNight_weather_native_des() {
        return this.night_weather_native_des;
    }

    public void setNight_weather_native_des(String night_weather_native_des) {
        this.night_weather_native_des = night_weather_native_des;
    }

    public int getNight_current_temperature() {
        return this.night_current_temperature;
    }

    public void setNight_current_temperature(int night_current_temperature) {
        this.night_current_temperature = night_current_temperature;
    }

    public int getNight_curr_hightemp() {
        return this.night_curr_hightemp;
    }

    public void setNight_curr_hightemp(int night_curr_hightemp) {
        this.night_curr_hightemp = night_curr_hightemp;
    }

    public int getNight_curr_lowtemp() {
        return this.night_curr_lowtemp;
    }

    public void setNight_curr_lowtemp(int night_curr_lowtemp) {
        this.night_curr_lowtemp = night_curr_lowtemp;
    }

    public String getAir_quality() {
        return this.air_quality;
    }

    public void setAir_quality(String air_quality) {
        this.air_quality = air_quality;
    }

    public String getAir_status_desc() {
        return this.air_status_desc;
    }

    public void setAir_status_desc(String air_status_desc) {
        this.air_status_desc = air_status_desc;
    }

    public int getAir_pnum() {
        return this.air_pnum;
    }

    public void setAir_pnum(int air_pnum) {
        this.air_pnum = air_pnum;
    }

    public int getAir_pm25() {
        return this.air_pm25;
    }

    public void setAir_pm25(int air_pm25) {
        this.air_pm25 = air_pm25;
    }

    public int getAir_pm10() {
        return this.air_pm10;
    }

    public void setAir_pm10(int air_pm10) {
        this.air_pm10 = air_pm10;
    }

    public String toString() {
        return "HwWeatherData{day_index=" + this.day_index + ", sunrise_time=" + this.sunrise_time + ", sunset_time=" + this.sunset_time + ", observation_time=" + this.observation_time + ", weather_icon=" + this.weather_icon + ", weather_native_des='" + this.weather_native_des + '\'' + ", current_temperature=" + this.current_temperature + ", curr_hightemp=" + this.curr_hightemp + ", curr_lowtemp=" + this.curr_lowtemp + ", night_weather_icon=" + this.night_weather_icon + ", night_weather_native_des='" + this.night_weather_native_des + '\'' + ", night_current_temperature=" + this.night_current_temperature + ", night_curr_hightemp=" + this.night_curr_hightemp + ", night_curr_lowtemp=" + this.night_curr_lowtemp + ", air_quality='" + this.air_quality + '\'' + ", air_status_desc='" + this.air_status_desc + '\'' + ", air_pnum=" + this.air_pnum + ", air_pm25=" + this.air_pm25 + ", air_pm10=" + this.air_pm10 + ", wind_directon='" + this.wind_directon + '\'' + ", wind_speed=" + this.wind_speed + '}';
    }
}
