package org.bitcoins.testkit

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.{BeforeAndAfterAll, Suite}

trait EmbeddedPg extends BeforeAndAfterAll { this: Suite =>

  lazy val pgEnabled: Boolean = sys.env.contains("PG_ENABLED")

  lazy val pg: Option[EmbeddedPostgres] = {

    if (pgEnabled) {
      val p = EmbeddedPostgres
        .builder()
        .setServerConfig("max_connections", "25")
        .setServerConfig("shared_buffers", "1MB")
        .start()
      Some(p)
    } else {
      None
    }
  }

  def pgUrl(): Option[String] =
    pg.map(_.getJdbcUrl(userName = "postgres", dbName = "postgres"))

  override def afterAll(): Unit = {
    super.afterAll()
    val _ = pg.foreach(_.close())
    ()
  }

  def executePgSql(sql: String): Unit =
    pg.foreach { pg =>
      try {
        val conn = pg.getPostgresDatabase.getConnection
        try {
          val st = conn.createStatement()
          try {
            st.execute(sql)
          } finally st.close()
        } finally conn.close()
      } catch {
        case ex: Throwable =>
          println(sql)
          ex.printStackTrace()
      }
    }

}
