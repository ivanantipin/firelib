package firelib.common.reader

import java.time.Instant
import firelib.common.config.TickerConfig
import firelib.domain.Timed

/**
 * Created by ivan on 9/4/14.
 */
trait ReadersFactory extends ((Seq[TickerConfig], Instant) => Seq[SimpleReader[Timed]])
