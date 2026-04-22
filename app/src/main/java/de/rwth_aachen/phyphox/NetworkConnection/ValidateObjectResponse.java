package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class ValidateObjectResponse {
    @SerializedName("valid")
    private boolean valid;

    @SerializedName("feedback")
    private String feedback;

    public boolean isValid() {
        return valid;
    }

    public String getFeedback() {
        return feedback;
    }
}
