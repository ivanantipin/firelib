import datetime
import numpy as np

import pandas as pd


def _str_to_datetime(d):
    try:
        return datetime.datetime.strptime(d, '%d.%m.%Y %H:%M:%S')
    except:
        return None

def vect_str_to_datetime(d):
    return np.vectorize(_str_to_datetime)(d)


mdOhlc = pd.read_csv('/home/ivan/trading/reports/bigorder/RI', index_col=False, sep=',',  parse_dates=['Date'], date_parser=vect_str_to_datetime)
mdOhlc.dropna(inplace=True)


trades = pd.read_csv('/home/ivan/trading/reports/bigorder/trades.csv', index_col=False, sep=';',  parse_dates=['EntryDate', 'ExitDate'], date_parser=vect_str_to_datetime)
trades.dropna(inplace=True)