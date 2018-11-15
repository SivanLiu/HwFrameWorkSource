package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class MetaWeatherInfo extends AManagedObject {
    public static final Creator<MetaWeatherInfo> CREATOR = new Creator<MetaWeatherInfo>() {
        public MetaWeatherInfo createFromParcel(Parcel in) {
            return new MetaWeatherInfo(in);
        }

        public MetaWeatherInfo[] newArray(int size) {
            return new MetaWeatherInfo[size];
        }
    };
    private Float air_pressure = Float.valueOf(-1.0f);
    private String alarm_level;
    private String alarm_type;
    private String city_code;
    private Float co;
    private String humidity;
    private Integer id;
    private Integer mReservedInt;
    private String mReservedText;
    private Float no2;
    private Float o3;
    private Integer p_num;
    private String p_status_cn;
    private String p_status_en;
    private Float pm10;
    private Float pm2_5;
    private Float so2;
    private Float temperature_high;
    private Float temperature_low;
    private String time_zone;
    private Long update_time;
    private String weather_text;
    private String wind_direction;
    private Integer wind_speed;

    public MetaWeatherInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.city_code = cursor.getString(2);
        this.time_zone = cursor.getString(3);
        this.update_time = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.temperature_high = cursor.isNull(5) ? null : Float.valueOf(cursor.getFloat(5));
        this.temperature_low = cursor.isNull(6) ? null : Float.valueOf(cursor.getFloat(6));
        this.weather_text = cursor.getString(7);
        this.wind_speed = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        this.wind_direction = cursor.getString(9);
        this.p_num = cursor.isNull(10) ? null : Integer.valueOf(cursor.getInt(10));
        this.p_status_cn = cursor.getString(11);
        this.p_status_en = cursor.getString(12);
        this.pm10 = cursor.isNull(13) ? null : Float.valueOf(cursor.getFloat(13));
        this.pm2_5 = cursor.isNull(14) ? null : Float.valueOf(cursor.getFloat(14));
        this.no2 = cursor.isNull(15) ? null : Float.valueOf(cursor.getFloat(15));
        this.so2 = cursor.isNull(16) ? null : Float.valueOf(cursor.getFloat(16));
        this.o3 = cursor.isNull(17) ? null : Float.valueOf(cursor.getFloat(17));
        this.co = cursor.isNull(18) ? null : Float.valueOf(cursor.getFloat(18));
        this.humidity = cursor.getString(19);
        this.air_pressure = cursor.isNull(20) ? null : Float.valueOf(cursor.getFloat(20));
        this.alarm_type = cursor.getString(21);
        this.alarm_level = cursor.getString(22);
        if (!cursor.isNull(23)) {
            num = Integer.valueOf(cursor.getInt(23));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(24);
    }

    public MetaWeatherInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.city_code = in.readByte() == (byte) 0 ? null : in.readString();
        this.time_zone = in.readByte() == (byte) 0 ? null : in.readString();
        this.update_time = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.temperature_high = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.temperature_low = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.weather_text = in.readByte() == (byte) 0 ? null : in.readString();
        this.wind_speed = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.wind_direction = in.readByte() == (byte) 0 ? null : in.readString();
        this.p_num = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.p_status_cn = in.readByte() == (byte) 0 ? null : in.readString();
        this.p_status_en = in.readByte() == (byte) 0 ? null : in.readString();
        this.pm10 = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.pm2_5 = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.no2 = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.so2 = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.o3 = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.co = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.humidity = in.readByte() == (byte) 0 ? null : in.readString();
        this.air_pressure = in.readByte() == (byte) 0 ? null : Float.valueOf(in.readFloat());
        this.alarm_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.alarm_level = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaWeatherInfo(Integer id, String city_code, String time_zone, Long update_time, Float temperature_high, Float temperature_low, String weather_text, Integer wind_speed, String wind_direction, Integer p_num, String p_status_cn, String p_status_en, Float pm10, Float pm2_5, Float no2, Float so2, Float o3, Float co, String humidity, Float air_pressure, String alarm_type, String alarm_level, Integer mReservedInt, String mReservedText) {
        this.id = id;
        this.city_code = city_code;
        this.time_zone = time_zone;
        this.update_time = update_time;
        this.temperature_high = temperature_high;
        this.temperature_low = temperature_low;
        this.weather_text = weather_text;
        this.wind_speed = wind_speed;
        this.wind_direction = wind_direction;
        this.p_num = p_num;
        this.p_status_cn = p_status_cn;
        this.p_status_en = p_status_en;
        this.pm10 = pm10;
        this.pm2_5 = pm2_5;
        this.no2 = no2;
        this.so2 = so2;
        this.o3 = o3;
        this.co = co;
        this.humidity = humidity;
        this.air_pressure = air_pressure;
        this.alarm_type = alarm_type;
        this.alarm_level = alarm_level;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getCity_code() {
        return this.city_code;
    }

    public void setCity_code(String city_code) {
        this.city_code = city_code;
        setValue();
    }

    public String getTime_zone() {
        return this.time_zone;
    }

    public void setTime_zone(String time_zone) {
        this.time_zone = time_zone;
        setValue();
    }

    public Long getUpdate_time() {
        return this.update_time;
    }

    public void setUpdate_time(Long update_time) {
        this.update_time = update_time;
        setValue();
    }

    public Float getTemperature_high() {
        return this.temperature_high;
    }

    public void setTemperature_high(Float temperature_high) {
        this.temperature_high = temperature_high;
        setValue();
    }

    public Float getTemperature_low() {
        return this.temperature_low;
    }

    public void setTemperature_low(Float temperature_low) {
        this.temperature_low = temperature_low;
        setValue();
    }

    public String getWeather_text() {
        return this.weather_text;
    }

    public void setWeather_text(String weather_text) {
        this.weather_text = weather_text;
        setValue();
    }

    public Integer getWind_speed() {
        return this.wind_speed;
    }

    public void setWind_speed(Integer wind_speed) {
        this.wind_speed = wind_speed;
        setValue();
    }

    public String getWind_direction() {
        return this.wind_direction;
    }

    public void setWind_direction(String wind_direction) {
        this.wind_direction = wind_direction;
        setValue();
    }

    public Integer getP_num() {
        return this.p_num;
    }

    public void setP_num(Integer p_num) {
        this.p_num = p_num;
        setValue();
    }

    public String getP_status_cn() {
        return this.p_status_cn;
    }

    public void setP_status_cn(String p_status_cn) {
        this.p_status_cn = p_status_cn;
        setValue();
    }

    public String getP_status_en() {
        return this.p_status_en;
    }

    public void setP_status_en(String p_status_en) {
        this.p_status_en = p_status_en;
        setValue();
    }

    public Float getPm10() {
        return this.pm10;
    }

    public void setPm10(Float pm10) {
        this.pm10 = pm10;
        setValue();
    }

    public Float getPm2_5() {
        return this.pm2_5;
    }

    public void setPm2_5(Float pm2_5) {
        this.pm2_5 = pm2_5;
        setValue();
    }

    public Float getNo2() {
        return this.no2;
    }

    public void setNo2(Float no2) {
        this.no2 = no2;
        setValue();
    }

    public Float getSo2() {
        return this.so2;
    }

    public void setSo2(Float so2) {
        this.so2 = so2;
        setValue();
    }

    public Float getO3() {
        return this.o3;
    }

    public void setO3(Float o3) {
        this.o3 = o3;
        setValue();
    }

    public Float getCo() {
        return this.co;
    }

    public void setCo(Float co) {
        this.co = co;
        setValue();
    }

    public String getHumidity() {
        return this.humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
        setValue();
    }

    public Float getAir_pressure() {
        return this.air_pressure;
    }

    public void setAir_pressure(Float air_pressure) {
        this.air_pressure = air_pressure;
        setValue();
    }

    public String getAlarm_type() {
        return this.alarm_type;
    }

    public void setAlarm_type(String alarm_type) {
        this.alarm_type = alarm_type;
        setValue();
    }

    public String getAlarm_level() {
        return this.alarm_level;
    }

    public void setAlarm_level(String alarm_level) {
        this.alarm_level = alarm_level;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt) {
        this.mReservedInt = mReservedInt;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText) {
        this.mReservedText = mReservedText;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.city_code != null) {
            out.writeByte((byte) 1);
            out.writeString(this.city_code);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.time_zone != null) {
            out.writeByte((byte) 1);
            out.writeString(this.time_zone);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.update_time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.update_time.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.temperature_high != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.temperature_high.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.temperature_low != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.temperature_low.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.weather_text != null) {
            out.writeByte((byte) 1);
            out.writeString(this.weather_text);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.wind_speed != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.wind_speed.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.wind_direction != null) {
            out.writeByte((byte) 1);
            out.writeString(this.wind_direction);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.p_num != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.p_num.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.p_status_cn != null) {
            out.writeByte((byte) 1);
            out.writeString(this.p_status_cn);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.p_status_en != null) {
            out.writeByte((byte) 1);
            out.writeString(this.p_status_en);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.pm10 != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.pm10.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.pm2_5 != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.pm2_5.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.no2 != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.no2.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.so2 != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.so2.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.o3 != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.o3.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.co != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.co.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.humidity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.humidity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.air_pressure != null) {
            out.writeByte((byte) 1);
            out.writeFloat(this.air_pressure.floatValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.alarm_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.alarm_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.alarm_level != null) {
            out.writeByte((byte) 1);
            out.writeString(this.alarm_level);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<MetaWeatherInfo> getHelper() {
        return MetaWeatherInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaWeatherInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaWeatherInfo { id: ").append(this.id);
        sb.append(", city_code: ").append(this.city_code);
        sb.append(", time_zone: ").append(this.time_zone);
        sb.append(", update_time: ").append(this.update_time);
        sb.append(", temperature_high: ").append(this.temperature_high);
        sb.append(", temperature_low: ").append(this.temperature_low);
        sb.append(", weather_text: ").append(this.weather_text);
        sb.append(", wind_speed: ").append(this.wind_speed);
        sb.append(", wind_direction: ").append(this.wind_direction);
        sb.append(", p_num: ").append(this.p_num);
        sb.append(", p_status_cn: ").append(this.p_status_cn);
        sb.append(", p_status_en: ").append(this.p_status_en);
        sb.append(", pm10: ").append(this.pm10);
        sb.append(", pm2_5: ").append(this.pm2_5);
        sb.append(", no2: ").append(this.no2);
        sb.append(", so2: ").append(this.so2);
        sb.append(", o3: ").append(this.o3);
        sb.append(", co: ").append(this.co);
        sb.append(", humidity: ").append(this.humidity);
        sb.append(", air_pressure: ").append(this.air_pressure);
        sb.append(", alarm_type: ").append(this.alarm_type);
        sb.append(", alarm_level: ").append(this.alarm_level);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
