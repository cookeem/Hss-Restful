package cmgd.zenghj.hss.common

import java.io.File

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.JavaConversions._

/**
  * Created by cookeem on 16/7/28.
  */
object CommonUtils {
  val config = ConfigFactory.parseFile(new File("conf/application.conf"))

  val configEs = config.getConfig("elasticsearch")
  val configEsNodes = configEs.getString("nodes")
  val configEsClusterName = configEs.getString("cluster-name")
  val configEsHosts: Array[(String, Int)] = configEs.getConfigList("hosts").map { conf =>
    (conf.getString("host"), conf.getInt("port"))
  }.toArray
  val configEsIndexName = configEs.getString("index-name")
  val configEsTypeName = configEs.getString("type-name")
  val configEsNumberOfShards = configEs.getInt("number-of-shards")
  val configEsNumberOfReplicas = configEs.getInt("number-of-replicas")

  val configHttp = config.getConfig("http")
  val configHttpPort = configHttp.getInt("port")

  def consoleLog(logType: String, msg: String) = {
    val timeStr = new DateTime().toString("yyyy-MM-dd HH:mm:ss")
    println(s"[$logType] $timeStr: $msg")
  }

  //从参数Map中获取Int
  def paramsGetInt(params: Map[String, String], key: String, default: Int): Int = {
    var ret = default
    if (params.contains(key)) {
      try {
        ret = params(key).toInt
      } catch {
        case e: Throwable =>
      }
    }
    ret
  }

  //从参数Map中获取String
  def paramsGetString(params: Map[String, String], key: String, default: String): String = {
    var ret = default
    if (params.contains(key)) {
      ret = params(key)
    }
    ret
  }
}
