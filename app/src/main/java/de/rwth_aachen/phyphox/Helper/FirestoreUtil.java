package de.rwth_aachen.phyphox.Helper;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.rwth_aachen.phyphox.model.DataModel;

public class FirestoreUtil {

    private static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Add or update a document in a collection with backup to secondary collection.
     * Collection names akan di-version secara otomatis oleh VersionHelper.
     * 
     * @param primaryCollection Base collection name (e.g., "record", "questions")
     * @param backupCollection Base backup collection name (akan di-version)
     * @param documentId Document ID
     * @param data Data to save
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    public static <T> void addOrUpdateDocumentWithBackup(
            String primaryCollection,
            String backupCollection,
            String documentId,
            T data,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        // Get version-aware collection names
        String versionedPrimaryCollection = VersionHelper.getCollectionName(primaryCollection);
        String versionedBackupCollection = VersionHelper.getCollectionName(backupCollection);
        
        android.util.Log.d("FirestoreUtil", "Saving to versioned collection: " + versionedPrimaryCollection + " (base: " + primaryCollection + ")");
        
        // First, save to primary collection
        firestore.collection(versionedPrimaryCollection)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Then save to backup collection
                    firestore.collection(versionedBackupCollection)
                            .document(documentId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(backupVoid -> onSuccess.run())
                            .addOnFailureListener(e -> {
                                // Primary save succeeded, backup failed - still consider success
                                android.util.Log.w("FirestoreUtil", "Primary save succeeded but backup failed: " + e.getMessage());
                                onSuccess.run();
                            });
                })
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }

    /**
     * Add or update a document in a collection.
     * Collection name akan di-version secara otomatis oleh VersionHelper.
     * 
     * @param collectionPath Base collection name (e.g., "record", "questions")
     * @param documentId Document ID
     * @param data Data to save
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    public static <T> void addOrUpdateDocument(
            String collectionPath,
            String documentId,
            T data,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        // Get version-aware collection name
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        android.util.Log.d("FirestoreUtil", "Saving to versioned collection: " + versionedCollection + " (base: " + collectionPath + ")");
        
        firestore.collection(versionedCollection)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }
    
    /**
     * Add or update document with versioning and history tracking.
     * This method ensures data is not lost by:
     * 1. Loading existing document first
     * 2. Creating history entry before update
     * 3. Merging new data with existing data (preserving unchanged fields)
     * 4. Incrementing version
     * 5. Updating timestamps
     * 
     * Collection name akan di-version secara otomatis oleh VersionHelper.
     * 
     * @param collectionPath Base collection name (e.g., "record", "questions")
     * @param documentId Document ID
     * @param newData New data to save
     * @param userId User ID
     * @param userName User name
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    public static void addOrUpdateDocumentWithVersioning(
            String collectionPath,
            String documentId,
            DataModel newData,
            String userId,
            String userName,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        // Get version-aware collection name
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        android.util.Log.d("FirestoreUtil", "Versioning save to: " + versionedCollection + " (base: " + collectionPath + ")");
        
        // Add appVersion to data
        if (newData != null) {
            // Set appVersion field if not already set
            try {
                java.lang.reflect.Method setAppVersion = newData.getClass().getMethod("setAppVersion", String.class);
                setAppVersion.invoke(newData, VersionHelper.getCurrentVersion());
            } catch (Exception e) {
                // If method doesn't exist, ignore (backward compatibility)
            }
        }
        
        // First, try to get existing document
        getDocument(versionedCollection, documentId, DataModel.class,
            existingData -> {
                try {
                    DataModel dataToSave;
                    long currentTime = System.currentTimeMillis();
                    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    
                    if (existingData != null) {
                        // UPDATE: Merge with existing data and add to history array
                        dataToSave = existingData;
                        
                        // Track changed fields for history
                        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
                        if (newData.getQuestion() != null && !newData.getQuestion().trim().isEmpty() && 
                            !newData.getQuestion().equals(existingData.getQuestion())) {
                            changedFields.put("question", newData.getQuestion());
                        }
                        if (newData.getLocationName() != null && !newData.getLocationName().trim().isEmpty() && 
                            !newData.getLocationName().equals(existingData.getLocationName())) {
                            changedFields.put("locationName", newData.getLocationName());
                        }
                        if (newData.getLatitude() != 0.0 && newData.getLatitude() != existingData.getLatitude()) {
                            changedFields.put("latitude", newData.getLatitude());
                        }
                        if (newData.getLongitude() != 0.0 && newData.getLongitude() != existingData.getLongitude()) {
                            changedFields.put("longitude", newData.getLongitude());
                        }
                        if (newData.getPhotoAnswerUrl() != null && !newData.getPhotoAnswerUrl().trim().isEmpty() && 
                            !newData.getPhotoAnswerUrl().equals(existingData.getPhotoAnswerUrl())) {
                            changedFields.put("photoAnswerUrl", newData.getPhotoAnswerUrl());
                        }
                        
                        // Merge new data with existing (preserve unchanged fields)
                        mergeDataModel(existingData, newData);
                        
                        // Increment version and update timestamps
                        dataToSave.incrementVersion();
                        dataToSave.setUpdatedAt(currentTime);
                        dataToSave.setUpdatedAtFormatted(formatter.format(new java.util.Date(currentTime)));
                        
                        // Add history entry to array (stored in same document, max 10 entries)
                        dataToSave.addHistoryEntry("update", userId, userName, changedFields);
                        
                        // Update user tracking
                        if (userId != null && !userId.isEmpty()) {
                            dataToSave.setIdCustomer(userId);
                        }
                        if (userName != null && !userName.isEmpty()) {
                            dataToSave.setCustomerName(userName);
                        }
                        
                        android.util.Log.d("FirestoreUtil", "Updating document " + documentId + " to version " + dataToSave.getVersion());
                        
                        // CRITICAL: Clean empty fields before save to prevent overwriting existing data
                        cleanEmptyFields(dataToSave);
                    } else {
                        // CREATE: New document
                        dataToSave = newData;
                        
                        // Initialize versioning for new record
                        dataToSave.initializeVersioning(userId, userName);
                        
                        // Clean empty fields for new document too
                        cleanEmptyFields(dataToSave);
                        
                        // Add initial history entry for creation
                        java.util.Map<String, Object> initialFields = new java.util.HashMap<>();
                        initialFields.put("created", true);
                        dataToSave.addHistoryEntry("create", userId, userName, initialFields);
                        
                        android.util.Log.d("FirestoreUtil", "Creating new document " + documentId);
                    }
                    
                    // Save to main collection with merge to preserve any fields not in DataModel
                    firestore.collection(versionedCollection)
                            .document(documentId)
                            .set(dataToSave, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("FirestoreUtil", "Document saved successfully: " + documentId);
                                onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirestoreUtil", "Failed to save document: " + e.getMessage());
                                onFailure.onFailure(e);
                            });
                    
                } catch (Exception e) {
                    android.util.Log.e("FirestoreUtil", "Error in versioning save: " + e.getMessage(), e);
                    onFailure.onFailure(e);
                }
            },
            e -> {
                // If get fails, assume new document
                try {
                    DataModel dataToSave = newData;
                    dataToSave.initializeVersioning(userId, userName);
                    
                    // Clean empty fields
                    cleanEmptyFields(dataToSave);
                    
                    // Add initial history entry
                    java.util.Map<String, Object> initialFields = new java.util.HashMap<>();
                    initialFields.put("created", true);
                    dataToSave.addHistoryEntry("create", userId, userName, initialFields);
                    
                    firestore.collection(versionedCollection)
                            .document(documentId)
                            .set(dataToSave, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.run())
                            .addOnFailureListener(onFailure::onFailure);
                } catch (Exception ex) {
                    onFailure.onFailure(ex);
                }
            }
        );
    }
    
    /**
     * Merge new DataModel into existing DataModel, preserving unchanged fields.
     * CRITICAL: Only updates fields that have NON-EMPTY values in newData.
     * Empty/null fields in newData will NOT overwrite existing values.
     * This prevents data loss when partial updates are made.
     */
    private static void mergeDataModel(DataModel existing, DataModel newData) {
        android.util.Log.d("FirestoreUtil", "=== MERGE DATA MODEL START ===");
        android.util.Log.d("FirestoreUtil", "Existing ID: " + existing.getId() + ", Version: " + existing.getVersion());
        android.util.Log.d("FirestoreUtil", "New Data ID: " + newData.getId());
        
        // Helper method to check if string has meaningful value
        java.util.function.Function<String, Boolean> hasValue = (str) -> 
            str != null && !str.trim().isEmpty();
        
        // ===== QUESTION & DESCRIPTION FIELDS =====
        // Only update if newData has non-empty value
        if (hasValue.apply(newData.getQuestion())) {
            android.util.Log.d("FirestoreUtil", "Updating question field");
            existing.setQuestion(newData.getQuestion());
        }
        if (hasValue.apply(newData.getTypeData())) {
            existing.setTypeData(newData.getTypeData());
        }
        if (hasValue.apply(newData.getTypeQuestion())) {
            existing.setTypeQuestion(newData.getTypeQuestion());
        }
        if (hasValue.apply(newData.getDesc())) {
            existing.setDesc(newData.getDesc());
        }
        if (hasValue.apply(newData.getTopics())) {
            existing.setTopics(newData.getTopics());
        }
        
        // ===== LOCATION FIELDS =====
        // Only update if newData has meaningful location data
        if (hasValue.apply(newData.getLocationName())) {
            existing.setLocationName(newData.getLocationName());
        }
        // Only update coordinates if they are not default (0.0)
        if (newData.getLatitude() != 0.0 || newData.getLongitude() != 0.0) {
            if (newData.getLatitude() != 0.0) {
                existing.setLatitude(newData.getLatitude());
            }
            if (newData.getLongitude() != 0.0) {
                existing.setLongitude(newData.getLongitude());
            }
        }
        
        // ===== ANSWER PHOTO FIELDS =====
        if (hasValue.apply(newData.getPhotoAnswerUrl())) {
            existing.setPhotoAnswerUrl(newData.getPhotoAnswerUrl());
        }
        if (hasValue.apply(newData.getPhotoAnswerPath())) {
            existing.setPhotoAnswerPath(newData.getPhotoAnswerPath());
        }
        // Legacy photoAnswer - only update if newData has value
        if (hasValue.apply(newData.getPhotoAnswer())) {
            existing.setPhotoAnswer(newData.getPhotoAnswer());
        }
        
        // ===== QUESTION IMAGE URLS =====
        // Only update if newData has non-empty URL
        if (hasValue.apply(newData.getQuestionImageUrl1())) {
            existing.setQuestionImageUrl1(newData.getQuestionImageUrl1());
        }
        if (hasValue.apply(newData.getQuestionImageUrl2())) {
            existing.setQuestionImageUrl2(newData.getQuestionImageUrl2());
        }
        if (hasValue.apply(newData.getQuestionImageUrl3())) {
            existing.setQuestionImageUrl3(newData.getQuestionImageUrl3());
        }
        if (hasValue.apply(newData.getQuestionImageUrl4())) {
            existing.setQuestionImageUrl4(newData.getQuestionImageUrl4());
        }
        if (hasValue.apply(newData.getQuestionImageUrl5())) {
            existing.setQuestionImageUrl5(newData.getQuestionImageUrl5());
        }
        
        // ===== QUESTION IMAGE PATHS =====
        if (hasValue.apply(newData.getQuestionImagePath1())) {
            existing.setQuestionImagePath1(newData.getQuestionImagePath1());
        }
        if (hasValue.apply(newData.getQuestionImagePath2())) {
            existing.setQuestionImagePath2(newData.getQuestionImagePath2());
        }
        if (hasValue.apply(newData.getQuestionImagePath3())) {
            existing.setQuestionImagePath3(newData.getQuestionImagePath3());
        }
        if (hasValue.apply(newData.getQuestionImagePath4())) {
            existing.setQuestionImagePath4(newData.getQuestionImagePath4());
        }
        if (hasValue.apply(newData.getQuestionImagePath5())) {
            existing.setQuestionImagePath5(newData.getQuestionImagePath5());
        }
        
        // ===== LEGACY BASE64 FIELDS =====
        // IMPORTANT: Only update if newData has value, preserve existing if empty
        if (hasValue.apply(newData.getBase64())) {
            existing.setBase64(newData.getBase64());
        }
        if (hasValue.apply(newData.getBase64_2())) {
            existing.setBase64_2(newData.getBase64_2());
        }
        if (hasValue.apply(newData.getBase64_3())) {
            existing.setBase64_3(newData.getBase64_3());
        }
        if (hasValue.apply(newData.getBase64_4())) {
            existing.setBase64_4(newData.getBase64_4());
        }
        if (hasValue.apply(newData.getBase64_5())) {
            existing.setBase64_5(newData.getBase64_5());
        }
        
        // ===== PHOTO DRAW (Canvas) =====
        // Append new items, don't replace
        if (newData.getPhotoDraw() != null && !newData.getPhotoDraw().isEmpty()) {
            if (existing.getPhotoDraw() == null) {
                existing.setPhotoDraw(new ArrayList<>());
            }
            // Only add items that don't already exist
            for (String newUrl : newData.getPhotoDraw()) {
                if (hasValue.apply(newUrl) && !existing.getPhotoDraw().contains(newUrl)) {
                    existing.getPhotoDraw().add(newUrl);
                }
            }
        }
        
        // ===== PHOTO DOCUMENTATION =====
        // Append new items, don't replace
        if (newData.getPhotoDocumentation() != null && !newData.getPhotoDocumentation().isEmpty()) {
            if (existing.getPhotoDocumentation() == null) {
                existing.setPhotoDocumentation(new ArrayList<>());
            }
            for (String newUrl : newData.getPhotoDocumentation()) {
                if (hasValue.apply(newUrl) && !existing.getPhotoDocumentation().contains(newUrl)) {
                    existing.getPhotoDocumentation().add(newUrl);
                }
            }
        }
        if (newData.getPhotoDocumentationPaths() != null && !newData.getPhotoDocumentationPaths().isEmpty()) {
            if (existing.getPhotoDocumentationPaths() == null) {
                existing.setPhotoDocumentationPaths(new ArrayList<>());
            }
            existing.getPhotoDocumentationPaths().addAll(newData.getPhotoDocumentationPaths());
        }
        
        // ===== NOTE PHOTOS =====
        if (newData.getNotePhotos() != null && !newData.getNotePhotos().isEmpty()) {
            if (existing.getNotePhotos() == null) {
                existing.setNotePhotos(new ArrayList<>());
            }
            for (String newUrl : newData.getNotePhotos()) {
                if (hasValue.apply(newUrl) && !existing.getNotePhotos().contains(newUrl)) {
                    existing.getNotePhotos().add(newUrl);
                }
            }
        }
        
        // ===== EXPERIMENT PHOTO =====
        if (hasValue.apply(newData.getExperimentPhotoUrl())) {
            existing.setExperimentPhotoUrl(newData.getExperimentPhotoUrl());
        }
        if (hasValue.apply(newData.getExperimentPhotoPath())) {
            existing.setExperimentPhotoPath(newData.getExperimentPhotoPath());
        }
        
        // ===== DOCUMENTATION NOTES =====
        if (hasValue.apply(newData.getDocumentationNotes())) {
            // Append to existing notes if both exist
            if (hasValue.apply(existing.getDocumentationNotes())) {
                existing.setDocumentationNotes(existing.getDocumentationNotes() + "\n" + newData.getDocumentationNotes());
            } else {
                existing.setDocumentationNotes(newData.getDocumentationNotes());
            }
        }
        
        // ===== OTHER FIELDS =====
        if (hasValue.apply(newData.getPhoto())) {
            existing.setPhoto(newData.getPhoto());
        }
        if (hasValue.apply(newData.getPhotoAcceleration())) {
            existing.setPhotoAcceleration(newData.getPhotoAcceleration());
        }
        if (hasValue.apply(newData.getValueAcceleration())) {
            existing.setValueAcceleration(newData.getValueAcceleration());
        }
        if (hasValue.apply(newData.getStorageUserId())) {
            existing.setStorageUserId(newData.getStorageUserId());
        }
        
        // ===== STATUS & FLAGS =====
        // Only update if explicitly provided (not null)
        if (newData.getFinished() != null) {
            existing.setFinished(newData.getFinished());
        }
        if (hasValue.apply(newData.getStatus())) {
            existing.setStatus(newData.getStatus());
        }
        
        // ===== ADMIN TRACKING =====
        if (hasValue.apply(newData.getCreatorName())) {
            existing.setCreatorName(newData.getCreatorName());
        }
        if (hasValue.apply(newData.getDibuatOleh())) {
            existing.setDibuatOleh(newData.getDibuatOleh());
        }
        if (hasValue.apply(newData.getCreatedBy())) {
            existing.setCreatedBy(newData.getCreatedBy());
        }
        if (hasValue.apply(newData.getSourceQuestionId())) {
            existing.setSourceQuestionId(newData.getSourceQuestionId());
        }
        
        // ===== STATISTICS =====
        // Increment totalEdit counter
        existing.setTotalEdit(existing.getTotalEdit() + 1);
        // Preserve views count (don't reset)
        if (newData.getViews() > existing.getViews()) {
            existing.setViews(newData.getViews());
        }
        
        // ===== PRESERVE VERSIONING FIELDS =====
        // Don't overwrite versioning fields from newData
        // They are handled separately in the calling method
        
        android.util.Log.d("FirestoreUtil", "=== MERGE DATA MODEL COMPLETE ===");
        android.util.Log.d("FirestoreUtil", "Preserved existing fields, updated only non-empty fields from newData");
    }
    
    /**
     * Clean empty fields from DataModel to prevent them from overwriting existing data in Firestore.
     * When using SetOptions.merge(), empty strings will still overwrite existing values.
     * This method sets empty strings to null so they won't be included in the merge.
     */
    private static void cleanEmptyFields(DataModel data) {
        // Set empty strings to null so they won't overwrite existing data in Firestore merge
        if (data.getQuestion() != null && data.getQuestion().trim().isEmpty()) {
            // Keep question even if empty (might be intentional)
        }
        if (data.getBase64() != null && data.getBase64().trim().isEmpty()) {
            data.setBase64(null);
        }
        if (data.getBase64_2() != null && data.getBase64_2().trim().isEmpty()) {
            data.setBase64_2(null);
        }
        if (data.getBase64_3() != null && data.getBase64_3().trim().isEmpty()) {
            data.setBase64_3(null);
        }
        if (data.getBase64_4() != null && data.getBase64_4().trim().isEmpty()) {
            data.setBase64_4(null);
        }
        if (data.getBase64_5() != null && data.getBase64_5().trim().isEmpty()) {
            data.setBase64_5(null);
        }
        if (data.getPhotoAnswer() != null && data.getPhotoAnswer().trim().isEmpty()) {
            data.setPhotoAnswer(null);
        }
        if (data.getPhotoAnswerUrl() != null && data.getPhotoAnswerUrl().trim().isEmpty()) {
            data.setPhotoAnswerUrl(null);
        }
        if (data.getQuestionImageUrl1() != null && data.getQuestionImageUrl1().trim().isEmpty()) {
            data.setQuestionImageUrl1(null);
        }
        if (data.getQuestionImageUrl2() != null && data.getQuestionImageUrl2().trim().isEmpty()) {
            data.setQuestionImageUrl2(null);
        }
        if (data.getQuestionImageUrl3() != null && data.getQuestionImageUrl3().trim().isEmpty()) {
            data.setQuestionImageUrl3(null);
        }
        if (data.getQuestionImageUrl4() != null && data.getQuestionImageUrl4().trim().isEmpty()) {
            data.setQuestionImageUrl4(null);
        }
        if (data.getQuestionImageUrl5() != null && data.getQuestionImageUrl5().trim().isEmpty()) {
            data.setQuestionImageUrl5(null);
        }
        // Note: We don't clean other fields as they might be intentionally empty
        // The merge logic will handle preserving existing values
    }

    /**
     * Add a document with auto-generated ID.
     */
    /**
     * Add a new document to a collection (auto-generates document ID).
     * Collection name akan di-version secara otomatis oleh VersionHelper.
     */
    public static <T> void addDocument(
            String collectionPath,
            T data,
            OnSuccessWithIdCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        firestore.collection(versionedCollection)
                .add(data)
                .addOnSuccessListener(documentRef -> onSuccess.onSuccess(documentRef.getId()))
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }

    /**
     * Get a document by its ID.
     * Collection name akan di-version secara otomatis oleh VersionHelper.
     */
    public static <T> void getDocument(
            String collectionPath,
            String documentId,
            Class<T> clazz,
            OnSuccessWithDataCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        firestore.collection(versionedCollection)
                .document(documentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    T data = snapshot.exists() ? snapshot.toObject(clazz) : null;
                    onSuccess.onSuccess(data);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }

    /**
     * Get all documents in a collection with retry mechanism.
     */
    public static <T> void getAllDocuments(
            String collectionPath,
            Class<T> clazz,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        getAllDocumentsWithRetry(collectionPath, clazz, onSuccess, onFailure, 3);
    }

    /**
     * Get all documents in a collection with retry mechanism.
     */
    public static <T> void getAllDocuments(
            String collectionPath,
            Class<T> clazz,
            String field,
            Object value,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        getAllDocumentsWithRetry(collectionPath, clazz, field, value, onSuccess, onFailure, 3);
    }

    /**
     * Get all documents with retry mechanism.
     * Collection name akan di-version secara otomatis oleh VersionHelper.
     */
    private static <T> void getAllDocumentsWithRetry(
            String collectionPath,
            Class<T> clazz,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure,
            int retryCount
    ) {
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        firestore.collection(versionedCollection)
                .get()
                .addOnSuccessListener(snapshot -> {
                    android.util.Log.d("FirestoreUtil", "getAllDocuments: Retrieved " + snapshot.size() + " documents from " + versionedCollection + " (base: " + collectionPath + ")");
                    List<T> dataList = new ArrayList<>();
                    int successCount = 0;
                    int failCount = 0;
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            T data = null;
                            
                            // Special handling for DataModel to fix null latitude/longitude before deserialization
                            if (clazz == DataModel.class) {
                                Map<String, Object> docData = document.getData();
                                if (docData != null) {
                                    // Fix null latitude/longitude (Firestore cannot deserialize null to primitive double)
                                    boolean needsFix = false;
                                    Map<String, Object> fixedData = new HashMap<>(docData);
                                    
                                    if (fixedData.containsKey("latitude") && fixedData.get("latitude") == null) {
                                        fixedData.put("latitude", 0.0);
                                        needsFix = true;
                                    }
                                    if (fixedData.containsKey("longitude") && fixedData.get("longitude") == null) {
                                        fixedData.put("longitude", 0.0);
                                        needsFix = true;
                                    }
                                    // Fix null lastPhotoUpdate if missing (primitive long cannot be null)
                                    if (!fixedData.containsKey("lastPhotoUpdate") || fixedData.get("lastPhotoUpdate") == null) {
                                        fixedData.put("lastPhotoUpdate", System.currentTimeMillis());
                                        needsFix = true;
                                    }
                                    
                                    // Try deserialization first
                                    try {
                                        data = document.toObject(clazz);
                                    } catch (Exception deserializeException) {
                                        android.util.Log.w("FirestoreUtil", "Deserialization failed for " + document.getId() + ", creating manually: " + deserializeException.getMessage());
                                        // If deserialization failed, create DataModel manually from fixed data
                                        try {
                                            data = (T) createDataModelFromMap(fixedData, document.getId());
                                            android.util.Log.d("FirestoreUtil", "Successfully created DataModel manually for " + document.getId());
                                        } catch (Exception manualException) {
                                            android.util.Log.e("FirestoreUtil", "Failed to create DataModel manually: " + manualException.getMessage());
                                            data = null;
                                        }
                                    }
                                    
                                    // If we fixed null values and have valid data, update Firestore document (async, don't wait)
                                    if (needsFix && data != null) {
                                        firestore.collection(versionedCollection)
                                            .document(document.getId())
                                            .update(fixedData)
                                            .addOnSuccessListener(aVoid -> {
                                                android.util.Log.d("FirestoreUtil", "Fixed null values in document " + document.getId());
                                            })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.w("FirestoreUtil", "Failed to fix null values: " + e.getMessage());
                                            });
                                    }
                                }
                            } else {
                                // For other classes, use standard deserialization
                                data = document.toObject(clazz);
                            }
                            
                            if (data != null) {
                                dataList.add(data);
                                successCount++;
                            } else {
                                android.util.Log.w("FirestoreUtil", "Document " + document.getId() + " parsed to null");
                                failCount++;
                            }
                        } catch (Exception e) {
                            android.util.Log.e("FirestoreUtil", "Failed to parse document " + document.getId() + ": " + e.getMessage(), e);
                            android.util.Log.e("FirestoreUtil", "Document data: " + document.getData());
                            failCount++;
                            // Skip any documents that fail to parse
                        }
                    }
                    android.util.Log.d("FirestoreUtil", "getAllDocuments: Successfully parsed " + successCount + " documents, failed " + failCount + " documents");
                    onSuccess.onSuccess(dataList);
                })
                .addOnFailureListener(e -> {
                    if (retryCount > 0) {
                        android.util.Log.w("FirestoreUtil", "Retrying getAllDocuments, attempts left: " + (retryCount - 1));
                        // Wait 1 second before retry
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            getAllDocumentsWithRetry(collectionPath, clazz, onSuccess, onFailure, retryCount - 1);
                        }, 1000);
                    } else {
                        onFailure.onFailure(e);
                    }
                });
    }

    /**
     * Get all documents with retry mechanism and field filter.
     */
    private static <T> void getAllDocumentsWithRetry(
            String collectionPath,
            Class<T> clazz,
            String field,
            Object value,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure,
            int retryCount
    ) {
        String versionedCollection = VersionHelper.getCollectionName(collectionPath);
        firestore.collection(versionedCollection)
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<T> dataList = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            T data = document.toObject(clazz);
                            if (data != null) {
                                dataList.add(data);
                            }
                        } catch (Exception e) {
                            android.util.Log.w("FirestoreUtil", "Failed to parse document " + document.getId() + ": " + e.getMessage());
                            // Skip any documents that fail to parse
                        }
                    }
                    onSuccess.onSuccess(dataList);
                })
                .addOnFailureListener(e -> {
                    if (retryCount > 0) {
                        android.util.Log.w("FirestoreUtil", "Retrying getAllDocuments with filter, attempts left: " + (retryCount - 1));
                        // Wait 1 second before retry
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            getAllDocumentsWithRetry(collectionPath, clazz, field, value, onSuccess, onFailure, retryCount - 1);
                        }, 1000);
                    } else {
                        onFailure.onFailure(e);
                    }
                });
    }

    /**
     * Get all documents from multiple collections and merge results.
     * Collection names akan di-version secara otomatis oleh VersionHelper.
     */
    public static <T> void getAllDocumentsFromMultipleCollections(
            List<String> collectionPaths,
            Class<T> clazz,
            String field,
            Object value,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        final List<T> allData = new ArrayList<>();
        final int[] completedCollections = {0};
        final boolean[] hasError = {false};

        for (String collectionPath : collectionPaths) {
            // VersionHelper akan di-handle di dalam getAllDocuments
            getAllDocuments(collectionPath, clazz, field, value,
                    data -> {
                        allData.addAll(data);
                        completedCollections[0]++;
                        
                        if (completedCollections[0] == collectionPaths.size()) {
                            onSuccess.onSuccess(allData);
                        }
                    },
                    e -> {
                        if (!hasError[0]) {
                            hasError[0] = true;
                            onFailure.onFailure(e);
                        }
                    });
        }
    }

    /**
     * Backup data from one collection to another.
     * Collection names akan di-version secara otomatis oleh VersionHelper.
     */
    public static <T> void backupCollection(
            String sourceCollection,
            String backupCollection,
            Class<T> clazz,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        getAllDocuments(sourceCollection, clazz, data -> {
            if (data.isEmpty()) {
                onSuccess.run();
                return;
            }

            WriteBatch batch = firestore.batch();
            int batchCount = 0;
            final int MAX_BATCH_SIZE = 500;
            
            // Get versioned collection names
            String versionedBackupCollection = VersionHelper.getCollectionName(backupCollection);

            for (T item : data) {
                try {
                    // Use reflection to get ID field
                    java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                    String id = (String) getIdMethod.invoke(item);
                    
                    if (id != null && !id.isEmpty()) {
                        batch.set(firestore.collection(versionedBackupCollection).document(id), item);
                        batchCount++;
                        
                        if (batchCount >= MAX_BATCH_SIZE) {
                            batch.commit();
                            batch = firestore.batch();
                            batchCount = 0;
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("FirestoreUtil", "Error backing up item: " + e.getMessage());
                }
            }

            if (batchCount > 0) {
                batch.commit()
                        .addOnSuccessListener(aVoid -> onSuccess.run())
                        .addOnFailureListener(e -> onFailure.onFailure(e));
            } else {
                onSuccess.run();
            }
        }, onFailure);
    }

    /**
     * Restore data from backup collection.
     * Collection names akan di-version secara otomatis oleh VersionHelper.
     */
    public static <T> void restoreFromBackup(
            String backupCollection,
            String targetCollection,
            Class<T> clazz,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        getAllDocuments(backupCollection, clazz, data -> {
            if (data.isEmpty()) {
                onSuccess.onSuccess(new ArrayList<>());
                return;
            }

            WriteBatch batch = firestore.batch();
            int batchCount = 0;
            final int MAX_BATCH_SIZE = 500;
            
            // Get versioned collection name
            String versionedTargetCollection = VersionHelper.getCollectionName(targetCollection);

            for (T item : data) {
                try {
                    java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                    String id = (String) getIdMethod.invoke(item);
                    
                    if (id != null && !id.isEmpty()) {
                        batch.set(firestore.collection(versionedTargetCollection).document(id), item);
                        batchCount++;
                        
                        if (batchCount >= MAX_BATCH_SIZE) {
                            batch.commit();
                            batch = firestore.batch();
                            batchCount = 0;
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("FirestoreUtil", "Error restoring item: " + e.getMessage());
                }
            }

            if (batchCount > 0) {
                batch.commit()
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(data))
                        .addOnFailureListener(e -> onFailure.onFailure(e));
            } else {
                onSuccess.onSuccess(data);
            }
        }, onFailure);
    }

    /**
     * Add or update photo documentation with metadata
     */
    public static <T> void addPhotoDocumentation(
            String collectionPath,
            String documentId,
            String photoUrl,
            String storagePath,
            String noteText,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        try {
            // Get current document
            getDocument(collectionPath, documentId, DataModel.class, 
                existingData -> {
                    if (existingData != null) {
                        // Add photo documentation to existing data
                        existingData.addPhotoDocumentation(photoUrl, storagePath);
                        
                        // Update documentation notes
                        String currentNotes = existingData.getDocumentationNotes();
                        String newNote = "Photo: " + noteText + " - " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        
                        if (currentNotes == null || currentNotes.isEmpty()) {
                            existingData.setDocumentationNotes(newNote);
                        } else {
                            existingData.setDocumentationNotes(currentNotes + "\n" + newNote);
                        }
                        
                        // Update last photo update timestamp
                        existingData.setLastPhotoUpdate(System.currentTimeMillis());
                        
                        // Save updated document
                        addOrUpdateDocument(collectionPath, documentId, existingData, onSuccess, onFailure);
                    } else {
                        onFailure.onFailure(new Exception("Document not found"));
                    }
                },
                onFailure
            );
        } catch (Exception e) {
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Add or update note photo with metadata
     */
    public static <T> void addNotePhoto(
            String collectionPath,
            String documentId,
            String photoUrl,
            String storagePath,
            String noteText,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        try {
            // Get current document
            getDocument(collectionPath, documentId, DataModel.class, 
                existingData -> {
                    if (existingData != null) {
                        // Add note photo to existing data
                        existingData.addNotePhoto(photoUrl, storagePath);
                        
                        // Update last photo update timestamp
                        existingData.setLastPhotoUpdate(System.currentTimeMillis());
                        
                        // Save updated document
                        addOrUpdateDocument(collectionPath, documentId, existingData, onSuccess, onFailure);
                    } else {
                        onFailure.onFailure(new Exception("Document not found"));
                    }
                },
                onFailure
            );
        } catch (Exception e) {
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Get all photo documentation for a user
     */
    public static void getUserPhotoDocumentation(
            String userId,
            OnSuccessWithListCallback<DataModel> onSuccess,
            OnFailureCallback onFailure
    ) {
        // Get from both collections
        List<String> collections = new ArrayList<>();
        collections.add("record");
        collections.add("questions");
        
        getAllDocumentsFromMultipleCollections(
            collections,
            DataModel.class,
            "idCustomer",
            userId,
            data -> {
                // Filter documents with photo documentation
                List<DataModel> photoDocs = new ArrayList<>();
                for (DataModel doc : data) {
                    if (doc.getPhotoDocumentation() != null && !doc.getPhotoDocumentation().isEmpty()) {
                        photoDocs.add(doc);
                    }
                }
                onSuccess.onSuccess(photoDocs);
            },
            onFailure
        );
    }
    
    /**
     * Get photo documentation by experiment ID
     */
    public static void getExperimentPhotoDocumentation(
            String collectionPath,
            String experimentId,
            OnSuccessWithDataCallback<DataModel> onSuccess,
            OnFailureCallback onFailure
    ) {
        getDocument(collectionPath, experimentId, DataModel.class, onSuccess, onFailure);
    }
    
    /**
     * Delete photo documentation
     */
    public static void deletePhotoDocumentation(
            String collectionPath,
            String documentId,
            int photoIndex,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        try {
            // Get current document
            getDocument(collectionPath, documentId, DataModel.class, 
                existingData -> {
                    if (existingData != null) {
                        // Remove photo documentation by index
                        existingData.removePhotoDocumentation(photoIndex);
                        
                        // Update last photo update timestamp
                        existingData.setLastPhotoUpdate(System.currentTimeMillis());
                        
                        // Save updated document
                        addOrUpdateDocument(collectionPath, documentId, existingData, onSuccess, onFailure);
                    } else {
                        onFailure.onFailure(new Exception("Document not found"));
                    }
                },
                onFailure
            );
        } catch (Exception e) {
            onFailure.onFailure(e);
        }
    }
    
    /**
     * Delete note photo
     */
    public static void deleteNotePhoto(
            String collectionPath,
            String documentId,
            int photoIndex,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        try {
            // Get current document
            getDocument(collectionPath, documentId, DataModel.class, 
                existingData -> {
                    if (existingData != null) {
                        // Remove note photo by index
                        existingData.removeNotePhoto(photoIndex);
                        
                        // Update last photo update timestamp
                        existingData.setLastPhotoUpdate(System.currentTimeMillis());
                        
                        // Save updated document
                        addOrUpdateDocument(collectionPath, documentId, existingData, onSuccess, onFailure);
                    } else {
                        onFailure.onFailure(new Exception("Document not found"));
                    }
                },
                onFailure
            );
        } catch (Exception e) {
            onFailure.onFailure(e);
        }
    }

    // Callback interfaces for success and failure

    public interface OnSuccessWithIdCallback {
        void onSuccess(String id);
    }

    public interface OnSuccessWithDataCallback<T> {
        void onSuccess(T data);
    }

    /**
     * Create DataModel from Map data (handles null values for primitive types)
     */
    @SuppressWarnings("unchecked")
    private static DataModel createDataModelFromMap(Map<String, Object> data, String documentId) {
        DataModel model = new DataModel();
        
        // Set basic fields
        if (data.containsKey("id")) model.setId((String) data.get("id"));
        else if (documentId != null) model.setId(documentId);
        
        if (data.containsKey("question")) model.setQuestion((String) data.get("question"));
        if (data.containsKey("topics")) model.setTopics((String) data.get("topics"));
        if (data.containsKey("typeQuestion")) model.setTypeQuestion((String) data.get("typeQuestion"));
        if (data.containsKey("typeData")) model.setTypeData((String) data.get("typeData"));
        if (data.containsKey("desc")) model.setDesc((String) data.get("desc"));
        if (data.containsKey("dateTime")) model.setDateTime((String) data.get("dateTime"));
        if (data.containsKey("customerName")) model.setCustomerName((String) data.get("customerName"));
        if (data.containsKey("idCustomer")) model.setIdCustomer((String) data.get("idCustomer"));
        if (data.containsKey("locationName")) model.setLocationName((String) data.get("locationName"));
        
        // Handle latitude/longitude (can be null, convert to 0.0)
        if (data.containsKey("latitude")) {
            Object lat = data.get("latitude");
            model.setLatitude(lat == null ? 0.0 : ((Number) lat).doubleValue());
        }
        if (data.containsKey("longitude")) {
            Object lng = data.get("longitude");
            model.setLongitude(lng == null ? 0.0 : ((Number) lng).doubleValue());
        }
        
        // Handle boolean
        if (data.containsKey("isFinished")) {
            Object finished = data.get("isFinished");
            model.setFinished(finished == null ? false : (Boolean) finished);
        }
        
        // Handle integers
        if (data.containsKey("totalEdit")) {
            Object totalEdit = data.get("totalEdit");
            model.setTotalEdit(totalEdit == null ? 0 : ((Number) totalEdit).intValue());
        }
        if (data.containsKey("views")) {
            Object views = data.get("views");
            model.setViews(views == null ? 0 : ((Number) views).intValue());
        }
        
        // Handle arrays
        if (data.containsKey("photoDraw")) {
            Object photoDraw = data.get("photoDraw");
            if (photoDraw instanceof List) {
                model.setPhotoDraw(new ArrayList<>((List<String>) photoDraw));
            }
        }
        
        // Handle string fields
        if (data.containsKey("photo")) model.setPhoto((String) data.get("photo"));
        if (data.containsKey("photoAnswerUrl")) model.setPhotoAnswerUrl((String) data.get("photoAnswerUrl"));
        if (data.containsKey("photoAnswerPath")) model.setPhotoAnswerPath((String) data.get("photoAnswerPath"));
        if (data.containsKey("creatorName")) model.setCreatorName((String) data.get("creatorName"));
        if (data.containsKey("dibuatOleh")) model.setDibuatOleh((String) data.get("dibuatOleh"));
        if (data.containsKey("createdBy")) model.setCreatedBy((String) data.get("createdBy"));
        if (data.containsKey("sourceQuestionId")) model.setSourceQuestionId((String) data.get("sourceQuestionId"));
        if (data.containsKey("storageUserId")) model.setStorageUserId((String) data.get("storageUserId"));
        if (data.containsKey("experimentPhotoUrl")) model.setExperimentPhotoUrl((String) data.get("experimentPhotoUrl"));
        if (data.containsKey("experimentPhotoPath")) model.setExperimentPhotoPath((String) data.get("experimentPhotoPath"));
        if (data.containsKey("documentationNotes")) model.setDocumentationNotes((String) data.get("documentationNotes"));
        
        // Handle "Dibuat Oleh" field (alternative name)
        if (data.containsKey("Dibuat Oleh")) {
            String dibuatOleh = (String) data.get("Dibuat Oleh");
            model.setDibuatOleh(dibuatOleh);
            model.setCreatorName(dibuatOleh);
            model.setCreatedBy(dibuatOleh);
        }
        
        // Handle arrays for photoDocumentation, notePhotos, etc.
        if (data.containsKey("photoDocumentation")) {
            Object photoDoc = data.get("photoDocumentation");
            if (photoDoc instanceof List) {
                model.setPhotoDocumentation(new ArrayList<>((List<String>) photoDoc));
            }
        }
        if (data.containsKey("notePhotos")) {
            Object notes = data.get("notePhotos");
            if (notes instanceof List) {
                model.setNotePhotos(new ArrayList<>((List<String>) notes));
            }
        }
        
        // Handle lastPhotoUpdate (can be null, use current time)
        if (data.containsKey("lastPhotoUpdate")) {
            Object lastUpdate = data.get("lastPhotoUpdate");
            model.setLastPhotoUpdate(lastUpdate == null ? System.currentTimeMillis() : ((Number) lastUpdate).longValue());
        } else {
            model.setLastPhotoUpdate(System.currentTimeMillis());
        }
        
        // Handle versioning fields
        if (data.containsKey("version")) {
            Object version = data.get("version");
            model.setVersion(version == null ? 1 : ((Number) version).intValue());
        }
        if (data.containsKey("status")) {
            model.setStatus((String) data.get("status"));
        }
        if (data.containsKey("createdAt")) {
            Object createdAt = data.get("createdAt");
            model.setCreatedAt(createdAt == null ? System.currentTimeMillis() : ((Number) createdAt).longValue());
        }
        if (data.containsKey("updatedAt")) {
            Object updatedAt = data.get("updatedAt");
            model.setUpdatedAt(updatedAt == null ? System.currentTimeMillis() : ((Number) updatedAt).longValue());
        }
        
        return model;
    }

    public interface OnSuccessWithListCallback<T> {
        void onSuccess(List<T> data);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }
}
