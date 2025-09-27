import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UploadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val storage = FirebaseStorage.getInstance()
            val storageReference = storage.reference
            val file = File("image.jpg")
            val uploadTask = storageReference.child("images/image.jpg").putFile(file)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "上传成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "上传失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}