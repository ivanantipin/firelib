namespace Fire.Common.Robot
{
    public interface IPredictorClient
    {
        string SendTrain(double response, double[] features);
        string Predict(double[] features);
    }
}