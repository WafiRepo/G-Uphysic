package de.rwth_aachen.phyphox.NetworkConnection;

public class ApiRequest {
    private String language;
    private String user_id;

    public ApiRequest(String language, String userId) {
        this.language = language;
        this.user_id = userId;
    }

    public String getLanguage() {
        return language;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String userId) {
        this.user_id = userId;
    }
}
