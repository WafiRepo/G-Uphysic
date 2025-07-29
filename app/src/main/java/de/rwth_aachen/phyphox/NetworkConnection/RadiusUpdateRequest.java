package de.rwth_aachen.phyphox.NetworkConnection;

public class RadiusUpdateRequest {
    private String user_id;
    private float new_radius;

    public RadiusUpdateRequest(String user_id, float new_radius) {
        this.user_id = user_id;
        this.new_radius = new_radius;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public float getNew_radius() {
        return new_radius;
    }

    public void setNew_radius(float new_radius) {
        this.new_radius = new_radius;
    }
}
