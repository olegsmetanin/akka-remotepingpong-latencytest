package sample.remote.pingpong

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import com.typesafe.config.Config
import akka.actor._
import akka.dispatch._

//https://gist.github.com/patriknw/5786787

object MetricsMailboxExtension extends ExtensionId[MetricsMailboxExtension] with ExtensionIdProvider {
  def lookup = this

  def createExtension(s: ExtendedActorSystem) = new MetricsMailboxExtension(s)
}

class MetricsMailboxExtension(val system: ExtendedActorSystem) extends Extension {
  private val mailboxes = new ConcurrentHashMap[ActorRef, MetricsMailbox]

  def register(actorRef: ActorRef, mailbox: MetricsMailbox): Unit =
    mailboxes.put(actorRef, mailbox)

  def unregister(actorRef: ActorRef): Unit = mailboxes.remove(actorRef)

  def mailboxSize(ref: ActorRef): Int =
    mailboxes.get(ref) match {
      case null ⇒ 0
      case mailbox ⇒ mailbox.numberOfMessages
    }
}

class MetricsMailboxType(settings: ActorSystem.Settings, config: Config) extends MailboxType {
  override def create(owner: Option[ActorRef], system: Option[ActorSystem]) = (owner, system) match {
    case (Some(o), Some(s)) ⇒
      val mailbox = new MetricsMailbox(o, s)
      MetricsMailboxExtension(s).register(o, mailbox)
      mailbox
    case _ ⇒ throw new Exception("no mailbox owner or system given")
  }
}

class MetricsMailbox(owner: ActorRef, system: ActorSystem) extends UnboundedMailbox.MessageQueue {

  private val queueSize = new AtomicInteger

  override def dequeue(): Envelope = {
    val x = super.dequeue()
    if (x ne null) queueSize.decrementAndGet
    x
  }

  override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    super.enqueue(receiver, handle)
    queueSize.incrementAndGet
  }

  override def numberOfMessages: Int = queueSize.get

  override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    super.cleanUp(owner, deadLetters)
    MetricsMailboxExtension(system).unregister(owner)
  }
}