package com.akluev.timeserver
import akka.util.ByteString

object StrImplicits {
  implicit def Str2ByteStr(str:String): ByteString = ByteString(str)
  implicit def ByteStr2Str(str:ByteString): String = str.utf8String.replace("\r\n","").trim
}
