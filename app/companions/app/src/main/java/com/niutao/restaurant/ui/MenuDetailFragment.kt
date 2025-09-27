class MenuDetailFragment : Fragment() {
    private lateinit var menuRepository: MenuRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_menu_detail, container, false)
        menuRepository = MenuRepositoryImpl()
        val menu = menuRepository.getMenuById(arguments?.getInt("menuId")!!)
        nameTextView.text = menu.name
        priceTextView.text = "¥${menu.price}" + "
        descriptionTextView.text = menu.description
        imageImageView.setImageResource(R.drawable.menu_image)
        return view
    }
}
