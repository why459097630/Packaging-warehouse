import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val text = findViewById<EditText>(R.id.text)
            Toast.makeText(this, "编辑成功", Toast.LENGTH_SHORT).show()
        }
    }
}