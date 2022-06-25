package com.karim.shopapi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;

/**
 * Товар из списка items в теле запроса import.
 */
@Data
public class ShopUnitImport {
    @NotNull
    @Id
    private String id;

    @NotNull
    private String name;

    private String parentId;

    @NotNull
    private ShopUnitType type;

    private Long price;
}
