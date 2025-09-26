class AddMenuItemActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMenuItemBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMenuItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.uploadBtn.setOnClickListener {
            val image = binding.imageview.image
            val imageUri = uploadImage(image)
            // 上传图片逻辑
        }

        binding.saveBtn.setOnClickListener {
            val name = binding.nameEt.text.toString()
            val description = binding.descriptionEt.text.toString()
            val price = binding.priceEt.text.toString().toDouble()
            val image = binding.imageview.image
            val likes = 0

            val menuItem = MenuItem(name, description, price, imageUri, likes)
            // 保存菜品逻辑
        }
    }
}
