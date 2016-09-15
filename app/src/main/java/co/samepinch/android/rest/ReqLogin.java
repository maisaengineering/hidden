package co.samepinch.android.rest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import co.samepinch.android.app.helpers.Utils;

/**
 * Created by imaginationcoder on 6/30/15.
 */
public class ReqLogin extends RestBase<Map<String, String>> implements Serializable {
    transient String email;
    transient String authKey;
    transient String country;
    transient String password;
    transient String deviceToken;
    transient String platform;
    transient String isNewUser;


    @Override
    public Map<String, String> getBody() {
        return body;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String isNewUser() {
        return isNewUser;
    }

    public void setIsNewUser(String isNewUser) {
        this.isNewUser = isNewUser;
    }

    public ReqLogin build() {
        Map<String, String> body = new HashMap<>();
        body.put("email", Utils.emptyIfNull(email));
        body.put("password", Utils.emptyIfNull(password));
        body.put("platform", Utils.emptyIfNull(platform));
        body.put("auth_key", Utils.emptyIfNull(authKey));
        body.put("country", Utils.emptyIfNull(country));
        body.put("isNewUser", Utils.emptyIfNull(isNewUser));
        this.setBody(body);

        return this;
    }
}
