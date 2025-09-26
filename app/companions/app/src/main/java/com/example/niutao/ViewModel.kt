import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelScope

class ViewModel : ViewModel() {
    private val dao: Dao = DaoImpl(Database.getInstance())

    fun insertMenu(menu: Menu) {
        viewModelScope.launch {
            dao.insertMenu(menu)
        }
    }

    fun getMenu(): List<Menu> {
        return dao.getMenu()
    }

    fun insertComment(comment: Comment) {
        viewModelScope.launch {
            dao.insertComment(comment)
        }
    }

    fun getComment(): List<Comment> {
        return dao.getComment()
    }
}
