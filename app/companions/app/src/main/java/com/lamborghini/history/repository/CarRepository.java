class CarRepository(private val carDao: CarDao) {
    fun getCarList(): List<Car> {
        return carDao.getCarList()
    }
}
