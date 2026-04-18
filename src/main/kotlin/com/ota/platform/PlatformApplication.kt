package com.ota.platform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class PlatformApplication

fun main(args: Array<String>) {
	runApplication<PlatformApplication>(*args)
}
