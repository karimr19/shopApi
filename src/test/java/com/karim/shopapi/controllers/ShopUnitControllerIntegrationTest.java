package com.karim.shopapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.karim.shopapi.exceptions.ApiException;
import com.karim.shopapi.models.Error;
import com.karim.shopapi.models.*;
import com.karim.shopapi.services.ShopUnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты контроллера, проверяющие ответы контроллера на запрос.
 */
@WebMvcTest(ShopUnitController.class)
public class ShopUnitControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private ShopUnitService shopUnitService;

    private final ObjectWriter writer = objectMapper.writer().withDefaultPrettyPrinter();

    @Test
    void returnsOKWHenGetShopUnitByIdGetsValidId() throws Exception {
        var validId = "valid_id";
        var shopUnit = new ShopUnit();
        when(shopUnitService.getShopUnitById(validId)).thenReturn(shopUnit);
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/nodes/{id}", validId))
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, ShopUnit.class);
        assertEquals(shopUnit, actualResult);
    }

    @Test
    void returnsNotFoundErrorWhenGetShopUnitByIdThrowsNotFoundException() throws Exception {
        var notFoundId = "Not existing id";
        when(shopUnitService.getShopUnitById(notFoundId)).thenThrow(new ApiException("Some exception message",
                ErrorType.NOTFOUND));
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/nodes/{not_found_id}", notFoundId))
                .andExpect(status().isNotFound())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.NOTFOUND);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void returnsValidationErrorWhenImportShopUnitThrowsValidationException() throws Exception {
        var notValidRequest = new ShopUnitImportRequest();
        doThrow(new ApiException("Some exception message", ErrorType.VALIDATION)).when(shopUnitService)
                .importShopUnit(notValidRequest);
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports", notValidRequest))
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void returnsOKWhenImportShopUnitGetsValidInput() throws Exception {
        var validRequest = new ShopUnitImportRequest();
        validRequest.setItems(new ArrayList<ShopUnitImport>());
        validRequest.setUpdateDate(LocalDateTime.now().toString());
        doNothing().when(shopUnitService).importShopUnit(validRequest);

        String requestJson = writer.writeValueAsString(validRequest);

        mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void returnsOKWhenDeleteShopUnitByIdGetsValidId() throws Exception {
        var validId = "valid_id";
        doNothing().when(shopUnitService).deleteShopUnitById(validId);
        mockMvc.perform(MockMvcRequestBuilders.delete("/delete/{id}", validId))
                .andExpect(status().isOk());
    }

    @Test
    void returnsValidationErrorWhenDeleteShopUnitByIdThrowsValidationException() throws Exception {
        var notValidId = "not_valid_id";
        doThrow(new ApiException("Some exception message", ErrorType.VALIDATION)).when(shopUnitService)
                .deleteShopUnitById(notValidId);
        var result = mockMvc.perform(MockMvcRequestBuilders.delete("/delete/{id}", notValidId))
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void returnsNotFoundErrorWhenDeleteShopUnitByIdThrowsNotFoundException() throws Exception {
        var notFoundId = "not_found_id";
        doThrow(new ApiException("Some exception message", ErrorType.NOTFOUND)).when(shopUnitService)
                .deleteShopUnitById(notFoundId);
        var result = mockMvc.perform(MockMvcRequestBuilders.delete("/delete/{id}", notFoundId))
                .andExpect(status().isNotFound())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.NOTFOUND);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitWithNullId() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(LocalDateTime.now().toString());
        var itemWithIncorrectId = new ShopUnitImport();
        String nullId = null;
        itemWithIncorrectId.setId(nullId);
        itemWithIncorrectId.setName("name");
        itemWithIncorrectId.setType(ShopUnitType.OFFER);
        itemWithIncorrectId.setPrice(100L);
        request.getItems().add(itemWithIncorrectId);
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitWithNullName() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(LocalDateTime.now().toString());
        var itemWithNullName = new ShopUnitImport();
        String correctId = "id";
        itemWithNullName.setId(correctId);
        itemWithNullName.setType(ShopUnitType.OFFER);
        itemWithNullName.setPrice(100L);
        request.getItems().add(itemWithNullName);
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitWithNullType() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        request.setUpdateDate(LocalDateTime.now().toString());
        var itemWithNullType = new ShopUnitImport();
        String correctId = "id";
        itemWithNullType.setName("name");
        itemWithNullType.setId(correctId);
        itemWithNullType.setPrice(100L);
        request.getItems().add(itemWithNullType);
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitImportRequestWithNullUpdateDate() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        var item = new ShopUnitImport();
        String correctId = "id";
        item.setName("name");
        item.setId(correctId);
        item.setPrice(100L);
        item.setType(ShopUnitType.OFFER);
        request.getItems().add(item);
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitImportRequestWithNullItems() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setUpdateDate(LocalDateTime.now().toString());
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void importShopUnitThrowsNotValidExceptionWhenGetsShopUnitImportRequestItemsContainsNull() throws Exception {
        var request = new ShopUnitImportRequest();
        request.setItems(new ArrayList<ShopUnitImport>());
        var item = new ShopUnitImport();
        String correctId = "id";
        item.setName("name");
        item.setId(correctId);
        item.setPrice(100L);
        item.setType(ShopUnitType.OFFER);
        request.getItems().add(item);
        request.getItems().add(null);
        String requestJson = writer.writeValueAsString(request);

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/imports")
                        .contentType(APPLICATION_JSON_UTF8).content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }

    @Test
    void getSalesThrowsNotValidExceptionWhenGetSalesThrowsValidationException() throws Exception {
        var invalidDate = "invalid";

        when(shopUnitService.getSales(invalidDate))
                .thenThrow(new ApiException("Some message", ErrorType.VALIDATION));

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/sales")
                        .param("date", invalidDate))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        var actualResult = objectMapper.readValue(contentAsString, Error.class);
        var expectedError = new Error(ErrorType.VALIDATION);
        assertEquals(expectedError, actualResult);
    }
}
