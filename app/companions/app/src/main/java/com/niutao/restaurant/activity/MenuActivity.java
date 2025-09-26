// MenuActivity.kt
class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        val name = findViewById<EditText>(R.id.name)
        val description = findViewById<EditText>(R.id.description)
        val price = findViewById<EditText>(R.id.price)
        val image = findViewById<ImageView>(R.id.image)
        val upload = findViewById<Button>(R.id.upload)
        val save = findViewById<Button>(R.id.save)

        upload.setOnClickListener {
            // 上传照片逻辑
        }

        save.setOnClickListener {
            // 保存菜品逻辑
        }
    }
}
