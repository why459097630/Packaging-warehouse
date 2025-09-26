import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface Dao {
    @Insert
    suspend fun insertMenu(menu: Menu)
    @Query("SELECT * FROM Menu")
    suspend fun getMenu(): List<Menu>
    @Insert
    suspend fun insertComment(comment: Comment)
    @Query("SELECT * FROM Comment")
    suspend fun getComment(): List<Comment>
}
