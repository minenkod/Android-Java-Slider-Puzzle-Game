package mobile.labs.acw;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

///////////////////////////////////////////////////////Class is completely independent from an internet connection////////////////
public class offline_puzzles extends Fragment {
    boolean newgame = false;
    ListView offlineList;
    MainActivity activity;
    SeekBar bar;
    String pictureSet = "";
    CheckBox chk;
    TextView txtFilter;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.offline_puzzles, container, false);
        offlineList = (ListView) view.findViewById(R.id.lstpuzzle);
        bar = (SeekBar) view.findViewById(R.id.seekbr);
        activity = (MainActivity) getActivity();
        txtFilter = (TextView) view.findViewById(R.id.txtFilter);
        chk = (CheckBox) view.findViewById(R.id.chkGame);
        List<String> puzzlelst = filterPuzzle("");
        setListView(puzzlelst);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int barVal = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                barVal = i;
                setListView(filter(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getActivity(), getContext().getString(R.string.toastFilter), Toast.LENGTH_SHORT).show();
            }
        });
        offlineList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Information needed to move onto puzzle activity : puzzle name, layout number, board array, column and row size.
                String puzzleName = offlineList.getItemAtPosition(i).toString();  //Get this from the downloaded puzzles
                puzzleName = puzzleName.substring(0, puzzleName.indexOf("-"));
                puzzleName = puzzleName.replaceAll("(\\r|\\n)", "");
                String newGameLayout = loadData(puzzleName, true);
                if (chk.isChecked()) {
                    newgame = true;
                    overwriteSave(newGameLayout, puzzleName);   //overwrite newgame layout
                } else {
                    newgame = false;
                }
                String saveGameLayout = loadData(puzzleName, false);
                Intent intent = new Intent(getActivity(), puzzle_class.class);
                activity.getBoardOffline(saveGameLayout);    //Pass the new game layout and save state layout to the puzzle file.
                intent.putExtra("puzzleName", puzzleName);
                intent.putExtra("rowSize", String.valueOf(activity.getRowCount()));
                intent.putExtra("colSize", String.valueOf(activity.getColCount()));
                intent.putExtra("picFolder", pictureSet);
                ArrayList<Integer> workBoard = activity.getBoardOffline(saveGameLayout);
                intent.putIntegerArrayListExtra("work-board", workBoard);
                startActivity(intent);
            }
        });
        return view;
    }

    private  void overwriteSave(String newgamelayout, String puzzleName)
    {
        SQLiteDatabase db = new PuzzleDBHelper(getContext()).getWritableDatabase();
        String sqlQuery = "UPDATE puzzle SET save_game_layout = '" + newgamelayout + "' WHERE name = " + "'" +  puzzleName + "';";
        db.execSQL(sqlQuery);
    }

    private ArrayList<String> filter(int val) {
        ArrayList<String> lst = new ArrayList<String>();
        String filterMessage = "";
        switch (val) {
            case 0:
                lst = filterPuzzle("");
                 filterMessage = getString(R.string.dtf );
                txtFilter.setText(filterMessage);
                break;
            case 1: //3 x3
                lst = filterPuzzle("3 x 3 ");
                filterMessage = getString(R.string.tbt );
                txtFilter.setText(filterMessage);
                break;
            case 2: //3 x4
                lst = filterPuzzle("3 x 4 ");
                filterMessage = getString(R.string.tbf );
                txtFilter.setText(filterMessage);
                break;
            case 3: // 4 x 3
                lst = filterPuzzle("4 x 3 ");
                filterMessage = getString(R.string.fbt );
                txtFilter.setText(filterMessage);
                break;

            case 4:
                lst = filterPuzzle("4 x 4 ");
                filterMessage = getString(R.string.fbf );
                txtFilter.setText(filterMessage);

                break;
            case 5:
                lst = filterPuzzle("hs");      //Puzzles ordered by a highest score.  (Lowest move count);
                filterMessage = getString(R.string.hs );
                txtFilter.setText(filterMessage);
                break;
        }
        return lst;
    }

    private void setListView(List<String> lst) {
        ArrayAdapter<String> puzzleList = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, lst);
        offlineList.setAdapter(puzzleList);
    }

        private String loadData(String puzzleName, boolean innewGame) {
            String result = "";
            SQLiteDatabase db = new PuzzleDBHelper(getActivity()).getReadableDatabase();
            String[] projection = {PuzzleDBContract.puzzleEntry._ID,
                    PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME,
                    PuzzleDBContract.puzzleEntry.COLUMN_NAME_NEWGAME,
                    PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME,
                    PuzzleDBContract.puzzleEntry.COLUMN_NAME_PICTURE

            };
            Cursor c = db.query(PuzzleDBContract.puzzleEntry.TABLE_NAME, projection, null, null, null, null, null);
            for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {
                if (puzzleName.equals(c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME)))) {
                    if (innewGame) {
                        result = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NEWGAME));
                    } else {
                        result = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME));
                    }
                    pictureSet = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_PICTURE));
                    c.close();
                    return result;
                }
            }
            c.close();
            return result;
        }

    private ArrayList<String> filterPuzzle(String size) {
        ArrayList<String> lst = new ArrayList<String>();
        SQLiteDatabase db = new PuzzleDBHelper(getActivity()).getReadableDatabase();

        String[] projection = {PuzzleDBContract.scoreEntry._ID,
                PuzzleDBContract.scoreEntry.COLUMN_NAME_NAME,
                PuzzleDBContract.scoreEntry.COLUMN_NAME_SIZE,
                PuzzleDBContract.scoreEntry.COLUMN_NAME_SCORE

        };
        Cursor c = db.query(PuzzleDBContract.scoreEntry.TABLE_NAME, projection, null, null, null, null, null);

        for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {
            // Stuff
            String fsize= c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.scoreEntry.COLUMN_NAME_SIZE));
            String fname =  c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.scoreEntry.COLUMN_NAME_NAME));
            String fscore =  c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.scoreEntry.COLUMN_NAME_SCORE));
            if (size.equals(fsize)) {
                lst.add(fname + "-      "  + fsize);
            }
            else if(size.equals(""))
            {
                lst.add(fname + "-      "  + fsize);
            }
            else if(size.equals("hs"))
            {
                if(Integer.valueOf(fscore).equals(0))
                {
                    fscore = " NA";
                }
                String highscoreStr = getContext().getString(R.string.toastMoves2);
                lst.add(fname + "- " + highscoreStr +  fscore );
            }
        }
        c.close();
        return lst;
    }
}


