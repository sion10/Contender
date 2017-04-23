package com.moyersoftware.contender.game;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.moyersoftware.contender.R;
import com.moyersoftware.contender.game.adapter.JoinGamesAdapter;
import com.moyersoftware.contender.game.data.Event;
import com.moyersoftware.contender.game.data.GameInvite;
import com.moyersoftware.contender.util.Util;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;

public class JoinIdFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final int LOCATION_PERMISSION_CODE = 0;
    private static final float GAME_SEARCH_RADIUS_MILES = 100;
    private static final float GAME_SEARCH_RADIUS_METERS = (float) (GAME_SEARCH_RADIUS_MILES
            * 1609.34);

    // Views
    @Bind(R.id.join_id_recycler)
    RecyclerView mGamesIdRecycler;
    @Bind(R.id.join_id_edit_txt)
    EditText mIdEditTxt;
    @Bind(R.id.join_id_title_txt)
    TextView mTitleIdTxt;
    @Bind(R.id.join_location_recycler)
    RecyclerView mGamesLocationRecycler;
    @Bind(R.id.join_location_title_txt)
    TextView mTitleLocationTxt;
    @Bind(R.id.join_location_search_txt)
    TextView mSearchTxt;

    // Usual variables
    private ArrayList<GameInvite.Game> mIdGames = new ArrayList<>();
    private JoinGamesAdapter mIdAdapter;
    private String mQuery;
    private DataSnapshot mIdDataSnapshot;
    private String mMyId;
    private String mMyEmail;
    private String mMyName;
    private String mMyPhoto;
    private HashMap<String, Long> mIdEventTimes = new HashMap<>();
    private DataSnapshot mIdGamesSnapshot;

    private ArrayList<GameInvite.Game> mLocationGames = new ArrayList<>();
    private DataSnapshot mDataSnapshot;
    private JoinGamesAdapter mLocationAdapter;
    private GoogleApiClient mGoogleApiClient;
    private Location mMyLocation;
    private HashMap<String, Long> mLocationEventTimes = new HashMap<>();
    private DataSnapshot mLocationGamesSnapshot;

    public JoinIdFragment() {
        // Required empty public constructor
    }

    public static JoinIdFragment newInstance() {
        return new JoinIdFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_id, container, false);
        ButterKnife.bind(this, view);

        initUser();
        initSearchField();
        initIdRecycler();
        initLocationRecycler();
        initDatabase();
        initGoogleClient();

        return view;
    }

    private void initGoogleClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void initUser() {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            mMyId = firebaseUser.getUid();
            mMyEmail = firebaseUser.getEmail();
            mMyName = Util.getDisplayName();
            mMyPhoto = Util.getPhoto();
        }
    }

    private void initSearchField() {
        mIdEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mQuery = mIdEditTxt.getText().toString();
                updateIdGames(mDataSnapshot);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initIdRecycler() {
        mGamesIdRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mGamesIdRecycler.setHasFixedSize(true);
        mIdAdapter = new JoinGamesAdapter((JoinActivity) getActivity(), mIdGames, mMyId, mMyEmail,
                mMyName, mMyPhoto);
        mGamesIdRecycler.setAdapter(mIdAdapter);
    }

    private void initLocationRecycler() {
        mGamesLocationRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mGamesLocationRecycler.setHasFixedSize(true);
        mLocationAdapter = new JoinGamesAdapter((JoinActivity) getActivity(), mLocationGames,
                mMyId, mMyEmail,
                mMyName, mMyPhoto);
        mGamesLocationRecycler.setAdapter(mLocationAdapter);
    }

    private void initDatabase() {
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        Query query = database.child("games").orderByChild("time");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mDataSnapshot = dataSnapshot;
                updateIdGames(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * Updates the games list.
     */
    private void updateIdGames(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) return;

        mIdGamesSnapshot = dataSnapshot;

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        Query query = database.child("events");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Util.Log("find games0");
                mIdEventTimes.clear();
                for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {
                    try {
                        final Event event = gameSnapshot.getValue(Event.class);
                        if (event.getTime() > 0) {
                            mIdEventTimes.put(event.getId(), event.getTime());
                        } else {
                            mIdEventTimes.put(event.getId(), event.getTime());
                        }
                    } catch (Exception e) {
                        // Can't retrieve game time
                    }
                }
                mIdGames.clear();
                if (mMyId != null) {
                    for (DataSnapshot gameSnapshot : mIdGamesSnapshot.getChildren()) {
                        try {
                            GameInvite.Game game = gameSnapshot.getValue(GameInvite.Game.class);

                            if (!mIdEventTimes.containsKey(game.getEventId())) continue;

                            game.setEventTime(mIdEventTimes.get(game.getEventId()));

                            if (!TextUtils.isEmpty(mQuery) && game.getId().contains(mQuery)
                                    && game.getAuthor() != null && !game.getAuthor().getUserId()
                                    .equals(mMyId) && game.getEventTime() != -1
                                    && game.getEventTime() != -2 && game.getEventTime()
                                    > System.currentTimeMillis()) {
                                mIdGames.add(game);
                            }
                        } catch (Exception e) {
                            Util.Log("The game is corrupted: " + e);
                        }
                    }
                }
                mIdAdapter.setGames(mIdGames);
                mIdAdapter.notifyDataSetChanged();

                mTitleIdTxt.setVisibility(mIdGames.size() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * Updates the games list.
     */
    private void updateLocationGames(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) return;

        mLocationGamesSnapshot = dataSnapshot;

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        Query query = database.child("events");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mLocationEventTimes.clear();
                for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {
                    try {
                        final Event event = gameSnapshot.getValue(Event.class);
                        mLocationEventTimes.put(event.getId(), event.getTime());
                    } catch (Exception e) {
                        // Can't retrieve game time
                    }
                }


                mLocationGames.clear();
                if (mMyId != null) {
                    for (DataSnapshot gameSnapshot : mLocationGamesSnapshot.getChildren()) {
                        GameInvite.Game game = gameSnapshot.getValue(GameInvite.Game.class);

                        if (game.getLatitude() != 0 && game.getLongitude() != 0) {
                            Location gameLocation = new Location("");
                            gameLocation.setLatitude(game.getLatitude());
                            gameLocation.setLongitude(game.getLongitude());
                            if (mMyLocation != null && mMyLocation.distanceTo(gameLocation)
                                    < GAME_SEARCH_RADIUS_METERS && !game.getAuthor().getUserId()
                                    .equals(mMyId)) {
                                mLocationGames.add(game);
                            }
                        }
                    }
                }
                mLocationAdapter.notifyDataSetChanged();

                mTitleLocationTxt.setVisibility(mLocationGames.size() > 0 ? View.VISIBLE : View.GONE);
                if (mLocationGames.size() > 0) {
                    mSearchTxt.setVisibility(View.GONE);
                } else {
                    mSearchTxt.setText(R.string.join_location_empty);
                    mSearchTxt.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                    (getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_CODE);
            } else {
                mMyLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }
        } else {
            mMyLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        updateLocationGames(mDataSnapshot);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMyLocation = LocationServices.FusedLocationApi.getLastLocation
                            (mGoogleApiClient);
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

}
