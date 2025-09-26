import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.random.Random

class UploadActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        imageView = findViewById(R.id.image_view)
        editText = findViewById(R.id.edit_text)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val photo = takePhoto()
            imageView.setImageBitmap(photo)
        }
    }

    private fun takePhoto(): Bitmap {
        val photo = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(photo)
        canvas.drawColor(Color.WHITE)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(50f, 50f, 40f, paint)
        return photo
    }
}
