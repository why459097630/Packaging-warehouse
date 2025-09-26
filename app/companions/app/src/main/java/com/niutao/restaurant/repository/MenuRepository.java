// MenuRepository.kt
interface MenuRepository {
    suspend fun getMenu(): List<Menu>
    suspend fun saveMenu(menu: Menu)
}
