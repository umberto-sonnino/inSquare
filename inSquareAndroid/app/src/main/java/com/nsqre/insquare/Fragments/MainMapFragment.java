package com.nsqre.insquare.Fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.MainActivity;
import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;
import com.nsqre.insquare.Utilities.REST.DownloadClosestSquares;
import com.nsqre.insquare.Utilities.Square;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainMapFragment extends Fragment
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback
{

    private SupportMapFragment mainMapFragment;
    public static final int SQUARE_DOWNLOAD_LIMIT = 1000;

    private static final String TAG = "MainMapFragment";
    private GoogleApiClient mGoogleApiClient;
    public Location mCurrentLocation;
    private LatLng mLastUpdateLocation; // Da dove ho scaricato i pin l'ultima volta
    private static final float PIN_DOWNLOAD_RADIUS = 30.0f;

    private final int[] MAP_TYPES = {GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_NONE};
    private int curMapTypeIndex = 1;
    public GoogleMap mGoogleMap;

    private static final int REQUEST_FINE_LOCATION = 0;
    private static final int REQUEST_COARSE_LOCATION = 1;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute in milliseconds
    private static String[] PERMISSIONS =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    // Relazione fra Square e Marker sulla mappa
    private HashMap<Marker, Square> squareHashMap;
    private MapActivity rootMapActivity;
    private MainActivity rootMainActivity;

    private TextView bottomSheetSquareName;
    private ImageButton bottomSheetButton;
    private RecyclerView bottomSheetList;

    private Tracker mTracker;

    // Variabili per l'inizializzazione della Chat
    public static final String SQUARE_ID_TAG = "SQUARE_URL";
    public static final String SQUARE_NAME_TAG = "SQUARE_NAME";
    private String mSquareId;
    private String mSquareName;

    public MainMapFragment() {
        // Required empty public constructor
    }

    public static MainMapFragment newInstance() {
        return new MainMapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();

        mLastUpdateLocation = new LatLng(0,0);

        squareHashMap = new LinkedHashMap<>();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        rootMapActivity = (MapActivity) getActivity();
//        rootMainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main_map_copy, container, false);

        // Recuperiamo un po' di riferimenti ai layout
        bottomSheetButton = (ImageButton) v.findViewById(R.id.bottom_sheet_button);
        bottomSheetSquareName = (TextView) v.findViewById(R.id.bottom_sheet_square_name);
//        bottomSheetList = (RecyclerView) v.findViewById(R.id.bottom_sheet_list);

        FrameLayout bottomSheet = (FrameLayout) bottomSheetSquareName.getParent().getParent().getParent();
        BottomSheetBehavior bsb = BottomSheetBehavior.from(bottomSheet);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mainMapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.main_map_container, mainMapFragment).commit();
        mainMapFragment.getMapAsync(this);

        if(mGoogleApiClient == null)
        {
            Toast.makeText(getContext(), "Google API Client is null", Toast.LENGTH_SHORT).show();
        }

        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: I've just paused!");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG, "onStop: I've just stopped!");
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            this.requestPermissions(PERMISSIONS,
                    REQUEST_COARSE_LOCATION);
            this.requestPermissions(PERMISSIONS,
                    REQUEST_FINE_LOCATION);
            return;
        }
        setupLocation();
    }

    private void setupLocation()
    {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mCurrentLocation == null)
        {
            Log.d(TAG, "Nessuna locazione corrente, ora provvedo");
            LocationManager locationManager = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if(getContext() != null) {
                        mCurrentLocation = location;
                        initCamera(mCurrentLocation);
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            String GPS = LocationManager.GPS_PROVIDER;
            String NETWORK = LocationManager.NETWORK_PROVIDER;

            if(locationManager.isProviderEnabled(GPS))
            {
                locationManager.requestLocationUpdates(GPS,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                Location location = locationManager.getLastKnownLocation(GPS);
                if(location != null)
                {
                    Log.d(TAG, "Locazione da GPS - Lat: ("
                            + location.getLatitude()
                            + "; Lon: "
                            + location.getLongitude() + ")");
                    mCurrentLocation = location;
                }else
                {
                    Toast.makeText(getContext(), "Non ho modo di prendere la locazione corrente!", Toast.LENGTH_LONG).show();
                }

            }else if(locationManager.isProviderEnabled(NETWORK))
            {
                locationManager.requestLocationUpdates(NETWORK,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                Location location = locationManager.getLastKnownLocation(NETWORK);
                if(location != null)
                {
                    Log.d(TAG, "Locazione da NETWORK -  Lat: ("
                            + location.getLatitude()
                            + "; Lon: "
                            + location.getLongitude() + ")");
                    mCurrentLocation = location;
                }else
                {
                    Toast.makeText(getContext(), "Non ho modo di prendere la locazione corrente!", Toast.LENGTH_LONG).show();
                }
            }else
            {
                Toast.makeText(getContext(),
                        "Geolocalizzazione disattivata..?",
                        Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // Se ci sono GPS o Network Provider attivati, richiedi la locazione
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0 , 0, locationListener);
        }else {
            initCamera(mCurrentLocation);
        }
    }

    // Permessi di locazioni richiesti
    // Gestione di ritorno dalla richiesta
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_COARSE_LOCATION:
            case REQUEST_FINE_LOCATION:
                if(grantResults.length>0)
                {
                    setupLocation();
                    Toast.makeText(getContext(), "Permessi ottenuti!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(),
                            "Senza permessi non posso funzionare!", Toast.LENGTH_SHORT).show();
                }

                return;
        }
    }

    private void initCamera(Location mCurrentLocation) {

        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        mGoogleMap.setMapType(MAP_TYPES[curMapTypeIndex]);
        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setMyLocationEnabled(true);
        // mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        VisibleRegion vr = mGoogleMap.getProjection().getVisibleRegion();

        double distance = getDistance(mLastUpdateLocation, cameraPosition.target);

//        double distance = getDistance(vr.latLngBounds.southwest, vr.latLngBounds.northeast);
//        if (!waitingDelay)
//            downloadAndInsertPins(30.0f,cameraPosition.target);
        if( distance > PIN_DOWNLOAD_RADIUS*0.9f)
        {
            downloadAndInsertPins(PIN_DOWNLOAD_RADIUS, cameraPosition.target);
            Log.d(TAG, "Downloading From: " + cameraPosition.target.toString());
        }
    }

    private void downloadAndInsertPins(double distance, LatLng position)
    {
        String d = distance + "km";

        DownloadClosestSquares dcs;

        //TODO check sul centro del mondo
        if(position != null)
        {
            dcs = new DownloadClosestSquares(d,
                    position.latitude,
                    position.longitude);
        }
        else
        {
            dcs = new DownloadClosestSquares(d, 0, 0);
            Log.d(TAG, "downloadAndInsertPins: downloading at the center of the world..?");
        }


        try {
            // Update con l'ultima posizione a cui ho effettuato l'aggiornamento
            mLastUpdateLocation = position;

            HashMap<String, Square> squarePins = dcs.execute().get();

            // Rimuovi le Squares di troppo
            for (Square closeSquare : squarePins.values()) {
                LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                Marker m = createSquarePin(coords, closeSquare.getName());
                squareHashMap.put(m, closeSquare);
                if(squareHashMap.size() > SQUARE_DOWNLOAD_LIMIT) {
                    Marker key = squareHashMap.entrySet().iterator().next().getKey();
                    key.remove();
                    squareHashMap.remove(key);
                }
            }
            Log.d(TAG, "downloadAndInsertPins: squareHashMapSize: " + squareHashMap.size());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // Con due locazioni restituisce il valore in km di distanza
    private float getDistance(LatLng southwest, LatLng frnd_latlong){
        Location l1=new Location("Southwest");
        l1.setLatitude(southwest.latitude);
        l1.setLongitude(southwest.longitude);

        Location l2=new Location("Northeast");
        l2.setLatitude(frnd_latlong.latitude);
        l2.setLongitude(frnd_latlong.longitude);

        float distance=l1.distanceTo(l2);

        // Converti da metri in km
        distance=distance/1000.0f;

        return distance;
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        startChatActivity(marker);
    }

    private void startChatActivity(Marker marker) {

        // [START PinButton_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("MapActivity")
                .setAction("PinButton")
                .build());
        // [END PinButton_event]

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        Square s = squareHashMap.get(marker);
        intent.putExtra(SQUARE_ID_TAG, s.getId());
        intent.putExtra(SQUARE_NAME_TAG, s.getName());
        startActivity(intent);
    }

    // LatLng | Name |
    private Marker createSquarePin(LatLng pos, String name) {

        MarkerOptions options = new MarkerOptions().position(pos);
        options.title(name);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.nsqre_map_pin));
        Marker marker = mGoogleMap.addMarker(options);

        return marker;
    }

    @Override
    public void onMapClick(final LatLng latLng) {
        final String lat = Double.toString(latLng.latitude);
        final String lon = Double.toString(latLng.longitude);

        final Dialog mDialog = new Dialog(getContext());
        mDialog.setContentView(R.layout.dialog_crea_square);
        mDialog.setTitle("Crea una Square");
        mDialog.setCancelable(true);
        mDialog.show();

        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final EditText usernameEditText = (EditText) mDialog.findViewById(R.id.et_square);
        TextInputLayout textInputLayout = (TextInputLayout) mDialog.findViewById(R.id.input_layout_crea_square);
        Button crea = (Button) mDialog.findViewById(R.id.button_crea);
        crea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String squareName = usernameEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(squareName)) {
                    Marker m = createSquarePin(latLng, squareName);
                    // Richiesta Volley POST per la creazione di piazze
                    // Si occupa anche di creare e aggiungere la nuova Square al HashMap
                    String ownerId = InSquareProfile.getUserId();
                    createSquarePostRequest(squareName, lat, lon, m, ownerId);
                    mDialog.dismiss();
                }
            }
        });
    }

    private void createSquarePostRequest(final String squareName,
                                         final String latitude,
                                         final String longitude,
                                         final Marker marker,
                                         final String ownerId) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = "http://recapp-insquare.rhcloud.com/squares";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Create Square response: " + response);
                        try {
                            /*
                                Devo recuperare ID della Square dal responso del server
                                per poter mantenere coerenti le strutture dati della mappa
                            */

                            JSONObject o = new JSONObject(response);
                            String squareId = o.getString("_id");
                            String name = o.getString("name");
                            double lat = Double.parseDouble(latitude);
                            double lon = Double.parseDouble(longitude);
                            String owner = o.getString("ownerId");
                            Square s = new Square(squareId, name, lat, lon, "geo_point",owner);
                            squareHashMap.put(marker, s);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(getContext(), "Square creata con successo!", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("CreateSquare Response", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", squareName);
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("ownerId",ownerId);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(mainMapFragment != null)
        {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            if(locationManager != null && googleMap != null)
            {
                mGoogleMap = googleMap;
            }
        }

        initListeners();
    }

    private void initListeners() {
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        // TODO on secondo click start chat
        marker.showInfoWindow();
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()),
                400, // Tempo di spostamento in ms
                null); // callback
        Square currentSquare = squareHashMap.get(marker);

        setSquareId(currentSquare.getId());
        setSquareName(marker.getTitle());
        Log.d(TAG, currentSquare.getId() + " " + currentSquare.getName());

        String text = marker.getTitle();

        bottomSheetSquareName.setText(text);
        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(rootMapActivity, ChatActivity.class);
                // [START FloatingButton_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("MapActivity")
                        .setAction("FloatingButton")
                        .build());
                // [END FloatingButton_event]
                intent.putExtra(SQUARE_ID_TAG, mSquareId);
                intent.putExtra(SQUARE_NAME_TAG, mSquareName);
                startActivity(intent);
            }
        });

        bottomSheetButton.setVisibility(View.VISIBLE);

        return true;
    }

    // Questo e il prossimo metodo mantengono il riferimento al marker che viene cliccato
    public void setSquareName(String squareName) {
        this.mSquareName = squareName;
    }

    //
    public void setSquareId(String mSquareId) {
        this.mSquareId = mSquareId;
    }
}