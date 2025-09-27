class MenuAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
    val priceTextView: TextView = itemView.findViewById(R.id.price_text_view)
    val descriptionTextView: TextView = itemView.findViewById(R.id.description_text_view)
    val imageImageView: ImageView = itemView.findViewById(R.id.image_image_view)
}
