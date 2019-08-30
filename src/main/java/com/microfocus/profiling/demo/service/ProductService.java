package com.microfocus.profiling.demo.service;

import com.microfocus.profiling.demo.model.Product;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class ProductService {

    private static final long BYTES_IN_MB = 1048576;

    private final DecimalFormat decimalFormat = new DecimalFormat("##.##");

    private final Random random = new Random(20000);

    private final Runtime runtime = Runtime.getRuntime();

    private double totalSales = 0;

    @Scheduled(fixedRate = 2000)
    public void simulateProductsProcessing() {
        System.out.println();

        final long memoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_IN_MB;
        System.out.println("JVM memory in use before: " + memoryBefore + " MB");

        final long now = System.currentTimeMillis();
        processALotOfProducts();
        System.out.println("The entire processing took " + (System.currentTimeMillis() - now) + " ms");

        final long memoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_IN_MB;
        System.out.println("JVM memory in use after: " + memoryAfter + " MB");
    }

    private void processALotOfProducts() {
        final int size = random.nextInt(50);
        final Set<Product> products = new HashSet<>(size);
        IntStream.range(0, size)
                 .forEach(index -> products.add(new Product(index, "The product " + index, 20 * random.nextDouble())));

        final double totalPrice = products.stream()
                                          .mapToDouble(Product::getPrice)
                                          .sum();
        System.out.println("The total price of the " + products.size() + " products is " + decimalFormat.format(totalPrice));

        totalSales += totalPrice;
    }

    public double getTotalSales() {
        return totalSales;
    }
}
