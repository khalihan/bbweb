package org.biobank.controllers

import org.biobank.domain.{ DomainValidation, DomainError }
import org.biobank.domain.user.{ UserId, UserHelper }
import org.biobank.service.users.UsersService

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json._
import play.api.cache._
import play.api.Play.current
import scaldi.{Injectable, Injector}

import scalaz._
import scalaz.Scalaz._

/**
 * Security actions that should be used by all controllers that need to protect their actions.
 * Can be composed to fine-tune access control.
 */
trait Security { self: Controller =>

  val AuthTokenCookieKey = "XSRF-TOKEN"
  val AuthTokenHeader = "X-XSRF-TOKEN"
  val AuthTokenUrlKey = "auth"

  sealed case class AuthenticationInfo(token: String, userId: UserId)

  /*
   * Checks that the token is:
   *
   *  - present in the cookie header of the request,
   *
   *  - either in the header or in the query string,
   *
   *  - matches a token already stored in the play cache
   */
  private def validateToken[A]
    (request: Request[A])
    (implicit usersService: UsersService)
      : DomainValidation[AuthenticationInfo] = {
     request.cookies.get(AuthTokenCookieKey).fold {
       DomainError("Invalid XSRF Token cookie").failNel[AuthenticationInfo]
     } { xsrfTokenCookie =>
       request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey)).fold {
         DomainError("No token").failNel[AuthenticationInfo]
       } { token =>
         if (!xsrfTokenCookie.value.equals(token)) {
           DomainError("Token mismatch").failNel[AuthenticationInfo]
         } else {
           Cache.getAs[UserId](token).fold {
             DomainError("invalid token").failNel[AuthenticationInfo]
           } { userId =>
             for {
               user       <- usersService.getUser(userId.id)
               activeUser <- UserHelper.isUserActive(user)
               auth       <- AuthenticationInfo(token, userId).successNel
             } yield auth
           }
         }
       }
     }
  }

  /**
   * Ensures that the request has the proper authorization.
   *
   */
  def AuthAction[A]
    (p: BodyParser[A] = parse.anyContent)
    (f: String => UserId => Request[A] => Result)
    (implicit usersService: UsersService)
      : Action[A] =
    Action(p) { implicit request =>
      validateToken(request).fold(
        err => Unauthorized(Json.obj("status" ->"error", "message" -> err.list.mkString(", "))),
        authInfo => f(authInfo.token)(authInfo.userId)(request))
    }

  /**
   * Ensures that the request has the proper authorization.
   *
   */
  def AuthActionAsync[A]
    (p: BodyParser[A] = parse.anyContent)
    (f: String => UserId => Request[A] => Future[Result])
    (implicit usersService: UsersService) =
    Action.async(p) { implicit request =>
      validateToken(request).fold(
        err => Future.successful(Unauthorized(Json.obj(
          "status" ->"error",
          "message" -> err.list.mkString(", ")))),
        authInfo => f(authInfo.token)(authInfo.userId)(request))
    }

}
