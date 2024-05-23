package net.safedata.performance.training;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ParallelProcessing {

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    private static final Random RANDOM = new Random(100);

    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(CORES / 2);
            ExecutorCompletionService<Integer> executorCompletionService = new ExecutorCompletionService<>(executorService);

            for (int i=0; i <=20; i++) {
                // forking - sending tasks to be processed in parallel
                executorCompletionService.submit(new DepositStockComputeTask());
            }

            Set<Integer> stocks = new HashSet<>();
            for (int i=0; i <=20; i++) {
                final Future<Integer> future = executorCompletionService.poll(100, TimeUnit.MILLISECONDS);
                if (future != null && future.isDone() && !future.isCancelled()) {
                    // joining
                    stocks.add(future.get());
                }
            }

            System.out.println("Obtained stocks: " + stocks);
            System.out.println("The processing took " + (System.currentTimeMillis() - now) + " ms");

            final List<Runnable> unfinishedTasks = executorService.shutdownNow();
            System.out.println("There are " + unfinishedTasks.size() + " unfinished tasks");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class DepositStockComputeTask implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            Thread.sleep(RANDOM.nextInt(500));
            System.out.println("Current thread: " + Thread.currentThread().getName());
            return RANDOM.nextInt(100);
        }
    }
}
