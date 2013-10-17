package com.akluev.timeserver

import akka.actor.Actor
import java.io.{BufferedWriter, FileWriter, File}

class LogFileWriterActor[CustomMessage](fileName_ : String) extends Actor {
  val fileName: String = fileName_
  val sessionDelimiter: String = "************************************************"
  println("create file: " + fileName)
  val out = new BufferedWriter (new FileWriter(fileName, true))
  writeLine(sessionDelimiter)

  def receive = {
    case WriteToFileLogWriterMessage(str) => {
      writeLine(str)
    }
    case  GetLogFileLogWriterMessage => {
      println("LogFileWriterActor: received 'GetLogWriterActor'")
      val logFile = new File(fileName)
      sender ! logFile
    }
    case _ => {

    }
  }

  def writeLine (l: String): Unit = {
    val out = new BufferedWriter ( new FileWriter(fileName, true))
    out.append(l + "\r\n")
    out.close()
  }
}
