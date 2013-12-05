bsp1.json: [base=foo]|([base=foo][base=bar])* meta author=Goethe&year=1815
bsp2.json: ([base=foo]|[base=bar])[base=foobar]
bsp3.json: shrink({[base=Mann]})
bsp4.json: shrink({[base=foo]}[orth=bar])
bsp5.json: shrink(1:[base=Der]{1:[base=Mann]}) 
bsp6.json: [base=Katze]
bsp7.json: [base!=Katze]
bsp8.json: [!base=Katze]
bsp9.json: [base=Katze&orth=Katzen]
bsp10.json:	[base=Katze][orth=und][orth=Hunde]
bsp11.json:	[!(base=Katze&orth=Katzen)]
bsp12.json:	contains(<np>,[base=Mann])
bsp13.json:	startswith(<np>,[!pos=Det])
bsp13b.json:	startswith(<np>,[pos=Det])
bsp14.json:	'vers{2,3}uch'
bsp15.json:	[orth='vers.*ch']
bsp16.json: [(base=bar|base=foo)&orth=foobar]
bsp17.json:	within(<np>,[base=Mann])
