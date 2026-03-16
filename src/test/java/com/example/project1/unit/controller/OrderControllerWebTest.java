package com.example.project1.unit.controller;

import com.example.project1.controller.GlobalExceptionHandler;
import com.example.project1.controller.OrderController;
import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.security.JwtAuthenticationFilter;
import com.example.project1.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单控制器 Web 层测试。
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createOrderShouldReturnValidationErrorWhenItemsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRequestId("REQ-VALIDATION-1");
        request.setUserId(1L);
        request.setItems(List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("items不能为空"));
    }

    @Test
    void createOrderShouldReturnSuccess() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRequestId("REQ-WEB-OK-1");
        request.setUserId(1L);

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1001L);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("9.90"));
        request.setItems(List.of(item));

        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("ORD-WEB-OK-1");
        order.setRequestId("REQ-WEB-OK-1");
        order.setUserId(1L);
        order.setStatus(0);
        order.setTotalAmount(new BigDecimal("9.90"));

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").value("ORD-WEB-OK-1"));
    }

    @Test
    void getInventoryShouldReturnValidationErrorWhenProductIdInvalid() throws Exception {
        mockMvc.perform(get("/orders/inventory/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void getInventoryShouldReturnSuccess() throws Exception {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setId(1L);
        inventory.setSkuCode("SKU-1001");
        inventory.setAvailableQty(80);
        inventory.setLockedQty(0);

        when(orderService.getInventoryByProductId(1001L)).thenReturn(inventory);

        mockMvc.perform(get("/orders/inventory/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuCode").value("SKU-1001"))
                .andExpect(jsonPath("$.data.availableQty").value(80));
    }
}

