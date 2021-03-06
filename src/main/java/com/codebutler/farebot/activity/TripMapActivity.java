/*
 * TripMapActivity.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewFragment;

import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.util.Marker;
import com.codebutler.farebot.util.Utils;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.BuildConfig;
import au.id.micolous.farebot.R;

public class TripMapActivity extends Activity {
    public static final String TRIP_EXTRA = "trip";
    private static final String TAG = "TripMapActivity";

    private WebView mWebView;
    private String mTileURL;
    private String mSubdomains;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Trip trip = getIntent().getParcelableExtra(TRIP_EXTRA);
        if (trip == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_trip_map);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TripMapActivity.this);

        mTileURL = prefs.getString("pref_map_tile_url", null);
        mSubdomains = prefs.getString("pref_map_tile_subdomains", null);

        if (mTileURL == null || mTileURL.isEmpty()) {
            mTileURL = Utils.localizeString(R.string.default_map_tile_url);
        }

        if (mSubdomains == null || mSubdomains.isEmpty()) {
            mSubdomains = Utils.localizeString(R.string.default_map_tile_subdomains);
        }

        // Overwrite map preferences again with defaults if it was missing
        prefs.edit()
                .putString("pref_map_tile_url", mTileURL)
                .putString("pref_map_tile_subdomains", mSubdomains)
                .apply();

        Log.d(TAG, "TilesURL: " + mTileURL);
        Log.d(TAG, "Subdomains: " + mSubdomains);

        mWebView = ((WebViewFragment) getFragmentManager().findFragmentById(R.id.map)).getWebView();
        mWebView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUserAgentString(settings.getUserAgentString() + " metrodroid/" + BuildConfig.VERSION_NAME);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(Trip.formatStationNames(trip));
        actionBar.setSubtitle((trip.getRouteName() == null) ? trip.getAgencyName()
                : String.format("%s %s", trip.getAgencyName(), trip.getRouteName()));

        int startMarkerId = R.drawable.marker_start;
        int endMarkerId = R.drawable.marker_end;

        /* FIXME: Need icons...

        if (trip.getMode() == Trip.Mode.BUS) {
            startMarkerId = R.drawable.marker_bus_start;
            endMarkerId   = R.drawable.marker_bus_end;

        } else if (trip.getMode() == Trip.Mode.TRAIN) {
            startMarkerId = R.drawable.marker_train_start;
            endMarkerId   = R.drawable.marker_train_end;

        } else if (trip.getMode() == Trip.Mode.TRAM) {
            startMarkerId = R.drawable.marker_tram_start;
            endMarkerId   = R.drawable.marker_tram_end;

        } else if (trip.getMode() == Trip.Mode.METRO) {
            startMarkerId = R.drawable.marker_metro_start;
            endMarkerId   = R.drawable.marker_metro_end;

        } else if (trip.getMode() == Trip.Mode.FERRY) {
            startMarkerId = R.drawable.marker_ferry_start;
            endMarkerId   = R.drawable.marker_ferry_end;
        }
        */


        final List<Marker> points = new ArrayList<>();
        //LatLngBounds.Builder builder = LatLngBounds.builder();

        if (trip.getStartStation() != null) {
            points.add(new Marker(trip.getStartStation(), "start-marker"));
        }

        if (trip.getEndStation() != null) {
            points.add(new Marker(trip.getEndStation(), "end-marker"));
        }

        TripMapShim shim = new TripMapShim(points.toArray(new Marker[points.size()]), mTileURL, mSubdomains);

        mWebView.addJavascriptInterface(shim, "TripMapShim");

        mWebView.loadUrl("file:///android_asset/map.html");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    /*
        private LatLng addStationMarker(Station station, int iconId) {
            LatLng pos = new LatLng(Double.valueOf(station.getLatitude()), Double.valueOf(station.getLongitude()));
            mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(station.getStationName())
                .snippet(station.getCompanyName())
                .icon(BitmapDescriptorFactory.fromResource(iconId))
            );
            return pos;
        }
    */
    public class TripMapShim {
        private static final String TAG = "TripMapShim";
        private Marker[] mMarkers;
        private String mTileUrl;
        private String mSubdomains;

        TripMapShim(Marker[] markers, String tileUrl, String subdomains) {
            this.mMarkers = markers;
            this.mTileUrl = tileUrl;
            this.mSubdomains = subdomains;
        }

        // Lets build an interface where you can't pass arrays!
        @JavascriptInterface
        public int getMarkerCount() {
            return this.mMarkers.length;
        }

        @JavascriptInterface
        public Object getMarker(int index) {
            return this.mMarkers[index];
        }

        @JavascriptInterface
        public String getTileUrl() {
            return this.mTileUrl;
        }

        @JavascriptInterface
        public String getSubdomains() {
            return this.mSubdomains;
        }

    }

}
