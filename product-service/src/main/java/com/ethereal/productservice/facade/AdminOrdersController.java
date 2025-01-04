package com.ethereal.productservice.facade;

import com.ethereal.productservice.entity.OrderItemDTO;
import com.ethereal.productservice.entity.OrderProductSummaryDTO;
import com.ethereal.productservice.entity.SimpleProductDTO;
import com.ethereal.productservice.entity.UserDTO;
import com.ethereal.productservice.mediator.AdminOrdersMediator;
import com.ethereal.productservice.service.AdminOrdersService;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/transactions")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final AdminOrdersMediator orderMediator;
    private final AdminOrdersService adminOrdersService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getAllOrdersForAdmin() {
        return orderMediator.getAllOrders();
    }

    @GetMapping("/items")
    public ResponseEntity<List<OrderItemDTO>> getOrderItems(@RequestParam String orderUuid) {
        List<OrderItemDTO> items = adminOrdersService.getOrderItemsByOrderUuid(orderUuid);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/products")
    public ResponseEntity<List<SimpleProductDTO>> getOrderProducts(@RequestParam String orderUuid) {
        List<SimpleProductDTO> products = adminOrdersService.getOrderProducts(orderUuid);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/order-details")
    public ResponseEntity<OrderProductSummaryDTO> getOrderDetails(@RequestParam String orderUuid) {
        OrderProductSummaryDTO orderDetails = adminOrdersService.getOrderDetails(orderUuid);
        return ResponseEntity.ok(orderDetails);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "show-details", required = false, defaultValue = "false") boolean showDetails,
            @RequestParam(value = "uuid", required = false) String uuid) {

        if (showDetails) {
            if (uuid == null || uuid.isEmpty()) {
                return ResponseEntity.badRequest().body("Missing 'uuid' parameter for detailed user request.");
            }
            try {
                UserDTO user = adminOrdersService.getUserByUuid(uuid);
                return ResponseEntity.ok(user);
            } catch (NoResultException e) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("User with UUID " + uuid + " not found.");
            }
        } else {
            List<UserDTO> users = adminOrdersService.getAllUsers();
            return ResponseEntity.ok(users);
        }
    }
}
