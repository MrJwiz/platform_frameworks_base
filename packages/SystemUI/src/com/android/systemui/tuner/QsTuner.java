/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.tuner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSDragPanel;
import com.android.systemui.qs.QSPage;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Host.Callback;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.SecurityController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QsTuner extends Fragment implements Callback {

    private static final String TAG = "QsTuner";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_EDIT = Menu.FIRST + 1;

    private DraggableQsPanel mQsPanel;
    private CustomHost mTileHost;

    private ScrollView mScrollRoot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, com.android.internal.R.string.reset);
        menu.add(0, MENU_EDIT, 0, "toggle edit");
    }

    public void onResume() {
        super.onResume();
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER_QS, true);
    }

    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER_QS, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EDIT:
                mQsPanel.setEditing(!mQsPanel.isEditing());
                break;
            case MENU_RESET:
                mTileHost.resetTiles();
                break;
            case android.R.id.home:
                getFragmentManager().popBackStack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mScrollRoot = (ScrollView) inflater.inflate(R.layout.tuner_qs, container, false);

        mQsPanel = new DraggableQsPanel(getContext());
        mTileHost = new CustomHost(getContext());
        mTileHost.setCallback(this);
        mQsPanel.setTiles(mTileHost.getTiles());
        mQsPanel.setHost(mTileHost);
        mQsPanel.refreshAllTiles();
        ((ViewGroup) mScrollRoot.findViewById(R.id.all_details)).addView(mQsPanel, 0);

        return mScrollRoot;
    }

    @Override
    public void onDestroyView() {
        mTileHost.destroy();
        super.onDestroyView();
    }

    @Override
    public void onTilesChanged() {
        mQsPanel.setTiles(mTileHost.getTiles());
    }

    @Override
    public void setEditing(boolean editing) {
        mQsPanel.setEditing(editing);
    }

    @Override
    public boolean isEditing() {
        return mTileHost.isEditing();
    }

    private static int getLabelResource(String spec) {
        if (spec.equals("wifi")) return R.string.quick_settings_wifi_label;
        else if (spec.equals("bt")) return R.string.quick_settings_bluetooth_label;
        else if (spec.equals("inversion")) return R.string.quick_settings_inversion_label;
        else if (spec.equals("cell")) return R.string.quick_settings_cellular_detail_title;
        else if (spec.equals("airplane")) return R.string.airplane_mode;
        else if (spec.equals("dnd")) return R.string.quick_settings_dnd_label;
        else if (spec.equals("rotation")) return R.string.quick_settings_rotation_locked_label;
        else if (spec.equals("flashlight")) return R.string.quick_settings_flashlight_label;
        else if (spec.equals("location")) return R.string.quick_settings_location_label;
        else if (spec.equals("cast")) return R.string.quick_settings_cast_title;
        else if (spec.equals("hotspot")) return R.string.quick_settings_hotspot_label;
        else if (spec.equals("brightness")) return R.string.quick_settings_brightness_label;
        else if (spec.equals("screenoff")) return R.string.quick_settings_screen_off;
        else if (spec.equals("screenshot")) return R.string.quick_settings_screenshot_label;
        else if (spec.equals("volume")) return R.string.quick_settings_volume_panel_label;
        else if (spec.equals("headsup")) return R.string.quick_settings_heads_up_label;
        else if (spec.equals("usb_tether")) return R.string.quick_settings_usb_tether_label;
        else if (spec.equals("ambient_display")) return R.string.quick_settings_ambient_display_label;
        else if (spec.equals("nfc")) return R.string.quick_settings_nfc_label;
        else if (spec.equals("sync")) return R.string.quick_settings_sync_label;
        else if (spec.equals("timeout")) return R.string.quick_settings_timeout_label;
        else if (spec.equals("music")) return R.string.quick_settings_music_label;
        else if (spec.equals("reboot")) return R.string.quick_settings_reboot_label;
        else if (spec.equals("battery_saver")) return R.string.quick_settings_battery_saver;
        else if (spec.equals("expanded_desktop")) return R.string.quick_settings_expanded_desktop;
        else if (spec.equals("compass")) return R.string.quick_settings_compass_label;
        else if (spec.equals("adb_network")) return R.string.quick_settings_adb_network;
        return 0;
    }

    private static class CustomHost extends QSTileHost {

        public CustomHost(Context context) {
            super(context, null, null, null, null, null, null, null, null, null,
                    null, null, new BlankSecurityController());
        }

        @Override
        public QSTile<?> createTile(String tileSpec) {
            return new DraggableTile(this, tileSpec);
        }

        public void replace(String oldTile, String newTile) {
            if (oldTile.equals(newTile)) {
                return;
            }
            MetricsLogger.action(getContext(), MetricsLogger.TUNER_QS_REORDER, oldTile + ","
                    + newTile);
            List<String> order = new ArrayList<>(mTileSpecs);
            int index = order.indexOf(oldTile);
            if (index < 0) {
                Log.e(TAG, "Can't find " + oldTile);
                return;
            }
            order.remove(newTile);
            order.add(index, newTile);
            setTiles(order);
        }

        private static class BlankSecurityController implements SecurityController {
            @Override
            public boolean hasDeviceOwner() {
                return false;
            }

            @Override
            public boolean hasProfileOwner() {
                return false;
            }

            @Override
            public String getDeviceOwnerName() {
                return null;
            }

            @Override
            public String getProfileOwnerName() {
                return null;
            }

            @Override
            public boolean isVpnEnabled() {
                return false;
            }

            @Override
            public String getPrimaryVpnName() {
                return null;
            }

            @Override
            public String getProfileVpnName() {
                return null;
            }

            @Override
            public void onUserSwitched(int newUserId) {
            }

            @Override
            public void addCallback(SecurityControllerCallback callback) {
            }

            @Override
            public void removeCallback(SecurityControllerCallback callback) {
            }
        }
    }

        public static class DraggableTile extends QSTile<QSTile.State> {
        private String mSpec;
        private QSTileView mView;

        protected DraggableTile(QSTile.Host host, String tileSpec) {
            super(host);
            Log.d(TAG, "Creating tile " + tileSpec);
            mSpec = tileSpec;
        }

        @Override
        public QSTileView createTileView(Context context) {
            mView = super.createTileView(context);
            return mView;
        }

        @Override
        public boolean hasDualTargetsDetails() {
            return "wifi".equals(mSpec) || "bt".equals(mSpec);
        }

        @Override
        public void setListening(boolean listening) {
        }

        @Override
        protected QSTile.State newTileState() {
            return new QSTile.State();
        }

        @Override
        protected void handleClick() {
        }

        @Override
        protected void handleUpdateState(QSTile.State state, Object arg) {
            state.visible = true;
            state.icon = ResourceIcon.get(getIcon());
            state.label = getLabel();
        }

        private String getLabel() {
            int resource = QSTileHost.getLabelResource(mSpec);
            if (resource != 0) {
                return mContext.getString(resource);
            }
            if (mSpec.startsWith(IntentTile.PREFIX)) {
                int lastDot = mSpec.lastIndexOf('.');
                if (lastDot >= 0) {
                    return mSpec.substring(lastDot + 1, mSpec.length() - 1);
                } else {
                    return mSpec.substring(IntentTile.PREFIX.length(), mSpec.length() - 1);
                }
            }
            return mSpec;
        }

        private int getIcon() {
            if (mSpec.equals("wifi")) return R.drawable.ic_qs_wifi_full_3;
            else if (mSpec.equals("bt")) return R.drawable.ic_qs_bluetooth_connected;
            else if (mSpec.equals("inversion")) return R.drawable.ic_invert_colors_enable;
            else if (mSpec.equals("cell")) return R.drawable.ic_qs_signal_full_3;
            else if (mSpec.equals("airplane")) return R.drawable.ic_signal_airplane_enable;
            else if (mSpec.equals("dnd")) return R.drawable.ic_qs_dnd_on;
            else if (mSpec.equals("rotation")) return R.drawable.ic_portrait_from_auto_rotate;
            else if (mSpec.equals("flashlight")) return R.drawable.ic_signal_flashlight_enable;
            else if (mSpec.equals("location")) return R.drawable.ic_signal_location_enable;
            else if (mSpec.equals("cast")) return R.drawable.ic_qs_cast_on;
            else if (mSpec.equals("hotspot")) return R.drawable.ic_hotspot_enable;
            else if (mSpec.equals("brightness")) return R.drawable.ic_qs_brightness_auto_off_alpha;
            else if (mSpec.equals("screenoff")) return R.drawable.ic_qs_power;
            else if (mSpec.equals("screenshot")) return R.drawable.ic_qs_screenshot;
            else if (mSpec.equals("volume")) return R.drawable.ic_qs_volume_panel;
            else if (mSpec.equals("headsup")) return R.drawable.ic_qs_heads_up_on;
            else if (mSpec.equals("usb_tether")) return R.drawable.ic_qs_usb_tether_off;
            else if (mSpec.equals("ambient_display")) return R.drawable.ic_qs_ambientdisplay_on;
            else if (mSpec.equals("nfc")) return R.drawable.ic_qs_nfc_on;
            else if (mSpec.equals("sync")) return R.drawable.ic_qs_sync_on;
            else if (mSpec.equals("timeout")) return R.drawable.ic_qs_screen_timeout_vector;
            else if (mSpec.equals("music")) return R.drawable.ic_qs_media_play;
            else if (mSpec.equals("reboot")) return R.drawable.ic_qs_reboot;
            else if (mSpec.equals("battery_saver")) return R.drawable.ic_qs_battery_saver_on;
            else if (mSpec.equals("expanded_desktop")) return R.drawable.ic_qs_expanded_desktop;
            else if (mSpec.equals("twisted")) return R.drawable.ic_qs_twisted;
            else if (mSpec.equals("compass")) return R.drawable.ic_qs_compass_on;
            else if (mSpec.equals("adb_network")) return R.drawable.ic_qs_network_adb_on;
            return R.drawable.android;
        }

        @Override
        public int getMetricsCategory() {
            return 20000;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DraggableTile) {
                return mSpec.equals(((DraggableTile) o).mSpec);
            }
            return false;
        }

        @Override
        public String toString() {
            return mSpec;
        }
    }

    private class DraggableQsPanel extends QSDragPanel {
        public DraggableQsPanel(Context context) {
            super(context);

            setEditing(true);
        }

    }

}
