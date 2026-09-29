# java-performance-training
The samples for the 'Java performance' training

To generate an ``OutOfMemoryError`` - add the following params in the 'VM Options':
``-Xms200m -Xmx200m -XX:+HeapDumpOnOutOfMemoryError``

To run a lab as a Gatling test, see [docs/gatling-labs.md](docs/gatling-labs.md).
The problems the labs reproduce are described in [docs/lab-issues.md](docs/lab-issues.md).
