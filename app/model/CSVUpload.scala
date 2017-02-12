package model

import play.api.libs.json.Writes
import play.api.libs.json.Json

case class CSVUpload(id: String, filename: String, path: String)

object CSVUpload {
  implicit val csvUploadJsonWrite = new Writes[CSVUpload] {
    def writes(csvUpload: CSVUpload) = Json.obj(
      "id" -> csvUpload.id,
      "filename" -> csvUpload.filename,
      "path" -> csvUpload.path)
  }
}
