package com.Ecommerce.CustomerService.Controller;

import com.Ecommerce.CustomerService.Exception.AddCustomerConflictException;
import com.Ecommerce.CustomerService.Exception.CustomerNotFoundException;
import com.Ecommerce.CustomerService.Request.AddCustomer;
import com.Ecommerce.CustomerService.Service.Customer_Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final Customer_Service customerService;

    public CustomerController(Customer_Service customerService) {
        this.customerService = customerService;
    }

    //Add Customer
    @PostMapping("/add-customer")
    public ResponseEntity<?> addCustomer(@RequestBody AddCustomer customer) {
        try {
            return ResponseEntity.ok(customerService.Add_Customer(customer));
        } catch (AddCustomerConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    //Get Customer Details by ID
    @GetMapping("/customerDetails/{customerId}")
    public ResponseEntity<?> CustomerDetails(@PathVariable String customerId, @RequestHeader("Authorization") String bearerToken) {
        try {
            return ResponseEntity.ok(customerService.CustomerDetails(customerId,bearerToken));
        }catch (CustomerNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
