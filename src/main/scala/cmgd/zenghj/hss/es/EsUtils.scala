package cmgd.zenghj.hss.es

import java.net.InetAddress

import cmgd.zenghj.hss.common.CommonUtils._
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.sort.SortOrder
import play.api.libs.json._

import scala.collection.JavaConversions._

/**
  * Created by cookeem on 16/8/10.
  */
object EsUtils {
  var esClient: TransportClient = null
  try {
    val settings = Settings.settingsBuilder().put("cluster.name", configEsClusterName).build()
    esClient = TransportClient.builder().settings(settings).build()
    configEsHosts.foreach { case (host, port) =>
      esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port))
    }
  } catch {
    case e: Throwable =>
      consoleLog("ERROR", s"elasticsearch client init error: ${e.getMessage}, ${e.getCause}, ${e.getClass}, ${e.getStackTrace.mkString("\n")}")
  }

  def esQuery(fields: Array[String] = Array[String](), page: Int = 1, count: Int = 10, descSort: Boolean = true, fromStartTime: String = "", toStartTime: String = "", termFields: Array[(String, String)] = Array[(String, String)]()): JsValue = {
    var success = 0
    var errmsg = ""
    var rscount = 0L
    var took = 0L
    var data = Array[JsValue]()
    try {
      val request: SearchRequestBuilder = esClient.prepareSearch(configEsIndexName).setTypes(configEsTypeName).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      if (fields.nonEmpty) {
        request.addFields(fields: _*)
      }
      var iPage = 1
      var iCount = 10
      if (page > 0) {
        iPage = page
      }
      if (count > 0) {
        iCount = count
      }
      request.setFrom((iPage - 1) * iCount).setSize(iCount)
      if (descSort) {
        request.addSort("StartTime", SortOrder.DESC)
      } else {
        request.addSort("StartTime", SortOrder.ASC)
      }
      request.setHighlighterPreTags("""##begin##""").setHighlighterPostTags("""##end##""")
      if (fromStartTime != "" && toStartTime != "" && fromStartTime < toStartTime) {
        request.setPostFilter(
          QueryBuilders.rangeQuery("StartTime")
            .gte(fromStartTime)
            .lte(toStartTime)
        )
      }
      val query: BoolQueryBuilder = QueryBuilders.boolQuery()
      if (termFields.nonEmpty) {
        termFields.foreach { case (field, term) =>
          if (term.trim != "") {
            request.addHighlightedField(field, 0, 0)
            if (field == "FullRequest" || field == "FullResponse" || field == "Target" || field == "RootLogId") {
              term.split("\\W+").foreach { str =>
                query.must(QueryBuilders.termQuery(field, str.toLowerCase))
              }
            } else {
              query.must(QueryBuilders.termQuery(field, term))
            }
          }
        }
      }
      request.setQuery(query)
      //执行查询
      val response: SearchResponse = request.execute().actionGet()

      took = response.getTook.getMillis
      rscount = response.getHits.getTotalHits
      response.getHits.getHits.foreach { sh =>
        val item = sh.fields().map { case (k, v) =>
          if (sh.highlightFields.containsKey(k)) {
            (k, sh.highlightFields.get(k).fragments().mkString)
          } else {
            (k, v.values().mkString)
          }
        }.toMap
        data = data :+ Json.toJson(item)
      }
      success = 1
    } catch {
      case e: Throwable =>
        consoleLog("ERROR", s"esQuery error: fields = ${fields.mkString(",")}, fromStartTime = $fromStartTime, toStartTime = $toStartTime, termFields = ${termFields.mkString(",")}. ${e.getMessage}, ${e.getCause}, ${e.getClass}, ${e.getStackTrace.mkString("\n")}")
        errmsg = s"esQuery error: fields = ${fields.mkString(",")}, fromStartTime = $fromStartTime, toStartTime = $toStartTime, termFields = ${termFields.mkString(",")}."
    }
    Json.obj(
      "success" -> success,
      "errmsg" -> errmsg,
      "took" -> took,
      "rscount" -> rscount,
      "data" -> data
    )
  }
}
