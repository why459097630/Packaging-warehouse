import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.ArrayList

class LikeActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        imageView = findViewById(R.id.image_view)
        editText = findViewById(R.id.edit_text)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val like = Like(editText.text.toString(), imageView.drawable)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("like", like)
            startActivity(intent)
        }
    }
}
