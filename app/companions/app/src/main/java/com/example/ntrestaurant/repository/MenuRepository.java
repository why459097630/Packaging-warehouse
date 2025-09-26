interface MenuRepository {
    suspend fun getMenu(): List<Menu>
    suspend fun uploadMenu(menu: Menu)
}
