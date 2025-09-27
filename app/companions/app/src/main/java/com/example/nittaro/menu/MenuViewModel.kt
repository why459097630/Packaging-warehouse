import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.nittaro.menu.R
import com.example.nittaro.menu.MenuRepository

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MenuRepository

    init {
        repository = MenuRepository(application)
    }

    fun getMenus(): LiveData<List<Menu>> {
        return repository.getMenus()
    }

    fun addMenu(menu: Menu) {
        repository.addMenu(menu)
    }
}
