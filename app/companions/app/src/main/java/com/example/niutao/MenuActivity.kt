import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class MenuActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var menuName: EditText
    private lateinit var menuPrice: EditText
    private lateinit var menuDescription: EditText
    private lateinit var menuImage: ImageView
    private lateinit var uploadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        db = FirebaseFirestore.getInstance()
        menuName = findViewById(R.id.menu_name)
        menuPrice = findViewById(R.id.menu_price)
        menuDescription = findViewById(R.id.menu_description)
        menuImage = findViewById(R.id.menu_image)
        uploadButton = findViewById(R.id.upload_button)

        uploadButton.setOnClickListener {
            // 上传照片逻辑
        }
    }
}
