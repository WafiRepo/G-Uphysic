package de.rwth_aachen.phyphox.model;

import com.google.gson.annotations.SerializedName;

public class JobStatus {
    @SerializedName("job_id")
    public String jobId;

    @SerializedName("status")
    public String status;

    @SerializedName("step")
    public String step;

    @SerializedName("progress_pct")
    public float progressPct;

    @SerializedName("message")
    public String message;

    @SerializedName("elapsed_s")
    public long elapsedS;
}
