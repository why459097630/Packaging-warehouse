class CarListFragment : Fragment() {
    private lateinit var carAdapter: CarAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_car_list, container, false)
        carAdapter = CarAdapter(carRepository.getCarList())
        view.findViewById<RecyclerView>(R.id.car_list).adapter = carAdapter
        return view
    }
}
