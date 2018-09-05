package mobile.labs.acw;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ExpandedMenuView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    int rowSize = 0;
    int colSize = 0;
    PuzzleDBHelper mDbHelper = new PuzzleDBHelper(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       //  File f = new File(getFilesDir(), "");     //Uncomment to delete puzzles on next run
        // clearAll(f);
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.offlineFrame, new offline_puzzles());
        ft.replace(R.id.onlineFrame, new list_fragment());
        ft.commit();
    }

    private  void writeInfo(String puzzleName, String layout, String pictureSet)
    {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.clear();
        values.put(PuzzleDBContract.scoreEntry.COLUMN_NAME_NAME, puzzleName);
        values.put(PuzzleDBContract.scoreEntry.COLUMN_NAME_SCORE, 0);
        String size = colSize + " x " + rowSize + " ";
        values.put(PuzzleDBContract.scoreEntry.COLUMN_NAME_SIZE, size);
        db.insert(PuzzleDBContract.scoreEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NAME, puzzleName);
        values.put(PuzzleDBContract.puzzleEntry.COLUMN_NAME_NEWGAME, layout);
        values.put(PuzzleDBContract.puzzleEntry.COLUMN_NAME_SAVEGAME, layout);
        values.put(PuzzleDBContract.puzzleEntry.COLUMN_NAME_PICTURE, pictureSet);
        db.insert(PuzzleDBContract.puzzleEntry.TABLE_NAME, null, values);
    }

    public void deleteRecursive(File file) {

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }

    void clearAll(File f) {
        File[] files = f.listFiles();
        for (File file : files) {
            File sda = new File(file.getPath());
            deleteRecursive(sda);
        }
    }
    String result = "";
    String formattedResult = "";

    public int getRowCount()//
    {
        return rowSize;
    }

    public int getColCount() //
    {
        return colSize;
    }

    public ArrayList<Integer> getBoardOffline(String layout) //Layout structure is passed in
    {
        rowSize = 0;
        colSize = 0;
        String fNum = "";
        ArrayList<Integer> numList = new ArrayList<Integer>();
        ArrayList<Integer> finalList = new ArrayList<Integer>();
        char letter2 = ' ';
        for (int i = 0; i < layout.length(); i++) {
            char letter = layout.charAt(i);

            if (letter == ']')
                rowSize++;
            else if (Character.isDigit(letter)) {
                letter2 = layout.charAt(i + 1);
                fNum = String.valueOf(letter) + String.valueOf(letter2);
                int num = Integer.parseInt(fNum);
                numList.add(num);
                ++i;
            } else if (letter == 'e') {
                numList.add(0);
                i += 5;
            }
        }
        colSize = numList.size() / rowSize;
        return numList;
    }

    public ArrayList<Integer> getBoard(String layoutNumber) //Layout structure is passed in
    {
        String layoutUrl = "http://www.simongrey.net/08027/slidingPuzzleAcw/layouts/" + layoutNumber;
        String pSetResult = runJsonQuery(layoutUrl, "layout", "");
        String fNum = "";
        ArrayList<Integer> numList = new ArrayList<Integer>();
        char letter2 = ' ';
        for (int i = 0; i < pSetResult.length(); i++) {
            char letter = pSetResult.charAt(i);
            if (letter == ']')
                rowSize++;
            else if (Character.isDigit(letter)) {
                letter2 = pSetResult.charAt(i + 1);
                fNum = String.valueOf(letter) + String.valueOf(letter2);
                int num = Integer.parseInt(fNum);
                numList.add(num);
                ++i;
            } else if (letter == 'e') {
                numList.add(0);
                i += 5;
            }
        }
        colSize = numList.size() / rowSize;
        return numList;
    }

    public String runJsonQuery(final String url, final String attr, final String qType) {
        try {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        result = "";
                        formattedResult = "";
                        InputStream stream = (InputStream) new URL(url).getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line = "";
                        while (line != null) {
                            result += line;
                            line = reader.readLine();
                        }

                        JSONObject json = new JSONObject(result);
                        if (qType == "single") {
                            String single = json.getString(attr);
                            formattedResult = single;
                            return;
                        }

                        JSONArray puzzles = json.getJSONArray(attr);
                        for (int i = 0; i < puzzles.length(); ++i) {
                            formattedResult += puzzles.get(i) + "\r\n";
                            //   PuzzleArray[i] = puzzles.get(i).toString();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            t.join();

        } catch (Exception e) {
        }

        return formattedResult;
    }

    public boolean downloadP(String puzzleName) {
        boolean complete = false;
        colSize = 0;
        rowSize = 0;
        String puzzleFile = puzzleName + ".json";
        String pSetUrl = "http://www.simongrey.net/08027/slidingPuzzleAcw/puzzles/" + puzzleFile;
        String pSetResult = runJsonQuery(pSetUrl, "PictureSet", "single"); //Approriate picture set name for the chosen puzzle.
        String layoutResult = runJsonQuery(pSetUrl, "layout", "single");     //The layout for downloading pictures.
        String layoutUrl = "http://www.simongrey.net/08027/slidingPuzzleAcw/layouts/" + layoutResult;
        String layoutData = runJsonQuery(layoutUrl, "layout", "");     //The layout for downloading pictures.
        layoutData = layoutData.replaceAll("(\\r|\\n)", "");
        //with the layout determine puzzle size.
        ArrayList<Integer> puzzleBoard = getBoard(layoutResult); //Get the appropriate board array matching the downloaded board.
        writeInfo(puzzleName, layoutData, pSetResult); //Create file Responsible for holding puzzle layout for offline play plus player high scores.

        File PicFolder = new File(getFilesDir(), pSetResult);
        if (!PicFolder.exists()) {
            PicFolder.mkdir();
            ExecutorService pool = Executors.newFixedThreadPool(10);
            try {
            for (int i = 0; i < puzzleBoard.size(); i++)//Create a text file with the puzzles layout number, high scores stored in this file.
            {
                if (puzzleBoard.get(i) == 0) {
                    continue;
                }
                String piece = String.valueOf(puzzleBoard.get(i)) + ".jpg";
                String strPuzzleUrl = "http://www.simongrey.net/08027/slidingPuzzleAcw/images/" + pSetResult + "/" + piece;
                    String saveLocation = pSetResult + "/" + piece;
                    pool.submit(new DownloadTask(strPuzzleUrl, saveLocation, this));
            }
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
         complete = true;
            } catch (Exception e) {
                Log.i("", e.getMessage());
            }
        }
        return  complete;
    }

    private static class DownloadTask implements Runnable {
        String saveLocation = "";
        String piece = "";
        private String url;
        private final String path;
        MainActivity activity;
        public DownloadTask(String name, String toPath, MainActivity inActivity) {
            this.url = name;
            this.path = toPath;
            this.activity  = inActivity;
        }

        private void downloadFile(String inUrl, String path, MainActivity activity) {
            Bitmap bitmap = null;
            try {
                url = inUrl;
                bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
                saveLocation = path;
                String puzzleName = saveLocation.substring(0, saveLocation.indexOf("/"));
                piece = saveLocation.substring(saveLocation.indexOf("/") + 1); //trim string to get only the name of piece.
                File file = new File(activity.getFilesDir(), piece);
                FileOutputStream writer = new FileOutputStream(file);
                try {
                    writer =activity.getApplicationContext().openFileOutput(piece, Context.MODE_PRIVATE);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, writer);
                } catch (Exception e) {
                } finally {
                    writer.close();
                }
            } catch (Exception e) {
            }
        }

        @Override
        public void run() {

            downloadFile(url, path, activity);   // surround with try-catch if downloadFile() throws something
            copyAll(); //move file
        }

        public void copyAll() {
            piece = "/" + piece;
            File source = new File(activity.getFilesDir(), piece);
            File dest = new File(activity.getFilesDir(), saveLocation);
            try {
                copyFile(source, dest);
            } catch (Exception e) {
            }
        }

        public void copyFile(File src, File dst) throws IOException {
            //Method takes all downloaded images and places them into the correct folder then deletes the image from original source.
            FileChannel inChannel = new FileInputStream(src).getChannel();
            FileChannel outChannel = new FileOutputStream(dst).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
                src.delete();
            }
        }

    }
}
