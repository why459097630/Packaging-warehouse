class MenuItemDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuItemDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menuItem = intent.getParcelableExtra<MenuItem>("menuItem")

        binding.nameTv.text = menuItem.name
        binding.descriptionTv.text = menuItem.description
        binding.priceTv.text = menuItem.price.toString()

        binding.likeBtn.setOnClickListener {
            // 点赞逻辑
        }
    }
}
