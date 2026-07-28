package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class InquiryGenerateRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("topic")
    private String topic;

    @SerializedName("location")
    private String location;

    @SerializedName("language")
    private String language;

    @SerializedName("device_count")
    private int deviceCount;

    public InquiryGenerateRequest(String userId, String topic, String location) {
        this(userId, topic, location, "id", 1);
    }

    public InquiryGenerateRequest(String userId, String topic, String location, String language) {
        this(userId, topic, location, language, 1);
    }

    public InquiryGenerateRequest(String userId, String topic, String location, String language, int deviceCount) {
        this.userId = userId != null ? userId : "default";
        this.topic = topic != null ? topic : "centripetal acceleration";
        this.location = location != null ? location : "";
        this.language = language != null ? language : "id";
        this.deviceCount = deviceCount;
    }
}
