package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoUtils {
    private static final String TAG = "VideoUtils";

    public static Bitmap extractFirstFrame(Context context, Uri uri) {
        Log.d(TAG, "Starting frame extraction for URI: " + uri);

        File tempFile = FileExtensions.uriToFile(context, uri);
        if (tempFile != null && tempFile.exists()) {
            Log.d(TAG, "Temp file: " + tempFile.getAbsolutePath() + " (size: " + tempFile.length() + ")");
            Bitmap frame = extractFrameWithRetriever(tempFile.getAbsolutePath());
            if (frame != null) return frame;

            Log.w(TAG, "MediaMetadataRetriever failed, trying MediaCodec path");
            frame = extractFrameWithMediaCodec(tempFile.getAbsolutePath());
            if (frame != null) return frame;
        }

        Log.w(TAG, "Temp file unavailable, trying MediaMetadataRetriever with AFD/URI");
        Bitmap frame = extractFrameWithRetriever(context, uri);
        if (frame != null) return frame;

        Log.e(TAG, "All frame extraction methods failed");
        return null;
    }

    private static Bitmap extractFrameWithRetriever(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            Log.d(TAG, "Video duration: " + duration + "ms");

            Bitmap frame = null;
            frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame == null) frame = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame == null) frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame == null) frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC);

            if (frame != null) {
                Log.d(TAG, "Retriever success: " + frame.getWidth() + "x" + frame.getHeight());
            }
            return frame;
        } catch (Exception e) {
            Log.e(TAG, "Retriever error", e);
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private static Bitmap extractFrameWithRetriever(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            if (afd != null) {
                retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } else {
                retriever.setDataSource(context, uri);
            }

            Bitmap frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame == null) frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame != null) Log.d(TAG, "Retriever (AFD) success: " + frame.getWidth() + "x" + frame.getHeight());
            return frame;
        } catch (Exception e) {
            Log.e(TAG, "Retriever AFD error", e);
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private static Bitmap extractFrameWithMediaCodec(String filePath) {
        MediaExtractor extractor = null;

        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filePath);

            int trackIndex = -1;
            MediaFormat videoFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat fmt = extractor.getTrackFormat(i);
                String mime = fmt.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    trackIndex = i;
                    videoFormat = fmt;
                    break;
                }
            }
            if (trackIndex < 0) {
                Log.e(TAG, "No video track found");
                return null;
            }

            String mime = videoFormat.getString(MediaFormat.KEY_MIME);
            int width = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            Log.d(TAG, "MediaCodec decoding: " + mime + " " + width + "x" + height);

            List<String> decoderNames = getDecoderNames(mime);
            Log.d(TAG, "Available decoders for " + mime + ": " + decoderNames);

            extractor.selectTrack(trackIndex);

            for (String decoderName : decoderNames) {
                Log.d(TAG, "Trying decoder: " + decoderName);
                Bitmap result = decodeWithSpecificCodec(extractor, videoFormat, decoderName, width, height);
                if (result != null) return result;
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            }

            Log.e(TAG, "All decoders failed for " + mime);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "MediaCodec error", e);
            return null;
        } finally {
            if (extractor != null) {
                try { extractor.release(); } catch (Exception ignored) {}
            }
        }
    }

    private static List<String> getDecoderNames(String mime) {
        List<String> names = new ArrayList<>();
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo info : codecList.getCodecInfos()) {
            if (info.isEncoder()) continue;
            for (String type : info.getSupportedTypes()) {
                if (type.equals(mime)) {
                    names.add(info.getName());
                    break;
                }
            }
        }
        Collections.reverse(names);
        return names;
    }

    private static Bitmap decodeWithSpecificCodec(MediaExtractor extractor, MediaFormat format, String decoderName, int width, int height) {
        MediaCodec decoder = null;
        ImageReader reader = null;

        try {
            reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3);
            decoder = MediaCodec.createByCodecName(decoderName);
            decoder.configure(format, reader.getSurface(), null, 0);
            decoder.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            Bitmap result = null;
            boolean inputDone = false;
            int attempts = 50;

            while (result == null && attempts-- > 0) {
                if (!inputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            int flags = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                                    ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), flags);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outputIndex >= 0) {
                    decoder.releaseOutputBuffer(outputIndex, true);
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        result = yuv420888ToBitmap(image);
                        image.close();
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "Output format changed for " + decoderName);
                }
            }

            if (result != null) {
                Log.d(TAG, "Decoder " + decoderName + " success: " + result.getWidth() + "x" + result.getHeight());
            }
            return result;
        } catch (Exception e) {
            Log.w(TAG, "Decoder " + decoderName + " failed: " + e.getMessage());
            return null;
        } finally {
            if (decoder != null) {
                try { decoder.stop(); } catch (Exception ignored) {}
                try { decoder.release(); } catch (Exception ignored) {}
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static Bitmap yuv420888ToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int yRowStride = yPlane.getRowStride();
        int uRowStride = uPlane.getRowStride();
        int vRowStride = vPlane.getRowStride();
        int uPixelStride = uPlane.getPixelStride();
        int vPixelStride = vPlane.getPixelStride();

        byte[] nv21 = new byte[width * height * 3 / 2];
        byte[] row = new byte[yRowStride];

        for (int rowIdx = 0; rowIdx < height; rowIdx++) {
            yBuffer.position(rowIdx * yRowStride);
            yBuffer.get(row, 0, Math.min(yRowStride, width));
            System.arraycopy(row, 0, nv21, rowIdx * width, width);
        }

        int uvHeight = height / 2;
        int uvWidth = width / 2;
        int ySize = width * height;

        for (int rowIdx = 0; rowIdx < uvHeight; rowIdx++) {
            for (int col = 0; col < uvWidth; col++) {
                int uvOffset = ySize + rowIdx * width + col * 2;
                int uOffset = rowIdx * uRowStride + col * uPixelStride;
                int vOffset = rowIdx * vRowStride + col * vPixelStride;

                if (uOffset < uBuffer.capacity() && vOffset < vBuffer.capacity()) {
                    nv21[uvOffset] = vBuffer.get(vOffset);
                    nv21[uvOffset + 1] = uBuffer.get(uOffset);
                }
            }
        }

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, out);
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
    }

    public static Pair<Integer, Integer> getVideoDimensions(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            File tempFile = FileExtensions.uriToFile(context, uri);
            if (tempFile != null && tempFile.exists()) {
                retriever.setDataSource(tempFile.getAbsolutePath());
            } else {
                retriever.setDataSource(context, uri);
            }
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            int width = w != null ? Integer.parseInt(w) : 0;
            int height = h != null ? Integer.parseInt(h) : 0;
            return new Pair<>(width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(0, 0);
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }
}
