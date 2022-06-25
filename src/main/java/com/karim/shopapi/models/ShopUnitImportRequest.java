package com.karim.shopapi.models;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Список товаров и дата обновления, поступающие в теле запроса import.
 */
@Data
public class ShopUnitImportRequest {
    @Valid
    @NotNull
    private List<ShopUnitImport> items;

    @NotNull
    private String updateDate;
}
