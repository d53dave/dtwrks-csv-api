package service

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import org.apache.kafka.clients.producer.KafkaProducer
import play.api.Logger
import model.CSVUpload
import play.api.libs.json.Json
import com.google.inject.Inject
import com.typesafe.config.ConfigFactory

class KafkaService @Inject(){
  val url = ConfigFactory.load().getString("messagebroker.urls")
  val topic = ConfigFactory.load().getString("messagebroker.topic")

  object KafkaSender {
    def props(topic: String): Props = Props(new KafkaSender(topic))
  }

  class KafkaSender(topic: String) extends Actor {
    val producer = new KfkProducer(topic, url)

    def receive = {
      case message: String => {
        try {
          producer.send(message)
        } catch {
          case e: Exception => Logger.error("Could not send message.", e)
        }
      }
    }
  }

  val system = ActorSystem("dtwrks-test-kafka")
  val kafkaProducer: ActorRef = system.actorOf(KafkaSender.props("myevents"), "producertest")
  
  def sendCSVUploadEvent(csv: CSVUpload): Unit = {
    val csvJSON = Json.toJson(csv).toString()
    kafkaProducer ! s"""{ "event": "CSV Uploaded", "csv": $csvJSON }"""
  }
}