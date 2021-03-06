package org.biobank.service.users

import org.biobank.service._
import org.biobank.domain._
import org.biobank.domain.user._
import org.biobank.infrastructure.command.UserCommands._
import org.biobank.infrastructure.event.UserEvents._

import akka.actor.{ ActorSystem, ActorRef }
import akka.persistence.{ RecoveryCompleted, SnapshotOffer }
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scaldi.akka.AkkaInjectable
import scaldi.{Injectable, Injector}

import scalaz._
import scalaz.Scalaz._

/**
  * Handles the commands to configure users.
  */
class UsersProcessor(implicit inj: Injector) extends Processor with Injectable {

  case class PasswordInfo(password: String, salt: String)

  case class SnapshotState(users: Set[User])

  val userRepository = inject [UserRepository]

  val passwordHasher = inject [PasswordHasher]

  override def persistenceId = "user-processor-id"

  def encryptPassword(user: ActiveUser, newPlainPassword: String): PasswordInfo = {
    val newSalt = passwordHasher.generateSalt
    val newPwd = passwordHasher.encrypt(newPlainPassword, newSalt)
    PasswordInfo(newPwd, newSalt)
  }

  val receiveRecover: Receive = {
    case wevent: WrappedEvent[_] =>
      wevent.event match {
        case event: UserRegisteredEvent      => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserActivatedEvent       => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserNameUpdatedEvent     => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserEmailUpdatedEvent    => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserPasswordUpdatedEvent => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserLockedEvent          => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserUnlockedEvent        => recoverEvent(event, wevent.userId, wevent.dateTime)
        case event: UserPasswordResetEvent   => recoverEvent(event, wevent.userId, wevent.dateTime)

        case event => throw new IllegalStateException(s"event not handled: $event")
      }

    case SnapshotOffer(_, snapshot: SnapshotState) =>
      snapshot.users.foreach(i => userRepository.put(i))

    case event: RecoveryCompleted =>

    case event => throw new IllegalStateException(s"event not handled: $event")
  }

  val receiveCommand: Receive = {
    case procCmd: WrappedCommand =>
      implicit val userId = procCmd.userId
      procCmd.command match {
        case cmd: RegisterUserCmd       => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: ActivateUserCmd       => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: UpdateUserNameCmd     => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: UpdateUserEmailCmd    => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: UpdateUserPasswordCmd => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: ResetUserPasswordCmd  => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: LockUserCmd           => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }
        case cmd: UnlockUserCmd         => process(validateCmd(cmd)){ wevent => recoverEvent(wevent.event, wevent.userId, wevent.dateTime) }

        case cmd => log.error(s"wrapped command handled: $cmd")
      }

    case "snap" =>
      saveSnapshot(SnapshotState(userRepository.getValues.toSet))
      stash()

    case cmd => log.error(s"UsersProcessor: message not handled: $cmd")
  }

  def validateCmd(cmd: RegisterUserCmd): DomainValidation[UserRegisteredEvent] = {
    val userId = userRepository.nextIdentity

    if (userRepository.getByKey(userId).isSuccess) {
      throw new IllegalStateException(s"user with id already exsits: $userId")
    }

    val salt = passwordHasher.generateSalt
    val encryptedPwd = passwordHasher.encrypt(cmd.password, salt)
    for {
      emailAvailable <- emailAvailable(cmd.email)
      user <- RegisteredUser.create(
        userId, -1L, DateTime.now, cmd.name, cmd.email, encryptedPwd, salt, cmd.avatarUrl)
      event <- UserRegisteredEvent(
        user.id.id, user.name, user.email, encryptedPwd, salt, user.avatarUrl).success
    } yield event
  }

  def validateCmd(cmd: ActivateUserCmd): DomainValidation[UserActivatedEvent] = {
    val timeNow = DateTime.now
    val v = updateRegistered(cmd) { u => u.activate }
    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserActivatedEvent(user.id.id, user.version).success
    )
  }

  def validateCmd(cmd: UpdateUserNameCmd): DomainValidation[UserNameUpdatedEvent] = {
    val timeNow = DateTime.now
    val v = updateActive(cmd) { _.updateName(cmd.name) }
    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserNameUpdatedEvent(user.id.id, user.version, user.name).success
    )
  }

  def validateCmd(cmd: UpdateUserEmailCmd): DomainValidation[UserEmailUpdatedEvent] = {
    val timeNow = DateTime.now

    val v = updateActive(cmd) { user =>
      for {
        emailAvailable <- emailAvailable(cmd.email, user.id)
        updatedUser <- user.updateEmail(cmd.email)
      } yield updatedUser
    }

    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserEmailUpdatedEvent(user.id.id, user.version, user.email).success
    )
  }

  def validateCmd(cmd: UpdateUserPasswordCmd): DomainValidation[UserPasswordUpdatedEvent] = {
    val timeNow = DateTime.now

    val v = updateActive(cmd) { user =>
      if (passwordHasher.valid(user.password, user.salt, cmd.oldPassword)) {
        val passwordInfo = encryptPassword(user, cmd.newPassword)
        user.updatePassword(passwordInfo.password, passwordInfo.salt)
      } else {
        DomainError("invalid password").failNel
      }
    }

    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserPasswordUpdatedEvent(user.id.id, user.version, user.password, user.salt).success
    )
  }

  // only active users can request a password reset
  def validateCmd(cmd: ResetUserPasswordCmd): DomainValidation[UserPasswordResetEvent] = {
    val timeNow = DateTime.now

    userRepository.getByEmail(cmd.email).fold(
      err => err.failure,
      user => {
        user match {
          case activeUser: ActiveUser =>
            val plainPassword = Utils.randomString(8)
            val passwordInfo = encryptPassword(activeUser, plainPassword)
            activeUser.updatePassword(passwordInfo.password, passwordInfo.salt).fold(
              err => err.failure,
              updatedUser => {
                EmailService.passwordResetEmail(user.email, plainPassword)
                UserPasswordResetEvent(
                  updatedUser.id.id, updatedUser.version, updatedUser.password,
                  updatedUser.salt).success
              }
            )
          case user => s"$user for $cmd is not active".failNel
        }
      }
    )
  }

  def validateCmd(cmd: LockUserCmd): DomainValidation[UserLockedEvent] = {
    val timeNow = DateTime.now
    val v = updateActive(cmd) { u => u.lock }
    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserLockedEvent(user.id.id, user.version).success
    )
  }

  def validateCmd(cmd: UnlockUserCmd): DomainValidation[UserUnlockedEvent] = {
    val timeNow = DateTime.now
    val v = updateLocked(cmd) { u => u.unlock }
    v.fold(
      err => DomainError(s"error $err occurred on $cmd").failNel,
      user => UserUnlockedEvent(user.id.id, user.version).success
    )
  }

  def updateUser[T <: User]
    (cmd: UserCommand)
    (fn: User => DomainValidation[T])
      : DomainValidation[T] = {
    for {
      user         <- userRepository.getByKey(UserId(cmd.id))
      validVersion <- user.requireVersion(cmd.expectedVersion)
      updatedUser  <- fn(user)
    } yield updatedUser
  }

  def updateRegistered[T <: User]
    (cmd: UserCommand)
    (fn: RegisteredUser => DomainValidation[T])
      : DomainValidation[T] = {
    updateUser(cmd) {
      case user: RegisteredUser => fn(user)
      case user => s"$user for $cmd is not registered".failNel
    }
  }

  def updateActive[T <: User]
    (cmd: UserCommand)
    (fn: ActiveUser => DomainValidation[T])
      : DomainValidation[T] = {
    updateUser(cmd) {
      case user: ActiveUser => fn(user)
      case user => s"$user for $cmd is not active".failNel
    }
  }

  def updateLocked[T <: User]
    (cmd: UserCommand)
    (fn: LockedUser => DomainValidation[T])
      : DomainValidation[T] = {
    updateUser(cmd) {
      case user: LockedUser => fn(user)
      case user => s"$user for $cmd is not locked".failNel
    }
  }

  def recoverEvent(event: UserRegisteredEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.put(RegisteredUser(UserId(event.id), 0L, dateTime, None, event.name,
      event.email, event.password, event.salt, event.avatarUrl))
    ()
  }

  def recoverEvent(event: UserActivatedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getRegistered(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"activating user from event failed: $err"),
      u =>
      userRepository.put(ActiveUser(u.id, event.version, u.timeAdded, Some(dateTime),
        u.name, u.email, u.password, u.salt, u.avatarUrl))
    )
    ()
  }

  def recoverEvent(event: UserNameUpdatedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getActive(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"updating name on user from event failed: $err"),
      u => userRepository.put(u.copy(
        version = event.version, name = event.name, timeModified = Some(dateTime)))
    )
    ()
  }

  def recoverEvent(event: UserEmailUpdatedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getActive(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"updating email on user from event failed: $err"),
      u => userRepository.put(u.copy(
        version = event.version, email = event.email, timeModified = Some(dateTime)))
    )
    ()
  }

  def recoverEvent(event: UserPasswordUpdatedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getActive(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"updating password on user from event failed: $err"),
      u => userRepository.put(u.copy(
        version = event.version, password = event.password, salt = event.salt,
        timeModified = Some(dateTime)))
    )
    ()
  }

  def recoverEvent(event: UserPasswordResetEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getActive(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"resetting password on user from event failed: $err"),
      u => userRepository.put(u.copy(
        version = event.version, password = event.password, salt = event.salt,
        timeModified = Some(dateTime)))
    )
    ()
  }

  def recoverEvent(event: UserLockedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getActive(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"locking user from event failed: $err"),
      u => userRepository.put(LockedUser(
        u.id, event.version, u.timeAdded, Some(dateTime), u.name, u.email, u.password, u.salt,
        u.avatarUrl))
    )
    ()
  }

  def recoverEvent(event: UserUnlockedEvent, userId: Option[UserId], dateTime: DateTime): Unit = {
    log.debug(s"recoverEvent: $event")
    userRepository.getLocked(UserId(event.id)).fold(
      err => throw new IllegalStateException(s"unlocking user from event failed: $err"),
      u => userRepository.put(ActiveUser(
        u.id, event.version, u.timeAdded, Some(dateTime), u.name, u.email, u.password, u.salt,
        u.avatarUrl))
    )
    ()
  }

  /** Searches the repository for a matching item.
    */
  protected def emailAvailableMatcher(
    email: String)(matcher: User => Boolean): DomainValidation[Boolean] = {
    val exists = userRepository.getValues.exists { item =>
      matcher(item)
    }
    if (exists) {
      DomainError(s"user with email already exists: $email").failNel
    } else {
      true.success
    }
  }

  val errMsgNameExists = "user with email already exists"

  private def emailAvailable(email: String): DomainValidation[Boolean] = {
    emailAvailableMatcher(email){ item =>
      item.email.equals(email)
    }
  }

  private def emailAvailable(email: String, excludeUserId: UserId): DomainValidation[Boolean] = {
    emailAvailableMatcher(email){ item =>
      item.email.equals(email) && (item.id != excludeUserId)
    }
  }

}
