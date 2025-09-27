class MenuFragment : Fragment() {
    private lateinit var menuRepository: MenuRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        menuRepository = MenuRepositoryImpl()
        val menuList = menuRepository.getMenu()
        val adapter = MenuAdapter(menuList)
        recyclerView.adapter = adapter
        return view
    }
}
