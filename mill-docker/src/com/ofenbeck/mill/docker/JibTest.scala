package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties
import mill.api.SystemStreams


object JibTestLogger extends mill.api.Logger{

  override def colored: Boolean = true

  override def systemStreams: SystemStreams =  new SystemStreams(System.out, System.err, System.in) 

  override def info(s: String): Unit = println(s"info: $s")

  override def error(s: String): Unit = println(s"error: $s")

  override def ticker(s: String): Unit = println(s"ticker: $s")

  override def debug(s: String): Unit = println(s"debug: $s")

  
} 


object JibTest extends App {
     
    val settings = DockerSettings("sha256:6fd955f66c231c1a946653170d096a28ac2b2052a02080c0b84ec082a07f7d12","ofenbeck/local:latest")
    JibBuilds.buildToLocalDockerDemon(settings, JibTestLogger)

}
