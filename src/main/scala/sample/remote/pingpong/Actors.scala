package sample.remote.pingpong

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.concurrent.duration._
import akka.actor.Actor
import java.util.concurrent.atomic.{AtomicLong}


case class Ping(id: Long, tick: Long)

case class Pong(id: Long, tick: Long, pongRcvMailBoxSize: Int)

case class Save()

case class PingThemAll()

case class StartUp()


class PingActor extends Actor {

  val pingCounter = new AtomicLong(PingPongParam.maxRequest)
  val pongActor = context.actorSelection("akka.tcp://PongSystem@" + PingPongParam.pongHostname + ":2553/user/pongActor")

  private val store = new ConcurrentLinkedHashMap.Builder[Long, (Long, Long, Int, Int)]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()

  sealed trait State

  case class Warming() extends State

  case class Recording() extends State

  case class Waiting() extends State

  var state: State = Warming()
  var recordTime = System.nanoTime()

  def receive = {

    case PingThemAll => {
      for (i <- 0 to PingPongParam.concurrency) {
        pongActor ! Ping(
          pingCounter.incrementAndGet(),
          System.nanoTime()
        )
      }
    }

    case Pong(id: Long, tick: Long, pongRcvMailBoxSize) => {
      (id, state) match {
        case (id, Recording()) if id <= PingPongParam.maxRequest => {
          store.put(id, (tick, System.nanoTime(), pongRcvMailBoxSize, MetricsMailboxExtension(context.system).mailboxSize(self)))
          pongActor ! Ping(
            pingCounter.incrementAndGet(),
            System.nanoTime()
          )
        }
        case (id, Recording()) if id > PingPongParam.maxRequest => {
          state = Waiting()
          // Wait 10 sec after recording and exit
          import scala.concurrent.ExecutionContext.Implicits.global
          context.system.scheduler.scheduleOnce(10.second) {
            println("Start saving")
            pingCounter.set(0)
            self ! Save
          }
        }
        case (id, Warming()) if (id <= PingPongParam.maxRequest + PingPongParam.warmupRequest) => {
          pongActor ! Ping(
            pingCounter.incrementAndGet(),
            System.nanoTime()
          )
        }
        case (id, Warming()) if (id > PingPongParam.maxRequest + PingPongParam.warmupRequest) => {
          state = Waiting()
          // Wait 10 sec after warmup and start pinging again
          import scala.concurrent.ExecutionContext.Implicits.global
          context.system.scheduler.scheduleOnce(10.second) {
            println("Start recording")
            state = Recording()
            pingCounter.set(0)
            recordTime = System.nanoTime()
            self ! PingThemAll
          }
        }
        case _ =>
      }
    }

    case Save => {
      import collection.JavaConversions._
      val pw = new java.io.PrintWriter("stat/stat" + PingPongParam.concurrency.toString + ".csv")
      pw.println("id,time,latency,pongrcvmailboxsize,pingrcvmailboxsize,concurrency")
      store.ascendingMap() foreach {
        case (k, v) => pw.println(
          k.toString + "," +
            ((v._2 - recordTime).toFloat / 1000000000).toString + "," +
            ((v._2 - v._1).toFloat / 1000000).toString + "," +
            v._3.toString + "," +
            v._4.toString + "," +
            PingPongParam.concurrency.toString
        )
      }
      pw.close()
      println("Saved")
      context.system.shutdown
    }

  }

}


class PongActor extends Actor {

  def receive = {
    case StartUp => println("Started " + self.toString)
    case Ping(id: Long, tick: Long) =>
      sender ! Pong(id, tick, MetricsMailboxExtension(context.system).mailboxSize(self))
  }

}
