import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

class UploadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        val button = findViewById<Button>(R.id.upload_button)
        button.setOnClickListener {
            val image = findViewById<ImageView>(R.id.image)
            val description = findViewById<EditText>(R.id.description)
            val price = findViewById<EditText>(R.id.price)

            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("image", image.drawable)
            intent.putExtra("description", description.text.toString())
            intent.putExtra("price", price.text.toString())
            startActivity(intent)
        }
    }
}
