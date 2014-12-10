package firelib.common.timeservice

import java.time.Instant


trait TimeService{
    def currentTime :Instant
}

class TimeServiceReal extends TimeService{
    override def currentTime: Instant = Instant.now()
}

trait TimeServiceComponent {
    var timeService : TimeService = _
}

trait TimeServiceManagedComponent {

    val timeServiceManaged = new TimeServiceManaged

    class TimeServiceManaged extends TimeService {

        var dtGmt : Instant = Instant.EPOCH

        override def currentTime: Instant = dtGmt
    }

}

