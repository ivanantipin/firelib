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


mdOhlc = pd.read_csv('/home/ivan/trading/reports/audnzd/ohlcs.csv', index_col='DT', sep=';',  parse_dates=['DT'], date_parser=vect_str_to_datetime)
mdOhlc.dropna(inplace=True)
mdOhlc=mdOhlc[['C']]


trades = pd.read_csv('/home/ivan/trading/reports/audnzd/trades.csv', index_col=False, sep=';',  parse_dates=['EntryDate', 'ExitDate'], date_parser=vect_str_to_datetime)
trades.dropna(inplace=True)


def showTrade(trdIdx):
    rec=trades.ix[trdIdx, :]

    startDt = (rec['EntryDate'] - timedelta(minutes=500))
    endDt = (rec['ExitDate'] + timedelta(minutes=500))

    y_formatter = matplotlib.ticker.ScalarFormatter(useOffset=False)

    #aa=mdOhlc[startDt:endDt].plot()

    aa=plt.plot(mdOhlc[startDt:endDt].index, mdOhlc[startDt:endDt]['C'])

    aa[0].axes.yaxis.set_major_formatter(y_formatter)

    col='red' if rec['BuySell'] == -1 else 'green'

    plt.plot([rec['EntryDate'],rec['ExitDate']],[rec['EntryPrice'],rec['ExitPrice']],color=col)

    plt.show()
