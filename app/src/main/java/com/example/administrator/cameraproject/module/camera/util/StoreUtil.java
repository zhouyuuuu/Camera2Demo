package com.example.administrator.cameraproject.module.camera.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

import com.example.administrator.cameraproject.module.camera.config.CameraConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StoreUtil {

    /**
     * 载入适合ImageView尺寸的本地图片
     * @param fileName 文件名
     * @param imageView ImageView
     * @return Bitmap
     */
    public static Bitmap loadBitmapThumbnail(String fileName, ImageView imageView) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        String path = CameraConfig.PHOTO_PATH + fileName;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        options.inSampleSize = width / imageView.getWidth();
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path,options);
    }

    /**
     * 保存图片
     * @param bitmap 图片
     * @param fileName 文件名
     */
    public static void saveBitmap(@NonNull Bitmap bitmap, String fileName) {
        // 如果不存在该文件夹则创建
        File folder = new File(CameraConfig.PHOTO_PATH);
        if (!folder.exists()||!folder.isDirectory()){
            if (!folder.mkdir()) {
                return;
            }
        }
        File f = new File(CameraConfig.PHOTO_PATH, fileName);
        if (f.exists()) {
            boolean deleted = f.delete();
            if (!deleted) return;
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
