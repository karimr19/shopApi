package com.karim.shopapi.repositories;

import com.karim.shopapi.models.ShopUnit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для общения с бд.
 */
public interface ShopUnitRepository extends MongoRepository<ShopUnit, String> {
    /**
     * Получает товар по идентификатору.
     *
     * @param id идентификатор в формате uuid.
     * @return Искомый товар.
     */
    Optional<ShopUnit> findShopUnitById(String id);

    /**
     * Получает список товаров, обновленных или добавленных в заданном промежутке.
     *
     * @param from Начало промежутка(не включительно).
     * @param to   Конец промежутка(не включительно).
     * @return Список искомых товаров.
     */
    Optional<List<ShopUnit>> findAllByDateBetween(String from, String to);
}
