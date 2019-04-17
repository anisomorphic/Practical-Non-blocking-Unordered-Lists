# Practical-Non-blocking-Unordered-Lists
##### A Java based highly-concurrent linked list algorithm based on work by Zhang, Et Al.

### by Michael Harris, Marcus Sooter

University of Central Florida


This is our semester project for Dr. Dechev's COP4520 multicore programming class.

Each directory contains a different version of the program with straightforward
instructions inside 'Read Me.txt' to run and execute the source code in Java.


The STM versions of the code require use of DeuceSTM 1.2, which can be found at this link:

[https://sites.google.com/site/deucestm/download/deuceAgent-1.2.0.jar?attredirects=0&d=1]

The above website has instructions on how to execute the java file with the --javaagent flag,
but we would recommend adding the .jar file linked above as a compile-time library in your 
IDE of choice, which facilitates seamless execution of the code with the real-time 
instrumentation provided by DeuceSTM.

Further, a pre-configured NetBeans (8.2) project file will be provided as a .zip file in each
STM directory to ensure ease of use running the code and to assist in verifying benchmarks
without needing to wrestle with configuring Deuce.
