package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.rwth_aachen.phyphox.model.DataModel;

/**
 * Uploads graph/table (and related) question images from {@link DataModel} legacy base64 fields
 * to Firebase Storage, then fills {@code questionImageUrl*} / {@code questionImagePath*} on the model.
 */
public final class QuestionImageFirestoreSync {

    private static final String TAG = "QuestionImgSync";

    public interface BatchCallback {
        void onComplete();

        void onFailure(String message);
    }

    private QuestionImageFirestoreSync() {
    }

    /**
     * Copy URL/path fields from DataModel into a Firestore progress map.
     */
    public static void mergeQuestionImageFieldsIntoMap(DataModel dm, Map<String, Object> progressData) {
        if (dm == null || progressData == null) return;
        putIfNonEmpty(progressData, "questionImageUrl1", dm.getQuestionImageUrl1());
        putIfNonEmpty(progressData, "questionImageUrl2", dm.getQuestionImageUrl2());
        putIfNonEmpty(progressData, "questionImageUrl3", dm.getQuestionImageUrl3());
        putIfNonEmpty(progressData, "questionImageUrl4", dm.getQuestionImageUrl4());
        putIfNonEmpty(progressData, "questionImageUrl5", dm.getQuestionImageUrl5());
        putPathIfNonEmpty(progressData, "questionImagePath1", dm.getQuestionImagePath1());
        putPathIfNonEmpty(progressData, "questionImagePath2", dm.getQuestionImagePath2());
        putPathIfNonEmpty(progressData, "questionImagePath3", dm.getQuestionImagePath3());
        putPathIfNonEmpty(progressData, "questionImagePath4", dm.getQuestionImagePath4());
        putPathIfNonEmpty(progressData, "questionImagePath5", dm.getQuestionImagePath5());
    }

    private static void putIfNonEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && value.startsWith("http")) {
            map.put(key, value);
        }
    }

    private static void putPathIfNonEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    /**
     * @param useRecordsPath true → {@code records/{id}/question_images/...}, false → {@code questions/{id}/question_images/...}
     */
    public static void uploadQuestionImagesFromDataModel(
            Context context,
            DataModel dataModel,
            String documentId,
            boolean useRecordsPath,
            BatchCallback callback
    ) {
        if (dataModel == null || documentId == null || documentId.isEmpty()) {
            callback.onComplete();
            return;
        }

        String prefix = useRecordsPath ? "records" : "questions";
        String storageFolder = prefix + "/" + documentId + "/question_images";
        String source = useRecordsPath ? "Question images (record)" : "Question images (questions)";
        String userName = SessionManager.getName(context);

        String[] payloads = new String[]{
                dataModel.getBase64(),
                dataModel.getBase64_2(),
                dataModel.getBase64_3(),
                dataModel.getBase64_4(),
                dataModel.getBase64_5()
        };

        AtomicInteger pending = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);

        Runnable maybeFinish = () -> {
            if (failed.get()) return;
            if (pending.get() == 0) {
                callback.onComplete();
            }
        };

        for (int i = 0; i < 5; i++) {
            final int slot = i + 1;
            String payload = payloads[i];
            if (payload == null || payload.trim().isEmpty()) {
                continue;
            }
            String trimmed = payload.trim();
            if (trimmed.startsWith("http")) {
                applyUrlOnly(dataModel, slot, trimmed, null);
                continue;
            }

            byte[] jpeg = base64PayloadToJpegBytes(trimmed, 82);
            if (jpeg == null || jpeg.length == 0) {
                Log.w(TAG, "Slot " + slot + ": could not decode image, skipping");
                continue;
            }

            pending.incrementAndGet();
            String fileName = "q_img_" + slot + "_" + System.currentTimeMillis() + ".jpg";
            StorageUtil.uploadBytesWithUserIdAndMetadata(
                    context,
                    jpeg,
                    storageFolder,
                    fileName,
                    source,
                    userName,
                    (downloadUrl, fullPath) -> {
                        applyUrlOnly(dataModel, slot, downloadUrl, fullPath);
                        pending.decrementAndGet();
                        maybeFinish.run();
                    },
                    e -> {
                        failed.set(true);
                        callback.onFailure(e.getMessage() != null ? e.getMessage() : "Upload failed");
                    },
                    null
            );
        }

        maybeFinish.run();
    }

    private static void applyUrlOnly(DataModel dm, int slot, String url, String fullPath) {
        switch (slot) {
            case 1:
                dm.setQuestionImageUrl1(url);
                if (fullPath != null) dm.setQuestionImagePath1(fullPath);
                break;
            case 2:
                dm.setQuestionImageUrl2(url);
                if (fullPath != null) dm.setQuestionImagePath2(fullPath);
                break;
            case 3:
                dm.setQuestionImageUrl3(url);
                if (fullPath != null) dm.setQuestionImagePath3(fullPath);
                break;
            case 4:
                dm.setQuestionImageUrl4(url);
                if (fullPath != null) dm.setQuestionImagePath4(fullPath);
                break;
            case 5:
                dm.setQuestionImageUrl5(url);
                if (fullPath != null) dm.setQuestionImagePath5(fullPath);
                break;
            default:
                break;
        }
    }

    private static byte[] base64PayloadToJpegBytes(String payload, int quality) {
        try {
            String data = payload.trim();
            if (data.contains(",")) {
                data = data.split(",", 2)[1];
            }
            data = data.replaceAll("\\s+", "");
            byte[] raw = Base64.decode(data, Base64.NO_WRAP);
            Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bmp == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            bmp.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "decode failed: " + e.getMessage());
            return null;
        }
    }
}
