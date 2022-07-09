package com.jtech.genshinswitcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AndroidUtils {
    private Activity context;
    private Uri dataUri;
    private final String preferenceName = "GenshinSwitcher";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int requestCode;
    private PackageManager pm;

    public AndroidUtils(Activity context, int requestCode) {
        this.requestCode = requestCode;
        this.context = context;
        preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        editor = preferences.edit();
        DocumentFile document = DocumentFile.fromTreeUri(context, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"));
        dataUri = document.getUri();
        this.pm = context.getPackageManager();
    }

    public void setStoredStringValue(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    public String getStoredStringValue(String key) {
        return preferences.getString(key, "");
    }

    public Set<String> getInstalledApps() {
        Set<String> apps = new HashSet<String>();
        List<PackageInfo> list = this.pm.getInstalledPackages(PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        for (PackageInfo p : list) {
            int flags = p.applicationInfo.flags;
            // system app
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            } else {
                apps.add(p.applicationInfo.packageName);
                Log.d("GSInfo", "getApps: " + p.applicationInfo.packageName);
            }
        }
        return apps;
    }

    public String getAppNameByPackageName(String packageName) {
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return info.applicationInfo.loadLabel(pm).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(context, "需要全部文件访问权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivityForResult(intent, 1);
            }
        }
    }

    //申请data访问权限请在onActivityResult事件中调用savePermissions方法保存权限
    public void requestDataDirPermission() {
        Intent intent1 = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent1.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dataUri);
        context.startActivityForResult(intent1, requestCode);
    }

    public void savePermissions(int requestCode, int resultCode, Intent data) {
        if (this.requestCode != requestCode) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Uri uri = data.getData();
                if (uri == null) return;
                int flags = data.getFlags();
                context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkDataDirPermission() {
        DocumentFile documentFile = DocumentFile.fromTreeUri(this.context, this.dataUri);
        if (documentFile == null) return false;
        return documentFile.canWrite();
    }

    //删除data目录中的指定路径的文件
    public boolean deleteFiledInData(String fileName) {
        try {
            DocumentFile documentFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                documentFile = DocumentFile.fromTreeUri(this.context, dataUri);
            } else {
                String path = "/storage/emulated/0/Android/data";
                File f = new File(path);
                documentFile = DocumentFile.fromFile(f);
            }
            documentFile = documentFile.findFile(fileName);
            return documentFile.delete();
        } catch (Exception e) {
            Log.d("GSInfo", "delete: failed->" + fileName);
            e.printStackTrace();
            return false;
        }
    }

    //重命名文件
    public boolean renameFiledInData(String srcName, String targetName) {
        try {
            DocumentFile documentFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                documentFile = DocumentFile.fromTreeUri(this.context, dataUri);
            } else {
                String path = "/storage/emulated/0/Android/data";
                File f = new File(path);
                documentFile = DocumentFile.fromFile(f);
            }
            documentFile = documentFile.findFile(srcName);
            return documentFile.renameTo(targetName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //获取目录下所有文件
    public String[] getFileListInData() {
        try {
            DocumentFile documentFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                documentFile = DocumentFile.fromTreeUri(this.context, dataUri);
            } else {
                String path = "/storage/emulated/0/Android/data";
                File f = new File(path);
                documentFile = DocumentFile.fromFile(f);
            }
            DocumentFile[] documentFile1 = documentFile.listFiles();
            String[] res = new String[documentFile1.length];
            Log.d("GSInfo", "content: " + documentFile1.length);
            for (int i = 0; i < documentFile1.length; i++) {
                res[i] = documentFile1[i].getName();
                Log.d("GSInfo", "Data: " + res[i]);
            }

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean exists(DocumentFile documentFile, String name) {
        try {
            return documentFile.findFile(name).exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
