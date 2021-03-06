package org.biobank.controllers.study

import org.biobank.fixture._
import org.biobank.domain.study.{ Study, ProcessingType }
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

class ProcessingTypeControllerSpec extends ControllerFixture {
  import TestGlobal._

  val log = LoggerFactory.getLogger(this.getClass)

  val nameGenerator = new NameGenerator(this.getClass)

  def uri(study: Study): String = s"/studies/${study.id.id}/proctypes"

  def uri(study: Study, procType: ProcessingType): String =
    uri(study) + s"/${procType.id.id}"

  def uriWithQuery(study: Study, procType: ProcessingType): String =
    uri(study) + s"?procTypeId=${procType.id.id}"

  def uri(study: Study, procType: ProcessingType, version: Long): String =
    uri(study, procType) + s"/${version}"

  private def procTypeToAddCmdJson(procType: ProcessingType) = {
    Json.obj(
      "studyId"     -> procType.studyId.id,
      "name"        -> procType.name,
      "description" -> procType.description,
      "enabled"     -> procType.enabled
    )
  }

  private def procTypeToUpdateCmdJson(procType: ProcessingType) = {
    procTypeToAddCmdJson(procType) ++ Json.obj(
      "id"              -> procType.id.id,
      "expectedVersion" -> Some(procType.version)
    )
  }

  def addOnNonDisabledStudy(study: Study) {
    studyRepository.put(study)

    val sg = factory.createSpecimenGroup
    specimenGroupRepository.put(sg)

    val procType = factory.createProcessingType

    val json = makeRequest(
      POST,
      uri(study),
      BAD_REQUEST,
      procTypeToAddCmdJson(procType))

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  def updateOnNonDisabledStudy(study: Study) {
    studyRepository.put(study)

    val procType = factory.createProcessingType
    processingTypeRepository.put(procType)

    val procType2 = factory.createProcessingType

    val json = makeRequest(
      PUT,
      uri(study, procType2),
      BAD_REQUEST,
      procTypeToUpdateCmdJson(procType2))

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  def removeOnNonDisabledStudy(study: Study) {
    studyRepository.put(study)

    val procType = factory.createProcessingType
    processingTypeRepository.put(procType)

    val json = makeRequest(
      DELETE,
      uri(study, procType, procType.version),
      BAD_REQUEST)

    (json \ "status").as[String] must include ("error")
    (json \ "message").as[String] must include ("is not disabled")
  }

  "Processing Type REST API" when {

    "GET /studies/proctypes" must {
      "list none" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val json = makeRequest(GET, uri(study))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]
        jsonList must have size 0
      }

      "list a single processing type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val json = makeRequest(GET, uri(study))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]
        jsonList must have size 1
        compareObj(jsonList(0), procType)
      }

      "get a single processing type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val json = makeRequest(GET, uriWithQuery(study, procType)).as[JsObject]
        (json \ "status").as[String] must include ("success")
        val jsonObj = (json \ "data").as[JsObject]
        compareObj(jsonObj, procType)
      }

      "list multiple processing types" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val proctypes = List(factory.createProcessingType, factory.createProcessingType)

        proctypes map { procType => processingTypeRepository.put(procType) }

        val json = makeRequest(GET, uri(study))
        (json \ "status").as[String] must include ("success")
        val jsonList = (json \ "data").as[List[JsObject]]

        jsonList must have size proctypes.size
          (jsonList zip proctypes).map { item => compareObj(item._1, item._2) }
        ()
      }

      "fail for an invalid study ID" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy

        val json = makeRequest(GET, uri(study), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("invalid study id")
      }

      "fail for an invalid study ID when using an processing type id" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        val procType = factory.createProcessingType

        val json = makeRequest(GET, uriWithQuery(study, procType), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("invalid study id")
      }

      "fail for an invalid processing type id" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType

        val json = makeRequest(GET, uriWithQuery(study, procType), BAD_REQUEST)
        (json \ "status").as[String] must include ("error")
        (json \ "message").as[String] must include ("processing type does not exist")
      }
    }

    "POST /studies/proctypes" must {
      "add a processing type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        val json = makeRequest(
          POST,
          uri(study),
          json = procTypeToAddCmdJson(procType))

        (json \ "status").as[String] must include ("success")
      }
    }

    "POST /studies/proctypes" must {
      "not add a processing type to an enabled study" in new App(fakeApp) {
        doLogin
        addOnNonDisabledStudy(
          factory.createDisabledStudy.enable(1, 1) | fail)
      }
    }

    "POST /studies/proctypes" must {
      "not add a processing type to an retired study" in new App(fakeApp) {
        doLogin
        addOnNonDisabledStudy(
          factory.createDisabledStudy.retire | fail)
      }
    }

    "PUT /studies/proctypes" must {
      "update a processing type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val procType2 = factory.createProcessingType.copy(
          id = procType.id,
          version = procType.version
        )

        val json = makeRequest(
          PUT,
          uri(study, procType2),
          json = procTypeToUpdateCmdJson(procType2))

        (json \ "status").as[String] must include ("success")
      }
    }

    "PUT /studies/proctypes" must {
      "not update a processing type on an enabled study" in new App(fakeApp) {
        doLogin
        updateOnNonDisabledStudy(
          factory.createDisabledStudy.enable(1, 1) | fail)
      }
    }

    "PUT /studies/proctypes" must {
      "not update a processing type on an retired study" in new App(fakeApp) {
        doLogin
        updateOnNonDisabledStudy(
          factory.createDisabledStudy.retire | fail)
      }
    }

    "DELETE /studies/proctypes" must {
      "remove a processing type" in new App(fakeApp) {
        doLogin
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        val procType = factory.createProcessingType
        processingTypeRepository.put(procType)

        val json = makeRequest(DELETE, uri(study, procType, procType.version))

        (json \ "status").as[String] must include ("success")
      }
    }

    "DELETE /studies/proctypes" must {
      "not remove a processing type on an enabled study" in new App(fakeApp) {
        doLogin
        removeOnNonDisabledStudy(
          factory.createDisabledStudy.enable(1, 1) | fail)
      }
    }

    "DELETE /studies/proctypes" must {
      "not remove a processing type on an retired study" in new App(fakeApp) {
        doLogin
        removeOnNonDisabledStudy(
          factory.createDisabledStudy.retire | fail)
      }
    }
  }

}
