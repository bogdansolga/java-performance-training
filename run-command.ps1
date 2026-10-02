# Starts the training application with lab 1 (unbounded retention) and a small, fixed JVM setup.
# PowerShell equivalent of run-command.sh. Run it from the repository folder:  .\run-command.ps1
# Build first if needed:  .\mvnw.cmd -q -DskipTests package

$jvmOptions = @(
    # Initial heap size: start with 256 MB, so the heap never has to grow
    '-Xms256m'
    # Maximum heap size: 256 MB, small enough to see memory pressure quickly
    '-Xmx256m'
    # Young generation size: 200 MB of the 256 MB, so short-lived objects get most of the heap
    '-XX:NewSize=200m'
    # Metaspace size that triggers the first class-metadata collection: 10 MB
    '-XX:MetaspaceSize=10m'
    # Threads the garbage collector uses in parallel phases: 4
    '-XX:ParallelGCThreads=4'
    # Keep generation sizes fixed: no automatic resizing between collections
    '-XX:-UseAdaptiveSizePolicy'
    # Space for JIT-compiled code: only 10 MB (the default is 240 MB)
    '-XX:ReservedCodeCacheSize=10m'
    # Calls before a method is compiled: 100 (only applies when tiered compilation is off)
    '-XX:CompileThreshold=100'
)

$appArguments = @(
    # Switch on lab 1: the quote service that keeps every quote it creates
    '--spring.profiles.active=unbounded-retention'
)

# Start the application with the options and arguments above
& java @jvmOptions -jar target\java-performance-training-0.0.1-SNAPSHOT.jar @appArguments
