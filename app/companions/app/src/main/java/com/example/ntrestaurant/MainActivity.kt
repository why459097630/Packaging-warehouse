import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            // 上传照片
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        val editText = findViewById<EditText>(R.id.editText)
        val textView = findViewById<TextView>(R.id.textView)
        val priceEditText = findViewById<EditText>(R.id.priceEditText)
        val commentEditText = findViewById<EditText>(R.id.commentEditText)
        val likeButton = findViewById<Button>(R.id.likeButton)

        button.setOnClickListener {
            // 编辑文字介绍
            val text = editText.text.toString()
            textView.text = text

            // 设定价格
            val price = priceEditText.text.toString().toDouble()
            // 评论点赞
            val comment = commentEditText.text.toString()
            likeButton.setOnClickListener {
                // 点赞
                Toast.makeText(this, "点赞成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val imageView = findViewById<ImageView>(R.id.imageView)
            imageView.setImageURI(imageUri)
        }
    }
}
