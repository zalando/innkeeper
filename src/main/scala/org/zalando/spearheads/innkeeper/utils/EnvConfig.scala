package org.zalando.spearheads.innkeeper.utils

import com.google.inject.Inject
import com.typesafe.config.{Config, ConfigObject}
import scala.collection.immutable.Seq
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

  def getStringSeq(key: String): Seq[String]

  def getObject(key: String): ConfigObject

}

class InnkeeperEnvConfig @Inject() (val config: Config) extends EnvConfig {

  lazy val env = config.getString("innkeeper.env")

  override def getString(key: String): String = {
    Try {
      config.getString(envKey(key))
    }.getOrElse {
      config.getString(key)
    }
  }

  override def getInt(key: String): Int = {
    Try {
      config.getInt(envKey(key))
    }.getOrElse {
      config.getInt(key)
    }
  }

  override def getStringSet(key: String): Set[String] = {
    Try {
      config.getStringList(envKey(key)).toSet
    }.getOrElse {
      config.getStringList(key).toSet
    }
  }

  override def getStringSeq(key: String): Seq[String] = {
    Try {
      config.getStringList(envKey(key)).toList
    }.getOrElse {
      config.getStringList(key).toList
    }
  }

  override def getObject(key: String): ConfigObject = {
    Try {
      config.getObject(envKey(key))
    }.getOrElse {
      config.getObject(key)
    }
  }

  private def envKey(key: String): String = s"$env.$key"

}
