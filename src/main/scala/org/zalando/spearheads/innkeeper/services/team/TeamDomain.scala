package org.zalando.spearheads.innkeeper.services.team

/**
 * @author dpersa
 */
case class Team(name: String, teamType: TeamType)

trait TeamType

case object Official extends TeamType

case object Virtual extends TeamType
