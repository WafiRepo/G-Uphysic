package de.rwth_aachen.phyphox.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SidecarJson {
    @SerializedName("tracked_object")
    public TrackedObject trackedObject;

    @SerializedName("reference_geometry")
    public ReferenceGeometry referenceGeometry;

    @SerializedName("display_w")
    public int displayW;

    @SerializedName("display_h")
    public int displayH;

    @SerializedName("video_w")
    public int videoW;

    @SerializedName("video_h")
    public int videoH;

    public static class TrackedObject {
        @SerializedName("visual_cues")
        public List<String> visualCues;
    }

    public static class ReferenceGeometry {
        @SerializedName("label")
        public String label;

        @SerializedName("bbox_center_px")
        public List<Integer> bboxCenterPx;

        @SerializedName("physical_size")
        public Double physicalSize;
    }
}
