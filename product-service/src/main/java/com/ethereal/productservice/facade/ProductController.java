package com.ethereal.productservice.facade;

import com.ethereal.productservice.entity.*;
import com.ethereal.productservice.mediator.ProductMediator;
import com.ethereal.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMediator productMediator;
    private final ProductService productService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam(required = false) String name_like,
                                 @RequestParam(required = false) String data,
                                 @RequestParam(required = false) String _category,
                                 @RequestParam(required = false) Float price_min,
                                 @RequestParam(required = false) Float price_max,
                                 @RequestParam(required = false, defaultValue = "1") int _page,
                                 @RequestParam(required = false, defaultValue = "10") int _limit,
                                 @RequestParam(required = false, defaultValue = "priority") String _sort,
                                 @RequestParam(required = false, defaultValue = "asc") String _order) {
        return productMediator.getProduct(_page, _limit, name_like, _category, price_min, price_max, data, _sort, _order);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Response> save(@RequestBody ProductFormDTO productFormDTO) {
        System.out.println("Received ProductFormDTO: " + productFormDTO);
        return productMediator.saveProduct(productFormDTO);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Response> delete(@RequestParam String uuid) {
        return productMediator.deleteProduct(uuid);
    }

    @RequestMapping(value = "/getExternal", method = RequestMethod.GET)
    public ResponseEntity<?> getProduct(@RequestParam String uuid) {
        return productMediator.getProductExtend(uuid);
    }

    @PatchMapping("/discount")
    public ResponseEntity<?> updateProductDiscount(@RequestParam String uuid, @RequestBody ProductDiscountUpdateRequest request) {
        ProductEntity updatedProduct = productService.updateProductDiscount(uuid, request.isDiscount(), request.getPrice(), request.getDiscountedPrice());
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/priority")
    public ResponseEntity<?> updateProductPriority(@RequestParam String uuid, @RequestBody PriorityRequest request) {
        ProductEntity updatedProduct = productService.updatePriority(uuid, request.priority());
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<SimpleProductDTO> getProductByUuid(@PathVariable String uuid) {
        SimpleProductDTO product = productService.getSimpleProductByUuid(uuid);
        return ResponseEntity.ok(product);
    }
}