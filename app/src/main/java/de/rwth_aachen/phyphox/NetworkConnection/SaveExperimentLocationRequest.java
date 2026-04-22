package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class SaveExperimentLocationRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("location")
    private String location;

    public SaveExperimentLocationRequest(String userId, String location) {
        this.userId = userId;
        this.location = location;
    }

    public String getUserId() {
        return userId;
    }

    public String getLocation() {
        return location;
    }
}
