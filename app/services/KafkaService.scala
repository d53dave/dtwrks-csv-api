package services

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import play.api.Logger
import models.CSVUpload
import play.api.libs.json.Json
import com.google.inject.Inject
import com.typesafe.config.ConfigFactory
import akka.actor.ActorLogging
import util.KafkaProducer

/**
 * Simple Kafka client that pushes upload events into Kafka
 */
class KafkaService @Inject(){
  val logger: Logger = Logger(this.getClass())
  
  val url = ConfigFactory.load().getString("messagebroker.urls")
  val topic = ConfigFactory.load().getString("messagebroker.topic")
  val system = ActorSystem("dtwrks-test-kafka") // using separate actor system
  
  val kafkaSender: ActorRef = system.actorOf(KafkaSender.props(topic), "uploader-producertest")

  object KafkaSender {
    def props(topic: String): Props = Props(new KafkaSender(topic))
  }

  class KafkaSender(topic: String) extends Actor with ActorLogging {
    val producer = new KafkaProducer(topic, url)

    def receive = {
      case message: String => {
        try {
          producer.send(message)
        } catch {
          case e: Exception => log.error("Could not send message.", e)
        }
      }
    }
  }
  
  def sendCSVUploadEvent(csv: CSVUpload): Unit = {
    val csvJSON = Json.toJson(csv).toString()
    logger.info(s"Pushing $csvJSON to message broker")
    
    kafkaSender ! csvJSON
  }
}