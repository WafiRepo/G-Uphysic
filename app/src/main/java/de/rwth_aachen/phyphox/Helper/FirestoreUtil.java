package de.rwth_aachen.phyphox.Helper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;

public class FirestoreUtil {

    private static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

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
     * Get all documents in a collection.
     */
    public static <T> void getAllDocuments(
            String collectionPath,
            Class<T> clazz,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
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
                            // Skip any documents that fail to parse
                        }
                    }
                    onSuccess.onSuccess(dataList);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e));
    }
    public static <T> void getAllDocuments(
            String collectionPath,
            Class<T> clazz,
            String field,
            Object value,
            OnSuccessWithListCallback<T> onSuccess,
            OnFailureCallback onFailure
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
                            // Skip any documents that fail to parse
                        }
                    }
                    onSuccess.onSuccess(dataList);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e));
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
