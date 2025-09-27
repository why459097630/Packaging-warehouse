//SetPriceActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetPriceActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_price)

        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            // 设定价格逻辑
            val price = editText.text.toString().toDouble()
            // 保存价格
        }
    }
}
