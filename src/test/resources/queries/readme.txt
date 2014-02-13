bsp1:   [base=foo]|([base=foo][base=bar])* meta author=Goethe&year=1815
bsp1b:  [base=foo]|([base=foo][base=bar]) meta author=Goethe&year=1815
bsp2:   ([base=foo]|[base=bar])[base=foobar]
bsp3:   shrink({[base=Mann]})
bsp4:   shrink({[base=foo]}[orth=bar])
bsp5:   shrink(1:[base=Der]{1:[base=Mann]}) 
bsp6:   [base=Katze]
bsp7:   [base!=Katze]
bsp8:   [!base=Katze]
bsp9:   [base=Katze&orth=Katzen]
bsp10:  [base=Katze][orth=und][orth=Hunde]
bsp11:  [!(base=Katze&orth=Katzen)]
bsp12:  contains(<np>,[base=Mann])
bsp13:  startswith(<np>,[!pos=Det])
bsp13b: startswith(<np>,[pos=Det])
bsp14:  'vers{2,3}uch'
bsp15:  [orth='vers.*ch']
bsp16:  [(base=bar|base=foo)&orth=foobar]
bsp17:  within(<np>,[base=Mann])

// Based on KorAP-querySerialization/examples/
cosmas3:  "das /+w1:3 Buch" # word-distance constraint
cosmas4:  "das /+w1:3,s1:1 Buch" # combined word-distance and sent-distance constraint
cosmas10: "Institut für $deutsche Sprache" # finds both
cosmas16: "$wegen #IN(L) <s>"  # finds 'wegen' at beginning of sentence, also when capitalised
cosmas17: "#BED($wegen , +sa)" # equivalent to above
cosmas20: "MORPH(V) #IN(R) #ELEM(S)" # e.g. subordinate clauses
