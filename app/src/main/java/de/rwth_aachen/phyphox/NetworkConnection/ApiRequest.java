package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class ApiRequest {
    private String language;
    private String user_id;

    @SerializedName("device1_label")
    private String device1Label;

    @SerializedName("device2_label")
    private String device2Label;

    public ApiRequest(String language, String userId) {
        this.language = language;
        this.user_id = userId;
    }

    public ApiRequest(String language, String userId, String device1Label, String device2Label) {
        this.language = language;
        this.user_id = userId;
        this.device1Label = device1Label;
        this.device2Label = device2Label;
    }

    public String getLanguage() {
        return language;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String userId) {
        this.user_id = userId;
    }
}
