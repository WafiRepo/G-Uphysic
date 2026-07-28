package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

public class OverwriteLabelRequest {
    private String user_id;
    private String image_path;
    private String new_label;

    @SerializedName("physical_radius")
    private Double physicalRadius;

    @SerializedName("gear_front_radius")
    private Double gearFrontRadius;

    @SerializedName("gear_rear_radius")
    private Double gearRearRadius;

    public OverwriteLabelRequest(String user_id, String image_path, String new_label) {
        this.user_id = user_id;
        this.image_path = image_path;
        this.new_label = new_label;
    }

    public OverwriteLabelRequest(String user_id, String image_path, String new_label,
                                  Double physicalRadius, Double gearFrontRadius, Double gearRearRadius) {
        this.user_id = user_id;
        this.image_path = image_path;
        this.new_label = new_label;
        this.physicalRadius = physicalRadius;
        this.gearFrontRadius = gearFrontRadius;
        this.gearRearRadius = gearRearRadius;
    }

    public String getUser_id() { return user_id; }
    public String getImage_path() { return image_path; }
    public String getNew_label() { return new_label; }
    public Double getPhysicalRadius() { return physicalRadius; }
    public Double getGearFrontRadius() { return gearFrontRadius; }
    public Double getGearRearRadius() { return gearRearRadius; }
}
