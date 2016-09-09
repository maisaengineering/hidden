package co.samepinch.android.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Created by imaginationcoder on 9/7/16.
 */
public class CountryVO {
    String name;
    @SerializedName("dial_code")
    String phonePrefix;
    String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhonePrefix() {
        return phonePrefix;
    }

    public void setPhonePrefix(String phonePrefix) {
        this.phonePrefix = phonePrefix;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
