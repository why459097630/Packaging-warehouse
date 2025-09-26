import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class CommentActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var comment: Comment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            comment = Comment(editText.text.toString(), 0)
            Toast.makeText(this, "评论上传成功", Toast.LENGTH_SHORT).show()
        }
    }
}
