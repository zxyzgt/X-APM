package github.tornaco.xposedmoduletest.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.jaredrummler.android.shell.Shell;

import java.util.List;

import dev.nick.tiles.tile.Category;
import dev.nick.tiles.tile.DashboardFragment;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.compat.os.PowerManagerCompat;
import github.tornaco.xposedmoduletest.compat.pm.PackageManagerCompat;
import github.tornaco.xposedmoduletest.provider.AppSettings;
import github.tornaco.xposedmoduletest.ui.FragmentController;
import github.tornaco.xposedmoduletest.ui.Themes;
import github.tornaco.xposedmoduletest.ui.activity.app.AboutDashboardActivity;
import github.tornaco.xposedmoduletest.ui.activity.app.AppDashboardActivity;
import github.tornaco.xposedmoduletest.ui.activity.app.GetPlayVersionActivity;
import github.tornaco.xposedmoduletest.ui.activity.app.ToolsDashboardActivity;
import github.tornaco.xposedmoduletest.ui.activity.helper.RunningServicesActivity;
import github.tornaco.xposedmoduletest.ui.activity.whyyouhere.UserGiudeActivity;
import github.tornaco.xposedmoduletest.ui.tiles.AppBoot;
import github.tornaco.xposedmoduletest.ui.tiles.AppGuard;
import github.tornaco.xposedmoduletest.ui.tiles.AppStart;
import github.tornaco.xposedmoduletest.ui.tiles.Blur;
import github.tornaco.xposedmoduletest.ui.tiles.CompReplacement;
import github.tornaco.xposedmoduletest.ui.tiles.ComponentManager;
import github.tornaco.xposedmoduletest.ui.tiles.Doze;
import github.tornaco.xposedmoduletest.ui.tiles.Greening;
import github.tornaco.xposedmoduletest.ui.tiles.Lazy;
import github.tornaco.xposedmoduletest.ui.tiles.LockKill;
import github.tornaco.xposedmoduletest.ui.tiles.NFManager;
import github.tornaco.xposedmoduletest.ui.tiles.PermControl;
import github.tornaco.xposedmoduletest.ui.tiles.Privacy;
import github.tornaco.xposedmoduletest.ui.tiles.RFKill;
import github.tornaco.xposedmoduletest.ui.tiles.SmartSense;
import github.tornaco.xposedmoduletest.ui.tiles.UnInstall;
import github.tornaco.xposedmoduletest.util.XExecutor;
import github.tornaco.xposedmoduletest.xposed.XAppBuildVar;
import github.tornaco.xposedmoduletest.xposed.app.XAppGuardManager;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;
import lombok.Getter;

/**
 * Created by guohao4 on 2017/11/10.
 * Email: Tornaco@163.com
 */

public class NavigatorActivity extends WithWithCustomTabActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Getter
    private FragmentController cardController;

    protected int getUserSetThemeResId(Themes themes) {
        return themes.getThemeStyleResNoActionBarDrawer();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_navigator);

        setupView();
        setupFragment();

        // This is a workaround that some apps is installed on SD.
        // We trigger a package scan now, to ensure wo got all packages.
        if (XAshmanManager.get().isServiceAvailable()) {
            XAshmanManager.get().forceReloadPackages();
        }

        initFirstRun();

        // showBatteryDrainFuckingDialog();
    }

    private void setupView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem play = navigationView.getMenu().findItem(R.id.action_play_version);
        MenuItem donate = navigationView.getMenu().findItem(R.id.action_donate);
        boolean isPlayVersion = XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.PLAY);
        play.setVisible(!isPlayVersion);
        donate.setVisible(!isPlayVersion);

        navigationView.setCheckedItem(R.id.action_home);
    }

    protected void setupFragment() {
        final List<? extends Fragment> cards =
                ImmutableList.of(onCreateFragment(), new ToolsDashboardActivity.Dashboards());
        cardController = new FragmentController(getSupportFragmentManager(), cards, R.id.container);
        cardController.setDefaultIndex(0);
        cardController.setCurrent(0);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void initFirstRun() {

        if (AppSettings.isFirstRun(getApplicationContext())) {
            UserGiudeActivity.start(getActivity());
            new AlertDialog.Builder(NavigatorActivity.this)
                    .setTitle(R.string.title_app_update_log)
                    .setMessage(getString(R.string.message_first_run))
                    .setCancelable(false)
                    .setNeutralButton(R.string.no_remind, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppSettings.setFirstRun(getApplicationContext());
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void showBatteryDrainFuckingDialog() {
        if (AppSettings.isShowInfoEnabled(getContext(), "showBatteryDrainFuckingDialog")) {
            new AlertDialog.Builder(NavigatorActivity.this)
                    .setTitle("关于耗电情况")
                    .setMessage("必须申明，软件无需后台运行。如果你开启了闯入抓拍，会造成系统耗电增加，因此造成的电池使用情况不理想请不要埋怨。")
                    .setPositiveButton("继续使用", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setNeutralButton("别再啰嗦", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppSettings.setShowInfo(getContext(), "showBatteryDrainFuckingDialog", false);
                        }
                    })
                    .setNegativeButton("我要卸载", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            PackageManagerCompat.unInstallUserAppWithIntent(getContext(), getPackageName());
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            navigateToWebPage(getString(R.string.app_wiki_url));
        }

        if (item.getItemId() == R.id.action_view_running_services) {
            startActivity(new Intent(this, RunningServicesActivity.class));
        }

        if (item.getItemId() == R.id.action_change_column_count) {
            boolean two = AppSettings.show2ColumnsIn(getActivity(), NavigatorActivity.class.getSimpleName());
            AppSettings.setShow2ColumnsIn(getContext(), NavigatorActivity.class.getSimpleName(), !two);
            Toast.makeText(getContext(), "Duang~~~~~~~", Toast.LENGTH_SHORT).show();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    protected Fragment onCreateFragment() {
        return new NavigatorFragment();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, AppDashboardActivity.class));
        }

        if (item.getItemId() == R.id.action_play_version) {
            GetPlayVersionActivity.start(getActivity());
        }

        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, AboutDashboardActivity.class));
        }
        if (item.getItemId() == R.id.action_tools) {
            getCardController().setCurrent(1);
            setTitle(R.string.action_tools);
        }

        if (item.getItemId() == R.id.action_home) {
            getCardController().setCurrent(0);
            setTitle(R.string.app_name);
        }

        if (item.getItemId() == R.id.action_donate) {
            startActivity(new Intent(getContext(), DonateActivity.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static class NavigatorFragment extends DashboardFragment {
        @Getter
        private View rootView;

        @Override
        protected int getLayoutId() {
            return R.layout.fragment_navigator;
        }

        @Override
        protected int getNumColumes() {
            boolean two = AppSettings.show2ColumnsIn(getActivity(), NavigatorActivity.class.getSimpleName());
            return two ? 2 : 1;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = super.onCreateView(inflater, container, savedInstanceState);
            setupView();
            return rootView;
        }

        private void setupView() {

            findView(rootView, R.id.card)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showPopMenu(v);
                        }
                    });

            TextView statusTitle = findView(rootView, android.R.id.title);
            ImageView imageView = findView(rootView, R.id.icon1);

            ViewGroup btnContainer = findView(rootView, R.id.button_container);
            Button button = findView(rootView, R.id.button);

            boolean isNewBuild = AppSettings.isNewBuild(getActivity());
            btnContainer.setVisibility(View.GONE);

            statusTitle.setText(isServiceAvailable() ?
                    R.string.title_service_connected : R.string.title_service_not_connected);

            TextView summaryView = findView(rootView, android.R.id.text1);

            if (isNewBuild) {
                summaryView.setText(R.string.app_intro_need_restart);
                ViewGroup header = findView(rootView, R.id.header1);
                header.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.amber));
                imageView.setImageResource(R.drawable.ic_error_black_24dp);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PowerManagerCompat.restartAndroid();
                    }
                });
            } else {

                TypedValue typedValue = new TypedValue();
                getActivity().getTheme().resolveAttribute(R.attr.torCardAccentBackgroundColor, typedValue, true);
                int resId = typedValue.resourceId;

                int cardAccentColor = ContextCompat.getColor(getActivity(), resId);

                summaryView.setText(R.string.app_intro);
                ViewGroup header = findView(rootView, R.id.header1);
                header.setBackgroundColor(
                        XAppGuardManager.get().isServiceAvailable() ?
                                cardAccentColor
                                : ContextCompat.getColor(getActivity(), R.color.red));
                imageView.setImageResource(isServiceAvailable()
                        ? R.drawable.ic_check_circle_black_24dp
                        : R.drawable.ic_error_black_24dp);
            }
        }

        private boolean isServiceAvailable() {
            return XAppGuardManager.get().isServiceAvailable();
        }

        @Override
        protected void onCreateDashCategories(List<Category> categories) {
            super.onCreateDashCategories(categories);
            Category category = new Category();
            category.titleRes = R.string.title_secure;

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_LOCK)) {
                category.addTile(new AppGuard(getActivity()));
            }
            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_BLUR)) {
                category.addTile(new Blur(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_UNINSTALL)) {
                category.addTile(new UnInstall(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_PRIVACY)) {
                category.addTile(new Privacy(getActivity()));
            }

            Category rest = new Category();
            rest.titleRes = R.string.title_restrict;

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_BOOT)) {
                rest.addTile(new AppBoot(getActivity()));
            }
            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_START)) {
                rest.addTile(new AppStart(getActivity()));
            }
            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_LK)) {
                rest.addTile(new LockKill(getActivity()));
            }
            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_RFK)) {
                rest.addTile(new RFKill(getActivity()));
            }

            if (AppSettings.isShowInfoEnabled(getContext(), "show_lazy_ui", false)
                    && (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_LAZY))) {
                rest.addTile(new Lazy(getActivity()));
            }

            Category ash = new Category();
            ash.titleRes = R.string.title_control;

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_COMP_EDIT)) {
                ash.addTile(new ComponentManager(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_COMP_REPLACE)) {
                ash.addTile(new CompReplacement(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_OPS)) {
                ash.addTile(new PermControl(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_SMART_SENSE)) {
                ash.addTile(new SmartSense(getActivity()));
            }

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_GREEN)) {
                ash.addTile(new Greening(getActivity()));
            }

            // Only add when firewall is enabled for this build.
            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_FIREWALL)) {
                ash.addTile(new NFManager(getActivity()));
            }

            Category battery = new Category();
            battery.titleRes = R.string.title_battery;

            if (XAppBuildVar.BUILD_VARS.contains(XAppBuildVar.APP_DOZE)) {
                battery.addTile(new Doze(getActivity()));
            }

            if (category.getTilesCount() > 0) categories.add(category);
            if (rest.getTilesCount() > 0) categories.add(rest);
            if (ash.getTilesCount() > 0) categories.add(ash);
            if (ash.getTilesCount() > 0) categories.add(battery);
        }

        @SuppressWarnings("unchecked")
        protected <T extends View> T findView(@IdRes int idRes) {
            return (T) getRootView().findViewById(idRes);
        }

        @SuppressWarnings("unchecked")
        protected <T extends View> T findView(View root, @IdRes int idRes) {
            return (T) root.findViewById(idRes);
        }

        protected void showPopMenu(View anchor) {
            PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
            popupMenu.inflate(getPopupMenuRes());
            popupMenu.setOnMenuItemClickListener(onCreateOnMenuItemClickListener());
            popupMenu.show();
        }

        private PopupMenu.OnMenuItemClickListener onCreateOnMenuItemClickListener() {
            return new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_soft_restart) {
                        executeCommandAsync("stop;start");
                    }
                    if (item.getItemId() == R.id.action_restart_rec) {
                        executeCommandAsync("reboot recovery");
                    }
                    if (item.getItemId() == R.id.action_restart_bl) {
                        executeCommandAsync("reboot bootloader");
                    }
                    return false;
                }
            };
        }

        void executeCommandAsync(final String cmd) {
            XExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Shell.SU.run(cmd);
                }
            });
        }

        public int getPopupMenuRes() {
            return R.menu.card;
        }
    }
}
