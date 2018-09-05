package mobile.labs.acw;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class puzzle_class extends AppCompatActivity {
    boolean newGame = true;
boolean playing = false;
    String puzzleFolder = "";
    int colSize;
    int rowSize;
    TextView txtStatus;
    int[][] workBoardA = null; //Save for the user to resume their puzzle.
    ArrayList<Integer> newBoard;
    ArrayList<Integer> workBoard;
    GridLayout gl;
    ImageView[] imageV;
    int item;
    int size;
    ArrayList<Bitmap> bitmaps;
    private GestureDetectorCompat detector;
    long tStart;
    int movesCount;
    String puzzleName;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.puzzle_screen);
        tStart = System.currentTimeMillis();
        movesCount = 0;
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(0,0,0));
        workBoard = getIntent().getIntegerArrayListExtra("work-board");
        String strCol = getIntent().getStringExtra("colSize");
        puzzleName = getIntent().getStringExtra("puzzleName");
        String strRow = getIntent().getStringExtra("rowSize");
        puzzleFolder = getIntent().getStringExtra("picFolder");
        rowSize = Integer.valueOf(strRow);
        colSize = Integer.valueOf(strCol);
        workBoardA = to2D(workBoard, colSize, rowSize); //SAve board will be used as default.
        bitmaps = bitmapArray(workBoardA);
            workBoardA = to2D(workBoard, colSize, rowSize); //SAve board will be used as default.
            bitmaps = bitmapArray(workBoardA);
            makeBoard2D(workBoard, bitmaps);
            detector = new GestureDetectorCompat(this, new MyGestureListener());
    }

    int tileMoved;

    void makeBoard2D(final ArrayList<Integer> inBoard, ArrayList<Bitmap> inBMaps) {
        gl = new GridLayout(puzzle_class.this);
        gl.setLayoutParams(new ViewGroup.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        gl.setOrientation(GridLayout.HORIZONTAL);
        size = rowSize * colSize;
        gl.setColumnCount(colSize);
        gl.setRowCount(rowSize);
        imageV = new ImageView[size];
        for (int i = 0; i < size; i++) {
            Bitmap img;
            imageV[i] = new ImageView(this);
            imageV[i].setLayoutParams(new android.view.ViewGroup.LayoutParams(130, 150));   //80 60
            imageV[i].setMaxHeight(80);
            imageV[i].setMaxWidth(80);
            if (inBoard.get(i) == 0)    //Accounting for the empty space
            {
               img = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_4444);
                imageV[i].setId(0);
            } else {
                img = inBMaps.get(i);
                imageV[i].setId(inBoard.get(i));
            }
            imageV[i].setImageBitmap(img);
            gl.addView(imageV[i]);
        }
        setContentView(gl);

        for (item = 0; item < size; item++) {
            imageV[item].setOnTouchListener(new View.OnTouchListener() {
                int tileID = imageV[item].getId();

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    detector.onTouchEvent(event);
                    tileMoved = tileID;
                    playing = true;
                    return false;
                }
            });
        }
    }

private void takeAction(char inDirection, int inTileID) //Positions of the empty tile and pictures tile/tiles are changed around.
    {
        switch (inDirection)   //2d Array is traversed according to the direction chosen.
        {
            case 'u':
              spaceToTile(inTileID, 'u');
                break;
            case 'd':
            spaceToTile(inTileID, 'd');
                break;
            case 'l':
                spaceToTile(inTileID, 'l');
                break;
            case 'r':
                spaceToTile(inTileID, 'r');
                break;
        }
    }

      void setHighScore(String highScore) {
        SQLiteDatabase db = new PuzzleDBHelper(getApplicationContext()).getWritableDatabase();
        String sqlQuery = "UPDATE highscore SET score = '" + highScore + "' WHERE name = " + "'" +  puzzleName + "';";
         db.execSQL(sqlQuery);

    }

    String readHighScore()
    {
String result = "";
        ArrayList<String> lst = new ArrayList<String>();
        SQLiteDatabase db = new PuzzleDBHelper(getApplicationContext()).getReadableDatabase();
        String[] projection = {PuzzleDBContract.scoreEntry._ID,
                PuzzleDBContract.scoreEntry.COLUMN_NAME_NAME,
                PuzzleDBContract.scoreEntry.COLUMN_NAME_SCORE

        };
        Cursor c = db.query(PuzzleDBContract.scoreEntry.TABLE_NAME, projection, null, null, null, null, null);
        for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {

            String fname = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.scoreEntry.COLUMN_NAME_NAME));
            String fscore = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.scoreEntry.COLUMN_NAME_SCORE));
            if (fname.equals(puzzleName))
            {
                result = fscore;
                break;
            }
        }

        c.close();
        return result;
    }

    //Saving the state
    void saveGame(String puzzleName)
    {
        //Where puzzlename get layout, assign picture set.
        SQLiteDatabase db = new PuzzleDBHelper(getApplicationContext()).getWritableDatabase();
        String saveG =   getSaveLayout(dimensionUpdate(), colSize);
        String sqlQuery = 	"UPDATE puzzle SET save_game_layout = '" + saveG + "' WHERE name = '"  + puzzleName + "';";
        db.execSQL(sqlQuery);
    }

//Reverting back using the save state.
    void loadGame(String puzzleName) {

        String result = "";
        ArrayList<String> lst = new ArrayList<String>();
        SQLiteDatabase db = new PuzzleDBHelper(getApplicationContext()).getReadableDatabase();
        String[] projection = {PuzzleDBContract.puzzleEntry._ID,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME
        };

        Cursor c = db.query(PuzzleDBContract.puzzleEntry.TABLE_NAME, projection, null, null, null, null, null);
        for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {

            String fname = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME));

            if (fname.equals(puzzleName))
            {
                String fsave = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME));
                result = fsave;
                break;
            }
        }
        c.close();
        MainActivity activity = new MainActivity();

        ArrayList<Integer> board = activity.getBoardOffline(result);
        int loadRowSize = activity.getRowCount();
        int loadColize = activity.getColCount();
        workBoardA = to2D(board, loadColize, loadRowSize);
        ArrayList<Bitmap> bitmaps = bitmapArray(workBoardA);
        makeBoard2D(board, bitmaps);
    }

    private  String getSaveLayout(List<Integer> lst, int rows) //list format into how the original json layout looks like
    {
        StringBuilder sb = new StringBuilder();
        int rowIndex= 0;
        int eSize = 0;
        int size = lst.size();
        for(int i = 0; i< lst.size(); i++)
        {
            eSize++;
            if(rowIndex == rows)
            {
                sb.append("]");
                rowIndex =0;
            }
            if(lst.get(i) == 0)
            {
                sb.append(" empty ");
                rowIndex ++;
                continue;
            }
            sb.append(String.valueOf(lst.get(i)));
            sb.append(" ");
            rowIndex ++;
            if(eSize == size)
            {
                //Do nothing
            }
        }
        sb.append("]");
        return  sb.toString();
    }
    private  int[][] to2D( ArrayList<Integer> board, int colSize, int rowSize) //Take array and convert to 2d dimensions
    {
        int[][] board2d =new int[colSize][rowSize];   //describe layout

        int rc= 0;
        int cc= 0;
        for (int i = 0; i < board.size(); i++) {
            if(cc == colSize)
            {
                cc =0;
                rc++;
            }
            int num = board.get(i);
            board2d[cc][rc] = num;
            cc++;
        }
        return  board2d;
    }


    private  ArrayList<Bitmap> bitmapArray( int[][] puzzleBoard) //Return array of bitmap ids
    {
        ArrayList<Bitmap> imgList = new ArrayList<Bitmap>();
        imageV = new ImageView[size];
        for (int j = 0; j < rowSize; j++)
        {
        for (int i = 0; i < colSize; i++)
        {
                try
                {
                    int num = puzzleBoard[i][j];
                    if(  num == 0)
                    {
                        imgList.add(null);
                        continue;
                    }
                    String imgName = String.valueOf( puzzleBoard[i][j]) + ".jpg";
                    File f=new File(getFilesDir(), puzzleFolder + "/" + imgName); // SaveLocation is Puzzle folder number + image numbe.
                    Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                    imgList.add(b);
                }
                catch (FileNotFoundException e) {}
            }
        }
        return imgList;
    }

    private void spaceToTile(int id, char direction)//Method returns ids of tile between selected and space
    {
        boolean foundSpace = false;
        int colPos = getPos(id, "col");
        int rowPos = getPos(id, "row");
        ArrayList<Integer> selToSpace = new ArrayList<Integer>();

        switch (direction)
       {
           case 'd':  //Look ahead at rows for a space.
               for(int i = rowPos; i< rowSize; i++)  //Iterate through all rows in the current column
               {
                   try {
                       if (workBoardA[colPos][i + 1] == 0)   //Space found end search and swap.
                       {
                           foundSpace = true; //Its a valid move operation as space exists in the direction.
                           break;
                       } else {
                           selToSpace.add(workBoardA[colPos][i + 1]);
                       }
                   }
                  catch (Exception e){ //Array out of index exception means no space found aka illegal move.

                      return;
                  }
               }
            break;
           case 'u':
               try {
                       for(int i = rowPos; i< rowSize; i--)  //Iterate through all rows in the current column
                       {
                               if (workBoardA[colPos][i -1] == 0)   //Space found end search and swap.
                               {
                                   foundSpace = true; //Its a valid move operation as space exists in the direction.
                                   break;
                               } else {
                                   selToSpace.add(workBoardA[colPos][i - 1]);
                               }
                           }

                       }
               catch (Exception e){ //Array out of index exception means no space found aka illegal move.

               }
                       break;
           case 'r':
               try {

                   for (int i = colPos; i < colSize; i++)  //Iterate through all rows in the current column
                   {
                       if (workBoardA[i +1][rowPos] == 0)   //Space found end search and swap.
                       {
                           foundSpace = true; //Its a valid move operation as space exists in the direction.
                           break;
                       } else {
                           selToSpace.add(workBoardA[i + 1][rowPos]);
                       }
                   }
               }
               catch(Exception e){}
               break;

           case 'l':
               try {

                   for (int i = colPos; i < colSize; i--)  //Iterate through all rows in the current column
                   {
                       if (workBoardA[i -1][rowPos] == 0)   //Space found end search and swap.
                       {
                           foundSpace = true; //Its a valid move operation as space exists in the direction.
                           break;
                       } else {
                           selToSpace.add(workBoardA[i - 1][rowPos]);
                       }
                   }
               }
               catch(Exception e){}
               break;
               }
        if(foundSpace)
        {
            if(selToSpace.size() == 0) //No tiles between selected and space
            {
                swapById(id, 0);
            }
            else if(selToSpace.size() == 1) //1 tile between
            {
                int tile = selToSpace.get(0);   //get the last item in selToSpace
                swapById(tile, 0);
                swapById(id, 0);
            }
	else if(selToSpace.size() == 2)//2 tiles between
        {
            int last = selToSpace.get(selToSpace.size() - 1);
            int last2 = selToSpace.get(selToSpace.size() - 2); //2nd to last
            swapById(last, 0);
            swapById(last2, 0);
            swapById(id, 0);
        }
            movesCount +=1;
            checkEnd(workBoardA);
        }
        else//Invalid move!
         {}
       }

    private int getPos(int id, String type)
    {
        for(int i = 0; i< colSize; i++)
        {
            for(int j = 0; j< rowSize; j++)
            {

                if(type == "col")
                {
                    if(workBoardA[i][j] == id)
                    {
                        return i;
                    }
                }
                else if(type == "row")
                {
                    if(workBoardA[i][j] == id)
                    {
                        return j;
                    }
                }

            }
        }
        return 0;
    }

private  void swapById(int id1, int id2)//High level method taking 2 tiles and swapping their positions.
{
    int col1 = 0; int row1 = 0; int col2 = 0; int row2 = 0;
     int aRowSize =rowSize ;
    int aColSize =colSize;

    for (int i = 0; i< aColSize; i++ )
    {
        for(int j =0; j <aRowSize;j ++)
        {
            if(workBoardA[i][j] == id1)
            {
                  row1 =j;
                    col1 =i;
            }
            if(workBoardA[i][j] == id2)
            {
                row2 =j;
                col2 =i;
            }
        }
    }
    swap(col1, row1, col2, row2);
    ArrayList<Integer> list =   dimensionUpdate();
    ArrayList<Bitmap> bitmaps = bitmapArray(workBoardA);
    makeBoard2D(list, bitmaps);
}

private void swap(int col1, int row1, int col2, int row2)
{
    int temp = workBoardA[col1][row1];
    workBoardA[col1][row1] = workBoardA[col2][row2];
    workBoardA[col2][row2] = temp;
}

private  ArrayList<Integer> dimensionUpdate()
{
    ArrayList<Integer> list = new ArrayList<Integer>();

    for (int i = 0; i < rowSize; i++) {
        for (int j = 0; j < colSize; j++) {
            int num = workBoardA[j][i];
            list.add(num);
        }
    }
                return list;
}


    @Override
    public void onResume() {
        super.onResume();
        loadGame(puzzleName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
      saveGame(puzzleName);
    }

    private double getTimer() //display time taken to complete puzzle.
    {
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        return  elapsedSeconds;
    }

    private boolean checkEnd(int[][] layout)
    {
        boolean complete  = false;
        String strLayout = "";
        String winLayout = "";
        if(rowSize == 3 && colSize == 3) {winLayout = "02131122232132333";}
        if(rowSize == 4 && colSize == 3) {winLayout = "02131122232132333142434";}
        if(rowSize == 3 && colSize == 4) {winLayout = "02131411222324213233343";}
     if(rowSize == 4 && colSize == 4) {winLayout = "0213141122232421323334314243444";}

        for(int i = 0; i < rowSize; i++)
            {
                for(int j =0 ; j< colSize; j++)
                {
                    strLayout+=  layout[j][i];
                }
            }
if(strLayout.equals( winLayout)) {
    complete = true;
    int score = Integer.valueOf( readHighScore());
    if(movesCount < score || score == 0 )
    {
        setHighScore(String.valueOf(movesCount));
        Toast.makeText(getBaseContext(),getBaseContext().getString(R.string.toastRecord), Toast.LENGTH_SHORT).show();
    }
else
    {
        Toast.makeText(getBaseContext(),getBaseContext().getString(R.string.toastNoRecord), Toast.LENGTH_SHORT).show();
    }
    String secondsstr= getBaseContext().getString(R.string.toastSeconds );
    String movesstr = getBaseContext().getString(R.string.toastMoves );
    String winnerstr = getBaseContext().getString(R.string.toastWinner2 );
    String messagestr = winnerstr + getTimer() + " " + secondsstr + "\n" + movesstr +  ":" + movesCount ;
    Toast.makeText(getBaseContext(),messagestr, Toast.LENGTH_LONG).show();
    movesCount= 0;
    tStart = System.currentTimeMillis();
}
        return  complete;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            float diffY = event2.getY() - event1.getY();
            float diffX = event2.getX() - event1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }
            }
            return true;
        }
    }

    private void onSwipeLeft() {takeAction('l',  tileMoved);}

    private void onSwipeRight() {takeAction('r',  tileMoved);}

    private void onSwipeTop() {
        takeAction('u',  tileMoved);
    }

    private void onSwipeBottom() {
        takeAction('d',  tileMoved);
    }
}
//Swipe gesture section of the code had parts borrowed from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures.
