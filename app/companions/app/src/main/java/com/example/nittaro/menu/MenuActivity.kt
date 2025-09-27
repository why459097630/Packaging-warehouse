import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nittaro.menu.R
import com.google.android.material.snackbar.Snackbar

class MenuActivity : AppCompatActivity() {
    private lateinit var menuName: EditText
    private lateinit var menuPrice: EditText
    private lateinit var menuDescription: EditText
    private lateinit var menuImage: ImageView
    private lateinit var uploadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        menuName = findViewById(R.id.menu_name)
        menuPrice = findViewById(R.id.menu_price)
        menuDescription = findViewById(R.id.menu_description)
        menuImage = findViewById(R.id.menu_image)
        uploadButton = findViewById(R.id.upload_button)

        uploadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.data
            menuImage.setImageURI(selectedImage)
        }
    }
}
