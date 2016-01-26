package org.zalando.spearheads.innkeeper.services.team

import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import spray.json.pimpString
import scala.collection.immutable.Seq

/**
  * @author dpersa
  */
class TeamJsonProtocolSpec extends FunSpec with Matchers {


  describe("Team") {

    it("should unmarshall a Team") {
      val team =
        """{"id":"pathfinder","id_name":"ignore","team_id":"ignore","type":"virtual","name":"ignore","mail":["ignore"]}"""
          .stripMargin.parseJson.convertTo[Team]
      team.id should be("pathfinder")
      team.teamType should be(Virtual)
    }

    it("should unmarshall an array of Teams") {
      val teams =
        """[{"id":"pathfinder","id_name":"ignore","team_id":"ignore","type":"virtual","name":"ignore","mail":["ignore"]},
          |{"id":"secondteam","id_name":"ignore","team_id":"ignore","type":"official","name":"ignore","mail":["ignore","ignore"]},
          |{"id":"thirdteam","id_name":"ignore","team_id":"ignore","type":"virtual","name":"ignore","mail":["ignore"]}]"""
          .stripMargin.parseJson.convertTo[Seq[Team]]
      val team = teams(1)
      team.id should be("secondteam")
      team.teamType should be(Official)
    }
  }
}
