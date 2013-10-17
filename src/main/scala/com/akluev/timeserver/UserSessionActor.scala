package com.akluev.timeserver

import java.net.InetSocketAddress
import java.util.Date
import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.{PeerClosed, Close, Write, Received}
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global


sealed trait UserSessionMessage
case class PublishUserMessage(sessionId: String, addr: InetSocketAddress, role: Role) extends UserSessionMessage
case class UnpublishUserMessage(sessionId: String, addr: InetSocketAddress, role: Role) extends UserSessionMessage
case class ForceOutMessage() extends UserSessionMessage
case class MaxGuestCountIsReachedMessage() extends UserSessionMessage
case class UserPublishedResponseMessage() extends UserSessionMessage

sealed trait UserCommandsRequests
case class StatusRequest() extends UserCommandsRequests
case class StatusResponse(srvStartedTime: Date, currentConn: Int, allConn: Int, logFile: String, logFileSize: Long) extends UserCommandsRequests
case class ErrorResponse(reason: String) extends UserCommandsRequests


class UserSessionActor(sessionId_ : String, addr : InetSocketAddress, tcpConnection_ : ActorRef, adminName_ : String, adminPass_ : String) extends Actor with Role {

  val sessionId = sessionId_
  val address = addr
  val tcpConnection = tcpConnection_
  val adminName = adminName_
  val adminPass = adminPass_

  var name: String = null
  var password: String = null
  var role: Role = null



  import StrImplicits._

  def receive: Receive = inputNameState

  def inputNameState: Receive = {
    case Received(data) => {
      sender ! Write("Welcome to the TimeServer! \n")

      context become saveNameState
      sender ! Write("name:")
    }
  }

  def saveNameState: Receive = {
    case Received(data) => {
      name = data

      if (name == adminName) {
        sender ! Write("\r\npassword (Leave it empty and press ENTER to log in as guest):")
        context become savePasswordState
      }
      else {
        //Ask server about capacity
        role = new GuestRole()
        implicit val timeout = Timeout(2)
        val f  = context.parent ? PublishUserMessage(sessionId, address, role)
        f onSuccess {
          case MaxGuestCountIsReachedMessage => {
            tcpConnection  ! Write("Sorry, but maximum of guest sessions is reached. Please try again later. \r\n")
            tcpConnection  ! Close
          }
          case _ => {
            context become processState

            tcpConnection ! Write("Hello, " + name + "!")
            tcpConnection ! Write("\r\nYou are logged in as guest\r\n" + name + ">>")
          }
        }

      }
    }
  }

  def savePasswordState: Receive = {
    case Received(data) => {
      password = data
      password match {
        case `adminPass` => {
          sender ! Write("\r\nYou are logged in as admin\r\n")
          context become processState
          role = new AdminRole()
          sender ! Write("admin>>")
          context.parent ! PublishUserMessage(sessionId, address, role)
        }
        case "" => {
          context become saveNameState
          sender ! Write("name:")
        }
        case _  => {
          sender ! Write("\r\nPassword is wrong! Please try again or leave it empty to log in as guest\r\n")
          sender ! Write("\r\npassword:")
        }
      }
    }
  }

  def processState: Receive = {
    case ForceOutMessage => {
      tcpConnection  ! Write("You has been forced out by another Admin! \r\n")
      context.parent ! UnpublishUserMessage(sessionId, address, role)
      tcpConnection  ! Close
    }
    case Received(data) => {
      val command = ByteStr2Str(data)
      command match {
        case msg: String if msg.startsWith("exit") => {
          context.parent ! UnpublishUserMessage(sessionId, address, role)
          sender ! Close
        }
        case msg: String if msg.startsWith("time") => {
          val time = new Date().toGMTString
          sendProcessMessage("Server time is " + time)

        }
        case msg: String if role.isInstanceOf[AdminRole] && msg.startsWith("stop server") => {
          println("'stop' message is received. Halt down the server")
          System.exit(0)

        }

        case msg: String if role.isInstanceOf[AdminRole] && msg.startsWith("status") => {
          implicit val timeout = Timeout(5)
          val f = context.parent ? StatusRequest
          val tcpActor = sender
          f onSuccess {
            case StatusResponse(srvStartedTime, currentConn, allConn, logFile, logFileSize) => {
              println("received status response")
              val statusOutput: String = "Server started:       " + srvStartedTime + "\r\n" +
                                         "Existing connections: " + currentConn + "\r\n" +
                                         "All connections:      " + allConn + "\r\n" +
                                         "Log file:             " + logFile + " (" + logFileSize + " bytes)\r\n"
              sendProcessMessage(tcpActor, "Server status: \r\n" + statusOutput)
            }
            case _ => {
              println("Unknown status response")
            }
          }
        }
        case msg: String if msg.isEmpty => {
          sendProcessMessage("")
          println("Empty input... Just skipping it")
        }
        case msg: String => {
          val text: String = name + " sent: " + msg
          sendProcessMessage("Command  '" + msg + "' is unknown")
          println(text)
        }
      }
    }

    case PeerClosed => {
      context.parent ! UnpublishUserMessage(sessionId, address, role)
    }

    case _ => {
      println("Unknown message is received by UserSession")
    }
  }

  def sendProcessMessage(msg: String) : Unit = {
    sender ! Write(msg + "\r\n" + name + ">>")
  }

  def sendProcessMessage(actor: ActorRef, msg: String) : Unit = {
    val txt: String = msg match {
      case m:String if m.isEmpty => ""
      case m:String => m + "\r\n"
      case _ => ""
    }

    actor ! Write(txt + name + ">>")
  }

}

sealed trait Role

case class AdminRole() extends Role {
  override def toString(): String = {"Admin role"}
}

case class GuestRole() extends Role{
  override def toString(): String = {"Guest role"}
}
