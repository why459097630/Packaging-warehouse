class MainActivity : AppCompatActivity() {
    private lateinit var menuRepository: MenuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menuRepository = MenuRepositoryImpl()
        val menuList = menuRepository.getMenu()
        val adapter = MenuAdapter(menuList)
        recyclerView.adapter = adapter
    }
}
