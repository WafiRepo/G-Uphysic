package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

/**
 * Request untuk inquiry problem exploring stage 2 (interpretasi grafik/sensor).
 * Kirim screenshot grafik (+ gambar canvas) sebagai base64 JPEG.
 */
public class InquiryGenerateProblemExploringStage2Request {

    @SerializedName("user_id")
    private final String userId;

    @SerializedName("topic")
    private final String topic;

    @SerializedName("language")
    private final String language;

    @SerializedName("session_id")
    private final String sessionId;

    @SerializedName("graph_context")
    private final String graphContext;

    @SerializedName("graph_image_base64")
    private final String graphImageBase64;

    public InquiryGenerateProblemExploringStage2Request(
            String userId,
            String topic,
            String language,
            String sessionId,
            String graphContext,
            String graphImageBase64
    ) {
        this.userId = userId != null ? userId : "default";
        this.topic = topic != null ? topic : "centripetal acceleration";
        this.language = language != null ? language : "id";
        this.sessionId = sessionId;
        this.graphContext = graphContext != null ? graphContext : "";
        this.graphImageBase64 = graphImageBase64;
    }
}
