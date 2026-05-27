package de.rwth_aachen.phyphox.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class JobResult {
    @SerializedName("job_id")
    public String jobId;

    @SerializedName("stats")
    public Map<String, String> stats;

    @SerializedName("files")
    public Files files;

    public static class Files {
        @SerializedName("summary_panel")
        public String summaryPanel;

        @SerializedName("student_pdf")
        public String studentPdf;

        @SerializedName("teacher_pdf")
        public String teacherPdf;

        @SerializedName("annotated_video")
        public String annotatedVideo;
    }
}
