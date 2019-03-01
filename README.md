Simple test application demonstarting the problem of temporary destinations not
being properly removed on close of pooled connections due to existing consumers.

Background:
	The JMS specification states that close of a connection must close all
	of the sessions, and related JMS objects.  This program demonstrates
	that this is not happening properly with pooled connections.

To run the test:
	mvn clean package
	mvn exec:java -Dexec.mainClass=example.ExampleRunner

Interpreting the results:
	Note the "== TEMP QUEUES AFTER ==" heading.  There should be no
	temporary queues listed at this point if all is working properly.

