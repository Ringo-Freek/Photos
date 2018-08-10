package com.sequenia.photo;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Ringo on 30.06.2016.
 * Работа с файлами
 * - создает файл в нужном месте
 * - проверяет файл на существование
 */
public class FilesManager {

    /**
     * Создает файл для записи фотографии
     *
     * @return возвращает созданный файл
     * @throws IOException
     */
    public static File createJPGFileInOpenDirectory(Context context) throws IOException {
        return createFile(createOpenDirectory(context), "jpg");
    }

    public static File createJPGFileInCloseDirectory(Context context) throws IOException {
        return createFile(createCloseDirectory(context), "jpg");
    }

    /**
     * Создание закрытой директории (доступ только для приложения)
     *
     * @return - директория
     */
    public static File createCloseDirectory(Context context) {
        return isExternalStorageWritable() ? context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                : context.getFilesDir();
    }

    /**
     * Создание открытой директории
     * (файлы отображаются в проводниках)
     *
     * @return - директория
     */
    public static File createOpenDirectory(Context context) {
        return isExternalStorageWritable() ? Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) :
                context.getDir(Environment.DIRECTORY_PICTURES, Context.MODE_PRIVATE);
    }

    /**
     * Создание файла в указанной директории и с указанным расширением
     *
     * @param storageDir - директория
     * @param exp        - расширение
     * @return - созданный файл
     * @throws IOException
     */
    public static File createFile(File storageDir, String exp) throws IOException {
        // Создание имени файла
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = exp + "_" + timeStamp + "_";

        File image = File.createTempFile(imageFileName, "." + exp, storageDir);

        // Доступность файла для внешних приложений
        image.setReadable(true, false);

        return image;
    }

    /**
     * Запись битмапа в файл
     *
     * @param base - битмап, который нужно сохранить в файле
     * @throws IOException
     */
    public static String saveBitmapInFile(Context context, Bitmap base) throws IOException {
        File file = createFile(createOpenDirectory(context), "png");
        FileOutputStream fOut = new FileOutputStream(file);
        base.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        fOut.flush();
        fOut.close();
        return file.getAbsolutePath();
    }

    /**
     * @return true, если внешнее хранилище доступно для записи
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Получает ссылку на файл по уришке на курсор
    public static String getPath(Context context, Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Проверка файла на существование и
     * корректность информации
     *
     * @param uri - путь к файлу
     * @return - true - файл существует и информация в нем корректна
     */
    public static boolean checkedFile(Context context, Uri uri) {
        return checkedFile(getPath(context, uri));
    }

    /**
     * Проверка файла на существование и
     * корректность информации
     *
     * @param path - путь к файлу
     * @return - true - файл существует и информация в нем корректна
     */
    public static boolean checkedFile(String path) {
        if (path != null) {
            File file = new File(path);
            return file.exists() && file.length() > 0;
        } else {
            return false;
        }
    }

    /**
     * Удаляем все содержимое каталога
     * катинок продуктов
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Получение URI для файла
     *
     * @param context - контекст
     * @param file - файл
     * @return - URI
     */
    public static Uri getFileUri(Context context, File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
        }
        return uri;
    }

}