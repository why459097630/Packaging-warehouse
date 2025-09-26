import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper2(context: Context) : SQLiteOpenHelper(context, "like.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE like (id INTEGER PRIMARY KEY, text TEXT, image BLOB)")
    }
}
