package firelib.common.tradegate

import firelib.common.misc.Topic
import firelib.common.{Order, Trade}
import firelib.domain.OrderState

case class OrderRec (val order : Order, val trdSubject : Topic[Trade], val ordSubject : Topic[OrderState])
