package brooklyn.entity.messaging.rabbit;

import java.util.Map;

import brooklyn.catalog.Catalog;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.messaging.MessageBroker;
import brooklyn.entity.messaging.amqp.AmqpServer;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.annotations.Beta;

/**
 * An {@link brooklyn.entity.Entity} that represents a single Rabbit MQ broker instance, using AMQP 0-9-1.
 */
@Catalog(name="RabbitMQ Broker", description="RabbitMQ is an open source message broker software (i.e. message-oriented middleware) that implements the Advanced Message Queuing Protocol (AMQP) standard", iconUrl="classpath:///RabbitMQLogo.png")
@ImplementedBy(RabbitBrokerImpl.class)
public interface RabbitBroker extends SoftwareProcess, MessageBroker, AmqpServer {

    @SetFromFlag("version")
    public static final BasicConfigKey<String> SUGGESTED_VERSION = new BasicConfigKey<String>(SoftwareProcess.SUGGESTED_VERSION, "2.8.7");

    @SetFromFlag("erlangVersion")
    public static final BasicConfigKey<String> ERLANG_VERSION = new BasicConfigKey<String>(String.class, "erlang.version", "Erlang runtime version", "R15B");
    
    @SetFromFlag("amqpPort")
    public static final PortAttributeSensorAndConfigKey AMQP_PORT = AmqpServer.AMQP_PORT;

    @SetFromFlag("virtualHost")
    public static final BasicAttributeSensorAndConfigKey<String> VIRTUAL_HOST_NAME = AmqpServer.VIRTUAL_HOST_NAME;

    @SetFromFlag("amqpVersion")
    public static final BasicAttributeSensorAndConfigKey<String> AMQP_VERSION = new BasicAttributeSensorAndConfigKey<String>(
            AmqpServer.AMQP_VERSION, AmqpServer.AMQP_0_9_1);
    
    RabbitQueue createQueue(Map properties);

    // TODO required by RabbitDestination due to close-coupling between that and RabbitBroker; how best to improve?
    @Beta
    Map<String, String> getShellEnvironment();
    
    @Beta
    String getRunDir();
}
