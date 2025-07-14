package de.rwth_aachen.phyphox.NetworkConnection;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BufferData {
    @SerializedName("acc")
    private List<Double> acc;

    @SerializedName("gyr")
    private List<Double> gyr;

    @SerializedName("gyr_squared")
    private List<Double> gyrSquared;

    @SerializedName("t")
    private List<Double> t;

    @SerializedName("user_id")
    private String userId;

    public BufferData(List<Double> acc, List<Double> gyr, List<Double> gyrSquared, List<Double> t, String userId) {
        this.acc = acc;
        this.gyr = gyr;
        this.gyrSquared = gyrSquared;
        this.t = t;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getters and setters if needed
}
