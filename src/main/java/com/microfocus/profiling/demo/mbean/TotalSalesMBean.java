package com.microfocus.profiling.demo.mbean;

import com.microfocus.profiling.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

@ManagedResource(
        objectName = "ProfilingDemo:category=ProfilingDemo,name=TotalSalesMBean",
        description = "MBean used to display the sales for today"
)
@Service
public class TotalSalesMBean {

    private final ProductService productService;

    @Autowired
    public TotalSalesMBean(final ProductService productService) {
        this.productService = productService;
    }

    @ManagedAttribute(description = "The total sales volume for today")
    public double getTotalSales() {
        return productService.getTotalSales();
    }
}
