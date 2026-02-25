package com.forge.webide

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ForgeWebIdeApplication

fun main(args: Array<String>) {
    runApplication<ForgeWebIdeApplication>(*args)
}
