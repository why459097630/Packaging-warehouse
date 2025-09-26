// MenuDetailActivity.kt
class MenuDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_detail)
        val name = findViewById<TextView>(R.id.name)
        val description = findViewById<TextView>(R.id.description)
        val price = findViewById<TextView>(R.id.price)
        val image = findViewById<ImageView>(R.id.image)
        val like = findViewById<Button>(R.id.like)
        val comment = findViewById<Button>(R.id.comment)

        like.setOnClickListener {
            // 点赞逻辑
        }

        comment.setOnClickListener {
            // 评论逻辑
        }
    }
}
