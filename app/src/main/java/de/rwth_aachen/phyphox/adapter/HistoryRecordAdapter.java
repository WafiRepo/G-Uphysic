package de.rwth_aachen.phyphox.adapter;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.DetailRecordActivity;
import de.rwth_aachen.phyphox.activity.GeneratesActivity;
import de.rwth_aachen.phyphox.activity.GeneratesClassQuestionActivity;
import de.rwth_aachen.phyphox.activity.RecordPreviewActivity;
import de.rwth_aachen.phyphox.model.DataModel;

public class HistoryRecordAdapter extends RecyclerView.Adapter<HistoryRecordAdapter.ViewHolder> {
    private Context context;
    private Application application;
    private List<DataModel> progressList;
    private List<Boolean> selectedList = new ArrayList<>();

    public HistoryRecordAdapter(Context context, Application application) {
        this.context = context;
        this.application = application;
        this.progressList = new ArrayList<>();
    }

    public void addData(List<DataModel> progressList) {
        this.progressList = progressList;
        selectedList = new ArrayList<>();
        for (int i = 0; i < progressList.size(); i++) selectedList.add(false);
        notifyDataSetChanged();
    }
    
    /**
     * Clear all data from adapter
     */
    public void clearData() {
        this.progressList.clear();
        this.selectedList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModel item = progressList.get(position);

        // Set topic title
        String topicTitle = item.getTopics();
        if (topicTitle == null || topicTitle.isEmpty()) {
            topicTitle = "Eksperimen Fisika";
        }
        holder.tvTopic.setText(topicTitle);

        // Set type info with consistent difficulty format
        String typeInfo = item.getTypeQuestion();
        if (typeInfo == null || typeInfo.isEmpty()) {
            typeInfo = "Sedang"; // Default to medium instead of Advanced
        }
        String formattedDifficulty = getDifficultyWithEmoji(typeInfo);
        holder.tvType.setText(formattedDifficulty);

        // Set checkbox state and ensure visibility
        holder.cbSelect.setVisibility(View.VISIBLE);
        holder.cbSelect.setChecked(selectedList.get(position));
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedList.set(position, isChecked);
        });

        // Handle finished vs in-progress states
        if (item.getFinished() != null && item.getFinished()) {
            // Completed experiment
            holder.ivCompletedAction.setVisibility(View.VISIBLE);
            holder.cvEdit.setVisibility(View.VISIBLE);
            holder.cvContinue.setVisibility(View.GONE);
            holder.tvStatus.setText("✅ Selesai");
            
            // Click to view details
            holder.itemView.setOnClickListener(view -> {
                App app = (App) ((Application) context.getApplicationContext());
                app.setDataModel(item);
                Intent intent = new Intent(context, RecordPreviewActivity.class);
                context.startActivity(intent);
            });

            // Edit button click
            holder.cvEdit.setOnClickListener(view -> {
                navigateToExperiment(item, false);
            });

        } else {
            // In-progress experiment
            holder.ivCompletedAction.setVisibility(View.GONE);
            holder.cvEdit.setVisibility(View.GONE);
            holder.cvContinue.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("🔄 Berlanjut");

            // Continue button click
            holder.cvContinue.setOnClickListener(view -> {
                navigateToExperiment(item, false);
            });
        }
    }

    private void navigateToExperiment(DataModel item, boolean isFromMainMenu) {
        Intent intent;
        if (item.getTopics().equals("Out Class") || item.getTopics().equals("In Class")) {
            intent = new Intent(context, GeneratesClassQuestionActivity.class);
        } else {
            intent = new Intent(context, GeneratesActivity.class);
        }
        intent.putExtra("isFromMainMenu", isFromMainMenu);
        App app = (App) application;
        app.setDataModel(item);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    public List<DataModel> getSelectedItems() {
        List<DataModel> selected = new ArrayList<>();
        for (int i = 0; i < progressList.size(); i++) {
            if (selectedList.get(i)) selected.add(progressList.get(i));
        }
        return selected;
    }

    private String getDifficultyWithEmoji(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "easy":
            case "mudah":
                return "🟢 Mudah";
            case "medium":
            case "sedang":
            case "intermediate":
                return "🟡 Sedang";
            case "hard":
            case "sulit":
            case "lanjutan":
            case "advanced":
                return "🔴 Sulit";
            default:
                return "🟡 Sedang"; // Default to medium
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvType, tvStatus, tvDifficultyBadge;
        ImageView ivCompletedAction;
        CheckBox cbSelect;
        CardView cvAdvanced, cvContinue, cvEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvType = itemView.findViewById(R.id.tvType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDifficultyBadge = itemView.findViewById(R.id.tvDifficultyBadge);
            ivCompletedAction = itemView.findViewById(R.id.ivCompletedAction);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            cvAdvanced = itemView.findViewById(R.id.cvAdvanced);
            cvContinue = itemView.findViewById(R.id.cvContinue);
            cvEdit = itemView.findViewById(R.id.cvEdit);
        }
    }
}

