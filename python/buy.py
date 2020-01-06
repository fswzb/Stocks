import sys
import settings
import easytrader
from easytrader import grid_strategies

def marketBuy(stockCode, marketPrice, amount):
    marketPrice = float(1+0.07)*float(marketPrice)
    d = user.buy(stockCode, price=marketPrice, amount=amount)
    return d

user = easytrader.use('ths')
user.connect(settings.sXiaDanFile)
user.grid_strategy = grid_strategies.Xls

stockCode=sys.argv[1] #'600030'
price=sys.argv[2]
amount=sys.argv[3]
if len(sys.argv) == 5:
    marketPrice=float(sys.argv[4])

if 'marketPrice' not in locals():
    d = user.buy(stockCode, price=price, amount=amount)
else:
    d = marketBuy(stockCode, marketPrice, amount)
print(d['entrust_no'])

