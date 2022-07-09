package com.jtech.genshinswitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private String selectedGenshinPackageName;
    private String currentPackageName;
    private String networkBoosterPackage;
    private String networkBoosterName;
    private AndroidUtils androidUtils;

    ArrayList<String> installedGenshinPackages;
    Set<String> apps;
    private final int DIALOG_SELECT_DATA_MODE = 1;
    private final int DIALOG_SELECT_TARGET_MODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button switchVersionBtn = (Button) findViewById(R.id.switchVersionBtn);
        Button startGenshinBtn = (Button) findViewById(R.id.startGameBtn);
        Button startNetBoosterBtn = (Button) findViewById(R.id.StartBoosterBtn);
        Button selectNetBoosterBtn = (Button) findViewById(R.id.SelectBoosterBtn);
        Button aboutBtn = (Button) findViewById(R.id.showInfoButton);
        Button reloadBtn = (Button) findViewById(R.id.restartBtn);
        reloadBtn.setOnClickListener(v -> initSwitcher());
        aboutBtn.setOnClickListener(v -> showInfoDialog());
        startNetBoosterBtn.setOnClickListener(new StartNetBoosterBtnOnClick());
        switchVersionBtn.setOnClickListener(new SwitchVersionBtnOnclick());
        startGenshinBtn.setOnClickListener(new StartGameBtnOnClick());
        selectNetBoosterBtn.setOnClickListener(new SelectNetBoosterBtnOnClick());
        androidUtils = new AndroidUtils(this, 66);
        if (checkAppRequiredPermission()) {
            Log.d("permission", "have all needed permission");
            initSwitcher();
        } else {
            androidUtils.requestManageExternalStoragePermission();
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.QUERY_ALL_PACKAGES
                    }, 1);

        }
    }

    protected boolean checkAppRequiredPermission() {

        List<String> permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions = Arrays.asList(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.QUERY_ALL_PACKAGES

            );
        } else {
            permissions = Arrays.asList(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        }
        for (String p : permissions) {
            Log.d("permission:", "check:" + p);
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                Log.d("permission", "not have " + p);
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        androidUtils.savePermissions(requestCode, resultCode, data);
        if (requestCode == 66) {
            Log.d("GSInfo", "onActivityResult: init switcher");
            initSwitcher();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int len = permissions.length;
        for (int i = 0; i < len; i++) {
            Log.d("GSInfo", "onRequestPermissionsResult: " + String.valueOf(i) + " " + permissions[i] + " " + String.valueOf(grantResults[i]));
        }
        initSwitcher();
    }

    private void setGameVersionText() {
        TextView versionText = (TextView) findViewById(R.id.gameVersionText);
        versionText.setText(GenshinUtils.versionStringMap.get(currentPackageName));
    }

    private void initSwitcher() {
        TextView statusView = (TextView) findViewById(R.id.statusText);
        TextView infoView = (TextView) findViewById(R.id.infoText);
        // check basic permission
        if (!checkAppRequiredPermission()) {
            Log.d("GSInfo", "initSwitcher: still not have enough permission");
            infoView.setText("未获得所需权限");
            infoView.setText("请关闭应用然后重新打开，并在打开时授予相关权限");
            return;
        }
        // for android 11 and above, check permission for Android/Data dir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!androidUtils.checkDataDirPermission()) {
                Log.d("GSInfo", "initSwitcher: missing Android/Data privilege");
                statusView.setText("安卓版本11以上!");
                infoView.setText("安卓11及以上需要手动为Android/Data文件夹授权，点击下面按钮继续");

                // button to grant privileges
                Button grantBtn;
                grantBtn = (Button) findViewById(R.id.grantPrivilegeBtn);
                grantBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        androidUtils.requestDataDirPermission();
                    }
                });
                grantBtn.setVisibility(View.VISIBLE);
                return;
            }
        }
        // find installed genshin packages
        apps = androidUtils.getInstalledApps();
        installedGenshinPackages = GenshinUtils.getCurrentGenshinPackageNames(apps);
        if (installedGenshinPackages.size() < 2) {
            statusView.setText("版本检测错误");
            infoView.setText("至少需要安装两个版本的原神!");
            return;
        }

        String[] filelists = androidUtils.getFileListInData();
        ArrayList<String> genshinDataFolders = GenshinUtils.getGenshinVersionsWithData(filelists);
        if (genshinDataFolders.size() == 0) {
            statusView.setText("数据检测错误");
            infoView.setText("未发现原神数据包，请在任意一个原神客户端内下载完整数据!");
            return;
        }
        if (genshinDataFolders.size() > 1) {
            Log.d("GSInfo", "initSwitcher: more than one data folder :" + String.valueOf(genshinDataFolders.size()));
            selectGenshinVersionAndShowDialog(genshinDataFolders, DIALOG_SELECT_DATA_MODE);
            return;
        }
        currentPackageName = genshinDataFolders.get(0);
        // show switch activity
        LinearLayout initlayout = (LinearLayout) findViewById(R.id.initLayout);
        initlayout.setVisibility(View.INVISIBLE);
        LinearLayout ctrlLayout = (LinearLayout) findViewById(R.id.ctrlLayout);
        ctrlLayout.setVisibility(View.VISIBLE);
        setGameVersionText();
        networkBoosterPackage = androidUtils.getStoredStringValue("booster");
        if (apps.contains(networkBoosterPackage)) {
            networkBoosterName = androidUtils.getAppNameByPackageName(networkBoosterPackage);
        } else {
            networkBoosterName = "";
            networkBoosterPackage = "";
            androidUtils.setStoredStringValue("booster", "");
        }
        TextView networkBoosterText = (TextView) findViewById(R.id.networkBoosterId);
        networkBoosterText.setText(networkBoosterName);
        Toast.makeText(MainActivity.this, "加载完成!", Toast.LENGTH_SHORT).show();
    }

    public class SwitchVersionBtnOnclick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            selectGenshinVersionAndShowDialog(installedGenshinPackages, DIALOG_SELECT_TARGET_MODE);
        }
    }

    private void selectGenshinVersionAndShowDialog(ArrayList<String> versionToSelected, int mode) {
        String[] installed = new String[versionToSelected.size()];
        for (int i = 0; i < versionToSelected.size(); i++) {
            installed[i] = GenshinUtils.versionStringMap.get(versionToSelected.get(i));
        }
        int defaultId = versionToSelected.indexOf(currentPackageName);
        defaultId = defaultId == -1 ? 0 : defaultId;
        selectedGenshinPackageName = versionToSelected.get(defaultId);
        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        if (mode == DIALOG_SELECT_DATA_MODE) {
            singleChoiceDialog.setTitle("找到不止一个原神数据文件夹，选择保留的那个:");
        } else {
            singleChoiceDialog.setTitle("选择切换的目标版本:");
        }
        singleChoiceDialog.setSingleChoiceItems(installed, defaultId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedGenshinPackageName = versionToSelected.get(which);
                Log.d("GSInfo", "Selected: " + installed[which]);
            }
        });
        singleChoiceDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "选择版本为: " + GenshinUtils.versionStringMap.get(selectedGenshinPackageName) + "!", Toast.LENGTH_SHORT).show();
            }
        });
        if (mode == DIALOG_SELECT_DATA_MODE) {
            // delete unused data
            singleChoiceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    for (String s : versionToSelected) {
                        if (s.equals(selectedGenshinPackageName)) {
                            continue;
                        }
                        androidUtils.deleteFiledInData(s);
                    }
                    initSwitcher();
                }
            });
        } else {
            // rename data folder
            singleChoiceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (!currentPackageName.equals(selectedGenshinPackageName)) {
                        androidUtils.renameFiledInData(currentPackageName, selectedGenshinPackageName);
                        currentPackageName = selectedGenshinPackageName;
                        selectedGenshinPackageName = "";
                        setGameVersionText();
                    }
                }
            });
        }
        singleChoiceDialog.show();
    }

    private void showInfoDialog() {

        TextView tv = new TextView(this);
        tv.setText(R.string.AboutInfo);
        tv.setTextSize(16);
        tv.setLineSpacing(0, 1.5f);
        tv.setPadding(50, 50, 50, 50);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog infoDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("关于本应用")
                .setView(tv)

                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();

        infoDialog.show();
    }

    private void selectNetworkBoosterAndShowDialog() {
        ArrayList<String> boosterPackages = new ArrayList<>();
        ArrayList<String> boosters = new ArrayList<>();
        for (String s : apps) {
            String appName = androidUtils.getAppNameByPackageName(s);
            if (appName.contains("加速器")) {
                boosters.add(appName);
                boosterPackages.add(s);
            }
        }

        String[] installed = new String[boosters.size()];
        boosters.toArray(installed);
        int defaultId = -1;
        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle("选择网游加速器:");
        singleChoiceDialog.setSingleChoiceItems(installed, defaultId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                networkBoosterPackage = boosterPackages.get(which);
                networkBoosterName = boosters.get(which);
                Log.d("GSInfo", "Selected network booster:  " + boosterPackages.get(which));
            }
        });
        singleChoiceDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!networkBoosterPackage.equals("")) {
                    Toast.makeText(MainActivity.this, "设置加速器为 " + networkBoosterName + "!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        singleChoiceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                TextView networkBoosterText = (TextView) findViewById(R.id.networkBoosterId);
                networkBoosterText.setText(networkBoosterName);
                androidUtils.setStoredStringValue("booster", networkBoosterPackage);
            }
        });

        singleChoiceDialog.show();
    }

    public class SelectNetBoosterBtnOnClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            selectNetworkBoosterAndShowDialog();
        }
    }

    public class StartNetBoosterBtnOnClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (networkBoosterPackage.equals("")) {
                Toast.makeText(MainActivity.this, "未设置网游加速器!", Toast.LENGTH_SHORT).show();
                return;
            }
            openApp(networkBoosterPackage);
            finish();
        }
    }

    public class StartGameBtnOnClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            openApp(currentPackageName);
            finish();
        }
    }

    private void openApp(String packageName) {
        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(LaunchIntent);
    }

}