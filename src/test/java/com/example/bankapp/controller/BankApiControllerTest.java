package com.example.bankapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BankApiControllerTest {

    @Mock
    private BankService bankService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BankApiController(bankService)).build();
    }

    @Test
    void getAccountsReturnsJsonData() throws Exception {
        Customer customer = new Customer();
        customer.setAccountNumber("100001");
        customer.setFullName("Alex Customer");
        customer.setEmail("alex@example.com");
        customer.setBalance(new BigDecimal("2500.00"));
        when(bankService.getCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].accountNumber").value("100001"))
                .andExpect(jsonPath("$[0].fullName").value("Alex Customer"))
                .andExpect(jsonPath("$[0].balance").value(2500.00));
    }
}
