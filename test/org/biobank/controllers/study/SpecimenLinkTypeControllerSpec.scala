package org.biobank.controllers.study

import org.biobank.fixture._
import org.biobank.domain.study.{ Study, ProcessingType, SpecimenLinkType }
import org.biobank.fixture.ControllerFixture
import org.biobank.domain.JsonHelper._

import play.api.test.Helpers._
import play.api.test.WithApplication
import play.api.libs.json._
import org.scalatest.Tag
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import com.typesafe.plugin._
import play.api.Play.current
import org.scalatestplus.play._

class SpecimenLinkTypeControllerSpec extends ControllerFixture {
  import TestGlobal._

  val log = LoggerFactory.getLogger(this.getClass)

  val nameGenerator = new NameGenerator(this.getClass)

  def uri(procType: ProcessingType): String = s"/studies/${procType.id.id}/sltypes"

  def uri(procType: ProcessingType, slType: SpecimenLinkType): String =
    uri(procType) + s"/${slType.id.id}"

  def uriWithQuery(procType: ProcessingType, slType: SpecimenLinkType): String =
    uri(procType) + s"?slTypeId=${slType.id.id}"

  def uri(procType: ProcessingType, slType: SpecimenLinkType, version: Long): String =
    uri(procType, slType) + s"/${version}"

  private def annotTypeJson(slType: SpecimenLinkType) = {
    if (!slType.annotationTypeData.isEmpty) {
      Json.obj(
        "annotationTypeData"    -> Json.arr(
          Json.obj(
            "annotationTypeId"  -> slType.annotationTypeData(0).annotationTypeId,
            "required"          -> slType.annotationTypeData(0).required
          ))
      )
    } else {
      Json.obj(
        "annotationTypeData"    ->  Json.arr()
      )
    }
  }

  private def slTypeToAddCmdJson(slType: SpecimenLinkType) = {
    Json.obj(
      "processingTypeId"      -> slType.processingTypeId.id,
      "expectedInputChange"   -> slType.expectedInputChange,
      "expectedOutputChange"  -> slType.expectedOutputChange,
      "inputCount"            -> slType.inputCount,
      "outputCount"           -> slType.outputCount,
      "inputGroupId"          -> slType.inputGroupId.id,
      "outputGroupId"         -> slType.outputGroupId.id,
      "inputContainerTypeId"  -> slType.inputContainerTypeId.map(_.id),
      "outputContainerTypeId" -> slType.outputContainerTypeId.map(_.id)
    ) ++ annotTypeJson(slType)
  }

  private def slTypeToUpdateCmdJson(slType: SpecimenLinkType) = {
    slTypeToAddCmdJson(slType) ++ Json.obj(
      "id"              -> slType.id.id,
      "expectedVersion" -> Some(slType.version)
    )
  }

  def addOnNonDisabledStudy(study: Study, procType: ProcessingType) {
    studyRepository.put(study)
    processingTypeRepository.put(procType)

    val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
    specimenGroupRepository.put(inputSg)
    specimenGroupRepository.put(outputSg)
    specimenLinkTypeRepository.put(slType)

    val json = makeRequest(
      POST,
      uri(procType),
      BAD_REQUEST,
      slTypeToAddCmdJson(slType))

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  def updateOnNonDisabledStudy(
    study: Study,
    procType: ProcessingType) {
    studyRepository.put(study)
    processingTypeRepository.put(procType)

    val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
    specimenGroupRepository.put(inputSg)
    specimenGroupRepository.put(outputSg)
    specimenLinkTypeRepository.put(slType)

    val slType2 = factory.createSpecimenLinkType.copy(id = slType.id)

    val json = makeRequest(
      PUT,
      uri(procType, slType),
      BAD_REQUEST,
      slTypeToUpdateCmdJson(slType2))

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  def removeOnNonDisabledStudy(
    study: Study,
    procType: ProcessingType) {
    studyRepository.put(study)
    processingTypeRepository.put(procType)

    val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
    specimenGroupRepository.put(inputSg)
    specimenGroupRepository.put(outputSg)
    specimenLinkTypeRepository.put(slType)

    val json = makeRequest(DELETE, uri(procType, slType, slType.version), BAD_REQUEST)

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  "SpecimenLink Type REST API" when {

    "GET /studies/sltypes" must {
      "list none" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val json = makeRequest(GET, uri(procType))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]
        jsonList must have size 0
      }

      "list a single specimen link type" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
        specimenGroupRepository.put(inputSg)
        specimenGroupRepository.put(outputSg)
        specimenLinkTypeRepository.put(slType)

        val json = makeRequest(GET, uri(procType))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]
        jsonList must have size 1
        compareObj(jsonList(0), slType)
      }

      "get a single specimen link type" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
        specimenGroupRepository.put(inputSg)
        specimenGroupRepository.put(outputSg)
        specimenLinkTypeRepository.put(slType)

        val json = makeRequest(GET, uriWithQuery(procType, slType))
        (json \ "status").as[String] must include ("success")
        val jsonObj = (json \ "data").as[JsObject]
        compareObj(jsonObj, slType)
      }

      "list multiple specimen link types" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val sltypes = List(factory.createSpecimenLinkType, factory.createSpecimenLinkType)

        sltypes map { slType => specimenLinkTypeRepository.put(slType) }

        val json = makeRequest(GET, uri(procType))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]

        jsonList must have size sltypes.size
          (jsonList zip sltypes).map { item => compareObj(item._1, item._2) }
        ()
      }

      "fail for invalid processing type id" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType

        val json = makeRequest(GET, uri(procType), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("invalid processing type id")
      }

      "fail for an invalid study ID when using an specimen link type id" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        val slType = factory.createSpecimenLinkType

        val json = makeRequest(GET, uriWithQuery(procType, slType), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("invalid processing type id")
      }

      "fail for an invalid specimen link type id" in new App(fakeApp) {
        doLogin
        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val slType = factory.createSpecimenLinkType

        val json = makeRequest(GET, uriWithQuery(procType, slType), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("specimen link type does not exist")
      }

    }

    "POST /studies/sltypes" must {
      "add a specimen link type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
        specimenGroupRepository.put(inputSg)
        specimenGroupRepository.put(outputSg)

        val json = makeRequest(POST, uri(procType), json = slTypeToAddCmdJson(slType))

        (json \ "status").as[String] must include ("success")
      }

      "not add a specimen link type to an enabled study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.enable(1, 1) | fail)
        addOnNonDisabledStudy(study, factory.createProcessingType)
      }

      "not add a specimen link type to an retired study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.retire | fail)
        addOnNonDisabledStudy(study, factory.createProcessingType)
      }
    }

    "PUT /studies/sltypes" must {
      "must update a specimen link type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
        specimenGroupRepository.put(inputSg)
        specimenGroupRepository.put(outputSg)
        specimenLinkTypeRepository.put(slType)

        val slType2 = factory.createSpecimenLinkType.copy(
          id = slType.id,
          version = slType.version,
          inputGroupId = slType.inputGroupId,
          outputGroupId = slType.outputGroupId
        )

        val json = makeRequest(PUT, uri(procType, slType2), json = slTypeToUpdateCmdJson(slType2))
        (json \ "status").as[String] must include ("success")
      }

      "not update a specimen link type on an enabled study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.enable(1, 1) | fail)
        updateOnNonDisabledStudy(study, factory.createProcessingType)
      }

      "not update a specimen link type on an retired study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.retire | fail)
        updateOnNonDisabledStudy(study, factory.createProcessingType)
      }
    }

    "DELETE /studies/sltypes" must {
      "remove a specimen link type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val (slType, inputSg, outputSg) = factory.createSpecimenLinkTypeAndSpecimenGroups
        specimenGroupRepository.put(inputSg)
        specimenGroupRepository.put(outputSg)
        specimenLinkTypeRepository.put(slType)

        val json = makeRequest(DELETE, uri(procType, slType, slType.version))

        (json \ "status").as[String] must include ("success")
      }

      "not remove a specimen link type on an enabled study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.enable(1, 1) | fail)
        removeOnNonDisabledStudy(study, factory.createProcessingType)
      }

      "not remove a specimen link type on an retired study" in new App(fakeApp) {
        doLogin
        val study = studyRepository.put(
          factory.createDisabledStudy.retire | fail)
        removeOnNonDisabledStudy(study, factory.createProcessingType)
      }
    }
  }

}
