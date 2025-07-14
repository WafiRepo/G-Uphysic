package de.rwth_aachen.phyphox.model;

import java.util.List;

public class QuestionModel {
    private String question;
    private String type;
    private List<Integer> images;

    public QuestionModel(String question, List<Integer> images, String type) {
        this.question = question;
        this.images = images;
        this.type = type;
    }

    public String getQuestion() {
        return question;
    }
    public String getType() {
        return type;
    }

    public List<Integer> getImages() {
        return images;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setImages(List<Integer> images) {
        this.images = images;
    }
}


