package com.karim.shopapi.services;

import com.karim.shopapi.exceptions.ApiException;
import com.karim.shopapi.models.*;
import com.karim.shopapi.repositories.ShopUnitRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Сервис, переводящий запросы контроллера в запросы к базе данных.
 */
@AllArgsConstructor
@Service
public class ShopUnitService {

    private final ShopUnitRepository shopUnitRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    private static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        return UUID_REGEX_PATTERN.matcher(str).matches();
    }

    /**
     * Получает товар по переданному идентификатору.
     *
     * @param id идентификатор товара.
     * @return Искомый товар.
     */
    public ShopUnit getShopUnitById(String id) {
        if (!isValidUUID(id)) {
            throw new ApiException(String.format("id is not in UUID format, id = %s", id),
                    ErrorType.VALIDATION);
        }
        var shopUnit = shopUnitRepository.findShopUnitById(id)
                .orElseThrow(() -> new ApiException(id));
        convertEmptyChildrenListToNull(shopUnit);
        return shopUnit;
    }

    /**
     * Переводит пустые списки children в null у товаров типа OFFER.
     *
     * @param shopUnit Корневой товар, с которого начинается обработка.
     */
    private void convertEmptyChildrenListToNull(ShopUnit shopUnit) {
        Stack<ShopUnit> shopUnits = new Stack<>();
        shopUnits.push(shopUnit);
        while (!shopUnits.empty()) {
            var current = shopUnits.peek();
            shopUnits.pop();
            if (current.getType() == ShopUnitType.OFFER) {
                current.setChildren(null);
            } else {
                for (var child : current.getChildren()) {
                    shopUnits.push(child);
                }
            }
        }
    }

    static private String generateShopUnitNotExistString(String id) {
        return String.format("Shop unit with input id doesn't exist, input id = %s", id);
    }

    /**
     * Обновляет или добавляет новый товар.
     *
     * @param shopUnitImportRequest Запрос, содержащий новые или обновленные товары.
     */
    public void importShopUnit(ShopUnitImportRequest shopUnitImportRequest) {
        // Получает индексы в порядке top-sort для того, чтобы правильно обработать товары: сначала будут
        // добавляться или обновляться те, у которых нет родителей, а затем - те, у которых есть родители.
        List<Integer> topSortOrderedIndexes = validateImport(shopUnitImportRequest);
        for (int i : topSortOrderedIndexes) {
            var item = shopUnitImportRequest.getItems().get(i);
            if (shopUnitRepository.existsById(item.getId())) {
                // Конвертирует shopUnitImport в shopUnit
                var updatedItem = modelMapper.map(item, ShopUnit.class);
                // Обновляет дату
                updatedItem.setDate(shopUnitImportRequest.getUpdateDate());

                ShopUnit oldShopUnit = shopUnitRepository.findShopUnitById(item.getId())
                        .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(item.getId()),
                                ErrorType.VALIDATION));
                // Восстанавливаем количество детей, их суммарную стоимость и цену категории.
                if (updatedItem.getType() == ShopUnitType.CATEGORY) {
                    updatedItem.setChildrenOffersCnt(oldShopUnit.getChildrenOffersCnt());
                    updatedItem.setChildrenPriceSum(oldShopUnit.getChildrenPriceSum());
                    if (oldShopUnit.getChildrenOffersCnt() > 0) {
                        updatedItem.setPrice((long) ((double) oldShopUnit.getChildrenPriceSum() /
                                oldShopUnit.getChildrenOffersCnt()));
                    }
                    updatedItem.setChildren(oldShopUnit.getChildren());
                }
                if (updatedItem.getParentId() != null) {
                    if (oldShopUnit.getParentId() != null) {
                        var oldParent = shopUnitRepository.findShopUnitById(oldShopUnit.getParentId())
                                .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(
                                        oldShopUnit.getParentId()), ErrorType.VALIDATION));
                        // Проверяет, не изменился ли у обновленного объекта родитель.
                        if (!Objects.equals(updatedItem.getParentId(), oldShopUnit.getParentId())) {
                            var newParent = shopUnitRepository.findShopUnitById(updatedItem.getParentId())
                                    .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(
                                            updatedItem.getParentId()), ErrorType.VALIDATION));
                            // Удаляет shopUnit из детей старого родителя.
                            oldParent.getChildren().remove(oldShopUnit);
                            // Уменьшает суммарную стоимость старого родителя на price shopUnit'a, если это товар, или
                            // на childrenPriceSum если это категория.
                            if (oldShopUnit.getType() == ShopUnitType.CATEGORY) {
                                pushItemPrice(-oldShopUnit.getChildrenPriceSum(), -oldShopUnit.getChildrenOffersCnt(),
                                        updatedItem.getDate(), oldParent, false);
                            } else {
                                pushItemPrice(-oldShopUnit.getPrice(), -1, updatedItem.getDate(),
                                        oldParent, false);
                            }

                            // TODO можно объединить с предыдущим if else блоком
                            newParent.getChildren().add(updatedItem);
                            if (updatedItem.getType() == ShopUnitType.OFFER) {
                                pushItemPrice(updatedItem.getPrice(), 1, updatedItem.getDate(),
                                        newParent, false);
                            } else {
                                pushItemPrice(updatedItem.getChildrenPriceSum(), 0,
                                        updatedItem.getDate(), newParent, false);
                            }
                        } else {
                            oldParent.getChildren().remove(oldShopUnit);
                            oldParent.getChildren().add(updatedItem);
                            if (updatedItem.getType() == ShopUnitType.OFFER) {
                                pushItemPrice(updatedItem.getPrice() - oldShopUnit.getPrice(), 0,
                                        updatedItem.getDate(), oldParent, false);
                            } else {
                                pushItemPrice(updatedItem.getChildrenPriceSum() -
                                                oldShopUnit.getChildrenPriceSum(), -oldShopUnit.getChildrenOffersCnt(),
                                        updatedItem.getDate(), oldParent, false);
                            }
                        }
                        // Сохраняет изменения, произведенные со старым родителем в бд.
                        shopUnitRepository.save(oldParent);
                    } else {
                        var newParent = shopUnitRepository.findShopUnitById(updatedItem.getParentId())
                                .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(
                                        updatedItem.getParentId()), ErrorType.VALIDATION));

                        newParent.getChildren().add(updatedItem);
                        if (updatedItem.getType() == ShopUnitType.OFFER) {
                            pushItemPrice(updatedItem.getPrice(), 1, updatedItem.getDate(),
                                    newParent, false);
                        } else {
                            pushItemPrice(updatedItem.getChildrenPriceSum(), updatedItem.getChildrenOffersCnt(),
                                    updatedItem.getDate(), newParent, false);
                        }
                        shopUnitRepository.save(newParent);
                    }
                } else {
                    if (oldShopUnit.getParentId() != null) {
                        var oldParent = shopUnitRepository.findShopUnitById(oldShopUnit.getParentId())
                                .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(
                                        oldShopUnit.getParentId()), ErrorType.VALIDATION));
                        oldParent.getChildren().remove(oldShopUnit);
                        if (oldShopUnit.getType() == ShopUnitType.OFFER) {
                            pushItemPrice(-oldShopUnit.getPrice(), -1, updatedItem.getDate(),
                                    oldParent, false);
                        } else {
                            pushItemPrice(-oldShopUnit.getChildrenPriceSum(), -oldShopUnit.getChildrenOffersCnt(),
                                    updatedItem.getDate(), oldParent, false);
                        }
                        // Сохраняет изменения, произведенные со старым родителем в бд.
                        shopUnitRepository.save(oldParent);
                    }
                }
                shopUnitRepository.save(updatedItem);
            } else {
                var newItem = modelMapper.map(item, ShopUnit.class);
                newItem.setDate(shopUnitImportRequest.getUpdateDate());
                if (item.getParentId() != null) {
                    ShopUnit parent = shopUnitRepository.findShopUnitById(item.getParentId())
                            .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(item.getParentId()),
                                    ErrorType.VALIDATION));

                    // Проталкивает цену нового shopUnit'a вверх родителю пока родитель не станет null.
                    if (newItem.getType() == ShopUnitType.OFFER) {
                        pushItemPrice(newItem.getPrice(), 1, newItem.getDate(), parent, false);
                    } else {
                        pushItemPrice(newItem.getChildrenPriceSum(), newItem.getChildrenOffersCnt(), newItem.getDate(),
                                parent, false);
                    }
                    parent.getChildren().add(newItem);
                    shopUnitRepository.save(parent);
                }
                shopUnitRepository.insert(newItem);
            }
        }
    }

    /**
     * Проталкивает несогласованность в цене, кол-ве детей типа OFFER и дату обновления наверх к родителю.
     *
     * @param price             несогласованность в цене
     * @param childrenOffersCnt несогласованность в кол-ве детей типа OFFER
     * @param updateTime        дата обновления
     * @param parent            родитель
     * @param delete            true, если просходит удаление товара, false - иначе.
     */
    protected void pushItemPrice(long price, long childrenOffersCnt, String updateTime, ShopUnit parent,
                                 boolean delete) {
        parent.setChildrenPriceSum(parent.getChildrenPriceSum() + price);
        if (!delete) {
            parent.setDate(updateTime);
        }
        parent.setChildrenOffersCnt(parent.getChildrenOffersCnt() + childrenOffersCnt);
        if (parent.getChildrenOffersCnt() > 0) {
            parent.setPrice((long) ((double) parent.getChildrenPriceSum() / parent.getChildrenOffersCnt()));
        } else {
            parent.setPrice(null);
        }
        var current = parent;
        while (current.getParentId() != null) {
            var child = current;
            var currentParentId = current.getParentId();
            current = shopUnitRepository.findShopUnitById(current.getParentId())
                    .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(currentParentId),
                            ErrorType.VALIDATION));
            current.setChildrenPriceSum(current.getChildrenPriceSum() + price);
            current.setChildrenOffersCnt(current.getChildrenOffersCnt() + childrenOffersCnt);
            if (!delete) {
                current.setDate(updateTime);
            }
            if (current.getChildrenOffersCnt() > 0) {
                current.setPrice((long) ((double) current.getChildrenPriceSum() / current.getChildrenOffersCnt()));
            } else {
                current.setPrice(null);
            }
            shopUnitRepository.save(current);
        }
    }

    /**
     * Сортирует вершины графа g в порядке top-sort.
     *
     * @param node         текущая вершина
     * @param g            граф
     * @param used         массив индикаторов использованных вершин.
     * @param topSortOrder список вершин в порядке top-sort.
     */
    private static void topSort(int node, List<List<Integer>> g, boolean[] used, List<Integer> topSortOrder) {
        if (!used[node]) {
            used[node] = true;
            for (int u : g.get(node)) {
                if (used[u]) {
                    continue;
                }
                topSort(u, g, used, topSortOrder);
            }
            topSortOrder.add(node);
        }
    }

    /**
     * Валидирует тело поступившего import запроса и возвращает список индексов элементов
     * из items в порядке top sort.
     *
     * @param shopUnitImportRequest Тело поступившего import запроса
     * @return список индексов элементов из items в порядке top sort
     */
    private List<Integer> validateImport(ShopUnitImportRequest shopUnitImportRequest) {
        Map<String, ShopUnitType> idToShopUnitType;
        idToShopUnitType = getIdToShopUnitTypeMap(shopUnitImportRequest);
        validateDateIsInISO(shopUnitImportRequest.getUpdateDate());

        Map<String, Integer> fromIdToIndex = new HashMap<>();
        for (int i = 0; i < shopUnitImportRequest.getItems().size(); ++i) {
            fromIdToIndex.put(shopUnitImportRequest.getItems().get(i).getId(), i);
        }
        List<List<Integer>> fromVToVertices = new ArrayList<>();
        for (int i = 0; i < idToShopUnitType.size(); ++i) {
            fromVToVertices.add(new ArrayList<>());
        }

        for (var item : shopUnitImportRequest.getItems()) {
            // Проверяет, что индекс является UUID.
            if (!isValidUUID(item.getId()) || (item.getParentId() != null && !isValidUUID(item.getParentId()))) {
                throw new ApiException(String.format("id of item is not in UUID format, id = %s", item.getId()),
                        ErrorType.VALIDATION);
            }

            // у категорий поле price должно содержать null
            if (item.getType() == ShopUnitType.CATEGORY && item.getPrice() != null) {
                throw new ApiException(String.format("price of category is not null, id = %s", item.getId()),
                        ErrorType.VALIDATION);
            }

            // цена товара не может быть null и должна быть больше либо равна нулю.
            if (item.getType() == ShopUnitType.OFFER && (item.getPrice() == null || item.getPrice() < 0)) {
                throw new ApiException(String.format("price of OFFER should be not null and >= 0 integer, id = %s",
                        item.getId()), ErrorType.VALIDATION);
            }

            if (shopUnitRepository.existsById(item.getId())) {
                var updatedItem = modelMapper.map(item, ShopUnit.class);
                ShopUnit oldShopUnit = shopUnitRepository.findShopUnitById(item.getId())
                        .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(item.getId()),
                                ErrorType.VALIDATION));
                // Изменение типа элемента с товара на категорию или с категории на товар не допускается
                if (updatedItem.getType() != oldShopUnit.getType()) {
                    throw new ApiException(String.format("Changing the type of shop unit is forbidden, id = %s",
                            oldShopUnit.getId()), ErrorType.VALIDATION);
                }

                if (Objects.equals(updatedItem.getParentId(), oldShopUnit.getParentId())) {
                    continue;
                }
                if (updatedItem.getParentId() != null) {
                    if (idToShopUnitType.containsKey(updatedItem.getParentId())) {
                        // родителем товара или категории может быть только категория
                        if (idToShopUnitType.get(updatedItem.getParentId()) != ShopUnitType.CATEGORY) {
                            throw new ApiException(String.format("Only the category can be a parent, new " +
                                    "parent id = %s", updatedItem.getParentId()), ErrorType.VALIDATION);
                        }
                        fromVToVertices.get(fromIdToIndex.get(updatedItem.getParentId()))
                                .add(fromIdToIndex.get(updatedItem.getId()));
                    } else {
                        var newParent = shopUnitRepository.findShopUnitById(updatedItem.getParentId())
                                .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(
                                        updatedItem.getParentId()), ErrorType.VALIDATION));

                        // родителем товара или категории может быть только категория
                        if (newParent.getType() != ShopUnitType.CATEGORY) {
                            throw new ApiException(String.format("Only the category can be a parent, new parent" +
                                    " id = %s", newParent.getId()), ErrorType.VALIDATION);
                        }
                    }
                }
            } else {
                if (item.getParentId() != null) {
                    if (idToShopUnitType.containsKey(item.getParentId())) {
                        if (idToShopUnitType.get(item.getParentId()) != ShopUnitType.CATEGORY) {
                            throw new ApiException(String.format("Only the category can be a parent, new parent" +
                                    " id = %s", item.getParentId()), ErrorType.VALIDATION);
                        }
                        fromVToVertices.get(fromIdToIndex.get(item.getParentId()))
                                .add(fromIdToIndex.get(item.getId()));
                    } else {
                        ShopUnit parent = shopUnitRepository.findShopUnitById(item.getParentId())
                                .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(item.getParentId()),
                                        ErrorType.VALIDATION));

                        // родителем товара или категории может быть только категория
                        if (parent.getType() != ShopUnitType.CATEGORY) {
                            throw new ApiException(String.format("Only the category can be a parent, new parent" +
                                    " id = %s", parent.getId()), ErrorType.VALIDATION);
                        }
                    }
                }
            }
        }

        boolean[] used = new boolean[idToShopUnitType.size()];
        List<Integer> topSortOrder = new ArrayList<>();
        for (int i = 0; i < idToShopUnitType.size(); ++i) {
            topSort(i, fromVToVertices, used, topSortOrder);
        }
        Collections.reverse(topSortOrder);
        return topSortOrder;
    }

    /**
     * Генерирует словарь (id, ShopUnit) по списку ShopUnit'ов.
     *
     * @param shopUnitImportRequest тело запроса import со списком ShopUnit'ов
     * @return словарь (id, ShopUnit)
     * @throws ApiException исключение, если невозможно сгенерировать словарь.
     */
    private static Map<String, ShopUnitType> getIdToShopUnitTypeMap(ShopUnitImportRequest shopUnitImportRequest)
            throws ApiException {
        Map<String, ShopUnitType> idToShopUnitType;
        try {
            idToShopUnitType = shopUnitImportRequest.getItems().stream()
                    .collect(Collectors.toMap(ShopUnitImport::getId, ShopUnitImport::getType));
        } catch (IllegalStateException ex) {
            // в одном запросе не может быть двух элементов с одинаковым id
            throw new ApiException("2 shop units with equal id",
                    ErrorType.VALIDATION);
        }
        return idToShopUnitType;
    }

    /**
     * Проверяет, что дата в формате ISO 8601.
     *
     * @param updateDate дата
     * @throws ApiException исключение, если дата не в формате ISO 8601.
     */
    private static void validateDateIsInISO(String updateDate) throws ApiException {
        if (updateDate != null) {
            try {
                LocalDateTime.parse(updateDate, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException ex) {
                throw new ApiException(String.format("date is not in ISO 8601 format: %s", updateDate),
                        ErrorType.VALIDATION);
            }
        } else {
            throw new ApiException(String.format("date is not in ISO 8601 format: %s", updateDate),
                    ErrorType.VALIDATION);
        }
    }

    /**
     * Удаляет товар по идентификатору.
     *
     * @param id идентификатор.
     */
    public void deleteShopUnitById(String id) {
        if (!isValidUUID(id)) {
            throw new ApiException(String.format("id is not in UUID format, id = %s", id),
                    ErrorType.VALIDATION);
        }
        var shopUnitToDelete = shopUnitRepository.findShopUnitById(id)
                .orElseThrow(() -> new ApiException(id));
        if (shopUnitToDelete.getParentId() != null) {
            var parent = shopUnitRepository.findShopUnitById(shopUnitToDelete.getParentId())
                    .orElseThrow(() -> new ApiException(generateShopUnitNotExistString(shopUnitToDelete.getParentId()),
                            ErrorType.VALIDATION));
            if (shopUnitToDelete.getType() == ShopUnitType.OFFER) {
                pushItemPrice(-shopUnitToDelete.getPrice(), -1, null, parent, true);
            } else {
                pushItemPrice(-shopUnitToDelete.getChildrenPriceSum(), -shopUnitToDelete.getChildrenOffersCnt(),
                        null, parent, true);
            }
            parent.getChildren().remove(shopUnitToDelete);
            shopUnitRepository.save(parent);
        }

        // Проходит по всем детям с помощью dfs и удаляет их из бд.
        if (shopUnitToDelete.getType() == ShopUnitType.CATEGORY) {
            deleteAllChildren(shopUnitToDelete);
        } else {
            shopUnitRepository.delete(shopUnitToDelete);
        }
    }

    /**
     * Удаляет категорию и всех детей категории проходом dfs.
     *
     * @param shopUnitToDelete категория, у которой надо удалить всех детей.
     */
    private void deleteAllChildren(ShopUnit shopUnitToDelete) {
        Stack<ShopUnit> shopUnitsToDelete = new Stack<ShopUnit>();
        shopUnitsToDelete.push(shopUnitToDelete);
        while (!shopUnitsToDelete.empty()) {
            var current = shopUnitsToDelete.peek();
            shopUnitsToDelete.pop();
            shopUnitRepository.delete(current);
            if (current.getType() == ShopUnitType.CATEGORY) {
                if (current.getChildren() != null) {
                    for (var child : current.getChildren()) {
                        shopUnitsToDelete.push(child);
                    }
                }
            }
        }
    }

    /**
     * Получает список товаров, обновленных или добавленных за последние 24 часа перед заданной датой.
     *
     * @param dateTime заданная дата.
     * @return искомый список товаров
     */
    public Sales getSales(String dateTime) {
        validateDateIsInISO(dateTime);
        var toDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME).plusSeconds(1);
        var fromDateTime = toDateTime.minusDays(1).minusSeconds(1);
        Sales sales = new Sales();
        shopUnitRepository.findAllByDateBetween(fromDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
                        toDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .ifPresent((items) -> sales.setItems(items));
        return sales;
    }
}
