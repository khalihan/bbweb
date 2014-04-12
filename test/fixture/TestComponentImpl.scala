package fixture

import org.biobank.service._
import org.biobank.domain._

import play.api.Mode
import play.api.Mode._
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import org.slf4j.LoggerFactory
import akka.actor._
import com.typesafe.config.ConfigFactory
import com.mongodb.casbah.Imports._
import scala.language.postfixOps

import scalaz._
import scalaz.Scalaz._

trait TestComponentImpl extends TopComponent with ServiceComponentImpl {

  protected val nameGenerator: NameGenerator

  implicit override val system: ActorSystem = ActorSystem("bbweb-test", TestComponentImpl.config())
  implicit val timeout = Timeout(5 seconds)

  // clear the event store
  MongoConnection()("bbweb-test")("messages").drop
  MongoConnection()("bbweb-test")("snapshots").drop

}

object TestComponentImpl {

  def config() = ConfigFactory.parseString(
    s"""
      |akka.persistence.journal.plugin = "casbah-journal"
      |akka.persistence.snapshot-store.plugin = "casbah-snapshot-store"
      |akka.persistence.journal.max-deletion-batch-size = 3
      |akka.persistence.publish-plugin-commands = on
      |akka.persistence.publish-confirmations = on
      |casbah-journal.mongo-journal-url = "mongodb://localhost/bbweb-test.messages"
      |casbah-journal.mongo-journal-write-concern = "acknowledged"
      |casbah-journal.mongo-journal-write-concern-timeout = 10000
      |casbah-snapshot-store.mongo-snapshot-url = "mongodb://localhost/bbweb-test.snapshots"
      |casbah-snapshot-store.mongo-snapshot-write-concern = "acknowledged"
      |casbah-snapshot-store.mongo-snapshot-write-concern-timeout = 10000
    """.stripMargin)

}
