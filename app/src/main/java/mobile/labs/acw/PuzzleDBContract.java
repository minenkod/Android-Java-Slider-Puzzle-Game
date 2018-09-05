package mobile.labs.acw;

import android.provider.BaseColumns;

public class PuzzleDBContract {
    private PuzzleDBContract(){}

    public static abstract class puzzleEntry implements BaseColumns {
        public static final String TABLE_NAME = "puzzle";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_NEWGAME = "new_game_layout";
        public static final String COLUMN_NAME_SAVEGAME = "save_game_layout";
        public static final String COLUMN_NAME_PICTURE = "picture";
    }
    public static abstract class scoreEntry implements BaseColumns {
        public static final String TABLE_NAME = "highscore";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SIZE = "size";
        public static final String COLUMN_NAME_SCORE = "score";
    }


    public static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_PUZZLE_TABLE = "CREATE TABLE " + puzzleEntry.TABLE_NAME +
            " (" + puzzleEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
            puzzleEntry.COLUMN_NAME_NAME + COMMA_SEP + puzzleEntry.COLUMN_NAME_NEWGAME
            + COMMA_SEP + puzzleEntry.COLUMN_NAME_SAVEGAME + COMMA_SEP +  puzzleEntry.COLUMN_NAME_PICTURE +" )";

    public static final String SQL_CREATE_SCORE_TABLE = "CREATE TABLE " + scoreEntry.TABLE_NAME +
            " (" + scoreEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
            scoreEntry.COLUMN_NAME_NAME + COMMA_SEP + scoreEntry.COLUMN_NAME_SIZE + COMMA_SEP + scoreEntry.COLUMN_NAME_SCORE + " )";



}
