import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nittaro.menu.R
import com.google.firebase.firestore.FirebaseFirestore

class MenuRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    fun getMenus(): LiveData<List<Menu>> {
        val menus = MutableLiveData<List<Menu>>()
        db.collection("menus")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val menuList = task.result!!.toObjects(Menu::class.java)
                    menus.value = menuList
                } else {
                    Log.e("MenuRepository", "Error getting menus", task.exception)
                }
            }
        return menus
    }

    fun addMenu(menu: Menu) {
        db.collection("menus")
            .add(menu)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MenuRepository", "Menu added successfully")
                } else {
                    Log.e("MenuRepository", "Error adding menu", task.exception)
                }
            }
    }
}
