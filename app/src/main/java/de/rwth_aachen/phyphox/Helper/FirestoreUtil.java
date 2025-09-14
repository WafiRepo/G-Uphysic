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
     */
    public static <T> void addOrUpdateDocumentWithBackup(
            String primaryCollection,
            String backupCollection,
            String documentId,
            T data,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        // First, save to primary collection
        firestore.collection(primaryCollection)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Then save to backup collection
                    firestore.collection(backupCollection)
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
     */
    public static <T> void addOrUpdateDocument(
            String collectionPath,
            String documentId,
            T data,
            Runnable onSuccess,
            OnFailureCallback onFailure
    ) {
        firestore.collection(collectionPath)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }

    /**
     * Add a document with auto-generated ID.
     */
    public static <T> void addDocument(
            String collectionPath,
            T data,
            OnSuccessWithIdCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        firestore.collection(collectionPath)
                .add(data)
                .addOnSuccessListener(documentRef -> onSuccess.onSuccess(documentRef.getId()))
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }

    /**
     * Get a document by its ID.
     */
    public static <T> void getDocument(
            String collectionPath,
            String documentId,
            Class<T> clazz,
            OnSuccessWithDataCallback<T> onSuccess,
            OnFailureCallback onFailure
    ) {
        firestore.collection(collectionPath)
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
     */
    private static <T> void getAllDocumentsWithRetry(
            String collectionPath,
            Class<T> clazz,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure,
            int retryCount
    ) {
        firestore.collection(collectionPath)
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
        firestore.collection(collectionPath)
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

            for (T item : data) {
                try {
                    // Use reflection to get ID field
                    java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                    String id = (String) getIdMethod.invoke(item);
                    
                    if (id != null && !id.isEmpty()) {
                        batch.set(firestore.collection(backupCollection).document(id), item);
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

            for (T item : data) {
                try {
                    java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                    String id = (String) getIdMethod.invoke(item);
                    
                    if (id != null && !id.isEmpty()) {
                        batch.set(firestore.collection(targetCollection).document(id), item);
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

    public interface OnSuccessWithListCallback<T> {
        void onSuccess(List<T> data);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }
}
