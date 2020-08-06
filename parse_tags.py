import edn_format
import csv
import pathlib
from collections import defaultdict

csvpath = '/home/karlerik/Downloads/cardtags.csv'
outpath = '/home/karlerik/Downloads/cardtags.edn'
cardtags = defaultdict(set)

with open(csvpath) as f:
    rdr = csv.reader(f.readlines())
    for (cardname, _, *tags) in rdr:
        if not cardname:
            continue
        cardtags[cardname] = list([t for t in tags if t])
    

# alltags = set(sum([[t for t in _tags]
#                    for _tags in cardtags.values()], start=[]))

with open(outpath, 'w') as f:
    f.write(edn_format.dumps(cardtags))

    
