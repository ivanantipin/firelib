import datetime
import numpy as np

import pandas as pd


filename='/home/ivan/tmp/report/trades.csv'


dateparse = lambda x: pd.datetime.strptime(x, '%Y-%m-%d %H:%M:%S')

lst=[]

def _str_to_datetime(d):
    try:
        return datetime.datetime.strptime(d, '%d.%m.%Y %H:%M:%S')
    except:
        return None

def vect_str_to_datetime(d):
    return np.vectorize(_str_to_datetime)(d)





trades = pd.read_csv(filename, sep=';',  parse_dates=[2, 4] ,date_parser=vect_str_to_datetime)

trades['ExitDate']