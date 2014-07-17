package firelib.backtest

trait IQuoteListener[T] {
    def AddQuote(idx: Int, quote: T);
}
