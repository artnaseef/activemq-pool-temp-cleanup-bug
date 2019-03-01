package example;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.RegionBroker;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.jms.pool.PooledConnectionFactory;

import java.net.URI;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

/**
 * The code below uses the JMS connection close operation to cleanup resources on the connection.  Pooled connections
 * in ActiveMQ (as of at least 5.15.6) do not properly cleanup consumers.
 *
 * TODO: review the JMS specification to verify expected operation is defined in the spec
 */
public class ExampleRunner {
    private BrokerService activemqEmbeddedBroker;

    private ActiveMQConnectionFactory rawConnectionFactory;
    private PooledConnectionFactory pooledConnectionFactory;

    public static void main(String[] args) {
        ExampleRunner runner = new ExampleRunner();
        runner.instanceMain(args);
    }

//========================================
// Internals
//----------------------------------------

    private void instanceMain(String[] args) {
        try {
            this.activemqEmbeddedBroker = new BrokerService();
            this.activemqEmbeddedBroker.setUseJmx(false);
            this.activemqEmbeddedBroker.setPersistent(false);
            this.activemqEmbeddedBroker.start();
            this.activemqEmbeddedBroker.waitUntilStarted();

            URI brokerUri = this.activemqEmbeddedBroker.getVmConnectorURI();
            this.rawConnectionFactory = new ActiveMQConnectionFactory(brokerUri);
            this.pooledConnectionFactory = new PooledConnectionFactory();
            this.pooledConnectionFactory.setConnectionFactory(this.rawConnectionFactory);

            this.pooledConnectionFactory.createConnection();

            System.out.println("---------- TEST WITH RAW CONNECTION ----------");
            this.runTest(this.rawConnectionFactory);

            System.out.println("---------- TEST WITH POOLED CONNECTION ----------");
            this.runTest(this.pooledConnectionFactory);

            this.activemqEmbeddedBroker.stop();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void runTest(ConnectionFactory connectionFactory) throws Exception {
        System.out.println("== TEMP QUEUES BEFORE ==");
        this.dumpTempDestQueueList(this.activemqEmbeddedBroker);

        System.out.println("== CREATE CONNECTION + TEMP QUEUE, THEN CLOSE CONNECTION ==");
        this.closeConnectionWithTempCreated(connectionFactory);

        System.out.println("== TEMP QUEUES AFTER ==");
        this.dumpTempDestQueueList(this.activemqEmbeddedBroker);
    }

    private void closeConnectionWithTempCreated(ConnectionFactory connectionFactory) throws Exception {
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        TemporaryQueue temporaryQueue = session.createTemporaryQueue();
        MessageConsumer consumer = session.createConsumer(temporaryQueue);

        System.out.println("Created temporary queue " + temporaryQueue.getQueueName());

        System.out.println("== TEMP QUEUES DURING ==");
        this.dumpTempDestQueueList(this.activemqEmbeddedBroker);

        connection.close();
    }

    private void dumpTempDestQueueList(BrokerService brokerService) throws Exception {
        Set<ActiveMQDestination>
            destinations = ((RegionBroker)brokerService.getRegionBroker()).getTempQueueRegion().getDestinationMap().keySet();

        if (destinations != null) {
            System.out.println(destinations.size() + " temporary queues");
            for (ActiveMQDestination oneDestination : destinations) {
                System.out.println("  - " + oneDestination.getQualifiedName());
            }
        } else {
            System.out.println("0 temporary queues");
        }
    }
}
