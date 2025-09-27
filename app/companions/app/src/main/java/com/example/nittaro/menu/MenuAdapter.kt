import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nittaro.menu.R

class MenuAdapter(private val menus: List<Menu>) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = menus[position]
        holder.menuName.text = menu.name
        holder.menuPrice.text = menu.price
        holder.menuDescription.text = menu.description
        holder.menuImage.setImageResource(menu.image)
    }

    override fun getItemCount(): Int {
        return menus.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuName: TextView = itemView.findViewById(R.id.menu_name)
        val menuPrice: TextView = itemView.findViewById(R.id.menu_price)
        val menuDescription: TextView = itemView.findViewById(R.id.menu_description)
        val menuImage: ImageView = itemView.findViewById(R.id.menu_image)
    }
}
