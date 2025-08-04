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
import de.rwth_aachen.phyphox.adapter.HistoryRecordAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityHistoryRecordBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data -> {
            allData = data;
            adapter.addData(data);
            updateStatistics(data);
            updateEmptyState(data.isEmpty());
        }, e -> {
            Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateEmptyState(true);
        });
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
        for (DataModel item : selected) {
            if (item.getId() != null && !item.getId().isEmpty()) {
                db.collection("record").document(item.getId()).delete();
            }
        }
        
        Toast.makeText(getContext(), "✅ " + selected.size() + " data berhasil dihapus", Toast.LENGTH_SHORT).show();
        
        // Refresh data
        loadDataAndUpdateStats();
        if (listener != null) listener.onRecordChanged();
    }

    private void clearAllData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data -> {
            for (DataModel item : data) {
                if (item.getId() != null && !item.getId().isEmpty()) {
                    db.collection("record").document(item.getId()).delete();
                }
            }
            
            Toast.makeText(getContext(), "🔄 Semua data berhasil dihapus", Toast.LENGTH_SHORT).show();
            
            // Refresh data
            loadDataAndUpdateStats();
            if (listener != null) listener.onRecordChanged();
            
        }, e -> {
            Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

