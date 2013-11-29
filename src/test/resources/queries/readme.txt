bsp1.json: [base=foo]|([base=foo][base=bar])* meta author=Goethe&year=1815
bsp2.json: ([base=foo]|[base=bar])[base=foobar]
bsp3.json: shrink({[base=Mann]})
bsp4.json: shrink({[base=foo]}[orth=bar])
bsp5.json: shrink(1:[base=Der]{1:[base=Mann]}) 
