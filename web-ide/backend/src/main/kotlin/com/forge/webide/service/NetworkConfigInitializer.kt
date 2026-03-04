package com.forge.webide.service

import com.forge.webide.repository.OrgEnvConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * On startup, restore any DNS configuration stored in org_env_configs (category=network, key=DNS_SERVERS).
 * This ensures that custom DNS settings survive container restarts.
 */
@Component
class NetworkConfigInitializer(
    private val envConfigRepository: OrgEnvConfigRepository,
    private val networkConfigService: NetworkConfigService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(NetworkConfigInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val dnsConfig = envConfigRepository.findAll()
            .firstOrNull { it.category == "network" && it.configKey == "DNS_SERVERS" && !it.configValue.isNullOrBlank() }

        if (dnsConfig != null) {
            logger.info("Restoring DNS config from database: {}", dnsConfig.configValue)
            networkConfigService.applyDns(dnsConfig.configValue!!)
        } else {
            logger.debug("No custom DNS config found in database, using container defaults")
        }
    }
}
