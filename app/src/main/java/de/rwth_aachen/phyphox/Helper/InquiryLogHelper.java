package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Menyimpan jejak inquiry / exploring ke Firestore agar bisa ditampilkan di panel admin.
 * Koleksi: {@code inquiry_logs} (tidak di-versioning VersionHelper).
 */
public final class InquiryLogHelper {

    private static final String TAG = "InquiryLogHelper";
    public static final String COLLECTION = "inquiry_logs";
    private static final String STORAGE_PREVIEW_FOLDER = "admin_inquiry_previews";

    public static final String KIND_PROBLEM_FINDING = "problem_finding";
    public static final String KIND_EXPLORING_STAGE1 = "exploring_stage1";
    public static final String KIND_EXPLORING_STAGE2 = "exploring_stage2";

    private static final int MAX_FIELD_LEN = 12000;

    private InquiryLogHelper() {
    }

    private static String clip(@Nullable String s) {
        if (s == null) return "";
        if (s.length() <= MAX_FIELD_LEN) return s;
        return s.substring(0, MAX_FIELD_LEN) + "…";
    }

    private static void writeDoc(Map<String, Object> doc) {
        FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .add(doc)
                .addOnFailureListener(e -> Log.w(TAG, "inquiry_logs write failed: " + e.getMessage()));
    }

    /**
     * @param kind salah satu {@link #KIND_PROBLEM_FINDING}, {@link #KIND_EXPLORING_STAGE1}, {@link #KIND_EXPLORING_STAGE2}
     * @param problemImageUrl URL foto (Firebase Storage atau API) untuk problem finding di admin
     * @param graphCanvasImageUrl URL preview grafik canvas (Firebase Storage) untuk exploring stage 2 di admin
     */
    public static void log(
            Context ctx,
            String kind,
            @Nullable String inquiryText,
            @Nullable String userResponse,
            @Nullable String feedbackSummary,
            @Nullable String sessionId,
            @Nullable String objectName,
            @Nullable String experimentLocation,
            @Nullable String problemImageUrl,
            @Nullable String graphCanvasImageUrl
    ) {
        if (ctx == null || kind == null) return;
        String uid = SessionManager.getId(ctx);
        if (uid == null || uid.isEmpty()) uid = "unknown";

        Map<String, Object> doc = new HashMap<>();
        doc.put("idCustomer", uid);
        doc.put("kind", kind);
        doc.put("inquiryText", clip(inquiryText));
        doc.put("userResponse", clip(userResponse));
        doc.put("feedbackSummary", clip(feedbackSummary));
        if (sessionId != null && !sessionId.isEmpty()) doc.put("sessionId", sessionId);
        if (objectName != null && !objectName.isEmpty()) doc.put("objectName", clip(objectName));
        if (experimentLocation != null && !experimentLocation.isEmpty()) {
            doc.put("experimentLocation", clip(experimentLocation));
        }
        if (problemImageUrl != null && !problemImageUrl.trim().isEmpty()) {
            doc.put("problemImageUrl", clip(problemImageUrl.trim()));
        }
        if (graphCanvasImageUrl != null && !graphCanvasImageUrl.trim().isEmpty()) {
            doc.put("graphCanvasImageUrl", clip(graphCanvasImageUrl.trim()));
        }
        doc.put("createdAt", FieldValue.serverTimestamp());

        writeDoc(doc);
    }

    /**
     * Unggah JPEG grafik ke Firebase Storage lalu tulis log stage 2 dengan {@code graphCanvasImageUrl}.
     * Jika upload gagal, log tetap dibuat tanpa URL gambar.
     */
    public static void logExploringStage2WithCanvasUpload(
            Context ctx,
            @Nullable String inquiryText,
            @Nullable String userResponse,
            @Nullable String feedbackSummary,
            @Nullable String sessionId,
            @Nullable byte[] graphJpegBytes
    ) {
        if (ctx == null) return;
        if (graphJpegBytes == null || graphJpegBytes.length == 0) {
            log(ctx, KIND_EXPLORING_STAGE2, inquiryText, userResponse, feedbackSummary, sessionId,
                    null, null, null, null);
            return;
        }
        String uid = SessionManager.getId(ctx);
        if (uid == null || uid.isEmpty()) uid = "unknown";
        String fileName = (sessionId != null && !sessionId.isEmpty() ? sessionId + "_" : "")
                + System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child(STORAGE_PREVIEW_FOLDER)
                .child(uid)
                .child(fileName);
        ref.putBytes(graphJpegBytes)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> log(ctx, KIND_EXPLORING_STAGE2, inquiryText, userResponse,
                                feedbackSummary, sessionId, null, null, null, uri.toString()))
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "getDownloadUrl canvas preview failed", e);
                            log(ctx, KIND_EXPLORING_STAGE2, inquiryText, userResponse, feedbackSummary, sessionId,
                                    null, null, null, null);
                        }))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "putBytes canvas preview failed", e);
                    log(ctx, KIND_EXPLORING_STAGE2, inquiryText, userResponse, feedbackSummary, sessionId,
                            null, null, null, null);
                });
    }
}
