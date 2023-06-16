package space.geek.examples.springadmin

import de.codecentric.boot.admin.server.config.EnableAdminServer
import de.codecentric.boot.admin.server.domain.events.InstanceEvent
import de.codecentric.boot.admin.server.services.InstanceRegistry
import de.codecentric.boot.admin.server.services.StatusUpdater
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import space.geek.examples.springadmin.trigger.StatusUpdatedTrigger

@EnableAdminServer
@SpringBootApplication
class SpringAdminApplication {

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun statusUpdatedTrigger(
        instanceRegistry: InstanceRegistry,
        statusUpdater: StatusUpdater,
        publisher: Publisher<InstanceEvent>
    ): StatusUpdatedTrigger {
        return StatusUpdatedTrigger(instanceRegistry, statusUpdater, publisher)
    }
}

fun main(args: Array<String>) {
    runApplication<SpringAdminApplication>(*args)
}
