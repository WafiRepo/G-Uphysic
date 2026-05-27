package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Pair;

public class VideoUtils {

    public static Bitmap extractFirstFrame(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }

    public static Pair<Integer, Integer> getVideoDimensions(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            int width = w != null ? Integer.parseInt(w) : 0;
            int height = h != null ? Integer.parseInt(h) : 0;
            return new Pair<>(width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(0, 0);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }
}
