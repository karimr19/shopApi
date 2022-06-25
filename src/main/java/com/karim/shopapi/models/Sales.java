package com.karim.shopapi.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Список товаров, возвращаемых пользователю, при запросе товаров,
 * добавленных или обновленных за последние 24 часа.
 */
@Data
public class Sales {
    private List<ShopUnit> items = new ArrayList<ShopUnit>();
}
