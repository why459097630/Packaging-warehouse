class CommentAdapter(private val commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentList[position]
        holder.nameTextView.text = comment.name
        holder.contentTextView.text = comment.content
    }
    override fun getItemCount(): Int {
        return commentList.size
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val contentTextView: TextView = itemView.findViewById(R.id.content)
    }
}
