package com.example.bankapp.controller;

import java.math.BigDecimal;
import java.util.List;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService transactionAuditService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {
        populateDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, null);
            return "bank";
        }

        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute(
                    "success", "Customer " + customer.getFullName() + " was added.");
            return "redirect:/";
        } catch (BankException exception) {
            if (exception.getMessage().contains("account number")) {
                bindingResult.rejectValue("accountNumber", "customer.duplicate",
                        exception.getMessage());
            } else {
                bindingResult.rejectValue("email", "customer.duplicate",
                        exception.getMessage());
            }
            populateDashboard(model, null);
            return "bank";
        }
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, null);
            return "bank";
        }

        try {
            bankService.transferMoney(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        } catch (BankException | AccountNotFoundException | InsufficientBalanceException exception) {
            transactionAuditService.recordFailedTransaction(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    "Transfer failed: " + exception.getMessage());
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model,
            RedirectAttributes redirectAttributes) {
        Customer customer = bankService.getCustomer(accountNumber).orElse(null);
        if (customer == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Account " + accountNumber + " was not found.");
            return "redirect:/";
        }

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankService.getTransactionHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalAmountSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {
        if (min.signum() < 0) {
            model.addAttribute("error", "Minimum amount cannot be negative.");
            model.addAttribute("transfers", List.<BankTransaction>of());
        } else {
            model.addAttribute("transfers", bankService.getSuccessfulTransfersAbove(min));
        }
        model.addAttribute("min", min);
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {
        if (amount.signum() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Deposit amount must be greater than zero.");
            return "redirect:/deposit";
        }

        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully.");
        } catch (BankException | AccountNotFoundException | InsufficientBalanceException exception) {
            transactionAuditService.recordFailedTransaction(
                    null, accountNumber, amount, "Deposit failed: " + exception.getMessage());
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {
        if (amount.signum() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal amount must be greater than zero.");
            return "redirect:/withdraw";
        }

        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully.");
        } catch (BankException | AccountNotFoundException | InsufficientBalanceException exception) {
            transactionAuditService.recordFailedTransaction(
                    accountNumber, null, amount, "Withdrawal failed: " + exception.getMessage());
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/withdraw";
    }

    private void populateDashboard(Model model, String keyword) {
        List<Customer> allCustomers = bankService.getCustomers();
        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customers",
                keyword == null || keyword.isBlank()
                        ? allCustomers
                        : bankService.searchCustomers(keyword));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("totalCustomers", allCustomers.size());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessfulTransactionCount());
        model.addAttribute("failedCount", bankService.getFailedTransactionCount());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
    }
}
