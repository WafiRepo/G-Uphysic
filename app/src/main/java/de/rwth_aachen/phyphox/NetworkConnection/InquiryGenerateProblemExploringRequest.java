package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class InquiryGenerateProblemExploringRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("topic")
    private String topic;

    @SerializedName("language")
    private String language;

    public InquiryGenerateProblemExploringRequest(String userId, String topic) {
        this(userId, topic, "id");
    }

    public InquiryGenerateProblemExploringRequest(String userId, String topic, String language) {
        this.userId = userId != null ? userId : "default";
        this.topic = topic != null ? topic : "centripetal acceleration";
        this.language = language != null ? language : "id";
    }
}
