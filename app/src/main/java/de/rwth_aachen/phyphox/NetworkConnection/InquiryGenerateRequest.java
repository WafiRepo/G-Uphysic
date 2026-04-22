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

    public InquiryGenerateRequest(String userId, String topic, String location) {
        this(userId, topic, location, "id");
    }

    public InquiryGenerateRequest(String userId, String topic, String location, String language) {
        this.userId = userId != null ? userId : "default";
        this.topic = topic != null ? topic : "centripetal acceleration";
        this.location = location != null ? location : "";
        this.language = language != null ? language : "id";
    }
}
