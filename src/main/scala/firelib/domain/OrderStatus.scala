package firelib.domain

object OrderStatusEnum {
    val New = new OrderStatus("New")
    val Accepted = new OrderStatus("Accepted")
    val PendingCancel = new OrderStatus("PendingCancel")
    val CancelFailed = new OrderStatus("CancelFailed")
    val Done = new OrderStatus("Done")
    val Rejected = new OrderStatus("Rejected")
    val Cancelled = new OrderStatus("Cancelled")

}

sealed class OrderStatus(val Name: String) {

    def IsFinal: Boolean = {
        this == OrderStatusEnum.Rejected || this == OrderStatusEnum.Done || this == OrderStatusEnum.Cancelled;
    }

    def IsPending: Boolean = {
        this == OrderStatusEnum.New || this == OrderStatusEnum.PendingCancel;
    }

    def IsLiveAccepted: Boolean = {
        this == OrderStatusEnum.Accepted
    }

    override def toString: String = Name

}



