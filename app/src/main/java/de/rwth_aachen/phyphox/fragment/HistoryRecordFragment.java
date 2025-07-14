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

import java.util.List;

public class HistoryRecordFragment extends Fragment {
    private ActivityHistoryRecordBinding binding;
    private HistoryRecordAdapter adapter;
    private OnRecordChangedListener listener;

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

        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data -> {
            adapter.addData(data);
        }, e -> {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        binding.btnDeleteSelected.setOnClickListener(v -> {
            // Ambil data terpilih
            List<DataModel> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(getContext(), "Pilih data yang ingin dihapus", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            for (DataModel item : selected) {
                if (item.getId() != null && !item.getId().isEmpty()) {
                    db.collection("record").document(item.getId()).delete();
                }
            }
            Toast.makeText(getContext(), "Data terpilih dihapus", Toast.LENGTH_SHORT).show();
            // Refresh data
            FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data -> {
                adapter.addData(data);
                if (listener != null) listener.onRecordChanged();
            }, e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnClearAll.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data -> {
                for (DataModel item : data) {
                    if (item.getId() != null && !item.getId().isEmpty()) {
                        db.collection("record").document(item.getId()).delete();
                    }
                }
                Toast.makeText(getContext(), "Semua data dihapus", Toast.LENGTH_SHORT).show();
                // Refresh data
                FirestoreUtil.getAllDocuments("record", DataModel.class, "idCustomer", SessionManager.getId(requireContext()), data2 -> {
                    adapter.addData(data2);
                    if (listener != null) listener.onRecordChanged();
                }, e -> {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }, e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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

