package de.rwth_aachen.phyphox;

import org.opencv.core.Mat;

public interface PictureCallback {
    void onPictureTaken(Mat picture);
}
