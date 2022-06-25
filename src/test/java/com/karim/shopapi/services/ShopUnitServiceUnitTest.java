package com.karim.shopapi.services;

import com.karim.shopapi.exceptions.ApiException;
import com.karim.shopapi.models.*;
import com.karim.shopapi.repositories.ShopUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты сервиса, проверяющие, что сервис выдает ошибки при некорректных входных и возвращает
 * нужные ответы при корректных входных данных.
 */
@ExtendWith(MockitoExtension.class)
class ShopUnitServiceUnitTest {
    @Mock
    ShopUnitRepository shopUnitRepository;

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitWithIdNotInUUIDFormat() {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(LocalDateTime.now().toString());
        var itemWithIncorrectId = new ShopUnitImport();
        var invalidId = "1111";
        itemWithIncorrectId.setId(invalidId);
        itemWithIncorrectId.setName("name");
        itemWithIncorrectId.setType(ShopUnitType.OFFER);
        itemWithIncorrectId.setPrice(100L);
        request.getItems().add(itemWithIncorrectId);

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("id of item is not in UUID format, id = %s", invalidId);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsRequestWithUpdateDateNotISO() {
        var requestWithDateNotInISO = new ShopUnitImportRequest();
        requestWithDateNotInISO.setItems(new ArrayList<ShopUnitImport>());
        var dateNotInISO = "10:36:08 18.06.2022";
        requestWithDateNotInISO.setUpdateDate(dateNotInISO);
        var item = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        item.setId(id);
        item.setName("name");
        item.setType(ShopUnitType.OFFER);
        item.setPrice(100L);
        requestWithDateNotInISO.getItems().add(item);

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(requestWithDateNotInISO));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("date is not in ISO 8601 format: %s", dateNotInISO);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsCategoryWithNotNullPrice() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var item = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.CATEGORY;
        var price = 100L;
        String parentId = null;

        item.setId(id);
        item.setName(name);
        item.setType(type);
        item.setPrice(price);
        item.setParentId(parentId);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(item);

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("price of category is not null, id = %s", id);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsOfferWithNegativePrice() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var item = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = -1L;
        String parentId = null;

        item.setId(id);
        item.setName(name);
        item.setType(type);
        item.setPrice(price);
        item.setParentId(parentId);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(item);

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("price of OFFER should be not null and >= 0 integer, id = %s", id);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsParentNotCategoryAndItsChildThatExisted() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var child = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = 1L;

        var parent = new ShopUnitImport();
        var parentItemId = "3fa85f64-5717-4562-b3fc-2c963f66a111";
        var parentName = "name";
        var parentType = ShopUnitType.OFFER;
        var parentPrice = 1L;
        String parentParentId = null;
        var childParentId = parentItemId;

        child.setId(id);
        child.setName(name);
        child.setType(type);
        child.setPrice(price);
        child.setParentId(childParentId);
        parent.setId(parentItemId);
        parent.setName(parentName);
        parent.setType(parentType);
        parent.setPrice(parentPrice);

        var existingItem = new ShopUnit();
        existingItem.setId(id);
        existingItem.setType(ShopUnitType.OFFER);
        existingItem.setParentId("parent id");

        when(shopUnitRepository.findShopUnitById(id)).thenReturn(java.util.Optional.of(existingItem));
        when(shopUnitRepository.existsById(id)).thenReturn(true);
        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        assertAll(
                () -> {
                    request.getItems().add(child);
                    request.getItems().add(parent);
                    ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
                    var expectedType = ErrorType.VALIDATION;
                    var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                            parentItemId);
                    assertAll(
                            () -> assertEquals(expectedType, ex.getErrorType()),
                            () -> assertEquals(expectedMessage, ex.getMessage())
                    );
                },
                () -> {
                    request.getItems().clear();
                    request.getItems().add(parent);
                    request.getItems().add(child);
                    ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
                    var expectedType = ErrorType.VALIDATION;
                    var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                            parentItemId);
                    assertAll(
                            () -> assertEquals(expectedType, ex.getErrorType()),
                            () -> assertEquals(expectedMessage, ex.getMessage())
                    );
                }
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsParentNotCategoryAndItsChildThatNotExisted() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var child = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = 1L;

        var parent = new ShopUnitImport();
        var parentItemId = "3fa85f64-5717-4562-b3fc-2c963f66a111";
        var parentName = "name";
        var parentType = ShopUnitType.OFFER;
        var parentPrice = 1L;
        String parentParentId = null;
        var childParentId = parentItemId;

        child.setId(id);
        child.setName(name);
        child.setType(type);
        child.setPrice(price);
        child.setParentId(childParentId);
        parent.setId(parentItemId);
        parent.setName(parentName);
        parent.setType(parentType);
        parent.setPrice(parentPrice);

        when(shopUnitRepository.existsById(id)).thenReturn(false);
        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        assertAll(
                () -> {
                    request.getItems().add(child);
                    request.getItems().add(parent);
                    ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
                    var expectedType = ErrorType.VALIDATION;
                    var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                            parentItemId);
                    assertAll(
                            () -> assertEquals(expectedType, ex.getErrorType()),
                            () -> assertEquals(expectedMessage, ex.getMessage())
                    );
                },
                () -> {
                    request.getItems().clear();
                    request.getItems().add(parent);
                    request.getItems().add(child);
                    ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
                    var expectedType = ErrorType.VALIDATION;
                    var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                            parentItemId);
                    assertAll(
                            () -> assertEquals(expectedType, ex.getErrorType()),
                            () -> assertEquals(expectedMessage, ex.getMessage())
                    );
                }
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenAddsChildThatNotExistedToParentNotCategory() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var child = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = 1L;

        var parent = new ShopUnit();
        var parentItemId = "3fa85f64-5717-4562-b3fc-2c963f66a111";
        var parentName = "name";
        var parentType = ShopUnitType.OFFER;
        var parentPrice = 1L;
        var childParentId = parentItemId;

        child.setId(id);
        child.setName(name);
        child.setType(type);
        child.setPrice(price);
        child.setParentId(childParentId);
        parent.setId(parentItemId);
        parent.setName(parentName);
        parent.setType(parentType);
        parent.setPrice(parentPrice);

        when(shopUnitRepository.existsById(id)).thenReturn(false);
        when(shopUnitRepository.findShopUnitById(parentItemId)).thenReturn(java.util.Optional.of(parent));
        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(child);
        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                parentItemId);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenAddsChildToOffer() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var child = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = 1L;

        var parent = new ShopUnit();
        var parentItemId = "3fa85f64-5717-4562-b3fc-2c963f66a111";
        var parentName = "name";
        var parentType = ShopUnitType.OFFER;
        var parentPrice = 1L;
        var childParentId = parentItemId;

        child.setId(id);
        child.setName(name);
        child.setType(type);
        child.setPrice(price);
        child.setParentId(childParentId);
        parent.setId(parentItemId);
        parent.setName(parentName);
        parent.setType(parentType);
        parent.setPrice(parentPrice);

        var existingItem = new ShopUnit();
        existingItem.setId(id);
        existingItem.setType(ShopUnitType.OFFER);
        existingItem.setParentId("parent id");

        when(shopUnitRepository.findShopUnitById(id)).thenReturn(java.util.Optional.of(existingItem));
        when(shopUnitRepository.findShopUnitById(parentItemId)).thenReturn(java.util.Optional.of(parent));
        when(shopUnitRepository.existsById(id)).thenReturn(true);
        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(child);
        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("Only the category can be a parent, new parent id = %s",
                parentItemId);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsItemWithExistingIdButAnotherType() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var item = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        var name = "name";
        var type = ShopUnitType.OFFER;
        var price = 1L;
        String parentId = null;

        item.setId(id);
        item.setName(name);
        item.setType(type);
        item.setPrice(price);
        item.setParentId(parentId);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(item);

        var existingItem = new ShopUnit();
        existingItem.setId(id);
        existingItem.setType(ShopUnitType.CATEGORY);

        when(shopUnitRepository.findShopUnitById(id)).thenReturn(java.util.Optional.of(existingItem));
        when(shopUnitRepository.existsById(id)).thenReturn(true);
        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("Changing the type of shop unit is forbidden, id = %s", id);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsItemsWithEqualId() {
        var request = new ShopUnitImportRequest();
        var date = LocalDateTime.now().toString();
        var firstItem = new ShopUnitImport();
        var id = "3fa85f64-5717-4562-b3fc-2c963f66a444";
        firstItem.setId(id);
        firstItem.setType(ShopUnitType.OFFER);

        var secondItem = new ShopUnitImport();
        secondItem.setId(id);
        secondItem.setType(ShopUnitType.CATEGORY);

        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(date);
        request.getItems().add(firstItem);
        request.getItems().add(secondItem);

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.importShopUnit(request));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = "2 shop units with equal id";
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void getShopUnitByIdThrowsValidationExceptionWhenNotUUIDIdGiven() {
        String notUUID = "not in UUID";

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.getShopUnitById(notUUID));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("id is not in UUID format, id = %s", notUUID);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void pushItemPriceSetsCorrectPriceWhenOfferAddedToRoot() {
        var root = new ShopUnit();
        root.setName("Root");
        root.setType(ShopUnitType.CATEGORY);
        root.setChildrenOffersCnt(2);
        root.setChildrenPriceSum(200);

        var newOffer = new ShopUnit();
        newOffer.setType(ShopUnitType.OFFER);
        newOffer.setPrice(100L);
        var newTime = "newTime";

        ShopUnitService service = new ShopUnitService(shopUnitRepository);
        service.pushItemPrice(newOffer.getPrice(), 1, newTime, root, false);

        var expectedPrice = 100L;
        assertAll(
                () -> assertEquals(expectedPrice, root.getPrice()),
                () -> assertEquals(newTime, root.getDate())
        );
    }

    @Test
    void pushItemPriceSetsCorrectPriceWhenOfferAddedToRootsChild() {
        var root = new ShopUnit();
        var rootId = "root id";
        root.setId(rootId);
        root.setName("Root");
        root.setType(ShopUnitType.CATEGORY);
        root.setChildrenOffersCnt(2);
        root.setChildrenPriceSum(200);

        var parent = new ShopUnit();
        parent.setType(ShopUnitType.CATEGORY);
        parent.setParentId(rootId);

        var newOffer = new ShopUnit();
        var newOfferParentId = "parent id";
        newOffer.setType(ShopUnitType.OFFER);
        newOffer.setPrice(100L);
        newOffer.setParentId(newOfferParentId);
        var newTime = "newTime";

        when(shopUnitRepository.findShopUnitById(parent.getParentId())).thenReturn(Optional.of(root));
        ShopUnitService service = new ShopUnitService(shopUnitRepository);
        service.pushItemPrice(newOffer.getPrice(), 1, newTime, parent, false);

        var expectedPrice = 100L;
        assertAll(
                () -> assertEquals(expectedPrice, root.getPrice()),
                () -> assertEquals(newTime, root.getDate())
        );
    }

    @Test
    void pushDeletedItemPriceSetsCorrectPriceWhenOfferAddedToRootsChild() {
        var root = new ShopUnit();
        var rootId = "root id";
        var rootTime = "rootTime";
        root.setId(rootId);
        root.setName("Root");
        root.setDate(rootTime);
        root.setType(ShopUnitType.CATEGORY);
        root.setChildrenOffersCnt(2);
        root.setChildrenPriceSum(200);

        var parent = new ShopUnit();
        parent.setType(ShopUnitType.CATEGORY);
        parent.setParentId(rootId);

        var newOffer = new ShopUnit();
        var newOfferParentId = "parent id";
        newOffer.setType(ShopUnitType.OFFER);
        newOffer.setPrice(100L);
        newOffer.setParentId(newOfferParentId);
        var newTime = "newTime";

        when(shopUnitRepository.findShopUnitById(parent.getParentId())).thenReturn(Optional.of(root));
        ShopUnitService service = new ShopUnitService(shopUnitRepository);
        service.pushItemPrice(-newOffer.getPrice(), -1, newTime, parent, true);

        var expectedPrice = 100L;
        assertAll(
                () -> assertEquals(expectedPrice, root.getPrice()),
                () -> assertEquals(rootTime, root.getDate())
        );
    }

    @Test
    void deleteShopUnitByIdThrowsValidationExceptionWhenIdNotInUUID() {
        var notUUID = "not in uuid";

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteShopUnitById(notUUID));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("id is not in UUID format, id = %s", notUUID);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }

    @Test
    void getSalesThrowsNotValidExceptionWhenGetsInvalidDateParameter() {
        var invalidDate = "invalid";

        ShopUnitService service = new ShopUnitService(shopUnitRepository);

        ApiException ex = assertThrows(ApiException.class, () -> service.getSales(invalidDate));
        var expectedType = ErrorType.VALIDATION;
        var expectedMessage = String.format("date is not in ISO 8601 format: %s", invalidDate);
        assertAll(
                () -> assertEquals(expectedType, ex.getErrorType()),
                () -> assertEquals(expectedMessage, ex.getMessage())
        );
    }
}