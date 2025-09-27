// MainActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.niutao.canteen.R
import com.example.niutao.canteen.model.Menu
import com.example.niutao.canteen.util.FileUtil
import com.example.niutao.canteen.util.PhotoUtil

class MainActivity : AppCompatActivity() {
    private lateinit var menuEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var photoImageView: ImageView
    private lateinit var addButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menuEditText = findViewById(R.id.menu_edit_text)
        priceEditText = findViewById(R.id.price_edit_text)
        photoImageView = findViewById(R.id.photo_image_view)
        addButton = findViewById(R.id.add_button)

        addButton.setOnClickListener {
            val menu = Menu(menuEditText.text.toString(), priceEditText.text.toString(), PhotoUtil.getPhotoPath(this))
            // 上传菜品到服务器
            Toast.makeText(this, "菜品添加成功", Toast.LENGTH_SHORT).show()
        }
    }
}
