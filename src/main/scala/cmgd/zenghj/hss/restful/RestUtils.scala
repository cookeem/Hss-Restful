package cmgd.zenghj.hss.restful

import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import cmgd.zenghj.hss.common.CommonUtils._
import cmgd.zenghj.hss.es.EsUtils._
import cmgd.zenghj.hss.HssRestful.ec

import akka.http.scaladsl.server.Directives._
import play.api.libs.json.Json

import scala.async.Async._
/**
  * Created by cookeem on 16/8/12.
  */
object RestUtils {
  val queryRoute = post {
    path("json" / "query") {
      formFieldMap { params =>
        val searchType = paramsGetString(params, "searchType", "pro")
        val fieldsStr = paramsGetString(params, "fields", "") //逗号分隔格式
        val page = paramsGetInt(params, "page", 1)
        val count = paramsGetInt(params, "count", 10)
        val descSortNum = paramsGetInt(params, "descSort", 1) //等于1表示true
        val fromStartTime = paramsGetString(params, "fromStartTime", "")
        val toStartTime = paramsGetString(params, "toStartTime", "")
        //json字符串格式: [{"field":"f1", "term":"t1"},{"field":"f2", "term":"t2"}]
        val termFieldsStr = paramsGetString(params, "termFields", "")
        val isdn = paramsGetString(params, "isdn", "")
        val epslock = paramsGetInt(params, "epslock", -1)
        val status4G = paramsGetInt(params, "status4G", -1)
        var termFieldsJsonStr = ""
        if (searchType == "4g") {
          var termFieldArr = Array[(String ,String)](("Operation", "MOD_LCK"))
          if (status4G == 0) {
            termFieldArr = termFieldArr :+ ("Status" ,"FAILED")
          } else if (status4G == 1) {
            termFieldArr = termFieldArr :+ ("Status" ,"SUCCESSFUL")
          }
          if (epslock == 0) {
            termFieldArr = termFieldArr :+ ("FullRequest", "EPSLOCK TRUE EPSLOCK")
          } else if (epslock == 1) {
            termFieldArr = termFieldArr :+ ("FullRequest", "EPSLOCK FALSE EPSLOCK")
          }
          if (isdn.length >= 11) {
            if (isdn.length == 11) {
              termFieldArr = termFieldArr :+ ("FullRequest", s"86$isdn")
            } else if (isdn.length == 13 && isdn.startsWith("86")) {
              termFieldArr = termFieldArr :+ ("FullRequest", isdn)
            }
          }
          val json = Json.toJson(
            termFieldArr.map{ case (k,v) =>
              Map("field" -> k, "term" -> v)
            }
          )
          termFieldsJsonStr = Json.stringify(json)
        } else {
          termFieldsJsonStr = termFieldsStr
        }
        val jsonFuture = async {
          val fields = fieldsStr.split(",")
          val descSort = if (descSortNum == 1) true else false
          val termFieldMaps: Array[Map[String, String]] = Json.parse(termFieldsJsonStr).as[Array[Map[String, String]]]
          val termFields = termFieldMaps.map{ m =>
            val field = m("field")
            val term = m("term")
            (field, term)
          }
          val json = esQuery(
            fields = fields,
            page = page,
            count = count,
            descSort = descSort,
            fromStartTime = fromStartTime,
            toStartTime = toStartTime,
            termFields = termFields
          )
          val jsonBody = Json.stringify(json)
          HttpEntity(ContentTypes.`application/json`, jsonBody)
        } recover {
          case e: Throwable =>
            val errmsg = s"query error: fieldsStr = $fieldsStr, fromStartTime = $fromStartTime, toStartTime = $toStartTime, termFieldsStr = $termFieldsStr. ${e.getMessage}, ${e.getCause}, ${e.getClass}, ${e.getStackTrace.mkString("\n")}"
            consoleLog("ERROR", errmsg)
            val json = Json.obj(
              "errmsg" -> errmsg
            )
            val jsonBody = Json.stringify(json)
            HttpEntity(ContentTypes.`application/json`, jsonBody)
        }
        complete(jsonFuture)
      }
    }
  }


}
