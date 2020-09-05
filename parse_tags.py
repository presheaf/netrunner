import edn_format
import csv
import pathlib
import sys
from collections import defaultdict

csvpath = sys.argv[1]
outpath = sys.argv[2]
cardtags = defaultdict(set)

with open(csvpath) as f:
    rdr = csv.reader(f.readlines())
    for (cardname, *tags) in rdr:
        if not cardname:
            continue
        cardtags[cardname] = list(
            [t for t in tags if t]
        )
    

for cname, ctag in {'Sure Gamble': 'Gamble',
                    'Hedge Fund': 'Hedge',
                    'Jackson Howard': 'Jackson',
                    'Crypsis': 'Crypsis'}.items():
    cardtags[cname].append(ctag)
        
# alltags = set(sum([[t for t in _tags]
#                    for _tags in cardtags.values()], start=[]))

with open(outpath, 'w') as f:
    f.write(edn_format.dumps(cardtags))

    
