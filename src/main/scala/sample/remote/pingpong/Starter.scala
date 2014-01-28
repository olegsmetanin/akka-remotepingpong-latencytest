/**
 * Copyright (C) 2014 Oleg Smetanin
 */
package sample.remote.pingpong


import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory


object PingPongParam {
  var concurrency = 100
  var warmupRequest = 1000
  var maxRequest = 10000
  var pongHostname = "localhost"
}

object Starter {

  def main(args: Array[String]): Unit = {

    args.toList match {
      case "ping" :: host :: conc :: warmup :: max :: xs => {
        PingPongParam.pongHostname = host
        PingPongParam.concurrency = conc.toInt
        PingPongParam.warmupRequest = warmup.toInt
        PingPongParam.maxRequest = max.toInt
        startPingSystem()
      }
      case "pong" :: xs => {
        startPongSystem()
      }
      case _ => println("nonvalid arg")
    }
  }

  def config(ip: String, port: Int) = {
    s"""
     akka {

       actor {
         provider = "akka.remote.RemoteActorRefProvider"
       }

       remote {
         netty.tcp {
           hostname = "$ip"
           port = $port
         }
       }


     }

    metrics-dispatcher {
      mailbox-type = "sample.remote.pingpong.MetricsMailboxType"
    }


     """
  }


  val ip = java.net.InetAddress.getLocalHost().getHostAddress()

  def startPingSystem(): Unit = {

    val customConf = ConfigFactory.parseString(config(ip, 2552))

    val system = ActorSystem("PingSystem", ConfigFactory.load(customConf))
    val pingActor = system.actorOf(Props(classOf[PingActor]).withDispatcher("metrics-dispatcher"), "pingActor")

    println("Started PingSystem")

    import system.dispatcher
    system.scheduler.scheduleOnce(1.second) {
      println("Start warming")
      pingActor ! PingThemAll
    }

  }


  def startPongSystem(): Unit = {

    val customConf = ConfigFactory.parseString(config(ip, 2553))

    val system = ActorSystem("PongSystem", ConfigFactory.load(customConf))
    val pongActor = system.actorOf(Props(classOf[PongActor]).withDispatcher("metrics-dispatcher"), "pongActor")

    println("Started PongSystem on " + ip)
    println( s"""
Now run sbt "run-main sample.remote.pingpong.Starter ping $ip 10 10000 1000000" on other computer
     """)

    pongActor ! StartUp
  }

}
