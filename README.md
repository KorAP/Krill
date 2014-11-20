KorAP Lucene Index
==================

KorAP is available at
https://korap.ids-mannheim.de/

Prerequisites
-------------

Installation/Starting
---------------------

To run the test suite, type

  $ mvn test

To start the server, type

  $ mvn compile exec:java

To compile the indexer, type

  $ mvn compile assembly:single

To run the indexer, type

  $ java -jar target/KorAP-lucene-index-X.XX.jar
    src/main/resources/korap.conf
    src/test/resources/examples/

Limitations
-----------

### Tokenization

The Lucene backend is not character but token based.
In addition to that it only has support for one single tokenization.
Although it supports multiple annotations on tokenizations, these
annotations have to match the basic token's character offsets.

Token annotations that do not match the basic tokenization are
not indexed. Span annotations, that span a smaller range than one
basic token, will not be indexed as well.

Tokens are only indexed in case they are word tokens, i.e. not
punctuations. This limitation is necessary to make distance query
work on word levels.

### Repetitions

The maximum value for repetitions is 100.

### Distances

The maximum value for distance units is 100.

Copyright
---------

Copyright 2014, IDS Mannheim, Germany
Authors: Nils Diewald, Eliza Margaretha and contributors.

Citation
--------

???

Further References
------------------

Named entities annotated in the test data by CoreNLP was done using
models based on:
Manaal Faruqui and Sebastian Padó (2010):
Training and Evaluating a German Named Entity
Recognizer with Semantic Generalization,
Proceedings of KONVENS 2010,
Saarbrücken, Germany
