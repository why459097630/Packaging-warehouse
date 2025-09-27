import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CommentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val text = findViewById<EditText>(R.id.text)
            Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
        }
    }
}