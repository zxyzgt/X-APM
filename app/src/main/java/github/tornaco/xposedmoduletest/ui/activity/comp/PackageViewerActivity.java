package github.tornaco.xposedmoduletest.ui.activity.comp;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.common.io.Files;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import org.newstand.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.compat.os.PowerManagerCompat;
import github.tornaco.xposedmoduletest.compat.pm.PackageManagerCompat;
import github.tornaco.xposedmoduletest.loader.ComponentLoader;
import github.tornaco.xposedmoduletest.model.CommonPackageInfo;
import github.tornaco.xposedmoduletest.model.SenseAction;
import github.tornaco.xposedmoduletest.ui.activity.common.CommonPackageInfoListActivity;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoAdapter;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoViewerAdapter;
import github.tornaco.xposedmoduletest.ui.widget.SwitchBar;
import github.tornaco.xposedmoduletest.util.ArrayUtil;
import github.tornaco.xposedmoduletest.util.XExecutor;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;
import github.tornaco.xposedmoduletest.xposed.util.PkgUtil;

/**
 * Created by guohao4 on 2017/11/18.
 * Email: Tornaco@163.com
 */
@RuntimePermissions
public class PackageViewerActivity extends CommonPackageInfoListActivity {

    private boolean mShowSystemApp;

    private String disabledString = null;

    private String mAppPackageToExport = null;

    @Override
    protected void initView() {
        super.initView();
        fab.hide();
    }

    @Override
    protected void onInitSwitchBar(SwitchBar switchBar) {
        switchBar.hide();
    }

    @Override
    protected int getSummaryRes() {
        return R.string.summary_comp_edit;
    }

    @Override
    protected CommonPackageInfoAdapter onCreateAdapter() {
        CommonPackageInfoViewerAdapter adapter = new CommonPackageInfoViewerAdapter(this);
        adapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CommonPackageInfo info = getCommonPackageInfoAdapter().getCommonPackageInfos().get(position);
                showPopMenu(info, info.isDisabled(), view);
            }
        });
        return adapter;
    }

    @Override
    protected List<CommonPackageInfo> performLoading() {
        return ComponentLoader.Impl.create(this).loadInstalledApps(mShowSystemApp, ComponentLoader.Sort.byState());
    }

    private void showPopMenu(final CommonPackageInfo packageInfo, boolean isDisabledCurrently, View anchor) {
        PopupMenu popupMenu = new PopupMenu(PackageViewerActivity.this, anchor);
        popupMenu.inflate(R.menu.package_viewer_pop);
        if (isDisabledCurrently) {
            popupMenu.getMenu().findItem(R.id.action_enable_app).setVisible(true);
        } else {
            popupMenu.getMenu().findItem(R.id.action_disable_app).setVisible(true);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_disable_app:
                        XAshmanManager.get().setApplicationEnabledSetting(
                                packageInfo.getPkgName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                        startLoading();
                        break;
                    case R.id.action_enable_app:
                        XAshmanManager.get().setApplicationEnabledSetting(
                                packageInfo.getPkgName(), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                        startLoading();
                        break;
                    case R.id.action_comp_edit:
                        ComponentEditorActivity.start(getActivity(), packageInfo.getPkgName());
                        break;
                    case R.id.action_comp_uninstall:

                        if (!PkgUtil.isSystemApp(getContext(), packageInfo.getPkgName())) {
                            PackageManagerCompat.unInstallUserAppWithIntent(getContext(), packageInfo.getPkgName());
                        } else {
                            PackageManagerCompat.unInstallSystemApp(PackageViewerActivity.this,
                                    packageInfo.getPkgName(), new PackageManagerCompat.UnInstallCallback() {
                                        @Override
                                        public void onSuccess() {
                                            showTips(R.string.tips_uninstall_sys_app_success,
                                                    true,
                                                    getString(R.string.title_restart_android),
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            PowerManagerCompat.restartAndroid();
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onFail(int err) {
                                            showTips(getString(R.string.tips_uninstall_sys_app_fail) + err,
                                                    true,
                                                    null,
                                                    null);
                                        }

                                        @Override
                                        public void maybeSuccess() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    startLoading();
                                                }
                                            });
                                        }
                                    });
                        }
                        break;
                    case R.id.action_comp_details:
                        PackageManagerCompat.showAppDetails(getActivity(), packageInfo.getPkgName());
                        break;
                    case R.id.action_comp_export_apk:
                        mAppPackageToExport = packageInfo.getPkgName();
                        PackageViewerActivityPermissionRequester.pickSingleFileChecked
                                (getActivity(), REQUEST_CODE_PICK_APK_EXPORT_PATH,
                                        PackageViewerActivity.this);
                        break;

                    case R.id.action_app_focused_action:
                        onRequestAddAppFocusedAction(packageInfo.getPkgName());
                        break;
                    case R.id.action_app_unfocused_action:
                        onRequestAddAppUnFocusedAction(packageInfo.getPkgName());
                        break;

                }
                return true;
            }
        });
        popupMenu.show();
    }

    private static final int REQUEST_CODE_PICK_APK_EXPORT_PATH = 0x111;

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    static void pickSingleFile(Activity activity, int requestCode) {
        // This always works
        Intent i = new Intent(activity, FilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to getSingleton paths to the SD-card or
        // internal memory.
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        activity.startActivityForResult(i, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_APK_EXPORT_PATH && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(data);
            File file = Utils.getFileForUri(files.get(0));
            onApkExportPathPick(file);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PackageViewerActivityPermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onApkExportPathPick(final File file) {
        if (mAppPackageToExport == null) {
            Toast.makeText(getContext(), R.string.err_file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        final String appPath = PkgUtil.pathOf(getContext(), mAppPackageToExport);
        if (appPath == null) {
            Toast.makeText(getContext(), R.string.err_file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        XExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Files.copy(new File(appPath),
                            new File(file, PkgUtil.loadNameByPkgName(getContext(),
                                    mAppPackageToExport) + ".apk"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showTips(R.string.title_export_success, false, null, null);
                        }
                    });
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), Logger.getStackTraceString(e), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void onRequestAddAppFocusedAction(final String who) {
        pickAppFocusActions(new ActionReceiver() {
            @Override
            public void onActionReceived(List<String> actions) {
                String[] actionString = ArrayUtil.convertObjectArrayToStringArray(actions.toArray());
                XAshmanManager.get().addOrRemoveAppFocusAction(who, actionString, true);
            }
        });
    }

    private void onRequestAddAppUnFocusedAction(final String who) {
        pickAppFocusActions(new ActionReceiver() {
            @Override
            public void onActionReceived(List<String> actions) {
                String[] actionString = ArrayUtil.convertObjectArrayToStringArray(actions.toArray());
                XAshmanManager.get().addOrRemoveAppUnFocusAction(who, actionString, true);
            }
        });
    }

    private void pickAppFocusActions(final ActionReceiver receiver) {

        final SenseAction[] items = SenseAction.values();
        final String[] actionNames = new String[items.length];
        for (int i = 0; i < actionNames.length; i++) {
            actionNames[i] = getString(items[i].getStringRes());
        }

        final List<String> actionSet = new ArrayList<>(items.length);

        // Default all false.
        boolean[] def = new boolean[items.length];

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_action_exe_by_sort)
                .setCancelable(false)
                .setMultiChoiceItems(actionNames, def,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                SenseAction sa = items[which];
                                if (isChecked) {
                                    actionSet.add(sa.name());
                                } else {
                                    actionSet.remove(sa.name());
                                }
                            }
                        })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        receiver.onActionReceived(actionSet);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private interface ActionReceiver {
        void onActionReceived(List<String> actions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comp_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_system_app) {
            mShowSystemApp = !mShowSystemApp;
            item.setChecked(mShowSystemApp);
            startLoading();
        }
        if (item.getItemId() == R.id.action_settings) {
            CompSettingsDashboardActivity.start(getActivity());
        }
        return super.onOptionsItemSelected(item);
    }
}
