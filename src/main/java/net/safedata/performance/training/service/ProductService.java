package net.safedata.performance.training.service;

import net.safedata.performance.training.model.Product;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class ProductService {

    private static final long BYTES_IN_MB = 1048576;

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private final DecimalFormat decimalFormat = new DecimalFormat("##.##");

    private final Random random = new Random(20000);

    private double totalSales = 0;
    private final List<Product> products = new ArrayList<>();

    @Scheduled(
            fixedRate = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void simulateProductsProcessing() {
        System.out.println();

        final long memoryBefore = getFreeMemory() / BYTES_IN_MB;
        System.out.println("JVM memory in use before: " + memoryBefore + " MB");

        final long now = System.currentTimeMillis();
        processALotOfProducts();
        System.out.println("The entire processing took " + (System.currentTimeMillis() - now) + " ms");

        final long memoryAfter = getFreeMemory() / BYTES_IN_MB;
        System.out.println("JVM memory in use after: " + memoryAfter + " MB");

        // double brace init - potential cause for a memory leak
        new HashMap<Integer, Product>() {{
            IntStream.range(0, 200)
                     .forEach(index -> put(index, buildProduct(random.nextInt())));
        }};

        { // inner / private / anonymous block
            final Product product = buildProduct(20);
        }
    }

    private void processALotOfProducts() {
        final int size = random.nextInt(500000);
        IntStream.range(0, size)
                 .forEach(index -> products.add(buildProduct(index)));

        final double totalPrice = products.stream()
                                          .peek(it -> sleepALittle(100))
                                          .mapToDouble(Product::getPrice)
                                          .sum();
        System.out.println("The total price of the " + products.size() + " products is " + decimalFormat.format(totalPrice));

        totalSales += totalPrice;
    }

    //@Scheduled(fixedRate = 5000)
    public void generateALotOfData() {
        final int howMany = 1_000_000;

        final long memoryBefore = getFreeMemory() / BYTES_IN_MB;
        System.out.println("JVM memory in use before generating a lot of data: " + memoryBefore + " MB");

        final Set<Product> products = new HashSet<>(howMany);
        IntStream.range(0, howMany)
                 .forEach(index -> products.add(buildProduct(index)));
        System.out.println(products.stream()
                                   .peek(it -> sleepALittle(200))
                                   .mapToDouble(Product::getPrice)
                                   .sum());

        final long memoryAfter = getFreeMemory() / BYTES_IN_MB;
        System.out.println("JVM memory in use after generating a lot of data: " + memoryAfter + " MB");
    }

    private long getFreeMemory() {
        return RUNTIME.totalMemory() - RUNTIME.freeMemory();
    }

    public double getTotalSales() {
        return totalSales;
    }

    public List<Product> getALotOfProducts(final String productType, final String retrievingType) {
        final long now = System.currentTimeMillis();

        final int howMany = random.nextInt(30);
        final List<Product> products = new ArrayList<>(howMany);
        IntStream.range(0, howMany)
                 .peek(this::sleepALittle)
                 .forEach(index -> products.add(buildProduct(index)));

        System.out.println("[" + retrievingType + "] Returning " + products.size() + " " + productType + "s took "
                + (System.currentTimeMillis() - now) + " ms");
        return products;
    }

    public synchronized List<Product> getSynchronizedProducts(final String productType) {
        return getALotOfProducts(productType, "synchronized");
    }

    private Product buildProduct(final int index) {
        return new Product(index, "The product " + index, 1000 * random.nextInt(50000));
    }

    private void sleepALittle(final int bound) {
        if (true) return;
        try {
            Thread.sleep(random.nextInt(Math.abs(bound) + 10));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
