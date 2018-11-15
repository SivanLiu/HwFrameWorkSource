package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModel extends AManagedObject {
    public static final Creator<AiModel> CREATOR = new Creator<AiModel>() {
        public AiModel createFromParcel(Parcel in) {
            return new AiModel(in);
        }

        public AiModel[] newArray(int size) {
            return new AiModel[size];
        }
    };
    private String allowed_user;
    private String busi_domain;
    private String check_code;
    private String chip_type;
    private String cloud_update_policy;
    private Long cloud_update_time;
    private String compression_desc;
    private Long create_time;
    private String create_type;
    private String create_user;
    private String current_business;
    private String description;
    private String encrypt_desc;
    private Long expired_time;
    private String file_path;
    private String format;
    private Long id;
    private Integer is_compressed;
    private Integer is_encrypt;
    private Integer is_need_authority;
    private Integer is_none = Integer.valueOf(0);
    private Integer is_preset_model;
    private String key;
    private Long last_update_time;
    private String last_update_type;
    private Integer model_type;
    private String name;
    private Integer none_type = Integer.valueOf(0);
    private Long origin_id;
    private Long parent_id;
    private String platform;
    private Integer priority;
    private String region;
    private String reserved_1;
    private String reserved_2;
    private String reserved_attributes;
    private String resid;
    private String serial_number;
    private Long size;
    private String storage_type;
    private String suffix;
    private String tech_domain;
    private Long top_model_id;
    private String usable_condition;
    private Long version;

    public AiModel(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.key = cursor.getString(2);
        this.origin_id = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.serial_number = cursor.getString(4);
        this.name = cursor.getString(5);
        this.description = cursor.getString(6);
        this.file_path = cursor.getString(7);
        this.model_type = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        this.parent_id = cursor.isNull(9) ? null : Long.valueOf(cursor.getLong(9));
        this.top_model_id = cursor.isNull(10) ? null : Long.valueOf(cursor.getLong(10));
        this.is_preset_model = cursor.isNull(11) ? null : Integer.valueOf(cursor.getInt(11));
        this.platform = cursor.getString(12);
        this.tech_domain = cursor.getString(13);
        this.busi_domain = cursor.getString(14);
        this.region = cursor.getString(15);
        this.chip_type = cursor.getString(16);
        this.version = cursor.isNull(17) ? null : Long.valueOf(cursor.getLong(17));
        this.format = cursor.getString(18);
        this.storage_type = cursor.getString(19);
        this.size = cursor.isNull(20) ? null : Long.valueOf(cursor.getLong(20));
        this.suffix = cursor.getString(21);
        this.create_type = cursor.getString(22);
        this.create_user = cursor.getString(23);
        this.create_time = cursor.isNull(24) ? null : Long.valueOf(cursor.getLong(24));
        this.expired_time = cursor.isNull(25) ? null : Long.valueOf(cursor.getLong(25));
        this.last_update_time = cursor.isNull(26) ? null : Long.valueOf(cursor.getLong(26));
        this.last_update_type = cursor.getString(27);
        this.cloud_update_time = cursor.isNull(28) ? null : Long.valueOf(cursor.getLong(28));
        this.is_need_authority = cursor.isNull(29) ? null : Integer.valueOf(cursor.getInt(29));
        this.is_encrypt = cursor.isNull(30) ? null : Integer.valueOf(cursor.getInt(30));
        this.is_compressed = cursor.isNull(31) ? null : Integer.valueOf(cursor.getInt(31));
        this.encrypt_desc = cursor.getString(32);
        this.compression_desc = cursor.getString(33);
        this.reserved_attributes = cursor.getString(34);
        this.cloud_update_policy = cursor.getString(35);
        this.allowed_user = cursor.getString(36);
        this.current_business = cursor.getString(37);
        this.usable_condition = cursor.getString(38);
        this.is_none = cursor.isNull(39) ? null : Integer.valueOf(cursor.getInt(39));
        this.none_type = cursor.isNull(40) ? null : Integer.valueOf(cursor.getInt(40));
        if (!cursor.isNull(41)) {
            num = Integer.valueOf(cursor.getInt(41));
        }
        this.priority = num;
        this.check_code = cursor.getString(42);
        this.reserved_1 = cursor.getString(43);
        this.reserved_2 = cursor.getString(44);
        this.resid = cursor.getString(45);
    }

    public AiModel(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.key = in.readByte() == (byte) 0 ? null : in.readString();
        this.origin_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.serial_number = in.readByte() == (byte) 0 ? null : in.readString();
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.description = in.readByte() == (byte) 0 ? null : in.readString();
        this.file_path = in.readByte() == (byte) 0 ? null : in.readString();
        this.model_type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.parent_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.top_model_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.is_preset_model = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.platform = in.readByte() == (byte) 0 ? null : in.readString();
        this.tech_domain = in.readByte() == (byte) 0 ? null : in.readString();
        this.busi_domain = in.readByte() == (byte) 0 ? null : in.readString();
        this.region = in.readByte() == (byte) 0 ? null : in.readString();
        this.chip_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.version = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.format = in.readByte() == (byte) 0 ? null : in.readString();
        this.storage_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.size = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.suffix = in.readByte() == (byte) 0 ? null : in.readString();
        this.create_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.create_user = in.readByte() == (byte) 0 ? null : in.readString();
        this.create_time = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.expired_time = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.last_update_time = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.last_update_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.cloud_update_time = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.is_need_authority = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.is_encrypt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.is_compressed = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.encrypt_desc = in.readByte() == (byte) 0 ? null : in.readString();
        this.compression_desc = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved_attributes = in.readByte() == (byte) 0 ? null : in.readString();
        this.cloud_update_policy = in.readByte() == (byte) 0 ? null : in.readString();
        this.allowed_user = in.readByte() == (byte) 0 ? null : in.readString();
        this.current_business = in.readByte() == (byte) 0 ? null : in.readString();
        this.usable_condition = in.readByte() == (byte) 0 ? null : in.readString();
        this.is_none = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.none_type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.priority = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.check_code = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved_1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved_2 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.resid = str;
    }

    private AiModel(Long id, String key, Long origin_id, String serial_number, String name, String description, String file_path, Integer model_type, Long parent_id, Long top_model_id, Integer is_preset_model, String platform, String tech_domain, String busi_domain, String region, String chip_type, Long version, String format, String storage_type, Long size, String suffix, String create_type, String create_user, Long create_time, Long expired_time, Long last_update_time, String last_update_type, Long cloud_update_time, Integer is_need_authority, Integer is_encrypt, Integer is_compressed, String encrypt_desc, String compression_desc, String reserved_attributes, String cloud_update_policy, String allowed_user, String current_business, String usable_condition, Integer is_none, Integer none_type, Integer priority, String check_code, String reserved_1, String reserved_2, String resid) {
        this.id = id;
        this.key = key;
        this.origin_id = origin_id;
        this.serial_number = serial_number;
        this.name = name;
        this.description = description;
        this.file_path = file_path;
        this.model_type = model_type;
        this.parent_id = parent_id;
        this.top_model_id = top_model_id;
        this.is_preset_model = is_preset_model;
        this.platform = platform;
        this.tech_domain = tech_domain;
        this.busi_domain = busi_domain;
        this.region = region;
        this.chip_type = chip_type;
        this.version = version;
        this.format = format;
        this.storage_type = storage_type;
        this.size = size;
        this.suffix = suffix;
        this.create_type = create_type;
        this.create_user = create_user;
        this.create_time = create_time;
        this.expired_time = expired_time;
        this.last_update_time = last_update_time;
        this.last_update_type = last_update_type;
        this.cloud_update_time = cloud_update_time;
        this.is_need_authority = is_need_authority;
        this.is_encrypt = is_encrypt;
        this.is_compressed = is_compressed;
        this.encrypt_desc = encrypt_desc;
        this.compression_desc = compression_desc;
        this.reserved_attributes = reserved_attributes;
        this.cloud_update_policy = cloud_update_policy;
        this.allowed_user = allowed_user;
        this.current_business = current_business;
        this.usable_condition = usable_condition;
        this.is_none = is_none;
        this.none_type = none_type;
        this.priority = priority;
        this.check_code = check_code;
        this.reserved_1 = reserved_1;
        this.reserved_2 = reserved_2;
        this.resid = resid;
    }

    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
        setValue();
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
        setValue();
    }

    public Long getOrigin_id() {
        return this.origin_id;
    }

    public void setOrigin_id(Long origin_id) {
        this.origin_id = origin_id;
        setValue();
    }

    public String getSerial_number() {
        return this.serial_number;
    }

    public void setSerial_number(String serial_number) {
        this.serial_number = serial_number;
        setValue();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
        setValue();
    }

    public String getFile_path() {
        return this.file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
        setValue();
    }

    public Integer getModel_type() {
        return this.model_type;
    }

    public void setModel_type(Integer model_type) {
        this.model_type = model_type;
        setValue();
    }

    public Long getParent_id() {
        return this.parent_id;
    }

    public void setParent_id(Long parent_id) {
        this.parent_id = parent_id;
        setValue();
    }

    public Long getTop_model_id() {
        return this.top_model_id;
    }

    public void setTop_model_id(Long top_model_id) {
        this.top_model_id = top_model_id;
        setValue();
    }

    public Integer getIs_preset_model() {
        return this.is_preset_model;
    }

    public void setIs_preset_model(Integer is_preset_model) {
        this.is_preset_model = is_preset_model;
        setValue();
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
        setValue();
    }

    public String getTech_domain() {
        return this.tech_domain;
    }

    public void setTech_domain(String tech_domain) {
        this.tech_domain = tech_domain;
        setValue();
    }

    public String getBusi_domain() {
        return this.busi_domain;
    }

    public void setBusi_domain(String busi_domain) {
        this.busi_domain = busi_domain;
        setValue();
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
        setValue();
    }

    public String getChip_type() {
        return this.chip_type;
    }

    public void setChip_type(String chip_type) {
        this.chip_type = chip_type;
        setValue();
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
        setValue();
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
        setValue();
    }

    public String getStorage_type() {
        return this.storage_type;
    }

    public void setStorage_type(String storage_type) {
        this.storage_type = storage_type;
        setValue();
    }

    public Long getSize() {
        return this.size;
    }

    public void setSize(Long size) {
        this.size = size;
        setValue();
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
        setValue();
    }

    public String getCreate_type() {
        return this.create_type;
    }

    public void setCreate_type(String create_type) {
        this.create_type = create_type;
        setValue();
    }

    public String getCreate_user() {
        return this.create_user;
    }

    public void setCreate_user(String create_user) {
        this.create_user = create_user;
        setValue();
    }

    public Long getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(Long create_time) {
        this.create_time = create_time;
        setValue();
    }

    public Long getExpired_time() {
        return this.expired_time;
    }

    public void setExpired_time(Long expired_time) {
        this.expired_time = expired_time;
        setValue();
    }

    public Long getLast_update_time() {
        return this.last_update_time;
    }

    public void setLast_update_time(Long last_update_time) {
        this.last_update_time = last_update_time;
        setValue();
    }

    public String getLast_update_type() {
        return this.last_update_type;
    }

    public void setLast_update_type(String last_update_type) {
        this.last_update_type = last_update_type;
        setValue();
    }

    public Long getCloud_update_time() {
        return this.cloud_update_time;
    }

    public void setCloud_update_time(Long cloud_update_time) {
        this.cloud_update_time = cloud_update_time;
        setValue();
    }

    public Integer getIs_need_authority() {
        return this.is_need_authority;
    }

    public void setIs_need_authority(Integer is_need_authority) {
        this.is_need_authority = is_need_authority;
        setValue();
    }

    public Integer getIs_encrypt() {
        return this.is_encrypt;
    }

    public void setIs_encrypt(Integer is_encrypt) {
        this.is_encrypt = is_encrypt;
        setValue();
    }

    public Integer getIs_compressed() {
        return this.is_compressed;
    }

    public void setIs_compressed(Integer is_compressed) {
        this.is_compressed = is_compressed;
        setValue();
    }

    public String getEncrypt_desc() {
        return this.encrypt_desc;
    }

    public void setEncrypt_desc(String encrypt_desc) {
        this.encrypt_desc = encrypt_desc;
        setValue();
    }

    public String getCompression_desc() {
        return this.compression_desc;
    }

    public void setCompression_desc(String compression_desc) {
        this.compression_desc = compression_desc;
        setValue();
    }

    public String getReserved_attributes() {
        return this.reserved_attributes;
    }

    public void setReserved_attributes(String reserved_attributes) {
        this.reserved_attributes = reserved_attributes;
        setValue();
    }

    public String getCloud_update_policy() {
        return this.cloud_update_policy;
    }

    public void setCloud_update_policy(String cloud_update_policy) {
        this.cloud_update_policy = cloud_update_policy;
        setValue();
    }

    public String getAllowed_user() {
        return this.allowed_user;
    }

    public void setAllowed_user(String allowed_user) {
        this.allowed_user = allowed_user;
        setValue();
    }

    public String getCurrent_business() {
        return this.current_business;
    }

    public void setCurrent_business(String current_business) {
        this.current_business = current_business;
        setValue();
    }

    public String getUsable_condition() {
        return this.usable_condition;
    }

    public void setUsable_condition(String usable_condition) {
        this.usable_condition = usable_condition;
        setValue();
    }

    public Integer getIs_none() {
        return this.is_none;
    }

    public void setIs_none(Integer is_none) {
        this.is_none = is_none;
        setValue();
    }

    public Integer getNone_type() {
        return this.none_type;
    }

    public void setNone_type(Integer none_type) {
        this.none_type = none_type;
        setValue();
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
        setValue();
    }

    public String getCheck_code() {
        return this.check_code;
    }

    public void setCheck_code(String check_code) {
        this.check_code = check_code;
        setValue();
    }

    public String getReserved_1() {
        return this.reserved_1;
    }

    public void setReserved_1(String reserved_1) {
        this.reserved_1 = reserved_1;
        setValue();
    }

    public String getReserved_2() {
        return this.reserved_2;
    }

    public void setReserved_2(String reserved_2) {
        this.reserved_2 = reserved_2;
        setValue();
    }

    public String getResid() {
        return this.resid;
    }

    public void setResid(String resid) {
        this.resid = resid;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.key != null) {
            out.writeByte((byte) 1);
            out.writeString(this.key);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.origin_id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.origin_id.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.serial_number != null) {
            out.writeByte((byte) 1);
            out.writeString(this.serial_number);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.description != null) {
            out.writeByte((byte) 1);
            out.writeString(this.description);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.file_path != null) {
            out.writeByte((byte) 1);
            out.writeString(this.file_path);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.model_type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.model_type.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.parent_id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.parent_id.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.top_model_id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.top_model_id.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.is_preset_model != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.is_preset_model.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.platform != null) {
            out.writeByte((byte) 1);
            out.writeString(this.platform);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tech_domain != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tech_domain);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.busi_domain != null) {
            out.writeByte((byte) 1);
            out.writeString(this.busi_domain);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.region != null) {
            out.writeByte((byte) 1);
            out.writeString(this.region);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.chip_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.chip_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.version != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.version.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.format != null) {
            out.writeByte((byte) 1);
            out.writeString(this.format);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.storage_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.storage_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.size != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.size.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.suffix != null) {
            out.writeByte((byte) 1);
            out.writeString(this.suffix);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.create_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.create_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.create_user != null) {
            out.writeByte((byte) 1);
            out.writeString(this.create_user);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.create_time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.create_time.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.expired_time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.expired_time.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.last_update_time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.last_update_time.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.last_update_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.last_update_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.cloud_update_time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.cloud_update_time.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.is_need_authority != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.is_need_authority.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.is_encrypt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.is_encrypt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.is_compressed != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.is_compressed.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.encrypt_desc != null) {
            out.writeByte((byte) 1);
            out.writeString(this.encrypt_desc);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.compression_desc != null) {
            out.writeByte((byte) 1);
            out.writeString(this.compression_desc);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved_attributes != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved_attributes);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.cloud_update_policy != null) {
            out.writeByte((byte) 1);
            out.writeString(this.cloud_update_policy);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.allowed_user != null) {
            out.writeByte((byte) 1);
            out.writeString(this.allowed_user);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.current_business != null) {
            out.writeByte((byte) 1);
            out.writeString(this.current_business);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.usable_condition != null) {
            out.writeByte((byte) 1);
            out.writeString(this.usable_condition);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.is_none != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.is_none.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.none_type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.none_type.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.priority != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.priority.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.check_code != null) {
            out.writeByte((byte) 1);
            out.writeString(this.check_code);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved_1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved_1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved_2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved_2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.resid != null) {
            out.writeByte((byte) 1);
            out.writeString(this.resid);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModel> getHelper() {
        return AiModelHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModel";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModel { id: ").append(this.id);
        sb.append(", key: ").append(this.key);
        sb.append(", origin_id: ").append(this.origin_id);
        sb.append(", serial_number: ").append(this.serial_number);
        sb.append(", name: ").append(this.name);
        sb.append(", description: ").append(this.description);
        sb.append(", file_path: ").append(this.file_path);
        sb.append(", model_type: ").append(this.model_type);
        sb.append(", parent_id: ").append(this.parent_id);
        sb.append(", top_model_id: ").append(this.top_model_id);
        sb.append(", is_preset_model: ").append(this.is_preset_model);
        sb.append(", platform: ").append(this.platform);
        sb.append(", tech_domain: ").append(this.tech_domain);
        sb.append(", busi_domain: ").append(this.busi_domain);
        sb.append(", region: ").append(this.region);
        sb.append(", chip_type: ").append(this.chip_type);
        sb.append(", version: ").append(this.version);
        sb.append(", format: ").append(this.format);
        sb.append(", storage_type: ").append(this.storage_type);
        sb.append(", size: ").append(this.size);
        sb.append(", suffix: ").append(this.suffix);
        sb.append(", create_type: ").append(this.create_type);
        sb.append(", create_user: ").append(this.create_user);
        sb.append(", create_time: ").append(this.create_time);
        sb.append(", expired_time: ").append(this.expired_time);
        sb.append(", last_update_time: ").append(this.last_update_time);
        sb.append(", last_update_type: ").append(this.last_update_type);
        sb.append(", cloud_update_time: ").append(this.cloud_update_time);
        sb.append(", is_need_authority: ").append(this.is_need_authority);
        sb.append(", is_encrypt: ").append(this.is_encrypt);
        sb.append(", is_compressed: ").append(this.is_compressed);
        sb.append(", encrypt_desc: ").append(this.encrypt_desc);
        sb.append(", compression_desc: ").append(this.compression_desc);
        sb.append(", reserved_attributes: ").append(this.reserved_attributes);
        sb.append(", cloud_update_policy: ").append(this.cloud_update_policy);
        sb.append(", allowed_user: ").append(this.allowed_user);
        sb.append(", current_business: ").append(this.current_business);
        sb.append(", usable_condition: ").append(this.usable_condition);
        sb.append(", is_none: ").append(this.is_none);
        sb.append(", none_type: ").append(this.none_type);
        sb.append(", priority: ").append(this.priority);
        sb.append(", check_code: ").append(this.check_code);
        sb.append(", reserved_1: ").append(this.reserved_1);
        sb.append(", reserved_2: ").append(this.reserved_2);
        sb.append(", resid: ").append(this.resid);
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
        return "0.0.8";
    }

    public int getDatabaseVersionCode() {
        return 8;
    }

    public String getEntityVersion() {
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
