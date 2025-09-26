import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class CommentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            // 评论
            val comment = findViewById<EditText>(R.id.commentEditText).text.toString()
            Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
        }
    }
}
