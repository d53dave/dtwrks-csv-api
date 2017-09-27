package util

import java.util.UUID
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer => ApacheKafkaProducer}
import org.apache.kafka.clients.producer.ProducerRecord
import com.typesafe.config.ConfigFactory


case class KafkaProducer(
    topic: String,
    brokerList: String,
    clientId: String = UUID.randomUUID().toString) {

  val props = new Properties()
  
  props.put("bootstrap.servers", ConfigFactory.load().getString("messagebroker.urls"));
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
  props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
  props.put("client.id", clientId.toString)

  val producer = new ApacheKafkaProducer[AnyRef, AnyRef](props)

  def kafkaMesssage(message: Array[Byte], partition: Array[Byte]): ProducerRecord[AnyRef, AnyRef] = {
    if (partition == null) {
      new ProducerRecord(topic, message)
    } else {
      new ProducerRecord(topic, partition, message)
    }
  }

  def send(message: String, partition: String = null): Unit = send(message.getBytes("UTF8"), if (partition == null) null else partition.getBytes("UTF8"))

  def send(message: Array[Byte], partition: Array[Byte]): Unit = {
    try {
      producer.send(kafkaMesssage(message, partition))
      ()
    } catch {
      case e: Exception =>
        e.printStackTrace
        System.exit(1)
    }
  }
}