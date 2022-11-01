package net.safedata.performance.training.controller;

import net.safedata.performance.training.model.Product;
import net.safedata.performance.training.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(final ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/long/{productType}")
    public List<Product> longRunningOperation(@PathVariable final String productType) {
        return productService.getALotOfProducts(productType, "non-synchronized");
    }

    @GetMapping("/long/sync/{productType}")
    public List<Product> getSynchronizedProducts(@PathVariable final String productType) {
        return productService.getSynchronizedProducts(productType);
    }
}