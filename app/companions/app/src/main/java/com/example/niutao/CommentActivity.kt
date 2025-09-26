import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CommentActivity : AppCompatActivity() {
    private lateinit var mEditText: EditText
    private lateinit var mImageView: ImageView
    private lateinit var mButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        mEditText = findViewById(R.id.editText)
        mImageView = findViewById(R.id.imageView)
        mButton = findViewById(R.id.button)

        mButton.setOnClickListener {
            likeComment()
        }
    }

    private fun likeComment() {
        // 点赞逻辑
    }
}
