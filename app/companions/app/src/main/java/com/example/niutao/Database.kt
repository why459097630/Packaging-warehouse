// Database.kt
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.niutao.Menu
import com.example.niutao.Comment

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "menu.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE menu (id INTEGER PRIMARY KEY, name TEXT, description TEXT, price REAL)")
        db.execSQL("CREATE TABLE comment (id INTEGER PRIMARY KEY, content TEXT, likes INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 升级数据库逻辑
    }
}
