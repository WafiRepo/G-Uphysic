package de.rwth_aachen.phyphox.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class JobsListResponse {
    @SerializedName("jobs")
    public List<JobEntry> jobs;

    public static class JobEntry {
        @SerializedName("job_id")
        public String jobId;

        @SerializedName("status")
        public String status;

        @SerializedName("step")
        public String step;

        @SerializedName("progress_pct")
        public float progressPct;

        @SerializedName("elapsed_s")
        public long elapsedS;
    }
}
