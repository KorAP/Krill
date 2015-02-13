# Krill

A Corpus Retrieval Index using Lucene for Look-Ups

## Synopsis

## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora,
developed at the Institute for German Language (IDS) in Mannheim,
Germany.

## Features

Krill is the reference implementation for the
[KoralQuery](https://github.com/KorAP/Koral) protocol, covering
most of its query features, including ...

### Fulltext search

"Find all occurrences of the phrase 'sea monster'!"

"Find all case-insensitive words matching the regular expression /krak.*/"

### Token-based annotation search

"Find all plural nouns in accusative!"

### Span-based annotation search

"Find all nominal phrases!"

### Distance search

...

### Positional search

...

### Nested queries

...

### Multiple annotation resources

"Find all words marked as a noun by
[TreeTagger](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/treetagger.html)
and marked as an adjective by CoreNLP](https://github.com/stanfordnlp/CoreNLP)!"

### and many more ...

Virtual Collections;
partial highlightings;
Support for overlapping spans;
relational queries;
hierarchical queries ...

## Prerequisites

...

## Setup

  $ git clone https://github.com/KorAP/Krill
  $ cd Krill

To run the test suite, type in ...

  $ mvn test

To start the server, type in ...

  $ mvn compile exec:java

To compile and run the indexer, type ...

  $ mvn compile assembly:single

  $ java -jar target/KorAP-krill-X.XX.jar
    src/main/resources/korap.conf
    src/test/resources/examples/

## Development and License

**Authors**: [Nils Diewald](http://nils-diewald.de/), Eliza Margaretha

Copyright 2013-2015, IDS Mannheim, Germany

Krill is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Institute for German Language (IDS).

For recent changes and compatibility issues, please consult the
[Changes](https://raw.githubusercontent.com/KorAP/Krill/master/Changes)
file.

Krill is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).

To cite this work, please ...

## References and bundled Software

Named entities annotated in the test data by CoreNLP were using
models based on:

  Manaal Faruqui and Sebastian Padó (2010):
  Training and Evaluating a German Named Entity
  Recognizer with Semantic Generalization,
  Proceedings of KONVENS 2010,
  Saarbrücken, Germany
