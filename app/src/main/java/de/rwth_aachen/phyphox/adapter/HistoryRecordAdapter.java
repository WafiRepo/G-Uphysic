package de.rwth_aachen.phyphox.adapter;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.DetailRecordActivity;
import de.rwth_aachen.phyphox.activity.FeedbackActivity;
import de.rwth_aachen.phyphox.activity.GeneratesActivity;
import de.rwth_aachen.phyphox.activity.GeneratesClassQuestionActivity;
import de.rwth_aachen.phyphox.model.DataModel;

public class HistoryRecordAdapter extends RecyclerView.Adapter<HistoryRecordAdapter.ViewHolder> {
    private Context context;
    private Application application;
    private List<DataModel> progressList;
    private List<Boolean> selectedList = new ArrayList<>();

    public HistoryRecordAdapter(Context context, Application application) {
        this.context = context;
        this.application= application;
        this.progressList = new ArrayList<>();
    }

    public void addData(List<DataModel> progressList) {
        this.progressList = progressList;
        selectedList = new ArrayList<>();
        for (int i = 0; i < progressList.size(); i++) selectedList.add(false);
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

        holder.tvTopic.setText(item.getTopics());
        holder.tvType.setText(item.getTypeQuestion());
        holder.cbSelect.setChecked(selectedList.get(position));
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedList.set(position, isChecked);
        });

        if (item.getFinished()) {
            holder.ivCompletedAction.setVisibility(View.VISIBLE);
            holder.tvEdit.setVisibility(View.VISIBLE);
            holder.tvInProgressAction.setVisibility(View.GONE);
            holder.tvStatus.setText("Completed");
            holder.tvStatus.setBackgroundResource(R.drawable.background_8_gray);
            holder.itemView.setOnClickListener(view -> {
                App app = (App) ((Application) context.getApplicationContext());
                app.setDataModel(item);
                Intent intent = new Intent(context, DetailRecordActivity.class);
                context.startActivity(intent);
            });
            holder.tvEdit.setOnClickListener(view -> {
                if (item.getTopics().equals("Out Class") || item.getTopics().equals("In Class")) {
                    Intent intent = new Intent(context, GeneratesClassQuestionActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    App app = (App) application;
                    app.setDataModel(item);
                    context.startActivity(intent);
                }else{
                    Intent intent = new Intent(context, GeneratesActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    App app = (App) application;
                    app.setDataModel(item);
                    context.startActivity(intent);
                }
            });
        } else {
            holder.ivCompletedAction.setVisibility(View.GONE);
            holder.tvInProgressAction.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("In Progress");
            holder.tvStatus.setBackgroundResource(R.drawable.background_8_orange);
            holder.tvInProgressAction.setOnClickListener(view -> {
                if (item.getTopics().equals("Out Class") || item.getTopics().equals("In Class")) {
                    Intent intent = new Intent(context, GeneratesClassQuestionActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    App app = (App) application;
                    app.setDataModel(item);
                    context.startActivity(intent);
                }else{
                    Intent intent = new Intent(context, GeneratesActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    App app = (App) application;
                    app.setDataModel(item);
                    context.startActivity(intent);
                }

            });
        }
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvType, tvStatus, tvInProgressAction, tvEdit;
        Button btnAction;
        ImageView ivCompletedAction;
        CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvType = itemView.findViewById(R.id.tvType);
            tvEdit = itemView.findViewById(R.id.tvEdit);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvInProgressAction = itemView.findViewById(R.id.tvInProgressAction);
            ivCompletedAction = itemView.findViewById(R.id.ivCompletedAction);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}

