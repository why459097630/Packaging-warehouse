class UploadMenuActivity : AppCompatActivity() {
    private lateinit var menuRepository: MenuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_menu)

        menuRepository = MenuRepositoryImpl()
        uploadMenu()
    }

    private fun uploadMenu() {
        val menu = Menu(1, "牛太郎餐厅", 10.99, "美味的介绍", "image.jpg")
        menuRepository.uploadMenu(menu)
    }
}
