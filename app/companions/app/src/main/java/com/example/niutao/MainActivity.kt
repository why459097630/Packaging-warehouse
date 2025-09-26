import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        val editText = findViewById<EditText>(R.id.editText)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val priceEditText = findViewById<EditText>(R.id.priceEditText)
        val commentButton = findViewById<Button>(R.id.commentButton)
        val likeButton = findViewById<Button>(R.id.likeButton)

        button.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            startActivity(intent)
        }

        commentButton.setOnClickListener {
            val comment = editText.text.toString()
            val price = priceEditText.text.toString().toDouble()
            val likeCount = likeButton.tag as Int
            likeCount++
            likeButton.tag = likeCount
            Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
        }
    }
}
