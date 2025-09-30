// Menu.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.niutao.R
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

data class Menu(
    val name: String,
    val description: String,
    val price: Double
)

class MenuActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        imageView = findViewById(R.id.imageView)
        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val menu = Menu(editText.text.toString(), "", 0.0)
            Toast.makeText(this, "菜品上传成功", Toast.LENGTH_SHORT).show()
        }
    }
}
