package de.rwth_aachen.phyphox.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.rwth_aachen.phyphox.R;
import android.util.Log;

public class ArtifactSliderAdapter extends RecyclerView.Adapter<ArtifactSliderAdapter.ArtifactViewHolder> {
    private final List<ArtifactItem> artifactList;
    private final Context context;

    public ArtifactSliderAdapter(Context context, List<ArtifactItem> artifactList) {
        this.context = context;
        this.artifactList = artifactList;
    }

    @NonNull
    @Override
    public ArtifactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artifact, parent, false);
        return new ArtifactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtifactViewHolder holder, int position) {
        ArtifactItem item = artifactList.get(position);
        holder.tvLocation.setText(item.location);
        holder.tvUserName.setText(item.userName);
        holder.tvProvince.setText(item.province);
        Log.d("Slider", "Image URL: " + item.imageUrl);
        holder.loadImageWithGlide(item.imageUrl);
    }

    @Override
    public int getItemCount() {
        int count = artifactList.size();
        Log.d("Slider", "Item count: " + count);
        return count;
    }

    public static class ArtifactViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArtifact;
        TextView tvLocation, tvUserName, tvProvince;
        public ArtifactViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArtifact = itemView.findViewById(R.id.imgArtifact);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvProvince = itemView.findViewById(R.id.tvProvince);
        }
        private void loadImageWithGlide(String url) {
            Glide.with(itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(imgArtifact);
        }
    }

    // Data model untuk item
    public static class ArtifactItem {
        public String imageUrl;
        public String location;
        public String userName;
        public String province;
        public ArtifactItem(String imageUrl, String location, String userName, String province) {
            this.imageUrl = imageUrl;
            this.location = location;
            this.userName = userName;
            this.province = province;
        }
    }
}
