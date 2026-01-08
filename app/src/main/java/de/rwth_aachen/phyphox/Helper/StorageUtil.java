package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import de.rwth_aachen.phyphox.Helper.SessionManager;

public class StorageUtil {
    private static final String TAG = "StorageUtil";
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    
    // Storage paths (base names, akan di-version oleh VersionHelper)
    private static final String STORAGE_PATH_PHOTOS_BASE = "photos";
    private static final String STORAGE_PATH_DRAWINGS_BASE = "drawings";
    private static final String STORAGE_PATH_DOCUMENTATION_BASE = "documentation";
    private static final String STORAGE_PATH_NOTES_BASE = "notes";
    private static final String STORAGE_PATH_EXPERIMENTS_BASE = "experiments";
    
    /**
     * Get version-aware storage paths
     */
    public static String getStoragePathPhotos() {
        return VersionHelper.getStoragePath(STORAGE_PATH_PHOTOS_BASE);
    }
    
    public static String getStoragePathDrawings() {
        return VersionHelper.getStoragePath(STORAGE_PATH_DRAWINGS_BASE);
    }
    
    public static String getStoragePathDocumentation() {
        return VersionHelper.getStoragePath(STORAGE_PATH_DOCUMENTATION_BASE);
    }
    
    public static String getStoragePathNotes() {
        return VersionHelper.getStoragePath(STORAGE_PATH_NOTES_BASE);
    }
    
    public static String getStoragePathExperiments() {
        return VersionHelper.getStoragePath(STORAGE_PATH_EXPERIMENTS_BASE);
    }
    
    // Legacy constants untuk backward compatibility (deprecated, gunakan getter methods)
    @Deprecated
    public static final String STORAGE_PATH_PHOTOS = getStoragePathPhotos();
    @Deprecated
    public static final String STORAGE_PATH_DRAWINGS = getStoragePathDrawings();
    @Deprecated
    public static final String STORAGE_PATH_DOCUMENTATION = getStoragePathDocumentation();
    @Deprecated
    public static final String STORAGE_PATH_NOTES = getStoragePathNotes();
    @Deprecated
    public static final String STORAGE_PATH_EXPERIMENTS = getStoragePathExperiments();
    
    /**
     * Upload file to Firebase Storage with userID prefix
     */
    public static void uploadFileWithUserId(
            Context context,
            String filePath,
            String storagePath,
            String fileName,
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure,
            OnUploadProgressListener onProgress
    ) {
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Create storage reference with userID prefix (clear and consistent)
            // users/{userId}/{storagePath}/{fileName}
            String fullPath = "users/" + userId + "/" + storagePath + "/" + fileName;
            StorageReference storageRef = storage.getReference().child(fullPath);
            
            // Upload file
            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(new File(filePath)));
            
            // Monitor progress
            if (onProgress != null) {
                uploadTask.addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    onProgress.onProgress(progress);
                });
            }
            
            // Handle success
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "File uploaded successfully: " + uri.toString());
                    onSuccess.onSuccess(uri.toString(), fullPath);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                    onFailure.onFailure(e);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed: " + e.getMessage());
                onFailure.onFailure(e);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading file: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Upload byte array to Firebase Storage with userID prefix
     */
    public static void uploadBytesWithUserId(
            Context context,
            byte[] data,
            String storagePath,
            String fileName,
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure,
            OnUploadProgressListener onProgress
    ) {
        uploadBytesWithUserIdAndMetadata(context, data, storagePath, fileName, null, null, onSuccess, onFailure, onProgress);
    }
    
    /**
     * Upload byte array to Firebase Storage with userID prefix and metadata (userId, source)
     */
    public static void uploadBytesWithUserIdAndMetadata(
            Context context,
            byte[] data,
            String storagePath,
            String fileName,
            String source, // e.g., "Buat Pertanyaan Sendiri", "Buat Pertanyaan dengan AI", "Experiment", etc.
            String userName, // User name for metadata
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure,
            OnUploadProgressListener onProgress
    ) {
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Create storage reference with userID prefix (clear and consistent)
            // users/{userId}/{storagePath}/{fileName}
            String fullPath = "users/" + userId + "/" + storagePath + "/" + fileName;
            StorageReference storageRef = storage.getReference().child(fullPath);
            
            // Create metadata with userId and source footprint
            StorageMetadata.Builder metadataBuilder = new StorageMetadata.Builder();
            metadataBuilder.setCustomMetadata("userId", userId);
            if (userName != null && !userName.isEmpty()) {
                metadataBuilder.setCustomMetadata("userName", userName);
            }
            if (source != null && !source.isEmpty()) {
                metadataBuilder.setCustomMetadata("source", source);
            }
            metadataBuilder.setCustomMetadata("uploadedAt", String.valueOf(System.currentTimeMillis()));
            StorageMetadata metadata = metadataBuilder.build();
            
            // Upload bytes with metadata
            UploadTask uploadTask = storageRef.putBytes(data, metadata);
            
            // Monitor progress
            if (onProgress != null) {
                uploadTask.addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    onProgress.onProgress(progress);
                });
            }
            
            // Handle success
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Bytes uploaded successfully: " + uri.toString());
                    Log.d(TAG, "Storage path: " + fullPath + ", Source: " + (source != null ? source : "N/A"));
                    onSuccess.onSuccess(uri.toString(), fullPath);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                    onFailure.onFailure(e);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed: " + e.getMessage());
                onFailure.onFailure(e);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading bytes: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Upload photo documentation with metadata
     */
    public static void uploadPhotoDocumentation(
            Context context,
            String filePath,
            String experimentId,
            String noteText,
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure
    ) {
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Generate unique filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "doc_" + experimentId + "_" + timestamp + ".jpg";
            
            // Upload to documentation path (version-aware)
            uploadFileWithUserId(
                context,
                filePath,
                getStoragePathDocumentation(),
                fileName,
                onSuccess,
                onFailure,
                null
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading photo documentation: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Upload note photo with metadata
     */
    public static void uploadNotePhoto(
            Context context,
            String filePath,
            String noteId,
            String noteText,
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure
    ) {
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Generate unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "note_" + noteId + "_" + timestamp + ".jpg";
            
            // Upload to notes path (version-aware)
            uploadFileWithUserId(
                context,
                filePath,
                getStoragePathNotes(),
                fileName,
                onSuccess,
                onFailure,
                null
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading note photo: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Upload experiment photo with metadata
     */
    public static void uploadExperimentPhoto(
            Context context,
            String filePath,
            String experimentId,
            String experimentType,
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure
    ) {
        uploadExperimentPhotoWithSource(context, filePath, experimentId, experimentType, "Experiment", onSuccess, onFailure);
    }
    
    /**
     * Upload experiment photo with metadata including source
     */
    public static void uploadExperimentPhotoWithSource(
            Context context,
            String filePath,
            String experimentId,
            String experimentType,
            String source, // e.g., "Buat Pertanyaan Sendiri", "Buat Pertanyaan dengan AI", "Experiment"
            OnUploadSuccessListener onSuccess,
            OnUploadFailureListener onFailure
    ) {
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            String userName = SessionManager.getName(context);
            
            // Generate unique filename with userId
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = userId + "_exp_" + experimentId + "_" + experimentType + "_" + timestamp + ".jpg";
            
            // Read file and upload with metadata
            File file = new File(filePath);
            if (!file.exists()) {
                onFailure.onFailure(new Exception("File not found: " + filePath));
                return;
            }
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] fileBytes = new byte[(int) file.length()];
                fis.read(fileBytes);
                
                // Upload with metadata (version-aware)
                uploadBytesWithUserIdAndMetadata(
                    context,
                    fileBytes,
                    getStoragePathExperiments(),
                    fileName,
                    source, // Source metadata
                    userName, // User name metadata
                    onSuccess,
                    onFailure,
                    null
                );
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading experiment photo: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Delete file from Firebase Storage
     */
    public static void deleteFile(String filePath, OnDeleteSuccessListener onSuccess, OnDeleteFailureListener onFailure) {
        try {
            StorageReference storageRef = storage.getReference().child(filePath);
            storageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "File deleted successfully: " + filePath);
                onSuccess.onSuccess();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete file: " + e.getMessage());
                onFailure.onFailure(e);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Get file metadata from Firebase Storage
     */
    public static void getFileMetadata(String filePath, OnMetadataSuccessListener onSuccess, OnMetadataFailureListener onFailure) {
        try {
            StorageReference storageRef = storage.getReference().child(filePath);
            storageRef.getMetadata().addOnSuccessListener(metadata -> {
                Log.d(TAG, "File metadata retrieved: " + metadata.getName());
                onSuccess.onSuccess(metadata);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get file metadata: " + e.getMessage());
                onFailure.onFailure(e);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting file metadata: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Generate unique filename with userID and timestamp
     */
    public static String generateUniqueFileName(Context context, String prefix, String extension) {
        String userId = SessionManager.getId(context);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return userId + "_" + prefix + "_" + timestamp + "_" + uniqueId + "." + extension;
    }
    
    /**
     * Get storage path with userID prefix
     */
    public static String getStoragePathWithUserId(Context context, String storagePath) {
        String userId = SessionManager.getId(context);
        return userId + "/" + storagePath;
    }
    
    // Callback interfaces
    public interface OnUploadSuccessListener {
        void onSuccess(String downloadUrl, String storagePath);
    }
    
    public interface OnUploadFailureListener {
        void onFailure(Exception e);
    }
    
    public interface OnUploadProgressListener {
        void onProgress(double progress);
    }
    
    public interface OnDeleteSuccessListener {
        void onSuccess();
    }
    
    public interface OnDeleteFailureListener {
        void onFailure(Exception e);
    }
    
    public interface OnMetadataSuccessListener {
        void onSuccess(com.google.firebase.storage.StorageMetadata metadata);
    }
    
    public interface OnMetadataFailureListener {
        void onFailure(Exception e);
    }
} 