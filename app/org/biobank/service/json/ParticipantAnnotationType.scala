package org.biobank.service.json

import org.biobank.infrastructure._
import org.biobank.domain._
import org.biobank.domain.study._
import org.biobank.infrastructure.command.StudyCommands._
import org.biobank.infrastructure.event.StudyEvents._
import org.biobank.domain.AnnotationValueType._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import scala.collection.immutable.Map

object ParticipantAnnotationType {
  import JsonUtils._
  import EnumUtils._
  import Study._
  import StudyAnnotationType._

  implicit val participantAnnotationTypeWrites: Writes[ParticipantAnnotationType] = (
      (__ \ "studyId").write[StudyId] and
      (__ \ "id").write[AnnotationTypeId] and
      (__ \ "version").write[Long] and
      (__ \ "addedDate").write[DateTime] and
      (__ \ "lastUpdateDate").write[Option[DateTime]] and
      (__ \ "name").write[String] and
      (__ \ "description").write[Option[String]] and
      (__ \ "valueType").write[AnnotationValueType] and
      (__ \ "maxValueCount").write[Option[Int]] and
      (__ \ "options").write[Option[Map[String, String]]] and
      (__ \ "required").write[Boolean]
  )(unlift(org.biobank.domain.study.ParticipantAnnotationType.unapply))

  implicit val addParticipantAnnotationTypeCmdReads: Reads[AddParticipantAnnotationTypeCmd] = (
    (__ \ "studyId").read[String](minLength[String](2)) and
      (__ \ "name").read[String](minLength[String](2)) and
      (__ \ "description").readNullable[String] and
      (__ \ "valueType").read[AnnotationValueType] and
      (__ \ "maxValueCount").readNullable[Int] and
      (__ \ "options").readNullable[Map[String, String]] and
      (__ \ "required").read[Boolean]
  )(AddParticipantAnnotationTypeCmd.apply _)

  implicit val updateParticipantAnnotationTypeCmdReads: Reads[UpdateParticipantAnnotationTypeCmd] = (
    (__ \ "studyId").read[String](minLength[String](2)) and
      (__ \ "id").read[String](minLength[String](2)) and
      (__ \ "expectedVersion").readNullable[Long](min[Long](0)) and
      (__ \ "name").read[String](minLength[String](2)) and
      (__ \ "description").readNullable[String] and
      (__ \ "valueType").read[AnnotationValueType] and
      (__ \ "maxValueCount").readNullable[Int] and
      (__ \ "options").readNullable[Map[String, String]] and
      (__ \ "required").read[Boolean]
  )(UpdateParticipantAnnotationTypeCmd.apply _)

  implicit val removeParticipantAnnotationTypeCmdReads: Reads[RemoveParticipantAnnotationTypeCmd] = (
    (__ \ "studyId").read[String](minLength[String](2)) and
      (__ \ "id").read[String](minLength[String](2)) and
      (__ \ "expectedVersion").readNullable[Long](min[Long](0))
  )(RemoveParticipantAnnotationTypeCmd.apply _)

  implicit val participantAnnotationTypeAddedEventWriter: Writes[ParticipantAnnotationTypeAddedEvent] = (
    (__ \ "studyId").write[String] and
      (__ \ "annotationTypeId").write[String] and
      (__ \ "dateTime").write[DateTime] and
      (__ \ "name").write[String] and
      (__ \ "description").writeNullable[String] and
      (__ \ "valueType").write[AnnotationValueType] and
      (__ \ "maxValueCount").writeNullable[Int] and
      (__ \ "options").write[Option[Map[String, String]]] and
      (__ \ "required").write[Boolean]
  )(unlift(ParticipantAnnotationTypeAddedEvent.unapply))

  implicit val participantAnnotationTypeUpdatedEventWriter: Writes[ParticipantAnnotationTypeUpdatedEvent] = (
    (__ \ "studyId").write[String] and
      (__ \ "annotationTypeId").write[String] and
      (__ \ "version").write[Long] and
      (__ \ "dateTime").write[DateTime] and
      (__ \ "name").write[String] and
      (__ \ "description").writeNullable[String] and
      (__ \ "valueType").write[AnnotationValueType] and
      (__ \ "maxValueCount").writeNullable[Int] and
      (__ \ "options").write[Option[Map[String, String]]] and
      (__ \ "required").write[Boolean]
  )(unlift(ParticipantAnnotationTypeUpdatedEvent.unapply))

  implicit val participantAnnotationTypeRemovedEventWriter: Writes[ParticipantAnnotationTypeRemovedEvent] = (
    (__ \ "studyId").write[String] and
      (__ \ "annotationTypeId").write[String]
  )(unlift(ParticipantAnnotationTypeRemovedEvent.unapply))

}