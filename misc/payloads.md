# Payload Handling in Krill
Apache Lucene supports payloads as arbitrary byte sequences to store information for terms specific to any token position. Krill uses payloads to store various information in a compact way. This documents describes the payload information for index payloads (payloads stored in the index for different term concepts) and computed payloads (payloads created during the retrieval phase).

## Payload Type Identifier (PTI)
Payloads (both indexed and computed) have a leading byte indicating the type of the payload sequence. This is necessary because the origin (i.e. the requested term) of a payload is lost during the retrieval phase. Payload type identifiers range between 0 and 255 and have the length of a byte (\<b\>). In case a term has no payload, no payload type identifier is stored.

## Token-unique Identifier (TUI)
Terms, elements and relations in the index may contain token-unique identifiers (TUI) to distinguish between lucene-terms starting at the same token position. TUIs are used for matching attributes to terms, elements and relations, and to refer to terms and elements from relations. TUIs have the length of a short (\<s\>).

## Index Payloads

### Token position payloads
A token always has a special character payload storing the start and end offset of the token. The special character is a reference symbol for this payload, which is an underscore followed by the corresponding token position. For example, the _1$\<i\>0\<i\>3 is the special character payload for the token in position 1 describing that the token ranges from 0 to 3. This offset information is stored in integer.
Token payloads are not retrieved via SpanQueries and therefore do not have a PTI.

### Term payloads
Some terms are indexed with a TUI in their payloads, if they take part in a relation.
Terms may also have a certainty value attached, expressed as a byte (with a ranging certainty value from probably incorrect to probably correct (0-255)). For example:

    pos:NN$<b>128<s>1
    pos:NN$<b>129<b>34
    pos:NN$<b>130<s>1<b>34

*PTIs* (it’s a term payload, if the first bit is set):

    128: Term with a TUI
    129: Term with certainty value
    130: Term with TUI and certainty value

### Element payloads
Each element has payloads consisting of start and end character
offset information, the PTI, the token position which is the end of
the element, and the depth (in an abstract tree, with 0 being root).
In addition, it may have a TUI and a certainty value.
The offset information and the end element position are stored in
integer, whereas the TUI is stored in short, and the depth and certainty
information is stored as byte values. The stored data type for the end
element, the depth, the TUI and the certainty are written explicitly:
\<i\> for integer (4 bytes), \<s\> for short (2 bytes), and \<b\> for
byte (1 byte). For example:

    <>:s$<b>64<i>0<i>38<i>7<b>0

means that element \<s\> starts from character offset position 0 and
ends to character offset position 38. The element ends at token
position 7 which is stored in integer. It is a root element or no
further information on a tree level is given (depth=0).

    <>:s$<b>64<i>0<i>38<i>7<b>0<s>1

means \<s\> has an additional TUI.

    <>:s$<b>64<i>0<i>38<i>7<b>0<b>166

means \<s\> has an additional certainty value.

    <>:s$<b>64<i>0<i>38<i>7<b>0<s>1<b>166

means \<s\> has an additional TUI and a certainty value.

Elements may also be empty - meaning they behave as milestones.
In that case, character offsets are only given once.

    <>:s$<b>65<i>38<b>0

means \<s\> is a milestone at position 38 in root.

*PTIs* (It’s an element payload if the second bit is set):

    64  Element (with optional TUI and certainty)
    65  Milestone (with optional TUI and certainty)

### Relation payloads
Each relation are indexed with two instances for both directions.
he direction of a relation is determined by the following symbols: 

    > source to target
    < target to source

Each relation comprises two parts: a left part and a right part.
The positions of a relation instance always refer to the positions
of the left part, that are:
* the source token/span positions for \> relation 
* the target token/span positions for \< relation.

Relation payloads are varied based on the types of their left and
ight parts, which again can be either a source or a target of the
relation. 

* If the left part of a relation is an element, the end position
  of the element has to be stored in payload; if it is a token,
  nothing has to be stored additionally.
* The same applies for the right part of a relation. 

These positions are always stored in integer. 

Left-part TUI, right-part TUI, and relation TUI are also stored in
payloads. Relation TUIs are only necessary, in case an attribute
refers to it. It is necessary to have different length in payloads
for each of the following relations, so that the start and the end
ositions of the left and right parts can be determined. 

1) Term to term relation
has 1 integer for the right part token position, 1 short for the left-part TUI, 1 short for
right-part TUI and 1 short for the
relation TUI. For example:

    >:dependency$\<b\>32\<i\>3\<s\>5\<s\>4\<s\>3

has a token as the right part at (end) position 3, the source TUI 5, the target TUI 4 and the relation TUI 3.

2) Term to element relation
has 1 integer for the start position of the right part, 1 integer
or the end position of the right part, and 3 TUIs as above.

    >:dependency$\<b\>33\<i\>1\<i\>3\<s\>5\<s\>4\<s\>3

means the right part starts at token position 1 and ends at token
position 3.

3) Element to term relation
has 1 integer for end position of the left part, 1 integer for end position of the
right part, and 3 TUIs as above.

    >:dependency$\<b\>34\<i\>2\<i\>3\<s\>5\<s\>4\<s\>3

means the left part ends at token position 2, and right part is a
term ending at position 3.

4) Element to element relation
has 1 integer for end position of the left part, 1 integer for the
start position of the right part, 1 integer for end position of the
right part, and 3 TUIs as above.

    >:dependency$\<b\>35\<i\>2\<i\>3\<i\>4\<s\>5\<s\>4\<s\>3

means the left part ends at token position 2, the right part is an
element starting at position 3 and ending at position 4.

*PTIs* (it’s a relation payload if the third bit is set):

    32 term to term relation (with optional relation TUI and certainty)
    33 term to element relation (with optional relation TUI and certainty)
    34 element to term relation (with optional relation TUI and certainty)
    35 element to element relation (with optional relation TUI and certainty)

### Attribute payloads
Each attribute has two payloads: 

* the TUI of the term, element or relation to which the attribute
  belongs to (stored in short)
* for elements: the corresponding element end position (stored in integer)

For example:

    @:class=header$<b>17<i>6<s>1

means the attribute belongs to the term, element, or relation in the
same token position whose TUI is 1 and end position is 6.

*PTIs* (it’s an attribute payload, if the fourth bit is set):

    16. Attribute for Terms
    17. Attribute for Elements
    18. Attribute for Relations

## Computed Payloads
### Class payloads
are added "on the fly" to the payload collection of span queries and
contain a start position and an end position (both as integers) and
a class number as a byte.

    $<b>0<i>4<i>6<b>1

Classes start with 1.
Class payloads always have the length 10.

The *PTI* is 

    0 (no bit is set).

