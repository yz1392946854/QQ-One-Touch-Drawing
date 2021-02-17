package com.yzzz.eulerpath.util;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;

import com.orhanobut.logger.Logger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

/**
 * @author yzzz
 * @create 2021/2/13
 */
public class GBData {
    private static final String TAG = "GBData";
    public static ImageReader reader;

    private static Bitmap bitmap;

    /**
     * 获取目标点的RGB值
     */
    public static int getColor(int x, int y) {
        if (reader == null) {
            Logger.t(TAG).d("reader is null");
            return -1;
        }
        Image image = reader.acquireLatestImage();
        if (image == null) {
            if (bitmap == null) {
                Logger.t(TAG).d("image is null");
                return -1;
            }
            return bitmap.getPixel(x, y);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        }
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        return bitmap.getPixel(x, y);
    }

    /**
     * 获取⚪的位置
     * @return
     */
    public static Mat getCircles(){
        Image image = reader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        }
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap,mat);
        Mat m = new Mat();
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2GRAY, 1);
        Imgproc.HoughCircles(mat,m , Imgproc.HOUGH_GRADIENT, 1, 18, 500, 50, 20, 250);
        return m;
    }
}
