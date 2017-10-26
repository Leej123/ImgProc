package com.vejoe.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import com.vejoe.imgproc.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 工具类
 * Created by Leej on 2017/4/21 0021.
 */

public class Tools {

    public static float dip2px(Context context, float dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    public static int[] getScreenSize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return new int[] {metrics.widthPixels, metrics.heightPixels};
    }

    /**
     * 显示文件选择器
     * @param ac 指定的Activity
     * @param path 指定目录，可以为空
     * @param type 文件的MIME类型
     * @param requestCode 请求码
     */
    public static void showFileChooser(Activity ac, String path, String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (!TextUtils.isEmpty(path)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(getUriForFile(ac, new File(path)), type);
        } else {
            intent.setType(type);
        }

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ac.startActivityForResult(intent, requestCode);
    }

    /**
     * 从uri中解析文件的路径
     * @param context
     * @param uri
     * @return
     */
    public static String parsePathFromUri(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(columnIndex);
                }
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * 带有认证的uri
     * @param context
     * @param file
     * @return
     */
    public static Uri getUriForFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context.getApplicationContext(), "com.vejoe.utils.fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public static File createDirectory(String dirName) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_UNMOUNTED.equals(state))
            return null;

        File rootPath = Environment.getExternalStorageDirectory();
        File dir = new File(rootPath, dirName);
        if (dir.exists())
            return dir;
        dir.mkdir();
        return dir;
    }

    /**
     * 使用振动。需要添加{@link android.Manifest.permission#VIBRATE}权限
     * @param context
     * @param stayDurationInMill 静止时长 (ms)
     * @param vibrateDurationInMill 振动时长（ms)
     * @param repeat 是否重复
     */
    public static void vibrate(Context context, long stayDurationInMill, long vibrateDurationInMill, boolean repeat) {
        Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {stayDurationInMill, vibrateDurationInMill};
        vib.vibrate(pattern, repeat? 1 : -1);
    }

    /**
     * 检测是否具有某项权限，如果已经有返回true；否则请求该权限。
     * 应用中应该覆写{@link Activity#onRequestPermissionsResult}函数，以便得知是否成功取得该权限
     * @param ac
     * @param permission
     * @return
     */
    public static boolean checkPermission(Activity ac, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(ac, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ac, new String[] {permission}, requestCode);
            return false;
        } else {
            return true;
        }
    }

    public static AlertDialog createAlertDialog(Context context, CharSequence title, CharSequence msg, boolean cancelable, DialogInterface.OnClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title))
            builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(cancelable);

        if (cancelable) {
            builder.setNegativeButton(context.getString(R.string.cancel_label), null);
        }

        builder.setPositiveButton(context.getString(R.string.confirm_label), clickListener);

        return builder.create();
    }

    public static boolean copyFileFromAssetsToSDCard(Context context, String assetsFilePath, String sdCardFilePath) {
        InputStream in = null;
        OutputStream os = null;
        boolean suc = false;
        try {
            in = context.getAssets().open(assetsFilePath);
//            File sdCardFile = new File(sdCardFilePath);
//            if (sdCardFile.exists()) {
//                sdCardFile.delete();
//            }
//            sdCardFile.createNewFile();
            os = new FileOutputStream(new File(sdCardFilePath));
            byte[] buffer = new byte[1024];
            int readLen = 0;
            while ((readLen = in.read(buffer)) > 0) {
                os.write(buffer, 0, readLen);
            }
            os.flush();
            suc = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return suc;
    }

    /**
     * 将毫秒转化成固定格式的时间
     * 时间格式: yyyy-MM-dd HH:mm:ss
     *
     * @param millisecond
     * @return
     */
    public static String getDateTimeFromMillisecond(Long millisecond){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
        Date date = new Date(millisecond);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    public static String strMd5(String data) {
        if (TextUtils.isEmpty(data))
            return "";

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(data.getBytes());
            StringBuilder strBuilder = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xFF);
                if (temp.length() == 1) {
                    strBuilder.append("0");
                }
                strBuilder.append(temp);
            }
            return strBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 验证手机号码
     * 移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
     * 联通：130、131、132、152、155、156、185、186
     * 电信：133、153、180、189、（1349卫通）
     * 总结起来就是第一位必定为1，第二位必定为3或5或8，其他位置的可以为0-9
     * @param phoneNumber
     * @return
     */
    public static boolean isPhoneNumberValid(String phoneNumber) {
        //"[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(phoneNumber))
            return false;
        String telRegex = "[1][34578]\\d{9}" ;
        return phoneNumber.matches(telRegex);
    }
}
