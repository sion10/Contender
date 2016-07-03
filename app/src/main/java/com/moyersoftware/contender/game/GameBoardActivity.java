package com.moyersoftware.contender.game;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.print.PrintHelper;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.moyersoftware.contender.R;
import com.moyersoftware.contender.game.adapter.GameBoardAdapter;
import com.moyersoftware.contender.game.adapter.GameRowAdapter;
import com.moyersoftware.contender.game.data.Event;
import com.moyersoftware.contender.game.data.Game;
import com.moyersoftware.contender.game.data.SelectedSquare;
import com.moyersoftware.contender.menu.data.Player;
import com.moyersoftware.contender.util.Util;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class GameBoardActivity extends AppCompatActivity {

    // Constants
    private final static int COLUMNS_COUNT = 10;
    public static final String EXTRA_GAME_ID = "GameId";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;

    // Views
    @Bind(R.id.board_game_img)
    ImageView mGameImg;
    @Bind(R.id.board_recycler)
    RecyclerView mBoardRecycler;
    @Bind(R.id.board_row_recycler)
    RecyclerView mRowRecycler;
    @Bind(R.id.board_column_recycler)
    RecyclerView mColumnRecycler;
    @Bind(R.id.board_horizontal_scroll_view)
    HorizontalScrollView mHorizontalScrollView;
    @Bind(R.id.board_bottom_sheet)
    View mBottomSheet;
    @Bind(R.id.board_info_home_img)
    ImageView mTeam1Img;
    @Bind(R.id.board_info_away_img)
    ImageView mTeam2Img;
    @Bind(R.id.board_title_txt)
    TextView mTitleTxt;
    @Bind(R.id.board_layout)
    View mLayout;
    @Bind(R.id.board_away_name_txt)
    TextView mAwayNameTxt;
    @Bind(R.id.board_home_name_txt)
    TextView mHomeNameTxt;
    @Bind(R.id.board_info_away_name_txt)
    TextView mInfoAwayNameTxt;
    @Bind(R.id.board_info_home_name_txt)
    TextView mInfoHomeNameTxt;
    @Bind(R.id.board_info_home_total_score_txt)
    TextView mInfoHomeTotalScoreTxt;
    @Bind(R.id.board_info_away_total_score_txt)
    TextView mInfoAwayTotalScoreTxt;
    @Bind(R.id.board_info_q1_score_txt)
    TextView mQ1ScoreTxt;
    @Bind(R.id.board_info_q2_score_txt)
    TextView mQ2ScoreTxt;
    @Bind(R.id.board_info_q3_score_txt)
    TextView mQ3ScoreTxt;
    @Bind(R.id.board_info_final_score_txt)
    TextView mFinalScoreTxt;
    @Bind(R.id.board_info_time_txt)
    TextView mTimeTxt;
    @Bind(R.id.board_print_img)
    ImageView mPrintImg;
    @Bind(R.id.board_pdf_img)
    ImageView mPdfImg;
    @Bind(R.id.board_progress_txt)
    View mProgressBar;

    // Usual variables
    private int mTotalScrollY;
    private LinearLayoutManager mColumnLayoutManager;
    private LinearLayoutManager mRowLayoutManager;
    private DatabaseReference mDatabase;
    private String mGameId;
    private ArrayList<Integer> mRowNumbers = new ArrayList<>();
    private ArrayList<Integer> mColumnNumbers = new ArrayList<>();
    private ArrayList<SelectedSquare> mSelectedSquares = new ArrayList<>();
    private GameRowAdapter mColumnAdapter;
    private GameRowAdapter mRowAdapter;
    private String mGameName;
    private String mMyId;
    private String mMyName;
    private String mMyPhoto;
    private GameBoardAdapter mBoardAdapter;
    private Boolean mGameLive = false;
    private boolean mPendingUpload = false;
    private boolean mIgnoreUpdate = false;
    private ArrayList<String> mPlayerEmails = new ArrayList<>();
    private String mAuthorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_board);
        ButterKnife.bind(this);

        mGameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        if (TextUtils.isEmpty(mGameId)) return;

        initUser();
        initRowRecycler();
        initColumnRecycler();
        initBoardRecycler();
        initHorizontalScrollView();
        initBottomSheet();
        initDatabase();
    }

    private void initUser() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            mMyId = firebaseUser.getUid();
            mMyName = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                mMyPhoto = firebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void initDatabase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Query query = mDatabase.child("games").child(mGameId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(GameBoardActivity.this, "Game not found", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                } else {
                    Game game = dataSnapshot.getValue(Game.class);

                    initGameDetails(game);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void initGameDetails(Game game) {
        if (mIgnoreUpdate) {
            mIgnoreUpdate = false;
            return;
        }

        Util.Log("update game details");

        // Set game image
        Picasso.with(this).load(game.getImage()).centerCrop().fit()
                .placeholder(R.drawable.placeholder).into(mGameImg);

        // Set game name
        mGameName = game.getName();
        mTitleTxt.setText(mGameName);

        // Update row numbers
        mRowNumbers.clear();
        mRowNumbers.addAll(game.getRowNumbers());
        mRowAdapter.notifyDataSetChanged();

        // Update column numbers
        mColumnNumbers.clear();
        mColumnNumbers.addAll(game.getColumnNumbers());
        mColumnAdapter.notifyDataSetChanged();

        // Update prices
        mQ1ScoreTxt.setText("Q1: " + game.getQuarter1Price() + " points");
        mQ2ScoreTxt.setText("Q2: " + game.getQuarter2Price() + " points");
        mQ3ScoreTxt.setText("Q3: " + game.getQuarter3Price() + " points");
        mFinalScoreTxt.setText("FINAL: " + game.getFinalPrice() + " points");

        // Get players
        mPlayerEmails.clear();
        mPlayerEmails.add(game.getAuthor().getEmail());
        mAuthorId = game.getAuthor().getUserId();
        if (game.getPlayers() != null) {
            for (Player player : game.getPlayers()) {
                mPlayerEmails.add(player.getEmail());
            }
        }

        // Update selected squares
        mSelectedSquares.clear();
        ArrayList<SelectedSquare> selectedSquares = game.getSelectedSquares();
        if (selectedSquares == null) selectedSquares = new ArrayList<>();

        for (SelectedSquare selectedSquare : selectedSquares) {
            mSelectedSquares.add(selectedSquare);
        }
        mBoardAdapter.refresh(mSelectedSquares);

        updateLiveState();

        mDatabase.child("events").child(game.getEventId()).addListenerForSingleValueEvent
                (new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Event event = dataSnapshot.getValue(Event.class);
                        if (event != null) {
                            mGameLive = (event.getTime() != -1 && System.currentTimeMillis()
                                    > event.getTime()) || mSelectedSquares.size() == 100;
                            mColumnAdapter.setLive(mGameLive);
                            mRowAdapter.setLive(mGameLive);
                            mBoardAdapter.setLive(mGameLive);
                            mColumnAdapter.notifyDataSetChanged();
                            mRowAdapter.notifyDataSetChanged();
                            mBoardAdapter.notifyDataSetChanged();

                            mTimeTxt.setText(event.getTimeText());

                            mAwayNameTxt.setText(event.getTeamAway().getName());
                            mHomeNameTxt.setText(event.getTeamHome().getName());

                            // Init bottom section info
                            mInfoAwayNameTxt.setText(event.getTeamAway().getName());
                            mInfoHomeNameTxt.setText(event.getTeamHome().getName());

                            Picasso.with(GameBoardActivity.this).load(event.getTeamAway()
                                    .getImage()).into(mTeam1Img);
                            Picasso.with(GameBoardActivity.this).load(event.getTeamHome()
                                    .getImage()).into(mTeam2Img);

                            if (event.getTeamAway().getScore() != null) {
                                mInfoAwayTotalScoreTxt.setText(event.getTeamAway().getScore()
                                        .getTotal());
                                mInfoHomeTotalScoreTxt.setText(event.getTeamHome().getScore()
                                        .getTotal());
                            } else {
                                mInfoAwayTotalScoreTxt.setText("00");
                                mInfoHomeTotalScoreTxt.setText("00");
                            }
                        } else {
                            Toast.makeText(GameBoardActivity.this, "Event not found",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private void initBoardRecycler() {
        mBoardRecycler.setLayoutManager(new GridLayoutManager(this, COLUMNS_COUNT));
        mBoardRecycler.setHasFixedSize(true);
        mBoardAdapter = new GameBoardAdapter(this);
        mBoardRecycler.setAdapter(mBoardAdapter);

        mBoardRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mTotalScrollY += dy;

                mColumnLayoutManager.scrollToPositionWithOffset(0, -mTotalScrollY);
            }
        });
    }

    private void initRowRecycler() {
        mRowLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        mRowRecycler.setLayoutManager(mRowLayoutManager);
        mRowRecycler.setHasFixedSize(true);
        mRowAdapter = new GameRowAdapter(mRowNumbers);
        mRowRecycler.setAdapter(mRowAdapter);
    }

    private void initColumnRecycler() {
        mColumnLayoutManager = new LinearLayoutManager(this);
        mColumnRecycler.setLayoutManager(mColumnLayoutManager);
        mColumnRecycler.setHasFixedSize(true);
        mColumnAdapter = new GameRowAdapter(mColumnNumbers);
        mColumnRecycler.setAdapter(mColumnAdapter);
    }

    private void initHorizontalScrollView() {
        mHorizontalScrollView.getViewTreeObserver().addOnScrollChangedListener
                (new ViewTreeObserver.OnScrollChangedListener() {

                    @Override
                    public void onScrollChanged() {
                        int scrollX = mHorizontalScrollView.getScrollX();

                        mRowLayoutManager.scrollToPositionWithOffset(0, -scrollX);
                    }
                });
    }

    private void initBottomSheet() {
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        behavior.setPeekHeight(Util.convertDpToPixel(48));
    }

    /**
     * Required for the calligraphy library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void onBackButtonClicked(View view) {
        finish();
    }

    public void onPrintButtonClicked(View view) {
        mLayout.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mBoardAdapter.setPrintMode(true);
        mBoardRecycler.post(new Runnable() {
            @Override
            public void run() {
                takeScreenshot();
            }
        });
    }

    private void takeScreenshot() {
        int boardSize = (int) (getResources().getDimension(R.dimen.board_cell_size) * 11
                + Util.convertDpToPixel(32));
        mLayout.getLayoutParams().width = boardSize;
        mLayout.getLayoutParams().height = boardSize;
        mLayout.requestLayout();
        mLayout.post(new Runnable() {
            @Override
            public void run() {
                PrintHelper photoPrinter = new PrintHelper(GameBoardActivity.this);
                photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
                Bitmap bitmap = loadBitmapFromView(mLayout);
                photoPrinter.printBitmap(mGameName + ", Contender", bitmap);

                mLayout.getLayoutParams().width = ViewPager.LayoutParams.MATCH_PARENT;
                mLayout.getLayoutParams().height = ViewPager.LayoutParams.MATCH_PARENT;
                mLayout.requestLayout();
                mBoardAdapter.setPrintMode(false);

                mProgressBar.setVisibility(View.GONE);
                mLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private static String FILE = Environment.getExternalStorageDirectory() + "/Contender.pdf";

    public void onPdfButtonClicked(View view) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            } else {
                onPdfButtonClicked();
            }
        } else {
            onPdfButtonClicked();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPdfButtonClicked();
                }
            }
        }
    }

    private void onPdfButtonClicked() {
        mLayout.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mBoardAdapter.setPrintMode(true);
        mBoardRecycler.post(new Runnable() {
            @Override
            public void run() {
                makePdf();
            }
        });
    }

    private void makePdf() {
        int boardSize = (int) (getResources().getDimension(R.dimen.board_cell_size) * 11
                + Util.convertDpToPixel(32));
        mLayout.getLayoutParams().width = boardSize;
        mLayout.getLayoutParams().height = boardSize;
        mLayout.requestLayout();
        mLayout.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmapFromView(mLayout);

                Document document = new Document(new Rectangle(bitmap.getWidth(),
                        bitmap.getHeight()), 0, 0, 0, 0);

                try {
                    PdfWriter.getInstance(document, new FileOutputStream(FILE));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                document.open();

                addImage(document, bitmap);
                document.close();

                Util.Log("FILE = " + FILE);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                String[] playerEmails = new String[mPlayerEmails.size()];
                playerEmails = mPlayerEmails.toArray(playerEmails);
                intent.putExtra(Intent.EXTRA_EMAIL, playerEmails);
                intent.putExtra(Intent.EXTRA_SUBJECT, mGameName + " Contender board");
                File file = new File(FILE);
                if (!file.exists() || !file.canRead()) {
                    Toast.makeText(GameBoardActivity.this, "Attachment Error", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                Uri uri = FileProvider.getUriForFile(GameBoardActivity.this, getPackageName(),
                        file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));

                mLayout.getLayoutParams().width = ViewPager.LayoutParams.MATCH_PARENT;
                mLayout.getLayoutParams().height = ViewPager.LayoutParams.MATCH_PARENT;
                mLayout.requestLayout();
                mBoardAdapter.setPrintMode(false);

                mProgressBar.setVisibility(View.GONE);
                mLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addImage(Document document, Bitmap bitmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            document.add(Image.getInstance(byteArray));

            Util.Log("saved = " + document.getPageSize().getWidth());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
        v.draw(c);
        return b;
    }

    private Handler mHandler = new Handler();

    public void selectSquare(int position) {
        boolean squareExists = false;
        for (SelectedSquare selectedSquare : mSelectedSquares) {
            if (selectedSquare.getPosition() == position) squareExists = true;
        }
        if (!squareExists) {
            int column = 0;
            int row = position;
            while (row / 10 > 0) {
                row -= 10;
                column++;
            }
            mSelectedSquares.add(new SelectedSquare(mMyId, mMyName, mMyPhoto, column, row,
                    position));
            mBoardAdapter.refresh(mSelectedSquares, position);

            if (mPendingUpload) {
                mHandler.removeCallbacks(updateSquaresRunnable);
            }
            mPendingUpload = true;
            mHandler.postDelayed(updateSquaresRunnable, 500);
        }

        updateLiveState();
    }

    Runnable updateSquaresRunnable = new Runnable() {
        @Override
        public void run() {
            //noinspection unchecked
            AsyncTaskCompat.executeParallel(new UpdateSquareTask());
        }
    };

    private class UpdateSquareTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected final Void doInBackground(Void... params) {
            Util.Log("updated squares");
            mIgnoreUpdate = true;
            mDatabase.child("games").child(mGameId).child("selectedSquares")
                    .setValue(mSelectedSquares);
            mPendingUpload = false;
            return null;
        }
    }

    private void updateLiveState() {
        if (mSelectedSquares.size() == 100 != mGameLive) {
            mGameLive = mSelectedSquares.size() == 100;
            mPrintImg.setVisibility(mGameLive ? View.VISIBLE : View.GONE);
            mPdfImg.setVisibility(mGameLive && mAuthorId.equals(mMyId) ? View.VISIBLE : View.GONE);
            mBoardAdapter.setLive(mGameLive);
            mRowAdapter.setLive(mGameLive);
            mColumnAdapter.setLive(mGameLive);
            mBoardAdapter.notifyDataSetChanged();
            mRowAdapter.notifyDataSetChanged();
            mColumnAdapter.notifyDataSetChanged();
        }
    }
}
