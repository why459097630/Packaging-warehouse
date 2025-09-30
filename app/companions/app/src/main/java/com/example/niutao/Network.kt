// Network.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.niutao.R
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

class NetworkActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val response = sendRequest(editText.text.toString())
            Toast.makeText(this, "请求成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRequest(url: String): String {
        // 发送网络请求逻辑
        return "" // 返回请求结果
    }
}
