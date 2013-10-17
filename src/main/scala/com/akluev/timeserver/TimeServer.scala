package com.akluev.timeserver

import akka.actor.{Props, ActorSystem}

object TimeServer extends App {

   override def main(args: Array[String]) {
     var adminName: String = "admin"
     var adminPass: String = "admin"
     var guestCapacity: Int = 10
     if (args.length == 3 && args(0) != null && args(1) != null && args(2).matches("[0-9]")) {
       //Valid input parameters.
       adminName = args(0)
       adminPass = args(1)
       guestCapacity = args(2).toInt
     }
     println("Current settings: \r\n" +
             " admin name: " + adminName + "\r\n" +
             " admin pass: " + adminPass + "\r\n" +
             " guest capacity: "  + guestCapacity + "\r\n")
     ActorSystem().actorOf(Props(classOf[Server], adminName, adminPass, guestCapacity))
   }
 }