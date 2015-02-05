# Seaweeds

Search Engine for General Corpus Queries

## Synopsis

## Description

Seaweeds is a [Lucene](https://lucene.apache.org/) based search
engine for large corpora, providing support for complex linguistic queries.

## Features

Seaweeds provides ...
* fulltext search
  (/Give me all occurrences of the phrase "tree hug"!/)
* token-based annotation search
  (/Give me all plural nouns in accusative!/)
* span-based annotation search
  (/Give me all nominal phrases!/)
* support for multiple annotation sources
  (/Give me all words marked as a noun by TreeTagger and marked as an adjective by CoreNLP!/)
* support for complex queries ..
  Example
* support for conflicting span-based annotations
  (i.e. overlapping spans)
* support for multiple query languages by using
  the [CoralQuery](https://github.com/KorAP/Koral)
  protocol


## Prerequisites
...

## Starting

  $ git clone https://github.com/korap/Seaweeds
  $ cd Seaweeds

To run the test suite, type ...

  $ mvn test

To start the server, type ...

  $ mvn compile exec:java

To compile and run the indexer, type ...

  $ mvn compile assembly:single

  $ java -jar target/KorAP-lucene-index-X.XX.jar
    src/main/resources/korap.conf
    src/test/resources/examples/

## Development

For recent changes, please consult the Changes file.

## Reference

Authors: Nils Diewald, Eliza Margaretha

Copyright 2013-2015, IDS Mannheim, Germany

Seaweeds is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform

To cite this work, please ...

## Bundled Software

Named entities annotated in the test data by CoreNLP were using
models based on:

  Manaal Faruqui and Sebastian Padó (2010):
  Training and Evaluating a German Named Entity
  Recognizer with Semantic Generalization,
  Proceedings of KONVENS 2010,
  Saarbrücken, Germany
