package de.rwth_aachen.phyphox.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.model.DataModel;

public class DataRecoveryService extends Service {
    private static final String TAG = "DataRecoveryService";
    private ExecutorService executorService;
    private FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(3);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Data Recovery Service started");
        
        // Start recovery process in background
        executorService.execute(() -> {
            performDataRecovery();
        });
        
        // Stop service after recovery is complete
        stopSelf();
        return START_NOT_STICKY;
    }

    private void performDataRecovery() {
        Log.d(TAG, "Starting data recovery process");
        
        // Step 1: Check for orphaned data in backup collections
        checkAndRecoverOrphanedData();
        
        // Step 2: Validate data integrity
        validateDataIntegrity();
        
        // Step 3: Create comprehensive backup
        createComprehensiveBackup();
        
        Log.d(TAG, "Data recovery process completed");
    }

    private void checkAndRecoverOrphanedData() {
        Log.d(TAG, "Checking for orphaned data in backup collections");
        
        // Get all backup collections
        db.collectionGroup("backup")
            .get()
            .addOnSuccessListener(snapshot -> {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String collectionPath = doc.getReference().getParent().getId();
                    if (collectionPath.startsWith("backup_")) {
                        processBackupCollection(collectionPath);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check backup collections: " + e.getMessage());
            });
    }

    private void processBackupCollection(String backupCollectionName) {
        Log.d(TAG, "Processing backup collection: " + backupCollectionName);
        
        // Extract user ID from backup collection name
        String[] parts = backupCollectionName.split("_");
        if (parts.length >= 2) {
            String userId = parts[1];
            
            // Check if user still exists and has data in main collections
            checkUserDataIntegrity(userId, backupCollectionName);
        }
    }

    private void checkUserDataIntegrity(String userId, String backupCollectionName) {
        // Check record collection
        db.collection("record")
            .whereEqualTo("idCustomer", userId)
            .get()
            .addOnSuccessListener(recordSnapshot -> {
                
                // Check questions collection
                db.collection("questions")
                    .whereEqualTo("idCustomer", userId)
                    .get()
                    .addOnSuccessListener(questionsSnapshot -> {
                        
                        int totalMainData = recordSnapshot.size() + questionsSnapshot.size();
                        
                        // If main collections have less data than backup, restore missing data
                        if (totalMainData == 0) {
                            Log.w(TAG, "User " + userId + " has no data in main collections, restoring from backup");
                            restoreUserDataFromBackup(userId, backupCollectionName);
                        } else {
                            Log.d(TAG, "User " + userId + " has " + totalMainData + " records in main collections");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check questions collection for user " + userId + ": " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check record collection for user " + userId + ": " + e.getMessage());
            });
    }

    private void restoreUserDataFromBackup(String userId, String backupCollectionName) {
        Log.d(TAG, "Restoring user data from backup: " + backupCollectionName);
        
        FirestoreUtil.restoreFromBackup(
            backupCollectionName,
            "record", // Restore to record collection
            DataModel.class,
            restoredData -> {
                Log.d(TAG, "Successfully restored " + restoredData.size() + " items for user " + userId);
                
                // Also restore to questions collection if applicable
                for (DataModel item : restoredData) {
                    if (item.getTopics() != null && (item.getTopics().equals("Custom Question") || item.getTopics().equals("Custom"))) {
                        db.collection("questions").document(item.getId()).set(item)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Restored to questions collection: " + item.getId()))
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to restore to questions collection: " + item.getId()));
                    }
                }
            },
            e -> {
                Log.e(TAG, "Failed to restore user data from backup: " + e.getMessage());
            }
        );
    }

    private void validateDataIntegrity() {
        Log.d(TAG, "Validating data integrity");
        
        // Check for documents with missing required fields
        validateCollection("record");
        validateCollection("questions");
    }

    private void validateCollection(String collectionName) {
        db.collection(collectionName)
            .get()
            .addOnSuccessListener(snapshot -> {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    validateDocument(doc, collectionName);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to validate collection " + collectionName + ": " + e.getMessage());
            });
    }

    private void validateDocument(DocumentSnapshot doc, String collectionName) {
        // Check for required fields
        if (!doc.contains("idCustomer") || doc.getString("idCustomer") == null || doc.getString("idCustomer").isEmpty()) {
            Log.w(TAG, "Document " + doc.getId() + " in " + collectionName + " missing idCustomer");
            
            // Try to infer idCustomer from other fields or mark for manual review
            markDocumentForReview(doc.getId(), collectionName, "Missing idCustomer");
        }
        
        if (!doc.contains("dateTime") || doc.getString("dateTime") == null || doc.getString("dateTime").isEmpty()) {
            Log.w(TAG, "Document " + doc.getId() + " in " + collectionName + " missing dateTime");
            
            // Add default timestamp
            addDefaultTimestamp(doc.getId(), collectionName);
        }
    }

    private void markDocumentForReview(String docId, String collectionName, String reason) {
        db.collection("review_required")
            .document(docId)
            .set(new java.util.HashMap<String, Object>() {{
                put("originalCollection", collectionName);
                put("reason", reason);
                put("timestamp", new java.util.Date());
                put("status", "pending");
            }})
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Marked document " + docId + " for review"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to mark document for review: " + e.getMessage()));
    }

    private void addDefaultTimestamp(String docId, String collectionName) {
        String defaultTimestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(new java.util.Date());
        
        db.collection(collectionName)
            .document(docId)
            .update("dateTime", defaultTimestamp)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Added default timestamp to " + docId))
            .addOnFailureListener(e -> Log.w(TAG, "Failed to add default timestamp to " + docId + ": " + e.getMessage()));
    }

    private void createComprehensiveBackup() {
        Log.d(TAG, "Creating comprehensive backup");
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(new java.util.Date());
        
        // Backup record collection
        FirestoreUtil.backupCollection(
            "record",
            "comprehensive_backup_record_" + timestamp,
            DataModel.class,
            () -> Log.d(TAG, "Comprehensive backup of record collection completed"),
            e -> Log.e(TAG, "Failed to create comprehensive backup of record collection: " + e.getMessage())
        );
        
        // Backup questions collection
        FirestoreUtil.backupCollection(
            "questions",
            "comprehensive_backup_questions_" + timestamp,
            DataModel.class,
            () -> Log.d(TAG, "Comprehensive backup of questions collection completed"),
            e -> Log.e(TAG, "Failed to create comprehensive backup of questions collection: " + e.getMessage())
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 