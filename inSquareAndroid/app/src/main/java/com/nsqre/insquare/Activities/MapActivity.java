package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.nsqre.insquare.Fragments.MainMapFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.ProfileFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;
import com.nsqre.insquare.Utilities.DrawerListAdapter;
import com.nsqre.insquare.Utilities.NavItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener
{

    private static final String TAG = "MapActivity";

    private float startTouchY;

    private String mSquareId;
    private String mSquareName;

    private Tracker mTracker;

    private SearchView searchView;
    private SimpleCursorAdapter mSearchAdapter;

    ListView mDrawerList;
    RelativeLayout mDrawerPane;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    ProfileFragment profileFragment;
    RecentSquaresFragment recentSquaresFragment;
    MainMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        mNavItems.add(new NavItem("Mappa", "Dai un'occhiata in giro", R.drawable.google_maps));
        mNavItems.add(new NavItem("Squares recenti", "Non perderti un messaggio", R.drawable.google_circles_extended));
        mNavItems.add(new NavItem("Profilo", "Gestisci il tuo profilo", R.drawable.account_circle));

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        // Populate the Navigtion Drawer with options
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        mDrawerList = (ListView) findViewById(R.id.navList);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItemFromDrawer(position);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //crea il bottone toggle del drawer menu
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // C'è l'override di questo metodo, ma non serve a nulla
                // (teoricamente toglie dall'action bar ciò che non vogliamo far vedere quando l'hamburger è aperto
                invalidateOptionsMenu();
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "onDrawerClosed: " + getTitle());
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);


        mapFragment = new MainMapFragment();
        recentSquaresFragment = new RecentSquaresFragment();
        profileFragment = new ProfileFragment();

        selectItemFromDrawer(2);  //TODO RIMUOVERE O METTERE 0
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                startTouchY = event.getY();
            case MotionEvent.ACTION_UP:
                float endTouchY = event.getY();

                if(endTouchY < startTouchY)
                {
                    Log.d(TAG, "Moving Up");
//                    linearLayout.setVisibility(View.VISIBLE);
//                    linearLayout.startAnimation(animationUp);
                }else {
                    Log.d(TAG, "Moving Down!");
//                    linearLayout.setVisibility(View.GONE);
//                    linearLayout.startAnimation(animationDown);
                }
        }

        return super.onTouchEvent(event);
    }

    public void setSquareName(String squareName) {
        this.mSquareName = squareName;
    }

    public void setSquareId(String mSquareId) {
        this.mSquareId = mSquareId;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map_actions, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search_squares_action);

        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getString(R.string.hint_cerca_squares));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        AutoCompleteTextView searchTextView =
                (AutoCompleteTextView) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchTextView.setHintTextColor(ContextCompat.getColor(this, R.color.light_grey));
        searchTextView.setTextColor(ContextCompat.getColor(this, R.color.white));

        ArrayAdapter<Address> adapter = new ArrayAdapter<Address>(this, R.layout.drop_down_entry);
        searchTextView.setAdapter(adapter);

        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled
        // the nav drawer indicator touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {   //permette al toggle di aprire e chiudere il menu hamburger
            return true;
        }
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search_squares_action:
                // [START search_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Search")
                        .build());
                // [END search_event]

                Log.d(TAG, "I've just initiated search");
                break;
            case R.id.instfeedback:

                // [START feedback_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Feedback")
                        .build());
                // [END feedback_event]

                final Dialog d = new Dialog(this);
                d.setContentView(R.layout.dialog_feedback);
                d.setTitle("Feedback");
                d.setCancelable(true);
                d.show();

                final EditText feedbackText = (EditText) d.findViewById(R.id.dialog_feedbacktext);
                Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String feedback = feedbackText.getText().toString();
                        final String activity = this.getClass().getSimpleName();
                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(MapActivity.this);
                        String url = "http://recapp-insquare.rhcloud.com/feedback";

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("VOLLEY","ServerResponse: "+response);
                                        CharSequence text = getString(R.string.thanks_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("VOLLEY", error.toString());
                                CharSequence text = getString(R.string.error_feedback);
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                toast.show();
                            }
                        }) {
                            @Override
                            protected Map<String, String> getParams() {
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("feedback", feedback);
                                params.put("username", InSquareProfile.getInstance(getApplicationContext()).getUserId());
                                params.put("activity", activity);
                                return params;
                            }

                        };
                        queue.add(stringRequest);
                        d.dismiss();
                    }
                });
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //TODO ORA QUESTI ELEMENTI SONO IN MAP FRAGMENT
        /*
        if(linearLayout.getVisibility() == View.VISIBLE)
        {
            linearLayout.startAnimation(animationDown);
            linearLayout.setVisibility(View.GONE);
            mapFab.setVisibility(View.GONE);
            return;
        }else
        {
        */
            // Termina l'activity quando viene premuto BACK
            this.finishAffinity();
        //}
        super.onBackPressed();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "onQueryTextSubmit: Currently looking for: " + query);
        searchLocationName(query.trim());
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, "onQueryTextChange: just written:" + newText);
        return false;
    }

    private void searchLocationName(String placeName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            MapFragment mFrag = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);
            Location l;
            if(mFrag.mCurrentLocation == null)
            {
                l = new Location("");
                l.setLatitude(0.0d);
                l.setLongitude(0.0d);
            }
            else
                l = mFrag.mCurrentLocation;

            LatLng northeast = calculateOffset(l, 8000);
            LatLng southwest = calculateOffset(l, -8000);

            List<Address> results = geocoder.getFromLocationName(placeName,
                    5, // num results
                    southwest.latitude, //double lowerLeftLatitude,
                    southwest.longitude, //double lowerLeftLongitude,
                    northeast.latitude, //double upperRightLatitude,
                    northeast.longitude //double upperRightLongitude
            );


            if(!results.isEmpty())
            {
                Address first = results.get(0);
                LatLng pos = new LatLng(first.getLatitude(), first.getLongitude());
                mFrag.mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLng(pos),
                        400,
                        null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LatLng calculateOffset(Location position, float offset)
    {
        final int RADIUS = 6378137;
        float diff_lat = offset/RADIUS;
        double diff_lon = offset/(RADIUS * Math.cos(Math.PI * position.getLatitude()/180));

        double lat = position.getLatitude() + diff_lat*180/Math.PI;
        double lon = position.getLongitude() + diff_lon*180/Math.PI;

        return new LatLng(lat, lon);
    }

    /*
    *  Apre il fragment scelto dal burger menu
    *
    * */
    private void selectItemFromDrawer(int position) {
        //switch che in base al pulsante scelto esegue
        switch (position) {
            case 0:   //caso mappa
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mainContent, mapFragment, "MAPPA")
                        .commit();
//                mapFab.show();
//                linearLayout.setVisibility(View.VISIBLE);
                break;
            case 1:  //caso recenti
                getFragmentManager().beginTransaction()
                        .replace(R.id.mainContent, recentSquaresFragment, "RECENTS")
                        .commit();
//                mapFab.hide();
//                linearLayout.setVisibility(View.INVISIBLE);
                break;
            case 2:  //caso profilo
                getFragmentManager().beginTransaction()
                        .replace(R.id.mainContent, profileFragment, "PROFILE")
                        .commit();
//                mapFab.hide();
//                linearLayout.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        mDrawerList.setItemChecked(position, true);
        setTitle(mNavItems.get(position).getmTitle());
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawerPane);
    }
    @Override
    // Called when invalidateOptionsMenu() is invoked
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        //menu.findItem(R.id.action_search).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}
