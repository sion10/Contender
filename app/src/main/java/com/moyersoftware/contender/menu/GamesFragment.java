package com.moyersoftware.contender.menu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.moyersoftware.contender.R;
import com.moyersoftware.contender.game.HostActivity;
import com.moyersoftware.contender.game.JoinActivity;
import com.moyersoftware.contender.game.data.Game;
import com.moyersoftware.contender.menu.adapter.GamesAdapter;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class GamesFragment extends Fragment {

    // Views
    @Bind(R.id.games_recycler)
    RecyclerView mGamesRecycler;
    @Bind(R.id.games_host_btn)
    Button mHostBtn;
    @Bind(R.id.games_join_btn)
    Button mJoinBtn;
    @Bind(R.id.games_title_txt)
    TextView mTitleTxt;

    // Usual variables
    private ArrayList<Game> mGames = new ArrayList<>();
    private DatabaseReference mDatabase;
    private GamesAdapter mAdapter;

    public GamesFragment() {
        // Required empty public constructor
    }

    public static GamesFragment newInstance() {
        return new GamesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);
        ButterKnife.bind(this, view);

        initDatabase();
        initRecycler();
        initButtons();

        return view;
    }

    private void initDatabase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();

        Query myTopPostsQuery = mDatabase.child("games").orderByChild("time");
        myTopPostsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Update the games list
                mGames.clear();
                for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {
                    Game game = gameSnapshot.getValue(Game.class);
                    mGames.add(game);
                }
                mAdapter.notifyDataSetChanged();

                // Update the title text
                mTitleTxt.setText(mGames.size() > 0 ? R.string.games_title
                        : R.string.games_title_empty);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void initRecycler() {
        mGamesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mGamesRecycler.setHasFixedSize(true);
        mAdapter = new GamesAdapter(getContext(), mGames);
        mGamesRecycler.setAdapter(mAdapter);
    }

    private void initButtons() {
        mHostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), HostActivity.class));
            }
        });
        mJoinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), JoinActivity.class));
            }
        });
    }
}
