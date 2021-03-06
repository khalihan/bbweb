package org.biobank.domain.study

import org.biobank.domain.{ AnnotationTypeId, DomainValidation }
import org.biobank.domain.AnnotationValueType._
import org.biobank.infrastructure.JsonUtils._
import org.biobank.infrastructure.event.StudyEvents._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.github.nscala_time.time.Imports._
import scalaz._
import scalaz.Scalaz._

/** Used to add custom annotations to participants. The study can define multiple
  * annotation types on participants to store different types of data.
  */
case class ParticipantAnnotationType (
  studyId: StudyId,
  id: AnnotationTypeId,
  version: Long,
  timeAdded: DateTime,
  timeModified: Option[DateTime],
  name: String,
  description: Option[String],
  valueType: AnnotationValueType,
  maxValueCount: Option[Int],
  options: Option[Seq[String]],
  required: Boolean)
    extends StudyAnnotationType
    with StudyAnnotationTypeValidations {

  override def toString: String =
    s"""|ParticipantAnnotationTypex: {
        |  studyId: $studyId,
        |  id: $id,
        |  version: $version,
        |  timeAdded: $timeAdded,
        |  timeModified: $timeModified,
        |  name: $name,
        |  description: $description,
        |  valueType: $valueType,
        |  maxValueCount: $maxValueCount,
        |  options: { $options }
        |  required: $required
        |}""".stripMargin

  def update(
    name: String,
    description: Option[String],
    valueType: AnnotationValueType,
    maxValueCount: Option[Int] = None,
    options: Option[Seq[String]] = None,
    required: Boolean = false): DomainValidation[ParticipantAnnotationType] = {
    ParticipantAnnotationType.create(
      this.studyId, this.id, this.version, this.timeAdded, name, description, valueType, maxValueCount,
      options, required)
  }

}

object ParticipantAnnotationType extends StudyAnnotationTypeValidations {
  import org.biobank.domain.CommonValidations._

  def create(
    studyId: StudyId,
    id: AnnotationTypeId,
    version: Long,
    dateTime: DateTime,
    name: String,
    description: Option[String],
    valueType: AnnotationValueType,
    maxValueCount: Option[Int],
    options: Option[Seq[String]],
    required: Boolean): DomainValidation[ParticipantAnnotationType] = {
    (validateId(studyId) |@|
      validateId(id) |@|
      validateAndIncrementVersion(version) |@|
      validateString(name, NameRequired) |@|
      validateNonEmptyOption(description, NonEmptyDescription) |@|
      validateMaxValueCount(maxValueCount) |@|
      validateOptions(options)) {
        ParticipantAnnotationType(_, _, _, dateTime, None, _, _, valueType, _, _, required)
      }
  }

  implicit val participantAnnotationTypeWrites: Writes[ParticipantAnnotationType] = (
      (__ \ "studyId").write[StudyId] and
      (__ \ "id").write[AnnotationTypeId] and
      (__ \ "version").write[Long] and
      (__ \ "timeAdded").write[DateTime] and
      (__ \ "timeModified").write[Option[DateTime]] and
      (__ \ "name").write[String] and
      (__ \ "description").write[Option[String]] and
      (__ \ "valueType").write[AnnotationValueType] and
      (__ \ "maxValueCount").write[Option[Int]] and
      (__ \ "options").write[Option[Seq[String]]] and
      (__ \ "required").write[Boolean]
  )(unlift(org.biobank.domain.study.ParticipantAnnotationType.unapply))

}
