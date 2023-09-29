# java-performance-training
The samples for the 'Java performance' training

To generate an ``OutOfMemoryError`` - add the following params in the 'VM Options':
``-Xms200m -Xmx200m -XX:+HeapDumpOnOutOfMemoryError -Xverify:none``