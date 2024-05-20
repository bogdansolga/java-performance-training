package net.safedata.performance.training.service;

import net.safedata.performance.training.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private static final long BYTES_IN_MB = 1048576;

    private static final Random RANDOM = new Random(20000);

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private final DecimalFormat decimalFormat = new DecimalFormat("#,###.#");

    private double totalSales = 0;
    private final List<Product> products = new ArrayList<>();

    @Scheduled(
            fixedRate = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void simulateProductsProcessing() {
        System.out.println();

        final long memoryBefore = getFreeMemory() / BYTES_IN_MB;
        LOGGER.info("JVM memory in use before: {} MB", memoryBefore);

        final long now = System.currentTimeMillis();
        processALotOfProducts();
        LOGGER.info("The entire processing took {} ms", System.currentTimeMillis() - now);

        final long memoryAfter = getFreeMemory() / BYTES_IN_MB;
        LOGGER.info("JVM memory in use after: {} MB", memoryAfter);

        // double brace init - potential cause for a memory leak
        new HashMap<Integer, Product>() {{
            IntStream.range(0, 200)
                     .forEach(index -> put(index, buildProduct(RANDOM.nextInt())));
        }};

        { // inner / private / anonymous block
            @SuppressWarnings("unused")
            final Product product = buildProduct(20);
        }
    }

    private void processALotOfProducts() {
        final int size = RANDOM.nextInt(500000);
        IntStream.range(0, size)
                 .forEach(index -> products.add(buildProduct(index)));

        final double totalPrice = products.stream()
                                          .mapToDouble(Product::getPrice)
                                          .sum();
        LOGGER.info("The total price of {} products is {}", products.size(), decimalFormat.format(totalPrice));

        totalSales += totalPrice;
    }

    //@Scheduled(fixedRate = 5000)
    public void generateALotOfData() {
        final int howMany = 1_000_000;

        final long memoryBefore = getFreeMemory() / BYTES_IN_MB;
        LOGGER.info("JVM memory in use before generating a lot of data: {} MB", memoryBefore);

        final Set<Product> products = new HashSet<>(howMany);
        IntStream.range(0, howMany)
                 .forEach(index -> products.add(buildProduct(index)));
        LOGGER.info("The products cost: {}", products.stream()
                                                     .mapToDouble(Product::getPrice)
                                                     .sum());

        final long memoryAfter = getFreeMemory() / BYTES_IN_MB;
        LOGGER.info("JVM memory in use after generating a lot of data: {} MB", memoryAfter);
    }

    private long getFreeMemory() {
        return RUNTIME.totalMemory() - RUNTIME.freeMemory();
    }

    public double getTotalSales() {
        return totalSales;
    }

    public List<Product> getALotOfProducts(final String productType, final String retrievingType) {
        final long now = System.currentTimeMillis();

        final int howMany = RANDOM.nextInt(30);
        final List<Product> products = new ArrayList<>(howMany);
        IntStream.range(0, howMany)
                 .peek(this::sleepALittle)
                 .forEach(index -> products.add(buildProduct(index)));

        LOGGER.info("[{}] Returning {} {}s took {} ms", retrievingType, products.size(), productType, (System.currentTimeMillis() - now));
        return products;
    }

    public synchronized List<Product> getSynchronizedProducts(final String productType) {
        return getALotOfProducts(productType, "synchronized");
    }

    private Product buildProduct(final int index) {
        sleepALittle(50);
        return new Product(index, "The product " + index, 1000 * RANDOM.nextInt(50000));
    }

    private void sleepALittle(final int bound) {
        if (true) return; //TODO uncomment to add some processing time
        try {
            Thread.sleep(RANDOM.nextInt(Math.abs(bound) + 10));
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
