package org.zalando.spearheads.innkeeper.utils

import com.google.inject.Inject;
import com.typesafe.config.Config
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * Tries to get a config dependent on the current environment.
 *
 * If it can't find it in the current environment, it will fallback to the default config
 *
 */
trait EnvConfig {

  def env: String

  def getString(key: String): String

  def getInt(key: String): Int

  def getStringSet(key: String): Set[String]

}

class InnkeeperEnvConfig @Inject() (val config: Config) extends EnvConfig {

  lazy val env = config.getString("innkeeper.env")

  override def getString(key: String): String = {
    Try {
      config.getString(s"$env.$key")
    }.getOrElse {
      config.getString(key)
    }
  }

  override def getInt(key: String): Int = {
    Try {
      config.getInt(s"$env.$key")
    }.getOrElse {
      config.getInt(key)
    }
  }

  override def getStringSet(key: String): Set[String] = {
    Try {
      config.getStringList(s"$env.$key").toSet
    }.getOrElse {
      config.getStringList(key).toSet
    }
  }
}
