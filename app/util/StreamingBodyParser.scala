package util

import storage.StreamableResource
import play.api.mvc.BodyParser
import akka.util.ByteString
import akka.stream.scaladsl.Sink
import java.io.OutputStream
import scala.concurrent.Future
import akka.stream.scaladsl.Flow
import play.api.Logger
import akka.stream.scaladsl.Keep
import play.api.libs.streams.Accumulator


/** Body Parser that streams a request body into a StreamableResource (which basically wraps an output stream).
 *
 * This allows avoiding loading large bodies into memory, as well as piping into temporary
 * files and moving them around later, and allows using any storage strategy/medium (i.e. anything that can
 * provide an OutputStream).
 */
object StreamingCSVBodyParser {
  val logger: Logger = Logger("application")
  val emptyBytes = ByteString()
  def buildStreamingBodyParser(streamableResource: StreamableResource): BodyParser[ByteString] = {
    val streamingBodyHandler: BodyParser[ByteString] = BodyParser { req =>
      val sink: Sink[ByteString, Future[ByteString]] = Flow[ByteString]
        .toMat(Sink.fold(ByteString())((acc, chunk) => {
          streamableResource.stream match {
            case Some(stream: OutputStream) => {
              logger.debug(s"Writing ${chunk.length} bytes to $stream")
              stream.write(chunk.toArray[Byte])
            }
            case None => throw new IllegalStateException(s"Outputstream was not present in StreamableResource $streamableResource");
          }
          emptyBytes
        }))(Keep.right)
        
      import play.api.libs.concurrent.Execution.Implicits._

      Accumulator(sink).map(Right.apply)
    }
    streamingBodyHandler
  }
}