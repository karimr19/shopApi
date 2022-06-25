package com.karim.shopapi.controllers;

import com.karim.shopapi.models.Error;
import com.karim.shopapi.models.Sales;
import com.karim.shopapi.models.ShopUnit;
import com.karim.shopapi.models.ShopUnitImportRequest;
import com.karim.shopapi.services.ShopUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST контроллер товаров.
 */
@RestController
@AllArgsConstructor
public class ShopUnitController {
    private final ShopUnitService shopUnitService;

    /**
     * Получает информацию об элементе по идентификатору.
     *
     * @param id Идентификатор в формате UUID.
     * @return Искомый элемент.
     */
    @Operation(summary = "Получить информацию об элементе по id", description = "Получить информацию об элементе по " +
            "идентификатору. При получении информации о категории также предоставляется информация о её дочерних " +
            "элементах.",
            tags = "Get")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация об элементе",
            content = {@Content(mediaType = "application/json",
            examples = {
                    @ExampleObject(name = "Example",
                    value = """
                            {
                            "id": "d515e43f-f3f6-4471-bb77-6b455017a2d2",
                            "name": "Смартфоны",
                            "date": "2022-02-02T02:00:00.000Z",
                            "type": "CATEGORY",
                            "parentId": null,
                            "price": 800,
                            "children": [
                                    {
                                    "id": "d515e43f-f3f6-4471-bb77-6b455017a2d3",
                                    "name": "Apple",
                                    "date": "2022-02-02T02:00:00.000Z",
                                    "type": "CATEGORY",
                                    "parentId": "d515e43f-f3f6-4471-bb77-6b455017a2d2",
                                    "price": 800,
                                    "children": [
                                    {
                                    "id": "d515e43f-f3f6-4471-bb77-6b455017a2d5",
                                    "name": "IPhone 13",
                                    "date": "2022-02-02T02:00:00.000Z",
                                    "type": "OFFER",
                                    "parentId": "d515e43f-f3f6-4471-bb77-6b455017a2d3",
                                    "price": 800,
                                    "children": null
                                    }]
                                    }
                                ]
                            }
                            """)
            })}),
            @ApiResponse(responseCode = "400", description = "Невалидная схема документа или входные данные не верны.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                            value = """
                                    {
                                       "code": 400,
                                       "message": "Validation Failed"
                                     }
                                    """)
                            })}),
            @ApiResponse(responseCode = "404", description = "Категория/товар не найден.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                                    value = """
                                            {
                                            "code": 404,
                                            "message": "Item not found"
                                            }
                                            """)
                            })})
    })
    @GetMapping("/nodes/{id}")
    @ResponseBody
    public ShopUnit getShopUnitById(@PathVariable String id) {
        return shopUnitService.getShopUnitById(id);
    }

    /**
     * Получает список товаров, цена которых была обновлена за последние 24 часа
     * включительно [now() - 24h, now()] от времени переданном в запросе.
     *
     * @param date Время в формате ISO 8601.
     * @return Список искомых товаров.
     */

    @GetMapping("/sales")
    @Operation(summary = "Получить список обновленных за последние 24 часа элементов.",
            description = "Получение списка товаров, цена которых была обновлена за последние 24 часа " +
                    "включительно [now() - 24h, now()] от времени переданном в запросе. ",
            tags = "Get")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список товаров, цена которых была обновлена.",
                    content = {@Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Example",
                                            value = """
                                                    {
                                                      "items": [
                                                        {
                                                          "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                                                          "name": "Оффер",
                                                          "date": "2022-05-28T21:12:01.000Z",
                                                          "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                                                          "price": 234,
                                                          "type": "OFFER"
                                                        }
                                                      ]
                                                    }                                                   
                                                    """)
                            })}),
            @ApiResponse(responseCode = "400", description = "Невалидная схема документа или входные данные не верны.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                                    value = """
                                    {
                                       "code": 400,
                                       "message": "Validation Failed"
                                     }
                                    """)
                            })})
    })
    public Sales getSales(@RequestParam String date) {
        return shopUnitService.getSales(date);
    }

    /**
     * Импортирует новые товары и/или категории. Товары/категории импортированные повторно обновляют текущие.
     *
     * @param shopUnitImportRequest Новые товары и категории.
     */
    @Operation(summary = "Импортировать новые товары и/или категории.", description = "Импортирует новые товары " +
            "и/или категории. Товары/категории импортированные повторно обновляют текущие.",
            tags = "Post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Вставка или обновление прошли успешно."),
            @ApiResponse(responseCode = "400", description = "Невалидная схема документа или входные данные не верны.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                                    value = """
                                    {
                                       "code": 400,
                                       "message": "Validation Failed"
                                     }
                                    """)
                            })})
    })
    @PostMapping("/imports")
    public void importShopUnit(@Valid @RequestBody(content = @Content(examples = {
            @ExampleObject(name = "request", value = """
                    {
                      "items": [
                        {
                          "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                          "name": "Оффер",
                          "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                          "price": 234,
                          "type": "OFFER"
                        }
                      ],
                      "updateDate": "2022-05-28T21:12:01.000Z"
                    }
                    """)})) @org.springframework.web.bind.annotation.RequestBody
                                           ShopUnitImportRequest shopUnitImportRequest) {
        shopUnitService.importShopUnit(shopUnitImportRequest);
    }

    /**
     * Удаляет элемент по идентификатору.
     *
     * @param id Идентификатор в формате UUID.
     */
    @Operation(summary = "Удалить элемент по id", description = "Удалить элемент по идентификатору. " +
            "При удалении категории удаляются все дочерние элементы.",
            tags = "Delete")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Удаление прошло успешно"),
            @ApiResponse(responseCode = "400", description = "Невалидная схема документа или входные данные не верны.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                                    value = """
                                    {
                                       "code": 400,
                                       "message": "Validation Failed"
                                     }
                                    """)
                            })}),
            @ApiResponse(responseCode = "404", description = "Категория/товар не найден.",
                    content = {@Content(mediaType = "application/json",
                            examples = {@ExampleObject(name = "response",
                                    value = """
                                            {
                                            "code": 404,
                                            "message": "Item not found"
                                            }
                                            """)
                            })})
    })
    @DeleteMapping("/delete/{id}")
    public void deleteShopUnitById(@PathVariable String id) {
        shopUnitService.deleteShopUnitById(id);
    }
}
