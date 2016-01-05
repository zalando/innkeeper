package org.zalando.spearheads.innkeeper

import com.whisk.docker.DockerContainer
import com.whisk.docker.config.DockerKitConfig

/**
  * @author dpersa
  */
trait DockerInnkeeperService extends DockerKitConfig {

  val innkeeperContainer = configureDockerContainer("docker.innkeeper")

  abstract override def dockerContainers: List[DockerContainer] =
    innkeeperContainer :: super.dockerContainers
}
