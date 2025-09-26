import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Menu::class, Comment::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun commentDao(): CommentDao
}
