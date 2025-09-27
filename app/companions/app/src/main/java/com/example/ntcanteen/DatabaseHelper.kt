//DatabaseHelper.kt
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, "ntcanteen.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS menu (id INTEGER PRIMARY KEY, name TEXT, price REAL, comment TEXT)")
    }

    fun saveMenu(name: String, price: Double, comment: String) {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put("name", name)
        contentValues.put("price", price)
        contentValues.put("comment", comment)
        db.insert("menu", null, contentValues)
        db.close()
    }

    fun getMenu(): List<Menu> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM menu", null)
        val menus = mutableListOf<Menu>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val price = cursor.getDouble(2)
            val comment = cursor.getString(3)
            menus.add(Menu(id, name, price, comment))
        }
        cursor.close()
        db.close()
        return menus
    }
}
