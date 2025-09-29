class CarAdapter(private val carList: List<Car>) : RecyclerView.Adapter<CarAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_car, parent, false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = carList[position]
        holder.nameTextView.text = car.name
        holder.yearTextView.text = car.year.toString()
    }
    override fun getItemCount(): Int {
        return carList.size
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val yearTextView: TextView = itemView.findViewById(R.id.year)
    }
}
