//LikeActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LikeActivity : AppCompatActivity() {
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        button = findViewById(R.id.button)

        button.setOnClickListener {
            // 点赞逻辑
            // 保存点赞状态
        }
    }
}
