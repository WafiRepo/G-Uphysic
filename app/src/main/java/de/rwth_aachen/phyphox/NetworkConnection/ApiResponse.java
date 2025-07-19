package de.rwth_aachen.phyphox.NetworkConnection;

import java.util.List;
import java.util.Map;

public class ApiResponse {
    private String question;
    private String label;
    private String python_code;
    private String error;
    private String local_image_base64;
    private String table_img_base64;
    private List<String> graph_images;


    //advanced question
    private String image1_base64;
    private String table_img_base64_1;
    private String table_img_base64_2;


    private List<Map<String, Object>> experiment_1_data;
    private List<Map<String, Object>> experiment_2_data;

    public String getQuestions() {
        return question;
    }

    public String getPythonCode() {
        return python_code;
    }

    public List<String> getGraphImages() {
        return graph_images;
    }

    public List<Map<String, Object>> getExperiment1Data() {
        return experiment_1_data;
    }

    public List<Map<String, Object>> getExperiment2Data() {
        return experiment_2_data;
    }


    private Map<String, Object> classification_result;
    private Map<String, Integer> circle_details;
    private String processed_image_path;
    private String message;


    public Map<String, Object> getClassificationResult() {
        return classification_result;
    }

    public Map<String, Integer> getCircleDetails() {
        return circle_details;
    }

    public String getProcessedImagePath() {
        return processed_image_path;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public String getTable_img_base64() {
        return table_img_base64;
    }

    public String getLocal_image_base64() {
        return local_image_base64;
    }

    public String getImage1_base64() {
        return image1_base64;
    }

    public String getTable_img_base64_1() {
        return table_img_base64_1;
    }

    public String getTable_img_base64_2() {
        return table_img_base64_2;
    }

    private String image2_base64;

    public String getImage2_base64() {
        return image2_base64;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
