import datetime
import numpy as np
from datetime import timedelta

import matplotlib.pyplot as plt
import matplotlib
import pandas as pd


def _str_to_datetime(d):
    try:
        return datetime.datetime.strptime(d, '%d.%m.%Y %H:%M:%S')
    except:
        return None

def vect_str_to_datetime(d):
    return np.vectorize(_str_to_datetime)(d)


dirr='/home/ivan/trading/reports/delta/'

mdOhlc = pd.read_csv(dirr+'ohlcs.csv', index_col='DT', sep=';',  parse_dates=['DT'], date_parser=vect_str_to_datetime)
mdOhlc.sort_index(inplace=True)
mdOhlc.dropna(inplace=True)

trades = pd.read_csv(dirr+'trades.csv', index_col=False, sep=';',  parse_dates=['EntryDate', 'ExitDate'], date_parser=vect_str_to_datetime)
trades.dropna(inplace=True)

orders = pd.read_csv(dirr+'orders.csv', index_col='EntryDate', sep=';',  parse_dates=['EntryDate'], date_parser=vect_str_to_datetime)
orders.dropna(inplace=True)



def plotOrders(startDt,endDt):
    ords=orders[startDt:endDt]
    ords[(ords.BuySell==-1) & (ords.OrderType=='Limit')].ix[:,'Price'].plot(color='red', linestyle='_', marker=">")
    ords[(ords.BuySell==1) & (ords.OrderType=='Limit')].ix[:,'Price'].plot(color='blue', linestyle='_', marker=">")


def showTrade(trdIdx):
    rec=trades.ix[trdIdx, :]

    startDt = (rec['EntryDate'] - timedelta(minutes=120))
    endDt = (rec['ExitDate'] + timedelta(minutes=120))
    startDt=startDt.replace(second=0)
    endDt=endDt.replace(second=0)
    print(startDt)
    print(endDt)

    trds=trades[(trades.EntryDate > startDt) & (trades.EntryDate < endDt)]


    y_formatter = matplotlib.ticker.ScalarFormatter(useOffset=False)

    aa=plt.plot(mdOhlc[startDt:endDt].index, mdOhlc[startDt:endDt]['C'])

    aa[0].axes.yaxis.set_major_formatter(y_formatter)

    for a,r in trds.iterrows():
        col='red' if r['BuySell'] == -1 else 'green'
        plt.plot([r['EntryDate'],r['ExitDate']],[r['EntryPrice'],r['ExitPrice']],color=col, marker='o')

    plt.plot(mdOhlc[startDt:endDt].index, mdOhlc[startDt:endDt]['H'], color='red', linestyle=':')

    plt.plot(mdOhlc[startDt:endDt].index, mdOhlc[startDt:endDt]['L'], color='green', linestyle=':')

    #plotOrders(startDt,endDt)

    plt.show()

