interface MenuRepository {
    fun getMenu(): List<Menu>
    fun saveMenu(menu: Menu)
}
