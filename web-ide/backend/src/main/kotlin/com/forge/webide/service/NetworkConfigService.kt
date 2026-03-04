package com.forge.webide.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class NetworkConfigService {

    private val logger = LoggerFactory.getLogger(NetworkConfigService::class.java)
    private val resolvConf = File("/etc/resolv.conf")

    /**
     * Apply custom DNS servers to /etc/resolv.conf so all subprocesses (e.g. git) use them immediately.
     * servers: comma-separated, e.g. "10.0.0.1,8.8.8.8"
     *
     * If the file is not writable (e.g. running outside Docker), logs a warning and skips.
     */
    fun applyDns(servers: String) {
        val serverList = servers.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (serverList.isEmpty()) {
            clearDns()
            return
        }
        if (!resolvConf.canWrite()) {
            logger.warn(
                "/etc/resolv.conf is not writable — custom DNS [{}] cannot be applied at runtime. " +
                "Restart the backend container after saving to take effect.",
                serverList
            )
            return
        }
        val content = serverList.joinToString("\n") { "nameserver $it" } + "\n"
        resolvConf.writeText(content)
        logger.info("Applied custom DNS servers: {}", serverList)
    }

    fun clearDns() {
        if (!resolvConf.canWrite()) return
        // Restore Docker's default: empty resolv.conf (Docker will use daemon DNS on next restart)
        resolvConf.writeText("# DNS cleared by Forge — restart container to restore Docker defaults\n")
        logger.info("Cleared custom DNS config from /etc/resolv.conf")
    }
}
