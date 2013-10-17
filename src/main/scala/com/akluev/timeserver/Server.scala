package com.akluev.timeserver


import akka.actor.{ActorSystem, Props, Actor}
import akka.io.{IO, Tcp}
import akka.util.Timeout
import akka.pattern.ask
import java.net.InetSocketAddress
import java.util.{Date, Calendar}
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global


sealed trait CustomLogWriterMessage
case class WriteToFileLogWriterMessage(msg: String) extends CustomLogWriterMessage
case class GetLogFileLogWriterMessage() extends CustomLogWriterMessage


class Server(adminName_ : String, adminPass_ : String, guestCapacity: Int) extends Actor {

  val logFileName: String = "server.log"
  var serverStartedTime: Date = Calendar.getInstance().getTime
  val fileWriter = ActorSystem().actorOf(Props(classOf[LogFileWriterActor[CustomLogWriterMessage]],logFileName))

  var inUserCounter: Int = 0
  var outUserCounter: Int = 0
  var adminSessionId: String = ""

  var adminName = adminName_
  var adminPass = adminPass_
  var maxGuestsCount: Int = guestCapacity

  import Tcp._
  import context.system
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 23))

  def receive = userSessionHandler orElse tcpSpecificHandler orElse logWriterHandler orElse userRequestHandler orElse commonReceiveHandler

  def userSessionHandler: Receive = {
    case p @ PublishUserMessage(sessionId, address, role) => {
      println("Tyring to publishing user with id: " + sessionId)
      var addUser: Boolean  = false

      role match {
        case GuestRole() => {
          //Check Max Guest Count value
          val adminPresent: Int = if (adminSessionId.isEmpty) 0 else 1

          if ((inUserCounter - outUserCounter - adminPresent) >= maxGuestsCount ) {
            sender ! MaxGuestCountIsReachedMessage
          }
          else {
            addUser = true
          }
        }
        case AdminRole() => {
          //Check if admin is already logged in
          println("Current admin session is " + adminSessionId)
          if (!adminSessionId.isEmpty) {
            //Drop existing Admin
            val adminSessionOption =  context.child(adminSessionId)
            if (adminSessionOption.isDefined) {
              println("existing admin Actor is found. Sending ForceOutRequest")
              adminSessionOption.get ! ForceOutMessage
            }
          }
          adminSessionId = sessionId
          addUser = true
        }
        case _ => {
          println("User has unknown role!")
        }
      }

      if (addUser) {
        inUserCounter += 1
        val currentTime: Date = new Date
        fileWriter ! WriteToFileLogWriterMessage(currentTime.toGMTString + " " + role + "(" + sessionId + " " + address + ") connected. " +
          "Number of existing connections is " + (inUserCounter - outUserCounter ))
        sender ! UserPublishedResponseMessage
      }
    }
    case p @ UnpublishUserMessage(sessionId, address, role) => {
      outUserCounter += 1
      val currentTime: Date = new Date
      fileWriter ! WriteToFileLogWriterMessage(currentTime.toGMTString + " " + role + "(" + sessionId + " " + address + ") disconnected. " +
        "Number of existing connections is " + (inUserCounter - outUserCounter))
    }
  }

  def tcpSpecificHandler: Receive = {
    case CommandFailed(_: Bind) => context stop self
    case c @ Connected(remote, local) => {
      val r = remote
      println("User address: " + r)
      val connection = sender
      val sessionId: String = r.getAddress.toString.replace("/","").replace(".", "") + r.getPort
      val handler = context.actorOf(Props(classOf[UserSessionActor], sessionId, r, connection, adminName, adminPass), sessionId)

      connection ! Register(handler)
      println("New client was added: " + sessionId)
    }
  }

  def logWriterHandler: Receive = {
    case m @ WriteToFileLogWriterMessage(msg: String) => {
      fileWriter ! m
    }
  }

  def userRequestHandler: Receive = {
    case StatusRequest => {
      val userSession = sender
      implicit val timeout = Timeout(1)
      //We are trying to get Log File data
      val f = fileWriter ? GetLogFileLogWriterMessage
      f onSuccess {
        case file: File => userSession ! StatusResponse(serverStartedTime, inUserCounter - outUserCounter, inUserCounter, file.getAbsolutePath, file.length())
      }
    }
  }

  def commonReceiveHandler: Receive = {
    case _ => {
      println("Server received some message: ")
    }
  }

}

