import androidx.room.Room
import androidx.room.RoomDatabase

class DaoImpl(private val database: Database) : Dao {
    override suspend fun insertMenu(menu: Menu) {
        database.menuDao().insert(menu)
    }

    override suspend fun getMenu(): List<Menu> {
        return database.menuDao().getMenu()
    }

    override suspend fun insertComment(comment: Comment) {
        database.commentDao().insert(comment)
    }

    override suspend fun getComment(): List<Comment> {
        return database.commentDao().getComment()
    }
}
