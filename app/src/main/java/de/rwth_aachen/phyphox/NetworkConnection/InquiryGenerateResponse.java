package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class InquiryGenerateResponse {
    @SerializedName("inquiry")
    private String inquiry;

    @SerializedName("stage")
    private String stage;

    @SerializedName("ability_band")
    private String abilityBand;

    @SerializedName("targeting_mode")
    private String targetingMode;

    @SerializedName("detected_object")
    private String detectedObject;

    public String getInquiry() { return inquiry; }
    public String getStage() { return stage; }
    public String getAbilityBand() { return abilityBand; }
    public String getTargetingMode() { return targetingMode; }
    public String getDetectedObject() { return detectedObject; }
}
