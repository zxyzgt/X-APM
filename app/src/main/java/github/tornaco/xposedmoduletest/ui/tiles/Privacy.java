package github.tornaco.xposedmoduletest.ui.tiles;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import dev.nick.tiles.tile.QuickTile;
import dev.nick.tiles.tile.QuickTileView;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.ui.activity.app.PrivacySettingsActivity;

/**
 * Created by guohao4 on 2017/11/10.
 * Email: Tornaco@163.com
 */

public class Privacy extends QuickTile {

    public Privacy(final Context context) {
        super(context);
        this.titleRes = R.string.title_privacy;
        this.iconRes = R.drawable.ic_track_changes_black_24dp;
        this.tileView = new QuickTileView(context, this) {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                Toast.makeText(context, "埋头调试中@-@", Toast.LENGTH_SHORT).show();
            }
        };
    }
}
