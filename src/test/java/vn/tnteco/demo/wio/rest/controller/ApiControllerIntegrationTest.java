package vn.tnteco.demo.wio.rest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration Test kiểm tra tất cả các API Endpoint và SingleReturnValueHandler")
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/users/1 -> Trả về thông tin user thành công")
    void testGetUserById_Success() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/users/1"))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nguyen Van A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/users/999 -> Trả về 404 USER_NOT_FOUND qua RestControllerAdvice")
    void testGetUserById_NotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/users/999"))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/users/999"));
    }

    @Test
    @DisplayName("GET /api/users/1/profile -> Trả về user kèm danh sách orders (flatMap + Pair)")
    void testGetUserProfile_Success() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/users/1/profile"))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.orders").isArray());
    }

    @Test
    @DisplayName("GET /api/users/1/dashboard -> Trả về dashboard 3 nguồn song song (Single.zip)")
    void testGetUserDashboard_Success() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/users/1/dashboard"))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.totalSpent").isNumber())
                .andExpect(jsonPath("$.executionDurationMs").isNumber());
    }

    @Test
    @DisplayName("POST /api/orders với quantity <= 0 -> Trả về 400 INVALID_REQUEST")
    void testCreateOrder_InvalidQuantity() throws Exception {
        String requestJson = """
                {
                    "userId": 1,
                    "productId": 2,
                    "quantity": 0
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    @DisplayName("GET /api/products/1/availability -> Trả về thông tin tình trạng tồn kho")
    void testGetProductAvailability_Success() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/products/1/availability"))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.available").value(true));
    }
}
