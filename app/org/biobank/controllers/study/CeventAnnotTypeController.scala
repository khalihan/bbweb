package org.biobank.controllers.study

import org.biobank.controllers._
import org.biobank.service._
import org.biobank.service.{ ServiceComponent, ServiceComponentImpl }
import org.biobank.service.json.Events._
import org.biobank.service.json.Study._
import org.biobank.service.json.StudyAnnotationType._
import org.biobank.service.json.CollectionEventAnnotationType._
import org.biobank.infrastructure.command.StudyCommands._
import org.biobank.domain.study._
import org.biobank.domain.AnnotationValueType._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.{ Logger, Play }
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Results._

import scalaz._
import Scalaz._

object CeventAnnotTypeController extends BbwebController  {

  private def studyService = Play.current.plugin[BbwebPlugin].map(_.studyService).getOrElse {
    sys.error("Bbweb plugin is not registered")
  }

  def list(studyId: String) = AuthAction(parse.empty) { token => userId => implicit request =>
    Logger.debug(s"CeventAnnotTypeController.list: studyId: $studyId")
    Ok(Json.toJson(studyService.collectionEventAnnotationTypesForStudy(studyId).toList))
  }

  def addAnnotationType = CommandAction { cmd: AddCollectionEventAnnotationTypeCmd => implicit userId =>
    val future = studyService.addCollectionEventAnnotationType(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"error", "message" -> err.list.mkString(", "))),
        event => Ok(eventToJsonReply(event))
      )
    }
  }

  def updateAnnotationType(id: String) = CommandAction { cmd: UpdateCollectionEventAnnotationTypeCmd => implicit userId =>
    val future = studyService.updateCollectionEventAnnotationType(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"error", "message" -> err.list.mkString(", "))),
        event => Ok(eventToJsonReply(event))
      )
    }
  }

  def removeAnnotationType(id: String) = CommandAction { cmd: RemoveCollectionEventAnnotationTypeCmd => implicit userId =>
    val future = studyService.removeCollectionEventAnnotationType(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"error", "message" -> err.list.mkString(", "))),
        event => Ok(eventToJsonReply(event))
      )
    }
  }

}
