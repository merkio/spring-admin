package space.geek.examples.springadmin.trigger

import de.codecentric.boot.admin.server.domain.events.InstanceEvent
import de.codecentric.boot.admin.server.domain.events.InstanceRegisteredEvent
import de.codecentric.boot.admin.server.domain.events.InstanceRegistrationUpdatedEvent
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent
import de.codecentric.boot.admin.server.domain.values.InstanceId
import de.codecentric.boot.admin.server.services.AbstractEventHandler
import de.codecentric.boot.admin.server.services.InstanceRegistry
import de.codecentric.boot.admin.server.services.IntervalCheck
import de.codecentric.boot.admin.server.services.StatusUpdater
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class StatusUpdatedTrigger(
    private val instanceRegistry: InstanceRegistry,
    private val statusUpdater: StatusUpdater,
    publisher: Publisher<InstanceEvent>
) : AbstractEventHandler<InstanceEvent>(publisher, InstanceEvent::class.java) {

    private val log = KotlinLogging.logger {}
    private val intervalCheck: IntervalCheck

    init {
        log.info("Register new status update trigger")
        intervalCheck = IntervalCheck("status") { instance -> this.updateStatus(instance) }
    }

    override fun handle(publisher: Flux<InstanceEvent>): Publisher<Void> {
        return publisher
            .filter { event ->
                event is InstanceRegisteredEvent ||
                        event is InstanceRegistrationUpdatedEvent ||
                        event is InstanceStatusChangedEvent
            }
            .flatMap { event ->
                if (event is InstanceStatusChangedEvent &&
                    (event.statusInfo.isOffline || event.statusInfo.isUnknown)
                ) {
                    instanceRegistry.deregister(event.instance).then()
                } else {
                    updateStatus(event.instance).then()
                }
            }
    }

    override fun start() {
        super.start()
        intervalCheck.start()
    }

    override fun stop() {
        super.stop()
        intervalCheck.stop()
    }

    fun setInterval(updateInterval: Duration) {
        intervalCheck.setInterval(updateInterval)
    }

    fun setLifetime(statusLifetime: Duration) {
        intervalCheck.setMinRetention(statusLifetime)
    }

    private fun updateStatus(instanceId: InstanceId): Mono<Void> {
        return statusUpdater.updateStatus(instanceId)
            .onErrorResume { e: Throwable? ->
                log.warn("Unexpected error while updating status for {}", instanceId, e)
                Mono.empty()
            }.doFinally { _ ->
                intervalCheck.markAsChecked(instanceId)
            }
    }
}