package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
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
    
    // Storage paths
    public static final String STORAGE_PATH_PHOTOS = "photos";
    public static final String STORAGE_PATH_DRAWINGS = "drawings";
    public static final String STORAGE_PATH_DOCUMENTATION = "documentation";
    public static final String STORAGE_PATH_NOTES = "notes";
    public static final String STORAGE_PATH_EXPERIMENTS = "experiments";
    
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
            
            // Create storage reference with userID prefix
            String fullPath = userId + "/" + storagePath + "/" + fileName;
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
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Create storage reference with userID prefix
            String fullPath = userId + "/" + storagePath + "/" + fileName;
            StorageReference storageRef = storage.getReference().child(fullPath);
            
            // Upload bytes
            UploadTask uploadTask = storageRef.putBytes(data);
            
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
            
            // Upload to documentation path
            uploadFileWithUserId(
                context,
                filePath,
                STORAGE_PATH_DOCUMENTATION,
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
            
            // Upload to notes path
            uploadFileWithUserId(
                context,
                filePath,
                STORAGE_PATH_NOTES,
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
        try {
            String userId = SessionManager.getId(context);
            if (userId == null || userId.isEmpty()) {
                onFailure.onFailure(new Exception("User ID not found"));
                return;
            }
            
            // Generate unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "exp_" + experimentId + "_" + experimentType + "_" + timestamp + ".jpg";
            
            // Upload to experiments path
            uploadFileWithUserId(
                context,
                filePath,
                STORAGE_PATH_EXPERIMENTS,
                fileName,
                onSuccess,
                onFailure,
                null
            );
            
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