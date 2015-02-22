package com.hackaton.findme;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    static String REQUESTING_LOCATION_KEY;
    static String LOCATION_KEY;
    static int MY_FRIEND_ID;
    GoogleApiClient GoogleAC;
    MapFragment mFragment;
    LocationRequest locRequest;
    Location currentLocation;
    boolean reqLocationUpdates = true;

    //Variables pour Expandable menu
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mFragment.getMapAsync(this);
        buildGoogleAPI();
        updateValFromBundle(savedInstanceState);

        //receive friendid from SelectFriend Activity
        Bundle previousActivityBundle = getIntent().getExtras();
        MY_FRIEND_ID = previousActivityBundle.getInt("FriendId");

        // Expandable menu
        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new com.hackaton.findme.ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // Listview Group click listener
        expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                // Toast.makeText(getApplicationContext(),
                // "Group Clicked " + listDataHeader.get(groupPosition),
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Listview Group expanded listener
        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
                Toast.makeText(getApplicationContext(),
                        listDataHeader.get(groupPosition) + " Expanded",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Listview Group collasped listener
        expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

            @Override
            public void onGroupCollapse(int groupPosition) {
                Toast.makeText(getApplicationContext(),
                        listDataHeader.get(groupPosition) + " Collapsed",
                        Toast.LENGTH_SHORT).show();

            }
        });

        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                String newMode = listDataChild.get(listDataHeader.get(0)).get(childPosition).toString();
                listDataChild.get(listDataHeader.get(0)).remove(childPosition);  // enleve mode de la liste car rendu en header
                listDataChild.get(listDataHeader.get(0)).add(childPosition, listDataHeader.get(groupPosition));

                Toast.makeText(
                        getApplicationContext(),
                                "Mode changed to "
                                + listDataChild.get(
                                listDataHeader.get(groupPosition)).get(
                                childPosition), Toast.LENGTH_SHORT)
                        .show();

                // TODO: recollapser le menu
                return false;
            }
        });
        //
    }

    protected synchronized void buildGoogleAPI() {
       GoogleAC = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        locRequest = new LocationRequest();
        locRequest.setInterval(5000);
        locRequest.setFastestInterval(2500);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        createLocationRequest();
        if(reqLocationUpdates) {
            startRcvLocation();
        }
        Toast.makeText(this, "Connected to Google Play Services", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        int errcode = connectionResult.getErrorCode();
        Toast.makeText(this, "The connection failed? Error Code: " + errcode, Toast.LENGTH_SHORT).show();
    }

    protected void startRcvLocation() {
        LocationServices.FusedLocationApi.requestLocationUpdates(GoogleAC, locRequest, this);
    }


    @Override
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setMyLocationEnabled(true);
        //TODO: add current location as default view
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        //TODO: add sending to couchbase the new location info
    }

    /*
        TODO: Add fetching FriendId location data to plot from our location to theirs on Google Maps
    */

    @Override
    protected void onPause(){
        super.onPause();
        stopLocUpdates();
    }

    protected void stopLocUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(GoogleAC, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(GoogleAC.isConnected() && !reqLocationUpdates) {
            startRcvLocation();
        }
    }

    //Save instance
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_KEY, reqLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, currentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    //Instance value updates
    private void updateValFromBundle(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_KEY)) {
                reqLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
    }

    /*
         * Preparing the list data pour Expandable menu
         */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add("Mode: "+MainActivity.selectedAction.toString());

        // Adding child data
        List<String> options = new ArrayList<String>();
        options.add("The Shawshank Redemption");
        options.add("The Godfather");
        options.add("The Godfather: Part II");
        options.add("Pulp Fiction");
        options.add("The Good, the Bad and the Ugly");
        options.add("The Dark Knight");
        options.add("12 Angry Men");

        listDataChild.put(listDataHeader.get(0), options); // Header, Child data
        //listDataChild.put(listDataHeader.get(1), nowShowing);
        //listDataChild.put(listDataHeader.get(2), comingSoon);
    }
}
