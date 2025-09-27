class MenuAdapter(private val menuList: List<Menu>) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = menuList[position]
        holder.nameTextView.text = menu.name
        holder.priceTextView.text = "¥${menu.price}" + "
        holder.descriptionTextView.text = menu.description
        holder.imageImageView.setImageResource(R.drawable.menu_image)
    }

    override fun getItemCount(): Int {
        return menuList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
        val priceTextView: TextView = itemView.findViewById(R.id.price_text_view)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description_text_view)
        val imageImageView: ImageView = itemView.findViewById(R.id.image_image_view)
    }
}
