package firelib.backtest;


interface IModifier<T,V>{
    void apply(T md, V val);
}
