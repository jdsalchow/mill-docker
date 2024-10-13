package com.ofenbeck.mill.docker
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption

object JibImageWithCredentials {

  def imageFactory(
      imageReference: ImageReference,
      credentialsEnvironment: (String, String),
      logger: mill.api.Logger 
  ): RegistryImage = {
    
    val image                      = RegistryImage.named(imageReference)
    val factory                    = CredentialRetrieverFactory.forImage(imageReference, JibLogging.getEventLogger(logger))
    val (usernameEnv, passwordEnv) = credentialsEnvironment

    image.addCredentialRetriever(retrieveEnvCredentials(usernameEnv, passwordEnv))
  }

  private def retrieveEnvCredentials(usernameEnv: String, passwordEnv: String): CredentialRetriever =
    new CredentialRetriever {
      def retrieve(): java.util.Optional[Credential] = {
        val option = for {
          username <- sys.env.get(usernameEnv)
          password <- sys.env.get(passwordEnv)
        } yield Credential.from(username, password)
        option.toJava
      }
    }
}
