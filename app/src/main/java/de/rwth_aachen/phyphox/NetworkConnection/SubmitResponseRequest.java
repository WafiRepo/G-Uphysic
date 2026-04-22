package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class SubmitResponseRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("response_text")
    private String responseText;

    @SerializedName("inquiry_id")
    private String inquiryId;

    @SerializedName("language")
    private String language;

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("graph_context")
    private String graphContext;

    @SerializedName("graph_image_base64")
    private String graphImageBase64;

    @SerializedName("current_inquiry_text")
    private String currentInquiryText;

    public SubmitResponseRequest(String userId, String responseText) {
        this(userId, responseText, "id");
    }

    public SubmitResponseRequest(String userId, String responseText, String language) {
        this.userId = userId;
        this.responseText = responseText;
        this.inquiryId = null;
        this.language = language != null ? language : "id";
    }

    /** Stage 1 submit: sertakan teks pertanyaan aktif agar evaluasi benar-benar sesuai konteks. */
    public SubmitResponseRequest(String userId, String responseText, String language, String currentInquiryText) {
        this.userId = userId;
        this.responseText = responseText;
        this.inquiryId = null;
        this.language = language != null ? language : "id";
        this.currentInquiryText = currentInquiryText;
    }

    /** Stage 2 inquiry: sertakan session yang sama dengan generate + opsional gambar untuk konteks LLM. */
    public SubmitResponseRequest(
            String userId,
            String responseText,
            String language,
            String sessionId,
            String graphContext,
            String graphImageBase64
    ) {
        this.userId = userId;
        this.responseText = responseText;
        this.inquiryId = null;
        this.language = language != null ? language : "id";
        this.sessionId = sessionId;
        this.graphContext = graphContext;
        this.graphImageBase64 = graphImageBase64;
    }
}
