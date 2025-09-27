//CommentActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class CommentActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            // 评论逻辑
            val comment = editText.text.toString()
            // 保存评论
        }
    }
}
