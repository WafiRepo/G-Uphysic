package de.rwth_aachen.phyphox.NetworkConnection;

public class OverwriteLabelRequest {
    private String user_id;
    private String image_path;
    private String new_label;

    public OverwriteLabelRequest(String user_id, String image_path, String new_label) {
        this.user_id = user_id;
        this.image_path = image_path;
        this.new_label = new_label;
    }

    public String getUser_id() { return user_id; }
    public String getImage_path() { return image_path; }
    public String getNew_label() { return new_label; }
} 