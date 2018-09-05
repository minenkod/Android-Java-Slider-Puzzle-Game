package mobile.labs.acw;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import  android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import  android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.ListViewCompat;
import android.util.Log;
import  android.view.LayoutInflater;
import  android.view.View;
import  android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

import static android.R.attr.name;

//The fragment that needs an internet connection
public class list_fragment extends Fragment {
    ListView onlineList;
    MainActivity activity;
    TextView txtStatus;
    boolean internetAva = false;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_section, container, false);
        onlineList = (ListView) view.findViewById(R.id.indexList);
        txtStatus = (TextView) view.findViewById(R.id.txtStatus);
        activity = (MainActivity) getActivity();
        internetAva = isNetworkAvailable();
        if(internetAva)
        {
            setListView();
            onlineList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                   try {
                    Toast.makeText(getContext(),getContext().getString(R.string.toastDownStart), Toast.LENGTH_SHORT).show();
                    String puzzleName = onlineList.getItemAtPosition(i).toString();  //Get this from the downloaded puzzles
                    activity.downloadP(puzzleName);
                      sendData();
                    setListView();
                 Toast.makeText(getContext(),getContext().getString(R.string.toastDownFinish), Toast.LENGTH_SHORT).show();
                   }catch (Exception e){  txtStatus.setText(R.string.offlineMode);
                   }
                }
            });
        }
       else
        {
            txtStatus.setText(R.string.offlineMode);   //internet is not available, inform user to play the offline puzzles.
        }
        return view;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

public  void sendData() //Listview in the offline puzzle fragment need to be updated.
{
    android.support.v4.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
    ft.replace(R.id.offlineFrame, new offline_puzzles());
    ft.replace(R.id.onlineFrame, new list_fragment());
    ft.commit();
}
public  void setListView()
{
    List<String> onlineOnly = getUnique();
    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, onlineOnly);
    onlineList.setAdapter(arrayAdapter);
}

    //Get existing puzzle folders and online folders. Then create list holding only the unique online puzzles.
    private List<String> uniqueOnline(String[] onlineP) {
        String result = activity.runJsonQuery("http://www.simongrey.net/08027/slidingPuzzleAcw/index.json", "PuzzleIndex", "");
        String[] lines = onlineP;

        List<String> uniqueOnlineP = new ArrayList<String>();
        List<String> puzzleList = addPuzzles();
        if (puzzleList.size() == 0) {
            uniqueOnlineP = Arrays.asList(lines);
            return uniqueOnlineP;
        } else {
            List<String> normalList = Arrays.asList(lines);

            for (String item : normalList) {

                String puzzle = item;
                if (puzzleList.contains(puzzle)) {        //Item is already downloaded dont add

                } else {
                    uniqueOnlineP.add(puzzle);
                }
            }
        }
        return uniqueOnlineP;
    }

private List<String> getUnique ()
{

    String result = activity.runJsonQuery("http://www.simongrey.net/08027/slidingPuzzleAcw/index.json", "PuzzleIndex", "");
    String[] lines = result.split(System.getProperty("line.separator"));
    String[] Clines = new String[lines.length];

    for(int i =0; i < lines.length; i++)
    {
        String cleanLine = lines[i].substring(0, lines[i].indexOf("."));
        Clines[i] = cleanLine;
    }
    List<String> onlineOnly = uniqueOnline(Clines);
    return  onlineOnly;

}

    private List<String> addPuzzles()
    {
        List<String> puzzleList = new ArrayList<String>();

        SQLiteDatabase db = new PuzzleDBHelper(getActivity()).getReadableDatabase();
        String[] projection = {PuzzleDBContract.puzzleEntry._ID,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME ,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_NEWGAME ,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME,
                PuzzleDBContract.puzzleEntry.COLUMN_NAME_PICTURE

        };
        Cursor c = db.query(PuzzleDBContract.puzzleEntry.TABLE_NAME, projection, null, null, null, null, null);
        for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {
            String name = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME));
            puzzleList.add(name);
        }
        c.close();
        return  puzzleList;
    }
}





