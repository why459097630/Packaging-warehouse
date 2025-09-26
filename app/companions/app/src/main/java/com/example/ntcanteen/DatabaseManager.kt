import android.content.Context
import android.database.sqlite.SQLiteDatabase

class DatabaseManager(context: Context) {
    private val database: Database = Database(context)
    private val commentDatabase: DatabaseHelper = DatabaseHelper(context)
    private val likeDatabase: DatabaseHelper2 = DatabaseHelper2(context)

    fun insertMenu(name: String, image: Int) {
        database.writableDatabase.execSQL("INSERT INTO menu (name, image) VALUES (?, ?)", arrayOf(name, image))
    }

    fun insertComment(text: String, image: Int) {
        commentDatabase.writableDatabase.execSQL("INSERT INTO comment (text, image) VALUES (?, ?)", arrayOf(text, image))
    }

    fun insertLike(text: String, image: Int) {
        likeDatabase.writableDatabase.execSQL("INSERT INTO like (text, image) VALUES (?, ?)", arrayOf(text, image))
    }
}
