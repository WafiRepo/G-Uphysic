package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class InquiryGenerateProblemExploringRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("topic")
    private String topic;

    @SerializedName("language")
    private String language;

    @SerializedName("device1_label")
    private String device1Label;

    @SerializedName("device2_label")
    private String device2Label;

    public InquiryGenerateProblemExploringRequest(String userId, String topic) {
        this(userId, topic, "id", null, null);
    }

    public InquiryGenerateProblemExploringRequest(String userId, String topic, String language) {
        this(userId, topic, language, null, null);
    }

    public InquiryGenerateProblemExploringRequest(String userId, String topic, String language, String device1Label, String device2Label) {
        this.userId = userId != null ? userId : "default";
        this.topic = topic != null ? topic : "centripetal acceleration";
        this.language = language != null ? language : "id";
        this.device1Label = device1Label;
        this.device2Label = device2Label;
    }
}
