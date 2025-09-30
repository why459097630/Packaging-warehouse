// MainActivity.kt
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

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val photo = takePhoto()
            imageView.setImageBitmap(photo)
        }
    }

    private fun takePhoto(): Bitmap {
        // 上传照片逻辑
        return BitmapFactory.decodeStream(FileInputStream(File("/sdcard/photo.jpg")))
    }
}
