class MenuDetailActivity : AppCompatActivity() {
    private lateinit var menuRepository: MenuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_detail)

        menuRepository = MenuRepositoryImpl()
        getMenu()
    }

    private fun getMenu() {
        val menu = menuRepository.getMenu()
        val menuId = intent.getStringExtra("menuId")
        val menuDetail = menu.find { it.id == menuId }
        if (menuDetail != null) {
            val description = menuDetail.description
            val price = menuDetail.price
            val image = menuDetail.image
            // 展示菜品详细信息
        }
    }
}
