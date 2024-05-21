package net.safedata.performance.training.service;

import net.safedata.performance.training.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private static final long BYTES_IN_MB = 1048576;

    private static final Random RANDOM = new Random(20000);

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private final DecimalFormat decimalFormat = new DecimalFormat("#,###.#");

    // kept in memory to show the case of a continuously growing memory
    private final List<Product> products = new ArrayList<>();
    private double totalSales = 0;

    /*
    @Scheduled(
            fixedRate = 5,
            timeUnit = TimeUnit.SECONDS
    )
    */
    public void simulateProductsProcessing() {
        System.out.println();

        final long memoryBefore = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use before: {} MB", memoryBefore);

        final long now = System.currentTimeMillis();
        processALotOfProducts();
        LOGGER.info("The entire processing took {} ms", System.currentTimeMillis() - now);

        final long memoryAfter = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use after: {} MB", memoryAfter);
    }

    @Scheduled(
            fixedRate = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void simulateProductsProcessingUsingAStopwatch() {
        System.out.println();

        StopWatch stopWatch = new StopWatch("simulateProductsProcessing");

        final long memoryBefore = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use before the processing: {} MB", memoryBefore);

        stopWatch.start("Processing a lot of products");
        processALotOfProducts();
        stopWatch.stop();

        stopWatch.start("A short expensive task");
        sleepALittle(200);
        stopWatch.stop();

        stopWatch.start("A long expensive task");
        sleepALittle(5000);
        stopWatch.stop();

        final long memoryAfter = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use after the processing: {} MB", memoryAfter);

        LOGGER.info("Execution summary: {}", stopWatch.prettyPrint());
    }

    private void processALotOfProducts() {
        final int productsNumber = RANDOM.nextInt(1000); //000
        generateProducts(productsNumber);

        final double totalPrice = getProductsPriceSum(products);
        LOGGER.info("The total price of {} products is {}", products.size(), decimalFormat.format(totalPrice));

        totalSales += totalPrice;
        LOGGER.info("The total sales is currently {}", decimalFormat.format(totalSales));
    }

    private void generateProducts(int productsNumber) {
        IntStream.rangeClosed(0, productsNumber)
                 .forEach(index -> products.add(buildProduct(index)));
    }

    //@Scheduled(fixedRate = 5000)
    public void generateALotOfData() {
        final long memoryBefore = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use before generating a lot of data: {} MB", memoryBefore);

        final int productsNumber = 1_000_000;
        final Set<Product> innerProducts = new HashSet<>(productsNumber);
        generateProducts(productsNumber);
        LOGGER.info("The products cost: {}", getProductsPriceSum(innerProducts));

        final long memoryAfter = getFreeMemoryInMB();
        LOGGER.info("JVM memory in use after generating a lot of data: {} MB", memoryAfter);
    }

    private static double getProductsPriceSum(Collection<Product> products) {
        //TODO replace with StreamSupport.parallel
        return products.stream()
                       .mapToDouble(Product::getPrice)
                       .sum();
    }

    private long getFreeMemoryInMB() {
        return (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / BYTES_IN_MB;
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
        sleepALittle(10);
        return new Product(index, "The product " + index, 1000 * RANDOM.nextInt(50000));
    }

    private void sleepALittle(final int bound) {
        //if (true) return; //TODO uncomment to add some processing time
        try {
            Thread.sleep(RANDOM.nextInt(Math.abs(bound) + 10));
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    public void potentialMemoryLeaksGenerators() {
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
}
