package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class PhotoAttemptRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("device_id")
    private int deviceId;

    @SerializedName("label")
    private String label;

    @SerializedName("is_valid")
    private boolean isValid;

    @SerializedName("attempt_number")
    private int attemptNumber;

    @SerializedName("image_path")
    private String imagePath;

    public PhotoAttemptRequest(String userId, int deviceId, String label,
                               boolean isValid, int attemptNumber, String imagePath) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.label = label != null ? label : "";
        this.isValid = isValid;
        this.attemptNumber = attemptNumber;
        this.imagePath = imagePath;
    }
}
