package de.rwth_aachen.phyphox.NetworkConnection;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubmitResponseResponse {
    @SerializedName("feedback")
    private String feedback;

    @SerializedName("next_step")
    private String nextStep;

    @SerializedName("matched_answer_type")
    private String matchedAnswerType;

    @SerializedName("step_by_step")
    private List<String> stepByStep;

    @SerializedName("example")
    private String example;

    @SerializedName("comprehension_check")
    private String comprehensionCheck;

    public String getFeedback() {
        return feedback;
    }

    public String getNextStep() {
        return nextStep;
    }

    public String getMatchedAnswerType() {
        return matchedAnswerType;
    }

    public List<String> getStepByStep() {
        return stepByStep;
    }

    public String getExample() {
        return example;
    }

    public String getComprehensionCheck() {
        return comprehensionCheck;
    }
}
