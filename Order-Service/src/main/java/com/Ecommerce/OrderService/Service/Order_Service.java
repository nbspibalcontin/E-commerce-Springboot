package com.Ecommerce.OrderService.Service;


import com.Ecommerce.OrderService.Entity.Order;
import com.Ecommerce.OrderService.Entity.OrderItem;
import com.Ecommerce.OrderService.Enum.OrderStatus;
import com.Ecommerce.OrderService.Exception.OrderCreationException;
import com.Ecommerce.OrderService.Exception.ProductsNotFoundException;
import com.Ecommerce.OrderService.Repository.OrderRepository;

import com.Ecommerce.OrderService.Request.Customer;
import com.Ecommerce.OrderService.Request.OrderRequest;
import com.Ecommerce.OrderService.Request.Product;
import com.Ecommerce.OrderService.Response.MessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class Order_Service {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public Order_Service(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
    }

    private static final String PRODUCT_SERVICE_URL = "http://Product-Service/api/product";

    public MessageResponse addOrder(OrderRequest orderRequest, String bearerToken, Customer customer) throws JsonProcessingException {
        try {
            // Extract product IDs from the order request
            List<Long> productIds = orderRequest.getOrderItems().stream()
                    .map(OrderRequest.OrderItemRequest::getProductId)
                    .toList();

            // Convert product IDs to a comma-separated string
            String commaSeparatedIds = productIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // Fetch product details from the Product Service
            List<Product> products = webClientBuilder.build().get()
                    .uri(PRODUCT_SERVICE_URL + "/getByIds?productIds={productIds}", commaSeparatedIds)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
                    .block();

            if (products != null && !products.isEmpty()) {
                // Create a new order
                Order order = new Order();
                order.setOrderStatus(OrderStatus.CREATED);
                order.setCustomerId(customer.getConsumerId());
                List<OrderItem> orderItems = new ArrayList<>();

                // Process each product and order item
                for (Product product : products) {
                    int totalQuantity = 0;
                    for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
                        if (itemRequest.getProductId().equals(product.getId())) {
                            totalQuantity += itemRequest.getQuantity();
                        }
                    }

                    if (totalQuantity > 0) {
                        // Create an order item and calculate total price
                        OrderItem orderItem = new OrderItem();
                        orderItem.setQuantity(totalQuantity);
                        orderItem.setTotalPrice(product.getPrice() * totalQuantity); // Calculate total price based on product price and combined quantity
                        orderItem.setProductId(product.getId());
                        orderItem.setOrder(order); // Associate the order item with the order
                        orderItems.add(orderItem);
                    }
                }

                // Associate order items with the order
                order.setOrderItems(orderItems);

                // Save the order to the repository if needed
                orderRepository.save(order);
            }

            return new MessageResponse("Order created successfully");
        }catch (WebClientResponseException.NotFound ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new ProductsNotFoundException(responseBody);
        }
    }
}