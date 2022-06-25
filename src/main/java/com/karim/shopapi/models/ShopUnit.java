package com.karim.shopapi.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Единица товара, может быть товаром или категорией.
 */
@Data
@Document
@Schema
public class ShopUnit {
    @Id
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String date;

    private String parentId;

    @NotNull
    private ShopUnitType type;

    private Long price;

    // Суммарная стоимость детей товаров.
    @JsonIgnore
    private long childrenPriceSum;

    // Кол-во детей товаров в категории.
    @JsonIgnore
    private long childrenOffersCnt;

    @Reference
    private List<ShopUnit> children = new ArrayList<>();
}
