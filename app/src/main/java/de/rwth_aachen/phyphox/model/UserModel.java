package de.rwth_aachen.phyphox.model;

public class UserModel {
    private String id = "";
    private String name = "";
    private String email = "";
    private Long totalVisitingIntroduction = 0L;
    private Long totalVisitDurationMillis = 0L;

    // Default constructor
    public UserModel() {
    }

    // Constructor with parameters
    public UserModel(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getTotalVisitingIntroduction() {
        return totalVisitingIntroduction;
    }

    public void setTotalVisitingIntroduction(Long totalVisitingIntroduction) {
        this.totalVisitingIntroduction = totalVisitingIntroduction;
    }

    public Long getTotalVisitDurationMillis() {
        return totalVisitDurationMillis;
    }

    public void setTotalVisitDurationMillis(Long totalVisitDurationMillis) {
        this.totalVisitDurationMillis = totalVisitDurationMillis;
    }

    // Companion object equivalent in Java (constants)
    public static class Fields {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
    }
}
