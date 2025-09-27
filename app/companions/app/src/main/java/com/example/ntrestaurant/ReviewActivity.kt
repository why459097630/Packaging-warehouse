import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

class ReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val button = findViewById<Button>(R.id.review_button)
        button.setOnClickListener {
            val description = findViewById<EditText>(R.id.description)
            val rating = findViewById<RatingBar>(R.id.rating)

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("description", description.text.toString())
            intent.putExtra("rating", rating.rating)
            startActivity(intent)
        }
    }
}
