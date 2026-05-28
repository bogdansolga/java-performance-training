package net.safedata.performance.training.error;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        System.out.println("Caught exception: " + e.getMessage());
        e.printStackTrace();
    }

}
