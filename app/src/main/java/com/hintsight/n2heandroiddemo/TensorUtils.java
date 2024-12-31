package com.hintsight.n2heandroiddemo;

import android.graphics.Bitmap;
import org.pytorch.executorch.Tensor;
import java.nio.FloatBuffer;

public class TensorUtils {
    public static Tensor bitmapToFloat32Tensor(final Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final FloatBuffer floatBuffer = Tensor.allocateFloatBuffer(3 * width * height);
        final int pixelsCount = width * height;
        final int[] pixels = new int[pixelsCount];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixelsCount; i++) {
            final int pixelColor = pixels[i];
            float r = (((pixelColor >> 16) & 0xff) - 127.5f) / 128.0f;
            float g = (((pixelColor >> 8) & 0xff) - 127.5f) / 128.0f;
            float b = ((pixelColor & 0xff) - 127.5f) / 128.0f;
            floatBuffer.put(i, r);
            floatBuffer.put(pixelsCount + i, g);
            floatBuffer.put(2*pixelsCount + i, b);
        }

        return Tensor.fromBlob(floatBuffer, new long[] {1, 3, height, width});
    }
}
