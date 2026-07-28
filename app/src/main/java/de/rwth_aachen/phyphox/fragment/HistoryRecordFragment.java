package de.rwth_aachen.phyphox.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.Helper.VersionHelper;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.adapter.HistoryRecordAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityHistoryRecordBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HistoryRecordFragment extends Fragment {
    private ActivityHistoryRecordBinding binding;
    private HistoryRecordAdapter adapter;
    private OnRecordChangedListener listener;
    private List<DataModel> allData;

    public interface OnRecordChangedListener {
        void onRecordChanged();
    }

    public void setOnRecordChangedListener(OnRecordChangedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityHistoryRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize adapter
        adapter = new HistoryRecordAdapter(requireContext(), requireActivity().getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvData.setAdapter(adapter);

        // Load data and update UI
        loadDataAndUpdateStats();

        // Delete selected button
        binding.btnDeleteSelected.setOnClickListener(v -> {
            List<DataModel> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(getContext(), "🔸 Pilih data yang ingin dihapus terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Konfirmasi Hapus")
                .setMessage("Yakin ingin menghapus " + selected.size() + " data yang dipilih?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    deleteSelectedData(selected);
                })
                .setNegativeButton("Batal", null)
                .show();
        });

        // Clear all button
        binding.btnClearAll.setOnClickListener(v -> {
            if (allData == null || allData.isEmpty()) {
                Toast.makeText(getContext(), "📂 Tidak ada data untuk dihapus", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔄 Konfirmasi Hapus Semua")
                .setMessage("Yakin ingin menghapus SEMUA data eksperimen? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus Semua", (dialog, which) -> {
                    clearAllData();
                })
                .setNegativeButton("Batal", null)
                .show();
        });
    }

    private void loadDataAndUpdateStats() {
        // Fetch experiment records only — "questions" collection excluded intentionally
        loadRecordData();
    }
    
    private void loadRecordData() {
        String userId = SessionManager.getId(requireContext());
        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", userId, data -> {
            allData = data;
            android.util.Log.d("HISTORY_LOAD", "Loaded " + allData.size() + " records from record collection");
            List<DataModel> filteredData = filterOutUnwantedRecords(allData);
            adapter.addData(filteredData);
            updateStatistics(filteredData);
            updateEmptyState(filteredData.isEmpty());
            createBackupIfNeeded();
        }, e -> {
            android.util.Log.e("HISTORY_LOAD", "Failed to load records: " + e.getMessage());
            loadRecordDataFallback();
        });
    }

    private void loadRecordDataFallback() {
        android.util.Log.d("HISTORY_LOAD", "Trying fallback loading method");
        String userId = SessionManager.getId(requireContext());
        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", userId, recordData -> {
            allData = recordData;
            android.util.Log.d("HISTORY_LOAD", "Fallback: Loaded " + recordData.size() + " records");
            List<DataModel> filteredData = filterOutUnwantedRecords(allData);
            adapter.addData(filteredData);
            updateStatistics(filteredData);
            updateEmptyState(filteredData.isEmpty());
            createBackupIfNeeded();
        }, recordError -> {
            android.util.Log.e("HISTORY_LOAD", "Fallback: Failed to load records, trying backup restore");
            restoreFromBackup();
        });
    }
    
    /**
     * Filter out records that should not be displayed in Record History page
     * Specifically filters out:
     * - Out Class - Buat Pertanyaan Sendiri
     * - Out Class - Buat Pertanyaan dengan AI  
     * - In Class - Buat Pertanyaan Sendiri
     * - In Class - Buat Pertanyaan dengan AI
     */
    private List<DataModel> filterOutUnwantedRecords(List<DataModel> allRecords) {
        if (allRecords == null || allRecords.isEmpty()) {
            android.util.Log.d("HISTORY_FILTER", "No records to filter");
            return new ArrayList<>();
        }
        
        android.util.Log.d("HISTORY_FILTER", "Starting filter process with " + allRecords.size() + " records");
        
        List<DataModel> filteredRecords = new ArrayList<>();
        int filteredOutCount = 0;
        
        for (DataModel record : allRecords) {
            // Log every record for debugging
            android.util.Log.d("HISTORY_FILTER_DEBUG", "Checking record: ID=" + record.getId() + 
                ", TypeData='" + record.getTypeData() + "'" +
                ", Desc='" + record.getDesc() + "'" +
                ", TypeQuestion='" + record.getTypeQuestion() + "'");
            
            // Check if this record should be filtered out
            if (shouldFilterOutRecord(record)) {
                filteredOutCount++;
                android.util.Log.d("HISTORY_FILTER", "Filtering out record: " + record.getId() + 
                    " - Type: " + record.getTypeData() + 
                    " - Desc: " + record.getDesc() + 
                    " - Reason: Out Class/In Class with Buat Pertanyaan Sendiri/AI");
                continue; // Skip this record
            }
            
            // Add record to filtered list
            filteredRecords.add(record);
            android.util.Log.d("HISTORY_FILTER_DEBUG", "Keeping record: " + record.getId() + " - Type: " + record.getTypeData());
        }
        
        if (filteredOutCount > 0) {
            android.util.Log.i("HISTORY_FILTER", "Filtered out " + filteredOutCount + " unwanted records. " +
                "Kept " + filteredRecords.size() + " valid records out of " + allRecords.size() + " total records.");
        } else {
            android.util.Log.i("HISTORY_FILTER", "No records were filtered out. All " + allRecords.size() + " records kept.");
        }
        
        return filteredRecords;
    }
    
    /**
     * Determine if a record should be filtered out from Record History
     */
    private boolean shouldFilterOutRecord(DataModel record) {
        if (record == null) return true;
        
        String typeData = record.getTypeData();
        String desc = record.getDesc();
        String typeQuestion = record.getTypeQuestion();
        
        android.util.Log.d("HISTORY_FILTER_LOGIC", "Filtering record: TypeData='" + typeData + "', Desc='" + desc + "', TypeQuestion='" + typeQuestion + "'");
        
        // ONLY filter out records that are SPECIFICALLY Out Class or In Class with Buat Pertanyaan Sendiri/AI
        // DO NOT filter out other Buat Pertanyaan Sendiri records that are not Out Class/In Class
        
        if (typeData != null) {
            // Check for Out Class - Buat Pertanyaan Sendiri/AI
            if (typeData.contains("Out Class") || typeData.contains("Out - Class") || typeData.contains("SA3")) {
                // Only filter out if description also contains Buat Pertanyaan Sendiri/AI
                if (desc != null && (desc.contains("Buat Pertanyaan Sendiri") || desc.contains("Buat Pertanyaan dengan AI"))) {
                    android.util.Log.d("HISTORY_FILTER_LOGIC", "Filtering out: SA1/Out Class with Buat Pertanyaan Sendiri/AI");
                    return true; // Filter out
                }
            }
            
            // Check for In Class - Buat Pertanyaan Sendiri/AI  
            if (typeData.contains("In Class")) {
                // Only filter out if description also contains Buat Pertanyaan Sendiri/AI
                if (desc != null && (desc.contains("Buat Pertanyaan Sendiri") || desc.contains("Buat Pertanyaan dengan AI"))) {
                    android.util.Log.d("HISTORY_FILTER_LOGIC", "Filtering out: In Class with Buat Pertanyaan Sendiri/AI");
                    return true; // Filter out
                }
            }
        }
        
        // Check typeQuestion field - but ONLY if it's combined with Out Class/In Class
        if (typeQuestion != null && typeData != null) {
            if (typeQuestion.contains("Buat Pertanyaan Sendiri") || typeQuestion.contains("Buat Pertanyaan dengan AI")) {
                // Only filter out if typeData is Out Class or In Class
                if (typeData.contains("Out Class") || typeData.contains("Out - Class") || typeData.contains("SA3") || typeData.contains("In Class")) {
                    android.util.Log.d("HISTORY_FILTER_LOGIC", "Filtering out: TypeQuestion contains Buat Pertanyaan Sendiri/AI and is Out/In Class");
                    return true; // Filter out
                }
            }
        }
        
        // IMPORTANT: Do NOT filter out records that are just "Buat Pertanyaan Sendiri" 
        // but are NOT Out Class or In Class - these should be displayed!
        
        android.util.Log.d("HISTORY_FILTER_LOGIC", "Keeping record: No filter conditions met - record will be displayed");
        return false; // Keep this record
    }
    
    /**
     * Refresh data with current filter applied
     * This method can be called from parent activity or other fragments
     */
    public void refreshDataWithFilter() {
        if (binding != null) {
            // Clear current data
            if (adapter != null) {
                adapter.clearData();
            }
            
            // Reload data with filter
            loadDataAndUpdateStats();
        }
    }
    
    /**
     * Get filtered data count for external use
     */
    public int getFilteredDataCount() {
        if (allData == null) return 0;
        List<DataModel> filteredData = filterOutUnwantedRecords(allData);
        return filteredData.size();
    }
    
    /**
     * Get total data count before filtering for external use
     */
    public int getTotalDataCount() {
        return allData != null ? allData.size() : 0;
    }
    
    /**
     * Debug method to show current data state
     */
    public void debugCurrentData() {
        if (allData == null || allData.isEmpty()) {
            android.util.Log.d("HISTORY_DEBUG", "No data available");
            return;
        }
        
        android.util.Log.d("HISTORY_DEBUG", "=== CURRENT DATA DEBUG ===");
        android.util.Log.d("HISTORY_DEBUG", "Total records: " + allData.size());
        
        for (int i = 0; i < allData.size(); i++) {
            DataModel record = allData.get(i);
            android.util.Log.d("HISTORY_DEBUG", "Record " + i + ": ID=" + record.getId() + 
                ", TypeData='" + record.getTypeData() + "'" +
                ", Desc='" + record.getDesc() + "'" +
                ", TypeQuestion='" + record.getTypeQuestion() + "'" +
                ", IsFinished=" + record.getFinished() +
                ", Collection='questions'" + // Assume questions collection for custom questions
                ", Will be filtered: " + shouldFilterOutRecord(record));
        }
        
        // Test filter
        List<DataModel> filteredData = filterOutUnwantedRecords(allData);
        android.util.Log.d("HISTORY_DEBUG", "After filtering: " + filteredData.size() + " records kept");
        
        // Show what was filtered out
        List<DataModel> filteredOutData = new ArrayList<>();
        for (DataModel record : allData) {
            if (shouldFilterOutRecord(record)) {
                filteredOutData.add(record);
            }
        }
        
        if (!filteredOutData.isEmpty()) {
            android.util.Log.d("HISTORY_DEBUG", "=== FILTERED OUT RECORDS ===");
            for (DataModel record : filteredOutData) {
                android.util.Log.d("HISTORY_DEBUG", "Filtered out: ID=" + record.getId() + 
                    ", TypeData='" + record.getTypeData() + "'" +
                    ", Desc='" + record.getDesc() + "'" +
                    ", TypeQuestion='" + record.getTypeQuestion() + "'");
            }
        }
    }
    
    /**
     * Force refresh with current filter
     */
    public void forceRefreshWithFilter() {
        android.util.Log.d("HISTORY_DEBUG", "Force refreshing with filter...");
        if (binding != null && adapter != null) {
            // Clear current display
            adapter.clearData();
            
            // Reload and filter
            loadDataAndUpdateStats();
        }
    }
    
    private void createBackupIfNeeded() {
        // DISABLED: Backup collections are no longer used
        // Collection backup_questions and backup_record have been removed
        // This method is kept for compatibility but does nothing
        return;
        
        /* DISABLED CODE - Backup collections removed
        if (allData == null || allData.isEmpty()) {
            return;
        }
        
        // Create backup collection names
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String backupCollectionName = "backup_" + SessionManager.getId(requireContext()) + "_" + timestamp;
        
        // Backup current data to backup collection
        for (DataModel item : allData) {
            if (item.getId() != null && !item.getId().isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection(backupCollectionName).document(item.getId()).set(item)
                    .addOnSuccessListener(aVoid -> android.util.Log.d("BACKUP", "Backed up: " + item.getId()))
                    .addOnFailureListener(e -> android.util.Log.w("BACKUP", "Failed to backup: " + item.getId() + " - " + e.getMessage()));
            }
        }
        
        android.util.Log.d("BACKUP", "Created backup collection: " + backupCollectionName + " with " + allData.size() + " items");
        */
    }
    
    private void restoreFromBackup() {
        // Try to find and restore from latest backup
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Get list of backup collections for this user
        db.collectionGroup("backup")
            .whereEqualTo("idCustomer", SessionManager.getId(requireContext()))
            .get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    // Find the most recent backup
                    String latestBackupCollection = null;
                    long latestTimestamp = 0;
                    
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String collectionPath = doc.getReference().getParent().getId();
                        if (collectionPath.startsWith("backup_")) {
                            try {
                                String timestampStr = collectionPath.split("_")[2]; // backup_userId_timestamp
                                long timestamp = Long.parseLong(timestampStr);
                                if (timestamp > latestTimestamp) {
                                    latestTimestamp = timestamp;
                                    latestBackupCollection = collectionPath;
                                }
                            } catch (Exception e) {
                                android.util.Log.w("BACKUP_RESTORE", "Failed to parse timestamp from: " + collectionPath);
                            }
                        }
                    }
                    
                    if (latestBackupCollection != null) {
                        android.util.Log.d("BACKUP_RESTORE", "Restoring from backup: " + latestBackupCollection);
                        restoreFromSpecificBackup(latestBackupCollection);
                    } else {
                        showNoDataMessage();
                    }
                } else {
                    showNoDataMessage();
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("BACKUP_RESTORE", "Failed to find backup collections: " + e.getMessage());
                showNoDataMessage();
            });
    }
    
    private void restoreFromSpecificBackup(String backupCollectionName) {
        FirestoreUtil.restoreFromBackup(
            backupCollectionName,
            "record", // Restore to record collection
            DataModel.class,
            restoredData -> {
                android.util.Log.d("BACKUP_RESTORE", "Successfully restored " + restoredData.size() + " items from backup");
                allData = restoredData;
                
                // Filter out unwanted records before updating UI
                List<DataModel> filteredData = filterOutUnwantedRecords(allData);
                android.util.Log.d("BACKUP_RESTORE", "After filtering restored data: " + filteredData.size() + " records (removed " + (allData.size() - filteredData.size()) + " unwanted records)");
                
                adapter.addData(filteredData);
                updateStatistics(filteredData);
                updateEmptyState(filteredData.isEmpty());
                
                Toast.makeText(getContext(), "✅ Berhasil memulihkan " + filteredData.size() + " data dari backup", Toast.LENGTH_LONG).show();
            },
            e -> {
                android.util.Log.e("BACKUP_RESTORE", "Failed to restore from backup: " + e.getMessage());
                showNoDataMessage();
            }
        );
    }
    
    private void showNoDataMessage() {
        android.util.Log.e("HISTORY_LOAD", "No data available and no backup found");
        allData = new ArrayList<>();
        adapter.addData(allData);
        updateStatistics(allData);
        updateEmptyState(true);
        
        Toast.makeText(getContext(), "❌ Tidak ada data yang tersedia. Silakan coba lagi atau hubungi admin.", Toast.LENGTH_LONG).show();
    }

    private void updateStatistics(List<DataModel> data) {
        if (binding == null) return;

        // Update total experiments count
        binding.tvTotalExperiments.setText(String.valueOf(data.size()));

        // Count today's experiments
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        int todayCount = 0;
        for (DataModel item : data) {
            if (item.getDateTime() != null && item.getDateTime().contains(today)) {
                todayCount++;
            }
        }
        binding.tvTodayProgress.setText(String.valueOf(todayCount));

        // Update level based on total experiments
        String level = calculateLevel(data.size());
        binding.tvLevel.setText(level);
    }

    private String calculateLevel(int experimentCount) {
        if (experimentCount == 0) return "🌱 Pemula";
        else if (experimentCount < 5) return "📚 Pelajar";
        else if (experimentCount < 10) return "🔬 Peneliti";
        else if (experimentCount < 20) return "🏆 Ahli";
        else if (experimentCount < 50) return "🚀 Master";
        else return "⭐ Profesor";
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding == null) return;
        
        if (isEmpty) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvData.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvData.setVisibility(View.VISIBLE);
        }
    }

    private void deleteSelectedData(List<DataModel> selected) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int totalDeleted = 0;

        for (DataModel item : selected) {
            if (item.getId() != null && !item.getId().isEmpty()) {
                db.collection(VersionHelper.getCollectionName("record")).document(item.getId()).delete()
                    .addOnSuccessListener(aVoid -> android.util.Log.d("DELETE", "Deleted from record: " + item.getId()))
                    .addOnFailureListener(e -> android.util.Log.w("DELETE", "Failed to delete from record: " + item.getId()));
                totalDeleted++;
            }
        }

        Toast.makeText(getContext(), "✅ " + totalDeleted + " data berhasil dihapus dari history", Toast.LENGTH_SHORT).show();

        loadDataAndUpdateStats();
        if (listener != null) listener.onRecordChanged();
    }

    private void clearAllData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = SessionManager.getId(requireContext());

        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", userId, recordData -> {
            if (recordData.isEmpty()) {
                Toast.makeText(getContext(), "Tidak ada data untuk dihapus", Toast.LENGTH_SHORT).show();
                return;
            }

            int deletable = 0;
            for (DataModel item : recordData) {
                if (item.getId() != null && !item.getId().isEmpty()) deletable++;
            }
            if (deletable == 0) {
                Toast.makeText(getContext(), "Tidak ada data untuk dihapus", Toast.LENGTH_SHORT).show();
                return;
            }

            final int total = deletable;
            AtomicInteger done = new AtomicInteger(0);

            Runnable onEachComplete = () -> {
                if (done.incrementAndGet() == total) {
                    deleteBufferDataFromApi(userId, total);
                }
            };

            for (DataModel item : recordData) {
                if (item.getId() != null && !item.getId().isEmpty()) {
                    db.collection(VersionHelper.getCollectionName("record")).document(item.getId()).delete()
                        .addOnCompleteListener(t -> onEachComplete.run());
                }
            }

        }, recordError -> {
            android.util.Log.e("CLEAR_ALL", "Failed to load record: " + recordError.getMessage());
            Toast.makeText(getContext(), "❌ Error: " + recordError.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteCollectionItems(FirebaseFirestore db, List<DataModel> items, String collection, int total) {
        if (total == 0) {
            Toast.makeText(getContext(), "Tidak ada data untuk dihapus", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = SessionManager.getId(requireContext());
        AtomicInteger done = new AtomicInteger(0);
        for (DataModel item : items) {
            if (item.getId() != null && !item.getId().isEmpty()) {
                db.collection(collection).document(item.getId()).delete()
                    .addOnCompleteListener(t -> {
                        if (done.incrementAndGet() == total) {
                            deleteBufferDataFromApi(userId, total);
                        }
                    });
            } else {
                done.incrementAndGet();
            }
        }
    }

    private void deleteBufferDataFromApi(String userId, int firestoreDeletedCount) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.deleteBufferData(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                requireActivity().runOnUiThread(() -> {
                    String msg = response.isSuccessful()
                            ? "Semua data berhasil dihapus (" + firestoreDeletedCount + " records)"
                            : "Data history dihapus, namun data sensor gagal dihapus (HTTP " + response.code() + ")";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    loadDataAndUpdateStats();
                    if (listener != null) listener.onRecordChanged();
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    android.util.Log.w("CLEAR_ALL", "API delete failed (offline?): " + t.getMessage());
                    Toast.makeText(getContext(), "Data history dihapus. Gagal hapus data sensor: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    loadDataAndUpdateStats();
                    if (listener != null) listener.onRecordChanged();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

// Make sure to replace your activity layout with a corresponding fragment layout in your XML files!

// Let me know if you'd like me to add navigation or handle anything else! 🚀

